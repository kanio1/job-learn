# Feature Specification: Payment Order List with Filtering, Pagination, and Sorting

**Feature Branch**: `004-payment-order-create-read`

**Created**: 2026-05-28

**Status**: Draft

**Phase**: 2 — Payment Orders

**Lesson**: 07

**Input**: Lesson 07 Spec Kit input. Extends `003-payment-order-access-lifecycle` with list/filter/pagination/sort behavior on the existing payment order resource. See `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Prompts/Prompt - Lesson 07 - Payment Order List Filter SpecKit.md`.

## Business Purpose *(mandatory)*

This feature adds the first multi-resource retrieval capability for payment orders: a list endpoint with filtering, pagination, and sorting. After `003-payment-order-access-lifecycle` established single-resource create/read, merchants need to browse, filter, and navigate their payment orders at scale.

This feature creates:

- The first `GET` collection endpoint in the payment domain, transforming the API from single-resource to resource-collection capable.
- Dynamic query-parameter-based filtering (`status`, `currency`, date range, amount range, text search) that becomes executable test oracles for `queryParam()` and decision-table testing.
- Pagination and sorting contracts (`page`, `size`, `sort`) that introduce `WHERE`, `ORDER BY`, `LIMIT/OFFSET` SQL patterns and page-metadata response contracts.
- The first `JpaSpecificationExecutor` dynamic query in the repository layer.
- A dedicated security decision for cross-tenant list access (`403`, not masked `404`).
- Reusable REST Assured framework architecture: `RequestSpecBuilder`, `ResponseSpecBuilder`, failure-only logging, `Authorization` masking, typed DTO extraction, and custom AssertJ assertions.
- Material for 22 backlog topics across REST Assured (7 new methods), AssertJ (5 patterns), JUnit (4 patterns), SQL (5 constructs), and Test Design (2 methods).

No aggregation (`GROUP BY`), lifecycle actions, PSP integration, or frontend changes are implemented.

## Clarifications

### Session 2026-05-28

- Q: What HTTP status does cross-tenant list access return? → A: `403 forbidden`. Unlike single-resource read (masked `404`), listing is an overt operation — refusing access does not enumerate resources.
- Q: Does `merchant:payments:create` grant list access? → A: No. Only `merchant:payments:read` and `platform:payments:read` can list payment orders.
- Q: What are the default pagination values? → A: `page` defaults to `0`, `size` defaults to `20`, maximum `size` is `100`.
- Q: How does `clientOrderReference` filtering work? → A: Partial case-insensitive match (ILIKE `%value%`). An empty or absent value means no text filter is applied.
- Q: Which status values are valid for filtering? → A: Only `CREATED` in this phase (the only existing payment status). The filter parameter is forward-compatible — when lifecycle adds more statuses, they become valid filter values without API changes.
- Q: What is the default sort order? → A: `createdAt,desc` (newest first). The `sort` parameter accepts `createdAt,asc` and `createdAt,desc`.
- Q: Can `platform:payments:read` list cross-merchant? → A: Yes. A platform payment reader can list any merchant's payment orders for support/investigation purposes.
- Q: Does the list endpoint support multi-column sorting? → A: No. Single-column sort only in this phase.
- Q: Are all query parameters optional? → A: Yes. An empty query (no params) returns all payment orders for the merchant with default pagination and sorting.
- Q: How are date range boundaries handled? → A: `fromDate` is inclusive start-of-day, `toDate` is inclusive end-of-day. Both are optional and independent.
- Q: How are amount range boundaries handled? → A: `minAmount` and `maxAmount` are inclusive. Both are optional and independent.

## Actors *(mandatory)*

- **Merchant Payment Reader**: Authenticated user with `merchant:payments:read` authority scoped to one merchant. Lists payment orders belonging to their merchant. Cannot create or operate on payment orders. Already defined in `003`.
- **Platform Payment Reader**: Authenticated user with `platform:payments:read` authority. Lists any merchant's payment orders. Already defined in `003`.
- **Merchant Payment Creator**: Authenticated user with `merchant:payments:create`. Cannot list payment orders (`403`). Already defined in `003`.
- **Cross-Tenant Actor**: Authenticated merchant reader for merchant A who attempts to list merchant B's payment orders. Receives `403 forbidden` (overt refusal, not masked).
- **Unauthenticated User**: `401`.
- **Denied Identity**: `403`.

