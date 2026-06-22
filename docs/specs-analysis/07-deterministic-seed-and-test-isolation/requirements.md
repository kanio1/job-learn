# Requirements Document

## Introduction

This feature adds a **deterministic seed dataset** and a **feature-flagged, test-only reset/seed REST affordance** to the Payment Quality Engineering Lab. It is **Spec #5** of the Playwright/SDET learning roadmap and the **capstone enabler** of the first five specs: it provides predictable, reset-able data across all prior specs' entities (tenants, merchants, payment orders, and — by alignment, not duplication — the Keycloak realm test users) so that future REST Assured and Playwright lessons can run against a known, stable, repeatable state.

This is a **brownfield enhancement** that extends — never rewrites — the existing backend (`apps/backend`), and it makes **no production behavior change**. It introduces three new application capabilities that do not exist today: (1) a new `seed` Spring profile that loads a deterministic dataset on startup, (2) a fixed-identity fixtures catalog of documented natural keys and UUIDs, and (3) two test-only endpoints, `POST /api/test/reset` and `POST /api/test/seed`, guarded by a feature flag.

Verified current-state facts that this spec MUST NOT contradict:

- **No `seed` profile exists.** The only profile mechanism in use today is `@Profile({"dev","test"})` (currently used for CORS); profiles are available and the pattern is established.
- **No `data.sql`, no Flyway repeatable (`R__`) migration, and no test-reset endpoint exist today.**
- Tests isolate state via **per-class Testcontainers** plus manual `deleteAll()` in `@BeforeEach`. `PostgresContainerSupport` provides `newPostgresContainer` and `registerPostgresProperties`.
- JPA runs with `ddl-auto: validate`; **Flyway owns the schema**. A Flyway **repeatable** migration in the default location would run on **every** startup, **including production** — therefore seeding MUST NOT be implemented as a plain Flyway repeatable migration in the default migration location.

The seeding mechanism is therefore **profile-gated application code** (an `ApplicationRunner`/`CommandLineRunner` or `@Profile`-gated component) that runs **only** when the `seed` profile is active, never under the `prod` profile, and never interferes with `ddl-auto: validate`. The reset and seed endpoints are an explicit, **deliberate, isolated test affordance**: they are disabled by default via the feature flag `app.testing.enabled` (default `false`), are forced off under the `prod` profile regardless of external configuration, and return `404 Not Found` (not `403`) when disabled so their existence is never advertised.

