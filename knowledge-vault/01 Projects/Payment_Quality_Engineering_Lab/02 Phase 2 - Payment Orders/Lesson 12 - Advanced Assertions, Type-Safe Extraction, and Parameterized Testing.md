---
type: lesson
status: planned
project: Payment Quality Engineering Lab
phase: 2
lesson: 12
area: Payment Orders
module: Advanced Assertions, Type-Safe Extraction, and Parameterized Testing
date: 2026-06-04
tags:
  - lesson
  - lesson-12
  - payment-quality-lab
  - assertj
  - rest-assured
  - junit
  - parameterized-tests
  - senior-sdet
---

# Lesson 12 - Advanced Assertions, Type-Safe Extraction, and Parameterized Testing

> **Status:** PLANNED - revised precision assertions slice after Lesson 11
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Current Sprint]] | [[Lesson Evidence Tracker]]
>
> **Main decision:** Lesson 12 is not a new payment feature. It makes the existing Payment Order REST tests more precise, typed, data-driven and readable after Lesson 11 introduces API clients, builders and reusable error specs.

## 1. Cel Lekcji

Lekcja 12 odpowiada na pytanie:

**Jak pisać testy REST Assured i AssertJ tak, żeby były precyzyjne, typowane, data-driven i odporne na przypadkowe dane testowe?**

Po Lessons 06-10 umiemy już tworzyć realne REST Assured tests dla create/read/list/summary, HTTP edge cases i authorization matrix. Lesson 11 ma dołożyć framework maturity: API clients, builders, error specs, secret masking i organizację testów. Lesson 12 idzie krok dalej, ale bez rozszerzania produktu:

- dobiera właściwą strategię ekstrakcji odpowiedzi: wrapper DTO vs `jsonPath().getList(...)` vs `TypeRef<T>`,
- używa GPath tylko tam, gdzie nadal pracujemy na surowym JSON,
- używa AssertJ po deserializacji: `filteredOn`, `extracting`, `tuple`, `allSatisfy`, `satisfiesExactlyInAnyOrder`, `usingRecursiveComparison`, `SoftAssertions`,
- projektuje parametryzowane testy z deterministycznymi danymi per case,
- porządkuje aktualne ryzyka w warstwie testowej bez dodawania lifecycle, PSP, Kafka ani nowych endpointów.

## 2. Warunek Startu

Lesson 12 ma sens **po Lesson 11**.

Przed implementacją sprawdź, czy istnieją lub zostały świadomie zastąpione:

| Expected Lesson 11 artifact | Why Lesson 12 needs it |
|---|---|
| `PaymentOrderApi.java` / `MerchantApi.java` or equivalent | Parametryzowane testy nie powinny powtarzać raw REST Assured setupu w każdej iteracji. |
| `PaymentOrderBuilder.java` / `MerchantBuilder.java` or equivalent | Każdy `@MethodSource` case potrzebuje czytelnego, izolowanego test data setupu. |
| `PaymentErrorSpecs.java` or equivalent | Walidacja błędów w wielu cases ma mieć jeden oracle. |
| `RestAssuredLoggingConfig` with secret masking | Więcej testów oznacza więcej logowania; tokeny i idempotency keys nie mogą wyciekać. |

Jeśli Lesson 11 nie jest ukończona, wykonaj najpierw [[Prompt - Lesson 11 - REST Assured Framework Architecture and Test Organization]].

## 3. Co Budujemy / Co Ćwiczymy

### Core system/test batch

| Batch | Scope | Expected outcome |
|---|---|---|
| 12A | Test-suite hygiene preflight | Usunięte lub świadomie oznaczone fałszywe przykłady, duplikaty i podejrzane list testy. |
| 12B | Type-safe extraction choices | Test pokazuje wrapper DTO extraction, typed content extraction i kiedy `TypeRef<T>` ma sens. |
| 12C | GPath vs typed AssertJ | Testy pokazują granicę: GPath dla surowego JSON, AssertJ dla typowanych obiektów. |
| 12D | Advanced AssertJ assertions | `usingRecursiveComparison`, `SoftAssertions`, `filteredOn`, `extracting`, `tuple`, domain assertions. |
| 12E | Parameterized validation/list/security-adjacent checks | `@MethodSource` i `@CsvSource` z per-case merchant/test data ownership. |

Default implementation: **12A + 12B + 12C + 12D + 12E**.

Nie tworzymy `PaymentOrderPerformanceTest` jako core lesson. Response-time assertions są awareness/deferred, bo vault governance odkłada performance/load testing.

## 4. Learning Delta Względem Lessons 06-11

