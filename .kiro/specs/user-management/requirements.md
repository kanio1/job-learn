# Requirements Document

## Introduction

This feature adds **user management** to the Payment Quality Engineering Lab: the ability for privileged operators to list, create, view, update, enable/disable, and assign roles to the platform's users through the Dashboard and a backing REST API. It is **Spec #3** of the Playwright/SDET learning roadmap and is a **brownfield enhancement** that extends — never rewrites — the existing backend security layer (`apps/backend`), the existing Nuxt frontend (`apps/frontend`), and the existing Keycloak realm.

The defining product decision for this spec is the **identity store model**. Users are **not** modeled as a local `app_user` database table. Instead, identity is owned by **Keycloak**, and the backend acts as a **façade (proxy) over the Keycloak Admin REST API**. Keycloak is the single source of truth for user id, username, email, enabled flag, user attributes (including `tenant_id` and `merchant_id`), and assigned composite roles. This avoids dual-write and synchronization complexity, and it teaches a realistic identity-administration integration pattern that SDETs encounter in production systems. The resolved decision and its rationale are recorded under Resolved Decisions.

The five named business roles introduced by the `iam-roles-and-keycloak-login` spec (PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER, SUPPORT_AGENT, READ_ONLY_USER) are the **assignable role set**. Tenant scoping reuses the model established by `tenant-model-and-isolation`: a user's tenant is the Keycloak user attribute `tenant_id`, carrying a natural-key Tenant_Reference (for example `TENANT_ALPHA`). A TENANT_ADMIN may manage only users whose `tenant_id` matches the TENANT_ADMIN's own resolved tenant; a PLATFORM_ADMIN may manage users across all tenants.

This spec introduces a new pair of authority families — `platform:users:*` and `tenant:users:*` — which require the `iam-roles-and-keycloak-login` realm composite roles to be **extended** with the new authorities. This is an explicit cross-spec dependency captured in the Implementation Sequencing & Prerequisites section and in the relevant requirements.

Because this is a QA/SDET learning application, the feature is designed to create realistic future test situations (multi-role RBAC on a CRUD admin surface, tenant isolation on user reads and writes, 401-vs-403 surfaces, conflict handling, masked cross-tenant reads, accessibility-first locators). **No Playwright test files are created by this spec**; a conceptual Future Playwright Scenarios section records the lessons to be written later. This document is a specification only and contains **no implementation**.

The existing REST contract conventions are preserved exactly: `application/problem+json` on every 4xx, `X-Correlation-ID` on every response, `Vary` on responses whose representation depends on request headers, masked `404` for cross-tenant reads, `403` for cross-tenant writes, and `409` for duplicate username/email. The Keycloak admin token used by the façade **never** reaches the browser; all browser calls flow through the Nuxt Server_Proxy, which itself calls the backend.

## Glossary

- **Backend_API**: The Payment Quality Lab Spring Boot REST API exposed under `/api/*`, including its Spring Security authorization rules and service layer.
- **User_Management_Facade**: The Backend_API component that exposes the `/api/users` endpoints and translates each request into one or more Keycloak Admin REST API operations. It owns no user persistence of its own.
- **Keycloak_Admin_API**: The administrative REST API of the Keycloak server (the realm-admin endpoints for users and role mappings), distinct from the OIDC token endpoints used for login.
- **Keycloak_Admin_Client**: The backend client, authenticated by a dedicated service-account or admin credential, that the User_Management_Facade uses to call the Keycloak_Admin_API.
- **Keycloak_Admin_Token**: The privileged access token obtained by the Keycloak_Admin_Client to authorize Keycloak_Admin_API calls. It is a server-side secret.
- **Keycloak_Realm**: The `payment-quality` Keycloak realm defined by `infra/keycloak/realms/payment-quality-realm.json`.
- **Managed_User**: A user identity that lives in the Keycloak_Realm and is administered through the User_Management_Facade. Its fields are `id`, `username`, `email`, `enabled`, attributes (including `tenant_id` and optional `merchant_id`), and assigned Composite_Roles.
- **User_Id**: The Keycloak-assigned immutable identifier of a Managed_User (the Keycloak user UUID), used in `/api/users/{id}` paths.
- **Composite_Role**: One of the five named Keycloak realm composite roles: PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER, SUPPORT_AGENT, READ_ONLY_USER. These are the only assignable roles for user management.
- **Fine_Grained_Authority**: A low-level permission string enforced by the Backend_API (for example `platform:merchants:read`). The complete pre-existing set is unchanged except for the additive user-management authorities defined by this spec.
- **User_Management_Authority**: One of the new Fine_Grained_Authorities introduced by this spec: `platform:users:read`, `platform:users:create`, `platform:users:update`, `platform:users:assign-roles`, `tenant:users:read`, `tenant:users:create`, `tenant:users:update`, `tenant:users:assign-roles`.
- **Tenant_Reference**: The stable, human-readable natural-key string that identifies a tenant (for example `TENANT_ALPHA`), as defined by `tenant-model-and-isolation`. It is the value carried by the `tenant_id` user attribute and the `tenant_id` JWT claim.
- **Tenant_Resolver**: The backend component introduced by `tenant-model-and-isolation` that resolves the acting principal's `tenant_id` claim to a tenant and classifies the principal as platform-scoped or tenant-scoped.
- **Acting_Principal**: The authenticated user making a user-management request, identified by the validated JWT (granted authorities, `tenant_id` claim).
- **Platform_Scoped_Principal**: An Acting_Principal whose resolved tenant classification is platform-wide; entitled to manage users across all tenants via `platform:users:*`.
- **Tenant_Scoped_Principal**: An Acting_Principal restricted to a single tenant; entitled to manage only users whose `tenant_id` matches the principal's resolved Tenant_Reference via `tenant:users:*`.
- **Dashboard**: The Nuxt 4 frontend application at `apps/frontend`.
- **Server_Proxy**: The Nuxt server routes (`server/api/**`, `server/utils/backendApi.ts`) that attach the bearer token server-side and forward requests to the Backend_API. The browser never holds the bearer token.
- **Users_Page**: The Dashboard route `/admin/users` that lists users and hosts the create, edit, and role-assignment surfaces.
- **Masked_Not_Found**: A `404 Not Found` response with the standard `not_found` problem shape returned for a resource that exists but is not visible to the Acting_Principal, so existence is not disclosed.
- **Problem_Response**: A `4xx` response with `Content-Type: application/problem+json` carrying the members `type`, `title`, `status`, `detail`, `instance`.
- **Rbac_Matrix**: The frontend capability matrix in `apps/frontend/app/utils/rbacMatrix.ts` that maps each Composite_Role to capability booleans.
- **Test_Id**: A stable `data-testid` attribute used as a future Playwright locator; stable means byte-identical across rebuilds and sessions and resolving to exactly one element per rendered page.
- **Temporary_Password**: An initial password set on a newly created Managed_User that the user is required to change at first login.

