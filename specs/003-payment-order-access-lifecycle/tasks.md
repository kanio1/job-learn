# Tasks: Payment Order Access, Idempotent Creation, And Minimal Create/Read Lifecycle Foundation

**Input**: Design documents from `specs/003-payment-order-access-lifecycle/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/payment-order-api.md, quickstart.md

**Tests**: Included because the feature changes API contracts, security decisions, persistence, concurrency, and idempotency behavior. All FR requirements have explicit test coverage.

**Organization**: Tasks grouped by user story (US1 = P1 create, US2 = P2 read, US3 = P3 security boundary, US4 = P4 dashboard) with foundational prerequisites first.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- **Label**: Prefix with one lab label: `[AGENT-IMPLEMENT]`, `[AGENT-EXPLAIN]`, `[TESTER-ANALYZE]`, `[TESTER-DESIGN]`, `[TESTER-AUTOMATE]`, `[AGENT-REVIEW]`, `[DISCUSS]`
- Include exact file paths in descriptions

## Validation Commands

- Backend build: `./mvnw clean compile` (from `apps/backend`)
- Backend tests: `./mvnw test` (from `apps/backend`)
- Backend integration: `./mvnw verify` (from `apps/backend`)
- Architecture: verify `ApplicationModules.verify()` passes in `ModulithArchitectureTest`
- Frontend typecheck: `pnpm typecheck` (from `apps/frontend`)
- Frontend e2e: `pnpm test:e2e` (from `apps/frontend`)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Merchant public API boundary and payment module scaffolding that all user stories depend on.

- [x] T001 [AGENT-IMPLEMENT] Create merchant public eligibility API: `MerchantPaymentEligibility` record and `MerchantPaymentEligibilityService` interface in `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibility.java` and `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibilityService.java`. FR-004, FR-024.
- [x] T002 [AGENT-IMPLEMENT] Implement `MerchantPaymentEligibilityAdapter` in `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantPaymentEligibilityAdapter.java` delegating to existing `JpaMerchantRepository` for eligibility lookup. FR-004.
- [x] T003 [AGENT-IMPLEMENT] Create payment module `package-info.java` with `@ApplicationModule(displayName = "Payment Orders")` in `apps/backend/src/main/java/lab/paymentquality/payment/package-info.java`. FR-024.
- [x] T004 [AGENT-IMPLEMENT] Create payment internal package-info in `apps/backend/src/main/java/lab/paymentquality/payment/internal/package-info.java`.
- [x] T005 [TESTER-AUTOMATE] Verify merchant public API boundary loads in `apps/backend/src/test/java/lab/paymentquality/merchant/internal/application/MerchantPaymentEligibilityAdapterTest.java` with mocked repository and active/inactive/not-found scenarios. FR-004.

**Checkpoint**: Merchant eligibility API and payment module shell exist. `./mvnw clean compile` passes.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database schema, security extensions, domain value objects, and test infrastructure. BLOCKS all user stories.

- [x] T006 [AGENT-IMPLEMENT] Create Flyway migration `apps/backend/src/main/resources/db/migration/payment/V1__create_payment_orders.sql` with `payment_orders`, `idempotency_records`, `payment_order_status_history` tables, FK constraints, check constraints, unique idempotency constraint, and indexes per data-model.md. FR-003, FR-009, FR-010, FR-011, FR-020, FR-021.
- [x] T007 [AGENT-IMPLEMENT] Update Flyway config in `apps/backend/src/main/resources/application.yml` to include `classpath:db/migration/payment` location.
- [x] T008 [AGENT-IMPLEMENT] Create `PaymentStatus` enum with only `CREATED` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentStatus.java`. FR-003.
- [x] T009 [P] [AGENT-IMPLEMENT] Create `PaymentAmount` value object validating `1..100_000_000` minor units in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentAmount.java`. FR-009.
- [x] T010 [P] [AGENT-IMPLEMENT] Create `CurrencyCode` value object validating `PLN`, `EUR`, `USD` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/CurrencyCode.java`. FR-010.
- [x] T011 [P] [AGENT-IMPLEMENT] Create `ClientOrderReference` value object with trim and max 120 validation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/ClientOrderReference.java`. Data-model entity required by FR-001 (create includes client reference).
- [x] T012 [P] [AGENT-IMPLEMENT] Create `IdempotencyKey` value object with non-blank printable ASCII max 128 and SHA-256 hash in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/IdempotencyKey.java`. FR-005.
- [x] T013 [P] [AGENT-IMPLEMENT] Create `RequestFingerprint` value object with canonical JSON derivation and SHA-256 hash per research.md Decision 3 in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/RequestFingerprint.java`. FR-007, FR-008.
- [x] T014 [AGENT-IMPLEMENT] Create `PaymentOrder` JPA entity in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java` with UUID ID, merchantId, clientOrderReference, amountMinor, currency, status, timestamps, version, and factory method. FR-001, FR-002, FR-003, FR-011.
- [x] T015 [AGENT-IMPLEMENT] Create `IdempotencyRecord` JPA entity in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/IdempotencyRecord.java` with merchantId, idempotencyKeyHash, requestFingerprintHash, paymentOrderId, timestamps.
- [x] T016 [AGENT-IMPLEMENT] Create `PaymentOrderStatusHistory` JPA entity in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrderStatusHistory.java` with paymentOrderId, fromStatus, toStatus, actorSubject, correlationId, createdAt. FR-020.
- [x] T017 [AGENT-IMPLEMENT] Create `JpaPaymentOrderRepository` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java` with `findByMerchantIdAndPaymentOrderId` and `findByPaymentOrderId`. FR-012.
- [x] T018 [AGENT-IMPLEMENT] Create `JpaIdempotencyRecordRepository` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaIdempotencyRecordRepository.java` with `findByMerchantIdAndIdempotencyKeyHash`. FR-007, FR-008.
- [x] T019 [AGENT-IMPLEMENT] Create `JpaPaymentOrderStatusHistoryRepository` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderStatusHistoryRepository.java`. FR-020.
- [x] T020 [AGENT-IMPLEMENT] Update `KeycloakRealmRoleConverter` in `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java` to use mixed mapping: roles containing `:` pass through unchanged; legacy roles without `:` get `platform:` prefix. FR-025.
- [x] T021 [AGENT-IMPLEMENT] Extend `SecurityConfig` in `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java` with payment endpoint matchers for POST and GET payment-orders. FR-016, FR-017, FR-030, FR-031.
- [x] T022 [AGENT-IMPLEMENT] Update `TestJwtSupport` in `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java` to add `merchantPaymentCreatorToken(merchantId)`, `merchantPaymentReaderToken(merchantId)`, `merchantPaymentOperatorToken(merchantId)`, `platformPaymentReaderToken()`, and `merchantScopedDeniedToken(merchantId)` methods with appropriate roles and `merchant_id` claim. FR-025, FR-032, FR-034.
- [x] T023 [AGENT-IMPLEMENT] Create `PaymentApiTestSupport` in `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentApiTestSupport.java` with helper to create an active merchant and return its ID for payment tests. Supports NFR-002 (unique namespaced references without shared global record).
- [x] T024 [AGENT-IMPLEMENT] Update Keycloak realm in `infra/keycloak/realms/payment-quality-realm.json` to add `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, `platform:payments:read` roles, payment users, and `merchant_id` attribute mapper. Preserve existing Phase 1 roles/users. FR-025, FR-032, FR-034.
- [x] T025 [AGENT-EXPLAIN] Create payment domain value objects explanation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/` documenting money precision, currency EP, idempotency hashing, and request fingerprint canonicalization rationale.
- [x] T026 [TESTER-DESIGN] Create test conditions document for idempotency, money precision, ownership, cross-tenant leakage, concurrency, atomicity, and error contract testing in `specs/003-payment-order-access-lifecycle/checklists/test-conditions.md`.

**Checkpoint**: All domain, infrastructure, security, and test support exist. `./mvnw clean compile` passes. Flyway migration runs on Testcontainers PostgreSQL. Architecture verification still passes with payment module present.

---

## Phase 3: User Story 1 - Create Payment Order Idempotently (Priority: P1) :MVP

**Goal**: Merchant payment creator can create a payment order for their active merchant with idempotent behavior, receiving `201` on first create, `200` on replay, `409` on conflict.

**Independent Test**: Authenticate as merchant payment creator, POST to `/api/merchants/{merchantId}/payment-orders`, verify `201` with `Location`, `ETag`, `X-Correlation-ID`, body. Replay same request, verify `200` with same ID. Send different body with same key, verify `409`.

### Implementation for User Story 1

- [x] T027 [US1] [AGENT-IMPLEMENT] Create domain exceptions: `MerchantNotPaymentEligibleException`, `IdempotencyConflictException`, `PaymentOrderNotFoundException` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/`.
- [x] T028 [US1] [AGENT-IMPLEMENT] Create `PaymentCreateResult` record with created/replayed marker and `PaymentOrder` reference in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentCreateResult.java`.
- [x] T029 [US1] [AGENT-IMPLEMENT] Create `PaymentActorContext` record with subject, merchantId, and authorities in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentActorContext.java`.
- [x] T030 [US1] [AGENT-IMPLEMENT] Implement `PaymentOrderService.create()` method with idempotent creation flow per plan.md section 6 in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderService.java`. Depends on T001, T009-T019, T027-T029. FR-001, FR-004, FR-005, FR-007, FR-008, FR-021.
- [x] T031 [US1] [AGENT-IMPLEMENT] Create `CreatePaymentOrderRequest` DTO with Bean Validation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/CreatePaymentOrderRequest.java`.
- [x] T032 [US1] [AGENT-IMPLEMENT] Create `PaymentOrderResponse` DTO in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderResponse.java`.
- [x] T033 [US1] [AGENT-IMPLEMENT] Create `PaymentOrderMapper` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderMapper.java`.
- [x] T034 [US1] [AGENT-IMPLEMENT] Create `PaymentErrorResponse` DTO in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentErrorResponse.java`.
- [x] T035 [US1] [AGENT-IMPLEMENT] Implement `PaymentOrderController.createPaymentOrder()` with `POST /api/merchants/{merchantId}/payment-orders`, `Idempotency-Key` header binding, `X-Correlation-ID` propagation, `Location`/`ETag`/`X-Correlation-ID` response headers in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`. ETag format: `"po-<paymentOrderId>-v<version>"` per research.md Decision 5. Verify existing `CorrelationIdFilter` applies to new payment paths. Depends on T030-T034. FR-001, FR-005, FR-006, FR-022, FR-023.
- [x] T036 [US1] [AGENT-IMPLEMENT] Create `PaymentExceptionHandler` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java` mapping domain exceptions to HTTP status codes and stable error codes per plan.md section 6. Error messages must be clear enough to distinguish validation, idempotency conflict, missing access, insufficient authority, not found, and merchant eligibility. FR-019, NFR-008.
- [ ] T037 [US1] [AGENT-REVIEW] Verify create endpoint returns correct status codes, headers, and body for success, replay, conflict, validation, and eligibility failure scenarios. Run `./mvnw clean compile`. FR-001 through FR-008, FR-019, FR-021, FR-022, FR-023.

### Tests for User Story 1

- [x] T038 [P] [US1] [TESTER-AUTOMATE] Domain unit tests for `PaymentAmount` BVA (0, 1, 100000000, 100000001, negative) in `apps/backend/src/test/java/lab/paymentquality/payment/internal/domain/PaymentAmountTest.java`. FR-009.
- [x] T039 [P] [US1] [TESTER-AUTOMATE] Domain unit tests for `CurrencyCode` EP (PLN/EUR/USD valid, unsupported uppercase, lowercase, malformed) in `apps/backend/src/test/java/lab/paymentquality/payment/internal/domain/CurrencyCodeTest.java`. FR-010.
- [x] T040 [P] [US1] [TESTER-AUTOMATE] Domain unit tests for `ClientOrderReference` validation in `apps/backend/src/test/java/lab/paymentquality/payment/internal/domain/ClientOrderReferenceTest.java`.
- [x] T041 [P] [US1] [TESTER-AUTOMATE] Domain unit tests for `IdempotencyKey` validation and hash determinism in `apps/backend/src/test/java/lab/paymentquality/payment/internal/domain/IdempotencyKeyTest.java`. FR-005.
- [x] T042 [US1] [TESTER-AUTOMATE] Application service test for `PaymentOrderService.create()` covering successful create, idempotent replay, idempotency conflict, non-active merchant, and status history creation in `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderServiceTest.java`. FR-001, FR-004, FR-007, FR-008, FR-020, FR-021.
- [x] T043 [US1] [TESTER-AUTOMATE] Concurrency test proving near-simultaneous same-key same-fingerprint requests create exactly one payment order in `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderIdempotencyConcurrencyTest.java`. FR-007, NFR-004.
- [x] T044 [US1] [TESTER-AUTOMATE] Repository test verifying Flyway migration, payment order persistence, idempotency unique constraint, FK behavior, and check constraints in `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepositoryTest.java`. FR-003, FR-009, FR-010, FR-011.
- [x] T045 [US1] [TESTER-AUTOMATE] REST Assured contract test for POST create endpoint covering `201` success (headers: Location, ETag, X-Correlation-ID; body), `200` replay, `409` idempotency conflict, `400` validation errors, `409` merchant not eligible in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`. FR-001 through FR-010, FR-019, FR-022, FR-023.

