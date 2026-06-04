# Tasks: Payment Order Contract and Consumer Hardening

**Input**: Design documents from `specs/008-payment-order-contract-consumer-hardening/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/payment-order-contract-hardening.md`, `quickstart.md`

**Tests**: No new test tasks are generated. The feature specification explicitly excludes new REST Assured tests, Playwright specs, test-support clients/builders/specs and test-only production hooks. Existing tests are used only as regression verification in the final phase.

**Organization**: Tasks are grouped by implementation story so each increment can be completed and verified independently.

## Phase 1: Setup (Shared Context)

**Purpose**: Establish scope, guardrails and current source ownership before implementation.

- [X] T001 [AGENT-IMPLEMENT] Review implementation scope and non-goals in specs/008-payment-order-contract-consumer-hardening/spec.md and specs/008-payment-order-contract-consumer-hardening/plan.md
- [X] T002 [P] [AGENT-IMPLEMENT] Review backend payment web/application files in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java, apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListRequest.java, apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java, and apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java
- [X] T003 [P] [AGENT-IMPLEMENT] Review frontend payment consumer files in apps/frontend/app/stores/payment-orders.ts, apps/frontend/app/schemas/payment-order.schema.ts, apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue, apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue, and apps/frontend/app/components/payment/CreatePaymentOrderForm.vue

---

## Phase 2: Foundational (Blocking Guardrails)

**Purpose**: Confirm implementation boundaries that must hold across every story.

**Critical**: No user story work should add new tests, new migrations, new Keycloak roles, new lifecycle behavior, PSP/Kafka/webhook/outbox flows or fake dashboard KPIs.

- [X] T004 [AGENT-IMPLEMENT] Confirm the implementation will not edit database migrations unless a real production data gap is discovered in apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql and apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql
- [X] T005 [P] [AGENT-IMPLEMENT] Confirm existing security role policy remains the authorization source of truth in apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java and apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java
- [X] T006 [P] [AGENT-IMPLEMENT] Confirm frontend work does not require merchant response Zod schemas unless merchant consumer code is touched in apps/frontend/app/schemas/payment-order.schema.ts and apps/frontend/app/schemas/merchant.schema.ts

**Checkpoint**: Backend and frontend implementation can start while preserving Phase 0 guardrails.

---

## Phase 3: User Story 1 - Backend List Query Contract (Priority: P1) MVP

**Goal**: Invalid payment order list query parameters are rejected as production validation behavior before repository query execution, while defaults, empty pages and existing authorization remain unchanged.

**Independent Test**: Verify manually or with existing regression only that omitted params use defaults, invalid page/size/status/currency/date/amount/sort fail as validation errors, page beyond last page succeeds with an empty page, and no new test files are added.

### Tests for User Story 1

No new test tasks. Existing regression verification is scheduled in the final phase only.

### Implementation for User Story 1

- [X] T007 [US1] [AGENT-IMPLEMENT] Change list endpoint binding to use PaymentOrderListRequest as the effective validated request boundary in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java
- [X] T008 [US1] [AGENT-IMPLEMENT] Adjust PaymentOrderListRequest validation constraints, defaults or validation messages for page, size, status, currency, date, amount and sort contract rules in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListRequest.java
- [X] T009 [US1] [AGENT-IMPLEMENT] Add request/application-level validation for fromDate <= toDate and minAmount <= maxAmount before repository query construction in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListRequest.java or apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java
- [X] T010 [US1] [AGENT-IMPLEMENT] Preserve list defaults page=0, size=20 and sort=createdAt,desc while keeping allowed sort values to createdAt,asc and createdAt,desc in apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java
- [X] T011 [US1] [AGENT-IMPLEMENT] Ensure invalid model binding, invalid query values and date parsing failures use safe payment validation responses in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java
- [X] T012 [US1] [AGENT-REVIEW] Verify list authorization and merchant/platform ownership behavior remains unchanged in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java and apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java

**Checkpoint**: Backend list query contract is independently implemented and ready for existing regression verification.

---

## Phase 4: User Story 2 - Backend Create Protocol And Error Contract (Priority: P1)

**Goal**: Payment order create explicitly consumes JSON and returns stable payment-owned errors for malformed JSON, unsupported media type and missing Idempotency-Key without changing idempotency, ownership or successful response headers.

**Independent Test**: Verify manually or with existing regression only that valid create/replay behavior still works, malformed JSON returns 400 malformed_json, unsupported media type returns 415 unsupported_media_type, and missing Idempotency-Key returns validation.