## Personas

- **Platform_Admin_User**: Holds the PLATFORM_ADMIN Composite_Role. Manages users across all tenants, including choosing the tenant for a newly created user.
- **Tenant_Admin_User**: Holds the TENANT_ADMIN Composite_Role. Manages only the users belonging to the Tenant_Admin_User's own resolved tenant; new users are auto-assigned to that tenant.
- **Merchant_Manager_User**: Holds the MERCHANT_MANAGER Composite_Role. Has no user-management access.
- **Support_Agent_User**: Holds the SUPPORT_AGENT Composite_Role. Has no user-management access in this spec.
- **Read_Only_Viewer**: Holds the READ_ONLY_USER Composite_Role. Has no user-management access.
- **QA_Automation_Engineer**: Writes future REST Assured and Playwright tests; needs deterministic role/tenant fixtures, an explicit RBAC and isolation matrix, stable Test_Ids, and predictable outcomes (403 forbidden, masked 404 cross-tenant reads, 409 duplicates).

## In Scope

- A User_Management_Facade in the Backend_API exposing `GET /api/users`, `POST /api/users`, `GET /api/users/{id}`, `PATCH /api/users/{id}`, and `POST /api/users/{id}/roles`, each implemented as a proxy over the Keycloak_Admin_API.
- A Keycloak_Admin_Client authenticated by a dedicated service-account/admin credential, used by the façade for: create user, list users, get user, update user (email/enabled/attributes), enable/disable, and assign/remove realm composite roles.
- Use of Keycloak as the single source of truth for identity; no local user table, no dual-write, no synchronization job.
- Tenant scoping of user-management operations using the Managed_User's `tenant_id` attribute and the Tenant_Resolver classification of the Acting_Principal.
- A new set of additive User_Management_Authorities (`platform:users:*`, `tenant:users:*`) and the documented extension of the `iam-roles-and-keycloak-login` realm composite-role definitions to grant them.
- Assignment and removal of the five Composite_Roles as the only assignable roles.
- A documented safe-edit (re-fetch before update) approach for the absence of ETags in the Keycloak_Admin_API, with the last-write-wins risk recorded as an explicit requirement.
- A `/admin/users` Users_Page in the Dashboard: paginated, filterable list; create in a modal; edit in a slide-over; role assignment via a searchable multiple-select; full set of UI states.
- A role-gated navigation link to the Users_Page, visible only to PLATFORM_ADMIN and TENANT_ADMIN.
- Extension of the frontend Rbac_Matrix with `canManageUsers` and `canAssignRoles` capabilities.
- Preservation of all existing REST contract conventions (problem+json, `X-Correlation-ID`, `Vary`, masked-404 cross-tenant reads, 403 cross-tenant writes, 409 duplicate username/email).
- Backend-authoritative authorization and tenant isolation, independent of any UI gating; Zod validation on forms as a convenience only.
- A conceptual Future Playwright Scenarios section (no test files).

## Out of Scope (Non-Goals)

- A local `app_user` database table, any user persistence in PostgreSQL, dual-write, or a Keycloak-to-database synchronization process.
- User deletion, password reset/forgot-password flows, email verification flows, and self-service profile editing (the acting user editing their own account) — each may be a later spec.
- Group-based modeling of tenants in Keycloak (tenant remains a user attribute, consistent with current specs).
- Creating, editing, or deleting tenants (owned by `tenant-model-and-isolation` and a future `tenant-management` spec).
- Assigning raw Realm_Authority_Roles directly to users; only the five Composite_Roles are assignable.
- Any Playwright or other automated frontend test files (deferred to future learning lessons).
- Bulk user actions, CSV import/export of users, and a notifications center (separate roadmap items).
- PSP integration, Kafka, webhooks, settlement, or any item in the project-wide active non-goals.

## Resolved Decisions

### Decision 1: Identity store — Keycloak Admin API façade vs local app_user table

**Original question:** Should user management persist users in a local `app_user` table (with its own CRUD and a sync to Keycloak), or should the backend act as a façade over the Keycloak Admin REST API with Keycloak as the single source of truth?