**Checkpoint**: Payment order create works end-to-end. `./mvnw verify` passes with all US1 tests. MVP is deliverable.

---

## Phase 4: User Story 2 - Read Payment Order Within Merchant Scope (Priority: P2)

**Goal**: Merchant payment reader retrieves payment order by ID within their merchant scope; platform reader reads any merchant's order; cross-tenant read returns masked `404`.

**Independent Test**: Create a payment order for merchant A, read with merchant A reader token (`200`), read with merchant B reader token (`404`), read with platform reader token (`200`).

### Implementation for User Story 2

- [x] T046 [US2] [AGENT-IMPLEMENT] Add `PaymentOrderService.findForMerchant()` and `findForPlatform()` methods to `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderService.java`. FR-012, FR-013, FR-033.
- [x] T047 [US2] [AGENT-IMPLEMENT] Add `PaymentOrderController.getPaymentOrder()` with `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`, ETag format `"po-<paymentOrderId>-v<version>"` per research.md Decision 5, and X-Correlation-ID response headers in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`. FR-012, FR-014, FR-015, FR-022, FR-023.
- [x] T048 [US2] [AGENT-REVIEW] Verify read endpoint returns `200` for own merchant, masked `404` for cross-tenant, `404` for unknown ID, `400` for malformed ID. Run `./mvnw clean compile`. FR-012 through FR-015, FR-022, FR-023.

### Tests for User Story 2

- [x] T049 [US2] [TESTER-AUTOMATE] Application service test for `findForMerchant()` success, cross-tenant masked not-found, and unknown ID in `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderServiceTest.java` (extend existing). FR-012, FR-013, FR-014.
- [x] T050 [US2] [TESTER-AUTOMATE] REST Assured contract test for GET read endpoint covering `200` success with ETag, masked `404` cross-tenant, `404` unknown ID, `400` malformed ID, platform reader cross-merchant `200` in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java` (extend existing). FR-012 through FR-015, FR-033.

