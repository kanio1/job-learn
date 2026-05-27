# Feature Specification: Payment Order Access, Idempotent Creation, And Minimal Create/Read Lifecycle Foundation

**Feature Branch**: `004-payment-order-create-read`

**Created**: 2026-05-27

**Status**: Draft

**Input**: User description: "Create a new Spec Kit feature from specs/003-payment-order-access-lifecycle/spec-input.md. Feature target: Payment Order Access, Idempotent Creation, And Minimal Create/Read Lifecycle Foundation."

## Business Purpose *(mandatory)*

This feature establishes the first payment-domain capability for the Payment Quality Engineering Lab: a merchant-scoped payment order that an authorized actor can create idempotently and read within their ownership boundary.

This phase comes after Merchant Registry because payment orders must belong to a real merchant ownership context. The platform must already recognize active merchants as valid business participants before payment activity begins.

This feature creates:

- The first real payment-domain resource in the product.
- Idempotent creation behavior that prevents duplicate payment orders across retries.
- A meaningful ownership and tenant-isolation boundary for payment data.
- Protocol-level HTTP contracts (`Location`, `ETag`, `X-Correlation-ID`, `Idempotency-Key`) that become executable test oracles.
- The foundation for a future lifecycle slice (authorize/capture/cancel) without implementing it now.
- Rich test-design material for idempotency, money precision, cross-tenant isolation, SQL constraints, and security matrix testing.

No payment processing, PSP integration, card data, settlement, reconciliation, refunds, webhooks, Kafka, or messaging is implemented.

## Clarifications

### Session 2026-05-27

- Q: What is the first implementation slice scope? → A: Payment order create and read only. Lifecycle actions (authorize, capture, cancel) are deferred to the next slice.
- Q: What HTTP status does cross-tenant single-resource read return? → A: Masked `404 not_found` to reduce resource enumeration.
- Q: What HTTP status does an authenticated caller missing the required role receive? → A: `403 forbidden`.
- Q: What status does the first successful idempotent create return? → A: `201 Created`.
- Q: What headers does a successful create response include? → A: `Location`, `ETag`, `X-Correlation-ID`, plus the response body.
- Q: What does a replay with the same `Idempotency-Key` and same request fingerprint return? → A: `200 OK` with the original payment order identity. No duplicate row is created.
- Q: What does a request with the same `Idempotency-Key` but a different request fingerprint return? → A: `409 idempotency_conflict`.
- Q: How is amount represented? → A: Minor units, positive, range `1..100_000_000`.
- Q: Which currencies are supported in the first slice? → A: `PLN`, `EUR`, `USD`.
- Q: Which payment roles are implemented in the first slice? → A: `merchant:payments:create` and `merchant:payments:read`.
- Q: Is `merchant:payments:operate` implemented now? → A: No. It is planned for the lifecycle slice and must not expand first-slice endpoint scope. It is added to the Keycloak realm and test JWT support now as a planned unused role so security matrix tests can reference it. No Keycloak changes will be needed for the lifecycle slice.
- Q: What access/ownership model is used? → A: The smallest useful merchant-scoped test slice, not a full Merchant Team Management product.
- Q: Can the payment module depend on `merchant.internal`? → A: No. It must use a merchant public API boundary for eligibility or ownership lookups.
- Q: Is initial status history with correlation ID required? → A: Yes, at least the creation status history record with correlation ID is required for the first slice.
- Q: Can a platform payment reader read merchant payment orders for support in the first slice? → A: Yes. The `platform:payments:read` role is included in the first slice. A platform payment reader can read any merchant's payment orders for support/investigation. A `platform.payment.reader` test identity with `platform:payments:read` authority is added to the Keycloak realm and test JWT support. Security matrix tests must cover cross-merchant read access for this role.

## Actors *(mandatory)*

- **Merchant Payment Creator**: Authenticated user with `merchant:payments:create` authority scoped to one merchant. Creates payment orders for their active merchant. Cannot read payment orders unless also granted read authority.
- **Merchant Payment Reader**: Authenticated user with `merchant:payments:read` authority scoped to one merchant. Reads payment orders belonging to their merchant. Cannot create or operate on payment orders.
- **Merchant Payment Operator**: Future actor preserved for the lifecycle slice. Not implemented in this phase.
- **Platform Payment Reader**: Authenticated user with `platform:payments:read` authority. Can read any merchant's payment orders for support and investigation purposes. Cannot create or operate on payment orders. Included in the first slice.
- **Unauthenticated User**: User without a valid session or token. May access only the public technical status capability. Must not access payment order behavior.
- **Denied Identity**: Authenticated user without any payment role. Receives `403` for all payment endpoints.
- **Cross-Tenant Actor**: Authenticated user with a valid payment role for merchant A who attempts to access merchant B's payment order. Receives masked `404` for single-resource reads.

