# Contract: Summary HTTP Edge and Authorization Matrix

## Endpoint Under Test

```http
GET /api/merchants/{merchantId}/payment-orders/summary
```

This contract documents the HTTP edge behavior and authorization matrix for the existing summary endpoint. It does not introduce new API behavior.

## HTTP Edge Contract

### Route Collision Guardrail

The `/summary` literal route MUST resolve to the summary endpoint. It MUST NOT be confused with the `/{paymentOrderId}` wildcard read route.

**Verification**: Response body contains `totalOrders` (summary shape), not `paymentOrderId` (single-order shape).

### Malformed Path Variable

| Input | Expected Status | Expected Error Code | Expected Message |
|---|---|---|---|
| `merchantId=not-a-uuid` | `400` | `validation` | `Invalid merchantId: must be a valid UUID` |
| `merchantId=12345` | `400` | `validation` | `Invalid merchantId: must be a valid UUID` |
| `merchantId=` (empty) | `400` | `validation` | `Invalid merchantId: must be a valid UUID` |
| `merchantId=null` | `400` | `validation` | `Invalid merchantId: must be a valid UUID` |

### Unsupported Methods

| Method | Expected Status | Expected `Allow` Header | Notes |
|---|---|---|---|
| `PUT` | `405` | `GET, HEAD` | No mutation surface |
| `PATCH` | `405` | `GET, HEAD` | No mutation surface |
| `DELETE` | `405` | `GET, HEAD` | No mutation surface |
| `POST` | `405` | `GET, HEAD` | No mutation surface on summary URI |

### Content Negotiation

| `Accept` Header | Expected Status | Expected `Content-Type` | Notes |
|---|---|---|---|
| `application/json` | `200` | `application/json` | Normal behavior |
| `*/*` | `200` | `application/json` | Wildcard accept is permissive |
| `application/json, text/xml` | `200` | `application/json` | JSON is acceptable |
| (no `Accept` header) | `200` | `application/json` | Default behavior |
| `text/xml` | `406` or `200` | characterize first | Must not silently mislead |
| `application/xml` | `406` or `200` | characterize first | Must not silently mislead |

### Conditional Headers

| Header | Value | Expected Status | Expected `ETag` | Notes |
|---|---|---|---|---|
| `If-None-Match` | `"some-etag"` | `200` | none | Summary has no `ETag`; conditional is ignored |
| `If-Modified-Since` | any valid date | `200` | none | Summary has no cache semantics |

### Error Contract Stability

When the request reaches the application (not rejected by Spring Security or Spring MVC framework), error responses follow the existing `PaymentErrorResponse` shape:

```json
{
  "error": "validation",
  "message": "Invalid merchantId: must be a valid UUID",
  "details": null,
  "correlationId": "corr-l10-example"
}
```

For `403 Forbidden` from the controller's `AccessDeniedException`:

```json
{
  "error": "forbidden",
  "message": "Access denied",
  "details": null,
  "correlationId": "corr-l10-example"
}
```

For `401 Unauthorized` from Spring Security Resource Server, the response body shape is determined by Spring Security defaults (not `PaymentErrorResponse`), because the request does not reach the controller.

## Authorization Matrix Contract

### Authentication Failures (401)

| # | Actor | Token | Expected Status | Notes |
|---|---|---|---|---|
| 1 | Unauthenticated | none | `401` | No `Authorization` header |
| 2 | Invalid issuer | `invalidIssuerToken()` | `401` | Issuer mismatch |
| 3 | Invalid signature | `invalidSignatureToken()` | `401` | Signature tampered |
| 4 | Expired | `expiredToken()` | `401` | Token past expiration |

### Authorization Failures (403)

| # | Actor | Token | Target | Expected Status | BOLA/BFLA |
|---|---|---|---|---|---|
| 5 | Denied | `deniedToken()` | any merchant | `403` | BFLA |
| 6 | Create-only | `merchantPaymentCreatorToken(own)` | own merchant | `403` | BFLA |
| 7 | Operate-only | `merchantPaymentOperatorToken(own)` | own merchant | `403` | BFLA |
| 8 | Read no claim | `merchantPaymentReaderTokenWithoutMerchantIdClaim()` | any merchant | `403` | BFLA |
| 10 | Cross-tenant | `merchantPaymentReaderToken(A)` | merchant B | `403` | BOLA |
| 12 | Platform merchant-only | `platformOperatorToken()` | any merchant | `403` | BFLA |

### Success (200)

| # | Actor | Token | Target | Expected Status |
|---|---|---|---|---|
| 9 | Own merchant | `merchantPaymentReaderToken(own)` | own merchant | `200` |
| 11 | Platform reader | `platformPaymentReaderToken()` | any merchant | `200` |

### Response Headers (Success)

| Header | Required | Value |
|---|---|---|
| `Content-Type` | Yes | `application/json` |
| `X-Correlation-ID` | Yes | Existing or generated |
| `ETag` | No | MUST NOT be present |

### Response Body (Success)

```json
{
  "totalOrders": 0,
  "totalAmountMinor": 0,
  "byCurrency": [],
  "byStatus": []
}
```

## Non-Contractual Notes

- No new API behavior is introduced by this feature.
- This contract documents existing behavior with additional test coverage for edge cases.
- Production code changes are made only if characterization reveals a real defect.
