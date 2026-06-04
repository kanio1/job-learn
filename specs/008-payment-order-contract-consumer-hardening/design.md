# System Design: Payment Order Contract and Consumer Hardening

**Date**: 2026-06-04

**Scope type**: system implementation design, not lesson material

**Source**: vault learning items, current backend/frontend implementation, Phase 0 guardrails

**Status**: designed for implementation

## 1. Design Intent

This slice converts selected vault learning items into real system behavior only. It explicitly excludes writing new tests as part of this plan.

The goal is to make the existing PayU-like foundation more coherent and testable later by improving production/backend behavior and frontend consumer behavior now.

Target system surfaces:

- `POST /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- Nuxt dashboard payment list/create/detail routes

## 2. Scope Decision

Implementation only.

In scope:

- backend request validation behavior,
- backend HTTP error handling behavior,
- backend media type and malformed request handling,
- frontend dashboard route consistency,
- frontend store ownership for create/detail API calls,
- frontend Zod parsing and error-state normalization.

Out of scope:

- no new backend test classes,
- no new REST Assured tests,
- no new Playwright tests,
- no test-support clients/builders/specs,
- no test framework refactoring,
- no test data builders,
- no verification matrix implementation,
- no code written only for tests.

Existing tests may be run only as regression verification after implementation. They are not deliverables of this plan.

## 3. Vault Items Converted To System Implementation

| Vault item | System implementation target | Production value |
|---|---|---|
| Malformed JSON | Stable backend error handling for unreadable create request bodies | API clients receive predictable `400` responses. |
| Unsupported media type | Explicit create endpoint media type contract | API clients receive predictable `415` responses. |
| Missing required header | Stable handling for missing `Idempotency-Key` | Retry-safe create contract becomes clearer. |
| List query validation | Effective validation for status, currency, page, size, sort, dates and amount range | Invalid list requests fail before reaching query execution. |
| X-Correlation-ID | Preserve correlation header on successful payment list/create/read responses where owned by controller | API diagnostics remain consistent. |
| Frontend as API consumer | Dashboard shell, route consistency, store-owned create/detail, Zod parsing | UI consumes the existing API coherently without fake business behavior. |

## 4. Explicit Non-Goals

Do not implement these in this slice:

- No `POST /payments`.
- No payment lifecycle actions: authorize, capture, cancel, refund.
- No new payment statuses such as `AUTHORIZED`, `CAPTURED`, `FAILED`, `REFUNDED`.
- No PSP integration or PSP mock flow.
- No Kafka, webhooks, outbox, event pipeline or async processing.
- No complete OAuth/OIDC application integration.
- No complete business dashboard or fake analytics KPIs.
- No Pact/WireMock/OpenAPI/JSON Schema automation.
- No performance/load thresholds.
- No database index migration unless implementation exposes a real production need.
- No new tests as implementation deliverables.

## 5. Capability Discovery Brief

### Working name

Payment Order Contract and Consumer Hardening

### Business problem

The existing foundation has useful payment order behavior, but parts of the production contract are uneven:

- create handles domain validation and idempotency, but malformed JSON/media type/missing header behavior is not intentionally shaped in production code;
- list query validation intent exists in `PaymentOrderListRequest`, but the controller manually constructs the record, so Bean Validation annotations are not effectively applied;
- frontend create/detail screens consume the API less consistently than the list/summary panel;
- create/detail navigation points at a merchant detail route that does not exist.

### Desired outcome

The system behaves coherently before test expansion:

- invalid query params return stable validation errors;
- invalid create protocol inputs return stable HTTP errors;
- create endpoint accepts JSON intentionally;
- frontend create/detail routes are part of the same dashboard experience;
- frontend API calls and parsing live in the Pinia store rather than being scattered in pages/components.

## 6. Backend Implementation Design

### Slice A: Effective list query validation

Files likely affected:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`

Design decision:

`PaymentOrderListRequest` should become the effective query contract. The controller should not manually construct it from raw parameters in a way that bypasses validation.

Recommended controller shape:

```java
@GetMapping
public ResponseEntity<PaymentOrderListResponse> listPaymentOrders(
        @PathVariable UUID merchantId,
        @Valid @ModelAttribute PaymentOrderListRequest request,
        Authentication authentication,
        @AuthenticationPrincipal Jwt jwt) {
    // preserve current authorization policy
    // service applies defaults for null page/size/sort
}
```

Production behavior to implement:

| Case | Production behavior |
|---|---|
| no query params | default `page=0`, `size=20`, `sort=createdAt,desc` |
| `page < 0` | validation error |
| `size < 1` | validation error |
| `size > 100` | validation error |
| unsupported status | validation error |
| unsupported currency | validation error |
| invalid date format | validation error |
| `fromDate > toDate` | validation error |
| negative amount filter | validation error |
| `minAmount > maxAmount` | validation error |
| unsupported sort field/direction | validation error |
| page beyond last page | successful empty page |

Implementation notes:

- Keep `PaymentOrderListService` read-only transactional.
- Keep defaulting in service or in request normalization.
- Add cross-field validation for date range and amount range.
- Keep sort allowlist intentionally narrow unless product scope expands.
- Keep list authorization semantics unchanged: platform payment reader can list selected merchant path; merchant reader must match `merchant_id`.

### Slice B: POST create HTTP protocol hardening

Files likely affected:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`

Recommended production changes:

- Add `consumes = MediaType.APPLICATION_JSON_VALUE` to `@PostMapping` for payment order create.
- Add explicit exception handling where needed for Spring MVC protocol failures owned by the payment API.

Recommended handler behavior:

| Exception / condition | Production response |
|---|---|
| `HttpMessageNotReadableException` | `400`, stable `PaymentErrorResponse`, preferably `error=malformed_json` |
| `HttpMediaTypeNotSupportedException` | `415`, stable `PaymentErrorResponse`, preferably `error=unsupported_media_type` |
| `MissingRequestHeaderException` for `Idempotency-Key` | `400`, stable `PaymentErrorResponse`, `error=validation` |
| `MethodArgumentNotValidException` | existing `400 validation` with details |
| domain validation exception | existing `400 validation` |

Implementation notes:

- Preserve current `201 Created`, replay `200 OK`, `Location`, `ETag` and `X-Correlation-ID` behavior.
- Do not change idempotency semantics.
- Do not add lifecycle behavior.
- Keep all changes inside payment web/application boundaries unless Spring shared config absolutely requires otherwise.

### Slice C: Backend response consistency

Files likely affected:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentErrorResponse.java`

Production behavior:

- Payment-owned errors should use `PaymentErrorResponse` consistently.
- Error bodies should include `error`, `message` and `correlationId`.
- Validation details should remain available where field validation produces them.
- Avoid leaking token values, raw request bodies or stack traces.

## 7. Frontend Implementation Design

### Slice D: Dashboard shell consistency

Files likely affected:

- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`

Production behavior:

- Both create and detail pages use `definePageMeta({ layout: 'dashboard' })`.
- Both pages use the same Nuxt UI dashboard shell patterns as the payment list page.
- Create/detail pages should not render as plain standalone pages inside the authenticated admin area.

### Slice E: Route and back-link consistency

Files likely affected:

- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`
- `apps/frontend/app/components/payment/PaymentOrderListTable.vue` if link copy needs alignment

Production behavior:

- Back links from create/detail go to `/admin/merchants/{merchantId}/payments`.
- Do not link to `/admin/merchants/{merchantId}` unless a real merchant detail page exists.
- Link labels should say `Back to payment orders` or equivalent task-context copy.

### Slice F: Store-owned create and detail API calls

Files likely affected:

- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`

Production behavior:

- Add `loadDetail(merchantId, paymentOrderId)` to `usePaymentOrdersStore`.
- Add `createOrder(merchantId, form, idempotencyKey)` or equivalent to `usePaymentOrdersStore`.
- Detail page uses `store.currentOrder`, `store.loading`, `store.error` and `store.insufficientAuthority`.
- Create form owns form-local validation state only; it does not directly mutate store loading/error fields.
- Created response parses with `paymentOrderResponseSchema` and updates `lastCreatedOrder`.

### Slice G: Frontend error-state normalization

Files likely affected:

- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/new.vue`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/[paymentOrderId].vue`
- `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue`

Production behavior:

- Use dashboard-consistent `UAlert` states for insufficient authority, backend unavailable, not found and creation failure.
- For `403`, do not keep stale payment data visible.
- For detail `404`, show a neutral not-found state.
- For backend unavailable, keep existing learner-friendly copy, but normalize the presentation.
- Do not add unsupported payment actions.

### Slice H: Zod consumer contract tightening

Files likely affected:

- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/schemas/merchant.schema.ts`

Production behavior:

- Detail response parsing uses `paymentOrderResponseSchema`.
- Payment store error normalization uses backend error schema where practical.
- Merchant response/list schemas may be added if touching merchant API consumer code, but do not widen scope just for symmetry.