**Resolved:** Use the **Keycloak_Admin_API façade**. The Backend_API holds no user table; every user-management operation is translated into Keycloak_Admin_API calls, and Keycloak remains the single source of truth for identity, attributes, and role assignments. **Rationale:** a local table would require dual-write and a synchronization mechanism that is error-prone and out of proportion to the learning value; the façade teaches a realistic identity-administration integration (service-account admin client, attribute-based tenant scoping, role-mapping APIs) and keeps identity authoritative in one place. The trade-offs (no ETags, last-write-wins) are accepted and addressed by Requirement 9.

### Decision 2: Assignable roles limited to the five composite roles

**Original question:** May an administrator assign arbitrary Keycloak roles, or only the five named business composite roles?

**Resolved:** Only the five Composite_Roles are assignable. **Rationale:** raw authority roles are composition building blocks (per the `iam-roles-and-keycloak-login` Decision 1); exposing them in the UI would break the one-role-per-identity teaching model and the RBAC matrix.

## Requirements

### Requirement 1: User-Management Authorities and Realm Composite-Role Extension

**User Story:** As a Platform_Admin_User, I want user-management permissions modeled as fine-grained authorities granted through the existing composite roles, so that user administration is authorized consistently with the rest of the platform.

#### Acceptance Criteria

1. THE Keycloak_Realm SHALL define eight additive User_Management_Authorities exposed as realm authority roles: `platform:users:read`, `platform:users:create`, `platform:users:update`, `platform:users:assign-roles`, `tenant:users:read`, `tenant:users:create`, `tenant:users:update`, and `tenant:users:assign-roles`.
2. THE Keycloak_Realm SHALL extend the PLATFORM_ADMIN Composite_Role definition to include the four `platform:users:*` User_Management_Authorities.
3. THE Keycloak_Realm SHALL extend the TENANT_ADMIN Composite_Role definition to include the four `tenant:users:*` User_Management_Authorities.
4. THE Keycloak_Realm SHALL compose the MERCHANT_MANAGER, SUPPORT_AGENT, and READ_ONLY_USER Composite_Roles without any User_Management_Authority.
5. THE Backend_API SHALL convert each User_Management_Authority realm role into its corresponding granted authority using the existing Role_Converter rule without modifying that rule.
6. WHEN the extended Keycloak_Realm import is loaded into a Keycloak instance, THE Keycloak_Realm SHALL import without error and SHALL expose the eight User_Management_Authorities and the extended composite-role mappings.
7. THE requirements document SHALL record the mapping of each Composite_Role to the User_Management_Authorities it grants in the RBAC matrix in Requirement 11.

### Requirement 2: Keycloak Admin Client for the Façade

**User Story:** As a Platform_Admin_User, I want the backend to perform user administration through a dedicated Keycloak admin client, so that user changes are applied to the single identity source of truth without the backend storing users itself.

#### Acceptance Criteria

1. THE User_Management_Facade SHALL use a Keycloak_Admin_Client authenticated by a dedicated service-account or admin credential to call the Keycloak_Admin_API.
2. THE User_Management_Facade SHALL support, through the Keycloak_Admin_Client, the operations create user, list users, get user by id, update user email, update user enabled flag, update user attributes, and assign and remove realm composite roles.
3. THE Backend_API SHALL hold the Keycloak_Admin_Token only server-side and SHALL NOT include the Keycloak_Admin_Token in any response body, response header, or log entry.
4. THE Backend_API SHALL treat Keycloak as the single source of truth for Managed_User identity, attributes, and role assignments, and SHALL NOT persist Managed_User records in its own database.
5. IF the Keycloak_Admin_API is unreachable or returns a server-side failure when the User_Management_Facade performs an operation, THEN THE Backend_API SHALL return a Problem_Response with status `502` and SHALL exclude the Keycloak_Admin_Token from the response.
6. WHEN the User_Management_Facade obtains or refreshes the Keycloak_Admin_Token, THE Backend_API SHALL obtain the token through the Keycloak_Admin_Client's own credential and SHALL NOT reuse the Acting_Principal's bearer token for Keycloak_Admin_API calls.

### Requirement 3: List Users

**User Story:** As a Tenant_Admin_User, I want to list users with filtering and pagination, so that I can find and review the users I am responsible for.

#### Acceptance Criteria

1. WHEN a Platform_Scoped_Principal holding `platform:users:read` requests `GET /api/users`, THE Backend_API SHALL return a paginated list of Managed_Users across all tenants.
2. WHEN a Tenant_Scoped_Principal holding `tenant:users:read` requests `GET /api/users`, THE Backend_API SHALL return only Managed_Users whose `tenant_id` attribute matches the principal's resolved Tenant_Reference.
3. WHERE a `role` filter parameter is supplied, THE Backend_API SHALL return only Managed_Users assigned the specified Composite_Role.
4. WHERE a `status` filter parameter with value `enabled` or `disabled` is supplied, THE Backend_API SHALL return only Managed_Users whose `enabled` flag matches the requested status.
5. WHERE a `search` filter parameter is supplied, THE Backend_API SHALL return only Managed_Users whose username or email contains the supplied value.
6. THE Backend_API SHALL paginate the user list using the same page and size parameter conventions used by the existing payment-order list endpoint, with a maximum page size of 100.
7. WHEN the Backend_API returns a user-list response, THE Backend_API SHALL include the `X-Correlation-ID` response header and SHALL include a `Vary` response header containing `Authorization`.
8. IF an Acting_Principal lacking both `platform:users:read` and `tenant:users:read` requests `GET /api/users`, THEN THE Backend_API SHALL return a Problem_Response with status `403`.
9. THE Backend_API SHALL exclude every credential value, including any Temporary_Password and the Keycloak_Admin_Token, from each user-list entry.

