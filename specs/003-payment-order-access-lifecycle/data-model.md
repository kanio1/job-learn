# Data Model: Payment Order Access, Idempotent Creation, And Minimal Create/Read Lifecycle Foundation

**Feature**: `003-payment-order-access-lifecycle`  
**Branch**: `004-payment-order-create-read`  
**Date**: 2026-05-27

## Overview

This feature introduces the first payment-domain persistence model. The data model supports merchant-owned payment orders, idempotent creation, and an initial append-only status history entry. It deliberately does not model authorization, capture, cancel, PSP interaction, card data, refunds, settlement, webhooks, or payment listing.

## Entity Relationship Summary

```text
merchants (existing Phase 1 table)
  1 ── * payment_orders

payment_orders
  1 ── 1 idempotency_records
  1 ── * payment_order_status_history
```

`payment` owns `payment_orders`, `idempotency_records`, and `payment_order_status_history`. The foreign key to `merchants` protects data integrity, but application eligibility checks go through the merchant module public API rather than `merchant.internal` packages.

## Entity: PaymentOrder

**Purpose**: Merchant-owned resource representing an intended payment created by an authorized merchant actor.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `paymentOrderId` | UUID | Yes | Application-generated internal identifier; distinct from client reference |
| `merchantId` | UUID | Yes | FK to `merchants.merchant_id`; must be active at create time via merchant public API |
| `clientOrderReference` | String | Yes | Trimmed, non-blank, max 120 characters; namespaced in tests |
| `amountMinor` | Long | Yes | Integer minor units only; `1..100_000_000` |
| `currency` | String | Yes | Exactly one of `PLN`, `EUR`, `USD`; uppercase and case-sensitive |
| `status` | PaymentStatus | Yes | Initial and only first-slice value: `CREATED` |
| `createdAt` | Instant | Yes | Set on create; durable across restarts |
| `updatedAt` | Instant | Yes | Same as `createdAt` on create; future lifecycle updates will change it |
| `version` | Long | Yes | JPA optimistic-locking version; source for ETag |

### Validation Rules

- Amount is never represented as decimal or floating point.
- Amount `0`, negative values, and `100_000_001+` return `400 validation`.
- Unsupported currency codes such as `GBP` return `400 validation`.
- Malformed currency values such as `PL`, `PLNN`, `pln`, or `123` return `400 validation`.
- Client order reference must be trimmed server-side; blank or oversized values return `400 validation`.
- Creation for `DRAFT` or `SUSPENDED` merchant returns `409 merchant_not_payment_eligible`.

## Entity: IdempotencyRecord

**Purpose**: Durable deduplication record for create payment order retries.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `idempotencyRecordId` | UUID | Yes | Application-generated internal identifier |
| `merchantId` | UUID | Yes | FK to `merchants.merchant_id`; part of uniqueness scope |
| `idempotencyKeyHash` | String | Yes | SHA-256 hex of trimmed `Idempotency-Key`; length 64 |
| `requestFingerprintHash` | String | Yes | SHA-256 hex of canonical request fingerprint; length 64 |
| `paymentOrderId` | UUID | Yes after success | FK to `payment_orders.payment_order_id` |
| `createdAt` | Instant | Yes | Time the record was reserved |
| `completedAt` | Instant | Yes after success | Time the payment order link was completed |

### Validation And Conflict Rules

- Missing or blank `Idempotency-Key` returns `400 validation`.
- Key length above 128 characters returns `400 validation`.
- Same `(merchantId, idempotencyKeyHash)` and same `requestFingerprintHash` returns `200 OK` with the existing payment order identity.
- Same `(merchantId, idempotencyKeyHash)` and different `requestFingerprintHash` returns `409 idempotency_conflict`.
- The unique database constraint on `(merchant_id, idempotency_key_hash)` is the authoritative concurrency guard.

## Entity: PaymentOrderStatusHistory

**Purpose**: Append-only audit trail for payment order status changes. This slice writes only the creation entry.

| Field | Type | Required | Rules |
|---|---|---:|---|
| `statusHistoryId` | UUID | Yes | Application-generated internal identifier |
| `paymentOrderId` | UUID | Yes | FK to `payment_orders.payment_order_id` |
| `fromStatus` | PaymentStatus nullable | No | `NULL` for create entry |
| `toStatus` | PaymentStatus | Yes | `CREATED` in this slice |
| `actorSubject` | String | Yes | JWT subject or equivalent authenticated principal; max 200 |
| `correlationId` | String | Yes | Propagated or generated `X-Correlation-ID`; max 128 |
| `createdAt` | Instant | Yes | Time history entry was appended |

### Audit Rules

- Initial create must append a row with `from_status = NULL` and `to_status = 'CREATED'`.
- Correlation ID in status history must match the response `X-Correlation-ID`.
- Actor subject must not contain tokens or authorization headers.

