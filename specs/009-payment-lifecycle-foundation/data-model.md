# Data Model: Payment Lifecycle Foundation

## PaymentOrder (extended existing entity)

**Source path**: `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`

Existing fields retained:
- `paymentOrderId` UUID, primary key
- `merchantId` UUID, foreign key to merchant
- `clientOrderReference` string, max 120
- `amountMinor` long, 1..100000000
- `currency` enum/string, one of `PLN`, `EUR`, `USD`
- `status` enum/string, currently `CREATED`
- `createdAt` UTC timestamp
- `updatedAt` UTC timestamp
- `version` long, optimistic-locking source

Lifecycle fields to add or activate:
- `authorizedAt` UTC timestamp, nullable
- `expiresAt` UTC timestamp, nullable
- `capturedAt` UTC timestamp, nullable
- `cancelledAt` UTC timestamp, nullable
- `refundedAt` UTC timestamp, nullable
- `capturedAmountMinor` long, nullable, positive when present
- `refundedAmountMinor` long, nullable, positive when present
- `cancellationReason` string, nullable, bounded length
- `refundReason` string, nullable, bounded length
- `metadata` JSON/text map for PATCH metadata updates, nullable or empty by default

Validation rules:
- Status must be one of `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, `REFUNDED`.
- Authorize is valid only from `CREATED`.
- Capture is valid only from non-expired `AUTHORIZED`.
- Cancel is valid from `CREATED` or `AUTHORIZED`.
- Refund is valid only from `CAPTURED`.
- Capture amount must be positive and must not exceed the authorized order amount.
- Refund amount must be positive and must not exceed the captured amount.
- `expiresAt` is set on authorize and cleared on successful capture/cancel.
- Expired authorization capture transitions the order to `EXPIRED` and returns `422 authorization_expired`.
- Metadata PATCH must not change status and must not increment lifecycle version per `FR-LOCKING-005`.

## PaymentStatus (extended existing enum)

**Source path**: `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentStatus.java`

Values:
- `CREATED`
- `AUTHORIZED`
- `CAPTURED`
- `CANCELLED`
- `EXPIRED`
- `REFUNDED`

State transition matrix:

| From | Authorize | Capture | Cancel | Refund |
|------|-----------|---------|--------|--------|
| CREATED | AUTHORIZED | invalid_transition | CANCELLED | invalid_transition |
| AUTHORIZED | invalid_transition | CAPTURED or EXPIRED | CANCELLED | invalid_transition |
| CAPTURED | invalid_transition | invalid_transition | invalid_transition | REFUNDED |
| CANCELLED | invalid_transition | invalid_transition | invalid_transition | invalid_transition |
| EXPIRED | invalid_transition | invalid_transition | invalid_transition | invalid_transition |
| REFUNDED | invalid_transition | invalid_transition | invalid_transition | invalid_transition |

## PaymentOrderStatusHistory (extended existing entity/table)

**Source path**: `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrderStatusHistory.java`

Existing fields retained:
- `statusHistoryId` UUID, primary key
- `paymentOrderId` UUID, foreign key to payment order
- `fromStatus` string, nullable for any legacy creation entry
- `toStatus` string
- `actorSubject` string
- `correlationId` string
- `createdAt` UTC timestamp

Lifecycle fields to add:
- `idempotencyKeyHash` string, nullable only for legacy rows if needed
- `action` enum/string, one of `AUTHORIZE`, `CAPTURE`, `CANCEL`, `REFUND`, `EXPIRE`
- `reason` string, nullable
- `amountMinor` long, nullable
- `pspReference` string, nullable

Validation and integrity rules:
- History records are append-only.
- A lifecycle status update and its history insert occur in the same transaction.
- History query returns chronological lifecycle transition records for one payment order.
- Creation entries must not appear in the Lesson 14 lifecycle-history response unless the contract is explicitly revised later.

Relationships:
- Many history records belong to one `PaymentOrder`.
- History table keeps a foreign key to `payment_orders(payment_order_id)`.
- Query index remains scoped by `(payment_order_id, created_at, status_history_id)` or equivalent stable chronological order.

## IdempotencyRecord (extended existing entity/table)

**Source path**: `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/IdempotencyRecord.java`

Existing fields retained:
- `idempotencyRecordId` UUID, primary key
- `merchantId` UUID
- `idempotencyKeyHash` string
- `requestFingerprintHash` string
- `paymentOrderId` UUID
- `createdAt` UTC timestamp
- `completedAt` UTC timestamp

Lifecycle needs:
- Fingerprint must include lifecycle operation, merchant ID, payment order ID, request body, and relevant amount/reason fields.
- Same merchant plus same idempotency key plus same fingerprint returns cached lifecycle result.
- Same merchant plus same idempotency key plus different fingerprint returns `409 idempotency_conflict`.
- The current unique payment-order constraint may need revision if it blocks multiple lifecycle records for the same payment order.

## PspClient (new interface)

**Planned package**: `apps/backend/src/main/java/lab/paymentquality/payment/internal/application` or `internal/infrastructure`

Operations:
- `authorize(paymentOrderId, amountMinor, currency)`
- `capture(paymentOrderId, amountMinor, currency)`
- `voidAuthorization(paymentOrderId, pspReference)`
- `refund(paymentOrderId, amountMinor, currency)`

Rules:
- Interface is injected into lifecycle application service.
- Lesson 14 implementation always succeeds and returns a mock reference.
- No network calls or PSP failure paths are introduced in this feature.

## Frontend Schemas (extended)

**Source path**: `apps/frontend/app/schemas/payment-order.schema.ts`

Changes:
- Expand `paymentStatusSchema` to the six lifecycle statuses.
- Extend `paymentOrderResponseSchema` with optional lifecycle timestamps and amount fields that backend exposes.
- Add `paymentOrderStatusHistoryResponseSchema` for history timeline display.
- Preserve `backendErrorSchema` for lifecycle/metadata proxy failures.

## Database Migration V4

**Planned path**: `apps/backend/src/main/resources/db/migration/payment/V4__add_payment_lifecycle.sql`

Schema changes:
- Expand `chk_payment_orders_status` to all lifecycle statuses.
- Add lifecycle timestamp, amount, reason, and metadata columns as needed.
- Extend `payment_order_status_history` status constraints to all lifecycle statuses.
- Add lifecycle audit fields such as `idempotency_key_hash`, `action`, `reason`, `amount_minor`, and `psp_reference`.
- Review `idempotency_records` constraints so lifecycle actions can reuse the table without blocking more than one action per payment order.

Migration safety:
- Existing rows remain `CREATED`.
- Existing `version` column is reused.
- Existing indexes remain unless a targeted replacement is required for chronological history retrieval.
