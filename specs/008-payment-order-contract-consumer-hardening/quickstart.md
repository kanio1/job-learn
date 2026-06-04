# Quickstart: Payment Order Contract and Consumer Hardening

**Feature**: `008-payment-order-contract-consumer-hardening`

## Scope Reminder

This quickstart is for implementing and verifying the production hardening slice. It is not a prompt to write new tests.

Do not add:

- REST Assured test classes,
- Playwright specs,
- test-support clients/builders/specs,
- test-only production hooks,
- lifecycle actions or statuses,
- PSP/Kafka/webhook/outbox/event flows,
- Keycloak roles or realm JSON,
- database migrations by default,
- fake dashboard KPIs.

## Implementation Sequence

### 1. Backend Contract Hardening

1. Make the list endpoint bind query parameters through `PaymentOrderListRequest` or an equivalent validated request model.
2. Preserve defaults for omitted `page`, `size` and `sort`.
3. Reject invalid field-level and cross-field list query values before repository query execution.
4. Keep page beyond last page as a successful empty response.
5. Add explicit JSON consumption to payment order create.
6. Add or adjust safe payment-owned handling for malformed JSON, unsupported media type and missing `Idempotency-Key`.
7. Preserve existing idempotent create/replay, ownership and response header behavior.

### 2. Frontend Consumer Hardening

1. Apply dashboard layout to payment create and detail pages.
2. Point create/detail back links to `/admin/merchants/{merchantId}/payments`.
3. Move detail API loading into the payment orders store.
4. Move create API behavior into the payment orders store.
5. Parse create/detail responses with `paymentOrderResponseSchema`.
6. Keep create form state local to the form and stop direct mutation of store API state.
7. Normalize dashboard alerts for `403`, detail `404`, backend unavailable, malformed backend response and create failures.

### 3. Boundary Review

1. Confirm no new endpoint, lifecycle action, status, PSP/Kafka/webhook/outbox behavior, role, migration or fake dashboard KPI was added.
2. Confirm no new automated test files or test-support abstractions were added.
3. Confirm validation messages describe the allowed contract without echoing raw rejected input values.
4. Confirm Lesson 13 remains a future testing/reliability layer.

## Existing Regression Verification

Run from `apps/backend` after backend implementation:

```bash
./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest,PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

Run from `apps/frontend` after frontend implementation:

```bash
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts payment-order-create.spec.ts payment-order-read.spec.ts payment-order-auth-deny.spec.ts
```

Run from the repository root to check plan artifacts:

```bash
git diff --check -- specs/008-payment-order-contract-consumer-hardening
```

## Expected End State

- Backend list validation is effective production behavior.
- Backend create protocol failures have stable payment-owned responses.
- Database and Keycloak remain unchanged by default.
- Frontend create/detail pages are dashboard-consistent consumers.
- Store owns create/detail API state and Zod parsing.
- Future Lesson 13 tests can target implemented behavior instead of inventing behavior.
