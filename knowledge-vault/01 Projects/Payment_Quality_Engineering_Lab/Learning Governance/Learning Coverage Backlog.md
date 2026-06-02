---
type: tracker
status: active
date: 2026-05-31
tags:
  - learning-os
  - backlog
  - coverage
---

# Learning Coverage Backlog

Complete inventory of topics for Senior SDET readiness. Every topic has a status, assigned lesson, evidence, and next action.

Statuses: `Not Started` | `Planned` | `Lesson Created` | `Reading Assigned` | `Read` | `Practiced` | `Evidence Strong` | `Needs Repeat` | `Deferred`

Evidence rule: do NOT mark `Practiced` or `Evidence Strong` without proof in lesson files, test files, code files, exercises, quiz results, or explicit learner confirmation.

## 1. HTTP and REST Semantics

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| request/response anatomy | Evidence Strong | Lesson 01 | Prompt, vault note | — |
| HTTP methods (GET, POST, PUT, PATCH, DELETE) | Practiced | Lesson 01 | Lesson 01 prompt | — |
| safe vs idempotent | Introduced | Lesson 06 | REST Assured tests | Deepen in 06B |
| status codes (1xx-5xx) | Practiced | Lesson 01 | Lesson 01 prompt | — |
| `201 Created` + `Location` | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| `200 OK` replay (idempotent) | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| `400 validation` | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| `401` unauthenticated | Practiced | Phase 1 | `MerchantSecurityTest` | Deepen in 06E |
| `403` forbidden | Practiced | Phase 1 | `MerchantSecurityTest` | Deepen in 06E |
| masked `404` (cross-tenant) | Evidence Strong | Lesson 06 | `PaymentOrderSecurityTest` | — |
| `409 idempotency_conflict` | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| `409 merchant_not_payment_eligible` | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| Content-Type | Practiced | Lesson 02 | REST Assured docs | — |
| Accept | Evidence Strong | Lesson 10 | `PaymentOrderSummaryHttpContractRestAssuredTest` | — |
| content negotiation (415, 406) | Evidence Strong | Lesson 10 | `PaymentOrderSummaryHttpContractRestAssuredTest` | — |
| ETag | Introduced | Lesson 06 | `PaymentOrderController` | Deepen in 06C |
| If-Match / 412 | Deferred | Spec Kit 004+ | — | After lifecycle actions exist |
| Idempotency-Key | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| Retry-After / 429 | Deferred | Future | — | Rate limit sprint |
| WWW-Authenticate | Not Started | — | — | Optional auth-header inspection after Lesson 10 |
| X-Correlation-ID | Introduced | Lesson 06 | `CorrelationIdFilter`, header tests | Add RA header test exercise |
| malformed JSON | Not Started | — | — | POST/create HTTP deep-dive, not summary GET |
| unsupported media type | Planned | Lesson 10 follow-up | Lesson 10 identifies HTTP edge gap | Use POST/create only if in scope later |
| API error contract (stable codes) | Evidence Strong | Lesson 06 | `PaymentExceptionHandler`, `PaymentErrorResponse` | — |

## 2. REST Assured Fundamentals

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| what REST Assured is | Practiced | Lesson 02 | Prompt, docs | — |
| `given()` | Practiced | Lesson 02 | All RA tests | — |
| `when()` | Practiced | Lesson 02 | All RA tests | — |
| `then()` | Practiced | Lesson 02 | All RA tests | — |
| `contentType()` | Introduced | Lesson 02 | RA docs | Deepen in 06A |
| `accept()` | Evidence Strong | Lesson 10 | `PaymentOrderSummaryHttpContractRestAssuredTest` | — |
| `body()` (request) | Practiced | Lesson 05 | All RA tests | — |
| `pathParam()` | Practiced | Lesson 04 | `MerchantRestAssuredTest` | — |
| `queryParam()` | Practiced | Lesson 07 | `PaymentOrderListRestAssuredTest` | — |
| `header()` (request) | Practiced | Lesson 04 | `MerchantRestAssuredTest` | — |
| `get/post/put/patch/delete` | Practiced | Lesson 02 | All RA tests | — |
| `statusCode()` | Practiced | Lesson 02 | All RA tests | — |
| `body(path, matcher)` | Practiced | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| `extract().path()` | Evidence Strong | Lesson 06-07 | `PaymentOrderListRestAssuredTest` | — |
| `header()` (response) | Practiced | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| `auth()` / auth methods | Introduced | Phase 1 | `MerchantSecurityTest` | Deepen in 06E |
| JSON/GPath basics | Introduced | Lesson 05 | Request body tests | Deepen in 06A |
| schema validation basics | Deferred | Future contract-doc readiness | — | After Lesson 10 REST/HTTP hardening |

