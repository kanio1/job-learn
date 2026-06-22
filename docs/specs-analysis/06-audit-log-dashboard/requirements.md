# Requirements Document

## Introduction

This feature adds a **global, searchable audit log** to the Payment Quality Engineering Lab: an append-only record of who did what, to which resource, in which tenant, with what outcome, exposed read-only through a backend REST API and a Dashboard page. It is **Spec #4** of the Playwright/SDET learning roadmap and is a **brownfield enhancement** that extends — never rewrites — the existing backend (`apps/backend`), the existing Nuxt frontend (`apps/frontend`), and the existing Keycloak realm.

The audit log is **system-written, never user-written**. There are no create, update, or delete endpoints. Audit rows are produced by a new backend `audit` Spring Modulith module that **consumes Spring Modulith application events** published by the emitting modules (merchant, payment, and the `iam`/user-management surface) and persists one `audit_event` row per consumed event. This decouples audit capture from the emitting modules: an emitter publishes a domain event and is unaware of how the event is recorded. The audit log is therefore an **eventual, observational record** of actions, not a synchronous part of any write transaction's contract.

A verified current-state fact this spec rests on: **no `ApplicationEventPublisher` usage and no `@ApplicationModuleListener` exist anywhere in the backend today**, and there are no published domain events. Therefore the emitting modules **must be extended to publish events** before any action can be audited. That extension is an explicit cross-spec / cross-module touch point captured in the Implementation Sequencing & Prerequisites section and in the relevant requirements. The existing `PaymentOrderStatusHistory` is a **payment-domain-specific** status trail bound to a single payment order; it is **not** the global audit log and is neither replaced nor consumed by this feature.

This spec introduces a **new authority family** for audit reads — `platform:audit:read` and `tenant:audit:read` — which is deliberately **distinct** from the pre-existing payment-scoped `platform:payments:audit` authority. The new family requires the `iam-roles-and-keycloak-login` realm composite roles to be **extended**, the backend authority catalog and `KeycloakRealmRoleConverter` allowlist to be **extended**, and the frontend RBAC matrix to be **reconciled** (the existing `canReadAudit` capability maps to payment audit, while this feature needs a global audit-log capability). These are explicit cross-spec dependencies.

The audit store is a **real database table** (`audit_event`) created by a Flyway migration on PostgreSQL 18, owned by the new `audit` module. It is append-only at the API boundary. Date filtering on the Dashboard uses the native `<input type="date">` control for accessibility rather than a custom calendar.

Because this is a QA/SDET learning application, the feature is designed to create realistic future test situations (audit-trail verification after an action, date-range filtering, deep-linking to a single entry, tenant-scoped audit visibility, and time mocking with `page.clock`). **No Playwright test files are created by this spec**; a conceptual Future Playwright Scenarios section records the lessons to be written later. This document is a specification only and contains **no implementation**.

The existing REST contract conventions are preserved exactly: `application/problem+json` on every 4xx, `X-Correlation-ID` on every response, `Vary` on responses whose representation depends on request headers, masked `404` for cross-tenant single-entry reads, and `403` for unauthorized access. No sensitive data (bearer tokens, passwords, PAN/CVV, personal financial data) is ever written to an audit row or returned in an audit response.

## Glossary

