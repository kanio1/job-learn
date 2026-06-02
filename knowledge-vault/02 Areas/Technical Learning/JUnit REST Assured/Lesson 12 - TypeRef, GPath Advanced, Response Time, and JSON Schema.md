---
type: lesson
status: planned
area: JUnit REST Assured
lesson: 12
module: TypeRef, GPath Advanced, Response Time, and JSON Schema
date: 2026-05-31
tags:
  - rest-assured
  - typeref
  - gpath
  - response-time
  - json-schema
  - lesson-12
  - senior-sdet
---

# Lesson 12 — TypeRef, GPath Advanced, Response Time, and JSON Schema

> **Evidence link:** `PaymentOrderListRestAssuredTest.java` (extended), `PaymentOrderPerformanceTest.java` (planned)
>
> **Navigation:** [[JUnit REST Assured MOC]] | [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się zaawansowanych REST Assured features:
- **TypeRef<T>** dla generic list extraction
- **GPath advanced** (deep scan `..`, `findAll`, array indexing)
- **Response time assertions** (`.time()`, `.timeIn()`)
- **JSON Schema validation** (`matchesJsonSchemaInClasspath()`)

## 2. Prerequisites

- `extract().as(Class)` (Lesson 06-07).
- GPath basics (`body("field", matcher)`) — Lesson 05.
- Hamcrest matchers (`equalTo`, `hasItems`, `everyItem`) — Lesson 06-07.

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PaymentOrderListRestAssuredTest.java` | Obecne `extract().as(PaymentOrderListResponse.class)` → target: TypeRef<T> |
| `PaymentOrderPerformanceTest.java` | Response time assertions dla summary/list/create |
| `src/test/resources/schemas/payment-order-response.json` | JSON Schema dla validation |

## 4. Kluczowe Pojęcia

### 4.1 TypeRef<T> dla Generic List Extraction

```java
// PRZED (Lesson 07):
PaymentOrderListResponse listResponse = given()
    .port(port)
    .auth().oauth2(token)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .statusCode(200)
    .extract().as(PaymentOrderListResponse.class);

List<PaymentOrderResponse> orders = listResponse.content();

// PO (Lesson 12):
List<PaymentOrderResponse> orders = given()
    .port(port)
    .auth().oauth2(token)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .statusCode(200)
    .extract().as(new TypeRef<List<PaymentOrderResponse>>(){});
```

**Dlaczego TypeRef<T>?**
- Java generics używają type erasure — w runtime `List<PaymentOrderResponse>` staje się `List`
- `extract().as(List.class)` zwraca `List<Map>`, nie `List<PaymentOrderResponse>`
- `TypeRef<T>` używa anonymous class trick aby zachować generic type information
- Anonymous class `new TypeRef<List<PaymentOrderResponse>>(){}` tworzy type token

**Kiedy używać TypeRef<T>?**
- Gdy API zwraca raw JSON array (nie wrapped w object)
- Gdy nie masz wrapper DTO (np. `PaymentOrderListResponse`)
- Gdy chcesz bezpośrednio pracować z `List<T>`

### 4.2 GPath Advanced: Deep Scan (`..`)

```java
// Deep scan (recursive field search):
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .body("content..currency", hasItems("PLN", "EUR", "USD"))
    .body("content..amountMinor", everyItem(greaterThan(0)));
```

**Jak działa `..`?**
- `content.currency` — szuka `currency` tylko w direct children of `content`
- `content..currency` — szuka `currency` recursively we wszystkich descendants of `content`
- Przydatne gdy struktura jest nested (np. `content[*].details.currency`)

**Kiedy używać deep scan?**
- Gdy nie znasz exact path do field
- Gdy field może być na różnych poziomach nesting
- Gdy chcesz sprawdzić wszystkie occurrences of field w całym response

### 4.3 GPath Advanced: findAll

```java
// Filter collections:
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .body("content.findAll { it.currency == 'PLN' }.size()", equalTo(3))
    .body("content.findAll { it.amountMinor > 5000 }.currency", hasItems("EUR", "USD"))
    .body("content.findAll { it.status == 'CREATED' }.amountMinor", everyItem(greaterThan(0)));
```

**Jak działa `findAll`?**
- `content.findAll { condition }` — zwraca wszystkie elementy spełniające condition
- `{ it.currency == 'PLN' }` — Groovy closure (lambda) z `it` jako current element
- Możesz chain methods: `.findAll { ... }.size()`, `.findAll { ... }.currency`

**Kiedy używać findAll?**
- Gdy potrzebujesz filter collection po complex condition
- Gdy chcesz assert properties of filtered subset
- Gdy AssertJ `filteredOn` nie jest dostępne (inline GPath assertions)

### 4.4 GPath Advanced: Array Indexing

```java
// Array indexing:
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .body("content[0].currency", equalTo("PLN"))           // Pierwszy element
    .body("content[-1].currency", equalTo("USD"))          // Ostatni element
    .body("content[0..2].currency", hasItems("PLN", "EUR", "USD"))  // Pierwsze 3
    .body("content.size()", equalTo(10));                  // Total count
```

**Jak działa array indexing?**
- `[0]` — pierwszy element (0-based)
- `[-1]` — ostatni element (negative indexing)
- `[0..2]` — range od index 0 do 2 (inclusive)
- `.size()` — liczba elementów

**Kiedy używać array indexing?**
- Gdy kolejność elementów jest ważna (np. sorted by date)
- Gdy chcesz sprawdzić specific elements (first, last, middle)
- Gdy chcesz sprawdzić subset of elements (range)

### 4.5 Response Time Assertions

```java
// Inline assertion:
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
.then()
    .time(lessThan(500L))                          // < 500ms
    .time(lessThan(1L), TimeUnit.SECONDS);         // < 1s

// Extract time:
long responseTime = given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
.then()
    .extract().time();

assertThat(responseTime).isLessThan(500);

// Z TimeDetail (bardziej precyzyjne):
Response response = given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId);