## Scope *(mandatory)*

### In Scope

- Create a payment order for an active merchant with amount, currency, client order reference, `Idempotency-Key`, and `X-Correlation-ID`.
- Validate amount as minor units in range `1..100_000_000`.
- Validate currency as one of `PLN`, `EUR`, `USD`.
- Require `Idempotency-Key` header for create.
- Return `201 Created` with `Location`, `ETag`, `X-Correlation-ID`, and response body for first successful creation.
- Return `200 OK` replay with the original payment order identity for same `Idempotency-Key` and same request fingerprint.
- Return `409 idempotency_conflict` for same `Idempotency-Key` with different request fingerprint.
- Reject payment creation for non-active merchants with `409 merchant_not_payment_eligible`.
- Retrieve a payment order by ID within the caller's merchant scope.
- Return masked `404 not_found` for cross-tenant single-resource reads.
- Return `403 forbidden` for authenticated callers missing the required payment role.
- Return `401` for missing, invalid, or expired tokens.
- Store amount as minor units and currency as a constrained code.
- Persist payment order, idempotency record, and initial status history atomically.
- Append creation status history record with correlation ID.
- Protect all payment endpoints with authentication, role authorization, and merchant ownership.
- Return stable machine-readable error codes.
- Expose `ETag` on create and read responses as a planned compatibility point for the lifecycle slice.
- Preserve Spring Modulith boundaries; payment module depends only on merchant public API.
- Provide a minimal dashboard journey for payment order creation and detail view.
- Preserve parallel-safe test data conventions using unique payment references and idempotency keys.

### Out of Scope

- Payment order lifecycle actions: authorize, capture, cancel.
- `If-Match` conditional request behavior for lifecycle actions.
- `412 Precondition Failed` for stale ETag.
- `409 invalid_transition` for lifecycle state machine.
- Payment status transitions beyond initial `CREATED` status.
- PSP integration, PSP mock flows, real cards, PAN, CVV, PCI, 3DS.
- Kafka, webhooks, event broker, messaging.
- Refunds, settlement, reconciliation.
- GraphQL, gRPC.
- Complete merchant self-service portal.
- Complete admin platform.
- Client Credentials Flow.
- Full Merchant Team Management (user invite, role assignment, team administration).
- Payment order list/filter/search endpoint.
- Payment order mutation after creation (no update, no delete).
- Currency conversion or exchange rates.
- Country-specific payment rules.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Payment Order Idempotently (Priority: P1)

A merchant payment creator creates a payment order for their active merchant. The system records the intended payment exactly once, even if the client retries the request.

**Why this priority**: Idempotent creation is the core payment-domain behavior. It establishes the first real payment resource, prevents duplicate orders, and introduces protocol-level contracts (`Location`, `ETag`, `X-Correlation-ID`, `Idempotency-Key`) that become the primary test oracles for Lesson 6.

**Independent Test**: Can be fully tested by authenticating as a merchant payment creator, creating a payment order with a unique idempotency key, verifying the `201` response with headers and body, then replaying the same request and verifying `200 OK` with the same payment order identity.

**Acceptance Scenarios**:

1. **Given** an authenticated merchant payment creator with an active merchant, **When** they create a payment order with valid amount, currency, client order reference, `Idempotency-Key`, and `X-Correlation-ID`, **Then** the system returns `201 Created` with `Location` pointing to the new payment order, `ETag`, `X-Correlation-ID` echoed back, and a response body containing payment order ID, merchant ID, amount, currency, status `CREATED`, and timestamps.
2. **Given** a payment order was created with `Idempotency-Key` K and request fingerprint F, **When** the same caller sends the same request with key K and fingerprint F, **Then** the system returns `200 OK` with the same payment order identity and no duplicate row.
3. **Given** a payment order was created with `Idempotency-Key` K and request fingerprint F, **When** the same caller sends a request with key K but a different request fingerprint, **Then** the system returns `409 idempotency_conflict` with a stable error code.
4. **Given** a merchant exists in `DRAFT` or `SUSPENDED` status, **When** an authenticated merchant payment creator attempts to create a payment order for that merchant, **Then** the system returns `409 merchant_not_payment_eligible`.
5. **Given** an authenticated merchant payment creator submits an amount of `0`, negative, or above `100_000_000` minor units, **When** they attempt to create a payment order, **Then** the system returns `400 validation` with field-level details.
6. **Given** an authenticated merchant payment creator submits an unsupported currency code, **When** they attempt to create a payment order, **Then** the system returns `400 validation`.
7. **Given** an authenticated merchant payment creator submits a request without `Idempotency-Key`, **When** they attempt to create a payment order, **Then** the system returns `400 validation`.
8. **Given** a payment order is created, **When** the creation status history is inspected, **Then** an initial status history record exists with `to_status = CREATED`, the actor subject, and the correlation ID from the request.