- **Backend_API**: The Payment Quality Lab Spring Boot REST API exposed under `/api/*`, including its Spring Security authorization rules and service layer.
- **Audit_Module**: The new Spring Modulith module introduced by this spec (recommended package `lab.paymentquality.audit`) that owns the Audit_Event entity, the `audit_event` table, the Audit_Listener, and the audit read endpoints. The module name is recorded as an Open Question.
- **Audit_Event**: A single persisted, append-only record of one audited action, mapped to one row of the `audit_event` table.
- **Audit_Listener**: The Audit_Module component, annotated with `@ApplicationModuleListener`, that consumes published Domain_Audit_Events and persists one Audit_Event per consumed event.
- **Domain_Audit_Event**: A Spring application event published by an Emitting_Module to signal that an auditable action occurred. These events do not exist today and must be introduced by extending the Emitting_Modules.
- **Emitting_Module**: An existing backend module whose actions are audited and which must be extended to publish Domain_Audit_Events. The Emitting_Modules in this spec are the merchant module, the payment module, and the `iam`/user-management surface from Spec #3.
- **Audited_Action**: A specific operation recorded as an Audit_Event, identified by a stable `action` string (for example `USER_CREATED`, `MERCHANT_SUSPENDED`, `PAYMENT_CAPTURED`).
- **Outcome**: The result classification stored on an Audit_Event. Allowed values are `SUCCESS`, `DENIED`, and `FAILED`.
- **Actor_Subject**: The stable identifier of the principal who performed the Audited_Action, derived from the authenticated JWT subject. It is stored but is not the primary display value.
- **Actor_Display**: The safe, human-readable label for the actor (for example a username), surfaced in audit responses and the Dashboard. It exposes no more identity information than the existing status-history surface already exposes.
- **Target_Type**: The kind of resource an Audited_Action acted on (for example `USER`, `MERCHANT`, `PAYMENT_ORDER`).
- **Target_Id**: The identifier of the specific resource an Audited_Action acted on.
- **Correlation_Id**: The `X-Correlation-ID` value associated with the request that produced the Audited_Action, stored on the Audit_Event to link the audit record to request traces.
- **Audit_Authority**: One of the two new Fine_Grained_Authorities introduced by this spec: `platform:audit:read` (cross-tenant audit read) and `tenant:audit:read` (own-tenant audit read).
- **Payment_Audit_Authority**: The pre-existing, unrelated Fine_Grained_Authority `platform:payments:audit`, which governs payment-domain audit data and is NOT extended, reused, or repurposed by this spec.
- **Fine_Grained_Authority**: A low-level permission string enforced by the Backend_API (for example `platform:merchants:read`). The complete pre-existing set is unchanged except for the additive Audit_Authorities defined by this spec.
- **Composite_Role**: One of the five named Keycloak realm composite roles defined by `iam-roles-and-keycloak-login`: PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER, SUPPORT_AGENT, READ_ONLY_USER.
- **Role_Converter**: The backend `KeycloakRealmRoleConverter` that converts realm role names from the `realm_access.roles` JWT claim into Fine_Grained_Authority values via an allowlist mapping.
- **Authorities_Catalog**: The backend `Authorities` constants class that declares each Fine_Grained_Authority literal enforced by the Backend_API.
- **Keycloak_Realm**: The `payment-quality` Keycloak realm defined by `infra/keycloak/realms/payment-quality-realm.json`.
- **Tenant_Reference**: The stable, human-readable natural-key string that identifies a tenant (for example `TENANT_ALPHA`), as defined by `tenant-model-and-isolation`. It is the value carried by the `tenant_id` JWT claim.
- **Tenant_Resolver**: The backend component introduced by `tenant-model-and-isolation` that resolves the acting principal's `tenant_id` claim to a Tenant and classifies the principal as platform-scoped or tenant-scoped via the Tenant's `tenant_type`.
- **Acting_Principal**: The authenticated user making an audit read request, identified by the validated JWT (granted authorities, `tenant_id` claim).
- **Platform_Scoped_Principal**: An Acting_Principal whose resolved Tenant has `tenant_type = PLATFORM`; entitled to read audit records across all tenants via `platform:audit:read`.
- **Tenant_Scoped_Principal**: An Acting_Principal restricted to a single tenant; entitled to read only Audit_Events whose `tenant_id` matches the principal's resolved Tenant_Reference via `tenant:audit:read`.
- **Dashboard**: The Nuxt 4 frontend application at `apps/frontend`.
- **Server_Proxy**: The Nuxt server routes (`server/api/**`, `server/utils/backendApi.ts`) that attach the bearer token server-side and forward requests to the Backend_API. The browser never holds the bearer token.
- **Audit_Page**: The Dashboard route `/admin/audit` that lists Audit_Events with filters and pagination and hosts the single-entry detail drawer.
- **Audit_Detail_Drawer**: The `USlideover` on the Audit_Page that displays a single Audit_Event, opened by selecting a row or by deep link.
- **Masked_Not_Found**: A `404 Not Found` response with the standard `not_found` problem shape returned for a resource that exists but is not visible to the Acting_Principal, so existence is not disclosed.
- **Problem_Response**: A `4xx` response with `Content-Type: application/problem+json` carrying the members `type`, `title`, `status`, `detail`, `instance`.
- **Rbac_Matrix**: The frontend capability matrix in `apps/frontend/app/utils/rbacMatrix.ts` that maps each Composite_Role to capability booleans.
- **Payment_Order_Status_History**: The existing payment-domain status trail (`GET /api/merchants/{merchantId}/payment-orders/{id}/history`) bound to a single payment order. It is NOT the global audit log and is out of scope for change.
- **Test_Id**: A stable `data-testid` attribute used as a future Playwright locator; stable means byte-identical across rebuilds and sessions and resolving to exactly one element per rendered page.

## Personas

- **Platform_Admin_User**: Holds the PLATFORM_ADMIN Composite_Role. Reads audit records across all tenants via `platform:audit:read`.
- **Support_Agent_User**: Holds the SUPPORT_AGENT Composite_Role. Reads audit records across all tenants via `platform:audit:read` to assist with operational investigations.
- **Tenant_Admin_User**: Holds the TENANT_ADMIN Composite_Role. Reads only the audit records of the Tenant_Admin_User's own resolved tenant via `tenant:audit:read`.
- **Merchant_Manager_User**: Holds the MERCHANT_MANAGER Composite_Role. Has no audit-read access.
- **Read_Only_Viewer**: Holds the READ_ONLY_USER Composite_Role. Has no audit-read access.
- **QA_Automation_Engineer**: Writes future REST Assured and Playwright tests; needs deterministic role/tenant fixtures, an explicit RBAC and tenant-isolation matrix, stable Test_Ids, predictable outcomes (403 forbidden, masked 404 cross-tenant single reads), and the ability to verify that an action produces a corresponding audit entry.

## In Scope

