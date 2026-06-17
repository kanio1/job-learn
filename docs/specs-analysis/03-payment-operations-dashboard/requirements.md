# Requirements Document

## Introduction

The Payment Operations Dashboard is a learning-oriented Nuxt 4 frontend that visually and functionally reflects the business capabilities already exposed by the Payment Quality Engineering Lab backend REST API. Its purpose is twofold: give Payment Operators a clean, business-readable view of merchants and payment orders, and give Developers and QA Automation Engineers an honest, transparent window into the underlying HTTP exchange (request method, path, headers, response status, response headers, and `application/problem+json` bodies).

Unlike a production dashboard, this UI must NOT hide HTTP learning details. The dashboard deliberately surfaces protocol concerns (ETag, If-Match, Idempotency-Key, Vary, Cache-Control, X-Correlation-ID, problem+json) so the screens double as a teaching surface for REST/API testing, manual endpoint exploration, and future Playwright UI tests.

This is a BROWNFIELD enhancement of the existing frontend at `apps/frontend`. The existing layout (`dashboard.vue`), pages (`index`, `login`, `admin/merchants`, `admin/merchants/[merchantId]/payments/*`), components (`merchant/*`, `payment/*`), schemas, Pinia stores, and server proxy routes (including lifecycle and history proxies and header forwarding in `server/utils/backendApi.ts`) MUST be preserved and improved/extended rather than rewritten or duplicated. Where a component, page, or proxy route already exists, requirements call for improving the existing artifact.

The dashboard reflects only what the backend actually exposes. It introduces no fabricated business metrics, no top-level `POST /payments` API, and no integrations beyond the discovered controller surface.

## Glossary

- **Dashboard**: The Nuxt 4 frontend application at `apps/frontend` that consumes the backend REST API.
- **Backend_API**: The Payment Quality Lab Spring Boot REST API exposed under `/api/*`.
- **Server_Proxy**: The Nuxt server (`server/api/**`) routes that forward authenticated requests to the Backend_API and forward selected response headers back to the browser via `server/utils/backendApi.ts`.
- **Merchant**: A registered payment platform tenant with `merchantReference`, `displayName`, and a lifecycle status of ACTIVE, PENDING, or SUSPENDED.
- **Payment_Order**: A merchant-scoped order with `amountMinor`, `currency`, `clientOrderReference`, `status`, and lifecycle/amount fields.
- **Payment_Status**: One of CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED.
- **Lifecycle_Action**: One of authorize, capture, cancel, refund — a state-changing POST operation on a Payment_Order.
- **ETag**: The entity version identifier returned by the Backend_API on Payment_Order reads and lifecycle responses.
- **If_Match**: The request header carrying the latest ETag to enforce optimistic concurrency on writes.
- **Idempotency_Key**: The request header that de-duplicates create and lifecycle operations.
- **Problem_Response**: An `application/problem+json` error body returned by the Backend_API payment endpoints.
- **Correlation_ID**: The `X-Correlation-ID` header value used to trace a request across the system.
- **Http_Learning_Detail**: The displayed request method, request path, request headers, response status, response headers, and response body used for teaching the HTTP exchange.
- **Api_Debug_Panel**: A reusable UI surface that renders Http_Learning_Detail for the most recent relevant request.
- **Error_Lab**: A dedicated page where the user intentionally triggers supported HTTP error responses to observe status codes, headers, and problem bodies.
- **Platform_Reader**: A user whose token carries `platform:payments:read` authority and can read payment orders across merchants.
- **Auth_Session**: The `nuxt-auth-utils` server session that holds the OIDC access token; the token is never exposed to the browser in plaintext.
- **Test_Id**: A stable `data-testid` attribute used by Playwright locators.
- **Loading_State**: A Nuxt UI skeleton or spinner shown while a request is in flight.
- **Empty_State**: A meaningful message plus a next action shown when a collection has no items.
- **Error_State**: A UI surface that renders a failed request using the Problem_Response without leaking tokens or secrets.

## Personas

