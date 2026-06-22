---
type: lesson
status: planned
project: Payment Quality Engineering Lab
phase: 2
lesson: 11
area: Payment Orders
module: REST Assured Framework Architecture and Test Organization
date: 2026-05-31
tags:
  - lesson
  - lesson-11
  - payment-quality-lab
  - rest-assured
  - framework-architecture
  - test-organization
  - senior-sdet
---

# Lesson 11 - REST Assured Framework Architecture and Test Organization

> **Status:** PLANNED - framework maturity slice
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Current Sprint]] | [[Lesson Evidence Tracker]]
>
> **Main decision:** Lesson 11 transforms the existing REST Assured test suite from "working tests" to "professional framework" by introducing API client wrappers, test data builders, reusable error specs, secret masking, and structured test organization with @Nested/@Tag.

## 1. Cel Lekcji

Lekcja 11 adresuje największą lukę w competency matrix: **REST Assured Framework Architecture**. Po Lessons 06-10 mamy solidne fundamenty (given/when/then, extract, RequestSpecBuilder, header assertions), ale testy są nadal "surowe" — każdy test powtarza setup, nie ma biznesowo-czytelnych API clients, brak test data builders, brak reusable error specs, brak secret masking w logach.

Celem jest transformacja test suite w profesjonalny framework, który:
- Używa **API client wrapper pattern** zamiast surowych REST Assured chainów
- Buduje test data przez **builder pattern** zamiast `Map.of(...)`
- Definiuje **reusable ResponseSpecification** dla error contracts
- Maskuje **Authorization** i inne sekrety w logach
- Organizuje testy przez **@Nested** i **@Tag**
- Pisze **multi-step scenario tests** (create → list → summary)

## 2. Co Budujemy / Co Ćwiczymy

### Framework components

| Component | Opis | Plik docelowy |
|---|---|---|
| API client wrapper | Business-readable methods zamiast raw REST Assured | `PaymentOrderApi.java`, `MerchantApi.java` |
| Test data builder | Fluent builder dla request payloads | `PaymentOrderBuilder.java` |
| Error response specs | Reusable specs dla validation/forbidden/not_found | `PaymentErrorSpecs.java` |
| Secret masking | Blacklist Authorization w logach | Rozszerzenie `RestAssuredLoggingConfig.java` |
| Test organization | @Nested groups, @Tag labels | Refaktor istniejących testów |
| Scenario flows | Multi-step create → list → summary | `PaymentOrderScenarioFlowTest.java` |

### System batch

| Batch | Scope | Expected files |
|---|---|---|
| 11A | API client wrapper pattern | `PaymentOrderApi.java`, `MerchantApi.java` |
| 11B | Test data builders | `PaymentOrderBuilder.java`, `MerchantBuilder.java` |
| 11C | Reusable error specs + secret masking | `PaymentErrorSpecs.java`, `RestAssuredLoggingConfig.java` |
| 11D | Test organization (@Nested, @Tag) | Refaktor `PaymentOrderRestAssuredTest.java` |
| 11E | Scenario flows | `PaymentOrderScenarioFlowTest.java` |

Default implementation: **11A + 11B + 11C** (core framework). 11D i 11E są optional extensions.

## 3. Learning Delta Względem Poprzednich Lekcji

| Temat | Status |
|---|---|
| API client wrapper pattern | **New** — zastępuje surowe `given().when().then()` |
| Test data builders | **New** — zastępuje `Map.of(...)` i `createPaymentOrderBody(...)` |
| Reusable ResponseSpecification dla errors | **New** — rozszerza `successListSpec()` z Lesson 07 |
| Secret masking (blacklistHeader) | **New** — rozszerza `RestAssuredLoggingConfig` |
| REST Assured Filter | **New** — interceptory request/response |
| @Nested test groups | **New** — organizacja testów w inner classes |
| @Tag selective execution | **New** — tagowanie do CI/CD |
| Scenario flows (multi-step) | **Extension** Lesson 06 — Create → Get → List → Summary |
| sealed interface dla test data | **New** JDK 25 feature |
| Map.copyOf / List.copyOf | **New** — defensive copies |
| Comparator.comparing / thenComparing | **New** — test data ordering |
| Custom AbstractAssert dla error response | **Extension** — rozszerza `PaymentOrderAssertions` |
| satisfiesExactly / allSatisfy / anySatisfy | **New** AssertJ collection assertions |
| matches(Predicate) | **New** AssertJ custom conditions |