- A new Audit_Module in the Backend_API that owns the `audit_event` table, the Audit_Event entity, the Audit_Listener, and the read endpoints, with module boundaries verified by the existing Spring Modulith architecture tests.
- A Flyway migration that creates the `audit_event` table on PostgreSQL 18 with the columns `id`, `occurred_at`, `actor_subject`, `actor_display`, `action`, `target_type`, `target_id`, `tenant_id`, `correlation_id`, `outcome`, and supporting indexes on `occurred_at`, `tenant_id`, `actor_subject`, and `action`.
- Audit capture via Spring Modulith application events: the Audit_Listener consumes Domain_Audit_Events through `@ApplicationModuleListener` and persists one Audit_Event per event.
- The documented, cross-spec extension of the Emitting_Modules (merchant, payment, iam/user-management) to publish Domain_Audit_Events for the initial set of Audited_Actions, since no events exist today.
- Two read-only endpoints: `GET /api/audit` (filterable, paginated list) and `GET /api/audit/{id}` (single entry, deep-link target).
- A new additive Audit_Authority family (`platform:audit:read`, `tenant:audit:read`) and the documented extension of the `iam-roles-and-keycloak-login` realm composite roles, the Authorities_Catalog, and the Role_Converter allowlist to grant and convert them.
- Tenant scoping of audit reads using the Audit_Event `tenant_id` and the Tenant_Resolver classification of the Acting_Principal.
- A `/admin/audit` Audit_Page in the Dashboard: paginated, filterable list (actor, action, target type, from-date, to-date); URL-reflected filters; deep link to a single entry via `?entry={id}` opening the Audit_Detail_Drawer.
- A role-gated navigation link to the Audit_Page, visible only to PLATFORM_ADMIN, SUPPORT_AGENT, and TENANT_ADMIN.
- Native `<input type="date">` from/to date filtering on the Audit_Page.
- Read-only Dashboard UI states: loading, empty, filtered-empty, error, forbidden (403), and deep-link-not-found. No success, conflict, or create states.
- Reconciliation of the frontend Rbac_Matrix to express a global audit-log-viewing capability distinct from the existing payment-audit `canReadAudit`.
- Preservation of all existing REST contract conventions (problem+json, `X-Correlation-ID`, `Vary`, masked-404 cross-tenant single reads, 403 unauthorized).
- Backend-authoritative authorization and tenant isolation, independent of any UI gating.
- A conceptual Future Playwright Scenarios section (no test files).

## Out of Scope (Non-Goals)

- Any create, update, or delete endpoint for audit records; audit is append-only and written only by the Audit_Listener.
- Replacing, consuming, or modifying the existing Payment_Order_Status_History.
- Repurposing or extending the pre-existing `platform:payments:audit` Payment_Audit_Authority; the new audit-log family is distinct.
- Audit retention, archival, purging, rotation, or volume-management policy (recorded as an Open Question).
- Exporting audit records (CSV/file download), bulk actions, or notifications (separate roadmap items).
- Synchronous, transactional coupling of audit writes to the emitting operation's request/response contract (audit capture is event-driven and eventual).
- Capturing the full set of every conceivable action; only the documented initial set of Audited_Actions is in scope.
- Any Playwright or other automated frontend test files (deferred to future learning lessons).
- Tenant CRUD, user CRUD, or login flows (owned by their respective specs).
- PSP integration, Kafka, webhooks, settlement, or any item in the project-wide active non-goals.

## Resolved Decisions

Each decision records the original question for traceability, followed by the resolution and rationale. The acceptance criteria, matrices, and sequencing in this document are consistent with these decisions.

### Decision A: Audit capture mechanism — Spring Modulith events

**Original question:** Should audit rows be written synchronously inside each emitting operation (direct service-to-service call or shared audit service), or asynchronously by a dedicated module that consumes domain events?

**Resolved:** Use **Spring Modulith application events**. Each Emitting_Module (merchant, payment, iam/user-management) publishes a Domain_Audit_Event; the Audit_Module's Audit_Listener consumes it via `@ApplicationModuleListener` and persists one Audit_Event. **Rationale:** this decouples audit capture from emitters (an emitter does not depend on the Audit_Module or know how records are stored), teaches the Spring Modulith event model, and keeps the audit log an observational record rather than part of each operation's HTTP contract. **Cross-module consequence:** because **no `ApplicationEventPublisher` usage and no `@ApplicationModuleListener` exist today**, the Emitting_Modules must be extended to publish events; those events do not exist yet. The existing Payment_Order_Status_History is payment-domain-specific and is explicitly NOT the global audit log.

### Decision B: New audit authority family distinct from payment audit

**Original question:** Should audit-log reads reuse the existing `platform:payments:audit` authority, or introduce a new authority family?

**Resolved:** Introduce a **new family**: `platform:audit:read` (cross-tenant) and `tenant:audit:read` (own-tenant), distinct from `platform:payments:audit`. PLATFORM_ADMIN and SUPPORT_AGENT receive `platform:audit:read`; TENANT_ADMIN receives `tenant:audit:read`; MERCHANT_MANAGER and READ_ONLY_USER receive none. **Rationale:** the existing `platform:payments:audit` governs payment-domain audit data, not a global cross-domain audit log; conflating them would over-grant payment auditors and under-model tenant-scoped audit reads. **Cross-spec consequence:** the `iam-roles-and-keycloak-login` realm composite roles, the backend Authorities_Catalog, and the Role_Converter allowlist must each be extended additively to introduce and convert the new family.

### Decision C: Audit store is a real database table with a Flyway migration

**Original question:** Should audit records be persisted in a real database table, or derived/proxied like the user-management façade?

**Resolved:** The `audit_event` table is a **real PostgreSQL 18 table** created by a **Flyway migration**, owned by the Audit_Module, with columns `id` (UUID PK), `occurred_at` (timestamptz), `actor_subject`, `actor_display`, `action`, `target_type`, `target_id`, `tenant_id`, `correlation_id`, `outcome`, and indexes on `occurred_at`, `tenant_id`, `actor_subject`, and `action`. It is **append-only** at the API boundary (no update or delete endpoints). **Rationale:** audit data is first-class persistent platform data that must be queryable and filterable with predictable performance, unlike identity which is owned by Keycloak; a real indexed table teaches schema design, indexing for filter performance, and append-only modeling.