---

### User Story 2 - Read Payment Order Within Merchant Scope (Priority: P2)

A merchant payment reader retrieves a payment order belonging to their merchant and sees its current status and protocol metadata.

**Why this priority**: Read access completes the minimal create/read slice and introduces ownership-scoped retrieval with cross-tenant isolation as a primary security oracle.

**Independent Test**: Can be tested by creating a payment order for merchant A, then reading it with merchant A's reader token and verifying `200 OK` with `ETag`, and attempting to read it with merchant B's reader token and verifying masked `404`.

**Acceptance Scenarios**:

1. **Given** a payment order exists for merchant A, **When** an authenticated merchant payment reader for merchant A retrieves it by ID, **Then** the system returns `200 OK` with `ETag`, `X-Correlation-ID`, and the payment order body.
2. **Given** a payment order exists for merchant A, **When** an authenticated merchant payment reader for merchant B attempts to retrieve it by ID, **Then** the system returns masked `404 not_found`.
3. **Given** a payment order ID does not exist, **When** an authenticated merchant payment reader retrieves it, **Then** the system returns `404 not_found`.
4. **Given** a payment order ID is malformed, **When** an authenticated merchant payment reader requests it, **Then** the system returns `400 validation`.
5. **Given** a payment order exists for merchant A, **When** an authenticated platform payment reader with `platform:payments:read` retrieves it by ID, **Then** the system returns `200 OK` with `ETag`, `X-Correlation-ID`, and the payment order body.

---

### User Story 3 - Enforce Payment Access Boundary (Priority: P3)

The platform protects payment order behavior so unauthenticated users, users without payment roles, and users without merchant ownership cannot create or read payment orders.

**Why this priority**: Payment data is sensitive business data. The first payment feature must establish a clear, testable security boundary separating authentication, role authorization, and merchant ownership.

**Independent Test**: Can be tested by attempting payment create and read actions without authentication, with insufficient authority, with valid authority for the wrong merchant, and with valid authority for the correct merchant.

**Acceptance Scenarios**:

1. **Given** a user is unauthenticated, **When** they attempt to create or read a payment order, **Then** the system returns `401` and no payment data is disclosed or changed.
2. **Given** an authenticated user has no payment role, **When** they attempt to create or read a payment order, **Then** the system returns `403 forbidden`.
3. **Given** an authenticated user has `merchant:payments:create` but not `merchant:payments:read`, **When** they attempt to read a payment order, **Then** the system returns `403 forbidden`.
4. **Given** an authenticated user has `merchant:payments:read` but not `merchant:payments:create`, **When** they attempt to create a payment order, **Then** the system returns `403 forbidden`.
5. **Given** an authenticated user has only `merchant:payments:operate` (planned unused role), **When** they attempt to create or read a payment order, **Then** the system returns `403 forbidden` because no operate endpoints exist and the role alone does not grant create or read access.
6. **Given** any user accesses the technical status capability, **When** the platform is running, **Then** the status capability remains publicly available and does not expose payment data.

---

### User Story 4 - Dashboard Payment Order Journey (Priority: P4)

An authenticated merchant user creates a payment order and views its detail through the dashboard.

**Why this priority**: The dashboard provides a real consumer of the backend API contract and validates that the UI correctly handles response headers, error states, and role-aware visibility.

**Independent Test**: Can be tested by logging in as a merchant payment creator, navigating to the merchant detail page, creating a payment order through the form, and verifying the detail page shows the order with correct status and metadata.

**Acceptance Scenarios**:

1. **Given** an authenticated merchant payment creator is on the merchant detail page, **When** they create a payment order with valid data, **Then** the payment order appears in the detail view with status `CREATED`.
2. **Given** an authenticated merchant payment creator submits invalid data, **When** they attempt to create a payment order, **Then** field-level validation feedback is shown.
3. **Given** an authenticated merchant payment reader views a payment order detail, **When** the page loads, **Then** the order status, amount, currency, and creation time are visible.
4. **Given** an authenticated user without payment create authority views the merchant detail page, **When** the page renders, **Then** the create payment order action is not visible, but the backend still enforces `403` for direct API attempts.

---

### Edge Cases

- Amount is exactly `1` (minimum valid) or exactly `100_000_000` (maximum valid).
- Amount is `0`, negative, or `100_000_001`.
- Currency is a valid 3-letter code but not in the supported set (e.g., `GBP`, `JPY`).
- Currency is malformed (e.g., `PL`, `PLNN`, `pln`, `123`).
- `Idempotency-Key` is missing, blank, or excessively long.
- Two create requests with the same `Idempotency-Key` arrive nearly simultaneously.
- Client order reference is missing, blank, or exceeds maximum length.
- Payment order ID is malformed or does not exist.
- Merchant ID in the path does not match the caller's merchant scope.
- Merchant exists but is in `DRAFT` or `SUSPENDED` status.
- `X-Correlation-ID` is missing (system generates one), present (system echoes it), or excessively long.
- Token is expired, has invalid signature, or has wrong issuer.
- Tests run in parallel and generate payment orders with unique namespaced references and idempotency keys.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow an authenticated caller with `merchant:payments:create` for an active merchant to create a payment order using amount, currency, client order reference, and `Idempotency-Key`.
- **FR-002**: System MUST assign each created payment order an internal `paymentOrderId` distinct from the client order reference.
- **FR-003**: System MUST assign newly created payment orders the initial status `CREATED`.
- **FR-004**: System MUST reject payment creation for non-active merchants with `409 merchant_not_payment_eligible`.
- **FR-005**: System MUST require `Idempotency-Key` header for create and reject missing or blank keys with `400 validation`.
- **FR-006**: System MUST return `201 Created` with `Location`, `ETag`, `X-Correlation-ID`, and response body for first successful creation.
- **FR-007**: System MUST return `200 OK` with the original payment order identity for same `Idempotency-Key` and same request fingerprint (idempotent replay).
- **FR-008**: System MUST return `409 idempotency_conflict` for same `Idempotency-Key` with different request fingerprint.
- **FR-009**: System MUST represent amount as minor units with valid range `1..100_000_000` and reject out-of-range values with `400 validation`.
- **FR-010**: System MUST allow only `PLN`, `EUR`, and `USD` as currency codes and reject unsupported codes with `400 validation`.
- **FR-011**: System MUST store `createdAt` and `updatedAt` timestamps for each payment order.
- **FR-012**: System MUST allow an authenticated caller with `merchant:payments:read` to retrieve a payment order by `paymentOrderId` within their merchant scope.
- **FR-013**: System MUST return masked `404 not_found` for cross-tenant single-resource reads without disclosing resource existence.
- **FR-014**: System MUST return `404 not_found` for unknown payment order IDs within the caller's merchant scope.
- **FR-015**: System MUST reject malformed payment order IDs with `400 validation`.
- **FR-016**: System MUST return `403 forbidden` for authenticated callers missing the required payment role.
- **FR-017**: System MUST return `401` for missing, invalid, or expired tokens.
- **FR-018**: System MUST distinguish missing authentication from insufficient authorization in externally observable behavior.
- **FR-019**: System MUST return stable machine-readable error codes for all error responses.
- **FR-020**: System MUST append at least the initial creation status history record with correlation ID, actor subject, and timestamp.
- **FR-021**: System MUST persist payment order, idempotency record, and initial status history atomically within a single transaction.
- **FR-022**: System MUST expose `ETag` on create and read responses.
- **FR-023**: System MUST propagate or generate `X-Correlation-ID` for all payment requests and include it in responses.
- **FR-024**: System MUST preserve Spring Modulith boundaries; the payment module MUST NOT depend on `merchant.internal`.
- **FR-025**: System MUST support the payment authority concepts `merchant:payments:create`, `merchant:payments:read`, and `merchant:payments:operate`. The `merchant:payments:operate` authority is registered in Keycloak and test JWT support as a planned unused role; no operate endpoints are implemented in this phase.
- **FR-033**: System MUST allow an authenticated caller with `platform:payments:read` to retrieve any merchant's payment order by `paymentOrderId` for support and investigation purposes.
- **FR-034**: The local lab identity setup MUST include a `platform.payment.reader` test identity with `platform:payments:read` authority.
- **FR-026**: System MUST NOT implement payment order lifecycle actions (authorize, capture, cancel), `If-Match` conditional behavior, `412 Precondition Failed`, or lifecycle state transitions in this phase.
- **FR-027**: System MUST provide a dashboard route for payment order creation and detail view within the merchant context.
- **FR-028**: The payment order UI MUST show a creation form, detail view with status badge, and role-aware action visibility.
- **FR-029**: The payment order UI MUST show validation feedback, success feedback, loading state, and error state.
- **FR-030**: System MUST deny unauthenticated access to all payment order behavior.
- **FR-031**: System MUST deny authenticated users who lack the relevant payment authority.
- **FR-032**: The local lab identity setup MUST include test identities with `merchant:payments:create` and `merchant:payments:read` authorities, and a `merchant:payments:operate` role registered as a planned unused role.