## 4. Mapa Kodu

### Istniejące pliki do refaktoru

| Plik | Obecny stan | Target stan |
|---|---|---|
| `PaymentApiTestSupport.java` | `createPaymentOrderBody(...)` returns `Map<String, Object>` | Zastąpione przez `PaymentOrderBuilder` |
| `MerchantApiTestSupport.java` | `publicRequest()`, `operatorRequest()`, `requestWithToken()` | Zastąpione przez `MerchantApi` client |
| `PaymentOrderListApiTestSupport.java` | `listRequestSpec()`, `successListSpec()` | Rozszerzone o error specs |
| `RestAssuredLoggingConfig.java` | `enableLoggingOfRequestAndResponseIfValidationFails()` | Dodane `blacklistHeader("Authorization")` |
| `PaymentOrderAssertions.java` | Tylko dla `PaymentOrderListResponse` | Rozszerzone o `PaymentErrorResponse` |
| `PaymentOrderRestAssuredTest.java` | Płaska klasa z 10+ testami | Zorganizowana z @Nested groups |

### Nowe pliki do stworzenia

| Plik | Odpowiedzialność |
|---|---|
| `testsupport/PaymentOrderApi.java` | API client wrapper dla payment order endpoints |
| `testsupport/MerchantApi.java` | API client wrapper dla merchant endpoints |
| `testsupport/PaymentOrderBuilder.java` | Builder pattern dla `CreatePaymentOrderRequest` |
| `testsupport/MerchantBuilder.java` | Builder pattern dla `CreateMerchantRequest` |
| `testsupport/PaymentErrorSpecs.java` | Reusable `ResponseSpecification` dla error contracts |
| `rest/PaymentOrderScenarioFlowTest.java` | Multi-step scenario tests |

## 5. Architecture Walkthrough

### API Client Wrapper Pattern

```java
// PRZED (Lesson 06-10):
MerchantApiTestSupport.requestWithToken(port, token)
    .contentType(ContentType.JSON)
    .header("Idempotency-Key", idempotencyKey)
    .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "REF-001"))
    .when()
    .post("/api/merchants/{merchantId}/payment-orders", merchantId)
    .then()
    .statusCode(201)
    .extract().as(PaymentOrderResponse.class);

// PO (Lesson 11):
PaymentOrderApi paymentApi = new PaymentOrderApi(port);
PaymentOrderResponse order = paymentApi.createOrder(
    merchantId, 
    creatorToken,
    aPaymentOrder().withAmountMinor(1000).withCurrency("PLN").withReference("REF-001")
);
```

### Test Data Builder Pattern

```java
// PRZED:
Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(12500, "PLN", "PAY-001");

// PO:
CreatePaymentOrderRequest request = aPaymentOrder()
    .withAmountMinor(12500)
    .withCurrency("PLN")
    .withClientOrderReference("PAY-001")
    .build();
```

### Reusable Error Specs

```java
// PRZED (każdy test inline):
.then()
    .statusCode(400)
    .contentType(ContentType.JSON)
    .body("error", equalTo("validation"))
    .body("message", containsString("currency"));

// PO:
.then()
    .spec(PaymentErrorSpecs.validationError())
    .body("message", containsString("currency"));
```

### Secret Masking

