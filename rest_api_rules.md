# REST API Rules

This repository has real REST APIs. Design tests against the implemented contract, not old Phase 0 assumptions.

## Public Status

`GET /api/status`

- Public without JWT.
- Returns `200`.
- Body contains `application`, `phase`, and `status`.
- Must not expose database, Keycloak, merchant, payment, or secret details.

## Merchant API

Endpoints:

- `POST /api/merchants`
- `GET /api/merchants`
- `GET /api/merchants/{id}`
- `POST /api/merchants/{id}/activate`
- `POST /api/merchants/{id}/suspend`

Rules:

- Create requires `platform:merchants:create` and returns `201`.
- List/retrieve require `platform:merchants:read` and return `200`.
- Activate/suspend require `platform:merchants:update-status` and return `200`.
- Missing, invalid, or expired JWT returns `401`.
- Valid JWT without authority returns `403`.
- Malformed UUID returns `400`.
- Unknown merchant returns `404`.
- Duplicate normalized reference returns `409 duplicate_merchant_reference`.
- Invalid lifecycle transition returns `409 invalid_transition`.
- Merchant references are trimmed, uppercased, constrained to 3-64 letters/numbers/hyphens with no leading/trailing hyphen.
- Response assertions should include `merchantId`, `merchantReference`, `displayName`, and `status`.

## Payment Order API

Endpoints:

- `POST /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `GET /api/merchants/{merchantId}/payment-orders/summary`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund`

Rules:

- Create requires `merchant:payments:create`, matching `merchant_id`, active merchant eligibility, JSON body, and `Idempotency-Key`.
- Payment reads require `merchant:payments:read` with matching `merchant_id`, or `platform:payments:read`.
- Lifecycle mutations require `merchant:payments:lifecycle` with matching `merchant_id`, or `platform:payments:lifecycle`.
- History can be read by the implemented read/lifecycle/audit authorities.
- Create returns `201` first time and `200` for same idempotent replay.
- Idempotency conflict returns `409`.
- Non-active merchant creation returns `409 merchant_not_payment_eligible`.
- Cross-merchant single-resource reads are masked where implemented.
- Lifecycle invalid transitions return `422`.
- Conditional lifecycle/metadata updates use the current `ETag` / `If-Match` contract where implemented.
- Payment resource responses should preserve implemented `ETag`, `X-Correlation-ID`, `Cache-Control`, `Vary`, `Location`, `Allow`, and `Accept-Patch` headers.
- Do not invent ETag, idempotency, or correlation requirements for endpoints where the current code/spec does not implement them.

## Test Design Expectations

- For each endpoint, cover happy path, validation, unauthorized, forbidden, not found, conflict, and relevant lifecycle/tenant cases.
- Assert response body and headers, not status code alone.
- Add persistence assertions when the risk is database state, uniqueness, idempotency, lifecycle history, or versioning.
- Use unique merchant references, client order references, and idempotency keys.
- Keep REST Assured tests readable and focused on business/API oracles.
