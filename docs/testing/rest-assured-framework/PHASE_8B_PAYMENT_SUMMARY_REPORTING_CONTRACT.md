# Phase 8B — Payment Summary / Reporting Contract

## Goal

Verify that `GET /api/merchants/{merchantId}/payment-orders/summary` returns correct aggregate
totals, enforces merchant-scope authorization, and produces RFC-7807 problem+json for invalid
query parameters.

## Files Added / Modified

| File | Change |
|------|--------|
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/payment/dto/PaymentSummaryResponse.java` | New DTO with nested `CurrencySummary` / `StatusSummary` records |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/api/payment/PaymentOrdersApi.java` | Added `summary()` and `summaryWithCurrency()` facade methods |
| `apps/api-tests/src/test/java/lab/paymentquality/apitest/scenarios/PaymentSummaryContractSpec.java` | New spec: 3 tests |
| `docs/testing/rest-assured-framework/REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` | Phase 8B row added |

## Endpoint Contract Discovered

### Route and Method

```
GET /api/merchants/{merchantId}/payment-orders/summary
```

Implemented in `PaymentOrderController.summarizePaymentOrders()`.

### Authorization

Two authority paths (same pattern as list/create):

| Caller type | JWT check |
|---|---|
| `platform:payments:read` | bypasses `merchant_id` claim check; can read any merchant's summary |
| merchant-scoped (e.g., `MERCHANT_MANAGER`) | JWT `merchant_id` claim must equal path `{merchantId}` UUID |

Mismatch throws `AccessDeniedException` → `PaymentExceptionHandler.handleAccessDenied` → 403 `forbidden`.

### Query Parameters

All optional:

| Param | Type | Validation |
|---|---|---|
| `currency` | String | PLN, EUR, or USD. Any other value → `IllegalArgumentException` → 400 `validation` |
| `status` | String | Must match `PaymentStatus` enum. Invalid value → `IllegalArgumentException` → 400 `validation` |
| `fromDate` | String | ISO date `YYYY-MM-DD`. Parse failure → `IllegalArgumentException` → 400 `validation` |
| `toDate` | String | ISO date `YYYY-MM-DD`. Parse failure → `IllegalArgumentException` → 400 `validation` |

### Response Shape (200)

```json
{
  "totalOrders": 101,
  "totalAmountMinor": 104600,
  "byCurrency": [
    { "currency": "EUR", "orderCount": 34, "totalAmountMinor": 35200 },
    { "currency": "PLN", "orderCount": 34, "totalAmountMinor": 34100 },
    { "currency": "USD", "orderCount": 33, "totalAmountMinor": 35300 }
  ],
  "byStatus": [
    { "status": "AUTHORIZED", "orderCount": 21, "totalAmountMinor": 22200 },
    { "status": "CANCELLED",  "orderCount": 19, "totalAmountMinor": 19000 },
    { "status": "CAPTURED",   "orderCount": 21, "totalAmountMinor": 23300 },
    { "status": "CREATED",    "orderCount": 21, "totalAmountMinor": 21100 },
    { "status": "REFUNDED",   "orderCount": 19, "totalAmountMinor": 19000 }
  ]
}
```

### Response Headers

```
Cache-Control: no-store
Vary: Authorization
X-Correlation-ID: <uuid>
Content-Type: application/json
```

No `ETag` (the aggregate is not version-tracked). No `Location` header.

### Error Responses

| Condition | Status | error code |
|---|---|---|
| Unsupported currency value | 400 | `validation` |
| Invalid status value | 400 | `validation` |
| Invalid date format | 400 | `validation` |
| JWT `merchant_id` ≠ path UUID (non-platform caller) | 403 | `forbidden` |

All error bodies are `application/problem+json` via `PaymentExceptionHandler`.

## SQL Ordering Contract

The backend's `JpaPaymentOrderRepository` uses explicit `ORDER BY` clauses:

```jpql
-- byCurrency
GROUP BY po.currency ORDER BY po.currency ASC
→ alphabetical: EUR < PLN < USD

-- byStatus
GROUP BY po.status ORDER BY po.status ASC
→ alphabetical: AUTHORIZED < CANCELLED < CAPTURED < CREATED < REFUNDED
```

This is a **stable contract** (not incidental ordering), safe to assert with index-based access.

## Deterministic Data Derivation

`SeedApi.seed()` loads for `MERCHANT_ALPHA_001`:

**3 named orders** (fixed amounts):
| ref | currency | status | amountMinor |
|---|---|---|---|
| SEED-ALPHA-001-CREATED | PLN | CREATED | 1 100 |
| SEED-ALPHA-001-AUTHORIZED | EUR | AUTHORIZED | 2 200 |
| SEED-ALPHA-001-CAPTURED | USD | CAPTURED | 3 300 |