```java
// PRZED:
RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
// Logi mogą wyciekać Authorization header

// PO:
LogConfig logConfig = logConfig()
    .blacklistHeader("Authorization")
    .blacklistHeader("Idempotency-Key")
    .enableLoggingOfRequestAndResponseIfValidationFails();
```

## 6. HTTP I REST API

### Filter Pattern (Request/Response Interceptors)

```java
// Custom filter dla correlation ID tracking:
public class CorrelationIdFilter implements Filter {
    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                          FilterableResponseSpecification responseSpec,
                          FilterContext ctx) {
        String correlationId = UUID.randomUUID().toString();
        requestSpec.header("X-Correlation-ID", correlationId);
        Response response = ctx.next(requestSpec, responseSpec);
        // Log or assert correlation ID
        return response;
    }
}

// Usage:
RestAssured.filters(new CorrelationIdFilter());
```

### Scenario Flow Example

```java
@Test
@DisplayName("merchant creates payment order, then lists it, then reads summary")
void createListAndSummarizeFlow() {
    // Arrange
    String merchantId = merchantApi.createActiveMerchant();
    
    // Act: create
    PaymentOrderResponse created = paymentApi.createOrder(
        merchantId, 
        creatorToken,
        aPaymentOrder().withAmountMinor(5000).withCurrency("PLN")
    );
    
    // Act: list
    PaymentOrderListResponse list = paymentApi.listOrders(merchantId, readerToken);
    
    // Act: summary
    PaymentOrderSummaryResponse summary = paymentApi.getSummary(merchantId, readerToken);
    
    // Assert
    assertThat(list.content()).hasSize(1);
    assertThat(list.content().get(0).paymentOrderId()).isEqualTo(created.paymentOrderId());
    assertThat(summary.totalOrders()).isEqualTo(1);
    assertThat(summary.totalAmountMinor()).isEqualTo(5000);
}
```

## 7. Java 25 I Java Code Reading

### sealed interface dla Test Data Hierarchies

```java
public sealed interface PaymentTestData 
    permits ValidPaymentOrder, InvalidPaymentOrder {
    
    record ValidPaymentOrder(long amountMinor, String currency, String reference) 
        implements PaymentTestData {}
    
    record InvalidPaymentOrder(String reason, Map<String, Object> payload) 
        implements PaymentTestData {}
}
```

### Map.copyOf / List.copyOf (Defensive Copies)

```java
// PRZED:
Map<String, Object> body = new LinkedHashMap<>();
body.put("amountMinor", 1000);
body.put("currency", "PLN");
return body; // Mutable!

// PO:
Map<String, Object> body = new LinkedHashMap<>();
body.put("amountMinor", 1000);
body.put("currency", "PLN");
return Map.copyOf(body); // Immutable defensive copy
```

### Comparator.comparing / thenComparing

```java
// Sortowanie test data:
List<PaymentOrderResponse> sorted = orders.stream()
    .sorted(Comparator.comparing(PaymentOrderResponse::currency)
                      .thenComparing(PaymentOrderResponse::amountMinor))
    .toList();
```

### Method References w Assertions

```java
// PRZED:
assertThat(orders).extracting(o -> o.currency()).containsExactly("PLN", "EUR");

// PO:
assertThat(orders).extracting(PaymentOrderResponse::currency).containsExactly("PLN", "EUR");
```

## 8. SQL, PostgreSQL I Flyway

### Test Data Isolation Strategies

| Strategia | Kiedy używać | Trade-offs |
|---|---|---|
| Per-test merchant creation | Obecny standard (Lesson 06+) | Wolne (HTTP calls), ale bezpieczne |
| Schema-per-test | Duże test suites | Izolacja, ale overhead |
| Truncation + @BeforeEach | Szybkie testy | Ryzyko flaky tests jeśli nie complete |
| @Sql scripts | Fixed test data | Mniej elastyczne |

### Flyway Test Migrations

