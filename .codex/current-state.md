# Current State for Codex CLI

## Branch

`018-rest-security-p1-error-auth-method-hardening`

## Goal

Continue `tenant-model-and-isolation` after Wave 1, using `.kiro` specs as read-only design input and `.codex` files as the mutable execution layer.

## Important Rule

Do not modify files under `.kiro/specs/tenant-model-and-isolation/` during implementation. If execution status changes, update this file or another `.codex/*.md` file instead.

## Verified / Reported Completion

### Wave 0 — Tenant module foundation

Current code contains the tenant module foundation:

- `apps/backend/src/main/java/lab/paymentquality/tenant/package-info.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantReference.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantContext.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantResolutionException.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantResolver.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/domain/Tenant.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/domain/TenantStatus.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/domain/TenantType.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/infrastructure/JpaTenantRepository.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/application/TenantResolverService.java`
- `apps/backend/src/main/resources/db/migration/tenant/V0.1__create_tenants.sql`

Flyway locations are already extended in `apps/backend/src/main/resources/application.yml` to include:

```text
classpath:db/migration/tenant,classpath:db/migration/merchant,classpath:db/migration/payment
```

### Wave 1 — Merchant schema and test support

Reported complete and visible in current code:

- `apps/backend/src/main/resources/db/migration/merchant/V1.1__add_tenant_to_merchants.sql`
  - Adds `merchants.tenant_id` as nullable.
  - Backfills with seeded `PLACEHOLDER_TENANT_ID` tenant UUID.
  - Enforces `NOT NULL`.
  - Adds FK to `tenants(tenant_id)`.
  - Adds `idx_merchants_tenant_id`.
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/Merchant.java`
  - Adds `tenantId` UUID column field.
  - Adds four-argument `Merchant.create(..., tenantId)` factory.
  - Keeps three-argument bridge overload.
  - Adds `getTenantId()`.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java`
  - Adds `tokenWithRolesAndTenantId(...)`.
  - Adds `tokenWithRolesTenantIdAndMerchantId(...)`.
  - Adds `platformAdminToken()`.
  - Adds `tenantAdminToken()`.
  - Adds `tokenWithoutTenantClaim()`.

Previous run summary from user: `./mvnw test` passed and build was GREEN after Wave 1.

### Wave 2 — Merchant service tenant-aware application behavior

Completed on 2026-06-17.

Changed files:

- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantResolver.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/application/TenantResolverService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/infrastructure/JpaMerchantRepository.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/CreateMerchantRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/MissingTenantReferenceException.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/TenantBoundaryViolationException.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/UnresolvableTenantReferenceException.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantService.java`
- `apps/backend/src/test/java/lab/paymentquality/merchant/MerchantModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/merchant/internal/application/MerchantServiceTest.java`

Implementation notes:

- `JpaMerchantRepository` now exposes tenant-filtered lookup/list methods.
- `CreateMerchantRequest` accepts optional `tenantReference` while preserving the existing two-argument constructor for pre-Wave-3 callers and tests.
- `MerchantService` now has `TenantContext`-aware create, find, list, activate, and suspend overloads.
- Tenant-scoped merchant operations are constrained to the caller tenant.
- Platform-scoped merchant creation requires a body `tenantReference`.
- Unknown platform body tenant references are translated into a merchant-layer `UnresolvableTenantReferenceException`.
- The public tenant API was extended with `resolveTenantId(TenantReference)`; merchant code still imports only `lab.paymentquality.tenant.*` and does not import `lab.paymentquality.tenant.internal.*`.
- The pre-Wave-3 controller-compatible service overloads remain temporarily so Wave 3 can wire controller JWT resolution without breaking existing REST contracts.
- `MerchantModuleTest` now runs with direct dependencies because the merchant module depends on the tenant module public API.

Test evidence:

- `cd apps/backend && ./mvnw -Dtest=MerchantServiceTest test`
  - Result: GREEN.
- `cd apps/backend && ./mvnw -Dtest='<all *Test classes except apps/backend/src/test/java/lab/paymentquality/restkit/** and apps/backend/src/test/java/lab/paymentquality/paymentsupport/**>' test`
  - Result: GREEN.
  - Maven summary: `Tests run: 258, Failures: 0, Errors: 0, Skipped: 5`.

Important test-scope note:

- Per the repository instruction in `AGENTS.md`, backend tests under `apps/backend/src/test/java/lab/paymentquality/restkit/` and `apps/backend/src/test/java/lab/paymentquality/paymentsupport/` are not run unless the user explicitly asks for those suites.
- An earlier unfiltered `./mvnw test` exposed a failure in `restkit/contract/create/PaymentOrderReadContractRestKitTest`, which was already locally modified before Wave 2 work and is outside the requested filtered validation scope.

### Wave 3 — Controller and exception-handler tenant wiring

Completed on 2026-06-17.

Changed files:

- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantExceptionHandler.java`
- `apps/backend/src/test/java/lab/paymentquality/merchant/internal/web/MerchantControllerTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/MerchantSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java`

Implementation notes:

- `MerchantController` now injects the public `TenantResolver`.
- Protected merchant endpoints accept `@AuthenticationPrincipal Jwt jwt`.
- Each protected merchant endpoint resolves `TenantContext` once per request after the existing `@PreAuthorize` check.
- Controller delegation now uses the tenant-aware `MerchantService` overloads from Wave 2.
- `GET /api/merchants` accepts optional `?tenantId=` for platform-scoped callers.
- Tenant-scoped list callers ignore `?tenantId=` and remain constrained to their own tenant.
- Platform-scoped callers with an unresolvable non-blank `tenantId` filter receive an empty merchant list with status `200`.
- `MerchantExceptionHandler` maps tenant resolution and tenant-boundary failures to generic `403` responses with `detail = "Access denied"` and no tenant identifiers.
- `MerchantExceptionHandler` maps missing and unresolvable create body tenant references to `400` validation responses.
- Existing positive merchant security fixtures now include a seeded tenant claim so they pass the new tenant resolution layer before asserting authority separation.
- `TestJwtSupport.platformOperatorToken()` now carries the seeded `PLACEHOLDER_TENANT_ID` tenant claim for existing merchant setup helpers. Use `platformAdminToken()` when a test needs a true `PLATFORM_TENANT` principal.

Test evidence:

- `cd apps/backend && ./mvnw -Dtest=MerchantControllerTest test`
  - Result: GREEN.