### Tests for User Story 2

No new test tasks. Existing regression verification is scheduled in the final phase only.

### Implementation for User Story 2

- [X] T013 [US2] [AGENT-IMPLEMENT] Add explicit JSON consumption to the payment order create mapping in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java
- [X] T014 [US2] [AGENT-IMPLEMENT] Add or adjust safe handling for HttpMessageNotReadableException with error=malformed_json in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java
- [X] T015 [US2] [AGENT-IMPLEMENT] Add or adjust safe handling for HttpMediaTypeNotSupportedException with error=unsupported_media_type in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java
- [X] T016 [US2] [AGENT-IMPLEMENT] Add or adjust safe handling for missing Idempotency-Key as a validation response in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java
- [X] T017 [US2] [AGENT-IMPLEMENT] Preserve successful create, idempotent replay, Location, ETag and X-Correlation-ID behavior in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java
- [X] T018 [US2] [AGENT-REVIEW] Verify error messages do not expose raw tokens, raw request bodies, stack traces or internal class names in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java and apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentErrorResponse.java

**Checkpoint**: Backend create protocol contract is independently implemented and ready for existing regression verification.

---

## Phase 5: User Story 3 - Frontend Dashboard Consumer Contract (Priority: P2)

**Goal**: Payment create/detail pages behave as coherent dashboard consumers with correct back links, store-owned API behavior, Zod parsing and consistent alert states.

**Independent Test**: Verify manually or with existing frontend regression only that create/detail pages render in the dashboard layout, back links return to payment orders list, detail/create API state comes from the store, and 403/detail 404/backend unavailable/create failure states render dashboard alerts without stale data.

### Tests for User Story 3

No new test tasks. Existing frontend typecheck and targeted Playwright regression are scheduled in the final phase only.

### Implementation for User Story 3

- [X] T019 [US3] [AGENT-IMPLEMENT] Add current detail state and loadDetail action with paymentOrderResponseSchema parsing to apps/frontend/app/stores/payment-orders.ts
- [X] T020 [US3] [AGENT-IMPLEMENT] Add createOrder action with paymentOrderResponseSchema parsing, idempotency-key header handling and lastCreatedOrder update to apps/frontend/app/stores/payment-orders.ts
- [X] T021 [US3] [AGENT-IMPLEMENT] Normalize payment store error handling for backendErrorSchema, insufficient authority, detail not found, backend unavailable and malformed backend response in apps/frontend/app/stores/payment-orders.ts
- [X] T022 [US3] [AGENT-IMPLEMENT] Render the payment detail route inside the dashboard layout, use store.loadDetail, remove direct $fetch/any state, and link back to /admin/merchants/{merchantId}/payments in apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue
- [X] T023 [US3] [AGENT-IMPLEMENT] Render the payment create route inside the dashboard layout and link back to /admin/merchants/{merchantId}/payments in apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue
- [X] T024 [US3] [AGENT-IMPLEMENT] Refactor create form to keep only form-local state and call store.createOrder without direct store loading/error mutation in apps/frontend/app/components/payment/CreatePaymentOrderForm.vue
- [X] T025 [US3] [AGENT-IMPLEMENT] Keep payment order response and backend error parsing centered on existing schemas without touching merchant schemas unless required in apps/frontend/app/schemas/payment-order.schema.ts
- [X] T026 [US3] [AGENT-REVIEW] Verify no unsupported payment lifecycle actions, fake KPIs or merchant detail route assumptions were introduced in apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue and apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue

**Checkpoint**: Frontend consumer contract is independently implemented and ready for existing regression verification.

---

## Phase 6: User Story 4 - Boundary Review And Lesson 13 Handoff (Priority: P3)

**Goal**: Confirm implementation remains within Phase 0 guardrails and leaves stable behavior for future Lesson 13 testing/reliability work without adding test-only production code.

**Independent Test**: Inspect changed files and run existing verification commands only; confirm no new test files, migrations, Keycloak roles, lifecycle statuses or unsupported integrations were added.

### Tests for User Story 4

No new test tasks. This story performs review and existing regression verification only.

### Implementation for User Story 4

