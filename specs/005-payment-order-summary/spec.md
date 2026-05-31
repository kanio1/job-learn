# Feature Specification: Payment Order Aggregation Summary

**Feature Branch**: `005-payment-order-summary`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Payment Order Aggregation Summary — read-only summary endpoint for merchant-scoped payment orders. GROUP BY currency/status, COUNT, SUM. Lesson 08 system implementation without tests."

**Lesson**: 08

**Phase**: 2 — Payment Orders

## Business Purpose *(mandatory)*

This feature adds the first aggregation capability to the Payment Quality Engineering Lab: a read-only summary endpoint that returns totals and breakdowns for a merchant's payment orders.

After Lesson 06 established single-resource create/read and Lesson 07 established collection list/filter/search, Lesson 08 introduces database-backed aggregation: counting payment orders, summing amounts, and grouping by currency and status — all within the existing merchant ownership boundary.

This feature creates:

- The first SQL aggregation query (`GROUP BY`, `COUNT`, `SUM`) in the payment domain.
- A reporting-style REST API response that does not map one-to-one to an entity — a new test oracle challenge.
- Controlled seed data as the foundation for deterministic aggregation tests.
- The foundation for future analytics, dashboard cards, and reporting endpoints without implementing a full business dashboard.
- Educational material for `EXPLAIN` analysis, aggregation assertions, and DB-as-oracle testing.

No payment processing, PSP integration, card data, settlement, reconciliation, refunds, webhooks, Kafka, lifecycle actions, or complete business dashboard is implemented.

## Clarifications

### Session 2026-05-30

- Q: What HTTP status does empty merchant summary return? → A: `200 OK` with zero totals. Summary is a reporting view — an empty merchant has valid zero values, not a `404`.
- Q: What HTTP status does cross-tenant merchant summary return? → A: `403 forbidden`. Like list (Lesson 07), summary is an overt collection/report operation — refusing access does not enumerate merchants.
- Q: Does `merchant:payments:create` grant summary access? → A: No. Only `merchant:payments:read` and `platform:payments:read` can access summary.
- Q: Does `merchant:payments:operate` grant summary access? → A: No. The operate role is preserved for future lifecycle actions.
- Q: Can `platform:payments:read` access any merchant's summary? → A: Yes. A platform payment reader can view any merchant's summary for support/investigation.
- Q: Where are aggregation totals computed? → A: In PostgreSQL via `GROUP BY`, `COUNT(*)`, `SUM(amount_minor)`. Totals must NOT be computed in Java by fetching all entities and summing in memory.
- Q: Are new payment statuses added for aggregation? → A: No. Only `CREATED` exists. The endpoint is forward-compatible — when lifecycle adds more statuses, `byStatus` groups them automatically.
- Q: Which optional query filters does summary support? → A: `currency`, `status`, `fromDate`, `toDate` — small set to limit aggregation population. No amount range or text search filters on summary.
- Q: Does summary return `ETag`? → A: No. Summary is a transient aggregation — there is no single representation version to protect with optimistic concurrency.
- Q: Does summary require a Flyway migration? → A: Only if a new composite index is justified by the aggregation query shape. The existing indexes from V2 (merchant_id, created_at) and V3 (merchant_id, status; merchant_id, currency) may be sufficient.

## Actors *(mandatory)*

- **Merchant Payment Reader**: Authenticated user with `merchant:payments:read` authority scoped to one merchant. Views payment order summary belonging to their merchant. Cannot create or operate on payment orders. Already defined in `003`.
- **Platform Payment Reader**: Authenticated user with `platform:payments:read` authority. Views any merchant's payment order summary for support/investigation purposes. Already defined in `003`.
- **Merchant Payment Creator**: Authenticated user with `merchant:payments:create`. Cannot view summary (`403`). Already defined in `003`.
- **Merchant Payment Operator**: Authenticated user with `merchant:payments:operate` only. Cannot view summary (`403`). Already defined in `003` as a planned unused role.
- **Cross-Tenant Actor**: Authenticated merchant reader for merchant A who attempts to view merchant B's summary. Receives `403 forbidden` (overt refusal, same as list behavior).
- **Unauthenticated User**: `401`.
- **Denied Identity**: `403`.

## Scope *(mandatory)*

### In Scope

