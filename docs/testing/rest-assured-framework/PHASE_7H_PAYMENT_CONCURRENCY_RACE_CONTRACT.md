# Phase 7H — Payment Concurrency and Optimistic-Lock Race Contract

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 37 live specs
> (1 status + 2 security smoke + 13 merchant contract + 21 payment order contract).

---

## Summary

Phase 7H adds one deterministic concurrency test: two threads fire concurrent `POST .../authorize`
requests on the same CREATED payment order, using different Idempotency-Keys but the same
`If-Match: "v0"`. A `CyclicBarrier(2)` releases both threads simultaneously to maximize the race
window. The outcome is deterministic on categories — exactly one 200, exactly one 412, no 500 —
regardless of which thread wins. In the observed live run the `CyclicBarrier` achieved true
concurrent overlap, producing the `concurrency_conflict` 412 sub-type.

---

## Backend Concurrency Architecture (Discovered)

### Dual-layer optimistic locking

```
HTTP Request
    │
    ▼
PaymentLifecycleService.authorize()
    │
    1. isIdempotentLifecycleReplay()        ← idempotency pre-check (SELECT)
    │
    2. PaymentVersionPrecondition            ← service-level ETag pre-check
    │   .requireCurrentVersion(order, v)    ← reads order.getVersion() loaded at tx start
    │   throws PaymentOrderVersionMismatch  ← → 412 payment_order_version_mismatch
    │
    3. reserveIdempotency()                  ← INSERT ON CONFLICT DO NOTHING
    │   idempotency_records (unique key)
    │
    4. pspClient.authorize()                 ← external PSP call
    │
    5. order.authorize()                     ← mutates JPA entity in memory
    │
    6. @Transactional commit / JPA flush     ← UPDATE payment_orders
           SET version = v+1, status = 'AUTHORIZED'
           WHERE id = X AND version = v
           ← if 0 rows: ObjectOptimisticLockingFailureException
               → PaymentExceptionHandler.handleOptimisticLock()
               → 412 concurrency_conflict
```

### Two distinct 412 sub-types

| Code | When triggered | Layer |
|---|---|---|
| `payment_order_version_mismatch` | Request B reads the order AFTER Request A has committed. B's in-memory `order.getVersion()` is 1; `requireCurrentVersion(1, expected=0)` fails immediately, before any DB write. | Service pre-check |
| `concurrency_conflict` | Both A and B read the order BEFORE either commits. Both see version=0 and pass the pre-check. JPA flushes both with `UPDATE WHERE version=0`; one wins, the other finds 0 rows → `ObjectOptimisticLockingFailureException` at commit time. | JPA flush |

Both map to HTTP 412. Client recovery is identical for both: re-read the resource, get the new
ETag, retry.

### Why different Idempotency-Keys are required

The idempotency subsystem uses `INSERT ... ON CONFLICT DO NOTHING` on a unique constraint
`(merchantId, paymentOrderId, action, idempotencyKeyHash)`. With the **same** key, one thread
wins the insert and the other thread's `reserveIdempotency()` returns `false` (replay path),
which returns the current order state early — testing idempotency semantics, not the version race.
With **different** keys, both threads insert different rows (no conflict), both proceed to the
PSP call, and the race is decided by the JPA `@Version` counter.

### Why two 200s are impossible

JPA's optimistic lock generates:
```sql
UPDATE payment_orders
SET    version = 1, status = 'AUTHORIZED', ...
WHERE  payment_order_id = X AND version = 0
```
PostgreSQL executes this statement atomically. Only one transaction can match `version = 0` and
return 1 updated row; the second gets 0 rows and JPA throws `ObjectOptimisticLockingFailureException`.
This is a database-level guarantee independent of application timing.

### Idempotency record rollback on losing transaction

When the losing transaction throws `ObjectOptimisticLockingFailureException` at JPA flush, the
entire `@Transactional` scope rolls back. This includes the idempotency record row that was inserted
in step 3. After rollback, the DB contains only the winner's idempotency record. The loser's key
is effectively "never used" — a client that receives 412 `concurrency_conflict` can safely retry
with the same key (or a new one) after re-reading the ETag.

---

## Test Design

### Scenario

`concurrent_authorize_with_different_idempotency_keys_yields_one_success_and_one_412`

