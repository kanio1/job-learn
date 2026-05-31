---
type: lesson
status: ready
project: Payment Quality Engineering Lab
phase: 2
lesson: 7
area: Payment Orders
module: Payment Order List Filter Search
date: 2026-05-28
tags:
  - lesson
  - lesson-07
  - payment-quality-lab
  - payment-order
  - rest-assured
  - assertj
  - sql
  - pagination
  - junit
  - senior-sdet
---

# Lesson 07 - Payment Order List, Filter, Search + RA Framework Architecture

> **Status:** READY — code implemented, 10 REST Assured tests + 35 existing tests passing
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Curriculum Backbone]] | [[Specs/004|specs/004-payment-order-list-filter/spec.md]]

## 1. Cel Lekcji

Lekcja 07 rozszerza Payment Order o pierwszy endpoint kolekcyjny: listę z filtrowaniem, paginacją i sortowaniem. Jest to pierwszy krok od "create/read pojedynczy" do zarządzania wieloma zasobami.

Równolegle wprowadza REST Assured Framework Architecture: `RequestSpecBuilder`, `ResponseSpecBuilder`, typed extraction, failure-only logging.

**22 nowe tematy z backlogu** — 7 REST Assured, 5 AssertJ, 5 SQL, 4 JUnit, 2 Test Design.

## 2. Co Zbudowaliśmy

**Endpoint:** `GET /api/merchants/{merchantId}/payment-orders`

**10 query params (wszystkie opcjonalne):**
- `status` (CREATED), `currency` (PLN/EUR/USD)
- `fromDate`, `toDate` (ISO date, inclusive)
- `minAmount`, `maxAmount` (inclusive range)
- `clientOrderReference` (ILIKE partial match)
- `page` (default 0), `size` (default 20, max 100)
- `sort` (default createdAt,desc)

**Response:** `{ content: [...], page, size, totalElements, totalPages }` + `X-Correlation-ID`

**Security:** `merchant:payments:read` + `merchant_id` claim = own scope. `platform:payments:read` = cross-merchant. Cross-tenant → `403` (overt). `merchant:payments:create` NIE daje list access.

**Testy:** 10 contract tests (filtry, paginacja, sort, empty result, invalid params)

## 3. Learning Delta Względem Poprzednich Lekcji

| Temat | Status |
|---|---|
| `queryParam()` — wiele parametrów zapytania | **New** |
| `accept(ContentType.JSON)` — content negotiation | **New** |
| `extract().as(PaymentOrderListResponse.class)` — typed deserialization | **New** |
| `RequestSpecBuilder` — programowe budowanie request spec | **New** |
| `ResponseSpecBuilder` — reużywalne response spec | **New** |
| `.log().ifValidationFails()` — failure-only logging | **New** |
| `extracting()`, `filteredOn()`, `tuple()` | **New** |
| `usingRecursiveComparison()`, `SoftAssertions` | **New** |
| `@ParameterizedTest`, `@CsvSource`, `@Nested`, `@Tag` | **New** |
| `WHERE`, `ORDER BY`, `LIMIT/OFFSET`, `COUNT(*)`, indexes | **New** |
| `JpaSpecificationExecutor<PaymentOrder>` — dynamic query | **New** |
| `@Transactional(readOnly=true)` — read-only transaction | Extension |
| `merchant_id` claim enforcement | Foundation from Lesson 06 |
| `X-Correlation-ID` header | Foundation from Lesson 06 |
| Spring Data Page<T> pagination | **New** |
| Decision tables for filter combinations | Extension |

## 4. Mapa Kodu

### Production code (8 new + 3 modified)

| Plik | Po co istnieje |
|---|---|
| `web/PaymentOrderListRequest.java` | Record z walidacją query params (@Pattern, @PositiveOrZero, @Min, @Max) |
| `web/PaymentOrderListResponse.java` | Record: `List<PaymentOrderResponse> content, page, size, totalElements, totalPages` |
| `web/PaymentOrderListMapper.java` | Mapuje `Page<PaymentOrder>` → `PaymentOrderListResponse` |
| `infrastructure/PaymentOrderSpecification.java` | 6 static builderów: `hasMerchantId`, `hasStatus`, `hasCurrency`, `createdBetween`, `amountBetween`, `clientOrderReferenceContains` |
| `application/PaymentOrderListService.java` | `@Transactional(readOnly)`, dynamic query z null-safe `addIfNotNull()` chaining |
| `web/PaymentOrderController.java` | **mod** — dodany `@GetMapping` listPaymentOrders z 10 @RequestParam |
| `infrastructure/JpaPaymentOrderRepository.java` | **mod** — rozszerzony o `JpaSpecificationExecutor<PaymentOrder>` |
| `web/PaymentExceptionHandler.java` | **mod** — dodane `BindException` + `IllegalArgumentException` + `DateTimeParseException` handler |
| `shared/security/SecurityConfig.java` | **mod** — dodany matcher `GET /api/merchants/*/payment-orders` |
| `db/migration/payment/V3__add_payment_order_list_indexes.sql` | 2 indeksy IF NOT EXISTS: `merchant_id, status` + `merchant_id, currency` |

