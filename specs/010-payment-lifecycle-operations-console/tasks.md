# Tasks: Payment Lifecycle Operations Console

**Input**: Design documents from `specs/010-payment-lifecycle-operations-console/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/operations-console-contract.md`, `quickstart.md`

**Lesson Context**: Lesson 14 / Feature 010 builds on Lesson 14 / Feature 009 Payment Lifecycle Foundation. Feature 009 owns lifecycle REST semantics, lifecycle domain state, status history/audit, metadata PATCH, optimistic concurrency, idempotency, security/CORS support, Nuxt lifecycle schemas, detail component, proxy boundary, and lifecycle proxy routes.

**Co nowego technicznie dochodzi**: Feature 010 makes the application boundary and UI responsible for visible lifecycle operations: `ETag`/version capture, `If-Match`, `Idempotency-Key`, `X-Correlation-ID`, `Authorization`, no-store/cache headers, preserved backend error categories, stale-state feedback, role-aware affordances, metadata separation, and frontend -> Nuxt proxy -> backend -> database/history visibility.

**Tests**: New automated tests, REST Assured framework work, new backend test classes, and frontend E2E feature deliverables are intentionally out of scope. Tasks include existing verification commands, manual verification scenarios, and tester-facing analysis/design notes only.

**Explicit Non-Goals**: No multi-capture, no multi-refund, no PSP failures, no PSP provider integration, no Kafka/webhooks, no scheduled jobs, no full dashboard, no fake KPIs, no complete OAuth/OIDC integration, no new lifecycle states/transitions, and no new payment creation capability including no `POST /payments` scope.

**Organization**: Tasks are grouped by implementation phase and user story so schema/store/proxy/UI slices can be implemented and reviewed independently after shared foundation tasks are complete.

## Phase 1: Setup (Shared Scope Guardrails)

**Purpose**: Confirm Feature 010 is implemented as an application-only Nuxt console over the Feature 009 lifecycle foundation.

