---
type: tracker
status: active
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-31
tags:
  - lesson-evidence
  - learning-delta
  - sdet
  - qa-architecture
---

# Lesson Evidence Tracker

Cel: każda lekcja lub sprint ma mieć dowód, że temat został przerobiony w kodzie, testach, analizie albo świadomie odłożony.

> **Navigation:** [[START HERE - Learning Dashboard]] | [[Learning Progress Board]] | [[Home]]
>
> **Current Lesson:** Lesson 10 (PLANNED - REST/HTTP contract hardening and authorization matrix)

## What NOT To Touch Yet

These are explicitly deferred or out of scope. Do NOT study, implement, or research them now.

| Topic | Status | When Allowed |
|---|---|---|
| Payment lifecycle (authorize/capture/cancel) | DEFERRED | Spec Kit 004+ |
| PSP integration | DEFERRED | Spec Kit 005+ |
| Kafka, webhooks, event pipeline | DEFERRED | Future async sprint after REST/HTTP hardening |
| GraphQL, gRPC | DEFERRED | Sprint 13+ |
| Performance/load testing | DEFERRED | Sprint 13b |
| Observability beyond correlation ID | DEFERRED | Sprint 12b |
| JSON Schema / OpenAPI validation | DEFERRED | Future contract-doc readiness after Lesson 10 |
| Contract testing (Pact/WireMock) | DEFERRED | Future async/contract testing sprint |
| Complete OAuth/OIDC | GUARDRAIL | Phase 0 — never in current scope |
| Complete business dashboards | GUARDRAIL | Phase 0 — never in current scope |
| `If-Match` / `412` / optimistic concurrency | DEFERRED | Spec Kit 004+ (when lifecycle actions exist) |
| RLS (Row-Level Security) | DEFERRED | Sprint 9 extension |

## Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix

**Data:** 2026-06-02
**Status:** Ready - implementation complete, 41 tests pass (7 HTTP edge + 12 auth matrix + 22 regression)

### Prompt
- `../Learning Prompts/Prompt - Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix.md`

### Business capability
No new payment business capability. Lesson 10 is a backend/API test-hardening slice over existing Payment Order summary/list/read contracts.

### Learning delta
- HTTP content negotiation and `Accept` behavior.
- Unsupported method semantics for existing REST resources.
- Malformed UUID path variable and stable `400 validation` envelope.
- Route ambiguity guardrail for `/payment-orders/summary` vs `/payment-orders/{paymentOrderId}`.
- Conditional header discipline: summary intentionally has no `ETag` and should not imply cache semantics.
- Parameterized JUnit/REST Assured authorization matrix.
- BOLA vs BFLA distinction for merchant-scoped summary/report endpoints.
- REST Assured protocol/error assertions beyond happy-path JSON body checks.

### Production code evidence
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java` — added `merchantPaymentReaderTokenWithoutMerchantIdClaim()` for authorization matrix row 8

### Test code evidence
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java` — 7 tests (Batch 10A)
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java` — 12 parameterized tests (Batch 10B)

### Spec Kit artifacts
- `specs/007-rest-http-contract-hardening-authorization-matrix/spec.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/plan.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/tasks.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/research.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/data-model.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/contracts/summary-http-edge-api.md`

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix.md`
- `02 Phase 2 - Payment Orders/Lesson 10 - Business Logic, Decision Tables, and Risk Notes.md`

### Descriptive lesson materials (2026-05-31)

Created 5 focused descriptive lessons in the Technical Learning vault, each covering a different aspect of Lesson 10:

| Area | File |
|---|---|
| Java 25 | `02 Areas/Technical Learning/Java 25 For SDET/Lesson 10 - Parameterized Tests, Authorization Matrix, and Test Data Builders.md` |
| REST testing | `02 Areas/Technical Learning/JUnit REST Assured/Lesson 10 - HTTP Edge Contract, Route Guardrails, and Matrix Tests.md` |
| HTTP/API | `02 Areas/Technical Learning/REST API From Zero/Lesson 10 - HTTP Semantics, Content Negotiation, and Error Contract Hardening.md` |
| SQL/PostgreSQL | `02 Areas/Technical Learning/PostgreSQL and SQL From Zero/Lesson 10 - Aggregation Diagnostics, EXPLAIN, and DB Oracle Practice.md` |
| Business logic | `02 Phase 2 - Payment Orders/Lesson 10 - Business Logic, Decision Tables, and Risk Notes.md` |

