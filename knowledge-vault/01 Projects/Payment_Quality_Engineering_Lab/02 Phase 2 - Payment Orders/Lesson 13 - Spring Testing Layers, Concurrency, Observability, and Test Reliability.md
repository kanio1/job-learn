---
type: lesson
status: planned
project: Payment Quality Engineering Lab
phase: 2
lesson: 13
area: Payment Orders
module: Spring Testing Layers, Concurrency, Observability, and Test Reliability
date: 2026-05-31
tags:
  - lesson
  - lesson-13
  - payment-quality-lab
  - spring-testing
  - concurrency
  - observability
  - test-reliability
  - senior-sdet
---

# Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability

> **Status:** PLANNED - test layers, async behavior, and production-grade test infrastructure
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Current Sprint]] | [[Lesson Evidence Tracker]]
>
> **Main decision:** Lesson 13 addresses the final gaps in Senior SDET competency — Spring testing layers (@WebMvcTest/MockMvc), concurrency testing (parallel execution, transaction isolation), observability (log assertions, flaky test diagnosis), and test reliability (Maven lifecycle, Awaitility).

## 1. Cel Lekcji

Lekcja 13 zamyka wszystkie luki w competency matrix dla Senior SDET. Po Lessons 06-12 mamy:
- ✅ REST Assured fundamentals + advanced (TypeRef, GPath, response time)
- ✅ AssertJ fundamentals + advanced (recursive comparison, soft assertions)
- ✅ JUnit 5 fundamentals + parameterized tests
- ✅ Java 25 features (records, sealed types, generics, pattern matching)
- ✅ SQL fundamentals + advanced (CTE, window functions)
- ✅ Security testing (BOLA/BFLA, authorization matrix)

**Brakuje:**
- ❌ Spring testing layers (@WebMvcTest, @MockBean, @SpyBean)
- ❌ Concurrency testing (parallel execution, transaction isolation, locking)
- ❌ Observability (log assertions, flaky test diagnosis)
- ❌ Test reliability (Maven lifecycle, Awaitility, failure analysis)
- ❌ Advanced Java 25 (EnumSet, EnumMap, streams advanced)
- ❌ Advanced SQL (EXPLAIN ANALYZE, deadlock detection)

Celem jest transformacja z "test writer" do "test infrastructure architect" — osoba, która projektuje nie tylko testy, ale cały test ecosystem.

## 2. Co Budujemy / Co Ćwiczymy

### Spring Testing Layers

| Component | Opis | Plik docelowy |
|---|---|---|
| @WebMvcTest | Focused controller tests (bez full Spring context) | `PaymentOrderControllerTest.java` |
| MockMvc | HTTP request/response simulation (bez network) | `PaymentOrderControllerTest.java` |
| @MockBean | Mock dependencies (service, repository) | `PaymentOrderControllerTest.java` |
| @SpyBean | Spy on real beans (partial mocking) | `PaymentOrderControllerTest.java` |
| Spring profiles | Test-specific configuration | `application-test.yml` |

### Concurrency & Transactions

| Component | Opis | Plik docelowy |
|---|---|---|
| @Execution(CONCURRENT) | Parallel test execution | `PaymentOrderParallelTest.java` |
| Transaction isolation | READ_COMMITTED vs REPEATABLE_READ vs SERIALIZABLE | `PaymentOrderTransactionTest.java` |
| Locking | Pessimistic vs optimistic locking | `PaymentOrderLockingTest.java` |
| Deadlock detection | Wykrywanie i zapobieganie deadlockom | `PaymentOrderDeadlockTest.java` |

### Observability & Reliability

| Component | Opis | Plik docelowy |
|---|---|---|
| Log assertions | Weryfikacja logów (logback-test.xml) | `PaymentOrderLoggingTest.java` |
| Flaky test diagnosis | Methodology wykrywania flaky tests | `FlakyTestDiagnosis.md` |
| Failure analysis | App bug vs test bug vs data bug vs env bug | `FailureAnalysisChecklist.md` |
| Awaitility | Async polling (np. webhook delivery) | `PaymentOrderAsyncTest.java` |
| Maven lifecycle | surefire (unit) vs failsafe (integration) | `pom.xml` |

### System batch

