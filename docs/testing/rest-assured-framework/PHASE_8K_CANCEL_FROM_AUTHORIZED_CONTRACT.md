# Phase 8K — Cancel From AUTHORIZED Contract

## Goal

Add one focused black-box live spec proving that an AUTHORIZED payment order can be cancelled:
`CREATED → AUTHORIZED → CANCELLED`, with the expected ETag chain, response fields, conditional
headers, and synchronous status history.

## Backend Behavior Discovered

State machine:

```text
CREATED    -> AUTHORIZED, CANCELLED
AUTHORIZED -> CAPTURED, CANCELLED, EXPIRED
CAPTURED   -> REFUNDED
```

`PaymentLifecycleService.cancel()` behavior:

1. Finds the payment order.
2. Checks lifecycle idempotency replay before version validation.
3. Validates the current version against `If-Match`.
4. Reserves the lifecycle idempotency record.
5. If the previous status is `AUTHORIZED`, calls `pspClient.voidAuthorization(...)`.
6. Calls `order.cancel(reason)`, which transitions to `CANCELLED`, sets `cancelledAt`, clears
   `expiresAt`, and updates the entity timestamp.
7. Records status history with action `CANCEL`.
8. Publishes `PAYMENT_CANCELLED`.

Response behavior:

- Status: `200 OK`
- ETag chain: create `"v0"` -> authorize `"v1"` -> cancel `"v2"`
- Headers: lifecycle response uses `Cache-Control: no-store`, `Vary: If-Match`, and
  `X-Correlation-ID`
- Body: `status = "CANCELLED"`, `cancelledAt` non-null, `authorizedAt` remains non-null,
  `expiresAt` cleared, capture/refund timestamps and amounts remain null
- History: synchronous `GET .../history` returns `AUTHORIZE` then `CANCEL` in chronological order

## Test Added

`PaymentOrdersContractSpec.create_authorize_cancel_happy_path_returns_200_and_records_history`

Flow:

1. Create a fresh payment order and assert `ETag: "v0"`.
2. Authorize with `If-Match: "v0"` and assert `ETag: "v1"`.
3. Cancel with `If-Match: "v1"` and a fresh `Idempotency-Key`.
4. Assert `200`, `ResponseSpecs.conditional()`, `status: CANCELLED`, `cancelledAt` non-null,
   `expiresAt` null, and capture/refund fields null.
5. Read history and assert exactly two entries:
   `CREATED -> AUTHORIZED` with action `AUTHORIZE`, then
   `AUTHORIZED -> CANCELLED` with action `CANCEL`.

## Files Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/PaymentOrdersContractSpec.java` | Added one cancel-from-AUTHORIZED live spec |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Added Phase 8K row and removed cancel-from-AUTHORIZED from deferred row |
| `docs/testing/rest-assured-framework/PHASE_8K_CANCEL_FROM_AUTHORIZED_CONTRACT.md` | Added this phase note |

## Test Results

- **79 offline tests**: all pass
- **69 live tests**: all pass

Commands run:

```bash
cd apps/api-tests && mvn -q test
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
```
