# Phase 8C — Tenant / Merchant Isolation Security Matrix

## Goal

Document the backend's real isolation model for payment and merchant endpoints and add a focused
set of live specs that verify the four most important isolation properties:
BOLA masking on GET-by-ID, 403 on collection/sub-resource cross-merchant access, and
the platform authority bypass.

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/TenantIsolationContractSpec.java` | New spec: 4 tests |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8C row added |

## Backend Isolation Model (Discovered)

### Authority Architecture

The backend has two orthogonal authority namespaces:

| Namespace | Example | Scope |
|---|---|---|
| `merchant:payments:*` | `merchant:payments:read` | Caller is scoped to ONE merchant (JWT `merchant_id` claim must match path UUID) |
| `platform:payments:*` | `platform:payments:read` | Caller is a platform operator; bypasses `merchant_id` check; cross-merchant reads allowed |
| `platform:merchants:*` | `platform:merchants:read` | ALL merchant endpoint operations (no merchant-scoped equivalent) |

### Payment Order Endpoint Isolation

The `PaymentOrderController` enforces merchant-scope by reading the JWT `merchant_id` claim and
comparing it against the `{merchantId}` path parameter. The response code differs by endpoint type:

```
Endpoint                             merchant-scoped cross-merchant    platform:payments:read
─────────────────────────────────────────────────────────────────────────────────────────────
GET  /payment-orders/{id}            404 not_found (BOLA masking)      200 bypass
GET  /payment-orders                 403 forbidden (collection)         200 bypass
GET  /payment-orders/summary         403 forbidden (aggregate)          200 bypass
GET  /payment-orders/{id}/history    403 forbidden (sub-resource)       200 bypass
POST /payment-orders                 403 forbidden (write)              N/A (lifecycle role)
POST /payment-orders/{id}/authorize  403 forbidden (write)              N/A (lifecycle role)
POST /payment-orders/{id}/capture    403 forbidden (write)              N/A (lifecycle role)
POST /payment-orders/{id}/cancel     403 forbidden (write)              N/A (lifecycle role)
POST /payment-orders/{id}/refund     403 forbidden (write)              N/A (lifecycle role)
PATCH /payment-orders/{id}           403 forbidden (write)              N/A (lifecycle role)
```

### Why GET-by-ID returns 404 but collection returns 403

**GET /{id} — 404 (BOLA masking):**
```java
// PaymentOrderController.getPaymentOrder
if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
    throw new PaymentOrderNotFoundException(paymentOrderId);  // → 404
}
```
The controller throws `PaymentOrderNotFoundException` (not `AccessDeniedException`).
This maps to 404 `not_found` via `PaymentExceptionHandler.handleNotFound`.

A 404 prevents the caller from distinguishing "this payment order belongs to another merchant"
from "this payment order does not exist" — existence masking, a deliberate BOLA (API1:2023) defence.

**GET list, summary, history — 403:**
```java
// PaymentOrderController.listPaymentOrders, summarizePaymentOrders, getHistory
if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
    throw new AccessDeniedException("Merchant scope mismatch");  // → 403
}
```
Collection and aggregate endpoints cannot mask their own existence (the URL itself is the
collection, not a specific resource), so 403 is the correct response.

**Why not an empty list for the collection?**
Returning `{"content": []}` with 200 would incorrectly imply authorization succeeded but the
merchant has no orders. 403 is semantically correct: the caller is denied access to enumerate
the collection at all.

### Merchant Endpoint Isolation

Merchant endpoints require `platform:merchants:*` authorities — there is no merchant-scoped
equivalent. Tenant isolation for merchants is enforced through `TenantContext`:

```java
// MerchantService.findById(UUID id, TenantContext tenantContext)
Merchant merchant = tenantContext.isTenantScoped()
    ? repository.findByMerchantIdAndTenantId(id, tenantContext.tenantId())
          .orElseThrow(() -> new MerchantNotFoundException(id.toString()))  // → 404
    : repository.findById(id)...
