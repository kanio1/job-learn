# Phase 8F — Partial Refund Contract

## Goal

Verify the partial-refund semantics and amount-validation error contract for
`POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund`.
Extends the full-refund happy path from Phase 7F with targeted coverage of:
the partial-amount happy path, over-refund, and zero-amount boundary.

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/core/problem/ProblemCodes.java` | Added `REFUND_AMOUNT_EXCEEDS_CAPTURED = "refund_amount_exceeds_captured"` |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/payment/PaymentOrdersApi.java` | Added `refundWithAmount()` facade method |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/PartialRefundContractSpec.java` | New spec: 3 tests |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8F row added |

## Backend Partial Refund Contract (Discovered)

### Request Body

```
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund
Content-Type:    application/json
Idempotency-Key: <key>               (required — @RequestHeader, not required = false)
If-Match:        "v{N}"              (required = false at MVC; functionally required via PaymentEtag.requireVersion)
Body:            optional — RefundRequest(Long amountMinor, String reason)
```

`RefundRequest` is a plain Java record with no bean-validation annotations.
All amount validation is domain-level only, inside `PaymentOrder.refund()`.

### Amount Semantics

```java
// PaymentOrder.refund(Long refundAmountMinor, String reason):
long effectiveAmount = refundAmountMinor != null ? refundAmountMinor : capturedAmountMinor;
if (effectiveAmount <= 0 || effectiveAmount > capturedAmountMinor) {
    throw new InvalidRefundAmountException(effectiveAmount, capturedAmountMinor);
}
this.status = PaymentStatus.REFUNDED;
this.refundedAmountMinor = effectiveAmount;
this.refundedAt = Instant.now();
this.refundReason = reason;
```

| amountMinor in body | Effective amount | Result |
|---|---|---|
| absent / null | `capturedAmountMinor` | 200 REFUNDED — full refund |
| `1 ≤ n ≤ capturedAmountMinor` | `n` | 200 REFUNDED — partial refund |
| `n > capturedAmountMinor` | `n` | 422 `refund_amount_exceeds_captured` |
| `n ≤ 0` | `n` | 422 `refund_amount_exceeds_captured` |

**Note:** the error code name (`_exceeds_captured`) is technically imprecise for the zero/negative
case (`0` does not exceed `capturedAmountMinor`). The same `InvalidRefundAmountException` class
covers both paths; the name reflects the primary use-case (over-refund). Testing both paths
confirms there is no separate guard for negative amounts.

### Multiple Refunds

Not supported. The order transitions to REFUNDED (terminal state) on the first successful
refund call, whether partial or full. A second refund attempt (with a corrected or remaining
amount) returns 422 `invalid_transition` because the Phase 7G pre-guard fires:

```java
if (!order.canTransitionTo(PaymentStatus.REFUNDED)) {
    throw new InvalidStateTransitionException(order.getStatus(), PaymentStatus.REFUNDED);
}
```

For a REFUNDED order: `canTransitionTo(REFUNDED)` = false → 422 `invalid_transition`.

### Guard Order in Service

```
1. Idempotency replay check        — early return if same key+fingerprint
2. ETag version check              — 412 if stale
3. Idempotency reservation         — DB insert (rolled back if exception later)
4. canTransitionTo(REFUNDED) guard — 422 invalid_transition if wrong state
5. PSP call                        — refund settled (mock in test env)
6. order.refund(amountMinor, ...)  — 422 refund_amount_exceeds_captured if amount invalid
                                      transaction rolls back (step 3 rolled back too)
