---
type: lesson
status: ready
area: PostgreSQL and SQL From Zero
lesson: 08
module: GROUP BY, COUNT, SUM, COALESCE, Null Semantics in Aggregation Queries
date: 2026-05-30
tags:
  - sql
  - postgresql
  - group-by
  - aggregation
  - null-semantics
  - projection
  - lesson-08
  - payment-order-summary
  - senior-sdet
---

# Lesson 08 — GROUP BY, COUNT, SUM, COALESCE, and Null Semantics in Aggregation Queries

> **Evidence link:** `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java`
>
> **Navigation:** [[PostgreSQL and SQL From Zero MOC]] | [[Lesson 08 - Payment Aggregation Summary]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Zrozumieć i przetestować SQL agregacyjny dla realnego endpointu payment order summary:
- `GROUP BY currency` i `GROUP BY status`,
- `COUNT(*)` i `SUM(amount_minor)`,
- `COALESCE` dla obsługi `NULL` z pustych result setów,
- opcjonalne filtry (`currency`, `status`, `fromDate`, `toDate`) w `WHERE`,
- `ORDER BY` dla stabilnej kolejności,
- `EXPLAIN` jako narzędzie diagnostyczne (myślenie, nie benchmark).

## 2. Prerequisites

- Podstawy `SELECT`, `WHERE`, `ORDER BY` (SQL Lessons 1-5).
- Podstawy `CREATE TABLE`, `CONSTRAINT`, indeksy (Lesson 06D).
- Co to jest `payment_orders` i jakie ma kolumny (`merchant_id`, `currency`, `status`, `amount_minor`, `created_at`).
- Flyway: V2 tworzy tabelę, V3 dodaje indeksy (`merchant_id, status`, `merchant_id, currency`).

## 3. Code Reading Map

| Plik / Artefakt | Co zawiera |
|---|---|
| `JpaPaymentOrderRepository.java` | 3 zapytania JPQL: `findSummaryTotals`, `findSummaryByCurrency`, `findSummaryByStatus` |
| `V2__create_payment_orders.sql` | definicja tabeli `payment_orders` z indeksem `merchant_id, created_at DESC` |
| `V3__add_payment_order_list_indexes.sql` | indeksy `merchant_id, status` i `merchant_id, currency` |
| `PaymentOrderSummaryService.java` | wywołuje 3 zapytania, parsuje parametry, normalizuje `null` |

## 4. Kluczowe Pojęcia

### 4.1 GROUP BY — dzielenie na kategorie

```sql
SELECT currency, COUNT(*), SUM(amount_minor)
FROM payment_orders
WHERE merchant_id = ?
GROUP BY currency
ORDER BY currency;
```

- `GROUP BY currency` — PostgreSQL tworzy jedną grupę dla każdej unikalnej wartości `currency`.
- Każda grupa jest agregowana osobno — `COUNT(*)` i `SUM(amount_minor)` liczone per grupa.
- Bez `GROUP BY` — jedna wielka grupa (wszystkie wiersze).

### 4.2 COUNT(*) i SUM(amount_minor)

```sql
SELECT COUNT(po) AS orderCount,
       COALESCE(SUM(po.amountMinor), 0) AS totalAmountMinor
FROM PaymentOrder po WHERE ...
```

- `COUNT(po)` — liczy encje (wiersze) pasujące do `WHERE`.
- `SUM(po.amountMinor)` — sumuje `BIGINT` z kolumny. Zwraca `NULL` gdy zero wierszy.
- `COALESCE(SUM(...), 0)` — zamienia `NULL` na `0` (empty merchant → zero totals).

### 4.3 Opcjonalne filtry w WHERE — NULL semantics

```sql
WHERE po.merchantId = :merchantId
  AND (CAST(:currency AS string) IS NULL OR po.currency = :currency)
  AND (CAST(:status AS string) IS NULL OR po.status = :status)
  AND (CAST(:fromInclusive AS java.time.Instant) IS NULL OR po.createdAt >= :fromInclusive)
  AND (CAST(:toInclusive AS java.time.Instant) IS NULL OR po.createdAt <= :toInclusive)
```

- `CAST(:param AS type) IS NULL` — PostgreSQL 18 potrzebuje jawnego castowania, by określić typ parametru.
- Gdy parametr jest `NULL` → warunek `IS NULL` jest `TRUE` — filtr pominięty.
- Gdy parametr ma wartość → `IS NULL` jest `FALSE` → sprawdzamy `po.currency = :currency`.
- **Dlaczego tak?** Chcemy jednego zapytania, które działa zarówno z filtrem, jak i bez.

### 4.4 ORDER BY dla stabilnych wyników

```sql
GROUP BY po.currency ORDER BY po.currency ASC
GROUP BY po.status  ORDER BY po.status ASC
```

- Bez `ORDER BY` kolejność grup jest nieokreślona.
- Kontrakt API wymaga `byCurrency` sorted by currency ascending, `byStatus` sorted by status ascending.
- W testach używamy `containsExactly(...)` — wymaga dokładnej kolejności.

### 4.5 Projekcje Spring Data

```java
interface CurrencySummaryProjection {
    String getCurrency();
    long getOrderCount();
    Long getTotalAmountMinor();  // Long (boxed) — może być null z SQL
}
```

- Spring Data automatycznie mapuje aliasy kolumn (`AS currency`, `AS orderCount`) na gettery.
- `Long` (boxed) dla `SUM` — bo może być `NULL`.
- Service normalizuje `null` → `0L` przed włożeniem do response DTO.

## 5. Walkthrough — Od Requestu Do SQL

1. `GET /api/merchants/{id}/payment-orders/summary?currency=PLN&fromDate=2026-01-01`
2. Service parsuje query params: `currency="PLN"`, `fromDate=2026-01-01T00:00:00Z`.
3. Service wywołuje `findSummaryTotals(merchantId, "PLN", null, fromInstant, null)`.
4. JPQL: `WHERE merchant_id=? AND (TRUE OR currency=?) AND (TRUE OR ...) AND ...` — tylko `currency` filter jest aktywny.
5. PostgreSQL wykonuje `GROUP BY` na przefiltrowanych wierszach.
6. Wynik: `{orderCount=2, totalAmountMinor=3000}`.

## 6. Delta vs Lesson 07

| Aspekt | Lesson 07 SQL | Lesson 08 SQL |
|---|---|---|
| Główna operacja | `WHERE` z 6 filtrami | `GROUP BY` z `COUNT`/`SUM` |
| Paginacja | `LIMIT`/`OFFSET` przez `PageRequest` | brak paginacji — zawsze pełny agregat |
| Sortowanie | `ORDER BY created_at DESC` przez `Sort` | `ORDER BY currency ASC` / `ORDER BY status ASC` |
| Null handling | brak (filtry pomijane przez Specification) | `COALESCE(SUM(...), 0)` + `CAST(:param AS type) IS NULL` |
| Nowe | — | `GROUP BY`, agregacje, nullable parametry w `WHERE`, projekcje |

## 7. Typowe Błędy i Antywzorce

| Błąd | Dlaczego zły | Poprawnie |
|---|---|---|
| `SUM(amount_minor)` bez `COALESCE` | zwraca `NULL` dla pustego merchanta | `COALESCE(SUM(...), 0)` |
| `:param IS NULL` bez `CAST` w PostgreSQL 18 | `could not determine data type of parameter` | `CAST(:param AS type) IS NULL` |
| Pobranie wszystkich encji i sumowanie w Java | N+1, memory, wolne | `GROUP BY` w SQL |
| `GROUP BY` bez `ORDER BY` | niestabilna kolejność w testach | `ORDER BY currency ASC` |
| `COUNT(column)` zamiast `COUNT(*)` lub `COUNT(po)` | `COUNT(column)` ignoruje `NULL` | `COUNT(po)` dla encji |
| Brak indeksu na `merchant_id` | sequential scan na dużej tabeli | istniejący indeks z V2 |

## 8. Ćwiczenia

1. **Uruchom EXPLAIN** w głowie dla zapytania bez filtrów vs z `?currency=PLN` — który indeks może być użyty?
2. **Napisz SQL**, który zwraca sumę amount_minor per merchant_id — co trzeba zmienić?
3. **Co się stanie** gdy `?currency=XYZ` (nieistniejąca waluta)? — zero wierszy, nie 400.
4. **Dodaj w myśli** nowy filter `?minAmount=500` — jak zmieni się `WHERE`?
5. **Porównaj** `SUM(amount_minor)` z `AVG(amount_minor)` — kiedy użyć którego?

## 9. Pytania Kontrolne

1. Co robi `GROUP BY currency`?
2. Dlaczego potrzebujemy `CAST(:param AS type) IS NULL` w PostgreSQL 18?
3. Co zwróci `SUM(amount_minor)` gdy nie ma pasujących wierszy?
4. Dlaczego `ORDER BY` w `GROUP BY` query jest ważne?
5. Które indeksy z Lesson 07 pomagają Lesson 08?

## 10. Jak To Testować (SQL)

- **Na poziomie API**: kontraktowe testy REST z seeded data — sprawdź, czy agregaty się zgadzają.
- **Na poziomie DB**: `@DataJpaTest` + `Testcontainers` — seed, wywołaj query, sprawdź wynik.
- **EXPLAIN thinking**: dla każdego query zastanów się, który indeks PostgreSQL użyje. Zweryfikuj w psql/DataGrip.
- **Anti-flakiness**: zawsze seeduj dane przed query, nie polegaj na istniejących danych.

## 11. Next Links

- [[Lesson 08 - Payment Aggregation Summary]] — pełna notatka lekcji
- [[Lesson 08 - Java Records, Read-Only Services, and Input Validation]] — Java side
- [[Lesson 08 - Business Logic, Decision Tables, and Risk Notes]] — domain rules
- [[Lesson 06D - SQL and Flyway Constraints for Payment Orders]] — poprzedni SQL bridge
- [[PostgreSQL and SQL From Zero MOC]]
- [[Lesson Evidence Tracker]]