### Decision D: Native date input for date filtering

**Original question:** Should the from/to date filters use a custom calendar component or the native browser date control?

**Resolved:** Use the native `<input type="date">` control for both from and to filters. **Rationale:** the native control is accessible by default (keyboard, locale, assistive technology), reduces custom UI surface, and produces stable, predictable values for future Playwright tests, including `page.clock` time-mocking scenarios.

## Requirements

### Requirement 1: Audit Authority Family and Realm/Backend Extension

**User Story:** As a Platform_Admin_User, I want audit-read permissions modeled as a dedicated authority family granted through the existing composite roles, so that audit visibility is authorized consistently and separately from payment audit.

#### Acceptance Criteria

1. THE Keycloak_Realm SHALL define two additive Audit_Authorities exposed as realm authority roles: `platform:audit:read` and `tenant:audit:read`.
2. THE Keycloak_Realm SHALL extend the PLATFORM_ADMIN Composite_Role definition to include the `platform:audit:read` Audit_Authority.
3. THE Keycloak_Realm SHALL extend the SUPPORT_AGENT Composite_Role definition to include the `platform:audit:read` Audit_Authority.
4. THE Keycloak_Realm SHALL extend the TENANT_ADMIN Composite_Role definition to include the `tenant:audit:read` Audit_Authority.
5. THE Keycloak_Realm SHALL compose the MERCHANT_MANAGER and READ_ONLY_USER Composite_Roles without any Audit_Authority.
6. THE Backend_API SHALL declare the two Audit_Authorities in the Authorities_Catalog as new constants distinct from the pre-existing Payment_Audit_Authority `platform:payments:audit`.
7. THE Role_Converter SHALL convert the `platform:audit:read` and `tenant:audit:read` realm roles into their corresponding granted authorities by an additive extension of its allowlist mapping, without changing the conversion of any existing role.
8. THE Backend_API SHALL preserve the existing `platform:payments:audit` Payment_Audit_Authority unchanged and SHALL NOT grant audit-log read access through it.
9. WHEN the extended Keycloak_Realm import is loaded into a Keycloak instance, THE Keycloak_Realm SHALL import without error and SHALL expose the two Audit_Authorities and the extended composite-role mappings.

### Requirement 2: Audit Event Persistence Schema

**User Story:** As a QA_Automation_Engineer, I want audit records persisted in a real indexed table, so that audit reads are deterministic and filter performance is predictable for tests.

#### Acceptance Criteria

1. THE Backend_API SHALL create, via a Flyway migration, an `audit_event` table with the columns `id` (UUID, primary key), `occurred_at` (timestamp with time zone, not null), `actor_subject` (string, not null), `actor_display` (string, not null), `action` (string, not null), `target_type` (string, not null), `target_id` (string, not null), `tenant_id` (string, not null), `correlation_id` (string, not null), and `outcome` (string, not null).
2. THE Flyway migration SHALL define indexes on the `occurred_at`, `tenant_id`, `actor_subject`, and `action` columns to support filtered list queries.
3. THE Flyway migration SHALL constrain the `outcome` column to the values `SUCCESS`, `DENIED`, and `FAILED`.
4. THE Backend_API SHALL map the Audit_Event entity to the `audit_event` table with JPA mappings that pass startup schema validation while `ddl-auto` is set to `validate`.
5. WHEN the Backend_API starts against a database where the audit migration has run, THE Backend_API SHALL complete Flyway migration and JPA schema validation without error.
6. THE Audit_Module SHALL own the `audit_event` table and the Audit_Event entity, and no other module SHALL import the Audit_Module's internal persistence types.

### Requirement 3: Event-Driven Audit Capture

**User Story:** As a Platform_Admin_User, I want audited actions captured automatically from domain events, so that the audit log records actions without coupling each operation to the audit store.

#### Acceptance Criteria

1. THE Audit_Module SHALL provide an Audit_Listener annotated with `@ApplicationModuleListener` that consumes published Domain_Audit_Events.
2. WHEN an Emitting_Module publishes a Domain_Audit_Event, THE Audit_Listener SHALL persist exactly one Audit_Event recording the event's Actor_Subject, Actor_Display, Audited_Action, Target_Type, Target_Id, tenant Tenant_Reference, Correlation_Id, Outcome, and an `occurred_at` timestamp.
3. THE Backend_API SHALL set the Audit_Event `correlation_id` to the `X-Correlation-ID` value of the request that produced the Domain_Audit_Event.
4. THE Audit_Listener SHALL NOT be exposed as an HTTP endpoint, and THE Backend_API SHALL provide no endpoint that creates, updates, or deletes an Audit_Event.
5. WHERE a Domain_Audit_Event represents an action whose request carried no resolvable tenant, THE Audit_Listener SHALL record the Audit_Event with the tenant value carried by the event so that every Audit_Event has a non-null `tenant_id`.
6. THE Audit_Module SHALL depend only on the published Domain_Audit_Event contracts and SHALL NOT import any Emitting_Module's internal packages.
7. THE Emitting_Modules SHALL publish Domain_Audit_Events without depending on the Audit_Module's internal types, so that emitters remain unaware of how audit records are stored.

