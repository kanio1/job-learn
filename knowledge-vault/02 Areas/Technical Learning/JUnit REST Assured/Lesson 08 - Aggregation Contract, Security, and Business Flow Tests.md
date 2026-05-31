---
type: lesson
status: ready
area: JUnit REST Assured
lesson: 08
module: Aggregation Contract, Security Matrix, and Business Flow Tests
date: 2026-05-30
tags:
  - rest-assured
  - assertj
  - junit
  - aggregation-testing
  - security-testing
  - lesson-08
  - payment-order-summary
  - senior-sdet
---

# Lesson 08 — Aggregation Contract, Security Matrix, and Business Flow Tests

> **Evidence link:**
> - `PaymentOrderSummaryRestAssuredTest.java` — kontrakt + filtry + walidacja
> - `PaymentOrderSummarySecurityTest.java` — macierz 401/403/200
> - `PaymentOrderSummaryBusinessFlowRestAssuredTest.java` — seed → oracle → assertions
> - `PaymentOrderSummaryApiTestSupport.java` — wspólny support
>
> **Navigation:** [[JUnit REST Assured MOC]] | [[Lesson 08 - Payment Aggregation Summary]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Zaprojektować **trzy warstwy testów REST** dla agregacyjnego endpointu summary:
- **kontrakt API**: shape, statusy, filtry, nagłówki, validacja,
- **security matrix**: macierz 401/403/200 dla wszystkich aktorów,
- **business flow**: seed → oracle → multi-assertion z `SoftAssertions` i `tuple`.

Każda warstwa używa innego test class i wspólnego support helpera.

## 2. Prerequisites

- `given()/when()/then()` (Lesson 04-05).
- `extract().as(PaymentOrderSummaryResponse.class)` (Lesson 07).
- `RequestSpecBuilder` z tokenem (Lesson 07).
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `Testcontainers` (Lesson 06).
- `TestJwtSupport` — token factory (Lesson 06).

## 3. Code Reading Map — Testy

| Plik | Co testuje | Ile testów |
|---|---|---|
| `PaymentOrderSummaryRestAssuredTest.java` | kontrakt: 200/400/header/filter/date | 10 |
| `PaymentOrderSummarySecurityTest.java` | security: 401/403/200 + ownership | 7 |
| `PaymentOrderSummaryBusinessFlowRestAssuredTest.java` | flow: seed → oracle → cross-tenant | 3 |
| `PaymentOrderSummaryApiTestSupport.java` | reusable seed + expected oracle + request specs | — (helper) |

## 4. Kluczowe Pojęcia

### 4.1 Agregacyjny test oracle

```java
var seed = PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);
var expected = PaymentOrderSummaryApiTestSupport.expectedFor(seed);

assertThat(response.totalOrders()).isEqualTo(expected.totalOrders());
assertThat(response.totalAmountMinor()).isEqualTo(expected.totalAmountMinor());
```

- Seed data jest **jawnie zdefiniowana** w support helperze.
- Expected totals są **wyliczone z seed data**, nie z response API.
- To zapobiega „wierzę API na słowo” — oracle jest niezależny.

### 4.2 Tuple assertions dla grouped rows

```java
assertThat(response.byCurrency())
    .extracting(CurrencySummary::currency, CurrencySummary::orderCount, CurrencySummary::totalAmountMinor)
    .containsExactly(
        Tuple.tuple("EUR", 1L, 3_000L),
        Tuple.tuple("PLN", 2L, 3_000L),
        Tuple.tuple("USD", 1L, 4_000L)
    );
```

- `extracting(...)` wybiera 3 pola z każdego elementu listy.
- `containsExactly(...)` sprawdza dokładną kolejność (sortowanie ASC).
- Każdy `Tuple.tuple(...)` to oczekiwany wiersz.

### 4.3 SoftAssertions dla wielu agregatów

```java
SoftAssertions softly = new SoftAssertions();
softly.assertThat(response.totalOrders()).isEqualTo(4);
softly.assertThat(response.totalAmountMinor()).isEqualTo(10000);
softly.assertThat(response.byCurrency()).hasSize(3);
softly.assertThat(response.byStatus()).hasSize(1);
softly.assertAll();  // pokazuje wszystkie błędy naraz
```

- W przeciwieństwie do zwykłego `assertThat`, `SoftAssertions` **nie zatrzymuje się na pierwszym błędzie**.
- Idealne dla agregacji, gdzie chcemy zobaczyć wszystkie niezgodności naraz.

### 4.4 Security matrix — 3 wzorce asercji

```java
// 401: brak tokena
MerchantApiTestSupport.publicRequest(port).when().get("/api/...").then().statusCode(401);

// 403 z JSON body (AccessDeniedException z kontrolera)
.summaryReaderRequest(...).when().get(...).then().statusCode(403).body("error", equalTo("forbidden"));

// 403 bez body (Spring Security filter odrzuca przed kontrolerem)
.summaryReaderRequest(...).when().get(...).then().statusCode(403);  // bez .body()!
```

- **Z body**: cross-tenant, platform reader — kontroler rzuca `AccessDeniedException` → handler zwraca JSON.
- **Bez body**: creator-only, operator-only, denied — Spring Security `AuthorizationFilter` odrzuca przed kontrolerem (brak `Content-Type` w odpowiedzi).

### 4.5 Data ownership i parallel safety

```java
public static List<SeedOrder> seedDefaultDataset(int port, String merchantId, String creatorToken) {
    List<SeedOrder> seed = List.of(
        new SeedOrder(1_000, "PLN", uniquePaymentReference("L08-A")),
        new SeedOrder(2_000, "PLN", uniquePaymentReference("L08-B")),
        new SeedOrder(3_000, "EUR", uniquePaymentReference("L08-C")),
        new SeedOrder(4_000, "USD", uniquePaymentReference("L08-D"))
    );
    // każdy order z unikalnym clientOrderReference + unikalnym Idempotency-Key
}
```

- Każdy test tworzy własnego merchanta → izolacja danych.
- `uniquePaymentReference` i `uniqueIdempotencyKey` gwarantują brak kolizji.
- Testy mogą biec równolegle bez ryzyka.

## 5. Walkthrough — Jak Czytać Testy Kontraktowe

1. **Klasa**: `@SpringBootTest` + `Testcontainers` + `extends PostgresContainerSupport`.
2. **Setup**: każdy test tworzy merchanta przez `createActiveMerchant(port, operatorRequest)`.
3. **Seed (gdy potrzebny)**: `seedDefaultDataset(...)` tworzy 4 payment orders (PLN/PLN/EUR/USD).
4. **Request**: `summaryReaderRequest(port, token, correlationId)` — reusable spec z auth + accept + correlation header.
5. **Assertions**: status code, content-type, headers (X-Correlation-ID, brak ETag), body (typed DTO lub JSON path).
6. **Negative paths**: invalid param → status 400, body `"error": "validation"`.

## 6. Delta vs Lesson 07

| Aspekt | Lesson 07 testy | Lesson 08 testy |
|---|---|---|
| Response type | `PaymentOrderListResponse` (Page) | `PaymentOrderSummaryResponse` (agregat) |
| Oracle | page metadata, sorted content | jawnie policzony expected totals |
| Assertions | `PaymentOrderAssertions` custom | `SoftAssertions` + `tuple` |
| Security | 401/403 dla create/read (6 testów) | 401/403 dla summary + ownership (7 testów) |
| Nowe | — | grouped rows assertions, SoftAssertions, empty summary oracle |
| Reuse | `TestJwtSupport`, `MerchantApiTestSupport`, `PaymentApiTestSupport` | TE SAME |
| Support | `PaymentOrderListApiTestSupport` | `PaymentOrderSummaryApiTestSupport` |

## 7. Typowe Błędy i Antywzorce

| Błąd | Objaw | Poprawnie |
|---|---|---|
| `.body("error", equalTo(...))` na 403-odrzuconym przez Spring Security | `IllegalStateException: no content-type` | pomiń `.body()` dla security-filter 403 |
| Asercja na `totalOrders` bez seed danych | test przechodzi przypadkiem (0=0) | zawsze seeduj dane przed asercją agregacji |
| `extract().as()` bez rejestracji ObjectMappera | deserializacja się nie udaje | REST Assured automatycznie używa Jacksona jeśli w classpath |
| Współdzielenie merchanta między testami | flaky — kolejność testów wpływa na wyniki | każdy test tworzy własnego merchanta |
| Porównywanie totals z response, nie z oracle | „wierzę API na słowo” | expected = policz z seed, nie z response |
| `.header("ETag", notNullValue())` — summary NIE ma ETag | test fail | asercja: brak ETag lub `nullValue()` |

## 8. Ćwiczenia

1. **Dodaj test** dla filtru `?currency=PLN&status=CREATED` — co się stanie z totals?
2. **Dodaj test** dla `?status=INVALID` i sprawdź message w body odpowiedzi.
3. **Dodaj security test** dla tokenu bez `merchant_id` claim — co zwróci?
4. **Przerób jeden test** z `SoftAssertions` na zwykłe `assertThat` — jaka jest różnica w output przy fail?
5. **Napisz test**, który seeduje 0 orderów i sprawdza empty summary przez `extract().as()`.

## 9. Pytania Kontrolne

1. Dlaczego testy security dla creator/operator/denied nie asercjonują `.body()`?
2. Co robi `SoftAssertions.assertAll()` i kiedy go używać?
3. Jak `containsExactly` różni się od `containsExactlyInAnyOrder`?
4. Dlaczego każdy test tworzy własnego merchanta?
5. Jak sprawdzić, że summary NIE ma headera `ETag`?

## 10. Jak To Testować (Stabilność)

- **Oracle**: always compute expected from seed, never from previous API response.
- **Dane**: `uniquePaymentReference` + `uniqueIdempotencyKey` w każdym teście.
- **Parallel-safe**: każdy test klasa używa własnego `@Container` PostgreSQL.
- **Anti-flakiness**: brak sleepów, brak zależności od kolejności, brak globalnych fixtures.

## 11. Next Links

- [[Lesson 08 - Payment Aggregation Summary]] — pełna notatka lekcji
- [[Lesson 08 - Java Records, Read-Only Services, and Input Validation]] — Java side
- [[Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy]] — HTTP/API contract
- [[Lesson 08 - Business Logic, Decision Tables, and Risk Notes]] — domain rules
- [[JUnit REST Assured MOC]]
- [[Lesson Evidence Tracker]]
