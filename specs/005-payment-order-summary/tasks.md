# Tasks: Payment Order Aggregation Summary

**Input**: Design documents from `specs/005-payment-order-summary/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/payment-order-summary-api.md, quickstart.md

**Tests**: Test implementation is intentionally omitted in this task set because the user requested a system-only implementation slice. Verification tasks use `./mvnw clean compile`, `./mvnw -DskipTests package`, and existing `PaymentModuleTest`. REST Assured, JUnit, AssertJ, and Playwright tests are deferred to a later tester/automation slice.

**Organization**: Tasks are grouped by user story to enable independent implementation and verification of the summary data behavior (US1) and access boundary behavior (US2).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it touches different files and has no dependency on incomplete tasks.
- **[Story]**: Which user story this task belongs to (`US1`, `US2`).
- **Label**: Descriptions use lab labels such as `[AGENT-IMPLEMENT]`, `[AGENT-REVIEW]`, `[AGENT-EXPLAIN]`, `[TESTER-ANALYZE]`.
- All implementation tasks include exact file paths.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm feature context and preserve the system-only guardrail before implementation.

- [X] T001 [AGENT-REVIEW] Read `specs/005-payment-order-summary/spec.md`, `specs/005-payment-order-summary/plan.md`, `specs/005-payment-order-summary/research.md`, `specs/005-payment-order-summary/data-model.md`, and `specs/005-payment-order-summary/contracts/payment-order-summary-api.md` to confirm scope, endpoint, response shape, and no-test constraint.
- [X] T002 [AGENT-REVIEW] Inspect existing payment controller, list service, repository, security config, and exception handler in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`, `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java`, `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java`, `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`, and `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`.
- [X] T003 [AGENT-REVIEW] Confirm no new Flyway migration is needed by comparing summary filters against existing indexes in `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql` and `apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create shared request/response and repository foundation required by both user stories.

**CRITICAL**: No user story endpoint work can begin until this phase is complete.

- [X] T004 [P] [AGENT-IMPLEMENT] Create `PaymentOrderSummaryRequest` record in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryRequest.java` with nullable fields `String currency`, `String status`, `String fromDate`, and `String toDate`.
- [X] T005 [P] [AGENT-IMPLEMENT] Create `PaymentOrderSummaryResponse` record in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryResponse.java` with fields `long totalOrders`, `long totalAmountMinor`, `List<CurrencySummary> byCurrency`, and `List<StatusSummary> byStatus`, including nested records `CurrencySummary` and `StatusSummary`.
- [X] T006 [AGENT-IMPLEMENT] Add summary projection interfaces or records for total, currency, and status aggregation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java` or a package-local companion type under `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/`.
- [X] T007 [AGENT-IMPLEMENT] Add repository aggregation methods to `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java` for total summary, grouped-by-currency summary, and grouped-by-status summary, using database aggregation and optional filter parameters.
- [X] T008 [AGENT-REVIEW] Verify foundational code compiles conceptually against `specs/005-payment-order-summary/data-model.md`: empty sums normalize later to zero, grouped rows expose `orderCount` and `totalAmountMinor`, and ordering by currency/status is handled at query or service level.

**Checkpoint**: Request/response DTOs and repository aggregation surface exist. User story implementation can start.

---

## Phase 3: User Story 1 - View Payment Order Summary (Priority: P1) MVP

**Goal**: Merchant or platform payment reader can receive accurate summary totals and grouped rows for matching payment orders.

**Independent Test**: After implementation, this story can be verified manually or by later automation by creating known payment orders and calling `GET /api/merchants/{merchantId}/payment-orders/summary`, expecting `totalOrders`, `totalAmountMinor`, `byCurrency`, and `byStatus` to match the controlled dataset from `data-model.md`.

### Tests for User Story 1

> No test implementation tasks are generated in this system-only task set. Future automation should add REST Assured contract tests for summary totals, empty summary, filters, and aggregate invariants.

### Implementation for User Story 1

