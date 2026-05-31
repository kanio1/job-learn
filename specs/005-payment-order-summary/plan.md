# Implementation Plan: Payment Order Aggregation Summary

**Branch**: `005-payment-order-summary` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/005-payment-order-summary/spec.md`. Lesson 08 adds a system-only read summary endpoint on the existing payment order resource. Test implementation is explicitly out of scope for this planning slice.

## Summary

Lesson 08 adds a read-only Payment Order Summary endpoint that lets merchant payment readers and platform payment readers retrieve totals and grouped breakdowns for a merchant's existing payment orders.

The implementation stays inside the existing `payment` Spring Modulith module and adds:

- `GET /api/merchants/{merchantId}/payment-orders/summary`.
- Response DTO records for `totalOrders`, `totalAmountMinor`, `byCurrency[]`, and `byStatus[]`.
- Query request record for optional `currency`, `status`, `fromDate`, and `toDate` filters.
- Application service with `@Transactional(readOnly = true)`.
- Repository-level PostgreSQL aggregation using `COUNT(*)`, `SUM(amount_minor)`, `GROUP BY currency`, and `GROUP BY status`.
- Security matcher and controller ownership enforcement consistent with the existing list endpoint.
- `X-Correlation-ID` response header and existing validation/error handling patterns.

No new module, public module API, table, role, lifecycle action, PSP integration, frontend dashboard, or test implementation is introduced in this slice.

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**: Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6, Spring Data JPA, Spring Security Resource Server, Flyway, Maven 3.9.11

**Storage**: PostgreSQL 18 via existing `payment_orders` table; no new table by default

**Testing**: No new tests in this system-only slice. Verification uses compile/package plus existing `PaymentModuleTest` architecture verification.

**Target Platform**: Linux server/local lab

**Project Type**: Modular monolith backend service with existing Nuxt frontend outside this slice

**Performance Goals**: Merchant-scoped summary should remain sub-second for local-lab data volumes and avoid loading full payment order collections into application memory.

**Constraints**: Read-only; existing `payment` module only; no new roles; no new status values; no lifecycle actions; no PSP/Kafka/webhooks; no frontend dashboard; aggregation must happen in PostgreSQL, not Java iteration.

**Scale/Scope**: One backend endpoint, one request DTO, one response DTO with nested/group DTOs, one service, repository aggregation methods/projections, one security matcher, no new test classes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Plan Response |
|---|---|---|
| Tester-Led Product Learning | PASS | The feature explicitly exposes aggregation risks: null-to-zero totals, grouped rows, controlled seed data, API vs DB oracle, `EXPLAIN` diagnostics, and tenant-safe reporting. Test implementation is deferred but tester-facing conditions are captured in spec, data model, and quickstart. |
| Spec-Driven Delivery | PASS | `spec.md` contains business purpose, actors, scope, functional/non-functional requirements, acceptance scenarios, assumptions, success criteria, and clarified decisions. No `NEEDS CLARIFICATION` markers remain. |
| Modular Monolith Boundaries | PASS | Existing `payment` module owns summary behavior. No new module, no public module API, no cross-module dependency, no events. Architecture verification remains `PaymentModuleTest`. |
| Parallel-Ready Quality Engineering | PASS | No new tests are implemented in this slice. Future tests will use per-test merchants and controlled seed data; this plan keeps summary endpoint stateless and read-only. |
| Security, Data Integrity, and Observability | PASS | Reuses `merchant:payments:read` / `platform:payments:read`, enforces `merchant_id` claim for merchant readers, returns `403` for cross-tenant summary, uses read-only transactions, preserves stable validation errors, and returns `X-Correlation-ID`. |

No constitution violations require complexity tracking.

## Project Structure

### Documentation (this feature)

```text
specs/005-payment-order-summary/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── payment-order-summary-api.md
├── checklists/
│   └── requirements.md
└── tasks.md                         # Created later by /speckit.tasks
```

### Source Code (repository root)

```text
apps/backend/
├── src/main/java/lab/paymentquality/payment/internal/
│   ├── application/
│   │   └── PaymentOrderSummaryService.java        # New: read-only orchestration
│   ├── infrastructure/
│   │   ├── JpaPaymentOrderRepository.java         # Modify: aggregation query methods/projections
│   │   └── PaymentOrderSpecification.java         # Reuse existing filter parsing concepts only if useful
│   └── web/
│       ├── PaymentOrderController.java            # Modify: add summary endpoint
│       ├── PaymentOrderSummaryRequest.java        # New: optional query filters
│       └── PaymentOrderSummaryResponse.java       # New: response and grouped totals records
├── src/main/java/lab/paymentquality/shared/security/
│   └── SecurityConfig.java                        # Modify: add summary matcher before wildcard GET matcher
└── src/main/resources/db/migration/payment/
    └── V4__*.sql                                  # Not planned initially; add only if EXPLAIN justifies it