## Scope *(mandatory)*

### In Scope

- `GET /api/merchants/{merchantId}/payment-orders` — list payment orders with filtering, pagination, and sorting.
- Query parameters: `status`, `currency`, `fromDate`, `toDate`, `minAmount`, `maxAmount`, `clientOrderReference`, `page`, `size`, `sort`.
- Default pagination: `page=0`, `size=20`, max `size=100`.
- Default sort: `createdAt,desc`.
- Response body: `{ content: [...], page, size, totalElements, totalPages }` with `200 OK` + `X-Correlation-ID` header.
- Validation: `400 validation` for invalid query params (`page=-1`, `size=1001`, `status=INVALID`, `currency=GBP`, malformed dates).
- Supported status filter values: `CREATED` only (forward-compatible with future lifecycle statuses).
- Supported currency filter values: `PLN`, `EUR`, `USD`.
- Dynamic query via `JpaSpecificationExecutor<PaymentOrder>` (`Specification<PaymentOrder>` with AND-composition of optional predicates).
- `clientOrderReference` partial case-insensitive match (ILIKE).
- `fromDate` / `toDate` inclusive date-range filtering on `createdAt`.
- `minAmount` / `maxAmount` inclusive range filtering on `amountMinor`.
- Flyway migration `V3__add_payment_order_list_indexes.sql` — indexes supporting filtered/sorted list queries.
- Security: `merchant:payments:read` for merchant scope; `platform:payments:read` for cross-merchant access.
- `merchant_id` claim enforcement for merchant readers.
- Cross-tenant list → `403 forbidden`.
- Stable machine-readable error codes for query param validation.
- `X-Correlation-ID` propagated in all responses.
- Spring Modulith boundary preserved — list behavior lives inside the existing `payment` module.

### Out of Scope

- `GROUP BY`, `COUNT` per status, aggregation, reporting — Lesson 08.
- Payment order lifecycle actions (authorize, capture, cancel) — `003` out-of-scope, unchanged.
- `If-Match` / `412 Precondition Failed` — `003` out-of-scope, unchanged.
- PSP integration, Kafka, webhooks, GraphQL, gRPC — Phase 0 guardrails.
- Frontend list page (Nuxt) — deferred to optional extension.
- Playwright E2E for list — deferred.
- CSV/PDF export.
- Full-text search.
- Multi-column sorting (single-column only).
- Rate limiting, cache headers (`Cache-Control`, `ETag` for list).
- Payment order mutation (update, delete).
- Currency conversion or exchange rates.

## User Scenarios & Testing *(mandatory)*

### User Story 1 — List Payment Orders with Optional Filters (Priority: P1)

A merchant payment reader lists payment orders for their merchant, optionally filtering by status, currency, date range, amount range, and client reference text.

**Why this priority**: List is the first collection endpoint in the payment domain. It introduces query-parameter-based filtering, pagination, and sorting — the most important new REST Assured, SQL, and AssertJ learning surface in Lesson 07.

**Independent Test**: Create 5 payment orders with varied statuses/currencies/amounts for a merchant, then call `GET /api/merchants/{merchantId}/payment-orders?status=CREATED&currency=PLN` and assert response contains only matching orders using AssertJ `filteredOn` and `extracting`.

**Acceptance Scenarios**:

1. **Given** a merchant has 5 payment orders (3 PLN, 2 EUR), **When** a merchant payment reader lists with `?currency=PLN`, **Then** the response `content` array contains exactly 3 orders, all with `currency=PLN`, `totalElements=3`, `totalPages=1`.
2. **Given** a merchant has 5 payment orders (all CREATED), **When** a merchant payment reader lists with `?status=CREATED`, **Then** the response contains all 5 orders.
3. **Given** a merchant has 5 payment orders created on different dates, **When** a merchant payment reader lists with `?fromDate=2026-05-01&toDate=2026-06-01`, **Then** only orders within that date range are returned.
4. **Given** a merchant has 5 payment orders with different amounts, **When** a merchant payment reader lists with `?minAmount=1000&maxAmount=10000`, **Then** only orders with `amountMinor` in `[1000, 10000]` are returned.
5. **Given** a merchant has payment orders with client references `PAY-ALPHA`, `PAY-BETA`, `PAY-GAMMA`, **When** a merchant payment reader lists with `?clientOrderReference=ALP`, **Then** only `PAY-ALPHA` is returned (ILIKE partial match).
6. **Given** a merchant has 0 payment orders, **When** a merchant payment reader lists, **Then** the response `content` is an empty array, `totalElements=0`, `totalPages=0`.

---

### User Story 2 — Paginate Through Payment Orders (Priority: P2)

A merchant payment reader navigates through pages of payment orders.

**Why this priority**: Pagination introduces `page`/`size` query params, `LIMIT/OFFSET` SQL, page-metadata response contracts, and edge-case test scenarios (empty page, last page, oversized page).

**Acceptance Scenarios**:

1. **Given** a merchant has 25 payment orders, **When** a merchant payment reader lists with `?size=10&page=0`, **Then** the response contains 10 orders, `totalElements=25`, `totalPages=3`.
2. **Given** a merchant has 25 payment orders, **When** a merchant payment reader lists with `?size=10&page=2`, **Then** the response contains 5 orders (last page), `totalElements=25`, `totalPages=3`.
3. **Given** a merchant has 5 payment orders, **When** a merchant payment reader lists with `?size=10&page=1`, **Then** the response `content` is an empty array (page beyond data).
4. **Given** a merchant payment reader lists with `?size=1001`, **When** the request is sent, **Then** `400 validation` is returned (size exceeds maximum).

---

### User Story 3 — Sort Payment Orders (Priority: P3)

A merchant payment reader sorts payment orders by creation time.

**Acceptance Scenarios**:

1. **Given** a merchant has 3 payment orders created at T1 < T2 < T3, **When** a merchant payment reader lists with `?sort=createdAt,desc`, **Then** orders are returned in reverse chronological order (T3 first, T1 last).
2. **Given** a merchant has 3 payment orders, **When** a merchant payment reader lists with `?sort=createdAt,asc`, **Then** orders are returned in chronological order (T1 first, T3 last).
3. **Given** a merchant payment reader lists with `?sort=invalidField,desc`, **When** the request is sent, **Then** `400 validation` is returned.

---

### User Story 4 — Validate Query Parameters (Priority: P3)

The system rejects invalid query parameters with `400 validation` and stable error codes.

**Acceptance Scenarios**:

1. **Given** a merchant payment reader lists with `?page=-1`, **Then** `400 validation` is returned.
2. **Given** a merchant payment reader lists with `?size=0`, **Then** `400 validation` is returned.
3. **Given** a merchant payment reader lists with `?status=INVALID`, **Then** `400 validation` is returned.
4. **Given** a merchant payment reader lists with `?currency=GBP`, **Then** `400 validation` is returned.
5. **Given** a merchant payment reader lists with `?fromDate=not-a-date`, **Then** `400 validation` is returned.

---

### User Story 5 — Enforce Access Boundary for List (Priority: P2)

The platform protects the list endpoint with authentication, role authorization, and merchant ownership.

**Acceptance Scenarios**:

1. **Given** an unauthenticated user, **When** they attempt to list payment orders, **Then** `401` is returned.
2. **Given** an authenticated user with no payment role (denied identity), **When** they attempt to list, **Then** `403` is returned.
3. **Given** an authenticated user with `merchant:payments:create` (but not `merchant:payments:read`), **When** they attempt to list, **Then** `403` is returned.
4. **Given** an authenticated merchant reader for merchant A, **When** they attempt to list merchant B's payment orders, **Then** `403` is returned (cross-tenant list — overt refusal).
5. **Given** an authenticated platform payment reader, **When** they list any merchant's payment orders, **Then** `200 OK` is returned.

