# Phase 7E — Payment Lifecycle Contract Foundation

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 33 live specs
> (1 status + 2 security smoke + 13 merchant contract + 17 payment order contract).

---

## Summary

Phase 7E extends the lifecycle contract coverage beyond Phase 7D's authorize-only tests.
Three new live specs cover the complete CREATED → AUTHORIZED → CAPTURED happy path, the
CREATED → CANCELLED happy path, and a negative transition (capture before authorize → 422).
Two new `PaymentOrdersApi` facade methods (`capture` and `cancel`) follow the same
`RequestSpecs.lifecycle(ifMatch, key)` pattern established in Phase 7D for authorize.
No new DTOs or problem codes were required.

---

## Backend Lifecycle Contract (Discovered)

### State machine

Defined in `PaymentOrder.VALID_TRANSITIONS`:

```
CREATED    → {AUTHORIZED, CANCELLED}
AUTHORIZED → {CAPTURED, CANCELLED, EXPIRED}
CAPTURED   → {REFUNDED}
```

Terminal states: `CANCELLED`, `REFUNDED`, `EXPIRED` (no further transitions allowed).

### Endpoint signatures

All four lifecycle endpoints share the same HTTP contract:

```
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/{action}
Content-Type:    application/json
Idempotency-Key: <key>               (required at Spring MVC layer — @RequestHeader)
If-Match:        "v{N}"              (required = false at MVC; functionally required via PaymentEtag.requireVersion)
Body:            optional (action-specific fields, all optional)
```

| Action | Path suffix | Optional body fields |
|---|---|---|
| authorize | `/authorize` | `reason` (String) |
| capture | `/capture` | `amountMinor` (Long), `reason` (String) |
| cancel | `/cancel` | `reason` (String) |
| refund | `/refund` | `amountMinor` (Long), `reason` (String) |

Body `{}` is sufficient for all actions tested in Phase 7E (omitting optional fields triggers defaults).

### ETag increment chain

| State transition | If-Match required | Resulting ETag |
|---|---|---|
| Create (POST to collection) | — | `"v0"` |
| Authorize | `"v0"` | `"v1"` |
| Capture (after authorize) | `"v1"` | `"v2"` |
| Cancel from CREATED | `"v0"` | `"v1"` |
| Cancel from AUTHORIZED | `"v1"` | `"v2"` |
| Refund (after capture) | `"v2"` | `"v3"` |

Each lifecycle action increments the JPA `@Version` counter exactly once.

### Response contract — lifecycle success (all actions)

All lifecycle endpoints use `lifecycleResponse()` in `PaymentOrderController`:

| Header | Observed value |
|---|---|
| HTTP status | 200 OK |
| `ETag` | `"v{N+1}"` |
| `Vary` | `If-Match` (see Phase 7D for Vary observation) |
| `Cache-Control` | `no-store` |
| `X-Correlation-ID` | propagated |
| `Content-Type` | `application/json` |

Response body shape: `PaymentLifecycleResponse` (same fields as `PaymentOrderResponse` in test DTOs).

### Capture semantics (full vs. partial)

`CaptureRequest.amountMinor` is optional. When absent (body `{}`):
- `request.amountMinor()` returns `null`
- Backend uses `order.getAmountMinor()` as the captured amount
- Result: `capturedAmountMinor` in response equals the original `amountMinor`

`expiresAt` is cleared to `null` on capture — the authorization window closes when funds are settled.

### Cancel semantics (CREATED vs. AUTHORIZED)

`PaymentLifecycleService.cancel()` conditionally calls the PSP:

```java
if (previousStatus == PaymentStatus.AUTHORIZED) {
    pspClient.voidAuthorization(paymentOrderId, "AUTH-" + paymentOrderId);
}
order.cancel(reason);
```

Cancelling a CREATED order skips the PSP void (no authorization reference exists).
Cancelling an AUTHORIZED order voids the authorization with the PSP first.

### Invalid state transition → 422

`PaymentOrder.capture()` (and all domain methods) call `canTransitionTo(target)`:

```java
if (!canTransitionTo(PaymentStatus.CAPTURED)) {
    throw new InvalidStateTransitionException(status, PaymentStatus.CAPTURED);
}
```

`PaymentExceptionHandler` maps `InvalidStateTransitionException`:

```java
@ExceptionHandler(InvalidStateTransitionException.class)
public ResponseEntity<PaymentErrorResponse> handleInvalidStateTransition(...) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_INVALID_TRANSITION,
                   ex.getMessage(), preconditionHeaders());
}
```

- Status: **422 Unprocessable Entity**
- Error code: **`invalid_transition`** (`ProblemCodes.INVALID_TRANSITION` — already existed)
- Headers: `preconditionHeaders()` → `Vary: Authorization, If-Match` (observed: `If-Match`)
- `Cache-Control: no-store`

### Guard ordering in lifecycle service

For `capture()` (and all lifecycle actions):

