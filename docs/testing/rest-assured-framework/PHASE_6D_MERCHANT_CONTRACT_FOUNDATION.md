# Phase 6D — Merchant API Contract Foundation

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 11 live specs
> (1 status + 2 security smoke + 8 merchant contract).

---

## Summary

Phase 6D implements the first authenticated, data-touching live contract specs for the
Merchant API. It adds local DTOs, a thin `MerchantsApi` facade, a `SeedApi` helper for
seed/reset lifecycle, and 8 live `MerchantsContractSpec` scenarios covering create, read,
list, 400 validation, and 409 conflict behavior.

---

## Contract Discovered

### Endpoints

| Method | Path | Authority | Happy-path status |
|---|---|---|---|
| `POST` | `/api/merchants` | `platform:merchants:create` | 201 |
| `GET` | `/api/merchants/{id}` | `platform:merchants:read` | 200 |
| `GET` | `/api/merchants` | `platform:merchants:read` | 200 |
| `POST` | `/api/merchants/{id}/activate` | `platform:merchants:update-status` | 200 |
| `POST` | `/api/merchants/{id}/suspend` | `platform:merchants:update-status` | 200 |

### Request body — POST /api/merchants

```json
{
  "merchantReference": "MERCH-ALPHA-001",
  "displayName": "Alpha Merchant",
  "tenantReference": "TENANT_ALPHA"
}
```

Validation rules (from `CreateMerchantRequest` + `MerchantReference` domain):
- `merchantReference` — `@NotBlank`, `@Size(max=64)`, pattern `[A-Z0-9][A-Z0-9-]{1,62}[A-Z0-9]` after uppercase normalization (min 3 chars)
- `displayName` — `@NotBlank`, `@Size(min=2, max=120)`, must not be whitespace-only
- `tenantReference` — optional for tenant-scoped callers; **required** for platform-scoped callers (e.g. `platform.admin`) — the service throws `MissingTenantReferenceException` → 400 if absent

### Response body — merchant resource

```json
{
  "merchantId": "uuid",
  "merchantReference": "MERCH-ALPHA-001",
  "displayName": "Alpha Merchant",
  "status": "DRAFT",
  "createdAt": "2026-01-15T09:30:00Z",
  "updatedAt": "2026-01-15T09:30:00Z"
}
```

Initial status is always `DRAFT` — the backend enforces this; the client cannot set it.

### Response body — list

```json
{ "merchants": [ {...}, {...} ] }
```

### Error shapes (all from `MerchantExceptionHandler`)

| Scenario | Status | `error` field |
|---|---|---|
| Bean validation failure | 400 | `validation` |
| Blank/missing `merchantReference` | 400 | `validation` |
| Duplicate `merchantReference` | 409 | `duplicate_merchant_reference` |
| Invalid state transition (activate/suspend) | 409 | `invalid_transition` |
| Merchant not found | 404 | `not_found` |
| Malformed UUID path param | 400 | `validation` |
| Tenant resolution failure | 403 | `forbidden` |

### Headers

No `Location` header is set on `POST /api/merchants` — the controller returns
`ResponseEntity.status(201).body(response)` only.
`X-Correlation-ID`, `Cache-Control: no-store`, `Vary: Authorization` are present on error responses
(via `MerchantExceptionHandler` and `GlobalExceptionHandler`).

---

## Key Discoveries

### Platform-scoped callers must supply `tenantReference`

`MerchantService.resolveAssignedTenantId()` checks `tenantContext.isTenantScoped()`:
- Tenant-scoped callers → tenant ID taken from JWT `tenant_id` claim.
- Platform-scoped callers → `tenantReference` from request body is resolved to a tenant ID via DB lookup. If absent → `MissingTenantReferenceException` → 400.

Tests that use `platform.admin` (which is platform-scoped via `PLATFORM_TENANT`) **must**
supply `tenantReference` in the request body. `CreateMerchantRequest.withTenantRef(ref, name, "TENANT_ALPHA")` is the correct factory method.