- [X] T009 [P] [US1] [AGENT-IMPLEMENT] Create `PaymentOrderSummaryService` class in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java` with constructor injection of `JpaPaymentOrderRepository` and class-level `@Transactional(readOnly = true)`.
- [X] T010 [US1] [AGENT-IMPLEMENT] Implement `PaymentOrderSummaryService.summarize(UUID merchantId, PaymentOrderSummaryRequest request)` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java`, parsing `fromDate` and `toDate`, delegating to repository aggregation methods, and returning `PaymentOrderSummaryResponse`.
- [X] T011 [US1] [AGENT-IMPLEMENT] Implement validation in `PaymentOrderSummaryService` or `PaymentOrderSummaryRequest` handling in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java` so unsupported `currency`, unsupported `status`, and malformed dates produce the existing `400 validation` behavior through current exception handling.
- [X] T012 [US1] [AGENT-IMPLEMENT] Implement null-to-zero normalization for total `SUM(amount_minor)` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java`, returning `totalAmountMinor=0` for empty result sets.
- [X] T013 [US1] [AGENT-IMPLEMENT] Map grouped currency projection rows to `PaymentOrderSummaryResponse.CurrencySummary` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java`, preserving ascending currency ordering.
- [X] T014 [US1] [AGENT-IMPLEMENT] Map grouped status projection rows to `PaymentOrderSummaryResponse.StatusSummary` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java`, preserving ascending status ordering.
- [X] T015 [US1] [AGENT-IMPLEMENT] Inject `PaymentOrderSummaryService` into `PaymentOrderController` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java` without changing existing create, read, or list behavior.
- [X] T016 [US1] [AGENT-IMPLEMENT] Add `@GetMapping("/summary")` handler `summarizePaymentOrders(...)` to `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java` that binds `currency`, `status`, `fromDate`, and `toDate`, builds `PaymentOrderSummaryRequest`, calls `PaymentOrderSummaryService`, and returns `200 OK` with `X-Correlation-ID`.
- [X] T017 [US1] [AGENT-IMPLEMENT] Ensure `summarizePaymentOrders(...)` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java` does not return `ETag`, does not mutate state, and does not call lifecycle or idempotency logic.
- [X] T018 [US1] [AGENT-REVIEW] Run `./mvnw clean compile` from `apps/backend` and fix compile errors related to summary DTOs, service, repository methods, imports, or controller injection.

**Checkpoint**: User Story 1 endpoint is implemented and compile-verified for happy-path summary behavior.

---

## Phase 4: User Story 2 - Enforce Summary Access Boundary (Priority: P2)

**Goal**: Summary endpoint uses the same read-role and ownership boundary as the list endpoint: merchant readers only for own merchant, platform readers for any merchant, and no access for creator-only/operator-only/denied identities.

**Independent Test**: After implementation, this story can be verified manually or by later automation by calling summary with own merchant reader, cross-tenant merchant reader, platform reader, creator-only identity, operator-only identity, denied identity, and no token.

### Tests for User Story 2

> No test implementation tasks are generated in this system-only task set. Future automation should add a summary security matrix using REST Assured or the existing security test conventions.

### Implementation for User Story 2

