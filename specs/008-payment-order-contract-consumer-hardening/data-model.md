# Data Model: Payment Order Contract and Consumer Hardening

**Feature**: `008-payment-order-contract-consumer-hardening`

## Model Decision

No new persistent data model is introduced by this feature.

The feature hardens request, response, authorization and frontend consumer behavior around existing data. It does not add a table, column, enum value, index, database trigger, materialized view, lock mode, isolation-level rule or migration by default.

## Existing Persistent Entities

| Entity / structure | Current role | Change in this feature |
|---|---|---|
| `merchants` | Merchant ownership root for merchant-scoped payment orders. | None. |
| `payment_orders` | Existing payment order storage boundary. | None. |
| payment order idempotency records | Existing per-merchant idempotent create/replay support. | None. |
| payment status history | Existing foundation audit/history support for current status model. | None. |

## Existing Constraints And Indexes

| Rule | Existing source | Planned change |
|---|---|---|
| Payment order belongs to a merchant | FK from `payment_orders.merchant_id` to `merchants.merchant_id` | None |
| Amount is bounded | Existing `amount_minor` check constraint | None |
| Currency is allowlisted | Existing currency check constraint for `PLN`, `EUR`, `USD` | None |
| Status is foundation-only | Existing status check constraint for `CREATED` | None |
| Idempotency is unique per merchant/key | Existing unique constraint | None |
| List filtering has supporting indexes | Existing V3 payment order list indexes | None by default |

## Request Models

### Payment Order List Request

The list request model is treated as a production contract, not a test-only convenience.

Fields and rules:

| Field | Rule |
|---|---|
| `page` | Default `0`; must be `>= 0` when supplied. |
| `size` | Default `20`; must be between `1` and `100` when supplied. |
| `sort` | Default `createdAt,desc`; allowed values are `createdAt,asc` and `createdAt,desc`. |
| `status` | Optional; supported value remains `CREATED`. |
| `currency` | Optional; supported values remain `PLN`, `EUR`, `USD`. |
| `fromDate` / `toDate` | Optional ISO date values; when both supplied, `fromDate <= toDate`. |
| `minAmount` / `maxAmount` | Optional non-negative minor units; when both supplied, `minAmount <= maxAmount`. |

Invalid request model values fail before repository query execution.

### Payment Order Create Request

The create request body remains the existing payment order create body. This feature does not add business fields.

Protocol rules added or clarified:

| Input aspect | Rule |
|---|---|
| `Content-Type` | Must be JSON. Unsupported media type returns `415`. |
| JSON body | Malformed JSON returns `400`. |
| `Idempotency-Key` | Required; missing key returns stable validation response. |
| Successful create/replay | Existing idempotency, `Location`, `ETag` and `X-Correlation-ID` behavior remains unchanged. |

## Frontend Store State

The feature may add or clarify non-persistent Pinia state only.

| Store state / action | Purpose |
|---|---|
| `loadList` | Existing list behavior remains. |
| `loadSummary` | Existing summary behavior remains. |
| `loadDetail` | Store-owned detail API load and response parsing. |
| `createOrder` | Store-owned create API call and response parsing. |
| `currentOrder` | Current detail resource; cleared on forbidden/not-found/malformed response as appropriate. |
| `lastCreatedOrder` | Existing post-create state, set only after Zod-validated response. |
| `loading`, `error`, `insufficientAuthority` | Store-owned API state; form components do not mutate these directly. |

## Migration Policy

No migration should be generated for this feature unless implementation uncovers a concrete production data gap that invalidates the specification. If that happens, stop the implementation path and document the gap before changing schema.