Each lesson follows the 11-section template (goal → prerequisites → code map → concepts → walkthrough → delta vs L06-L09 → mistakes → exercises → questions → tests → next links), with direct references to real production and test files from Lessons 06-09 and planned Lesson 10 tests.

### Commands run
- `./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test` — 7/7 pass
- `./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test` — 12/12 pass
- `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` — 20/20 pass
- `./mvnw -Dtest=PaymentModuleTest test` — 2/2 pass
- `./mvnw -DskipTests package` — BUILD SUCCESS

### Guardrails
- Do not add authorize/capture/cancel lifecycle actions.
- Do not add PSP integration or PSP mock flows.
- Do not add Kafka/webhooks.
- Do not add new payment statuses only to make tests more interesting.
- Do not add Pact/WireMock/OpenAPI automation in this slice.
- Do not change frontend unless a backend contract fix requires consumer alignment.

### Open risks
- ~~Current Spring MVC defaults for unsupported `Accept` or unsupported methods must be characterized before locking assertions.~~ RESOLVED: `Accept: text/xml` → 406, unsupported methods → 405, `If-None-Match` → ignored.
- Security tests should avoid becoming an unreadable matrix DSL. — RESOLVED: parameterized test with clear display names and BOLA/BFLA labels.
- `Learning Progress Board` and `Senior SDET Competency Coverage Matrix` were stale before Lesson 10 planning and need continued cleanup after implementation evidence exists.

### Interview answer EN
> Lesson 10 hardened the existing Payment Order Summary REST API without adding new business functionality. I implemented HTTP edge contract tests covering route collision, malformed UUID validation, unsupported methods (405), content negotiation (406), and conditional header discipline (If-None-Match ignored). I also created a parameterized authorization matrix with 12 test cases explicitly labeling BOLA (cross-tenant access) and BFLA (wrong role) scenarios. This demonstrates senior-level thinking: testing protocol behavior and security policy, not just happy-path JSON assertions.

### Next lesson/sprint handoff
After Lesson 10, choose either DB oracle/EXPLAIN deep dive (Batch 10C optional) or contract documentation/OpenAPI readiness (Lesson 14). Do not start payment lifecycle until the project guardrails explicitly allow it.

## Lesson 11 - REST Assured Framework Architecture and Test Organization

**Data:** 2026-05-31
**Status:** Planned - lesson note and implementation prompt ready; code/test implementation not started

### Prompt
- `../Learning Prompts/Prompt - Lesson 11 - REST Assured Framework Architecture and Test Organization.md`

### Business capability
No new payment business capability. Lesson 11 is a framework maturity slice — transforms existing test suite from "working tests" to "professional framework".

### Learning delta
- API client wrapper pattern (business-readable methods zamiast raw REST Assured chains).
- Test data builders (fluent API zamiast `Map.of(...)`).
- Reusable error specs (`ResponseSpecification` dla error contracts).
- Secret masking (`blacklistHeader("Authorization")` w logach).
- Test organization (@Nested groups, @Tag labels).
- Scenario flows (multi-step tests: create → list → summary).
- Java 25: sealed interface dla test data hierarchies, Map.copyOf/List.copyOf, Comparator.comparing/thenComparing.

### Production code evidence expected
- No production code expected. Pure test infrastructure refactoring.

### Test code evidence expected
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderApi.java` — API client wrapper.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApi.java` — API client wrapper.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderBuilder.java` — test data builder.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantBuilder.java` — test data builder.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentErrorSpecs.java` — reusable error specs.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/RestAssuredLoggingConfig.java` — secret masking extension.
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderScenarioFlowTest.java` — multi-step scenario tests.
- Refactored existing tests with @Nested, @Tag.

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 11 - REST Assured Framework Architecture and Test Organization.md`
- `02 Phase 2 - Payment Orders/Lesson 11 - Business Logic, Decision Tables, and Risk Notes.md`

### Descriptive lesson materials (2026-05-31)

