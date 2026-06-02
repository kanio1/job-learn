---
type: lesson
status: planned
area: PostgreSQL and SQL From Zero
lesson: 12
module: CTE and Window Functions
date: 2026-05-31
tags:
  - sql
  - postgresql
  - cte
  - window-functions
  - lesson-12
  - senior-sdet
---

# Lesson 12 — CTE and Window Functions

> **Evidence link:** `JpaPaymentOrderRepository.java`, `V2__create_payment_orders.sql`
>
> **Navigation:** [[PostgreSQL and SQL From Zero MOC]] | [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się zaawansowanych SQL features dla test data analysis i diagnostics:
- **CTE (Common Table Expressions)** — named subqueries dla czytelności
- **Window functions** — `ROW_NUMBER()`, `RANK()`, `LAG()`, `LEAD()`, running totals
- **Kiedy używać CTE vs nested subqueries**
- **Kiedy używać window functions vs GROUP BY**

## 2. Prerequisites

- `SELECT`, `WHERE`, `ORDER BY`, `GROUP BY` (Lesson 06-08).
- `COUNT`, `SUM`, `COALESCE` (Lesson 08).
- Subqueries (basic awareness).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `JpaPaymentOrderRepository.java` | Obecne JPQL queries (proste aggregations) |
| `V2__create_payment_orders.sql` | Table structure dla window function examples |
| `V3__add_payment_order_list_indexes.sql` | Indexes wpływające na window function performance |

## 4. Kluczowe Pojęcia

### 4.1 CTE (Common Table Expressions)

```sql
-- CTE dla complex test data queries:
WITH merchant_orders AS (
    SELECT 
        merchant_id, 
        COUNT(*) as order_count, 
        SUM(amount_minor) as total_amount
    FROM payment_orders
    WHERE status = 'CREATED'
    GROUP BY merchant_id
),
active_merchants AS (
    SELECT merchant_id, reference, display_name
    FROM merchants
    WHERE status = 'ACTIVE'
)
SELECT 
    am.reference, 
    am.display_name, 
    mo.order_count, 
    mo.total_amount
FROM active_merchants am
JOIN merchant_orders mo ON am.merchant_id = mo.merchant_id
WHERE mo.order_count > 5
ORDER BY mo.total_amount DESC;
```

**Dlaczego CTE?**
- **Czytelność:** Named subqueries zamiast nested subqueries (łatwiej zrozumieć)
- **Reusability:** CTE może być użyte wielokrotnie w tym samym query
- **Debugging:** Możesz `SELECT * FROM cte_name` aby sprawdzić intermediate results
- **Performance:** PostgreSQL optymalizuje CTE (materializes jeśli użyte wielokrotnie)

**CTE vs nested subqueries:**

```sql
-- Nested subquery (trudne do czytania):
SELECT am.reference, am.display_name, mo.order_count, mo.total_amount
FROM (
    SELECT merchant_id, reference, display_name
    FROM merchants
    WHERE status = 'ACTIVE'
) am
JOIN (
    SELECT merchant_id, COUNT(*) as order_count, SUM(amount_minor) as total_amount
    FROM payment_orders
    WHERE status = 'CREATED'
    GROUP BY merchant_id
) mo ON am.merchant_id = mo.merchant_id
WHERE mo.order_count > 5;

-- CTE (czytelne):
WITH active_merchants AS (
    SELECT merchant_id, reference, display_name
    FROM merchants
    WHERE status = 'ACTIVE'
),
merchant_orders AS (
    SELECT merchant_id, COUNT(*) as order_count, SUM(amount_minor) as total_amount
    FROM payment_orders
    WHERE status = 'CREATED'
    GROUP BY merchant_id
)
SELECT am.reference, am.display_name, mo.order_count, mo.total_amount
FROM active_merchants am
JOIN merchant_orders mo ON am.merchant_id = mo.merchant_id
WHERE mo.order_count > 5;
```

### 4.2 Window Functions: ROW_NUMBER, RANK, DENSE_RANK

```sql
-- ROW_NUMBER: unikalny numer dla każdego wiersza (bez ties)
SELECT 
    payment_order_id,
    merchant_id,
    amount_minor,
    ROW_NUMBER() OVER (ORDER BY amount_minor DESC) as row_num
FROM payment_orders;

-- RANK: numer z gaps dla ties (1, 2, 2, 4)
SELECT 
    payment_order_id,
    merchant_id,
    amount_minor,
    RANK() OVER (ORDER BY amount_minor DESC) as rank
FROM payment_orders;

-- DENSE_RANK: numer bez gaps dla ties (1, 2, 2, 3)
SELECT 
    payment_order_id,
    merchant_id,
    amount_minor,
    DENSE_RANK() OVER (ORDER BY amount_minor DESC) as dense_rank
FROM payment_orders;

-- PARTITION BY: ranking per group
SELECT 
    payment_order_id,
    merchant_id,
    amount_minor,
    ROW_NUMBER() OVER (PARTITION BY merchant_id ORDER BY amount_minor DESC) as rank_per_merchant
FROM payment_orders;
```

**Kiedy używać ROW_NUMBER vs RANK vs DENSE_RANK?**
- **ROW_NUMBER:** Gdy potrzebujesz unikalny numer (np. top N per group)
- **RANK:** Gdy chcesz widzieć ties (np. "2. miejsce ex aequo")
- **DENSE_RANK:** Gdy chcesz continuous ranking bez gaps (np. "3. najwyższy amount")

### 4.3 Window Functions: LAG, LEAD

```sql
-- LAG: poprzedni wiersz (np. porównanie z poprzednim payment order)
SELECT 
    payment_order_id,
    amount_minor,
    LAG(amount_minor) OVER (ORDER BY created_at) as previous_amount,
    amount_minor - LAG(amount_minor) OVER (ORDER BY created_at) as amount_change
FROM payment_orders
WHERE merchant_id = '123';

-- LEAD: następny wiersz
SELECT 
    payment_order_id,
    amount_minor,
    LEAD(amount_minor) OVER (ORDER BY created_at) as next_amount,
    LEAD(amount_minor) OVER (ORDER BY created_at) - amount_minor as amount_change
FROM payment_orders
WHERE merchant_id = '123';

-- LAG/LEAD z default value (gdy nie ma poprzedniego/następnego)
SELECT 
    payment_order_id,
    amount_minor,
    LAG(amount_minor, 1, 0) OVER (ORDER BY created_at) as previous_amount
FROM payment_orders;
```

**Kiedy używać LAG/LEAD?**
- Porównanie z poprzednim/następnym wierszem (np. "czy amount wzrósł?")
- Obliczanie zmian (np. "różnica w amount między kolejnymi orders")
- Wykrywanie anomalies (np. "nagły spadek amount")

### 4.4 Window Functions: Running Totals

```sql
-- Running total (cumulative sum):
SELECT 
    payment_order_id,
    amount_minor,
    created_at,
    SUM(amount_minor) OVER (ORDER BY created_at) as running_total
FROM payment_orders
WHERE merchant_id = '123';

-- Running total per group:
SELECT 
    payment_order_id,
    merchant_id,
    amount_minor,
    created_at,
    SUM(amount_minor) OVER (PARTITION BY merchant_id ORDER BY created_at) as running_total_per_merchant
FROM payment_orders;

-- Moving average (średnia z ostatnich 3 orders):
SELECT 
    payment_order_id,
    amount_minor,
    AVG(amount_minor) OVER (ORDER BY created_at ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) as moving_avg_3
FROM payment_orders
WHERE merchant_id = '123';
```

**Kiedy używać running totals?**
- Cumulative metrics (np. "total amount do tej pory")
- Trend analysis (np. "czy running total rośnie?")
- Moving averages (np. "średnia z ostatnich N orders")

### 4.5 Window Functions vs GROUP BY

| Scenario | GROUP BY | Window Function |
|---|---|---|
| Agregacja (total per merchant) | ✅ `GROUP BY merchant_id` | ❌ Overkill |
| Ranking (top 3 per merchant) | ❌ Trudne (self-join) | ✅ `ROW_NUMBER() OVER (PARTITION BY merchant_id)` |
| Porównanie z poprzednim wierszem | ❌ Trudne (self-join) | ✅ `LAG()` |
| Running total | ❌ Trudne (correlated subquery) | ✅ `SUM() OVER (ORDER BY ...)` |
| Zachowanie wszystkich wierszy + agregacja | ❌ Niemożliwe (GROUP BY redukuje wiersze) | ✅ Window function zachowuje wszystkie wiersze |

**Kluczowa różnica:**
- **GROUP BY:** Redukuje liczbę wierszy (agregacja)
- **Window function:** Zachowuje wszystkie wiersze + dodaje computed column

## 5. Walkthrough — Od Nested Subquery Do CTE

```
PRZED (nested subquery):
1. Czytasz query od wewnątrz (innermost subquery)
2. Próbujesz zrozumieć co każda subquery robi
3. Śledzisz aliases przez wiele poziomów nesting
4. Trudne do debugowania (nie możesz SELECT * FROM subquery)

PO (CTE):
1. Czytasz query od góry (pierwsza CTE)
2. Każda CTE ma nazwę (self-documenting)
3. Możesz debugować każdą CTE osobno (SELECT * FROM cte_name)
4. Łatwe do refaktoru (możesz reużywać CTE)
```

## 6. Learning Delta — Co Nowe vs Lessons 06-11

| Temat | Lesson 06-11 | Lesson 12 |
|---|---|---|
| Subqueries | Basic (WHERE clause) | CTE (named subqueries) |
| Agregacja | `GROUP BY` (redukuje wiersze) | Window functions (zachowuje wiersze) |
| Ranking | Brak | `ROW_NUMBER()`, `RANK()`, `DENSE_RANK()` |
| Porównania | Brak | `LAG()`, `LEAD()` |
| Running totals | Brak | `SUM() OVER (ORDER BY ...)` |

## 7. Typowe Błędy

1. **CTE bez WHERE clause.** Jeśli CTE nie filtruje danych, może być wolne. Zawsze dodawaj WHERE gdy to możliwe.
2. **Window function bez ORDER BY.** Bez ORDER BY, wynik jest non-deterministic. Zawsze dodawaj ORDER BY (chyba że chcesz arbitrary order).
3. **ROW_NUMBER vs RANK confusion.** ROW_NUMBER daje unikalne numery (1, 2, 3, 4), RANK daje ties (1, 2, 2, 4). Wybierz odpowiedni dla use case.
4. **LAG/LEAD bez PARTITION BY.** Jeśli nie dodasz PARTITION BY, LAG/LEAD działa na całym dataset (nie per group).
5. **Window function w WHERE clause.** Window functions nie mogą być użyte w WHERE (są obliczane po WHERE). Użyj CTE lub subquery.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Napisz CTE zwracające merchants z > 5 payment orders | 20 min |
| 2 | Napisz query z ROW_NUMBER() zwracające top 3 payment orders per merchant | 25 min |
| 3 | Napisz query z LAG() porównujące amount z poprzednim payment order | 20 min |
| 4 | Napisz query z running total (cumulative sum) dla payment orders | 20 min |
| 5 | Porównaj GROUP BY vs window function dla "total per merchant + all rows" | 15 min |
| 6 | Zdebuguj CTE: SELECT * FROM cte_name aby sprawdzić intermediate results | 15 min |

## 9. Pytania

1. Dlaczego CTE jest bardziej czytelne niż nested subqueries?
2. Jak PostgreSQL optymalizuje CTE (materialization)?
3. Jaka jest różnica między ROW_NUMBER, RANK i DENSE_RANK?
4. Kiedy używać PARTITION BY w window functions?
5. Dlaczego LAG/LEAD nie mogą być użyte w WHERE clause?
6. Jak obliczyć moving average z window function?
7. Kiedy window function jest lepsze niż GROUP BY?
8. Czy CTE może być recursive?
9. Jak window function wpływa na performance (czy jest wolne)?
10. Czy window functions mogą być użyte w JPQL?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `cteReturnsMerchantsWithMoreThan5Orders` | CTE works |
| `rowNumberReturnsTop3PerMerchant` | ROW_NUMBER with PARTITION BY works |
| `lagReturnsPreviousAmount` | LAG works |
| `runningTotalCalculatesCumulativeSum` | Running total works |

## 11. Powiązane Notatki

- [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]]
- [[Lesson 11 - Test Data Isolation Strategies and Flyway Test Migrations]]
- [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Senior SDET Competency Coverage Matrix]]
