---
type: lesson
status: planned
area: Payment Quality Engineering Lab — Phase 2
lesson: 12
module: Precision Assertions, Data-Driven Testing, and Contract Verification
date: 2026-05-31
tags:
  - business-logic
  - precision-assertions
  - data-driven-testing
  - contract-verification
  - lesson-12
  - senior-sdet
---

# Lesson 12 — Precision Assertions, Data-Driven Testing, and Contract Verification

> **Evidence link:** `PaymentOrderParameterizedTest.java` (planned), `PaymentOrderPerformanceTest.java` (planned), `src/test/resources/schemas/payment-order-response.json` (planned)
>
> **Navigation:** [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]] | [[Lesson Evidence Tracker]] | [[Current Sprint]]

## 1. Cel Lekcji

Zrozumieć zaawansowane assertion i testing patterns:
- **Precision assertions:** TypeRef<T>, GPath advanced, recursive comparison, soft assertions
- **Data-driven testing:** @ParameterizedTest z @MethodSource, @CsvSource, @EnumSource
- **Contract verification:** JSON Schema validation, OpenAPI compliance
- **Performance awareness:** Response time assertions, baseline establishment

## 2. Prerequisites

- Lesson 06-10: REST Assured fundamentals, HTTP hardening.
- Lesson 11: API clients, builders, error specs.
- Basic AssertJ (Lesson 06-08).
- Basic JUnit 5 (Lesson 06-07).

## 3. Code Reading Map

| Plik | Reguła biznesowa / decyzja |
|---|---|
| `PaymentOrderListRestAssuredTest.java` | Obecny stan: `extract().as(PaymentOrderListResponse.class)` |
| `PaymentOrderSummaryRestAssuredTest.java` | Obecny stan: field-by-field assertions |
| `PaymentOrderSecurityTest.java` | Obecny stan: ręczne @Test methods dla każdego role |
| `src/test/resources/schemas/` | Target: JSON Schema files dla contract validation |

## 4. Decision Table — Assertion Strategy

| Scenario | Approach | Dlaczego |
|---|---|---|
| **Single object response** | `extract().as(Class)` | Proste, type-safe |
| **List response** | `extract().as(new TypeRef<List<T>>(){})` | Zachowuje generic type |
| **Nested list** | `extract().as(WrapperDTO.class).content()` | Wrapper class ma typed list |
| **Complex object comparison** | `usingRecursiveComparison()` | Concise, less error-prone |
| **Multiple assertions** | `SoftAssertions.assertAll()` | All failures reported |
| **Collection filtering** | GPath `findAll { condition }` | Powerful inline filtering |
| **Deep field search** | GPath `..` (deep scan) | Recursive field search |
| **Custom assertions** | Custom `AbstractAssert` | Reusable, domain-specific |

## 5. Decision Table — Data-Driven Testing

| Approach | Kiedy używać | Trade-offs |
|---|---|---|
| **@MethodSource** | Complex test data (objects, builders) | Flexible, ale więcej boilerplate |
| **@CsvSource** | Simple test data (primitives, strings) | Concise, ale limited types |
| **@EnumSource** | Enum-based test data | Type-safe, ale tylko enums |
| **@RepeatedTest** | Identical test logic, multiple executions | Simple, ale no data variation |
| **DynamicTest / @TestFactory** | Runtime-generated tests | Maximum flexibility, ale complex |

## 6. Decision Table — Contract Verification

| Approach | Kiedy używać | Trade-offs |
|---|---|---|
| **Field-by-field assertions** | Simple responses, few fields | Explicit, ale verbose |
| **JSON Schema validation** | Complex responses, contract tests | Comprehensive, ale wolniejsze |
| **OpenAPI compliance** | Public APIs, documentation | Standard, ale tooling overhead |
| **Consumer-driven contracts (Pact)** | Multiple consumers, microservices | Rigorous, ale complex setup |

