---
type: lesson
status: planned
area: Payment Quality Engineering Lab - Phase 2
lesson: 12
module: Precision Assertions, Data-Driven Testing, and Contract Verification
date: 2026-06-04
tags:
  - business-logic
  - precision-assertions
  - data-driven-testing
  - contract-verification
  - lesson-12
  - senior-sdet
---

# Lesson 12 - Precision Assertions, Data-Driven Testing, and Contract Verification

> **Evidence link:** revised target evidence after Lesson 11: typed extraction/list tests, advanced AssertJ assertions, parameterized validation/filter tests.
>
> **Navigation:** [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]] | [[Lesson Evidence Tracker]] | [[Current Sprint]]

## 1. Cel Lekcji

Zrozumieć, jak wybrać właściwy oracle i technikę asercji dla istniejącego PayU-like Payment Order API.

Ta lekcja nie dodaje nowej funkcjonalności płatniczej. Jej wartość biznesowa polega na tym, że testy mają szybciej wykrywać regresje w kontrakcie, autoryzacji, danych listy i agregacji, bez produkowania fałszywych lub flaky sygnałów.

Core topics:

- precision assertions for existing list/summary/create responses,
- type-safe extraction for wrapper and nested list responses,
- GPath vs typed AssertJ decision-making,
- parameterized tests with deterministic test data,
- risk of over-assertion, flaky data rows and false performance gates.

Deferred topics:

- JSON Schema/OpenAPI automation,
- performance/load testing,
- complete contract testing with Pact/WireMock,
- new payment lifecycle behavior.

## 2. Prerequisites

- Lessons 06-10: create/read, list/filter, summary, frontend consumer and HTTP/security hardening.
- Lesson 11: API clients, builders, reusable error specs and secret masking.
- Existing API shape: list endpoint returns `PaymentOrderListResponse`, not a raw JSON array.
- Existing status model: payment orders are currently foundation-only and use `CREATED`.

## 3. Code Reading Map

| File | Business/test decision |
|---|---|
| `PaymentOrderListRestAssuredTest.java` | List/filter contract, wrapper extraction, query parameter behavior. |
| `PaymentOrderSummaryRestAssuredTest.java` | Aggregate oracle for total amount, total orders, by-currency and by-status sections. |
| `PaymentOrderRestAssuredTest.java` | Create/read/idempotency and validation error contract. |
| `PaymentOrderSummaryAuthorizationMatrixTest.java` | Existing strong example of parameterized matrix design. |
| `PaymentOrderAssertions.java` | Candidate for reusable domain assertions when they improve readability. |
| `apps/frontend/app/schemas/payment-order.schema.ts` | Consumer contract reminder: backend test assertions protect what Nuxt/Zod expects. |

## 4. Decision Table - Extraction Strategy

| Scenario | Approach | Why |
|---|---|---|
| Single DTO response | `extract().as(PaymentOrderResponse.class)` | Direct and type-safe. |
| Paged/list wrapper response | `extract().as(PaymentOrderListResponse.class)` | Preserves full contract: `content`, paging and totals. |
| Content array inside wrapper | `jsonPath().getList("content", PaymentOrderResponse.class)` | Teaches typed list extraction for the real API shape. |
| Direct raw JSON array endpoint | `extract().as(new TypeRef<List<T>>() {})` | Correct use of `TypeRef<T>` when the whole response is generic. |
| Quick JSON structure check | REST Assured GPath | Good before/despite DTO extraction, but keep it focused. |
| Business rule over typed objects | AssertJ over records/DTOs | Better failure messages and refactoring support. |

## 5. Decision Table - Assertion Strategy

| Scenario | Best assertion | Risk if wrong |
|---|---|---|
| Many independent summary facts | `SoftAssertions` | First failure hides the rest of the aggregate diagnosis. |
| Comparing expected DTO to actual DTO | `usingRecursiveComparison()` with ignored technical fields | Field-by-field assertions become noisy or miss new fields. |
| List contains exact expected business rows | `extracting(...).containsExactlyInAnyOrder(tuple(...))` | Order-dependent tests become flaky. |
| Every returned order must satisfy a rule | `allSatisfy` | Manual loops produce weaker failure messages. |
| Some returned order must match a condition | `anySatisfy` / `filteredOn` | GPath-only checks hide type mistakes after deserialization. |
| Error response contract | reusable error spec/custom assert | Inline assertions drift across tests. |

## 6. Decision Table - Data-Driven Testing

| Approach | Use now | Rule |
|---|---|---|
| `@MethodSource` | Validation cases with request builders | Each case owns merchant, token and request body. |
| `@CsvSource` | Simple list filter cases, e.g. currency/count | Seed deterministic data inside each row. |
| `@EnumSource` | Only current, stable enum values | Do not add future statuses to production/test data. |
| `@RepeatedTest` | Awareness for idempotency stability | Not a replacement for parameterized input coverage. |
| Dynamic tests | Deferred | Too much framework complexity for this lesson. |