```java
// Custom test migration dla seed data:
@Sql(scripts = "classpath:test-data/seed-merchants.sql", 
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Test
void testWithPreloadedData() { ... }
```

## 9. Security I Tenant Isolation

### Secret Masking w Logach

```java
// Headers do blacklistowania:
- Authorization (Bearer token)
- Idempotency-Key (może być sensitive w niektórych systemach)
- X-API-Key (jeśli używane)
- Cookie (jeśli session-based auth)
```

### Test Organization dla Security Tests

```java
@Nested
@DisplayName("Security Matrix")
@Tag("security")
class SecurityTests {
    
    @Nested
    @DisplayName("Authentication (401)")
    class AuthenticationTests {
        @Test void unauthenticatedReturns401() { ... }
        @Test void expiredTokenReturns401() { ... }
        @Test void invalidSignatureReturns401() { ... }
    }
    
    @Nested
    @DisplayName("Authorization (403)")
    class AuthorizationTests {
        @Test void wrongRoleReturns403() { ... }
        @Test void crossTenantReturns403() { ... }
    }
}
```

## 10. REST Assured Learning Path

### API Client Implementation

```java
public final class PaymentOrderApi {
    private final int port;
    
    public PaymentOrderApi(int port) {
        this.port = port;
    }
    
    public PaymentOrderResponse createOrder(String merchantId, String token, 
                                           PaymentOrderBuilder builder) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(builder.build())
        .when()
            .post("/api/merchants/{merchantId}/payment-orders", merchantId)
        .then()
            .statusCode(201)
            .extract().as(PaymentOrderResponse.class);
    }
    
    public PaymentOrderListResponse listOrders(String merchantId, String token) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
        .when()
            .get("/api/merchants/{merchantId}/payment-orders", merchantId)
        .then()
            .statusCode(200)
            .extract().as(PaymentOrderListResponse.class);
    }
    
    public PaymentOrderSummaryResponse getSummary(String merchantId, String token) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .accept(ContentType.JSON)
        .when()
            .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
        .then()
            .statusCode(200)
            .extract().as(PaymentOrderSummaryResponse.class);
    }
}
```

### Test Data Builder Implementation

```java
public final class PaymentOrderBuilder {
    private long amountMinor = 1000;
    private String currency = "PLN";
    private String clientOrderReference = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
    
    private PaymentOrderBuilder() {}
    
    public static PaymentOrderBuilder aPaymentOrder() {
        return new PaymentOrderBuilder();
    }
    
    public PaymentOrderBuilder withAmountMinor(long amount) {
        this.amountMinor = amount;
        return this;
    }
    
    public PaymentOrderBuilder withCurrency(String currency) {
        this.currency = currency;
        return this;
    }
    
    public PaymentOrderBuilder withClientOrderReference(String reference) {
        this.clientOrderReference = reference;
        return this;
    }
    
    public CreatePaymentOrderRequest build() {
        return new CreatePaymentOrderRequest(amountMinor, currency, clientOrderReference);
    }
}
```

### Error Specs Implementation

```java
public final class PaymentErrorSpecs {
    
    private PaymentErrorSpecs() {}
    
    public static ResponseSpecification validationError() {
        return new ResponseSpecBuilder()
            .expectStatusCode(400)
            .expectContentType(ContentType.JSON)
            .expectBody("error", equalTo("validation"))
            .build();
    }
    
    public static ResponseSpecification forbiddenError() {
        return new ResponseSpecBuilder()
            .expectStatusCode(403)
            .expectContentType(ContentType.JSON)
            .expectBody("error", equalTo("forbidden"))
            .build();
    }
    
    public static ResponseSpecification notFoundError() {
        return new ResponseSpecBuilder()
            .expectStatusCode(404)
            .expectContentType(ContentType.JSON)
            .expectBody("error", equalTo("not_found"))
            .build();
    }
    
    public static ResponseSpecification conflictError(String errorCode) {
        return new ResponseSpecBuilder()
            .expectStatusCode(409)
            .expectContentType(ContentType.JSON)
            .expectBody("error", equalTo(errorCode))
            .build();
    }
}
```