## 3. REST Assured Framework Architecture

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| raw test before helper | Practiced | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| RequestSpecification | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| ResponseSpecification | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| RequestSpecBuilder | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| ResponseSpecBuilder | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| auth specs | Evidence Strong | Lesson 10 | `PaymentOrderSummaryAuthorizationMatrixTest` | — |
| JSON specs | Planned | Lesson 11 | Lesson 11 prompt/note | Reusable error specs |
| API clients (wrapper pattern) | Planned | Lesson 11 | Lesson 11 prompt/note | PaymentOrderApi, MerchantApi |
| fixtures | Planned | Lesson 11 | Lesson 11 prompt/note | Object Mother pattern |
| test data builders | Planned | Lesson 11 | Lesson 11 prompt/note | PaymentOrderBuilder, MerchantBuilder |
| scenario flows (multi-step tests) | Planned | Lesson 11 | Lesson 11 prompt/note | Create → List → Summary flow |
| error assertions | Practiced | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| AssertJ integration after extract | Practiced | Lesson 08 | Summary typed extraction and aggregate assertions | Deepen with DB oracle if needed |
| logging only on validation failure | Practiced | Lesson 07 | `RestAssuredLoggingConfig` | — |
| masking Authorization | Planned | Lesson 11 | Lesson 11 prompt/note | blacklistHeader w RestAssuredLoggingConfig |
| parallel-safe test data | Planned | Lesson 13 | Lesson 13 prompt/note | @Execution(CONCURRENT) + test data isolation |

## 4. Java 25 For SDET

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| classes | Practiced | Lesson 02 | All code | — |
| methods | Practiced | Lesson 02 | All code | — |
| constructors | Practiced | Lesson 02 | All code | — |
| records | Practiced | Phase 1 | `CreateMerchantRequest`, `ErrorResponse` | — |
| enums | Practiced | Phase 1 | `MerchantStatus` | — |
| value objects | Evidence Strong | Lesson 06 | `PaymentAmount`, `CurrencyCode` | — |
| exceptions | Practiced | Phase 1 | Domain exceptions | — |
| collections (List, Map) | Practiced | Lesson 05 | Request body, test data | — |
| Map.of | Practiced | Lesson 05 | Request body construction | — |
| Map.copyOf / immutability | Planned | Lesson 11 | Lesson 11 Java 25 note | Defensive copies w builders |
| Optional | Planned | Lesson 13 | Lesson 13 Java 25 note | Optional w return types i test assertions |
| Streams | Planned | Lesson 13 | Lesson 13 Java 25 note | groupingBy, partitioningBy, downstream collectors |
| UUID | Practiced | Lesson 06 | `@PathVariable UUID` | — |
| java.time (Instant, LocalDateTime) | Introduced | Phase 1 | Merchant timestamps | Deepen in 06F |
| annotations (@Test, @Entity, @Valid) | Practiced | Phase 1 | All code | — |
| generics | Planned | Lesson 12 | Lesson 12 Java 25 note | Bounded wildcards, PECS |
| deprecated API awareness | Not Started | — | — | Build hygiene lesson |
| JDK warnings | Not Started | — | — | Build hygiene lesson |
| Mockito javaagent | Not Started | — | — | Concept Lesson |
| sealed interface | Planned | Lesson 11 | Lesson 11 Java 25 note | Closed polymorphism dla test data |
| pattern matching instanceof | Planned | Lesson 12 | Lesson 12 Java 25 note | JDK 16+ feature |
| text blocks | Planned | Lesson 12 | Lesson 12 Java 25 note | Multi-line JSON fixtures |
| EnumSet / EnumMap | Planned | Lesson 13 | Lesson 13 Java 25 note | Efficient enum collections |
| Comparator.comparing / thenComparing | Planned | Lesson 11 | Lesson 11 Java 25 note | Test data ordering |
| Effective Java principles | Introduced | Multiple | Code design patterns | Deepen across lessons |
| KISS / DRY / SOLID for testability | Introduced | Multiple | Test architecture | Deepen |