**Checkpoint**: Read works for merchant-scoped and platform readers. Cross-tenant isolation verified. `./mvnw verify` passes.

---

## Phase 5: User Story 3 - Enforce Payment Access Boundary (Priority: P3)

**Goal**: All payment endpoints are protected: `401` for missing/invalid auth, `403` for missing role, `403` for operate-only, `403` for create-without-create-role, masked `404` for cross-tenant, platform reader cross-merchant access.

**Independent Test**: Exercise all actor types (unauthenticated, denied, creator-only, reader-only, operate-only, platform reader) against create and read endpoints.

### Implementation for User Story 3

- [x] T051 [US3] [AGENT-REVIEW] Verify `SecurityConfig` endpoint matchers cover all payment paths and `GET /api/status` remains public. Run `./mvnw clean compile`. FR-026, FR-030, FR-031.
- [x] T052 [US3] [AGENT-REVIEW] Verify `KeycloakRealmRoleConverter` maps all 7 realm roles correctly (3 merchant legacy, 4 payment namespaced). FR-025.

### Tests for User Story 3

- [x] T053 [US3] [TESTER-AUTOMATE] Security matrix test in `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java` covering: unauthenticated `401` (create+read), expired/invalid/issuer-wrong token `401`, denied identity `403`, merchant creator can create but not read `403`, merchant reader can read but not create `403`, operate-only `403` for both, platform reader can read cross-merchant `200` but cannot create `403`. FR-016, FR-017, FR-018, FR-025, FR-030, FR-031, FR-033.
- [x] T054 [US3] [TESTER-AUTOMATE] Verify `merchant:payments:operate` role is registered in Keycloak realm and test JWT but grants no create/read access. Test both Keycloak JSON content and security matrix outcomes. FR-025, FR-032, SC-011.
- [x] T055 [US3] [TESTER-AUTOMATE] Verify `GET /api/status` remains public and returns `200` without payment data for unauthenticated users. FR-026.