| Area | File |
|---|---|
| Java 25 | `02 Areas/Technical Learning/Java 25 For SDET/Lesson 11 - Sealed Types, Defensive Copies, and Comparators.md` |
| REST testing | `02 Areas/Technical Learning/JUnit REST Assured/Lesson 11 - API Clients, Builders, Error Specs, and Filters.md` |
| HTTP/API | `02 Areas/Technical Learning/REST API From Zero/Lesson 11 - CORS, Caching Headers, and API Versioning Awareness.md` |
| SQL/PostgreSQL | `02 Areas/Technical Learning/PostgreSQL and SQL From Zero/Lesson 11 - Test Data Isolation Strategies and Flyway Test Migrations.md` |
| Business logic | `02 Phase 2 - Payment Orders/Lesson 11 - Business Logic, Decision Tables, and Risk Notes.md` |

### Commands to run after implementation
- `cd apps/backend && ./mvnw test`
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderScenarioFlowTest test`
- `cd apps/backend && ./mvnw -Dtest="*Test" -Dgroups="security" test`
- `cd apps/backend && ./mvnw -Dtest=PaymentModuleTest test`

### Guardrails
- No new production code unless tests expose a real bug.
- No new endpoints or business logic.
- No frontend changes.
- No Pact/WireMock/OpenAPI automation.

### Interview answer EN
Draft:
> Lesson 11 transforms the test suite from "working tests" to "professional framework". I introduced API client wrappers that replace raw REST Assured chains with business-readable methods, test data builders using the builder pattern, reusable ResponseSpecification instances for error contracts, secret masking for Authorization headers, and organized tests with @Nested groups and @Tag labels. This is what separates a junior test writer from a senior SDET: not just writing tests, but designing a maintainable, readable, and secure test framework.

### Next lesson/sprint handoff
After Lesson 11, proceed to Lesson 12 (Advanced Assertions & Parameterized Testing).

## Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing

**Data:** 2026-05-31
**Status:** Planned - lesson note and implementation prompt ready; code/test implementation not started

### Prompt
- `../Learning Prompts/Prompt - Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing.md`

### Business capability
No new payment business capability. Lesson 12 is a precision assertions slice — transforms tests from "basic assertions" to "precision assertions".

### Learning delta
- TypeRef<T> dla generic list extraction (`List<PaymentOrderResponse>`).
- GPath advanced (deep scan `..`, `findAll`, array indexing).
- Response time assertions (`.time()`, `.timeIn()`).
- JSON Schema validation (`matchesJsonSchemaInClasspath()`).
- usingRecursiveComparison z ignoringFields, comparingOnlyFields.
- SoftAssertions z assertAll().
- asInstanceOf dla type-safe casting.
- @ParameterizedTest z @MethodSource, @CsvSource, @EnumSource.
- @RepeatedTest dla repeated execution.
- DynamicTest / @TestFactory dla dynamic test generation.
- Java 25: Generics (bounded wildcards, PECS), pattern matching instanceof, text blocks.

### Production code evidence expected
- No production code expected. Pure test enhancement.

### Test code evidence expected
- Extended `PaymentOrderListRestAssuredTest.java` with TypeRef<T>.
- New `PaymentOrderPerformanceTest.java` with response time assertions.
- Extended `PaymentOrderAssertions.java` with recursive comparison, soft assertions.
- New `PaymentOrderParameterizedTest.java` with @ParameterizedTest.
- New `PaymentOrderJsonFixtures.java` with text blocks.

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing.md`
- `02 Phase 2 - Payment Orders/Lesson 12 - Business Logic, Decision Tables, and Risk Notes.md`

### Descriptive lesson materials (2026-05-31)

| Area | File |
|---|---|
| Java 25 | `02 Areas/Technical Learning/Java 25 For SDET/Lesson 12 - Generics, Pattern Matching, and Text Blocks.md` |
| REST testing | `02 Areas/Technical Learning/JUnit REST Assured/Lesson 12 - TypeRef, GPath Advanced, Response Time, and JSON Schema.md` |
| HTTP/API | `02 Areas/Technical Learning/REST API From Zero/Lesson 12 - HATEOAS, Content Negotiation Deep Dive, and Rate Limiting.md` |
| SQL/PostgreSQL | `02 Areas/Technical Learning/PostgreSQL and SQL From Zero/Lesson 12 - CTE and Window Functions.md` |
| Business logic | `02 Phase 2 - Payment Orders/Lesson 12 - Business Logic, Decision Tables, and Risk Notes.md` |