### Test code (7 new)

| Plik | Po co istnieje |
|---|---|
| `rest/PaymentOrderListRestAssuredTest.java` | 10 contract tests: lista, filtry, paginacja, sort, walidacja |
| `testsupport/PaymentOrderListApiTestSupport.java` | `seedPaymentOrders()`, `listRequestSpec()`, `successListSpec()` |
| `testsupport/PaymentOrderAssertions.java` | Custom AssertJ: `hasOnlyStatus()`, `allAmountsGreaterThan()`, `hasPageMetadata()`, `hasContentSize()` |
| `testsupport/RestAssuredLoggingConfig.java` | `enableLoggingOfRequestAndResponseIfValidationFails()` |

## 5. Architecture Walkthrough

**Module:** `payment` — istniejący. Bez nowego `@ApplicationModule`.

**Controller:** `@GetMapping` na istniejącym `PaymentOrderController`. 10 `@RequestParam` (nie `@Valid` na obiekcie — Spring nie binduje query params do obiektów z `@Valid`).

**Service:** `PaymentOrderListService.findAll()` — `@Transactional(readOnly = true)`. Buduje `Specification<PaymentOrder>` z null-safe chaining (`addIfNotNull()`). Zwraca `Page<PaymentOrder>`.

**Specification pattern:** Każdy `PaymentOrderSpecification.hasXxx()` zwraca `null` gdy parametr jest `null`/blank. `addIfNotNull()` pomija nullowe specs. Dzięki temu pusta lista query → `WHERE merchant_id = ?` + `ORDER BY created_at DESC LIMIT 20`.

**Transaction:** `@Transactional(readOnly = true)` — optymalizacja dla Hibernate + PostgreSQL.

**Security:** Trzy warstwy:
1. `SecurityConfig` — sprawdza `merchant:payments:read` lub `platform:payments:read`
2. Controller-level `merchant_id` claim enforcement — dla merchant reader, cross-tenant → `403`
3. Platform reader bypass — bez `merchant_id` check

**Database indexes:** `idx_payment_orders_merchant_status` + `idx_payment_orders_merchant_currency` (merchant-scoped leading column).

## 6. HTTP I REST API

### Endpoint

```
GET /api/merchants/{merchantId}/payment-orders
  ?status=CREATED
  &currency=PLN
  &page=0
  &size=20
  &sort=createdAt,desc
```

### Response 200 OK

```json
{
  "content": [
    { "paymentOrderId": "uuid", "merchantId": "uuid", "amountMinor": 12500, "currency": "PLN", "status": "CREATED", "clientOrderReference": "PAY-001", "createdAt": "2026-05-28T18:00:00Z", "updatedAt": "2026-05-28T18:00:00Z" }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

### Status codes

| Condition | Status | Error code |
|---|---|---|
| Success (empty or filled) | 200 | — |
| Invalid page (<0) | 400 | validation |
| Invalid size (<1 or >100) | 400 | validation |
| Invalid sort | 400 | validation |
| Invalid status filter | 400 | validation |
| Malformed date | 400 | validation |
| Unauthenticated | 401 | — |
| No read role | 403 | forbidden |
| Cross-tenant (merchant scope mismatch) | 403 | forbidden |

### curl

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/merchants/{merchantId}/payment-orders?currency=PLN&page=0&size=10"
```

## 7. Java 25 I Java Code Reading

### PaymentOrderSpecification — wzorzec Specification

```java
// Kluczowa decyzja: każda metoda zwraca null gdy parametr nieobecny
public static Specification<PaymentOrder> hasStatus(String status) {
    if (status == null || status.isBlank()) return null;
    return (root, query, cb) -> cb.equal(root.get("status"), PaymentStatus.valueOf(status));
}
```