- **Payment_Operator**: Reviews merchants and payment orders, performs lifecycle actions, and needs clear business status and confirmation flows. Cares about correctness and clarity over protocol internals, but benefits from visible outcomes.
- **QA_Automation_Engineer**: Writes Playwright UI tests and validates API contracts. Needs stable Test_Id locators, deterministic states, and visible HTTP headers/status to assert against.
- **Developer_Learning_REST**: Learns REST/HTTP by exploring endpoints manually. Needs the request/response details, ETag/If-Match/Idempotency-Key mechanics, and problem+json bodies made explicit and readable.

## Requirements

### Requirement 1: Dashboard Overview

**User Story:** As a Payment_Operator, I want an overview landing page summarizing merchants, payment orders, and lifecycle status, so that I can understand current platform state at a glance and navigate to detailed areas.

#### Acceptance Criteria

1. WHEN the Dashboard Overview page loads, THE Dashboard SHALL display a merchant-count card, a payment-order-count card, and one count card per Payment_Status value, populated only from values returned by the Backend_API without recomputing counts on the client.
2. WHEN the Dashboard Overview page loads, THE Dashboard SHALL display a recent payment orders section containing at most 10 payment orders ordered by creation time descending, populated from the Backend_API payment order list endpoint.
3. THE Dashboard SHALL display a navigation control on the Dashboard Overview page that navigates to the Error_Lab when activated.
4. WHILE a summary section or the recent-orders section is awaiting its Backend_API response, THE Dashboard SHALL display a Loading_State for that section and SHALL remove the Loading_State within 1 second of receiving the response.
5. IF the recent-orders list response contains zero payment orders for the requested merchant scope, THEN THE Dashboard SHALL display an Empty_State with an action to create a payment order.
6. IF a summary or recent-orders request returns a non-success response or does not complete within 10 seconds, THEN THE Dashboard SHALL display an Error_State rendered from the Problem_Response, SHALL provide a retry control, and SHALL exclude the access token from displayed and logged content.
7. THE Dashboard SHALL NOT display any business metric that is not derived from a Backend_API response.

### Requirement 2: Merchant Management

**User Story:** As a Payment_Operator, I want to list, create, view, and activate merchants, so that I can manage the merchant registry that owns payment orders.

#### Acceptance Criteria

1. WHEN the merchants page loads, THE Dashboard SHALL send `GET /api/merchants` via the Server_Proxy and SHALL display the returned merchant list within 3 seconds of receiving a successful response.
2. WHILE the `GET /api/merchants` request is in progress, THE Dashboard SHALL display a loading indicator and SHALL replace it with either the merchant list or an Error_State when the request completes.
3. IF the `GET /api/merchants` response contains zero merchants, THEN THE Dashboard SHALL display an empty-state message indicating that no merchants exist instead of an empty table.
4. WHEN the user submits the create merchant form with a `merchantReference` of 1 to 64 characters and a `displayName` of 1 to 140 characters that both satisfy the Zod schema, THE Dashboard SHALL send `POST /api/merchants` via the Server_Proxy and SHALL display the created merchant on success.
5. IF the create merchant form contains input that fails the Zod schema, THEN THE Dashboard SHALL display a field-level validation message for each invalid field and SHALL NOT send the `POST /api/merchants` request.
6. WHEN the user activates a merchant, THE Dashboard SHALL send `POST /api/merchants/{id}/activate` via the Server_Proxy and SHALL display the merchant status returned in the success response.
7. THE Dashboard SHALL display each merchant status using a status badge that distinguishes status by a text label and not by color alone.
8. WHEN the user opens a merchant detail view, THE Dashboard SHALL send `GET /api/merchants/{id}` via the Server_Proxy and SHALL display the merchant business fields from the success response.
9. IF a merchant request returns a failure response, THEN THE Dashboard SHALL display an Error_State rendered from the backend error response that identifies the failed operation, and SHALL retain any user-entered form input.
10. THE Dashboard SHALL implement merchant functionality by modifying the existing `CreateMerchantForm`, `MerchantStatusBadge`, and `MerchantTable` components and SHALL NOT create duplicate components for the same purpose.

### Requirement 3: Payment Order Listing, Creation, and Filtering

**User Story:** As a Payment_Operator, I want to create payment orders and browse a filterable table, so that I can locate and manage orders for a merchant.

#### Acceptance Criteria