### MerchantReference is uppercased server-side

`MerchantReference.from()` calls `raw.trim().toUpperCase(Locale.ROOT)`. The normalized
(uppercase) value is stored and returned. Assertions must compare against
`ref.toUpperCase(Locale.ROOT)` rather than the sent form.

### Seed data required for create/read tests

`TenantResolverService.resolve(jwt)` looks up `PLATFORM_TENANT` in the database. Without
seed data the lookup fails → `TenantResolutionException` → 403. This is why security smoke
tests (`merchant.denied`) did not need seed data: Spring Security's `@PreAuthorize` fires
before the service method executes.

`SeedApi.seed()` → `POST /api/test/seed` loads all fixture data including `PLATFORM_TENANT`,
`TENANT_ALPHA`, and three seeded merchants. This endpoint is `permitAll` and active because
`BackendSupport` sets `APP_TESTING_ENABLED=true` and `SPRING_PROFILES_ACTIVE=dev`.

### `jackson-datatype-jsr310` is not on the test classpath

`java.time.Instant` cannot be deserialized without registering `JavaTimeModule`. Since
`jackson-datatype-jsr310` is not in `~/.m2` (offline constraint), `MerchantResponse.createdAt`
and `updatedAt` are typed as `String`. Contract assertions only verify non-null presence,
which `String` supports correctly.

---

## Files Added

| File | Purpose |
|---|---|
| `api/merchant/dto/MerchantResponse.java` | Local DTO for merchant response body |
| `api/merchant/dto/MerchantListResponse.java` | Local DTO for list response envelope |
| `api/merchant/dto/CreateMerchantRequest.java` | Local DTO for create request body + factory methods |
| `api/merchant/MerchantsApi.java` | Thin facade: create, getById, list, listByTenant |
| `api/SeedApi.java` | Seed/reset lifecycle client for `POST /api/test/seed` and `POST /api/test/reset` |
| `scenarios/MerchantsContractSpec.java` | 8 live contract specs (201 create, 200 read, 200 list, 2×400 validation, 409 duplicate, 404 not found, 400 malformed id) |

---

## MerchantsContractSpec — Test Map

| Test | Contract verified | HTTP/SDET concept |
|---|---|---|
| `create_merchant_returns_201_with_draft_status` | 201 + DRAFT body | 201 Created; initial state enforced server-side |
| `read_created_merchant_returns_200` | Create→read round-trip | Write-then-read consistency |
| `list_merchants_returns_200` | 200 with non-empty list | Collection GET; never 404 for empty list |
| `create_with_blank_reference_returns_400` | `@NotBlank` validation | Bean validation before service layer |
| `create_with_short_display_name_returns_400` | `@Size(min=2)` validation | Multiple validation rules on same endpoint |
| `create_duplicate_reference_returns_409` | `duplicate_merchant_reference` | 409 vs 400 distinction; POST non-idempotency |
| `get_unknown_merchant_returns_404` | `not_found` error | 404 masking; use fresh UUID not hardcoded |
| `get_malformed_id_returns_400` | malformed path param | 400 vs 404 for syntactically invalid IDs |

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests
BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 11 IT specs (1 status + 2 security smoke + 8 merchant contract), BUILD SUCCESS
# Startup: Postgres ~3s, Keycloak ~10s, Backend ~20s
```

---

## Deferred to Phase 7+

| Item | Blocker / reason |
|---|---|
| `POST /api/merchants/{id}/activate` and `/{id}/suspend` specs | Needs `ACTIVE`-status merchant setup; out of Phase 6D scope |
| `GET /api/merchants?tenantId=` filtering spec | Tenant-scoped persona testing (TENANT_ALPHA persona) |
| `MerchantListResponse` deserialize to typed record | `TypeRef<MerchantListResponse>` works but requires registered modules for timestamp fields |
| Tenant isolation contract (404 vs 403 boundary) | Phase 7 tenant isolation spec |
| `PaymentOrdersApi` | Separate phase |
