---
type: learning-os
status: active
date: 2026-06-02
tags:
  - learning-os
  - current-lesson
---

# Current Lesson

> **Active Lesson:** 11 - REST Assured Framework Architecture and Test Organization
>
> **Status:** PLANNED - framework maturity slice after Lesson 10 completion
>
> **Next:** Implement Lesson 11 batches: API client wrappers, test data builders, reusable error specs, secret masking

## NOW: What To Learn

| Priority | Item | Type | Time |
|---|---|---|---|
| 1 | [[Lesson 11 - REST Assured Framework Architecture and Test Organization]] - implement framework maturity tests | Test implementation | 1-2 sessions |
| 2 | [[Prompt - Lesson 11 - REST Assured Framework Architecture and Test Organization]] - use as execution prompt | Prompt | 15 min |
| 3 | Design API client wrappers: PaymentOrderApi, MerchantApi | Test architecture | 45 min |
| 4 | Implement test data builders: PaymentOrderBuilder, MerchantBuilder | Java 25 patterns | 1 session |

## COVERED: Lessons 1-5 (Foundations)

| Lesson | Topic | Status |
|---|---|---|
| 01 | REST API request/response flow | Covered |
| 02 | REST Assured — what it is | Covered |
| 03 | REST Assured foundations (method/endpoint/content-type basics) | Covered |
| 04 | Path params, query params, headers, `Authorization`, `X-Correlation-ID` | Covered |
| 05 | Request body, JSON, `Map.of`, DTO serialization | Covered |

## COVERED: From Lesson 06

| Topic | Evidence | Confidence |
|---|---|---|
| Idempotent payment order creation | `PaymentOrderService`, `PaymentOrderRestAssuredTest` | Strong |
| `Idempotency-Key` and request fingerprint | `IdempotencyKey`, `RequestFingerprint` | Strong |
| HTTP headers: `Location`, `ETag`, `X-Correlation-ID` | `PaymentOrderController`, REST Assured tests | Strong |
| `201` vs replay `200` vs `409 conflict` | Tests and controller | Strong |
| Role × tenant isolation matrix | `PaymentOrderSecurityTest` | Strong |
| Cross-tenant read returns masked `404` | Security tests | Strong |
| DB constraints for amount/currency/status | `V2__create_payment_orders.sql` | Strong |
| Flyway migration as executable DB contract | `V2__create_payment_orders.sql`, repo test | Strong |
| Spring Modulith module boundary | `MerchantPaymentEligibilityService` interface | Strong |
| Frontend as API consumer | `CreatePaymentOrderForm.vue`, server routes | Moderate |
| Test data ownership per test | Per-test merchant creation, unique keys | Strong |
| REST Assured body/header/status assertions | `PaymentOrderRestAssuredTest` | Strong |

## COVERED: From Lesson 07

| Topic | Evidence | Confidence |
|---|---|---|
| Payment order list/filter/search | `PaymentOrderListRestAssuredTest`, `PaymentOrderListService` | Strong |
| `queryParam()` for filters | `PaymentOrderListRestAssuredTest` | Strong |
| Typed extraction with `extract().as(...)` | `PaymentOrderListRestAssuredTest` | Strong |
| `RequestSpecBuilder` / `ResponseSpecBuilder` | `PaymentOrderListApiTestSupport` | Strong |
| Failure-only logging | `RestAssuredLoggingConfig` | Moderate |
| `WHERE`, `ORDER BY`, `LIMIT/OFFSET`, pagination count | `PaymentOrderSpecification`, `PaymentOrderListService` | Strong |
| Cross-tenant list returns `403` | Lesson 07 decision, tests pending as extension | Moderate |

## COVERED: From Lesson 08

| Topic | Evidence | Confidence |
|---|---|---|
| Payment order summary endpoint | `PaymentOrderSummaryService`, `PaymentOrderController` | Strong |
| SQL aggregation: `GROUP BY`, `COUNT`, `SUM` | `JpaPaymentOrderRepository`, summary tests | Strong |
| Summary contract tests | `PaymentOrderSummaryRestAssuredTest` | Strong |
| Summary business-flow oracle | `PaymentOrderSummaryBusinessFlowRestAssuredTest` | Strong |
| Summary security matrix | `PaymentOrderSummarySecurityTest` | Strong |
| Modulith boundary after summary | `PaymentModuleTest` | Strong |
| Package verification after testCompile fix | `./mvnw -DskipTests package` | Strong |

## COVERED: From Lesson 09

