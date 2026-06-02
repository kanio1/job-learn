---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 12
date: 2026-05-31
tags:
  - prompt
  - lesson-12
  - assertj
  - rest-assured
  - junit
  - parameterized-tests
  - senior-sdet
---

# Prompt - Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing

Copy this prompt and give it to Kilo when starting Lesson 12 implementation.

```text
Jesteś moim zespołem: AssertJ Expert, REST Assured Advanced User, JUnit 5 Specialist, i Agent Kodowania.

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
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing.md`

Przeczytaj kod:

- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderAssertions.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`

## Skills do użycia

Użyj skills:

- `payment-quality-lab-orchestrator`
- `java-rest-api-testing-effective-java-mentor`
- `junit6-assertj-restassured-testcraft`
- `test-analysis-design-and-data`
- `obsidian-learning-os`

## Czego NIE powtarzać

Nie tłumacz od nowa:

- Basic AssertJ (`assertThat(...)`, `isEqualTo(...)`, `extracting(...)`)
- Basic REST Assured (`given().when().then()`, `extract().as(Class)`)
- Basic JUnit (`@Test`, `@DisplayName`)
- `RequestSpecBuilder` / `ResponseSpecBuilder` (Lesson 07, 11)

Użyj tych tematów jako prerequisites.

## Cel Lesson 12

Zaprojektuj i zaimplementuj **Advanced Assertions, Type-Safe Extraction, and Parameterized Testing** — transformację testów z "basic assertions" do "precision assertions".

Główne pytanie:

Jak przekształcić testy z prostych field-by-field assertions w profesjonalne testy z TypeRef<T>, GPath advanced, recursive comparison, soft assertions i parameterized testing?

## Scope Decision

Domyślna decyzja: Lesson Extension, no Spec Kit.

Uzasadnienie:

- nie dodajemy nowych endpointów ani business logic,
- rozszerzamy istniejące testy o zaawansowane assertion patterns,
- production code pozostaje niezmieniony,
- to jest "assertion precision" slice.

## Scope IN

### Batch 12A: TypeRef<T> + GPath Advanced

Zaimplementuj:

- **TypeRef<T> dla list extraction:**
  - Rozszerz `PaymentOrderListRestAssuredTest` aby używał `extract().as(new TypeRef<List<PaymentOrderResponse>>(){})`
  - Porównaj z obecnym `extract().as(PaymentOrderListResponse.class).content()`
  - Dodaj test: `listOrdersWithTypeRefReturnsTypedList`

- **GPath deep scan (`..`):**
  - Dodaj test: `allPaymentOrdersHavePositiveAmount` używający `body("content..amountMinor", everyItem(greaterThan(0)))`
  - Dodaj test: `allPaymentOrdersHaveValidCurrency` używający `body("content..currency", hasItems("PLN", "EUR", "USD"))`

- **GPath findAll:**
  - Dodaj test: `filterPaymentOrdersByCurrency` używający `body("content.findAll { it.currency == 'PLN' }.size()", greaterThan(0))`
  - Dodaj test: `filterPaymentOrdersByAmount` używający `body("content.findAll { it.amountMinor > 5000 }.currency", hasItems(...))`

- **GPath array indexing:**
  - Dodaj test: `firstPaymentOrderHasExpectedCurrency` używający `body("content[0].currency", equalTo("PLN"))`
  - Dodaj test: `lastPaymentOrderHasExpectedCurrency` używający `body("content[-1].currency", equalTo("USD"))`

### Batch 12B: Response Time Assertions [OPTIONAL]

Zaimplementuj:

- **Nowy plik:** `rest/PaymentOrderPerformanceTest.java`
- **Testy:**
  - `summaryEndpointRespondsWithin500ms` — `time(lessThan(500L))`
  - `listEndpointRespondsWithin1Second` — `time(lessThan(1L), TimeUnit.SECONDS)`
  - `createEndpointRespondsWithin500ms` — `time(lessThan(500L))`
- **Uwaga:** Performance tests mogą być flaky w CI. Rozważ `@Tag("performance")` aby móc je wyłączać.

### Batch 12C: AssertJ Advanced

Zaimplementuj:

- **usingRecursiveComparison:**
  - Rozszerz `PaymentOrderRestAssuredTest` aby używał `usingRecursiveComparison().ignoringFields("createdAt", "updatedAt")`
  - Dodaj test: `createPaymentOrderMatchesExpectedStructure`

- **SoftAssertions:**
  - Rozszerz `PaymentOrderSummaryRestAssuredTest` aby używał `SoftAssertions.assertAll()`
  - Dodaj test: `summaryResponseHasAllExpectedFields` z 5+ assertions w SoftAssertions

- **asInstanceOf:**
  - Dodaj test używający `assertThat(obj).asInstanceOf(InstanceOfAssertFactories.LIST)`
  - Pokaż type-safe casting

- **Custom AbstractAssert dla error response:**
  - Rozszerz `PaymentOrderAssertions` o `PaymentErrorResponseAssert`
  - Dodaj metody: `hasErrorCode(String)`, `hasMessageContaining(String)`, `hasCorrelationId()`

### Batch 12D: @ParameterizedTest

Zaimplementuj:

- **Nowy plik:** `rest/PaymentOrderParameterizedTest.java`

- **@MethodSource:**
  - Dodaj test: `createPaymentOrderValidation` z `@MethodSource("validationTestCases")`
  - Test data: negative amount, zero amount, invalid currency, blank reference
  - Każda iteracja sprawdza `validationError()` spec i `message` contains expected error

- **@CsvSource:**
  - Dodaj test: `listPaymentOrdersByCurrency` z `@CsvSource({"PLN, 5", "EUR, 3", "USD, 2"})`
  - Każda iteracja filtruje po currency i sprawdza count

- **@EnumSource:**
  - Dodaj test: `listPaymentOrdersByStatus` z `@EnumSource(PaymentStatus.class)`
  - Każda iteracja filtruje po status i sprawdza wszystkie orders mają ten status

- **@RepeatedTest:**
  - Dodaj test: `createPaymentOrderIdempotency` z `@RepeatedTest(3)`
  - Pokaż że idempotent create zwraca ten sam paymentOrderId

### Batch 12E: Generics + Pattern Matching + Text Blocks [OPTIONAL]

Zaimplementuj:

- **Generics (bounded wildcards):**
  - Dodaj helper method: `<T extends PaymentOrderResponse> void assertAllCreated(List<T> orders)`
  - Użyj w teście

- **Pattern matching instanceof:**
  - Zrefaktoruj istniejący kod aby używał `if (obj instanceof PaymentOrderResponse order)` zamiast explicit casting

- **Text blocks:**
  - Dodaj `testsupport/PaymentOrderJsonFixtures.java` z text blocks dla JSON fixtures
  - Dodaj test używający text block jako request body

## Scope OUT

- Nowe endpointy production code
- Nowe business logic
- Zmiany w Spring Modulith structure
- Frontend changes
- Contract testing (Pact/WireMock)
- OpenAPI/Swagger generation
- Performance optimization (tylko assertions)

## Implementation Requirements

1. **TypeRef<T>** musi:
   - Używać anonymous class: `new TypeRef<List<PaymentOrderResponse>>(){}`
   - Zwracać typed list (nie `List<Map>`)
   - Być porównywalne z obecnym `extract().as(PaymentOrderListResponse.class).content()`

2. **GPath advanced** musi:
   - Używać `..` dla deep scan (recursive field search)
   - Używać `findAll { condition }` dla collection filtering
   - Używać `[0]`, `[-1]`, `[0..2]` dla array indexing
   - Być czytelne (nie over-complicated)

3. **Response time assertions** muszą:
   - Używać `.time(lessThan(500L))` lub `.time(lessThan(1L), TimeUnit.SECONDS)`
   - Być oznaczone `@Tag("performance")` aby móc wyłączać w CI
   - Mieć reasonable thresholds (500ms dla single endpoint, 1s dla list)

4. **AssertJ advanced** musi:
   - `usingRecursiveComparison` z `ignoringFields("createdAt", "updatedAt")`
   - `SoftAssertions` z `assertAll()` dla 5+ assertions
   - `asInstanceOf` dla type-safe casting
   - Custom `AbstractAssert` dla `PaymentErrorResponse`

5. **@ParameterizedTest** musi:
   - `@MethodSource` z `Stream<Arguments>` dla validation test cases
   - `@CsvSource` dla currency filtering
   - `@EnumSource` dla status filtering
   - `@RepeatedTest` dla idempotency verification
   - Czytelne `name` parameter (np. `"{0}: {1} → {2}"`)

6. **Generics + pattern matching + text blocks** muszą:
   - Używać bounded wildcards zgodnie z PECS (Producer Extends, Consumer Super)
   - Używać pattern matching instanceof (JDK 16+)
   - Używać text blocks dla multi-line JSON fixtures

## Required Tests

### TypeRef<T> Tests

- `listOrdersWithTypeRefReturnsTypedList`
- `listOrdersWithTypeRefAllowsDirectIteration`

### GPath Advanced Tests

- `allPaymentOrdersHavePositiveAmount` (deep scan)
- `allPaymentOrdersHaveValidCurrency` (deep scan)
- `filterPaymentOrdersByCurrency` (findAll)
- `filterPaymentOrdersByAmount` (findAll)
- `firstPaymentOrderHasExpectedCurrency` (array indexing)
- `lastPaymentOrderHasExpectedCurrency` (array indexing)

### Response Time Tests [OPTIONAL]

- `summaryEndpointRespondsWithin500ms`
- `listEndpointRespondsWithin1Second`
- `createEndpointRespondsWithin500ms`

### AssertJ Advanced Tests

- `createPaymentOrderMatchesExpectedStructure` (usingRecursiveComparison)
- `summaryResponseHasAllExpectedFields` (SoftAssertions)
- `errorResponseHasExpectedStructure` (custom AbstractAssert)

### @ParameterizedTest Tests

- `createPaymentOrderValidation` (@MethodSource)
- `listPaymentOrdersByCurrency` (@CsvSource)
- `listPaymentOrdersByStatus` (@EnumSource)
- `createPaymentOrderIdempotency` (@RepeatedTest)

## Acceptance Criteria

1. TypeRef<T> użyte w co najmniej 2 testach
2. GPath deep scan (`..`) użyte w co najmniej 2 testach
3. GPath findAll użyte w co najmniej 2 testach
4. GPath array indexing użyte w co najmniej 2 testach
5. Response time assertions użyte w co najmniej 3 testach (optional)
6. usingRecursiveComparison użyte w co najmniej 1 teście
7. SoftAssertions użyte w co najmniej 1 teście z 5+ assertions
8. Custom AbstractAssert dla error response zaimplementowane
9. @ParameterizedTest z @MethodSource użyte w co najmniej 1 teście
10. @ParameterizedTest z @CsvSource użyte w co najmniej 1 teście
11. @ParameterizedTest z @EnumSource użyte w co najmniej 1 teście
12. @RepeatedTest użyte w co najmniej 1 teście (optional)
13. Wszystkie istniejące testy nadal przechodzą (no regression)
14. `PaymentModuleTest` nadal przechodzi
15. Vault evidence zaktualizowany

## Verification Commands

```bash
cd apps/backend
./mvnw test
./mvnw -Dtest=PaymentOrderParameterizedTest test
./mvnw -Dtest=PaymentOrderPerformanceTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -Dtest="*Test" -Dgroups="performance" test  # Run only performance tests
./mvnw -Dtest="*Test" -DexcludedGroups="performance" test  # Run all except performance
```

## Evidence Update Required

Po implementacji:

1. Zaktualizuj `Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing.md` z actual files i command results
2. Zaktualizuj `Lesson Evidence Tracker.md` z test evidence
3. Zaktualizuj `Current Lesson.md` i `Current Sprint.md` jeśli Lesson 12 becomes ready
4. Zaktualizuj `Learning Coverage Backlog.md` dla AssertJ advanced, TypeRef<T>, @ParameterizedTest topics
5. Dodaj interview answer EN
```