long timeInMs = response.timeIn(TimeUnit.MILLISECONDS);
long timeInNs = response.timeIn(TimeUnit.NANOSECONDS);
```

**Dlaczego response time assertions?**
- Performance regression detection (jeśli endpoint nagle staje się wolny)
- SLA verification (np. "95% requests < 500ms")
- Baseline establishment (jak szybki jest endpoint pod normal load)

**Kiedy używać response time assertions?**
- Performance tests (oznaczone `@Tag("performance")`)
- Regression tests (sprawdzaj czy czas nie wzrósł)
- **Uwaga:** Response time może być flaky w CI (shared resources, network latency). Używaj reasonable thresholds (500ms-2s, nie 50ms).

### 4.6 JSON Schema Validation

```java
// src/test/resources/schemas/payment-order-response.json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["paymentOrderId", "merchantId", "amountMinor", "currency", "status"],
  "properties": {
    "paymentOrderId": { "type": "string", "format": "uuid" },
    "merchantId": { "type": "string", "format": "uuid" },
    "amountMinor": { "type": "integer", "minimum": 1 },
    "currency": { "type": "string", "enum": ["PLN", "EUR", "USD"] },
    "status": { "type": "string", "enum": ["CREATED", "AUTHORIZED", "CAPTURED", "CANCELLED"] },
    "clientOrderReference": { "type": "string", "minLength": 1, "maxLength": 100 },
    "createdAt": { "type": "string", "format": "date-time" },
    "updatedAt": { "type": "string", "format": "date-time" }
  },
  "additionalProperties": false
}

// Test:
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantId, paymentOrderId)
.then()
    .statusCode(200)
    .body(matchesJsonSchemaInClasspath("schemas/payment-order-response.json"));