**Checkpoint**: Full security boundary verified. All `401`/`403`/masked `404`/`200` scenarios pass. `./mvnw verify` passes.

---

## Phase 6: User Story 4 - Dashboard Payment Order Journey (Priority: P4)

**Goal**: Authenticated merchant user creates payment order and views detail through the dashboard. Create form validates, submits, shows success. Detail page shows status, amount, currency, reference, timestamps.

**Independent Test**: Sign in as merchant payment creator, navigate to merchant detail, create payment order, verify detail page shows `CREATED`. Sign in as reader, verify read-only view. Sign in as user without create authority, verify create action is hidden.

### Implementation for User Story 4

- [x] T056 [P] [US4] [AGENT-IMPLEMENT] Create Zod schema for payment order form validation (amountMinor `1..100_000_000`, currency enum, clientOrderReference trim max 120) in `apps/frontend/app/schemas/payment-order.schema.ts`. FR-009, FR-010.
- [x] T057 [P] [US4] [AGENT-IMPLEMENT] Create Pinia payment-orders store with transient create/read state, loading, error feedback in `apps/frontend/app/stores/payment-orders.ts`. Must not store tokens.
- [x] T058 [US4] [AGENT-IMPLEMENT] Create Nuxt server API handler for POST create with idempotency key and correlation ID in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/index.post.ts`. Uses existing server-side backendApi utility. FR-001, FR-005, FR-006.
- [x] T059 [US4] [AGENT-IMPLEMENT] Create Nuxt server API handler for GET read in `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].get.ts`. FR-012.
- [x] T060 [P] [US4] [AGENT-IMPLEMENT] Create `PaymentStatusBadge.vue` component displaying `CREATED` badge in `apps/frontend/app/components/payment/PaymentStatusBadge.vue`. FR-003, FR-028.
- [x] T061 [US4] [AGENT-IMPLEMENT] Create `CreatePaymentOrderForm.vue` with Nuxt UI form, amount/currency/reference fields, Zod validation, submit handler, field-level errors, success/loading/error feedback in `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue`. FR-028, FR-029.
- [x] T062 [US4] [AGENT-IMPLEMENT] Create `PaymentOrderDetail.vue` showing status, amount, currency, reference, timestamps in `apps/frontend/app/components/payment/PaymentOrderDetail.vue`. FR-028.
- [x] T063 [US4] [AGENT-IMPLEMENT] Create payment create page at `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue` with form component and role-aware visibility. FR-027, FR-028.
- [x] T064 [US4] [AGENT-IMPLEMENT] Create payment detail page at `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue` with detail component. FR-027.
- [x] T065 [US4] [AGENT-IMPLEMENT] Add payment navigation link to merchant detail page for users with create authority in `apps/frontend/app/pages/admin/merchants.vue`. FR-027, FR-028.
- [ ] T066 [US4] [AGENT-REVIEW] Verify frontend typecheck passes. Run `pnpm typecheck` from `apps/frontend`. Verify no lifecycle buttons, PSP placeholders, or payment list UI exists. FR-026, FR-028, FR-029.

### Tests for User Story 4

- [x] T067 [P] [US4] [TESTER-AUTOMATE] Playwright create journey: sign in as merchant payment creator, navigate to merchant detail, open create payment order, fill valid form, submit, verify detail shows `CREATED` status in `apps/frontend/tests/e2e/payment-order-create.spec.ts`. FR-027, FR-028, FR-029, SC-001, SC-008.
- [x] T068 [P] [US4] [TESTER-AUTOMATE] Playwright read journey: sign in as merchant payment reader, navigate to existing payment order detail, verify status/amount/currency/reference visible in `apps/frontend/tests/e2e/payment-order-read.spec.ts`. FR-027, FR-028.
- [x] T069 [US4] [TESTER-AUTOMATE] Playwright auth denial: sign in as user without create authority, verify create action is hidden; attempt direct navigation, verify redirect or backend denial in `apps/frontend/tests/e2e/payment-order-auth-deny.spec.ts`. FR-028, FR-030, FR-031.

**Checkpoint**: Full dashboard journey works. `pnpm typecheck` and `pnpm test:e2e` pass. Payment order create and detail are accessible to authorized users only.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Architecture verification, documentation, and scope guardrail checks across all stories.

- [x] T070 [AGENT-REVIEW] Verify `ModulithArchitectureTest` in `apps/backend/src/test/java/lab/paymentquality/architecture/ModulithArchitectureTest.java` passes with payment module: no `merchant.internal` imports, no cycles, `ApplicationModules.verify()`. FR-024.
- [x] T071 [AGENT-REVIEW] Verify `PaymentModuleTest` in `apps/backend/src/test/java/lab/paymentquality/payment/PaymentModuleTest.java` boots payment module with test profile and verifies beans and boundaries. FR-024.
- [ ] T072 [AGENT-REVIEW] Verify no lifecycle action endpoints exist (authorize, capture, cancel), no `If-Match` handling, no `412`, no payment list/search, no PSP/card/Kafka/webhook code. FR-026.
- [ ] T073 [AGENT-REVIEW] Verify `GET /api/status` remains public and unchanged. FR-026.
- [ ] T074 [AGENT-REVIEW] Verify logs do not contain tokens, authorization headers, or raw idempotency keys. NFR secret safety.
- [ ] T075 [TESTER-ANALYZE] Create tester orientation document for payment order feature covering risks, test conditions, learning value, and connection to Lesson 6 in `docs/setup/phase-2-payment-order-orientation-pack.md`. NFR-008, Constitution I (Tester-Led Product Learning).
- [ ] T076 [TESTER-ANALYZE] Create test design notes for idempotency, money precision, ownership, cross-tenant isolation, concurrency, and security matrix testing in `docs/testing/phase-2-payment-order-test-design.md`. NFR-005, Constitution IV (Parallel-Ready Quality Engineering), Constitution V (Security, Data Integrity, and Observability by Design).
- [ ] T077 [AGENT-EXPLAIN] Run quickstart.md walkthrough from `specs/003-payment-order-access-lifecycle/quickstart.md` and verify all manual API and dashboard steps work.
- [ ] T078 [DISCUSS] Review scope guardrails: confirm no lifecycle actions, PSP, Kafka, webhooks, refunds, settlement, reconciliation, GraphQL, gRPC, Client Credentials, or full team management was introduced.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 completion — BLOCKS all user stories
- **Phase 3 (US1 Create)**: Depends on Phase 2 completion
- **Phase 4 (US2 Read)**: Depends on Phase 2 completion (can run parallel with US1 if different agent)
- **Phase 5 (US3 Security)**: Depends on Phase 2 completion (can run parallel with US1/US2)
- **Phase 6 (US4 Dashboard)**: Depends on Phase 2 for backend API contract; can start frontend after T056-T064 in parallel with US1 backend
- **Phase 7 (Polish)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1 Create)**: After Phase 2 — no dependency on US2, US3, or US4
- **US2 (P2 Read)**: After Phase 2 — independent of US1 but T049-T050 extend US1 test files
- **US3 (P3 Security)**: After Phase 2 — independent; tests exercise both create and read endpoints
- **US4 (P4 Dashboard)**: After Phase 2 backend API exists; frontend Zod/store/server handlers can start in parallel with US1 backend

### Within Each User Story

- Models and value objects before services
- Services before controllers/endpoints
- Core implementation before tests
- Tests before review checkpoints

### Parallel Opportunities

- T009, T010, T011, T012, T013 can run in parallel (different value object files)
- T038, T039, T040, T041 can run in parallel (different test files)
- T056, T057, T060 can run in parallel (different frontend files)
- T067, T068 can run in parallel (different Playwright spec files)
- US1 backend, US2, US3 can proceed in parallel after Phase 2

---

## Parallel Example: User Story 1

```bash
# Launch all domain value object tests together:
Task T038: "PaymentAmount BVA tests"
Task T039: "CurrencyCode EP tests"
Task T040: "ClientOrderReference tests"
Task T041: "IdempotencyKey tests"
```

---

## Parallel Example: User Story 4

```bash
# Launch independent frontend files together:
Task T056: "Zod schema"
Task T057: "Pinia store"
Task T060: "PaymentStatusBadge component"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: `./mvnw verify` — all US1 tests pass
5. MVP is deliverable: idempotent create works

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → `./mvnw verify` → MVP with idempotent create
3. US2 → `./mvnw verify` → read completes create/read slice
4. US3 → `./mvnw verify` → full security matrix verified
5. US4 → `pnpm typecheck` + `pnpm test:e2e` → dashboard journey complete
6. Polish → architecture + docs + scope guardrail review

