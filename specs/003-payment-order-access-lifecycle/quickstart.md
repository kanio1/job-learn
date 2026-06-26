# Quickstart: Payment Order Create/Read Foundation

**Feature**: `003-payment-order-access-lifecycle`  
**Branch**: `004-payment-order-create-read`

## Purpose

This quickstart describes how a local learner/tester should exercise the planned payment order feature after implementation. It is intentionally limited to create and read. Lifecycle actions, PSP calls, cards, Kafka, webhooks, refunds, settlement, and payment lists are out of scope.

## Prerequisites

- Java 25 available on `PATH`
- Maven Wrapper from `apps/backend`
- Node/pnpm compatible with `apps/frontend/package.json`
- Docker or compatible container runtime for PostgreSQL 18, Keycloak 26.6.1, and Testcontainers
- Local Keycloak realm import updated with payment roles and test identities

## Start Local Infrastructure

From repository root:

```bash
cd infra/compose
docker compose up -d
```

Expected services:

- PostgreSQL 18 for backend persistence
- Keycloak 26.6.1 with `payment-quality` realm

## Run Backend

From `apps/backend`:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Public health/status check:

```bash
curl -i http://localhost:8080/api/status
```

`/api/status` should remain public and should not reveal payment order data.

## Run Frontend

From `apps/frontend`:

```bash
pnpm install
pnpm dev
```

Open the dashboard at:

```text
http://localhost:3000
```

The dashboard should use server-mediated Keycloak authentication. Browser state must not expose access tokens or refresh tokens.

## Local Test Identities

Planned identities after implementation:

| Username | Purpose | Authorities |
|---|---|---|
| `merchant.payment.creator` | Create payment orders for one active merchant | `merchant:payments:create` |
| `merchant.payment.reader` | Read payment orders for one merchant | `merchant:payments:read` |
| `merchant.payment.operator` | Planned unused lifecycle role | `merchant:payments:operate` only |
| `platform.payment.reader` | Support/investigation cross-merchant reader | `platform:payments:read` |
| `merchant.denied` | Denial-path identity | none |

Merchant-scoped identities need a single `merchant_id` claim or equivalent local realm mapper. Full Merchant Team Management is not part of this feature.

## Manual API Walkthrough

Use a valid token for a merchant payment creator and an active merchant ID.

Create request:

```bash
curl -i \
  -X POST "http://localhost:8080/api/merchants/$MERCHANT_ID/payment-orders" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-local-001" \
  -H "X-Correlation-ID: quickstart-payment-001" \
  -d '{
    "amountMinor": 12500,
    "currency": "PLN",
    "clientOrderReference": "PAY-local-001"
  }'
```

Expected first response:

- `201 Created`
- `Location` header
- `ETag` header
- `X-Correlation-ID: quickstart-payment-001`
- Body with status `CREATED`

Replay the same request with the same `Idempotency-Key` and same body:

```bash
curl -i \
  -X POST "http://localhost:8080/api/merchants/$MERCHANT_ID/payment-orders" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: idem-local-001" \
  -H "X-Correlation-ID: quickstart-payment-002" \
  -d '{
    "amountMinor": 12500,
    "currency": "PLN",
    "clientOrderReference": "PAY-local-001"
  }'
```

Expected replay response:

- `200 OK`
- Same `paymentOrderId`
- No duplicate payment order

Reuse the same key with a different amount or reference:

Expected response:

- `409 Conflict`
- `error = idempotency_conflict`

Read by ID:

```bash
curl -i \
  "http://localhost:8080/api/merchants/$MERCHANT_ID/payment-orders/$PAYMENT_ORDER_ID" \
  -H "Authorization: Bearer $READER_TOKEN" \
  -H "X-Correlation-ID: quickstart-payment-read-001"
```

Expected response for correct merchant reader:

- `200 OK`
- `ETag` header
- `X-Correlation-ID` header
- Body with amount, currency, status `CREATED`, and timestamps

Expected response for wrong merchant reader:

- `404 not_found`

Expected response for platform payment reader:

- `200 OK` for any merchant payment order

## Dashboard Walkthrough

1. Sign in as a merchant payment creator.
2. Navigate to an active merchant context from the merchant dashboard.
3. Open the payment order create action.
4. Enter amount in minor units, currency, and client order reference.
5. Submit and verify success feedback.
6. Verify the payment order detail view shows status `CREATED`, amount, currency, reference, and timestamps.
7. Sign in as a reader and verify read-only access.
8. Sign in as an identity without create authority and verify the create action is hidden while backend direct calls still return `403`.

No lifecycle buttons should be shown.

## Verification Commands

Backend from `apps/backend`:

```bash
./mvnw test
./mvnw verify
```

Frontend from `apps/frontend`:

```bash
pnpm typecheck
pnpm test:e2e
```

If local Keycloak or PostgreSQL is already running from `infra/compose`, ensure ports match the existing project configuration before running browser tests.

## Parallel Test Data Convention

Use namespaced values:

```text
clientOrderReference = PAY-{testRunId}-{workerId}-{uuid}
idempotencyKey      = idem-{testRunId}-{workerId}-{uuid}
correlationId       = corr-{testRunId}-{workerId}-{uuid}
```

Only reuse the same idempotency key inside tests that intentionally verify replay or conflict behavior.
