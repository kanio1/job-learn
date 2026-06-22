# Tasks: Payment Order List with Filtering, Pagination, and Sorting

**Input**: Design documents from `specs/004-payment-order-list-filter/`

**Prerequisites**: plan.md, spec.md

**Tests**: Included because the feature introduces a new API endpoint, query parameter validation, dynamic JPA queries, pagination contract, security decisions, database indexes, and 7 previously unused REST Assured methods. All 29 FRs have explicit test coverage.

**Organization**: Tasks grouped by user story (US1 = P1 list with filters, US2 = P2 pagination, US3 = P3 sort, US4 = P3 query validation, US5 = P2 access boundary) with foundational prerequisites first.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, US5)
- **Label**: Prefix with one lab label: `[AGENT-IMPLEMENT]`, `[AGENT-EXPLAIN]`, `[TESTER-ANALYZE]`, `[TESTER-DESIGN]`, `[TESTER-AUTOMATE]`, `[AGENT-REVIEW]`, `[DISCUSS]`
- Include exact file paths in descriptions

## Validation Commands

- Backend build: `./mvnw clean compile` (from `apps/backend`)
- Backend tests: `./mvnw test` (from `apps/backend`)
- Backend specific: `./mvnw -Dtest="PaymentOrderList*" test` (list tests only)
- Backend specific: `./mvnw -Dtest="PaymentOrder*" test` (all payment tests)
- Architecture: verify `ApplicationModules.verify()` passes in `PaymentModuleTest`
- Frontend typecheck: `corepack pnpm typecheck` (from `apps/frontend`)

---

## Phase 1: Setup (Domain and Infrastructure Prerequisites)

**Purpose**: Web layer request/response types, JPA specification infrastructure, and database migration that all user stories depend on.