## 11. Assertion Strategy

### Custom AbstractAssert dla Error Response

```java
public class PaymentErrorResponseAssert 
    extends AbstractAssert<PaymentErrorResponseAssert, PaymentErrorResponse> {
    
    public PaymentErrorResponseAssert(PaymentErrorResponse actual) {
        super(actual, PaymentErrorResponseAssert.class);
    }
    
    public static PaymentErrorResponseAssert assertThat(PaymentErrorResponse actual) {
        return new PaymentErrorResponseAssert(actual);
    }
    
    public PaymentErrorResponseAssert hasErrorCode(String expectedCode) {
        isNotNull();
        if (!actual.error().equals(expectedCode)) {
            failWithMessage("Expected error code <%s> but was <%s>", 
                          expectedCode, actual.error());
        }
        return this;
    }
    
    public PaymentErrorResponseAssert hasMessageContaining(String substring) {
        isNotNull();
        if (!actual.message().contains(substring)) {
            failWithMessage("Expected message to contain <%s> but was <%s>", 
                          substring, actual.message());
        }
        return this;
    }
    
    public PaymentErrorResponseAssert hasCorrelationId() {
        isNotNull();
        if (actual.correlationId() == null || actual.correlationId().isBlank()) {
            failWithMessage("Expected non-blank correlationId but was <%s>", 
                          actual.correlationId());
        }
        return this;
    }
}
```

### satisfiesExactly / allSatisfy / anySatisfy

```java
// satisfiesExactly — ordered assertions:
assertThat(orders).satisfiesExactly(
    order -> assertThat(order.currency()).isEqualTo("PLN"),
    order -> assertThat(order.currency()).isEqualTo("EUR"),
    order -> assertThat(order.currency()).isEqualTo("USD")
);

// allSatisfy — all elements match predicate:
assertThat(orders).allSatisfy(order -> {
    assertThat(order.status()).isEqualTo("CREATED");
    assertThat(order.amountMinor()).isPositive();
});

// anySatisfy — at least one element matches:
assertThat(orders).anySatisfy(order -> {
    assertThat(order.currency()).isEqualTo("PLN");
    assertThat(order.amountMinor()).isGreaterThan(5000);
});
```

### matches(Predicate)

```java
assertThat(order).matches(
    o -> o.amountMinor() > 0 && o.currency().equals("PLN"),
    "valid PLN payment order"
);
```

## 12. Test Data Ownership

### Builder vs Factory Method

| Approach | Kiedy używać | Trade-offs |
|---|---|---|
| Factory method (`createPaymentOrderBody(...)`) | Proste payloady, mało wariantów | Szybkie, ale nieczytelne przy wielu parametrach |
| Builder pattern (`aPaymentOrder().withAmount(...)`) | Złożone payloady, wiele wariantów | Czytelne, ale więcej kodu |
| Object Mother (pre-built fixtures) | Common test scenarios | Ryzyko "magic" data |

### Defensive Copies

```java
// Builder zwraca immutable request:
public CreatePaymentOrderRequest build() {
    return new CreatePaymentOrderRequest(
        amountMinor,
        currency,
        clientOrderReference
    );
}

// Record jest naturalnie immutable:
public record CreatePaymentOrderRequest(
    long amountMinor,
    String currency,
    String clientOrderReference
) {}
```

## 13. Pytania Do Samodzielnej Odpowiedzi