### Requirement 4: Initial Audited Actions and Event Sources

**User Story:** As a QA_Automation_Engineer, I want a documented initial set of audited actions and their sources, so that I can verify each action produces a corresponding audit entry.

#### Acceptance Criteria

1. THE iam/user-management Emitting_Module SHALL publish a Domain_Audit_Event for user creation, user update, and user role-assignment actions introduced by the `user-management` spec.
2. THE merchant Emitting_Module SHALL publish a Domain_Audit_Event for merchant creation, merchant activation, and merchant suspension actions.
3. THE payment Emitting_Module SHALL publish a Domain_Audit_Event for payment lifecycle actions authorize, capture, cancel, and refund.
4. THE Backend_API SHALL record each captured Audit_Event with a stable `action` string that identifies the Audited_Action.
5. THE Backend_API SHALL record the Outcome of each captured Audit_Event as `SUCCESS` for a completed action, `FAILED` for an action that failed due to a server-side or business error, and `DENIED` for an action rejected by authorization.
6. WHERE tenant-scoped access denials are published as Domain_Audit_Events, THE Audit_Listener SHALL record them as Audit_Events with Outcome `DENIED`; recording access denials is OPTIONAL and its inclusion is recorded as an Open Question.
7. THE Backend_API SHALL exclude bearer tokens, passwords, Temporary_Passwords, PAN, CVV, and other sensitive values from every Audit_Event field.

### Requirement 5: List Audit Events

**User Story:** As a Support_Agent_User, I want to list audit events with filters and pagination, so that I can investigate what actions occurred.

#### Acceptance Criteria

1. WHEN a Platform_Scoped_Principal holding `platform:audit:read` requests `GET /api/audit`, THE Backend_API SHALL return a paginated list of Audit_Events across all tenants.
2. WHEN a Tenant_Scoped_Principal holding `tenant:audit:read` requests `GET /api/audit`, THE Backend_API SHALL return only Audit_Events whose `tenant_id` matches the principal's resolved Tenant_Reference.
3. WHERE an `actor` filter parameter is supplied, THE Backend_API SHALL return only Audit_Events whose Actor_Subject or Actor_Display matches the supplied value.
4. WHERE an `action` filter parameter is supplied, THE Backend_API SHALL return only Audit_Events whose `action` equals the supplied value.
5. WHERE a `target_type` filter parameter is supplied, THE Backend_API SHALL return only Audit_Events whose `target_type` equals the supplied value.
6. WHERE a `from` date filter parameter is supplied, THE Backend_API SHALL return only Audit_Events whose `occurred_at` is on or after the start of the supplied date.
7. WHERE a `to` date filter parameter is supplied, THE Backend_API SHALL return only Audit_Events whose `occurred_at` is on or before the end of the supplied date.
8. THE Backend_API SHALL paginate the audit list using the same `page` and `size` parameter conventions used by the existing payment-order list endpoint, with a maximum page size of 100.
9. THE Backend_API SHALL order the audit list by `occurred_at` descending by default.
10. WHEN the Backend_API returns an audit-list response, THE Backend_API SHALL include the `X-Correlation-ID` response header and a `Vary` response header containing `Authorization`.
11. IF an Acting_Principal lacking both `platform:audit:read` and `tenant:audit:read` requests `GET /api/audit`, THEN THE Backend_API SHALL return a Problem_Response with status `403`.
12. WHEN a filter combination matches no Audit_Events, THE Backend_API SHALL return an empty page with status `200`.

### Requirement 6: Get Single Audit Event

**User Story:** As a Platform_Admin_User, I want to retrieve a single audit entry by id, so that I can deep-link to and inspect one recorded action.

#### Acceptance Criteria

1. WHEN a Platform_Scoped_Principal holding `platform:audit:read` requests `GET /api/audit/{id}` for an existing Audit_Event, THE Backend_API SHALL return that Audit_Event with status `200`.
2. WHEN a Tenant_Scoped_Principal holding `tenant:audit:read` requests `GET /api/audit/{id}` for an Audit_Event whose `tenant_id` matches the principal's resolved Tenant_Reference, THE Backend_API SHALL return that Audit_Event with status `200`.
3. IF a Tenant_Scoped_Principal requests `GET /api/audit/{id}` for an Audit_Event whose `tenant_id` does not match the principal's resolved Tenant_Reference, THEN THE Backend_API SHALL return Masked_Not_Found with status `404`.
4. IF an Acting_Principal requests `GET /api/audit/{id}` for an id that does not exist, THEN THE Backend_API SHALL return Masked_Not_Found with status `404` using the same response shape as criterion 3.
5. IF an Acting_Principal lacking both `platform:audit:read` and `tenant:audit:read` requests `GET /api/audit/{id}`, THEN THE Backend_API SHALL return a Problem_Response with status `403`.
6. WHEN the Backend_API returns a single Audit_Event response, THE Backend_API SHALL include the `X-Correlation-ID` response header and a `Vary` response header containing `Authorization`.
7. THE Backend_API SHALL include in the single Audit_Event response the `id`, `occurred_at`, `actor_display`, `action`, `target_type`, `target_id`, `tenant_id`, `correlation_id`, and `outcome`, and SHALL exclude every sensitive value.

### Requirement 7: REST Contract Conventions for Audit Endpoints

**User Story:** As a QA_Automation_Engineer, I want the audit endpoints to follow the platform's existing REST conventions, so that contract tests reuse the same assertions as the rest of the API.