- [ ] T001 [AGENT-IMPLEMENT] Create `PaymentOrderListRequest` record with `@Valid` annotations for query parameter validation in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListRequest.java`. Fields: `status` (`@Pattern` CREATED), `currency` (`@Pattern` PLN|EUR|USD), `fromDate`, `toDate`, `minAmount` (`@PositiveOrZero`), `maxAmount` (`@PositiveOrZero`), `clientOrderReference`, `page` (`@PositiveOrZero`), `size` (`@Min(1)` `@Max(100)`), `sort` (`@Pattern` createdAt,(asc|desc)). FR-202 to FR-208, FR-211, FR-213.
- [ ] T002 [P] [AGENT-IMPLEMENT] Create `PaymentOrderListResponse` record in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListResponse.java`. Fields: `List<PaymentOrderResponse> content`, `int page`, `int size`, `long totalElements`, `int totalPages`. FR-215.
- [ ] T003 [P] [AGENT-IMPLEMENT] Create `PaymentOrderListMapper` utility in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListMapper.java` mapping `Page<PaymentOrder>` → `PaymentOrderListResponse`. FR-215.
- [ ] T004 [AGENT-IMPLEMENT] Create `PaymentOrderSpecification` utility class with static `Specification<PaymentOrder>` builder methods in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/PaymentOrderSpecification.java`. Methods: `hasMerchantId(UUID)`, `hasStatus(String)` (null-safe, returns null for absent), `hasCurrency(String)`, `createdBetween(LocalDate, LocalDate)`, `amountBetween(Long, Long)`, `clientOrderReferenceContains(String)` (ILIKE escaped). FR-201, FR-202 to FR-208, FR-229.
- [ ] T005 [AGENT-IMPLEMENT] Extend `JpaPaymentOrderRepository` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java` to also extend `JpaSpecificationExecutor<PaymentOrder>`. No new query methods needed — specifications handle all dynamic queries. FR-225.
- [ ] T006 [AGENT-IMPLEMENT] Create Flyway migration `apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql` with three composite indexes: `idx_payment_orders_merchant_status ON payment_orders(merchant_id, status)`, `idx_payment_orders_merchant_created ON payment_orders(merchant_id, created_at DESC)`, `idx_payment_orders_merchant_currency ON payment_orders(merchant_id, currency)`. FR-228.

**Checkpoint**: Request/response types, specification builders, repository extension, and migration exist. `./mvnw clean compile` passes. Migration runs on Testcontainers.

---

## Phase 2: User Story 1 - List Payment Orders with Optional Filters (Priority: P1) :MVP

**Goal**: Merchant payment reader retrieves all payment orders belonging to their merchant, optionally filtered by status, currency, date range, amount range, and client reference text search.

**Independent Test**: Create 5 payment orders with varied data via `PaymentOrderListApiTestSupport`, call `GET /api/merchants/{merchantId}/payment-orders?currency=PLN`, assert response contains only PLN orders using `extract().as(PaymentOrderListResponse.class)` and AssertJ `filteredOn`.

### Implementation for User Story 1

- [ ] T007 [US1] [AGENT-IMPLEMENT] Create `PaymentOrderListService` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java` with `findAll(UUID merchantId, PaymentOrderListRequest request)` method. Annotate `@Transactional(readOnly = true)`. Use `JpaPaymentOrderRepository.findAll(Specification, Pageable)`. Default page=0, size=20, sort=createdAt,desc. FR-201, FR-209, FR-210, FR-211, FR-213, FR-226.
- [ ] T008 [US1] [AGENT-IMPLEMENT] Add `listPaymentOrders()` method to `PaymentOrderController` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`. Endpoint: `@GetMapping` on `GET /api/merchants/{merchantId}/payment-orders`. Bind `@RequestParam` with defaults. Apply `merchant_id` claim enforcement (identical pattern to existing `createPaymentOrder`/`getPaymentOrder`). Return `200 OK` with `PaymentOrderListResponse` body and `X-Correlation-ID` header. FR-201, FR-216, FR-217, FR-218, FR-219, FR-220, FR-221.
- [ ] T009 [US1] [AGENT-IMPLEMENT] Extend `PaymentExceptionHandler` in `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java` to handle query parameter `MethodArgumentNotValidException` and `BindException` from `@Valid` on `PaymentOrderListRequest`. Map to `400 validation` with field-level details. FR-222, FR-223, FR-224.
- [ ] T010 [US1] [AGENT-IMPLEMENT] Add list endpoint matcher to `SecurityConfig` in `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`: `.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders").hasAnyAuthority("merchant:payments:read", "platform:payments:read")`. Must appear BEFORE the single-resource GET matcher `payment-orders/*`. FR-217, FR-218, FR-221.
- [ ] T011 [US1] [AGENT-EXPLAIN] Verify list endpoint returns correct status codes and body shape for: no filters (all orders), single filter (status), multi-filter (status+currency+amount range), empty result set, date range boundaries, ILIKE partial match. Run `./mvnw clean compile`. FR-201 through FR-216.

### Tests for User Story 1