### Edge Cases

- No query parameters → returns all orders with default pagination (page=0, size=20, sort=createdAt,desc).
- `size` = 1 (minimum valid).
- `size` = 100 (maximum valid).
- `page` = 0 with empty result set → 200 with empty content and totalElements=0.
- `page` beyond available data → 200 with empty content, totalElements unchanged.
- All filters combined returning zero results → 200 with empty content.
- `fromDate` without `toDate` → filters from that date onward.
- `toDate` without `fromDate` → filters up to that date.
- `minAmount` without `maxAmount` → filters from that amount upward.
- `maxAmount` without `minAmount` → filters up to that amount.
- `minAmount` > `maxAmount` → 200 with empty content (valid range, zero results).
- `clientOrderReference` with special SQL characters (`%`, `_`, `'`) → escaped safely, no injection.
- Parallel test execution → unique merchant references prevent cross-test contamination.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-201**: System MUST allow an authenticated caller with `merchant:payments:read` for a given merchant to list payment orders belonging to that merchant via `GET /api/merchants/{merchantId}/payment-orders`.
- **FR-202**: System MUST support optional query parameter `status` filtering to only `CREATED` in this phase, returning `400 validation` for unsupported values.
- **FR-203**: System MUST support optional query parameter `currency` filtering to `PLN`, `EUR`, or `USD`, returning `400 validation` for unsupported values.
- **FR-204**: System MUST support optional query parameter `fromDate` (ISO date) to filter payment orders with `createdAt >= fromDate` (inclusive start-of-day).
- **FR-205**: System MUST support optional query parameter `toDate` (ISO date) to filter payment orders with `createdAt <= toDate` (inclusive end-of-day).
- **FR-206**: System MUST support optional query parameter `minAmount` to filter payment orders with `amountMinor >= minAmount` (inclusive).
- **FR-207**: System MUST support optional query parameter `maxAmount` to filter payment orders with `amountMinor <= maxAmount` (inclusive).
- **FR-208**: System MUST support optional query parameter `clientOrderReference` for partial case-insensitive match (ILIKE `%value%`) on `client_order_reference`.
- **FR-209**: System MUST return all matching payment orders when no query parameters are provided.
- **FR-210**: System MUST compose multiple query parameters as AND conditions.
- **FR-211**: System MUST support pagination via `page` (default 0) and `size` (default 20) query parameters.
- **FR-212**: System MUST validate `page >= 0` and `size` in range `1..100`, returning `400 validation` for out-of-range values.
- **FR-213**: System MUST support sorting via `sort` query parameter accepting `createdAt,asc` and `createdAt,desc` (default `createdAt,desc`).
- **FR-214**: System MUST return `400 validation` for unsupported `sort` values.
- **FR-215**: System MUST return `200 OK` with paginated response body containing `content` (array), `page`, `size`, `totalElements`, `totalPages`.
- **FR-216**: System MUST include `X-Correlation-ID` header in list responses.
- **FR-217**: System MUST return `401` for unauthenticated list requests.
- **FR-218**: System MUST return `403 forbidden` for authenticated users without `merchant:payments:read` or `platform:payments:read`.
- **FR-219**: System MUST return `403 forbidden` for cross-tenant list attempts by merchant readers (overt refusal).
- **FR-220**: System MUST enforce `merchant_id` claim matching for merchant-scoped readers.
- **FR-221**: System MUST allow `platform:payments:read` to list any merchant's payment orders.
- **FR-222**: System MUST return `400 validation` for malformed date parameters.
- **FR-223**: System MUST return `400 validation` for non-numeric `page`, `size`, `minAmount`, or `maxAmount` values.
- **FR-224**: System MUST return stable machine-readable error codes for all query-param validation errors.
- **FR-225**: System MUST use dynamic JPA Specification query composition, not hardcoded repository query methods per filter combination.
- **FR-226**: System MUST execute list queries as `@Transactional(readOnly = true)`.
- **FR-227**: System MUST preserve Spring Modulith boundaries — list behavior lives within `payment.internal`, no new public API types.
- **FR-228**: System MUST provide database indexes supporting filtered, sorted, and paginated list queries.
- **FR-229**: System MUST escape `clientOrderReference` input safely for ILIKE queries (SQL injection prevention).

