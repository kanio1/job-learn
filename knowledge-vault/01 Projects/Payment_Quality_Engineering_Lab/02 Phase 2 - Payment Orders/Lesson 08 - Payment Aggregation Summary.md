---
type: lesson
status: in-progress
project: Payment Quality Engineering Lab
phase: 2
lesson: 8
area: Payment Orders
module: Payment Aggregation Summary
date: 2026-05-30
tags:
  - lesson
  - lesson-08
  - payment-quality-lab
  - payment-order
  - aggregation
  - sql
  - postgresql
  - rest-assured
  - assertj
  - security-testing
  - nuxt
  - senior-sdet
---

# Lesson 08 - Payment Aggregation Summary

> **Status:** IN PROGRESS — backend system slice implemented; final package/modulith verification blocked by existing test source compilation errors
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Curriculum Backbone]] | [[Lesson Evidence Tracker]]
>
> **Main decision:** Lesson 08 is a read-only aggregation extension of the existing `payment` module, not a payment lifecycle sprint.

## 1. Cel Lekcji

Lekcja 08 uczy, jak projektować i testować agregacje danych w REST API: `GROUP BY`, `COUNT`, `SUM`, kontrolowane dane testowe, agregacyjne oracle w AssertJ oraz podstawy `EXPLAIN`.

Nie przechodzimy jeszcze do `authorize`, `capture`, `cancel`, PSP, Kafka ani pełnego dashboardu. Budujemy mały, merchant-scoped read model dla istniejących payment orders.

## 2. Co Budujemy / Co Ćwiczymy

Capability:

- `Payment Order Summary` dla jednego merchanta.
- Read-only endpoint: `GET /api/merchants/{merchantId}/payment-orders/summary`.
- Agregacja istniejących danych z `payment_orders`.
- Role takie jak w Lesson 07: `merchant:payments:read` i `platform:payments:read`.
- Frontend jako opcjonalny, minimalny merchant-scoped panel: summary cards + link do listy, bez pełnego business dashboardu.

Proponowany response:

```json
{
  "totalOrders": 3,
  "totalAmountMinor": 6000,
  "byCurrency": [
    { "currency": "PLN", "orderCount": 2, "totalAmountMinor": 3000 },
    { "currency": "EUR", "orderCount": 1, "totalAmountMinor": 3000 }
  ],
  "byStatus": [
    { "status": "CREATED", "orderCount": 3, "totalAmountMinor": 6000 }
  ]
}
```

## 2a. Implementation Status (2026-05-30)

System slice (backend) completed for summary endpoint:

- Implemented `PaymentOrderSummaryRequest`, `PaymentOrderSummaryResponse`, and `PaymentOrderSummaryService`.
- Added DB-side aggregation projections and queries in `JpaPaymentOrderRepository`.
- Added `GET /api/merchants/{merchantId}/payment-orders/summary` in `PaymentOrderController` with `X-Correlation-ID` response header.
- Enforced access boundary parity with list endpoint (`merchant:payments:read` and `platform:payments:read`) and merchant claim ownership check.
- Added explicit security matcher ordering in `SecurityConfig` so `/summary` is not captured by single-resource wildcard.

Verification evidence for this slice:

- `./mvnw clean compile` -> `BUILD SUCCESS`.
- `./mvnw -DskipTests package` -> `BUILD FAILURE` at `testCompile`.
- `./mvnw -Dtest=PaymentModuleTest test` -> `BUILD FAILURE` at `testCompile`.
- Blocking file: `apps/backend/src/test/java/lab/paymentquality/rest/MyPaymentOrderBusinessFlowRestAssuredTest.java` (`';' expected` and `illegal start of expression`).

Scope guardrails preserved:

- No `POST /payments`, no payment lifecycle actions (`authorize/capture/cancel`), no PSP integration, no Kafka/webhooks.
- No new payment status introduced.
- No `V4` Flyway migration added.

## 3. Learning Delta Względem Poprzednich Lekcji

| Temat | Status |
|---|---|
| `GROUP BY` po `currency` i `status` | Nowy |
| `COUNT(*)` jako business total, nie tylko page metadata | Rozszerzenie Lesson 07 |
| `SUM(amount_minor)` i pieniądze bez `double` | Nowy |
| Repository projection / DTO dla agregacji | Nowy |
| Agregacyjny test oracle z kontrolowanym seed dataset | Nowy |
| AssertJ `tuple()` i `SoftAssertions` dla wielu agregatów | Praktyka po Lesson 07 |
| `EXPLAIN` jako diagnostyka query | Nowy |
| Merchant tenant isolation dla endpointu raportowego | Rozszerzenie Lesson 06-07 |
| `RequestSpecBuilder` / `ResponseSpecBuilder` | Używamy, nie tłumaczymy od zera |
| `given()` / `when()` / `then()` / path params / body basics | Nie powtarzamy |