#### Acceptance Criteria

1. WHEN the Backend_API returns any `4xx` response from an `/api/audit` endpoint, THE Backend_API SHALL use `Content-Type: application/problem+json` with the members `type`, `title`, `status`, `detail`, and `instance`.
2. THE Backend_API SHALL include an `X-Correlation-ID` response header on every `/api/audit` response, success or error.
3. THE Backend_API SHALL include a `Vary` response header containing `Authorization` on every `/api/audit` read response.
4. WHEN a cross-tenant single-entry read is denied for a Tenant_Scoped_Principal, THE Backend_API SHALL return Masked_Not_Found with status `404` and the standard `not_found` problem shape.
5. WHEN an audit read is requested by a principal lacking any Audit_Authority, THE Backend_API SHALL return a Problem_Response with status `403`.
6. THE Backend_API SHALL respond to an unsupported `Accept` header on an `/api/audit` endpoint with status `406` and to an unsupported HTTP method with status `405`, consistent with existing endpoints.
7. THE Backend_API SHALL exclude bearer tokens, passwords, and other sensitive values from every `/api/audit` response body, response header, and log entry.
8. THE Server_Proxy SHALL forward `/api/audit` requests to the Backend_API with the bearer token attached server-side and SHALL forward the `X-Correlation-ID` and `Vary` response headers to the browser.

### Requirement 8: Audit Page Listing and Filtering

**User Story:** As a Support_Agent_User, I want a Dashboard page that lists audit events with filters and pagination, so that I can investigate actions through the UI.

#### Acceptance Criteria

1. THE Dashboard SHALL provide an Audit_Page at the route `/admin/audit` that lists Audit_Events in a `UTable` with columns for `occurred_at`, Actor_Display, `action`, Target_Type, Target_Id, and Outcome.
2. THE Audit_Page SHALL provide an actor filter, an action filter, and a target-type filter, and SHALL provide from and to date filters rendered as native `<input type="date">` controls each wrapped in a `UFormField` with a visible label.
3. WHEN a user applies or changes a filter on the Audit_Page, THE Dashboard SHALL reflect the active filters in the URL query parameters so that the filtered view is shareable and restorable.
4. WHEN the Audit_Page loads with filter query parameters present in the URL, THE Dashboard SHALL initialize the filter controls and the requested list from those parameters.
5. THE Audit_Page SHALL paginate results using the backend `page` and `size` conventions and SHALL reflect the current page in the URL query parameters.
6. THE Dashboard SHALL validate every audit API response with its Zod schema before rendering, and IF validation fails THEN THE Dashboard SHALL render the error state and SHALL NOT render unvalidated data.
7. THE Audit_Page SHALL surface the `correlation_id` of each Audit_Event so that it is available for support and debugging.
8. THE Audit_Page SHALL request audit data only through the Server_Proxy and SHALL NOT call the Backend_API directly from the browser.

### Requirement 9: Audit Entry Deep Link and Detail Drawer

**User Story:** As a Platform_Admin_User, I want to deep-link to a single audit entry, so that I can share and open one recorded action directly.

#### Acceptance Criteria

1. WHEN a user selects an Audit_Event row on the Audit_Page, THE Dashboard SHALL open the Audit_Detail_Drawer displaying that Audit_Event and SHALL set an `entry={id}` URL query parameter.
2. WHEN the Audit_Page loads with an `entry={id}` query parameter referencing an Audit_Event the Acting_Principal may read, THE Dashboard SHALL open the Audit_Detail_Drawer for that Audit_Event.
3. IF the Audit_Page loads with an `entry={id}` query parameter for which the single-entry request returns `404`, THEN THE Dashboard SHALL render the deep-link-not-found state.
4. WHEN the user closes the Audit_Detail_Drawer, THE Dashboard SHALL remove the `entry` query parameter and SHALL return keyboard focus to the triggering row control.
5. THE Audit_Detail_Drawer SHALL display the `occurred_at`, Actor_Display, `action`, Target_Type, Target_Id, `tenant_id`, `correlation_id`, and Outcome of the Audit_Event.

### Requirement 10: Role-Gated Navigation and Read-Only UI States

**User Story:** As a Read_Only_Viewer, I want the audit navigation to appear only when my role can use it, so that I am not shown a destination I cannot access, while the backend still enforces authorization.

#### Acceptance Criteria

1. THE Dashboard SHALL render the audit navigation link only for Acting_Principals whose Composite_Roles are PLATFORM_ADMIN, SUPPORT_AGENT, or TENANT_ADMIN.
2. THE Dashboard SHALL treat the navigation gating as a convenience only and SHALL rely on the Backend_API as the authoritative enforcement point for every audit read.
3. WHEN the Audit_Page issues an audit request and the Backend_API responds with status `403`, THE Dashboard SHALL render the forbidden state distinct from the empty and error states.
4. WHILE an audit request is in flight, THE Dashboard SHALL render the loading state.
5. WHEN an audit list request succeeds with zero unfiltered results, THE Dashboard SHALL render the empty state, and WHEN a request with active filters succeeds with zero results, THE Dashboard SHALL render the filtered-empty state distinct from the empty state.
6. WHEN an audit request fails with a Problem_Response, THE Dashboard SHALL render the error state using the `ProblemDetailsCard`.
7. THE Dashboard SHALL NOT render any create, update, conflict, or success-write state on the Audit_Page, because the audit feature is read-only.

