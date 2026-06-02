---
type: lesson
status: planned
project: Payment Quality Engineering Lab
phase: 2
lesson: 12
area: Payment Orders
module: Advanced Assertions, Type-Safe Extraction, and Parameterized Testing
date: 2026-05-31
tags:
  - lesson
  - lesson-12
  - payment-quality-lab
  - assertj
  - rest-assured
  - junit
  - parameterized-tests
  - senior-sdet
---

# Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing

> **Status:** PLANNED - precision assertions and data-driven tests
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Current Sprint]] | [[Lesson Evidence Tracker]]
>
> **Main decision:** Lesson 12 addresses advanced assertion patterns, type-safe generic extraction, and parameterized testing — three areas where the current test suite uses basic patterns but lacks professional-grade precision.

## 1. Cel Lekcji

Lekcja 12 adresuje luki w **AssertJ advanced features**, **REST Assured TypeRef<T>**, i **JUnit @ParameterizedTest**. Po Lessons 06-11 mamy solidne fundamenty (basic assertions, extract().as(), @Test), ale brakuje:

- **TypeRef<T>** dla generic list extraction (`List<PaymentOrderResponse>`)
- **GPath advanced** (deep scan `..`, `findAll`, array indexing)
- **Response time assertions** (`.time()`, `.timeIn()`)
- **usingRecursiveComparison** z ignoringFields, comparingOnlyFields
- **SoftAssertions** z assertAll()
- **asInstanceOf** dla type-safe casting
- **@ParameterizedTest** z @MethodSource, @CsvSource, @EnumSource
- **Generics** (bounded wildcards, PECS)
- **Pattern matching instanceof** (JDK 16+)
- **Text blocks** dla JSON fixtures

Celem jest transformacja testów z "basic assertions" do "precision assertions" — testy, które sprawdzają dokładnie to, co mają sprawdzać, z pełnym type-safety i data-driven coverage.

## 2. Co Budujemy / Co Ćwiczymy

### Assertion patterns

| Pattern | Opis | Przykład |
|---|---|---|
| TypeRef<T> | Generic list extraction | `extract().as(new TypeRef<List<PaymentOrderResponse>>(){})` |
| GPath deep scan | `..` operator | `body("content..currency", hasItems("PLN", "EUR"))` |
| GPath findAll | Filter collections | `body("content.findAll { it.amountMinor > 5000 }.size()", equalTo(3))` |
| Response time | Performance assertions | `time(lessThan(500L))` |
| usingRecursiveComparison | Deep object comparison | `assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)` |
| SoftAssertions | Multiple assertions, one failure | `SoftAssertions.assertAll()` |
| asInstanceOf | Type-safe casting | `assertThat(obj).asInstanceOf(InstanceOfAssertFactories.LIST)` |
| @ParameterizedTest | Data-driven tests | `@ParameterizedTest @MethodSource("testData")` |

### System batch

| Batch | Scope | Expected files |
|---|---|---|
| 12A | TypeRef<T> + GPath advanced | Rozszerzenie `PaymentOrderListRestAssuredTest.java` |
| 12B | Response time assertions | Nowy `PaymentOrderPerformanceTest.java` |
| 12C | AssertJ advanced (recursive, soft, asInstanceOf) | Rozszerzenie `PaymentOrderAssertions.java` |
| 12D | @ParameterizedTest z @MethodSource/@CsvSource/@EnumSource | Nowy `PaymentOrderParameterizedTest.java` |
| 12E | Generics + pattern matching + text blocks | Refaktor istniejących testów |

Default implementation: **12A + 12C + 12D** (core advanced patterns). 12B i 12E są optional extensions.

## 3. Learning Delta Względem Poprzednich Lekcji

