# Requirements Document

## Introduction

This feature introduces a realistic multi-role Identity and Access Management (IAM) foundation and a real Keycloak OpenID Connect (OIDC) login flow for the Payment Quality Engineering Lab. The goal is to give the learning application a meaningful Role-Based Access Control (RBAC) model, server-side OIDC authentication, role-aware user interface rendering, protected and role-specific behavior, and a clear distinction between unauthenticated (401) and forbidden (403) outcomes.

This is a BROWNFIELD enhancement. The existing backend security (`apps/backend` — Spring Boot 4 / Spring Modulith / Spring Security JWT resource server) and the existing frontend session layer (`apps/frontend` — Nuxt 4 + @nuxt/ui + Pinia + Zod + `nuxt-auth-utils`) MUST be extended, not rewritten or duplicated. The existing fine-grained authorities (`platform:merchants:create|read|update-status`, `merchant:payments:create|read|operate|lifecycle`, `platform:payments:read|lifecycle|audit`) and the REST contracts that depend on them MUST remain stable and backward compatible with existing backend security tests.

The five named business roles are modeled in Keycloak as COMPOSITE realm roles that aggregate the existing fine-grained authorities. The named roles do not replace the fine-grained authorities; they map onto them. The backend continues to authorize on the fine-grained authorities it already enforces, so no REST contract or `@PreAuthorize` expression needs to change for authorization to keep working.

Because this is a QA/SDET learning application, the feature is designed to create realistic future test situations: multi-role storage states, an RBAC access matrix, permission-based rendering, authorization-bypass checks, 401-versus-403 flows, and accessibility-first locators. No Playwright test files are created by this spec; the application must simply be highly testable, accessible, and locator-friendly so those lessons can be written later.

A minimal `tenant_id` concept is introduced (a JWT claim alongside the existing `merchant_id` claim, plus deterministic tenant values on test users) so that role scoping is meaningful. Full tenant CRUD and tenant isolation enforcement are explicitly OUT OF SCOPE for this spec and are deferred to a separate follow-up spec named `tenant-model-and-isolation`.

## Glossary