## 7. Risk Notes

### 7.1 Wrong `TypeRef<T>` Teaching

**Risk:** The test uses `extract().as(new TypeRef<List<PaymentOrderResponse>>() {})` against an endpoint that returns a wrapper object.

**Impact:** Learner gets a false mental model and the test either fails or encourages a fake endpoint shape.

**Mitigation:** Use `PaymentOrderListResponse` for the whole response, and use `jsonPath().getList("content", PaymentOrderResponse.class)` for the nested list.

### 7.2 Brittle GPath Examples

**Risk:** Assertions like `content[0]`, `content[-1]`, `hasItems("PLN", "EUR", "USD")` depend on order/data that the test did not create.

**Mitigation:** Create explicit data per test and set explicit sorting, or move the assertion to typed AssertJ with order-insensitive checks.

### 7.3 Over-Assertion

**Risk:** A test asserts timestamps, UUID formats, order of rows or internal fields that are not relevant to the business rule.

**Mitigation:** Assert business-relevant fields; ignore generated/technical fields in recursive comparison.

### 7.4 Flaky Parameterized Rows

**Risk:** Parameterized iterations share merchant/order data or static state.

**Mitigation:** Each iteration creates its own merchant, token and order set. Do not use mutable static fixtures.

### 7.5 Performance False Gates

**Risk:** Response-time assertions fail because of Testcontainers, CI CPU, IO or network jitter.

**Mitigation:** Keep performance as awareness/deferred. If used later, mark with `@Tag("performance")` and run separately from contract tests.

### 7.6 Frontend/API Contract Drift

**Risk:** Backend tests assert details that do not protect what the Nuxt/Zod consumer uses, while missing the shape the UI depends on.

**Mitigation:** Cross-check payment order response schemas in frontend, but keep API assertions in backend REST tests.

## 8. Learning Delta - What Is New vs Lessons 06-11

| Topic | Earlier lessons | Lesson 12 |
|---|---|---|
| Wrapper contract | Used in list tests | Explicit extraction decision and consumer alignment. |
| GPath | Basic paths | Controlled advanced use with deterministic setup. |
| AssertJ | Basic DTO/list assertions | Recursive comparison, soft assertions, tuple/allSatisfy/anySatisfy. |
| Parameterized tests | Authorization matrix in Lesson 10 | Validation/filter matrix with data isolation discipline. |
| Test oracle thinking | Introduced in Lesson 06/08 | Explicit choice between JSON path, DTO assertion, DB oracle and UI assertion. |

## 9. Questions

1. Why is `PaymentOrderListResponse` the right extraction target for the whole list endpoint?
2. When is `TypeRef<List<T>>` correct, and when is it misleading?
3. What does GPath give you before DTO extraction?
4. Why should business assertions usually move to AssertJ after deserialization?
5. How do you make a `@ParameterizedTest` row parallel-safe?
6. What should be ignored in recursive comparison for created payment orders?
7. Why are performance thresholds not part of the normal Lesson 12 contract test suite?
8. How do backend REST tests protect the frontend Zod/Pinia consumer without becoming Playwright tests?

### Answers

1. The list endpoint returns a wrapper with `content` and pagination metadata, so extracting only a raw list would ignore part of the API contract.
2. `TypeRef<List<T>>` is correct for a direct JSON array or a generic value already isolated from the wrapper. It is misleading for a wrapper object response.
3. GPath lets REST Assured assert raw JSON paths without creating DTOs, useful for quick shape/protocol checks.
4. AssertJ over DTOs is type-safe, easier to refactor and usually gives clearer failure messages for business facts.
5. Create fresh merchant/order data per row and avoid mutable static shared state.
6. Generated IDs, timestamps and other technical metadata unless they are the subject of the test.
7. Contract tests should be stable. Performance thresholds are environment-sensitive and belong to a separate tagged/baselined suite.
8. Assert the backend response shape and semantics that frontend schemas consume; leave rendering, loading and route behavior to Playwright.

## 10. Test Ideas

| Test idea | What it teaches |
|---|---|
| `listResponseExtractionPreservesWrapperContract` | Wrapper DTO is the API contract. |
| `listContentCanBeExtractedAsTypedOrders` | Nested list extraction without `List<Map>`. |
| `gpathAndAssertJFilterTheSameCurrencyRows` | Difference between JSON filtering and typed object filtering. |
| `summaryResponseReportsAllAggregateFactsTogether` | SoftAssertions for aggregate diagnostics. |
| `createPaymentOrderValidationCases` | `@MethodSource` with request builders and expected error fragments. |
| `listPaymentOrdersByCurrencyCases` | `@CsvSource` with deterministic per-row data. |

## 11. Related Notes

- [[Lesson 10 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 11 - Business Logic, Decision Tables, and Risk Notes]]
- [[Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing]]
- [[Senior SDET Competency Coverage Matrix]]
