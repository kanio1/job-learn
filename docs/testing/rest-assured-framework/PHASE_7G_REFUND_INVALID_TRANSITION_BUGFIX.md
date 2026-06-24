# Phase 7G — Refund Invalid-Transition Bug Fix and Regression Coverage

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 36 live specs
> (1 status + 2 security smoke + 13 merchant contract + 20 payment order contract).

---

## Summary

Phase 7G fixes a backend bug discovered in Phase 7F: attempting a refund on a non-captured
(e.g. AUTHORIZED) payment order with an empty body (`{}`) produced an unhandled
`NullPointerException` → 500, instead of the expected `InvalidStateTransitionException` → 422
`invalid_transition`. One line of defensive guard was added to
`PaymentLifecycleService.refund()` before the PSP call. The fix is minimal, preserves all
existing behavior, and is covered by a new black-box regression spec.

---

## Bug Analysis

### Location

`apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`,
method `refund()`.

### Faulty execution path (pre-fix)

```java
// 1. Version check: passes (ETag "v1" matches AUTHORIZED order version 1)
PaymentVersionPrecondition.requireCurrentVersion(order, expectedVersion);
PaymentStatus previousStatus = order.getStatus();   // = AUTHORIZED

// 2. Idempotency reservation: inserts a row (transaction is open)
if (!reserveIdempotency(...)) { return order; }

// 3. PSP call — THE BUG IS HERE
//    amountMinor is null (body was `{}`)
//    order.getCapturedAmountMinor() is null (order never reached CAPTURED)
//    ternary evaluates to null (the Long object)
//    pspClient.refund() takes a primitive `long` parameter
//    auto-unboxing null -> NullPointerException -> 500
PspClient.PspResult pspResult = pspClient.refund(paymentOrderId,
        amountMinor != null ? amountMinor : order.getCapturedAmountMinor(), // <- null
        order.getCurrency());

// 4. Domain state check — NEVER REACHED
//    This would have thrown InvalidStateTransitionException -> 422
order.refund(amountMinor, reason);
```

### Affected states

Any source state except CAPTURED, combined with an empty body (`amountMinor = null`):

| Source state | `capturedAmountMinor` | Pre-fix result | Post-fix result |
|---|---|---|---|
| CREATED | `null` | 500 NPE | 422 `invalid_transition` |
| AUTHORIZED | `null` | 500 NPE | 422 `invalid_transition` |
| CANCELLED | `null` | 500 NPE | 422 `invalid_transition` |
| CAPTURED | set | 200 REFUNDED | 200 REFUNDED (unchanged) |
| REFUNDED | set | 422 `invalid_transition` | 422 `invalid_transition` (unchanged) |

### Why capture and cancel were unaffected

`PaymentLifecycleService.capture()` uses `order.getAmountMinor()` (always non-null) as the
fallback when `amountMinor` is null — no NPE risk.

`PaymentLifecycleService.cancel()` does not use `capturedAmountMinor` at all.

---

## The Fix

### Change

`apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`

```diff
         if (!reserveIdempotency(merchantId, paymentOrderId, idempotencyKeyHash,
                 PaymentLifecycleAction.REFUND, amountMinor, reason)) {
             return order;
         }

+        // Guard: verify the state transition is valid before calling the PSP.
+        // Without this check, orders without a captured amount (e.g. AUTHORIZED) would reach
+        // pspClient.refund() with a null amount (getCapturedAmountMinor() == null), causing NPE
+        // before order.refund() could fire its own canTransitionTo() guard.
+        if (!order.canTransitionTo(PaymentStatus.REFUNDED)) {
+            throw new InvalidStateTransitionException(order.getStatus(), PaymentStatus.REFUNDED);
+        }
+
         PspClient.PspResult pspResult = pspClient.refund(paymentOrderId,
                 amountMinor != null ? amountMinor : order.getCapturedAmountMinor(), order.getCurrency());
         order.refund(amountMinor, reason);
```

