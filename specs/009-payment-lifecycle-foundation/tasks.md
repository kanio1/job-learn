# Tasks: Payment Lifecycle Foundation

**Input**: Design documents from `specs/009-payment-lifecycle-foundation/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/payment-lifecycle-api.md`, `quickstart.md`

**Tests**: New automated tests are intentionally omitted because `spec.md` marks new automated tests as out of scope for this feature. Tasks include tester analysis/design artifacts and regression verification using existing test commands.

**Organization**: Tasks are grouped by user story to enable independent implementation and review of each story after the shared foundation is complete.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm feature state, implementation scope, and existing project baselines before changing production code.

- [x] T001 [AGENT-IMPLEMENT] Confirm `.specify/feature.json` points to `specs/009-payment-lifecycle-foundation` before implementation work starts in `.specify/feature.json`
- [x] T002 [AGENT-IMPLEMENT] Review lifecycle API contract and align implementation notes in `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`
- [x] T003 [TESTER-ANALYZE] Capture Lesson 14 state-machine and risk-analysis notes for future tester work in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T004 [P] [AGENT-IMPLEMENT] Inspect existing backend payment module boundaries before editing in `apps/backend/src/main/java/lab/paymentquality/payment/package-info.java`
- [x] T005 [P] [AGENT-IMPLEMENT] Inspect existing frontend payment detail flow before editing in `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared lifecycle schema, domain vocabulary, security roles, error contracts, and service seams that all user stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T006 [AGENT-IMPLEMENT] Add V4 lifecycle migration expanding payment status constraints and lifecycle columns in `apps/backend/src/main/resources/db/migration/payment/V4__add_payment_lifecycle.sql`
- [x] T007 [AGENT-IMPLEMENT] Update the Keycloak realm seed with `merchant:payments:lifecycle`, `platform:payments:lifecycle`, and `platform:payments:audit` roles in `infra/keycloak/realms/payment-quality-realm.json`
- [x] T008 [AGENT-IMPLEMENT] Expand payment lifecycle statuses in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentStatus.java`
- [x] T009 [AGENT-IMPLEMENT] Extend payment order lifecycle fields and metadata storage in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T010 [AGENT-IMPLEMENT] Extend lifecycle history fields and transition factory methods in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrderStatusHistory.java`
- [x] T011 [AGENT-IMPLEMENT] Add lifecycle action enum for idempotency and audit mapping in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentLifecycleAction.java`
- [x] T012 [AGENT-IMPLEMENT] Add lifecycle transition validation helpers in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T013 [AGENT-IMPLEMENT] Extend idempotency fingerprint support for lifecycle actions in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/RequestFingerprint.java`
- [x] T014 [AGENT-IMPLEMENT] Review idempotency persistence constraints for multiple lifecycle actions per order in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaIdempotencyRecordRepository.java`
- [x] T015 [P] [AGENT-IMPLEMENT] Add `PspClient` interface for authorize, capture, void, and refund operations in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PspClient.java`
- [x] T016 [P] [AGENT-IMPLEMENT] Add always-success mock PSP implementation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/MockPspClient.java`
- [x] T017 [AGENT-IMPLEMENT] Add lifecycle request records for authorize, capture, cancel, refund, and metadata PATCH in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentLifecycleRequests.java`
- [x] T018 [AGENT-IMPLEMENT] Add lifecycle response and history response records in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentLifecycleResponses.java`
- [x] T019 [AGENT-IMPLEMENT] Add lifecycle error constants and HTTP mappings for 412 and 422 in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- [x] T020 [AGENT-IMPLEMENT] Update security matchers for lifecycle, metadata PATCH, and history endpoints in `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- [x] T021 [TESTER-DESIGN] Document the 6-status by 4-action lifecycle decision table in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T022 [AGENT-REVIEW] Verify Spring Modulith ownership remains inside the payment module in `apps/backend/src/test/java/lab/paymentquality/payment/PaymentModuleTest.java`

**Checkpoint**: Foundation ready. User story implementation can now begin in priority order or in parallel where file ownership does not conflict.

---

## Phase 3: User Story 1 - Authorize Payment Order (Priority: P1) MVP

**Goal**: Merchant lifecycle operator can transition a CREATED payment order to AUTHORIZED with expiration, idempotency replay, updated ETag, and correct authorization checks.

**Independent Test**: Create an order, authorize it with `Idempotency-Key` and `If-Match`, verify AUTHORIZED status, `authorizedAt`, `expiresAt`, ETag increment, `Cache-Control: no-store`, and idempotent replay behavior.

### Implementation for User Story 1

- [x] T023 [US1] [AGENT-IMPLEMENT] Create `PaymentLifecycleService` authorize workflow with transaction boundary in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T024 [US1] [AGENT-IMPLEMENT] Add authorize transition method setting `authorizedAt` and `expiresAt` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T025 [US1] [AGENT-IMPLEMENT] Add lifecycle idempotency reservation and replay handling for authorize in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T026 [US1] [AGENT-IMPLEMENT] Add authorize endpoint with `Idempotency-Key`, `If-Match`, ETag, no-store, and Vary headers in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T027 [US1] [AGENT-IMPLEMENT] Map authorize responses through existing payment response conventions in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderMapper.java`
- [x] T028 [US1] [AGENT-IMPLEMENT] Record AUTHORIZE lifecycle history entries in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T029 [P] [US1] [AGENT-IMPLEMENT] Add frontend schema support for AUTHORIZED status and authorization timestamps in `apps/frontend/app/schemas/payment-order.schema.ts`
- [x] T030 [P] [US1] [AGENT-IMPLEMENT] Update payment status badge presentation for AUTHORIZED status in `apps/frontend/app/components/payment/PaymentStatusBadge.vue`
- [x] T031 [US1] [TESTER-ANALYZE] Add authorize risk notes and independent verification checklist in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T032 [US1] [AGENT-REVIEW] Verify authorize story security, idempotency, ETag, and audit behavior against `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`

