# Contract: Payment Lifecycle API

## Common Path Parameters

- `merchantId`: UUID path parameter.
- `paymentOrderId`: UUID path parameter.

Base path:

```text
/api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

## Common Request Headers

- `Authorization: Bearer <jwt>` required for all endpoints.
- `X-Correlation-ID` optional; response includes the effective correlation ID.
- `Idempotency-Key` required for lifecycle actions only.
- `If-Match: "v{version}"` required for lifecycle actions and metadata PATCH.
- `Content-Type: application/json` required when a body is present.

## Common Response Headers

- `ETag: "v{version}"` on payment-order mutation responses.
- `Cache-Control: no-store` on lifecycle and history responses.
- `Vary: Authorization, If-Match` on lifecycle mutation responses.
- `X-Correlation-ID` on all responses.

## Error Body

All errors use the existing payment error shape:

```json
{
  "error": "invalid_transition",
  "message": "Cannot capture from CREATED status",
  "correlationId": "corr-123",
  "details": {}
}
```

## Authorize

```text
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize
```

Request body:

```json
{
  "reason": "customer_checkout"
}
```

Success response: `200 OK`

```json
{
  "paymentOrderId": "00000000-0000-0000-0000-000000000001",
  "merchantId": "00000000-0000-0000-0000-000000000010",
  "status": "AUTHORIZED",
  "amountMinor": 10000,
  "currency": "PLN",
  "authorizedAt": "2026-06-04T20:00:00Z",
  "expiresAt": "2026-06-11T20:00:00Z"
}
```

## Capture

```text
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture
```

Request body:

```json
{
  "amountMinor": 5000,
  "reason": "shipment_1"
}
```

Success response: `200 OK`

```json
{
  "paymentOrderId": "00000000-0000-0000-0000-000000000001",
  "merchantId": "00000000-0000-0000-0000-000000000010",
  "status": "CAPTURED",
  "amountMinor": 10000,
  "capturedAmountMinor": 5000,
  "currency": "PLN",
  "capturedAt": "2026-06-04T20:05:00Z"
}
```

Rules:
- Omitted `amountMinor` means full capture.
- `amountMinor <= 0` returns `400 validation` or equivalent validation error.
- `amountMinor` greater than authorized amount returns `422 capture_amount_exceeds_authorized`.
- Capture at or after `expiresAt` returns `422 authorization_expired` and transitions the order to `EXPIRED`.

## Cancel

```text
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel
```

Request body:

```json
{
  "reason": "customer_cancelled"
}
```

Success response: `200 OK`

```json
{
  "paymentOrderId": "00000000-0000-0000-0000-000000000001",
  "merchantId": "00000000-0000-0000-0000-000000000010",
  "status": "CANCELLED",
  "cancelledAt": "2026-06-04T20:10:00Z",
  "cancellationReason": "customer_cancelled"
}
```

## Refund

```text
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund
```

Request body:

```json
{
  "amountMinor": 5000,
  "reason": "customer_return"
}
```

Success response: `200 OK`

```json
{
  "paymentOrderId": "00000000-0000-0000-0000-000000000001",
  "merchantId": "00000000-0000-0000-0000-000000000010",
  "status": "REFUNDED",
  "refundedAmountMinor": 5000,
  "refundedAt": "2026-06-04T20:15:00Z",
  "refundReason": "customer_return"
}
```

Rules:
- Omitted `amountMinor` means full refund of captured amount.
- `amountMinor` greater than captured amount returns `422 refund_amount_exceeds_captured`.

## Metadata PATCH

```text
PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

Request headers:
- `If-Match: "v{version}"` required.
- `Idempotency-Key` not required.

Request body:

```json
{
  "metadata": {
    "supportTicket": "SUP-123",
    "note": "Customer requested invoice resend"
  }
}
```

Success response: `200 OK`

Rules:
- Metadata PATCH must not change status.
- Metadata PATCH must not increment lifecycle version per feature spec.
- Unknown metadata keys are stored without semantic validation.

## Status History

```text
GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history
```

Success response: `200 OK`

```json
{
  "content": [
    {
      "statusHistoryId": "00000000-0000-0000-0000-000000000100",
      "paymentOrderId": "00000000-0000-0000-0000-000000000001",
      "fromStatus": "CREATED",
      "toStatus": "AUTHORIZED",
      "action": "AUTHORIZE",
      "actorSubject": "merchant-user-1",
      "correlationId": "corr-123",
      "createdAt": "2026-06-04T20:00:00Z"
    }
  ]
}
```

## Authorization Matrix

| Endpoint | Merchant lifecycle owner | Platform lifecycle | Platform audit | Merchant read | Platform read |
|----------|--------------------------|--------------------|----------------|---------------|---------------|
| Authorize | allow matching merchant | allow any merchant | deny | deny | deny |
| Capture | allow matching merchant | allow any merchant | deny | deny | deny |
| Cancel | allow matching merchant | allow any merchant | deny | deny | deny |
| Refund | allow matching merchant | allow any merchant | deny | deny | deny |
| Metadata PATCH | allow matching merchant | allow any merchant | deny | deny | deny |
| History GET | deny unless also lifecycle/read role is granted by implementation policy | deny unless also audit/read role is granted by implementation policy | allow any merchant | deny unless existing read policy is intentionally extended | deny unless existing read policy is intentionally extended |

Cross-tenant merchant lifecycle access must be denied using the existing ownership-check pattern unless the caller has `platform:payments:lifecycle`.

## Status and Error Contract

| Condition | HTTP status | Error code |
|-----------|-------------|------------|
| Missing `Idempotency-Key` on lifecycle action | 400 | `validation` or `missing_required_header` |
| Missing `If-Match` | 400 | `missing_required_header` |
| Stale `If-Match` | 412 | `concurrency_conflict` |
| Invalid state transition | 422 | `invalid_transition` |
| Expired authorization capture | 422 | `authorization_expired` |
| Capture amount too high | 422 | `capture_amount_exceeds_authorized` |
| Refund amount too high | 422 | `refund_amount_exceeds_captured` |
| Same idempotency key, different action/fingerprint | 409 | `idempotency_conflict` |
| Missing/wrong role | 403 | existing security error contract |
| Unknown payment order | 404 | existing not-found contract |

## CORS Preflight

```text
OPTIONS /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize
```

Dev/test profile expected headers:
- `Access-Control-Allow-Origin`
- `Access-Control-Allow-Methods`
- `Access-Control-Allow-Headers`

Production profile must not enable broad CORS unless separately specified.
