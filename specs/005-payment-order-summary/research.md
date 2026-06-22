# Research: Payment Order Aggregation Summary

## Decision 1: Keep Summary In Existing Payment Module

**Decision**: Implement summary behavior inside the existing `payment` Spring Modulith module under `lab.paymentquality.payment.internal.*`.

**Rationale**: Summary is a read path over existing `payment_orders`. It does not introduce a new domain lifecycle, module boundary, public module API, or cross-module collaboration. Keeping it in the owning module preserves the modular monolith boundary and makes `PaymentModuleTest` the relevant architecture verification.

**Alternatives considered**:

- New reporting module: rejected as premature because there is no independent reporting domain or cross-module read model yet.
- Shared analytics module: rejected because it would create fake platform analytics scope and violate the lesson guardrail against complete dashboards.

## Decision 2: Use `/summary` Under Existing Collection Path

**Decision**: Add `GET /api/merchants/{merchantId}/payment-orders/summary`.

**Rationale**: The endpoint is merchant-scoped and conceptually belongs to the payment order collection. The `/summary` suffix clearly distinguishes aggregation from list and single-resource read.

**Implementation caveat**: Security matcher and controller mapping must avoid collision with `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`. The summary matcher should appear before wildcard single-resource matchers.

**Alternatives considered**:

- `/api/merchants/{merchantId}/payment-order-summary`: rejected because it creates a separate resource name outside the established payment-order path.
- `/api/merchants/{merchantId}/payment-orders/report`: rejected because `report` suggests broader analytics/export scope.
- `/api/platform/payment-orders/summary`: rejected because platform-wide analytics is out of scope.

## Decision 3: Compute Aggregates In PostgreSQL

**Decision**: Compute totals and grouped rows with database aggregation: `COUNT(*)`, `SUM(amount_minor)`, `GROUP BY currency`, and `GROUP BY status`.

**Rationale**: The feature's learning purpose is SQL aggregation and DB-backed reporting. Fetching all payment orders into Java and summing in memory would hide the relevant SQL behavior, scale poorly, and make `EXPLAIN` meaningless.

**Alternatives considered**:

- Application-level aggregation over entities: rejected due to performance and learning mismatch.
- Materialized view or summary table: rejected as premature for current local-lab scale and because no async refresh model exists.
- Native SQL only: acceptable if JPQL projection becomes unclear, but JPQL/Spring Data projection is preferred first for minimal integration with the existing repository.

## Decision 4: Use Existing Read Roles, No New Summary Role

**Decision**: Reuse `merchant:payments:read` and `platform:payments:read` for summary access.

**Rationale**: Summary is read-only payment data. Existing read roles already authorize access to list and single-resource read. A new `payments:summary` role would expand security model complexity without current business need.

**Alternatives considered**:

- New `merchant:payments:summary` role: rejected because it would require Keycloak realm and test JWT changes, contrary to the requested system-only, minimal scope.
- Allow `merchant:payments:create`: rejected because create authority does not imply read/reporting access.
- Allow `merchant:payments:operate`: rejected because operate is reserved for future lifecycle actions.

## Decision 5: Cross-Tenant Summary Returns 403

**Decision**: Merchant reader trying to summarize another merchant receives `403 forbidden`.

**Rationale**: Summary is an overt collection/report operation. This follows Lesson 07 list behavior, where `403` does not reveal individual payment order existence. Single-resource masked `404` from Lesson 06 remains unchanged for object-level reads.

**Alternatives considered**:

- Masked `404`: rejected because collection/report operations in this project intentionally use overt denial.
- Empty `200`: rejected because it could hide authorization bugs and incorrectly imply the caller has access to that merchant scope.

## Decision 6: Limit Optional Filters To Currency, Status, FromDate, ToDate

**Decision**: Summary supports only `currency`, `status`, `fromDate`, and `toDate` filters.

**Rationale**: These filters align naturally with aggregation dimensions and existing indexes. Amount range and text search belong to list/search behavior and would expand the reporting scope beyond the lesson goal.

**Alternatives considered**:

- Reuse all Lesson 07 list filters: rejected as too broad and not necessary for summary learning.
- No filters at all: rejected because date/currency/status filters make aggregation population visible and useful for future testing.

## Decision 7: No New Flyway Migration By Default

**Decision**: Do not create a V4 migration initially. Reuse existing indexes:

- `idx_payment_orders_merchant_created` from V2.
- `idx_payment_orders_merchant_status` from V3.
- `idx_payment_orders_merchant_currency` from V3.

**Rationale**: Summary filters are merchant-scoped and optionally currency/status/date-scoped. Existing indexes support the expected predicates sufficiently for local-lab scale. Adding an index without observed need adds schema churn and can distract from aggregation learning.

**Alternatives considered**:

- Add `(merchant_id, currency, status, created_at)` composite index: rejected until `EXPLAIN` shows a need.
- Add separate summary table: rejected as premature and out of scope.

## Decision 8: No Frontend Changes In This System Slice

**Decision**: Backend-only implementation. Frontend summary cards and Nuxt proxy remain optional future extension.

**Rationale**: The user explicitly requested system implementation, not tests, and the current lesson goal is backend aggregation. A full dashboard is prohibited by Phase 0 guardrails. Frontend work should happen only after backend behavior is compiled and contract is stable.

**Alternatives considered**:

- Add Nuxt proxy and summary cards now: rejected to keep first slice focused and avoid fake dashboard behavior.
- Add global admin dashboard: rejected as complete business dashboard scope creep.

## Decision 9: Map Empty Aggregates To Zero Totals

**Decision**: Empty summary returns `totalOrders=0`, `totalAmountMinor=0`, `byCurrency=[]`, and `byStatus=[]`.

**Rationale**: A merchant with no payment orders is a valid report state. PostgreSQL `SUM` over empty results can return `NULL`; the service must normalize that to `0` for a stable API contract.

**Alternatives considered**:

- Return `404`: rejected because the merchant summary exists even if data is empty.
- Return `null` for `totalAmountMinor`: rejected because clients should not need to special-case missing numeric totals.

## Decision 10: No ETag For Summary

**Decision**: Summary responses do not include `ETag`.

**Rationale**: Summary is transient collection-derived data, not a single versioned representation. `ETag` in Lesson 06 is a compatibility point for future single-resource lifecycle concurrency and does not apply to this read-only aggregation.

**Alternatives considered**:

- Weak ETag for summary: rejected as unnecessary for current scope and likely to distract from aggregation learning.
