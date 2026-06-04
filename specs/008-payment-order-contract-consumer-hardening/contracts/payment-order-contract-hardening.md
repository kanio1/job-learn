# Contracts: Payment Order Contract and Consumer Hardening

**Feature**: `008-payment-order-contract-consumer-hardening`

## Backend API Contracts

### List Payment Orders

```http
GET /api/merchants/{merchantId}/payment-orders
```

Required behavior:

| Input | Expected behavior |
|---|---|
| no query params | Successful list with defaults `page=0`, `size=20`, `sort=createdAt,desc`. |
| `page < 0` | Payment validation error before repository query execution. |
| `size < 1` | Payment validation error before repository query execution. |
| `size > 100` | Payment validation error before repository query execution. |
| unsupported `status` | Payment validation error before repository query execution. |
| unsupported `currency` | Payment validation error before repository query execution. |
| invalid date format | Payment validation error before repository query execution. |
| `fromDate > toDate` | Payment validation error before repository query execution. |
| negative `minAmount` or `maxAmount` | Payment validation error before repository query execution. |
| `minAmount > maxAmount` | Payment validation error before repository query execution. |
| unsupported `sort` | Payment validation error before repository query execution. |
| page beyond last page | Successful empty page response. |

Sort contract:

| Value | Supported |
|---|---|
| `createdAt,desc` | Yes; default. |
| `createdAt,asc` | Yes. |
| any other field or direction | No; validation error. |

### Create Payment Order

```http
POST /api/merchants/{merchantId}/payment-orders
Content-Type: application/json
Idempotency-Key: {client-generated-key}
```

Required behavior:

| Input | Expected behavior |
|---|---|
| valid JSON and valid `Idempotency-Key` | Existing `201 Created` behavior for new create or existing replay behavior. |
| duplicate valid idempotency key | Existing idempotent replay behavior remains unchanged. |
| malformed JSON | `400` with `error=malformed_json`. |
| unsupported media type | `415` with `error=unsupported_media_type`. |
| missing `Idempotency-Key` | Stable validation response. |
| blank/invalid `Idempotency-Key` | Existing validation response. |
| invalid body fields | Existing validation response with details where available. |

Successful create/replay responses must preserve existing header behavior for `Location`, `ETag` and `X-Correlation-ID` where already present.

### Payment Error Response Shape

Payment-owned errors use `PaymentErrorResponse` semantics:

```json
{
  "error": "validation",
  "message": "Request does not match the payment order contract.",
  "correlationId": "...",
  "details": []
}
```

Rules:

- `malformed_json` is used for unreadable JSON request bodies.
- `unsupported_media_type` is used for unsupported create content types.
- `validation` remains valid for request validation and missing required header failures.
- Validation messages describe the allowed contract and avoid echoing raw rejected input values.
- Responses do not expose raw tokens, raw JWT claims beyond safe user-facing messages, full request bodies, stack traces or internal class names.

### Authorization Contract

| Endpoint | Allowed actor | Ownership behavior |
|---|---|---|
| `POST /api/merchants/{merchantId}/payment-orders` | `merchant:payments:create` | Merchant token must have matching `merchant_id` claim. |
| `GET /api/merchants/{merchantId}/payment-orders` | `merchant:payments:read` | Merchant token must have matching `merchant_id` claim. |
| `GET /api/merchants/{merchantId}/payment-orders` | `platform:payments:read` | Platform reader may read selected merchant path. |
| `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` | Existing read policy | Unchanged. |
| `GET /api/merchants/{merchantId}/payment-orders/summary` | Existing summary policy | Unchanged. |

No new role, claim, realm JSON change or frontend authorization source is introduced.

## Frontend Consumer Contracts

### Routes

| Route | Required behavior |
|---|---|
| `/admin/merchants/{merchantId}/payments` | Existing dashboard list/summary route remains the reference shell. |
| `/admin/merchants/{merchantId}/payments/new` | Dashboard layout, payment create form, back link to payment orders list. |
| `/admin/merchants/{merchantId}/payments/{paymentOrderId}` | Dashboard layout, store-owned detail load, back link to payment orders list. |

### Store Responsibilities

| Responsibility | Contract |
|---|---|
| List | Existing `loadList` behavior remains. |
| Summary | Existing `loadSummary` behavior remains. |
| Detail | Store owns `loadDetail(merchantId, paymentOrderId)` or equivalent. |
| Create | Store owns `createOrder(merchantId, payload, idempotencyKey)` or equivalent. |
| Parsing | Detail/create responses parse with `paymentOrderResponseSchema`. |
| Error normalization | Backend errors parse with `backendErrorSchema` where practical. |
| Form state | Create form owns only field-local state and submit intent. |

### UI Error States

| Backend / consumer failure | Required UI behavior |
|---|---|
| `403` | Dashboard permission alert; stale protected data is not displayed. |
| detail `404` | Local neutral dashboard not-found state; not a route-level missing page. |
| backend unavailable | Dashboard-consistent unavailable alert. |
| create failure | Dashboard-consistent create failure alert. |
| malformed backend response | Safe error state; no partial stale data rendering. |

No lifecycle buttons, fake KPIs, merchant detail page, frontend role model or unsupported payment actions are introduced.