Identity scope is deliberately split. The **DB seed** owned by this spec covers tenants, merchants, and payment orders (and, as an Open Question, optionally audit events). **Keycloak realm users and roles are seeded by `iam-roles-and-keycloak-login` (Spec #1)** and are NOT duplicated here. This spec instead **aligns** the seeded DB identities (the `merchant_id` and `tenant_id`/`tenant_reference` values of seeded merchants and tenants) with the attributes already carried by the realm test users, so a Playwright login as `merchant.manager` lands on data that actually exists and is owned by the right tenant and merchant.

Because this is a QA/SDET learning application, the feature is designed to create realistic future test situations: deterministic fixtures targeted by stable identifiers, API-driven setup and cleanup, and reset-between-runs isolation. **No Playwright or other automated frontend test files are created by this spec**; a conceptual Future Playwright / SDET Usage section records the lessons to be written later. This document is a specification only and contains **no implementation**.

The existing security posture is preserved exactly. The reset and seed endpoints never log or expose bearer tokens, passwords, PAN, CVV, or personal data, and they are designed to be **impossible to reach in production**.

## Glossary

- **Backend_API**: The Payment Quality Lab Spring Boot REST API exposed under `/api/*`, including its Spring Security authorization rules and service layer.
- **Seed_Profile**: A new Spring profile named `seed` introduced by this spec. While active, the Backend_API loads the Deterministic_Dataset on startup.
- **Seed_Runner**: The profile-gated application component (an `ApplicationRunner`, `CommandLineRunner`, or equivalent `@Profile`-gated bean) that loads the Deterministic_Dataset on startup. It is active only while the Seed_Profile is active and is never active under the Prod_Profile.
- **Deterministic_Dataset**: The fixed, documented set of seed records (tenants, merchants, payment orders, and optionally audit events) defined by the Fixtures_Catalog, identical across every run.
- **Fixtures_Catalog**: The single documented table of fixed identities (UUID primary keys and natural-key references) for every seeded entity, used by test authors to target records deterministically.
- **Seed_Operation**: The act of loading the Deterministic_Dataset into the database, whether performed by the Seed_Runner on startup or by the Seed_Endpoint at runtime. A Seed_Operation is Idempotent.
- **Reset_Operation**: The act of clearing mutable seeded and test-created data back to a known Baseline_State, performed by the Reset_Endpoint.
- **Baseline_State**: The known database state produced by a Reset_Operation: all mutable test data removed in foreign-key-safe order, leaving the schema intact and ready for a subsequent Seed_Operation.
- **Idempotent**: A property of a Seed_Operation whereby running it one or more additional times produces the same final database state as running it once (achieved by upsert on fixed keys or by clear-then-insert).
- **Testing_Flag**: The boolean feature flag `app.testing.enabled`, default `false`, that gates the Test_Endpoints. When `false`, the Test_Endpoints are not reachable.
- **Test_Endpoints**: The two test-only endpoints introduced by this spec: the Reset_Endpoint `POST /api/test/reset` and the Seed_Endpoint `POST /api/test/seed`.
- **Reset_Endpoint**: The endpoint `POST /api/test/reset` that performs a Reset_Operation.
- **Seed_Endpoint**: The endpoint `POST /api/test/seed` that performs a Seed_Operation.
- **Prod_Profile**: The Spring profile named `prod` that designates a production runtime. While active, the Testing_Flag is forced off and the Test_Endpoints are unreachable.
- **Testing_Module**: The Spring Modulith module (or package) that owns the Seed_Runner, the Test_Endpoints, and the seeding/reset orchestration. Its exact placement is recorded as an Open Question.
- **Module_Seed_Capability**: A capability that a domain module (tenant, merchant, payment, audit) exposes through its PUBLIC module API to seed or clear its own data, so that the Testing_Module orchestrates seeding without importing any module's `internal` packages.
- **Tenant_Reference**: The stable, human-readable natural-key string that identifies a tenant (for example `TENANT_ALPHA`), as defined by `tenant-model-and-isolation`. It is the value carried by the `tenant_id` JWT claim.
- **Merchant_Reference**: The stable, human-readable natural-key string that identifies a merchant (for example `MERCHANT_ALPHA_001`), as carried by the realm `merchant_id` user attribute.
- **Realm_Test_User**: One of the deterministic per-role Keycloak users defined by `iam-roles-and-keycloak-login` (for example `platform.admin`, `tenant.admin`, `merchant.manager`, `support.agent`, `readonly.user`).
- **Payment_Order_Status**: One of the payment order lifecycle status values defined by the payment module: `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, `REFUNDED`.
- **Problem_Response**: A `4xx` response with `Content-Type: application/problem+json` carrying the members `type`, `title`, `status`, `detail`, and `instance`.
- **Prior_Specs**: The four preceding roadmap specs whose entities this feature seeds: `iam-roles-and-keycloak-login` (#1), `tenant-model-and-isolation` (#2), `user-management` (#3), and `audit-log-dashboard` (#4).
- **Keycloak_Realm**: The `payment-quality` Keycloak realm defined by `infra/keycloak/realms/payment-quality-realm.json`, owned by `iam-roles-and-keycloak-login`.

## Personas

- **QA_Automation_Engineer**: Writes future REST Assured and Playwright tests; needs a deterministic, documented dataset targeted by stable identifiers, API-driven setup and cleanup, and reset-between-runs isolation.
- **Backend_Developer**: Runs the Backend_API locally under the Seed_Profile to obtain realistic data for manual exploration and frontend development without hand-crafting records.
- **Platform_Operator**: Operates the production deployment and requires absolute assurance that the Test_Endpoints and the Seed_Runner can never run or be reached in production.
- **Future_Playwright_Author**: Will later build the Playwright framework that logs in per role using storage state and relies on the Deterministic_Dataset existing and being reset between runs.

## In Scope

- A new Seed_Profile (`seed`) that, while active, loads the Deterministic_Dataset on startup via the Seed_Runner, with a Seed_Operation that is Idempotent and never interferes with `ddl-auto: validate`.
- A profile-gated Seed_Runner implemented as application code (not a default-location Flyway repeatable migration), active only under the Seed_Profile and never under the Prod_Profile.
- A Fixtures_Catalog: one documented table of fixed UUID primary keys and natural-key references for every seeded tenant, merchant, and payment order, aligned with the `tenant_id` and `merchant_id` attributes of the existing Realm_Test_Users.
- A Deterministic_Dataset comprising at least two tenants, several merchants across those tenants, and payment orders spanning the statuses `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, and `REFUNDED`, sufficient to exercise lists, filters, lifecycle, and audit.
- The Testing_Flag `app.testing.enabled` (default `false`) gating the Test_Endpoints.
- A Reset_Endpoint `POST /api/test/reset` that clears mutable data to the Baseline_State in foreign-key-safe order, and a Seed_Endpoint `POST /api/test/seed` that performs an Idempotent Seed_Operation, both reachable only when the Testing_Flag is enabled.
- Defense-in-depth profile/flag interaction: the Prod_Profile forces the Testing_Flag off regardless of external configuration, and the Test_Endpoints return `404` when disabled.
- A documented test-isolation guidance distinguishing the new API-driven seed/reset approach (for E2E, manual exploration, and future Playwright) from the existing per-class Testcontainers + `deleteAll()` unit pattern, which is retained unchanged.
- Preservation of Spring Modulith boundaries: the Testing_Module orchestrates seeding/reset through each domain module's PUBLIC API (Module_Seed_Capability) and never imports another module's `internal` packages.
- A conceptual Future Playwright / SDET Usage section (no test files).

## Out of Scope (Non-Goals)

- Seeding or duplicating Keycloak realm users, roles, or attributes; realm identity is owned by `iam-roles-and-keycloak-login` (#1). This spec only aligns DB identities with the realm test users' attributes.
- Any production behavior change, any change to `ddl-auto: validate`, and any default-location Flyway repeatable migration used for seeding.
- Any change to the existing REST contracts, headers, status codes, or security authorities of the merchant, payment, tenant, user, or audit surfaces.
- A user-facing UI for seeding or reset; the affordance is API plus profile only (no frontend page, no Playwright files).
- Replacing the existing per-class Testcontainers + `deleteAll()` unit-test isolation pattern; it is retained.
- Tenant CRUD, user CRUD, audit write endpoints, or login flows (owned by their respective specs).
- PSP integration, Kafka, webhooks, settlement, or any item in the project-wide active non-goals.

## Resolved Decisions

Each decision records the original question for traceability, followed by the resolution and rationale. The acceptance criteria and sequencing in this document are consistent with these decisions.

### Decision A: Seeding mechanism — profile-gated seeder plus feature-flagged reset endpoint

**Original question:** How should deterministic data be loaded and reset given that JPA runs `ddl-auto: validate`, Flyway owns the schema, and a default-location Flyway repeatable migration would run on every startup including production?

**Resolved:** Use a **profile-gated Seed_Runner** plus **feature-flagged Test_Endpoints**. Seeding is performed by application code (an `ApplicationRunner`/`CommandLineRunner` or `@Profile`-gated component) that is active **only** under the Seed_Profile (and optionally `dev`), **never** under the Prod_Profile, and that never alters the schema or interferes with `ddl-auto: validate`. Runtime reset and re-seed are performed by `POST /api/test/reset` and `POST /api/test/seed`, guarded by the Testing_Flag `app.testing.enabled` (default `false`), explicitly disabled under the Prod_Profile, and isolated behind a dedicated filter/profile guard. **Rationale:** a plain Flyway repeatable in the default location would execute in production and conflate seed data with schema ownership; profile-gated code keeps seeding strictly out of production while teaching a realistic SDET data-lifecycle pattern (deterministic startup seed plus API-driven reset).

### Decision B: Predictable identities — fixed UUIDs and natural keys in one catalog

**Original question:** How should seeded records be identified so future tests can target them deterministically?

**Resolved:** Every seeded tenant, merchant, and payment order is assigned a **fixed, documented UUID primary key** and, where applicable, a **fixed natural-key reference** (Tenant_Reference, Merchant_Reference). All fixed identities are collected in a single **Fixtures_Catalog** so REST Assured and Playwright authors target them deterministically. **Rationale:** stable, centrally documented identifiers eliminate per-run discovery, make assertions deterministic, and teach the natural-key versus surrogate-key distinction already established by `tenant-model-and-isolation`.

### Decision C: Seed scope — DB entities here, realm identities aligned not duplicated

**Original question:** Should this spec seed Keycloak realm users/roles in addition to database entities?

**Resolved:** This spec seeds **database entities only** — tenants, merchants, payment orders in varied statuses, and optionally audit events (Open Question). **Keycloak realm users and roles remain owned by `iam-roles-and-keycloak-login` (#1)** and are not duplicated here. The Deterministic_Dataset instead **aligns** its tenant and merchant identities (`tenant_reference` and `merchant_id`) with the `tenant_id` and `merchant_id` attributes already carried by the Realm_Test_Users, so that a login as a given Realm_Test_User lands on data owned by the matching tenant and merchant. **Rationale:** Keycloak is the single source of truth for identity (per `user-management`); duplicating realm seed here would create dual-write drift, whereas alignment keeps one source of truth while guaranteeing the DB has matching records.

### Decision D: Reset/seed endpoint security — disabled-by-default, prod-forced-off, 404 when disabled

**Original question:** How should the test-only endpoints be secured so they cannot run in production or be discovered?

**Resolved:** The Testing_Flag `app.testing.enabled` defaults to `false`; the Prod_Profile **forces it off regardless of external configuration**; when disabled the Test_Endpoints return **`404 Not Found`** (not `403`, so their existence is not advertised); they never log or expose secrets; and they are protected so they cannot be enabled accidentally in production. They are documented as a deliberate, isolated test affordance. **Rationale:** returning `404` rather than `403` follows the platform's masked-not-found non-disclosure pattern and prevents probing; forcing the flag off under the Prod_Profile is defense-in-depth that does not rely solely on configuration discipline.

## Requirements

### Requirement 1: Seed Profile and Startup Seeding

**User Story:** As a Backend_Developer, I want a dedicated `seed` profile that loads a deterministic dataset on startup, so that I can run the platform locally against realistic, predictable data without crafting records by hand.

#### Acceptance Criteria

1. THE Backend_API SHALL define a Spring profile named `seed` that, when active, activates the Seed_Runner.
2. WHEN the Backend_API starts with the Seed_Profile active, THE Seed_Runner SHALL load the Deterministic_Dataset into the database.
3. WHILE the Seed_Profile is not active, THE Backend_API SHALL NOT load the Deterministic_Dataset on startup.
4. THE Seed_Runner SHALL be implemented as profile-gated application code and SHALL NOT be implemented as a Flyway repeatable migration placed in the default migration location.
5. WHEN the Seed_Runner loads the Deterministic_Dataset, THE Seed_Runner SHALL complete without altering the database schema and without causing `ddl-auto: validate` startup validation to fail.
6. WHEN the Backend_API starts with the Seed_Profile active against a database whose schema has been migrated by Flyway, THE Backend_API SHALL complete startup, Flyway migration, and JPA schema validation without error.

### Requirement 2: Idempotent Seed Operation

**User Story:** As a QA_Automation_Engineer, I want seeding to be safe to run repeatedly, so that re-running setup never corrupts or duplicates the deterministic dataset.

#### Acceptance Criteria

1. WHEN a Seed_Operation runs against an empty database, THE Backend_API SHALL produce the complete Deterministic_Dataset.
2. WHEN a Seed_Operation runs against a database that already contains the Deterministic_Dataset, THE Backend_API SHALL produce a final database state identical to the state after a single Seed_Operation.
3. THE Backend_API SHALL achieve Seed_Operation idempotency by upserting on the fixed identities of the Fixtures_Catalog or by clearing and re-inserting the seeded data.
4. WHEN a Seed_Operation completes, THE Backend_API SHALL assign each seeded record the exact fixed identity recorded in the Fixtures_Catalog.
5. IF a Seed_Operation cannot complete because a required schema object is absent, THEN THE Backend_API SHALL fail the Seed_Operation without leaving a partially seeded database state.

### Requirement 3: Deterministic Identity Fixtures Catalog

**User Story:** As a QA_Automation_Engineer, I want one documented catalog of fixed identifiers, so that I can target seeded records deterministically from REST Assured and Playwright tests.

#### Acceptance Criteria

1. THE requirements document SHALL record a Fixtures_Catalog listing the fixed UUID primary key and the natural-key reference of every seeded tenant, merchant, and payment order.
2. THE Backend_API SHALL seed each tenant, merchant, and payment order with the exact fixed identity recorded in the Fixtures_Catalog on every Seed_Operation.
3. THE Fixtures_Catalog SHALL include at least the Tenant_References `TENANT_ALPHA`, `PLATFORM_TENANT`, and `PLACEHOLDER_TENANT_ID`, consistent with the `tenant_id` attributes carried by the Realm_Test_Users.
4. THE Fixtures_Catalog SHALL include at least the Merchant_Reference `MERCHANT_ALPHA_001`, consistent with the `merchant_id` attribute carried by the `merchant.manager` Realm_Test_User.
5. THE Backend_API SHALL keep the fixed identities of the Fixtures_Catalog stable across every run so that no identity changes between Seed_Operations.

### Requirement 4: Alignment with Keycloak Realm Test Users

**User Story:** As a Future_Playwright_Author, I want seeded data owned by the same tenants and merchants that the realm test users carry, so that logging in as a given role lands on data that actually exists and is correctly scoped.

#### Acceptance Criteria

1. THE Backend_API SHALL seed a tenant whose `tenant_reference` equals each distinct `tenant_id` attribute value carried by the Realm_Test_Users (`TENANT_ALPHA`, `PLATFORM_TENANT`, and `PLACEHOLDER_TENANT_ID`).
2. THE Backend_API SHALL seed a merchant whose Merchant_Reference equals the `merchant_id` attribute carried by the `merchant.manager` Realm_Test_User (`MERCHANT_ALPHA_001`) and SHALL assign that merchant to the tenant whose `tenant_reference` equals that user's `tenant_id` attribute (`TENANT_ALPHA`).
3. THE Backend_API SHALL assign every seeded merchant to a seeded tenant present in the Fixtures_Catalog.
4. THE Backend_API SHALL NOT create, modify, or delete any Keycloak realm user, role, or attribute as part of any Seed_Operation or Reset_Operation.
5. WHERE the realm test-user attributes define a tenant or merchant identity, THE Deterministic_Dataset SHALL provide a matching database record so that no Realm_Test_User resolves to a missing tenant or merchant.

### Requirement 5: Deterministic Dataset Content

**User Story:** As a QA_Automation_Engineer, I want a dataset that spans multiple tenants, merchants, and payment-order statuses, so that I can exercise lists, filters, lifecycle, and audit deterministically.

#### Acceptance Criteria

1. THE Deterministic_Dataset SHALL contain at least two tenants recorded in the Fixtures_Catalog.
2. THE Deterministic_Dataset SHALL contain multiple merchants distributed across the seeded tenants.
3. THE Deterministic_Dataset SHALL contain payment orders whose statuses include `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, and `REFUNDED`.
4. THE Deterministic_Dataset SHALL contain enough merchants and payment orders to exercise list pagination, status filtering, and summary aggregation on the existing endpoints.
5. THE Backend_API SHALL produce byte-stable seeded business field values for each fixed identity across every Seed_Operation, so that assertions on seeded records are repeatable.
6. WHERE a seeded payment order carries a status reached only through lifecycle transitions, THE Backend_API SHALL seed that payment order directly in the target status without requiring a client to drive the lifecycle.

### Requirement 6: Testing Feature Flag

**User Story:** As a Platform_Operator, I want the test-only endpoints disabled by default, so that they are never exposed unless explicitly and safely enabled in a non-production environment.

#### Acceptance Criteria

1. THE Backend_API SHALL define a boolean Testing_Flag named `app.testing.enabled` whose default value is `false`.
2. WHILE the Testing_Flag is `false`, THE Backend_API SHALL NOT expose the Test_Endpoints as reachable routes.
3. WHILE the Testing_Flag is `true`, THE Backend_API SHALL expose the Reset_Endpoint and the Seed_Endpoint.
4. THE Backend_API SHALL determine the reachability of the Test_Endpoints solely from the Testing_Flag and the active profile, and SHALL NOT couple their reachability to any business authority.

### Requirement 7: Reset Endpoint

**User Story:** As a QA_Automation_Engineer, I want a reset endpoint that returns the database to a known baseline, so that each test run starts from a clean, predictable state.

#### Acceptance Criteria

1. WHILE the Testing_Flag is `true`, WHEN a client sends `POST /api/test/reset`, THE Backend_API SHALL clear all mutable seeded and test-created tenant, merchant, and payment-order data to the Baseline_State.
2. WHEN performing a Reset_Operation, THE Backend_API SHALL delete data in an order that respects foreign-key constraints so that the Reset_Operation completes without constraint violations.
3. WHEN a Reset_Operation completes, THE Backend_API SHALL leave the database schema intact and ready for a subsequent Seed_Operation.
4. WHEN a Reset_Operation completes successfully, THE Backend_API SHALL return a success status without exposing any bearer token, password, or other sensitive value in the response body.
5. IF the Testing_Flag is `false` WHEN a client sends `POST /api/test/reset`, THEN THE Backend_API SHALL return status `404`.
6. THE Backend_API SHALL include an `X-Correlation-ID` response header on every Reset_Endpoint response.

### Requirement 8: Seed Endpoint

**User Story:** As a QA_Automation_Engineer, I want a seed endpoint that reloads the deterministic dataset at runtime, so that I can restore known data between test runs without restarting the application.

#### Acceptance Criteria

1. WHILE the Testing_Flag is `true`, WHEN a client sends `POST /api/test/seed`, THE Backend_API SHALL perform an Idempotent Seed_Operation that loads the Deterministic_Dataset.
2. WHEN the Seed_Endpoint performs a Seed_Operation, THE Backend_API SHALL assign each seeded record the exact fixed identity recorded in the Fixtures_Catalog.
3. WHEN a client sends `POST /api/test/seed` two or more times in succession, THE Backend_API SHALL produce the same final database state as a single Seed_Operation.
4. WHEN a Seed_Operation completes successfully, THE Backend_API SHALL return a success status without exposing any bearer token, password, or other sensitive value in the response body.
5. IF the Testing_Flag is `false` WHEN a client sends `POST /api/test/seed`, THEN THE Backend_API SHALL return status `404`.
6. THE Backend_API SHALL include an `X-Correlation-ID` response header on every Seed_Endpoint response.

### Requirement 9: Production Safety and Defense-in-Depth

**User Story:** As a Platform_Operator, I want the seeder and test endpoints to be impossible to run or reach in production, so that production data can never be reset, reseeded, or probed.

#### Acceptance Criteria

1. WHILE the Prod_Profile is active, THE Backend_API SHALL force the effective value of the Testing_Flag to `false` regardless of any externally supplied configuration value.
2. WHILE the Prod_Profile is active, THE Backend_API SHALL NOT activate the Seed_Runner and SHALL NOT load the Deterministic_Dataset on startup.
3. WHILE the Prod_Profile is active, THE Backend_API SHALL return status `404` for `POST /api/test/reset` and `POST /api/test/seed`.
4. THE Backend_API SHALL NOT log bearer tokens, passwords, PAN, CVV, or personal data in any log statement produced by the Seed_Runner, the Reset_Endpoint, or the Seed_Endpoint.
5. THE Backend_API SHALL isolate the Test_Endpoints behind a dedicated guard (a filter or profile/flag condition) so that they cannot be enabled by an unrelated configuration change.

### Requirement 10: Module Boundaries for Seeding and Reset

**User Story:** As a Backend_Developer, I want seeding and reset to respect Spring Modulith boundaries, so that the test affordance does not break module encapsulation verified by the architecture tests.

#### Acceptance Criteria

1. THE Testing_Module SHALL orchestrate Seed_Operations and Reset_Operations through each domain module's PUBLIC API and SHALL NOT import any other module's `internal` packages.
2. WHERE a domain module owns seedable or clearable data, THE domain module SHALL expose a Module_Seed_Capability through its PUBLIC module API for use by the Testing_Module.
3. THE Backend_API SHALL preserve the existing Spring Modulith module boundaries so that the existing `ModulithArchitectureTest`, `MerchantModuleTest`, and `PaymentModuleTest` continue to pass.
4. THE Testing_Module SHALL be excluded from the production runtime by being gated on the Seed_Profile and the Testing_Flag, so that its components are not instantiated under the Prod_Profile.

### Requirement 11: Test Isolation Guidance and Coexistence

**User Story:** As a QA_Automation_Engineer, I want documented guidance on when to use API-driven seed/reset versus the existing per-class isolation pattern, so that unit tests and E2E tests each use the right isolation strategy.

#### Acceptance Criteria

1. THE requirements document SHALL record guidance that E2E, manual, and future Playwright flows use the Seed_Endpoint and Reset_Endpoint for API-driven setup and cleanup.
2. THE requirements document SHALL record guidance that existing per-class Testcontainers plus `@BeforeEach` `deleteAll()` unit and integration tests are retained unchanged and do not depend on the Test_Endpoints.
3. THE Backend_API SHALL NOT alter the existing `PostgresContainerSupport` per-class container and manual `deleteAll()` isolation behavior used by the current backend tests.
4. THE requirements document SHALL record that the Test_Endpoints are intended for E2E and manual use and are not a replacement for the narrowest-layer unit isolation pattern.

### Requirement 12: Fixtures Catalog Record

**User Story:** As a QA_Automation_Engineer, I want the concrete fixed identities written down in this spec, so that I can reference predictable tenants, merchants, and payment orders before any code exists.

#### Acceptance Criteria

1. THE requirements document SHALL record a Fixtures_Catalog table mapping each seeded entity to its fixed UUID primary key, its natural-key reference, its owning tenant, and (for payment orders) its seeded status.
2. THE Fixtures_Catalog SHALL mark the UUID values as illustrative placeholders to be finalized in design, while the natural-key references (`TENANT_ALPHA`, `PLATFORM_TENANT`, `PLACEHOLDER_TENANT_ID`, `MERCHANT_ALPHA_001`) SHALL be treated as fixed and authoritative.
3. THE recorded Fixtures_Catalog SHALL be internally consistent with the alignment, content, and identity requirements of this document.

#### Fixtures Catalog

UUID primary keys below are **illustrative placeholders** to be finalized in design; the natural-key references are **fixed and authoritative** because they must match the existing realm test-user attributes.

Tenants:

| Tenant_Reference | Fixed UUID (illustrative) | Tenant_Type | Purpose |
|---|---|---|---|
| `PLATFORM_TENANT` | `00000000-0000-0000-0000-0000000000a1` | `PLATFORM` | Platform-scoped tenant for `platform.admin` / `support.agent` |
| `TENANT_ALPHA` | `00000000-0000-0000-0000-0000000000a2` | `STANDARD` | Tenant for `tenant.admin`, `merchant.manager`, `readonly.user` |
| `PLACEHOLDER_TENANT_ID` | `00000000-0000-0000-0000-0000000000a3` | `STANDARD` | Default tenant for legacy/placeholder users |

Merchants (each owned by a seeded tenant):

| Merchant_Reference | Fixed UUID (illustrative) | Owning Tenant_Reference | Purpose |
|---|---|---|---|
| `MERCHANT_ALPHA_001` | `00000000-0000-0000-0000-0000000000b1` | `TENANT_ALPHA` | Primary merchant matching `merchant.manager` `merchant_id` |
| `MERCHANT_ALPHA_002` | `00000000-0000-0000-0000-0000000000b2` | `TENANT_ALPHA` | Second merchant in the same tenant for list/filter coverage |
| `MERCHANT_BETA_001` | `00000000-0000-0000-0000-0000000000b3` | `PLATFORM_TENANT` | Cross-tenant merchant for isolation/visibility coverage |

Payment orders (each owned by a seeded merchant, spanning statuses):

| Fixed UUID (illustrative) | Owning Merchant_Reference | Seeded Payment_Order_Status |
|---|---|---|
| `00000000-0000-0000-0000-0000000000c1` | `MERCHANT_ALPHA_001` | `CREATED` |
| `00000000-0000-0000-0000-0000000000c2` | `MERCHANT_ALPHA_001` | `AUTHORIZED` |
| `00000000-0000-0000-0000-0000000000c3` | `MERCHANT_ALPHA_001` | `CAPTURED` |
| `00000000-0000-0000-0000-0000000000c4` | `MERCHANT_ALPHA_002` | `CANCELLED` |
| `00000000-0000-0000-0000-0000000000c5` | `MERCHANT_ALPHA_002` | `REFUNDED` |
| `00000000-0000-0000-0000-0000000000c6` | `MERCHANT_BETA_001` | `CREATED` |

Note: the seeded set MUST be expanded in design with enough rows per merchant to exercise pagination (the existing list endpoint caps page size at 100) and summary aggregation; the table above records the minimum status coverage required by Requirement 5.

## Implementation Sequencing & Prerequisites

This feature is **Spec #5**, the final spec of the first-five roadmap, and the **capstone enabler** for future Playwright lessons. The roadmap order is:

1. `iam-roles-and-keycloak-login` — named composite roles, per-role test users, `tenant_id`/`merchant_id` claims.
2. `tenant-model-and-isolation` — `tenants` table, `merchants.tenant_id` association, tenant resolution and isolation.
3. `user-management` — Keycloak Admin API façade for user CRUD and role assignment.
4. `audit-log-dashboard` — `audit_event` table and event-driven audit capture.
5. **`deterministic-seed-and-test-isolation` (this spec).**

**This spec is implemented LAST**, after #1–#4, because the Deterministic_Dataset seeds **their** entities and aligns with **their** identities. It cannot seed a `tenants` table that `tenant-model-and-isolation` has not yet created, nor align with realm users that `iam-roles-and-keycloak-login` has not yet defined.

**Dependencies as data sources:**

- From #1 (`iam-roles-and-keycloak-login`): the Realm_Test_Users and their fixed `tenant_id` (`PLATFORM_TENANT`, `TENANT_ALPHA`, `PLACEHOLDER_TENANT_ID`) and `merchant_id` (`MERCHANT_ALPHA_001`) attributes, which the Fixtures_Catalog must match. This spec does not create realm identities.
- From #2 (`tenant-model-and-isolation`): the `tenants` table, the `tenant_reference` natural key, the `tenant_type` classification, and the `merchants.tenant_id` foreign key, which the seeded tenants and merchants populate.
- From #3 (`user-management`): the assignable role model and the Keycloak-as-source-of-truth identity decision, which this spec respects by not introducing a local user table.
- From #4 (`audit-log-dashboard`): the `audit_event` table and the event-driven capture model, relevant only if the Open Question to seed audit events is resolved as "seed directly."

**Incremental implementation is supported and recommended.** This spec can land in stages as each prior spec is implemented:

- **First, seed merchants and payment orders**, because those entities exist in the codebase today and need no prerequisite spec. The initial Seed_Profile, Seed_Runner, Reset_Endpoint, Seed_Endpoint, Testing_Flag, and production-safety guards can all be built and tested against merchants and payment orders alone.
- **Then add tenants** once `tenant-model-and-isolation` (#2) lands, extending the Deterministic_Dataset to assign each seeded merchant to a seeded tenant.
- **Then align user identities** once `iam-roles-and-keycloak-login` (#1) and `user-management` (#3) land, confirming the seeded tenant/merchant references match the realm test-user attributes.
- **Finally add audit events** once `audit-log-dashboard` (#4) lands, if the Open Question resolves toward seeding audit rows directly rather than letting them arise from seeded actions.

Each increment preserves the production-safety guarantees of Requirement 9 and the module-boundary guarantees of Requirement 10.

## Future Playwright / SDET Usage (Conceptual)

This section is **conceptual only**; no Playwright or other test files are created by this spec. It records how the Deterministic_Dataset and the Test_Endpoints will be used by future lessons.

- **Storage-state per role.** Future Playwright setup projects will log in once per Realm_Test_User (`platform.admin`, `tenant.admin`, `merchant.manager`, `support.agent`, `readonly.user`) and save a storage state per role. Because the Deterministic_Dataset aligns tenant and merchant identities with each user's attributes, a role's session will land on data owned by the correct tenant and merchant.
- **API-driven setup and cleanup.** Before a suite or spec, a fixture will call `POST /api/test/reset` followed by `POST /api/test/seed` (through the Nuxt proxy or directly against the backend in a non-production environment with the Testing_Flag enabled) to guarantee a known starting state, instead of crafting data through the UI.
- **Deterministic assertions.** Tests will assert against the fixed identities of the Fixtures_Catalog (for example, the merchant `MERCHANT_ALPHA_001` and its seeded payment orders in `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, and `REFUNDED`), making list, filter, summary, and detail assertions repeatable.
- **Reset between runs.** Suites will reset to the Baseline_State between runs to prevent cross-test contamination, complementing (not replacing) the existing per-class Testcontainers isolation used by backend unit and integration tests.
- **Worker-aware isolation (advisory).** When future Playwright runs parallelize, lessons should consider per-worker data ownership or serialized reset windows so that a global reset in one worker does not disturb another; this spec provides the reset/seed primitives but does not prescribe the parallel-execution policy.

## Open Questions

1. **Module placement of the seeder and test-reset orchestration.** Should the Seed_Runner and Test_Endpoints live in a new dedicated `testing` (or `devtools`) Spring Modulith module, or in a shared location? Because seeding and reset must touch repositories across the tenant, merchant, payment, and audit modules, the chosen approach must respect Modulith boundaries — for example, each module exposes a Module_Seed_Capability through its PUBLIC API, or the Testing_Module uses only published module APIs. The exact module name and packaging are deferred to design.
2. **Whether to seed audit events directly or let them arise from seeded actions.** The Deterministic_Dataset could insert `audit_event` rows directly for predictable audit-trail assertions, or it could leave the audit log empty and let audit rows arise only from actions performed during a test. This depends on `audit-log-dashboard` (#4) and is deferred.
3. **Reset granularity: full versus tenant-scoped.** Should the Reset_Endpoint always clear all mutable data, or should it optionally accept a tenant scope so that a reset clears only one tenant's data (useful for parallel, tenant-partitioned test runs)? The default is a full reset; a tenant-scoped variant is deferred.
4. **Whether seeding runs automatically under the `test` profile or only under `seed`.** Should the Seed_Runner also activate under the existing `test` profile (so all Spring-context tests start pre-seeded), or strictly only under the `seed` profile (keeping the current `deleteAll()` unit pattern unchanged)? Activating under `test` risks conflicting with the existing per-class `deleteAll()` isolation, so the default is `seed`-only; this is deferred to design.