**Checkpoint**: User Story 1 is independently functional and represents the MVP.

---

## Phase 4: User Story 2 - Capture Authorized Payment (Priority: P1)

**Goal**: Merchant lifecycle operator can capture an AUTHORIZED payment order, including partial capture and lazy expiration failure handling.

**Independent Test**: Authorize an order, capture it with valid headers and optional `amountMinor`, verify CAPTURED status, `capturedAt`, captured amount, cleared expiration, and amount/expiration rejection paths.

### Implementation for User Story 2

- [x] T033 [US2] [AGENT-IMPLEMENT] Add capture workflow with full, partial, and expired-authorization branches in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T034 [US2] [AGENT-IMPLEMENT] Add capture transition method setting `capturedAt`, `capturedAmountMinor`, and clearing `expiresAt` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T035 [US2] [AGENT-IMPLEMENT] Add expired authorization transition to EXPIRED during capture in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T036 [US2] [AGENT-IMPLEMENT] Add capture endpoint with amount validation and lifecycle headers in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T037 [US2] [AGENT-IMPLEMENT] Record CAPTURE and EXPIRE lifecycle history entries in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T038 [P] [US2] [AGENT-IMPLEMENT] Add frontend schema support for CAPTURED and EXPIRED statuses and capture fields in `apps/frontend/app/schemas/payment-order.schema.ts`
- [x] T039 [P] [US2] [AGENT-IMPLEMENT] Update payment status badge presentation for CAPTURED and EXPIRED statuses in `apps/frontend/app/components/payment/PaymentStatusBadge.vue`
- [x] T040 [US2] [TESTER-ANALYZE] Add capture boundary notes for zero, exact, partial, over-authorized, before-expiry, and at-expiry cases in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T041 [US2] [AGENT-REVIEW] Verify capture story amount validation, expiration, PSP mock, and audit behavior against `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`

**Checkpoint**: User Stories 1 and 2 support the core authorize/capture happy path.

---

## Phase 5: User Story 5 - Concurrency Control (Priority: P1)

**Goal**: Lifecycle actions reject stale clients and prevent lost updates using `If-Match` and the payment order version.

**Independent Test**: Read an order ETag, mutate the order through a lifecycle action, retry another lifecycle action with the stale ETag, and verify `412 concurrency_conflict`.

### Implementation for User Story 5