### Requirement 11: RBAC Matrix Reconciliation for Global Audit Viewing

**User Story:** As a QA_Automation_Engineer, I want the frontend capability matrix to express global audit-log viewing distinctly from payment audit, so that role-gated rendering tests assert the correct capability.

#### Acceptance Criteria

1. THE Rbac_Matrix SHALL express a capability that maps a Composite_Role's holding of an Audit_Authority (`platform:audit:read` or `tenant:audit:read`) to the ability to view the global Audit_Page.
2. THE Rbac_Matrix SHALL keep the global audit-log-viewing capability distinct from the existing `canReadAudit` capability that maps to the payment-audit authority `platform:payments:audit`.
3. THE requirements document SHALL record, in Open Questions, the decision of whether to reuse the existing `canReadAudit` capability or introduce a new `canViewAuditLog` capability, pending confirmation.
4. THE Dashboard SHALL grant the global audit-log-viewing capability to PLATFORM_ADMIN, SUPPORT_AGENT, and TENANT_ADMIN, and SHALL withhold it from MERCHANT_MANAGER and READ_ONLY_USER.
5. THE Rbac_Matrix SHALL serve frontend convenience gating only and SHALL NOT be the authoritative authorization mechanism.

### Requirement 12: Accessibility and Testability of the Audit Surface

**User Story:** As a QA_Automation_Engineer, I want the audit surface to be accessible and locator-friendly, so that future Playwright lessons use stable, accessibility-first locators.

#### Acceptance Criteria

1. THE Dashboard SHALL assign stable Test_Ids `audit-table`, `audit-date-from`, `audit-date-to`, `audit-action-filter`, `audit-entry-drawer`, and `nav-link-audit`, each resolving to exactly one element per rendered page.
2. THE Audit_Page SHALL assign a row-scoped Test_Id to each Audit_Event row so that future tests can target a specific row.
3. THE Audit_Page SHALL favour semantic locators, exposing the table with `<th scope="col">` column headers and giving every filter control a `UFormField` label, so that role- and label-based locators are preferred over Test_Ids.
4. THE Audit_Page SHALL render Outcome through visible text and not through color alone.
5. THE Audit_Page SHALL render a single semantic top-level heading and SHALL move keyboard focus to the primary heading or first interactive control on navigation to the page.
6. WHEN the Audit_Detail_Drawer opens, THE Dashboard SHALL trap keyboard focus within the drawer and SHALL restore focus to the triggering control on close.

### Requirement 13: Security and Data Confidentiality

**User Story:** As a Tenant_Admin_User, I want audit records to be tenant-isolated and free of secrets, so that audit visibility does not leak another tenant's data or any sensitive value.

#### Acceptance Criteria

1. THE Backend_API SHALL enforce audit authorization and tenant scoping on the server independent of any Dashboard navigation or capability gating.
2. WHEN a Tenant_Scoped_Principal reads audit data, THE Backend_API SHALL return only Audit_Events whose `tenant_id` matches the principal's resolved Tenant_Reference.
3. THE Backend_API SHALL exclude bearer tokens, passwords, Temporary_Passwords, PAN, CVV, and other sensitive values from every Audit_Event field and audit response.
4. THE Backend_API SHALL set the Audit_Event Actor_Display to a safe display value and SHALL NOT expose internal subject identity beyond what the existing Payment_Order_Status_History already exposes.
5. WHEN the Backend_API denies a cross-tenant single-entry read, THE Backend_API SHALL return Masked_Not_Found so that the existence of another tenant's Audit_Event is not disclosed.
6. THE Backend_API SHALL exclude the `tenant_id` and identifying values of other tenants from any error response produced by a denied cross-tenant audit request.

### Requirement 14: RBAC and Tenant-Isolation Matrix

**User Story:** As a QA_Automation_Engineer, I want an explicit matrix of audit access outcomes, so that I can write deterministic RBAC and isolation tests.

#### Acceptance Criteria

1. THE requirements document SHALL record an RBAC and tenant-isolation matrix mapping each Composite_Role and audit operation to its expected outcome.
2. THE Backend_API SHALL produce, for each Composite_Role and audit operation, exactly the outcomes recorded in the matrix.
3. THE Backend_API SHALL produce, for a Tenant_Scoped_Principal acting outside its tenant on a single-entry read, the masked `404` outcome recorded in the matrix.

#### RBAC and Tenant-Isolation Matrix

| Operation (required authority) | PLATFORM_ADMIN | SUPPORT_AGENT | TENANT_ADMIN | MERCHANT_MANAGER | READ_ONLY_USER |
|---|---|---|---|---|---|
| List audit (`platform:audit:read` / `tenant:audit:read`) | All tenants | All tenants | Own tenant only | `403` | `403` |
| Get audit entry, own/visible tenant | `200` | `200` | `200` | `403` | `403` |
| Get audit entry, other tenant | `200` (cross-tenant visible) | `200` (cross-tenant visible) | `404` Masked_Not_Found | `403` | `403` |
| Get audit entry, nonexistent id | `404` | `404` | `404` Masked_Not_Found | `403` | `403` |
| Create/update/delete audit | Not offered (no endpoint) | Not offered | Not offered | Not offered | Not offered |

Note: A request lacking any Audit_Authority is rejected with `403` before tenant scoping is evaluated. Platform-scoped principals (PLATFORM_ADMIN, SUPPORT_AGENT) read across all tenants; tenant-scoped principals (TENANT_ADMIN) are confined to their resolved Tenant_Reference, with cross-tenant single reads masked as `404`.

