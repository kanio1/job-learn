---
type: lesson
status: planned
area: Payment Quality Engineering Lab — Phase 2
lesson: 11
module: Test Architecture Maturity, Framework Evolution, and Quality Metrics
date: 2026-05-31
tags:
  - business-logic
  - test-architecture
  - framework-evolution
  - quality-metrics
  - lesson-11
  - senior-sdet
---

# Lesson 11 — Test Architecture Maturity, Framework Evolution, and Quality Metrics

> **Evidence link:** `PaymentOrderApi.java` (planned), `PaymentOrderBuilder.java` (planned), `PaymentErrorSpecs.java` (planned)
>
> **Navigation:** [[Lesson 11 - REST Assured Framework Architecture and Test Organization]] | [[Lesson Evidence Tracker]] | [[Current Sprint]]

## 1. Cel Lekcji

Zrozumieć ewolucję test architecture od "raw tests" do "professional framework":
- **Framework evolution path:** raw tests → helpers → specs → API clients → builders
- **Test data lifecycle:** ownership, isolation, cleanup strategies
- **Quality metrics:** test pyramid, coverage beyond code coverage, API coverage matrix
- **Negative-path first methodology:** pisanie testów negatywnych przed pozytywnymi

## 2. Prerequisites

- Lesson 06-10: REST Assured fundamentals, security testing, HTTP hardening.
- Lesson 07: RequestSpecBuilder, ResponseSpecBuilder.
- Lesson 08: Aggregation tests, business flow tests.

## 3. Code Reading Map

| Plik | Reguła biznesowa / decyzja |
|---|---|
| `PaymentApiTestSupport.java` | Obecny stan: factory methods (`createPaymentOrderBody`) |
| `MerchantApiTestSupport.java` | Obecny stan: request helpers (`publicRequest`, `operatorRequest`) |
| `PaymentOrderListApiTestSupport.java` | Obecny stan: RequestSpecBuilder + ResponseSpecBuilder |
| `PaymentOrderAssertions.java` | Obecny stan: custom AbstractAssert dla list response |
| `RestAssuredLoggingConfig.java` | Obecny stan: failure-only logging (brak secret masking) |

## 4. Decision Table — Framework Evolution Path

| Stage | Charakterystyka | Przykład | Kiedy używać |
|---|---|---|---|
| **Raw tests** | Inline `given().when().then()` w każdym teście | `given().port(port).auth().oauth2(token)...` | Prototyp, 1-2 testy |
| **Helpers** | Static methods dla common setup | `MerchantApiTestSupport.operatorRequest(port)` | 5-10 testów, powtarzalny setup |
| **Spec builders** | RequestSpecBuilder, ResponseSpecBuilder | `listRequestSpec(port, token)` | 10-20 testów, common expectations |
| **API clients** | Business-readable wrapper methods | `paymentApi.createOrder(merchantId, token, builder)` | 20+ testów, multi-endpoint scenarios |
| **Builders** | Fluent API dla test data | `aPaymentOrder().withAmount(5000).build()` | Complex payloads, wiele wariantów |
| **Fixtures** | Pre-built test scenarios | `PaymentOrderFixtures.validOrder()` | Common scenarios, Object Mother pattern |

## 5. Decision Table — Test Data Lifecycle

| Strategia | Kiedy używać | Trade-offs | Cleanup |
|---|---|---|---|
| **Per-test creation** | Małe suites (< 50 tests) | Wolne (HTTP calls), ale bezpieczne | Brak (dane pozostają) |
| **@BeforeEach truncation** | Średnie suites (50-100 tests) | Szybkie, ale ryzyko flaky tests | TRUNCATE CASCADE |
| **Schema-per-test** | Duże suites (> 100 tests) | Pełna izolacja, ale overhead | DROP SCHEMA CASCADE |
| **@Sql scripts** | Fixed test data | Szybkie, readable, ale less flexible | @Sql cleanup scripts |
| **Flyway test migrations** | Reference data | Automatyczne, ale globalne | Brak (one-way migrations) |

## 6. Decision Table — Quality Metrics

| Metryka | Co mierzy | Target | Kiedy mierzyć |
|---|---|---|---|
| **Test pyramid** | Unit vs integration vs E2E ratio | 70% unit, 20% integration, 10% E2E | Architecture review |
| **Code coverage** | % code executed by tests | > 80% line coverage | CI/CD pipeline |
| **Branch coverage** | % branches executed by tests | > 70% branch coverage | CI/CD pipeline |
| **API coverage** | % endpoints × behaviors tested | 100% endpoints, 80% behaviors | Sprint review |
| **Mutation testing** | % mutations killed by tests | > 60% mutation score | Quarterly review |
| **Flaky test rate** | % tests that fail intermittently | < 1% flaky rate | Weekly monitoring |
| **Test execution time** | Total time dla full suite | < 10 minutes | CI/CD pipeline |

## 7. Risk Notes (QA Architecture)

### 7.1 Over-Engineering Framework

**Ryzyko:** Dodawanie abstraction layers "just in case" (np. API client dla 3 testów).

