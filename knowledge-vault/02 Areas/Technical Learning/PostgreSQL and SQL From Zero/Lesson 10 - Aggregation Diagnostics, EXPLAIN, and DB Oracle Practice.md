---
type: lesson
status: planned
area: PostgreSQL and SQL From Zero
lesson: 10
module: Aggregation Diagnostics, EXPLAIN, and DB Oracle Practice
date: 2026-05-31
tags:
  - sql
  - postgresql
  - explain
  - aggregation
  - db-oracle
  - lesson-10
  - senior-sdet
---

# Lesson 10 — Aggregation Diagnostics, EXPLAIN, and DB Oracle Practice

> **Evidence link:** `JpaPaymentOrderRepository.java` (summary queries), optional repository/service aggregation test (planned Lesson 10C)
>
> **Navigation:** [[PostgreSQL and SQL From Zero MOC]] | [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się myśleć o bazie danych jako o **niezależnym oracle** dla testów API:
- Kiedy weryfikować przez API, a kiedy przez DB?
- Jak użyć `EXPLAIN` do diagnozowania zapytań agregacyjnych?
- Jak napisać test na poziomie repository/service, który izoluje logikę agregacji od HTTP?
- Jak zaprojektować test, który porównuje wynik API z wynikiem surowego SQL?

## 2. Prerequisites

- `SELECT`, `WHERE`, `ORDER BY`, `GROUP BY`, `COUNT`, `SUM`, `COALESCE` (Lesson 08).
- `JpaPaymentOrderRepository` — 3 zapytania JPQL dla summary (Lesson 08).
- `PaymentOrderSummaryService` — read-only orchestration (Lesson 08).
- Flyway: V2 tworzy tabelę, V3 dodaje indeksy (Lessons 06-07).
- `@DataJpaTest` i `@SpringBootTest` — różnica (Lesson 06).

## 3. Code Reading Map

| Plik | Co zawiera |
|---|---|
| `JpaPaymentOrderRepository.java` | 3 zapytania JPQL: `findSummaryTotals`, `findSummaryByCurrency`, `findSummaryByStatus` |
| `V3__add_payment_order_list_indexes.sql` | indeksy `(merchant_id, status)` i `(merchant_id, currency)` |
| `PaymentOrderSummaryService.java` | wywołuje 3 zapytania, normalizuje `null` |
| `PaymentOrderSummaryApiTestSupport.java` | kontrolowane seed data + expected oracle |

## 4. Kluczowe Pojęcia

### 4.1 DB jako test oracle — dlaczego?

Lesson 08 używa **API jako oracle** (seed → API response → porównanie z expected). To działa, ale ma słabość: gdy API zwraca zły wynik, nie wiemy czy to bug w SQL, bug w service, czy bug w kontrolerze.

**DB oracle** = sprawdzamy wynik bezpośrednio w bazie (repository/service test), niezależnie od HTTP.

```java
// API oracle (Lesson 08) — testuje cały stack
PaymentOrderSummaryResponse response = given()...get("/summary").as(PaymentOrderSummaryResponse.class);

// DB oracle (Lesson 10C) — izoluje agregację SQL
PaymentOrderSummaryResponse response = paymentOrderSummaryService.summarize(merchantId, request);
// Teraz wiemy: jeśli to przechodzi a API nie → bug w kontrolerze lub HTTP layer
```

### 4.2 Kiedy DB oracle, kiedy API oracle?

| Pytanie | API oracle | DB oracle |
|---|---|---|
| Czy kontrakt HTTP jest poprawny? | **Tak** | Nie |
| Czy status code i nagłówki są poprawne? | **Tak** | Nie |
| Czy JSON serializacja działa? | **Tak** | Nie |
| Czy agregacja SQL jest poprawna? | Pośrednio | **Bezpośrednio** |
| Czy `null` → `0` normalizacja działa? | Pośrednio | **Bezpośrednio** |
| Czy filtr currency/status działa? | Pośrednio | **Bezpośrednio** |
| Czy date filter (UTC, inclusive) działa? | Pośrednio | **Bezpośrednio** |

**Zasada:** DB oracle jest przydatny, gdy:
1. Agregacja jest złożona (`GROUP BY` + `SUM` + `COALESCE`),
2. Chcemy izolować bug w warstwie danych od bugu w HTTP,
3. Diagnozujemy problem z wydajnością (`EXPLAIN`),
4. Chcemy testować więcej wariantów danych szybciej (bez HTTP overhead).

### 4.3 EXPLAIN — analiza planu zapytania

```sql
EXPLAIN ANALYZE
SELECT COALESCE(SUM(po.amountMinor), 0)
FROM payment_orders po
WHERE po.merchant_id = '...';
```

- `EXPLAIN` — pokazuje **plan wykonania** (jak PostgreSQL planuje wykonać zapytanie).
- `EXPLAIN ANALYZE` — wykonuje zapytanie i pokazuje **rzeczywiste czasy**.
- Co czytać:
  - `Seq Scan` vs `Index Scan` — czy używa indeksu?
  - `cost=...` — szacowany koszt (pierwsza liczba = startup, druga = total).
  - `actual time=...` — rzeczywisty czas (tylko z `ANALYZE`).
  - `rows=...` — ile wierszy przetworzono.

**Nie automatyzuj EXPLAIN w teście** — to ćwiczenie diagnostyczne, nie asercja. Plany zapytań zmieniają się wraz z danymi, wersją PostgreSQL i statystykami.

### 4.4 Repository-level aggregation test — przykład

```java
@DataJpaTest
@Import(PaymentOrderSummaryService.class)
class PaymentOrderSummaryServiceAggregationTest {

    @Autowired PaymentOrderSummaryService service;
    @Autowired TestEntityManager em;

    @Test
    void emptyMerchantReturnsZeroTotals() {
        UUID merchantId = UUID.randomUUID();
        PaymentOrderSummaryRequest request = new PaymentOrderSummaryRequest(null, null, null, null);

        PaymentOrderSummaryResponse response = service.summarize(merchantId, request);

        assertThat(response.totalOrders()).isZero();
        assertThat(response.totalAmountMinor()).isZero();
        assertThat(response.byCurrency()).isEmpty();
        assertThat(response.byStatus()).isEmpty();
    }

    @Test
    void multiCurrencyAggregationMatchesDbState() {
        // Arrange: persist 3 PLN + 2 EUR orders via TestEntityManager
        // Act: service.summarize(merchantId, request)
        // Assert: compare response totals with expected from seed data
    }

    @Test
    void fromDateToDateBoundariesAreInclusiveUtc() {
        // Arrange: orders at 2026-01-01T00:00:00Z and 2026-01-01T23:59:59Z
        // Act: summarize with fromDate=2026-01-01, toDate=2026-01-01
        // Assert: both orders included (inclusive start and end of day)
    }
}
```

- `@DataJpaTest` — lżejszy kontekst niż `@SpringBootTest`, tylko JPA + repository.
- `TestEntityManager` — persist encji bezpośrednio, bez przechodzenia przez service/HTTP.
- Test izoluje **logikę agregacji** — nie HTTP, nie security, nie serializację JSON.

### 4.5 API vs DB — różnica w diagnostyce

| Błąd | API oracle (Lesson 08) | DB oracle (Lesson 10C) |
|---|---|---|
| `SUM(amount_minor)` zwraca `NULL` zamiast `0` | REST test: `totalAmountMinor=null` (fail) | Repository test: service normalizuje `null` → widzisz czy service to robi |
| Date filter `fromDate` traktowany jako exclusive | REST test: za mało orderów (fail) | Repository test: widzisz czy to SQL czy service parsuje źle |
| `GROUP BY currency` pomija jeden currency | REST test: `byCurrency` ma 2 zamiast 3 elementów | Repository test: widzisz dokładnie które zapytanie jest złe |
| Indeks nieużywany — wolne zapytanie | REST test: timeout | EXPLAIN pokazuje `Seq Scan` zamiast `Index Scan` |

## 5. Walkthrough — Od Pytania "Czy API Kłamie?" Do Diagnostyki

```
1. REST test pada: totalAmountMinor=0, a powinno być 10_000
2. Pytanie: czy to bug w SQL, service, kontrolerze, czy test data?
3. Odpalam DB oracle test na tym samym seed dataset:
   - service.summarize(merchantId, request) → też 0? → bug w SQL lub service
   - service.summarize(merchantId, request) → 10_000? → bug w kontrolerze lub HTTP layer
4. Jeśli bug w SQL:
   - EXPLAIN ANALYZE pokazuje plan zapytania
   - Sprawdzam WHERE clause — może filtr odrzuca wszystkie wiersze
5. Jeśli bug w service:
   - Debugger na normalizacji null → 0
6. Jeśli bug w HTTP layer:
   - Sprawdzam serializację JSON, response mapping, header
```

## 6. Learning Delta — Co Nowe vs Lessons 06-09

| Temat | Lesson 06-09 | Lesson 10 |
|---|---|---|
| Test oracle | API oracle (REST Assured) | + DB oracle (repository/service test) |
| Diagnostyka | "test nie przechodzi" | + "EXPLAIN pokazuje dlaczego" |
| Test isolation | `@SpringBootTest` (cały stack) | + `@DataJpaTest` (tylko JPA) |
| Agregacja | Lesson 08: `GROUP BY`, `COUNT`, `SUM` | + diagnostyka: kiedy indeks, kiedy seq scan |
| EXPLAIN | wspomniany w planie, nie zrobiony | faktyczne ćwiczenie diagnostyczne |
| Test data | API tworzy dane przez POST | DB oracle: persist bezpośrednio przez `TestEntityManager` |
| Null semantics | `COALESCE` w SQL (Lesson 08) | + test izolujący normalizację w service |

## 7. Typowe Błędy

1. **Używanie DB oracle do testowania HTTP.** DB oracle nie sprawdza status code, nagłówków, JSON serializacji — tylko logikę agregacji.
2. **EXPLAIN jako automatyczna asercja.** Plan zapytania zmienia się z danymi i wersją PostgreSQL. Nie asercjonuj `cost=...` ani `actual time=...`.
3. **Zapominanie o `COALESCE` w teście.** Pusta tabela → `SUM` zwraca `NULL`. Service musi to normalizować.
4. **TestEntityManager vs Repository.** `TestEntityManager.persistAndFlush(...)` zapisuje i flushuje natychmiast. `repository.save(...)` może nie flushować przed zapytaniem.
5. **Date boundary: mylenie `exclusive` z `inclusive`.** `fromDate` jest inclusive start-of-day (`>=`), `toDate` jest inclusive end-of-day (`<=`).
6. **Test na service zamiast na repository.** Service testuje orchestration (walidacja + wywołanie repo + normalizacja). Repository test testuje tylko SQL.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Odpal `EXPLAIN ANALYZE` dla 3 zapytań summary — zapisz czy używają indeksów | 30 min |
| 2 | Napisz `@DataJpaTest` który sprawdza agregację dla 0, 1, 5 orderów z różnymi walutami | 45 min |
| 3 | Porównaj wynik `service.summarize(...)` z wynikiem REST Assured dla tego samego merchanta | 30 min |
| 4 | Test na `fromDate` / `toDate` inclusive UTC — stwórz ordery o `23:59:59Z` i sprawdź czy są w summary | 30 min |
| 5 | Dodaj indeks dla `merchant_id, created_at` (już jest w V2) — EXPLAIN pokazuje Index Scan? | 20 min |
| 6 | Użyj `TestEntityManager` do persist 3 orderów i porównaj z `repository.save(...)` — jaka różnica w flush? | 20 min |

## 9. Pytania

1. Kiedy warto użyć DB oracle zamiast API oracle?
2. Jak `EXPLAIN` pomaga zdiagnozować wolne zapytanie agregacyjne?
3. Jaka jest różnica między `Seq Scan` a `Index Scan` w planie zapytania?
4. Dlaczego `COALESCE(SUM(...), 0)` jest potrzebne?
5. Jak `@DataJpaTest` różni się od `@SpringBootTest(webEnvironment = RANDOM_PORT)`?
6. Co robi `TestEntityManager.persistAndFlush(...)` czego nie robi `repository.save(...)`?
7. Dlaczego date filter używa `>=` i `<=` (inclusive), a nie `>` i `<` (exclusive)?
8. Jak przetestować, że `fromDate` i `toDate` są traktowane jako UTC, a nie lokalna strefa czasowa?
9. Co robić gdy API zwraca zły wynik agregacji, ale DB oracle test przechodzi?
10. Czy `EXPLAIN ANALYZE` nadaje się do CI? Dlaczego nie?

## 10. Testy (Opcjonalny Lesson 10C)

| Test | Co sprawdza |
|---|---|
| `emptyMerchantAggregationReturnsZeroTotals` | COALESCE normalizacja |
| `multiCurrencyAggregationMatchesPersistedState` | GROUP BY currency |
| `statusAggregationMatchesPersistedState` | GROUP BY status |
| `fromDateBoundaryIncludesStartOfDay` | inclusive date semantics |
| `toDateBoundaryIncludesEndOfDay` | inclusive date semantics |
| `crossDateBoundaryExcludesOrdersOutsideRange` | date filter precision |

## 11. Powiązane Notatki

- [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]]
- [[Lesson 06D - SQL and Flyway Constraints for Payment Orders]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Senior SDET Competency Coverage Matrix]]