1. WHEN the payment orders page loads for a merchant, THE Dashboard SHALL send `GET /api/merchants/{merchantId}/payment-orders` via the Server_Proxy and SHALL display the returned payment order table within 3 seconds of receiving a successful response.
2. WHEN the user submits the create payment order form with an `amountMinor` integer from 1 to 999999999999, a 3-letter ISO 4217 `currency` code, and a non-empty `clientOrderReference` of at most 255 characters, THE Dashboard SHALL generate an Idempotency_Key and send `POST /api/merchants/{merchantId}/payment-orders` with the `Idempotency-Key` header via the Server_Proxy.
3. IF the create payment order form contains input that fails the Zod schema, THEN THE Dashboard SHALL display a field-level validation message for each invalid field and SHALL NOT send the request.
4. IF the create payment order request returns a failure response, THEN THE Dashboard SHALL display an Error_State, SHALL retain the user-entered form values, and SHALL reuse the same Idempotency_Key when the user resubmits without changing the form values.
5. THE Dashboard SHALL provide payment order filters limited to status, currency, fromDate, toDate, minAmount, maxAmount, and clientOrderReference, matching the filters supported by the Backend_API list endpoint.
6. WHEN the user applies one or more filters, THE Dashboard SHALL send the corresponding query parameters to the Backend_API list endpoint and SHALL replace the displayed rows with the filtered results on success.
7. THE Dashboard SHALL support pagination using the `page`, `size`, and `sort` parameters exposed by the Backend_API list endpoint, defaulting to `page` 0 and `size` 20, and SHALL NOT request a `size` greater than 100.
8. IF a payment order list request returns a failure response, THEN THE Dashboard SHALL display an Error_State with a retry control and SHALL NOT display a stale or partially populated table.
9. IF the payment order list is empty for the active filters, THEN THE Dashboard SHALL display an Empty_State indicating no matching orders and an action to clear filters or create an order.
10. THE Dashboard SHALL implement payment order listing and creation by modifying the existing `CreatePaymentOrderForm`, `PaymentOrderListTable`, and `PaymentOrderSummaryCards` components and SHALL NOT create duplicate components for the same purpose.

### Requirement 4: Payment Order Detail with HTTP Learning Surface

**User Story:** As a Developer_Learning_REST, I want the payment order detail page to show business fields alongside HTTP response headers and the raw response body, so that I can learn how the REST resource is represented over HTTP.

#### Acceptance Criteria

1. WHEN the payment order detail page loads, THE Dashboard SHALL display all Payment_Order business fields retrieved from `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` via the Server_Proxy, with each field labeled and showing an explicit empty indicator for any field whose value is null or absent.
2. WHEN the payment order detail response is received, THE Dashboard SHALL display an HTTP headers panel listing the ETag, Location, Cache-Control, Vary, and X-Correlation-ID headers, rendering each forwarded value when the Server_Proxy includes it and rendering an explicit "not present" indicator for each of those headers that is absent.
3. WHEN the payment order detail response is received, THE Dashboard SHALL display a raw preview of the response body as formatted JSON text that reflects the unmodified response body content.
4. IF the payment order detail request returns a Problem_Response, THEN THE Dashboard SHALL display the problem+json body fields in a problem details card, SHALL display the response status code, and SHALL NOT display the business fields panel.
5. IF the payment order detail request fails to complete within 10 seconds or returns no response body, THEN THE Dashboard SHALL exit the Loading_State and SHALL display an error indication stating that the detail could not be retrieved.
6. WHILE the payment order detail data is being fetched, THE Dashboard SHALL display a Loading_State and SHALL NOT display the business fields panel, HTTP headers panel, or raw JSON preview.
7. WHEN the history endpoint returns one or more lifecycle status entries for the payment order, THE Dashboard SHALL display the lifecycle status history within the detail page.
8. IF the history endpoint returns no lifecycle status entries for the payment order, THEN THE Dashboard SHALL display an explicit empty-history indicator within the detail page.
9. THE Dashboard SHALL extend the existing `PaymentOrderDetail` component to add the HTTP headers panel and raw JSON preview, and SHALL NOT introduce a duplicate detail component.