- [X] T001 [AGENT-IMPLEMENT] Confirm the active Spec Kit feature points to `specs/010-payment-lifecycle-operations-console` before implementation work starts in `.specify/feature.json`
- [X] T002 [AGENT-IMPLEMENT] Review Feature 010 application-only boundaries against non-goals in `specs/010-payment-lifecycle-operations-console/plan.md`
- [X] T003 [AGENT-IMPLEMENT] Inspect existing Nuxt payment schemas before editing in `apps/frontend/app/schemas/payment-order.schema.ts`
- [X] T004 [P] [AGENT-IMPLEMENT] Inspect existing Pinia payment store state/actions before editing in `apps/frontend/app/stores/payment-orders.ts`
- [X] T005 [P] [AGENT-IMPLEMENT] Inspect existing payment detail page and payment components before editing in `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue` and `apps/frontend/app/components/payment/`
- [X] T006 [P] [AGENT-IMPLEMENT] Inspect existing Nuxt server payment proxy routes before editing under `apps/frontend/server/api/merchants/[merchantId]/payment-orders/`
- [X] T007 [TESTER-ANALYZE] Record the Feature 010 risk focus for stale state, header forwarding, action affordances, metadata separation, and error flattening in `specs/010-payment-lifecycle-operations-console/quickstart.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared view models, store state, proxy utilities, and display vocabulary required by all user stories.

**CRITICAL**: No user story UI implementation should begin until the schema/store/proxy foundations are complete.

- [X] T008 [AGENT-IMPLEMENT] Extend lifecycle status schema support for `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, and `REFUNDED` in `apps/frontend/app/schemas/payment-order.schema.ts`
- [X] T009 [AGENT-IMPLEMENT] Extend payment detail schema with lifecycle timestamps, lifecycle amounts/reasons, metadata, and internal `versionMarker` support in `apps/frontend/app/schemas/payment-order.schema.ts`
- [X] T010 [AGENT-IMPLEMENT] Add lifecycle history entry schema with safe actor, reason, amount, PSP reference, correlation, and oldest-first-compatible timestamp fields in `apps/frontend/app/schemas/payment-order.schema.ts`
- [X] T011 [AGENT-IMPLEMENT] Add lifecycle action request and metadata update request schemas that keep action fields separate from metadata fields in `apps/frontend/app/schemas/payment-order.schema.ts`
- [X] T012 [AGENT-IMPLEMENT] Add backend lifecycle error schema/categories for `401`, `403`, `404`, `409`, `412`, `415`, `422`, and backend unavailable outcomes in `apps/frontend/app/schemas/payment-order.schema.ts`
- [X] T013 [AGENT-IMPLEMENT] Extend Pinia state with separate detail, history, action, metadata, and scoped feedback state in `apps/frontend/app/stores/payment-orders.ts`
- [X] T014 [AGENT-IMPLEMENT] Store the current version marker from detail responses for later `If-Match` use without rendering it as prominent business content in `apps/frontend/app/stores/payment-orders.ts`
- [X] T015 [AGENT-IMPLEMENT] Add reusable action-availability derivation from current status and conservative role context in `apps/frontend/app/stores/payment-orders.ts`
- [X] T016 [AGENT-IMPLEMENT] Add proxy helper behavior for forwarding `Authorization`, `If-Match`, `Idempotency-Key`, and `X-Correlation-ID` while preserving backend error status/body shape in `apps/frontend/server/utils/backendApi.ts`
- [X] T017 [AGENT-IMPLEMENT] Ensure detail GET proxy exposes backend `ETag` as the application version marker when no response version field exists in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].get.ts`
- [X] T018 [AGENT-REVIEW] Verify foundational tasks do not introduce REST Assured files, new test classes, Kafka/webhook code, PSP failure flows, dashboard metrics, OAuth/OIDC completion, or `POST /payments` scope in the changed files
- [X] T019 [TESTER-DESIGN] Document the six-status by four-action Feature 010 UI action matrix and expected feedback categories in `specs/010-payment-lifecycle-operations-console/quickstart.md`

**Checkpoint**: Shared schemas, store state, proxy conventions, version marker handling, and action matrix are ready for user story implementation.

---

## Phase 3: User Story 1 - Inspect Lifecycle Detail (Priority: P1) MVP

**Goal**: A read-capable user can open payment detail and understand status, amount, lifecycle facts, and metadata without raw API output.

**Independent Test**: Open orders in the supported lifecycle statuses and confirm status badge, lifecycle facts, amount/currency, metadata, and absent-field behavior match the spec.

### Implementation for User Story 1

- [X] T020 [US1] [AGENT-IMPLEMENT] Render lifecycle summary fields for current status, amount, currency, timestamps, captured/refunded amounts, and lifecycle reasons in the payment detail UI under `apps/frontend/app/components/payment/`
- [X] T022 [US1] [AGENT-IMPLEMENT] Render current metadata safely on the payment detail page without displaying raw tokens, credentials, or idempotency internals in `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`
- [X] T023 [US1] [AGENT-IMPLEMENT] Ensure nullable lifecycle fields render only when present and relevant to the current status in `apps/frontend/app/components/payment/`
- [X] T024 [US1] [AGENT-IMPLEMENT] Add detail loading, not-found, forbidden, and backend-unavailable states without rendering mutation controls during unavailable detail states in `apps/frontend/app/stores/payment-orders.ts` and `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`
- [X] T025 [US1] [TESTER-ANALYZE] Add manual detail-inspection checklist for all six Feature 009 statuses in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [X] T026 [US1] [AGENT-REVIEW] Verify detail display against `FR-SUMMARY-*`, secret-display constraints, and the contract in `specs/010-payment-lifecycle-operations-console/contracts/operations-console-contract.md`

**Checkpoint**: User Story 1 provides the MVP detail inspection experience.

---

## Phase 4: User Story 2 - Review Lifecycle History (Priority: P1)

**Goal**: Operators, auditors, and support users can read lifecycle history oldest-first while detail remains usable if history fails.

**Independent Test**: Open an order with empty history and an order with authorize/capture history; confirm empty, loading, error, and oldest-first display behavior.

### Implementation for User Story 2

- [X] T027 [US2] [AGENT-IMPLEMENT] Add Pinia action to load lifecycle history independently from detail loading in `apps/frontend/app/stores/payment-orders.ts`
- [X] T028 [US2] [AGENT-IMPLEMENT] Render lifecycle history timeline/list oldest-first with from status, to status, action timestamp, and safe actor fields in `apps/frontend/app/components/payment/`
- [X] T029 [US2] [AGENT-IMPLEMENT] Render optional reason, amount, PSP reference, and safe correlation fields only when returned by the backend in `apps/frontend/app/components/payment/`
- [X] T030 [US2] [AGENT-IMPLEMENT] Add empty history state that does not imply audit failure in `apps/frontend/app/components/payment/`
- [X] T031 [US2] [AGENT-IMPLEMENT] Add scoped history loading and history error state that does not remove or overwrite the payment detail summary in `apps/frontend/app/stores/payment-orders.ts` and `apps/frontend/app/components/payment/`
- [X] T032 [US2] [AGENT-IMPLEMENT] Update history proxy route to forward authorization/correlation headers and preserve backend error status/body shape in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/history.get.ts`
- [ ] T033 [US2] [TESTER-DESIGN] Add history verification notes for empty, single-entry, multi-entry, safe actor, safe PSP reference, and history-failure cases in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T034 [US2] [AGENT-REVIEW] Verify history display against `FR-HISTORY-*`, oldest-first ordering, and no secret rendering in `specs/010-payment-lifecycle-operations-console/contracts/operations-console-contract.md`

