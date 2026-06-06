# Contract: Payment HTTP Contract Resilience Hardening

Base resource:

```text
/api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

## Common Headers

### Request

- `Authorization: Bearer <jwt>` required for actual payment resource requests except CORS preflight/contract `OPTIONS`.
- `X-Correlation-ID` optional. The backend returns the supplied value or generates one.
- `If-Match: "v{version}"` required for lifecycle actions and metadata PATCH.
- `Idempotency-Key` required for lifecycle actions and create payment order.
- `Content-Type: application/json` required for JSON body requests.
- `Content-Type: application/merge-patch+json` is the preferred metadata PATCH media type.
- `Content-Type: application/json` remains temporarily accepted for metadata PATCH because the current frontend proxy sends JSON by default.
- `Accept: application/json` is supported for success responses.

### Response

- `X-Correlation-ID` always present on payment resource success and error responses.
- `ETag: "v{version}"` present on payment order detail and mutation responses.
- `Cache-Control: no-store` present on payment resource data and payment problem responses.
- `Vary: Authorization` for authenticated payment reads.
- `Vary: Authorization, If-Match` for conditional mutations.
- `Accept-Patch: application/merge-patch+json` where metadata PATCH support is advertised or a patch content type is rejected.

## Conditional Update Contract

Protected endpoints:

```text
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund
PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

Rules:

- Missing `If-Match` returns `428 Precondition Required`.
- Malformed `If-Match` returns `400 Bad Request`.
- Stale `If-Match` returns `412 Precondition Failed`.
- Matching `If-Match` allows the operation to proceed.
- Version comparison happens after authorization/tenant checks and before idempotency side effects, PSP calls, or domain mutation.
- Exact duplicate lifecycle replay with the same idempotency scope and same request fingerprint is recognized before stale-version rejection. This supports the real lost-response retry case where the first request succeeded and advanced the resource version.

Supported ETag syntax:

```text
"v0"
"v1"
"v42"
```

Unsupported examples:

```text
v1
"1"
"vabc"
*
```

## Problem Details Error Body

All payment controller errors use `application/problem+json` and include compatibility fields.

Example:

```json
{
  "type": "https://api.payment-quality.local/problems/payment-order-version-mismatch",
  "title": "Precondition Failed",
  "status": 412,
  "detail": "Payment order was modified after the client loaded it.",
  "code": "PAYMENT_ORDER_VERSION_MISMATCH",
  "correlationId": "corr-123",
  "error": "payment_order_version_mismatch",
  "message": "Payment order was modified after the client loaded it.",
  "details": null
}
```

## Status Code Distinctions

- `409 Conflict`: idempotency conflict or resource conflict not caused by a stale `If-Match` version.
- `412 Precondition Failed`: the client supplied a syntactically valid `If-Match`, but it does not match the current payment order version.
- `422 Unprocessable Entity`: the request is syntactically valid but violates lifecycle domain rules, such as invalid transition or amount greater than captured/authorized amount.
- `428 Precondition Required`: the endpoint requires a conditional update header and the client did not send it.

## Cache Contract

Payment resources use:

```text
Cache-Control: no-store
```

This applies to create, detail, list, summary, lifecycle action responses, metadata patch responses, history, and payment resource errors including masked `404`. Payment order details can contain lifecycle state, amounts, merchant identity, metadata, and operational context. They should not be retained by browser, proxy, or shared intermediary caches.

## OPTIONS Contract

Payment order detail:

```text
OPTIONS /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

Response:

```text
204 No Content
Allow: GET, HEAD, PATCH, OPTIONS
Accept-Patch: application/merge-patch+json
```

Lifecycle action example:

```text
OPTIONS /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture
```

Response:

```text
204 No Content
Allow: POST, OPTIONS
```

CORS preflight is not a payment business request. It may be allowed without a bearer token so the browser can discover whether the actual request is permitted. The actual `POST`, `PATCH`, `GET`, or `HEAD` request still requires authentication and authorization.

## HEAD Contract

```text
HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

Rules:

- Applies the same read authority and tenant rules as GET detail.
- Returns no body.
- Includes `ETag`.
- Includes `Cache-Control: no-store`.
- Includes `Vary: Authorization`.
- Includes `X-Correlation-ID`.

## Idempotency Scope

Create scope:

```text
merchantId + Idempotency-Key + action=CREATE
```

Lifecycle scope:

```text
merchantId + paymentOrderId + lifecycle action + Idempotency-Key
```

Fingerprint inputs:

- merchant ID
- operation/action
- payment order ID for lifecycle actions
- request payload facts
- amount where present
- reason where present

This prevents unsafe cases where the same key is treated as the same request for different actions, different payment orders, different merchants, or different payloads.

Replay rule:

- Same merchant + same payment order + same lifecycle action + same idempotency key + same fingerprint returns the current payment order representation without re-running the domain mutation.
- Same scope/key with a different fingerprint returns `409 Conflict`.
- New lifecycle work with a stale `If-Match` returns `412 Precondition Failed`.

## Payment Detail Representation

Payment order detail is the resource representation used by the operations console. It includes base payment fields and lifecycle facts:

- `paymentOrderId`
- `merchantId`
- `clientOrderReference`
- `amountMinor`
- `currency`
- `status`
- `capturedAmountMinor`
- `refundedAmountMinor`
- `authorizedAt`
- `expiresAt`
- `capturedAt`
- `cancelledAt`
- `refundedAt`
- `cancellationReason`
- `refundReason`
- `metadata`
- `createdAt`
- `updatedAt`

The current version marker is carried by the `ETag` header. Browser/application clients must preserve the exact quoted value, for example `"v3"`, when sending `If-Match`.

## Future Rest Assured Test Scenarios

Automated tests are intentionally not part of this phase. The next phase should cover:

- stale `If-Match` -> `412`
- missing `If-Match` -> `428`
- malformed `If-Match` -> `400`
- payment detail -> `Cache-Control: no-store`
- error response -> `application/problem+json`
- correlationId header equals body
- `HEAD` returns headers and no body
- `OPTIONS` returns `Allow` and `Accept-Patch`
- unsupported `Content-Type` -> `415`
- unsupported `Accept` -> `406`
- idempotency conflict -> `409`
- lifecycle lost-response retry with same idempotency key -> no duplicate domain mutation
- proxy preserves quoted `ETag` and forwards it as `If-Match`
- metadata PATCH forwards `If-Match` and uses `application/merge-patch+json`