### Requirement 5: Payment Order Lifecycle Operations

**User Story:** As a Payment_Operator, I want to authorize, capture, cancel, and refund a payment order with visible concurrency and idempotency controls, so that I can drive lifecycle transitions safely and observe the protocol mechanics.

#### Acceptance Criteria

1. WHERE a Lifecycle_Action is available for the current Payment_Status, THE Dashboard SHALL display an enabled control for that Lifecycle_Action, and WHERE a Lifecycle_Action is not available for the current Payment_Status, THE Dashboard SHALL hide or disable the control for that Lifecycle_Action.
2. WHEN the user initiates a Lifecycle_Action, THE Dashboard SHALL provide an Idempotency_Key input pre-populated with a value that is unique per initiation, that remains editable, and that is limited to at most 255 characters.
3. WHEN the user initiates a Lifecycle_Action, THE Dashboard SHALL provide an If_Match input pre-populated with the latest ETag from the most recent Payment_Order read.
4. WHERE the Lifecycle_Action is capture or refund, THE Dashboard SHALL provide an optional `amountMinor` input accepting a positive integer in minor units from 1 to 999999999999 inclusive.
5. WHERE a Lifecycle_Action is invoked, THE Dashboard SHALL provide an optional `reason` input limited to at most 500 characters.
6. WHEN a Lifecycle_Action returns a success response from the Backend_API, THE Dashboard SHALL display the resulting Payment_Status and the new ETag returned in that response.
7. IF a Lifecycle_Action returns a Problem_Response, THEN THE Dashboard SHALL display the problem details card with the response status code and SHALL retain the user-entered Idempotency_Key and If_Match values.
8. WHEN the user invokes a destructive Lifecycle_Action such as cancel or refund, THE Dashboard SHALL display a confirmation modal and SHALL withhold the request until the user explicitly confirms.
9. IF the user cancels or dismisses the confirmation modal, THEN THE Dashboard SHALL NOT send the request and SHALL retain the entered Idempotency_Key, If_Match, `amountMinor`, and `reason` values.
10. IF the Idempotency_Key input is empty or exceeds 255 characters, THEN THE Dashboard SHALL block the request and SHALL display a validation message on the Idempotency_Key input.
11. IF the `amountMinor` input is present and is not a positive integer from 1 to 999999999999, THEN THE Dashboard SHALL block the request and SHALL display a validation message on the `amountMinor` input.
12. THE Dashboard SHALL send each Lifecycle_Action via the existing Server_Proxy route for that action.

### Requirement 6: Error Lab and HTTP Learning Panel

**User Story:** As a QA_Automation_Engineer, I want a dedicated Error Lab to intentionally trigger supported HTTP errors and inspect the full exchange, so that I can learn and verify the API error contract.

#### Acceptance Criteria

1. THE Error_Lab SHALL provide exactly one control for each of the following supported error responses, and no control for any error response outside this list: 400 malformed or invalid request, 401 unauthenticated, 403 forbidden, 404 not found, 406 not acceptable for `Accept: application/xml`, 415 unsupported Content-Type, 409 idempotency conflict, 412 stale If-Match, and 428 missing If-Match.
2. WHEN the user triggers an Error_Lab scenario, THE Dashboard SHALL display, within 2 seconds of the trigger, the request method, request path, and request headers used for the scenario.
3. WHEN the Error_Lab scenario returns a response, THE Dashboard SHALL display, within 2 seconds of receiving the response, the response status code and forwarded response headers, and the displayed response status code SHALL equal the HTTP status code of the triggered scenario (one of 400, 401, 403, 404, 406, 409, 412, 415, or 428).
4. WHEN an Error_Lab scenario returns a Problem_Response, THE Dashboard SHALL display the problem+json body in the problem details card within 2 seconds of receiving the response.
5. THE Error_Lab SHALL only trigger error scenarios that the Backend_API contract supports as documented in the problem contract.
6. WHEN the Dashboard displays any request header in the Error_Lab or Api_Debug_Panel, THE Dashboard SHALL replace the entire bearer token value with a fixed masked placeholder so that no character of the actual token value is rendered in plaintext.
7. IF an Error_Lab scenario cannot be completed because the Backend_API is unreachable or does not return a response within 10 seconds, THEN THE Dashboard SHALL display an error indication that the scenario did not complete and SHALL retain any request and response details already displayed for that scenario.