### Requirement 4: Create User

**User Story:** As a Platform_Admin_User, I want to create a new user with an initial role set and tenant assignment, so that a new operator can sign in with appropriate access.

#### Acceptance Criteria

1. WHEN a Platform_Scoped_Principal holding `platform:users:create` submits `POST /api/users` with a username, email, Temporary_Password, a `tenant_id` Tenant_Reference, an optional `merchant_id`, and an initial set of Composite_Roles, THE Backend_API SHALL create the Managed_User in Keycloak with those values and SHALL return status `201`.
2. WHEN a Tenant_Scoped_Principal holding `tenant:users:create` submits `POST /api/users`, THE Backend_API SHALL set the new Managed_User's `tenant_id` attribute to the principal's resolved Tenant_Reference and SHALL ignore any `tenant_id` value supplied in the request body.
3. IF a Platform_Scoped_Principal submits `POST /api/users` without a `tenant_id` field, THEN THE Backend_API SHALL reject the request with a Problem_Response of status `400`.
4. IF a `POST /api/users` request supplies an initial role that is not one of the five Composite_Roles, THEN THE Backend_API SHALL reject the request with a Problem_Response of status `400`.
5. IF a `POST /api/users` request supplies a username or email that already exists in the Keycloak_Realm, THEN THE Backend_API SHALL reject the request with a Problem_Response of status `409`.
6. WHEN the Backend_API creates a Managed_User, THE Backend_API SHALL set the Temporary_Password as a credential requiring change at first login and SHALL set the new account's `enabled` flag to true.
7. WHEN the Backend_API returns a `201` create response, THE Backend_API SHALL include a `Location` response header referencing `/api/users/{id}` of the created Managed_User and SHALL include the `X-Correlation-ID` response header.
8. IF an Acting_Principal lacking both `platform:users:create` and `tenant:users:create` submits `POST /api/users`, THEN THE Backend_API SHALL return a Problem_Response with status `403`.
9. THE Backend_API SHALL exclude the Temporary_Password value from the create response body and from any log entry.

### Requirement 5: Get User by Id

**User Story:** As a Tenant_Admin_User, I want to retrieve a single user's details, so that I can review the user's email, status, tenant, and assigned roles before editing.

#### Acceptance Criteria

1. WHEN a Platform_Scoped_Principal holding `platform:users:read` requests `GET /api/users/{id}` for an existing Managed_User, THE Backend_API SHALL return that Managed_User with status `200`.
2. WHEN a Tenant_Scoped_Principal holding `tenant:users:read` requests `GET /api/users/{id}` for a Managed_User whose `tenant_id` matches the principal's resolved Tenant_Reference, THE Backend_API SHALL return that Managed_User with status `200`.
3. IF a Tenant_Scoped_Principal requests `GET /api/users/{id}` for a Managed_User whose `tenant_id` does not match the principal's resolved Tenant_Reference, THEN THE Backend_API SHALL return Masked_Not_Found with status `404`.
4. IF an Acting_Principal requests `GET /api/users/{id}` for a User_Id that does not exist, THEN THE Backend_API SHALL return Masked_Not_Found with status `404` using the same response shape as criterion 3.
5. WHEN the Backend_API returns a single-user response, THE Backend_API SHALL include the `X-Correlation-ID` response header and a `Vary` response header containing `Authorization`.
6. THE Backend_API SHALL include in the single-user response the `id`, `username`, `email`, `enabled` flag, `tenant_id`, optional `merchant_id`, and the list of assigned Composite_Roles, and SHALL exclude every credential value.

### Requirement 6: Update User

**User Story:** As a Tenant_Admin_User, I want to update a user's email, enabled status, and attributes, so that I can keep account information correct and disable accounts that should no longer sign in.

#### Acceptance Criteria

1. WHEN a Platform_Scoped_Principal holding `platform:users:update` submits `PATCH /api/users/{id}` with any of email, `enabled`, or attributes for an existing Managed_User, THE Backend_API SHALL apply the supplied changes in Keycloak and SHALL return status `200`.
2. WHEN a Tenant_Scoped_Principal holding `tenant:users:update` submits `PATCH /api/users/{id}` for a Managed_User whose `tenant_id` matches the principal's resolved Tenant_Reference, THE Backend_API SHALL apply the supplied changes and SHALL return status `200`.
3. IF a Tenant_Scoped_Principal submits `PATCH /api/users/{id}` for a Managed_User whose `tenant_id` does not match the principal's resolved Tenant_Reference, THEN THE Backend_API SHALL deny the request with a Problem_Response of status `403`.
4. IF a `PATCH /api/users/{id}` request supplies an email that already belongs to a different Managed_User in the Keycloak_Realm, THEN THE Backend_API SHALL reject the request with a Problem_Response of status `409`.
5. IF a `PATCH /api/users/{id}` request targets a User_Id that does not exist, THEN THE Backend_API SHALL return Masked_Not_Found with status `404` for a Tenant_Scoped_Principal and a `404` Problem_Response for a Platform_Scoped_Principal.
6. WHEN a `PATCH /api/users/{id}` request sets the `enabled` flag to false for an existing in-scope Managed_User, THE Backend_API SHALL disable the Managed_User in Keycloak so that the account can no longer authenticate.
7. WHEN a `PATCH /api/users/{id}` request sets the `enabled` flag to true for an existing in-scope Managed_User, THE Backend_API SHALL enable the Managed_User in Keycloak.
8. IF an Acting_Principal lacking both `platform:users:update` and `tenant:users:update` submits `PATCH /api/users/{id}`, THEN THE Backend_API SHALL return a Problem_Response with status `403`.
9. WHEN the Backend_API returns an update response, THE Backend_API SHALL include the `X-Correlation-ID` response header and SHALL exclude every credential value from the body and logs.

