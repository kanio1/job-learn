# Phase 7I — Concurrent Create Idempotency Race Fix and Regression Coverage

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 38 live specs
> (1 status + 2 security smoke + 13 merchant contract + 22 payment order contract).

---

## Summary

Phase 7I fixes the backend bug deferred from Phase 7H: `PaymentOrderService.create()` threw an
unhandled `IllegalStateException` ("Idempotency record is not completed") when a concurrent
second request found an idempotency record with `paymentOrderId == null`, producing a 500
response. The fix replaces the unhandled exception with `IdempotencyCreateInProgressException`
mapped to 409 `create_in_progress`. Two backend unit tests cover both affected code paths.
One black-box regression spec verifies that concurrent same-key creates never produce 500.

---

## Root Cause Analysis

### Two-phase idempotency write

`PaymentOrderService.create()` writes the idempotency record in two phases within a single
`@Transactional` scope:

```
@Transactional
create() {
    Phase 1: reserveIfAbsent()   INSERT idempotency_records (paymentOrderId = NULL)
    Phase 2: create payment order
             create history entry
    Phase 3: complete()          UPDATE idempotency_records SET paymentOrderId = X
    COMMIT (all three phases together)
}
```

### Normal-case PostgreSQL serialisation

The partial unique index `uk_idempotency_records_create_scope` on
`(merchant_id, idempotency_key_hash, action) WHERE action = 'CREATE'` causes PostgreSQL to
block B's `INSERT ON CONFLICT DO NOTHING` until A's full transaction commits. By the time B
unblocks and gets `reserved = 0`, A has committed Phase 3, so `paymentOrderId` is set:

```
A: INSERT (paymentOrderId=null) ─────────── COMMIT (paymentOrderId=X)
B:                          INSERT BLOCKS ──┘ (0 rows, conflict)
B:                                                  read record → paymentOrderId = X ✓
```

### Scenarios where the null path IS reachable

| Scenario | Why null is visible |
|---|---|
| Autocommit misconfiguration | Each SQL statement commits independently; A's INSERT commits before `complete()` runs |
| Future refactoring | Two-phase write split across separate transactions |
| Partial infrastructure failure | A's `complete()` UPDATE fails after INSERT commit (rare, requires REQUIRES_NEW propagation) |
| Application restart between phases | Crash after INSERT but before `complete()`; next restart doesn't clean up partial records |

Even in the "impossible" normal-case analysis, the defensive fix is correct: a 500 from
`IllegalStateException` is always wrong. Replacing it with a controlled 409 gives the client
actionable information.

### Two affected code paths

**Path 1 — initial lookup finds null paymentOrderId (lines 54–58):**
```java
if (existing.isPresent()) {
    IdempotencyRecord record = existing.get();
    if (record.getRequestFingerprintHash().equals(fingerprint.fingerprintHash())) {
        // B reads the record with paymentOrderId == null:
        PaymentOrder order = paymentOrderRepository.findByPaymentOrderId(record.getPaymentOrderId())
                // → findByPaymentOrderId(null) → WHERE payment_order_id = null → empty
                .orElseThrow(() -> new IllegalStateException(...));  // 500
```

**Path 2 — resolveExistingIdempotencyRecord finds null paymentOrderId (line 102):**
```java
UUID paymentOrderId = record.getPaymentOrderId();
if (paymentOrderId == null) {
    throw new IllegalStateException("Idempotency record is not completed");  // 500
}
```

---

## The Fix

### New exception

`IdempotencyCreateInProgressException` (domain package):

```java
public class IdempotencyCreateInProgressException extends RuntimeException {
    public IdempotencyCreateInProgressException() {
        super("A concurrent create for this idempotency key is still in progress; retry with the same key and body");
    }
}
```

### PaymentOrderService changes

**Path 1 (initial lookup):** extract `getPaymentOrderId()` and null-check before use:
```diff
-        PaymentOrder order = paymentOrderRepository.findByPaymentOrderId(record.getPaymentOrderId())
+        UUID existingPaymentOrderId = record.getPaymentOrderId();
+        if (existingPaymentOrderId == null) {
+            throw new IdempotencyCreateInProgressException();
+        }
+        PaymentOrder order = paymentOrderRepository.findByPaymentOrderId(existingPaymentOrderId)
```