## 5. JUnit and AssertJ

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| @Test | Practiced | Lesson 02 | All tests | — |
| AssertJ `assertThat()` | Practiced | Phase 1 | Service/repo tests | — |
| `extracting()` | Practiced | Lesson 06-07 | `PaymentOrderListRestAssuredTest` | — |
| `filteredOn()` | Practiced | Lesson 07 | `PaymentOrderListRestAssuredTest` | — |
| `tuple()` | Introduced | Lesson 07 | Lesson note mentions | Practice exercise |
| recursive comparison | Planned | Lesson 12 | Lesson 12 prompt/note | usingRecursiveComparison z ignoringFields |
| soft assertions | Planned | Lesson 12 | Lesson 12 prompt/note | SoftAssertions.assertAll() |
| parameterized tests | Practiced | Lesson 10 | `PaymentOrderSummaryAuthorizationMatrixTest`, `PaymentOrderSummaryHttpContractRestAssuredTest` | Deepen in Lesson 12 |
| @Nested / @Tag | Planned | Lesson 11 | Lesson 11 prompt/note | Test organization w inner classes |
| TypeRef<T> | Planned | Lesson 12 | Lesson 12 REST Assured note | Generic list extraction |
| GPath advanced | Planned | Lesson 12 | Lesson 12 REST Assured note | Deep scan, findAll, array indexing |
| response time assertions | Planned | Lesson 12 | Lesson 12 REST Assured note | .time(), .timeIn() |
| JSON Schema validation | Planned | Lesson 12 | Lesson 12 REST Assured note | matchesJsonSchemaInClasspath |
| asInstanceOf | Planned | Lesson 12 | Lesson 12 AssertJ note | Type-safe casting |
| satisfiesExactly / allSatisfy / anySatisfy | Planned | Lesson 12 | Lesson 12 AssertJ note | Collection assertions |
| matches(Predicate) | Planned | Lesson 12 | Lesson 12 AssertJ note | Custom conditions |
| @RepeatedTest | Planned | Lesson 12 | Lesson 12 JUnit note | Repeated execution |
| DynamicTest / @TestFactory | Planned | Lesson 12 | Lesson 12 JUnit note | Dynamic test generation |
| JUnit Extensions (@ExtendWith) | Planned | Lesson 13 | Lesson 13 JUnit note | Custom test lifecycle |
| Awaitility | Planned | Lesson 13 | Lesson 13 JUnit note | Async polling |
| @DisplayName | Practiced | Lesson 07 | All 10 list tests use @DisplayName | — |