### Requirement 7: Lifecycle History and Audit Timeline

**User Story:** As a Payment_Operator, I want a status history timeline for a payment order, so that I can audit how and when the order changed state and by whom.

#### Acceptance Criteria

1. WHEN the user opens the history view for a payment order, THE Dashboard SHALL retrieve the status history from `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history` via the Server_Proxy within 5 seconds.
2. WHILE the status history retrieval is in progress, THE Dashboard SHALL display a loading indicator until the response is received or the 5-second timeout elapses.
3. IF the status history retrieval fails or does not complete within 5 seconds, THEN THE Dashboard SHALL display an error message indicating that the history could not be loaded and SHALL provide a retry control.
4. WHEN the status history is retrieved successfully, THE Dashboard SHALL display each history entry showing the status transition from previous Payment_Status to next Payment_Status, the action, the safe actor display value, and the timestamp.
5. WHERE a history entry includes a Correlation_ID, THE Dashboard SHALL display the Correlation_ID for that entry.
6. WHEN displaying history entries, THE Dashboard SHALL order entries by timestamp in ascending order from oldest to newest.
7. IF the history collection contains zero entries, THEN THE Dashboard SHALL display an Empty_State indicating no recorded transitions.
8. THE Dashboard SHALL NOT display internal actor subject fields that are marked as non-display in the response schema.

### Requirement 8: Reusable Component Library

**User Story:** As a Developer_Learning_REST, I want a consistent set of reusable UI components for status, HTTP details, and protocol inputs, so that the dashboard stays maintainable and the HTTP concepts are presented consistently.

#### Acceptance Criteria

1. THE Dashboard SHALL provide a `BusinessStatusBadge` component that renders Merchant and Payment_Status values such that each status remains distinguishable from the others when color is removed.
2. THE Dashboard SHALL provide an `HttpStatusBadge` component that renders an HTTP status code together with its leading-digit category (1xx informational, 2xx success, 3xx redirection, 4xx client error, 5xx server error).
3. THE Dashboard SHALL provide a `HeaderKeyValuePanel` component that renders response or request header key-value pairs.
4. IF a `HeaderKeyValuePanel` is rendered with zero headers, THEN THE Dashboard SHALL display an explicit empty indicator instead of an empty panel.
5. THE Dashboard SHALL provide a `ProblemDetailsCard` component that renders the standard `application/problem+json` members (type, title, status, detail, instance) with an explicit empty indicator for any absent member.
6. THE Dashboard SHALL provide a `RawJsonViewer` component that renders a JSON response body as indented, multi-line text preserving the original key ordering.
7. IF the `RawJsonViewer` receives content that is not parseable as JSON, THEN THE Dashboard SHALL display the raw content with an indication that it is not valid JSON.
8. THE Dashboard SHALL provide an `IdempotencyKeyInput` component that generates a unique Idempotency_Key value and allows the user to edit it.
9. THE Dashboard SHALL provide an `EtagDisplay` component that displays the most recent ETag value and an `IfMatchInput` component that edits the If_Match value.
10. THE Dashboard SHALL provide a `PaymentOrderLifecycleActions` component that renders one control per available Lifecycle_Action.
11. THE Dashboard SHALL provide `MerchantStatusCard`, `PaymentOrderSummaryCards`, `ApiDebugPanel`, `EmptyStateCard`, `LoadingState`, `ErrorState`, and `ConfirmActionModal` components.
12. WHERE a listed reusable component already exists in the codebase, THE Dashboard SHALL extend the existing component and SHALL NOT create a duplicate.
13. THE Dashboard SHALL build reusable components from Nuxt UI primitives such as UCard, UTable, UBadge, UButton, UForm, UFormField, UInput, USelect, UTextarea, UModal, USlideover, UTabs, UAlert, UToast, and USkeleton before introducing custom CSS.

### Requirement 9: Loading, Empty, and Error States

**User Story:** As a Payment_Operator, I want clear loading, empty, and error states across the dashboard, so that I always understand the current state and my next action.