### Non-Functional Requirements

- **NFR-001**: Payment order creation and retrieval MUST provide deterministic outcomes suitable for repeated automated verification.
- **NFR-002**: Automated tests MUST be able to create payment orders with unique namespaced references and idempotency keys without relying on one global shared record.
- **NFR-003**: Payment order data MUST remain durable across normal application restarts.
- **NFR-004**: Idempotency behavior MUST remain reliable when two requests with the same `Idempotency-Key` arrive nearly simultaneously.
- **NFR-005**: Security behavior MUST be testable for missing authentication, invalid authentication, insufficient authority, wrong merchant scope, and valid authorized access.
- **NFR-006**: The dashboard journey MUST expose stable, accessible labels or controls so automated browser tests can exercise the business flow.
- **NFR-007**: The feature MUST preserve module-boundary clarity so future lifecycle capabilities can depend on the payment module through an explicit boundary.
- **NFR-008**: Error and feedback messages MUST be clear enough for a user to understand whether the problem is validation, idempotency conflict, missing access, insufficient authority, not found, or merchant eligibility.

## Quality and Architecture Impact *(mandatory)*

### Tester-Led Risk Notes

- Idempotency risk: same `Idempotency-Key` with same fingerprint must never create a duplicate payment order.
- Money precision risk: amount must be stored and validated as minor units, never as floating-point.
- Ownership risk: role alone is insufficient; merchant scope must be enforced at the service and data layer.
- Cross-tenant leakage risk: single-resource read must return masked `404`, not `403`, to avoid disclosing resource existence.
- Concurrency risk: two near-simultaneous creates with the same `Idempotency-Key` must result in exactly one payment order.
- Atomicity risk: payment order, idempotency record, and status history must be written in one transaction.
- Error contract risk: error codes must be stable and machine-readable.
- UI risk: dashboard must not imply lifecycle actions, PSP integration, or complete admin/self-service functionality.
- Parallel-test risk: shared payment references or idempotency keys would create order-dependent tests.
- Scope-creep risk: adding lifecycle actions, PSP, cards, Kafka, webhooks, or settlement would blur the first-slice milestone.

### Modulith Impact *(required for backend-relevant features; otherwise state N/A)*

- **Module Ownership**: `payment` is the owning business module for payment order creation, idempotency, read access, and status history.
- **Module API Impact**: This phase exposes payment behavior through REST endpoints only. No cross-module public API types are introduced. Future lifecycle capabilities may add root-package public API types.
- **Dependency Impact**: `payment` depends on the merchant module's public API for active-merchant eligibility. `payment` MUST NOT depend on `merchant.internal`. The merchant module must not depend on the payment module.
- **Event Impact**: No external broker or asynchronous event flow is part of this phase. Status history is written synchronously within the transaction.
- **Module Test Impact**: Architecture verification must continue to run. Payment module tests must verify lifecycle behavior, public boundary behavior, and absence of forbidden dependencies on `merchant.internal`.

### Security, Data, and Observability Impact

