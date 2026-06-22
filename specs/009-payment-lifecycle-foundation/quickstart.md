# Quickstart: Payment Lifecycle Foundation

## Purpose

Use this quickstart to understand and validate the planned Lesson 14 payment lifecycle foundation after implementation tasks are generated and completed.

## Prerequisites

- Backend and frontend dependencies are installed.
- Docker is available for PostgreSQL Testcontainers where backend integration tests run.
- Current branch is `009-payment-lifecycle-foundation`.
- `.specify/feature.json` points to `specs/009-payment-lifecycle-foundation`.

## Expected Backend Flow

1. Create a payment order with the existing create endpoint.
2. Read the order and capture its `ETag`.
3. Authorize with `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize`, `Idempotency-Key`, and `If-Match`.
4. Capture with `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`, a new `Idempotency-Key`, and the updated `If-Match`.
5. Retrieve `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history` and verify chronological lifecycle transitions.

## Expected HTTP Signals

- Successful lifecycle mutation returns `200 OK`.
- Response includes `ETag: "v{version}"`.
- Response includes `Cache-Control: no-store`.
- Response includes `Vary: Authorization, If-Match`.
- Stale `If-Match` returns `412 concurrency_conflict`.
- Invalid transitions return `422 invalid_transition`.
- Idempotency key conflict returns `409 idempotency_conflict`.

## Regression Commands

Backend regression command from repo root:

```bash
./mvnw -pl apps/backend test
```

Backend focused payment/security regression paths to inspect when task generation starts:
- `apps/backend/src/test/java/lab/paymentquality/rest/`
- `apps/backend/src/test/java/lab/paymentquality/security/`
- `apps/backend/src/test/java/lab/paymentquality/payment/PaymentModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/architecture/ModulithArchitectureTest.java`

Frontend typecheck from `apps/frontend`:

```bash
corepack pnpm typecheck
```

Frontend E2E regression from `apps/frontend` if UI display changes are implemented:

```bash
corepack pnpm test:e2e
```

## Tester Learning Prompts

- Build a decision table for six statuses times four lifecycle actions.
- Design stale ETag tests where two clients read the same version and race lifecycle actions.
- Design idempotency replay tests for same key/same action and same key/different action.
- Apply boundary value analysis to capture and refund amounts: zero, negative, exact amount, just over amount.
- Verify dev/test CORS preflight behavior separately from production profile behavior.
- Verify status history does not duplicate entries during idempotent replay.

## Known Planning Constraints

- No lifecycle action buttons are planned in the frontend.
- No PSP failure simulation is planned.
- No scheduled expiration job is planned.
- No Kafka, webhooks, event pipeline, or async lifecycle behavior is planned.
- No new automated tests are created by `/speckit.plan`; implementation and automation tasks are created later by `/speckit.tasks`.

## Implementation Summary

### Backend Changes

- **V4 Flyway migration**: `apps/backend/src/main/resources/db/migration/payment/V4__add_payment_lifecycle.sql`
  - Adds lifecycle timestamp, amount, reason, and metadata columns to `payment_orders`
  - Expands status constraints to 6 lifecycle statuses
  - Adds lifecycle audit fields to `payment_order_status_history`
  - Adds `action` column to `idempotency_records` and drops single-order-per-idempotency constraint

- **Domain model**:
  - `PaymentStatus` expanded to 6 values: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED
  - `PaymentOrder` extended with lifecycle fields and transition methods (authorize, capture, cancel, refund, updateMetadata)
  - `PaymentOrderStatusHistory` extended with lifecycle audit fields and `lifecycleEntry` factory
  - `PaymentLifecycleAction` enum: AUTHORIZE, CAPTURE, CANCEL, REFUND, EXPIRE
  - `RequestFingerprint.forLifecycle()` for lifecycle idempotency fingerprints
  - Domain exceptions: `InvalidStateTransitionException`, `AuthorizationExpiredException`, `InvalidCaptureAmountException`, `InvalidRefundAmountException`

- **Application layer**:
  - `PaymentLifecycleService` with authorize, capture, cancel, refund, updateMetadata, findHistory
  - `PspClient` interface with authorize, capture, voidAuthorization, refund
  - `MockPspClient` always-success implementation

- **Web layer**:
  - `PaymentOrderController` extended with lifecycle endpoints, metadata PATCH, and history GET
  - ETag format changed to `"v{version}"` per FR-LOCKING-002
  - Lifecycle responses include `Cache-Control: no-store` and `Vary: Authorization, If-Match`
  - `PaymentExceptionHandler` extended with 412, 422 lifecycle error mappings

- **Security**:
  - `SecurityConfig` extended with lifecycle, PATCH, and history matchers
  - CORS configuration active for dev/test profiles only
  - Keycloak realm extended with `merchant:payments:lifecycle`, `platform:payments:lifecycle`, `platform:payments:audit` roles and corresponding users

### Frontend Changes

- **Schemas**: Expanded `paymentStatusSchema` to 6 statuses, added lifecycle fields to response schema, added `paymentStatusHistoryResponseSchema`
- **Components**: `PaymentStatusBadge` with lifecycle colors, `PaymentOrderDetail` with lifecycle fields and history timeline
- **Proxy routes**: authorize, capture, cancel, refund, history, metadata PATCH

### Regression Results

- Backend: 216 tests pass, 0 failures, 5 skipped
- Frontend: typecheck passes

## Deferred Scope

- Multi-capture, multi-refund (Lesson 15)
- PSP failure simulation (Lesson 16)
- Webhooks, Kafka, event pipeline (Lesson 17)
- Scheduled expiration job (Lesson 16)
- API versioning, HATEOAS, rate limiting
- Complete OAuth/OIDC integration
- Complete business dashboards
- Performance/load testing
