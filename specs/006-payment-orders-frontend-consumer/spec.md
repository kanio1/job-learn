# Feature Specification: Payment Orders Frontend Consumer and Contract Alignment

**Feature Branch**: `006-payment-orders-frontend-consumer`

**Created**: 2026-05-31

**Status**: Draft

**Lesson**: 09

**Phase**: 2 - Payment Orders

**Input**: Lesson 09 light Spec Kit / lesson-extension prompt. Implement a typed Nuxt Dashboard consumer for existing payment order list and summary APIs without adding payment lifecycle behavior.

## Business Purpose *(mandatory)*

Lesson 09 closes the current system gap between the backend payment APIs and the Nuxt Dashboard frontend. Lessons 06-08 implemented and tested payment order create/read, list/filter, and aggregation summary on the backend, but the frontend only consumes create/read and stores payment response data as `any`.

This feature makes the dashboard a real, typed consumer of existing payment order contracts:

- merchant-scoped payment order list,
- merchant-scoped payment order summary,
- role-aware UI states for forbidden access,
- clear empty/loading/backend-unavailable states,
- Playwright coverage for UI behavior,
- REST Assured regression commands as backend API truth.

No payment lifecycle action, PSP integration, Kafka/webhook flow, settlement, refund, fake analytics dashboard, new status, new role, or database migration is introduced.

## Clarifications

### Session 2026-05-31

- Q: Is this a full backend feature spec? -> A: No. It is a light Spec Kit lesson-extension over existing backend APIs.
- Q: Does frontend compute summary from the list? -> A: No. The frontend consumes `GET /api/merchants/{merchantId}/payment-orders/summary`.
- Q: Does UI security replace backend security? -> A: No. Backend remains source of truth for roles, ownership, `401`, `403`, validation, and data scope.
- Q: Are new Keycloak roles required? -> A: No. Existing roles remain unchanged.
- Q: Are payment lifecycle buttons allowed? -> A: No. Do not add `authorize`, `capture`, `cancel`, `If-Match`, or `412` behavior.

## Actors *(mandatory)*

- **Merchant Payment Reader**: Authenticated user with `merchant:payments:read` and matching `merchant_id`. Can view payment summary and list for their merchant.
- **Platform Payment Reader**: Authenticated user with `platform:payments:read`. Can view selected merchant payment summary and list for support/investigation.
- **Denied Identity**: Authenticated user without payment read authority. Receives `403`; UI shows insufficient-authority state and no payment data.
- **Unauthenticated User**: Redirected by existing app auth middleware or receives `401` from server proxy/session handling.

## Scope *(mandatory)*

### In Scope

- Nuxt server proxy for `GET /api/merchants/{merchantId}/payment-orders`.
- Nuxt server proxy for `GET /api/merchants/{merchantId}/payment-orders/summary`.
- Query forwarding for list: `status`, `currency`, `fromDate`, `toDate`, `minAmount`, `maxAmount`, `clientOrderReference`, `page`, `size`, `sort`.
- Query forwarding for summary: `currency`, `status`, `fromDate`, `toDate`.
- Zod schemas and inferred TypeScript types for payment order response, payment list response, payment summary response, and backend error response.
- Typed Pinia payment store for `loading`, `error`, `summary`, `list`, `currentOrder`, `lastCreatedOrder`, and reset actions.
- Minimal Nuxt UI payments panel at `/admin/merchants/{merchantId}/payments`.
- Summary cards for total orders, total amount minor, currency breakdown, and status breakdown.
- Payment list table with order reference/id, amount/currency, status, dates, and detail link.
- UI states: loading, empty, insufficient authority (`403`), backend unavailable.
- Playwright tests for happy, empty, forbidden, and no-data-on-forbidden states.
- Backend regression commands for list/summary/security contracts and `PaymentModuleTest`.
- Vault evidence update after implementation.

### Out of Scope

- Payment lifecycle: `authorize`, `capture`, `cancel`.
- New payment statuses beyond `CREATED`.
- PSP integration, PSP mock, card data, 3DS, PCI.
- Kafka, webhooks, event broker.
- Refunds, settlement, reconciliation.
- RLS.
- `If-Match`, `ETag` changes, `412` optimistic concurrency.
- Pact/WireMock/OpenAPI generation.
- Complete business dashboard or fake analytics KPIs.
- Backend behavior changes unless strictly required to preserve existing contracts.
- New Keycloak roles/users.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Payment Summary and List (Priority: P1)

A payment reader opens the merchant payments panel and sees summary cards plus a list of payment orders based on existing backend APIs.