```

- `platform.admin` (PLATFORM_TENANT, `isPlatformScoped=true`) → sees ALL merchants.
- `tenant.admin` (TENANT_ALPHA, `isTenantScoped=true`) → sees only TENANT_ALPHA merchants;
  requesting MERCHANT_BETA_001 returns 404.

**Isolation model:**
```
Endpoint                  platform caller     tenant-scoped caller
──────────────────────────────────────────────────────────────────
GET  /api/merchants        all merchants       only own-tenant merchants
GET  /api/merchants/{id}   any merchant        404 if different tenant
POST /api/merchants        global              creates in own tenant (no tenantReference needed)
POST /{id}/activate        any merchant        403 if different tenant (TenantBoundaryViolation)
POST /{id}/suspend         any merchant        403 if different tenant (TenantBoundaryViolation)
```

Note: Merchant endpoint tenant isolation is covered by `MerchantsContractSpec` for the basic
cases. A dedicated tenant-boundary test for merchants is deferred (requires a non-platformAdmin
persona that has `platform:merchants:read` but is tenant-scoped — the existing `tenant.admin`
covers this but a focused cross-tenant merchant read test is Phase 8D scope).

### Keycloak Users and Their Effective Authorities

| Keycloak user | JWT attributes | Effective Spring authorities |
|---|---|---|
| `platform.admin` | `tenant_id=PLATFORM_TENANT` | `platform:merchants:*`, `platform:payments:*`, `platform:audit:read`, `platform:users:*` |
| `tenant.admin` | `tenant_id=TENANT_ALPHA` | `platform:merchants:read/create/update-status`, `merchant:payments:read`, `tenant:audit:read`, `tenant:users:*` |
| `merchant.alpha.creator` | `merchant_id=MERCHANT_ALPHA_001_ID`, `tenant_id=TENANT_ALPHA` | `merchant:payments:create/read/lifecycle` |
| `platform.payment.reader` | (none) | `platform:payments:read` |
| `merchant.denied` | `tenant_id=PLACEHOLDER_TENANT_ID` | (none — no authorities) |

## Tests Added

### `merchant_scoped_get_payment_order_for_other_merchant_returns_404`

**Identity**: `seededMerchantCreator()` (merchant_id=MERCHANT_ALPHA_001_ID, has `merchant:payments:read`)  
**Request**: `GET /api/merchants/MERCHANT_ALPHA_002_ID/payment-orders/PAYMENT_ORDER_ALPHA_002_CANCELLED_ID`  
**Expected**: 404 `not_found`  

Verifies BOLA masking: the controller throws `PaymentOrderNotFoundException` (not
`AccessDeniedException`) when the JWT `merchant_id` does not match the path UUID for a
single-resource GET. The 404 hides whether the order exists under a different merchant.

### `merchant_scoped_list_payment_orders_for_other_merchant_returns_403`

**Identity**: `seededMerchantCreator()` (merchant_id=MERCHANT_ALPHA_001_ID)  
**Request**: `GET /api/merchants/MERCHANT_ALPHA_002_ID/payment-orders`  
**Expected**: 403 `forbidden`  

Verifies that the collection endpoint denies access explicitly (not returns an empty list)
when the caller's `merchant_id` does not match the path `merchantId`.

### `merchant_scoped_get_history_for_other_merchant_returns_403`

**Identity**: `seededMerchantCreator()` (merchant_id=MERCHANT_ALPHA_001_ID)  
**Request**: `GET /api/merchants/MERCHANT_ALPHA_002_ID/payment-orders/PAYMENT_ORDER_ALPHA_002_CANCELLED_ID/history`  
**Expected**: 403 `forbidden`  

Verifies that the history sub-resource enforces the same merchant-scope boundary as the
collection endpoint.

### `platform_payment_reader_reads_across_merchant_boundary_returns_200`

**Identity**: `merchantReader(Seeds.MERCHANT_ALPHA_002_ID)` (platform:payments:read, no merchant_id claim)  
**Request**: `GET /api/merchants/MERCHANT_ALPHA_002_ID/payment-orders/PAYMENT_ORDER_ALPHA_002_CANCELLED_ID`  
**Expected**: 200 with CANCELLED payment order body  

Verifies that `platform:payments:read` bypasses the `merchant_id` JWT claim check and allows
reading any merchant's payment orders. Also asserts the response body contains the correct
seeded data (clientOrderReference, status, amountMinor, currency) — not just a 200 with an
empty or wrong body.

## Isolation Properties NOT Tested in This Phase

| Property | Reason deferred |
|---|---|
| Tenant admin reads cross-tenant merchant → 404 | Requires dedicated cross-tenant merchant test; existing `MerchantsContractSpec` covers basic list isolation implicitly |
| `platform:payments:lifecycle` bypasses merchant lifecycle guard | No existing persona with platform lifecycle but not platform read; Phase 8D scope |
| `tenant.admin` GET payment order → 404 (no `merchant_id` claim, wrong path UUID) | Interesting but lower priority than merchant:payments:read case; Phase 8D scope |
| Platform payment reader lists MERCHANT_ALPHA_002's payment orders → 200 | Would be redundant with the GET-by-ID platform bypass test above |

## SDET Interview Topics

- **BOLA vs BFLA**: BOLA (Broken Object-Level Authorization) is an access violation on a specific
  resource identified by an ID. BFLA (Broken Function-Level Authorization) is an access violation
  on an operation or endpoint type. GET-by-ID cross-merchant is BOLA; list cross-merchant is BFLA.

- **Why 404 not 403 for GET-by-ID?** 403 leaks existence information. An attacker can enumerate
  valid UUIDs across merchants by observing 403 (exists, not yours) vs 404 (does not exist).
  404 masking eliminates this oracle.

- **Why not return an empty list for unauthorized collection access?** An empty list implies
  authorization passed but no records matched the filter. 403 is semantically correct: the caller
  is not permitted to query the collection at all.

- **What guarantees does `platform:payments:read` need?** The authority must be in the JWT (not
  just requested), must survive the Keycloak `realm_access.roles` → converter → Spring authority
  chain, and must be checked BEFORE the `merchant_id` claim comparison in the controller.

## Test Results

- **79 offline tests**: all pass (unchanged)
- **50 live tests**: all pass (4 new in Phase 8C)