**Checkpoint**: User Story 2 makes lifecycle history visible without coupling history failures to detail failures.

---

## Phase 5: User Story 3 - Execute Valid Lifecycle Actions (Priority: P1)

**Goal**: A lifecycle-capable operator can perform only valid Feature 009 actions from the detail screen and receive clear success/failure feedback.

**Independent Test**: Open each lifecycle status, confirm the visible action set matches the matrix, perform valid actions, and confirm success feedback plus detail/history reload.

### Implementation for User Story 3

- [X] T035 [US3] [AGENT-IMPLEMENT] Render action controls from the state-aware action matrix for authorize, capture, cancel, and refund in `apps/frontend/app/components/payment/`
- [X] T036 [US3] [AGENT-IMPLEMENT] Add confirmation UX before capture, cancel, and refund submission in `apps/frontend/app/components/payment/`
- [X] T037 [US3] [AGENT-IMPLEMENT] Add action-specific input handling for capture amount, refund amount, refund reason, and cancellation reason without mixing metadata fields in `apps/frontend/app/components/payment/`
- [X] T038 [US3] [AGENT-IMPLEMENT] Add Pinia action submission methods for authorize, capture, cancel, and refund using current `versionMarker` and scoped action-submitting state in `apps/frontend/app/stores/payment-orders.ts`
- [ ] T039 [US3] [AGENT-IMPLEMENT] Refresh payment detail and lifecycle history after successful lifecycle action submission in `apps/frontend/app/stores/payment-orders.ts`
- [ ] T040 [US3] [AGENT-IMPLEMENT] Preserve user-entered action values after validation/backend-unavailable failures where the user can correct or retry safely in `apps/frontend/app/stores/payment-orders.ts` and `apps/frontend/app/components/payment/`
- [X] T041 [US3] [AGENT-IMPLEMENT] Update authorize proxy route to forward authorization/correlation, send `If-Match`, generate/forward one-attempt idempotency key, and preserve backend error shape in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/authorize.post.ts`
- [X] T042 [US3] [AGENT-IMPLEMENT] Update capture proxy route to forward authorization/correlation, send `If-Match`, generate/forward one-attempt idempotency key, and preserve backend error shape in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/capture.post.ts`
- [X] T043 [US3] [AGENT-IMPLEMENT] Update cancel proxy route to forward authorization/correlation, send `If-Match`, generate/forward one-attempt idempotency key, and preserve backend error shape in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/cancel.post.ts`
- [X] T044 [US3] [AGENT-IMPLEMENT] Update refund proxy route to forward authorization/correlation, send `If-Match`, generate/forward one-attempt idempotency key, and preserve backend error shape in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/refund.post.ts`
- [ ] T045 [US3] [TESTER-DESIGN] Add manual action-matrix verification notes for all six statuses and four actions in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T046 [US3] [AGENT-REVIEW] Verify action UI and proxy behavior against `FR-ACTION-*`, `FR-PROXY-*`, idempotency, and no multi-capture/multi-refund scope in `specs/010-payment-lifecycle-operations-console/contracts/operations-console-contract.md`

**Checkpoint**: User Story 3 supports valid lifecycle actions through the application boundary without adding new lifecycle semantics.

