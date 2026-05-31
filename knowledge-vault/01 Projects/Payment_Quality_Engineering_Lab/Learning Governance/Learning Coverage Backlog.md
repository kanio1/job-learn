---
type: tracker
status: active
date: 2026-05-30
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
| Accept | Not Started | — | — | Concept Lesson |
| content negotiation (415, 406) | Planned | Sprint 11b | — | After lifecycle |
| ETag | Introduced | Lesson 06 | `PaymentOrderController` | Deepen in 06C |
| If-Match / 412 | Deferred | Spec Kit 004+ | — | After lifecycle actions exist |
| Idempotency-Key | Evidence Strong | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| Retry-After / 429 | Deferred | Future | — | Rate limit sprint |
| WWW-Authenticate | Not Started | — | — | Concept Lesson or deferred |
| X-Correlation-ID | Introduced | Lesson 06 | `CorrelationIdFilter`, header tests | Add RA header test exercise |
| malformed JSON | Not Started | — | — | HTTP deep-dive |
| unsupported media type | Not Started | — | — | Sprint 11b |
| API error contract (stable codes) | Evidence Strong | Lesson 06 | `PaymentExceptionHandler`, `PaymentErrorResponse` | — |

## 2. REST Assured Fundamentals

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| what REST Assured is | Practiced | Lesson 02 | Prompt, docs | — |
| `given()` | Practiced | Lesson 02 | All RA tests | — |
| `when()` | Practiced | Lesson 02 | All RA tests | — |
| `then()` | Practiced | Lesson 02 | All RA tests | — |
| `contentType()` | Introduced | Lesson 02 | RA docs | Deepen in 06A |
| `accept()` | Not Started | — | — | Concept Lesson |
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
| schema validation basics | Deferred | Sprint 10b | — | After API stabilizes |