- [x] T042 [US5] [AGENT-IMPLEMENT] Add ETag parser and formatter for `"v{version}"` lifecycle tokens in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T043 [US5] [AGENT-IMPLEMENT] Reconcile existing create/read ETag generation with lifecycle `"v{version}"` contract in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T044 [US5] [AGENT-IMPLEMENT] Add stale `If-Match` detection and `412 concurrency_conflict` handling in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T045 [US5] [AGENT-IMPLEMENT] Add missing `If-Match` header error handling for lifecycle actions in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- [x] T046 [US5] [TESTER-DESIGN] Document stale ETag and simultaneous lifecycle-action verification scenarios in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T047 [US5] [AGENT-REVIEW] Verify optimistic locking behavior preserves no lost updates and no metadata version increment exception in `specs/009-payment-lifecycle-foundation/data-model.md`

**Checkpoint**: P1 lifecycle correctness includes authorize, capture, and stale-client rejection.

---

## Phase 6: User Story 3 - Cancel Payment Order (Priority: P2)

**Goal**: Merchant lifecycle operator can cancel CREATED or AUTHORIZED payment orders, including PSP void behavior for authorized orders.

**Independent Test**: Cancel a CREATED order and an AUTHORIZED order, verify CANCELLED status, reason persistence, and invalid cancel from CAPTURED/CANCELLED.

### Implementation for User Story 3

- [x] T048 [US3] [AGENT-IMPLEMENT] Add cancel workflow with CREATED and AUTHORIZED branches in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T049 [US3] [AGENT-IMPLEMENT] Add cancel transition method setting `cancelledAt` and `cancellationReason` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T050 [US3] [AGENT-IMPLEMENT] Add cancel endpoint with lifecycle headers and reason mapping in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T051 [US3] [AGENT-IMPLEMENT] Record CANCEL lifecycle history entries and PSP void references in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T052 [P] [US3] [AGENT-IMPLEMENT] Add frontend schema and badge support for CANCELLED status in `apps/frontend/app/schemas/payment-order.schema.ts`
- [x] T053 [US3] [TESTER-ANALYZE] Add cancel transition notes for CREATED, AUTHORIZED, CAPTURED, and CANCELLED cases in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T054 [US3] [AGENT-REVIEW] Verify cancel story state-machine and PSP void behavior against `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`

**Checkpoint**: Cancellation paths work independently of refund.

---

## Phase 7: User Story 4 - Refund Captured Payment (Priority: P2)

**Goal**: Merchant lifecycle operator can refund a CAPTURED payment order with full or partial amount validation.

**Independent Test**: Capture an order, refund it with valid `amountMinor` and reason, verify REFUNDED status, `refundedAt`, `refundReason`, and over-refund rejection.

### Implementation for User Story 4

- [x] T055 [US4] [AGENT-IMPLEMENT] Add refund workflow with full, partial, and over-captured amount validation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T056 [US4] [AGENT-IMPLEMENT] Add refund transition method setting `refundedAt`, `refundedAmountMinor`, and `refundReason` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T057 [US4] [AGENT-IMPLEMENT] Add refund endpoint with lifecycle headers and refund body mapping in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T058 [US4] [AGENT-IMPLEMENT] Record REFUND lifecycle history entries and PSP refund references in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T059 [P] [US4] [AGENT-IMPLEMENT] Add frontend schema and badge support for REFUNDED status in `apps/frontend/app/schemas/payment-order.schema.ts`
- [x] T060 [US4] [TESTER-ANALYZE] Add refund boundary notes for zero, exact, partial, over-captured, and wrong-status cases in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T061 [US4] [AGENT-REVIEW] Verify refund story state-machine, amount validation, idempotency, and audit behavior against `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`

**Checkpoint**: Refund completes the core financial lifecycle through REFUNDED.

---

## Phase 8: User Story 6 - Audit Trail (Priority: P2)

**Goal**: Platform auditor can retrieve chronological lifecycle history for any payment order with actor, idempotency, correlation, and transition context.

**Independent Test**: Execute authorize and capture, request history with `platform:payments:audit`, verify chronological records and empty history for a newly created order.

### Implementation for User Story 6

