---
type: tracker
status: active
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-27
tags:
  - competency-matrix
  - senior-sdet
  - java-25
  - rest-assured
  - assertj
  - security-testing
---

# Senior SDET Competency Coverage Matrix

Cel: pilnować, czy aplikacja i lekcje realnie wymuszają kompetencje potrzebne na mocniejsze stanowiska Java Backend QA Automation / SDET.

Statusy:

- `Not Started` - brak praktycznego pokrycia.
- `Introduced` - temat pojawił się koncepcyjnie lub w małym przykładzie.
- `Practiced` - temat był użyty w kodzie/testach.
- `Evidence Strong` - istnieje kod, testy, lesson note i potrafię wyjaśnić to interview-style.
- `Deferred` - świadomie później.

## Matrix

| Obszar | Kompetencja | Status | Evidence | Następny krok |
|---|---|---|---|---|
| Java 25 | records / DTOs | Practiced | `CreateMerchantRequest`, `MerchantResponse`, `ErrorResponse` | Payment DTOs + typed response extraction |
| Java 25 | enums / state machine | Practiced | `MerchantStatus`, lifecycle tests | PaymentOrderStatus with transition table |
| Java 25 | collections immutable / `Map.of` / `Map.copyOf` | Practiced | `createMerchantBody`, Lesson 5 | Test data builders for larger payloads |
| Java 25 | minor-unit money value object | Practiced | `PaymentAmount`, `PaymentOrderRestAssuredTest` | Later compare with `BigDecimal` when decimal input appears |
| Java 25 | `java.time` | Introduced | merchant timestamps | Assert timestamp contract without brittle exact times |
| Java 25 | streams / extracting data | Practiced | `PaymentOrderListRestAssuredTest` using typed extraction and AssertJ | Deepen with complex stream pipelines |
| REST Assured | `RequestSpecification` | Evidence Strong | `PaymentOrderListApiTestSupport` using `RequestSpecBuilder` | — |
| REST Assured | `ResponseSpecification` | Practiced | `successListSpec()` in `PaymentOrderListApiTestSupport` | Add error specs |
| REST Assured | object mapping / typed extraction | Practiced | `extract().as(PaymentOrderListResponse.class)` | TypeRef for generic types |
| AssertJ | basic assertions | Practiced | repository/service tests, list extraction | Custom assertions for error/payment response |
| AssertJ | `extracting`, `filteredOn`, `tuple` | Introduced | limited usage | Payment list/report tests |
| AssertJ | recursive comparison | Not Started | none | DTO comparison lesson |
| AssertJ | soft assertions | Not Started | none | Response object multi-field assertion |
| JUnit | basic `@Test` | Practiced | existing test suite | Keep as assumed knowledge |
| JUnit | parameterized tests | Not Started | none | Validation and decision-table tests |
| JUnit | `@Nested` / `@Tag` | Not Started | none | Organize security/payment lifecycle tests |
| Spring Testing | `@SpringBootTest` random port | Practiced | REST Assured tests | Keep for full HTTP contract |
| Spring Testing | `@DataJpaTest` / repository tests | Practiced | JPA repository tests | Add idempotency and constraints tests |
| Spring Testing | `@WebMvcTest` / MockMvc | Introduced/Unknown | controller tests need review | Use for focused error/controller tests if useful |
| Testcontainers | PostgreSQL containers | Practiced | `PostgresContainerSupport` | Worker-safe data strategy |
| Security | 401/403 auth matrix | Practiced | `MerchantSecurityTest` | Add ownership and tenant isolation |
| Security | BOLA / object ownership | Practiced | `PaymentOrderSecurityTest`, `PaymentOrderRestAssuredTest#crossTenantReadReturns404` | Extend with more ownership matrices in later slices |
| Security | OWASP API Top 10 abuse cases | Introduced | roadmap only | Add security test matrix note |
| SQL/PostgreSQL | FK / unique / check constraints | Practiced | `V2__create_payment_orders.sql`, `JpaPaymentOrderRepositoryTest` | Add reporting/query-plan exercises later |
| SQL/PostgreSQL | optimistic locking | Introduced | merchant version in plan | ETag/If-Match sprint |
| SQL/PostgreSQL | audit/status history | Practiced | `payment_order_status_history`, `PaymentOrderStatusHistory`, repository test | Expand when lifecycle transitions are introduced |
| Test Design | BVA/EP validation | Practiced | merchant validation tests | Parameterize and map to requirements |
| Test Design | decision tables | Introduced | Lesson prompts | Security/transition decision tables |
| Test Design | state transitions | Practiced | merchant lifecycle | Payment lifecycle state model |
| Test Design | test pyramid / level selection | Introduced | roadmap | Evidence per sprint in tracker |
| Frontend | Nuxt dashboard | Practiced | `CreatePaymentOrderForm.vue`, payment order detail page, Nuxt payment proxy | Add role-aware Playwright coverage later |
| Playwright | authenticated user journeys | Introduced/Planned | roadmap | Role-aware merchant/payment flows |
| Database Verification | DB as test oracle (when to verify via DB vs API) | Not Started | none | Decision framework lesson after Payment Order |
| Database Verification | CTE, window functions, EXPLAIN | Not Started | none | SQL deep-dive with payment data in sprints 7-8 |
| Database Verification | transaction isolation, deadlock, race condition | Introduced | idempotency concurrency test | Isolation level testing in lifecycle sprints |
| Test Data Management | data ownership, isolation, cleanup strategy | Introduced | per-test merchant creation, unique keys | Document strategy; add cleanup patterns |
| Test Data Management | parallel-safe test data design | Introduced | `Parallel Readiness Principles` note | Worker-namespaced data exercises |
| Assertion Strategy | RA vs AssertJ vs DB assertion decision | Not Started | none | Lesson 6 extension + deep-dive lesson |
| Failure Analysis | systematic test failure debugging | Not Started | none | Debugging exercises and checklist |
| Framework Architecture | RA spec builders, client evolution | Introduced | `MerchantApiTestSupport` returns spec | Evolution path note |
| API Contract | JSON Schema validation | Not Started | none | After API stabilizes |
| API Contract | documentation smells, OpenAPI compliance | Not Started | contract markdown only | Reviewer checklist extension |
| Observability | log assertions, metrics, traces | Not Started | correlation ID in headers only | Sprint 12+ observability lesson |
| HTTP Robustness | content negotiation, 415/406 | Not Started | none | HTTP deep-dive lesson |
| Performance-Light | response time, N+1, payload size | Not Started | none | After lifecycle |
| Flaky Test | diagnosis and CI failure triage | Not Started | none | Dedicated note + exercises |
| API Coverage | coverage beyond code coverage | Not Started | none | Quality metrics note |
| Negative-Path First | methodology for negative-first design | Introduced | comprehensive 400/401/403/404/409 tests | Explicit methodology exercise |
| Contract Testing | Pact, WireMock, service virtualization | Deferred | `wiremock/README.md` placeholder | Sprint 10+ webhook testing |
| Business-Readable Naming | `@DisplayName`, naming conventions | Introduced | technical test names exist | Add `@DisplayName` exercise + naming convention note |
| RA Advanced | GPath traps, JSON path, array indexing | Deferred | simple `.body(path, matcher)` used | When list endpoints exist |
| Observability | log/metric/trace assertions (not just existence) | Deferred | correlation ID in headers only | Sprint 12+ observability lesson |
| API Compat | backward compatibility, consumer-driven contracts | Deferred | no versioning strategy | Future API evolution lesson |
| LLM-Assisted Testing | using LLMs for test generation with human review gates | Deferred | none | Process note |
| OSS Review | framework teardown, learning from GitHub repos | Deferred | none | Future exercise |
| Reverse Engineering | DevTools traffic capture for API discovery | Not Started | none | Future note or exercise |
| Security | SQL injection and data security basics | Deferred | security matrix covers auth, not injection | Database security note in later sprint |
| Data Isolation | RLS / row-level security in PostgreSQL | Deferred | tenant isolation via claim, not RLS | Sprint 9 extension with RLS concept |

## Review Cadence

Update this matrix:

- after every Lesson prompt creation,
- after every Spec Kit feature,
- after implementation,
- after test automation,
- before moving to the next sprint.

Each update should link to `Lesson Evidence Tracker.md`.
