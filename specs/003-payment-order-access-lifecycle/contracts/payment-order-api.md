# REST API Contract: Payment Order Create/Read

**Feature**: `003-payment-order-access-lifecycle`  
**Branch**: `004-payment-order-create-read`  
**Base Path**: `/api`

## Contract Principles

- All payment endpoints require authentication.
- `GET /api/status` remains public and exposes no payment data.
- Merchant users require the relevant payment authority and matching merchant scope.
- Platform payment readers with `platform:payments:read` can read any merchant payment order.
- Cross-tenant merchant reads return masked `404 not_found`.
- Missing or invalid authentication returns `401`.
- Authenticated users missing required authority return `403 forbidden`.
- Error responses use stable machine-readable `error` codes.
- `X-Correlation-ID` is propagated or generated and returned in every payment response.

## Headers

### Request Headers

| Header | Required | Applies To | Rules |
|---|---:|---|---|
| `Authorization: Bearer <token>` | Yes | All payment endpoints | JWT from Keycloak or signed test JWT |
| `Idempotency-Key` | Yes | Create only | Non-blank, printable ASCII, max 128 chars |
| `X-Correlation-ID` | No | All payment endpoints | If absent, backend generates one; max 128 chars if supplied |
| `Content-Type: application/json` | Yes | Create only | Required for request body |

### Response Headers

| Header | Applies To | Rules |
|---|---|---|
| `Location` | First successful create | Absolute or path URI of created payment order |
| `ETag` | Successful create and read | Strong opaque validator: `"po-<paymentOrderId>-v<version>"` |
| `X-Correlation-ID` | All payment responses | Echo supplied valid value or generated value |

## Error Response Shape

```json
{
  "error": "validation",
  "message": "Request validation failed",
  "details": [
    {
      "field": "amountMinor",
      "message": "must be between 1 and 100000000"
    }
  ],
  "correlationId": "corr-123"
}
```

`details` is optional and used primarily for validation failures.

## Error Codes

| HTTP Status | Error Code | Meaning |
|---:|---|---|
| `400` | `validation` | Malformed path, invalid body, missing/blank idempotency key, invalid correlation ID |
| `401` | Spring security default or `unauthorized` | Missing, invalid, expired, wrong issuer, or invalid signature token |
| `403` | `forbidden` | Authenticated caller lacks required payment authority |
| `404` | `not_found` | Unknown payment order or masked cross-tenant read |
| `409` | `merchant_not_payment_eligible` | Merchant exists but is not active for payment creation |
| `409` | `idempotency_conflict` | Same idempotency key reused with a different request fingerprint |

## Create Payment Order

```http
POST /api/merchants/{merchantId}/payment-orders
```

### Required Authority

`merchant:payments:create`

### Ownership Rule

The authenticated merchant user must be scoped to `{merchantId}` through the JWT merchant scope claim. Platform payment readers cannot create payment orders.

### Path Parameters

| Name | Type | Required | Rules |
|---|---|---:|---|
| `merchantId` | UUID | Yes | Must be a syntactically valid UUID |

### Request Body

```json
{
  "amountMinor": 12500,
  "currency": "PLN",
  "clientOrderReference": "PAY-lesson6-worker1-8f80a0"
}
```

### Request Schema

| Field | Type | Required | Rules |
|---|---|---:|---|
| `amountMinor` | integer | Yes | `1..100_000_000` |
| `currency` | string | Yes | `PLN`, `EUR`, or `USD`; uppercase only |
| `clientOrderReference` | string | Yes | Trimmed, non-blank, max 120 chars |

### First Success Response

```http
HTTP/1.1 201 Created
Location: /api/merchants/4ad7af0a-ec54-48e4-a70e-77a885fd42ef/payment-orders/3a4ab97d-ff76-44ab-83b2-f252eaed5e4d
ETag: "po-3a4ab97d-ff76-44ab-83b2-f252eaed5e4d-v0"
X-Correlation-ID: lesson6-corr-001
Content-Type: application/json
```

```json
{
  "paymentOrderId": "3a4ab97d-ff76-44ab-83b2-f252eaed5e4d",
  "merchantId": "4ad7af0a-ec54-48e4-a70e-77a885fd42ef",
  "clientOrderReference": "PAY-lesson6-worker1-8f80a0",
  "amountMinor": 12500,
  "currency": "PLN",
  "status": "CREATED",
  "createdAt": "2026-05-27T19:20:00Z",
  "updatedAt": "2026-05-27T19:20:00Z"
}
```