```
Thread setup:
    POST /api/merchants/{merchantId}/payment-orders   → 201 CREATED, ETag "v0"

Race:
    Thread A (key idempotencyKeyA)     Thread B (key idempotencyKeyB)
    ──────────────────────────────     ──────────────────────────────
    Ctx.set(ctx)                       Ctx.set(ctx)
    barrier.await()  ◄──── sync ────► barrier.await()
    POST .../authorize                 POST .../authorize
      If-Match: "v0"                     If-Match: "v0"
      Idempotency-Key: keyA              Idempotency-Key: keyB

Result (one of):
    A → 200 AUTHORIZED                 B → 412 concurrency_conflict   (true race)
    A → 200 AUTHORIZED                 B → 412 payment_order_..._mismatch  (sequential)

Verification:
    GET .../payment-orders/{id}  → 200 AUTHORIZED, ETag "v1"
```

### Assertions

| Assertion | Rationale |
|---|---|
| `statusCodes == [200, 412]` | Exactly one success, one safe failure — no 500, no double-200 |
| 412 error ∈ {`concurrency_conflict`, `payment_order_version_mismatch`} | Both are valid outcomes depending on true/sequential overlap; both mean "re-read and retry" |
| Final `status == AUTHORIZED` | Only one authorize committed |
| Final ETag version `== 1` | Exactly one version increment — no double-mutation |

### Why `CyclicBarrier`

`CyclicBarrier(2)` blocks both threads until both have called `barrier.await()`. This makes the
HTTP requests fire as close together in time as the OS scheduler allows, maximising the probability
that both reach the `UPDATE WHERE version=0` before either commits — the true-concurrent path
that exercises `concurrency_conflict`. Without the barrier, requests are likely to be sequential,
which is a weaker test (only exercises the service-level pre-check). Both paths are valid; the
barrier makes the stronger path more likely.

### Thread context propagation

`Ctx` uses `ThreadLocal`. The test captures the `TestContext` before spawning threads and calls
`Ctx.set(capturedCtx)` inside each `Callable` body. This ensures `AuthFilter` (which reads
`Ctx.currentOrNull()`) injects the correct `Authorization: Bearer` header on each spawned thread.

---

## Observed Live Run

```
"type": "https://api.payment-quality.local/problems/concurrency-conflict",
"error": "concurrency_conflict"
```

The `CyclicBarrier` achieved true concurrent overlap on the first run — both requests were in
flight simultaneously, and the JPA flush path produced `concurrency_conflict`. This confirms
that the backend's `handleOptimisticLock()` handler is exercised correctly.

---

## Why Idempotency Create Race Is Deferred

A `concurrent create with same Idempotency-Key` scenario was considered and rejected:

```java
// PaymentOrderService.resolveExistingIdempotencyRecord()
UUID paymentOrderId = record.getPaymentOrderId();
if (paymentOrderId == null) {
    throw new IllegalStateException("Idempotency record is not completed");
}
```

The `idempotency_records` table has a two-phase write: `reserveIfAbsent()` inserts the row
without `payment_order_id`, then `complete()` updates it. Between these two writes, a concurrent
reader that finds the row sees `paymentOrderId == null` and throws `IllegalStateException` → 500.
The race window is narrow in practice but non-zero, making the test potentially flaky. Deferred
until the backend adds a waiting/polling mechanism or uses a single atomic insert.

---

## Validation

```bash
# Offline
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 37 IT specs (1 status + 2 security smoke + 13 merchant + 21 payment order), BUILD SUCCESS
# Observed: 412 error = "concurrency_conflict" (true-concurrent path)
```

---

## Files Changed

| File | Change |
|---|---|
| `core/problem/ProblemCodes.java` | Added `CONCURRENCY_CONFLICT = "concurrency_conflict"` with full Javadoc |
| `scenarios/PaymentOrdersContractSpec.java` | Added Phase 7H concurrency test (21 total payment order specs) |
| `docs/.../PHASE_7H_PAYMENT_CONCURRENCY_RACE_CONTRACT.md` | This document |

---

## Deferred to Phase 7I+

| Item | Reason |
|---|---|
| Concurrent create with same Idempotency-Key | Race window on `paymentOrderId == null` → potential 500; needs backend two-phase idempotency protection |
| Concurrent capture on same AUTHORIZED order | Same dual-412 pattern as authorize; add once authorize race is well understood |
| Concurrent lifecycle with same Idempotency-Key | Tests idempotency replay race; separate concern from version race |
| PSP call count assertion | Cannot be asserted at the black-box HTTP layer; belongs in unit/integration tests that mock `PspClient` |
| Retry-with-backoff pattern | A client that receives 412 `concurrency_conflict` should back off and retry; tests deferred to contract-level client spec |