- `cd apps/backend && ./mvnw -Dtest=MerchantSecurityTest test`
  - Result: GREEN.
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderRestAssuredTest test`
  - Result: GREEN.
- `cd apps/backend && ./mvnw -Dtest='<all *Test classes except apps/backend/src/test/java/lab/paymentquality/restkit/** and apps/backend/src/test/java/lab/paymentquality/paymentsupport/**>' test`
  - Result: GREEN.
  - Maven summary: `Tests run: 258, Failures: 0, Errors: 0, Skipped: 5`.

Boundary checks:

- `rg "lab\.paymentquality\.tenant\.internal" apps/backend/src/main/java/lab/paymentquality/merchant apps/backend/src/main/java/lab/paymentquality/payment -n`
  - Result: no matches.
- No `.kiro/**` files were modified.
- No payment module source files were modified.

Important test-scope note:

- Per the repository instruction in `AGENTS.md`, backend tests under `apps/backend/src/test/java/lab/paymentquality/restkit/` and `apps/backend/src/test/java/lab/paymentquality/paymentsupport/` are not run unless the user explicitly asks for those suites.
- The existing locally modified `restkit/` files remain outside the Wave 3 validation scope.

### Wave 4 — Focused backend tests for tenant isolation

Completed on 2026-06-17.

Changed files:

- `apps/backend/pom.xml`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantExceptionHandler.java`
- `apps/backend/src/test/java/lab/paymentquality/tenant/internal/application/TenantResolverServiceTest.java`
- `apps/backend/src/test/java/lab/paymentquality/merchant/internal/application/MerchantServiceTenantTest.java`
- `apps/backend/src/test/java/lab/paymentquality/merchant/internal/infrastructure/JpaMerchantRepositoryTenantTest.java`
- `apps/backend/src/test/java/lab/paymentquality/merchant/internal/web/MerchantControllerTenantSecurityTest.java`

Implementation notes:

- Added `spring-boot-webmvc-test` as a test-scoped dependency so Spring Boot 4 `@WebMvcTest` is available.
- `TenantResolverServiceTest` covers active/suspended platform and standard tenant claim resolution, missing/blank claims, and unknown tenant references without a Spring context.
- `MerchantServiceTenantTest` covers tenant-scoped create/read/write boundaries and platform-scoped tenant-reference assignment errors without a Spring context.
- `JpaMerchantRepositoryTenantTest` covers tenant-filtered repository lookups, cross-tenant exclusion, and `created_at desc, merchant_id asc` ordering with Flyway-managed PostgreSQL schema.
- `MerchantControllerTenantSecurityTest` covers JWT tenant resolution, tenant-aware service delegation, tenant validation mapping, tenant boundary mapping, generic 403 non-disclosure, and `application/problem+json` for tenant-related 4xx responses.
- Tiny production fix: tenant-related `MerchantExceptionHandler` 400/403 responses now set `Content-Type: application/problem+json` while preserving the existing `ErrorResponse` body shape.
- No `.kiro/**` files were modified.
- No payment module source files were modified.

Test evidence:

- `cd apps/backend && ./mvnw -Dtest=TenantResolverServiceTest,MerchantServiceTenantTest,JpaMerchantRepositoryTenantTest,MerchantControllerTenantSecurityTest test`
  - Result: GREEN.
  - Maven summary: `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`.
- `cd apps/backend && ./mvnw -Dtest='<all *Test classes except apps/backend/src/test/java/lab/paymentquality/restkit/** and apps/backend/src/test/java/lab/paymentquality/paymentsupport/**>' test`
  - Result: GREEN.
  - Maven summary: `Tests run: 280, Failures: 0, Errors: 0, Skipped: 5`.

Important test-scope note:

- The repository rule continues to exclude backend tests under `apps/backend/src/test/java/lab/paymentquality/restkit/**` and `apps/backend/src/test/java/lab/paymentquality/paymentsupport/**` unless explicitly requested.
- The existing locally modified `restkit/` files remain outside the Wave 4 validation scope.

### Wave 5 — Module and REST Assured integration coverage

Completed on 2026-06-17.

Changed files:

- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantExceptionHandler.java`
- `apps/backend/src/test/java/lab/paymentquality/tenant/TenantModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/TenantIsolationIT.java`

Implementation notes:

- Added `TenantModuleTest` using Spring Modulith standalone module test style with the existing PostgreSQL Testcontainers support.
- `TenantModuleTest` verifies the tenant module boots in isolation, wires the public `TenantResolver`, `TenantResolverService`, `JpaTenantRepository`, and `Tenant` JPA entity, and runs `ApplicationModules.of(PaymentQualityApplication.class).verify()`.
- Existing `MerchantModuleTest` was verified green with its Wave 2 direct-dependencies bootstrap mode; no Wave 5 change was needed.
- Added `TenantIsolationIT` REST Assured integration coverage for own-tenant read, cross-tenant masked read, cross-tenant forbidden write, platform create validation, platform tenant assignment, tenant-scoped create ignoring body tenant reference, missing tenant claim, suspended tenant, and platform list with unknown tenant filter.
- `TenantIsolationIT` asserts `application/problem+json` for 4xx responses and `X-Correlation-ID` on covered responses.
- Tiny production fix: `MerchantNotFoundException` responses now set `Content-Type: application/problem+json` so masked tenant-boundary 404 responses satisfy the Wave 5 problem-response contract while preserving the existing status and `ErrorResponse` body shape.
- No `.kiro/**` files were modified.
- No payment module source files were modified.

Test evidence:

- `cd apps/backend && ./mvnw -Dtest=TenantModuleTest,MerchantModuleTest test`
  - Result: GREEN.
  - Maven summary: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`.
- `cd apps/backend && ./mvnw -Dit.test=TenantIsolationIT failsafe:integration-test failsafe:verify`
  - Result: GREEN.
  - Maven summary: `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`.
- `cd apps/backend && ./mvnw -Dtest='<all *Test classes except apps/backend/src/test/java/lab/paymentquality/restkit/** and apps/backend/src/test/java/lab/paymentquality/paymentsupport/**>' -Dit.test='*IT' verify`
  - Result: GREEN.
  - Surefire summary: `Tests run: 282, Failures: 0, Errors: 0, Skipped: 5`.
  - Failsafe summary: `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`.

Boundary checks:

- `git diff --check`
  - Result: no whitespace errors.
- `rg "lab\.paymentquality\.tenant\.internal" apps/backend/src/main/java/lab/paymentquality/merchant apps/backend/src/main/java/lab/paymentquality/payment -n`
  - Result: no matches.
- `git diff --name-only -- .kiro apps/backend/src/main/java/lab/paymentquality/payment`
  - Result: no matches.

Important test-scope note:

- The Wave 5 validation used the repository-approved filtered `verify` command because backend tests under `apps/backend/src/test/java/lab/paymentquality/restkit/**` and `apps/backend/src/test/java/lab/paymentquality/paymentsupport/**` remain excluded unless explicitly requested.

### Wave 6 — Optional property-based tenant isolation tests

Completed on 2026-06-17.

Changed files:

- `apps/backend/src/test/java/lab/paymentquality/tenant/TenantIsolationPropertyTest.java`

Property-based framework:

- Used existing jqwik 1.9.2 test dependency from `apps/backend/pom.xml`.
- No new dependency was added.

Implemented properties:

- P1 cross-tenant read masking at merchant service level: tenant-scoped reads use tenant-filtered lookup and throw `MerchantNotFoundException` for misses.
- P2 classification determinism: `TenantContext.isPlatformScoped()` is true exactly for generated platform tenant types, independent of generated authority names.
- P3 create-merchant tenant assignment: tenant-scoped create always assigns the caller tenant; platform-scoped create assigns resolved valid references; missing and invalid references throw the expected merchant-layer exceptions.
- P4 transitive isolation at service boundary: tenant-scoped merchant writes against another tenant throw `TenantBoundaryViolationException`.
- P5 suspended tenant semantics at resolver level: suspended standard tenants are rejected and suspended platform tenants resolve.
- P6 tenant-reference natural-key resolution: seeded references resolve deterministically and unknown/non-exact variants throw.

Skipped or narrowed properties:

- Full REST-level P1/P4 and DB-level P5 jqwik properties were not added because the project has jqwik but no Spring/jqwik integration dependency such as `jqwik-spring`.
- Wave 6 therefore keeps property tests at deterministic unit/service level and relies on Wave 5 `TenantIsolationIT` for REST and database coverage.
- Payment module source files were not modified.
- No `.kiro/**` files were modified.

Test evidence:

- `cd apps/backend && ./mvnw -Dtest=TenantIsolationPropertyTest test`
  - Result: GREEN.
  - Maven summary: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`.
- `cd apps/backend && ./mvnw -Dtest='<all *Test classes except apps/backend/src/test/java/lab/paymentquality/restkit/** and apps/backend/src/test/java/lab/paymentquality/paymentsupport/**>' test`
  - Result: GREEN.
  - Maven summary: `Tests run: 292, Failures: 0, Errors: 0, Skipped: 5`.
- `cd apps/backend && ./mvnw -Dtest='<all *Test classes except apps/backend/src/test/java/lab/paymentquality/restkit/** and apps/backend/src/test/java/lab/paymentquality/paymentsupport/**>' -Dit.test='*IT' verify`
  - Result: GREEN.
  - Surefire summary: `Tests run: 292, Failures: 0, Errors: 0, Skipped: 5`.
  - Failsafe summary: `Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`.

Boundary checks:

- `git diff --check`
  - Result: no whitespace errors.
- `rg "lab\.paymentquality\.tenant\.internal" apps/backend/src/main/java/lab/paymentquality/merchant apps/backend/src/main/java/lab/paymentquality/payment -n`
  - Result: no matches.
- `git diff --name-only -- .kiro apps/backend/src/main/java/lab/paymentquality/payment`
  - Result: no matches.

Important test-scope note:

- The Wave 6 validation used the repository-approved filtered `test` and `verify` commands because backend tests under `apps/backend/src/test/java/lab/paymentquality/restkit/**` and `apps/backend/src/test/java/lab/paymentquality/paymentsupport/**` remain excluded unless explicitly requested.

## Current Active Work

Wave 6 optional property-based tests are complete. Stop before any further tenant isolation work unless the user explicitly asks to continue.

Next recommended wave:

- Review/hardening pass for tenant isolation, or explicitly approve adding Spring/jqwik integration if full REST-level property tests are desired.

## Resolved Spec Gap

The Kiro design says `MerchantService` should use `TenantResolver` to resolve a platform-provided `tenantReference` string. Wave 2 resolved this by extending the public `TenantResolver` interface while preserving the existing JWT resolver method:

```java
TenantContext resolve(Jwt jwt);
UUID resolveTenantId(TenantReference tenantReference);
```

`TenantResolverService` implements the new method using tenant internals. Merchant code imports only public tenant API from `lab.paymentquality.tenant.*`.

## Verification Command After Wave 6

Run backend verification from `apps/backend` while respecting the repository rule that excludes:

- `apps/backend/src/test/java/lab/paymentquality/restkit/**`
- `apps/backend/src/test/java/lab/paymentquality/paymentsupport/**`

Do not proceed beyond Wave 6 unless explicitly requested.

### Wave 7B — Corrected 6.7 Keycloak IT (user-management spec)

Completed on 2026-06-18.

Status: BLOCKED_BY_LOCAL_DOCKER_OR_TESTCONTAINERS_RUNTIME.

**Summary:**
- Removed incorrect WireMock-based `KeycloakAdminClientIT.java` that was pretending to satisfy task 6.7.
- Created proper `KeycloakContainerSupport` helper using GenericContainer with real Keycloak 26.6.1 image.
- Created `UserManagementKeycloakAdminIT.java` with real Keycloak integration tests.
- Container fails to start in local environment (connection refused during health check).
- Backend tests remain GREEN (339 tests, 0 failures, 5 skipped) when excluding the blocked IT.

**Backend status:** GREEN (excluding blocked Keycloak IT).

**Wave 8 allowed:** NO — task 6.7 is NON-optional and remains blocked by local Docker/Testcontainers runtime issues.

**Next action:** Investigate Keycloak container startup issues or defer 6.7 to CI/CD environment with explicit user approval.

### Wave 7B — Realm duplicate fix and Podman Keycloak IT retry (user-management spec)

Completed on 2026-06-18.

Status: BLOCKED_BY_REALM_IMPORT_OR_SERVICE_ACCOUNT_CONFIGURATION.

**Summary:**
- Fixed duplicate `platform.operator` user in realm JSON by merging attributes/roles
- Updated KeycloakContainerSupport to use `KC_BOOTSTRAP_ADMIN_USERNAME`/`KC_BOOTSTRAP_ADMIN_PASSWORD`
- Keycloak container now starts successfully via Testcontainers
- Realm imports successfully
- Service account authentication works
- **Blocker:** User attributes (tenant_id) are not being persisted or returned by Keycloak Admin API
- Integration tests fail because tenantId is null in responses

**Backend status:** GREEN (339 tests, 0 failures, 5 skipped) when excluding blocked IT.

**Wave 8 allowed:** NO — task 6.7 is NON-optional and remains blocked by user attribute persistence issue.

**Next action:** Debug Keycloak Admin API attribute handling or defer 6.7 with explicit user approval.

## Wave 7B — Task 6.7 real Keycloak IT resolved

Completed on 2026-06-18.

- Final 6.7 status: `COMPLETED`.
- Root cause: the Testcontainers helper inserted custom user-profile attributes into Keycloak's `groups` JSON array, so the profile PUT failed with HTTP 400 and Keycloak dropped unmanaged `tenant_id` values.
- Fix: structured Jackson editing now adds `tenant_id` and `merchant_id` to the root user-profile `attributes` array and fails fast on configuration errors.
- Real `UserManagementKeycloakAdminIT`: GREEN, 3 tests, including raw attribute persistence, facade mapping, enabled/disabled behavior, direct role assignment/removal checks, and tenant-scoped creation.
- Filtered backend test suite: GREEN, 339 tests, 0 failures, 0 errors, 5 skipped.
- Filtered Maven verify: GREEN; 339 Surefire tests plus 16 Failsafe integration tests, all passing (5 Surefire skips).
- WireMock was not used for task 6.7.
- `.kiro/**` was not modified.
- Wave 8 go/no-go: GO when explicitly requested. Wave 8 was not started.

## Wave 8 — User-management frontend foundation

Completed on 2026-06-18.

- Wave 8 status: `COMPLETED`.
- Added Zod request/response contracts in `app/schemas/user.schema.ts`.
- Added the header-aware, response-validating `useUsersApi` composable.
- Added five `/server/api/users/**` routes using the existing server-side bearer-token proxy.
- Added `canManageUsers` and `canAssignRoles` for `PLATFORM_ADMIN` and `TENANT_ADMIN` only.
- Frontend typecheck: GREEN.
- Frontend unit tests: GREEN, 34 files and 442 tests passed.
- No backend production files, `.kiro/**`, Playwright files, user-management UI components, page, or navigation were changed.
- Wave 9 go/no-go: GO when explicitly requested.
- Next recommended wave: Wave 9 — Frontend UI. Do not start automatically.

## Wave 9 — User-management frontend UI

Completed on 2026-06-18.

- Wave 9 status: `COMPLETED`.
- Added the CSR-only `/admin/users` page with URL-backed role/status/search filters and pagination.
- Added `UserTable`, `CreateUserForm`, `EditUserDrawer`, and `RoleAssignmentSelect` using existing Nuxt UI dashboard patterns.
- Added role-gated Users navigation and dashboard search entries for `PLATFORM_ADMIN` and `TENANT_ADMIN` only.
- Implemented loading, empty, filtered-empty, error, forbidden, success, and conflict states.
- Temporary passwords remain local to the create request flow and are cleared on submission/close; no token, secret, credential, raw Authorization header, or raw Keycloak representation is browser-visible.
- Frontend typecheck: GREEN.
- Frontend unit tests: GREEN, 34 files and 442 tests passed.
- No backend, Keycloak realm, `.kiro/**`, frontend test, or Playwright file was changed.
- Next recommended wave: Wave 10 — Frontend tests. Do not start automatically.
- Wave 10 go/no-go: GO when explicitly requested.

## Wave 10 — User-management frontend tests

Completed on 2026-06-18.

- Wave 10 status: `COMPLETED`.
- Added fast-check Property 5 proving the user-management capability biconditional for all five composite roles with 100 runs.
- Added component tests for loading, empty, filtered-empty, error/problem details, forbidden/no API call, success toast, and conflict states.
- Added security assertions that rendered UI contains no token, secret, temporary-password request field, credential, or raw Keycloak representation markers.
- Assertions use visible text, accessible action names, and focused state identifiers instead of whole-DOM snapshots or generated CSS classes.
- Tests revealed and drove minimal fixes for directory-prefixed component resolution and invalid empty-string `USelect` options.
- Frontend typecheck: GREEN.
- Frontend unit tests: GREEN, 38 files and 468 tests passed.
- Wave 10 changed no backend, Keycloak realm, `.kiro/**`, or Playwright file; Playwright was not run. An unrelated concurrent `PaymentErrorSpecs.java` worktree change was observed and left untouched.
- Next recommended wave: Wave 11 — Final checkpoint. Do not start automatically.
- Wave 11 go/no-go: GO when explicitly requested.

## Wave 11 — User-management final checkpoint

Completed on 2026-06-18.

- Wave 11 status: `COMPLETE_WITH_OPTIONAL_GAPS`.
- `user-management` spec (Spec #3) can be **closed**.

### Backend validation

- Environment: Podman 5.8.2, `DOCKER_HOST=unix:///run/user/1000/podman/podman.sock`, `TESTCONTAINERS_RYUK_DISABLED=true`.
- `cd apps/backend && ./mvnw -q -Dsurefire.excludes='**/restkit/**/*.java,**/paymentsupport/**/*.java' clean test`: GREEN.
  - Surefire: `Tests run: 321, Failures: 0, Errors: 0, Skipped: 5`.