### Why this specific fix

1. **Minimal** — one guard block, no structural changes.
2. **Same exception** — `InvalidStateTransitionException` is already thrown by `order.refund()` internally; the pre-check throws it earlier, before any null dereference.
3. **Same HTTP mapping** — `PaymentExceptionHandler.handleInvalidStateTransition()` already maps `InvalidStateTransitionException` → 422 `invalid_transition` with `preconditionHeaders()` (`Vary: If-Match`, `Cache-Control: no-store`).
4. **Transaction safety** — the guard fires inside the `@Transactional` boundary; if it throws, the idempotency record inserted by `reserveIdempotency()` is rolled back correctly.
5. **Consistent with domain** — `canTransitionTo()` is a pure read with no side effects. The pre-check is redundant for CAPTURED orders (where `order.refund()` would pass anyway) and protective for all other states.

### Why not move `order.refund()` before the PSP call instead?

`order.refund()` modifies the entity (sets status, timestamps, amounts). Moving it before the
PSP call would mutate domain state before the PSP transaction — if the PSP call then failed,
the order would be in REFUNDED state with no corresponding PSP settlement. The correct
transactional pattern for all lifecycle actions in this codebase is: PSP call first, domain
mutation second. The pre-check preserves this pattern while adding the missing guard.

---

## Regression Test Added

### Test

`PaymentOrdersContractSpec.refund_on_authorized_order_returns_422_invalid_transition`
(Phase 7G regression section)

### Chain

create (v0) → authorize (v1) → refund attempt

### Assertions

- HTTP status 422
- Content-Type `application/problem+json`
- `error: "invalid_transition"`
- `Cache-Control: no-store`
- `Vary` contains `If-Match`

### Why two steps (create + authorize) rather than one step (create only)?

Testing refund from AUTHORIZED state is more representative of the real-world client error:
a merchant authorized a payment and then accidentally called refund before capture. Testing
from CREATED would also trigger 422, but AUTHORIZED is the most likely state where this
mistake occurs in production.

---

## Validation

### Backend payment module tests (non-restkit)

```bash
cd apps/backend
./mvnw test -Dtest="PaymentOrderServiceTest,PaymentModuleTest,PaymentAmountTest,CurrencyCodeTest,ClientOrderReferenceTest,IdempotencyKeyTest"
# Result: 36 tests, BUILD SUCCESS
```

### Backend image rebuild

```bash
# Rootless Podman — uses the podman-build-image Maven profile documented in pom.xml
cd apps/backend
./mvnw -Ppodman-build-image spring-boot:build-image \
  -DskipTests \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local
# Result: Successfully built image 'docker.io/payment-quality/backend:local'
```

### Offline api-tests

```bash
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS
```

### Live api-tests

```bash
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 36 IT specs (1 status + 2 security smoke + 13 merchant + 20 payment order), BUILD SUCCESS
```

---

## Files Changed

| File | Change |
|---|---|
| `apps/backend/.../PaymentLifecycleService.java` | Added `canTransitionTo(REFUNDED)` pre-check before PSP call |
| `apps/api-tests/.../PaymentOrdersContractSpec.java` | Added Phase 7G regression test (20 total payment order specs) |
| `docs/.../PHASE_7G_REFUND_INVALID_TRANSITION_BUGFIX.md` | This document |

---

## Deferred to Phase 7H+

| Item | Reason |
|---|---|
| Refund from CREATED state (also gets 422 post-fix) | Same pattern; add once value is clear |
| Partial refund contract (`amountMinor` in body) | Needs `refundWithAmount()` facade + amount assertions |
| Over-refund → 422 `refund_amount_exceeds_captured` | New ProblemCode needed |
| Lifecycle idempotency replay (refund same key → 200) | Same as authorize/capture idempotency |
| `GET /history` after full lifecycle | Audit-level data; requires multi-step chain |
