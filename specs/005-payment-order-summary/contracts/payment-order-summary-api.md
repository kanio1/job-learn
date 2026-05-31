# Contract: Payment Order Summary API

## Endpoint

```http
GET /api/merchants/{merchantId}/payment-orders/summary
```

Returns read-only aggregate totals for payment orders belonging to the merchant identified by `{merchantId}`.

## Authorization

| Actor | Required Authority | Additional Rule | Result |
|---|---|---|---|
| Merchant payment reader | `merchant:payments:read` | JWT `merchant_id` must equal path `{merchantId}` | `200 OK` |
| Merchant payment reader for another merchant | `merchant:payments:read` | JWT `merchant_id` differs from path `{merchantId}` | `403 forbidden` |
| Platform payment reader | `platform:payments:read` | No `merchant_id` match required | `200 OK` |
| Merchant payment creator only | `merchant:payments:create` | No read authority | `403 forbidden` |
| Merchant payment operator only | `merchant:payments:operate` | No read authority | `403 forbidden` |
| Denied identity | None | Authenticated but no payment read authority | `403 forbidden` |
| Unauthenticated | None | Missing/invalid token | `401 Unauthorized` |

## Request

### Path Parameters

| Name | Type | Required | Description |
|---|---|---:|---|
| `merchantId` | UUID | Yes | Merchant whose payment orders are summarized |

### Query Parameters

All query parameters are optional.

| Name | Type | Allowed Values | Description |
|---|---|---|---|
| `currency` | String | `PLN`, `EUR`, `USD` | Limits aggregation to one currency |
| `status` | String | `CREATED` | Limits aggregation to one payment status |
| `fromDate` | ISO date | `YYYY-MM-DD` | Includes orders created on or after this date |
| `toDate` | ISO date | `YYYY-MM-DD` | Includes orders created on or before this date |

### Headers

| Name | Required | Description |
|---|---:|---|
| `Authorization` | Yes | Bearer token containing required authority |
| `Accept` | No | `application/json` expected |
| `X-Correlation-ID` | No | Propagated by existing correlation filter when present |

## Successful Response

### `200 OK`

Headers:

| Name | Required | Description |
|---|---:|---|
| `Content-Type` | Yes | `application/json` |
| `X-Correlation-ID` | Yes | Existing or generated correlation identifier |

Body:

```json
{
  "totalOrders": 4,
  "totalAmountMinor": 10000,
  "byCurrency": [
    {
      "currency": "EUR",
      "orderCount": 1,
      "totalAmountMinor": 3000
    },
    {
      "currency": "PLN",
      "orderCount": 2,
      "totalAmountMinor": 3000
    },
    {
      "currency": "USD",
      "orderCount": 1,
      "totalAmountMinor": 4000
    }
  ],
  "byStatus": [
    {
      "status": "CREATED",
      "orderCount": 4,
      "totalAmountMinor": 10000
    }
  ]
}
```

### Empty Summary Response

```json
{
  "totalOrders": 0,
  "totalAmountMinor": 0,
  "byCurrency": [],
  "byStatus": []
}
```

## Error Responses

### `400 Bad Request` — Validation

Examples:

- `currency=GBP`
- `status=INVALID`
- `fromDate=not-a-date`
- `toDate=not-a-date`

Body shape follows existing payment error contract:

```json
{
  "error": "validation",
  "message": "Request validation failed",
  "details": [
    {
      "field": "currency",
      "message": "must be one of PLN, EUR, USD"
    }
  ]
}
```

### `401 Unauthorized`

Returned by resource server when authentication is missing, invalid, or expired.

### `403 Forbidden`

Body shape follows existing forbidden error contract:

```json
{
  "error": "forbidden",
  "message": "Access denied"
}
```

Returned for:

- Authenticated caller without payment read authority.
- Merchant reader whose `merchant_id` claim does not match path `{merchantId}`.
- Merchant creator without read authority.
- Merchant operator without read authority.

## Sorting and Ordering

- `byCurrency` sorted by `currency` ascending.
- `byStatus` sorted by `status` ascending.

## Non-Contractual Notes

- No `ETag` header is returned.
- No lifecycle actions are exposed.
- No platform-wide summary endpoint is exposed.
- No summary data is persisted.