| Batch | Scope | Expected files |
|---|---|---|
| 13A | Spring testing layers (@WebMvcTest, MockMvc) | `PaymentOrderControllerTest.java` |
| 13B | Concurrency testing (parallel, transactions, locking) | `PaymentOrderParallelTest.java`, `PaymentOrderTransactionTest.java` |
| 13C | Observability (log assertions, flaky test diagnosis) | `PaymentOrderLoggingTest.java`, `FlakyTestDiagnosis.md` |
| 13D | Test reliability (Maven lifecycle, Awaitility) | `pom.xml`, `PaymentOrderAsyncTest.java` |
| 13E | Advanced Java 25 (EnumSet, EnumMap, streams) | Refaktor istniejących testów |
| 13F | Advanced SQL (EXPLAIN, deadlock) | `PaymentOrderQueryPerformanceTest.java` |

Default implementation: **13A + 13B + 13C** (core infrastructure). 13D, 13E, 13F są optional extensions.

## 3. Learning Delta Względem Poprzednich Lekcji

| Temat | Status |
|---|---|
| @WebMvcTest / MockMvc | **New** — focused controller tests bez full Spring context |
| @MockBean / @SpyBean | **New** — mocking dependencies w Spring tests |
| Spring profiles | **New** — test-specific configuration |
| @Execution(CONCURRENT) | **New** — parallel test execution |
| Transaction isolation levels | **New** — READ_COMMITTED vs REPEATABLE_READ vs SERIALIZABLE |
| Pessimistic vs optimistic locking | **New** — concurrency control |
| Deadlock detection | **New** — wykrywanie i zapobieganie |
| Log assertions | **New** — weryfikacja logów |
| Flaky test diagnosis | **New** — methodology |
| Failure analysis | **New** — app bug vs test bug vs data bug vs env bug |
| Awaitility | **New** — async polling |
| Maven surefire vs failsafe | **New** — unit vs integration tests |
| EnumSet / EnumMap | **New** — efficient enum collections |
| Streams advanced | **New** — groupingBy, partitioningBy, collectors |
| EXPLAIN ANALYZE | **New** — query performance diagnostics |

## 4. Mapa Kodu

### Istniejące pliki do rozszerzenia

| Plik | Obecny stan | Target stan |
|---|---|---|
| `PaymentOrderRestAssuredTest.java` | @SpringBootTest (full context) | Dodaj @WebMvcTest alternative |
| `pom.xml` | Tylko surefire plugin | Dodaj failsafe plugin dla integration tests |
| `application-test.yml` | Basic test config | Dodaj test-specific profiles |

### Nowe pliki do stworzenia

| Plik | Odpowiedzialność |
|---|---|
| `web/PaymentOrderControllerTest.java` | @WebMvcTest + MockMvc focused tests |
| `concurrency/PaymentOrderParallelTest.java` | @Execution(CONCURRENT) tests |
| `concurrency/PaymentOrderTransactionTest.java` | Transaction isolation tests |
| `concurrency/PaymentOrderLockingTest.java` | Pessimistic/optimistic locking tests |
| `observability/PaymentOrderLoggingTest.java` | Log assertions |
| `async/PaymentOrderAsyncTest.java` | Awaitility async polling |
| `FlakyTestDiagnosis.md` | Methodology documentation |
| `FailureAnalysisChecklist.md` | Failure analysis documentation |

## 5. Architecture Walkthrough

### @WebMvcTest vs @SpringBootTest

```java
// @SpringBootTest (Lesson 06-12):
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentOrderRestAssuredTest {
    // Full Spring context (controller + service + repository + database)
    // Real HTTP requests (network overhead)
    // Slower (5-10s startup)
    // Use for: integration tests, end-to-end tests
}

// @WebMvcTest (Lesson 13):
@WebMvcTest(PaymentOrderController.class)
class PaymentOrderControllerTest {
    @MockBean
    private PaymentOrderService paymentOrderService;
    
    @Autowired
    private MockMvc mockMvc;
    
    // Only web layer (controller + exception handler)
    // Mocked dependencies (service, repository)
    // No HTTP (MockMvc simulates requests)
    // Faster (1-2s startup)
    // Use for: unit tests, controller logic tests
}
```

**Kiedy używać @WebMvcTest?**
- Gdy testujesz tylko controller logic (request mapping, validation, exception handling)
- Gdy chcesz szybkie tests (bez database, bez network)
- Gdy chcesz isolate controller od service bugs