#### Acceptance Criteria

1. WHILE any data-fetching view is awaiting a response, THE Dashboard SHALL display a Nuxt UI skeleton or spinner Loading_State.
2. IF a data-fetching request does not complete within 10 seconds, THEN THE Dashboard SHALL exit the Loading_State and SHALL display an Error_State with a retry control.
3. IF a collection view has no items, THEN THE Dashboard SHALL display an Empty_State that includes a description of the empty condition and a next action the user can take.
4. IF a request fails, THEN THE Dashboard SHALL display an Error_State using the `ProblemDetailsCard` for problem+json responses and a human-readable message describing the failure for other errors.
5. WHEN the Dashboard displays an Error_State, THE Dashboard SHALL exclude the bearer token and any other secret value from the displayed content.
6. WHEN a write operation succeeds or fails, THE Dashboard SHALL display a UToast notification describing the outcome and SHALL keep the notification dismissible.

### Requirement 10: Form Validation

**User Story:** As a Payment_Operator, I want forms validated before submission, so that I get immediate feedback and avoid invalid requests.

#### Acceptance Criteria

1. WHEN the user submits a form, THE Dashboard SHALL validate all input fields against the corresponding Zod schema and complete validation before sending any request to the Backend_API.
2. IF one or more form fields fail validation, THEN THE Dashboard SHALL display a field-level message for each failing field indicating the specific validation rule that was violated, and SHALL prevent submission while retaining all entered field values.
3. WHEN all form fields pass validation against the corresponding Zod schema, THE Dashboard SHALL send the request to the Backend_API.
4. THE Dashboard SHALL validate every Backend_API response against the corresponding Zod schema before rendering any parsed data from that response.
5. IF a Backend_API response fails schema validation, THEN THE Dashboard SHALL display an Error_State indicating the response was invalid, SHALL NOT render any unvalidated data, and SHALL retain the prior valid view state.

### Requirement 11: Security and Token Handling

**User Story:** As a QA_Automation_Engineer, I want tokens handled safely and roles shown only as safe labels, so that the learning UI never leaks credentials.

#### Acceptance Criteria

1. THE Dashboard SHALL retain the OIDC access token only within the Auth_Session server-side store and SHALL NOT render, log, or embed the token value in any browser-accessible location, including DOM content, HTML attributes, inline scripts, client-side state, or browser storage.
2. WHERE the Dashboard displays authorization context, THE Dashboard SHALL display only role names and human-readable labels and SHALL NOT display any portion of the raw access token value.
3. WHEN the Dashboard renders any panel that shows HTTP header information, THE Dashboard SHALL replace the entire Authorization header value with a fixed masking placeholder so that no character of the token value is visible.
4. WHILE a Platform_Reader is authenticated, THE Dashboard SHALL allow reading payment orders across all merchants consistent with the `platform:payments:read` authority.
5. IF an authenticated principal lacks the `platform:payments:read` authority, THEN THE Dashboard SHALL deny the cross-merchant payment order read and SHALL display an authorization-denied indication without exposing token or credential details.
6. WHEN the Dashboard initiates any Backend_API call, THE Dashboard SHALL route the call through the Server_Proxy and attach the access token server-side so that the token is never present in the browser-issued request.
7. IF the Server_Proxy cannot attach a valid access token to a Backend_API call, THEN THE Dashboard SHALL block the outbound Backend_API request and SHALL display an authentication-required indication without exposing token or credential details.

### Requirement 12: Playwright Testability

**User Story:** As a QA_Automation_Engineer, I want stable test identifiers on key interactive elements, so that I can write reliable Playwright UI tests.

#### Acceptance Criteria