1. Dlaczego API client wrapper jest lepszy niż surowe `given().when().then()`?
2. Kiedy builder pattern jest overkill dla test data?
3. Jak `blacklistHeader("Authorization")` chroni sekrety w CI logs?
4. Dlaczego @Nested poprawia czytelność testów security matrix?
5. Jak @Tag umożliwia selektywne uruchamianie testów w CI/CD?
6. Dlaczego scenario flow test (create → list → summary) jest wartościowy?
7. Jak `Map.copyOf()` różni się od `Map.of()`?
8. Dlaczego `Comparator.comparing().thenComparing()` jest lepsze niż ręczne sortowanie?
9. Jak custom `AbstractAssert` poprawia czytelność error assertions?
10. Kiedy używać `satisfiesExactly` vs `allSatisfy` vs `anySatisfy`?

### Odpowiedzi

1. API client wrapper ukrywa techniczny setup REST Assured i wystawia metody w języku biznesowym. Test czyta się jak scenariusz, a endpointy/auth są centralnie utrzymane.
2. Builder jest overkill, gdy payload ma 1-2 pola i występuje w kilku testach. Warto go dodać, gdy payload ma wiele wariantów, defaults i często się powtarza.
3. `blacklistHeader("Authorization")` maskuje tokeny w logach REST Assured. To chroni sekrety w CI artifacts i przy failed tests.
4. `@Nested` grupuje security matrix według kontekstu, np. unauthenticated, BFLA, BOLA, success. Dzięki temu raport testów pokazuje intencję, nie tylko nazwy metod.
5. `@Tag` pozwala uruchamiać np. tylko `security`, `contract` albo `slow` tests. CI może mieć szybki pipeline i osobny pipeline dla cięższych scenariuszy.
6. Scenario flow test sprawdza integrację kilku endpointów w realnym użyciu: create, list i summary. Nie zastępuje małych testów kontraktowych, ale łapie błędy przepływu danych.
7. `Map.of()` tworzy małą niemutowalną mapę z podanych par. `Map.copyOf()` robi niemutowalną kopię istniejącej mapy i chroni przed późniejszą mutacją źródła.
8. `Comparator.comparing().thenComparing()` jest deklaratywny i trudniej w nim popełnić błąd niż w ręcznym `compare`. Jasno pokazuje primary i secondary sort key.
9. Custom `AbstractAssert` przenosi powtarzalne sprawdzanie error contract do jednej, nazwanej asercji. Test mówi wtedy `hasErrorCode(...)`, a nie powtarza techniczne `extracting`.

```java
assertThat(error)
    .hasStatus(403)
    .hasCode("forbidden")
    .hasCorrelationId();
```

10. `satisfiesExactly` sprawdza elementy w kolejności i liczbie. `allSatisfy` sprawdza warunek dla każdego elementu, a `anySatisfy` wymaga przynajmniej jednego pasującego elementu.

```java
assertThat(orders).allSatisfy(order -> assertThat(order.currency()).isEqualTo("PLN"));
assertThat(orders).anySatisfy(order -> assertThat(order.status()).isEqualTo("CREATED"));
```

## 14. Zadania Praktyczne

| Zadanie | Files | Command | Expected |
|---|---|---|---|
| Zaimplementuj `PaymentOrderApi` client | `testsupport/PaymentOrderApi.java` | `./mvnw test` | API client działa |
| Zaimplementuj `PaymentOrderBuilder` | `testsupport/PaymentOrderBuilder.java` | `./mvnw test` | Builder tworzy requesty |
| Zaimplementuj `PaymentErrorSpecs` | `testsupport/PaymentErrorSpecs.java` | `./mvnw test` | Reusable error specs |
| Dodaj secret masking | `RestAssuredLoggingConfig.java` | `./mvnw test` | Authorization nie w logach |
| Zrefaktoruj 3 testy do @Nested | `PaymentOrderRestAssuredTest.java` | `./mvnw test` | Testy zorganizowane |
| Napisz scenario flow test | `PaymentOrderScenarioFlowTest.java` | `./mvnw test` | Multi-step test działa |

### Rozwiązania / wskazówki