## Value Object: PaymentStatus

| Status | First-Slice Meaning | Allowed Transitions In This Slice |
|---|---|---|
| `CREATED` | Payment order exists but no external authorization/capture/cancel has occurred | None |

Future statuses such as `AUTHORIZED`, `CAPTURED`, and `CANCELED` are intentionally absent from implementation. They may be added in the lifecycle slice with explicit transition rules.

## SQL Schema Draft

Migration path:

`apps/backend/src/main/resources/db/migration/payment/V1__create_payment_orders.sql`

```sql
CREATE TABLE payment_orders (
    payment_order_id       UUID PRIMARY KEY,
    merchant_id            UUID NOT NULL,
    client_order_reference VARCHAR(120) NOT NULL,
    amount_minor           BIGINT NOT NULL,
    currency               VARCHAR(3) NOT NULL,
    status                 VARCHAR(20) NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_orders_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (merchant_id),
    CONSTRAINT chk_payment_orders_amount_minor
        CHECK (amount_minor BETWEEN 1 AND 100000000),
    CONSTRAINT chk_payment_orders_currency
        CHECK (currency IN ('PLN', 'EUR', 'USD')),
    CONSTRAINT chk_payment_orders_status
        CHECK (status IN ('CREATED'))
);

CREATE INDEX idx_payment_orders_merchant_created
    ON payment_orders (merchant_id, created_at DESC, payment_order_id ASC);

CREATE TABLE idempotency_records (
    idempotency_record_id    UUID PRIMARY KEY,
    merchant_id              UUID NOT NULL,
    idempotency_key_hash     CHAR(64) NOT NULL,
    request_fingerprint_hash CHAR(64) NOT NULL,
    payment_order_id         UUID,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at             TIMESTAMPTZ,
    CONSTRAINT fk_idempotency_records_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants (merchant_id),
    CONSTRAINT fk_idempotency_records_payment_order
        FOREIGN KEY (payment_order_id) REFERENCES payment_orders (payment_order_id),
    CONSTRAINT uk_idempotency_records_merchant_key
        UNIQUE (merchant_id, idempotency_key_hash),
    CONSTRAINT uk_idempotency_records_payment_order
        UNIQUE (payment_order_id),
    CONSTRAINT chk_idempotency_records_key_hash
        CHECK (idempotency_key_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_idempotency_records_fingerprint_hash
        CHECK (request_fingerprint_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX idx_idempotency_records_payment_order
    ON idempotency_records (payment_order_id);

CREATE TABLE payment_order_status_history (
    status_history_id UUID PRIMARY KEY,
    payment_order_id  UUID NOT NULL,
    from_status       VARCHAR(20),
    to_status         VARCHAR(20) NOT NULL,
    actor_subject     VARCHAR(200) NOT NULL,
    correlation_id    VARCHAR(128) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_order_status_history_order
        FOREIGN KEY (payment_order_id) REFERENCES payment_orders (payment_order_id),
    CONSTRAINT chk_payment_order_status_history_from_status
        CHECK (from_status IS NULL OR from_status IN ('CREATED')),
    CONSTRAINT chk_payment_order_status_history_to_status
        CHECK (to_status IN ('CREATED'))
);

CREATE INDEX idx_payment_order_status_history_order_created
    ON payment_order_status_history (payment_order_id, created_at ASC, status_history_id ASC);
```

## Transaction Boundary

Create payment order is one application-service transaction:

1. Validate path, header, and body.
2. Authorize authority and merchant scope.
3. Check active merchant eligibility through merchant public API.
4. Compute idempotency key hash and request fingerprint hash.
5. Reserve or load idempotency record.
6. If existing same fingerprint, return existing payment order as replay.
7. If existing different fingerprint, return `409 idempotency_conflict`.
8. Persist payment order.
9. Persist initial status history.
10. Link idempotency record to payment order and complete it.

The payment order, idempotency record, and status history must commit or roll back together.

## Read Model

Read by ID loads one payment order by `(merchantId, paymentOrderId)` for merchant users. Platform readers may read by `paymentOrderId` for any merchant, while the URL still includes the target `merchantId` for context. Unknown IDs and cross-tenant reads both return `404 not_found` for merchant readers.

## ETag Model

ETag is generated from `paymentOrderId` and `version`:

```text
"po-<paymentOrderId>-v<version>"
```

No `If-Match` request handling is implemented in this slice.

## Parallel Test Data

- Payment references: `PAY-{testRunId}-{workerId}-{uuid}`.
- Idempotency keys: `idem-{testRunId}-{workerId}-{uuid}`.
- Concurrency tests intentionally reuse one idempotency key inside a single scenario.
- REST and repository tests create their own active merchants through existing merchant support or a dedicated payment test fixture.