1. WHEN the create merchant form is rendered, THE Dashboard SHALL expose a Test_Id attribute on the form whose value is unique within the rendered page.
2. WHEN the activate merchant button is rendered, THE Dashboard SHALL expose a Test_Id attribute on the button whose value is unique within the rendered page.
3. WHEN the create payment order form is rendered, THE Dashboard SHALL expose a Test_Id attribute on the form whose value is unique within the rendered page.
4. WHEN the payment order table is rendered, THE Dashboard SHALL expose a Test_Id attribute on the table whose value is unique within the rendered page.
5. WHEN the payment order detail page is rendered, THE Dashboard SHALL expose a Test_Id attribute on the detail page container whose value is unique within the rendered page.
6. WHEN each of the authorize, capture, cancel, and refund lifecycle buttons is rendered, THE Dashboard SHALL expose a distinct Test_Id attribute on that button, such that each of the four values is unique within the rendered page.
7. WHEN the problem details card is rendered, THE Dashboard SHALL expose a Test_Id attribute on the card whose value is unique within the rendered page.
8. WHEN the HTTP headers panel is rendered, THE Dashboard SHALL expose a Test_Id attribute on the panel whose value is unique within the rendered page.
9. THE Dashboard SHALL keep each Test_Id value identical across rebuilds, deployments, sessions, and changes that alter only styling, layout, or surrounding content, so that an existing Playwright locator matching that Test_Id continues to resolve.
10. WHEN a Playwright locator queries a Test_Id value for an element that is currently rendered, THE Dashboard SHALL ensure that exactly one element in the page matches that Test_Id.

## Non-Goals

- No top-level `POST /payments` API or any endpoint not present in the discovered controller surface.
- No real PSP integration or PSP failure modeling beyond the existing local mock boundary.
- No Kafka, webhooks, outbox, settlement, payout, reconciliation, KYC, card/PAN/PCI, or 3DS features.
- No full production OAuth/OIDC integration; local Keycloak remains for development and tests.
- No fabricated KPI or business dashboard metrics beyond what the Backend_API summary and list endpoints expose.
- No rewrite of the existing frontend; existing structure is preserved and extended.
- No introduction of `.kilocode/` as the project organization target.
- No flashy or high-motion visual treatment; restraint over spectacle.

## Constraints

- Maximize reuse of Nuxt UI Dashboard primitives: UDashboardGroup, UDashboardSidebar, UDashboardPanel, UDashboardNavbar, UDashboardToolbar, UCard, UTable, UBadge, UButton, UForm, UFormField, UInput, USelect, UTextarea, UModal, USlideover, UTabs, UAlert, UToast, USkeleton.
- Prefer Nuxt UI components over custom CSS; add custom CSS only where the component library cannot express the needed state or layout.
- Use strict TypeScript across components, composables, schemas, and stores.
- Use Zod for both form schemas and Backend_API response validation.
- Use Pinia only where shared cross-component state is genuinely needed; prefer composables for API calls.
- Preserve and expose the response headers already forwarded by `server/utils/backendApi.ts`.
- Preserve existing Spring Modulith-aligned backend contracts; the Dashboard consumes contracts and does not change them.
- Keep code beginner-readable while remaining architecturally clean.
- Reflect only Backend_API capabilities; do not invent unsupported behavior.

## UX Principles

- Improve hierarchy before decoration: page title, primary action, filters, table/card grouping, status badges, then destructive actions.
- Keep operational screens scannable and compact with deliberate whitespace (VISUAL_DENSITY 5-7).
- Keep motion restrained (MOTION_INTENSITY 1-3): prefer focus, hover, disclosure, and state transitions over spectacle.
- Make HTTP learning details first-class and readable, not hidden behind obscure affordances.
- Keep copy functional: labels, helper text, and error messages describe what the user can decide or do next.
- Preserve accessibility: labelled controls, visible focus, keyboard-friendly disclosures, sufficient contrast, and non-color-only status indication.
- Present protocol concepts (ETag, If-Match, Idempotency-Key, problem+json) consistently through the shared reusable components.

## Testability Requirements

- THE Dashboard SHALL expose stable `data-testid` attributes on all interactive elements listed in Requirement 12.
- THE Dashboard SHALL render deterministic Loading_State, Empty_State, and Error_State surfaces that Playwright can assert against.
- THE Dashboard SHALL keep the HTTP headers panel and problem details card present in the DOM with stable identifiers whenever the corresponding data is available, so tests can assert HTTP outcomes.
- THE Dashboard SHALL keep Backend_API calls flowing through the Server_Proxy so Playwright tests exercise the same path as runtime usage.
- THE Dashboard SHALL avoid color-only status signaling so that status assertions can rely on text labels.
