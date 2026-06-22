---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 13
date: 2026-05-31
tags:
  - prompt
  - lesson-13
  - spring-testing
  - concurrency
  - observability
  - test-reliability
  - senior-sdet
---

# Prompt - Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability

Copy this prompt and give it to Kilo when starting Lesson 13 implementation.

```text
Jesteś moim zespołem: Spring Testing Expert, Concurrency Specialist, Observability Architect, i Agent Kodowania.

Pracujemy w repozytorium:

/home/suso/job-learn

## Kontekst

Przeczytaj przed rozpoczęciem:

- `AGENTS.md`
- `specs/005-payment-order-summary/plan.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability.md`

Przeczytaj kod:

- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderServiceTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepositoryTest.java`
- `apps/backend/pom.xml`
- `apps/backend/src/test/resources/application-test.yml`

## Skills do użycia

Użyj skills:

- `payment-quality-lab-orchestrator`
- `spring-boot4-spring7-backend-architect`
- `junit6-assertj-restassured-testcraft`
- `parallel-test-architecture-and-data-isolation`
- `postgres18-data-architecture-and-risk`
- `obsidian-learning-os`

## Czego NIE powtarzać

Nie tłumacz od nowa:

- @SpringBootTest basics (Lesson 06)
- @DataJpaTest basics (Lesson 06)
- Testcontainers basics (Lesson 06)
- Basic AssertJ (Lesson 06-07)
- Basic JUnit 5 (Lesson 06-07)

Użyj tych tematów jako prerequisites.

## Cel Lesson 13

Zaprojektuj i zaimplementuj **Spring Testing Layers, Concurrency, Observability, and Test Reliability** — transformację z "test writer" do "test infrastructure architect".

Główne pytanie:

Jak zaprojektować production-grade test infrastructure z focused controller tests (@WebMvcTest), parallel execution, transaction isolation, log assertions, flaky test diagnosis, i Maven lifecycle management?

## Scope Decision

Domyślna decyzja: Lesson Extension, no Spec Kit.

Uzasadnienie:

- nie dodajemy nowych endpointów ani business logic,
- rozszerzamy test infrastructure o zaawansowane patterns,
- production code pozostaje niezmieniony (chyba że tests ujawnią bug),
- to jest "test infrastructure maturity" slice.

## Scope IN

### Batch 13A: Spring Testing Layers (@WebMvcTest, MockMvc)

Zaimplementuj:

- **Nowy plik:** `web/PaymentOrderControllerTest.java`
- **@WebMvcTest dla PaymentOrderController:**
  - `createPaymentOrderReturns201` — MockMvc POST z valid request
  - `createPaymentOrderWithInvalidAmountReturns400` — MockMvc POST z invalid request
  - `listPaymentOrdersReturns200` — MockMvc GET z pagination
  - `getPaymentOrderReturns200` — MockMvc GET z valid ID
  - `getPaymentOrderNotFoundReturns404` — MockMvc GET z invalid ID
- **@MockBean dla PaymentOrderService:**
  - Mock service methods (createOrder, listOrders, getOrder)
  - Verify interactions (verify(service).createOrder(...))
- **MockMvc z JWT:**
  - `.with(jwt().jwt(jwt -> jwt.claim("merchant_id", merchantId.toString())))`
  - Test authorization (merchant_id claim validation)

### Batch 13B: Concurrency Testing

Zaimplementuj:

- **Nowy plik:** `concurrency/PaymentOrderParallelTest.java`
  - `@Execution(ExecutionMode.CONCURRENT)` dla parallel execution
  - 3-5 tests które mogą być uruchomione równolegle
  - Każdy test tworzy własne merchant (isolation)
  - Verify no shared mutable state

- **Nowy plik:** `concurrency/PaymentOrderTransactionTest.java`
  - `testReadCommittedIsolation` — @Transactional(isolation = READ_COMMITTED)
  - `testRepeatableReadIsolation` — @Transactional(isolation = REPEATABLE_READ)
  - Demonstrate phantom reads vs no phantom reads

- **Nowy plik:** `concurrency/PaymentOrderLockingTest.java`
  - `testPessimisticLocking` — LockModeType.PESSIMISTIC_WRITE
  - `testOptimisticLocking` — @Version annotation
  - Demonstrate OptimisticLockException when version conflict

### Batch 13C: Observability (Log Assertions, Flaky Test Diagnosis)

Zaimplementuj:

- **Nowy plik:** `observability/PaymentOrderLoggingTest.java`
  - `createPaymentOrderLogsCorrelationId` — ListAppender + log assertion
  - `createPaymentOrderLogsPaymentOrderId` — verify log contains paymentOrderId
  - `createPaymentOrderLogsMerchantId` — verify log contains merchantId

- **Nowy plik:** `FlakyTestDiagnosis.md`
  - Methodology: 7 kroków diagnozowania flaky tests
  - Checklist: shared mutable state, test data isolation, timing issues, environment issues
  - Examples: common flaky test patterns i fixes

- **Nowy plik:** `FailureAnalysisChecklist.md`
  - 4 kategorie failures: app bug, test bug, data bug, env bug
  - Diagnostic steps dla każdej kategorii
  - Examples: jak rozpoznać każdą kategorię

### Batch 13D: Test Reliability (Maven Lifecycle, Awaitility) [OPTIONAL]

Zaimplementuj:

- **Maven failsafe plugin:**
  - Rozszerz `pom.xml` aby dodać failsafe plugin
  - Konfiguracja: `*IT.java` dla integration tests
  - Verify: `./mvnw verify` uruchamia unit + integration tests

- **Awaitility awareness only:**
  - Do not create webhook/async production behavior in Lesson 13.
  - Document Awaitility vs Thread.sleep as a future async testing concept.
  - Add executable Awaitility tests only after an async feature is specified.

### Batch 13E: Advanced Java 25 (EnumSet, EnumMap, Streams) [OPTIONAL]

Zaimplementuj:

- **EnumSet:**
  - Rozszerz istniejące testy aby używały only current stable payment status values, e.g. `EnumSet.of(PaymentStatus.CREATED)`
  - Demonstrate memory efficiency vs HashSet

- **EnumMap:**
  - Rozszerz istniejące testy aby używały `EnumMap<PaymentStatus, Long>` dla status counts
  - Demonstrate performance vs HashMap

- **Streams advanced:**
  - Rozszerz istniejące testy aby używały `Collectors.groupingBy(PaymentOrderResponse::currency)`
  - Rozszerz istniejące testy aby używały `Collectors.partitioningBy(order -> order.amountMinor() > 5000)`

### Batch 13F: Advanced SQL (EXPLAIN, Deadlock) [OPTIONAL]

Zaimplementuj:

- **EXPLAIN ANALYZE:**
  - Napisz SQL query z EXPLAIN ANALYZE dla payment_orders SELECT
  - Analyze query plan (Seq Scan vs Index Scan, cost, actual time)
  - Dokumentacja: jak czytać EXPLAIN output

- **Deadlock detection:**
  - Napisz scenario które może spowodować deadlock (2 transactions locking rows w odwrotnej kolejności)
  - Demonstrate PostgreSQL deadlock detection (ERROR: deadlock detected)
  - Dokumentacja: jak zapobiec deadlockom (consistent lock ordering)

## Scope OUT

- Nowe endpointy production code
- Nowe business logic
- Zmiany w Spring Modulith structure
- Frontend changes
- Performance optimization (tylko test infrastructure)
- Contract testing (Pact/WireMock)
- OpenAPI/Swagger generation

## Implementation Requirements

1. **@WebMvcTest** musi:
   - Testować tylko web layer (controller + exception handler)
   - Używać @MockBean dla service dependencies
   - Używać MockMvc (nie real HTTP)
   - Być szybkie (< 2s startup)
   - Testować request mapping, validation, exception handling

2. **Concurrency testing** musi:
   - Używać @Execution(CONCURRENT) dla parallel execution
   - Zapewnić test data isolation (per-test merchant creation)
   - Nie mieć shared mutable state (static fields, singletons)
   - Demonstrate transaction isolation levels (READ_COMMITTED, REPEATABLE_READ)
   - Demonstrate pessimistic vs optimistic locking

3. **Log assertions** muszą:
   - Używać ListAppender z logback-test.xml
   - Weryfikować correlation ID, payment order ID, merchant ID w logach
   - Być thread-safe (dla parallel tests)
   - Nie interferować z application logs

4. **Flaky test diagnosis** musi:
   - Dokumentować 7 kroków diagnozowania flaky tests
   - Dostarczyć checklist dla common flaky test patterns
   - Dostarczyć examples i fixes
   - Być actionable (nie tylko teoria)

5. **Failure analysis** musi:
   - Kategoryzować failures na 4 typy: app bug, test bug, data bug, env bug
   - Dostarczyć diagnostic steps dla każdego typu
   - Dostarczyć examples jak rozpoznać każdy typ
   - Być practical (nie tylko teoria)

