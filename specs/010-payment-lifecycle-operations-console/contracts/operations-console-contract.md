# Contract: Payment Lifecycle Operations Console

This contract defines the application display and Nuxt server proxy behavior for Feature 010. It consumes Feature 009 backend lifecycle contracts rather than creating new payment lifecycle semantics.

## Browser-Facing Nuxt Routes

Base path:

```text
/api/merchants/{merchantId}/payment-orders/{paymentOrderId}
```

Existing route files:
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].get.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId].patch.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/history.get.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/authorize.post.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/capture.post.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/cancel.post.ts`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/[paymentOrderId]/refund.post.ts`

## Header Forwarding Contract

Nuxt server proxy routes must preserve lifecycle protocol behavior:

| Header | Detail GET | History GET | Metadata PATCH | Lifecycle POST |
|--------|------------|-------------|----------------|----------------|
| `Authorization` | forward from user session | forward from user session | forward from user session | forward from user session |
| `ETag` | expose current version marker to app state when backend returns it | N/A | expose updated marker when backend returns it | expose updated marker when backend returns it |
| `If-Match` | N/A | N/A | send current version marker | send current version marker |
| `Idempotency-Key` | N/A | N/A | N/A | forward if present, otherwise generate one per mutation attempt |
| `X-Correlation-ID` | forward/use app convention | forward/use app convention | forward/use app convention | forward/use app convention |
| `Cache-Control` | preserve safe no-store behavior when returned | preserve safe no-store behavior when returned | preserve when returned | preserve when returned |
| `Vary` | preserve when returned | preserve when returned | preserve when returned | preserve when returned |

The browser UI must not display `Authorization`, raw idempotency keys, idempotency key hashes, credentials, or other secret/internal values.

## Detail Display Contract

The payment detail UI must render:
- current lifecycle status badge for `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, `REFUNDED`
- amount and currency
- lifecycle timestamps only when present
- captured/refunded amounts only when present
- cancellation/refund reasons only when present
- current metadata when present
- current version marker as internal application state, not as prominent business content

## History Display Contract

The history UI must:
- load from `GET /history`
- render entries oldest-first
- show from status, to status, and action timestamp
- show safe actor information when returned
- show reason, amount, and PSP reference when returned and safe
- show empty state when no entries exist
- show history loading/error state without removing payment detail summary

## Lifecycle Action Contract

State-aware actions:

| Current status | Authorize | Capture | Cancel | Refund |
|----------------|-----------|---------|--------|--------|
| `CREATED` | offered | hidden | offered | hidden |
| `AUTHORIZED` | hidden | offered | offered | hidden |
| `CAPTURED` | hidden | hidden | hidden | offered |
| `CANCELLED` | hidden | hidden | hidden | hidden |
| `EXPIRED` | hidden | hidden | hidden | hidden |
| `REFUNDED` | hidden | hidden | hidden | hidden |

Submission rules:
- Capture, cancel, and refund require confirmation.
- Capture amount is optional where backend supports it; omitted means full capture.
- Refund amount is optional where backend supports it; omitted means full refund.
- Cancellation/refund reasons are optional unless backend validation says otherwise.
- Success reloads detail and history.
- Failure shows scoped feedback and must not show success.

## Metadata Contract

Metadata update must:
- use a separate edit flow from lifecycle actions
- send current version marker as `If-Match`
- not send `Idempotency-Key`
- refresh detail after success
- keep lifecycle status unchanged unless refreshed backend detail shows a change caused elsewhere

## Error Mapping Contract

Nuxt proxy and UI must preserve enough backend information to map:

| Backend condition | HTTP status | UI state |
|-------------------|-------------|----------|
| Missing/invalid auth | `401` | authentication/access problem, not lifecycle success |
| Forbidden role or merchant ownership | `403` | access denied |
| Unknown payment order | `404` | not found |
| Idempotency conflict | `409` | retry-safety conflict |
| Stale version/precondition failed | `412` | stale state, reload detail/history, no auto-retry |
| Unsupported media type | `415` | validation/request format feedback |
| Invalid transition or domain validation | `422` | invalid transition or validation feedback, refresh when state may be stale |
| Backend unavailable | `503` or proxy fallback | backend unavailable |

The UI may display safe `correlationId` values for support diagnostics when backend provides them.

## Explicit Non-Goals

- No REST Assured framework work
- No new test classes as feature deliverables
- No frontend E2E deliverable
- No multi-capture
- No multi-refund
- No PSP failure scenarios or PSP provider integration
- No Kafka, webhooks, scheduled jobs, or asynchronous lifecycle flow
- No full dashboard, fake KPIs, or fake operational metrics
- No complete OAuth/OIDC application integration
- No new payment lifecycle behavior beyond Feature 009 semantics
- No new payment creation capability, including no `POST /payments` scope