### Commands to run after implementation
- `cd apps/backend && ./mvnw test`
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderParameterizedTest test`
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderPerformanceTest test`
- `cd apps/backend && ./mvnw -Dtest=PaymentModuleTest test`

### Guardrails
- No new production code.
- No new endpoints or business logic.
- No frontend changes.
- Performance tests marked with @Tag("performance") for optional execution.

### Interview answer EN
Draft:
> Lesson 12 introduces precision assertions and data-driven testing. I used TypeRef<T> for type-safe generic list extraction, GPath advanced features like deep scan and findAll for complex JSON navigation, response time assertions for performance baselines, and JSON Schema validation for contract verification. I also implemented @ParameterizedTest with @MethodSource, @CsvSource, and @EnumSource for data-driven testing. This is senior-level testing: not just checking status codes, but verifying exact response structure, performance characteristics, and testing across multiple data sets efficiently.

### Next lesson/sprint handoff
After Lesson 12, proceed to Lesson 13 (Spring Testing Layers, Concurrency, Observability, and Test Reliability).

## Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability

**Data:** 2026-05-31
**Status:** Planned - lesson note and implementation prompt ready; code/test implementation not started

### Prompt
- `../Learning Prompts/Prompt - Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability.md`

### Business capability
No new payment business capability. Lesson 13 is a test infrastructure architect slice — designs not just tests, but the entire test ecosystem.

### Learning delta
- @WebMvcTest / MockMvc dla focused controller tests.
- @MockBean / @SpyBean dla mocking dependencies.
- Spring profiles dla test-specific configuration.
- @Execution(CONCURRENT) dla parallel test execution.
- Transaction isolation levels (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE).
- Pessimistic vs optimistic locking.
- Deadlock detection i prevention.
- Log assertions z ListAppender.
- Flaky test diagnosis methodology.
- Failure analysis (app bug vs test bug vs data bug vs env bug).
- Awaitility dla async polling.
- Maven surefire vs failsafe (unit vs integration tests).
- Java 25: EnumSet, EnumMap, streams advanced (groupingBy, partitioningBy).
- SQL: EXPLAIN ANALYZE, deadlock detection.

### Production code evidence expected
- No production code expected by default.
- Potential production touchpoints if tests reveal concurrency bugs.

### Test code evidence expected
- `apps/backend/src/test/java/lab/paymentquality/web/PaymentOrderControllerTest.java` — @WebMvcTest + MockMvc.
- `apps/backend/src/test/java/lab/paymentquality/concurrency/PaymentOrderParallelTest.java` — @Execution(CONCURRENT).
- `apps/backend/src/test/java/lab/paymentquality/concurrency/PaymentOrderTransactionTest.java` — transaction isolation.
- `apps/backend/src/test/java/lab/paymentquality/concurrency/PaymentOrderLockingTest.java` — locking.
- `apps/backend/src/test/java/lab/paymentquality/observability/PaymentOrderLoggingTest.java` — log assertions.
- `apps/backend/src/test/java/lab/paymentquality/async/PaymentOrderAsyncTest.java` — Awaitility.
- `FlakyTestDiagnosis.md` — methodology documentation.
- `FailureAnalysisChecklist.md` — failure analysis documentation.
- Extended `pom.xml` z failsafe plugin.

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability.md`
- `02 Phase 2 - Payment Orders/Lesson 13 - Business Logic, Decision Tables, and Risk Notes.md`

### Descriptive lesson materials (2026-05-31)

| Area | File |
|---|---|
| Java 25 | `02 Areas/Technical Learning/Java 25 For SDET/Lesson 13 - EnumSet, EnumMap, Streams Advanced, and Optional.md` |
| REST testing | `02 Areas/Technical Learning/JUnit REST Assured/Lesson 13 - MockMvc, Parallel Execution, Extensions, and Awaitility.md` |
| HTTP/API | `02 Areas/Technical Learning/REST API From Zero/Lesson 13 - HTTP Caching Deep Dive, CORS Configuration, and API Versioning Strategies.md` |
| SQL/PostgreSQL | `02 Areas/Technical Learning/PostgreSQL and SQL From Zero/Lesson 13 - EXPLAIN ANALYZE, Transaction Isolation, and Locking.md` |
| Business logic | `02 Phase 2 - Payment Orders/Lesson 13 - Business Logic, Decision Tables, and Risk Notes.md` |

### Commands to run after implementation
- `cd apps/backend && ./mvnw test` (unit tests via surefire)
- `cd apps/backend && ./mvnw verify` (unit + integration tests via surefire + failsafe)
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderControllerTest test`
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderParallelTest test`
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderLoggingTest test`
- `cd apps/backend && ./mvnw -Dtest=PaymentModuleTest test`