**Independent Test**: Mock the frontend API routes to return a summary and list payload, open `/admin/merchants/{merchantId}/payments`, and assert the summary totals, breakdown labels, list row reference, amount, status, and detail link are visible.

**Acceptance Scenarios**:

1. **Given** summary and list responses contain payment data, **When** a reader opens the payments panel, **Then** summary cards and list rows are rendered.
2. **Given** a merchant has no payment orders, **When** the reader opens the panel, **Then** zero summary totals and an empty-list state are shown.
3. **Given** list/summary API returns backend unavailable, **When** the panel loads, **Then** a recoverable backend-unavailable state is shown.

### User Story 2 - Handle Forbidden Access Safely (Priority: P2)

The UI must represent backend `403` without rendering payment data or implying that UI hiding is the security boundary.

**Independent Test**: Mock list or summary route as `403`, open the panel, assert insufficient-authority state is visible, and assert mocked payment references are not rendered.

**Acceptance Scenarios**:

1. **Given** summary/list returns `403`, **When** the panel loads, **Then** the UI shows an insufficient-authority alert.
2. **Given** a forbidden response body contains no authorized data, **When** the panel renders, **Then** no payment list rows or summary values from a happy-path fixture appear.

### Edge Cases

- Empty `content` with `totalElements=0` renders empty state.
- `byCurrency=[]` and `byStatus=[]` render no breakdown rows, not an error.
- `403` from either summary or list results in a single insufficient-authority state.
- `503` or backend-unavailable proxy error renders retry-safe error copy.
- Invalid filter errors remain backend-owned and surface as validation copy if filters are later exposed.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-401**: Frontend MUST provide a Nuxt server proxy for payment order list that forwards authenticated access tokens to the backend.
- **FR-402**: Frontend MUST provide a Nuxt server proxy for payment order summary that forwards authenticated access tokens to the backend.
- **FR-403**: List proxy MUST forward supported list query parameters without inventing new backend filters.
- **FR-404**: Summary proxy MUST forward supported summary query parameters without computing summary in the browser.
- **FR-405**: Frontend MUST define Zod schemas and inferred types for payment order, payment list, payment summary, and backend error response shapes.
- **FR-406**: Payment Pinia store MUST avoid `any` for `summary`, `list`, `currentOrder`, and `lastCreatedOrder`.
- **FR-407**: Payments panel MUST render summary totals and grouped rows from the summary endpoint response.
- **FR-408**: Payments panel MUST render list rows and pagination metadata from the list endpoint response.
- **FR-409**: Payments panel MUST render an explicit empty state when list content is empty.
- **FR-410**: Payments panel MUST render an insufficient-authority state for `403` and MUST NOT render payment data in that state.
- **FR-411**: Payments panel MUST render a backend-unavailable state for proxy/backend availability errors.
- **FR-412**: Playwright tests MUST cover happy, empty, forbidden, and no-payment-data-on-forbidden UI behavior.
- **FR-413**: Existing backend REST Assured list/summary/security tests MUST remain the backend API truth.

### Non-Functional Requirements

- **NFR-401**: No backend database migration is allowed.
- **NFR-402**: No new backend lifecycle behavior is allowed.
- **NFR-403**: UI must follow existing Nuxt UI Dashboard visual language.
- **NFR-404**: Test design must avoid shared mutable state and should prefer route-mocked UI state tests.

## Quality and Architecture Impact *(mandatory)*

### Frontend Architecture Impact

- Adds Nuxt server routes as BFF/proxy boundaries.
- Adds typed schemas at the frontend runtime boundary.
- Adds a dashboard panel that consumes existing backend contracts without duplicating backend business rules.

### Backend Impact

- No backend endpoint, role, database, or business behavior changes are planned.
- Backend verification remains `PaymentModuleTest` plus existing REST Assured suites.

### Security Impact

- Backend remains the security enforcement point.
- Frontend must not render data after `403`.
- No Authorization token logging or new token storage pattern is introduced.

## Success Criteria *(mandatory)*

- **SC-401**: Frontend typecheck passes.
- **SC-402**: Payment panel Playwright tests pass.
- **SC-403**: Backend list/summary/security regression tests pass.
- **SC-404**: `PaymentModuleTest` passes.
- **SC-405**: Vault evidence captures implemented files, test files, commands run, and deferred scope.

## Assumptions

- Existing backend list and summary APIs are correct and tested.
- Existing dashboard layout and auth middleware remain in place.
- Playwright UI tests can use route-mocked API responses rather than a full backend for UI state coverage.