- `GET /api/merchants/{merchantId}/payment-orders/summary` — read-only aggregation endpoint.
- Response body: `{ totalOrders, totalAmountMinor, byCurrency[], byStatus[] }` with `200 OK`.
- `byCurrency[]` group: `{ currency, orderCount, totalAmountMinor }` — one entry per currency present.
- `byStatus[]` group: `{ status, orderCount, totalAmountMinor }` — one entry per status present.
- `X-Correlation-ID` header in summary response.
- Aggregation computed in PostgreSQL using `GROUP BY`, `COUNT(*)`, `SUM(amount_minor)`.
- Optional query parameters: `currency` (PLN/EUR/USD), `status` (CREATED), `fromDate` (ISO date), `toDate` (ISO date).
- Empty merchant summary returns `200 OK` with `totalOrders=0`, `totalAmountMinor=0`, empty `byCurrency[]`, empty `byStatus[]`.
- Security: `merchant:payments:read` for merchant scope; `platform:payments:read` for cross-merchant access.
- `merchant_id` claim enforcement for merchant readers.
- Cross-tenant summary returns `403 forbidden` (consistent with list behavior from Lesson 07).
- `@Transactional(readOnly = true)` for summary queries.
- Spring Modulith boundary preserved — summary behavior lives inside the existing `payment` module.
- Validation: `400 validation` for invalid query parameters (`status=INVALID`, `currency=GBP`, malformed dates).
- Stable machine-readable error codes for validation failures.

### Out of Scope

- Test implementation (REST Assured, Playwright, JUnit, AssertJ tests).
- Payment order lifecycle actions: authorize, capture, cancel.
- New payment statuses beyond `CREATED`.
- `If-Match` / `412` / optimistic concurrency for summary.
- PSP integration, PSP mock flows, real cards, PAN, CVV, PCI, 3DS.
- Kafka, webhooks, event broker, messaging.
- GraphQL, gRPC.
- RLS (Row-Level Security).
- Rate limiting, cache headers.
- Refunds, settlement, reconciliation.
- Complete business dashboard, analytics platform, fake KPIs.
- CSV/PDF export.
- Multi-column group-by (e.g., currency × status cross-tabulation).
- Amount range filters or text search on summary.
- Summary history, trend analysis, or time-series buckets.
- Materialized views or pre-computed summary tables.
- Frontend implementation (deferred to optional extension after backend is green).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Payment Order Summary (Priority: P1)

A merchant payment reader views an aggregation summary of payment orders belonging to their merchant: total count, total amount in minor units, breakdown by currency, and breakdown by status.

**Why this priority**: Summary is the core aggregation behavior. It introduces `GROUP BY`, `COUNT`, `SUM` as SQL test oracles and a non-entity-mapped response DTO as a new REST Assured extraction challenge.

**Independent Test**: Can be fully tested by seeding 4 payment orders with varied currencies/amounts for a merchant, calling `GET /api/merchants/{merchantId}/payment-orders/summary`, and verifying `totalOrders=4`, `totalAmountMinor` equals the seeded sum, `byCurrency` contains one entry per currency with correct counts/sums, and `byStatus` contains `CREATED` with count=4.

**Acceptance Scenarios**:

1. **Given** a merchant has 4 payment orders (2 PLN totaling 3000, 1 EUR for 3000, 1 USD for 4000), **When** a merchant payment reader views summary, **Then** the system returns `200 OK` with `totalOrders=4`, `totalAmountMinor=10000`, `byCurrency` contains 3 entries with correct per-currency counts and sums, and `byStatus` contains one entry for `CREATED` with `orderCount=4` and `totalAmountMinor=10000`.
2. **Given** a merchant has 0 payment orders, **When** a merchant payment reader views summary, **Then** the system returns `200 OK` with `totalOrders=0`, `totalAmountMinor=0`, `byCurrency=[]`, `byStatus=[]`.
3. **Given** a merchant has 3 payment orders all in PLN, **When** a merchant payment reader views summary filtered by `?currency=PLN`, **Then** the system returns summary showing only PLN totals.
4. **Given** a merchant has 5 payment orders, **When** a merchant payment reader views summary filtered by `?fromDate=2026-05-01&toDate=2026-05-31`, **Then** only orders within the date range are included in aggregation.

---

### User Story 2 - Enforce Summary Access Boundary (Priority: P2)

The platform protects the summary endpoint with authentication, role authorization, and merchant ownership — consistent with list behavior from Lesson 07.

**Why this priority**: Summary contains aggregated financial data. Security boundary must be explicit and consistent with existing payment endpoints.

**Independent Test**: Can be tested by attempting summary access without authentication, with insufficient authority, with valid authority for the wrong merchant, and with valid authority for the correct merchant.

**Acceptance Scenarios**:

1. **Given** a user is unauthenticated, **When** they attempt to view summary, **Then** the system returns `401` and no payment data is disclosed.
2. **Given** an authenticated user has no payment role, **When** they attempt to view summary, **Then** the system returns `403 forbidden`.
3. **Given** an authenticated user has `merchant:payments:create` only, **When** they attempt to view summary, **Then** the system returns `403 forbidden`.
4. **Given** an authenticated user has `merchant:payments:operate` only, **When** they attempt to view summary, **Then** the system returns `403 forbidden`.
5. **Given** an authenticated merchant reader for merchant A, **When** they attempt to view merchant B's summary, **Then** the system returns `403 forbidden`.
6. **Given** an authenticated platform payment reader, **When** they view any merchant's summary, **Then** the system returns `200 OK` with summary data.

---

### Edge Cases

- Merchant with orders in only one currency → `byCurrency` has exactly one entry.
- Merchant with orders in all three currencies (PLN, EUR, USD) → `byCurrency` has three entries.
- Summary with `?currency=PLN` when merchant has no PLN orders → `200 OK` with `totalOrders=0`, empty groups.
- Summary with `?status=CREATED` (only existing status) → same as unfiltered for current data.
- Summary with `?fromDate` in the future → `200 OK` with `totalOrders=0`.
- Summary with `?fromDate` after `toDate` → `200 OK` with `totalOrders=0` (valid range, zero results).
- Summary for platform reader with cross-merchant access → same response shape as merchant reader for own merchant.
- Query parameter `currency=INVALID` → `400 validation`.
- Query parameter `status=INVALID` → `400 validation`.
- Query parameter `fromDate=not-a-date` → `400 validation`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-301**: System MUST allow an authenticated caller with `merchant:payments:read` for a given merchant to view payment order summary for that merchant via `GET /api/merchants/{merchantId}/payment-orders/summary`.
- **FR-302**: System MUST return `200 OK` with a summary response body containing `totalOrders` (long), `totalAmountMinor` (long), `byCurrency` (array of currency group objects), and `byStatus` (array of status group objects).
- **FR-303**: Each `byCurrency` entry MUST contain `currency` (string), `orderCount` (long), and `totalAmountMinor` (long).
- **FR-304**: Each `byStatus` entry MUST contain `status` (string), `orderCount` (long), and `totalAmountMinor` (long).
- **FR-305**: System MUST compute `totalOrders` as `COUNT(*)` from `payment_orders` filtered by merchant scope.
- **FR-306**: System MUST compute `totalAmountMinor` as `SUM(amount_minor)` from `payment_orders` filtered by merchant scope.
- **FR-307**: System MUST compute `byCurrency` groups using `GROUP BY currency` with per-group `COUNT(*)` and `SUM(amount_minor)`.
- **FR-308**: System MUST compute `byStatus` groups using `GROUP BY status` with per-group `COUNT(*)` and `SUM(amount_minor)`.
- **FR-309**: System MUST return `200 OK` with zero totals and empty group arrays when no payment orders match the merchant scope.
- **FR-310**: System MUST include `X-Correlation-ID` header in summary responses.
- **FR-311**: System MUST return `401` for unauthenticated summary requests.
- **FR-312**: System MUST return `403 forbidden` for authenticated users without `merchant:payments:read` or `platform:payments:read`.
- **FR-313**: System MUST return `403 forbidden` for cross-tenant summary attempts by merchant readers (overt refusal, consistent with list behavior).
- **FR-314**: System MUST enforce `merchant_id` claim matching for merchant-scoped readers.
- **FR-315**: System MUST allow `platform:payments:read` to view any merchant's summary.
- **FR-316**: System MUST execute summary queries as `@Transactional(readOnly = true)`.
- **FR-317**: System MUST compute aggregation in PostgreSQL using `GROUP BY`, `COUNT(*)`, `SUM(amount_minor)` — NOT by fetching entities into Java memory.
- **FR-318**: System MUST support optional query parameter `currency` filtering summary to `PLN`, `EUR`, or `USD`, returning `400 validation` for unsupported values.
- **FR-319**: System MUST support optional query parameter `status` filtering summary to `CREATED`, returning `400 validation` for unsupported values.
- **FR-320**: System MUST support optional query parameter `fromDate` (ISO date) to limit aggregation to orders with `createdAt >= fromDate` (inclusive start-of-day).
- **FR-321**: System MUST support optional query parameter `toDate` (ISO date) to limit aggregation to orders with `createdAt <= toDate` (inclusive end-of-day).
- **FR-322**: System MUST return stable machine-readable error codes for query parameter validation failures.
- **FR-323**: System MUST NOT return `ETag` on summary responses.
- **FR-324**: System MUST preserve Spring Modulith boundaries — summary behavior lives within `payment.internal`, no new public API types.
- **FR-325**: System MUST return `byCurrency` sorted by currency code alphabetically.
- **FR-326**: System MUST return `byStatus` sorted by status code alphabetically.