## 6. SQL, PostgreSQL, Flyway and Testcontainers

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| table, row, column | Practiced | Lesson 06 | `V2__create_payment_orders.sql` | — |
| primary key | Practiced | Lesson 06 | `payment_order_id UUID PK` | — |
| foreign key | Practiced | Lesson 06 | FK to merchants | — |
| unique constraint | Evidence Strong | Lesson 06 | idempotency unique constraint | — |
| check constraint | Evidence Strong | Lesson 06 | amount/currency/status checks | — |
| not null | Practiced | Lesson 06 | All columns | — |
| indexes | Planned | Sprint 7 | — | Add indexes for list/read |
| SELECT | Practiced | Lesson 06-07 | SQL exercises + list queries | — |
| WHERE | Practiced | Lesson 07 | `PaymentOrderSpecification` | — |
| ORDER BY | Practiced | Lesson 07 | `PaymentOrderListService` | — |
| LIMIT | Practiced | Lesson 07 | `PageRequest.of(page, size)` | — |
| indexes | Practiced | Lesson 07 | `V3__add_payment_order_list_indexes.sql` | — |
| COUNT, SUM | Evidence Strong | Lesson 08 | Summary endpoint and REST/business-flow tests | Optional DB oracle/EXPLAIN in Lesson 10C |
| GROUP BY | Evidence Strong | Lesson 08 | Summary currency/status breakdowns | Optional DB oracle/EXPLAIN in Lesson 10C |
| CTE | Planned | Lesson 12 | Lesson 12 SQL note | Common Table Expressions |
| window functions | Planned | Lesson 12 | Lesson 12 SQL note | ROW_NUMBER, RANK, LAG, LEAD, running totals |
| EXPLAIN | Planned | Lesson 13 | Lesson 13 SQL note | EXPLAIN ANALYZE dla query diagnostics |
| transactions | Planned | Lesson 13 | Lesson 13 SQL note | Transaction isolation levels |
| isolation levels | Planned | Lesson 13 | Lesson 13 SQL note | READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE |
| locking | Planned | Lesson 13 | Lesson 13 SQL note | Pessimistic vs optimistic locking |
| optimistic locking (version) | Planned | Lesson 13 | Lesson 13 SQL note | @Version annotation |
| idempotency constraints | Evidence Strong | Lesson 06 | Unique constraint + service logic | — |
| Flyway migrations | Evidence Strong | Lesson 06 | `V2__create_payment_orders.sql` | — |
| Testcontainers PostgreSQL | Practiced | Lesson 06 | `PostgresContainerSupport` | — |
| DB as test oracle | Planned | Lesson 10C | Lesson 10 note | Add repository/service aggregation diagnostic if needed |
| API vs DB verification | Introduced | Lesson 08 | Summary API controlled seed oracle | Deepen with Lesson 10C decision exercise |
| SQL as diagnostic tool | Lesson Created | Lesson 06D | `Lesson 06D - SQL and Flyway Constraints for Payment Orders` | Practice during Lesson 6 SQL session |
| test data isolation | Planned | Lesson 11 | Lesson 11 SQL note | 5 strategies (per-test, truncation, schema-per-test, @Sql, Flyway) |
| parallel execution safety | Planned | Lesson 13 | Lesson 13 SQL note | @Execution(CONCURRENT) + test data isolation |
| deadlock detection | Planned | Lesson 13 | Lesson 13 SQL note | Wykrywanie i zapobieganie deadlockom |

## 7. Spring MVC, Spring Data and Backend Testing

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| `@RestController` | Practiced | Phase 1 | `MerchantController` | — |
| `@RequestBody` | Practiced | Phase 1 | All controllers | — |
| `@Valid` / Bean Validation | Practiced | Phase 1 | `CreateMerchantRequest` | — |
| `@PathVariable UUID` | Practiced | Lesson 06 | `PaymentOrderController` | — |
| `@RestControllerAdvice` | Practiced | Lesson 06 | `PaymentExceptionHandler` | — |
| `@Transactional` | Practiced | Lesson 06 | `PaymentOrderService` | — |
| `@SpringBootTest` (random port) | Practiced | Lesson 06 | All RA tests | — |
| `@DataJpaTest` / repository tests | Practiced | Lesson 06 | `JpaPaymentOrderRepositoryTest` | — |
| `@WebMvcTest` / MockMvc | Planned | Lesson 13 | Lesson 13 prompt/note | Focused controller tests bez full Spring context |
| @MockBean / @SpyBean | Planned | Lesson 13 | Lesson 13 prompt/note | Mocking dependencies w Spring tests |
| Spring profiles | Planned | Lesson 13 | Lesson 13 prompt/note | Test-specific configuration |
| Maven surefire vs failsafe | Planned | Lesson 13 | Lesson 13 prompt/note | Unit vs integration tests lifecycle |
| Spring Data JPA repository methods | Practiced | Lesson 06 | `JpaPaymentOrderRepository` | — |
| `@ApplicationModuleTest` | Practiced | Lesson 06 | `PaymentModuleTest` | — |