| Temat | Status |
|---|---|
| TypeRef<T> dla List<T> | **New** — zastępuje `extract().path("content")` z manual casting |
| GPath deep scan (`..`) | **New** — recursive field search |
| GPath findAll | **New** — filter collections w GPath |
| GPath array indexing | **New** — `content[0]`, `content[-1]` |
| Response time assertions | **New** — `.time()`, `.timeIn()` |
| JSON Schema validation | **New** — `matchesJsonSchemaInClasspath()` |
| usingRecursiveComparison | **New** — deep object comparison |
| SoftAssertions | **New** — multiple assertions, one failure |
| asInstanceOf | **New** — type-safe casting |
| hasFieldOrPropertyWithValue | **New** — field existence check |
| matches(Predicate) | **New** — custom conditions |
| @ParameterizedTest | **New** — data-driven tests |
| @MethodSource | **New** — method-based test data |
| @CsvSource | **New** — CSV-based test data |
| @EnumSource | **New** — enum-based test data |
| @RepeatedTest | **New** — repeated execution |
| DynamicTest / @TestFactory | **New** — dynamic test generation |
| Generics (bounded wildcards) | **New** — `<? extends T>`, `<? super T>`, PECS |
| Pattern matching instanceof | **New** — JDK 16+ feature |
| Text blocks | **New** — multi-line strings dla JSON |

## 4. Mapa Kodu

### Istniejące pliki do rozszerzenia

| Plik | Obecny stan | Target stan |
|---|---|---|
| `PaymentOrderListRestAssuredTest.java` | `extract().as(PaymentOrderListResponse.class)` | Dodaj TypeRef<T> dla `List<PaymentOrderResponse>` |
| `PaymentOrderAssertions.java` | Custom assertions dla list response | Dodaj recursive comparison, soft assertions |
| `PaymentOrderRestAssuredTest.java` | Inline assertions | Dodaj @ParameterizedTest dla validation cases |

### Nowe pliki do stworzenia

| Plik | Odpowiedzialność |
|---|---|
| `rest/PaymentOrderPerformanceTest.java` | Response time assertions |
| `rest/PaymentOrderParameterizedTest.java` | @ParameterizedTest z @MethodSource/@CsvSource/@EnumSource |
| `testsupport/PaymentOrderJsonFixtures.java` | Text blocks dla JSON test data |

## 5. Architecture Walkthrough

### TypeRef<T> dla Generic List Extraction

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
- Java generics są erased w runtime (type erasure)
- `extract().as(List.class)` zwraca `List<Map>`, nie `List<PaymentOrderResponse>`
- `TypeRef<T>` zachowuje generic type information
- Anonymous class `new TypeRef<List<PaymentOrderResponse>>(){}` tworzy type token

### GPath Advanced

```java
// Deep scan (..):
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .body("content..currency", hasItems("PLN", "EUR", "USD"))  // Wszystkie currency w content
    .body("content..amountMinor", everyItem(greaterThan(0)));  // Wszystkie amountMinor > 0

// findAll:
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .body("content.findAll { it.currency == 'PLN' }.size()", equalTo(3))
    .body("content.findAll { it.amountMinor > 5000 }.currency", hasItems("EUR", "USD"));

// Array indexing:
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .body("content[0].currency", equalTo("PLN"))      // Pierwszy element
    .body("content[-1].currency", equalTo("USD"))     // Ostatni element
    .body("content[0..2].currency", hasItems("PLN", "EUR", "USD"));  // Pierwsze 3
```

### Response Time Assertions

```java
given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
.then()
    .time(lessThan(500L))              // Response time < 500ms
    .time(lessThan(1L), TimeUnit.SECONDS);  // Response time < 1s

// Lub z extract:
long responseTime = given()
    .port(port)
.when()
    .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
.then()
    .extract().time();

assertThat(responseTime).isLessThan(500);
```

### usingRecursiveComparison