**Kiedy używać @SpringBootTest?**
- Gdy testujesz full integration (controller + service + repository + database)
- Gdy chcesz real HTTP requests (network overhead, real ports)
- Gdy chcesz testować end-to-end workflows

### MockMvc Example

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
    
    @Test
    void createPaymentOrderWithInvalidAmountReturns400() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "amountMinor": -100,
                        "currency": "PLN",
                        "clientOrderReference": "TEST-002"
                    }
                    """)
                .header("Idempotency-Key", "idem-456")
                .with(jwt().jwt(jwt -> jwt.claim("merchant_id", merchantId.toString()))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message").value(containsString("amountMinor")));
    }
}
```

### @MockBean vs @SpyBean

```java
// @MockBean: complete mock (all methods return default values)
@MockBean
private PaymentOrderService paymentOrderService;

// @SpyBean: partial mock (real methods called unless stubbed)
@SpyBean
private PaymentOrderService paymentOrderService;

// Kiedy używać @MockBean?
// - Gdy chcesz isolate controller od service
// - Gdy service ma external dependencies (database, API calls)
// - Gdy chcesz testować tylko controller logic

// Kiedy używać @SpyBean?
// - Gdy chcesz testować service + controller razem
// - Gdy chcesz stub tylko niektóre methods (np. external API call)
// - Gdy chcesz verify interactions (verify(service).createOrder(...))
```

### @Execution(CONCURRENT) dla Parallel Test Execution

```java
@Execution(ExecutionMode.CONCURRENT)
class PaymentOrderParallelTest {
    
    @Test
    void test1() {
        // Runs in parallel with test2, test3
    }
    
    @Test
    void test2() {
        // Runs in parallel with test1, test3
    }
    
    @Test
    void test3() {
        // Runs in parallel with test1, test2
    }
}

// Konfiguracja w junit-platform.properties:
// junit.jupiter.execution.parallel.enabled=true
// junit.jupiter.execution.parallel.mode.default=concurrent
// junit.jupiter.execution.parallel.config.strategy=fixed
// junit.jupiter.execution.parallel.config.fixed.parallelism=4
```

**Wymagania dla parallel tests:**
- Test data isolation (każdy test ma własne dane)
- No shared mutable state (static fields, singletons)
- Thread-safe test support classes
- Resource locks (jeśli tests używają shared resources)

### Transaction Isolation Levels

```java
// READ_COMMITTED (default w PostgreSQL):
// - Widzi tylko committed changes
// - Może widzieć phantom reads (new rows inserted by other transactions)
@Transactional(isolation = Isolation.READ_COMMITTED)
void testReadCommitted() {
    // Transaction 1: reads data
    // Transaction 2: inserts new row and commits
    // Transaction 1: reads again and sees new row (phantom read)
}

// REPEATABLE_READ:
// - Widzi tylko committed changes z momentu startu transaction
// - Nie widzi phantom reads
@Transactional(isolation = Isolation.REPEATABLE_READ)
void testRepeatableRead() {
    // Transaction 1: reads data
    // Transaction 2: inserts new row and commits
    // Transaction 1: reads again and does NOT see new row (no phantom read)
}

// SERIALIZABLE:
// - Pełna izolacja (jakby transactions były serial)
// - Najwolniejsze (najwięcej locks)
@Transactional(isolation = Isolation.SERIALIZABLE)
void testSerializable() {
    // Transaction 1: reads data
    // Transaction 2: inserts new row and commits
    // Transaction 1: reads again and does NOT see new row
    // Transaction 1: updates data
    // Transaction 1: commits (może fail jeśli Transaction 2 conflicting)
}
```

### Pessimistic vs Optimistic Locking

```java
// Pessimistic locking (lock row przed read/write):
PaymentOrder order = repository.findById(orderId)
    .orElseThrow();
// Lock row (other transactions wait)
repository.lock(order, LockModeType.PESSIMISTIC_WRITE);
order.setStatus("AUTHORIZED");
repository.save(order);
// Lock released after transaction commits

// Optimistic locking (version column):
@Entity
class PaymentOrder {
    @Version
    private Long version;
}

PaymentOrder order = repository.findById(orderId).orElseThrow();
order.setStatus("AUTHORIZED");
repository.save(order);
// Jeśli version changed by other transaction → OptimisticLockException
```

**Kiedy używać pessimistic vs optimistic?**
- **Pessimistic:** Gdy conflicts są częste (np. inventory management)
- **Optimistic:** Gdy conflicts są rzadkie (np. payment order status updates)

### Log Assertions

```java
// logback-test.xml:
<configuration>
    <appender name="LIST" class="ch.qos.logback.core.read.ListAppender"/>
    <root level="INFO">
        <appender-ref ref="LIST"/>
    </root>
</configuration>

// Test:
@SpringBootTest
class PaymentOrderLoggingTest {
    
    @Autowired
    private LoggerContext loggerContext;
    
    @Test
    void createPaymentOrderLogsCorrelationId() {
        // Arrange
        ListAppender<ILoggingEvent> listAppender = 
            (ListAppender<ILoggingEvent>) loggerContext.getLogger("ROOT")
                .getAppender("LIST");
        listAppender.list.clear();
        
        // Act
        paymentOrderService.createOrder(...);
        
        // Assert
        assertThat(listAppender.list)
            .extracting(ILoggingEvent::getFormattedMessage)
            .anyMatch(msg -> msg.contains("correlationId=idem-123"));
    }
}
```

### Awaitility dla Async Polling

```java
// Awaitility: poll until condition is met (np. webhook delivery)
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
```

### Maven Surefire vs Failsafe

```xml
<!-- pom.xml -->
<build>
    <plugins>
        <!-- Surefire: unit tests (fast, no external dependencies) -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IT.java</exclude>
                </excludes>
            </configuration>
        </plugin>
        
        <!-- Failsafe: integration tests (slow, external dependencies) -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <includes>
                    <include>**/*IT.java</include>
                </includes>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Kiedy używać surefire vs failsafe?**
- **Surefire:** Unit tests (fast, isolated, no external dependencies)
- **Failsafe:** Integration tests (slow, external dependencies like database, API)
- **Naming convention:** `*Test.java` dla surefire, `*IT.java` dla failsafe

## 6. HTTP I REST API

### Testcontainers Advanced

```java
// Init scripts (seed data):
@Testcontainers
class PaymentOrderTestcontainersTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withInitScript("test-data/seed-merchants.sql");
    
    // Database starts with pre-loaded data
}

// Reuse container (faster tests):
@Testcontainers
class PaymentOrderTestcontainersTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
        .withReuse(true);  // Container reused between test runs
    
    // First run: 5s startup
    // Subsequent runs: 0.5s startup
}

// Custom image (np. z extensions):
@Testcontainers
class PaymentOrderTestcontainersTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("my-postgres:18-with-extensions")
        .withCommand("postgres -c shared_preload_libraries=pg_stat_statements");
}
```

## 7. Java 25 I Java Code Reading

### EnumSet / EnumMap

```java
// EnumSet: efficient set dla enums
EnumSet<PaymentStatus> allowedStatuses = EnumSet.of(
    PaymentStatus.CREATED, 
    PaymentStatus.AUTHORIZED
);

EnumSet<PaymentStatus> allStatuses = EnumSet.allOf(PaymentStatus.class);

EnumSet<PaymentStatus> noStatuses = EnumSet.noneOf(PaymentStatus.class);

// EnumMap: efficient map dla enum keys
EnumMap<PaymentStatus, Long> statusCounts = new EnumMap<>(PaymentStatus.class);
statusCounts.put(PaymentStatus.CREATED, 10L);
statusCounts.put(PaymentStatus.AUTHORIZED, 5L);

// Dlaczego EnumSet/EnumMap?
// - Memory efficient (bit vector dla EnumSet, array dla EnumMap)
// - Type-safe (compiler sprawdza enum type)
// - Fast (O(1) operations)
```

### Streams Advanced: groupingBy, partitioningBy

```java
// groupingBy: group elements by key
Map<String, List<PaymentOrderResponse>> ordersByCurrency = orders.stream()
    .collect(Collectors.groupingBy(PaymentOrderResponse::currency));

// groupingBy z downstream collector:
Map<String, Long> countByCurrency = orders.stream()
    .collect(Collectors.groupingBy(
        PaymentOrderResponse::currency,
        Collectors.counting()
    ));

Map<String, Long> totalAmountByCurrency = orders.stream()
    .collect(Collectors.groupingBy(
        PaymentOrderResponse::currency,
        Collectors.summingLong(PaymentOrderResponse::amountMinor)
    ));

// partitioningBy: split into two groups (predicate true/false)
Map<Boolean, List<PaymentOrderResponse>> partitionedByAmount = orders.stream()
    .collect(Collectors.partitioningBy(order -> order.amountMinor() > 5000));

List<PaymentOrderResponse> highAmount = partitionedByAmount.get(true);
List<PaymentOrderResponse> lowAmount = partitionedByAmount.get(false);
```

## 8. SQL, PostgreSQL I Flyway

### EXPLAIN ANALYZE

```sql
-- EXPLAIN: query plan (bez execution)
EXPLAIN SELECT * FROM payment_orders WHERE merchant_id = '123' AND status = 'CREATED';

-- EXPLAIN ANALYZE: query plan + actual execution time
EXPLAIN ANALYZE SELECT * FROM payment_orders WHERE merchant_id = '123' AND status = 'CREATED';

-- Output:
-- Seq Scan on payment_orders  (cost=0.00..10.50 rows=5 width=100) (actual time=0.015..0.020 rows=3 loops=1)
--   Filter: (merchant_id = '123'::uuid AND status = 'CREATED'::payment_status)
--   Rows Removed by Filter: 97
-- Planning Time: 0.100 ms
-- Execution Time: 0.050 ms

-- Co czytać:
-- - Seq Scan vs Index Scan (czy używa index?)
-- - cost (estimated cost, first row vs total)
-- - actual time (actual execution time)
-- - rows (estimated vs actual rows)
-- - Rows Removed by Filter (ile rows odrzuconych)
```

**Kiedy używać EXPLAIN ANALYZE?**
- Gdy query jest wolne (> 100ms)
- Gdy chcesz zrozumieć dlaczego query jest wolne
- Gdy chcesz verify czy index jest używany
- **Uwaga:** EXPLAIN ANALYZE wykonuje query (może mieć side effects dla INSERT/UPDATE/DELETE)

### Deadlock Detection

```sql
-- Deadlock scenario:
-- Transaction 1: locks row A, waits for row B
-- Transaction 2: locks row B, waits for row A
-- Result: deadlock (PostgreSQL kills one transaction)

-- Wykrywanie deadlocków:
SELECT * FROM pg_stat_activity WHERE wait_event_type = 'Lock';

-- Zapobieganie deadlockom:
-- 1. Zawsze lock rows w tej samej kolejności
-- 2. Używaj shorter transactions
-- 3. Używaj appropriate isolation level
```

## 9. Security I Tenant Isolation

### Parallel Test Execution z Test Data Isolation

```java
@Execution(ExecutionMode.CONCURRENT)
class PaymentOrderParallelTest {
    
    @Test
    void test1() {
        // Each test creates its own merchant (isolation)
        String merchantId = merchantApi.createActiveMerchant();
        // Test logic...
    }
    
    @Test
    void test2() {
        // Runs in parallel with test1
        // Uses different merchant (no interference)
        String merchantId = merchantApi.createActiveMerchant();
        // Test logic...
    }
}
```

**Wymagania dla parallel tests:**
- Per-test data creation (każdy test ma własne dane)
- No shared mutable state (static fields, singletons)
- Thread-safe test support classes
- Resource locks (jeśli tests używają shared resources)

## 10. REST Assured Learning Path

### @WebMvcTest z MockMvc

```java
@WebMvcTest(PaymentOrderController.class)
class PaymentOrderControllerTest {
    
    @MockBean
    private PaymentOrderService paymentOrderService;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void listPaymentOrdersReturns200() throws Exception {
        // Arrange
        Page<PaymentOrder> page = new PageImpl<>(List.of(
            aPaymentOrder().withId(UUID.randomUUID()).build()
        ));
        when(paymentOrderService.listOrders(any(), any())).thenReturn(page);
        
        // Act & Assert
        mockMvc.perform(get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .with(jwt().jwt(jwt -> jwt.claim("merchant_id", merchantId.toString()))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].paymentOrderId").exists());
    }
}
```

## 11. Assertion Strategy

### Log Assertions

```java
@Test
void createPaymentOrderLogsCorrelationId() {
    // Arrange
    ListAppender<ILoggingEvent> listAppender = getListAppender();
    listAppender.list.clear();
    
    // Act
    paymentOrderService.createOrder(...);
    
    // Assert
    assertThat(listAppender.list)
        .extracting(ILoggingEvent::getFormattedMessage)
        .anyMatch(msg -> msg.contains("correlationId=idem-123"))
        .anyMatch(msg -> msg.contains("Payment order created"));
}
```

### Failure Analysis Methodology

```
1. Sprawdź test failure message
   - Czy message jest clear? (np. "expected 200 but was 400")
   - Czy message wskazuje na app bug czy test bug?

2. Sprawdź test logs
   - Czy request został wysłany? (REST Assured logs)
   - Czy response został odebrany? (REST Assured logs)
   - Czy application logs pokazują error? (application logs)

3. Sprawdź application logs
   - Czy request dotarł do controller? (controller logs)
   - Czy service został wywołany? (service logs)
   - Czy database query się powiodło? (repository logs)

4. Kategoryzuj failure:
   - App bug: application logic error (np. validation bug, NPE)
   - Test bug: test logic error (np. wrong assertion, wrong test data)
   - Data bug: test data problem (np. stale data, race condition)
   - Env bug: environment problem (np. database down, network timeout)

5. Fix i verify:
   - App bug: fix application code, re-run test
   - Test bug: fix test code, re-run test
   - Data bug: fix test data isolation, re-run test
   - Env bug: fix environment, re-run test
```

## 12. Test Data Ownership

### Parallel Test Execution Strategy

```java
// Worker-safe data: każdy test tworzy własne dane
@Execution(ExecutionMode.CONCURRENT)
class PaymentOrderParallelTest {
    
    @Test
    void test1() {
        String merchantId = merchantApi.createActiveMerchant();  // Unique per test
        String idempotencyKey = UUID.randomUUID().toString();     // Unique per test
        // Test logic...
    }
    
    @Test
    void test2() {
        String merchantId = merchantApi.createActiveMerchant();  // Different from test1
        String idempotencyKey = UUID.randomUUID().toString();     // Different from test1
        // Test logic...
    }
}

// Resource locks: gdy tests używają shared resources
@ResourceLock("database")
@Test
void testThatModifiesDatabase() {
    // Runs sequentially with other tests that lock "database"
}
```

## 13. Pytania Do Samodzielnej Odpowiedzi

1. Kiedy używać @WebMvcTest vs @SpringBootTest?
2. Jaka jest różnica między @MockBean a @SpyBean?
3. Dlaczego MockMvc jest szybsze niż real HTTP requests?
4. Kiedy używać @Execution(CONCURRENT)?
5. Jakie są wymagania dla parallel test execution?
6. Jaka jest różnica między READ_COMMITTED a REPEATABLE_READ?
7. Kiedy używać pessimistic vs optimistic locking?
8. Jak wykryć deadlock w PostgreSQL?
9. Jak asercjonować logi w Spring Boot tests?
10. Jakie są 4 kategorie test failures (app bug, test bug, data bug, env bug)?
11. Kiedy używać Awaitility vs Thread.sleep?
12. Jaka jest różnica między Maven surefire a failsafe?
13. Dlaczego EnumSet jest bardziej efficient niż HashSet dla enums?
14. Jak używać Collectors.groupingBy dla grouping elements?
15. Jak używać EXPLAIN ANALYZE dla query diagnostics?

## 14. Zadania Praktyczne

| Zadanie | Files | Command | Expected |
|---|---|---|---|
| Napisz @WebMvcTest dla PaymentOrderController | `PaymentOrderControllerTest.java` | `./mvnw test` | Controller tests działają |
| Napisz test z @MockBean | `PaymentOrderControllerTest.java` | `./mvnw test` | Mocking działa |
| Napisz test z @SpyBean | `PaymentOrderControllerTest.java` | `./mvnw test` | Spying działa |
| Napisz parallel test z @Execution(CONCURRENT) | `PaymentOrderParallelTest.java` | `./mvnw test` | Parallel execution działa |
| Napisz transaction isolation test | `PaymentOrderTransactionTest.java` | `./mvnw test` | Isolation levels działają |
| Napisz log assertion test | `PaymentOrderLoggingTest.java` | `./mvnw test` | Log assertions działają |
| Napisz Awaitility test | `PaymentOrderAsyncTest.java` | `./mvnw test` | Async polling działa |
| Skonfiguruj Maven failsafe dla integration tests | `pom.xml` | `./mvnw verify` | Integration tests działają |
| Napisz test z EnumSet | Istniejące testy | `./mvnw test` | EnumSet działa |
| Napisz test z Collectors.groupingBy | Istniejące testy | `./mvnw test` | groupingBy działa |
| Napisz EXPLAIN ANALYZE query | SQL exercise | Manual | Query diagnostics działają |

## 15. Mini Interview Prep

**Q: When should you use @WebMvcTest vs @SpringBootTest?**

A: Use `@WebMvcTest` when you want to test only the web layer (controller, exception handler, validation) without starting the full Spring context. It's faster (1-2s vs 5-10s) and isolates controller logic from service/repository bugs. Use `@SpringBootTest` when you want to test full integration (controller + service + repository + database) with real HTTP requests. `@WebMvcTest` uses `MockMvc` (no network), `@SpringBootTest` uses real HTTP (network overhead).

**Q: What's the difference between @MockBean and @SpyBean?**

A: `@MockBean` creates a complete mock — all methods return default values (null, 0, false) unless stubbed. Use it when you want to isolate the class under test from its dependencies. `@SpyBean` creates a partial mock — real methods are called unless stubbed. Use it when you want to test the real behavior but stub specific methods (e.g., external API calls). `@SpyBean` also allows `verify()` to check interactions.

**Q: How do you diagnose a flaky test?**

A: Flaky tests fail intermittently without code changes. Diagnosis steps: (1) Run the test 100 times to confirm flakiness. (2) Check for shared mutable state (static fields, singletons). (3) Check for test data isolation (tests interfering with each other). (4) Check for timing issues (race conditions, async operations without proper waiting). (5) Check for environment issues (database connection pool exhaustion, network timeouts). (6) Use `@Execution(CONCURRENT)` to detect parallel execution issues. (7) Use Awaitility instead of `Thread.sleep()` for async operations.

## 16. Verification Commands

```bash
cd apps/backend
./mvnw test                                    # Unit tests (surefire)
./mvnw verify                                  # Unit + integration tests (surefire + failsafe)
./mvnw -Dtest=PaymentOrderControllerTest test  # @WebMvcTest tests
./mvnw -Dtest=PaymentOrderParallelTest test    # Parallel tests
./mvnw -Dtest=PaymentOrderLoggingTest test     # Log assertion tests
./mvnw -Dtest=PaymentModuleTest test           # Modulith architecture test
```

## 17. Learning Outcome Checklist

Po tej lekcji umiem:

- [ ] Napisać @WebMvcTest dla focused controller tests
- [ ] Użyć MockMvc do HTTP request simulation
- [ ] Użyć @MockBean do mocking dependencies
- [ ] Użyć @SpyBean do partial mocking
- [ ] Skonfigurować Spring profiles dla test-specific configuration
- [ ] Użyć @Execution(CONCURRENT) dla parallel test execution
- [ ] Zrozumieć transaction isolation levels (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE)
- [ ] Użyć pessimistic locking (LockModeType.PESSIMISTIC_WRITE)
- [ ] Użyć optimistic locking (@Version)
- [ ] Wykryć i zapobiec deadlockom
- [ ] Napisać log assertions z ListAppender
- [ ] Zdiagnozować flaky test (methodology)
- [ ] Skategoryzować test failure (app bug, test bug, data bug, env bug)
- [ ] Użyć Awaitility dla async polling
- [ ] Skonfigurować Maven surefire vs failsafe
- [ ] Użyć EnumSet dla efficient enum collections
- [ ] Użyć EnumMap dla efficient enum-keyed maps
- [ ] Użyć Collectors.groupingBy dla grouping elements
- [ ] Użyć Collectors.partitioningBy dla splitting elements
- [ ] Użyć EXPLAIN ANALYZE dla query performance diagnostics

## 18. Powiązane Notatki W Vault

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Prompt - Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]
- [[Learning Coverage Backlog]]
- [[Senior SDET Competency Coverage Matrix]]
- [[Lesson Evidence Tracker]]
