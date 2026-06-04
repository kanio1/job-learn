---
type: lesson
status: planned
area: Payment Quality Engineering Lab — Phase 2
lesson: 13
module: Test Infrastructure Architecture, Concurrency Safety, and Production Reliability
date: 2026-05-31
tags:
  - business-logic
  - test-infrastructure
  - concurrency-safety
  - production-reliability
  - lesson-13
  - senior-sdet
---

# Lesson 13 — Test Infrastructure Architecture, Concurrency Safety, and Production Reliability

> **Evidence link:** `PaymentOrderControllerTest.java` (planned), `PaymentOrderParallelTest.java` (planned), `PaymentOrderTransactionTest.java` (planned), `PaymentOrderLoggingTest.java` (planned)
>
> **Navigation:** [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]] | [[Lesson Evidence Tracker]] | [[Current Sprint]]

## 1. Cel Lekcji

Zrozumieć test infrastructure architecture:
- **Spring testing layers:** @WebMvcTest vs @SpringBootTest vs @DataJpaTest
- **Concurrency safety:** Parallel execution, transaction isolation, locking, deadlock prevention
- **Observability:** Log assertions, flaky test diagnosis, failure analysis
- **Production reliability:** Maven lifecycle (surefire vs failsafe), Awaitility, test environment management

## 2. Prerequisites

- Lesson 06-12: REST Assured, AssertJ, JUnit 5, Java 25, SQL.
- Lesson 11: Framework architecture, test data strategies.
- Lesson 12: Advanced assertions, parameterized testing.
- Basic Spring Boot testing (Lesson 06).

## 3. Code Reading Map

| Plik | Reguła biznesowa / decyzja |
|---|---|
| `PaymentOrderRestAssuredTest.java` | Obecny stan: @SpringBootTest (full context, real HTTP) |
| `PaymentOrderServiceTest.java` | Obecny stan: @SpringBootTest (full context) |
| `JpaPaymentOrderRepositoryTest.java` | Obecny stan: @DataJpaTest (JPA only) |
| `pom.xml` | Obecny stan: tylko surefire plugin (brak failsafe) |
| `application-test.yml` | Obecny stan: basic test configuration |

## 4. Decision Table — Spring Testing Layers

| Layer | Annotation | Kiedy używać | Trade-offs |
|---|---|---|---|
| **Controller** | @WebMvcTest | Testujesz tylko web layer (request mapping, validation, exception handling) | Szybkie (1-2s), ale brak service/repository |
| **Service** | @SpringBootTest + @MockBean | Testujesz business logic z mocked dependencies | Średnie (3-5s), ale brak real database |
| **Repository** | @DataJpaTest | Testujesz JPA queries, transactions | Szybkie (1-2s), ale brak controller/service |
| **Integration** | @SpringBootTest (full) | Testujesz full stack (controller + service + repository + database) | Wolne (5-10s), ale realistic |
| **E2E** | @SpringBootTest + Testcontainers | Testujesz z real external dependencies (PostgreSQL, Redis, Kafka) | Najwolniejsze (10-20s), ale production-like |

## 5. Decision Table — Concurrency Safety

| Scenario | Approach | Dlaczego |
|---|---|---|
| **Parallel test execution** | @Execution(CONCURRENT) + test data isolation | Szybsze CI, ale wymaga isolation |
| **Transaction isolation** | READ_COMMITTED (default) | Wystarczający dla większości use cases |
| **Rare conflicts** | Optimistic locking (@Version) | Lepsza performance, ale wymaga retry logic |
| **Frequent conflicts** | Pessimistic locking (PESSIMISTIC_WRITE) | Prevents conflicts, ale wolniejsze |
| **Deadlock prevention** | Consistent lock ordering | Zapobiega circular waits |

## 6. Decision Table — Observability

| Approach | Kiedy używać | Trade-offs |
|---|---|---|
| **Log assertions** | Weryfikacja correlation ID, business events | Wymaga ListAppender configuration |
| **Flaky test diagnosis** | Test failuje intermittentnie | Wymaga systematic approach (7 kroków) |
| **Failure analysis** | Test failuje consistently | Kategoryzacja: app bug, test bug, data bug, env bug |
| **Metrics collection** | Performance monitoring, trend analysis | Wymaga Micrometer/Prometheus setup |

## 7. Decision Table — Production Reliability

| Approach | Kiedy używać | Trade-offs |
|---|---|---|
| **Maven surefire** | Unit tests (fast, isolated) | Domyślny plugin, ale tylko unit tests |
| **Maven failsafe** | Integration tests (slow, external dependencies) | Wymaga konfiguracji, ale oddziela unit od integration |
| **Awaitility** | Future async operations after a feature is specified | Awareness only now; do not add webhook/message-queue behavior in this phase |
| **Testcontainers** | Real external dependencies (PostgreSQL, Redis) | Production-like, ale wolniejsze startup |

## 8. Risk Notes (QA Architecture)

### 8.1 Parallel Execution z Shared State

**Ryzyko:** Tests modyfikują static fields lub singletons, powodując race conditions w parallel execution.

**Mitigacja:**
- Brak static mutable fields w test classes
- Per-test data creation (każdy test tworzy własne dane)
- Thread-safe test support classes
- @ResourceLock dla shared resources

**Weryfikacja:** Uruchom tests z @Execution(CONCURRENT) — czy przechodzą?

### 8.2 Transaction Isolation Mismatch

**Ryzyko:** Test oczekuje REPEATABLE_READ, ale production używa READ_COMMITTED.