- **Keycloak_Realm**: The `payment-quality` Keycloak realm defined by the import file `infra/keycloak/realms/payment-quality-realm.json`.
- **Fine_Grained_Authority**: An existing low-level permission string enforced by the Backend_API. The complete set is: `platform:merchants:create`, `platform:merchants:read`, `platform:merchants:update-status`, `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, `merchant:payments:lifecycle`, `platform:payments:read`, `platform:payments:lifecycle`, `platform:payments:audit`.
- **Realm_Authority_Role**: An existing non-composite Keycloak realm role that corresponds to a single Fine_Grained_Authority after conversion (for example the realm role `merchants:create` becomes the authority `platform:merchants:create`).
- **Composite_Role**: A named Keycloak realm role that aggregates a defined set of Realm_Authority_Roles. The five Composite_Roles are PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER, SUPPORT_AGENT, and READ_ONLY_USER.
- **Role_Converter**: The backend `KeycloakRealmRoleConverter`, which converts realm role names from the `realm_access.roles` JWT claim into Fine_Grained_Authority values.
- **Backend_API**: The Payment Quality Lab Spring Boot REST API exposed under `/api/*`, including its Spring Security authorization rules.
- **Dashboard**: The Nuxt 4 frontend application at `apps/frontend`.
- **Server_Proxy**: The Nuxt server routes (`server/api/**`, `server/routes/auth/**`, `server/utils/backendApi.ts`) that attach the bearer token server-side and forward requests to the Backend_API.
- **Auth_Session**: The server-side `nuxt-auth-utils` session that holds the OIDC tokens and the derived user identity. The bearer token is never exposed to the browser.
- **OIDC_Login_Flow**: The Keycloak Authorization Code flow with PKCE initiated and completed through the Server_Proxy via `nuxt-auth-utils`.
- **Authenticated_User**: A user with a valid, non-expired Auth_Session.
- **Tenant_Id**: A string identifier representing the tenant a user belongs to, carried as the `tenant_id` JWT claim and stored as a Keycloak user attribute.
- **Merchant_Id**: The existing string identifier carried as the `merchant_id` JWT claim and stored as a Keycloak user attribute.
- **Login_Page**: The Dashboard route `/login` that presents the control to start the OIDC_Login_Flow.
- **Forbidden_Page**: The Dashboard surface shown when an Authenticated_User attempts an action or route they are not authorized for (the 403 case).
- **Auth_Required_Redirect**: The Dashboard behavior that sends an unauthenticated visitor to the Login_Page (the 401 / session-missing case).
- **Role_Aware_Navigation**: The sidebar links and Overview content that are rendered according to the Composite_Roles of the Authenticated_User.
- **Test_Id**: A stable `data-testid` attribute used as a Playwright locator. Stable means byte-identical across rebuilds, sessions, and styling changes, and resolving to exactly one element per rendered page.
- **Test_User**: A deterministic Keycloak user defined in the realm import with a fixed username, a fixed password, an enabled account, and a fixed set of Composite_Roles, intended for predictable role identities in future tests.
- **Masked_Authorization**: A display representation of the `Authorization` header in which the token value is replaced by a fixed placeholder (for example `Bearer ••••••••`).

## Personas

- **Platform_Operator**: A platform-level user who manages merchants/tenants and reads platform-wide data.
- **Merchant_User**: A user scoped to a merchant/tenant who creates and operates payment orders within that scope.
- **Support_User**: A user who reads operational data to assist customers and records non-financial operational notes, without changing financial state.
- **Read_Only_Viewer**: A user who only views lists and details with no write capability.
- **QA_Automation_Engineer**: Writes future Playwright/SDET tests; needs stable Test_Id locators, deterministic Test_Users per role, an explicit RBAC matrix, and clear 401-versus-403 surfaces.

## In Scope

- Five named Composite_Roles in the Keycloak_Realm mapping onto existing Fine_Grained_Authorities.
- Deterministic enabled Test_Users, one per Composite_Role.
- A `tenant_id` JWT claim and minimal tenant association on users.
- A real OIDC_Login_Flow, logout, and session-missing handling via the Server_Proxy.
- A Forbidden_Page (403) and an Auth_Required_Redirect (401) with a clear UI distinction between the two.
- Role_Aware_Navigation and role-aware action visibility, with backend authorization always enforced.
- Accessibility and testability requirements on the role-gated and authentication surfaces.

## Out of Scope (Non-Goals)

- Full tenant CRUD and tenant isolation enforcement (deferred to spec `tenant-model-and-isolation`).
- User management CRUD UI (deferred to spec `user-management`).
- An audit dashboard (separate spec).
- SUPPORT_AGENT note-writing capability and any non-financial note authority or note endpoint (deferred to follow-up spec `payment-order-notes`; see Resolved Decision (c)). In this spec SUPPORT_AGENT remains read + audit only.
- Any Playwright or other automated test files (future learning lessons).
- Any change to the existing REST contracts, status codes, headers, or `@PreAuthorize` authority expressions.
- PSP integration, Kafka, webhooks, settlement, or any item in the project-wide active non-goals.
- Adding any new financial-mutation authority or endpoint.

## Resolved Decisions

The items below were previously open questions. Each is now RESOLVED. The original question is retained for traceability, followed by the final decision and rationale. The acceptance criteria, Test User Catalog, and RBAC matrix in this document are consistent with these decisions.

1. **(a) Replace vs supplement raw authorities** — RESOLVED.
   - *Original question:* Should the named Composite_Roles be the only realm roles assigned to Test_Users (so the raw Realm_Authority_Roles are never assigned directly to users), or should both the Composite_Roles and the raw Realm_Authority_Roles remain assignable to users in parallel?
   - *Decision:* SUPPLEMENT + aggregate. The five named Composite_Roles are built ON TOP of the existing ten raw Realm_Authority_Roles using the standard Keycloak composite-role pattern. The raw Realm_Authority_Roles continue to exist as composable building blocks and are not removed or renamed. New deterministic Test_Users are assigned ONLY the single Composite_Role they represent and are never assigned raw Realm_Authority_Roles directly. Existing legacy users are retained unchanged.
   - *Rationale:* This teaches the real Keycloak composite-role model, keeps the backend Role_Converter and all `@PreAuthorize` expressions unchanged (backward compatible), and gives future tests a clean one-role-per-user identity.

2. **(b) tenant_id for existing PLACEHOLDER_MERCHANT_ID users** — RESOLVED.
   - *Original question:* Several existing realm users carry `merchant_id: PLACEHOLDER_MERCHANT_ID`. How should `tenant_id` be provisioned for them: assign a single shared placeholder tenant value (for example `PLACEHOLDER_TENANT_ID`), derive it from the placeholder merchant, or leave it absent?
   - *Decision:* Assign a single shared literal `PLACEHOLDER_TENANT_ID` as the `tenant_id` user attribute for the existing legacy/placeholder users where no deterministic tenant value is otherwise required. The new per-role Test_Users receive meaningful deterministic tenant values per the Test User Catalog (`PLATFORM_TENANT` for platform-scoped roles, `TENANT_ALPHA` for tenant-scoped roles).
   - *Rationale:* Deterministic and predictable, it clearly separates legacy placeholder identities from the new teaching identities while keeping `tenant_id` always present and valid for those users.

3. **(c) SUPPORT_AGENT note-adding capability** — RESOLVED.
   - *Original question:* The SUPPORT_AGENT role is intended to add operational notes without modifying financial fields, but no note authority or note endpoint exists in the current contract, and the metadata PATCH endpoint requires a lifecycle authority. Should a dedicated non-financial note authority/endpoint be planned?
   - *Decision:* DEFER note-write to a dedicated follow-up spec proposed as `payment-order-notes`. In THIS spec SUPPORT_AGENT remains read + audit only (no financial mutation, no lifecycle, no note-write), because no non-financial note authority or endpoint exists in the current contract and adding one would change the REST contracts (out of scope here). The follow-up spec should introduce a dedicated non-financial note authority (for example `platform:payments:note`) and a note endpoint so SUPPORT_AGENT gains write-notes without any financial-field mutation.
   - *Rationale:* Keeps the REST contracts stable in this spec, preserves the read-only-plus-audit shape of SUPPORT_AGENT, and records the gap explicitly so the capability is delivered cleanly later.

## Requirements

### Requirement 1: Named Composite Roles in Keycloak

**User Story:** As a Platform_Operator, I want five named business roles modeled in Keycloak as composite roles over the existing fine-grained permissions, so that user access reflects realistic business responsibilities without changing how the backend authorizes requests.

#### Acceptance Criteria

1. THE Keycloak_Realm SHALL define five Composite_Roles named PLATFORM_ADMIN, TENANT_ADMIN, MERCHANT_MANAGER, SUPPORT_AGENT, and READ_ONLY_USER.
2. THE Keycloak_Realm SHALL retain all ten existing Realm_Authority_Roles that map to the Fine_Grained_Authorities without removing or renaming any of them.
3. WHERE a Composite_Role is defined, THE Keycloak_Realm SHALL compose that Composite_Role from a documented set of the retained Realm_Authority_Roles (supplementing, not replacing, those raw roles) so that a token issued to a holder of the Composite_Role carries exactly the corresponding Fine_Grained_Authorities after conversion.
4. THE Keycloak_Realm SHALL compose PLATFORM_ADMIN from the Realm_Authority_Roles that yield `platform:merchants:create`, `platform:merchants:read`, `platform:merchants:update-status`, `platform:payments:read`, `platform:payments:lifecycle`, and `platform:payments:audit`.
5. THE Keycloak_Realm SHALL compose TENANT_ADMIN from the Realm_Authority_Roles that yield `platform:merchants:create`, `platform:merchants:read`, `platform:merchants:update-status`, and `merchant:payments:read`.
6. THE Keycloak_Realm SHALL compose MERCHANT_MANAGER from the Realm_Authority_Roles that yield `merchant:payments:create`, `merchant:payments:read`, and `merchant:payments:lifecycle`.
7. THE Keycloak_Realm SHALL compose SUPPORT_AGENT from the Realm_Authority_Roles that yield `platform:merchants:read`, `platform:payments:read`, and `platform:payments:audit`, and SHALL NOT include any Realm_Authority_Role that yields a payment create or payment lifecycle authority.
8. THE Keycloak_Realm SHALL compose READ_ONLY_USER from the Realm_Authority_Roles that yield `platform:merchants:read` and `platform:payments:read`, and SHALL NOT include any Realm_Authority_Role that yields a create, update-status, or lifecycle authority.
9. WHEN the Keycloak_Realm import is loaded into a Keycloak instance, THE Keycloak_Realm SHALL import without error and SHALL expose the five Composite_Roles and the ten Realm_Authority_Roles.

### Requirement 2: RBAC Access Matrix Backward Compatibility

**User Story:** As a QA_Automation_Engineer, I want the authority mapping to remain backward compatible, so that existing backend security tests continue to pass and a clear RBAC matrix is available for future authorization tests.

#### Acceptance Criteria

1. THE Role_Converter SHALL continue to convert each realm role name from the `realm_access.roles` claim into the same Fine_Grained_Authority value it produces today, without modification to its conversion rule.
2. WHEN a token carries a Composite_Role, THE Backend_API SHALL authorize requests using only the resulting Fine_Grained_Authorities, applying the existing authorization rules unchanged.
3. THE Backend_API SHALL preserve every existing `@PreAuthorize` expression and every existing `SecurityFilterChain` authorization rule without altering the required Fine_Grained_Authority for any endpoint.
4. WHEN the existing backend security test suite is executed against the updated configuration, THE Backend_API SHALL satisfy every existing security test without modification to those tests.
5. THE requirements document SHALL include an RBAC access matrix mapping each Composite_Role to the Fine_Grained_Authorities it grants and to the consequent allowed operations.

#### RBAC Access Matrix

| Operation (required authority) | PLATFORM_ADMIN | TENANT_ADMIN | MERCHANT_MANAGER | SUPPORT_AGENT | READ_ONLY_USER |
|---|---|---|---|---|---|
| Create merchant (`platform:merchants:create`) | Allowed | Allowed | Denied | Denied | Denied |
| Read merchant(s) (`platform:merchants:read`) | Allowed | Allowed | Denied | Allowed | Allowed |
| Activate/suspend merchant (`platform:merchants:update-status`) | Allowed | Allowed | Denied | Denied | Denied |
| Create payment order (`merchant:payments:create`) | Denied | Denied | Allowed | Denied | Denied |
| Read merchant-scoped payment orders (`merchant:payments:read`) | Denied | Allowed | Allowed | Denied | Denied |
| Read platform-wide payment orders (`platform:payments:read`) | Allowed | Denied | Denied | Allowed | Allowed |
| Payment lifecycle: authorize/capture/cancel/refund/metadata PATCH (`merchant:payments:lifecycle` or `platform:payments:lifecycle`) | Allowed (platform) | Denied | Allowed (merchant) | Denied | Denied |
| Read payment history (any read/lifecycle/audit authority) | Allowed | Allowed | Allowed | Allowed | Allowed |
| Audit data (`platform:payments:audit`) | Allowed | Denied | Denied | Allowed | Denied |

Note: "Allowed" reflects the Fine_Grained_Authority held; merchant-scoped operations remain subject to existing merchant-scope checks in the Backend_API. Tenant-level scoping enforcement is deferred to spec `tenant-model-and-isolation`. SUPPORT_AGENT is read + audit only in this spec; its operational note-writing capability (a dedicated non-financial note authority and endpoint) is deferred to the follow-up spec `payment-order-notes` (see Resolved Decision (c)).

### Requirement 3: Deterministic Test Users Per Role

**User Story:** As a QA_Automation_Engineer, I want one deterministic enabled user per business role in the realm import, so that future Playwright lessons have predictable role identities and reusable storage states.

#### Acceptance Criteria

1. THE Keycloak_Realm SHALL define exactly one enabled Test_User for each of the five Composite_Roles.
2. WHERE a Test_User is defined, THE Keycloak_Realm SHALL set the Test_User account to enabled and SHALL assign a non-temporary password credential.
3. WHERE a Test_User is defined, THE Keycloak_Realm SHALL assign the Test_User the single Composite_Role that the Test_User represents and SHALL NOT assign the Test_User any raw Realm_Authority_Role directly.
4. WHERE a Test_User is defined, THE Keycloak_Realm SHALL set a deterministic, fixed username and a deterministic, fixed password that remain stable across realm imports.
5. WHERE a Test_User represents a merchant-scoped role, THE Keycloak_Realm SHALL set a deterministic `merchant_id` user attribute for that Test_User.
6. THE Keycloak_Realm SHALL retain the existing users (`platform.operator`, `merchant.denied`, `merchant.payment.creator`, `merchant.payment.reader`, `merchant.payment.operator`, `platform.payment.reader`, `merchant.payment.lifecycle`, `platform.payment.admin`, `platform.payment.auditor`) without deleting them, and SHALL set the shared `PLACEHOLDER_TENANT_ID` `tenant_id` attribute on those retained users that carry the `PLACEHOLDER_MERCHANT_ID` placeholder and require no deterministic tenant value (see Requirement 4).
7. THE requirements document SHALL record the deterministic username, role, and scope of each Test_User so that test authors can reference predictable identities.

#### Test User Catalog

| Username | Composite_Role | merchant_id attribute | tenant_id attribute |
|---|---|---|---|
| `platform.admin` | PLATFORM_ADMIN | (none) | `PLATFORM_TENANT` |
| `tenant.admin` | TENANT_ADMIN | (none) | `TENANT_ALPHA` |
| `merchant.manager` | MERCHANT_MANAGER | deterministic merchant id | `TENANT_ALPHA` |
| `support.agent` | SUPPORT_AGENT | (none) | `PLATFORM_TENANT` |
| `readonly.user` | READ_ONLY_USER | (none) | `TENANT_ALPHA` |

Note: per Resolved Decision (b), the `tenant_id` literals shown above (`PLATFORM_TENANT`, `TENANT_ALPHA`) are fixed for the new Test_Users; the deterministic `merchant_id` literal value is finalized in design. Legacy/placeholder users receive the shared `PLACEHOLDER_TENANT_ID` value.

### Requirement 4: Tenant Identifier Claim

**User Story:** As a Platform_Operator, I want a tenant identifier carried in the token alongside the merchant identifier, so that role scoping is meaningful and future tenant isolation can build on a real claim.

#### Acceptance Criteria

1. THE Keycloak_Realm SHALL add a protocol mapper to the `payment-quality-dashboard` client that maps the user attribute `tenant_id` to a `tenant_id` JWT claim in the access token, the ID token, and the userinfo response.
2. THE Keycloak_Realm SHALL retain the existing `merchant-id-mapper` protocol mapper that maps `merchant_id` without modification to its behavior.
3. WHERE a user has a `tenant_id` user attribute, THE Keycloak_Realm SHALL include the `tenant_id` claim with that attribute value in the issued token.
4. IF a user does not have a `tenant_id` user attribute, THEN THE Keycloak_Realm SHALL issue the token without a `tenant_id` claim and SHALL still issue a valid token.
5. WHERE an existing legacy user carries the `merchant_id` attribute value `PLACEHOLDER_MERCHANT_ID` and no deterministic tenant value is otherwise required, THE Keycloak_Realm SHALL set that user's `tenant_id` user attribute to the shared literal `PLACEHOLDER_TENANT_ID`.
6. WHERE a new per-role Test_User is defined, THE Keycloak_Realm SHALL set that Test_User's `tenant_id` user attribute to the deterministic value recorded in the Test User Catalog (`PLATFORM_TENANT` for a platform-scoped role, `TENANT_ALPHA` for a tenant-scoped role).
7. THE Backend_API SHALL continue to start and authorize requests when a token includes the `tenant_id` claim, treating the claim as informational and applying no new tenant-scoped authorization rule in this spec.

### Requirement 5: Keycloak OIDC Login Flow

**User Story:** As an Authenticated_User, I want to log in through the real Keycloak OIDC flow, so that I access the Dashboard with a server-held session and my role identity.

#### Acceptance Criteria

1. WHEN an unauthenticated visitor activates the login control on the Login_Page, THE Server_Proxy SHALL initiate the OIDC_Login_Flow against the Keycloak_Realm using the Authorization Code flow with PKCE.
2. WHEN the OIDC_Login_Flow completes successfully, THE Server_Proxy SHALL create an Auth_Session that stores the issued tokens server-side and SHALL redirect the visitor to an authenticated Dashboard route.
3. THE Server_Proxy SHALL store the bearer token only in the server-side Auth_Session and SHALL NOT expose the bearer token to the browser DOM, client-side state, browser storage, or logs.
4. WHEN the Dashboard sends a request to the Backend_API, THE Server_Proxy SHALL attach the bearer token server-side and SHALL NOT require the browser to hold or send the token.
5. WHEN the Auth_Session is established, THE Dashboard SHALL make the Authenticated_User identity and Composite_Roles available to the application for Role_Aware_Navigation without exposing the bearer token.
6. IF the OIDC_Login_Flow fails or is cancelled at the Keycloak_Realm, THEN THE Dashboard SHALL return the visitor to the Login_Page and SHALL display an authentication-failed message that excludes any token value.

### Requirement 6: Logout

**User Story:** As an Authenticated_User, I want to log out, so that my session is cleared and I can no longer access protected routes.

#### Acceptance Criteria

1. WHEN an Authenticated_User activates the logout control, THE Server_Proxy SHALL clear the Auth_Session.
2. WHEN the Auth_Session is cleared, THE Dashboard SHALL navigate the user to the Login_Page.
3. WHEN the Auth_Session is cleared, THE Dashboard SHALL treat subsequent requests to protected routes as unauthenticated and SHALL apply the Auth_Required_Redirect.

### Requirement 7: Authentication-Required Handling (401)

**User Story:** As a Read_Only_Viewer, I want to be redirected to log in when I have no session, so that I understand I must authenticate before using the Dashboard.

#### Acceptance Criteria

1. WHEN an unauthenticated visitor requests a protected Dashboard route, THE Dashboard SHALL apply the Auth_Required_Redirect to the Login_Page.
2. IF the Auth_Session is missing or expired when the Server_Proxy calls the Backend_API, THEN THE Server_Proxy SHALL surface an unauthenticated outcome and THE Dashboard SHALL apply the Auth_Required_Redirect to the Login_Page.
3. WHEN the Backend_API responds with status 401 to a Server_Proxy request, THE Dashboard SHALL present the result as an authentication-required state distinct from a forbidden state.
4. WHEN the Auth_Required_Redirect sends a visitor to the Login_Page, THE Dashboard SHALL preserve the originally requested route so that the visitor returns to it after a successful login.
5. THE Login_Page SHALL expose a Test_Id on the login control and a Test_Id on the authentication-required surface.

### Requirement 8: Forbidden Handling (403) and 401-vs-403 Distinction

**User Story:** As a QA_Automation_Engineer, I want the UI to clearly distinguish "not authenticated" (401) from "authenticated but forbidden" (403), so that authorization-bypass and access-control tests can assert the correct outcome.

#### Acceptance Criteria

1. WHEN an Authenticated_User requests a route or action for which the user lacks the required Fine_Grained_Authority and the Backend_API responds with status 403, THE Dashboard SHALL display the Forbidden_Page.
2. THE Forbidden_Page SHALL state that the user is authenticated but not authorized for the requested action and SHALL NOT redirect to the Login_Page.
3. THE Dashboard SHALL render the authentication-required surface (401) and the Forbidden_Page (403) as visually and textually distinct surfaces, each conveying its meaning through text and not through color alone.
4. THE Forbidden_Page SHALL expose a stable Test_Id on its root container, and the authentication-required surface SHALL expose a different stable Test_Id on its root container.
5. THE Forbidden_Page SHALL provide a navigation control that returns the Authenticated_User to an authorized route, such as the Overview page.
6. WHILE displaying the Forbidden_Page, THE Dashboard SHALL exclude any bearer token value and SHALL render any displayed `Authorization` header as Masked_Authorization.

### Requirement 9: Role-Aware Navigation and Action Visibility

**User Story:** As a Merchant_User, I want the sidebar and actions to reflect what my role can do, so that I am not shown controls I cannot use, while the backend still enforces the real authorization.

#### Acceptance Criteria

1. WHEN the Dashboard renders Role_Aware_Navigation for an Authenticated_User, THE Dashboard SHALL display only the sidebar links whose destinations the user's Composite_Roles can access.
2. WHEN the Dashboard renders the Overview page for an Authenticated_User, THE Dashboard SHALL render only the Overview content sections that the user's Composite_Roles can access.
3. WHERE an Authenticated_User lacks the Fine_Grained_Authority required for an action, THE Dashboard SHALL hide the action control or render the action control in a disabled state with an accessible explanation.
4. THE Dashboard SHALL treat frontend hiding or disabling of controls as a convenience only and SHALL rely on the Backend_API as the authoritative enforcement point for every protected operation.
5. WHEN an Authenticated_User invokes a protected operation regardless of frontend control state, THE Backend_API SHALL enforce the required Fine_Grained_Authority and SHALL return 403 when the authority is absent.
6. THE Dashboard SHALL expose a stable Test_Id on each role-gated sidebar link and on each role-gated action control so that future tests can assert visibility per role.
7. WHEN the Composite_Roles of the Authenticated_User change between sessions, THE Dashboard SHALL render Role_Aware_Navigation according to the roles present in the current Auth_Session.

### Requirement 10: Accessibility and Testability of Role-Gated and Auth Surfaces

**User Story:** As a QA_Automation_Engineer, I want the role-gated controls and the login/forbidden surfaces to be accessible and locator-friendly, so that future Playwright lessons use stable, accessibility-first locators.

#### Acceptance Criteria

1. THE Dashboard SHALL assign a stable, unique Test_Id to each role-gated control, to the Login_Page login control, to the Forbidden_Page root, and to the authentication-required surface root, with each Test_Id resolving to exactly one element per rendered page.
2. THE Dashboard SHALL render the Login_Page, the Forbidden_Page, and the authentication-required surface with a single semantic top-level heading per surface.
3. THE Dashboard SHALL convey role labels, authorization status, and authentication status through text content so that the meaning is distinguishable without relying on color.
4. WHEN the Login_Page is displayed, THE Dashboard SHALL move keyboard focus to the primary login control so that keyboard and assistive-technology users reach it first.
5. WHERE an action control is rendered in a disabled state because the user lacks authority, THE Dashboard SHALL provide an accessible name or description that explains why the control is disabled.
6. THE Dashboard SHALL preserve visible keyboard focus indicators on the login control, the logout control, and the Forbidden_Page navigation control.

### Requirement 11: Token Confidentiality in Learning Panels

**User Story:** As a Support_User, I want the HTTP learning panels to never reveal the bearer token, so that the application demonstrates secure handling while still teaching the HTTP exchange.

#### Acceptance Criteria

1. WHERE any Dashboard panel displays request headers, THE Dashboard SHALL render the `Authorization` header value as Masked_Authorization.
2. THE Dashboard SHALL exclude the raw bearer token from the DOM, HTML attributes, client-side state, browser storage, and logs at all times.
3. WHEN the Server_Proxy forwards a response to the browser, THE Server_Proxy SHALL NOT include the bearer token in any forwarded header or body.
4. IF a learning panel renders a captured request that included an `Authorization` header, THEN THE Dashboard SHALL display only the Masked_Authorization placeholder and SHALL NOT display any portion of the real token value.