---

## Phase 6: User Story 4 - Submit Lifecycle Requests Safely Through the Application Boundary (Priority: P1)

**Goal**: Nuxt proxy behavior preserves Feature 009 protocol requirements and backend error categories instead of flattening them.

**Independent Test**: Perform or simulate lifecycle requests through Nuxt and confirm effective authorization, conditional update, idempotency, correlation, and backend error preservation behavior.

### Implementation for User Story 4

- [X] T047 [US4] [AGENT-IMPLEMENT] Normalize proxy-side lifecycle error handling so backend `401`, `403`, `404`, `409`, `412`, `415`, `422`, and unavailable errors remain distinguishable to the browser in `apps/frontend/server/utils/backendApi.ts`
- [X] T048 [US4] [AGENT-IMPLEMENT] Map preserved backend error categories into distinct Pinia feedback states for access denied, validation, invalid transition, stale state, idempotency conflict, not found, and backend unavailable in `apps/frontend/app/stores/payment-orders.ts`
- [X] T049 [US4] [AGENT-IMPLEMENT] Implement stale-state handling that shows feedback, reloads detail/history, and does not automatically retry lifecycle actions in `apps/frontend/app/stores/payment-orders.ts`
- [X] T050 [US4] [AGENT-IMPLEMENT] Implement `422 invalid_transition` handling that shows domain feedback and refreshes current state where controls may be stale in `apps/frontend/app/stores/payment-orders.ts`
- [ ] T051 [US4] [AGENT-IMPLEMENT] Ensure safe correlation identifiers are displayed only as support diagnostics and secrets are never rendered in error feedback in `apps/frontend/app/components/payment/`
- [ ] T052 [US4] [TESTER-ANALYZE] Add stale-state, invalid-transition, idempotency-conflict, forbidden, not-found, validation, and backend-unavailable risk notes in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T053 [US4] [AGENT-REVIEW] Verify proxy/header/error behavior against the header forwarding and error mapping tables in `specs/010-payment-lifecycle-operations-console/contracts/operations-console-contract.md`

**Checkpoint**: User Story 4 hardens the Nuxt application boundary for lifecycle protocol correctness.

---

## Phase 7: User Story 5 - Update Metadata Separately From Lifecycle (Priority: P2)

**Goal**: An operator can edit metadata from a separate flow using conditional update rules without implying a lifecycle transition.

**Independent Test**: Edit metadata, confirm detail refreshes, confirm status is not presented as changed by the metadata flow, and confirm stale metadata feedback refreshes current detail.

### Implementation for User Story 5