**Mitigacja:**
- Sprawdź production isolation level (PostgreSQL default: READ_COMMITTED)
- Używaj tego samego isolation level w tests jak w production
- Dokumentuj assumptions o isolation level w test comments

**Weryfikacja:** Czy test isolation level matches production?

### 8.3 Flaky Test Accumulation

**Ryzyko:** Flaky tests nie są naprawiane, accumulatują się, team ignoruje failures.

**Mitigacja:**
- Flaky test quarantine (oznacz @Disabled, napraw w dedicated sprint)
- Flaky test monitoring (weekly report: ile flaky tests, które tests)
- Zero tolerance policy: nowy flaky test = immediate fix lub disable

**Weryfikacja:** Jaki jest flaky test rate w CI? (Target: < 1%)

### 8.4 Test Environment Drift

**Ryzyko:** Test environment różni się od production (np. PostgreSQL version, Java version).

**Mitigacja:**
- Testcontainers z exact versions (np. `postgres:18`, nie `postgres:latest`)
- Docker Compose dla local development (matches CI environment)
- Regular environment audit (czy test env matches production?)

**Weryfikacja:** Porównaj test environment z production environment — czy są identyczne?

## 9. Learning Delta — Co Nowe vs Lessons 06-12

| Temat | Lesson 06-12 | Lesson 13 |
|---|---|---|
| Spring testing | @SpringBootTest (full context) | @WebMvcTest, MockMvc, @MockBean, @SpyBean |
| Concurrency | Sequential execution | @Execution(CONCURRENT), transaction isolation, locking |
| Observability | Brak log assertions | ListAppender, flaky test diagnosis, failure analysis |
| Reliability | Tylko surefire | Surefire + failsafe, Awaitility, environment management |

## 10. Pytania

1. Kiedy używać @WebMvcTest vs @SpringBootTest?
2. Jaka jest różnica między @MockBean a @SpyBean?
3. Dlaczego MockMvc jest szybsze niż real HTTP requests?
4. Jakie są wymagania dla parallel test execution?
5. Jaka jest różnica między READ_COMMITTED a REPEATABLE_READ?
6. Kiedy używać pessimistic vs optimistic locking?
7. Jak wykryć deadlock w PostgreSQL?
8. Jak asercjonować logi w Spring Boot tests?
9. Jakie są 4 kategorie test failures (app bug, test bug, data bug, env bug)?
10. Dlaczego Awaitility jest lepsze niż Thread.sleep?
11. Jaka jest różnica między Maven surefire a failsafe?
12. Jak zarządzać flaky tests (quarantine, monitoring, zero tolerance)?

### Odpowiedzi

1. `@WebMvcTest` wybierz do szybkiego testu controller layer: mapping, validation, exception handler. `@SpringBootTest` wybierz, gdy potrzebujesz pełnego kontekstu i integracji z service/repository/DB.
2. `@MockBean` całkowicie zastępuje bean mockiem. `@SpyBean` używa realnego beana, ale pozwala stubować lub weryfikować wybrane metody.
3. MockMvc działa wewnątrz Spring MVC bez prawdziwego portu i sieci. Dzięki temu testuje web layer szybciej niż real HTTP request.
4. Parallel test execution wymaga niezależnych danych, braku mutable static state i thread-safe test support. Wspólne zasoby muszą być blokowane lub izolowane.
5. `READ_COMMITTED` pozwala zobaczyć nowe commity między odczytami. `REPEATABLE_READ` utrzymuje stabilniejszy widok danych w tej samej transakcji.
6. Optimistic locking jest dobre przy rzadkich konfliktach i wyższej przepustowości. Pessimistic locking jest dobre przy częstych konfliktach, ale zwiększa blokowanie i ryzyko deadlocków.
7. Deadlock wykryjesz przez PostgreSQL exception/logi i analizę kolejności locków. Prewencja to stała kolejność blokowania zasobów.
8. Logi asercjonuj przez test appender i sprawdzaj tylko istotne business eventy lub correlation id. Nie wiąż testu z pełnym formatem log line.
9. App bug to błąd produktu, test bug to błędny test, data bug to problem danych testowych, env bug to środowisko. Klasyfikacja skraca diagnozę failure.
10. Awaitility czeka warunkowo do timeoutu i kończy od razu po spełnieniu warunku. `Thread.sleep` czeka zawsze i nie gwarantuje, że async praca naprawdę się zakończyła.
11. Surefire uruchamia szybkie testy w fazie `test`. Failsafe uruchamia integration tests w fazie `verify`, dzięki czemu lepiej separuje wolniejsze testy.
12. Flaky test najpierw oznacz i monitoruj, ale nie ignoruj stale. Zero tolerance oznacza: nowy flaky test jest naprawiany od razu albo tymczasowo izolowany z jasnym zadaniem naprawy.

## 11. Testy (Awareness)

| Test | Co sprawdza |
|---|---|
| `webMvcTestVsSpringBootTestComparison` | @WebMvcTest vs @SpringBootTest trade-offs |
| `parallelExecutionWithSharedStateDetection` | Race condition detection |
| `transactionIsolationLevelVerification` | READ_COMMITTED vs REPEATABLE_READ |
| `flakyTestQuarantineProcess` | Flaky test management |
| `testEnvironmentDriftDetection` | Environment comparison |

## 12. Powiązane Notatki

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 10 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 11 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 12 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]
- [[Senior SDET Competency Coverage Matrix]]