```

**Key implication for testing:** the amount validation (step 6) fires AFTER the ETag check
(step 2) and the PSP call (step 5). A request with both a stale ETag AND an invalid amount
would return 412, not 422. In these tests, the ETag is always correct (`"v2"` for the seeded
captured order) so the 422 is reached.

### Response on Success (Partial Refund)

| Header/Field | Value |
|---|---|
| HTTP status | 200 |
| `ETag` | `"v{N+1}"` — incremented from the CAPTURED order's version |
| `Vary` | `If-Match` |
| `Cache-Control` | `no-store` |
| `status` | `"REFUNDED"` |
| `refundedAmountMinor` | requested partial amount |
| `capturedAmountMinor` | unchanged (still the captured amount) |
| `refundedAt` | non-null timestamp |

### Response on Error (Amount Validation)

| Header/Field | Value |
|---|---|
| HTTP status | 422 |
| `Content-Type` | `application/problem+json` |
| `error` | `refund_amount_exceeds_captured` |
| `Vary` | `Authorization, If-Match` (via `preconditionHeaders()`) |
| `Cache-Control` | `no-store` |
| `correlationId` | present |

## Tests Added

### `partial_refund_returns_200_with_partial_refunded_amount_and_etag_v3`

**Setup**: create (1 000 PLN) → authorize → capture → `refundWithAmount(400)`.

**ETag chain**: `"v0"` → `"v1"` → `"v2"` → `"v3"` (same increment cadence as full refund).

**Assertions**: 200, `ETag: "v3"`, `status: "REFUNDED"`, `refundedAmountMinor: 400`,
`refundedAt: non-null`, `Vary: If-Match`, `Cache-Control: no-store`.

**Business risk verified**: merchant can return a portion of the settled amount without
triggering a full refund. `refundedAmountMinor = 400` is the authoritative record of the
financial reversal.

### `over_refund_exceeding_captured_amount_returns_422`

**Setup**: seeded `PAYMENT_ORDER_ALPHA_001_CAPTURED_ID` (capturedAmountMinor = 3 300, ETag `"v2"`)
→ `refundWithAmount(3 301)`.

**Assertions**: 422 `refund_amount_exceeds_captured`, `application/problem+json`,
`correlationId`, `no-store`, `Vary: If-Match`.

**Transaction rollback**: the idempotency record created at step 3 is rolled back; the seeded
order stays at CAPTURED / ETag `"v2"`.

**Business risk verified**: over-refund (returning more than captured) is blocked. Allows
the test to use a stable seeded order without consuming it.

### `zero_refund_amount_returns_422`

**Setup**: same seeded order → `refundWithAmount(0)`.

**Assertions**: identical to over-refund test — 422 `refund_amount_exceeds_captured`.

**Purpose**: confirms the `effectiveAmount <= 0` branch of the domain guard is covered
separately from the `effectiveAmount > capturedAmountMinor` branch. Both are valid
negative-boundary cases that production clients might trigger.

## New API Facade Method

### `PaymentOrdersApi.refundWithAmount()`

```java
public static Response refundWithAmount(
        String merchantId, String paymentOrderId, String ifMatch,
        String idempotencyKey, long amountMinor) {
    return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
            .contentType(ContentTypes.JSON)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body("{\"amountMinor\":" + amountMinor + "}")
            .when()
            .post(REFUND_PATH);
}
```

Complements the existing `refund()` (body `{}`, full refund) by serializing a specific amount.
Uses raw JSON string (same pattern as `authorizeWithReason()` in Phase 8D) — no separate DTO
needed for a single-field addition. Accepts `long` rather than `Long` to prevent callers from
accidentally passing `null` as the amount.

## New `ProblemCodes` Constant

```java
/** 422 — refund amount is invalid: exceeds captured amount or is zero/negative. */
public static final String REFUND_AMOUNT_EXCEEDS_CAPTURED = "refund_amount_exceeds_captured";
```

Confirmed from `PaymentExceptionHandler`:
```java
@ExceptionHandler(InvalidRefundAmountException.class)
public ResponseEntity<PaymentErrorResponse> handleInvalidRefundAmount(...) {
    return problem(HttpStatus.UNPROCESSABLE_ENTITY, ERROR_REFUND_AMOUNT_EXCEEDS_CAPTURED, ...);
}
```

## Deferred

| Item | Reason |
|---|---|
| Negative-amount refund (e.g. `amountMinor = -1`) | Same `effectiveAmount <= 0` branch as zero; not a distinct contract point |
| Partial refund with reason field | `RefundRequest.reason` is nullable and stored but has no contract behaviour differences |
| Second refund after partial refund → 422 `invalid_transition` | Already covered by Phase 7G's generic invalid-transition tests; would require `platformLifecycle` identity to use a REFUNDED seeded order (MERCHANT_ALPHA_002 scope) |
| Refund idempotency replay for partial amount | Same pattern as Phase 8D's `authorize_replay_*`; lower priority given existing coverage |

## HTTP/REST and SDET Topics

**Why does a partial refund still move the order to REFUNDED (terminal state)?**
This is a simplifying design decision in this system. Some payment platforms support
"multiple partial refunds summing to the captured amount"; this one does not. Once `REFUNDED`,
the state machine has no further valid transitions. If multi-step partial refunding were needed,
it would require a separate `refundedAmountMinor` tracker that allows incremental accumulation.
Testers should confirm terminal state behaviour regardless of the business model.

**Why is the amount validation domain-level rather than bean-validation (@Min)?**
`RefundRequest` is a plain record with no `@Valid` on the controller parameter. Domain-level
validation gives richer context: the error message includes both the requested and captured
amounts (`"Refund amount 3301 exceeds captured amount 3300"`). Bean validation at the request
level cannot know the captured amount (it has no access to the DB). This is the correct
separation: syntactic validation (field type, presence) at the boundary; semantic validation
(business rules) in the domain.

**Why does transaction rollback on `InvalidRefundAmountException` matter for test design?**
Because the idempotency record from step 3 is rolled back. The same idempotency key can be
reused after a failed refund — there is no "stuck" idempotency reservation. This means:
- Negative tests can reuse the same seeded order without ordering constraints
- The seeded order (CAPTURED, ETag `"v2"`) is safe to use for multiple negative tests in the
  same spec run because the order state is unchanged after each rollback

**Why assert `refundedAmountMinor` explicitly rather than trusting the status field?**
The status `REFUNDED` only tells us the final state, not how much was refunded. A partial
refund and a full refund both produce `status: "REFUNDED"`. The `refundedAmountMinor` field
is the authoritative financial record — asserting it confirms the backend stored the correct
partial amount, not the full captured amount.

## Test Results

- **79 offline tests**: all pass (unchanged)
- **60 live tests**: all pass (3 new in Phase 8F)