```java
// PRZED (field-by-field):
assertThat(actual.paymentOrderId()).isEqualTo(expected.paymentOrderId());
assertThat(actual.merchantId()).isEqualTo(expected.merchantId());
assertThat(actual.amountMinor()).isEqualTo(expected.amountMinor());
assertThat(actual.currency()).isEqualTo(expected.currency());
assertThat(actual.status()).isEqualTo(expected.status());

// PO (recursive comparison):
assertThat(actual)
    .usingRecursiveComparison()
    .ignoringFields("createdAt", "updatedAt")  // Ignoruj timestamps
    .isEqualTo(expected);

// Lub z comparingOnlyFields:
assertThat(actual)
    .usingRecursiveComparison()
    .comparingOnlyFields("paymentOrderId", "merchantId", "amountMinor", "currency", "status")
    .isEqualTo(expected);
```

### SoftAssertions

```java
// PRZED (first failure stops test):
assertThat(response.totalOrders()).isEqualTo(10);
assertThat(response.totalAmountMinor()).isEqualTo(50000);
assertThat(response.byCurrency()).hasSize(3);
// Jeśli pierwsza asercja failuje, nie widzisz pozostałych failures

// PO (all failures reported):
SoftAssertions softly = new SoftAssertions();
softly.assertThat(response.totalOrders()).isEqualTo(10);
softly.assertThat(response.totalAmountMinor()).isEqualTo(50000);
softly.assertThat(response.byCurrency()).hasSize(3);
softly.assertAll();  // Zgłasza wszystkie failures naraz
```

### @ParameterizedTest z @MethodSource

```java
@ParameterizedTest(name = "{0}: {1} → {2}")
@MethodSource("validationTestCases")
void createPaymentOrderValidation(String testName, CreatePaymentOrderRequest request, 
                                  String expectedErrorMessage) {
    given()
        .port(port)
        .auth().oauth2(token)
        .contentType(ContentType.JSON)
        .body(request)
    .when()
        .post("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
        .spec(PaymentErrorSpecs.validationError())
        .body("message", containsString(expectedErrorMessage));
}

static Stream<Arguments> validationTestCases() {
    return Stream.of(
        Arguments.of("negative amount", 
            aPaymentOrder().withAmountMinor(-100).build(), 
            "amountMinor must be positive"),
        Arguments.of("zero amount", 
            aPaymentOrder().withAmountMinor(0).build(), 
            "amountMinor must be positive"),
        Arguments.of("invalid currency", 
            aPaymentOrder().withCurrency("GBP").build(), 
            "currency must be PLN, EUR, or USD"),
        Arguments.of("blank reference", 
            aPaymentOrder().withClientOrderReference("").build(), 
            "clientOrderReference must not be blank")
    );
}
```

### @ParameterizedTest z @CsvSource

```java
@ParameterizedTest(name = "currency={0}, expectedCount={1}")
@CsvSource({
    "PLN, 5",
    "EUR, 3",
    "USD, 2"
})
void listPaymentOrdersByCurrency(String currency, int expectedCount) {
    given()
        .port(port)
        .auth().oauth2(token)
        .queryParam("currency", currency)
    .when()
        .get("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
        .statusCode(200)
        .body("content.size()", equalTo(expectedCount))
        .body("content.currency", everyItem(equalTo(currency)));
}
```

### @ParameterizedTest z @EnumSource

```java
@ParameterizedTest(name = "status={0}")
@EnumSource(value = PaymentStatus.class, names = {"CREATED", "AUTHORIZED", "CAPTURED"})
void listPaymentOrdersByStatus(PaymentStatus status) {
    given()
        .port(port)
        .auth().oauth2(token)
        .queryParam("status", status.name())
    .when()
        .get("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
        .statusCode(200)
        .body("content.status", everyItem(equalTo(status.name())));
}
```

## 6. HTTP I REST API

### JSON Schema Validation

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
    "status": { "type": "string", "enum": ["CREATED", "AUTHORIZED", "CAPTURED", "CANCELLED"] }
  }
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

### Text Blocks dla JSON Fixtures

