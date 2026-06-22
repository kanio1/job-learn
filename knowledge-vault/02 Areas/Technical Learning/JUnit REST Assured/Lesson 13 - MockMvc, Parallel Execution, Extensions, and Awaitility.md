---
type: lesson
status: planned
area: JUnit REST Assured
lesson: 13
module: MockMvc, Parallel Execution, Extensions, and Awaitility
date: 2026-05-31
tags:
  - junit
  - mockmvc
  - parallel-execution
  - extensions
  - awaitility
  - lesson-13
  - senior-sdet
---

# Lesson 13 — MockMvc, Parallel Execution, Extensions, and Awaitility

> **Evidence link:** `PaymentOrderControllerTest.java` (planned), `PaymentOrderParallelTest.java` (planned), `PaymentOrderAsyncTest.java` (planned)
>
> **Navigation:** [[JUnit REST Assured MOC]] | [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]] | [[Lesson Evidence Tracker]]

## 1. Cel Lekcji

Nauczyć się zaawansowanych JUnit 5 i Spring Testing features:
- **MockMvc** — HTTP request simulation bez network
- **@Execution(CONCURRENT)** — parallel test execution
- **JUnit 5 Extensions** — custom test lifecycle (@ExtendWith)
- **Awaitility** — async polling (zamiast Thread.sleep)

## 2. Prerequisites

- @SpringBootTest basics (Lesson 06).
- @DataJpaTest basics (Lesson 06).
- JUnit 5 basics (@Test, @DisplayName) — Lesson 06-07.
- Basic AssertJ (Lesson 06-07).

## 3. Code Reading Map

| Plik | Co czytać |
|---|---|
| `PaymentOrderControllerTest.java` | @WebMvcTest + MockMvc tests |
| `PaymentOrderParallelTest.java` | @Execution(CONCURRENT) tests |
| `PaymentOrderAsyncTest.java` | Awaitility async polling |
| `junit-platform.properties` | Parallel execution configuration |

## 4. Kluczowe Pojęcia

### 4.1 MockMvc — HTTP Request Simulation

```java
@WebMvcTest(PaymentOrderController.class)
class PaymentOrderControllerTest {
    
    @MockBean
    private PaymentOrderService paymentOrderService;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void createPaymentOrderReturns201() throws Exception {
        // Arrange
        PaymentOrder order = aPaymentOrder().withId(UUID.randomUUID()).build();
        when(paymentOrderService.createOrder(any(), any(), any()))
            .thenReturn(new PaymentCreateResult(order, true));
        
        // Act & Assert
        mockMvc.perform(post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "amountMinor": 5000,
                        "currency": "PLN",
                        "clientOrderReference": "TEST-001"
                    }
                    """)
                .header("Idempotency-Key", "idem-123")
                .with(jwt().jwt(jwt -> jwt.claim("merchant_id", merchantId.toString()))))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(header().exists("ETag"))
            .andExpect(jsonPath("$.paymentOrderId").exists())
            .andExpect(jsonPath("$.amountMinor").value(5000));
    }
}
```

**Dlaczego MockMvc?**
- **Szybkość:** Brak network overhead (symulacja HTTP, nie real requests)
- **Izolacja:** Testujesz tylko controller (nie service, repository, database)
- **Precyzja:** Możesz testować request mapping, validation, exception handling osobno
- **Debugging:** Łatwiej debugować controller logic bez full stack

**MockMvc vs REST Assured:**

| Feature | MockMvc | REST Assured |
|---|---|---|
| Network | Brak (symulacja) | Real HTTP requests |
| Speed | Szybkie (1-2s startup) | Wolniejsze (5-10s startup) |
| Isolation | Tylko controller | Full stack (controller + service + repository + database) |
| Use case | Unit tests, controller logic tests | Integration tests, end-to-end tests |
| Dependencies | @MockBean dla service | Real service, repository, database |

### 4.2 @Execution(CONCURRENT) — Parallel Test Execution

```java
@Execution(ExecutionMode.CONCURRENT)
class PaymentOrderParallelTest {
    
    @Test
    void test1() {
        // Runs in parallel with test2, test3
        String merchantId = merchantApi.createActiveMerchant();
        // Test logic...
    }
    
    @Test
    void test2() {
        // Runs in parallel with test1, test3
        String merchantId = merchantApi.createActiveMerchant();
        // Test logic...
    }
    
    @Test
    void test3() {
        // Runs in parallel with test1, test2
        String merchantId = merchantApi.createActiveMerchant();
        // Test logic...
    }
}
```

