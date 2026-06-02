# Implementation Plan: REST HTTP Contract Hardening and Authorization Matrix

**Branch**: `007-rest-http-contract-hardening-authorization-matrix` | **Date**: 2026-06-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/007-rest-http-contract-hardening-authorization-matrix/spec.md`. Lesson 10 adds backend/API test hardening over the existing Payment Order Summary endpoint. Production code changes are allowed only when tests expose a real bug.

## Summary

Lesson 10 strengthens the existing Payment Order Summary REST API contract through two focused test batches: HTTP edge contract tests (route collision, malformed UUID, unsupported methods, content negotiation, conditional headers) and a parameterized authorization matrix (12 rows covering 401, 403, 200 with explicit BOLA/BFLA labeling).

The implementation stays inside the existing `payment` Spring Modulith module and adds:

- `PaymentOrderSummaryHttpContractRestAssuredTest` — 5 HTTP edge contract tests.
- `PaymentOrderSummaryAuthorizationMatrixTest` — 1 parameterized test with 12 matrix rows.
- Optional `TestJwtSupport` extension for merchant reader without `merchant_id` claim.
- Production code changes only if characterization reveals a real contract bug.

No new module, public module API, endpoint, table, role, lifecycle action, PSP integration, frontend change, or business capability is introduced in this slice.

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6, Spring Data JPA, Spring Security Resource Server, REST Assured, JUnit 6, AssertJ, Testcontainers, Flyway, Maven 3.9.11

**Storage**: PostgreSQL 18 via existing `payment_orders` table; no new table

**Testing**: REST Assured integration tests with `@SpringBootTest(RANDOM_PORT)`, `@ParameterizedTest` / `@MethodSource` for authorization matrix, Testcontainers PostgreSQL for data isolation

**Target Platform**: Linux server/local lab

**Project Type**: Modular monolith backend service with existing Nuxt frontend outside this slice

**Performance Goals**: Not applicable — this is a test hardening slice, not a performance-sensitive feature

**Constraints**: Test-first; existing `payment` module only; no new roles; no new status values; no lifecycle actions; no PSP/Kafka/webhooks; no frontend changes; production code changes only when tests expose a real bug

**Scale/Scope**: Two new test classes, one optional test support extension, zero new production endpoints

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Plan Response |
|---|---|---|
| Tester-Led Product Learning | PASS | The feature explicitly exposes HTTP edge risks (content negotiation, unsupported methods, route ambiguity, conditional headers) and authorization policy risks (BOLA, BFLA). Test implementation is the primary deliverable. Tester-facing conditions are captured in spec, data model, and contracts. |
| Spec-Driven Delivery | PASS | `spec.md` contains business purpose, actors, scope, functional/non-functional requirements, acceptance scenarios, assumptions, success criteria, and clarified decisions. No `NEEDS CLARIFICATION` markers remain. |
| Modular Monolith Boundaries | PASS | Existing `payment` module owns summary behavior. No new module, no public module API, no cross-module dependency, no events. Architecture verification remains `PaymentModuleTest`. |
| Parallel-Ready Quality Engineering | PASS | Each test creates its own merchant and token. No shared mutable fixtures. Parameterized matrix rows use per-case token construction. Test data isolation follows existing `PostgresContainerSupport` pattern. |
| Security, Data Integrity, and Observability | PASS | Authorization matrix covers 401, 403, 200 with BOLA/BFLA labels. Token values are not exposed in test output. Error contract stability is verified. `X-Correlation-ID` propagation is tested. |

No constitution violations require complexity tracking.

## Project Structure

### Documentation (this feature)

```text
specs/007-rest-http-contract-hardening-authorization-matrix/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── summary-http-edge-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                         # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
apps/backend/
├── src/test/java/lab/paymentquality/
│   ├── rest/
│   │   └── PaymentOrderSummaryHttpContractRestAssuredTest.java   # New: HTTP edge contract tests
│   ├── security/
│   │   └── PaymentOrderSummaryAuthorizationMatrixTest.java       # New: parameterized auth matrix
│   └── testsupport/
│       └── TestJwtSupport.java                                   # Modify: add merchantPaymentReaderTokenWithoutMerchantIdClaim()
├── src/main/java/lab/paymentquality/payment/internal/web/
│   ├── PaymentOrderController.java                               # Modify only if tests expose route bug
│   └── PaymentExceptionHandler.java                              # Modify only if tests expose error contract bug
└── src/main/java/lab/paymentquality/shared/security/
    └── SecurityConfig.java                                       # Modify only if tests expose matcher ordering bug