1. `PaymentOrderApi` powinien mieć metody typu `createOrder`, `readOrder`, `listOrders`, `getSummary`. Nie ukrywaj expected statusu, jeśli test ma sprawdzać różne odpowiedzi.
2. `PaymentOrderBuilder` powinien mieć sensible defaults i fluent overrides. Każdy test zmienia tylko pola istotne dla scenariusza.
3. `PaymentErrorSpecs` powinien centralizować powtarzalne oczekiwania błędów, np. content type, status i error code. Nie rób z niego czarnej skrzynki ukrywającej business reason.
4. Po dodaniu secret masking wywołaj celowo failed test lokalnie i sprawdź log. `Authorization` nie powinien pokazywać tokena.
5. `@Nested` wprowadź wokół naturalnych grup, np. `CreateOrder`, `ReadOrder`, `Idempotency`. Refaktor nie powinien zmieniać danych ani oracle testów.
6. Scenario flow ma utworzyć order, znaleźć go na liście i zobaczyć wpływ w summary. Asercje powinny sprawdzać tylko business-relevant ciągłość danych.

```java
assertThat(list.content())
    .extracting("paymentOrderId")
    .contains(created.paymentOrderId());
assertThat(summary.totalOrders()).isGreaterThanOrEqualTo(1);
```

## 15. Mini Interview Prep

**Q: Why did you introduce an API client wrapper pattern?**

A: Raw REST Assured chains (`given().when().then()`) are verbose and repeat technical setup (port, auth, content type) in every test. An API client wrapper like `PaymentOrderApi` encapsulates this setup and exposes business-readable methods like `createOrder(...)`, `listOrders(...)`, `getSummary(...)`. This makes tests more readable, reduces duplication, and centralizes endpoint URLs and request construction.

**Q: How does the builder pattern improve test data construction?**

A: Instead of `Map.of("amountMinor", 1000, "currency", "PLN", ...)` which is positional and error-prone, a builder like `aPaymentOrder().withAmountMinor(1000).withCurrency("PLN")` is self-documenting, type-safe, and allows sensible defaults. It also makes it easy to create variants: `aPaymentOrder().withCurrency("EUR")` vs `aPaymentOrder().withAmountMinor(9999)`.

**Q: Why is secret masking important in test logs?**

A: When tests fail in CI, REST Assured logs the full request/response for debugging. Without masking, the `Authorization: Bearer <token>` header is logged, potentially exposing credentials in CI logs or shared artifacts. `blacklistHeader("Authorization")` ensures tokens are replaced with `[BLACKLISTED]` in logs.

## 16. Verification Commands

```bash
cd apps/backend
./mvnw test
./mvnw -Dtest=PaymentOrderScenarioFlowTest test
./mvnw -Dtest="*Test" -Dgroups="security" test  # Run only @Tag("security") tests
```

## 17. Learning Outcome Checklist

Po tej lekcji umiem:

- [ ] Zaimplementować API client wrapper pattern dla REST Assured
- [ ] Zbudować test data builder z fluent API i sensible defaults
- [ ] Stworzyć reusable ResponseSpecification dla error contracts
- [ ] Skonfigurować secret masking w REST Assured logs
- [ ] Zorganizować testy z @Nested i @Tag
- [ ] Napisać multi-step scenario flow test
- [ ] Użyć sealed interface dla test data hierarchies
- [ ] Zastosować Map.copyOf / List.copyOf dla defensive copies
- [ ] Użyć Comparator.comparing / thenComparing dla test data ordering
- [ ] Zaimplementować custom AbstractAssert dla domain-specific assertions
- [ ] Użyć satisfiesExactly / allSatisfy / anySatisfy dla collection assertions
- [ ] Użyć matches(Predicate) dla custom conditions

## 18. Powiązane Notatki W Vault

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 08 - Payment Aggregation Summary]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Prompt - Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Learning Coverage Backlog]]
- [[Senior SDET Competency Coverage Matrix]]
- [[Lesson Evidence Tracker]]
