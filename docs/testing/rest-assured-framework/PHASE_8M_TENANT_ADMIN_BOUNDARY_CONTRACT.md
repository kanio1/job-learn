# Phase 8M — Tenant Admin Boundary Contract

## Scope

Add focused black-box REST Assured live coverage for the real tenant administrator persona.

The phase uses the existing Keycloak user `tenant.admin`; no new realm user or backend change was required.

## Realm and Backend Support Discovered

Realm user:

- username: `tenant.admin`;
- password: `tenant.admin`;
- realm role: `TENANT_ADMIN`;
- JWT attribute: `tenant_id=TENANT_ALPHA`;
- no `merchant_id` attribute.

Effective Spring authorities from the `TENANT_ADMIN` composite:

- `platform:merchants:create`;
- `platform:merchants:read`;
- `platform:merchants:update-status`;
- `merchant:payments:read`;
- `tenant:audit:read`;
- tenant user-management authorities.

Merchant endpoints resolve `tenant_id` through `TenantResolverService`.

- Platform tenants see all merchants.
- Tenant-scoped callers see only merchants for their resolved tenant.
- `GET /api/merchants/{id}` for another tenant is masked as 404.
- Status changes for another tenant are guarded by `TenantBoundaryViolationException` and return 403, but this phase did not add a status-transition matrix.

Payment-order endpoints do not use `tenant_id` as sufficient object scope.

- `platform:payments:read` bypasses the merchant claim requirement.
- Otherwise, payment reads require JWT `merchant_id` to match the path `merchantId`.
- `tenant.admin` has `merchant:payments:read` but no `merchant_id`, so even same-tenant payment-order GET-by-ID is masked as 404.

## Added Specs

### `tenant_admin_reads_and_lists_only_own_tenant_merchants`

Verifies:

- `tenant.admin` can `GET /api/merchants/{MERCHANT_ALPHA_001_ID}` with 200;
- `tenant.admin` can `GET /api/merchants` and sees `MERCHANT_ALPHA_001` and `MERCHANT_ALPHA_002`;
- the same list excludes `MERCHANT_BETA_001` from `PLATFORM_TENANT`;
- `GET /api/merchants/{MERCHANT_BETA_001_ID}` returns 404 `not_found`.

Security value:

- proves tenant-scoped merchant administration works for own-tenant merchants;
- proves cross-tenant merchant object reads are masked.

### `tenant_admin_without_merchant_id_cannot_read_same_tenant_payment_order`

Verifies:

- `tenant.admin` cannot read `MERCHANT_ALPHA_001` payment order `PAYMENT_ORDER_ALPHA_001_CREATED_ID`;
- response is 404 `not_found`;
- response uses `application/problem+json`;
- response carries `Cache-Control: no-store`;
- `Vary` contains `Authorization`.

Security value:

- proves tenant administration authority does not imply direct payment-data access;
- proves payment-order object scope is narrower than tenant scope unless a platform payment authority is present.

## BOLA / BFLA Notes

- Cross-tenant `GET /api/merchants/{id}` is BOLA and is masked with 404.
- `GET /api/merchants` is a collection-level authorization/filtering contract; tenant admin receives only own-tenant objects.
- Same-tenant payment-order GET by tenant admin is still BOLA-protected because the payment order controller requires merchant object scope (`merchant_id`) or platform payment scope.

## Test Results

- Offline: `cd apps/api-tests && mvn -q test` — 79 tests passed.
- Live: `cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify` — 72 tests passed.