- Payment order management is protected business behavior for merchant-scoped actors.
- The existing technical status behavior remains public and must not reveal payment data.
- Payment management requests are protected by JWT access-token validation at the service boundary.
- Payment management access is controlled by `merchant:payments:create`, `merchant:payments:read`, and `platform:payments:read` authority concepts plus merchant ownership. The `merchant:payments:operate` role is registered but unused in this phase.
- Missing authentication returns `401`. Insufficient authorization returns `403`. Cross-tenant single-resource read returns masked `404`.
- Payment records require durable storage with FK integrity to merchants, unique idempotency constraints, and amount/currency check constraints.
- Payment references and idempotency keys should support parallel-safe namespacing.
- Payment lifecycle changes should update the payment order's `updatedAt` timestamp.
- Status history records must include correlation ID for traceability.
- Logs must not expose tokens, secrets, or sensitive authorization headers.
- `X-Correlation-ID` is propagated or generated for all payment requests and included in responses and status history.

### Key Entities *(include if feature involves data)*

- **Payment Order**: A merchant-owned resource representing an intended payment. Key attributes are `paymentOrderId`, `merchantId`, `clientOrderReference`, `amountMinor`, `currency`, `status`, `version`, `createdAt`, `updatedAt`.
- **Idempotency Record**: A request deduplication record that links an `Idempotency-Key` and request fingerprint to a payment order within a merchant scope.
- **Payment Order Status History**: An append-only record of status changes for a payment order, including correlation ID and actor subject.
- **Payment Status**: Lifecycle state for a payment order. This phase has only `CREATED`. Future phases add `AUTHORIZED`, `CAPTURED`, `CANCELED`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A merchant payment creator can create a valid payment order from the dashboard in under 2 minutes during a normal local lab walkthrough.
- **SC-002**: 100% of created payment orders have a unique internal ID, stable client order reference, amount, currency, status, creation time, and update time visible through payment order retrieval.
- **SC-003**: Idempotent retry with the same key and same fingerprint consistently returns the same payment order identity without creating a duplicate row, in both single-request and near-simultaneous request scenarios.
- **SC-004**: Idempotent retry with the same key and different fingerprint consistently returns `409 idempotency_conflict`.
- **SC-005**: An unauthenticated user cannot create or read payment orders, while the public technical status capability remains accessible.
- **SC-006**: A user without the required authority cannot perform payment actions, and tests can distinguish missing authentication (`401`) from insufficient authorization (`403`).
- **SC-007**: Cross-tenant single-resource reads return masked `404` and do not disclose resource existence.
- **SC-008**: The payment order journey can be verified end-to-end: sign in, navigate to merchant detail, create payment order, see `CREATED` status, view detail.
- **SC-009**: Automated tests can run repeatedly with unique payment references and idempotency keys and do not depend on one shared global record.
- **SC-010**: A platform payment reader with `platform:payments:read` can read payment orders across all merchants, and security matrix tests verify this cross-merchant read access.
- **SC-011**: The `merchant:payments:operate` role is registered in Keycloak and test JWT support but no operate endpoints exist; security matrix tests verify that this role alone does not grant create or read access.
- **SC-012**: This phase introduces no payment lifecycle actions, PSP integration, Kafka, webhooks, settlement, reconciliation, Client Credentials Flow, or complete admin/self-service portal behavior.

## Assumptions

- Merchant payment creators and readers are scoped to a single merchant in this phase.
- The initial dashboard is intended for desktop or standard responsive dashboard use.
- Amount is always represented in minor units (e.g., 1000 = 10.00 PLN) to avoid floating-point precision issues.
- Currency comparison is case-sensitive and uses uppercase 3-letter ISO-like codes.
- `Idempotency-Key` is an opaque string provided by the client, unique per merchant and operation.
- Request fingerprint for idempotency comparison is derived from the request body and relevant headers.
- Payment order deletion is intentionally absent.
- Payment order list/filter/search is deferred.
- Payment order mutation after creation is intentionally absent.
- Client Credentials Flow waits until merchant machine-to-machine payment API use cases exist.
- Country-specific rules, currency conversion, settlement, KYC, pricing, and routing are deferred.
- The existing `CorrelationIdFilter` already provides `X-Correlation-ID` propagation and MDC integration.
- The existing Keycloak realm and test JWT infrastructure can be extended with new payment roles.
- The `merchant:payments:operate` role is added to Keycloak and test JWT support now as a planned unused role, so the lifecycle slice does not require Keycloak changes.
- A `platform.payment.reader` test identity with `platform:payments:read` authority is available in the local lab identity setup.
- The merchant module will expose a public API boundary for active-merchant eligibility checks.
