# Tasks: Payment Orders Frontend Consumer and Contract Alignment

**Input**: `specs/006-payment-orders-frontend-consumer/spec.md` and `plan.md`

**Tests**: Playwright UI state tests are in scope. Backend REST Assured tests are regression guardrails only.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files.
- **[Story]**: `US1` for summary/list panel, `US2` for forbidden/security UI state.
- All tasks include exact file paths.

---

## Phase 1: Review Existing Contracts and Frontend Patterns

- [x] T001 [AGENT-REVIEW] Review backend response DTOs in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderResponse.java`, `PaymentOrderListResponse.java`, and `PaymentOrderSummaryResponse.java`.
- [x] T002 [AGENT-REVIEW] Review existing frontend payment schemas/store/routes in `apps/frontend/app/schemas/payment-order.schema.ts`, `apps/frontend/app/stores/payment-orders.ts`, and `apps/frontend/server/api/merchants/[merchantId]/payment-orders/`.
- [x] T003 [AGENT-REVIEW] Review existing Nuxt UI and Playwright patterns in `apps/frontend/app/pages/admin/merchants.vue`, `apps/frontend/app/components/merchant/MerchantTable.vue`, and `apps/frontend/tests/e2e/merchant-support.ts`.

---

## Phase 2: Add Zod Schemas and TypeScript Types

- [x] T004 [P] [US1] [AGENT-IMPLEMENT] Extend `apps/frontend/app/schemas/payment-order.schema.ts` with `paymentOrderResponseSchema`, `paymentOrderListResponseSchema`, `paymentOrderSummaryResponseSchema`, `backendErrorSchema`, and inferred types.

---

## Phase 3: Add Nuxt Server Proxy Routes

- [x] T005 [P] [US1] [AGENT-IMPLEMENT] Add `apps/frontend/server/api/merchants/[merchantId]/payment-orders/index.get.ts` to forward list query parameters and backend access token.
- [x] T006 [P] [US1] [AGENT-IMPLEMENT] Add `apps/frontend/server/api/merchants/[merchantId]/payment-orders/summary.get.ts` to forward summary query parameters and backend access token.

---

## Phase 4: Update Typed Pinia Store

- [x] T007 [US1] [AGENT-IMPLEMENT] Update `apps/frontend/app/stores/payment-orders.ts` to remove `any` for list, summary, current order, and last created order, and add `loadList`, `loadSummary`, `reset`, and `clearError` actions; update `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue` to parse create responses before storing them.

---

## Phase 5: Add UI Components

- [x] T008 [P] [US1] [AGENT-IMPLEMENT] Add `apps/frontend/app/components/payment/PaymentOrderSummaryCards.vue` to render total orders, total amount minor, currency breakdown, and status breakdown.
- [x] T009 [P] [US1] [AGENT-IMPLEMENT] Add `apps/frontend/app/components/payment/PaymentOrderListTable.vue` to render order rows, status, amount/currency, dates, and detail links with an empty state.

---

## Phase 6: Add Merchant-Scoped Payments Panel Page

- [x] T010 [US1] [AGENT-IMPLEMENT] Add `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` to load summary and list, render loading/empty/backend-unavailable states, and use the new components; move the merchant registry to `apps/frontend/app/pages/admin/merchants/index.vue` so nested payment routes resolve correctly.
- [x] T011 [US2] [AGENT-IMPLEMENT] Ensure `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` renders a `403` insufficient-authority state and clears payment data when forbidden.

---

## Phase 7: Add Playwright UI State Tests

- [x] T012 [US1] [TESTER-AUTOMATE] Add `apps/frontend/tests/e2e/payment-orders-panel.spec.ts` test for summary and list rendering from mocked responses.
- [x] T013 [US1] [TESTER-AUTOMATE] Add empty merchant state test in `apps/frontend/tests/e2e/payment-orders-panel.spec.ts`.
- [x] T014 [US2] [TESTER-AUTOMATE] Add forbidden state, backend-unavailable state, and no-data-rendering tests in `apps/frontend/tests/e2e/payment-orders-panel.spec.ts`.

---

## Phase 8: Backend Regression Verification

- [x] T015 [AGENT-REVIEW] Run `./mvnw -DskipTests package` from `apps/backend`.
- [x] T016 [AGENT-REVIEW] Run `./mvnw -Dtest=PaymentModuleTest test` from `apps/backend`.
- [x] T017 [AGENT-REVIEW] Run `./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` from `apps/backend`.

---

## Phase 9: Frontend Verification

- [x] T018 [AGENT-REVIEW] Run `corepack pnpm typecheck` from `apps/frontend`.
- [x] T019 [AGENT-REVIEW] Run `corepack pnpm test:e2e -- payment-orders-panel.spec.ts` from `apps/frontend`.

---

## Phase 10: Vault Evidence Update

- [x] T020 [AGENT-EXPLAIN] Update `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment.md` with implemented files, tests and command results.
- [x] T021 [AGENT-EXPLAIN] Update `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md` with Lesson 09 evidence.
- [x] T022 [AGENT-EXPLAIN] Update `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md` and `Current Sprint.md` with final Lesson 09 state.

---

## Dependencies

- Phase 1 before implementation.
- T004 before T007-T010.
- T005-T006 before T007 page data loading verification.
- T008-T009 before T010.
- T010-T011 before Playwright tests.
- Verification before vault evidence finalization.

## Guardrails

- Do not add backend lifecycle behavior.
- Do not add new payment statuses.
- Do not add DB migrations.
- Do not add new Keycloak roles.
- Do not add fake analytics or unsupported payment actions.
