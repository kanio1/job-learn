# Tasks: REST HTTP Contract Hardening and Authorization Matrix

**Input**: Design documents from `/specs/007-rest-http-contract-hardening-authorization-matrix/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/summary-http-edge-api.md, quickstart.md

**Tests**: This feature IS tests. All tasks produce test code. No production code changes are expected unless characterization exposes a real bug.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- **Label**: Prefix descriptions with one lab label when useful: `[AGENT-IMPLEMENT]`, `[AGENT-EXPLAIN]`, `[TESTER-ANALYZE]`, `[TESTER-DESIGN]`, `[TESTER-AUTOMATE]`, `[AGENT-REVIEW]`


## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Characterize Spring MVC behavior and verify that all prerequisites are in place before writing contract tests.

- [x] T001 [TESTER-ANALYZE] Characterize Spring MVC default behavior for unsupported `Accept` header on summary endpoint by sending an exploratory `GET` request with `Accept: text/xml` and documenting the actual status code and body in `research.md`
- [x] T002 [TESTER-ANALYZE] Characterize Spring MVC default behavior for unsupported methods by sending `PUT`, `PATCH`, `DELETE` exploratory requests to the summary endpoint and documenting actual status codes and `Allow` header in `research.md`
- [x] T003 [TESTER-ANALYZE] Characterize Spring MVC behavior for `If-None-Match` header on summary endpoint (no `ETag`) by sending exploratory request and verifying `200 OK` with no caching side effects
- [x] T004 [AGENT-REVIEW] Verify existing summary/list/security regression tests pass before any changes using `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test`
- [x] T005 [AGENT-REVIEW] Verify `PaymentModuleTest` and package pass before any changes using `./mvnw -Dtest=PaymentModuleTest test` and `./mvnw -DskipTests package`

**Checkpoint**: Spring MVC characterization complete. Regression baseline green. Ready to implement tests.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Test support infrastructure that MUST be complete before ANY user story test can run.

**⚠️ CRITICAL**: No user story test work can begin until this phase is complete

- [x] T006 [AGENT-IMPLEMENT] Add `merchantPaymentReaderTokenWithoutMerchantIdClaim()` factory method to `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java` — creates a token with `merchant:payments:read` role but no `merchant_id` claim, needed by authorization matrix row 8. Method name follows existing `merchantPaymentReaderToken()` naming convention.
- [x] T007 [AGENT-REVIEW] Verify `merchantPaymentReaderTokenWithoutMerchantIdClaim()` compiles with `./mvnw -pl . test-compile` — token must contain `realm_access.roles=["merchant:payments:read"]` and no `merchant_id` claim

**Checkpoint**: Test support foundation ready - user story test implementation can now begin

---

## Phase 3: User Story 1 - HTTP Edge Contract Hardening (Priority: P1) 🎯 MVP

**Goal**: Verify that the existing Payment Order Summary endpoint handles HTTP protocol edge cases correctly: route collision avoidance, malformed path variables, unsupported methods, content negotiation, and conditional header discipline.

**Independent Test**: Run `./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test` — all 5 tests must pass independently.

### Implementation for User Story 1

- [x] T008 [P] [US1] [AGENT-IMPLEMENT] Create `PaymentOrderSummaryHttpContractRestAssuredTest.java` class skeleton with `@SpringBootTest(RANDOM_PORT)`, `@Import(TestJwtConfiguration.class)`, `@Testcontainers`, extending `PostgresContainerSupport` in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java`
- [x] T009 [P] [US1] [TESTER-AUTOMATE] Implement `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape()` test — sends `GET /api/merchants/{merchantId}/payment-orders/summary` with valid token, asserts `200 OK`, `Content-Type: application/json`, response contains `totalOrders` (summary shape) not `paymentOrderId` (single-order shape) in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java`
- [x] T010 [P] [US1] [TESTER-AUTOMATE] Implement `malformedMerchantIdReturnsValidationError()` as a parameterized test covering 3 malformed UUID variants — `not-a-uuid`, `12345`, `null` — sends `GET /api/merchants/{variant}/payment-orders/summary` with valid token, asserts `400 Bad Request`, `error=validation`, message contains `must be a valid UUID`, `X-Correlation-ID` not null in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java`
- [x] T011 [P] [US1] [TESTER-AUTOMATE] Implement `unsupportedMethodsDoNotExposeSummaryMutationSurface()` test — sends `PUT`, `PATCH`, `DELETE` to `/api/merchants/{merchantId}/payment-orders/summary` with valid token, asserts `405 Method Not Allowed` (or characterizes actual if Spring differs), `Allow` header contains `GET`, no mutation surface exposed in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java`
- [x] T012 [P] [US1] [TESTER-AUTOMATE] Implement `unsupportedAcceptIsRejectedOrExplicitlyCharacterized()` test — sends `GET /api/merchants/{merchantId}/payment-orders/summary` with `Accept: text/xml`, asserts status based on T001 characterization results (`406` or `200`), documents Spring MVC content negotiation behavior in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java`
- [x] T013 [US1] [TESTER-AUTOMATE] Implement `ifNoneMatchDoesNotEnableSummaryCaching()` test — sends `GET /api/merchants/{merchantId}/payment-orders/summary` with `If-None-Match: \"some-etag\"` header and valid token, asserts `200 OK`, no `ETag` header in response, normal summary body returned in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java`
- [x] T014 [US1] [AGENT-REVIEW] Run `./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test` and verify all 5 tests pass, then run regression suite `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` to confirm no regressions. Also review T009-T013 against existing `PaymentOrderSummaryRestAssuredTest` to confirm no duplicate coverage (per NFR-403).

**Checkpoint**: HTTP edge contract hardened — all 5 tests pass, regression suite green. Route collision guardrail, malformed UUID, unsupported methods, content negotiation, and conditional header behavior are explicitly tested or characterized.

---

## Phase 4: User Story 2 - Parameterized Authorization Matrix (Priority: P1)

**Goal**: Verify that the existing Payment Order Summary endpoint enforces authentication, role authorization, and merchant ownership correctly through a parameterized test matrix with explicit BOLA and BFLA labels.

**Independent Test**: Run `./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test` — all parameterized rows must pass independently.

### Implementation for User Story 2

- [x] T015 [P] [US2] [AGENT-IMPLEMENT] Create `PaymentOrderSummaryAuthorizationMatrixTest.java` class skeleton with `@SpringBootTest(RANDOM_PORT)`, `@Import(TestJwtConfiguration.class)`, `@Testcontainers`, extending `PostgresContainerSupport`, using `@ParameterizedTest` and `@MethodSource` pattern in `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java`
- [x] T016 [P] [US2] [AGENT-IMPLEMENT] Define `SummaryAccessCase` record and `summaryAccessMatrix()` `@MethodSource` factory method returning 12 `Arguments.of(...)` rows with displayName, tokenSupplier, targetMerchantIdSupplier, expectedStatus, bolaBflaLabel in `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java`
- [x] T017 [US2] [TESTER-AUTOMATE] Implement the `@ParameterizedTest` method `summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership()` — for each row, creates merchant(s) as needed, obtains token from supplier, sends `GET /api/merchants/{merchantId}/payment-orders/summary`, asserts expected status code, includes BOLA/BFLA label in assertion message in `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java`
- [x] T018 [US2] [TESTER-AUTOMATE] Implement per-row assertion logic: for `200` rows verify `Content-Type: application/json` and `totalOrders` field present; for `403` rows verify `Content-Type: application/json` and `error=forbidden` for controller-rejected rows; for `401` rows verify no `PaymentErrorResponse` body (Spring Security handles) in `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java`
- [x] T019 [US2] [AGENT-REVIEW] Review matrix test output — confirm BOLA and BFLA labels visible in test display names, confirm no token values leaked in assertion failure messages, confirm all 12 matrix rows produce correct status codes in `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java`
- [x] T020 [US2] [AGENT-REVIEW] Run `./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test` and verify all 12 parameterized rows pass

**Checkpoint**: Authorization matrix hardened — all 12 rows pass, BOLA and BFLA labels visible, no token leakage. Auth policy for 401, 403, and 200 outcomes verified across all actor/token/merchant combinations.

---

## Phase 5: User Story 3 - Aggregation Diagnostic (Priority: P3 — Optional)

**Goal**: Optionally add a repository/service-level aggregation diagnostic test or EXPLAIN learning note to strengthen the DB oracle capability for the summary endpoint.

**Independent Test**: Run diagnostic test or document EXPLAIN output. This story is independent of US1 and US2.

### Implementation for User Story 3 (OPTIONAL — only if time remains after US1+US2 are green)

- [ ] T021 [P] [US3] [TESTER-DESIGN] Write `EXPLAIN ANALYZE` query in SQL console against the summary aggregation query and document index usage, scan types, and estimated costs in a learning note at `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 10 - Business Logic, Decision Tables, and Risk Notes.md`
- [ ] T022 [P] [US3] [TESTER-AUTOMATE] Add a repository-level aggregation diagnostic test that verifies `JpaPaymentOrderRepository` aggregation methods return correct totals when called directly (bypassing the controller) in `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepositoryAggregationTest.java`

**Checkpoint**: Aggregation diagnostics complete. DB oracle capability strengthened.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification, evidence capture, and lesson handoff.

- [x] T023 [AGENT-REVIEW] Run full regression suite: `./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest,PaymentOrderSummaryAuthorizationMatrixTest test` AND `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` AND `./mvnw -Dtest=PaymentModuleTest test` AND `./mvnw -DskipTests package` — all must pass
- [x] T024 [AGENT-EXPLAIN] Update `Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix.md` in `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/` with actual test file paths, test counts, command results, and implementation notes
- [x] T025 [AGENT-EXPLAIN] Update `Lesson Evidence Tracker.md` in `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/` with test evidence (files, test counts, command results), residual risks, and interview answer
- [x] T026 [AGENT-EXPLAIN] Update `Current Lesson.md` and `Current Sprint.md` in `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/` — mark Lesson 10 as completed, update evidence checklist
- [x] T027 [AGENT-EXPLAIN] Update `Learning Coverage Backlog.md` in `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/` — mark HTTP/REST `Accept`, content negotiation, BOLA, BFLA, parameterized tests as Practiced or Evidence Strong
- [x] T028 [AGENT-EXPLAIN] Update `specs/007-rest-http-contract-hardening-authorization-matrix/checklists/requirements.md` — check off all completed items

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) and Setup characterization (T001-T003)
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) — independently testable from US1
- **User Story 3 (Phase 5)**: Depends on Foundational (Phase 2) — optional, independently testable
- **Polish (Phase 6)**: Depends on US1 + US2 complete (US3 optional)

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2). No dependencies on other stories.
- **User Story 2 (P1)**: Can start after Foundational (Phase 2). No dependencies on US1. Needs T006 (`merchantPaymentReaderTokenWithoutMerchantIdClaim()`).
- **User Story 3 (P3)**: Can start after Foundational (Phase 2). Optional. Independent of US1 and US2.

### Within Each User Story

- T008 (skeleton) before T009-T013 (individual tests) for US1
- T015 (skeleton) and T016 (record/matrix) before T017-T018 (implementation) for US2
- T009-T012 marked [P] can be implemented in parallel for US1
- T015-T016 marked [P] can be implemented in parallel for US2
- US1 and US2 can be implemented in parallel by different team members

### Parallel Opportunities

- T001, T002, T003 (characterization) can run in parallel
- T004, T005 (regression baseline) can run in parallel
- T009, T010, T011, T012 (US1 tests) can be implemented in parallel after T008
- T015, T016 (US2 skeleton + matrix) can be implemented in parallel
- T021, T022 (US3 optional tasks) can run in parallel
- T024, T025, T026, T027, T028 (vault updates) can run in parallel
- US1 and US2 phases can run in parallel after Phase 2

---

## Parallel Example: User Story 1

```bash
# After T008 (skeleton), launch all US1 tests together:
Task: "T009 [US1] summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape"
Task: "T010 [US1] malformedMerchantIdReturnsValidationError"
Task: "T011 [US1] unsupportedMethodsDoNotExposeSummaryMutationSurface"
Task: "T012 [US1] unsupportedAcceptIsRejectedOrExplicitlyCharacterized"
# Then T013 (depends on T001 characterization result)
```

## Parallel Example: User Story 2

```bash
# Launch US2 skeleton and matrix definition together:
Task: "T015 [US2] Create PaymentOrderSummaryAuthorizationMatrixTest skeleton"
Task: "T016 [US2] Define SummaryAccessCase record and summaryAccessMatrix() factory"
# Then T017 and T018 (implementation) after skeleton complete
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (characterization + baseline) — T001-T005
2. Complete Phase 2: Foundational (test support) — T006-T007
3. Complete Phase 3: User Story 1 (HTTP edge contracts) — T008-T014
4. **STOP and VALIDATE**: Run T014 verification — all 5 HTTP edge tests pass
5. This alone delivers the primary learning value of HTTP edge hardening

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → 5 HTTP edge tests pass (MVP!)
3. Add User Story 2 → Test independently → 12 matrix rows pass
4. Optional: Add User Story 3 → Aggregation diagnostics
5. Polish: Update vault evidence → Lesson 10 complete

### Parallel Team Strategy

With two team members after Phase 2:
- Developer A: User Story 1 (T008-T014) — `PaymentOrderSummaryHttpContractRestAssuredTest`
- Developer B: User Story 2 (T015-T020) — `PaymentOrderSummaryAuthorizationMatrixTest`
- Both classes are in different packages (`rest/` vs `security/`) — no file conflicts

---

## Notes

- [P] tasks = different files, no dependencies
- [US1], [US2], [US3] labels map tasks to user stories
- All tasks produce test code — no production code changes unless characterization reveals a real bug
- Token values must not appear in test failure output — review assertion messages and REST Assured logging config
- Characterize before asserting — Spring MVC defaults may differ from expectations for `Accept` and unsupported methods
- Existing `PaymentOrderSummaryApiTestSupport.summaryReaderRequest()` helper is reusable for both US1 and US2 tests
- Each test creates its own merchant via `PaymentApiTestSupport.createActiveMerchant()` — no shared mutable state
