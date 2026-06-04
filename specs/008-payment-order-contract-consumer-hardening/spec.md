# Feature Specification: Payment Order Contract and Consumer Hardening

**Feature ID**: 008-payment-order-contract-consumer-hardening

**Date**: 2026-06-04

**Status**: Specified - clarified for `/speckit.plan`

**Scope Type**: Production/system behavior only. No implementation code and no new tests in this phase.

## 1. Feature Summary

Payment Order Contract and Consumer Hardening improves the existing merchant-scoped payment order API and Nuxt dashboard consumer without adding new payment business functionality.

The feature hardens:

- payment order list query validation,
- payment order create HTTP protocol handling,
- payment-owned error response consistency,
- database and transaction assumptions around existing tables and indexes,
- Keycloak/JWT role and merchant ownership boundaries,
- frontend dashboard create/detail route consistency,
- frontend store-owned API state and Zod response parsing,
- future Lesson 13 readiness for controller, reliability and consumer testing.

This feature does not add a new endpoint, payment lifecycle, PSP integration, Keycloak role, database migration, dashboard business capability, or automated test deliverable.

## 2. Business/System Goal

The current system already supports merchant-scoped payment order create, read, list and summary behavior. However, several production contracts are uneven or implicit:

- list request validation intent exists in `PaymentOrderListRequest`, but the controller currently constructs the record manually, so Bean Validation annotations are not guaranteed to run at the request boundary;
- create request body validation exists for domain fields, but malformed JSON, unsupported media type and missing `Idempotency-Key` behavior are not intentionally shaped as stable payment API responses;
- frontend create/detail pages are less integrated with the dashboard shell than the list/summary page;
- frontend detail/create API handling is scattered across page/component code instead of being centralized in the payment orders store.

The goal is to make these system contracts explicit and coherent so later quality engineering work can test stable production behavior instead of using tests to define the behavior.

## 3. Actors

| Actor | Goal | Boundary |
|---|---|---|
| Merchant payment creator | Create a payment order for the merchant they own, using `Idempotency-Key`. | Must have `merchant:payments:create` and matching `merchant_id` claim. |
| Merchant payment reader | List/read/summarize payment orders for their own merchant. | Must have `merchant:payments:read` and matching `merchant_id` claim. |
| Platform payment reader | Read/list/summarize payment orders through selected merchant paths. | Must have `platform:payments:read`. |
| Unauthenticated caller | Attempt access without a valid token. | Authentication remains enforced by Spring Security resource server. |
| Under-authorized caller | Attempt access with a valid token but wrong role or ownership. | Authorization remains enforced by backend, not frontend. |
| Dashboard user | Navigate payment orders list, create and detail pages with consistent UI feedback. | Uses Nuxt dashboard; sees backend-derived error states. |
| Future tester/learner | Practice testing controller/error/security/frontend consumer behavior on stable production contracts. | Lesson 13 and later testing work build on this feature. |

## 4. Current Problems

1. `PaymentOrderListRequest` has validation annotations, but the list controller receives individual `@RequestParam` values and manually constructs the record.
2. Invalid list query values may reach service/query construction instead of being rejected at the request boundary.
3. Cross-field list rules are not explicit production requirements: `fromDate <= toDate` and `minAmount <= maxAmount`.
4. Payment order create does not explicitly declare JSON-only consumption.
5. Malformed JSON and unsupported media type handling are not explicitly specified as payment-owned error responses.
6. Missing `Idempotency-Key` behavior needs stable production-level error semantics.
7. Payment create/detail frontend routes do not currently follow the same dashboard layout pattern as the list/summary route.
8. Payment create/detail back links point to `/admin/merchants/{merchantId}`, but that route is not part of the current system.
9. Payment detail page fetches directly and uses untyped `any` state instead of the payment store and Zod schema.
10. Create form directly mutates store API state (`loading`, `error`) instead of delegating API behavior to store actions.
11. Error UI for create/detail is less consistent than the list/summary dashboard state handling.

## 5. Functional Requirements

### Backend List

- **FR-LIST-001**: Default list params remain `page=0`, `size=20`, `sort=createdAt,desc` when omitted.
- **FR-LIST-002**: Invalid `page` or `size` values fail with a payment validation error before repository query execution.
- **FR-LIST-003**: Unsupported `status` or `currency` values fail with a payment validation error before repository query execution.
- **FR-LIST-004**: Invalid date format fails with a payment validation error before repository query execution.
- **FR-LIST-005**: `fromDate > toDate` fails with a payment validation error before repository query execution.
- **FR-LIST-006**: Negative `minAmount` or `maxAmount` fails with a payment validation error before repository query execution.
- **FR-LIST-007**: `minAmount > maxAmount` fails with a payment validation error before repository query execution.
- **FR-LIST-008**: Unsupported `sort` values fail with a payment validation error before repository query execution.
- **FR-LIST-009**: Page beyond last page returns a successful empty page response.
- **FR-LIST-010**: Existing authorization and merchant ownership policy remains unchanged.