## 7. Risk Notes (QA Architecture)

### 7.1 Over-Assertion

**Ryzyko:** Test sprawdza zbyt wiele szczegółów (np. exact timestamps, UUIDs).

**Mitigacja:**
- Używaj `ignoringFields("createdAt", "updatedAt")` w recursive comparison
- Używaj `matches(Predicate)` dla complex conditions
- Asercjonuj business-relevant fields, nie technical metadata

**Weryfikacja:** Czy test failuje gdy zmienisz non-business field (np. timestamp format)?

### 7.2 Flaky Parameterized Tests

**Ryzyko:** @ParameterizedTest z shared state powoduje interference między iterations.

**Mitigacja:**
- Każda iteration tworzy własne test data (per-iteration merchant)
- Brak static fields modyfikowanych przez iterations
- Używaj `@Execution(CONCURRENT)` aby wykryć interference

**Weryfikacja:** Uruchom @ParameterizedTest 10 razy — czy zawsze przechodzi?

### 7.3 JSON Schema Maintenance Burden

**Ryzyko:** JSON Schema files stają się outdated gdy API się zmienia.

**Mitigacja:**
- Generuj JSON Schema z OpenAPI spec (jeśli masz)
- Używaj JSON Schema validation w contract tests (nie w każdym teście)
- Regular review: czy schema matches actual API response?

**Weryfikacja:** Zmień API response shape — czy JSON Schema test failuje?

### 7.4 Performance Test Flakiness

**Ryzyko:** Response time assertions failują w CI przez shared resources, network latency.

**Mitigacja:**
- Używaj reasonable thresholds (500ms-2s, nie 50ms)
- Oznacz performance tests z `@Tag("performance")` — wyłączaj w CI
- Uruchamiaj performance tests w dedicated environment (nie shared CI)

**Weryfikacja:** Uruchom performance test 100 razy — jaki jest flaky rate?

## 8. Learning Delta — Co Nowe vs Lessons 06-11

| Temat | Lesson 06-11 | Lesson 12 |
|---|---|---|
| Assertions | Field-by-field, basic AssertJ | TypeRef<T>, GPath advanced, recursive comparison, soft assertions |
| Test data | Inline w testach | @ParameterizedTest z @MethodSource, @CsvSource, @EnumSource |
| Contract verification | Field-by-field assertions | JSON Schema validation, OpenAPI compliance |
| Performance | Brak assertions | Response time assertions, baseline establishment |

## 9. Pytania

1. Kiedy używać TypeRef<T> vs extract().as(WrapperDTO.class)?
2. Jaka jest różnica między GPath findAll a AssertJ filteredOn?
3. Dlaczego usingRecursiveComparison jest lepsze niż field-by-field assertions?
4. Kiedy używać SoftAssertions vs regular assertions?
5. Jaka jest różnica między @MethodSource a @CsvSource?
6. Kiedy używać JSON Schema validation vs field-by-field assertions?
7. Dlaczego performance tests mogą być flaky w CI?
8. Jak ustalić reasonable response time threshold?
9. Kiedy używać @Tag("performance") aby wyłączać tests?
10. Jak generować JSON Schema z OpenAPI spec?

## 10. Testy (Awareness)

| Test | Co sprawdza |
|---|---|
| `typeRefVsWrapperDtoComparison` | TypeRef<T> vs extract().as(WrapperDTO) |
| `gpathFindAllVsAssertJFilteredOn` | GPath findAll vs AssertJ filteredOn |
| `recursiveComparisonVsFieldByField` | Recursive comparison vs field-by-field |
| `parameterizedTestWithMethodSource` | @MethodSource data-driven testing |
| `jsonSchemaValidationExample` | JSON Schema contract verification |
| `responseTimeBaselineEstablishment` | Response time threshold determination |

## 11. Powiązane Notatki

- [[Lesson 08 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 10 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 11 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Senior SDET Competency Coverage Matrix]]
