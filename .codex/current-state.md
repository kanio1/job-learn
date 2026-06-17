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

## Current Active Work

Start with Wave 2 from `.kiro/specs/tenant-model-and-isolation/tasks.md`:

- 5.1 Extend `JpaMerchantRepository` with tenant-filtered methods.
- 5.2 Add merchant-layer tenant exceptions.
- 5.3 Add optional `tenantReference` to `CreateMerchantRequest`.
- 5.4 Replace the Wave 1 placeholder-tenant bridge in `MerchantService` with real `TenantContext`-based behavior.

## Current Code Still Pre-Wave-2

These files are not yet tenant-aware beyond the Wave 1 bridge:

- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/infrastructure/JpaMerchantRepository.java`
  - Currently only has `findByNormalizedReference(...)` and `findAllByOrderByCreatedAtDescMerchantIdAsc(...)`.
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/CreateMerchantRequest.java`
  - Currently has only `merchantReference` and `displayName`.
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantService.java`
  - Still injects `JdbcTemplate` to look up `PLACEHOLDER_TENANT_ID`.
  - `create(...)`, `findById(...)`, `listFirstPage()`, `activate(...)`, and `suspend(...)` are not yet tenant-aware.
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java`
  - Does not yet resolve `TenantContext` from JWT.
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantExceptionHandler.java`
  - Does not yet map tenant-resolution and tenant-boundary exceptions.

## Known Spec Gap to Resolve Carefully

The Kiro design says `MerchantService` should use `TenantResolver` to resolve a platform-provided `tenantReference` string, but the current public `TenantResolver` interface only has:

```java
TenantContext resolve(Jwt jwt);
```

Codex must not import `lab.paymentquality.tenant.internal.*` into the merchant module. The smallest acceptable implementation is to extend the tenant module PUBLIC API with an additional method or type that resolves a tenant reference to a UUID or `TenantContext`, while preserving the existing `resolve(Jwt)` method.

Preferred approach:

- Keep `TenantResolver.resolve(Jwt jwt)` unchanged.
- Add a small public method such as `UUID resolveTenantId(TenantReference tenantReference)` or `TenantContext resolve(TenantReference tenantReference)` only if required by Wave 2.
- Implement the method inside `TenantResolverService` using `JpaTenantRepository`.
- Keep error mapping consistent with Wave 2 exceptions: missing body reference -> `MissingTenantReferenceException`; unknown body reference -> `UnresolvableTenantReferenceException`.
- Do not make `merchant` depend on `tenant.internal.infrastructure.JpaTenantRepository`.

## Verification Command After Wave 2

Run from `apps/backend`:

```bash
./mvnw test
```

Do not proceed to Wave 3 unless Wave 2 compiles and existing tests remain green.
