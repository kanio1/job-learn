# Quickstart: Payment Order Aggregation Summary

This quickstart verifies the system-only implementation path for Lesson 08. It does not add or run new REST Assured or Playwright tests.

## Scope

System behavior to implement:

```http
GET /api/merchants/{merchantId}/payment-orders/summary
```

Expected response shape:

```json
{
  "totalOrders": 4,
  "totalAmountMinor": 10000,
  "byCurrency": [
    { "currency": "EUR", "orderCount": 1, "totalAmountMinor": 3000 },
    { "currency": "PLN", "orderCount": 2, "totalAmountMinor": 3000 },
    { "currency": "USD", "orderCount": 1, "totalAmountMinor": 4000 }
  ],
  "byStatus": [
    { "status": "CREATED", "orderCount": 4, "totalAmountMinor": 10000 }
  ]
}
```

## Implementation Order

1. Add summary request/response records under `payment.internal.web`.
2. Add repository aggregation projections/methods under `payment.internal.infrastructure`.
3. Add `PaymentOrderSummaryService` under `payment.internal.application`.
4. Add `GET /summary` method to `PaymentOrderController`.
5. Add `SecurityConfig` matcher for `GET /api/merchants/*/payment-orders/summary` before wildcard single-resource GET matcher.
6. Compile backend.
7. Run module architecture verification.

## Verification Commands

Run from `apps/backend`:

```bash
./mvnw clean compile
./mvnw -DskipTests package
./mvnw -Dtest=PaymentModuleTest test
```

No new REST Assured or Playwright tests are expected in this slice.

## Manual API Check, Optional

If local backend and auth setup are running, create or reuse a valid token and call:

```bash
curl -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/json" \
  "http://localhost:8080/api/merchants/$MERCHANT_ID/payment-orders/summary"
```

Expected:

- `200 OK` for merchant reader matching `$MERCHANT_ID`.
- `200 OK` for platform payment reader.
- `403 forbidden` for merchant reader of another merchant.
- `403 forbidden` for creator-only or operate-only identity.
- `401` for missing/invalid token.

## SQL Diagnostic Exercise, Optional

Run an `EXPLAIN` on the aggregation query once implementation query shape is known:

```sql
EXPLAIN
SELECT currency, COUNT(*), SUM(amount_minor)
FROM payment_orders
WHERE merchant_id = '<merchant-id>'
GROUP BY currency
ORDER BY currency;
```

Check whether an existing merchant-leading index is used or whether a future V4 index is justified.

## Guardrails

Do not implement in this slice:

- REST Assured tests.
- Playwright tests.
- authorize/capture/cancel.
- new payment statuses.
- PSP integration or PSP mock.
- Kafka/webhooks.
- complete dashboard.
- platform-wide analytics endpoint.
- materialized views or summary tables.