**Konfiguracja w junit-platform.properties:**
```properties
# Enable parallel execution
junit.jupiter.execution.parallel.enabled=true

# Default mode for test methods
junit.jupiter.execution.parallel.mode.default=concurrent

# Default mode for test classes
junit.jupiter.execution.parallel.mode.classes.default=concurrent

# Parallelism strategy
junit.jupiter.execution.parallel.config.strategy=fixed
junit.jupiter.execution.parallel.config.fixed.parallelism=4
```

**Wymagania dla parallel tests:**
- **Test data isolation:** Każdy test tworzy własne dane (per-test merchant creation)
- **No shared mutable state:** Brak static fields, singletons które tests modyfikują
- **Thread-safe test support:** Test support classes muszą być thread-safe
- **Resource locks:** Jeśli tests używają shared resources (np. database), użyj @ResourceLock

**Resource locks:**
```java
@ResourceLock("database")
@Test
void testThatModifiesDatabase() {
    // Runs sequentially with other tests that lock "database"
}

@ResourceLock(value = "database", mode = ResourceAccessMode.READ)
@Test
void testThatReadsDatabase() {
    // Can run in parallel with other READ tests
    // Runs sequentially with WRITE tests
}
```

### 4.3 JUnit 5 Extensions — Custom Test Lifecycle

```java
// Custom extension: setup test data before each test
public class TestDataExtension implements BeforeEachCallback, AfterEachCallback {
    
    @Override
    public void beforeEach(ExtensionContext context) {
        // Setup test data (np. create merchant, seed payment orders)
        String merchantId = merchantApi.createActiveMerchant();
        getStore(context).put("merchantId", merchantId);
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        // Cleanup test data (np. delete merchant, payment orders)
        String merchantId = getStore(context).get("merchantId", String.class);
        merchantApi.deleteMerchant(merchantId);
    }
    
    private ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(getClass(), context.getRequiredTestMethod()));
    }
}

// Usage:
@ExtendWith(TestDataExtension.class)
class PaymentOrderTest {
    
    @Test
    void test1(ExtensionContext context) {
        String merchantId = context.getStore(ExtensionContext.Namespace.create(TestDataExtension.class, context.getRequiredTestMethod()))
            .get("merchantId", String.class);
        // Test logic...
    }
}
```

**Kiedy używać Extensions?**
- Gdy chcesz custom test lifecycle (setup/cleanup przed/po każdym teście)
- Gdy chcesz inject dependencies do tests (np. merchantId, test data)
- Gdy chcesz reusable test infrastructure (np. database cleanup, test data seeding)

**Built-in Extensions:**
- `@TempDir` — temporary directory dla file tests
- `@RegisterExtension` — programmatic extension registration
- `@ExtendWith` — declarative extension registration

### 4.4 Awaitility — Async Polling

```java
// Awaitility: poll until condition is met
@Test
void webhookDeliveryCompletesWithin10Seconds() {
    // Act: trigger webhook
    webhookService.deliver(webhookId);
    
    // Assert: poll until webhook is delivered
    Awaitility.await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> {
            WebhookDelivery delivery = webhookRepository.findById(webhookId).orElseThrow();
            assertThat(delivery.getStatus()).isEqualTo("DELIVERED");
        });
}

// Awaitility z custom condition:
@Test
void asyncProcessingCompletesWithin5Seconds() {
    // Act: trigger async processing
    processingService.processAsync(orderId);
    
    // Assert: poll until processing is complete
    Awaitility.await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(200))
        .until(() -> {
            PaymentOrder order = orderRepository.findById(orderId).orElseThrow();
            return order.getStatus().equals("PROCESSED");
        });
}

// Awaitility z initial delay:
Awaitility.await()
    .atMost(Duration.ofSeconds(10))
    .pollDelay(Duration.ofSeconds(1))  // Wait 1s before first poll
    .pollInterval(Duration.ofMillis(500))
    .untilAsserted(() -> { ... });
```

**Dlaczego Awaitility zamiast Thread.sleep?**
- **Thread.sleep:** Fixed wait time (np. 5s). Jeśli operation kończy się w 1s, czekasz 4s za długo. Jeśli operation kończy się w 10s, test failuje.
- **Awaitility:** Polls until condition is met (lub timeout). Jeśli operation kończy się w 1s, test kończy się w 1s. Jeśli operation kończy się w 10s, test failuje po timeout.

**Kiedy używać Awaitility?**
- Async operations (webhook delivery, message queue processing)
- Eventual consistency (database replication, cache invalidation)
- Polling external systems (API calls, file system checks)

## 5. Walkthrough — Od @SpringBootTest Do @WebMvcTest