- [ ] T054 [US5] [AGENT-IMPLEMENT] Add separate metadata editing UI for key/value metadata away from lifecycle action controls in `apps/frontend/app/components/payment/`
- [ ] T055 [US5] [AGENT-IMPLEMENT] Add Pinia metadata save action using current `versionMarker`, scoped metadata-saving state, and detail refresh after success in `apps/frontend/app/stores/payment-orders.ts`
- [ ] T056 [US5] [AGENT-IMPLEMENT] Ensure metadata save success feedback cannot be displayed as lifecycle action success in `apps/frontend/app/stores/payment-orders.ts` and `apps/frontend/app/components/payment/`
- [ ] T057 [US5] [AGENT-IMPLEMENT] Add stale metadata update handling that informs the user, reloads current detail, and prevents continued editing against the stale marker in `apps/frontend/app/stores/payment-orders.ts`
- [ ] T058 [US5] [AGENT-IMPLEMENT] Update metadata PATCH proxy route to forward authorization/correlation, send `If-Match`, avoid `Idempotency-Key`, and preserve backend error shape in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].patch.ts`
- [ ] T059 [US5] [TESTER-DESIGN] Add metadata verification notes for separate flow, status unchanged by metadata save, validation feedback, stale-state refresh, and no idempotency key in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T060 [US5] [AGENT-REVIEW] Verify metadata behavior against `FR-METADATA-*`, `FR-PROXY-004`, and the metadata contract in `specs/010-payment-lifecycle-operations-console/contracts/operations-console-contract.md`

**Checkpoint**: User Story 5 provides metadata editing without product confusion or lifecycle side effects.

---

## Phase 8: User Story 6 - Respect Roles and Permissions in the Console (Priority: P2)

**Goal**: Read-only and audit users can inspect lifecycle information without misleading mutation affordances, while backend authorization remains final.

**Independent Test**: Open the detail view with read/audit and lifecycle-capable contexts, then confirm controls are hidden/disabled only when role context is known and forbidden backend responses are shown as access denied.

### Implementation for User Story 6

- [ ] T061 [US6] [AGENT-IMPLEMENT] Consume available role/permission context conservatively when deriving lifecycle and metadata affordances in `apps/frontend/app/stores/payment-orders.ts`
- [ ] T062 [US6] [AGENT-IMPLEMENT] Hide or disable mutation controls for known read-only/audit actors without claiming final authorization in `apps/frontend/app/components/payment/`
- [ ] T063 [US6] [AGENT-IMPLEMENT] Keep state-impossible actions hidden even when role context is incomplete and allow backend authorization to decide final permission for otherwise possible actions in `apps/frontend/app/components/payment/`
- [ ] T064 [US6] [AGENT-IMPLEMENT] Show backend `403` lifecycle and metadata failures as access-denied feedback with no success state in `apps/frontend/app/stores/payment-orders.ts` and `apps/frontend/app/components/payment/`
- [ ] T065 [US6] [TESTER-ANALYZE] Add role-affordance risk notes for merchant operator, merchant reader, platform operator, platform auditor, and incomplete role context in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T066 [US6] [AGENT-REVIEW] Verify role UX against `FR-ROLE-*` and confirm backend enforcement is not replaced by frontend-only checks in `specs/010-payment-lifecycle-operations-console/contracts/operations-console-contract.md`

**Checkpoint**: User Story 6 reduces misleading controls while preserving backend authorization authority.

---

## Phase 9: Conditional Backend Response-Contract Fallback

**Purpose**: Touch backend payment response code only if the existing Feature 009 API omits fields required for the Feature 010 display contract.

**CRITICAL**: Skip this entire phase if Nuxt can satisfy the detail/history/metadata/version display contract from existing backend responses and headers.

- [ ] T067 [AGENT-REVIEW] Compare existing backend detail, history, metadata, and lifecycle response fields against `specs/010-payment-lifecycle-operations-console/data-model.md` before deciding whether backend fallback work is needed
- [ ] T068 [AGENT-IMPLEMENT] If required, expose already-defined lifecycle display fields through existing payment response DTO/mapper code without adding lifecycle states, transitions, PSP failures, or payment creation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/`
- [ ] T069 [AGENT-IMPLEMENT] If required, expose safe lifecycle history display fields through existing history response DTO/mapper code without exposing raw tokens, credentials, or idempotency hashes in `apps/backend/src/main/java/lab/paymentquality/payment/internal/`
- [ ] T070 [AGENT-IMPLEMENT] If required, ensure existing backend responses or headers provide a version marker usable by the Nuxt detail/store contract in `apps/backend/src/main/java/lab/paymentquality/payment/internal/`
- [ ] T071 [AGENT-REVIEW] Verify any backend fallback stays inside the existing payment module and does not add new test classes, REST Assured framework work, Kafka/webhooks, PSP failure paths, OAuth/OIDC completion, or `POST /payments` scope

**Checkpoint**: Backend fallback, if needed, only supports application display/contract completeness.

---

## Final Phase: Verification, Documentation, and Learning Notes

**Purpose**: Validate implementation readiness, preserve tester learning context, and document what was built and deferred.

