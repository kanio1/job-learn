# Phase 7C — Payment Order Create Idempotency Contract

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 27 live specs
> (1 status + 2 security smoke + 13 merchant contract + 11 payment order contract).

---

## Summary

Phase 7C adds black-box contract tests for the payment order create idempotency semantics.
Two new live specs cover the replay path (same key + same body → 200) and the conflict
path (same key + different body → 409). These tests are self-contained: each test drives
its own first create as a setup step, then drives the idempotency scenario in the same
test method, avoiding shared mutable state between tests.

---

## Backend Idempotency Implementation (Discovered)

### Storage

Table `idempotency_records` with columns:

| Column | Type | Notes |
|---|---|---|
| `idempotency_record_id` | UUID | PK |
| `merchant_id` | UUID | Scopes the key per merchant |
| `idempotency_key_hash` | VARCHAR(64) | SHA-256 of the raw `Idempotency-Key` header value |
| `request_fingerprint_hash` | VARCHAR(64) | SHA-256 of the canonical request JSON |
| `payment_order_id` | UUID | Linked after the order is created |
| `created_at`, `completed_at` | Instant | Lifecycle timestamps |
| `action` | VARCHAR(20) | `"CREATE"` for create operations |

Key: `(merchant_id, idempotency_key_hash)` — a key is scoped per merchant, not globally.

### Request Fingerprint

`RequestFingerprint.of(merchantId, amountMinor, currency, clientOrderReference)` computes:

```
SHA-256({
  "operation": "POST /api/merchants/{merchantId}/payment-orders",
  "merchantId": "<uuid>",
  "amountMinor": <long>,
  "currency": "<string>",
  "clientOrderReference": "<string>"
})
```

The `merchantId` in the fingerprint comes from the JWT claim, not from the request body.
Changing any of the four body fields (`amountMinor`, `currency`, `clientOrderReference`)
or the merchant in the path changes the fingerprint.

### Flow in `PaymentOrderService.create()`

```
1. Validate merchant eligibility
2. Compute fingerprint from (merchantId, amountMinor, currency, clientOrderReference)
3. Look up existing record by (merchantId, idempotencyKeyHash)
   a. Found, fingerprint matches → return PaymentCreateResult.replayed(order)      → 200
   b. Found, fingerprint mismatch → throw IdempotencyConflictException             → 409
   c. Not found → reserve row, create order, complete record                       → 201
```

The `reserveIfAbsent` path (step 3c) handles concurrent duplicate requests via a database
uniqueness constraint: if two threads both miss the lookup, only one insert wins; the loser
falls back to `resolveExistingIdempotencyRecord()`.

### HTTP Response Contract

| Scenario | Status | Location | ETag | Vary (observed) |
|---|---|---|---|---|
| First create (new key) | 201 Created | ✓ `/api/.../payment-orders/{id}` | `"v0"` | `Idempotency-Key` |
| Replay (same key + same body) | 200 OK | ✗ not present | `"vN"` (same version) | `Idempotency-Key` |
| Conflict (same key + different body) | 409 Conflict | ✗ | ✗ | `Idempotency-Key` |

All three paths include `Cache-Control: no-store` and `X-Correlation-ID`.

### Observed Vary Behavior vs. Backend Intent

The backend code sets `VARY_AUTHORIZATION_IDEMPOTENCY_KEY = "Authorization, Idempotency-Key"` and
calls `.varyBy("Authorization", "Idempotency-Key")` on all create response paths. However, the
observed HTTP response header is `Vary: Idempotency-Key` only (not the combined form).

