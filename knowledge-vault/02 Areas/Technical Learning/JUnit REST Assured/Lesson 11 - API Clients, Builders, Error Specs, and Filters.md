---
type: lesson
status: planned
area: JUnit REST Assured
lesson: 11
module: API Clients, Builders, Error Specs, and Filters
date: 2026-05-31
tags:
  - rest-assured
  - api-client
  - builder-pattern
  - error-specs
  - filters
  - lesson-11
  - senior-sdet
---

# Lesson 11 — API Clients, Builders, Error Specs, and Filters

> **Evidence link:** `PaymentOrderApi.java` (planned), `PaymentOrderBuilder.java` (planned), `PaymentErrorSpecs.java` (planned)
>
> **Navigation:** [[JUnit REST Assured MOC]] | [[Lesson 11 - REST Assured Framework Architecture and Test Organization]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się profesjonalnych REST Assured patterns:
- **API client wrapper** — business-readable methods zamiast raw `given().when().then()`
- **Test data builders** — fluent API zamiast `Map.of(...)`
- **Reusable error specs** — `ResponseSpecification` dla error contracts
- **REST Assured Filter** — request/response interceptors
- **Secret masking** — `blacklistHeader()` dla Authorization

## 2. Prerequisites

- `given()/when()/then()` (Lesson 01-05).
- `RequestSpecBuilder` / `ResponseSpecBuilder` (Lesson 07).
- `extract().as(Class)` (Lesson 06-07).
- `RestAssuredLoggingConfig` (Lesson 07).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PaymentOrderApi.java` | API client wrapper z `createOrder()`, `listOrders()`, `getSummary()` |
| `PaymentOrderBuilder.java` | Builder pattern z `aPaymentOrder().withAmount(1000).build()` |
| `PaymentErrorSpecs.java` | Reusable `validationError()`, `forbiddenError()`, `notFoundError()` |
| `RestAssuredLoggingConfig.java` | `blacklistHeader("Authorization")` configuration |
| `PaymentOrderRestAssuredTest.java` | Refactored tests using API client + builders |

## 4. Kluczowe Pojęcia

### 4.1 API Client Wrapper Pattern

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
    
    public PaymentOrderResponse createOrderExpectingError(String merchantId, String token,
                                                          PaymentOrderBuilder builder,
                                                          ResponseSpecification errorSpec) {
        return given()
            .port(port)
            .auth().oauth2(token)
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(builder.build())
        .when()
            .post("/api/merchants/{merchantId}/payment-orders", merchantId)
        .then()
            .spec(errorSpec)
            .extract().as(PaymentErrorResponse.class);
    }
}
```

**Dlaczego API client?**
- Ukrywa technical setup (port, auth, content type, idempotency key)
- Eksponuje business intent (`createOrder`, `listOrders`)
- Centralizuje endpoint URLs (zmiana URL = zmiana w jednym miejscu)
- Type-safe responses (nie raw `Response`)

### 4.2 Test Data Builder Pattern

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

**Dlaczego builder?**
- Self-documenting (`withAmountMinor(1000)` vs positional `Map.of("amountMinor", 1000)`)
- Sensible defaults (nie musisz podawać wszystkich pól)
- Type-safe (compiler sprawdza typy)
- Immutable result (record)

### 4.3 Reusable Error Specs

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

**Dlaczego reusable specs?**
- DRY (nie powtarzaj `.statusCode(400).contentType(JSON).body("error", "validation")` w każdym teście)
- Consistency (wszystkie testy sprawdzają ten sam error contract)
- Maintainability (zmiana error contract = zmiana w jednym miejscu)

### 4.4 REST Assured Filter

```java
public class CorrelationIdFilter implements Filter {
    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                          FilterableResponseSpecification responseSpec,
                          FilterContext ctx) {
        // Add correlation ID if not present
        if (!requestSpec.getHeaders().hasHeaderWithName("X-Correlation-ID")) {
            requestSpec.header("X-Correlation-ID", UUID.randomUUID().toString());
        }
        
        // Execute request
        Response response = ctx.next(requestSpec, responseSpec);
        
        // Log correlation ID for debugging
        String correlationId = response.getHeader("X-Correlation-ID");
        System.out.println("Request correlation ID: " + correlationId);
        
        return response;
    }
}

// Usage:
RestAssured.filters(new CorrelationIdFilter());
```

**Dlaczego Filter?**
- Interceptory request/response (logging, correlation ID, timing)
- Globalne (skonfiguruj raz, używaj wszędzie)
- Non-invasive (nie zmieniaj testów, dodaj filter)

### 4.5 Secret Masking

```java
public final class RestAssuredLoggingConfig {
    
    private RestAssuredLoggingConfig() {}
    
    public static void configure() {
        LogConfig logConfig = logConfig()
            .blacklistHeader("Authorization")
            .blacklistHeader("Idempotency-Key")
            .enableLoggingOfRequestAndResponseIfValidationFails();
        
        RestAssured.config = RestAssured.config().logConfig(logConfig);
    }
}
```

**Dlaczego secret masking?**
- CI logs mogą być widoczne dla wielu osób
- Authorization header zawiera Bearer token (credential)
- Idempotency-Key może być sensitive w niektórych systemach
- `blacklistHeader()` zastępuje wartość `[BLACKLISTED]` w logach

## 5. Walkthrough — Od Raw Test Do API Client

```
PRZED:
1. Test wywołuje given().port(port).auth().oauth2(token).contentType(JSON)
2. Dodaje header("Idempotency-Key", UUID.randomUUID())
3. Buduje body z Map.of("amountMinor", 1000, "currency", "PLN", ...)
4. Wywołuje .when().post("/api/merchants/{merchantId}/payment-orders", merchantId)
5. Sprawdza .then().statusCode(201)
6. Extractuje .extract().as(PaymentOrderResponse.class)

PO:
1. Test wywołuje paymentApi.createOrder(merchantId, token, aPaymentOrder().withAmountMinor(1000))
2. API client ukrywa port, auth, content type, idempotency key
3. Builder buduje CreatePaymentOrderRequest record
4. API client wysyła request i zwraca typed response
5. Test asercjonuje response (nie raw Response)
```

## 6. Learning Delta — Co Nowe vs Lessons 06-10

| Temat | Lesson 06-10 | Lesson 11 |
|---|---|---|
| Test style | Raw `given().when().then()` | API client wrapper |
| Test data | `Map.of(...)` | Builder pattern |
| Error assertions | Inline `.statusCode(400).body(...)` | Reusable `ResponseSpecification` |
| Logging | `enableLoggingOfRequestAndResponseIfValidationFails()` | + `blacklistHeader("Authorization")` |
| Interceptors | Brak | REST Assured Filter |
| Scenario tests | Create → Get (2 steps) | Create → List → Summary (3+ steps) |

## 7. Typowe Błędy

1. **Over-engineering API client.** Nie dodawaj metod, których nie używasz. YAGNI.
2. **Builder z too many defaults.** Jeśli test wymaga specyficznych wartości, nie ukrywaj ich w defaults.
3. **Error spec zbyt ogólny.** `validationError()` sprawdza `error=validation`, ale nie `message`. Dodaj `.body("message", containsString(...))` w teście.
4. **Filter zmieniający behavior.** Filter powinien być non-invasive (logging, correlation ID), nie zmieniać request/response.
5. **Zapominanie o `blacklistHeader()`.** Jeśli dodajesz nowy sensitive header, dodaj go do blacklist.

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Zaimplementuj `PaymentOrderApi` z `createOrder()`, `getOrder()`, `listOrders()` | 45 min |
| 2 | Zaimplementuj `PaymentOrderBuilder` z fluent API i sensible defaults | 30 min |
| 3 | Zaimplementuj `PaymentErrorSpecs` z `validationError()`, `forbiddenError()`, `notFoundError()` | 20 min |
| 4 | Dodaj `blacklistHeader("Authorization")` do `RestAssuredLoggingConfig` | 10 min |
| 5 | Zrefaktoruj 3 testy z `PaymentOrderRestAssuredTest` aby używały API client + builder | 45 min |
| 6 | Napisz scenario flow test: create → list → summary | 30 min |
| 7 | Zaimplementuj `CorrelationIdFilter` i dodaj go globalnie | 20 min |

## 9. Pytania

1. Dlaczego API client wrapper jest lepszy niż surowe REST Assured chains?
2. Kiedy builder pattern jest overkill dla test data?
3. Jak `ResponseSpecification` różni się od inline assertions?
4. Dlaczego `blacklistHeader()` jest ważne w CI logs?
5. Jak REST Assured Filter różni się od `RequestSpecBuilder`?
6. Kiedy używać `createOrderExpectingError()` zamiast `createOrder()`?
7. Dlaczego builder zwraca immutable record zamiast mutable object?
8. Jak `PaymentErrorSpecs.conflictError(String errorCode)` obsługuje różne 409 cases?
9. Czy API client powinien rzucać wyjątki czy zwracać error responses?
10. Jak testować, że `blacklistHeader()` faktycznie maskuje sekrety?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `paymentOrderApiCreatesOrderSuccessfully` | API client wrapper works |
| `paymentOrderBuilderCreatesValidRequest` | Builder pattern works |
| `validationErrorSpecMatches400Response` | Reusable error spec works |
| `blacklistHeaderMasksAuthorization` | Secret masking works |
| `correlationIdFilterAddsHeader` | Filter interceptor works |
| `createListAndSummarizeFlow` | Multi-step scenario flow |

## 11. Powiązane Notatki

- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 08 - Aggregation Contract, Security, and Business Flow Tests]]
- [[13-22 Professional Practice After Refactoring]]
- [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Senior SDET Competency Coverage Matrix]]