| Temat | Lesson 06-11 | Lesson 12 |
|---|---|---|
| Response extraction | `extract().as(Class)` i `extract().path(...)` | Decision: wrapper DTO vs `jsonPath().getList("content", T.class)` vs `TypeRef<T>` |
| GPath | Basic `body(path, matcher)` | Controlled `findAll`, indexing, deep scan only with deterministic data |
| AssertJ collections | Basic `extracting`, `filteredOn` introduced | `tuple`, `allSatisfy`, `anySatisfy`, `satisfiesExactlyInAnyOrder` |
| Object comparison | Field-by-field assertions | `usingRecursiveComparison` with business-relevant ignored fields |
| Multi-fact assertions | First failure stops the test | `SoftAssertions` for aggregate/summary diagnostics |
| Parameterized testing | Lesson 10 authorization matrix | Validation/list matrix with isolated data per row |
| Test architecture | Lesson 11 clients/builders/specs | Data-driven tests reuse framework instead of raw chains |

## 5. Code Map

### Existing backend tests and support

| File | Current learning value | Lesson 12 use |
|---|---|---|
| `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java` | List/filter/query param coverage and typed wrapper extraction | Main place for typed content extraction, GPath vs AssertJ comparison and list parameterization. |
| `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java` | Summary contract over aggregate response | Good candidate for `SoftAssertions` and recursive/tuple checks over aggregate sections. |
| `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java` | Create/read/idempotency and error contract basics | Good candidate for validation `@MethodSource` and custom error assertions. |
| `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java` | Strong existing parameterized authorization matrix | Use as style reference, not as a place to add unrelated cases. |
| `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderAssertions.java` | Existing custom assertions for list responses | Extend toward domain-specific assertions only if it reduces duplication. |
| `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java` | Token/claim matrix support | Reuse for security-adjacent checks; do not add new roles. |

### Frontend and full-system context reviewed

Lesson 12 is backend-test focused, but it must respect what the frontend consumes:

| Area | Current state | Lesson 12 implication |
|---|---|---|
| Nuxt payment list/summary | `apps/frontend/app/stores/payment-orders.ts` and Zod schemas parse list/summary | Backend test assertions should protect the same response shape the UI consumes. |
| Payment create/detail pages | Implemented, but create/detail are less aligned with dashboard shell than list page | Frontend alignment is a next system batch, not core Lesson 12. |
| Playwright payment tests | Happy/empty/forbidden/backend-unavailable states exist | Do not duplicate UI checks in REST Assured; keep API oracle at backend layer. |
| Keycloak/OIDC | Backend resource-server tests use test JWT support; frontend proxy forwards token server-side | Lesson 12 may deepen role/claim test design, but no complete OIDC integration. |

## 6. Type-Safe Extraction Decision Table

| Response shape | Preferred extraction | Why |
|---|---|---|
| Single object, e.g. payment detail | `extract().as(PaymentOrderResponse.class)` | Direct and typed. |
| Wrapper object, e.g. list endpoint | `extract().as(PaymentOrderListResponse.class)` | The API returns `content`, `page`, `size`, `totalElements`, `totalPages`; wrapper is the contract. |
| Only the `content` array from wrapper | `response.jsonPath().getList("content", PaymentOrderResponse.class)` | Correct way to teach typed list extraction for the real API shape. |
| Endpoint directly returns JSON array | `extract().as(new TypeRef<List<PaymentOrderResponse>>() {})` | Use `TypeRef<T>` only when the response body is actually generic or a nested generic value is extracted first. |
| Quick JSON predicate before DTO exists | GPath in `.body(...)` | Useful for protocol/shape characterization, but can become brittle. |
| Business assertion after deserialization | AssertJ over DTOs | More readable, type-safe and refactor-friendly. |

Important correction: the current list endpoint does **not** return `List<PaymentOrderResponse>` directly. It returns `PaymentOrderListResponse`. Lesson 12 must not teach `extract().as(new TypeRef<List<PaymentOrderResponse>>() {})` against the whole list endpoint response.

## 7. Assertion Strategy

### Use GPath when

- checking raw JSON path shape before deserialization,
- proving a REST Assured-specific feature,
- validating a small protocol-level fact,
- data order and values are deterministic.

### Prefer AssertJ when

- the response has been deserialized into records/DTOs,
- the assertion is business-level,
- you need meaningful failure messages,
- you want type safety and IDE refactoring support.

### Avoid

- `content[0]` or `content[-1]` unless explicit sort and deterministic seed data are present,
- `hasItems("PLN", "EUR", "USD")` unless the test created exactly those currencies,
- deep scan `..` when a precise path is available and clearer,
- performance thresholds in normal contract tests.

## 8. Parameterized Test Design

Each parameterized row must own its data.