### Backend Create

- **FR-CREATE-001**: Payment order create endpoint explicitly consumes JSON.
- **FR-CREATE-002**: Malformed JSON returns a stable payment error response.
- **FR-CREATE-003**: Unsupported media type returns a stable `415` response with a payment error code.
- **FR-CREATE-004**: Missing `Idempotency-Key` returns a stable validation response.
- **FR-CREATE-005**: Existing idempotent create/replay behavior remains unchanged.
- **FR-CREATE-006**: Existing `Location`, `ETag`, and `X-Correlation-ID` behavior remains unchanged for successful create/replay responses.

### Frontend

- **FR-FE-001**: Payment create page uses dashboard layout.
- **FR-FE-002**: Payment detail page uses dashboard layout.
- **FR-FE-003**: Create/detail back links return to payment orders list route: `/admin/merchants/{merchantId}/payments`.
- **FR-FE-004**: Payment store owns detail loading.
- **FR-FE-005**: Payment store owns create API call.
- **FR-FE-006**: Create form owns form state only and does not mutate store API state directly.
- **FR-FE-007**: Detail/create responses parse through Zod schemas.
- **FR-FE-008**: `403`, `404`, backend unavailable and create errors render consistent dashboard alert states.
- **FR-FE-009**: Payment detail `404` is rendered as a local dashboard resource-not-found state, not as a route-level missing page.

### Database And Security

- **FR-DB-001**: No new table, column or index migration is added by default.
- **FR-DB-002**: Existing constraints and indexes remain valid and unchanged unless a real production need is explicitly discovered and approved later.
- **FR-SEC-001**: No new Keycloak roles or realm config are added.
- **FR-SEC-002**: Backend remains the authorization source of truth.
- **FR-SEC-003**: Existing merchant/platform payment access policy is preserved.

### Clarified Decisions For Planning

- **CD-001**: Malformed JSON must use `400` with `error=malformed_json`.
- **CD-002**: Unsupported media type must use `415` with `error=unsupported_media_type`.
- **CD-003**: Cross-field list validation must live at the request/application boundary, not in controller branching or repository behavior.
- **CD-004**: Payment detail `404` must stay inside the dashboard as a local resource-not-found state.
- **CD-005**: Merchant Zod response schemas are deferred unless merchant consumer code is touched by the implementation.
- **CD-006**: Supported list sorting remains `createdAt,asc` and `createdAt,desc` only.
- **CD-007**: Validation messages should describe the allowed contract and avoid echoing raw rejected input values.

## 6. Backend Requirements

### Request Binding

- The list endpoint must treat `PaymentOrderListRequest` or an equivalent request model as the effective request validation boundary.
- The implementation must avoid a manual request construction path that silently bypasses Bean Validation annotations.
- Query defaults must remain readable and predictable.
- Cross-field query validation must be production behavior, not only a test expectation.

### List Query Validation

The following list query values must be rejected before repository query execution:

- `page < 0`,
- `size < 1`,
- `size > 100`,
- unsupported `status`,
- unsupported `currency`,
- invalid ISO date values,
- `fromDate > toDate`,
- negative `minAmount` or `maxAmount`,
- `minAmount > maxAmount`,
- unsupported sort field or direction.

The following list query behavior must remain valid:

- omitted optional filters,
- default pagination and sorting,
- page beyond last page returning an empty page,
- merchant-scoped query execution,
- platform-reader selected merchant path behavior.

### Create Protocol Handling

- The create endpoint must explicitly consume JSON.
- Malformed JSON must map to `400` with `error=malformed_json`.
- Unsupported media type must map to `415` with `error=unsupported_media_type`.
- Missing `Idempotency-Key` must map to a stable validation response.
- Existing successful create and idempotent replay behavior must not change.
- Existing create ownership rule must not change: merchant creator token requires matching `merchant_id` claim.

### Error Response Consistency

Payment-owned errors must use `PaymentErrorResponse` semantics:

- `error`,
- `message`,
- `correlationId`,
- optional validation `details` where Spring field validation produces them.

The implementation must not expose:

- raw access tokens,
- raw JWT claims beyond safe user-facing messages,
- full request body content,
- stack traces,
- internal class names in normal API responses.

## 7. Database Requirements

- No new migration is expected for this feature.
- Existing `payment_orders` table remains the payment order storage boundary.
- Existing FK from `payment_orders.merchant_id` to `merchants.merchant_id` remains unchanged.
- Existing `amount_minor`, `currency`, `status`, idempotency and status-history constraints remain unchanged.
- Existing V3 list indexes remain unchanged unless a real production problem is explicitly discovered and approved later.
- List validation for query ranges must happen before repository query execution, not through a database constraint.
- List query transaction boundary remains read-only.
- Create transaction behavior and idempotency persistence remain unchanged.
- No locking, isolation-level or optimistic concurrency changes are introduced in this feature.