### Non-Functional Requirements

- **NFR-201**: List responses MUST be deterministic: same parameters on the same data set MUST return the same content and order.
- **NFR-202**: Automated tests MUST be able to seed multiple payment orders with unique references and verify list assertions without relying on shared global data.
- **NFR-203**: List query performance MUST remain acceptable (sub-second) for up to 1000 payment orders per merchant in local lab environments.
- **NFR-204**: Query parameter validation MUST be testable independently of data state (validation happens before query execution).
- **NFR-205**: Security behavior for list MUST be testable for all five actor types (unauthenticated, denied, merchant reader, cross-tenant, platform reader).
- **NFR-206**: The `page`/`size`/`sort` contract MUST remain stable so automated tests and future frontend consumers can depend on it.
- **NFR-207**: Index creation MUST NOT break existing payment order create/read tests.

## Quality and Architecture Impact *(mandatory)*

### Tester-Led Risk Notes

- **Filter-combination risk**: Multi-filter AND logic must be correct for all combinations — empty filter, one filter, all filters, conflicting range filters.
- **Pagination edge risk**: Empty page, last page, page beyond data, page=0 with no data must all return consistent responses.
- **SQL injection risk**: `clientOrderReference` ILIKE parameter must be properly escaped.
- **Cross-tenant leak risk**: List must return `403` (not `404`), but must not leak whether merchant B exists or has orders.
- **Role expansion risk**: `merchant:payments:create` must not gain list access. Tests must verify this explicitly.
- **Sort injection risk**: `sort` parameter must be validated against a whitelist, not interpolated directly into SQL.
- **Performance risk**: Without indexes, filtered list queries could degrade with data volume.
- **Parallel-test risk**: Seeded payment orders must use unique references and idempotency keys per test.
- **Contract drift risk**: List response shape must remain stable across future lifecycle changes.
- **Log safety risk**: Query parameters (especially `clientOrderReference`) must not leak into logs containing `Authorization` headers.

### Modulith Impact

- **Module Ownership**: `payment` — no new module.
- **Module API Impact**: None. No new root-package public API types. List behavior is internal to the payment module.
- **Dependency Impact**: None. `payment` continues to depend only on merchant public API for eligibility. No new cross-module dependencies.
- **Event Impact**: None. List is synchronous read-only.
- **Module Test Impact**: Architecture verification (`PaymentModuleTest`) must continue to pass — no `merchant.internal` access.

### Security, Data, and Observability Impact

- List is a read-only, merchant-scoped, authenticated operation.
- `merchant_id` claim enforcement remains mandatory for merchant readers.
- `platform:payments:read` is explicitly tested for cross-merchant list access.
- `merchant:payments:create` is explicitly tested to NOT grant list access.
- No tokens, `Authorization` headers, or raw `Idempotency-Key` values appear in logs.
- Query parameters are logged at DEBUG level only, never at INFO or above.
- `X-Correlation-ID` is included in all list responses.

### Key Entities

No new entities. The feature uses the existing `PaymentOrder` entity with additional query capabilities.

- **Payment Order** (existing): Extended with repository query methods via `JpaSpecificationExecutor`.
- **Payment Order List Request** (new value object / record): Immutable representation of validated query parameters.
- **Payment Order List Response** (new DTO record): `List<PaymentOrderResponse> content`, `int page`, `int size`, `long totalElements`, `int totalPages`.

### Database Impact

- **New Migration**: `V3__add_payment_order_list_indexes.sql` with indexes:
  - `idx_payment_orders_merchant_status ON payment_orders(merchant_id, status)`
  - `idx_payment_orders_merchant_created ON payment_orders(merchant_id, created_at DESC)`
  - `idx_payment_orders_merchant_currency ON payment_orders(merchant_id, currency)`
- No new tables, columns, or constraints beyond indexes.
- No data migration or historical data processing.

### Learning Impact *(Payment Quality Engineering Lab specific)*

