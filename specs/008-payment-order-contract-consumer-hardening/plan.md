# Implementation Plan: Payment Order Contract and Consumer Hardening

**Branch**: `008-payment-order-contract-consumer-hardening` | **Date**: 2026-06-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/008-payment-order-contract-consumer-hardening/spec.md`. This is a production/system hardening slice over existing payment order behavior. It is not a lesson implementation and does not create new automated tests.

## Summary

This slice hardens the existing merchant-scoped payment order API and Nuxt dashboard consumer so later testing work can exercise stable production contracts instead of defining behavior inside tests.

The implementation will make list query validation effective, stabilize create protocol errors, preserve existing authorization/data boundaries, and centralize frontend create/detail API behavior in the payment orders store.

No new payment business capability is added. No new endpoint, lifecycle action, status, PSP integration, Kafka/webhook/outbox flow, Keycloak role, database migration, dashboard KPI, REST Assured class, Playwright spec, or test-support abstraction is planned.

## Technical Context

| Area | Decision |
|---|---|
| Backend | Java 25, Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6 |
| Frontend | Nuxt 4.4.6, Nuxt UI 4.7.1, Pinia 3, Zod 4.4.3, TypeScript 6 |
| Database | PostgreSQL 18 through existing `merchants`, `payment_orders`, idempotency and status-history structures |
| Security | Spring Security Resource Server with Keycloak-style JWT realm roles and `merchant_id` claim |
| Primary backend scope | Existing `/api/merchants/{merchantId}/payment-orders` create/list/read/summary surfaces |
| Primary frontend scope | Existing `/admin/merchants/{merchantId}/payments` list/create/detail routes |
| Testing deliverables | None. Existing tests may be run only as regression verification. |
| New business behavior | None |
| New database migration | None by default |
| New Keycloak realm/roles | None |

## Constitution Check

| Principle | Status | Plan Response |
|---|---|---|
| Tester-Led Product Learning | PASS | The feature creates stable contracts for later tester learning, while this slice remains production implementation only. |
| Spec-Driven Delivery | PASS | `spec.md` captures resolved backend, database, security, frontend, non-goal and acceptance requirements. No planning-critical clarification remains. |
| Modular Monolith Boundaries | PASS | Work stays inside existing payment web/application code, shared security review boundaries, database migrations review, and the existing frontend dashboard area. |
| Parallel-Ready Quality Engineering | PASS | No new shared mutable state, global fixtures, test-only hooks or data ownership changes are introduced. |
| Security, Data Integrity, and Observability | PASS | Backend remains authorization source of truth, merchant ownership remains enforced, error envelopes become more consistent, and correlation behavior is preserved where already owned by payment controllers. |

No constitution violation requires complexity tracking.

## Project Structure

### Documentation

```text
specs/008-payment-order-contract-consumer-hardening/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── design.md
├── prompt-phase-1-specify.md
├── contracts/
│   └── payment-order-contract-hardening.md
└── checklists/
    └── requirements.md
```

### Backend Source Scope

```text
apps/backend/src/main/java/lab/paymentquality/
├── payment/internal/web/
│   ├── PaymentOrderController.java
│   ├── PaymentOrderListRequest.java
│   ├── PaymentExceptionHandler.java
│   └── PaymentErrorResponse.java
├── payment/internal/application/
│   └── PaymentOrderListService.java
└── shared/security/
    ├── SecurityConfig.java                # review only unless direct regression appears
    └── KeycloakRealmRoleConverter.java    # expected unchanged
```

### Frontend Source Scope

```text
apps/frontend/
├── app/pages/admin/merchants/[merchantId]/payments/
│   ├── index.vue                          # existing dashboard shell reference
│   ├── new.vue                            # align dashboard shell/back link/create flow
│   └── [paymentOrderId].vue               # align dashboard shell/back link/detail flow
├── app/components/payment/
│   └── CreatePaymentOrderForm.vue         # keep form-local state; stop direct store API-state mutation
├── app/stores/
│   └── payment-orders.ts                  # own create/detail API calls and normalized error state
└── app/schemas/
    └── payment-order.schema.ts            # parse create/detail responses and backend error shapes