- [X] T027 [US4] [AGENT-REVIEW] Verify no new backend test classes or test-support abstractions were added under apps/backend/src/test/java/lab/paymentquality
- [X] T028 [US4] [AGENT-REVIEW] Verify no new Playwright specs were added under apps/frontend/tests/e2e
- [X] T029 [US4] [AGENT-REVIEW] Verify no database migration was added or changed without documented production need under apps/backend/src/main/resources/db/migration
- [X] T030 [US4] [AGENT-REVIEW] Verify no Keycloak realm JSON, new role, lifecycle status, PSP, Kafka, webhook, outbox or event behavior was introduced under infra/keycloak, apps/backend/src/main/java/lab/paymentquality, and apps/frontend/app
- [X] T031 [US4] [AGENT-REVIEW] Confirm Lesson 13 readiness remains future-facing in specs/008-payment-order-contract-consumer-hardening/quickstart.md and specs/008-payment-order-contract-consumer-hardening/plan.md

**Checkpoint**: Feature remains implementation-only and ready for future testing work.

---

## Phase 7: Polish And Existing Regression Verification

**Purpose**: Run existing verification and final artifact checks without creating new tests.

- [X] T032 [AGENT-REVIEW] Run existing backend regression commands from apps/backend as listed in specs/008-payment-order-contract-consumer-hardening/quickstart.md
- [X] T033 [AGENT-REVIEW] Run frontend typecheck and existing targeted Playwright regression from apps/frontend as listed in specs/008-payment-order-contract-consumer-hardening/quickstart.md if frontend files changed
- [X] T034 [AGENT-REVIEW] Run git diff --check for specs/008-payment-order-contract-consumer-hardening, apps/backend, and apps/frontend from repository root
- [X] T035 [AGENT-REVIEW] Verify every acceptance criterion remains traceable to implementation or existing regression verification in specs/008-payment-order-contract-consumer-hardening/spec.md and specs/008-payment-order-contract-consumer-hardening/tasks.md

---

## Dependencies And Execution Order

### Phase Dependencies

- Phase 1 Setup has no dependencies.
- Phase 2 Foundational depends on Phase 1 and blocks all implementation stories.
- US1 Backend List Query Contract depends on Phase 2.
- US2 Backend Create Protocol And Error Contract depends on Phase 2 and can run in parallel with US1 after shared backend file coordination.
- US3 Frontend Dashboard Consumer Contract depends on Phase 2 and can run in parallel with backend stories.
- US4 Boundary Review depends on desired implementation stories being complete.
- Phase 7 Polish and Existing Regression Verification depends on desired implementation stories and US4 review.

### User Story Dependencies

- US1 (P1): Can start after Foundational; no dependency on frontend work.
- US2 (P1): Can start after Foundational; coordinates with US1 because both touch PaymentOrderController.java and PaymentExceptionHandler.java.
- US3 (P2): Can start after Foundational; no dependency on backend code changes except shared API contract assumptions.
- US4 (P3): Runs after implemented stories to confirm guardrails and handoff.

### Parallel Opportunities

- T002 and T003 can run in parallel during setup.
- T005 and T006 can run in parallel during foundational review.
- US1 and US2 can be split by file only after agreeing on PaymentOrderController.java and PaymentExceptionHandler.java edit order.
- US3 can run in parallel with backend work because it touches frontend files.
- T027, T028, T029, T030 and T031 can run in parallel after implementation.

---

## Parallel Examples

### Backend And Frontend Split

```text
Task: T007-T012 implement backend list query contract in apps/backend/src/main/java/lab/paymentquality/payment/internal
Task: T019-T026 implement frontend dashboard consumer contract in apps/frontend/app
```

### Boundary Review Split

```text
Task: T027 review backend test tree under apps/backend/src/test/java/lab/paymentquality
Task: T028 review frontend e2e tree under apps/frontend/tests/e2e
Task: T029 review migration tree under apps/backend/src/main/resources/db/migration
Task: T030 review guardrails under infra/keycloak, apps/backend/src/main/java/lab/paymentquality, and apps/frontend/app
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 Backend List Query Contract.
3. Stop and validate US1 independently with manual checks or existing regression only.

### Incremental Delivery

1. Complete US1 for effective list query validation.
2. Complete US2 for create protocol error hardening.
3. Complete US3 for frontend dashboard consumer hardening.
4. Complete US4 and Phase 7 verification.

### Guardrail Strategy

1. Do not add test files during any user story.
2. Do not add database migrations or Keycloak role changes unless implementation exposes and documents a real production need.
3. Do not add payment lifecycle, PSP, Kafka, webhook, outbox, event, async or fake dashboard behavior.
4. Use existing regression commands only after implementation.

## Summary

- Total tasks: 35
- US1 tasks: 6
- US2 tasks: 6
- US3 tasks: 8
- US4 tasks: 5
- Setup/foundational tasks: 6
- Polish/verification tasks: 4
- Suggested MVP scope: Phase 1, Phase 2, then US1 only