```

**Dlaczego JSON Schema validation?**
- Contract verification (response ma expected structure)
- Type checking (fields mają correct types)
- Required fields validation (wszystkie required fields są obecne)
- Enum validation (fields mają allowed values)

**Kiedy używać JSON Schema validation?**
- Contract tests (weryfikuj response shape)
- Integration tests (weryfikuj end-to-end data flow)
- **Uwaga:** JSON Schema validation jest wolniejsze niż field-by-field assertions. Używaj selective (nie w każdym teście).

## 5. Walkthrough — Od extract().as(Class) Do TypeRef<T>

```
PRZED:
1. Test wywołuje extract().as(PaymentOrderListResponse.class)
2. REST Assured deserializuje JSON do PaymentOrderListResponse record
3. Test wyciąga .content() aby dostać List<PaymentOrderResponse>
4. Test iteruje po list i asercjonuje elements

PO:
1. Test wywołuje extract().as(new TypeRef<List<PaymentOrderResponse>>(){})
2. REST Assured używa TypeRef aby zachować generic type information
3. REST Assured deserializuje JSON array bezpośrednio do List<PaymentOrderResponse>
4. Test iteruje po list i asercjonuje elements (bez wrapper DTO)
```

## 6. Learning Delta — Co Nowe vs Lessons 06-11

| Temat | Lesson 06-11 | Lesson 12 |
|---|---|---|
| List extraction | `extract().as(WrapperDTO.class).content()` | `extract().as(new TypeRef<List<T>>(){})` |
| GPath | `body("field", matcher)` | `body("content..field", matcher)`, `findAll`, `[0]` |
| Performance | Brak assertions | `.time(lessThan(500L))` |
| Contract validation | Field-by-field assertions | JSON Schema validation |

## 7. Typowe Błędy

1. **TypeRef<T> bez anonymous class.** `new TypeRef<List<T>>()` nie działa. Musisz użyć `new TypeRef<List<T>>(){}` (anonymous class).
2. **GPath deep scan na large responses.** `..` jest recursive — może być wolne dla large nested structures. Używaj selective.
3. **findAll z complex conditions.** Groovy closures mogą być trudne do debugowania. Jeśli condition jest złożona, rozważ AssertJ `filteredOn`.
4. **Response time assertions w CI.** Response time może być flaky w CI (shared resources). Używaj reasonable thresholds i `@Tag("performance")`.
5. **JSON Schema validation w każdym teście.** JSON Schema validation jest wolniejsze niż field-by-field assertions. Używaj selective (contract tests, nie unit tests).

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Zaimplementuj TypeRef<T> dla list extraction | 20 min |
| 2 | Napisz test z GPath deep scan (`..`) | 15 min |
| 3 | Napisz test z GPath findAll | 20 min |
| 4 | Napisz test z GPath array indexing (`[0]`, `[-1]`) | 15 min |
| 5 | Napisz response time test z threshold 500ms | 15 min |
| 6 | Napisz JSON Schema dla PaymentOrderResponse | 30 min |
| 7 | Napisz test używający matchesJsonSchemaInClasspath | 20 min |

## 9. Pytania

1. Dlaczego TypeRef<T> jest potrzebne dla generic list extraction?
2. Jak GPath `..` różni się od `.`?
3. Kiedy używać GPath findAll vs AssertJ filteredOn?
4. Dlaczego response time assertions mogą być flaky w CI?
5. Kiedy używać JSON Schema validation vs field-by-field assertions?
6. Jak TypeRef<T> zachowuje generic type information?
7. Czy GPath wspiera regex matching?
8. Jak mierzyć response time w nanoseconds vs milliseconds?
9. Czy JSON Schema validation sprawdza additional properties?
10. Kiedy używać array indexing vs iteration?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `listOrdersWithTypeRefReturnsTypedList` | TypeRef<T> works |
| `allPaymentOrdersHavePositiveAmount` | GPath deep scan works |
| `filterPaymentOrdersByCurrency` | GPath findAll works |
| `firstPaymentOrderHasExpectedCurrency` | GPath array indexing works |
| `summaryEndpointRespondsWithin500ms` | Response time assertion works |
| `paymentOrderResponseMatchesJsonSchema` | JSON Schema validation works |

## 11. Powiązane Notatki

- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 08 - Aggregation Contract, Security, and Business Flow Tests]]
- [[Lesson 11 - API Clients, Builders, Error Specs, and Filters]]
- [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Senior SDET Competency Coverage Matrix]]