## 8. Keycloak/Security Requirements

- No new Keycloak realm role is added.
- No Keycloak realm JSON is changed.
- `KeycloakRealmRoleConverter` remains unchanged unless a review finds a direct regression in existing behavior.
- `SecurityConfig` role policy remains materially unchanged:
  - `POST /api/merchants/*/payment-orders` requires `merchant:payments:create`,
  - list/read/summary payment order routes require `merchant:payments:read` or `platform:payments:read` as currently configured.
- Merchant reader access remains constrained by matching `merchant_id` claim.
- Platform payment reader access remains allowed for selected merchant paths.
- Frontend does not become the authorization source of truth.
- Frontend must handle backend `403` safely and must not keep stale protected data visible.
- Complete OAuth/OIDC application integration remains out of scope.

## 9. Frontend Requirements

### Dashboard Layout

- Payment create route must render inside the dashboard layout.
- Payment detail route must render inside the dashboard layout.
- Create/detail pages must follow the existing payment list page shell conventions where practical.
- No fake dashboard metrics or unsupported lifecycle actions are introduced.

### Navigation

- Create/detail pages must link back to `/admin/merchants/{merchantId}/payments`.
- Create/detail pages must not link to `/admin/merchants/{merchantId}` unless that route is implemented separately in a future feature.
- Link copy should preserve task context, e.g. `Back to payment orders`.

### Store Ownership

- `usePaymentOrdersStore` must own detail loading through `loadDetail(merchantId, paymentOrderId)` or equivalent.
- `usePaymentOrdersStore` must own create API behavior through `createOrder(merchantId, payload, idempotencyKey)` or equivalent.
- Create form must not directly mutate store-level API state such as `loading` or `error`.
- Create form may own local form field state, success copy and idempotency-key generation behavior.

### Zod And Error UI

- Detail response must parse through `paymentOrderResponseSchema`.
- Create response must parse through `paymentOrderResponseSchema` before updating `lastCreatedOrder`.
- Store error normalization should use `backendErrorSchema` where practical.
- `403` must show an insufficient-authority dashboard alert and clear stale payment data.
- `404` on detail must show a local neutral dashboard not-found state, not a route-level missing page.
- Backend unavailable behavior must use dashboard-consistent alert presentation.
- Create failure must use dashboard-consistent alert presentation.

## 10. Lesson 13 Readiness Requirements

This feature prepares stable production behavior for later Lesson 13 testing/reliability work. Lesson 13 work is not implemented in this feature.

The implementation should leave the system ready for future work on:

- focused controller-level testing over stable request binding and error handlers,
- Spring MVC protocol behavior review,
- authorization and ownership reasoning over unchanged roles,
- failure classification between validation, media type, authorization and backend unavailable states,
- frontend consumer behavior testing over store-owned API state,
- Modulith verification over unchanged module boundaries,
- database risk analysis over unchanged schema and indexes.

This feature must not introduce Lesson 13 topics prematurely:

- no executable webhook or async behavior,
- no Awaitility-driven production behavior,
- no lifecycle status changes for locking examples,
- no `If-Match` / `412` behavior,
- no test-only production hooks,
- no performance thresholds.

## 11. Non-Goals

- No new tests as deliverables.
- No REST Assured test classes.
- No Playwright specs.
- No test-support clients/builders/specs.
- No `POST /payments`.
- No payment lifecycle actions: authorize/capture/cancel/refund.
- No new statuses: `AUTHORIZED`, `CAPTURED`, `FAILED`, `REFUNDED`.
- No PSP integration or PSP mock.
- No Kafka, webhooks, outbox, events or async processing.
- No complete OAuth/OIDC application integration.
- No new Keycloak roles or realm changes.
- No new DB migration unless a real production need is explicitly discovered and approved later.
- No fake dashboard analytics/KPIs.
- No OpenAPI/Pact/WireMock/JSON Schema automation.
- No performance/load testing thresholds.
- No frontend role/permission model that replaces backend authorization.
- No merchant detail page as part of this feature.

## 12. Acceptance Criteria