**98 pagination orders** (all amountMinor=1000):
- currency cycles: PLN / EUR / USD per `offset % 3`
- status cycles: CREATED / AUTHORIZED / CAPTURED / CANCELLED / REFUNDED per `offset % 5`

**Currency totals** (named + pagination):

| currency | named | pagination count | pagination amount | total count | total amount |
|---|---|---|---|---|---|
| EUR | 2 200 | 33 | 33 000 | 34 | 35 200 |
| PLN | 1 100 | 33 | 33 000 | 34 | 34 100 |
| USD | 3 300 | 32 | 32 000 | 33 | 35 300 |
| **total** | **6 600** | **98** | **98 000** | **101** | **104 600** |

**Status totals** (named + pagination):

| status | named | pagination count | pagination amount | total count | total amount |
|---|---|---|---|---|---|
| AUTHORIZED | 2 200 | 20 | 20 000 | 21 | 22 200 |
| CANCELLED | 0 | 19 | 19 000 | 19 | 19 000 |
| CAPTURED | 3 300 | 20 | 20 000 | 21 | 23 300 |
| CREATED | 1 100 | 20 | 20 000 | 21 | 21 100 |
| REFUNDED | 0 | 19 | 19 000 | 19 | 19 000 |
| **total** | **6 600** | **98** | **98 000** | **101** | **104 600** |

## Tests Added

### `summary_for_seeded_merchant_returns_200_with_aggregate_shape`

Full aggregate contract chain:
1. Seed MERCHANT_ALPHA_001 deterministic data (101 orders).
2. Call `GET /summary` with `platform:payments:read` (merchantReader persona).
3. Assert 200, response headers (`no-store`, `Vary: Authorization`, `X-Correlation-ID`).
4. Deserialize body as `PaymentSummaryResponse`.
5. Assert all fields via `SoftAssertions`: `totalOrders=101`, `totalAmountMinor=104600`,
   3 `byCurrency` entries in alphabetical order with exact counts/amounts,
   5 `byStatus` entries in alphabetical order with exact counts/amounts.

**Key assertions:**
- `byCurrency` size is asserted first (catches extra/missing rows before index access).
- `byStatus` size is asserted first.
- All index-based accesses are protected by the preceding size assertion in `SoftAssertions`.
- No timestamps asserted (aggregate responses have no timestamp fields).

### `summary_with_invalid_currency_returns_400_validation`

Negative test for unsupported `currency` query param:
1. Call `GET /summary?currency=INVALID` (merchantReader persona).
2. Assert 400 `application/problem+json`, `error: "validation"`, `no-store`, `Vary: Authorization`.

Uses `ProblemAssert` to verify the full error contract.

### `summary_with_mismatched_merchant_scope_returns_403`

Authorization boundary test:
1. Use `seededMerchantCreator` (JWT `merchant_id = MERCHANT_ALPHA_001_ID`).
2. Call `GET /api/merchants/MERCHANT_ALPHA_002_ID/payment-orders/summary`.
3. Assert 403 `application/problem+json`, `error: "forbidden"`, `no-store`, `Vary: Authorization`.

Confirms the merchant-scope guard applies to summary as well as to individual order reads.

## Data Isolation Strategy

`PaymentSummaryContractSpec` is a standalone class with:
- `@BeforeAll SeedApi.seed()` — idempotent (clears then re-inserts), guarantees a clean 101-order state.
- `@AfterAll SeedApi.reset()` — removes all data after spec.
- **Zero write operations in any test method** — all 3 tests are read-only.

This makes the exact aggregate numbers safe to assert: no other test method can change the count mid-run.

## SoftAssertions Rationale

The happy-path test has 18 assertions across 3 top-level fields and 8 nested aggregate rows.
`SoftAssertions.assertSoftly()` collects all failures and reports them together — essential when
debugging an aggregate response where multiple fields may be wrong simultaneously (e.g., wrong SQL
GROUP BY clause affects counts and amounts in all rows at once).

## SDET Interview Topics

- Why is a reporting/aggregate endpoint harder to test than a CRUD endpoint?
  (Results depend on the full dataset state; isolation requires either read-only tests or
  a private dataset.)
- Why does `Cache-Control: no-store` matter on an aggregate response?
  (Stale aggregates could expose wrong totals to risk/compliance systems.)
- When is it safe to assert exact aggregate numbers in a black-box test?
  (When the dataset is deterministic seed-only AND the spec performs no writes.)
- How do you verify that a SQL `GROUP BY` query correctly handles `null` sums for empty groups?
  (Use `COALESCE(SUM(...), 0)`; test with a filtered query that returns no matching rows.)
- What does `ORDER BY po.currency ASC` in JPQL guarantee that JDBC result set ordering does not?
  (A stable, database-level sort — not dependent on memory layout or result set cursor order.)

## Test Results

- **79 offline tests**: all pass (unchanged)
- **46 live tests**: all pass (3 new in Phase 8B)