```
PRZED (@SpringBootTest):
1. Test uruchamia full Spring context (controller + service + repository + database)
2. Test wysyła real HTTP requests (network overhead)
3. Test trwa 5-10s (startup + network)
4. Test jest integration test (testuje full stack)

PO (@WebMvcTest):
1. Test uruchamia tylko web layer (controller + exception handler)
2. Test używa MockMvc (symulacja HTTP, brak network)
3. Test trwa 1-2s (startup, brak network)
4. Test jest unit test (testuje tylko controller logic)
```

## 6. Learning Delta — Co Nowe vs Lessons 06-12

| Temat | Lesson 06-12 | Lesson 13 |
|---|---|---|
| Spring testing | @SpringBootTest, @DataJpaTest | @WebMvcTest, MockMvc |
| HTTP requests | Real HTTP (REST Assured) | Simulated HTTP (MockMvc) |
| Test execution | Sequential | Parallel (@Execution(CONCURRENT)) |
| Test lifecycle | @BeforeEach, @AfterEach | Extensions (@ExtendWith) |
| Async testing | Thread.sleep (anti-pattern) | Awaitility (polling) |

## 7. Typowe Błędy

1. **@WebMvcTest bez @MockBean.** Jeśli nie dodasz @MockBean dla service dependencies, Spring próbuje stworzyć real beans (i failuje).
2. **MockMvc bez .with(jwt()).** Jeśli controller wymaga JWT authentication, musisz dodać `.with(jwt().jwt(...))` do MockMvc request.
3. **Parallel tests z shared mutable state.** Jeśli tests modyfikują static fields lub singletons, parallel execution powoduje race conditions.
4. **Parallel tests bez test data isolation.** Jeśli tests używają tego samego merchant/payment order, parallel execution powoduje interference.
5. **Awaitility bez timeout.** Jeśli nie dodasz `atMost(...)`, Awaitility czeka w nieskończoność (test hanguje).
6. **Awaitility z too short timeout.** Jeśli timeout jest zbyt krótki, test failuje nawet jeśli operation się powiedzie (flaky test).
7. **Extension bez cleanup.** Jeśli extension setup test data w `beforeEach()`, musisz cleanup w `afterEach()` (inaczej test data accumulates).

## 8. Ćwiczenia

| # | Ćwiczenie | Czas |
|---|---|---|
| 1 | Napisz @WebMvcTest dla PaymentOrderController.createPaymentOrder | 30 min |
| 2 | Napisz @WebMvcTest dla PaymentOrderController.listPaymentOrders | 25 min |
| 3 | Napisz @WebMvcTest z @MockBean dla service | 20 min |
| 4 | Napisz @WebMvcTest z .with(jwt()) dla authentication | 20 min |
| 5 | Napisz parallel test z @Execution(CONCURRENT) | 25 min |
| 6 | Skonfiguruj junit-platform.properties dla parallel execution | 15 min |
| 7 | Napisz custom extension dla test data setup/cleanup | 30 min |
| 8 | Napisz Awaitility test dla async operation | 25 min |
| 9 | Porównaj Thread.sleep vs Awaitility (timing, reliability) | 20 min |

## 9. Pytania

1. Kiedy używać @WebMvcTest vs @SpringBootTest?
2. Jaka jest różnica między MockMvc a REST Assured?
3. Dlaczego MockMvc jest szybsze niż real HTTP requests?
4. Kiedy używać @Execution(CONCURRENT)?
5. Jakie są wymagania dla parallel test execution?
6. Co to jest @ResourceLock i kiedy go używać?
7. Kiedy używać JUnit 5 Extensions?
8. Jaka jest różnica między @ExtendWith a @RegisterExtension?
9. Dlaczego Awaitility jest lepsze niż Thread.sleep?
10. Jak skonfigurować Awaitility timeout i poll interval?

## 10. Testy

| Test | Co sprawdza |
|---|---|
| `createPaymentOrderReturns201` | @WebMvcTest + MockMvc works |
| `listPaymentOrdersReturns200` | MockMvc GET works |
| `parallelTestsRunConcurrently` | @Execution(CONCURRENT) works |
| `extensionSetsUpTestData` | Custom extension works |
| `webhookDeliveryCompletesWithin10Seconds` | Awaitility works |

## 11. Powiązane Notatki

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 11 - API Clients, Builders, Error Specs, and Filters]]
- [[Lesson 12 - TypeRef, GPath Advanced, Response Time, and JSON Schema]]
- [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]
- [[Senior SDET Competency Coverage Matrix]]