```

### Database And Keycloak Scope

```text
apps/backend/src/main/resources/db/migration/
├── merchant/V1__create_merchants.sql      # unchanged
└── payment/
    ├── V2__create_payment_orders.sql      # unchanged
    └── V3__add_payment_order_list_indexes.sql # unchanged

infra/keycloak/                            # unchanged
```

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| N/A | N/A | N/A |

## Phase 0: Research

See [research.md](./research.md).

Resolved planning decisions:

- Malformed JSON returns `400` with `error=malformed_json`.
- Unsupported media type returns `415` with `error=unsupported_media_type`.
- Cross-field list validation lives at the request/application boundary before repository query execution.
- Payment detail `404` remains a local dashboard resource-not-found state.
- Merchant Zod schemas are deferred unless merchant consumer code is touched.
- Supported sort remains `createdAt,asc` and `createdAt,desc` only.
- Validation messages describe the allowed contract and avoid echoing raw rejected input values.

## Phase 1: Design And Contracts

See:

- [data-model.md](./data-model.md)
- [contracts/payment-order-contract-hardening.md](./contracts/payment-order-contract-hardening.md)
- [quickstart.md](./quickstart.md)

### Backend Design Summary

- Bind list query parameters through `@Valid @ModelAttribute PaymentOrderListRequest` or an equivalent validated request model, not through a manual construction path that bypasses Bean Validation.
- Preserve list defaults: `page=0`, `size=20`, `sort=createdAt,desc`.
- Reject invalid page, size, status, currency, date, amount range and sort values before repository query execution.
- Keep page beyond last page as a successful empty page.
- Add explicit JSON consumption to payment order create.
- Handle unreadable JSON as `400 malformed_json` and unsupported media type as `415 unsupported_media_type` through payment-owned error response semantics.
- Keep missing `Idempotency-Key` as a stable validation response.
- Preserve successful create/replay behavior, including existing `Location`, `ETag`, `X-Correlation-ID`, ownership and idempotency semantics.

### Frontend Design Summary

- Render payment create/detail pages inside the dashboard layout.
- Point create/detail back links to `/admin/merchants/{merchantId}/payments`.
- Move detail loading and create API calls into `usePaymentOrdersStore`.
- Parse detail/create responses with `paymentOrderResponseSchema` before exposing them to UI state.
- Normalize `403`, detail `404`, backend unavailable, malformed backend response and create failure states with dashboard-consistent alerts.
- Keep create form responsible for form field state only, not store-level API loading/error mutation.

### Database And Security Design Summary

- Do not add a database migration by default.
- Keep existing payment order constraints and V3 list indexes.
- Keep list query range validation in request/application code, not database constraints.
- Do not add Keycloak roles or realm JSON changes.
- Keep backend authorization as source of truth.
- Preserve merchant reader/creator ownership checks and platform payment reader behavior.

### Post-Design Constitution Check

| Principle | Status | Evidence |
|---|---|---|
| Tester-Led Product Learning | PASS | Contracts and quickstart preserve a future Lesson 13 handoff without creating tests now. |
| Spec-Driven Delivery | PASS | All acceptance criteria in `spec.md` map to implementation groups below. |
| Modular Monolith Boundaries | PASS | No new module, event, public module API or cross-module dependency is planned. |
| Parallel-Ready Quality Engineering | PASS | No new shared test fixtures, global frontend state or data ownership changes are planned. |
| Security, Data Integrity, and Observability | PASS | Security/no-migration decisions are explicit; protocol errors keep safe response shapes and correlation where payment advice owns the path. |

## Phase 2: Task Planning Handoff

`/speckit.tasks` should generate implementation-only tasks.

Recommended task groups:

1. Backend list request binding and field validation.
2. Backend list cross-field validation before repository query execution.
3. Backend create JSON consumption and protocol error handling.
4. Backend payment error response consistency and safe validation messages.
5. Database no-change review against existing constraints and indexes.
6. Keycloak/security no-change review against existing roles and ownership policy.
7. Frontend create/detail dashboard shell and navigation alignment.
8. Frontend store-owned create/detail actions and Zod parsing.
9. Frontend dashboard alert normalization for `403`, detail `404`, backend unavailable, malformed backend response and create failure.
10. Regression verification using existing commands only.
11. Lesson 13 readiness handoff, confirming future tests can target stable behavior without adding test-only production hooks.

Do not generate tasks that create REST Assured classes, Playwright specs, test-support clients/builders/specs, mock PSP flows, Pact/WireMock/OpenAPI/JSON Schema automation, or test-only production code.

## Implementation Order

### Cut 1: Backend Production Contract

1. Make `PaymentOrderListRequest` effective as the validated request boundary.
2. Add request/application-level cross-field validation for date and amount ranges.
3. Keep sort allowlist to `createdAt,asc` and `createdAt,desc`.
4. Add explicit JSON `consumes` to create.
5. Add or adjust payment-owned handlers for malformed JSON, unsupported media type and missing `Idempotency-Key`.
6. Preserve existing create/list/read security, idempotency and response headers.
7. Run existing backend regression verification.

### Cut 2: Frontend Consumer Contract

1. Apply dashboard layout to payment create/detail pages.
2. Fix back links to the existing payment orders list route.
3. Move detail load into `usePaymentOrdersStore`.
4. Move create API call into `usePaymentOrdersStore`.
5. Parse create/detail responses through Zod before updating store state.
6. Normalize dashboard alert states and prevent stale protected/missing data rendering.
7. Run frontend typecheck and existing targeted Playwright regression only if frontend files changed.

### Cut 3: Boundary Review And Handoff

1. Confirm no new role, status, table, index, event, endpoint or lifecycle action was added.
2. Confirm no new automated test files or test-support abstractions were added.
3. Confirm Lesson 13 readiness notes remain future-facing and do not imply async/webhook/lifecycle behavior now.
4. Update learner-facing evidence only after implementation completes, if requested by that future task.

## Verification Commands

These commands verify existing suites after implementation. They are not instructions to write tests.

Backend existing regression verification:

```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest,PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