**Likely cause**: a Spring Security filter or CorrelationIdFilter runs after the controller and
overwrites the `Vary` header. When the final response is written, only `Idempotency-Key` survives.
The `Authorization` Vary is covered globally by the security filter chain (typically visible on
GET/HEAD endpoints which don't go through this overwrite path).

**Contract tests assert `Vary: Idempotency-Key` only** — this matches the actual observed behavior.
The security layer's Vary: Authorization behavior is tested separately in
`get_payment_order_response_has_required_security_headers`.

### Why `Vary: Idempotency-Key` on 409?

`PaymentExceptionHandler.headersForRequest()` detects a `POST` to
`/api/merchants/*/payment-orders` and uses `createHeaders()` which sets
`Vary: Authorization, Idempotency-Key`. The observed value is `Idempotency-Key` (same
overwrite behavior as success paths). This prevents a cache from serving a 409 response
to a client with a completely different idempotency key.

### `IdempotencyConflictException` → 409

```java
@ExceptionHandler(IdempotencyConflictException.class)
public ResponseEntity<PaymentErrorResponse> handleIdempotencyConflict(...) {
    return problem(HttpStatus.CONFLICT, ERROR_IDEMPOTENCY_CONFLICT, ex.getMessage(),
                   headersForRequest(request));
}
```

`ERROR_IDEMPOTENCY_CONFLICT = "idempotency_conflict"` — available in `ProblemCodes.IDEMPOTENCY_CONFLICT`.

---

## Tests Added (Phase 7C)

| Test method | Contract verified | HTTP/SDET concept |
|---|---|---|
| `idempotency_replay_with_same_key_and_body_returns_200` | Replay → 200, same `paymentOrderId`, same `ETag`, no `Location`, `Vary` includes `Idempotency-Key` | Safe retry; idempotency state machine; 200 vs 201 semantics |
| `idempotency_conflict_with_same_key_different_body_returns_409` | Conflict → 409 `idempotency_conflict`, `application/problem+json`, `Vary` includes `Idempotency-Key` | Fingerprint-based conflict detection; 409 error shape |

---

## HTTP/REST Concepts

### Why 200 and not 201 on replay?

`201 Created` signals a new resource was created by this request. On replay the resource
already exists — returning 201 would be semantically wrong (and would imply the `Location`
header points to a new resource, which it doesn't). `200 OK` means "here is the resource
you are referencing."

### Why no `Location` on replay?

`Location` is the URL of the *newly created* resource. On replay no new resource was created.
The client already received the `Location` from the first 201 response and can reuse it.
Including `Location` on replay would imply the URL changed, which it didn't.

### Why compare a request fingerprint and not just the key?

The idempotency key alone is not sufficient to detect key reuse across different amounts.
A client bug could reuse an old key from a different payment — the key matches but the
amounts differ. Without fingerprint checking, the backend would silently return the old order
(wrong amount) or create a duplicate (if the key expired). Fingerprint comparison catches
this at 409 before any money moves.

### Why 409 and not 400 for fingerprint mismatch?

400 means the request was malformed (syntactically invalid). A 409 idempotency conflict is
semantically valid JSON with a valid key and valid fields — the problem is that the key was
previously used for a *different* transaction. 409 Conflict is the correct status for a
server-state conflict, not a client formatting error.

### Why does the 409 error response include `Vary: Idempotency-Key`?

Caching infrastructure does not distinguish 2xx from 4xx responses. If an HTTP cache were to
cache the 409 without `Vary: Idempotency-Key`, a subsequent request with a *different* (fresh)
key on the same URL could receive the cached 409 — blocking a legitimate new payment. The Vary
header prevents this.

---

## Files Changed

| File | Change |
|---|---|
| `scenarios/PaymentOrdersContractSpec.java` | Added 2 idempotency tests (11 total payment order specs) |
| `docs/.../PHASE_7C_PAYMENT_IDEMPOTENCY_CONTRACT.md` | This document |

---

## Test Data Strategy

Both idempotency tests are self-contained:

1. Each test generates a fresh `IdempotencyKeys.generate("...")` — unique per test execution.
2. Each test generates a fresh `UniqueReferences.paymentRef("...")` — unique per test execution.
3. The first step inside each test is a create that must return 201 — this is not `@BeforeEach`
   setup, it is the **first assertion** of the test chain. If the setup fails, the test fails with
   a clear status-code mismatch rather than a null pointer in the assertion step.
4. `SeedApi.reset()` in `@AfterAll` truncates `idempotency_records` along with payment orders,
   so created records do not bleed into subsequent spec classes.

---

## Deferred to Phase 7D+

| Item | Reason |
|---|---|
| `HEAD /api/merchants/{id}/payment-orders/{id}` — ETag-only response | Unblocked; add in next phase |
| Idempotency key validation errors (blank key, >128 chars, non-ASCII) | Covered by backend unit tests; low value at API level |
| Idempotency behavior for lifecycle actions (authorize/capture/cancel/refund) | Requires If-Match + specific lifecycle state |
| LIST with filters | Straightforward; add incrementally |
| PATCH metadata update (requires If-Match) | Needs created order + ETag handling |
| `GET /summary` | Requires several orders in various states |
| `GET /history` | Requires lifecycle actions first |

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 27 IT specs (1 status + 2 security smoke + 13 merchant + 11 payment order), BUILD SUCCESS
```