## 8. Test Design Methods

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| Boundary Value Analysis | Practiced | Phase 1 | Merchant validation tests | — |
| Equivalence Partitioning | Practiced | Phase 1 | Merchant validation tests | — |
| Decision Table Testing | Introduced | Lesson 06 | Idempotency decision table | Deepen in 06B |
| State Transition Testing | Introduced | Phase 1 | Merchant lifecycle | Deepen in Lesson 07 (payment lifecycle) |
| Pairwise / Combinatorial | Not Started | — | — | Future |
| Risk-Based Testing | Introduced | Lesson 06 | Lesson 06 risk analysis | Deepen |
| Test Oracle Analysis | Introduced | Lesson 06 | Lesson 06 §12a | Deepen in 06F |
| Model-Based Testing | Deferred | — | — | After state model matures |
| Property-Based Testing | Deferred | — | — | Future |
| Metamorphic Testing | Deferred | — | — | Future |
| Differential Testing | Deferred | — | — | Future |
| Exploratory charters | Introduced | Lesson 06 | Charters listed in lesson | Practice one charter |
| API documentation smells | Deferred | Sprint 15b | — | Future |
| API coverage beyond code coverage | Deferred | Sprint 14b | — | Future |

## 9. Security and Authorization Testing

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| 401 unauthenticated | Practiced | Phase 1 | `MerchantSecurityTest` | — |
| 403 forbidden | Practiced | Phase 1 | `MerchantSecurityTest` | — |
| masked 404 (cross-tenant) | Evidence Strong | Lesson 06 | `PaymentOrderSecurityTest` | — |
| role matrix | Evidence Strong | Lesson 06 | 7 roles × create/read | — |
| ownership (merchant_id) | Evidence Strong | Lesson 06 | `PaymentOrderSecurityTest` | — |
| tenant isolation | Evidence Strong | Lesson 06 | Cross-tenant tests | — |
| JWT claims | Introduced | Phase 1 | `TestJwtSupport` | Deepen in 06E |
| merchant_id claim | Evidence Strong | Lesson 06 | `KeycloakRealmRoleConverter` | — |
| BOLA (Broken Object Level Auth) | Evidence Strong | Lesson 10 | `PaymentOrderSummaryAuthorizationMatrixTest` | — |
| BFLA (Broken Function Level Auth) | Evidence Strong | Lesson 10 | `PaymentOrderSummaryAuthorizationMatrixTest` | — |
| mass assignment | Not Started | — | — | Security deep-dive |
| excessive data exposure | Not Started | — | — | Security deep-dive |
| OWASP API Top 10 | Deferred | — | — | Security review lesson |
| business flow abuse | Deferred | — | — | Sprint 9 |
| security tests vs UI hiding | Introduced | Lesson 06 | Frontend hides, backend enforces | Deepen in 06E |

## 10. Idempotency, Retry, Concurrency and Reliability

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| idempotent create | Evidence Strong | Lesson 06 | `PaymentOrderService.create()` | — |
| request fingerprint | Evidence Strong | Lesson 06 | `RequestFingerprint` | — |
| same key + same body → 200 replay | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| same key + different body → 409 | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| retry after timeout | Introduced | Lesson 06 | Idempotency deep dive | Concurrency test |
| duplicate request | Evidence Strong | Lesson 06 | Concurrency test | — |
| DB unique constraint as safety net | Evidence Strong | Lesson 06 | `V2__create_payment_orders.sql` | — |
| race condition | Introduced | Lesson 06 | `PaymentOrderIdempotencyConcurrencyTest` | Deepen |
| optimistic locking (version) | Deferred | Spec Kit 004+ | — | After lifecycle |
| ETag | Introduced | Lesson 06 | Controller headers | Deepen in 06C |
| If-Match | Deferred | Spec Kit 004+ | — | After lifecycle |
| stale update / 412 | Deferred | Spec Kit 004+ | — | After lifecycle |
| retry/backoff | Deferred | Future async sprint | — | After REST/HTTP hardening and lifecycle discovery |