### Requirement 7: Enable and Disable as Part of Update

**User Story:** As a Tenant_Admin_User, I want enabling and disabling a user to use the same update operation, so that account status changes follow one consistent contract.

#### Acceptance Criteria

1. THE Backend_API SHALL expose enabling and disabling a Managed_User through the `enabled` field of the `PATCH /api/users/{id}` operation and SHALL NOT require a separate enable or disable endpoint.
2. WHEN a `PATCH /api/users/{id}` request changes only the `enabled` flag, THE Backend_API SHALL apply that single change and SHALL leave the email, attributes, and role assignments unchanged.
3. THE requirements document SHALL record the resolved decision to fold enable/disable into `PATCH /api/users/{id}` rather than provide separate endpoints, with its rationale, in the Resolved Decisions narrative of the design phase, and SHALL list the alternative in Open Questions until confirmed.

### Requirement 8: Assign and Remove Composite Roles

**User Story:** As a Tenant_Admin_User, I want to assign and remove a user's business roles, so that the user's access reflects their responsibilities.

#### Acceptance Criteria

1. WHEN a Platform_Scoped_Principal holding `platform:users:assign-roles` submits `POST /api/users/{id}/roles` specifying Composite_Roles to assign and remove for an existing Managed_User, THE Backend_API SHALL apply the corresponding realm composite-role mappings in Keycloak and SHALL return status `200`.
2. WHEN a Tenant_Scoped_Principal holding `tenant:users:assign-roles` submits `POST /api/users/{id}/roles` for a Managed_User whose `tenant_id` matches the principal's resolved Tenant_Reference, THE Backend_API SHALL apply the role changes and SHALL return status `200`.
3. IF a Tenant_Scoped_Principal submits `POST /api/users/{id}/roles` for a Managed_User whose `tenant_id` does not match the principal's resolved Tenant_Reference, THEN THE Backend_API SHALL deny the request with a Problem_Response of status `403`.
4. IF a `POST /api/users/{id}/roles` request names any role that is not one of the five Composite_Roles, THEN THE Backend_API SHALL reject the request with a Problem_Response of status `400`.
5. IF a `POST /api/users/{id}/roles` request targets a User_Id that does not exist, THEN THE Backend_API SHALL return Masked_Not_Found with status `404` for a Tenant_Scoped_Principal and a `404` Problem_Response for a Platform_Scoped_Principal.
6. IF an Acting_Principal lacking both `platform:users:assign-roles` and `tenant:users:assign-roles` submits `POST /api/users/{id}/roles`, THEN THE Backend_API SHALL return a Problem_Response with status `403`.
7. WHEN the Backend_API returns a role-assignment response, THE Backend_API SHALL include the updated set of assigned Composite_Roles and the `X-Correlation-ID` response header.

### Requirement 9: Concurrency and Safe-Edit Without ETags

**User Story:** As a QA_Automation_Engineer, I want the documented behavior for concurrent edits to be explicit, so that I understand the safe-edit approach and the last-write-wins risk the Keycloak Admin API imposes.

#### Acceptance Criteria

1. THE requirements document SHALL record that the Keycloak_Admin_API does not expose ETags or `If-Match` preconditions for user resources, so user-management writes carry no optimistic-locking precondition.
2. WHEN the User_Management_Facade performs an update or role-assignment operation, THE User_Management_Facade SHALL re-fetch the current Managed_User from the Keycloak_Admin_API immediately before applying the change so that the change is computed against the latest known state.
3. THE Backend_API SHALL NOT require an `If-Match` header on `PATCH /api/users/{id}` or `POST /api/users/{id}/roles` and SHALL NOT return `428` or `412` for those operations.
4. THE requirements document SHALL record the last-write-wins risk: when two administrators edit the same Managed_User concurrently, the later write overwrites the earlier write without conflict detection.
5. WHERE a write operation depends on a value that may have changed since the Acting_Principal last read the Managed_User, THE User_Management_Facade SHALL base the write on the immediately re-fetched state rather than on values cached from an earlier read.

### Requirement 10: REST Contract Conventions

**User Story:** As a QA_Automation_Engineer, I want the user-management endpoints to follow the platform's existing REST conventions, so that contract tests reuse the same assertions as the rest of the API.

#### Acceptance Criteria

1. WHEN the Backend_API returns any `4xx` response from a `/api/users` endpoint, THE Backend_API SHALL use `Content-Type: application/problem+json` with the members `type`, `title`, `status`, `detail`, and `instance`.
2. THE Backend_API SHALL include an `X-Correlation-ID` response header on every `/api/users` response, success or error.
3. THE Backend_API SHALL include a `Vary` response header containing `Authorization` on every `/api/users` read response.
4. WHEN a cross-tenant read is denied for a Tenant_Scoped_Principal, THE Backend_API SHALL return Masked_Not_Found with status `404` and the standard `not_found` problem shape.
5. WHEN a cross-tenant write is denied for a Tenant_Scoped_Principal, THE Backend_API SHALL return a Problem_Response with status `403`.
6. WHEN a create or update request conflicts with an existing username or email, THE Backend_API SHALL return a Problem_Response with status `409`.
7. THE Backend_API SHALL exclude bearer tokens, the Keycloak_Admin_Token, passwords, and Temporary_Passwords from every `/api/users` response body, response header, and log entry.
8. THE Server_Proxy SHALL forward `/api/users` requests to the Backend_API with the bearer token attached server-side and SHALL forward the `X-Correlation-ID`, `Location`, and `Vary` response headers to the browser without exposing the bearer token or the Keycloak_Admin_Token.