- `cd apps/backend && ./mvnw -q -Dsurefire.excludes='**/restkit/**/*.java,**/paymentsupport/**/*.java' verify`: GREEN.
  - Failsafe: `Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`.
  - `UserManagementKeycloakAdminIT`: 3/3 GREEN (real Keycloak 26.6.1 via Testcontainers/Podman).
  - `TenantIsolationIT`: 9/9 GREEN. `MerchantPersistenceIT`: 4/4 GREEN.
- `IamModuleTest`: 4/4 GREEN. `ModulithArchitectureTest`: 1/1 GREEN. `MerchantModuleTest`: 2/2 GREEN. `TenantModuleTest`: 2/2 GREEN.
- IAM unit tests: `UserManagementServiceTest` 20, `UserManagementControllerSecurityTest` 17, `UserMapperTest` 6 — all GREEN.
- Compilation workaround: untracked broken `restkit/` file (`PaymentOrderSecurityContractRestKitTest.java`, concurrent work, excluded suite) blocked `test-compile`; temporarily moved to `/tmp/opencode/` during validation, restored exactly afterward. No tracked or `.kiro/**` files modified.

### Frontend validation

- `cd apps/frontend && corepack pnpm typecheck`: GREEN.
- `cd apps/frontend && corepack pnpm test:unit`: GREEN — 38 test files, 468 tests passed.
- Playwright was not run. No Playwright files were created.