### Non-Functional Requirements

- **NFR-301**: Summary queries MUST use database-level aggregation, not application-level iteration.
- **NFR-302**: Summary endpoint MUST maintain the same security contract as the existing list endpoint from Lesson 07.
- **NFR-303**: The feature MUST NOT require a new Flyway migration unless a new composite index is demonstrably justified by the aggregation query shape.
- **NFR-304**: Summary endpoint MUST NOT expose data beyond the caller's authorized merchant scope.

## Quality and Architecture Impact *(mandatory)*

### Tester-Led Risk Notes

- Aggregation test oracle is different from entity-mapped response — tester must calculate expected totals from seed data, not just verify field presence.
- Empty merchant summary returning `200 OK` with zero totals is a design decision — tester should verify it does not return `404`.
- `SUM(amount_minor)` returning `null` for empty merchant must be handled as `0` in response — tester should verify this null-to-zero mapping.
- Cross-tenant summary must return `403`, not masked `404` — tester should verify consistency with list behavior from Lesson 07.
- Optional filter parameters change the aggregation population — tester should verify filter application before aggregation, not after.
- `EXPLAIN` on summary queries can reveal full table scans if indexes are missing — tester should verify index usage.

### Modulith Impact *(required for backend-relevant features; otherwise state N/A)*

- **Module Ownership**: `payment` module — unchanged. Summary behavior is a new read path within the existing module. No new `@ApplicationModule`.
- **Module API Impact**: No new public API types. Summary response DTOs are internal to `payment.internal.web`.
- **Dependency Impact**: No new module dependencies. Summary uses existing `JpaPaymentOrderRepository` or adds aggregation methods to it.
- **Event Impact**: No events. Summary is a direct synchronous read.
- **Module Test Impact**: `PaymentModuleTest` architecture verification must still pass. No new module boundary to verify.

### Security, Data, and Observability Impact

- **Authentication**: Existing JWT Resource Server — no changes.
- **Authorization**: Reuses existing `merchant:payments:read` and `platform:payments:read` authorities. No new roles.
- **Ownership**: `merchant_id` claim enforcement identical to list endpoint.
- **Validation**: Query parameter validation for `currency`, `status`, `fromDate`, `toDate` — same pattern as list validation.
- **Persistence**: Read-only aggregation queries. No new tables, no writes.
- **Transactions**: `@Transactional(readOnly = true)` — same pattern as list service.
- **Audit**: No audit trail changes — summary is read-only.
- **Logging**: `X-Correlation-ID` propagated. No sensitive data in summary response.
- **Error Contract**: Same `PaymentExceptionHandler` pattern — `400 validation` with field-level details.

### Key Entities *(include if feature involves data)*

- **Payment Order** (existing): Source of aggregation data. No new fields or status values. Aggregated by `currency` and `status` columns.
- **Summary Response** (new): Not a persisted entity. A read-only projection containing `totalOrders`, `totalAmountMinor`, and grouped breakdowns by currency and status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-301**: Merchant payment reader can view summary of their payment orders and see accurate totals, currency breakdowns, and status breakdowns.
- **SC-302**: Empty merchant returns `200 OK` with zero totals — no errors, no `404`.
- **SC-303**: Cross-tenant summary access returns `403 forbidden` — consistent with list behavior.
- **SC-304**: Summary totals match independently calculated expected values from controlled seed data.
- **SC-305**: Aggregation is computed in PostgreSQL, not in Java application memory.
- **SC-306**: Existing payment tests (Lesson 06 + Lesson 07) continue to pass after summary implementation.
- **SC-307**: Spring Modulith architecture verification passes.

## Assumptions

- Existing `payment_orders` table structure (from V2 migration) is sufficient for aggregation queries.
- Existing composite indexes (V2: `merchant_id, created_at DESC`; V3: `merchant_id, status`, `merchant_id, currency`) support summary query patterns without requiring new indexes.
- `SUM(amount_minor)` returns `NULL` for empty result sets in PostgreSQL — the service must map `NULL` to `0` in the response.
- Only `CREATED` status exists — `byStatus` will contain at most one entry for current data.
- Only `PLN`, `EUR`, `USD` currencies exist — `byCurrency` will contain at most three entries for current data.
- The endpoint path `/summary` is appended to the existing collection path (`/api/merchants/{merchantId}/payment-orders/summary`) — consistent with REST resource nesting.
- `RequestSpecBuilder` and `ResponseSpecBuilder` patterns from Lesson 07 will be reused for future summary tests.
- Frontend summary panel is deferred — backend implementation is the sole scope of this spec.
