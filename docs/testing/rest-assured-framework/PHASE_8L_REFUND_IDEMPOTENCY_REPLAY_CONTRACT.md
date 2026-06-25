# Phase 8L — Refund Idempotency Replay Contract

## Scope

Add one focused black-box REST Assured live contract for refund replay semantics.

Covered flow:

1. Create payment order.
2. Authorize with current `If-Match`.
3. Capture with current `If-Match`.
4. Full refund with current `If-Match` and a fresh `Idempotency-Key`.
5. Replay the exact same refund request with the same `Idempotency-Key` and the original refund `If-Match`.

## Added Spec

`LifecycleIdempotencyContractSpec.refund_replay_returns_stable_200_and_does_not_create_duplicate_history_entry`

Assertions:

- first refund returns `200`;
- first refund response uses lifecycle conditional headers through `ResponseSpecs.conditional()`;
- ETag chain is stable and explicit: `"v0"` → `"v1"` → `"v2"` → `"v3"`;
- replay refund returns `200`;
- replay response keeps the same `ETag: "v3"`;
- replay body keeps the same `paymentOrderId`;
- replay body remains `status: REFUNDED`;
- `refundedAmountMinor` remains the original captured amount;
- status history remains exactly `AUTHORIZE`, `CAPTURE`, `REFUND`;
- history contains exactly one `REFUND` entry after replay.

## Backend Behavior Discovered

Refund uses the same lifecycle idempotency guard pattern as authorize and capture:

```text
Controller:
  PaymentEtag.requireVersion(ifMatch)      -> syntactic If-Match requirement
  IdempotencyKey.of(header)
  paymentLifecycleService.refund(...)

Service:
  findOrder(...)
  isIdempotentLifecycleReplay(...)         -> early return on same key + same fingerprint
  PaymentVersionPrecondition.requireCurrentVersion(...)
  reserveIdempotency(...)
  canTransitionTo(REFUNDED)                -> pre-PSP state guard
  pspClient.refund(...)
  order.refund(...)
  recordHistory(... REFUND ...)
```

Important contract point: a replay with the original refund `If-Match: "v2"` is accepted after the first refund has moved the order to `"v3"`. The controller still requires a syntactically valid `If-Match`, but the service detects the replay before semantic version validation.

## Risk Covered

Refund retry is financially sensitive. A client can lose the first response after the PSP refund succeeds, then retry the same HTTP request. This contract proves the backend returns the already-refunded representation without issuing another refund transition or duplicating audit history.

## Test Results

- Offline: `cd apps/api-tests && mvn -q test` — 79 tests passed.
- Live: `cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify` — 70 tests passed.
