---
type: prompt
status: ready-after-lesson-11
project: Payment Quality Engineering Lab
lesson: 12
date: 2026-06-04
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

Copy this prompt and give it to Kilo when starting Lesson 12 implementation **after Lesson 11 is complete**.

```text
Jesteś moim zespołem: QA Architect, REST Assured Advanced User, AssertJ Expert, JUnit Specialist, Backend Test Architect i Agent Kodowania.

Pracujemy w repozytorium:

/home/suso/job-learn

## Mission

Zaprojektuj i zaimplementuj Lesson 12: Advanced Assertions, Type-Safe Extraction, and Parameterized Testing.

To nie jest nowa funkcjonalność płatnicza. To jest backend/API test quality slice dla istniejącej platformy PayU-like do nauki Java, REST, HTTP, REST Assured, Keycloak-style JWT roles, headers/body contracts i test architecture.

Główne pytanie:

Jak dobrać właściwy oracle i assertion technique dla istniejącego Payment Order API: wrapper DTO, typed content extraction, GPath, AssertJ, SoftAssertions i parameterized tests?

## Required skills

Użyj skills:

- `payment-quality-lab-orchestrator`
- `junit6-assertj-restassured-testcraft`
- `java-rest-api-testing-effective-java-mentor`
- `test-analysis-design-and-data`
- `rest-api-security-oauth-testing`
- `spring-boot4-spring7-backend-architect`
- `obsidian-learning-os`

Jeśli oceniasz frontend follow-up, użyj też:

- `nuxt-dashboard-zod-pinia-frontend-engineering`

## Read first

Przeczytaj przed zmianami:

- `AGENTS.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/plan.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 11 - REST Assured Framework Architecture and Test Organization.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 12 - Business Logic, Decision Tables, and Risk Notes.md`

Przeczytaj kod:

- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderAssertions.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderResponse.java`
- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/stores/payment-orders.ts`

## Preflight gate

Sprawdź, czy Lesson 11 jest zaimplementowana albo świadomie zastąpiona równoważnym rozwiązaniem.

Expected artifacts:

- `PaymentOrderApi.java` / `MerchantApi.java` or equivalent API clients
- `PaymentOrderBuilder.java` / `MerchantBuilder.java` or equivalent builders
- `PaymentErrorSpecs.java` or equivalent reusable error specs
- `RestAssuredLoggingConfig` with secret masking

If these artifacts do not exist, do not force Lesson 12 implementation. Report that Lesson 11 must be completed first, or implement only a tiny documented preflight cleanup if it is clearly safe.

## Czego NIE powtarzać

Nie tłumacz od nowa:

- basic REST Assured `given().when().then()`
- basic `extract().as(Class)`
- basic `assertThat(...).isEqualTo(...)`
- basic JUnit `@Test`
- Lesson 10 summary HTTP edge and auth matrix details
- Lesson 11 API client/builder/error spec concepts except as prerequisites

## Scope decision

Default: Lesson Extension, no Spec Kit.

Reason:

- no new production endpoint,
- no new payment business behavior,
- no new roles/statuses,
- no frontend production scope by default,
- we improve the precision and maintainability of existing tests.

## Scope IN

### Batch 12A - Test hygiene preflight

Inspect and handle only if safe:

- `PaymentOrderListRestAssuredTest#listFilteredByStatusReturnsOnlyCreatedV2` looks suspicious; characterize it and fix/replace if it is wrong.
- `MyPaymentOrderBusinessFlowRestAssuredTest` and `MyMerchantRestAssuredTest` look like personal learning copies; do not delete blindly. Remove only if clearly obsolete and unreferenced.
- `IdempotencyKeysCopy.java` duplicates `IdempotencyKeys.java`; remove only if clearly obsolete and unreferenced.
- Minor readability smells in `PaymentOrderSummaryAuthorizationMatrixTest` may be fixed only if touched for this lesson.

### Batch 12B - Type-safe extraction choices

Implement tests that teach the real API shape:

- The list endpoint returns `PaymentOrderListResponse`, not a raw list.
- Keep wrapper extraction for the whole response: `extract().as(PaymentOrderListResponse.class)`.
- Demonstrate typed content extraction with `jsonPath().getList("content", PaymentOrderResponse.class)`.
- Use `TypeRef<List<PaymentOrderResponse>>` only if extracting an actual JSON array value, not the whole wrapper response.

Candidate tests:

- `listResponseExtractionPreservesWrapperContract`
- `listContentCanBeExtractedAsTypedOrders`
- `typedContentExtractionDoesNotReturnListOfMaps`