```java
public final class PaymentOrderJsonFixtures {
    
    public static final String VALID_PAYMENT_ORDER_REQUEST = """
        {
            "amountMinor": 5000,
            "currency": "PLN",
            "clientOrderReference": "TEST-001"
        }
        """;
    
    public static final String INVALID_PAYMENT_ORDER_REQUEST_NEGATIVE_AMOUNT = """
        {
            "amountMinor": -100,
            "currency": "PLN",
            "clientOrderReference": "TEST-002"
        }
        """;
    
    public static final String INVALID_PAYMENT_ORDER_REQUEST_UNKNOWN_CURRENCY = """
        {
            "amountMinor": 5000,
            "currency": "GBP",
            "clientOrderReference": "TEST-003"
        }
        """;
}

// Usage:
given()
    .port(port)
    .contentType(ContentType.JSON)
    .body(PaymentOrderJsonFixtures.VALID_PAYMENT_ORDER_REQUEST)
.when()
    .post("/api/merchants/{merchantId}/payment-orders", merchantId)
.then()
    .statusCode(201);
```

## 7. Java 25 I Java Code Reading

### Generics: Bounded Wildcards (PECS)

```java
// PECS: Producer Extends, Consumer Super

// Producer (wyciągasz dane z kolekcji):
public static <T extends PaymentOrderResponse> void assertAllCreated(List<T> orders) {
    assertThat(orders).allMatch(o -> o.status().equals("CREATED"));
}

// Consumer (wkładasz dane do kolekcji):
public static <T super CreatePaymentOrderRequest> void addDefaultOrder(List<T> requests) {
    requests.add(new CreatePaymentOrderRequest(1000, "PLN", "DEFAULT"));
}

// Dlaczego PECS?
// - `List<? extends T>` — możesz czytać T, ale nie możesz dodawać (nie wiesz jaki podtyp)
// - `List<? super T>` — możesz dodawać T, ale nie możesz czytać (nie wiesz jaki nadtyp)
```

### Pattern Matching instanceof (JDK 16+)

```java
// PRZED:
if (response instanceof PaymentOrderResponse) {
    PaymentOrderResponse order = (PaymentOrderResponse) response;
    assertThat(order.amountMinor()).isPositive();
}

// PO (pattern matching):
if (response instanceof PaymentOrderResponse order) {
    assertThat(order.amountMinor()).isPositive();
}

// W switch (JDK 21+):
String describe(Object obj) {
    return switch (obj) {
        case PaymentOrderResponse order -> "Payment order: " + order.paymentOrderId();
        case PaymentOrderListResponse list -> "Payment list: " + list.totalElements();
        case PaymentErrorResponse error -> "Error: " + error.error();
        default -> "Unknown: " + obj.getClass().getSimpleName();
    };
}
```

### Optional<T> w Return Types

```java
// PRZED (null checks):
PaymentOrderResponse order = paymentApi.getOrder(merchantId, orderId, token);
if (order != null) {
    assertThat(order.amountMinor()).isPositive();
}

// PO (Optional):
Optional<PaymentOrderResponse> order = paymentApi.getOrderOptional(merchantId, orderId, token);
order.ifPresent(o -> assertThat(o.amountMinor()).isPositive());

// Lub z AssertJ:
assertThat(order).isPresent().get().extracting(PaymentOrderResponse::amountMinor).isEqualTo(5000);
```

## 8. SQL, PostgreSQL I Flyway

### CTE (Common Table Expressions)

```sql
-- CTE dla complex test data queries:
WITH merchant_orders AS (
    SELECT merchant_id, COUNT(*) as order_count, SUM(amount_minor) as total_amount
    FROM payment_orders
    WHERE status = 'CREATED'
    GROUP BY merchant_id
)
SELECT m.reference, m.display_name, mo.order_count, mo.total_amount
FROM merchants m
JOIN merchant_orders mo ON m.merchant_id = mo.merchant_id
WHERE mo.order_count > 5
ORDER BY mo.total_amount DESC;
```