| Topic | Evidence | Confidence |
|---|---|---|
| Nuxt server proxy for payment list/summary | `apps/frontend/server/api/merchants/[merchantId]/payment-orders/*.get.ts` | Strong |
| Zod response schemas for backend consumer contracts | `apps/frontend/app/schemas/payment-order.schema.ts` | Strong |
| Typed Pinia payment order state | `apps/frontend/app/stores/payment-orders.ts` | Strong |
| Merchant-scoped payments panel | `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` | Strong |
| Summary/list UI without fake lifecycle actions | `PaymentOrderSummaryCards.vue`, `PaymentOrderListTable.vue` | Strong |
| Playwright happy/empty/forbidden/backend-unavailable UI states | `apps/frontend/tests/e2e/payment-orders-panel.spec.ts` | Strong |
| Backend list/summary regression guardrails for frontend consumer | REST Assured summary/list/security/business-flow commands | Strong |

## COVERED: From Lesson 10

| Topic | Evidence | Confidence |
|---|---|---|
| HTTP edge contract hardening | `PaymentOrderSummaryHttpContractRestAssuredTest` — 7 tests | Strong |
| Parameterized authorization matrix | `PaymentOrderSummaryAuthorizationMatrixTest` — 12 parameterized tests | Strong |
| Route collision guardrail | `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape` | Strong |
| Content negotiation (`Accept: text/xml` → 406) | `unsupportedAcceptIsRejectedOrExplicitlyCharacterized` | Strong |
| Unsupported methods (PUT/PATCH/DELETE → 405) | `unsupportedMethodsDoNotExposeSummaryMutationSurface` | Strong |
| Conditional header discipline (no ETag) | `ifNoneMatchDoesNotEnableSummaryCaching` | Strong |
| Malformed UUID validation (400) | `malformedMerchantIdReturnsValidationError` — 3 variants | Strong |
| BOLA vs BFLA labeling | Authorization matrix with explicit BOLA/BFLA labels | Strong |
| `TestJwtSupport` extension | `merchantPaymentReaderTokenWithoutMerchantIdClaim()` | Strong |
| Spring MVC characterization | Research documented in `specs/007/research.md` | Strong |

## PLANNED: Lesson 11

| Topic | Expected Evidence | Why Now |
|---|---|---|
| API client wrapper pattern | `PaymentOrderApi.java` | Zastępuje surowe REST Assured chains biznesowo-czytelnymi metodami |
| Test data builders | `PaymentOrderBuilder.java` | Zastępuje `Map.of(...)` fluent builder pattern |
| Reusable error specs | `PaymentErrorSpecs.java` | Reusable `ResponseSpecification` dla error contracts |
| Secret masking | `RestAssuredLoggingConfig.java` rozszerzenie | `blacklistHeader("Authorization")` w logach |
| Test organization | @Nested, @Tag w istniejących testach | Organizacja testów w inner classes z selective execution |
| Scenario flows | `PaymentOrderScenarioFlowTest.java` | Multi-step tests (create → list → summary) |

## PLANNED: Lesson 12

| Topic | Expected Evidence | Why Now |
|---|---|---|
| TypeRef<T> dla generic list extraction | Rozszerzenie `PaymentOrderListRestAssuredTest.java` | Type-safe extraction dla `List<T>` |
| GPath advanced (deep scan, findAll, array indexing) | Rozszerzenie istniejących testów | Zaawansowane GPath patterns |
| Response time assertions | `PaymentOrderPerformanceTest.java` | Performance baseline establishment |
| usingRecursiveComparison | Rozszerzenie `PaymentOrderAssertions.java` | Deep object comparison |
| SoftAssertions | Rozszerzenie summary tests | Multiple assertions, one failure |
| @ParameterizedTest z @MethodSource/@CsvSource/@EnumSource | `PaymentOrderParameterizedTest.java` | Data-driven testing |

## PLANNED: Lesson 13

| Topic | Expected Evidence | Why Now |
|---|---|---|
| @WebMvcTest / MockMvc | `PaymentOrderControllerTest.java` | Focused controller tests bez full Spring context |
| @MockBean / @SpyBean | Rozszerzenie controller tests | Mocking dependencies w Spring tests |
| @Execution(CONCURRENT) | `PaymentOrderParallelTest.java` | Parallel test execution |
| Transaction isolation levels | `PaymentOrderTransactionTest.java` | READ_COMMITTED vs REPEATABLE_READ vs SERIALIZABLE |
| Pessimistic vs optimistic locking | `PaymentOrderLockingTest.java` | Concurrency control |
| Log assertions | `PaymentOrderLoggingTest.java` | Weryfikacja logów |
| Flaky test diagnosis | `FlakyTestDiagnosis.md` | Methodology wykrywania flaky tests |
| Failure analysis | `FailureAnalysisChecklist.md` | App bug vs test bug vs data bug vs env bug |
| Awaitility | `PaymentOrderAsyncTest.java` | Async polling (zamiast Thread.sleep) |
| Maven surefire vs failsafe | `pom.xml` rozszerzenie | Unit vs integration tests lifecycle |

## INTRODUCED: Seen But Not Yet Mastered