```

**Structure Decision**: Test-first lesson extension in the existing modular monolith. Two new test classes in `apps/backend/src/test/java/lab/paymentquality/`. One optional test support extension in `TestJwtSupport`. Production code remains unchanged unless characterization reveals a real contract defect.

## Complexity Tracking

No constitution violations. No complexity exceptions.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

## Phase 0: Research

See [research.md](./research.md).

Research decisions resolve:

- Spring MVC default behavior for unsupported `Accept` header on `@RestController`.
- Spring MVC default behavior for unmapped HTTP methods (`PUT`, `PATCH`, `DELETE`) on a `@GetMapping` URI.
- Spring MVC behavior for `HEAD` and `OPTIONS` on a `@GetMapping` URI.
- `MethodArgumentTypeMismatchException` handling for malformed UUID path variables.
- Route resolution order: literal `/summary` vs wildcard `/{paymentOrderId}`.
- `If-None-Match` behavior when no `ETag` is returned.
- `TestJwtSupport` gap analysis for merchant reader without `merchant_id` claim.

## Phase 1: Design and Contracts

See:

- [data-model.md](./data-model.md)
- [contracts/summary-http-edge-api.md](./contracts/summary-http-edge-api.md)
- [quickstart.md](./quickstart.md)

### Design Summary

- `PaymentOrderSummaryHttpContractRestAssuredTest` contains 5 tests covering route collision, malformed UUID, unsupported methods, unsupported accept, and conditional headers.
- `PaymentOrderSummaryAuthorizationMatrixTest` uses `@ParameterizedTest` with `@MethodSource` to express 12 authorization rows as test cases.
- Each matrix row is a record: `SummaryAccessCase(String displayName, String tokenSupplier, String targetMerchantIdSupplier, int expectedStatus, String bolaBflaLabel)`.
- `TestJwtSupport` gains `merchantPaymentReaderTokenWithoutMerchantIdClaim()` if the existing helper cannot express that case.
- Production code remains unchanged unless characterization reveals a real defect.

### Post-Design Constitution Check

| Principle | Status | Evidence |
|---|---|---|
| Tester-Led Product Learning | PASS | `data-model.md` documents matrix row structure and seed data. Contract documents HTTP edge expectations. `quickstart.md` includes characterization-first workflow. |
| Spec-Driven Delivery | PASS | All FR-401 through FR-424 map to planned test classes and matrix rows. |
| Modular Monolith Boundaries | PASS | Design keeps test code in `lab.paymentquality.rest` and `lab.paymentquality.security`. No new public module API or cross-module dependency. |
| Parallel-Ready Quality Engineering | PASS | Each test creates its own merchant. Parameterized rows use per-case token construction. No shared mutable state. |
| Security, Data Integrity, and Observability | PASS | Matrix captures 401, 403, 200 with BOLA/BFLA labels. Token values are not logged. Error contract stability is verified. |

## Phase 2: Task Planning Handoff

`/speckit.tasks` should generate test implementation tasks. Production code tasks should be generated only as conditional follow-ups if characterization reveals bugs.

Recommended task groups:

1. **Characterization** — run exploratory requests to document Spring MVC defaults for summary endpoint.
2. **HTTP Edge Contract Tests** — implement `PaymentOrderSummaryHttpContractRestAssuredTest` with 5 tests.
3. **Authorization Matrix Tests** — implement `PaymentOrderSummaryAuthorizationMatrixTest` with parameterized 12-row matrix.
4. **Test Support Extension** — add `merchantPaymentReaderTokenWithoutMerchantIdClaim()` to `TestJwtSupport` if needed.
5. **Regression Verification** — run existing summary/list/security tests, `PaymentModuleTest`, and package.
6. **Optional Aggregation Diagnostic** — repository/service-level test or `EXPLAIN` note (batch 10C).
7. **Vault Evidence Update** — update lesson note, evidence tracker, current lesson, current sprint, coverage backlog.

## Verification Commands

Run from `apps/backend` after implementation:

```bash
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

Frontend verification only if frontend files change:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```