- [ ] T012 [P] [US1] [TESTER-AUTOMATE] Create `PaymentOrderListApiTestSupport` in `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderListApiTestSupport.java`. Add `seedPaymentOrders(int port, int count, String merchantId, String token)` — creates N payment orders with varied statuses, currencies, amounts, client references. Add `listRequestSpec(int port, String token)` using `RequestSpecBuilder` with pre-configured auth header. Add `successListSpec()` using `ResponseSpecBuilder` with `expectStatusCode(200)` and `expectContentType(ContentType.JSON)`. FR-201.
- [ ] T013 [P] [US1] [TESTER-AUTOMATE] Write `PaymentOrderListRestAssuredTest` in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java` with 12 contract tests:
  - `listAllWithoutFiltersReturns200WithAllOrders` — no query params, assert `totalElements` matches seeded count
  - `listFilteredByStatusReturnsOnlyMatchingOrders` — `queryParam("status", "CREATED")`, assert all items have status CREATED
  - `listFilteredByCurrencyReturnsOnlyMatchingOrders` — `queryParam("currency", "PLN")`, use `filteredOn("currency", "PLN")`
  - `listMultiFilterReturnsIntersection` — status + currency + amount range, AND conjunction
  - `listWithDateRangeReturnsOrdersInRange` — `fromDate`/`toDate`, assert `createdAt` within range
  - `listWithClientOrderReferencePartialMatch` — ILIKE `%PAY%`, assert matching orders
  - `listEmptyResultReturns200WithEmptyContent` — filter that matches zero orders
  - `listSeededNoneReturnsEmptyContent` — merchant with no orders, `totalElements=0`
  - `extractTypedResponseAsListDto` — `extract().as(PaymentOrderListResponse.class)`, assert typed fields
  - `listResponseIncludesCorrelationId` — assert `X-Correlation-ID` header present
  - `listResponseContentTypeIsJson` — `accept(ContentType.JSON)`, assert `contentType(ContentType.JSON)`
  - `listWithAcceptHeader` — `accept(ContentType.JSON)` asserted in response
  Use `RequestSpecBuilder`-based request specs from `PaymentOrderListApiTestSupport`. FR-201 through FR-210, FR-215, FR-216.
- [ ] T014 [US1] [AGENT-REVIEW] Verify all REST Assured list contract tests pass. Run `./mvnw -Dtest=PaymentOrderListRestAssuredTest test`. All tests green. FR-201 through FR-210, FR-215, FR-216.

**Checkpoint**: List with all filters works end-to-end. 12 contract tests pass. MVP deliverable.

---

## Phase 3: User Story 2 - Paginate Through Payment Orders (Priority: P2)

**Goal**: Merchant payment reader navigates pages, sees correct metadata, and handles edge cases (empty page, last page, oversized size).

**Independent Test**: Seed 25 payment orders, request page=0 size=10 → 10 items, totalElements=25, totalPages=3. Request page=2 → 5 items (last page).

### Implementation for User Story 2

*(No separate implementation tasks — pagination is built into T007/T008 via Spring Data Pageable. Tasks here are test-only.)*

### Tests for User Story 2

- [ ] T015 [US2] [TESTER-AUTOMATE] Add pagination tests to `PaymentOrderListRestAssuredTest` in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`:
  - `listFirstPageReturnsCorrectMetadata` — seed 25, page=0 size=10, content.size()=10, totalElements=25, totalPages=3
  - `listLastPageReturnsRemainingItems` — seed 25, page=2 size=10, content.size()=5
  - `listPageBeyondDataReturnsEmptyContent` — page=3, content empty, totalElements unchanged
  - `listDefaultPageIsZero` — no page param, first page returned
  - `listMaxSizeReturnsUpTo100` — size=100, accepted
  - `listMinSizeReturnsOne` — size=1, one item
  Use existing `PaymentOrderListApiTestSupport` to seed data. FR-211, FR-212, FR-215.
- [ ] T016 [US2] [AGENT-REVIEW] Verify pagination edge cases pass. Run `./mvnw -Dtest=PaymentOrderListRestAssuredTest test`. FR-211, FR-212.

**Checkpoint**: Pagination works for all edge cases. Metadata correct.

---

## Phase 4: User Story 3 - Sort Payment Orders (Priority: P3)

**Goal**: Merchant payment reader sorts orders by creation time ascending or descending.

**Independent Test**: Seed 3 payment orders with distinct creation times. Sort desc → newest first. Sort asc → oldest first.

### Implementation for User Story 3

*(No separate implementation tasks — sorting is built into T007 via Spring Data Sort. Tasks here are test-only.)*

### Tests for User Story 3