---

## Requirement Traceability Summary

| FR | Phase | Key Tasks |
|---|---|---|
| FR-001 | 3 | T030, T035, T045 |
| FR-002 | 2 | T014 |
| FR-003 | 2, 3 | T006, T008, T014, T044 |
| FR-004 | 1, 3 | T001, T002, T005, T030 |
| FR-005 | 2, 3 | T012, T035, T041, T045 |
| FR-006 | 3 | T035, T045 |
| FR-007 | 2, 3 | T013, T018, T030, T042, T043, T045 |
| FR-008 | 2, 3 | T013, T018, T030, T042, T045 |
| FR-009 | 2, 3 | T006, T009, T031, T038, T045 |
| FR-010 | 2, 3 | T006, T010, T031, T039, T045 |
| FR-011 | 2, 3 | T006, T014, T044 |
| FR-012 | 4 | T046, T047, T049, T050 |
| FR-013 | 4, 5 | T046, T050, T053 |
| FR-014 | 4 | T047, T050 |
| FR-015 | 4 | T047, T050 |
| FR-016 | 5 | T021, T053 |
| FR-017 | 5 | T021, T053 |
| FR-018 | 5 | T053 |
| FR-019 | 3 | T034, T036, T045 |
| FR-020 | 2, 3 | T016, T030, T042 |
| FR-021 | 2, 3 | T006, T030, T042, T044 |
| FR-022 | 3, 4 | T035, T045, T047, T050 |
| FR-023 | 3, 4 | T035, T045, T047, T050 |
| FR-024 | 1, 7 | T003, T070, T071 |
| FR-025 | 2, 5 | T020, T022, T024, T053, T054 |
| FR-026 | 5, 7 | T055, T072, T073 |
| FR-027 | 6 | T063, T064, T065, T067, T068 |
| FR-028 | 6 | T060, T061, T062, T063, T064, T067, T068, T069 |
| FR-029 | 6 | T061, T067 |
| FR-030 | 5, 6 | T021, T053, T069 |
| FR-031 | 5, 6 | T021, T053, T069 |
| FR-032 | 2, 5 | T022, T024, T054 |
| FR-033 | 4, 5 | T046, T050, T053 |
| FR-034 | 2, 5 | T022, T024 |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Verify `./mvnw clean compile` after each phase
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Do not implement lifecycle actions, PSP, Kafka, webhooks, or settlement
- Do not add `data-testid` unless Nuxt UI interaction cannot be located semantically