### Idempotent Replay Response

Same `Idempotency-Key` and same fingerprint:

```http
HTTP/1.1 200 OK
ETag: "po-3a4ab97d-ff76-44ab-83b2-f252eaed5e4d-v0"
X-Correlation-ID: lesson6-corr-002
Content-Type: application/json
```

Body is the existing payment order representation. No duplicate payment order or status history row is created.

### Conflict Response

Same `Idempotency-Key` and different fingerprint:

```http
HTTP/1.1 409 Conflict
X-Correlation-ID: lesson6-corr-003
Content-Type: application/json
```

```json
{
  "error": "idempotency_conflict",
  "message": "The idempotency key was already used with a different request fingerprint.",
  "correlationId": "lesson6-corr-003"
}
```

### Create Status Codes

| Scenario | Expected Status |
|---|---:|
| Valid first create | `201` |
| Same key and same fingerprint | `200` |
| Same key and different fingerprint | `409 idempotency_conflict` |
| Merchant not active | `409 merchant_not_payment_eligible` |
| Missing/blank `Idempotency-Key` | `400 validation` |
| Invalid amount | `400 validation` |
| Unsupported or malformed currency | `400 validation` |
| Blank or oversized client reference | `400 validation` |
| Malformed merchant ID | `400 validation` |
| Missing/invalid/expired token | `401` |
| Authenticated without `merchant:payments:create` | `403 forbidden` |
| Merchant scope mismatch | `403 forbidden` for create because the caller is authenticated but not allowed to create for that merchant |

## Read Payment Order

```http
GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

### Required Authority

One of:

- `merchant:payments:read` with matching merchant scope
- `platform:payments:read`

`merchant:payments:operate` alone does not grant read access.

### Path Parameters

| Name | Type | Required | Rules |
|---|---|---:|---|
| `merchantId` | UUID | Yes | Target merchant context |
| `paymentOrderId` | UUID | Yes | Target payment order ID |

### Success Response

```http
HTTP/1.1 200 OK
ETag: "po-3a4ab97d-ff76-44ab-83b2-f252eaed5e4d-v0"
X-Correlation-ID: lesson6-corr-004
Content-Type: application/json
```

```json
{
  "paymentOrderId": "3a4ab97d-ff76-44ab-83b2-f252eaed5e4d",
  "merchantId": "4ad7af0a-ec54-48e4-a70e-77a885fd42ef",
  "clientOrderReference": "PAY-lesson6-worker1-8f80a0",
  "amountMinor": 12500,
  "currency": "PLN",
  "status": "CREATED",
  "createdAt": "2026-05-27T19:20:00Z",
  "updatedAt": "2026-05-27T19:20:00Z"
}
```

### Read Status Codes

| Scenario | Expected Status |
|---|---:|
| Merchant reader reads own merchant payment order | `200` |
| Platform payment reader reads any merchant payment order | `200` |
| Merchant reader attempts cross-tenant read | `404 not_found` |
| Payment order does not exist | `404 not_found` |
| Malformed merchant ID or payment order ID | `400 validation` |
| Missing/invalid/expired token | `401` |
| Authenticated without `merchant:payments:read` or `platform:payments:read` | `403 forbidden` |
| User has only `merchant:payments:operate` | `403 forbidden` |

## Security Matrix

| Actor | Create | Read Own Merchant | Read Other Merchant | Notes |
|---|---:|---:|---:|---|
| Unauthenticated | `401` | `401` | `401` | No payment data disclosed |
| Denied identity | `403` | `403` | `403` | Authenticated but no payment role |
| Merchant payment creator only | `201`/`200` | `403` | `403` or `404` depending endpoint ownership phase | Create does not imply read |
| Merchant payment reader only | `403` | `200` | `404` | Masked cross-tenant read |
| Merchant payment operator only | `403` | `403` | `403` | Planned unused role |
| Platform payment reader | `403` | `200` | `200` | Cross-merchant read only |

## Explicitly Absent Endpoints

These endpoints must not exist in this slice:

- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel`
- `GET /api/merchants/{merchantId}/payment-orders`
- `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `DELETE /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- Any PSP, card, webhook, refund, settlement, GraphQL, or gRPC endpoint
