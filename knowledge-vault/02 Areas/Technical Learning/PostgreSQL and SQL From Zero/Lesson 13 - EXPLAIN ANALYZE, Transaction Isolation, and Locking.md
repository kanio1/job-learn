---
type: lesson
status: planned
area: PostgreSQL and SQL From Zero
lesson: 13
module: EXPLAIN ANALYZE, Transaction Isolation, and Locking
date: 2026-05-31
tags:
  - sql
  - postgresql
  - explain
  - transaction-isolation
  - locking
  - lesson-13
  - senior-sdet
---

# Lesson 13 — EXPLAIN ANALYZE, Transaction Isolation, and Locking

> **Evidence link:** `JpaPaymentOrderRepository.java`, `PaymentOrderTransactionTest.java` (planned), `PaymentOrderLockingTest.java` (planned)
>
> **Navigation:** [[PostgreSQL and SQL From Zero MOC]] | [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się zaawansowanych PostgreSQL features dla performance i concurrency:
- **EXPLAIN ANALYZE** — query performance diagnostics
- **Transaction isolation levels** — READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE
- **Locking** — pessimistic vs optimistic locking
- **Deadlock detection** — wykrywanie i zapobieganie

## 2. Prerequisites

- Basic SQL (SELECT, WHERE, ORDER BY, GROUP BY) — Lesson 06-08.
- @Transactional basics (Lesson 06).
- Indexes (Lesson 07).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `JpaPaymentOrderRepository.java` | JPQL queries (do EXPLAIN ANALYZE) |
| `V2__create_payment_orders.sql` | Table structure, indexes |
| `V3__add_payment_order_list_indexes.sql` | List indexes |
| `PaymentOrderTransactionTest.java` | Transaction isolation tests |
| `PaymentOrderLockingTest.java` | Locking tests |

## 4. Kluczowe Pojęcia

### 4.1 EXPLAIN ANALYZE — Query Performance Diagnostics

```sql
-- EXPLAIN: query plan (bez execution)
EXPLAIN SELECT * FROM payment_orders WHERE merchant_id = '123' AND status = 'CREATED';

-- Output:
-- Seq Scan on payment_orders  (cost=0.00..10.50 rows=5 width=100)
--   Filter: (merchant_id = '123'::uuid AND status = 'CREATED'::payment_status)

-- EXPLAIN ANALYZE: query plan + actual execution time
EXPLAIN ANALYZE SELECT * FROM payment_orders WHERE merchant_id = '123' AND status = 'CREATED';

-- Output:
-- Seq Scan on payment_orders  (cost=0.00..10.50 rows=5 width=100) (actual time=0.015..0.020 rows=3 loops=1)
--   Filter: (merchant_id = '123'::uuid AND status = 'CREATED'::payment_status)
--   Rows Removed by Filter: 97
-- Planning Time: 0.100 ms
-- Execution Time: 0.050 ms
```

**Co czytać w EXPLAIN output:**

| Element | Znaczenie |
|---|---|
| `Seq Scan` | Sequential scan (czyta wszystkie rows, wolne dla large tables) |
| `Index Scan` | Index scan (czyta tylko matching rows, szybkie) |
| `cost=0.00..10.50` | Estimated cost (first row..total rows) |
| `actual time=0.015..0.020` | Actual execution time (first row..total rows) |
| `rows=5` | Estimated number of rows |
| `rows=3` (actual) | Actual number of rows |
| `Rows Removed by Filter: 97` | Ile rows odrzuconych przez WHERE |
| `Planning Time: 0.100 ms` | Czas planowania query |
| `Execution Time: 0.050 ms` | Czas wykonania query |

**Seq Scan vs Index Scan:**
- **Seq Scan:** Czyta wszystkie rows w table (wolne dla large tables, > 10k rows)
- **Index Scan:** Czyta tylko matching rows używając index (szybkie, < 1ms dla selective queries)

**Kiedy PostgreSQL używa Seq Scan vs Index Scan?**
- **Seq Scan:** Gdy query zwraca > 10-20% rows w table (index scan nie jest faster)
- **Index Scan:** Gdy query zwraca < 10-20% rows w table (selective query)

**Jak wymusić Index Scan?**
```sql
-- Dodaj index:
CREATE INDEX idx_payment_orders_merchant_status ON payment_orders (merchant_id, status);

-- Query używa index:
EXPLAIN ANALYZE SELECT * FROM payment_orders WHERE merchant_id = '123' AND status = 'CREATED';
-- Output: Index Scan using idx_payment_orders_merchant_status on payment_orders
```

### 4.2 Transaction Isolation Levels

```java
// READ_COMMITTED (default w PostgreSQL):
// - Widzi tylko committed changes
// - Może widzieć phantom reads (new rows inserted by other transactions)
@Transactional(isolation = Isolation.READ_COMMITTED)
void testReadCommitted() {
    // Transaction 1: reads data
    List<PaymentOrder> orders1 = repository.findAll();
    
    // Transaction 2: inserts new row and commits
    // (w innym thread)
    
    // Transaction 1: reads again
    List<PaymentOrder> orders2 = repository.findAll();
    // orders2.size() > orders1.size() (phantom read)
}

// REPEATABLE_READ:
// - Widzi tylko committed changes z momentu startu transaction
// - Nie widzi phantom reads
@Transactional(isolation = Isolation.REPEATABLE_READ)
void testRepeatableRead() {
    // Transaction 1: reads data
    List<PaymentOrder> orders1 = repository.findAll();
    
    // Transaction 2: inserts new row and commits
    // (w innym thread)
    
    // Transaction 1: reads again
    List<PaymentOrder> orders2 = repository.findAll();
    // orders2.size() == orders1.size() (no phantom read)
}

// SERIALIZABLE:
// - Pełna izolacja (jakby transactions były serial)
// - Najwolniejsze (najwięcej locks)
@Transactional(isolation = Isolation.SERIALIZABLE)
void testSerializable() {
    // Transaction 1: reads data
    List<PaymentOrder> orders1 = repository.findAll();
    
    // Transaction 2: inserts new row and commits
    // (w innym thread)
    
    // Transaction 1: reads again
    List<PaymentOrder> orders2 = repository.findAll();
    // orders2.size() == orders1.size() (no phantom read)
    
    // Transaction 1: updates data
    orders1.get(0).setStatus("AUTHORIZED");
    repository.save(orders1.get(0));
    
    // Transaction 1: commits
    // Może fail jeśli Transaction 2 conflicting (serialization failure)
}
```

**Isolation levels comparison:**

| Isolation Level | Dirty Reads | Non-Repeatable Reads | Phantom Reads | Performance |
|---|---|---|---|---|
| READ_UNCOMMITTED | ✅ Possible | ✅ Possible | ✅ Possible | Fastest |
| READ_COMMITTED | ❌ Not possible | ✅ Possible | ✅ Possible | Fast (default) |
| REPEATABLE_READ | ❌ Not possible | ❌ Not possible | ✅ Possible | Medium |
| SERIALIZABLE | ❌ Not possible | ❌ Not possible | ❌ Not possible | Slowest |

**Kiedy używać którego isolation level?**
- **READ_COMMITTED:** Default, wystarczający dla większości use cases
- **REPEATABLE_READ:** Gdy potrzebujesz consistent reads (np. reporting, analytics)
- **SERIALIZABLE:** Gdy potrzebujesz full isolation (np. financial transactions, inventory management)

### 4.3 Pessimistic Locking

```java
// Pessimistic locking: lock row przed read/write
PaymentOrder order = repository.findById(orderId)
    .orElseThrow();

// Lock row (other transactions wait)
repository.lock(order, LockModeType.PESSIMISTIC_WRITE);

// Modify and save
order.setStatus("AUTHORIZED");
repository.save(order);

// Lock released after transaction commits
```

**Lock modes:**

| Lock Mode | Znaczenie | Kiedy używać |
|---|---|---|
| `PESSIMISTIC_READ` | Shared lock (inne transactions mogą czytać, nie mogą pisać) | Gdy czytasz data i chcesz zapobiec modifications |
| `PESSIMISTIC_WRITE` | Exclusive lock (inne transactions nie mogą czytać ani pisać) | Gdy modyfikujesz data i chcesz zapobiec conflicts |
| `PESSIMISTIC_FORCE_INCREMENT` | Exclusive lock + version increment | Gdy chcesz wymusić version update |

**Kiedy używać pessimistic locking?**
- Gdy conflicts są częste (np. inventory management, seat booking)
- Gdy chcesz zapobiec lost updates (dwie transactions modyfikują ten sam row)
- **Wada:** Inne transactions wait (może spowodować deadlocks, performance issues)

### 4.4 Optimistic Locking

```java
// Optimistic locking: version column
@Entity
@Table(name = "payment_orders")
class PaymentOrder {
    
    @Id
    private UUID paymentOrderId;
    
    @Version
    private Long version;  // Auto-incremented by JPA
    
    private PaymentStatus status;
    
    // ...
}

// Transaction 1:
PaymentOrder order1 = repository.findById(orderId).orElseThrow();
// order1.version = 1

// Transaction 2:
PaymentOrder order2 = repository.findById(orderId).orElseThrow();
// order2.version = 1

// Transaction 1:
order1.setStatus("AUTHORIZED");
repository.save(order1);
// order1.version = 2 (auto-incremented)

// Transaction 2:
order2.setStatus("CAPTURED");
repository.save(order2);
// Throws OptimisticLockException (version mismatch: expected 1, found 2)
```

**Kiedy używać optimistic locking?**
- Gdy conflicts są rzadkie (np. payment order status updates, user profile updates)
- Gdy chcesz avoid locks (lepsza performance)
- **Wada:** Musisz handle OptimisticLockException (retry logic)

**Pessimistic vs Optimistic:**

| Feature | Pessimistic | Optimistic |
|---|---|---|
| Locking | Locks row (other transactions wait) | No locks (version check) |
| Conflicts | Prevents conflicts (locks) | Detects conflicts (version mismatch) |
| Performance | Wolniejsze (locks, waits) | Szybsze (no locks) |
| Deadlocks | Possible (circular waits) | Not possible (no locks) |
| Use case | Frequent conflicts (inventory, booking) | Rare conflicts (status updates, profiles) |

### 4.5 Deadlock Detection

```sql
-- Deadlock scenario:
-- Transaction 1: locks row A, waits for row B
-- Transaction 2: locks row B, waits for row A
-- Result: deadlock (PostgreSQL kills one transaction)

-- Transaction 1:
BEGIN;
UPDATE payment_orders SET status = 'AUTHORIZED' WHERE payment_order_id = 'A';
-- Locks row A
UPDATE payment_orders SET status = 'AUTHORIZED' WHERE payment_order_id = 'B';
-- Waits for row B (locked by Transaction 2)

-- Transaction 2:
BEGIN;
UPDATE payment_orders SET status = 'AUTHORIZED' WHERE payment_order_id = 'B';
-- Locks row B
UPDATE payment_orders SET status = 'AUTHORIZED' WHERE payment_order_id = 'A';
-- Waits for row A (locked by Transaction 1)

-- PostgreSQL detects deadlock and kills one transaction:
-- ERROR: deadlock detected
-- DETAIL: Process 123 waits for ShareLock on transaction 456; blocked by process 789.
-- Process 789 waits for ShareLock on transaction 123; blocked by process 123.
```

**Jak zapobiec deadlockom?**
1. **Consistent lock ordering:** Zawsze lock rows w tej samej kolejności (np. ORDER BY payment_order_id)
2. **Shorter transactions:** Krótsze transactions = mniej czasu na deadlock
3. **Appropriate isolation level:** Niższy isolation level = mniej locks
4. **Avoid circular dependencies:** Nie lockuj resources w circular pattern

**Jak wykryć deadlock?**
```sql
-- Check for waiting transactions:
SELECT * FROM pg_stat_activity WHERE wait_event_type = 'Lock';

-- Check for deadlock cycles:
SELECT * FROM pg_locks WHERE NOT granted;
```

## 5. Walkthrough — Od Seq Scan Do Index Scan

```
PRZED (Seq Scan):
1. Query: SELECT * FROM payment_orders WHERE merchant_id = '123' AND status = 'CREATED'
2. PostgreSQL czyta wszystkie rows w table (1000 rows)
3. PostgreSQL filtruje rows (merchant_id = '123' AND status = 'CREATED')
4. Result: 3 rows (997 rows removed by filter)
5. Execution time: 50ms (wolne)

PO (Index Scan):
1. Dodaj index: CREATE INDEX idx_payment_orders_merchant_status ON payment_orders (merchant_id, status)
2. Query: SELECT * FROM payment_orders WHERE merchant_id = '123' AND status = 'CREATED'
3. PostgreSQL używa index aby znaleźć matching rows (3 rows)
4. PostgreSQL czyta tylko 3 rows (nie 1000)
5. Execution time: 0.5ms (szybkie)
```

## 6. Learning Delta — Co Nowe vs Lessons 06-12

| Temat | Lesson 06-12 | Lesson 13 |
|---|---|---|
| Query performance | Brak | EXPLAIN ANALYZE |
| Transaction isolation | @Transactional basics | READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE |
| Locking | Brak | Pessimistic vs optimistic locking |
| Deadlocks | Brak | Detection i prevention |

## 7. Typowe Błędy

1. **EXPLAIN ANALYZE dla INSERT/UPDATE/DELETE.** EXPLAIN ANALYZE wykonuje query (może mieć side effects). Używaj EXPLAIN (bez ANALYZE) dla write queries.
2. **Seq Scan na large tables.** Jeśli table ma > 10k rows i query jest selective (< 10% rows), dodaj index aby wymusić Index Scan.
3. **READ_COMMITTED dla reporting.** READ_COMMITTED może widzieć phantom reads. Dla reporting/analytics, użyj REPEATABLE_READ.
4. **Pessimistic locking dla rare conflicts.** Pessimistic locking jest wolne (locks, waits). Dla rare conflicts, użyj optimistic locking.
5. **Optimistic locking bez retry logic.** OptimisticLockException wymaga retry logic (inaczej user widzi error).
6. **Inconsistent lock ordering.** Jeśli transactions lockują rows w różnej kolejności, deadlock jest possible. Zawsze lockuj w tej samej kolejności.
7. **Long transactions.** Długie transactions zwiększają ryzyko deadlocks i performance issues. Używaj shorter transactions.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Napisz EXPLAIN ANALYZE dla payment_orders SELECT query | 20 min |
| 2 | Porównaj Seq Scan vs Index Scan (execution time) | 25 min |
| 3 | Dodaj index i verify Index Scan w EXPLAIN output | 20 min |
| 4 | Napisz test z READ_COMMITTED isolation | 30 min |
| 5 | Napisz test z REPEATABLE_READ isolation | 30 min |
| 6 | Napisz test z pessimistic locking (PESSIMISTIC_WRITE) | 30 min |
| 7 | Napisz test z optimistic locking (@Version) | 30 min |
| 8 | Napisz scenario który powoduje deadlock | 40 min |
| 9 | Zaimplementuj consistent lock ordering aby zapobiec deadlock | 30 min |

## 9. Pytania

1. Jaka jest różnica między EXPLAIN a EXPLAIN ANALYZE?
2. Kiedy PostgreSQL używa Seq Scan vs Index Scan?
3. Jak dodać index aby wymusić Index Scan?
4. Jaka jest różnica między READ_COMMITTED a REPEATABLE_READ?
5. Kiedy używać SERIALIZABLE isolation?
6. Jaka jest różnica między pessimistic a optimistic locking?
7. Kiedy używać pessimistic locking?
8. Kiedy używać optimistic locking?
9. Co to jest deadlock i jak go wykryć?
10. Jak zapobiec deadlockom (consistent lock ordering)?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `explainAnalyzeShowsIndexScan` | EXPLAIN ANALYZE works |
| `readCommittedAllowsPhantomReads` | READ_COMMITTED isolation works |
| `repeatableReadPreventsPhantomReads` | REPEATABLE_READ isolation works |
| `pessimisticLockingPreventsConflicts` | Pessimistic locking works |
| `optimisticLockingDetectsConflicts` | Optimistic locking works |
| `consistentLockOrderingPreventsDeadlocks` | Deadlock prevention works |

## 11. Powiązane Notatki

- [[Lesson 06D - SQL and Flyway Constraints for Payment Orders]]
- [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]]
- [[Lesson 12 - CTE and Window Functions]]
- [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]
- [[Senior SDET Competency Coverage Matrix]]
