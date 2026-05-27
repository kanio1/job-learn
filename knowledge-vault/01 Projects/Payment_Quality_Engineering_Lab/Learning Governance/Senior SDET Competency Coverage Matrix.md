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
| Java 25 | `BigDecimal` for money | Not Started | none | Payment Order amount model |
| Java 25 | `java.time` | Introduced | merchant timestamps | Assert timestamp contract without brittle exact times |
| Java 25 | streams / extracting data | Introduced | list extraction in `MerchantRestAssuredTest` | Replace brittle list checks with clear AssertJ extraction |
| Java 25 | deprecated APIs / JDK warning hygiene | Not Started | none | Add build hygiene lesson/checklist |
| HTTP | method semantics safe/idempotent | Introduced | REST Assured lessons | Payment idempotency and retry tests |
| HTTP | `Location` header | Not Started | none | `POST payment-orders` returns Location |
| HTTP | `X-Correlation-ID` | Introduced | `CorrelationIdFilter`, Lesson 4 | Add REST Assured header tests |
| HTTP | `Idempotency-Key` | Not Started | strategy/prompt only | Payment Order create sprint |
| HTTP | `ETag` / `If-Match` / `412` | Not Started | prompt only | Optimistic concurrency sprint |
| HTTP | `Retry-After` / `429` | Not Started | none | Rate limit / abuse-flow sprint |
| HTTP | `WWW-Authenticate` | Not Started | security behavior exists but not asserted | Security response header tests |
| REST | resource modeling | Introduced | Merchant resource | PaymentOrder resource design |
| REST | error representation | Practiced | `ErrorResponse`, exception handler | Stable error schema + schema validation |
| REST | OpenAPI contract analysis | Not Started | none | Add OpenAPI spec and contract review |
| REST Assured | basic request/response DSL | Practiced | `StatusRestAssuredTest`, `MerchantRestAssuredTest` | Move to framework architecture |
| REST Assured | path/body/header basics | Practiced | Lessons 3-5 | Stop repeating after Lesson 6 |
| REST Assured | `RequestSpecification` | Introduced | `MerchantApiTestSupport` returns `RequestSpecification` | `RequestSpecBuilder` with role specs |
| REST Assured | `ResponseSpecification` | Not Started | none | Common success/error specs |
| REST Assured | failure-only logging | Not Started | none | Add masked CI diagnostics config |
| REST Assured | secret/header masking | Not Started | none | Blacklist `Authorization` in logs |
| REST Assured | schema validation | Not Started | none | JSON schema or OpenAPI-backed validation |
| REST Assured | object mapping / typed extraction | Introduced | map extraction only | typed DTO extraction with `TypeRef` |
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
| Security | BOLA / object ownership | Not Started | none | Merchant user cannot access another merchant |
| Security | OWASP API Top 10 abuse cases | Introduced | roadmap only | Add security test matrix note |
| SQL/PostgreSQL | FK / unique / check constraints | Practiced partly | merchant schema/repository tests | Payment constraints + idempotency uniqueness |
| SQL/PostgreSQL | optimistic locking | Introduced | merchant version in plan | ETag/If-Match sprint |
| SQL/PostgreSQL | audit/status history | Not Started | none | payment_order_status_history |
| Test Design | BVA/EP validation | Practiced | merchant validation tests | Parameterize and map to requirements |
| Test Design | decision tables | Introduced | Lesson prompts | Security/transition decision tables |
| Test Design | state transitions | Practiced | merchant lifecycle | Payment lifecycle state model |
| Test Design | test pyramid / level selection | Introduced | roadmap | Evidence per sprint in tracker |
| Frontend | Nuxt dashboard | Introduced | existing/planned merchant UI | Payment order pages |
| Playwright | authenticated user journeys | Introduced/Planned | roadmap | Role-aware merchant/payment flows |

## Review Cadence

Update this matrix:

- after every Lesson prompt creation,
- after every Spec Kit feature,
- after implementation,
- after test automation,
- before moving to the next sprint.

Each update should link to `Lesson Evidence Tracker.md`.
