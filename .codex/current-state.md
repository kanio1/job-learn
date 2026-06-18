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