- [x] T062 [US6] [AGENT-IMPLEMENT] Add lifecycle history query method filtering out creation-only entries in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderStatusHistoryRepository.java`
- [x] T063 [US6] [AGENT-IMPLEMENT] Add status history application query method with ownership/audit policy inputs in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T064 [US6] [AGENT-IMPLEMENT] Add `GET /history` endpoint with `platform:payments:audit` support in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T065 [US6] [AGENT-IMPLEMENT] Map lifecycle history response records chronologically in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderMapper.java`
- [x] T066 [P] [US6] [AGENT-IMPLEMENT] Add frontend history schema and store loading state in `apps/frontend/app/stores/payment-orders.ts`
- [x] T067 [P] [US6] [AGENT-IMPLEMENT] Display lifecycle history timeline in payment detail view in `apps/frontend/app/components/payment/PaymentOrderDetail.vue`
- [x] T068 [US6] [TESTER-DESIGN] Document audit trail verification and duplicate-idempotent-replay checks in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T069 [US6] [AGENT-REVIEW] Verify history endpoint authorization, immutability assumptions, and creation-entry filtering against `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`

**Checkpoint**: Audit read model is visible and consistent with lifecycle transitions.

---

## Phase 9: User Story 7 - HTTP Protocol Hardening (Priority: P3)

**Goal**: Lifecycle consumers get correct CORS preflight, no-store cache behavior, Vary headers, and metadata PATCH behavior.

**Independent Test**: Send dev/test OPTIONS preflight to lifecycle endpoint, verify CORS headers; send lifecycle actions and verify no-store/Vary; send metadata PATCH and verify metadata changes without status or lifecycle-version increment.

### Implementation for User Story 7

- [x] T070 [US7] [AGENT-IMPLEMENT] Add dev/test profile CORS configuration for lifecycle endpoints in `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- [x] T071 [US7] [AGENT-IMPLEMENT] Ensure production profile does not enable broad lifecycle CORS in `apps/backend/src/main/resources/application.yml`
- [x] T072 [US7] [AGENT-IMPLEMENT] Add lifecycle response header helper for `Cache-Control: no-store` and `Vary: Authorization, If-Match` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T073 [US7] [AGENT-IMPLEMENT] Add metadata PATCH transition that updates metadata without status change in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- [x] T074 [US7] [AGENT-IMPLEMENT] Add metadata PATCH application workflow preserving lifecycle version semantics in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentLifecycleService.java`
- [x] T075 [US7] [AGENT-IMPLEMENT] Add metadata PATCH endpoint with `If-Match` handling in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- [x] T076 [P] [US7] [AGENT-IMPLEMENT] Add Nuxt server proxy route for metadata PATCH in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].patch.ts`
- [x] T077 [P] [US7] [AGENT-IMPLEMENT] Add Nuxt server proxy route for lifecycle history in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/history.get.ts`
- [x] T078 [P] [US7] [AGENT-IMPLEMENT] Add Nuxt server proxy route for authorize lifecycle action in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/authorize.post.ts`
- [x] T079 [P] [US7] [AGENT-IMPLEMENT] Add Nuxt server proxy route for capture lifecycle action in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/capture.post.ts`
- [x] T080 [P] [US7] [AGENT-IMPLEMENT] Add Nuxt server proxy route for cancel lifecycle action in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/cancel.post.ts`
- [x] T081 [P] [US7] [AGENT-IMPLEMENT] Add Nuxt server proxy route for refund lifecycle action in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/refund.post.ts`
- [x] T082 [US7] [TESTER-ANALYZE] Add CORS, no-store, Vary, and metadata PATCH verification notes in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T083 [US7] [AGENT-REVIEW] Verify HTTP hardening behavior against `specs/009-payment-lifecycle-foundation/contracts/payment-lifecycle-api.md`

**Checkpoint**: HTTP protocol hardening is complete without adding dashboards or lifecycle action buttons.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Validate regression, documentation consistency, and implementation readiness after selected stories are complete.

