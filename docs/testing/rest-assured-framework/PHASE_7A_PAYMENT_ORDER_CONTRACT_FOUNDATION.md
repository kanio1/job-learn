# Phase 7A — Payment Order Contract Foundation

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 22 live specs
> (1 status + 2 security smoke + 13 merchant contract + 6 payment order contract).

---

## Summary

Phase 7A adds the first live black-box contract tests for the Payment Order API. All tests
use seeded data and the `platform.payment.reader` persona (`platform:payments:read`), which
bypasses the controller's `merchant_id` JWT claim check. Payment order **create** is deferred
due to a Keycloak realm configuration gap (documented below).

---

## Contract Discovered

### Endpoints (full backend surface)

| Method | Path | Authority | Status |
|---|---|---|---|
| `POST` | `/api/merchants/{merchantId}/payment-orders` | `merchant:payments:create` + JWT `merchant_id` = UUID | **Deferred** — realm gap |
| `GET` | `/api/merchants/{merchantId}/payment-orders/{id}` | `merchant:payments:read` OR `platform:payments:read` | ✅ tested |
| `GET` | `/api/merchants/{merchantId}/payment-orders` | `merchant:payments:read` OR `platform:payments:read` | ✅ tested |
| `HEAD` | `/api/merchants/{merchantId}/payment-orders/{id}` | `merchant:payments:read` OR `platform:payments:read` | deferred |
| `PATCH` | `/api/merchants/{merchantId}/payment-orders/{id}` | `merchant:payments:lifecycle` OR `platform:payments:lifecycle` | deferred |
| `POST` | `/api/merchants/{merchantId}/payment-orders/{id}/authorize` | `merchant:payments:lifecycle` OR `platform:payments:lifecycle` | deferred |
| `POST` | `/api/merchants/{merchantId}/payment-orders/{id}/capture` | `merchant:payments:lifecycle` OR `platform:payments:lifecycle` | deferred |
| `POST` | `/api/merchants/{merchantId}/payment-orders/{id}/cancel` | `merchant:payments:lifecycle` OR `platform:payments:lifecycle` | deferred |
| `POST` | `/api/merchants/{merchantId}/payment-orders/{id}/refund` | `merchant:payments:lifecycle` OR `platform:payments:lifecycle` | deferred |
| `GET` | `/api/merchants/{merchantId}/payment-orders/{id}/history` | multiple read/lifecycle/audit authorities | deferred |
| `GET` | `/api/merchants/{merchantId}/payment-orders/summary` | `merchant:payments:read` OR `platform:payments:read` | deferred |
| `OPTIONS` | `/api/merchants/{merchantId}/payment-orders/{id}` | `permitAll` (security config OPTIONS rule) | deferred |

### Request body — POST /api/merchants/{merchantId}/payment-orders

```json
{
  "amountMinor": 12500,
  "currency": "PLN",
  "clientOrderReference": "ORDER-2026-001"
}
```