**Mitigacja:** YAGNI principle. Dodawaj abstraction tylko gdy:
- Masz 5+ testów z tym samym setup
- Setup zmienia się często (np. authentication mechanism)
- Testy są trudne do czytania przez inline setup

**Weryfikacja:** Czy nowy test jest bardziej czytelny z abstraction niż bez?

### 7.2 Test Data Leakage

**Ryzyko:** Tests interferują ze sobą przez shared data (np. merchant reference collision).

**Mitigacja:**
- Per-test unique identifiers (`UUID.randomUUID()`)
- Test data isolation strategies (Lesson 11 PostgreSQL note)
- Parallel execution safety (Lesson 13)

**Weryfikacja:** Uruchom tests w random order i parallel mode.

### 7.3 Secret Leakage w Logs

**Ryzyko:** Authorization tokens, API keys wyciekają w CI logs.

**Mitigacja:**
- `blacklistHeader("Authorization")` w RestAssuredLoggingConfig
- Maskowanie sensitive data w test reports
- Regular audit CI logs

**Weryfikacja:** Sprawdź CI logs po failed test — czy Authorization header jest widoczny?

### 7.4 Negative-Path First Methodology

**Ryzyko:** Pisanie tylko happy-path tests (brak coverage dla error cases).

**Mitigacja:**
- Pisz testy negatywne PRZED pozytywnymi
- Dla każdego endpoint: 400, 401, 403, 404, 409 tests
- Decision table dla wszystkich error scenarios

**Weryfikacja:** Czy masz więcej tests negatywnych niż pozytywnych? (Target: 60% negative, 40% positive)

## 8. Learning Delta — Co Nowe vs Lessons 06-10

| Temat | Lesson 06-10 | Lesson 11 |
|---|---|---|
| Test architecture | Raw tests + helpers | API clients + builders + specs |
| Test data | Per-test creation | 5 strategies (per-test, truncation, schema-per-test, @Sql, Flyway) |
| Quality metrics | Code coverage | Test pyramid, API coverage, mutation testing, flaky rate |
| Methodology | Ad-hoc test writing | Negative-path first, framework evolution path |

## 9. Pytania

1. Kiedy dodawać API client wrapper zamiast używać raw REST Assured?
2. Jaka jest różnica między Object Mother a Builder pattern?
3. Dlaczego YAGNI jest ważne w test architecture?
4. Jak mierzyć API coverage (nie tylko code coverage)?
5. Dlaczego negative-path first methodology poprawia test quality?
6. Jak wykryć test data leakage między tests?
7. Dlaczego secret masking jest ważne w CI logs?
8. Jaki jest idealny test pyramid ratio dla REST API tests?
9. Kiedy używać mutation testing?
10. Jak zdefiniować "flaky test" i jak go naprawić?

### Odpowiedzi

1. API client wrapper dodaj, gdy wiele testów powtarza ten sam auth, endpointy i request setup. Dla kilku prostych testów raw REST Assured jest wystarczający.
2. Object Mother daje gotowe fixture/scenariusze, często statyczne. Builder daje fluent API do tworzenia wariantów z defaults.
3. YAGNI chroni framework testowy przed abstrakcjami bez realnego użycia. Zbyt wczesne warstwy utrudniają naukę i debugowanie testów.
4. API coverage mierz jako endpointy × metody × statusy × role × edge cases. Code coverage nie mówi, czy kontrakt HTTP i security matrix są pokryte.
5. Negative-path first wymusza myślenie o odrzuceniu błędnych requestów i nadużyć. To zmniejsza ryzyko API, które działa tylko dla happy path.
6. Test data leakage wykryjesz przez losową kolejność, równoległe uruchomienie i powtarzanie suite. Objawem są testy przechodzące pojedynczo, ale failujące razem.
7. Secret masking chroni tokeny i klucze przed wyciekiem w CI logs. Failed tests często logują pełny request, więc maskowanie musi działać domyślnie.
8. Typowy cel to więcej unit/service tests, mniej full integration i najmniej E2E. Dla REST API ważniejsze od sztywnego ratio jest to, aby tanie testy łapały reguły, a drogie tylko przepływy.
9. Mutation testing używaj, gdy test suite jest stabilny i chcesz ocenić siłę oracle, nie tylko coverage. Najlepiej robić to okresowo, bo jest wolniejsze.
10. Flaky test failuje nieregularnie bez zmiany kodu. Naprawa zaczyna się od klasyfikacji przyczyny: timing, shared data, environment albo zły oracle.

## 10. Testy (Awareness)

| Test | Co sprawdza |
|---|---|
| `frameworkEvolutionPathDemonstration` | Porównanie raw test vs API client test |
| `testDataIsolationVerification` | Brak interference między tests |
| `secretMaskingVerification` | Authorization header nie w logs |
| `negativePathFirstExample` | Testy negatywne przed pozytywnymi |

## 11. Powiązane Notatki

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 08 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 10 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [[Senior SDET Competency Coverage Matrix]]