Frontend existing regression verification after frontend changes:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts payment-order-create.spec.ts payment-order-read.spec.ts payment-order-auth-deny.spec.ts
```

Plan-artifact verification:

```bash
git diff --check -- specs/008-payment-order-contract-consumer-hardening
```

## Deferred Scope And Risks

| Deferred item | Reason |
|---|---|
| New automated tests | Out of scope for this feature. Existing tests are regression verification only. |
| New DB migration | Existing schema and indexes are sufficient for the specified behavior. |
| New Keycloak roles/realm changes | Existing roles express current payment create/read/platform-reader behavior. |
| Payment lifecycle/status changes | Phase 0 guardrail and not part of this feature. |
| PSP/Kafka/webhook/outbox/event behavior | Phase 0 guardrail and future feature territory. |
| Complete OAuth/OIDC application integration | Explicitly out of scope. |
| Merchant Zod schemas | Deferred unless merchant consumer code is touched. |
| Fake dashboard KPIs | Would imply unsupported business behavior. |
| OpenAPI/Pact/WireMock/JSON Schema automation | Future contract/documentation work, not this implementation slice. |
| Lesson 13 executable test suite | Future quality-engineering work after this behavior is implemented. |

## Open Questions

No unresolved question blocks `/speckit.tasks`. All planning-critical clarifications are resolved in `spec.md` and summarized in [research.md](./research.md).