- [x] T084 [AGENT-IMPLEMENT] Update payment lifecycle quickstart with final implemented endpoint notes in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T085 [AGENT-EXPLAIN] Add tester-facing implementation summary and deferred scope notes in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T086 [AGENT-REVIEW] Run backend regression command and record result reference in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T087 [AGENT-REVIEW] Run frontend typecheck command and record result reference in `specs/009-payment-lifecycle-foundation/quickstart.md`
- [x] T088 [AGENT-REVIEW] Verify no out-of-scope Kafka, webhook, scheduler, dashboard, PSP failure, rate-limit, API-versioning, or HATEOAS code was introduced in `specs/009-payment-lifecycle-foundation/plan.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks all user stories.
- **US1 Authorize (Phase 3)**: Depends on Foundational; MVP increment.
- **US2 Capture (Phase 4)**: Depends on Foundational and is most useful after US1.
- **US5 Concurrency (Phase 5)**: Depends on Foundational and should be completed before broad P2 lifecycle work.
- **US3 Cancel (Phase 6)**: Depends on Foundational; uses US1 path for authorized-cancel verification but also works from CREATED.
- **US4 Refund (Phase 7)**: Depends on US2 because refund requires CAPTURED status.
- **US6 Audit Trail (Phase 8)**: Depends on lifecycle history records from US1/US2 and can then cover US3/US4.
- **US7 HTTP Hardening (Phase 9)**: Depends on lifecycle endpoints; metadata PATCH can be implemented after concurrency parsing is available.
- **Polish**: Depends on all selected user stories.

### User Story Dependencies

- **US1 (P1)**: No dependency on other stories after foundation.
- **US2 (P1)**: Functionally depends on US1 for the normal capture path.
- **US5 (P1)**: Cross-cuts lifecycle actions and should be implemented before relying on concurrent lifecycle behavior.
- **US3 (P2)**: Can cancel CREATED independently; authorized cancel benefits from US1.
- **US4 (P2)**: Depends on US2 because refund starts from CAPTURED.
- **US6 (P2)**: Depends on lifecycle actions creating audit entries.
- **US7 (P3)**: Depends on lifecycle endpoints and ETag parsing.

### Parallel Opportunities

- T004 and T005 can run in parallel during setup.
- T015 and T016 can run in parallel after lifecycle action signatures are agreed.
- Frontend schema/badge tasks marked [P] can run in parallel with backend story service work when contracts are stable.
- Nuxt proxy route tasks T076-T081 can run in parallel because each creates a different file.
- Tester analysis/design tasks can run in parallel with implementation once the relevant story contract is stable.

---

## Parallel Example: User Story 1

```text
Task: "T029 [P] [US1] Add frontend schema support for AUTHORIZED status and authorization timestamps in apps/frontend/app/schemas/payment-order.schema.ts"
Task: "T030 [P] [US1] Update payment status badge presentation for AUTHORIZED status in apps/frontend/app/components/payment/PaymentStatusBadge.vue"
```

---

## Parallel Example: User Story 7

```text
Task: "T076 [P] [US7] Add Nuxt server proxy route for metadata PATCH in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].patch.ts"
Task: "T077 [P] [US7] Add Nuxt server proxy route for lifecycle history in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/history.get.ts"
Task: "T078 [P] [US7] Add Nuxt server proxy route for authorize lifecycle action in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/authorize.post.ts"
Task: "T079 [P] [US7] Add Nuxt server proxy route for capture lifecycle action in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/capture.post.ts"
Task: "T080 [P] [US7] Add Nuxt server proxy route for cancel lifecycle action in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/cancel.post.ts"
Task: "T081 [P] [US7] Add Nuxt server proxy route for refund lifecycle action in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/refund.post.ts"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 Setup.
2. Complete Phase 2 Foundational tasks.
3. Complete Phase 3 User Story 1 Authorize.
4. Validate authorize independently using `specs/009-payment-lifecycle-foundation/quickstart.md`.
5. Stop for review before implementing capture and concurrency.

### Incremental Delivery

1. Deliver US1 authorize as the MVP.
2. Add US2 capture to complete the main success path.
3. Add US5 concurrency before expanding broader lifecycle operations.
4. Add US3 cancel and US4 refund as P2 lifecycle branches.
5. Add US6 audit read model.
6. Add US7 HTTP hardening and metadata PATCH.
7. Run final regression verification and update quickstart notes.

### Regression Commands

```bash
./mvnw -pl apps/backend test
```

```bash
corepack pnpm typecheck
```

## Notes

- All tasks follow `- [ ] T### [P?] [US?] Description with file path` format.
- `[P]` means the task touches a different file or can proceed without depending on incomplete task output.
- No task creates new automated test classes because the feature specification excludes new automated tests as deliverables.
- Existing regression tests must still be run before considering implementation complete.
