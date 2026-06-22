# Data Model: Payment Order Aggregation Summary

## Overview

Lesson 08 does not add persisted tables or new domain entities. It adds read-only summary DTOs and repository projections over the existing `payment_orders` table.

The summary response is an API read model, not a database table.

## Existing Source Entity

### Payment Order

**Source**: `payment_orders`

**Purpose**: Existing payment order row created in Lesson 06 and listed in Lesson 07. Summary queries aggregate over this table.

**Relevant fields**:

| Field | Type | Summary Use |
|---|---|---|
| `payment_order_id` | UUID | Not returned in summary; source row identity only |
| `merchant_id` | UUID | Mandatory tenant/ownership filter |
| `amount_minor` | BIGINT | Summed into total amount values |
| `currency` | VARCHAR(3) | Grouped into `byCurrency[]`; optional filter |
| `status` | VARCHAR(20) | Grouped into `byStatus[]`; optional filter |
| `created_at` | TIMESTAMPTZ | Optional `fromDate` / `toDate` filter |

**Existing constraints**:

- `amount_minor BETWEEN 1 AND 100000000`
- `currency IN ('PLN', 'EUR', 'USD')`
- `status IN ('CREATED')`
- FK to `merchants(merchant_id)`

**Existing indexes relevant to summary**:

- `idx_payment_orders_merchant_created ON payment_orders (merchant_id, created_at DESC, payment_order_id ASC)`
- `idx_payment_orders_merchant_status ON payment_orders(merchant_id, status)`
- `idx_payment_orders_merchant_currency ON payment_orders(merchant_id, currency)`

## New API Read Models

### PaymentOrderSummaryRequest

**Kind**: Web request record, not persisted.

**Purpose**: Captures optional query filters for summary population.

| Field | Type | Required | Validation | Notes |
|---|---|---:|---|---|
| `currency` | String | No | `PLN`, `EUR`, `USD` | Filters aggregation to one currency |
| `status` | String | No | `CREATED` | Forward-compatible with future statuses |
| `fromDate` | String / parsed LocalDate | No | ISO date | Inclusive start-of-day |
| `toDate` | String / parsed LocalDate | No | ISO date | Inclusive end-of-day |

**Validation behavior**:

- Unsupported currency -> `400 validation`.
- Unsupported status -> `400 validation`.
- Malformed date -> `400 validation`.
- `fromDate` after `toDate` is valid and returns zero totals.

### PaymentOrderSummaryResponse

**Kind**: Web response record, not persisted.

**Purpose**: Represents a merchant-scoped aggregate summary.

| Field | Type | Required | Semantics |
|---|---|---:|---|
| `totalOrders` | long | Yes | Count of all matching payment orders |
| `totalAmountMinor` | long | Yes | Sum of `amount_minor` over matching payment orders; zero when empty |
| `byCurrency` | List<CurrencySummary> | Yes | Per-currency grouped totals, sorted by currency |
| `byStatus` | List<StatusSummary> | Yes | Per-status grouped totals, sorted by status |

**Empty state**:

```json
{
  "totalOrders": 0,
  "totalAmountMinor": 0,
  "byCurrency": [],
  "byStatus": []
}
```

### CurrencySummary

**Kind**: Nested response record or package-private response record.

| Field | Type | Required | Semantics |
|---|---|---:|---|
| `currency` | String | Yes | Currency code (`PLN`, `EUR`, `USD`) |
| `orderCount` | long | Yes | Count of matching orders in this currency |
| `totalAmountMinor` | long | Yes | Sum of matching order amounts in this currency |

**Ordering**: Alphabetical by `currency`.

### StatusSummary

**Kind**: Nested response record or package-private response record.

| Field | Type | Required | Semantics |
|---|---|---:|---|
| `status` | String | Yes | Payment status; currently only `CREATED` |
| `orderCount` | long | Yes | Count of matching orders in this status |
| `totalAmountMinor` | long | Yes | Sum of matching order amounts in this status |

**Ordering**: Alphabetical by `status`.

## Repository Projection Models

### SummaryTotalsProjection

**Purpose**: Carries total count and total amount from the database.

| Field | Type | Nullability | Mapping Rule |
|---|---|---|---|
| `orderCount` | long | Non-null | `COUNT(*)` |
| `totalAmountMinor` | Long | Nullable | `SUM(amount_minor)`, normalized to `0` when null |

### CurrencySummaryProjection

**Purpose**: Carries one grouped row from `GROUP BY currency`.

| Field | Type | Nullability |
|---|---|---|
| `currency` | String | Non-null |
| `orderCount` | long | Non-null |
| `totalAmountMinor` | Long | Non-null for non-empty group |

### StatusSummaryProjection

**Purpose**: Carries one grouped row from `GROUP BY status`.

| Field | Type | Nullability |
|---|---|---|
| `status` | String | Non-null |
| `orderCount` | long | Non-null |
| `totalAmountMinor` | Long | Non-null for non-empty group |

## Query Population Rules

All summary queries use the same filter population:

| Filter | Predicate |
|---|---|
| Merchant scope | `merchant_id = :merchantId` |
| Currency present | `currency = :currency` |
| Status present | `status = :status` |
| From date present | `created_at >= start_of_day(fromDate)` |
| To date present | `created_at <= end_of_day(toDate)` |

The system must apply filters before aggregation.

## Aggregate Invariants

For any summary response:

- `totalOrders == sum(byCurrency.orderCount)` when `byCurrency` is non-empty.
- `totalAmountMinor == sum(byCurrency.totalAmountMinor)` when `byCurrency` is non-empty.
- `totalOrders == sum(byStatus.orderCount)` when `byStatus` is non-empty.
- `totalAmountMinor == sum(byStatus.totalAmountMinor)` when `byStatus` is non-empty.
- Empty response has zero totals and empty arrays.
- No group row should have `orderCount=0`.

## Controlled Dataset For Future Tests

Recommended deterministic dataset:

| Order | Currency | Amount minor | Status |
|---|---|---:|---|
| A | PLN | 1000 | CREATED |
| B | PLN | 2000 | CREATED |
| C | EUR | 3000 | CREATED |
| D | USD | 4000 | CREATED |

Expected summary:

- `totalOrders = 4`
- `totalAmountMinor = 10000`
- `byCurrency`: `EUR = 1 / 3000`, `PLN = 2 / 3000`, `USD = 1 / 4000`
- `byStatus`: `CREATED = 4 / 10000`

## State Transitions

None. Summary is read-only and does not mutate payment order status or append status history.