## 4. Mapa Kodu

Planowane pliki produkcyjne:

| Plik | Po co istnieje |
|---|---|
| `payment/internal/web/PaymentOrderSummaryResponse.java` | REST DTO dla totals, `byCurrency`, `byStatus` |
| `payment/internal/application/PaymentOrderSummaryService.java` | `@Transactional(readOnly = true)`, orkiestruje agregację i filtry |
| `payment/internal/infrastructure/PaymentOrderSummaryRepository.java` lub metody w `JpaPaymentOrderRepository` | Jawne query/projection dla `GROUP BY` |
| `payment/internal/web/PaymentOrderController.java` | Nowy `GET /summary` |
| `shared/security/SecurityConfig.java` | Matcher dla summary przed matcherem single-resource |
| `db/migration/payment/V4__add_payment_order_summary_indexes.sql` | Tylko jeśli query uzasadnia nowy indeks |

Planowane pliki testowe:

| Plik | Po co istnieje |
|---|---|
| `rest/PaymentOrderSummaryRestAssuredTest.java` | Główny kontrakt API summary |
| `security/PaymentOrderSummarySecurityTest.java` | 401/403/own merchant/platform matrix |
| `testsupport/PaymentOrderSummaryApiTestSupport.java` | Kontrolowane seed data i reusable specs |
| `payment/internal/infrastructure/JpaPaymentOrderSummaryRepositoryTest.java` | Opcjonalnie: query/projection na PostgreSQL/Testcontainers |

Frontend opcjonalny po backendzie:

| Plik | Po co istnieje |
|---|---|
| `server/api/merchants/[merchantId]/payment-orders/summary.get.ts` | Nuxt server proxy do backend summary |
| `app/components/payment/PaymentOrderSummaryCards.vue` | 3-4 edukacyjne karty: total, by currency/status |
| `app/pages/admin/merchants/[merchantId]/payments/index.vue` | Minimalny merchant-scoped payments panel |
| `app/schemas/payment-order.schema.ts` | Zod schema dla summary response |
| `app/stores/payment-orders.ts` | Stan summary/list bez lifecycle actions |

## 5. Architecture Walkthrough

Module owner: istniejący Spring Modulith module `payment`.

Decyzje:

- Nie tworzymy nowego modułu.
- Nie dodajemy nowych payment statusów tylko po to, żeby raport wyglądał ciekawiej.
- Summary jest read-only projection nad `payment_orders`.
- Transaction boundary: `PaymentOrderSummaryService` z `@Transactional(readOnly = true)`.
- Repository powinno liczyć agregaty w DB, nie pobierać encji do Javy i sumować w pamięci.
- Endpoint używa tych samych ról czytających co lista z Lesson 07.

Spec Kit decision:

- Według [[Current Learning Flow]] nowy endpoint w tym samym module może być Lesson Extension bez Full Spec Kit.
- Jeśli chcemy zachować formalny ślad jak Lesson 07, wystarczy Light Spec / prompt wykonawczy, nie Full Spec Kit z nową fazą.

## 6. HTTP I REST API

Endpoint:

```http
GET /api/merchants/{merchantId}/payment-orders/summary
Accept: application/json
Authorization: Bearer <token>
```

Opcjonalne query params w minimalnym scope:

| Param | Znaczenie |
|---|---|
| `currency` | Ogranicza summary do jednej waluty: `PLN`, `EUR`, `USD` |
| `status` | Na teraz tylko `CREATED` |
| `fromDate` | Inclusive start-of-day |
| `toDate` | Inclusive end-of-day |

Status codes:

| Condition | Status | Error |
|---|---|---|
| Success | `200 OK` | — |
| Empty merchant | `200 OK` z zerowymi totals | — |
| Invalid query param | `400 Bad Request` | `validation` |
| Unauthenticated | `401 Unauthorized` | — |
| No read role | `403 Forbidden` | `forbidden` |
| Merchant reader cross-tenant | `403 Forbidden` | `forbidden` |
| Platform reader for selected merchant | `200 OK` | — |

Headers:

- `X-Correlation-ID` wymagany w każdej odpowiedzi.
- Bez `ETag` i `If-Match`; summary jest read-only i nie wprowadza optimistic concurrency.

## 7. Java 25 I Java Code Reading

Nowe wzorce do nauki:

- Records jako immutable DTO: `PaymentOrderSummaryResponse`, `CurrencySummary`, `StatusSummary`.
- Projection query jako granica między SQL a API DTO.
- `long` dla `amountMinor` i totals, bez `double`.
- Minimalna widoczność i brak nowego public API poza REST.

Pytanie QA:

- Czy suma z API jest policzona na tej samej populacji danych, którą test faktycznie zasiała?

## 8. SQL, PostgreSQL I Flyway

Główne SQL do zrozumienia:

```sql
SELECT currency, COUNT(*) AS order_count, SUM(amount_minor) AS total_amount_minor
FROM payment_orders
WHERE merchant_id = ?
GROUP BY currency
ORDER BY currency;
```

```sql
SELECT status, COUNT(*) AS order_count, SUM(amount_minor) AS total_amount_minor
FROM payment_orders
WHERE merchant_id = ?
GROUP BY status
ORDER BY status;
```

EXPLAIN exercise:

```sql
EXPLAIN
SELECT currency, COUNT(*), SUM(amount_minor)
FROM payment_orders
WHERE merchant_id = '...'
GROUP BY currency;
```

Index decision:

- Existing Lesson 07 indexes cover `merchant_id, status` and `merchant_id, currency`.
- Add V4 only if `created_at` date-filtered summary needs a clearer composite index.
- Do not add materialized views or reporting tables in Lesson 08.

## 9. Security I Tenant Isolation

Security matrix:

| Actor | Own Merchant Summary | Other Merchant Summary | Expected |
|---|---:|---:|---|
| Unauthenticated | — | — | `401` |
| Denied identity | No | No | `403` |
| `merchant:payments:create` only | No | No | `403` |
| `merchant:payments:operate` only | No | No | `403` |
| `merchant:payments:read` + matching `merchant_id` | Yes | No | `200` / `403` |
| `platform:payments:read` | Yes | Yes | `200` |

Decision:

- Cross-tenant summary returns `403`, like list, because summary is an overt collection/report operation.
- Single-resource masked `404` from Lesson 06 remains unchanged.

## 10. REST Assured Learning Path

Recommended contract tests:

| # | Test | Co weryfikuje |
|---:|---|---|
| 1 | `summaryForEmptyMerchantReturnsZeroTotals` | Empty dataset is valid `200`, not `404` |
| 2 | `summaryForSeededMerchantReturnsCountsAndSums` | Controlled dataset -> exact totals |
| 3 | `summaryGroupsByCurrency` | `byCurrency` contains expected tuples |
| 4 | `summaryGroupsByStatus` | `byStatus` contains `CREATED` totals |
| 5 | `summaryFilteredByCurrencyAffectsTotals` | Query param changes aggregation population |
| 6 | `summaryResponseIncludesCorrelationId` | Observability contract |
| 7 | `invalidSummaryFilterReturns400` | Stable validation error |
| 8 | `merchantReaderCannotSummarizeOtherMerchant` | Tenant isolation |
| 9 | `platformReaderCanSummarizeSelectedMerchant` | Platform support read |
| 10 | `creatorWithoutReadCannotSummarize` | BFLA protection |

## 11. Assertion Strategy

| Sytuacja | Narzędzie | Dlaczego |
|---|---|---|
| HTTP status/header/error shape | REST Assured `.then()` | Kontrakt protokołu |
| Cały response DTO | `extract().as(PaymentOrderSummaryResponse.class)` | Type-safe oracle |
| Wiele pól totals naraz | AssertJ `SoftAssertions` | Jedna porażka pokazuje wszystkie różnice |
| Grupy currency/status | AssertJ `extracting(...).contains(tuple(...))` | Czytelny oracle agregacyjny |
| Wątpliwość co liczy DB | SQL/Repository test | DB jako oracle tylko gdy badamy query |

## 12. Test Data Ownership

Controlled seed dataset powinien być mały i przewidywalny:

| Order | Currency | Amount minor | Status |
|---|---|---:|---|
| A | PLN | 1000 | CREATED |
| B | PLN | 2000 | CREATED |
| C | EUR | 3000 | CREATED |
| D | USD | 4000 | CREATED |

Expected totals:

- `totalOrders = 4`
- `totalAmountMinor = 10000`
- `PLN = 2 / 3000`
- `EUR = 1 / 3000`
- `USD = 1 / 4000`
- `CREATED = 4 / 10000`

Rules:

- Każdy test tworzy własnego merchanta.
- Nie używamy globalnych fixtures współdzielonych między testami.
- `clientOrderReference` i `Idempotency-Key` pozostają unikalne.
- Expected totals powinny być jawnie policzone w teście albo w małym immutable expected object.

## 13. Pytania Do Samodzielnej Odpowiedzi

1. Dlaczego agregację liczymy w PostgreSQL, a nie w Javie po pobraniu wszystkich encji?
2. Co oznacza `GROUP BY currency`?
3. Jaka jest różnica między `COUNT(*)` w paginacji Lesson 07 a `COUNT(*)` w summary Lesson 08?
4. Dlaczego `SUM(amount_minor)` jest bezpieczniejsze niż sumowanie kwot jako `double`?
5. Dlaczego empty summary zwraca `200` zamiast `404`?
6. Dlaczego cross-tenant summary powinno zwracać `403`, a single-resource read z Lesson 06 maskowane `404`?
7. Kiedy API response jest wystarczającym oracle, a kiedy potrzebny jest DB/repository test?
8. Jak `EXPLAIN` pomaga testerowi znaleźć ryzyko wydajnościowe?
9. Które indeksy z Lesson 07 pomagają Lesson 08?
10. Jak uniknąć flaky aggregation testów przy równoległym uruchamianiu?

### Odpowiedzi

1. PostgreSQL liczy agregację bliżej danych i nie wymaga pobierania wszystkich encji do JVM. To jest szybsze, mniej pamięciożerne i lepiej skaluje się dla dużych merchantów.
2. `GROUP BY currency` grupuje rekordy według waluty, a agregaty liczy osobno dla każdej grupy. Dzięki temu dostajesz np. osobne sumy dla PLN, EUR i USD.
3. `COUNT(*)` w paginacji liczy wszystkie rekordy pasujące do filtrów, żeby wyliczyć metadane strony. `COUNT(*)` w summary jest częścią raportu biznesowego, np. liczba orderów w agregacji.
4. `SUM(amount_minor)` sumuje liczby całkowite, więc nie ma błędów floating-point. `double` może dać niedeterministyczne groszowe różnice.
5. Empty summary to poprawny wynik pustego zbioru, nie brak endpointu ani zasobu. Dlatego `200` z zerami jest czytelniejsze niż `404`.
6. Summary jest operacją kolekcji/raportu, więc `403` jasno odmawia dostępu do zakresu merchanta. Single read maskuje `404`, bo dotyczy konkretnego ID i ryzyka enumeracji zasobów.
7. API response wystarcza, gdy testujesz kontrakt: status, body i grupy wyników. DB/repository test jest potrzebny, gdy chcesz udowodnić SQL, constraints albo edge case agregacji niezależnie od HTTP.

```java
assertThat(response.byCurrency())
    .extracting("currency", "totalAmountMinor")
    .contains(tuple("PLN", 3000L), tuple("EUR", 5000L));
```

8. `EXPLAIN` pokazuje plan zapytania: scan, filter, group i użyte indeksy. Tester widzi, czy endpoint ma ryzyko full table scan przy większych danych.
9. Najbardziej pomagają indeksy po `merchant_id` oraz kombinacje wspierające filtry status/currency/date. Summary musi najpierw zawęzić dane do merchanta, a dopiero potem agregować.
10. Każdy test powinien mieć własnego merchanta i kontrolowany seed dataset. Nie opieraj agregacji na współdzielonych danych ani kolejności wykonania testów.

## 14. Zadania Praktyczne

| Zadanie | Files | Command | Expected |
|---|---|---|---|
| Zaprojektuj controlled seed dataset | `PaymentOrderSummaryRestAssuredTest` | — | Expected totals zapisane przed requestem |
| Napisz test empty summary | REST test | `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest test` | `200`, totals zero |
| Napisz test byCurrency | REST test | same | AssertJ tuples dla PLN/EUR/USD |
| Napisz security matrix | Security test | `./mvnw -Dtest=PaymentOrderSummarySecurityTest test` | 401/403/200 zgodne z tabelą |
| Uruchom EXPLAIN lokalnie | psql/Testcontainers log/manual SQL | manual | Umiesz wskazać filter, group, index |

