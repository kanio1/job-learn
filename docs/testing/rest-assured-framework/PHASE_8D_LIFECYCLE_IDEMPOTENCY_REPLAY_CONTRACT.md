# Phase 8D — Lifecycle Idempotency Replay Contract

## Goal

Verify that repeating a lifecycle action (authorize, capture) with the same `Idempotency-Key`
and identical request body is safe: the backend returns the same stable response, skips the PSP
call, does not increment the ETag, and does not write a duplicate status-history entry.
Also verify that a fingerprint mismatch (same key, same action, different body) produces a
409 idempotency conflict.

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/payment/PaymentOrdersApi.java` | Added `authorizeWithReason()` negative-test variant |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/LifecycleIdempotencyContractSpec.java` | New spec: 3 tests |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8D row added |

## Backend Lifecycle Idempotency Implementation (Discovered)

### Storage

The same `idempotency_records` table as for create operations. For lifecycle actions:

| Column | Value |
|---|---|
| `action` | `"AUTHORIZE"`, `"CAPTURE"`, `"CANCEL"`, or `"REFUND"` |
| `merchant_id` | from JWT `merchant_id` claim |
| `payment_order_id` | from URL path parameter |
| `idempotency_key_hash` | SHA-256 of raw `Idempotency-Key` header value |
| `request_fingerprint_hash` | SHA-256 of canonical fingerprint JSON (see below) |
| `completed_at` | set after action completes |

**DB lookup key scope:** `(merchantId, paymentOrderId, action, idempotencyKeyHash)`.

Contrast with create: `(merchantId, idempotencyKeyHash)` — no `paymentOrderId` or `action`.
This means:
- Same key + same action + different `paymentOrderId` → NOT a conflict (different DB row)
- Same key + different action + same `paymentOrderId` → NOT a conflict (different DB row)
- Same key + same action + same `paymentOrderId` + different body → 409 conflict

### Fingerprint for Lifecycle Actions

`RequestFingerprint.forLifecycle(merchantId, paymentOrderId, action, amountMinor, reason)`:

```json
{
  "operation": "POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize",
  "merchantId": "<uuid>",
  "paymentOrderId": "<uuid>",
  "action": "AUTHORIZE"
  // "amountMinor": <long>    -- included ONLY when non-null (capture/refund with explicit amount)
  // "reason": "<string>"     -- included ONLY when non-null (authorize/cancel/refund with reason)
}
```

For body `{}` (all optional fields absent): neither `amountMinor` nor `reason` is included in
the fingerprint. A replay with body `{}` matches exactly.

Changing `reason` from `null` to any non-null string changes the fingerprint → conflict.
Changing `amountMinor` from `null` to any value changes the fingerprint → conflict.

### Guard Order in Lifecycle Service

```
Controller (fires BEFORE service call):
  PaymentEtag.requireVersion(ifMatch)         → 428 if null/blank; parses "v{N}" → long
  IdempotencyKey.of(idempotencyKeyHeader)     → 400 if blank/missing

Service (fires in this order):
  1. isIdempotentLifecycleReplay()            → early return if same key+fingerprint
  2. PaymentVersionPrecondition.requireCurrentVersion()  → 412 if ETag version stale
  3. reserveIdempotency()                     → insert record; 409 if fingerprint mismatch
  4. pspClient.X()                            → PSP call (authorize/capture/cancel/refund)
  5. order.X()                                → domain state machine; 422 if wrong state
  6. recordHistory()                          → insert status_history row
```

**Critical asymmetry:** `isIdempotentLifecycleReplay()` (step 1) fires BEFORE the version check
(step 2). On replay, the service returns the current order WITHOUT checking the ETag, reserving a
record, calling the PSP, or writing history.

However, the controller calls `PaymentEtag.requireVersion(ifMatch)` BEFORE the service — so
`If-Match` must be syntactically valid even on the replay path. A null or blank `If-Match` still
returns 428.

### Replay Semantics

| Property | Value |
|---|---|
| HTTP status | 200 |
| ETag | `"v{current_version}"` — same as original response (no increment) |
| Body | Current order state — same as original response |
| If-Match | Syntactically required, semantically bypassed (version not checked) |
| PSP call | NOT repeated |
| History entry | NOT written |
| `idempotency_records` row | Reused (not duplicated) |

The client can safely resend the original `If-Match: "v0"` header (the value from the first
request) even though the order is now at version 1 after the first transition. The version check
is bypassed because the replay was detected at step 1.

### Conflict Semantics

| Property | Value |
|---|---|
| HTTP status | 409 |
| Error code | `idempotency_conflict` |
| When it fires | `isIdempotentLifecycleReplay()` — BEFORE version check |
| If-Match | Syntactically required; any valid ETag accepted (version not checked) |
| Trigger | Same key + same action + same `paymentOrderId` + different fingerprint |

Since the conflict fires before the version check, a conflict body sent with a stale If-Match
(e.g. `"v0"` after the order moved to v1) still returns 409, not 412.

## Tests Added

### `authorize_replay_returns_stable_200_and_does_not_create_duplicate_history_entry`

**Setup**: create → authorize(key=K, ifMatch="v0") → verify history has 1 AUTHORIZE entry.

**Replay**: authorize(key=K, ifMatch="v0") — same key, same stale If-Match.

**Assertions**:
- 200, ETag = `"v1"` (same as original), status = `"AUTHORIZED"`
- history still has exactly 1 entry (no duplicate AUTHORIZE row written)

