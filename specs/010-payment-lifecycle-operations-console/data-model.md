# Data Model: Payment Lifecycle Operations Console

Feature 010 introduces application view models and request/feedback state. It does not introduce new backend lifecycle entities, new database tables, new lifecycle statuses, or new lifecycle transitions.

## Payment Order Detail View Model

**Primary source paths**:
- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`

Fields:
- `paymentOrderId` UUID
- `merchantId` UUID
- `clientOrderReference` string when returned by the existing detail contract
- `status` one of `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, `REFUNDED`
- `amountMinor` integer
- `currency` one of `PLN`, `EUR`, `USD`
- `authorizedAt`, `expiresAt`, `capturedAt`, `cancelledAt`, `refundedAt` nullable timestamps
- `capturedAmountMinor`, `refundedAmountMinor` nullable integers
- `cancellationReason`, `refundReason` nullable strings
- `metadata` optional key/value object when returned by backend
- `versionMarker` application-held value derived from backend `ETag` or response version field

Validation and display rules:
- Unknown lifecycle status must fail schema parsing instead of rendering misleading actions.
- Nullable lifecycle fields render only when present and relevant.
- Terminal statuses `CANCELLED`, `EXPIRED`, and `REFUNDED` render no lifecycle mutation action.
- Sensitive values such as bearer tokens, raw credentials, and idempotency key hashes must never be displayed.

## Lifecycle Status History Entry View Model

**Primary source paths**:
- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/components/payment/`

Fields:
- `statusHistoryId` UUID
- `paymentOrderId` UUID
- `fromStatus` nullable lifecycle status
- `toStatus` lifecycle status
- `action` nullable action label from backend
- `createdAt` timestamp
- `actorDisplay` optional safe actor value derived from backend actor field
- `reason` optional safe reason value when returned
- `amountMinor` optional amount value when returned
- `pspReference` optional PSP reference when returned and safe for display
- `correlationId` optional safe support value when returned

Validation and display rules:
- History renders oldest-first by `createdAt` plus stable backend ordering when already supplied.
- Empty history renders an empty state, not an error.
- History loading/error state is independent from payment detail loading/error state.
- Raw tokens, credentials, and idempotency key hashes are not part of the display model.

## Lifecycle Action Request View Model

**Primary source paths**:
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/components/payment/`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/authorize.post.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/capture.post.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/cancel.post.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/refund.post.ts`

Action matrix:

| Current status | Offered actions |
|----------------|-----------------|
| `CREATED` | authorize, cancel |
| `AUTHORIZED` | capture, cancel |
| `CAPTURED` | refund |
| `CANCELLED` | none |
| `EXPIRED` | none |
| `REFUNDED` | none |

Request fields:
- `action` one of `AUTHORIZE`, `CAPTURE`, `CANCEL`, `REFUND`
- `amountMinor` optional for capture/refund; omitted means full amount according to Feature 009 behavior
- `reason` optional for authorize/cancel/refund where supported by backend contract
- `versionMarker` required for `If-Match`
- `idempotencyKey` optional from browser flow; proxy generates one when absent
- `correlationId` optional from browser/request context; proxy forwards when present or uses application convention

Validation and behavior rules:
- Capture, cancel, and refund require confirmation before submission.
- The UI must not offer impossible transitions.
- Failed mutation must not show success feedback.
- `412` stale-state responses trigger user feedback plus detail/history refresh and no automatic retry.
- `422 invalid_transition` triggers domain-error feedback plus refresh to remove stale controls.

## Metadata Update Request View Model

**Primary source paths**:
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/components/payment/`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].patch.ts`

Fields:
- `metadata` key/value map
- `versionMarker` required for `If-Match`
- `correlationId` optional

Rules:
- Metadata editing is separate from lifecycle action controls.
- Metadata update must not imply lifecycle status change.
- Successful metadata update refreshes payment detail.
- Stale metadata update informs the user and refreshes current detail before further editing.

## User Feedback State

**Primary source path**: `apps/frontend/app/stores/payment-orders.ts`

States:
- detail loading
- history loading
- action submitting
- metadata saving
- success
- empty history
- forbidden/access denied
- validation error
- invalid transition
- stale state/precondition failure
- idempotency conflict
- not found
- backend unavailable/unexpected backend error

Rules:
- Feedback state must be scoped so history failure does not erase detail summary.
- Forbidden responses must never appear as successful mutation or metadata update.
- Backend-unavailable state must not clear existing user input unless the user leaves the page.

## Backend Response-Contract Fallback

**Allowed only if needed**: existing payment module response DTOs may be adjusted to include already-specified display fields such as lifecycle timestamps, lifecycle amounts/reasons, safe actor/history fields, metadata, and version marker.

Not allowed:
- New lifecycle states or transitions
- Multi-capture or multi-refund behavior
- PSP failure scenarios
- Kafka, webhooks, or asynchronous lifecycle flows
- New test classes as feature deliverables
- REST Assured framework changes