- [ ] T072 [AGENT-REVIEW] Run frontend typecheck and record the result in `specs/010-payment-lifecycle-operations-console/quickstart.md` using `corepack pnpm --dir apps/frontend typecheck`
- [ ] T073 [AGENT-REVIEW] Run frontend build and record the result in `specs/010-payment-lifecycle-operations-console/quickstart.md` using `corepack pnpm --dir apps/frontend build`
- [ ] T074 [AGENT-REVIEW] If backend fallback code was touched, run backend regression and record the result in `specs/010-payment-lifecycle-operations-console/quickstart.md` using `./mvnw -pl apps/backend test`
- [ ] T075 [TESTER-AUTOMATE] Identify only future automation candidates for schema/store/proxy/header/error behavior in `specs/010-payment-lifecycle-operations-console/quickstart.md` without creating test classes, REST Assured framework work, or frontend E2E deliverables for Feature 010
- [ ] T076 [AGENT-EXPLAIN] Add tester-facing implementation summary for Lesson 14 / Feature 010 and its dependency on Lesson 14 / Feature 009 in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T077 [AGENT-EXPLAIN] Preserve the `Co nowego technicznie dochodzi` learning notes for headers, status/error categories, stale state, idempotency, role affordances, and frontend -> Nuxt proxy -> backend -> database/history visibility in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T078 [DISCUSS] Capture any remaining product or learning trade-offs about role context, optional authorize reason, partial capture/refund display, and safe correlation display in `specs/010-payment-lifecycle-operations-console/quickstart.md`
- [ ] T079 [AGENT-REVIEW] Perform final scope review confirming no multi-capture, no multi-refund, no PSP failures, no Kafka/webhooks, no full dashboard, no complete OAuth/OIDC integration, no REST Assured framework work, no new test classes, and no new `POST /payments` scope were introduced

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup completion and blocks user story UI work.
- **US1 Inspect Lifecycle Detail (Phase 3)**: Depends on Foundational; MVP increment.
- **US2 Review Lifecycle History (Phase 4)**: Depends on Foundational and benefits from US1 detail layout.
- **US3 Execute Valid Lifecycle Actions (Phase 5)**: Depends on Foundational and should follow US1 detail state.
- **US4 Safe Application Boundary (Phase 6)**: Depends on proxy foundations and should be completed before relying on lifecycle mutation UX.
- **US5 Metadata Update (Phase 7)**: Depends on version marker handling and proxy foundations.
- **US6 Roles and Permissions (Phase 8)**: Depends on action availability derivation and feedback mapping.
- **Backend Fallback (Phase 9)**: Conditional; run only after comparing existing Feature 009 responses with the Feature 010 display contract.
- **Verification and Learning Notes**: Depends on all selected implementation phases.

### User Story Dependencies

- **US1 (P1)**: First visible increment after foundations.
- **US2 (P1)**: Can be implemented independently from action submission after schemas/store support history.
- **US3 (P1)**: Depends on state/action matrix and version marker foundation.
- **US4 (P1)**: Cross-cuts US3 lifecycle submissions and should be finished before final action verification.
- **US5 (P2)**: Depends on version marker foundation but remains separate from lifecycle actions.
- **US6 (P2)**: Depends on role context availability and action affordance derivation.

### Parallel Opportunities

- T004, T005, and T006 can run in parallel during setup.
- T008, T010, and T012 can run in parallel if schema edits are coordinated carefully.
- T020/T021/T022 can run in parallel with T027/T028 when component ownership is separated.
- T041, T042, T043, and T044 can run in parallel because each lifecycle proxy route is a separate file.
- T054 and T058 can run in parallel after the metadata request schema and version marker convention are stable.
- T067 can run before or in parallel with Nuxt UI implementation to decide whether backend fallback is needed.

---

## Parallel Example: Lifecycle Proxy Routes

```text
Task: "T041 [US3] Update authorize proxy route to forward authorization/correlation, send If-Match, generate/forward one-attempt idempotency key, and preserve backend error shape in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/authorize.post.ts"
Task: "T042 [US3] Update capture proxy route to forward authorization/correlation, send If-Match, generate/forward one-attempt idempotency key, and preserve backend error shape in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/capture.post.ts"
Task: "T043 [US3] Update cancel proxy route to forward authorization/correlation, send If-Match, generate/forward one-attempt idempotency key, and preserve backend error shape in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/cancel.post.ts"
Task: "T044 [US3] Update refund proxy route to forward authorization/correlation, send If-Match, generate/forward one-attempt idempotency key, and preserve backend error shape in apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/refund.post.ts"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete US1 detail inspection.
3. Complete US2 history display.
4. Complete enough US3 and US4 for one safe lifecycle action path with detail/history refresh.
5. Verify with existing frontend commands and manual quickstart scenarios.

### Scope Discipline

- Keep Feature 010 application-only unless the backend response-contract fallback is explicitly proven necessary.
- Treat Feature 009 lifecycle behavior as consumed behavior, not as a place to add new semantics.
- Prefer visible application correctness over test-framework expansion.
- Keep metadata, lifecycle actions, and history feedback visibly separate so users do not infer behavior the backend does not provide.