## 8. Architecture Decisions

### ADR-008-1: Keep work inside existing modules

Decision: use existing `payment`, `merchant` and frontend dashboard boundaries.

Reason: the slice hardens existing behavior and does not introduce a new domain capability.

### ADR-008-2: Make list query validation real production behavior

Decision: bind and validate `PaymentOrderListRequest` as an actual request model.

Reason: annotations that are not executed create false confidence and weak contracts.

### ADR-008-3: Stabilize create protocol errors before adding payment behavior

Decision: malformed JSON, unsupported media type and missing idempotency header receive stable production responses.

Reason: API client behavior should be predictable before lifecycle, PSP or async flows exist.

### ADR-008-4: Frontend state belongs in the store

Decision: payment create/detail API calls belong in `usePaymentOrdersStore`.

Reason: API state, parsing and authorization/error behavior should be centralized for consistency.

### ADR-008-5: No tests in this plan

Decision: the plan intentionally excludes writing new tests.

Reason: the requested scope is system implementation only. Existing tests may be used for regression checks, but test creation is not part of the deliverable.

## 9. Implementation Task Breakdown

### Backend task group 1: list query validation

1. Change list endpoint binding to use `@Valid @ModelAttribute PaymentOrderListRequest` or equivalent validated request binding.
2. Preserve existing authorization policy.
3. Implement effective defaulting for nullable `page`, `size` and `sort`.
4. Add cross-field validation for `fromDate <= toDate`.
5. Add cross-field validation for `minAmount <= maxAmount`.
6. Keep unsupported status/currency/sort as validation failures.
7. Ensure invalid date format maps to stable validation error response.

### Backend task group 2: create protocol errors

1. Add explicit JSON `consumes` to payment order create endpoint.
2. Add stable handler for unreadable JSON.
3. Add stable handler for unsupported media type.
4. Add stable handler for missing `Idempotency-Key`.
5. Preserve current successful create and idempotent replay behavior.

### Backend task group 3: error response consistency

1. Keep payment-owned errors in `PaymentErrorResponse` format.
2. Include `correlationId` consistently.
3. Keep validation details where Spring field validation produces them.
4. Avoid exposing internal exception details.

### Frontend task group 4: dashboard route consistency

1. Add dashboard layout to payment create page.
2. Add dashboard layout to payment detail page.
3. Align navbar titles/actions with payment list page patterns.
4. Fix back links to payment orders list route.

### Frontend task group 5: store-owned API behavior

1. Add `loadDetail` action to payment orders store.
2. Add `createOrder` action to payment orders store.
3. Move direct `$fetch` for detail out of the page.
4. Move store loading/error mutations out of create form.
5. Parse detail/create responses with existing Zod schemas.

### Frontend task group 6: UI error normalization

1. Normalize `403`, `404`, backend unavailable and create failure presentation.
2. Use dashboard-consistent alert components.
3. Clear stale payment data after forbidden/not-found failures.
4. Keep copy functional and scoped to existing behavior.

## 10. Regression Verification Commands

These commands are for checking existing behavior after system implementation. They are not instructions to write tests.

Backend:

```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest,PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

Frontend, only after frontend implementation:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts payment-order-create.spec.ts payment-order-read.spec.ts payment-order-auth-deny.spec.ts
```

## 11. Resolved Risks And Decisions

| Risk / decision area | Decision |
|---|---|
| Unsupported `currency=GBP` | Implement validation error because the request model already declares allowed currencies. |
| Unsupported `sort=amountMinor,desc` | Keep rejected until product scope explicitly adds sortable fields. |
| Malformed JSON error code | Use `400` with `error=malformed_json`. |
| Unsupported media type error code | Use `415` with `error=unsupported_media_type`. |
| Cross-field list validation location | Request/application boundary before repository query execution. |
| Missing merchant detail route | Do not add a merchant detail page in this slice; fix links to the existing payment orders route. |
| Database schema changes | No schema change unless implementation exposes a real production need. |
| New test creation | No. Test writing is explicitly excluded from this plan. |

## 12. Recommended First Implementation Cut

The first implementation cut should be backend-only and production-code-only:

1. Effective list query binding and validation.
2. Cross-field list validation.
3. Stable create protocol error handling.
4. Existing regression verification.

The second implementation cut should be frontend-only:

1. Dashboard shell for create/detail.
2. Correct back links.
3. Store-owned detail/create calls.
4. Zod parsing and normalized error states.