- [X] T019 [US2] [AGENT-IMPLEMENT] Add explicit summary matcher to `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`: `GET /api/merchants/*/payment-orders/summary` requires `merchant:payments:read` or `platform:payments:read`.
- [X] T020 [US2] [AGENT-IMPLEMENT] Ensure the summary matcher in `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java` appears before the wildcard single-resource matcher `GET /api/merchants/*/payment-orders/*` to avoid treating `summary` as a payment order ID.
- [X] T021 [US2] [AGENT-IMPLEMENT] Add platform-reader detection to `summarizePaymentOrders(...)` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`, matching the existing list endpoint authority check.
- [X] T022 [US2] [AGENT-IMPLEMENT] Add merchant-reader `merchant_id` claim enforcement to `summarizePaymentOrders(...)` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`, throwing `AccessDeniedException("Merchant scope mismatch")` for missing or mismatched claim.
- [X] T023 [US2] [AGENT-IMPLEMENT] Confirm `summarizePaymentOrders(...)` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java` returns `403` for cross-tenant summary by using `AccessDeniedException`, not `PaymentOrderNotFoundException`.
- [X] T024 [US2] [AGENT-REVIEW] Run `./mvnw clean compile` from `apps/backend` and fix compile errors related to security matcher ordering, controller auth parameters, or injected services.
- [ ] T025 [US2] [AGENT-REVIEW] Run `./mvnw -DskipTests package` from `apps/backend` to verify the system implementation packages without executing tests. **Blocked** by existing `testCompile` errors in `apps/backend/src/test/java/lab/paymentquality/rest/MyPaymentOrderBusinessFlowRestAssuredTest.java`.
- [ ] T026 [US2] [AGENT-REVIEW] Run `./mvnw -Dtest=PaymentModuleTest test` from `apps/backend` to verify Spring Modulith architecture remains valid. **Blocked** by existing `testCompile` errors in `apps/backend/src/test/java/lab/paymentquality/rest/MyPaymentOrderBusinessFlowRestAssuredTest.java`.

**Checkpoint**: User Stories 1 and 2 are system-implemented; compile passed, while package/module verification is blocked by existing `testCompile` errors outside this slice.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Keep Spec Kit, vault, and implementation evidence aligned after system code is in place.

- [X] T027 [AGENT-REVIEW] Inspect `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`, `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java`, and `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java` to confirm no lifecycle actions, PSP/Kafka/webhook behavior, new statuses, or frontend dashboard behavior were introduced.
- [X] T028 [AGENT-EXPLAIN] Update `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 08 - Payment Aggregation Summary.md` with system implementation evidence once T018, T025, and T026 pass. (Updated with current implementation and explicit verification blockers.)
- [X] T029 [AGENT-EXPLAIN] Update `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md` with Lesson 08 system code evidence and commands run.
- [X] T030 [AGENT-EXPLAIN] Update `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md` to mark Lesson 08 system implementation tasks complete or note remaining automation/test slice work.
- [ ] T031 [AGENT-REVIEW] Re-run final verification from `apps/backend`: `./mvnw clean compile`, `./mvnw -DskipTests package`, and `./mvnw -Dtest=PaymentModuleTest test`. **Partially complete**: `clean compile` passed; package and Modulith command blocked by existing `testCompile` errors in `apps/backend/src/test/java/lab/paymentquality/rest/MyPaymentOrderBusinessFlowRestAssuredTest.java`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup** has no dependencies and can start immediately.
- **Phase 2: Foundational** depends on Phase 1 understanding and blocks all user story implementation.
- **Phase 3: US1 Summary Data Behavior** depends on Phase 2.
- **Phase 4: US2 Access Boundary** depends on Phase 2 and can begin after the controller summary method exists from US1, but security config tasks T019-T020 can be prepared independently.
- **Phase 5: Polish** depends on US1 and US2 implementation and verification.

### User Story Dependencies

- **US1 (P1)**: Depends on foundational DTOs and repository surface. Delivers MVP data contract.
- **US2 (P2)**: Depends on endpoint existence from US1 for controller ownership enforcement, but security matcher tasks can be done once the path is known.

### Within Each User Story

- Repository projections/methods before service mapping.
- Service mapping before controller endpoint.
- Controller endpoint before full access-boundary verification.
- Compile verification after each story.

---

## Parallel Opportunities

- T004 and T005 can run in parallel because they create separate DTO files.
- T009 can start after repository method signatures are known from T006-T007, while controller injection waits for service completion.
- T019 and T020 can be prepared in `SecurityConfig.java` while US1 service work proceeds, but final compile verification should happen after controller endpoint exists.
- Documentation updates T028-T030 can be prepared in parallel after implementation evidence is known.

## Parallel Example: Foundational DTO Work

```text
Task: "Create PaymentOrderSummaryRequest in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryRequest.java"
Task: "Create PaymentOrderSummaryResponse in apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryResponse.java"
```

## Parallel Example: Final Documentation Updates

```text
Task: "Update Lesson 08 note with implementation evidence"
Task: "Update Lesson Evidence Tracker with commands run"
Task: "Update Current Sprint with completed system implementation state"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 setup review.
2. Complete Phase 2 DTO and repository foundation.
3. Complete Phase 3 summary service and endpoint.
4. Run `./mvnw clean compile`.
5. Stop and verify the response contract manually if desired.

### Incremental Delivery

1. Build summary data contract (US1) first.
2. Add access boundary enforcement and security matcher (US2).
3. Run package and Modulith verification.
4. Update vault evidence.
5. Defer REST Assured/security automation to a separate test slice.

### Guardrail Strategy

- Do not add test files in this task set.
- Do not add lifecycle endpoints or statuses.
- Do not add PSP/Kafka/webhook code.
- Do not add frontend dashboard files.
- Do not add V4 migration unless implementation review explicitly documents a justified `EXPLAIN` need.

---

## Format Validation

All tasks follow the required checklist format:

```text
- [ ] T### [P?] [US?] Description with file path
```

Setup, foundational, and polish tasks omit story labels as required. User story tasks include `[US1]` or `[US2]`. Every implementation task references exact file paths.