### Spec compliance summary

- All required Waves 0–10 tasks: `DONE_VERIFIED`.
- All NON-optional tests (6.6 `IamModuleTest`, 6.7 real Keycloak IT): GREEN.
- Optional tasks skipped (6.1, 6.8, 6.9–6.13): `OPTIONAL_SKIPPED_ACCEPTABLE`, explicitly documented.
- No local user DB, JPA entity, repository, or Flyway migration: confirmed.
- No `keycloak-admin-client` dependency: confirmed.
- Thin Spring `RestClient` wrapper: confirmed.
- Admin token never browser-exposed: confirmed.
- Realm changes additive, no duplicate usernames: confirmed.
- `canManageUsers`/`canAssignRoles` granted only to `PLATFORM_ADMIN` and `TENANT_ADMIN`: confirmed.
- Seven UI states implemented and tested: confirmed.
- No `.kiro/**` modifications: confirmed.
- No token/secret/temporary-password exposure: confirmed.

### Can user-management be closed?

Yes — `COMPLETE_WITH_OPTIONAL_GAPS`.

### Can the next spec start?

Yes, when explicitly requested. The next spec on the SDET roadmap is `audit-log-dashboard` (Spec #4). It was not started during Wave 11.

### Next recommended spec/wave

`audit-log-dashboard` (Spec #4) — Wave 0 prerequisite gate, when explicitly requested by the user.

## Audit Log Dashboard — Wave 0 Prerequisite Gate

Verified on 2026-06-18.

- Wave 0 status: `READY_FOR_WAVE_1`.
- Spec #1 `iam-roles-and-keycloak-login`: prerequisite satisfied. Five composite roles, the shared `Authorities` catalog, converter allowlist, `tenant_id` claim mapping, and backend/frontend RBAC foundations are present.
- Spec #2 `tenant-model-and-isolation`: prerequisite satisfied. The tenant module and public `TenantResolver`, `TenantContext`, and `TenantReference` APIs exist; masked-404 reads and generic-403 writes are implemented and covered by tenant isolation tests.
- Spec #3 `user-management`: `COMPLETE_WITH_OPTIONAL_GAPS`; Task 6.3 IAM/user-management emitters are in scope for the later emitter wave.
- Event baseline: no `ApplicationEventPublisher` or `@ApplicationModuleListener` usage exists in backend production or test sources. This spec will introduce the first Spring Modulith event usage.
- No backend code, frontend code, dependencies, tests, Playwright files, or `.kiro/**` files were changed. No tests were run for this verify-only gate.
- Execution evidence, commands, risks, and deferred work are recorded in `.codex/audit-log-dashboard.md`.
- Wave 1 may start only when explicitly requested. It was not started during Wave 0.

## Audit Log Dashboard — Wave 1 Cross-Spec Authority Extensions

Completed on 2026-06-18.

- Wave 1 status: `COMPLETED`.
- Added realm roles `platform:audit:read` and `tenant:audit:read` additively.
- Granted platform audit read to `PLATFORM_ADMIN` and `SUPPORT_AGENT`; granted tenant audit read to `TENANT_ADMIN`; did not grant either authority to `MERCHANT_MANAGER` or `READ_ONLY_USER`.
- Added `PLATFORM_AUDIT_READ` and `TENANT_AUDIT_READ` to `Authorities` and the two matching data-only converter allowlist entries. `platform:payments:audit` and the fail-closed conversion rule remain unchanged.
- Updated the existing converter and authority-catalog regression tests for 20 known realm roles and 19 enforced catalog authorities.
- Targeted validation: GREEN, 21 tests, 0 failures, 0 errors, 0 skipped.
- Filtered backend validation outside the sandbox with Podman: GREEN, 339 tests, 0 failures, 0 errors, 5 skipped. Excluded `**/restkit/**/*.java` and `**/paymentsupport/**/*.java` per repository rules.
- The known untracked broken RestKit source blocked `testCompile`; it was temporarily moved to `/tmp` during validation and restored automatically without content changes.
- No dependencies, audit module, migrations, event code, frontend files, Playwright files, or `.kiro/**` files were changed. Playwright was not run.
- Full commands, static checks, changed files, risks, and deferred work are recorded in `.codex/audit-log-dashboard.md`.
- Wave 2 may start only when explicitly requested. It was not started during Wave 1.

## Audit Log Dashboard — Wave 2

Completed on 2026-06-18.

- Status: `COMPLETED`.
- Shared event contract: completed; `AuditableActionOccurredTest` GREEN, 3 tests.
- Durable event dependencies: completed. Added `spring-modulith-events-api` and `spring-modulith-events-jpa`, version-managed by the existing Spring Modulith 2.0.6 BOM.
- Event publication config/schema: completed. Republish-on-restart is enabled, an application-level Jackson `EventSerializer` is registered, and shared Flyway migration `V6__create_event_publication.sql` is active in production and tests.
- Dependencies added: yes, only the two explicitly approved Spring Modulith artifacts.
- Backend validation: `CORE_GREEN_WITH_KNOWN_RESTKIT_BLOCKER`. Production compile, 3 event-contract tests, PostgreSQL Flyway V6, Hibernate validation, and a full application-context smoke test are green. Standard/filtered Maven test lifecycle is currently blocked at shared `testCompile` by 11 unresolved symbols in the excluded `restkit/contract/create/PaymentOrderSecurityContractRestKitTest.java`.
- Security review: the production event contract contains no token, secret, password, Authorization, PAN/CVV, raw body, or generic payload field.
- `.kiro/**` modified: no.
- Playwright files created or run: no.
- Detailed commands, schema/config decisions, exact unrelated validation blocker, changed files, and deferred work are recorded in `.codex/audit-log-dashboard.md`.

Next recommended wave: Wave 3 — Audit module foundation, only after explicit user request. Wave 3 was not started.

## Audit Log Dashboard — Wave 3

Completed on 2026-06-19.

Status: `COMPLETED`

- Audit module declaration: completed; no PUBLIC API.
- Audit Flyway migration: completed as global version `V7` to avoid collision with existing merchant `V1`; `classpath:db/migration/audit` is registered additively.
- Audit entity/repository/exception: completed; `AuditEvent.fromEvent(...)` maps only explicit safe shared-event fields.
- Backend validation: focused green (5 tests, including Modulith architecture and PostgreSQL persistence/Hibernate validation); broad validation blocked by the known excluded RestKit compilation defect and the Wave 2 `eventSerializer` incompatibility with three existing `@DataJpaTest` classes (12 errors in 343 executed tests).
- Security/confidentiality grep: green; no secret/token/password/payment-sensitive/raw-body/generic-payload audit field.
- `.kiro/**` modified: no.
- Playwright files created: no. Playwright was not run.
- Wave 4 was not started.

Next recommended wave: Wave 4 — listener, read service, controller, DTOs, and handler, only after explicit user request. Wave 4 may start, with the existing broad-suite blockers kept visible.

## Audit Log Dashboard — Wave 4

Completed on 2026-06-19.

Status: `COMPLETED`

- Audit listener: completed; consumes the shared event and persists one mapped audit row per invocation.
- Audit query/service/controller/DTOs/handler: completed; list and single-entry reads are tenant-aware, DTOs redact internal actor subject, and failures use masked ProblemDetail responses.
- Endpoints: `GET /api/audit` and `GET /api/audit/{id}`; no write endpoint was added.
- Backend validation: `WAVE_4_CORE_GREEN_WITH_KNOWN_UNRELATED_BROAD_SUITE_FAILURES`. Compile and 5 focused audit/Modulith tests are green. The filtered broad suite has the existing 12 JPA-slice errors caused by the Wave 2 `eventSerializer` requiring an unavailable `ObjectMapper`. The former RestKit compilation blocker is resolved; excluded RestKit execution still has unrelated contract failures.
- `.kiro/**` modified: no.
- Playwright files created or run: no.
- Wave 5 started: no.

Next recommended wave: Wave 5 — emitter event publication, only after explicit user request. Wave 5 may start with the unrelated broad-suite failures kept visible.

## Audit Log Dashboard — Wave 5

Completed on 2026-06-19.

Status: `COMPLETED`

- Merchant emitters: completed for create, activate, and suspend successes.
- Payment emitters: completed for authorize, capture, cancel, and refund successes; idempotent replay does not emit again and status-history semantics are unchanged.
- IAM/user-management emitters: completed for create, update, and role-assignment successes.
- Actor/correlation/tenant metadata: captured at publication time through the safe shared factory from explicit lifecycle context, JWT claims, the authenticated name, and the existing correlation-id MDC; non-request calls use safe non-null fallbacks.
- Module boundary check: passed; emitters use only `shared.events` and no module imports `audit.internal.*`.
- Backend validation: `WAVE_5_CORE_GREEN_WITH_KNOWN_UNRELATED_BROAD_SUITE_FAILURES`. Compile/testCompile, 61 focused tests, 9 module/context tests, and `ModulithArchitectureTest` are green. The filtered broad run retains 12 known Wave 2 JPA-slice errors; excluded RestKit execution retains unrelated failures but no longer blocks compilation.
- `.kiro/**` modified: no.
- Playwright files created or run: no.
- Wave 6 started: no.

Next recommended wave: Wave 6 — backend tests, only after explicit user request. Wave 6 may start with the unrelated broad-suite failures kept visible.

## Audit Log Dashboard — Wave 6 validation repair

Completed on 2026-06-19.

Status: `COMPLETED`

- Unit tests: 8/8 green.
- Repository/PostgreSQL tests: 3/3 green.
- Web MVC tests: 8/8 green.
- Non-container plus architecture tests: 17/17 green.
- Container-backed repository/listener/module tests: 10/10 green.
- REST Assured audit security matrix: 5/5 green.
- Podman socket: available at `/run/user/1000/podman/podman.sock`; user socket active/listening and Docker-compatible API responsive.
- Repository-approved filtered `./mvnw test`: green, 369 tests, 0 failures/errors, 5 skipped.
- Repository-approved filtered `./mvnw verify`: green, 369 Surefire tests plus 21 Failsafe ITs, 0 failures/errors.
- Broad validation exclusions: `**/restkit/**` and `**/paymentsupport/**`, required by `AGENTS.md`.
- RestKit compile blocker: resolved; runtime suite not executed because it is repository-excluded.
- Minimal production fix: durable `EventSerializer` now resolves Jackson lazily through `ObjectProvider`, allowing both JPA slices and full/module contexts to start correctly.
- Module boundary check: passed; no production module imports `audit.internal.*`.
- `.kiro/**` modified: no.
- Playwright files created or run: no.
- Wave 7 started: no.

Next recommended wave: Wave 7 — Frontend foundation, only after explicit user request.

## Audit Log Dashboard — Wave 7

Completed on 2026-06-19.

Status: `COMPLETED`

- Zod schemas: completed; safe event/list/query contracts match backend DTOs and validate native dates plus pagination bounds.
- `useAuditApi`: completed; delegates to `useApiClient`, validates all responses, captures safe metadata, and maps detail 404 to null.
- Server proxy routes: completed for list and detail GET only; safe query whitelist and correlation/header forwarding preserved.
- `canViewAuditLog`: completed and exposed through `useAuthorization`; true only for PLATFORM_ADMIN, SUPPORT_AGENT, and TENANT_ADMIN.
- Frontend validation: green; `corepack pnpm typecheck` passed and 468 Vitest tests across 38 files passed.
- Backend files modified by Wave 7: no. Pre-existing backend worktree changes remain untouched.
- `.kiro/**` modified: no.
- Playwright files created or run: no.
- Wave 8 started: no.

Next recommended wave: Wave 8 — Frontend UI, only after explicit user request.

## Audit Log Dashboard — Wave 8

Completed on 2026-06-19.

Status: `COMPLETED`

- `/admin/audit` page: completed with URL query/filter synchronization, pagination, and `?entry={id}` drawer deep links.
- `AuditTable`: completed with safe read-only columns, row selection, visible outcome text, and stable test ids.
- `AuditFilters`: completed with labeled actor/action/target/date controls and clear/apply behavior.
- `AuditEntryDrawer`: completed with safe detail fields only.
- `nav-link-audit`: completed together with the role-gated dashboard search destination.
- Six UI states: completed for loading, empty, filtered-empty, error, forbidden, and deep-link-not-found.
- Frontend validation: green; `corepack pnpm typecheck` passed and 468 Vitest tests across 38 files passed.
- Backend files modified by Wave 8: no. Pre-existing backend worktree changes remain untouched.
- `.kiro/**` modified: no.
- Playwright files created or run: no.
- Wave 9 started: no.

Next recommended wave: Wave 9 — Frontend tests, only after explicit user request.

## Audit Log Dashboard — Wave 9

Completed on 2026-06-20.

Status: `COMPLETED`

- Property 5: completed with fast-check over all five composite roles plus an unknown role, 100 iterations.
- Capability distinction: verified; `canViewAuditLog` remains separate from `canReadAudit`.
- Six UI state component tests: completed for loading, empty, filtered-empty, error, forbidden, and deep-link-not-found; both frontend and backend forbidden paths are covered.
- Test-driven frontend fix: blank optional date filters now normalize to absent values before audit query validation.
- Frontend validation: green; `corepack pnpm typecheck` passed and 498 Vitest tests across 42 files passed.
- Backend files modified by Wave 9: no. Pre-existing backend worktree changes remain untouched.
- `.kiro/**` modified: no.
- Playwright files created or run: no.
- Final checkpoint started: no.

Next recommended wave: Final checkpoint, only after explicit user request.

## Audit Log Dashboard — Final checkpoint

Status: `COMPLETE_WITH_OPTIONAL_GAPS`

- Required backend waves: complete and audit-specific validation green.
- Required frontend waves: complete and verified.
- Backend repository-approved filtered `./mvnw test`: green, 368 tests run, 0 failures/errors, 5 skipped; one explicitly accepted payment-summary method excluded.
- Backend repository-approved filtered `./mvnw verify`: green; Surefire 368 tests and Failsafe 21 tests, 0 failures/errors.
- `ModulithArchitectureTest`: green.
- `AuditModuleTest`: green.
- Audit migration + JPA validate: green with PostgreSQL/Testcontainers.
- REST Assured audit security matrix: green, 5/5 tests.
- Frontend typecheck: green.
- Frontend unit tests: green, 498/498 tests.
- Security/confidentiality scan: passed; internal actor subject is not browser-visible and no sensitive audit payload is exposed.
- Podman socket and Docker-compatible API: available and green.
- RestKit/payment-support validation suites: excluded as required by `AGENTS.md`.
- `.kiro/**` modified: no.
- Playwright files created by this spec: no.
- Playwright run: no.
- Next spec started: no.

Next recommended work:

- Close `audit-log-dashboard`; required audit backend/frontend validation is green.
- Track the excluded payment-summary local-date versus UTC midnight test as deterministic-test-isolation technical debt.
- Prepare `deterministic-seed-and-test-isolation` or run a status-hygiene audit before starting it.