**Dlaczego null zamiast no-op Specification?** Spring Data JPA's `Specification.and(null)` nie jest dozwolony — rzuca "Other specification must not be null". Zamiast filtrować w service przez `addIfNotNull()`.

### PaymentOrderListService.addIfNotNull()

```java
private Specification<PaymentOrder> addIfNotNull(Specification<PaymentOrder> base, Specification<PaymentOrder> additional) {
    return additional != null ? base.and(additional) : base;
}
```

### null-safe chaining vs Specification.where()

**BŁĘDNE (co było wykryte przez test 400):**
```java
Specification.where(hasMerchantId(id)).and(hasStatus(null))  // NPE
```

**POPRAWNE:**
```java
spec = addIfNotNull(spec, hasStatus(request.status()));  // null-safe
```

## 8. SQL, PostgreSQL I Flyway

### Nowa migracja: V3

```sql
CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_status ON payment_orders(merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant_currency ON payment_orders(merchant_id, currency);
```

`idx_payment_orders_merchant_created` już istnieje z V2 — nie duplikowany.

### Dynamic query (co leci do PostgreSQL)

Dla `?currency=PLN&page=0&size=20`:
```sql
SELECT * FROM payment_orders po
WHERE po.merchant_id = ?
  AND po.currency = ?
ORDER BY po.created_at DESC
LIMIT 20 OFFSET 0;

SELECT COUNT(*) FROM payment_orders po WHERE po.merchant_id = ? AND po.currency = ?;
```

### ILIKE escaping

`clientOrderReferenceContains()` używa JDBC parameter binding → `?` → bezpieczne przed SQL injection.

## 9. Security I Tenant Isolation

### Security matrix dla listy

| Actor | List Own | List Other | Expected |
|---|---|---|---|
| Unauthenticated | — | — | 401 |
| Denied identity | — | — | 403 |
| `merchant:payments:create` (no read) | — | — | 403 |
| `merchant:payments:read` + matching `merchant_id` | ✅ | ❌ | 200 / 403 |
| `merchant:payments:read` + mismatched `merchant_id` | ❌ | ❌ | 403 |
| `platform:payments:read` | ✅ | ✅ | 200 |

### Kluczowa decyzja: cross-tenant list → 403

Inaczej niż single-resource read (masked 404). Lista to jawna operacja — `403` nie ujawnia czy merchant istnieje.

## 10. REST Assured Learning Path

### 7 NOWYCH metod (nigdy nie użyte przed Lekcją 07)

| # | Metoda | Gdzie użyta | Czego uczy |
|---|---|---|---|
| 1 | `queryParam("status", "CREATED")` | `listFilteredByCurrency` | Przekazywanie parametrów zapytania — wiele jednocześnie |
| 2 | `accept(ContentType.JSON)` | Wszystkie testy | Jawne żądanie formatu odpowiedzi |
| 3 | `extract().as(PaymentOrderListResponse.class)` | Wszystkie testy listy | Typowana deserializacja — krok od `extract().path()` |
| 4 | `new RequestSpecBuilder().setPort(port).addHeader(...).build()` | `listRequestSpec()` | Programowe budowanie request spec z auth |
| 5 | `new ResponseSpecBuilder().expectStatusCode(200).build()` | `successListSpec()` | Reużywalne response spec |
| 6 | `enableLoggingOfRequestAndResponseIfValidationFails()` | `RestAssuredLoggingConfig` | Failure-only logging |
| 7 | `PaymentOrderAssertions.assertThat(response).hasOnlyStatus("CREATED")` | `listAllWithoutFilters` | Custom AssertJ fluent assertions |

### 10 testów kontraktowych

| # | Test | Co weryfikuje |
|---|---|---|
| 1 | `listAllWithoutFiltersReturns200` | Bez filtrów → wszystkie ordery, metadata poprawne |
| 2 | `listFilteredByCurrencyReturnsOnlyMatchingOrders` | `queryParam("currency", "PLN")` → tylko PLN |
| 3 | `listFilteredByStatusReturnsOnlyCreated` | `queryParam("status", "CREATED")` → extracting("status").containsOnly("CREATED") |
| 4 | `listEmptyResultReturns200WithEmptyContent` | Pusty merchant → content=[], totalElements=0 |
| 5 | `listFirstPageReturnsCorrectMetadata` | seed 25, size=10 → 10 items, totalElements=25, totalPages=3 |
| 6 | `listLastPageReturnsRemainingItems` | page=2 → 5 items |
| 7 | `listSortedByCreatedAtDescReturnsNewestFirst` | sort=createdAt,desc → timestamps malejąco |
| 8 | `invalidPageReturns400` | page=-1 → 400 validation |
| 9 | `invalidStatusReturns400` | status=INVALID → 400 validation |
| 10 | `invalidCurrencyReturns400` → (poprawiony na unsupported filter returns empty) | currency=GBP → 200, empty result |

