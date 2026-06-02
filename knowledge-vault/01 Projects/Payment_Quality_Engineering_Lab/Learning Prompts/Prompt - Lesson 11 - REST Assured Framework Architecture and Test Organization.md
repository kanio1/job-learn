---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 11
date: 2026-05-31
tags:
  - prompt
  - lesson-11
  - rest-assured
  - framework-architecture
  - test-organization
  - senior-sdet
---

# Prompt - Lesson 11 - REST Assured Framework Architecture and Test Organization

Copy this prompt and give it to Kilo when starting Lesson 11 implementation.

```text
Jesteś moim zespołem: Senior REST Assured Architect, Java 25 Expert, Test Framework Designer, i Agent Kodowania.

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
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 11 - REST Assured Framework Architecture and Test Organization.md`

Przeczytaj kod:

- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentApiTestSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderListApiTestSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/RestAssuredLoggingConfig.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderAssertions.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java`

## Skills do użycia

Użyj skills:

- `payment-quality-lab-orchestrator`
- `java-rest-api-testing-effective-java-mentor`
- `junit6-assertj-restassured-testcraft`
- `parallel-test-architecture-and-data-isolation`
- `obsidian-learning-os`

## Czego NIE powtarzać

Nie tłumacz od nowa:

- `given()` / `when()` / `then()` basics
- `extract().as(Class)` typed extraction
- `RequestSpecBuilder` / `ResponseSpecBuilder` basics
- Header assertions (`header("X-Correlation-ID", ...)`)
- Basic AssertJ (`assertThat(...)`, `extracting(...)`)

Użyj tych tematów jako prerequisites.

## Cel Lesson 11

Zaprojektuj i zaimplementuj **REST Assured Framework Architecture** — transformację istniejącego test suite z "working tests" do "professional framework".

Główne pytanie:

Jak przekształcić surowe REST Assured testy (given/when/then z inline setup) w profesjonalny framework z API client wrappers, test data builders, reusable error specs, secret masking i zorganizowaną strukturą testów?

## Scope Decision

Domyślna decyzja: Lesson Extension, no Spec Kit.

Uzasadnienie:

- nie dodajemy nowych endpointów ani business logic,
- refaktoryzujemy wyłącznie infrastrukturę testową,
- production code pozostaje niezmieniony (chyba że testy ujawnią bug),
- to jest "test framework maturity" slice.

## Scope IN

### Batch 11A: API Client Wrapper Pattern

Zaimplementuj:

- `testsupport/PaymentOrderApi.java` — business-readable methods:
  - `createOrder(merchantId, token, builder)` → `PaymentOrderResponse`
  - `getOrder(merchantId, orderId, token)` → `PaymentOrderResponse`
  - `listOrders(merchantId, token)` → `PaymentOrderListResponse`
  - `getSummary(merchantId, token)` → `PaymentOrderSummaryResponse`
- `testsupport/MerchantApi.java` — business-readable methods:
  - `createMerchant(token, builder)` → `MerchantResponse`
  - `getMerchant(merchantId, token)` → `MerchantResponse`
  - `listMerchants(token)` → `MerchantListResponse`
  - `activateMerchant(merchantId, token)` → void
  - `suspendMerchant(merchantId, token)` → void

### Batch 11B: Test Data Builders

Zaimplementuj:

- `testsupport/PaymentOrderBuilder.java` — fluent builder:
  - `aPaymentOrder()` static factory
  - `withAmountMinor(long)`, `withCurrency(String)`, `withClientOrderReference(String)`
  - `build()` → `CreatePaymentOrderRequest`
- `testsupport/MerchantBuilder.java` — fluent builder:
  - `aMerchant()` static factory
  - `withReference(String)`, `withDisplayName(String)`
  - `build()` → `CreateMerchantRequest`

### Batch 11C: Reusable Error Specs + Secret Masking

Zaimplementuj:

- `testsupport/PaymentErrorSpecs.java` — reusable `ResponseSpecification`:
  - `validationError()` → 400 + `error=validation`
  - `forbiddenError()` → 403 + `error=forbidden`
  - `notFoundError()` → 404 + `error=not_found`
  - `conflictError(String errorCode)` → 409 + custom error code
- Rozszerz `RestAssuredLoggingConfig.java`:
  - Dodaj `blacklistHeader("Authorization")`
  - Dodaj `blacklistHeader("Idempotency-Key")`

### Batch 11D: Test Organization (@Nested, @Tag) [OPTIONAL]

Zrefaktoruj:

- `PaymentOrderRestAssuredTest.java` — dodaj @Nested groups:
  - `@Nested class CreateTests { ... }`
  - `@Nested class ReadTests { ... }`
  - `@Nested class IdempotencyTests { ... }`
- Dodaj @Tag labels:
  - `@Tag("contract")` dla contract tests
  - `@Tag("security")` dla security tests
  - `@Tag("business-flow")` dla scenario tests

### Batch 11E: Scenario Flows [OPTIONAL]

Zaimplementuj:

- `rest/PaymentOrderScenarioFlowTest.java` — multi-step tests:
  - `createListAndSummarizeFlow()` — create → list → summary
  - `idempotentCreateThenReadFlow()` — create → replay → read
  - `crossTenantIsolationFlow()` — create as merchant A → read as merchant B → 404

## Scope OUT

- Nowe endpointy production code
- Nowe business logic
- Zmiany w Spring Modulith structure
- Frontend changes
- Performance testing
- Contract testing (Pact/WireMock)
- OpenAPI/Swagger generation

## Implementation Requirements

1. **API client wrappers** muszą:
   - Ukrywać technical setup (port, auth, content type)
   - Eksponować business-readable methods
   - Zwracać typed responses (nie raw `Response`)
   - Obsługiwać zarówno success jak i error cases

2. **Test data builders** muszą:
   - Używać fluent API (`aPaymentOrder().withAmount(1000).build()`)
   - Mieć sensible defaults (amount=1000, currency="PLN", reference="TEST-<uuid>")
   - Zwracać immutable request objects (records)
   - Być type-safe (nie `Map<String, Object>`)

3. **Error specs** muszą:
   - Definiować reusable `ResponseSpecification` dla każdego error code
   - Sprawdzać status code, content type, i `error` field
   - Być używane przez `.spec(PaymentErrorSpecs.validationError())`

4. **Secret masking** musi:
   - Blacklistować `Authorization` header w logach
   - Blacklistować `Idempotency-Key` header w logach
   - Być skonfigurowane globalnie (nie per-test)

5. **Test organization** musi:
   - Używać @Nested dla logicznych grup testów
   - Używać @Tag dla selektywnego uruchamiania
   - Zachować @DisplayName dla czytelności

6. **Scenario flows** muszą:
   - Testować multi-step workflows (create → list → summary)
   - Używać API client wrappers (nie raw REST Assured)
   - Weryfikować end-to-end data consistency

## Required Tests

### API Client Tests

- `paymentOrderApiCreatesOrderSuccessfully`
- `paymentOrderApiListsOrdersSuccessfully`
- `paymentOrderApiGetsSummarySuccessfully`
- `merchantApiCreatesAndActivatesMerchant`

### Builder Tests

- `paymentOrderBuilderCreatesValidRequest`
- `paymentOrderBuilderUsesSensibleDefaults`
- `merchantBuilderCreatesValidRequest`

### Error Specs Tests

- `validationErrorSpecMatches400Response`
- `forbiddenErrorSpecMatches403Response`
- `notFoundErrorSpecMatches404Response`
- `conflictErrorSpecMatches409Response`

### Scenario Flow Tests

- `createListAndSummarizeFlow`
- `idempotentCreateThenReadFlow`
- `crossTenantIsolationFlow`

## Acceptance Criteria

1. API client wrappers zastępują surowe REST Assured chains w co najmniej 3 testach
2. Test data builders zastępują `Map.of(...)` w co najmniej 3 testach
3. Reusable error specs używane w co najmniej 5 testach
4. Secret masking skonfigurowane i zweryfikowane (Authorization nie w logach)
5. Testy zorganizowane z @Nested groups (co najmniej 1 test class)
6. Scenario flow test weryfikuje create → list → summary workflow
7. Wszystkie istniejące testy nadal przechodzą (no regression)
8. `PaymentModuleTest` nadal przechodzi
9. Vault evidence zaktualizowany

## Verification Commands

```bash
cd apps/backend
./mvnw test
./mvnw -Dtest=PaymentOrderScenarioFlowTest test
./mvnw -Dtest="*Test" -Dgroups="security" test
./mvnw -Dtest=PaymentModuleTest test
```

## Evidence Update Required

Po implementacji:

1. Zaktualizuj `Lesson 11 - REST Assured Framework Architecture and Test Organization.md` z actual files i command results
2. Zaktualizuj `Lesson Evidence Tracker.md` z test evidence
3. Zaktualizuj `Current Lesson.md` i `Current Sprint.md` jeśli Lesson 11 becomes ready
4. Zaktualizuj `Learning Coverage Backlog.md` dla REST Assured Framework Architecture topics
5. Dodaj interview answer EN
```