**Path 2 (resolveExistingIdempotencyRecord):** replace `IllegalStateException`:
```diff
 if (paymentOrderId == null) {
-    throw new IllegalStateException("Idempotency record is not completed");
+    throw new IdempotencyCreateInProgressException();
 }
```

### PaymentExceptionHandler change

```java
private static final String ERROR_IDEMPOTENCY_CREATE_IN_PROGRESS = "create_in_progress";

@ExceptionHandler(IdempotencyCreateInProgressException.class)
public ResponseEntity<PaymentErrorResponse> handleIdempotencyCreateInProgress(
        IdempotencyCreateInProgressException ex, HttpServletRequest request) {
    return problem(HttpStatus.CONFLICT, ERROR_IDEMPOTENCY_CREATE_IN_PROGRESS, ex.getMessage(),
            headersForRequest(request));
}
```

For create requests, `headersForRequest(request)` returns `createHeaders()` →
`Vary: Authorization, Idempotency-Key`, `Cache-Control: no-store` — the same headers as
`idempotency_conflict`.

### Why 409 rather than 503

409 Conflict is consistent with the idempotency error family already in use in this codebase
(`idempotency_conflict` is also 409). A client already handling 409 idempotency responses can
handle `create_in_progress` with the same retry logic. 503 Service Unavailable would be more
semantically precise ("infrastructure momentarily busy"), but it introduces a new HTTP status
class that would require updates to every API client's error handling.

### Client recovery path

1. Client receives `409 create_in_progress`
2. Client waits briefly (1–5 seconds is sufficient given the transaction duration)
3. Client retries the **same request** with the **same Idempotency-Key** and **same body**
4. On retry:
   - If the first request succeeded: 200 replay (idempotency)
   - If the first request failed and rolled back: 201 new create

---

## Acceptable Black-Box Test Outcomes

| Status codes | Interpretation |
|---|---|
| `[201, 200]` | Normal concurrent idempotency replay — PostgreSQL serialised correctly (most common) |
| `[201, 409] create_in_progress` | Race window was hit; fix converts 500 → 409 |
| `[201, 409] idempotency_conflict` | Extreme edge case; both hit the initial lookup simultaneously before either reserved |
| `[500, *]` | **ALWAYS WRONG** — the bug this phase fixes |
| `[201, 201]` | **ALWAYS WRONG** — double create; would indicate idempotency is broken |

---

## Files Changed

### Backend

| File | Change |
|---|---|
| `domain/IdempotencyCreateInProgressException.java` | New exception class |
| `application/PaymentOrderService.java` | Guard null `paymentOrderId` in both code paths |
| `web/PaymentExceptionHandler.java` | `ERROR_IDEMPOTENCY_CREATE_IN_PROGRESS` constant + `@ExceptionHandler` |
| `application/PaymentOrderServiceTest.java` | 2 new unit tests: null-in-resolve-path + null-in-initial-lookup-path |

### api-tests

| File | Change |
|---|---|
| `core/problem/ProblemCodes.java` | `IDEMPOTENCY_CREATE_IN_PROGRESS = "create_in_progress"` |
| `scenarios/PaymentOrdersContractSpec.java` | Phase 7I regression test (22 total payment order specs) |
| `docs/.../PHASE_7I_CONCURRENT_CREATE_IDEMPOTENCY_BUGFIX.md` | This document |

---

## Validation

```bash
# Backend unit tests (including 2 new in-progress tests)
cd apps/backend
./mvnw test -Dtest="PaymentOrderServiceTest,PaymentModuleTest" --no-transfer-progress
# Result: 12 tests, BUILD SUCCESS (10 service + 2 module)

# Backend image rebuild
cd apps/backend
./mvnw -Ppodman-build-image spring-boot:build-image -DskipTests \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local --no-transfer-progress
# Result: Successfully built image 'docker.io/payment-quality/backend:local'

# Offline api-tests
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live api-tests
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 38 IT specs (1 status + 2 security smoke + 13 merchant + 22 payment order), BUILD SUCCESS
# Phase 7I test outcome: [201, 200] — PostgreSQL blocking serialised correctly
```

---

## Deferred

| Item | Reason |
|---|---|
| Retry-After header on 409 `create_in_progress` | Requires a new `Retry-After` value; low priority since the race window is narrow |
| Cleanup of orphaned idempotency records (reserved but never completed) | Requires a scheduled cleanup job; separate concern from the fix |
| `create_in_progress` → automatic retry in client | Client-side concern; out of scope for this API test suite |