6. **Maven lifecycle** musi:
   - Konfigurować surefire dla unit tests (`*Test.java`)
   - Konfigurować failsafe dla integration tests (`*IT.java`)
   - `./mvnw test` uruchamia tylko unit tests
   - `./mvnw verify` uruchamia unit + integration tests

7. **Awaitility** musi:
    - Używać `Awaitility.await().atMost(...).pollInterval(...).untilAsserted(...)`
    - Nie używać `Thread.sleep()` (anti-pattern)
    - Pozostać awareness-only until an async feature is specified; no webhook/async implementation in this lesson

8. **EnumSet/EnumMap** muszą:
   - Demonstrate memory efficiency vs HashSet/HashMap
   - Być type-safe (compiler sprawdza enum type)
   - Być użyte w real test scenarios (nie tylko examples)

9. **Streams advanced** muszą:
   - Używać `Collectors.groupingBy` dla grouping elements by key
   - Używać `Collectors.partitioningBy` dla splitting elements by predicate
   - Demonstrate downstream collectors (counting, summingLong)
   - Być użyte w real test scenarios (nie tylko examples)

10. **EXPLAIN ANALYZE** musi:
    - Pokazać query plan dla payment_orders SELECT
    - Wyjaśnić Seq Scan vs Index Scan
    - Wyjaśnić cost, actual time, rows
    - Być educational (nie tylko output)

## Required Tests

### @WebMvcTest Tests

- `createPaymentOrderReturns201`
- `createPaymentOrderWithInvalidAmountReturns400`
- `listPaymentOrdersReturns200`
- `getPaymentOrderReturns200`
- `getPaymentOrderNotFoundReturns404`

### Concurrency Tests

- `parallelTestsRunConcurrently` (3-5 tests z @Execution(CONCURRENT))
- `testReadCommittedIsolation`
- `testRepeatableReadIsolation`
- `testPessimisticLocking`
- `testOptimisticLocking`

### Observability Tests

- `createPaymentOrderLogsCorrelationId`
- `createPaymentOrderLogsPaymentOrderId`
- `createPaymentOrderLogsMerchantId`

### Test Reliability Tests [OPTIONAL]

- Awaitility concept note or future-scope example only; no webhook/async executable test yet

### Advanced Java Tests [OPTIONAL]

- `enumSetIsMoreEfficientThanHashSet`
- `enumMapIsMoreEfficientThanHashMap`
- `groupingByGroupsElementsByKey`
- `partitioningBySplitsElementsByPredicate`

## Acceptance Criteria

1. @WebMvcTest z 5+ tests dla PaymentOrderController
2. @MockBean użyte dla mocking service dependencies
3. MockMvc użyte dla HTTP request simulation (nie real HTTP)
4. @Execution(CONCURRENT) użyte dla parallel test execution
5. Transaction isolation levels demonstrated (READ_COMMITTED, REPEATABLE_READ)
6. Pessimistic vs optimistic locking demonstrated
7. Log assertions z ListAppender (3+ tests)
8. FlakyTestDiagnosis.md z 7 kroków methodology
9. FailureAnalysisChecklist.md z 4 kategorie failures
10. Maven failsafe skonfigurowane dla integration tests (optional)
11. Awaitility użyte dla async polling (optional)
12. EnumSet/EnumMap użyte w testach (optional)
13. Collectors.groupingBy/partitioningBy użyte w testach (optional)
14. EXPLAIN ANALYZE query z dokumentacją (optional)
15. Wszystkie istniejące testy nadal przechodzą (no regression)
16. `PaymentModuleTest` nadal przechodzi
17. Vault evidence zaktualizowany

## Verification Commands

```bash
cd apps/backend
./mvnw test                                    # Unit tests (surefire)
./mvnw verify                                  # Unit + integration tests (surefire + failsafe)
./mvnw -Dtest=PaymentOrderControllerTest test  # @WebMvcTest tests
./mvnw -Dtest=PaymentOrderParallelTest test    # Parallel tests
./mvnw -Dtest=PaymentOrderLoggingTest test     # Log assertion tests
./mvnw -Dtest=PaymentModuleTest test           # Modulith architecture test
```

## Evidence Update Required

Po implementacji:

1. Zaktualizuj `Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability.md` z actual files i command results
2. Zaktualizuj `Lesson Evidence Tracker.md` z test evidence
3. Zaktualizuj `Current Lesson.md` i `Current Sprint.md` jeśli Lesson 13 becomes ready
4. Zaktualizuj `Learning Coverage Backlog.md` dla Spring testing, concurrency, observability topics
5. Dodaj interview answer EN
```