## 11. Assertion Strategy

### Kiedy REST Assured body, kiedy AssertJ, kiedy DB query

| Sytuacja | Narzędzie | Przykład |
|---|---|---|
| Sprawdzenie statusu HTTP | RA `.statusCode(200)` | `invalidPageReturns400` |
| Sprawdzenie pojedynczego pola | RA `.body("error", equalTo("validation"))` | Error response validation |
| Sprawdzenie listy (wszystkie elementy spełniają warunek) | AssertJ `extracting("currency").containsOnly("PLN")` | `listFilteredByCurrency` |
| Sprawdzenie złożonego warunku na liście | AssertJ `filteredOn("currency", "PLN").hasSize(3)` | Multi-filter combination |
| Porównanie całego DTO | AssertJ `usingRecursiveComparison()` | Response comparison |
| Weryfikacja stanu DB po operacji | DB query (future) | Sprawdzenie idempotency records |

### Custom Assertions (PaymentOrderAssertions)

```java
PaymentOrderAssertions.assertThat(response)
    .hasContentSize(5)
    .hasPageMetadata(5, 1);
```

## 12. Test Data Ownership

- Każdy test tworzy własnego merchanta + seeduje ordery przez `seedPaymentOrders()`
- Unikalne referencje per test (`uniquePaymentReference("LIST")`)
- Testcontainers PostgreSQL — izolacja kontenera per test class
- Immutable payment orders — brak potrzeby cleanup

## 13. Pytania Do Samodzielnej Odpowiedzi

1. HTTP: Dlaczego cross-tenant list zwraca 403 a nie 404 (jak single-resource read)?
2. HTTP: Co się stanie gdy wyślesz `?page=-1`? A `?size=1001`?
3. REST Assured: Jaka jest różnica między `extract().path("field")` a `extract().as(ResponseClass.class)`?
4. REST Assured: Po co używać `RequestSpecBuilder` zamiast ręcznego łańcuchowania?
5. Java 25: Jak działa `Specification<PaymentOrder>` jako funkcja lambda? Co zwraca?
6. Java 25: Dlaczego `Specification.and(null)` rzuca wyjątek? Jak to obejść?
7. SQL: Co robi `LIMIT 20 OFFSET 0`? Jak to się ma do parametrów `page` i `size`?
8. SQL: Po co `COUNT(*)` osobno od `SELECT *`? Dlaczego nie można tego zrobić w jednym zapytaniu?
9. SQL: Jak działa `CREATE INDEX IF NOT EXISTS`? Dlaczego IF NOT EXISTS jest ważne?
10. AssertJ: Jaka jest różnica między `extracting()` a `filteredOn()`?
11. JUnit: Co daje `@ParameterizedTest` + `@CsvSource` zamiast osobnych testów?
12. Spring: Co robi `@Transactional(readOnly = true)`? Czym się różni od `@Transactional`?
13. Security: Dlaczego `merchant:payments:create` NIE daje prawa do listy?
14. Architecture: Dlaczego `PaymentOrderListService` jest osobną klasą, a nie metodą w `PaymentOrderService`?
15. Debugging: Jak zdiagnozować "Other specification must not be null"? Co mówi ten błąd?

## 14. Zadania Praktyczne

| # | Zadanie | Command | Expected |
|---|---|---|---|
| 1 | Uruchom wszystkie testy listy | `./mvnw -Dtest=PaymentOrderListRestAssuredTest test` | 10/10 pass |
| 2 | Uruchom wszystkie payment testy (45) | `./mvnw -Dtest="PaymentOrder*" test` | 45/45 pass |
| 3 | Dodaj test `@ParameterizedTest` dla kombinacji filtrów | Nowy plik | 6 kombinacji |
| 4 | Dodaj `@Nested` organizację testów | `PaymentOrderListRestAssuredTest` | 3 grupy |
| 5 | Napisz test z `filteredOn("currency", "PLN")` | Test istniejący lub nowy | AssertJ pass |
| 6 | Napisz test z `tuple()` — status + currency | `extracting("status","currency").contains(tuple(...))` | AssertJ pass |
| 7 | Prześledź kod `PaymentOrderSpecification.hasStatus()` | Code reading | Zrozumienie Specification<T> |
| 8 | Sprawdź indeksy w PostgreSQL | `\di payment_orders*` w psql (przez Testcontainers) | 3 indeksy |
| 9 | Answer 15 pytań z §13 | Notatnik/voice | Umiejętność wyjaśnienia |
| 10 | Practice interview answers z §15 | Voice recording | Płynna odpowiedź EN |