### Requirement 11: RBAC and Tenant-Isolation Matrix

**User Story:** As a QA_Automation_Engineer, I want an explicit matrix of which role may perform which user-management operation and the cross-tenant outcomes, so that I can write deterministic RBAC and isolation tests.

#### Acceptance Criteria

1. THE requirements document SHALL record an RBAC matrix mapping each Composite_Role to the User_Management_Authorities it grants and to the consequent allowed user-management operations.
2. THE Backend_API SHALL deny every user-management operation to MERCHANT_MANAGER, SUPPORT_AGENT, and READ_ONLY_USER with a Problem_Response of status `403`.
3. THE Backend_API SHALL produce, for a Tenant_Scoped_Principal acting on a Managed_User outside the principal's tenant, exactly the outcomes recorded in the matrix: Masked_Not_Found `404` for reads, and `403` for writes and role assignments.
4. THE Backend_API SHALL produce, for a Platform_Scoped_Principal, the cross-tenant-visible outcomes recorded in the matrix.

#### User-Management RBAC Matrix

| Operation (required authority) | PLATFORM_ADMIN | TENANT_ADMIN | MERCHANT_MANAGER | SUPPORT_AGENT | READ_ONLY_USER |
|---|---|---|---|---|---|
| List users (`platform:users:read` / `tenant:users:read`) | Allowed, all tenants | Allowed, own tenant only | Denied (403) | Denied (403) | Denied (403) |
| Get user (`platform:users:read` / `tenant:users:read`) | Allowed, any tenant | Allowed own tenant; cross-tenant → masked 404 | Denied (403) | Denied (403) | Denied (403) |
| Create user (`platform:users:create` / `tenant:users:create`) | Allowed, picks tenant | Allowed, auto own tenant | Denied (403) | Denied (403) | Denied (403) |
| Update user (`platform:users:update` / `tenant:users:update`) | Allowed, any tenant | Allowed own tenant; cross-tenant → 403 | Denied (403) | Denied (403) | Denied (403) |
| Assign/remove roles (`platform:users:assign-roles` / `tenant:users:assign-roles`) | Allowed, any tenant | Allowed own tenant; cross-tenant → 403 | Denied (403) | Denied (403) | Denied (403) |

Note: cross-tenant outcomes apply only to a Tenant_Scoped_Principal. A request rejected by the authority check returns `403` regardless of tenant.

### Requirement 12: Users Page — List, Filters, and Pagination

**User Story:** As a Tenant_Admin_User, I want a users screen with filters and pagination, so that I can browse and locate users efficiently.

#### Acceptance Criteria

1. THE Dashboard SHALL provide a Users_Page at the route `/admin/users` that renders a user list table sourced from `GET /api/users` through the Server_Proxy.
2. THE Users_Page SHALL provide controls to filter the list by role, by status, and by a free-text search over username and email, and SHALL reflect the active filters in the page URL query parameters.
3. THE Users_Page SHALL provide pagination controls consistent with the existing payment-order list page.
4. THE Users_Page SHALL render the user list table with a stable Test_Id `users-table` on the table root.
5. WHEN the Users_Page reads a `GET /api/users` response, THE Dashboard SHALL validate the response against its Zod schema before rendering and SHALL render an error state without rendering unvalidated data when validation fails.
6. THE Users_Page SHALL reuse the existing shared list, table, loading, empty, and error components rather than duplicating them, extending them where needed.

### Requirement 13: Users Page — Create, Edit, and Role Assignment Surfaces

**User Story:** As a Platform_Admin_User, I want to create users, edit users, and assign roles from the users screen, so that I can perform user administration without leaving the page.

#### Acceptance Criteria

1. THE Users_Page SHALL present a create-user control with a stable Test_Id `create-user-button` that opens a create-user form in a modal.
2. THE create-user form SHALL be a Nuxt UI modal containing a form with a stable Test_Id `create-user-form`, with fields for username, email, Temporary_Password, tenant, optional merchant, and initial roles, each input associated with a visible label.
3. WHERE the Acting_Principal is a Platform_Admin_User, THE create-user form SHALL present a tenant selection control; WHERE the Acting_Principal is a Tenant_Admin_User, THE create-user form SHALL omit tenant selection and SHALL apply the Tenant_Admin_User's own tenant.
4. THE Users_Page SHALL present an edit-user surface as a Nuxt UI slide-over with a stable Test_Id `edit-user-drawer`, allowing edits to email, enabled status, and attributes.
5. THE create-user form and the edit-user surface SHALL present role assignment as a searchable multiple-select control with a stable Test_Id `role-assignment-select`, offering only the five Composite_Roles.
6. THE Dashboard SHALL validate the create-user and edit-user form inputs with Zod before submission and SHALL treat backend validation as authoritative.
7. WHEN a create or edit submission succeeds, THE Dashboard SHALL show a dismissible success toast and SHALL refresh the affected list entry.
8. WHEN a create or edit submission returns a `409` Problem_Response, THE Dashboard SHALL present a conflict state derived from the problem details without discarding the user's entered values.