## 11. Observability, CI Diagnostics and Flakiness

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| failure analysis: app bug vs test bug vs data bug vs env bug | Planned | Lesson 13 | Lesson 13 prompt/note | FailureAnalysisChecklist.md |
| correlation ID (X-Correlation-ID) | Introduced | Lesson 06 | `CorrelationIdFilter`, header tests | Add header assertion |
| trace ID | Deferred | Sprint 12b | — | Future |
| structured logs | Deferred | Sprint 12b | — | Future |
| failure-only logging | Practiced | Lesson 07 | `RestAssuredLoggingConfig` | — |
| secret masking (Authorization, tokens) | Planned | Lesson 11 | Lesson 11 prompt/note | blacklistHeader w RestAssuredLoggingConfig |
| CI diagnostics (GitHub Actions or similar) | Deferred | Sprint 12c | — | Future |
| flaky tests: identification and root cause | Planned | Lesson 13 | Lesson 13 prompt/note | FlakyTestDiagnosis.md |
| parallel-safe data: no ordered tests | Planned | Lesson 13 | Lesson 13 prompt/note | @Execution(CONCURRENT) + test data isolation |
| no shared mutable fixtures | Planned | Lesson 13 | Lesson 13 prompt/note | Thread-safe test support classes |
| polling / Awaitility for async | Planned | Lesson 13 | Lesson 13 prompt/note | Awaitility.await().atMost().untilAsserted() |
| log assertions | Planned | Lesson 13 | Lesson 13 prompt/note | ListAppender + log verification |
| evidence commands | Introduced | Lesson 06 | Verification commands section | Add to each lesson |

## 12. Service Virtualization and Contract Testing

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| WireMock basics | Deferred | Future async/contract sprint | — | After Lesson 10 REST/HTTP hardening |
| Pact / consumer-driven contracts | Deferred | Future contract testing sprint | — | After API contract documentation readiness |
| service virtualization concepts | Deferred | Future async/contract sprint | — | After Lesson 10 REST/HTTP hardening |

## 13. Frontend as API Consumer

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| Nuxt server proxy (API forwarding) | Practiced | Lesson 06 | `server/api/merchants/...` | — |
| Zod schema (input validation) | Practiced | Lesson 06 | `payment-order.schema.ts` | — |
| Pinia store (API state) | Practiced | Lesson 06 | `payment-orders.ts` | — |
| Vue components as API consumers | Practiced | Lesson 06 | `CreatePaymentOrderForm.vue` | — |
| stable idempotency key in frontend | Introduced | Lesson 06 | Form component | Deepen |
| Role-aware UI (hiding forbidden actions) | Introduced | Lesson 06 | Dashboard components | Add Playwright tests Sprint 7+ |
| Playwright authenticated user journeys | Planned | Sprint 7 | — | Role-aware E2E |

## 14. Spec Kit Learning Sprint Flow

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| when to use Spec Kit | Evidence Strong | Lesson 06 | Full Spec Kit for payment module | — |
| when NOT to use Spec Kit | Evidence Strong | Lesson 06 | Path A/B documented | — |
| spec.md creation | Practiced | Lesson 06 | `specs/003/.../spec.md` | — |
| plan.md creation | Practiced | Lesson 06 | `specs/003/.../plan.md` | — |
| tasks.md creation | Practiced | Lesson 06 | `specs/003/.../tasks.md` | — |
| data-model.md | Practiced | Lesson 06 | `specs/003/.../data-model.md` | — |
| contracts/ | Practiced | Lesson 06 | `payment-order-api.md` | — |
| checklists/ | Practiced | Lesson 06 | `requirements.md` | — |
| `/speckit.specify` workflow | Introduced | Lesson 06 | Spec creation | Deepen |
| `/speckit.plan` workflow | Introduced | Lesson 06 | Plan creation | Deepen |
| `/speckit.tasks` workflow | Introduced | Lesson 06 | Tasks creation | Deepen |
| `/speckit.implement` workflow | Introduced | Lesson 06 | Implementation | Deepen |

## Expert Gap Analysis Reference

Full expert gap analysis: [[Expert Gap Analysis - Senior SDET Coverage]]

## Navigation

- [[START HERE - Learning Dashboard]] — daily entry point
- [[Learning Progress Board]] — what's been covered
- [[Current Lesson]] — what to do NOW
- [[Senior SDET Competency Coverage Matrix]] — detailed per-competency status