## Implementation Sequencing & Prerequisites

This spec is **Spec #4** of the Playwright/SDET roadmap. The roadmap order is:

1. `iam-roles-and-keycloak-login`
2. `tenant-model-and-isolation`
3. `user-management`
4. **`audit-log-dashboard` (THIS SPEC)**
5. `deterministic-seed-and-test-isolation`

### Hard prerequisites (must be implemented before this spec)

- **Spec #1 `iam-roles-and-keycloak-login`** — provides the five Composite_Roles, the Role_Converter allowlist pattern, the deterministic Test_Users, and the `tenant_id` JWT claim. This spec **extends** the realm composite roles (PLATFORM_ADMIN, SUPPORT_AGENT, TENANT_ADMIN) with the new Audit_Authorities and **extends** the Authorities_Catalog and Role_Converter allowlist. Audit_Events carry the Actor_Subject derived from the authenticated session established here.
- **Spec #2 `tenant-model-and-isolation`** — provides the persisted Tenant entity, the `tenant_type` (`PLATFORM`/`STANDARD`) classification, and the Tenant_Resolver. This spec reuses the Tenant_Resolver to classify the Acting_Principal as platform-scoped or tenant-scoped and to scope audit reads by Tenant_Reference.

### Audited event sources (must be extended as part of, or alongside, this spec)

- **Spec #3 `user-management`** — its user create, user update, and user role-assignment actions become Domain_Audit_Event sources. The iam/user-management Emitting_Module must be extended to publish those events.
- **Merchant and payment modules** — merchant create/activate/suspend and payment authorize/capture/cancel/refund become Domain_Audit_Event sources. Both modules must be extended to publish events. **Verified:** no `ApplicationEventPublisher` usage and no `@ApplicationModuleListener` exist in the backend today, so these events do not exist and must be introduced. The existing Payment_Order_Status_History is payment-domain-specific and is not the global audit log.

### Cross-spec / cross-module touch points introduced by this spec

- Realm composite-role extension (in the `iam-roles-and-keycloak-login` realm import) to grant `platform:audit:read` and `tenant:audit:read`.
- Authorities_Catalog extension and Role_Converter allowlist extension for the two new authorities.
- Emitting_Module extensions (merchant, payment, iam/user-management) to publish Domain_Audit_Events.
- Frontend Rbac_Matrix reconciliation (global audit-log capability vs the existing payment-audit `canReadAudit`).

### Downstream dependent

- **Spec #5 `deterministic-seed-and-test-isolation`** — will need deterministic, reset-able audit data and predictable Audit_Events so future audit-trail verification tests are repeatable. This spec should keep Audit_Events deterministic given a deterministic sequence of actions.

## Future Playwright Scenarios (Conceptual — No Test Files)

These scenarios are recorded for future learning lessons. **No test files are created by this spec.**

- **Audit-trail verification:** perform an action (for example suspend a merchant or assign a user role) through the UI or API, then assert that a matching Audit_Event appears on the Audit_Page with the expected `action`, Target_Type, Target_Id, Actor_Display, and Outcome.
- **Date filtering:** set the native from and to date inputs and assert that only Audit_Events within the range are listed, including the boundary dates.
- **Deep link:** navigate directly to `/admin/audit?entry={id}` and assert the Audit_Detail_Drawer opens for the referenced Audit_Event; navigate to a nonexistent id and assert the deep-link-not-found state.
- **Tenant-scoped audit:** as a TENANT_ADMIN, assert only own-tenant Audit_Events are visible and a cross-tenant deep link yields the not-found state; as a PLATFORM_ADMIN or SUPPORT_AGENT, assert cross-tenant visibility.
- **`page.clock` time mocking:** mock the browser clock to a fixed instant, exercise relative date filtering and `occurred_at` rendering, and assert deterministic results independent of the real wall clock.
- **RBAC gating:** assert the `nav-link-audit` link is visible for PLATFORM_ADMIN, SUPPORT_AGENT, and TENANT_ADMIN and hidden for MERCHANT_MANAGER and READ_ONLY_USER; assert a direct route/API access by an unauthorized role yields the forbidden (403) surface.

## Open Questions

1. **Audit module name.** This spec recommends naming the new Spring Modulith module `audit` (package `lab.paymentquality.audit`). Confirm `audit` versus an alternative such as `auditlog` or `observability`.
2. **RBAC capability reconciliation.** Reuse the existing `canReadAudit` capability (currently mapped to the payment-audit authority `platform:payments:audit`) for the global Audit_Page, or introduce a new `canViewAuditLog` capability distinct from it? This spec leans toward a new `canViewAuditLog` to avoid conflating payment audit with the global audit log.
3. **Event-publishing touch points in emitter modules.** Which exact extension points in the merchant, payment, and iam/user-management modules publish Domain_Audit_Events, and whether publication occurs within or after the operation's transaction? (No events exist today.)
4. **Retention and volume.** Is there an audit retention, archival, or purge policy, and what volume/throughput is expected? Retention and volume management are out of scope for this spec and recorded here for a future decision.
5. **Auditing access denials.** Should `DENIED` access attempts (for example cross-tenant or unauthorized reads) be published as Domain_Audit_Events and recorded as Audit_Events, or only successful and failed mutating actions? Recording denials is currently optional (Requirement 4, criterion 6).
