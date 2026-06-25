# Phase 8J — PATCH Stale ETag Contract

## Goal

Add one focused black-box contract test proving that
`PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` with a stale
`If-Match` returns `412 payment_order_version_mismatch`.

## Test Added

`PatchMetadataContractSpec.patch_with_stale_if_match_returns_412_payment_order_version_mismatch`

Flow:

1. Create a fresh payment order and capture its initial `ETag: "v0"`.
2. PATCH metadata with `If-Match: "v0"` and a valid merge-patch body.
3. Assert the successful PATCH returns 200 and increments the ETag to `"v1"`.
4. PATCH again with another valid metadata body but reuse stale `If-Match: "v0"`.
5. Assert the response is `412 application/problem+json` with
   `error: "payment_order_version_mismatch"`, `Cache-Control: no-store`,
   `Vary: If-Match`, a correlation ID, and a body matching `problem.schema.json`.

## Discovery

This confirms the PATCH guard order documented in Phase 8E:

- Unsupported content type still fails first at Spring MVC dispatch with 415.
- Unknown top-level fields fail before ETag evaluation with 400.
- A structurally valid merge-patch body reaches the version precondition.
- If the supplied `If-Match` is stale, the backend returns
  `412 payment_order_version_mismatch`.

The backend consistently sets `Vary: If-Match` on this stale-version PATCH problem response.

## Files Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/PatchMetadataContractSpec.java` | Added one stale-ETag PATCH contract test |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Added Phase 8J row and removed stale PATCH item from deferred row |
| `docs/testing/rest-assured-framework/PHASE_8J_PATCH_STALE_ETAG_CONTRACT.md` | Added this phase note |

## Test Results

- **79 offline tests**: all pass
- **68 live tests**: all pass

Commands run:

```bash
cd apps/api-tests && mvn -q test
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
```