### Batch 12C - GPath vs typed AssertJ

Implement 1-2 tests showing the boundary:

- GPath for raw JSON/path checks.
- AssertJ for typed business assertions after deserialization.
- Use deterministic seed data and explicit sort when asserting first/last/order.

Candidate tests:

- `gpathAndAssertJFilterTheSameCurrencyRows`
- `typedListAssertionsExplainBusinessResultBetterThanRawJsonPaths`

### Batch 12D - Advanced AssertJ

Implement only patterns that improve readability:

- `SoftAssertions` for summary response with multiple independent aggregate facts.
- `usingRecursiveComparison()` for DTO comparison, ignoring generated/technical fields.
- `extracting(...).containsExactlyInAnyOrder(tuple(...))` for expected rows.
- `allSatisfy` / `anySatisfy` for list invariants.
- Extend `PaymentOrderAssertions` only if it removes duplication and creates a reusable domain assertion.

Candidate tests:

- `summaryResponseReportsAllAggregateFactsTogether`
- `listResponseContainsExpectedBusinessRowsRegardlessOfOrder`
- `createdPaymentOrderMatchesExpectedBusinessFields`

### Batch 12E - Parameterized tests

Use `@ParameterizedTest` with isolated data per row.

Recommended:

- `@MethodSource` for create payment order validation cases.
- `@CsvSource` for simple list filter cases such as currency/count.
- `@EnumSource` only for current stable enum values; do not invent `AUTHORIZED`, `CAPTURED`, `REFUNDED`.

Each row must create its own merchant and orders. No shared mutable static fixtures.

Candidate tests:

- `createPaymentOrderValidationCases`
- `listPaymentOrdersByCurrencyCases`
- `listPaymentOrdersByCurrentStatusCases`

## Scope OUT

- Do not add `POST /payments`.
- Do not add payment lifecycle actions: authorize, capture, cancel, refund.
- Do not add new payment statuses just for tests.
- Do not add PSP integration, PSP mocks, Kafka, webhooks or async pipelines.
- Do not add complete OAuth/OIDC integration.
- Do not add complete business dashboard.
- Do not add OpenAPI/Swagger/Pact/WireMock automation in this lesson.
- Do not make `PaymentOrderPerformanceTest` a core deliverable.
- Do not add response time thresholds to normal contract tests.
- Do not modify frontend unless a small consumer-contract fix is directly required and verified.

## Acceptance criteria

1. Lesson 11 preflight is checked and documented in final response.
2. The implementation respects that list endpoint returns `PaymentOrderListResponse`.
3. At least one test demonstrates typed extraction of `content` as `List<PaymentOrderResponse>` without raw `List<Map>` usage.
4. At least one test contrasts GPath with typed AssertJ in a deterministic way.
5. At least one summary/list test uses `SoftAssertions` or another advanced AssertJ feature for a real readability gain.
6. At least one `@ParameterizedTest` uses per-row data ownership.
7. No test relies on accidental row order, fake statuses or shared mutable data.
8. No production payment behavior is added.
9. Lesson 10 HTTP edge and authorization matrix tests still pass.
10. `PaymentModuleTest` still passes.
11. Vault evidence is updated after implementation.

## Verification commands

Run from `apps/backend`:

```bash
./mvnw -Dtest=PaymentOrderListRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest test
./mvnw -Dtest=PaymentOrderRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest,PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

If you create a dedicated class:

```bash
./mvnw -Dtest=PaymentOrderParameterizedTest test
```

Frontend verification only if frontend files changed:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts payment-order-create.spec.ts payment-order-read.spec.ts
```

## Vault evidence update

After implementation, update:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 12 - Business Logic, Decision Tables, and Risk Notes.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`

## Next batch recommendation after Lesson 12

Return a recommendation for one of these:

- `12G - Payment Order List contract and authorization matrix`: pagination boundaries, invalid filters, sort allowlist, 401/403/200 list matrix.
- `12F - Frontend consumer alignment`: dashboard shell for create/detail, typed detail parsing, Pinia action ownership, Playwright route/back-link checks.
- `13A - Spring testing layers and reliability`: MockMvc/WebMvcTest, parallel-safe data, concurrency awareness, surefire/failsafe.

Prefer `12G` if backend list contract risks remain. Prefer `12F` if learner wants frontend/Nuxt/UI consumer depth next. Prefer `13A` after backend and frontend consumer gaps are stable.

## Required final response

Return:

1. What Lesson 11 preflight found.
2. What code/tests changed.
3. Which tests were run and results.
4. What vault evidence was updated.
5. Which next batch is recommended and why.
```