| Pattern | Use in Lesson 12 | Rule |
|---|---|---|
| `@MethodSource` | Validation payloads and expected error messages | Best for builders, records and complex inputs. |
| `@CsvSource` | Simple query filters such as currency/count | Only if each row seeds its own merchant/orders. |
| `@EnumSource` | Only stable enum values currently supported by the system | Do not invent future statuses like `AUTHORIZED` or `CAPTURED`. |
| `@RepeatedTest` | Awareness only for idempotency/retry stability | Do not confuse repeated checks with data-driven coverage. |

Candidate row record:

```java
record ValidationCase(
    String displayName,
    CreatePaymentOrderRequest request,
    String expectedErrorFragment
) {}
```

## 9. Test Hygiene Preflight

Before adding Lesson 12 tests, inspect these findings:

| Finding | Action |
|---|---|
| `PaymentOrderListRestAssuredTest#listFilteredByStatusReturnsOnlyCreatedV2` appears suspicious because it seeds many orders but asserts a size that may not match default pagination. | Characterize and fix, disable with explanation, or replace with deterministic pagination assertion. |
| `MyPaymentOrderBusinessFlowRestAssuredTest` and `MyMerchantRestAssuredTest` look like personal/learning copies. | Do not delete blindly; verify references and purpose. If obsolete, remove or move through an explicit cleanup task. |
| `IdempotencyKeysCopy.java` duplicates `IdempotencyKeys.java`. | Verify if used. If not used, remove in a cleanup task to keep examples clean for learning. |
| `PaymentOrderSummaryAuthorizationMatrixTest` contains minor readability smells reported during review. | Clean only if touching the file for Lesson 12; do not refactor unrelated security behavior. |

## 10. Scope OUT

- No new production endpoint.
- No `POST /payments`.
- No payment lifecycle actions: authorize, capture, cancel, refund.
- No new payment statuses just to make tests more interesting.
- No PSP integration, PSP mock flow, Kafka, webhooks or async pipeline.
- No complete OAuth/OIDC integration.
- No complete business dashboard.
- No Pact/WireMock/OpenAPI automation in this lesson.
- No performance/load test suite as a core Lesson 12 deliverable.

## 11. Acceptance Criteria

1. Lesson 12 starts only after Lesson 11 framework artifacts exist or are consciously replaced.
2. At least one test demonstrates correct extraction of the real list wrapper contract.
3. At least one test demonstrates typed extraction of `content` as `List<PaymentOrderResponse>` without `List<Map>` casting.
4. At least one test compares GPath and AssertJ approaches and explains when each is appropriate.
5. At least one summary/list test uses `SoftAssertions` for multiple independent facts.
6. At least one object/list assertion uses `usingRecursiveComparison`, `tuple`, `allSatisfy`, or `satisfiesExactlyInAnyOrder` where it improves readability.
7. At least one validation or filter test uses `@ParameterizedTest` with per-case test data ownership.
8. No test relies on non-deterministic order or shared mutable data.
9. Existing Lesson 10 HTTP edge and authorization matrix tests still pass.
10. `PaymentModuleTest` still passes.
11. Vault evidence is updated after implementation.

## 12. Verification Commands

Run from `apps/backend` after implementation:

```bash
./mvnw -Dtest=PaymentOrderListRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest test
./mvnw -Dtest=PaymentOrderRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest,PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

If Lesson 12 creates a dedicated class, also run:

```bash
./mvnw -Dtest=PaymentOrderParameterizedTest test
```

Frontend verification is required only if frontend files change:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts payment-order-create.spec.ts payment-order-read.spec.ts
```

## 13. Next System/Test Batch After Lesson 12

Recommended next batch, depending on what Lesson 12 reveals:

| Option | Why | Scope |
|---|---|---|
| 13A - Spring testing layers and reliability | Natural continuation after advanced API assertions | MockMvc/WebMvcTest, test isolation, concurrency awareness, surefire/failsafe. |
| 12F - Frontend consumer alignment | Frontend has real gaps that are educational but separate from backend assertions | Dashboard shell for create/detail, typed detail parsing, Pinia action ownership, Playwright route/back-link checks. |
| 12G - Payment Order List contract and authorization matrix | Backend summary is hardened; list endpoint has the next API-risk surface | Pagination boundaries, invalid filters, sort allowlist, 401/403/200 list matrix. |

Recommended order: **finish Lesson 11 -> implement revised Lesson 12 -> choose 12G or 12F based on learner priority -> then Lesson 13 reliability**.

## 14. Interview Answer EN

> Lesson 12 was designed as a precision testing slice, not a product feature. The key decision was to match assertion technique to response shape: wrapper DTO extraction for paged responses, typed `content` extraction for list elements, GPath only for raw JSON checks, and AssertJ for business-level assertions after deserialization. I also used parameterized tests with isolated data per row, which makes coverage broader without creating flaky shared-state tests. This is senior SDET thinking: precision, determinism, maintainability and clear test oracles.