This feature is designed as Lesson 07 in the learning curriculum. It introduces:

**7 new REST Assured methods (zero prior usage in codebase):**
1. `queryParam("status", "CREATED")` — query parameter passing
2. `accept(ContentType.JSON)` — content negotiation
3. `extract().as(PaymentOrderListResponse.class)` — typed DTO deserialization
4. `new RequestSpecBuilder().addHeader(...).build()` — programmatic request spec building
5. `new ResponseSpecBuilder().expectStatusCode(200).build()` — reusable response specs
6. `.log().ifValidationFails()` — failure-only logging
7. `RestAssured.filters(...)` with Authorization blacklist — secret masking

**5 new AssertJ patterns:**
- `extracting()`, `filteredOn()`, `tuple()`, `usingRecursiveComparison()`, `SoftAssertions`

**4 new JUnit patterns:**
- `@ParameterizedTest`, `@CsvSource`, `@Nested`, `@Tag`

**5 new SQL constructs:**
- `WHERE` with optional predicates, `ORDER BY`, `LIMIT/OFFSET`, `COUNT(*)` for pagination, composite indexes

**2 deepened Test Design methods:**
- Decision tables (filter combinations), negative-path first (query param validation)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-201**: A merchant payment reader can list all their payment orders in under 1 second with up to 1000 orders in local lab.
- **SC-202**: 100% of list responses include correct `totalElements`, `totalPages`, `page`, and `size` metadata matching the actual data set.
- **SC-203**: Filter combinations (status, currency, date range, amount range, text search) all return only matching orders. An automated parameterized test covers at least 6 filter combinations.
- **SC-204**: Sorting by `createdAt,desc` and `createdAt,asc` produces correctly ordered results. An automated test verifies order with at least 3 payment orders.
- **SC-205**: All five security scenarios (unauthenticated, denied, merchant reader, cross-tenant, platform reader) are tested for the list endpoint.
- **SC-206**: Invalid query parameters (`page=-1`, `size=1001`, `status=INVALID`, `currency=GBP`, malformed date) all return `400 validation` with stable error codes.
- **SC-207**: Pagination edge cases (empty page, last page, page beyond data, minimum size, maximum size) all return consistent responses.
- **SC-208**: A `RequestSpecBuilder` and `ResponseSpecification` exist as reusable test artifacts, reducing duplication across list tests.
- **SC-209**: REST Assured test logs do not contain `Authorization` header values.
- **SC-210**: The existing Payment Order create/read tests (spec/003) continue to pass. No regression.
- **SC-211**: The `merchant:payments:create` role does NOT grant list access. A security test verifies this.
- **SC-212**: Spring Modulith architecture verification (`PaymentModuleTest`) passes. No `merchant.internal` dependency introduced.

## Assumptions

- The existing `PaymentOrder` entity and `JpaPaymentOrderRepository` can be extended with `JpaSpecificationExecutor<PaymentOrder>`.
- The existing `PaymentOrderController` stays at the same request mapping root; the new list endpoint is added to the same controller class or a separate controller within the same package.
- The existing `PaymentExceptionHandler` can be extended with query-param validation error handling.
- Query parameter validation follows the same `400 validation` + `ErrorResponse` pattern as existing request-body validation.
- The `createdAt` field on `PaymentOrder` is suitable for date-range filtering and sorting.
- The `CurrencyCode` value object's internal code is used for SQL comparison.
- `page` and `size` follow zero-based page indexing (standard Spring Data `Pageable` convention).
- `page` beyond available data returns an empty `content` array with correct `totalElements` (standard Spring Data behavior).
- Test JWT support (`TestJwtSupport`) already provides tokens for all required roles.
- The `PaymentApiTestSupport` class can be refactored to use `RequestSpecBuilder` without breaking existing tests.
- Existing indexes (`payment_orders` primary key, FK to merchants, idempotency unique constraint) remain unchanged.
- The list endpoint does not return `ETag` headers (ETag is reserved for single-resource state and future lifecycle concurrency).
- No Keycloak realm changes are needed — all roles already exist from spec/003.