Required fields (bean validation):
- `amountMinor` — `@NotNull`, `@Min(1)`, `@Max(100_000_000)` (minor currency units)
- `currency` — `@NotBlank`, `@Size(min=3, max=3)` (ISO 4217 3-letter code)
- `clientOrderReference` — `@NotBlank`, `@Size(max=120)` (merchant's own reference)

Required headers:
- `Idempotency-Key` — mandatory; 400 with `error: "validation"` if missing
- `Content-Type: application/json`

### Response body — payment order resource

```json
{
  "paymentOrderId":      "uuid",
  "merchantId":          "uuid",
  "clientOrderReference": "ORDER-2026-001",
  "amountMinor":         12500,
  "currency":            "PLN",
  "status":              "CREATED",
  "capturedAmountMinor": null,
  "refundedAmountMinor": null,
  "authorizedAt":        null,
  "expiresAt":           null,
  "capturedAt":          null,
  "cancelledAt":         null,
  "refundedAt":          null,
  "cancellationReason":  null,
  "refundReason":        null,
  "metadata":            null,
  "createdAt":           "2026-01-15T09:30:00Z",
  "updatedAt":           "2026-01-15T09:30:00Z"
}
```

Optional fields are `null` when not applicable to the current status.

### Response body — list

```json
{
  "content":       [ { ...payment order... }, ... ],
  "page":          0,
  "size":          20,
  "totalElements": 104,
  "totalPages":    6
}
```

Field name is **`content`** (not `items` or `payments`). Matches Spring Data `Page<T>` serialization.

### Response headers (GET/POST success)

| Header | Value | Note |
|---|---|---|
| `ETag` | `"vN"` (quoted) | N = JPA `@Version` counter; 0 for new order |
| `Cache-Control` | `no-store` | Payment resources must not be cached |
| `Vary` | `Authorization` | Prevents cross-user cache sharing; `Authorization, Idempotency-Key` on create |
| `X-Correlation-ID` | UUID or passed-in value | Distributed tracing; echoed from request |
| `Location` | `/api/merchants/{id}/payment-orders/{paymentOrderId}` | 201 create only |

### Response headers — 201 create (deferred but documented)

On successful create:
- `Location: /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` — canonical resource URL
- `ETag: "v0"` — initial version
- `Vary: Authorization, Idempotency-Key` — idempotency key also affects caching

### Error shapes (all from `PaymentExceptionHandler`)

All payment error responses set `Content-Type: application/problem+json`. The response body:
```json
{
  "type":          "https://api.payment-quality.local/problems/{error-slug}",
  "title":         "Bad Request",
  "status":        400,
  "detail":        "Human-readable message",
  "code":          "VALIDATION",
  "correlationId": "uuid",
  "error":         "validation",
  "message":       "Human-readable message",
  "details":       null
}
```

| Scenario | Status | `error` field |
|---|---|---|
| Bean validation failure | 400 | `validation` |
| Missing `Idempotency-Key` header | 400 | `validation` |
| Non-UUID path parameter | 400 | `validation` |
| Invalid amount/currency/reference domain rule | 400 | `validation` |
| Payment order not found (or wrong merchant) | 404 | `not_found` |
| Merchant scope mismatch (JWT `merchant_id` ≠ path UUID) | 403 | `forbidden` |
| Merchant not ACTIVE (not payment eligible) | 409 | `merchant_not_payment_eligible` |
| Idempotency conflict (same key, different body) | 409 | `idempotency_conflict` |
| Invalid state transition (lifecycle) | 422 | `invalid_transition` |
| If-Match precondition failed | 412 | `payment_order_version_mismatch` |
| If-Match header required but missing | 428 | `precondition_required` |
| Malformed If-Match header | 400 | `malformed_if_match` |

Note: lifecycle invalid transition uses **422 Unprocessable Entity** (not 409 Conflict) in the
payment module — this differs from the merchant module which uses 409 for `invalid_transition`.

---

## Authorization Model — Critical Detail

### Two distinct authority paths for reads

```
platform:payments:read  →  bypasses merchant_id claim check  →  can read any merchant's orders
merchant:payments:read  →  requires JWT merchant_id == path merchantId UUID
```

### Why CREATE is deferred — Keycloak realm gap

`POST /api/merchants/{merchantId}/payment-orders` controller logic (before authority check):
```java
String merchantIdClaim = jwt.getClaimAsString("merchant_id");
if (merchantIdClaim == null || !merchantId.toString().equals(merchantIdClaim)) {
    throw new AccessDeniedException("Merchant scope mismatch");
}
```

Available Keycloak users with `merchant:payments:create`:
| User | Enabled | `merchant_id` attribute | Problem |
|---|---|---|---|
| `merchant.payment.lifecycle` | ✅ | `"PLACEHOLDER_MERCHANT_ID"` | Not a UUID; cannot match seeded merchant UUID |
| `merchant.payment.creator` | ❌ | `"PLACEHOLDER_MERCHANT_ID"` | Disabled AND not a UUID |

The seeded ACTIVE merchants have UUIDs: `00000000-0000-0000-0000-0000000000b1` (MERCHANT_ALPHA_001),
`00000000-0000-0000-0000-0000000000b2`, `00000000-0000-0000-0000-0000000000b3`.
No Keycloak user has `merchant_id` = any of these.

**Resolution path for Phase 7B:** Add a new Keycloak user (e.g. `merchant.alpha.001.creator`)
with `merchant_id = "00000000-0000-0000-0000-0000000000b1"` and roles
`merchant:payments:create` + `merchant:payments:read` to `payment-quality-realm.json`.

---

## Payment Status Lifecycle (confirmed from backend)

```
CREATED ──authorize()──▶ AUTHORIZED ──capture()──▶ CAPTURED ──refund()──▶ REFUNDED
   │                          │
   └──────cancel()────────────┘
         (→ CANCELLED)
```

Status machine in `PaymentStatus` enum: `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`,
`EXPIRED`, `REFUNDED`. Invalid transitions return 422 with `error: "invalid_transition"`.

---

## Seeded Data Used by Tests

| Constant | UUID | Merchant | Amount | Currency | Status |
|---|---|---|---|---|---|
| `MERCHANT_ALPHA_001_ID` | `…b1` | — | — | — | ACTIVE |
| `PAYMENT_ORDER_ALPHA_001_CREATED_ID` | `…c1` | MERCHANT_ALPHA_001 | 1100 | PLN | CREATED |
| `PAYMENT_ORDER_ALPHA_001_AUTHORIZED_ID` | `…c2` | MERCHANT_ALPHA_001 | 2200 | EUR | AUTHORIZED |
| `PAYMENT_ORDER_ALPHA_001_CAPTURED_ID` | `…c3` | MERCHANT_ALPHA_001 | 3300 | USD | CAPTURED |
| MERCHANT_ALPHA_001 pagination orders | `…c101`–`…c198` | MERCHANT_ALPHA_001 | 1000 | varies | varies |

Total for MERCHANT_ALPHA_001: 3 named + 98 pagination = **101 payment orders**.

---

## Files Added

| File | Purpose |
|---|---|
| `core/data/Seeds.java` | Deterministic seed ID constants (mirrors backend `Fixtures`, no dependency) |
| `api/payment/dto/PaymentOrderResponse.java` | Test-side DTO for single payment order |
| `api/payment/dto/PaymentOrderListResponse.java` | Test-side DTO for paginated list response |
| `api/payment/PaymentOrdersApi.java` | Thin facade: `getById`, `list`; create as skeleton with `UnsupportedOperationException` |
| `scenarios/PaymentOrdersContractSpec.java` | 6 live contract specs |

---

## Test Map

| Test | Contract verified | HTTP/SDET concept |
|---|---|---|
| `get_seeded_payment_order_returns_200_with_body` | GET → 200 with body fields | Black-box serialization check; platform reader bypasses merchant_id |
| `get_payment_order_response_has_required_security_headers` | ETag, Cache-Control: no-store, Vary: Authorization | Header contract; payment security compliance |
| `get_authorized_payment_order_returns_200_with_lifecycle_timestamps` | AUTHORIZED status → authorizedAt/expiresAt non-null | Nullable field contract per lifecycle status |
| `list_payment_orders_returns_200_with_pagination_envelope` | GET list → `content`/`page`/`size`/`totalElements`/`totalPages` | Spring Page serialization contract; envelope field names |
| `get_unknown_payment_order_returns_404` | 404 with `application/problem+json` | 404 masking; all PaymentExceptionHandler responses set content type |
| `get_payment_order_with_malformed_id_returns_400` | non-UUID path param → 400 `validation` | 400 vs 404 for invalid input |

---

## Key Design Decisions

### `Seeds.java` instead of inline constants

Stable UUIDs from the backend's `Fixtures` class are extracted to a test-side `Seeds` class.
This provides: (1) a single place to update when fixtures change; (2) self-documenting names
in test code; (3) Javadoc explaining what each seed represents.

### All payment errors use `application/problem+json`

Unlike `MerchantExceptionHandler` (which only sets `application/problem+json` on some handlers),
`PaymentExceptionHandler.problem()` always sets it via a shared private method. This makes
`ProblemAssert.hasContentTypeProblemJson()` safe to call on all payment error assertions —
no handler-by-handler analysis needed.

### `PaymentOrdersApi.create()` throws `UnsupportedOperationException`

The skeleton method documents the future intent without providing a callable that would
mis-represent the current testable contract. Callers that accidentally invoke it get an
immediate, descriptive failure explaining why it's deferred.

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests
BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 22 IT specs (1 status + 2 security smoke + 13 merchant + 6 payment order), BUILD SUCCESS
```

---

## Deferred to Phase 7B+

| Item | Blocker / required work |
|---|---|
| `POST /api/merchants/{id}/payment-orders` create test | Add Keycloak user with `merchant_id = UUID` of a seeded merchant to realm JSON |
| HEAD (ETag-only response) | Low complexity; add after create is unblocked (needs real ETag from created order) |
| LIST with filters (`status`, `currency`, `fromDate`, etc.) | Stable; add incrementally once create is unblocked |
| `GET /summary` | Stable; add after pagination and filter coverage is solid |
| PATCH (metadata update) | Requires `If-Match`; needs created order first |
| Lifecycle: authorize/capture/cancel/refund | Require `If-Match`; need created order in right state |
| Idempotency replay (same key → 200) | Requires create test first |
| Idempotency conflict (same key, different body → 409) | Requires create test first |
| `GET /history` | Requires lifecycle actions first |
| Tenant isolation for payment orders | Phase 7+ tenant contract spec |
| `merchant:payments:read` scope (merchant-scoped reads) | Requires Keycloak user with real merchant_id UUID |