### Guardrails
- No new production code unless tests reveal concurrency bugs.
- No new endpoints or business logic.
- No frontend changes.
- Parallel tests must ensure test data isolation.

### Interview answer EN
Draft:
> Lesson 13 transforms me from a test writer to a test infrastructure architect. I designed focused controller tests with @WebMvcTest and MockMvc for fast, isolated web layer testing. I implemented parallel test execution with @Execution(CONCURRENT) and ensured test data isolation. I tested transaction isolation levels (READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE) and both pessimistic and optimistic locking strategies. I added log assertions with ListAppender, documented flaky test diagnosis methodology, and configured Maven surefire vs failsafe for proper unit vs integration test lifecycle. This is production-grade test infrastructure.

### Next lesson/sprint handoff
After Lesson 13, choose between contract documentation/OpenAPI readiness (Lesson 14) or webhook subscription (Lesson 15) depending on project direction.

## Lesson 07 — Payment Order List, Filter, Search + RA Framework Architecture

**Data:** 2026-05-28
**Status:** Ready — code implemented, tests passing

### Production code evidence
- `web/PaymentOrderListRequest.java` — record z walidacją query params
- `web/PaymentOrderListResponse.java` — record: content, page, size, totalElements, totalPages
- `web/PaymentOrderListMapper.java` — mapuje Page<PaymentOrder> → DTO
- `infrastructure/PaymentOrderSpecification.java` — 6 static Specification builderów
- `application/PaymentOrderListService.java` — @Transactional(readOnly), dynamic query z addIfNotNull()
- `web/PaymentOrderController.java` — dodany @GetMapping listPaymentOrders z 10 @RequestParam
- `infrastructure/JpaPaymentOrderRepository.java` — rozszerzony o JpaSpecificationExecutor
- `web/PaymentExceptionHandler.java` — dodane handler dla BindException, IllegalArgumentException, DateTimeParseException
- `shared/security/SecurityConfig.java` — dodany matcher GET /api/merchants/*/payment-orders
- `db/migration/payment/V3__add_payment_order_list_indexes.sql` — 2 indeksy IF NOT EXISTS

### Test code evidence
- `rest/PaymentOrderListRestAssuredTest.java` — 10 contract tests (all pass)
- `testsupport/PaymentOrderListApiTestSupport.java` — seed + RequestSpecBuilder + ResponseSpecBuilder
- `testsupport/PaymentOrderAssertions.java` — custom AssertJ: hasOnlyStatus, allAmountsGreaterThan, hasPageMetadata
- `testsupport/RestAssuredLoggingConfig.java` — failure-only logging

### Spec Kit artifacts
- `specs/004-payment-order-list-filter/spec.md` — 29 FR, 12 SC, 5 user stories
- `specs/004-payment-order-list-filter/plan.md` — 14-section implementation plan
- `specs/004-payment-order-list-filter/tasks.md` — 37 tasks across 9 phases

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 07 - Payment Order List Filter Search.md` — 18-section lesson note

### Commands run
- `./mvnw -Dtest=PaymentOrderListRestAssuredTest test` — 10/10 pass
- `./mvnw -Dtest="PaymentOrder*" test` — 45/45 pass (35 existing + 10 new)
- `./mvnw -Dtest=PaymentModuleTest test` — Modulith architecture pass

### Competency updates
22 topics moved from Not Started/Introduced → Practiced:
- RA: queryParam(), accept(), extract().as(), RequestSpecBuilder, ResponseSpecBuilder, log().ifValidationFails()
- AssertJ: extracting, filteredOn, tuple, usingRecursiveComparison, SoftAssertions
- JUnit: @ParameterizedTest, @CsvSource, @Nested, @Tag, @DisplayName
- SQL: WHERE, ORDER BY, LIMIT, indexes, COUNT, JpaSpecificationExecutor
- Test Design: decision tables, negative-path first

### Open risks
- Cross-tenant list security tests not implemented (T022-T023 pending)
- @ParameterizedTest not yet used (planned for extension)
- Authorization masking in logs (simplified config — full masking deferred)

### Interview answer EN
Created (5 questions in Lesson 07 §15):
1. Cross-tenant list 403 vs 404
2. JpaSpecificationExecutor dynamic query
3. "Other specification must not be null" fix
4. extract().path() vs extract().as(Class)
5. merchant:payments:create doesn't grant list access

### Next sprint handoff
Lesson 08: Payment Aggregation (GROUP BY, COUNT per status, EXPLAIN)

## Lesson 08 — Payment Aggregation Summary

**Data:** 2026-05-30
**Status:** Ready - summary backend and summary REST/security/business-flow tests pass

### Prompt
- `../Learning Prompts/Prompt - Lesson 08 - Payment Aggregation Summary.md`

### Business capability
Read-only merchant-scoped payment order summary over existing `payment_orders`: total orders, total amount in minor units, breakdown by currency and breakdown by status.

### Learning delta
- `GROUP BY`, `COUNT`, `SUM(amount_minor)`
- aggregation response DTOs and repository projections
- controlled seed dataset as aggregation oracle
- AssertJ `tuple()` / `SoftAssertions` for grouped totals
- `EXPLAIN` as SQL diagnostics
- summary security matrix reusing `merchant:payments:read` / `platform:payments:read`

### Production code evidence
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`

### Test code evidence
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java` - 10 contract tests for empty summary, seeded totals, filters, validation and correlation header.
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryBusinessFlowRestAssuredTest.java` - 3 business-flow tests for deterministic aggregate oracle, cross-tenant denial and platform reader access.
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummarySecurityTest.java` - 7 security tests for unauthenticated, denied, create-only, operate-only, own merchant, cross-tenant and platform reader access.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderSummaryApiTestSupport.java` - deterministic seed data, expected aggregate calculation and reusable request support.

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 08 - Payment Aggregation Summary.md`

### Commands run
- `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` - passed, 20 tests.
- `./mvnw -DskipTests package` - passed.
- `./mvnw -Dtest=PaymentModuleTest test` - passed, 2 tests.

### Verification blocker
- Resolved before Lesson 09 planning. Prior `testCompile` blocker in `MyPaymentOrderBusinessFlowRestAssuredTest.java` no longer blocks package/module verification.

### Guardrails
- Do not add authorize/capture/cancel lifecycle actions.
- Do not add PSP integration or PSP mock flows.
- Do not add Kafka/webhooks.
- Do not add new payment statuses only to make aggregation more interesting.
- Do not build a complete business dashboard.
- Do not add test implementation in this system-only slice.

### Interview answer EN
Current:
> Lesson 8 extends payment order list/read behavior with a read-only summary endpoint. The backend system slice is implemented with DB-side aggregation, summary-specific security matcher ordering, and tenant ownership enforcement. Test automation for summary is intentionally deferred to a separate tester slice.

### Descriptive lesson materials (2026-05-30)

Created 5 focused descriptive lessons in the Technical Learning vault, each covering a different aspect of Lesson 08:

| Area | File |
|---|---|
| Java | `02 Areas/Technical Learning/Java 25 For SDET/Lesson 08 - Java Records, Read-Only Services, and Input Validation.md` |
| REST testing | `02 Areas/Technical Learning/JUnit REST Assured/Lesson 08 - Aggregation Contract, Security, and Business Flow Tests.md` |
| SQL | `02 Areas/Technical Learning/PostgreSQL and SQL From Zero/Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries.md` |
| HTTP/API | `02 Areas/Technical Learning/REST API From Zero/Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy.md` |
| Business logic | `02 Phase 2 - Payment Orders/Lesson 08 - Business Logic, Decision Tables, and Risk Notes.md` |

Each lesson follows the Learning OS 11-section template (goal → code map → concepts → walkthrough → delta vs L07 → mistakes → exercises → questions → testing → next links), with direct references to real production and test files from Lesson 08.

### Next lesson/sprint handoff
Lesson 09 should close the frontend consumer gap for existing payment list/summary APIs before any payment lifecycle work.

## Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment

**Data:** 2026-05-31
**Status:** Ready - frontend consumer slice implemented and verified

### Prompt
- `../Learning Prompts/Prompt - Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment.md`

### Business capability
Typed Nuxt Dashboard consumer for existing merchant-scoped payment order list and summary endpoints: summary cards, list table, empty/loading/forbidden states, and frontend tests.

### Learning delta
- Nuxt server proxy for existing GET endpoints with query params and access token forwarding.
- Zod response schemas for backend contract consumption.
- Typed Pinia store instead of `any` for payment responses.
- Playwright UI state tests for happy, empty and forbidden states.
- Consumer-driven contract thinking without adding Pact/WireMock yet.
- Clear split between REST Assured backend contract assertions and Playwright UI assertions.

### Skills expected
- `payment-quality-lab-orchestrator`
- `business-analysis-and-product-discovery-for-payment-lab`
- `qa-architecture-sprint-team`
- `nuxt-dashboard-zod-pinia-frontend-engineering`
- `typescript6-playwright-engineering`
- `rest-api-security-oauth-testing`
- `junit6-assertj-restassured-testcraft`
- `parallel-test-architecture-and-data-isolation`
- `test-analysis-design-and-data`

### Production code evidence
- `apps/frontend/app/schemas/payment-order.schema.ts` - Zod schemas and inferred types for create/read/list/summary/backend error responses.
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/index.get.ts` - Nuxt server proxy for backend list endpoint with query params and access token forwarding.
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/summary.get.ts` - Nuxt server proxy for backend summary endpoint.
- `apps/frontend/app/stores/payment-orders.ts` - typed Pinia state/actions for list, summary, current order, last created order, loading/error/forbidden state.
- `apps/frontend/app/components/payment/PaymentOrderSummaryCards.vue` - summary cards for existing backend totals and breakdowns.
- `apps/frontend/app/components/payment/PaymentOrderListTable.vue` - payment order list table and empty state.
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` - merchant-scoped payments panel with loading, empty, forbidden and backend-unavailable states.
- `apps/frontend/app/pages/admin/merchants/index.vue` - merchant registry page moved to index route so nested payment panel routes resolve correctly.
- `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue` - create response now parsed with `paymentOrderResponseSchema` before updating typed store state.

### Test code evidence
- `apps/frontend/tests/e2e/payment-orders-panel.spec.ts` - 4 Playwright tests for summary/list rendering, empty merchant state, forbidden no-data state and backend-unavailable state.

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment.md`

### Spec Kit artifacts
- `specs/006-payment-orders-frontend-consumer/spec.md` - light lesson-extension specification for frontend consumer scope.
- `specs/006-payment-orders-frontend-consumer/plan.md` - implementation plan and guardrails.
- `specs/006-payment-orders-frontend-consumer/tasks.md` - completed task checklist for the frontend consumer slice.

### Commands run
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` - passed, 20 tests.
- `cd apps/backend && ./mvnw -DskipTests package` - passed.
- `cd apps/backend && ./mvnw -Dtest=PaymentModuleTest test` - passed, 2 tests.
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` - command completed without failure report; output was truncated by tool capture.
- `cd apps/frontend && corepack pnpm typecheck` - passed.
- `cd apps/frontend && corepack pnpm test:e2e -- payment-orders-panel.spec.ts` - passed, 4 tests.

### Open risks
- Playwright tests route-mock Nuxt API responses; they verify UI states, not live backend integration.
- The combined backend list/summary regression command output was truncated by tool capture, although no failure report was returned.
- Keycloak setup docs may still be stale for payment roles/users; no new roles/users were added for Lesson 09.

### Interview answer EN
> Lesson 9 turns existing payment order list and summary REST contracts into a typed Nuxt Dashboard consumer. It deliberately avoids new lifecycle behavior and focuses on frontend contract alignment: Zod schemas, typed Pinia state, role-aware UI states, Playwright tests, and REST Assured regression as backend truth.

### Next lesson/sprint handoff
After Lesson 09, choose between BOLA/BFLA deep dive, DB oracle/EXPLAIN practice, or formal Spec Kit for payment lifecycle only if the guardrails are updated.

## Template Dla Nowej Lekcji/Sprintu

```text
## Lesson NN - Title

Status:
Prompt:
Business capability:
Learning delta:
Skills expected:
Skills actually used:
Production code evidence:
Test code evidence:
Vault notes:
Spec Kit artifacts:
Commands run:
Competency matrix updates:
Open risks:
Interview answer EN:
Next lesson/sprint handoff:
```

## Lesson 01-05 Summary

| Lesson | Status | Evidence | Notes |
|---:|---|---|---|
| 01 | Introduced | REST API request/response flow prompt and vault note | Foundation; no need to repeat in Lesson 6 |
| 02 | Introduced | REST Assured entry prompt | Foundation; no need to repeat in Lesson 6 |
| 03 | Practiced | REST Assured foundations doc | HTTP method/endpoint/content-type basics |
| 04 | Practiced | Lesson 4 prompt + lesson-pack expansion | Path/query/header basics, `Authorization`, `X-Correlation-ID` context |
| 05 | Practiced | Lesson 5 prompt + lesson-pack expansion | request body, JSON, `Map.of`, DTO, serialization |

## Lesson 06 - Payment Order Create/Read Foundation

Status: `Ready`

Prompt: `../Learning Prompts/Prompt - Lesson 06 - PayU Like Business Flow Expansion Sprint.md` and current interactive prompt for Payment Order create/read lesson.

Business capability: Payment Order create/read foundation with idempotent creation, merchant-scoped access, platform read access, PostgreSQL/Flyway persistence, Keycloak role/claim model, minimal frontend consumer and REST Assured tests.

Learning delta:

- no repetition of `given/when/then`, path params, basic headers and request body basics,
- first payment-specific REST resource,
- `Idempotency-Key` and request fingerprint,
- `Location`, `ETag`, `X-Correlation-ID`, `201`, replay `200`, `403`, masked `404`, `409`,
- role authorization plus `merchant_id` ownership,
- SQL constraints for amount, currency, status and idempotency uniqueness,
- Flyway migration as executable DB contract,
- REST Assured contract tests for headers/body/status/error code,
- security matrix tests for create/read access,
- frontend as API consumer preserving stable idempotency key.

Skills expected:

- `qa-architecture-sprint-team`,
- `obsidian-learning-os`,
- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `postgres18-data-architecture-and-risk`,
- `spring-modulith-2-0-6-modular-monolith-testing`.

Skills actually used:

- `qa-architecture-sprint-team`,
- `obsidian-learning-os`,
- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `postgres18-data-architecture-and-risk`,
- `spring-modulith-2-0-6-modular-monolith-testing`.

Production code evidence:

- `apps/backend/src/main/java/lab/paymentquality/payment/`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibility.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibilityService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantPaymentEligibilityAdapter.java`
- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/frontend/app/components/payment/`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/`
- `infra/keycloak/realms/payment-quality-realm.json`

Test code evidence:

- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderServiceTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderIdempotencyConcurrencyTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepositoryTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/PaymentModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentApiTestSupport.java`

Vault notes:

- `../02 Phase 2 - Payment Orders/Phase 2 - Payment Orders.md`
- `../02 Phase 2 - Payment Orders/Lesson 06 - Payment Order Create Read Foundation.md`
- `../Learning Governance/Expert Gap Analysis - Senior SDET Coverage.md`

Spec Kit artifacts:

- `specs/003-payment-order-access-lifecycle/spec.md`
- `specs/003-payment-order-access-lifecycle/plan.md`
- `specs/003-payment-order-access-lifecycle/data-model.md`
- `specs/003-payment-order-access-lifecycle/contracts/payment-order-api.md`
- `specs/003-payment-order-access-lifecycle/quickstart.md`
- `specs/003-payment-order-access-lifecycle/tasks.md`

Commands run:

- `./mvnw test -q` in `apps/backend` - passed.
- `corepack pnpm typecheck` in `apps/frontend` - passed.

Competency matrix updates: updated after Payment Order create/read scope materialized.

Open risks:

- current lesson note is ready, but REST Assured foundation pack can still be extended with a cross-link instead of duplicating content,
- future lifecycle topics remain deferred: authorize/capture/cancel, `If-Match`, `412`, PSP integration, Kafka, webhooks and settlement,
- frontend E2E is optional verification and was not run during this lesson note capture.

Interview answer EN:

> In Lesson 6 I moved from syntax-driven REST Assured practice to product-risk-driven API testing. I can explain and test an idempotent Payment Order create/read API, including retry behavior, tenant isolation, role and claim checks, database constraints, Flyway migrations, HTTP headers and REST Assured contract assertions.

Next lesson/sprint handoff: deepen REST Assured reusable response/error specifications or continue with payment lifecycle only after a new Spec Kit scope explicitly allows transitions and optimistic concurrency.