- [ ] T017 [US3] [TESTER-AUTOMATE] Add sort tests to `PaymentOrderListRestAssuredTest` in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`:
  - `listSortedByCreatedAtDescReturnsNewestFirst` — seed 3 orders at known times, sort=createdAt,desc, extract timestamps, assert descending
  - `listSortedByCreatedAtAscReturnsOldestFirst` — sort=createdAt,asc, assert ascending
  - `listDefaultSortIsCreatedAtDesc` — no sort param, default desc
  Use AssertJ `extracting("createdAt")` with timestamp comparison. FR-213.
- [ ] T018 [US3] [AGENT-REVIEW] Verify sort tests pass. Run `./mvnw -Dtest=PaymentOrderListRestAssuredTest test`. FR-213.

**Checkpoint**: Sorting verified for both directions and default.

---

## Phase 5: User Story 4 - Validate Query Parameters (Priority: P3)

**Goal**: System rejects invalid query parameters with `400 validation` and stable error codes.

**Independent Test**: Send invalid values for each query param, assert `400` and machine-readable error codes.

### Implementation for User Story 4

*(Validation is built into T001 `@Valid` annotations and T009 exception handler. Tasks here are test-only.)*

### Tests for User Story 4

- [ ] T019 [US4] [TESTER-AUTOMATE] Create `PaymentOrderListParameterizedTest` in `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListParameterizedTest.java` using `@ParameterizedTest` + `@CsvSource` for 6 filter combinations:
  - `CREATED,PLN,MIN1000,,,5` — status + currency + min amount, expect 5 results
  - `CREATED,EUR,,,,3` — status + currency, expect 3 results
  - `,PLN,MIN5000,,,2` — currency + min amount, expect 2 results
  - `CREATED,,,,,10` — status only, expect 10 results
  - `,,,,,0` — no filters, expect all seeded count
  - `CREATED,PLN,MIN1000,MAX10000,,3` — status + currency + amount range
  Use `PaymentOrderListApiTestSupport` to seed controlled data. Use `extracting("status", "currency").contains(tuple("CREATED", "PLN"))`. FR-202 through FR-208, FR-210.
- [ ] T020 [P] [US4] [TESTER-AUTOMATE] Add query parameter validation tests to `PaymentOrderListRestAssuredTest`:
  - `invalidPageReturns400` — page=-1, assert 400 + error=validation
  - `invalidSizeReturns400` — size=0, assert 400
  - `oversizedSizeReturns400` — size=1001, assert 400
  - `invalidStatusReturns400` — status=INVALID, assert 400
  - `invalidCurrencyReturns400` — currency=GBP, assert 400
  - `malformedDateReturns400` — fromDate=not-a-date, assert 400
  - `invalidSortReturns400` — sort=invalidField,desc, assert 400
  Each test asserts stable machine-readable error code. FR-212, FR-213, FR-214, FR-222, FR-223, FR-224.
- [ ] T021 [US4] [AGENT-REVIEW] Verify all parameterized and validation tests pass. Run `./mvnw -Dtest="PaymentOrderListParameterizedTest,PaymentOrderListRestAssuredTest" test`. FR-202 through FR-208, FR-212, FR-213, FR-214, FR-222, FR-223, FR-224.

**Checkpoint**: All 7 invalid query param scenarios return `400` with stable error codes. Parameterized filter combinations produce correct results.

---

## Phase 6: User Story 5 - Enforce Access Boundary for List (Priority: P2)

**Goal**: List endpoint protected: `401` for unauthenticated, `403` for denied identity, `403` for `merchant:payments:create` (no read), `403` for cross-tenant, `200` for `merchant:payments:read` own-scope, `200` for `platform:payments:read` cross-merchant.

**Independent Test**: Exercise all 7 actor types against list endpoint and verify correct status codes.

### Implementation for User Story 5

*(Security is built into T008 controller claim check and T010 SecurityConfig. Tasks here are test-only.)*

### Tests for User Story 5

- [ ] T022 [US5] [TESTER-AUTOMATE] Write `PaymentOrderListSecurityTest` in `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderListSecurityTest.java` with 7 security tests:
  - `unauthenticatedListReturns401` — no token, `GET .../payment-orders`, 401
  - `deniedIdentityListReturns403` — `TestJwtSupport.deniedToken()`, 403
  - `merchantReaderOwnScopeReturns200` — `merchantPaymentReaderToken(merchantId)`, own merchant, 200
  - `merchantReaderCrossTenantReturns403` — reader for merchant A, list merchant B, 403
  - `merchantCreatorWithoutReadReturns403` — `merchantPaymentCreatorToken(merchantId)` (create only, no read), 403
  - `merchantOperatorWithoutReadReturns403` — `merchantPaymentOperatorToken(merchantId)` (operate only, no read), 403
  - `platformReaderCrossMerchantReturns200` — `platformPaymentReaderToken()`, list any merchant, 200
  Use `PaymentOrderListApiTestSupport` to seed data. FR-217, FR-218, FR-219, FR-220, FR-221.
- [ ] T023 [US5] [AGENT-REVIEW] Verify all security tests pass. Run `./mvnw -Dtest=PaymentOrderListSecurityTest test`. FR-217 through FR-221.

**Checkpoint**: 7-actor security matrix verified for list endpoint.

---

## Phase 7: Additional Test Layers

**Purpose**: Repository/integration, specification unit tests, test support quality, and regression verification.

- [ ] T024 [P] [TESTER-AUTOMATE] Write `PaymentOrderListRepositoryTest` in `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/PaymentOrderListRepositoryTest.java` with `@DataJpaTest` + Testcontainers:
  - `findAllByMerchantIdReturnsOnlyThatMerchant` — seed orders for 2 merchants, query one, assert no cross-tenant leak
  - `findAllWithSpecificationFiltersByStatus` — `Specification<PaymentOrder>`, status filter, correct results
  - `findAllWithPaginationReturnsCorrectPage` — Pageable page 0 size 5, 5 items
  - `findAllWithPaginationMetadataCorrect` — seed known count, assert `totalElements`
  - `findAllWithEmptySpecificationReturnsAllForMerchant` — null spec = no filters = all
  - `findAllEmptyResultSetReturnsEmptyPage` — filter with no match, empty page
  FR-201, FR-225, FR-228.
- [ ] T025 [P] [TESTER-AUTOMATE] Write `PaymentOrderSpecificationTest` in `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/PaymentOrderSpecificationTest.java` (unit tests, no Spring context):
  - `hasMerchantIdProducesCorrectPredicate` — verify root with merchant_id
  - `hasStatusNullReturnsNull` — absent param = no filter
  - `hasCurrencyValidReturnsSpecification` — param present, spec not null
  - `amountBetweenBothParamsReturnsSpecification` — min and max both present
  - `amountBetweenOnlyMinReturnsSpecification` — only min
  - `clientOrderReferenceContainsEscapesLikeChars` — input with `%` character, escaped in predicate
  FR-204 to FR-208, FR-229.
- [ ] T026 [P] [TESTER-AUTOMATE] Create `PaymentOrderAssertions` custom AssertJ assertions in `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderAssertions.java`:
  - `assertThat(response).hasOnlyStatus("CREATED")` — all items have given status
  - `assertThat(response).allAmountsGreaterThan(long min)` — all items above threshold
  - `assertThat(response).hasPageMetadata(int expectedTotalElements, int expectedTotalPages)` — page metadata correct
  Use in parameterized and contract tests to reduce assertion duplication. FR-215.
- [ ] T027 [P] [TESTER-AUTOMATE] Create `RestAssuredLoggingConfig` in `apps/backend/src/test/java/lab/paymentquality/testsupport/RestAssuredLoggingConfig.java`:
  - `configureFailureOnlyLogging()` — sets `.log().ifValidationFails()` globally
  - `configureAuthorizationMasking()` — adds `ResponseLoggingFilter` that blacklists `Authorization` and `Idempotency-Key` from logs
  Apply in `@BeforeAll` of `PaymentOrderListRestAssuredTest`. NFR-208.
- [ ] T028 [AGENT-REVIEW] Run full payment test suite to verify no regressions: `./mvnw -Dtest="PaymentOrder*" test`. All existing create/read tests from spec/003 must pass alongside new list tests.
- [ ] T029 [AGENT-REVIEW] Run Spring Modulith architecture verification: `./mvnw -Dtest=PaymentModuleTest test`. Verify no `merchant.internal` dependency was introduced.

**Checkpoint**: All 5 test layers pass. No regression. Modulith boundaries intact.

---

## Phase 8: Documentation and Learning Outputs

**Purpose**: Vault lesson note, evidence update, competency matrix update.

- [ ] T030 [TESTER-ANALYZE] Create Lesson 07 vault note in `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 07 - Payment Order List Filter Search.md` following `Template - Lesson 7+ Note.md` structure. Include: learning delta, code map, REST Assured learning path (7 new methods), AssertJ patterns (5), SQL (5), JUnit (4), exercises (10), interview answers (5 EN).
- [ ] T031 [TESTER-DESIGN] Update `Learning Governance/Learning Coverage Backlog.md` — mark 22 topics from Not Started/Planned → Practiced: `queryParam()`, `accept()`, `extract().as()`, `RequestSpecBuilder`, `ResponseSpecBuilder`, `log().ifValidationFails()`, `RestAssured.filters()`, `extracting`, `filteredOn`, `tuple`, `recursive comparison`, `soft assertions`, `@ParameterizedTest`, `@Nested`, `@Tag`, `@DisplayName`, `WHERE`, `ORDER BY`, `LIMIT`, `indexes`, `COUNT`, decision tables, negative-path first.
- [ ] T032 [AGENT-EXPLAIN] Update `Lesson Evidence Tracker` with Lesson 07 evidence: production code, test code, vault note, Spec Kit artifacts, commands run, competency updates, open risks, interview answer EN, next sprint handoff.
- [ ] T033 [AGENT-EXPLAIN] Update `Learning Governance/Learning Progress Board.md` — mark Lesson 07 as Active, update competency counts.
- [ ] T034 [AGENT-EXPLAIN] Update `Senior SDET Competency Coverage Matrix` — change 22 Not Started/Introduced items to Practiced/Evidence Strong.
- [ ] T035 [DISCUSS] Review scope guardrails: confirm no lifecycle actions, PSP, Kafka, GraphQL, gRPC, or frontend changes were introduced. Confirm `merchant:payments:create` does not grant list access. Confirm `GET /api/status` remains public.

**Checkpoint**: All evidence tracked. Learning OS updated. Guardrails verified.

---

## Phase 9: Final Verification

- [ ] T036 [AGENT-REVIEW] Full suite verification:
  - `./mvnw test` — all backend tests pass
  - `corepack pnpm typecheck` — frontend typecheck passes (unchanged)
  - `./mvnw -Dtest=PaymentModuleTest test` — Modulith architecture verification passes
- [ ] T037 [AGENT-EXPLAIN] Verify logs do not contain `Authorization` headers or raw `Idempotency-Key` values. Review `RestAssuredLoggingConfig` masking configuration.

**Checkpoint**: Full suite green. Log hygiene verified. Feature ready.

---

## Summary

| Phase | Tasks | User Story |
|---|---|---|
| 1. Setup | T001–T006 | All |
| 2. US1 List with Filters (MVP) | T007–T014 | US1 |
| 3. US2 Pagination | T015–T016 | US2 |
| 4. US3 Sort | T017–T018 | US3 |
| 5. US4 Query Validation | T019–T021 | US4 |
| 6. US5 Access Boundary | T022–T023 | US5 |
| 7. Additional Tests | T024–T029 | All |
| 8. Documentation | T030–T035 | All |
| 9. Final Verification | T036–T037 | All |
| **Total** | **37 tasks** | 5 user stories |