1. Invalid `size=0` is rejected before repository query execution and returns a payment validation error.
2. Invalid `page=-1` is rejected before repository query execution and returns a payment validation error.
3. Unsupported `currency=GBP` is rejected before repository query execution and returns a payment validation error.
4. Unsupported `status` values are rejected before repository query execution and return a payment validation error.
5. Invalid date format is rejected before repository query execution and returns a payment validation error.
6. `fromDate > toDate` is rejected before repository query execution and returns a payment validation error.
7. Negative amount filters are rejected before repository query execution and return a payment validation error.
8. `minAmount > maxAmount` is rejected before repository query execution and returns a payment validation error.
9. Unsupported sort values are rejected before repository query execution and return a payment validation error.
10. Page beyond last page still returns a successful empty page response.
11. Payment order create explicitly consumes JSON.
12. Malformed JSON create requests return `400` with `error=malformed_json`.
13. Unsupported media type create requests return `415` with `error=unsupported_media_type`.
14. Missing `Idempotency-Key` create requests return a stable validation response.
15. Existing successful create returns still preserve `Location`, `ETag`, and `X-Correlation-ID` behavior.
16. Existing idempotent replay behavior remains unchanged.
17. Existing merchant/platform authorization policy remains unchanged.
18. No Keycloak realm role or realm JSON change is introduced.
19. No database migration is introduced by default.
20. Payment create page renders within the dashboard layout.
21. Payment detail page renders within the dashboard layout.
22. Payment create/detail pages navigate back to `/admin/merchants/{merchantId}/payments`.
23. Payment detail data is loaded through the payment orders store and parsed with Zod.
24. Payment create API behavior is owned by the payment orders store and parsed with Zod.
25. Create form no longer directly mutates store API state.
26. `403`, `404`, backend unavailable and create failure states use dashboard-consistent alerts.
27. No new automated test files are added as part of this feature.
28. No new payment lifecycle/status/PSP/Kafka/webhook behavior is added.

## 13. Edge Cases

| Edge case | Expected behavior |
|---|---|
| `size=0` | Validation error. |
| `size=101` | Validation error. |
| `page=-1` | Validation error. |
| `currency=GBP` | Validation error. |
| `status=AUTHORIZED` | Validation error; do not add status. |
| `fromDate=not-a-date` | Validation error. |
| `fromDate=2026-06-10&toDate=2026-06-01` | Validation error. |
| `minAmount=-1` | Validation error. |
| `maxAmount=-1` | Validation error. |
| `minAmount=1000&maxAmount=100` | Validation error. |
| `sort=amountMinor,desc` | Validation error unless later explicitly added as supported sort. |
| page beyond last page | Successful empty page. |
| malformed JSON body on create | `400` with `error=malformed_json`. |
| `Content-Type: text/plain` on create | `415` with `error=unsupported_media_type`. |
| missing `Idempotency-Key` | Stable validation response. |
| merchant reader requests another merchant list | Existing backend forbidden behavior remains. |
| detail route receives `403` | Permission alert, no stale payment data. |
| detail route receives `404` | Neutral not-found state. |
| frontend receives backend unavailable error | Dashboard-consistent unavailable alert. |
| frontend receives malformed backend response | Safe error state, no partial stale data rendering. |

## 14. Assumptions

- Existing payment order foundation remains in Phase 0 scope.
- Existing status model remains foundation-only with `CREATED`.
- Existing `payment_orders` constraints and list indexes are sufficient.
- Existing Spring Security resource server configuration remains the security boundary.
- Existing Nuxt server proxy continues to forward backend access tokens server-side.
- Frontend user/session handling remains unchanged.
- Existing regression tests may be run for verification, but no new tests are created by this feature.
- Error response consistency applies to payment-owned controller advice paths; global Spring Security authentication failures may remain controlled by security infrastructure unless explicitly handled elsewhere.

## 15. Open Questions / Clarifications

All planning-critical questions are resolved for `/speckit.plan`:

1. Malformed JSON uses `400` with `error=malformed_json`.
2. Unsupported media type uses `415` with `error=unsupported_media_type`.
3. Cross-field list validation lives at the request/application boundary. Field-level constraints stay on the request model; range relationships are enforced before repository query execution.
4. Frontend detail `404` uses a local dashboard alert/state, because the route exists and only the backend resource is missing.
5. Merchant response Zod schemas are deferred unless merchant consumer code is touched by the implementation.
6. `sort=createdAt,asc|desc` remains the only supported sort contract for this feature.
7. Validation messages describe the allowed contract and avoid echoing raw rejected input values in public API responses.

No unresolved clarification blocks `/speckit.plan`.

## 16. Definition of Done

This feature is done when:

- `spec.md` exists and captures backend, database, security, frontend and Lesson 13 readiness requirements.
- `/speckit.plan` can derive implementation-only tasks from the specification.
- Backend list request validation is specified as production behavior.
- Backend create protocol hardening is specified as production behavior.
- Database and Keycloak no-change decisions are explicit.
- Frontend dashboard/store/Zod/error behavior is specified.
- Non-goals prevent accidental tests, lifecycle behavior, PSP, Kafka, webhooks, new roles, new statuses and fake dashboard scope.
- No implementation code is changed during `/speckit.specify`.
- No new automated tests are written during `/speckit.specify`.