**Dlaczego CTE?**
- Czytelność (named subqueries zamiast nested subqueries)
- Reusability (CTE może być użyte wielokrotnie w tym samym query)
- Debugging (możesz SELECT * FROM cte_name aby sprawdzić intermediate results)

### Window Functions

```sql
-- ROW_NUMBER dla ranking:
SELECT 
    payment_order_id,
    merchant_id,
    amount_minor,
    ROW_NUMBER() OVER (PARTITION BY merchant_id ORDER BY amount_minor DESC) as rank
FROM payment_orders;

-- LAG/LEAD dla porównań z poprzednim/następnym wierszem:
SELECT 
    payment_order_id,
    amount_minor,
    LAG(amount_minor) OVER (ORDER BY created_at) as previous_amount,
    LEAD(amount_minor) OVER (ORDER BY created_at) as next_amount
FROM payment_orders;

-- Running total:
SELECT 
    payment_order_id,
    amount_minor,
    SUM(amount_minor) OVER (ORDER BY created_at) as running_total
FROM payment_orders;
```

**Dlaczego window functions?**
- Ranking (top N per group)
- Porównania z poprzednim/następnym wierszem
- Running totals, moving averages
- Bez GROUP BY (zachowujesz wszystkie wiersze)

## 9. Security I Tenant Isolation

### @ParameterizedTest dla Security Matrix

```java
@ParameterizedTest(name = "{0}: {1} → {2}")
@MethodSource("securityMatrixTestCases")
void paymentOrderSecurityMatrix(String testName, String token, int expectedStatus) {
    given()
        .port(port)
        .auth().oauth2(token)
    .when()
        .get("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
        .statusCode(expectedStatus);
}

static Stream<Arguments> securityMatrixTestCases() {
    return Stream.of(
        Arguments.of("unauthenticated", null, 401),
        Arguments.of("expired token", TestJwtSupport.expiredToken(), 401),
        Arguments.of("invalid signature", TestJwtSupport.invalidSignatureToken(), 401),
        Arguments.of("wrong role", TestJwtSupport.merchantPaymentCreatorToken(merchantId), 403),
        Arguments.of("cross-tenant", TestJwtSupport.merchantPaymentReaderToken(otherMerchantId), 403),
        Arguments.of("valid reader", TestJwtSupport.merchantPaymentReaderToken(merchantId), 200),
        Arguments.of("platform reader", TestJwtSupport.platformPaymentReaderToken(), 200)
    );
}
```

## 10. REST Assured Learning Path

### TypeRef<T> Implementation

```java
// API client z TypeRef<T>:
public List<PaymentOrderResponse> listOrdersAsList(String merchantId, String token) {
    return given()
        .port(port)
        .auth().oauth2(token)
        .accept(ContentType.JSON)
    .when()
        .get("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
        .statusCode(200)
        .extract().as(new TypeRef<List<PaymentOrderResponse>>(){});
}
```

### GPath Advanced Implementation

```java
// Test z GPath deep scan:
@Test
void allPaymentOrdersHavePositiveAmount() {
    given()
        .port(port)
        .auth().oauth2(token)
    .when()
        .get("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
        .statusCode(200)
        .body("content..amountMinor", everyItem(greaterThan(0)));
}

// Test z GPath findAll:
@Test
void filterPaymentOrdersByCurrency() {
    given()
        .port(port)
        .auth().oauth2(token)
    .when()
        .get("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
        .statusCode(200)
        .body("content.findAll { it.currency == 'PLN' }.size()", greaterThan(0))
        .body("content.findAll { it.currency == 'PLN' }.amountMinor", everyItem(greaterThan(0)));
}
```

## 11. Assertion Strategy

### when to use TypeRef<T> vs extract().as(Class)