### Requirement 14: Role-Gated Navigation to the Users Page

**User Story:** As a Read_Only_Viewer, I want the users navigation entry hidden when I cannot use it, so that the interface reflects my access while the backend stays authoritative.

#### Acceptance Criteria

1. WHERE the Acting_Principal holds the PLATFORM_ADMIN or TENANT_ADMIN Composite_Role, THE Dashboard SHALL render a navigation link to the Users_Page with a stable Test_Id `nav-link-users`.
2. WHERE the Acting_Principal holds none of PLATFORM_ADMIN or TENANT_ADMIN, THE Dashboard SHALL omit the Users_Page navigation link.
3. IF an Acting_Principal without a User_Management_Authority navigates directly to `/admin/users`, THEN THE Dashboard SHALL present a forbidden (403) state and THE Backend_API SHALL return `403` to any `/api/users` request the page issues.
4. THE Dashboard SHALL treat navigation hiding as a convenience only and SHALL rely on the Backend_API as the authoritative enforcement point for every user-management operation.

### Requirement 15: Users Page — Required UI States

**User Story:** As a QA_Automation_Engineer, I want the users screen to implement every required UI state with stable locators, so that future Playwright lessons can assert each state deterministically.

#### Acceptance Criteria

1. WHILE a `GET /api/users` request is in flight, THE Users_Page SHALL render a loading state using the existing shared loading component.
2. WHEN `GET /api/users` returns an empty list with no active filters, THE Users_Page SHALL render an empty state using the existing shared empty-state component.
3. WHEN `GET /api/users` returns an empty list while filters are active, THE Users_Page SHALL render a filtered-empty state that indicates the active filters and offers to clear them.
4. WHEN a `/api/users` request returns a Problem_Response, THE Users_Page SHALL render an error state using the existing ProblemDetailsCard component.
5. WHEN a `/api/users` request returns `403`, THE Users_Page SHALL render a forbidden state distinct from the error state.
6. WHEN a create, edit, or role-assignment operation succeeds, THE Users_Page SHALL render a success state using a dismissible toast.
7. WHEN a create or edit operation returns `409`, THE Users_Page SHALL render a conflict state that explains the duplicate username or email.
8. THE Users_Page SHALL convey each state through text content so that the state is distinguishable without relying on color alone.

### Requirement 16: Frontend RBAC Matrix Extension

**User Story:** As a frontend developer, I want the capability matrix to express user-management capabilities, so that navigation and action gating derive from one source of truth.

#### Acceptance Criteria

1. THE Rbac_Matrix SHALL define a `canManageUsers` capability and a `canAssignRoles` capability for each Composite_Role.
2. THE Rbac_Matrix SHALL grant `canManageUsers` and `canAssignRoles` to PLATFORM_ADMIN and to TENANT_ADMIN.
3. THE Rbac_Matrix SHALL deny `canManageUsers` and `canAssignRoles` to MERCHANT_MANAGER, SUPPORT_AGENT, and READ_ONLY_USER.
4. THE Dashboard SHALL derive the visibility of the Users_Page navigation link and the create, edit, and role-assignment controls from the `canManageUsers` and `canAssignRoles` capabilities.

### Requirement 17: Accessibility and Testability of the Users Surfaces

**User Story:** As a QA_Automation_Engineer, I want the users surfaces to be accessible and locator-friendly, so that future Playwright lessons use stable, semantic locators.

#### Acceptance Criteria

1. THE Users_Page SHALL render a single semantic top-level heading and SHALL associate every filter and form input with a visible label.
2. THE Users_Page SHALL assign stable, unique Test_Ids `users-table`, `create-user-button`, `create-user-form`, `edit-user-drawer`, and `role-assignment-select`, each resolving to exactly one element per rendered page, and SHALL provide row-scoped Test_Ids that identify per-user actions by User_Id.
3. THE Dashboard SHALL prefer semantic locators (role and accessible name, then label, then text) over Test_Ids, using Test_Ids only where semantic locators are insufficient.
4. THE create-user modal and the edit-user slide-over SHALL trap keyboard focus while open and SHALL return focus to the triggering control when closed.
5. THE role-assignment multiple-select SHALL be fully keyboard-navigable, supporting selection, removal, and dismissal without a mouse.
6. THE Users_Page SHALL render the user-list table with `<th scope="col">` column headers and accessible names on all row-level action controls.

## Implementation Sequencing & Prerequisites

This section makes the cross-spec implementation order unambiguous so that future implementation cannot start `user-management` before its prerequisites exist.

### Roadmap position

`user-management` is **Spec #3** of the Playwright/SDET roadmap. The full order is:

1. `iam-roles-and-keycloak-login` — five composite roles, real OIDC login, role-aware UI. **Spec exists; not yet implemented.**
2. `tenant-model-and-isolation` — tenant database entity, `tenant_id` resolution, enforced isolation. **Spec exists; not yet implemented.**
3. `user-management` — **this spec.**
4. `audit-log-dashboard`.
5. `deterministic-seed-and-test-isolation`.

### Hard prerequisites (must be implemented first)

**Prerequisite A — `iam-roles-and-keycloak-login` must be implemented before this spec.** Reasons:

- This spec's authorization model is built on the five Composite_Roles and on the `tenant_id` JWT claim, both introduced by Spec #1. Without them there are no named roles to assign and no tenant claim to scope by.
- This spec **extends** the Spec #1 realm composite-role definitions with the new `platform:users:*` and `tenant:users:*` authorities (Requirement 1). That extension can only be applied to composite roles that already exist.
- The frontend gating in this spec consumes the `useAuthorization` composable and the `rbacMatrix.ts` capability source of truth created by Spec #1 (Requirement 16). This spec adds `canManageUsers` and `canAssignRoles` to that existing matrix; it does not create the matrix.
- Roles must already be exposed on the server-side session so the Dashboard can gate the `nav-link-users` link (Requirement 14).

**Prerequisite B — `tenant-model-and-isolation` must be implemented before this spec.** Reasons:

- This spec scopes user-management reads and writes by tenant using the Acting_Principal's resolved tenant. It relies on the `Tenant_Resolver` and the platform-vs-tenant classification delivered by Spec #2.
- The `tenant_id` user attribute values used to scope users (for example `TENANT_ALPHA`, `PLATFORM_TENANT`) are the same Tenant_Reference natural keys established and seeded by Spec #2. Tenant scoping of users is meaningless until those tenant references resolve to real tenants.
- The masked-404 cross-tenant read behavior and the 403 cross-tenant write behavior reused by this spec (Requirements 5, 6, 8, 10, 11) are the isolation patterns that Spec #2 introduces and enforces.

### Sequencing within this spec

When this spec is implemented (after A and B), the recommended internal order is:

1. Extend the Keycloak realm with the eight User_Management_Authorities and add them to the PLATFORM_ADMIN and TENANT_ADMIN composite roles (Requirement 1).
2. Add the Keycloak_Admin_Client and the User_Management_Facade backend module, then the five endpoints with authority checks and tenant scoping (Requirements 2–11).
3. Extend the frontend Rbac_Matrix with `canManageUsers` and `canAssignRoles` (Requirement 16).
4. Build the `/admin/users` Users_Page, role-gated navigation, and the create/edit/role-assignment surfaces with all required states (Requirements 12–15, 17).

### Cross-spec dependency notes for downstream specs

- `audit-log-dashboard` (Spec #4) will likely record user-management actions (create/update/role-assignment) as audited events; this spec does not implement auditing, but its operations are natural audit sources.
- `deterministic-seed-and-test-isolation` (Spec #5) will provide predictable users/roles/tenants; the deterministic test users from Spec #1 are sufficient for this spec's manual verification.

## Future Playwright Scenarios (Conceptual)

No Playwright files are created by this spec. The following scenarios are recorded for future lessons once the roadmap reaches the testing phase. They assume per-role storage states from `iam-roles-and-keycloak-login` and deterministic tenants from `tenant-model-and-isolation`.

- **RBAC navigation visibility:** for each of the five roles, assert that `nav-link-users` is visible for PLATFORM_ADMIN and TENANT_ADMIN and absent for the other three.
- **Forbidden direct navigation:** as READ_ONLY_USER, navigate directly to `/admin/users` and assert the forbidden (403) state.
- **List, filter, and pagination:** as TENANT_ADMIN, assert `users-table` shows only own-tenant users; apply role and status filters and a search term; assert filters appear in the URL and the filtered-empty state when no match.
- **Create user happy path:** as PLATFORM_ADMIN, open `create-user-button`, fill `create-user-form` including tenant and roles via `role-assignment-select`, submit, and assert the success toast and the new row.
- **Create user conflict:** submit a username/email that already exists and assert the `409` conflict state preserves entered values.
- **Tenant auto-assignment:** as TENANT_ADMIN, assert the create form omits tenant selection and the created user is bound to the admin's tenant.
- **Edit and disable:** open `edit-user-drawer`, change email and toggle enabled to false, submit, and assert the updated row and disabled status.
- **Role assignment:** assign and remove a composite role via `role-assignment-select` and assert the updated role set.
- **Cross-tenant masking:** as TENANT_ADMIN, attempt to GET a user in another tenant (via API-assisted setup) and assert masked `404`; attempt a cross-tenant PATCH and assert `403`.
- **Token confidentiality:** assert no panel or response visible to the browser ever exposes the bearer token or the Keycloak admin token.

## Open Questions

1. **New Spring Modulith module name.** Should the User_Management_Facade live in a new module named `iam`, a module named `usermanagement`, or a shared `admin` module alongside future audit administration? The choice affects module boundaries verified by `ModulithArchitectureTest`. (Leaning: a dedicated module; to be resolved in design.)
2. **Enable/disable endpoint shape.** This spec folds enable/disable into `PATCH /api/users/{id}` (Requirement 7). Confirm this over the alternative of dedicated `POST /api/users/{id}/enable` and `POST /api/users/{id}/disable` endpoints that would mirror the merchant activate/suspend pattern.
3. **SUPPORT_AGENT read-only access to users.** This spec denies SUPPORT_AGENT all user-management access. Confirm, or grant SUPPORT_AGENT a read-only `*:users:read` view (no create/update/assign) to support a help-desk lookup use case.
4. **Listing performance over the Keycloak Admin API.** Keycloak admin user search and role filtering have known pagination and filtering limitations. Confirm whether server-side role filtering should be delegated to Keycloak or applied in the façade after fetching a page, and document the resulting performance trade-off in design.
5. **Merchant assignment validation.** When an optional `merchant_id` is supplied at create or update, should the façade validate it against existing merchants (cross-module call) or accept it as an opaque attribute as the realm does today? (Leaning: opaque, consistent with the existing `merchant_id` claim handling.)