**Business risk verified**: authorize replay does not cause a second PSP authorization charge
and does not confuse the audit log with a phantom second authorization.

### `capture_replay_returns_stable_200_and_does_not_create_duplicate_history_entry`

**Setup**: create → authorize(key=Ka) → capture(key=Kc, ifMatch="v1") → verify history has
2 entries (AUTHORIZE + CAPTURE).

**Replay**: capture(key=Kc, ifMatch="v1") — same key, same stale If-Match.

**Assertions**:
- 200, ETag = `"v2"` (same as original), status = `"CAPTURED"`
- history still has exactly 2 entries (no duplicate CAPTURE row written)

**Business risk verified**: capture replay does not cause a second PSP capture (double settlement)
and does not show two CAPTURE entries in the payment audit log.

### `lifecycle_idempotency_conflict_with_different_fingerprint_returns_409`

**Setup**: create → authorize(key=K, ifMatch="v0", body={}) — establishes fingerprint without `reason`.

**Conflict**: authorize(key=K, ifMatch="v1", body=`{reason:"conflict-reason"}`) — same key,
current ETag, but fingerprint now includes `reason` → different fingerprint → 409.

**Assertions**:
- 409, `application/problem+json`, `error: "idempotency_conflict"`, `no-store`

**Business risk verified**: a client that accidentally reuses an old key for a different
authorization context (e.g., different reason) gets a hard error instead of silently returning
the wrong authorization response.

## New API Facade Method

### `PaymentOrdersApi.authorizeWithReason()`

```java
public static Response authorizeWithReason(
        String merchantId, String paymentOrderId,
        String ifMatch, String idempotencyKey, String reason) {
    return RequestSpecs.lifecycle(ifMatch, idempotencyKey)
            .contentType(ContentTypes.JSON)
            .pathParam("merchantId", merchantId)
            .pathParam("paymentOrderId", paymentOrderId)
            .body("{\"reason\":\"" + reason + "\"}")
            .when()
            .post(AUTHORIZE_PATH);
}
```

Needed because the existing `authorize()` always sends body `{}` (reason=null), which produces
a different fingerprint than `{reason:"..."}` (reason non-null). The conflict test requires both
variants to drive the fingerprint mismatch.

## Data Isolation Strategy

`LifecycleIdempotencyContractSpec` uses:
- `@BeforeAll SeedApi.seed()` — loads deterministic merchants and payment orders; required because
  other spec classes call `SeedApi.reset()` in their own `@AfterAll` which wipes all tables
  including the merchant table. Without `seed()`, the MERCHANT_ALPHA_001_ID merchant would not
  exist and all creates would fail.
- `@AfterAll SeedApi.reset()` — cleans up all orders and idempotency records created by the tests.
- Each test creates its own payment order with a unique `clientOrderReference`
  (`UniqueReferences.paymentRef("...")`) and unique idempotency keys (`IdempotencyKeys.generate("...")`).
  No test depends on another test's data.

## Deferred

| Item | Reason |
|---|---|
| Cancel replay (CREATED → CANCELLED) | Same pattern as authorize replay; lower priority |
| Refund replay (CAPTURED → REFUNDED) | Same pattern; lower priority |
| Lifecycle conflict on capture with different `amountMinor` | Would need `captureWithAmount()` facade variant; lower priority |
| Same key reused for different paymentOrderId → new request (no conflict) | Behavior verified from code; not worth a live test |
| Same key reused for different action on same order → new request (no conflict) | Same reasoning |

## HTTP/REST and SDET Topics

**Why must replay return the same ETag?**
The client's retry loop must not be confused by a changed ETag after a replay. If the ETag
changed on replay, the client's next lifecycle call (e.g. capture) would use the new ETag but
may have cached the old ETag from the original first response, causing 412.

**Why is no history entry written on replay?**
A duplicate history entry would imply the action occurred twice. Payment auditors and compliance
systems interpret each history row as one real business event. A phantom history row from a network
retry would falsely suggest double-authorization, double-capture, or double-refund.

**Why does the replay check fire BEFORE the version check?**
The idempotency replay is a fast-path return. The version check and state machine are only
relevant for new operations. Checking the version before the replay detection would mean a client
with a stale ETag (from before the first call) could not safely retry — 412 would be returned
even though the request already succeeded. This would break the fundamental retry safety that
idempotency is designed to provide.

**Why must If-Match be syntactically valid on replay?**
The controller parses `If-Match` before calling the service. This is a Spring MVC design decision
(the controller is responsible for extracting and validating all headers before dispatching to the
service layer). It would be possible to move the If-Match parsing into the service, but that would
require changing the controller's contract. The current design is correct: require the header
syntactically (so callers don't accidentally omit it), but bypass the semantic version check on replay.

**Why is the conflict scope `(merchantId, paymentOrderId, action, idempotencyKeyHash)` and not just `(merchantId, idempotencyKeyHash)`?**
For create operations, the key is merchant-scoped: the merchant created exactly one payment order
with this key. For lifecycle operations, the context is narrower: the key is scoped to one specific
payment order and one specific action. This allows the same key to be reused for different lifecycle
steps without conflict — each step occupies its own idempotency slot.

## Test Results

- **79 offline tests**: all pass (unchanged)
- **53 live tests**: all pass (3 new in Phase 8D)