## 3. REST Assured Framework Architecture

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| raw test before helper | Practiced | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| RequestSpecification | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| ResponseSpecification | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| RequestSpecBuilder | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| ResponseSpecBuilder | Practiced | Lesson 07 | `PaymentOrderListApiTestSupport` | — |
| auth specs | Not Started | — | — | 06H |
| JSON specs | Not Started | — | — | 06H |
| API clients (wrapper pattern) | Not Started | — | — | 06H |
| fixtures | Not Started | — | — | 06H |
| test data builders | Not Started | — | — | 06H or Sprint 8b |
| scenario flows (multi-step tests) | Introduced | Lesson 06 | Create → Get flow tests | Deepen in 06H |
| error assertions | Practiced | Lesson 06 | `PaymentOrderRestAssuredTest` | — |
| AssertJ integration after extract | Introduced | Lesson 06 | Service/repo tests | Deepen in 06F |
| logging only on validation failure | Practiced | Lesson 07 | `RestAssuredLoggingConfig` | — |
| masking Authorization | Not Started | — | — | 06H |
| parallel-safe test data | Introduced | Lesson 06 | `uniqueIdempotencyKey()` | Deepen in Sprint 8b |

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
| Map.copyOf / immutability | Not Started | — | — | Concept Lesson |
| Optional | Introduced | Phase 1 | Repository lookups | Deepen |
| Streams | Introduced | Phase 1 | list extraction in RA tests | Deepen in 06F |
| UUID | Practiced | Lesson 06 | `@PathVariable UUID` | — |
| java.time (Instant, LocalDateTime) | Introduced | Phase 1 | Merchant timestamps | Deepen in 06F |
| annotations (@Test, @Entity, @Valid) | Practiced | Phase 1 | All code | — |
| generics | Not Started | — | — | Concept Lesson |
| deprecated API awareness | Not Started | — | — | Build hygiene lesson |
| JDK warnings | Not Started | — | — | Build hygiene lesson |
| Mockito javaagent | Not Started | — | — | Concept Lesson |
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
| recursive comparison | Introduced | Lesson 07 | Lesson note | Practice exercise |
| soft assertions | Introduced | Lesson 07 | Lesson note | Practice exercise |
| parameterized tests | Introduced | Lesson 07 | `PaymentOrderListRestAssuredTest` example | Practice exercise |
| @Nested / @Tag | Introduced | Lesson 07 | Lesson note mentions | Practice exercise |
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
| COUNT, SUM | Planned | Lesson 08 | Lesson 08 summary plan | Implement summary endpoint and aggregation tests |
| GROUP BY | Planned | Lesson 08 | Lesson 08 summary plan | Implement currency/status breakdowns |
| CTE | Not Started | — | — | Sprint 7b |
| window functions | Not Started | — | — | Sprint 8 |
| EXPLAIN | Planned | Lesson 08 | Lesson 08 summary plan | Run EXPLAIN exercise after query exists |
| transactions | Introduced | Lesson 06 | `@Transactional` in `PaymentOrderService` | Deepen in Sprint 8b |
| isolation levels | Not Started | — | — | Sprint 8b |
| locking | Not Started | — | — | Sprint 8b |
| optimistic locking (version) | Planned | Spec Kit 004+ | — | After lifecycle |
| idempotency constraints | Evidence Strong | Lesson 06 | Unique constraint + service logic | — |
| Flyway migrations | Evidence Strong | Lesson 06 | `V2__create_payment_orders.sql` | — |
| Testcontainers PostgreSQL | Practiced | Lesson 06 | `PostgresContainerSupport` | — |
| DB as test oracle | Planned | Lesson 08 | Lesson 08 summary plan | Use DB/repository oracle for aggregation query if needed |
| API vs DB verification | Planned | Lesson 08 | Lesson 08 summary plan | Decide API vs DB oracle per aggregation risk |
| SQL as diagnostic tool | Lesson Created | Lesson 06D | `Lesson 06D - SQL and Flyway Constraints for Payment Orders` | Practice during Lesson 6 SQL session |
| test data isolation | Planned | Lesson 08 | Controlled aggregation seed plan | Implement per-test merchant aggregation dataset |
| parallel execution safety | Planned | Lesson 08 | Controlled aggregation seed plan | Keep summary tests independent |

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
| `@WebMvcTest` / MockMvc | Not Started | — | — | If useful for focused controller tests |
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
| BOLA (Broken Object Level Auth) | Introduced | Lesson 06 | Cross-tenant read test | Deepen in 06E |
| BFLA (Broken Function Level Auth) | Introduced | Lesson 06 | Role × action matrix | Deepen in 06E |
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
| retry/backoff | Deferred | Sprint 10+ | — | Webhook sprint |

## 11. Observability, CI Diagnostics and Flakiness

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| failure analysis: app bug vs test bug vs data bug vs env bug | Not Started | — | — | 06G Concept Lesson |
| correlation ID (X-Correlation-ID) | Introduced | Lesson 06 | `CorrelationIdFilter`, header tests | Add header assertion |
| trace ID | Deferred | Sprint 12b | — | Future |
| structured logs | Deferred | Sprint 12b | — | Future |
| failure-only logging | Not Started | — | — | 06H Concept Lesson |
| secret masking (Authorization, tokens) | Not Started | — | — | 06H Concept Lesson |
| CI diagnostics (GitHub Actions or similar) | Deferred | Sprint 12c | — | Future |
| flaky tests: identification and root cause | Deferred | Sprint 12c | — | Future |
| parallel-safe data: no ordered tests | Introduced | Lesson 06 | `uniqueIdempotencyKey()` | Deepen in Sprint 8b |
| no shared mutable fixtures | Introduced | Lesson 06 | Per-test data creation | Deepen in Sprint 8b |
| polling / Awaitility for async | Deferred | Sprint 10+ | — | Webhook sprint |
| evidence commands | Introduced | Lesson 06 | Verification commands section | Add to each lesson |

## 12. Service Virtualization and Contract Testing

| Topic | Status | First lesson/sprint | Evidence | Next action |
|---|---|---|---|---|
| WireMock basics | Deferred | Sprint 10 | — | Webhook sprint |
| Pact / consumer-driven contracts | Deferred | Sprint 10+ | — | Future |
| service virtualization concepts | Deferred | Sprint 10 | — | Webhook sprint |

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