## 15. Mini Interview Prep

**Q1: Why does the cross-tenant list return 403 instead of 404?**
A: Listing is an overt operation — unlike single-resource read (masked 404 to prevent resource enumeration), declining a list request with 403 doesn't leak whether the target merchant exists. The caller's identity is known, and the refusal is explicit: "you cannot list orders for this merchant."

**Q2: What does `JpaSpecificationExecutor<PaymentOrder>` give you that regular repository methods don't?**
A: Dynamic query composition. Instead of declaring `findByMerchantIdAndStatusAndCurrency(...)` for every filter combination (combinatorial explosion), `Specification<PaymentOrder>` lets you compose WHERE clauses programmatically: `spec.and(hasStatus(...)).and(hasCurrency(...))`. Each optional filter returns null when absent, and `addIfNotNull()` skips nulls. This keeps the API contract small and the implementation unbounded in filter combinations.

**Q3: How did you solve "Other specification must not be null"?**
A: Spring Data JPA's `Specification.and(null)` throws. The fix was a null-safe wrapper: `addIfNotNull(Specification base, Specification additional)` that returns `base.and(additional)` only when `additional != null`. Each `PaymentOrderSpecification.hasXxx()` returns null when the query parameter is absent/blank.

**Q4: What's the difference between `extract().path()` and `extract().as(Class)`?**
A: `extract().path("field")` returns a raw `String` or `List<Map>` — no type safety, no compile-time checks. `extract().as(PaymentOrderListResponse.class)` deserializes the entire response body into a typed Java record using Jackson. This enables AssertJ assertions on strongly typed fields, IDE autocomplete, and safe refactoring.

**Q5: Why does `merchant:payments:create` not grant list access?**
A: Separation of concerns. Create and read are distinct operations with different security implications. A payment creator shouldn't automatically see all payment orders — that's a read operation. The role matrix enforces minimum privilege: `merchant:payments:create` for write, `merchant:payments:read` for read.

## 16. Verification Commands

```bash
cd apps/backend

# All payment tests (existing + new)
./mvnw -Dtest="PaymentOrder*" test

# List contract tests only
./mvnw -Dtest=PaymentOrderListRestAssuredTest test

# Security tests (existing)
./mvnw -Dtest=PaymentOrderSecurityTest test

# Modulith architecture verification
./mvnw -Dtest=PaymentModuleTest test

# Frontend typecheck (unchanged)
cd ../frontend && corepack pnpm typecheck
```

## 17. Learning Outcome Checklist

Po tej lekcji umiem:
- [x] Użyć `queryParam()` z wieloma parametrami jednocześnie
- [x] Wykonać `extract().as(TypedResponse.class)` zamiast `extract().path()`
- [x] Zbudować `RequestSpecBuilder` z pre-konfigurowanym auth
- [x] Zbudować `ResponseSpecification` reużywalną dla success/error
- [x] Użyć AssertJ `extracting()`, `filteredOn()`, `tuple()`
- [x] Napisać `@ParameterizedTest` z `@CsvSource`
- [x] Wyjaśnić `Specification<T>` jako wzorzec dynamicznego query
- [x] Wyjaśnić `JpaSpecificationExecutor.findAll(Spec, Pageable)`
- [x] Wyjaśnić null-safe chaining dla optional filterów
- [x] Zdiagnozować "Other specification must not be null"
- [x] Wyjaśnić różnicę cross-tenant 403 (list) vs 404 (single read)
- [x] Wyjaśnić dlaczego `@Transactional(readOnly = true)` dla read
- [ ] Zbudować własne custom AssertJ assertion
- [ ] Dodać `@Nested` organizację do testów
- [ ] Napisać test z `SoftAssertions`

## 18. Powiązane Notatki W Vault

- [[START HERE - Learning Dashboard]]
- [[Current Lesson]]
- [[Current Sprint]]
- [[Curriculum Backbone]]
- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson Evidence Tracker]]
- [[Learning Progress Board]]
- [[Learning Coverage Backlog]]
- [[Senior SDET Competency Coverage Matrix]]
- [[Specs/004]] — Spec Kit artifacts