| Scenario | Approach | Dlaczego |
|---|---|---|
| Single object response | `extract().as(PaymentOrderResponse.class)` | Proste, type-safe |
| List response | `extract().as(new TypeRef<List<PaymentOrderResponse>>(){})` | Zachowuje generic type |
| Nested list | `extract().as(PaymentOrderListResponse.class)` + `.content()` | Wrapper class ma typed list |
| Raw JSON array | `extract().as(new TypeRef<List<Map<String, Object>>>(){})` | Gdy nie masz DTO |

### when to use GPath vs AssertJ

| Scenario | Approach | Dlaczego |
|---|---|---|
| Simple field check | GPath: `body("field", equalTo(value))` | Concise, inline |
| Complex collection filter | GPath: `body("content.findAll { ... }")` | Powerful filtering |
| Deep object comparison | AssertJ: `usingRecursiveComparison()` | Type-safe, readable |
| Multiple assertions | AssertJ: `SoftAssertions` | All failures reported |
| Custom assertions | AssertJ: custom `AbstractAssert` | Reusable, domain-specific |

## 12. Test Data Ownership

### @ParameterizedTest Data Isolation

```java
// Każda iteracja @ParameterizedTest ma własny context:
@ParameterizedTest
@MethodSource("testData")
void testWithDataIsolation(CreatePaymentOrderRequest request) {
    // Arrange: create fresh merchant for this iteration
    String merchantId = merchantApi.createActiveMerchant();
    
    // Act: create payment order with parameterized data
    PaymentOrderResponse response = paymentApi.createOrder(merchantId, token, request);
    
    // Assert
    assertThat(response.amountMinor()).isEqualTo(request.amountMinor());
}

// Dlaczego fresh merchant per iteration?
// - Izolacja (iteracje nie interferują)
// - Parallel execution (każda iteracja może być w innym thread)
// - Debugging (łatwiej zidentyfikować która iteracja failuje)
```

## 13. Pytania Do Samodzielnej Odpowiedzi

1. Dlaczego `TypeRef<T>` jest potrzebne dla generic list extraction?
2. Jak GPath `..` (deep scan) różni się od `.` (single level)?
3. Kiedy używać GPath `findAll` vs AssertJ `filteredOn`?
4. Dlaczego `usingRecursiveComparison` jest lepsze niż field-by-field assertions?
5. Jak `SoftAssertions` różni się od zwykłych assertions?
6. Kiedy używać `asInstanceOf` vs explicit casting?
7. Dlaczego `@ParameterizedTest` jest lepsze niż wiele podobnych @Test methods?
8. Jak `@MethodSource` różni się od `@CsvSource` i `@EnumSource`?
9. Kiedy używać bounded wildcards (`<? extends T>`, `<? super T>`)?
10. Jak pattern matching instanceof poprawia czytelność?
11. Dlaczego text blocks są lepsze niż string concatenation dla JSON?
12. Kiedy używać CTE vs nested subqueries?
13. Jak window functions różnią się od GROUP BY?

## 14. Zadania Praktyczne

| Zadanie | Files | Command | Expected |
|---|---|---|---|
| Zaimplementuj TypeRef<T> dla list extraction | `PaymentOrderListRestAssuredTest.java` | `./mvnw test` | TypeRef działa |
| Napisz test z GPath deep scan | `PaymentOrderListRestAssuredTest.java` | `./mvnw test` | Deep scan działa |
| Napisz test z GPath findAll | `PaymentOrderListRestAssuredTest.java` | `./mvnw test` | findAll działa |
| Napisz response time test | `PaymentOrderPerformanceTest.java` | `./mvnw test` | Time < 500ms |
| Zaimplementuj usingRecursiveComparison | `PaymentOrderRestAssuredTest.java` | `./mvnw test` | Recursive comparison działa |
| Zaimplementuj SoftAssertions | `PaymentOrderSummaryRestAssuredTest.java` | `./mvnw test` | Soft assertions działają |
| Napisz @ParameterizedTest z @MethodSource | `PaymentOrderParameterizedTest.java` | `./mvnw test` | Parameterized test działa |
| Napisz @ParameterizedTest z @CsvSource | `PaymentOrderParameterizedTest.java` | `./mvnw test` | CSV test działa |
| Napisz @ParameterizedTest z @EnumSource | `PaymentOrderParameterizedTest.java` | `./mvnw test` | Enum test działa |
| Napisz text block JSON fixture | `PaymentOrderJsonFixtures.java` | `./mvnw test` | Text blocks działają |