### Rozwiązania / wskazówki

1. Controlled seed dataset powinien zawierać małą liczbę orderów z przewidywalnymi walutami, statusami i kwotami. Expected totals zapisz w teście przed requestem, nie licz ich z response.
2. Empty summary powinien zwrócić `200`, `totalOrders=0` i puste lub zerowe grupy. To potwierdza, że pusty merchant jest poprawnym stanem biznesowym.
3. Test `byCurrency` powinien sprawdzać konkretne pary waluta + suma/liczba. Najlepszy oracle to AssertJ `tuple()`, bo weryfikuje kombinację pól.

```java
assertThat(summary.byCurrency())
    .extracting("currency", "orderCount", "totalAmountMinor")
    .containsExactlyInAnyOrder(
        tuple("PLN", 2L, 3000L),
        tuple("EUR", 1L, 5000L));
```

4. Security matrix powinna rozdzielać brak tokena (`401`), brak roli (`403`) i cross-tenant (`403`). Nie sprawdzaj tylko jednego denied case, bo BOLA i BFLA mają inne przyczyny.
5. W `EXPLAIN` szukaj, czy query filtruje po `merchant_id` przed agregacją. Jeśli widzisz kosztowny scan całej tabeli, to jest ryzyko performance dla dużej liczby orderów.

## 15. Mini Interview Prep

**Q1: Why did you add a summary endpoint after list/filter?**

Because list/filter teaches collection retrieval and pagination, while summary teaches database aggregation, reporting-style API contracts, and aggregate test oracles. It is the next read-only step without introducing payment lifecycle complexity.

**Q2: How do you test an aggregation endpoint reliably?**

I seed a small isolated dataset with known currencies, statuses and amounts, call the API, deserialize the response into a typed DTO, and assert totals and grouped rows with AssertJ tuples or soft assertions. Each test owns its merchant data to avoid cross-test contamination.

**Q3: Why use minor units for money totals?**

Minor units avoid floating-point rounding errors. The API sums integer `amountMinor` values and can later format display values at the UI boundary.

**Q4: Why does cross-tenant summary return 403?**

Summary is a collection/report operation. Returning `403` clearly communicates that the authenticated caller is not allowed to access that merchant scope without exposing individual resource existence.

**Q5: What does EXPLAIN add for an SDET?**

EXPLAIN helps connect API behavior to query shape and index usage. It turns performance and scalability assumptions into inspectable evidence, even before full performance testing.

## 16. Verification Commands

```bash
# Backend summary REST contract
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest test

# Backend summary security
./mvnw -Dtest=PaymentOrderSummarySecurityTest test

# All payment tests
./mvnw -Dtest="PaymentOrder*" test

# Modulith architecture
./mvnw -Dtest=PaymentModuleTest test

# Frontend typecheck, only if frontend files are touched
corepack pnpm typecheck
```

Run backend commands from `apps/backend`; run frontend command from `apps/frontend`.

## 17. Learning Outcome Checklist

Po tej lekcji umiem:

- [ ] Wyjaśnić różnicę między list endpointem a summary endpointem.
- [ ] Napisać SQL `GROUP BY` z `COUNT` i `SUM` dla merchant-scoped danych.
- [ ] Zaprojektować controlled seed dataset dla agregacji.
- [ ] Użyć AssertJ `tuple()` albo `SoftAssertions` jako aggregation oracle.
- [ ] Rozpoznać, kiedy potrzebny jest REST test, a kiedy repository/DB test.
- [ ] Wyjaśnić `403` dla cross-tenant summary.
- [ ] Przeczytać podstawowy `EXPLAIN` i wskazać ryzyko indeksów.

## 18. Powiązane Notatki W Vault

### Lekcje opisowe Lesson 08 (pogłębione materiały)
- [[Lesson 08 - Java Records, Read-Only Services, and Input Validation]] — Java
- [[Lesson 08 - Aggregation Contract, Security, and Business Flow Tests]] — REST Assured
- [[Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries]] — SQL/PostgreSQL
- [[Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy]] — HTTP/API kontrakt
- [[Lesson 08 - Business Logic, Decision Tables, and Risk Notes]] — logika biznesowa

### Pozostałe
- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 07 - Payment Order List Filter Search]]
- [[Current Learning Flow]]
- [[Curriculum Backbone]]
- [[Learning Coverage Backlog]]
- [[Lesson Evidence Tracker]]
- [[Prompt - Lesson 08 - Payment Aggregation Summary]]