| Topic | Where | What You Still Need |
|---|---|---|
| Assertion strategy (RA body vs AssertJ vs DB query) | Lesson 06 §12a | Practice making the decision yourself |
| Database verification as test layer | Lesson 06 §12b | Write tests that probe DB directly |
| Idempotency concurrency/race conditions | `PaymentOrderIdempotencyConcurrencyTest` | Write your own concurrency scenario |
| `ETag` / `If-Match` / `412` | Lesson 06 headers discussion | Not yet implemented — deferred |
| AssertJ `extracting`, `filteredOn`, `tuple` | Limited in existing tests | Write your own complex extractions |
| REST Assured `RequestSpecification` reuse | `MerchantApiTestSupport` | Create your own spec builders |
| Business-readable test names (`@DisplayName`) | Concept introduced | Add `@DisplayName` to 3 tests |
| Negative-path first methodology | Concept introduced | Write negative test before happy path |
| Frontend consumer contract | Lesson 09 implementation | Practice explaining the backend contract vs UI consumer split without reading notes |
| Playwright UI state coverage | `payment-orders-panel.spec.ts` | Add future tests only when new UI behavior exists |
| Consumer-driven contract thinking | Lesson 09 evidence | Practice choosing REST Assured vs Playwright assertions for new cases |
| Content negotiation and unsupported methods | Lesson 10 plan | Implement and explain `Accept`, `406`/characterized behavior and `405` |
| Parameterized authorization matrix | Lesson 10 plan | Convert role/claim cases into readable JUnit matrix rows |
| Error contract consistency | Lesson 10 plan | Assert status, `error`, message and content type where payment-domain handler applies |

## NEEDS PRACTICE: Exercises

| # | Exercise | Time |
|---|---|---|
| 1 | Explain why Lesson 10 is test hardening, not a new payment business feature | 15 min |
| 2 | Write the expected `401`/`403`/`200` rows for summary access before coding | 30 min |
| 3 | Explain `Accept` vs `Content-Type` using one Payment Order example | 20 min |
| 4 | Compare BOLA and BFLA using summary endpoint cases | 20 min |
| 5 | Explain why `/summary` route ordering matters next to `/{paymentOrderId}` | 20 min |
| 6 | Decide when an HTTP edge result is acceptable Spring behavior vs product bug | 30 min |

## DEFERRED: Do NOT Study Now

| Topic | When |
|---|---|
| Payment lifecycle (authorize/capture/cancel) | Future Spec Kit after frontend consumer gap is closed |
| PSP integration | Spec Kit 005+ |
| Kafka, webhooks, event pipeline | Future async sprint after REST/HTTP hardening |
| GraphQL, gRPC | Sprint 13+ |
| Performance/load testing | Sprint 13b |
| JSON Schema / OpenAPI validation | Future contract-doc readiness after Lesson 10 |
| Contract testing (Pact/WireMock) | Future async/contract testing sprint |
| `If-Match` / `412` / optimistic concurrency | Spec Kit 004+ |
| RLS (Row-Level Security) | Sprint 9 extension |
| Complete OAuth/OIDC | Phase 0 guardrail — never |
| Complete business dashboards | Phase 0 guardrail — never |

## Evidence Checklist

- [x] Lesson 08 production evidence captured
- [x] Lesson 08 REST/security/business-flow tests exist and pass
- [x] Lesson 08 package and Modulith verification pass
- [x] Lesson 09 note exists: [[Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]]
- [x] Lesson 09 prompt exists: [[Prompt - Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]]
- [x] Lesson 09 frontend implementation completed
- [x] Lesson 09 Playwright tests completed
- [x] Lesson 09 frontend typecheck passes
- [x] Lesson 09 backend regression guardrails captured
- [x] Lesson 09 evidence captured after implementation
- [x] Lesson 10 note created: [[Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [x] Lesson 10 prompt created: [[Prompt - Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [x] Lesson 10 HTTP edge contract tests implemented — 7 tests pass
- [x] Lesson 10 authorization matrix tests implemented — 12 parameterized tests pass
- [x] Lesson 10 backend verification commands captured — 41 tests pass total
- [x] Lesson 10 Spec Kit artifacts created — spec, plan, tasks, research, data-model, contracts
- [x] Lesson 10 evidence captured in Lesson Evidence Tracker
- [x] Lesson 11 note created: [[Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [x] Lesson 11 prompt created: [[Prompt - Lesson 11 - REST Assured Framework Architecture and Test Organization]]
- [x] Lesson 12 note created: [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [x] Lesson 12 prompt created: [[Prompt - Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [x] Lesson 13 note created: [[Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]
- [x] Lesson 13 prompt created: [[Prompt - Lesson 13 - Spring Testing Layers, Concurrency, Observability, and Test Reliability]]

## Navigation

- [[Current Learning Flow]] — process and flow
- [[Current Sprint]] — sprint status
- [[Curriculum Backbone]] — technology ↔ lesson map
- [[Lesson Evidence Tracker]] — detailed evidence
- [[Learning Progress Board]] — overall progress