```
1. isIdempotentLifecycleReplay() — returns early if same key+fingerprint already succeeded
2. PaymentVersionPrecondition.requireCurrentVersion() — throws 412 if ETag stale
3. reserveIdempotency() — insert idempotency record
4. pspClient.capture() — PSP call
5. order.capture(amountMinor) — domain state machine (throws 422 if wrong state)
```

The version check (step 2) fires **before** the state check (step 5). A stale ETag on an
invalid-transition request returns 412, not 422.

---

## Tests Added (Phase 7E)

| Test method | Contract verified | HTTP/SDET concept |
|---|---|---|
| `create_authorize_capture_happy_path_returns_200_and_increments_etag_to_v2` | Create → Authorize → Capture: ETag v0 → v1 → v2, status CAPTURED, capturedAmountMinor = amountMinor | Multi-step ETag chain; full capture semantics; conditional request round-trip |
| `create_cancel_happy_path_returns_200_and_increments_etag_to_v1` | Create → Cancel: ETag v0 → v1, status CANCELLED, cancelledAt set | Cancel from CREATED (no PSP void); single-step lifecycle; ETag increment |
| `capture_on_created_order_returns_422_invalid_transition` | Capture before authorize (CREATED → CAPTURED): 422 `invalid_transition` | Why 422 vs. 400 vs. 412; guard ordering; domain state machine at HTTP layer |

---

## Files Changed

| File | Change |
|---|---|
| `api/payment/PaymentOrdersApi.java` | Added `CAPTURE_PATH`, `CANCEL_PATH`, `capture()`, `cancel()` |
| `scenarios/PaymentOrdersContractSpec.java` | Added 3 lifecycle tests (17 total payment order specs) |
| `docs/.../PHASE_7E_PAYMENT_LIFECYCLE_CONTRACT_FOUNDATION.md` | This document |

---

## New API Facade Methods

### `PaymentOrdersApi.capture()`

```java
public static Response capture(
        String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey) {
    return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
            .contentType(ContentTypes.JSON)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body("{}")
            .when()
            .post(CAPTURE_PATH);
}
```

Body `{}` triggers a full capture. To test partial capture, a separate method with a body
containing `amountMinor` would be added (deferred to a future phase).

### `PaymentOrdersApi.cancel()`

```java
public static Response cancel(
        String merchantId, String paymentOrderId, String ifMatch, String idempotencyKey) {
    return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
            .contentType(ContentTypes.JSON)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body("{}")
            .when()
            .post(CANCEL_PATH);
}
```

Body `{}` omits `reason`, which is optional.

---

## HTTP/REST Concepts Exercised

### Why 200 (not 204) for lifecycle mutations?

204 No Content is appropriate when the server processes a request and returns no body.
Lifecycle actions return the updated payment order state — the client needs the new ETag and
the updated timestamps to know what happened. 200 with a body is the correct choice when
the response payload is meaningful.

### Why 422 and not 400 for invalid state transition?

400 Bad Request means the request was syntactically or structurally wrong — bad JSON, missing
required fields, wrong types. A capture request on a CREATED order is structurally valid: the
JSON is correct, the path is correct, the headers are correct. The problem is semantic: the
domain model's invariant (capture requires AUTHORIZED state) was violated. RFC 4918 introduced
422 Unprocessable Entity specifically for this case: "The server understands the content type
of the request entity... but was unable to process the contained instructions."

### Why does the capture test use the ETag from authorize, not from create?

After authorize succeeds, the JPA `@Version` counter is 1 (`ETag: "v1"`). The backend's version
check in `PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion)` compares the
parsed ETag against `order.getVersion()` (which is 1). Sending the old `"v0"` for capture would
fail with 412. The client must always read the ETag from the most recent response for the same
resource — not from a cached or stale value.

### Why does cancel from CREATED not call the PSP, but cancel from AUTHORIZED does?

CREATED means the order was recorded in the database but no PSP interaction has occurred. There
is no reservation to undo. AUTHORIZED means funds were reserved with the PSP (via `pspClient.authorize()`).
A PSP void must be sent to release those funds. The backend inspects `previousStatus` to determine
which path to take.

---

## Deferred to Phase 7F+

| Item | Reason |
|---|---|
| Cancel from AUTHORIZED state (ETag v1 → v2) | Requires authorize step first; same pattern tested via CREATED cancel |
| Refund full lifecycle (create → authorize → capture → refund) | Four-step chain; same ETag pattern; deferred to avoid over-engineering Phase 7E |
| Partial capture (`amountMinor` in body) | Needs `CaptureRequest.withAmount()` factory; deferred |
| Lifecycle idempotency replay (same key → 200) | Same-key same-action returns current order state |
| Cancel after CAPTURED → 422 invalid_transition | Same pattern as this phase; low incremental value |
| `GET /history` after lifecycle actions | Requires multiple lifecycle transitions; audit-level data |
| `authorization_expired` → 422 (capture after 7-day window) | Requires time manipulation or seeded expired order |

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 33 IT specs (1 status + 2 security smoke + 13 merchant + 17 payment order), BUILD SUCCESS
```