## 15. Mini Interview Prep

**Q: Why is TypeRef<T> needed for generic list extraction in REST Assured?**

A: Java generics use type erasure — at runtime, `List<PaymentOrderResponse>` becomes just `List`. When you call `extract().as(List.class)`, REST Assured doesn't know the element type and returns `List<Map>` instead of `List<PaymentOrderResponse>`. `TypeRef<T>` uses an anonymous class trick to preserve generic type information at runtime, allowing REST Assured to deserialize elements correctly.

**Q: When should you use usingRecursiveComparison vs field-by-field assertions?**

A: Use `usingRecursiveComparison` when comparing complex objects with many fields — it's more concise and less error-prone than writing 10+ individual assertions. Use field-by-field when you need to assert only specific fields, or when you need custom comparison logic (e.g., comparing timestamps with tolerance). `ignoringFields()` and `comparingOnlyFields()` give you fine-grained control.

**Q: Why is @ParameterizedTest better than multiple similar @Test methods?**

A: `@ParameterizedTest` eliminates code duplication — instead of writing 5 nearly identical test methods with different data, you write one test method and provide 5 data sets. It also makes it easy to add new test cases (just add another row to the data source) and ensures all cases are tested with the same logic. The `name` parameter makes test reports readable.

## 16. Verification Commands

```bash
cd apps/backend
./mvnw test
./mvnw -Dtest=PaymentOrderParameterizedTest test
./mvnw -Dtest=PaymentOrderPerformanceTest test
./mvnw -Dtest=PaymentModuleTest test
```

## 17. Learning Outcome Checklist

Po tej lekcji umiem:

- [ ] Użyć TypeRef<T> dla generic list extraction
- [ ] Użyć GPath deep scan (`..`) dla recursive field search
- [ ] Użyć GPath findAll dla collection filtering
- [ ] Użyć GPath array indexing (`[0]`, `[-1]`, `[0..2]`)
- [ ] Napisać response time assertions (`.time()`, `.timeIn()`)
- [ ] Użyć JSON Schema validation (`matchesJsonSchemaInClasspath`)
- [ ] Użyć usingRecursiveComparison z ignoringFields, comparingOnlyFields
- [ ] Użyć SoftAssertions dla multiple assertions
- [ ] Użyć asInstanceOf dla type-safe casting
- [ ] Użyć hasFieldOrPropertyWithValue dla field existence check
- [ ] Użyć matches(Predicate) dla custom conditions
- [ ] Napisać @ParameterizedTest z @MethodSource
- [ ] Napisać @ParameterizedTest z @CsvSource
- [ ] Napisać @ParameterizedTest z @EnumSource
- [ ] Napisać @RepeatedTest dla repeated execution
- [ ] Napisać DynamicTest / @TestFactory dla dynamic test generation
- [ ] Użyć bounded wildcards (`<? extends T>`, `<? super T>`) zgodnie z PECS
- [ ] Użyć pattern matching instanceof (JDK 16+)
- [ ] Użyć text blocks dla multi-line JSON fixtures
- [ ] Napisać CTE (Common Table Expressions) dla complex queries
- [ ] Użyć window functions (ROW_NUMBER, LAG/LEAD, running totals)

## 18. Powiązane Notatki W Vault

- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 08 - Payment Aggregation Summary]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Prompt - Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Learning Coverage Backlog]]
- [[Senior SDET Competency Coverage Matrix]]
- [[Lesson Evidence Tracker]]
