# Implementation Plan: Payment Orders Frontend Consumer and Contract Alignment

**Branch**: `006-payment-orders-frontend-consumer` | **Date**: 2026-05-31 | **Spec**: [spec.md](./spec.md)

**Input**: Lesson 09 light Spec Kit specification. Implement a typed Nuxt Dashboard consumer for existing payment order list and summary APIs.

## Summary

Lesson 09 adds a frontend consumer slice over existing backend payment order APIs:

- Nuxt server proxy for payment order list.
- Nuxt server proxy for payment order summary.
- Zod response schemas and inferred TypeScript types.
- Typed Pinia payment store.
- Minimal merchant-scoped payments panel with summary cards and list table.
- Playwright UI state tests.
- Backend regression verification using existing REST Assured and Modulith tests.

No backend lifecycle behavior, new status, DB migration, new Keycloak role, PSP integration, Kafka/webhook flow, or fake dashboard analytics are introduced.

## Technical Context

**Frontend**: Nuxt 4.4.6, Nuxt UI 4.7.1, Vue 3.5, Pinia 3, Zod 4.4.3, TypeScript 6.0.3, Playwright 1.60.

**Backend Context**: Spring Boot 4.0.6 / Spring Framework 7 / Java 25 backend already exposes list and summary endpoints.

**Testing**: `corepack pnpm typecheck`, targeted Playwright test, existing backend REST Assured regression, `PaymentModuleTest`.

**Storage**: No DB change.

**Constraints**: Existing API contracts only; backend remains security source of truth; frontend must not calculate summary from list data.

## Constitution Check

| Principle | Status | Plan Response |
|---|---|---|
| Tester-Led Product Learning | PASS | Lesson 09 teaches frontend consumer contracts, UI state tests, and assertion ownership between REST Assured and Playwright. |
| Spec-Driven Delivery | PASS | `spec.md` defines actors, scope, requirements, acceptance criteria, tests, DoD and non-goals. |
| Modular Monolith Boundaries | PASS | No backend module or boundary changes. Existing `PaymentModuleTest` remains verification. |
| Parallel-Ready Quality Engineering | PASS | Playwright uses route-mocked UI state coverage; backend tests retain per-test data ownership. |
| Security, Data Integrity, and Observability | PASS | Backend remains auth/ownership boundary; frontend handles `403` safely and forwards tokens through existing session pattern. |

## Project Structure

### Documentation

```text
specs/006-payment-orders-frontend-consumer/
├── spec.md
├── plan.md
└── tasks.md
```

### Source Code

```text
apps/frontend/
├── app/
│   ├── schemas/
│   │   └── payment-order.schema.ts
│   ├── stores/
│   │   └── payment-orders.ts
│   ├── components/payment/
│   │   ├── PaymentOrderSummaryCards.vue
│   │   └── PaymentOrderListTable.vue
│   └── pages/admin/merchants/[merchantId]/payments/
│       └── index.vue
├── server/api/merchants/[merchantId]/payment-orders/
│   ├── index.get.ts
│   └── summary.get.ts
└── tests/e2e/
    └── payment-orders-panel.spec.ts
```

## Architecture Decisions

### Frontend Server Proxy Boundary

Use Nuxt server routes to forward backend access tokens from the existing user session. Keep route behavior consistent with existing payment create/read proxies. Do not expose backend URL or access token to page code.

### Zod Schema Strategy

Keep create form schema and add response schemas for runtime contract checking and inferred TypeScript types. The schemas document what the UI depends on and reduce drift from backend DTO records.

### Pinia State Model

Use one payment order store for:

- request state: `loading`, `error`, `insufficientAuthority`,
- data state: `summary`, `list`, `currentOrder`, `lastCreatedOrder`,
- actions: `loadSummary`, `loadList`, `reset`, `clearError`.

### UI Component/Page Structure

`PaymentOrderSummaryCards.vue` renders only supported summary values. `PaymentOrderListTable.vue` renders existing order fields and detail links. The page coordinates loading, errors and data fetching.

### Playwright Test Strategy

Use route-mocked responses for UI states. Do not duplicate all REST Assured assertions. Test UI decisions: visible summary/list data, empty state, `403` state, and no data rendered on forbidden.

### Backend Regression Guardrails

Run existing backend package, Modulith and REST Assured regression tests. No backend code should change by default.

## Verification Commands

Backend:

```bash
cd apps/backend
./mvnw -DskipTests package
./mvnw -Dtest=PaymentModuleTest test
./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
```

Frontend:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## Phase 2: Task Planning Handoff

Generate tasks for frontend implementation and verification only. Backend changes are out of scope unless required to fix a regression directly discovered during verification.