```

**Structure Decision**: Backend-only lesson extension in the existing modular monolith. All feature code remains under `apps/backend/src/main/java/lab/paymentquality/payment/internal` except the security matcher in shared security. No frontend files and no test files are planned for this system implementation slice.

## Complexity Tracking

No constitution violations. No complexity exceptions.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |

## Phase 0: Research

See [research.md](./research.md).

Research decisions resolve:

- Existing module ownership vs new module.
- Endpoint shape and collision avoidance with `/{paymentOrderId}`.
- DB aggregation query strategy.
- Optional filter scope.
- No new role decision.
- No V4 migration by default.
- No frontend changes in system slice.

## Phase 1: Design and Contracts

See:

- [data-model.md](./data-model.md)
- [contracts/payment-order-summary-api.md](./contracts/payment-order-summary-api.md)
- [quickstart.md](./quickstart.md)

### Design Summary

- `PaymentOrderSummaryRequest` holds optional filters: `currency`, `status`, `fromDate`, `toDate`.
- `PaymentOrderSummaryResponse` is a read-only DTO, not a persisted entity.
- `PaymentOrderSummaryService` performs authorization-agnostic read orchestration after the controller has enforced actor scope.
- Repository aggregation methods compute total, grouped-by-currency, and grouped-by-status values in PostgreSQL.
- Controller returns `200 OK` with `X-Correlation-ID`; no `ETag`.
- `SecurityConfig` adds an explicit summary matcher before the existing wildcard single-resource GET matcher.

### Post-Design Constitution Check

| Principle | Status | Evidence |
|---|---|---|
| Tester-Led Product Learning | PASS | `data-model.md` documents deterministic expected totals and null-to-zero risk. Contract documents status/security/error paths. `quickstart.md` includes compile/package/module verification. |
| Spec-Driven Delivery | PASS | All FR-301 through FR-326 map to planned DTO, service, repository, controller, security, validation, and docs artifacts. |
| Modular Monolith Boundaries | PASS | Design keeps code in `payment.internal.*` and `shared.security` only. No new public module API or cross-module dependency. |
| Parallel-Ready Quality Engineering | PASS | No test implementation. Future tests are designed around per-test merchants and controlled seed data, not shared fixtures. |
| Security, Data Integrity, and Observability | PASS | Contract captures `401`, `403`, `400`, `200`; plan preserves `X-Correlation-ID`; DB aggregation remains read-only and tenant-scoped. |

## Phase 2: Task Planning Handoff

`/speckit.tasks` should generate system implementation tasks only. Do not create REST Assured, Playwright, JUnit, or AssertJ test implementation tasks in the first task set.

Recommended task groups:

1. Response/request DTOs.
2. Repository aggregation projections and methods.
3. `PaymentOrderSummaryService`.
4. Controller endpoint and route ordering.
5. Security matcher ordering.
6. Compile/package/module verification.
7. Spec/vault evidence update.

## Verification Commands

Run from `apps/backend` after implementation:

```bash
./mvnw clean compile
./mvnw -DskipTests package
./mvnw -Dtest=PaymentModuleTest test
```

No new automated tests are expected in this system-only slice.
