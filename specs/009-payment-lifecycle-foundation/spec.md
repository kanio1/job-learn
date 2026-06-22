# Feature Specification: Payment Lifecycle Foundation

**Feature Branch**: `009-payment-lifecycle-foundation`

**Created**: 2026-06-04

**Status**: Draft

**Input**: BA Discovery Pack + Comprehensive Gap Analysis for Lesson 14

## Business Purpose *(mandatory)*

This feature introduces realistic payment lifecycle operations to transform the current "payment order registry" into a production-grade payment platform. Currently, the system only supports `CREATED` status — payment orders can be created and read, but cannot progress through a real payment flow. This feature adds the ability to authorize, capture, cancel, and refund payment orders, enabling testers to practice state machine transitions, optimistic locking, idempotency for non-create actions, HTTP protocol hardening, and security matrices over realistic business scenarios.

The learning outcome is that a Senior QA Automation/SDET can design and verify state machine correctness, concurrency control, retry logic, and HTTP protocol behavior for a PayU-like payment system.

## Actors *(mandatory)*

- **Merchant Payment Operator**: Executes lifecycle actions (authorize, capture, cancel, refund) for the merchant they own. Must have role `merchant:payments:lifecycle` and matching `merchant_id` claim.
- **Platform Payment Administrator**: Can force lifecycle actions on any merchant's payment order. Must have role `platform:payments:lifecycle`. No ownership check required.
- **System Auditor**: Reads payment status history for compliance and debugging. Must have role `platform:payments:audit` (read-only, introduced in this feature).
- **Existing Actors**: Merchant payment creator (`merchant:payments:create`), merchant payment reader (`merchant:payments:read`), platform payment reader (`platform:payments:read`) — unchanged.

## Scope *(mandatory)*

### In Scope

- Four lifecycle actions for existing payment orders: authorize, capture, cancel, refund
- Six payment statuses: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED
- State machine with valid and invalid transition rules (24 possible state × action combinations)
- Optimistic locking using `If-Match` / ETag / version column
- Idempotency for lifecycle actions (replay produces cached result, mismatched action produces conflict)
- Payment status history audit trail (immutable log of all transitions)
- Authorization expiration: lazy check on capture (7-day window)
- Partial amount support for capture and refund (with amount validation)
- Simple PSP mock that always succeeds
- HTTP protocol hardening: CORS configuration, Cache-Control headers, Vary headers
- PATCH endpoint for metadata updates without status change
- New roles: `merchant:payments:lifecycle`, `platform:payments:lifecycle`, `platform:payments:audit`
- Database migration adding lifecycle columns, version column, and status history table
- Basic frontend display of lifecycle status and history timeline in the payment detail page

### Out of Scope

- Partial authorization (authorize amount always equals payment order amount)
- Multi-capture (single capture per authorization)
- Multi-refund (single refund per capture)
- PSP failure scenarios (mock always succeeds)
- Automatic expiration job (only lazy expiration on capture attempt)
- Dispute handling or chargebacks
- Webhooks, Kafka, event pipeline, or async processing
- Scheduled jobs or background processing
- Rate limiting
- API versioning
- HATEOAS links
- Complete OAuth/OIDC application integration
- Complete business dashboards
- Performance or load testing
- New automated tests as deliverables (existing tests run for regression only)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Authorize Payment Order (Priority: P1)

A merchant operator needs to reserve customer funds before finalizing a payment. They submit an authorize request that transitions the payment order from CREATED to AUTHORIZED, sets a 7-day expiration window, and returns an updated version identifier for future concurrency control.

**Why this priority**: Authorization is the gateway to all downstream lifecycle operations. Without it, capture, cancel-after-authorization, and expiration are impossible.

**Independent Test**: Create a payment order in CREATED status, send an authorize request with valid `Idempotency-Key` and current `If-Match` headers, verify the order transitions to AUTHORIZED with `expires_at` set 7 days in the future, ETag version increments, and `Cache-Control: no-store` is present. Repeat with the same idempotency key to verify replay returns the same result.

**Acceptance Scenarios**:

1. **Given** a payment order in CREATED status with ETag `"v1"`, **When** an authorized merchant operator sends `POST /{id}/authorize` with `Idempotency-Key`, `If-Match: "v1"`, and valid body, **Then** the order transitions to AUTHORIZED, `authorized_at` and `expires_at` are set, ETag becomes `"v2"`, response includes `Cache-Control: no-store` and `Vary: Authorization, If-Match`.

2. **Given** a successful authorize with `Idempotency-Key: "key-abc"`, **When** the same merchant sends another authorize with the same `Idempotency-Key: "key-abc"`, **Then** the response is 200 OK with the cached result (no duplicate authorization).

3. **Given** a payment order in CREATED status, **When** an operator with `merchant:payments:read` (not `lifecycle`) attempts authorize, **Then** 403 Forbidden is returned.

4. **Given** a payment order in AUTHORIZED or CAPTURED status, **When** authorize is attempted, **Then** 422 Unprocessable Entity with `error=invalid_transition` is returned.

---

### User Story 2 - Capture Authorized Payment (Priority: P1)

A merchant operator needs to transfer reserved funds after authorization. They submit a capture request that transitions the order from AUTHORIZED to CAPTURED, optionally capturing a partial amount, and clears the authorization expiration.

**Why this priority**: Capture is the financial settlement step. Together with authorize, it forms the core payment flow. Partial capture enables real-world scenarios like splitting shipments.

**Independent Test**: Authorize a payment order, send a capture request with valid headers, verify the order transitions to CAPTURED with `captured_at` set, `captured_amount_minor` matches the requested amount, and `expires_at` is cleared.

**Acceptance Scenarios**:

1. **Given** a payment order in AUTHORIZED status, **When** an authorized merchant sends `POST /{id}/capture` with `Idempotency-Key`, `If-Match`, and `amountMinor` equal to the authorized amount, **Then** the order transitions to CAPTURED, `captured_at` is set, and ETag increments.

2. **Given** a payment order in AUTHORIZED status with amount 10000, **When** capture is requested with `amountMinor: 5000`, **Then** the order transitions to CAPTURED with `captured_amount_minor: 5000`, and the remaining 5000 authorization is released.

3. **Given** a payment order in AUTHORIZED status with expired authorization (7 days past), **When** capture is attempted, **Then** 422 Unprocessable Entity with `error=authorization_expired` is returned, and the order transitions to EXPIRED.

4. **Given** a payment order in CREATED status, **When** capture is attempted, **Then** 422 Unprocessable Entity with `error=invalid_transition` is returned.

5. **Given** a payment order in AUTHORIZED status with amount 10000, **When** capture is requested with `amountMinor: 15000`, **Then** 422 Unprocessable Entity with `error=capture_amount_exceeds_authorized` is returned.

---

### User Story 3 - Cancel Payment Order (Priority: P2)

A merchant operator needs to cancel a payment order that is no longer needed, either before authorization (releasing order resources) or after authorization (voiding the authorization and releasing funds).

**Why this priority**: Cancellation is essential for business operations — orders are abandoned, customers change their mind, or errors occur. This is lower priority than core authorize/capture because the happy path is capture, not cancel.

**Independent Test**: Create a payment order in CREATED status, send a cancel request, verify status becomes CANCELLED. Repeat with an AUTHORIZED order to verify authorization void occurs.

**Acceptance Scenarios**:

1. **Given** a payment order in CREATED status, **When** cancel is requested, **Then** the order transitions to CANCELLED and no PSP action is triggered.

2. **Given** a payment order in AUTHORIZED status, **When** cancel is requested, **Then** the order transitions to CANCELLED and the PSP mock void is called.

3. **Given** a payment order in CAPTURED status, **When** cancel is attempted, **Then** 422 Unprocessable Entity with `error=invalid_transition` is returned (must use refund instead).

4. **Given** a payment order in CANCELLED status, **When** cancel is attempted, **Then** 422 Unprocessable Entity with `error=invalid_transition` is returned.

---

### User Story 4 - Refund Captured Payment (Priority: P2)

A merchant operator needs to return funds to a customer after a capture. They submit a refund request that transitions the order from CAPTURED to REFUNDED.

**Why this priority**: Refunds are critical for customer satisfaction and dispute resolution. This is P2 because it depends on capture (P1) completing first.

**Independent Test**: Capture a payment order, send a refund request with partial amount, verify status becomes REFUNDED with `refunded_at` and `refund_reason` logged.

**Acceptance Scenarios**:

1. **Given** a payment order in CAPTURED status with captured amount 10000, **When** refund is requested with `amountMinor: 5000`, **Then** the order transitions to REFUNDED, `refunded_at` is set, and `refund_reason` is recorded.

2. **Given** a payment order in CAPTURED status, **When** refund is requested with `amountMinor` exceeding the captured amount, **Then** 422 Unprocessable Entity with `error=refund_amount_exceeds_captured` is returned.

3. **Given** a payment order in AUTHORIZED status, **When** refund is attempted, **Then** 422 Unprocessable Entity with `error=invalid_transition` is returned.

---

### User Story 5 - Concurrency Control (Priority: P1)

Two operators attempt simultaneous lifecycle actions on the same payment order. The system must prevent lost updates by rejecting requests with stale version identifiers.

**Why this priority**: Lost updates in financial systems can result in double captures or incorrect balances. This is a correctness requirement, not a nice-to-have.

**Independent Test**: Obtain a payment order's ETag (v1), modify it through a lifecycle action (now v2), send another request with the stale ETag (v1), verify 412 Precondition Failed is returned.

**Acceptance Scenarios**:

1. **Given** a payment order with ETag `"v2"`, **When** a lifecycle action is sent with `If-Match: "v1"` (stale), **Then** 412 Precondition Failed with `error=concurrency_conflict` is returned.

2. **Given** a payment order, **When** a lifecycle action is sent without `If-Match` header, **Then** 400 Bad Request with `error=missing_required_header` is returned.

3. **Given** a successful lifecycle action, **When** the response is inspected, **Then** the ETag value reflects the incremented version number (e.g., `"v3"` after starting from `"v2"`).

---

### User Story 6 - Audit Trail (Priority: P2)

A platform auditor needs to trace the complete lifecycle of any payment order for compliance, debugging, and reconciliation.

**Why this priority**: Audit trails are essential for financial compliance and operational debugging. P2 because they document existing actions rather than enabling new ones.

**Independent Test**: Execute a sequence of lifecycle actions on a payment order (create → authorize → capture), query the status history endpoint, verify all transitions are logged with correct `from_status`, `to_status`, actor, timestamp, and correlation ID.

**Acceptance Scenarios**:

1. **Given** a payment order that has undergone create → authorize → capture, **When** a platform auditor requests `GET /{id}/history`, **Then** a chronologically ordered list of three status history records is returned.

2. **Given** a status history record, **When** inspected, **Then** it contains `from_status`, `to_status`, `actor_subject`, `idempotency_key_hash`, `correlation_id`, and `created_at`.

3. **Given** a payment order with no lifecycle actions beyond creation, **When** history is requested, **Then** an empty list is returned (creation is not tracked as a lifecycle action in Lesson 14).

---

### User Story 7 - HTTP Protocol Hardening (Priority: P3)

API consumers interact with the payment lifecycle endpoints over HTTP. The system must respect CORS boundaries in browser contexts, signal that responses should not be cached, and indicate which headers influence the response.

**Why this priority**: HTTP protocol correctness is a quality requirement for production APIs. P3 because the core business logic (P1-P2) must work first.

**Independent Test**: Send an OPTIONS preflight request to a lifecycle endpoint, verify CORS headers are present. Send lifecycle actions, verify `Cache-Control: no-store` and `Vary: Authorization, If-Match` are present in responses.

**Acceptance Scenarios**:

1. **Given** a CORS configuration active in the dev/test profile, **When** an OPTIONS request is sent to any lifecycle endpoint with `Origin: http://localhost:3000`, **Then** 200 OK with `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, and `Access-Control-Allow-Headers` headers is returned.

2. **Given** any lifecycle action response, **When** the response headers are inspected, **Then** `Cache-Control: no-store` and `Vary: Authorization, If-Match` are present.

3. **Given** a payment order, **When** `PATCH /{id}` is sent with a metadata body and valid `If-Match`, **Then** the metadata is updated without changing the status or incrementing the version.

---

### Edge Cases

- What happens when an authorization has exactly 0 seconds remaining before expiration? Capture succeeds within the window; at exactly `expires_at`, the expiration check has already triggered.
- What happens when an idempotency key hash collides with an existing record for a different merchant? No conflict — uniqueness is per merchant, not globally.
- What happens when a lifecycle action body contains extra unknown fields? Unknown fields are ignored (lenient parsing) unless they conflict with known field semantics.
- What happens when the `amountMinor` in a capture request is exactly 0? Rejected as validation error — capture amount must be positive.
- What happens when a PATCH request contains an unrecognized metadata key? The key-value pair is stored but no validation is applied to metadata keys.
- What happens when the database connection is temporarily unavailable during a lifecycle action? The request fails with 503 Service Unavailable (the same error-handling path as existing endpoints).
- What happens when the PSP mock response format changes between authorize and capture? Not applicable in Lesson 14 — PSP mock is stateless and always succeeds with a fixed response.
- What happens when a payment order version exceeds `Long.MAX_VALUE`? Not reachable in practice — each lifecycle action increments by 1; 2^63 actions would take millennia.
- What happens when a CORS request arrives from an origin not in the allowed list? The CORS configuration does not add `Access-Control-Allow-Origin` for the disallowed origin; the browser blocks the response.
- What happens when the status history table reaches significant size? The indexed query on `payment_order_id` ensures constant-time retrieval per order; archive strategy is deferred.

## Requirements *(mandatory)*

### Functional Requirements

**Lifecycle Actions:**
- **FR-LIFECYCLE-001**: System MUST provide `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize` to transition CREATED → AUTHORIZED.
- **FR-LIFECYCLE-002**: System MUST provide `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture` to transition AUTHORIZED → CAPTURED.
- **FR-LIFECYCLE-003**: System MUST provide `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel` to transition CREATED/AUTHORIZED → CANCELLED.
- **FR-LIFECYCLE-004**: System MUST provide `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund` to transition CAPTURED → REFUNDED.
- **FR-LIFECYCLE-005**: All lifecycle actions MUST require `Idempotency-Key` and `If-Match` request headers.
- **FR-LIFECYCLE-006**: All lifecycle responses MUST include updated ETag, `Cache-Control: no-store`, and `Vary: Authorization, If-Match` headers.
- **FR-LIFECYCLE-007**: Authorize MUST set `authorized_at` and `expires_at` (7 days from authorization) on the payment order.
- **FR-LIFECYCLE-008**: Capture MUST support optional `amountMinor` for partial capture; MUST clear `expires_at` on success.
- **FR-LIFECYCLE-009**: Refund MUST support optional `amountMinor` for partial refund; MUST record `refund_reason` when provided.
- **FR-LIFECYCLE-010**: Cancel MUST void authorization via PSP mock when transitioning from AUTHORIZED status.

**State Machine:**
- **FR-STATE-001**: System MUST reject invalid state transitions with `422 Unprocessable Entity` and `error=invalid_transition`.
- **FR-STATE-002**: Valid transitions MUST follow this matrix — CREATED → AUTHORIZED, CANCELLED; AUTHORIZED → CAPTURED, CANCELLED, EXPIRED; CAPTURED → REFUNDED; all others invalid.
- **FR-STATE-003**: Capture on an expired authorization MUST reject with `422` and `error=authorization_expired`, transitioning the order to EXPIRED.
- **FR-STATE-004**: Capture/refund with amount exceeding available amount MUST reject with `422` and appropriate error code.

**Optimistic Locking:**
- **FR-LOCKING-001**: System MUST increment the payment order `version` column on every successful lifecycle action.
- **FR-LOCKING-002**: ETag MUST be formatted as `"v{version}"` where version is the current version number.
- **FR-LOCKING-003**: Stale `If-Match` value MUST return `412 Precondition Failed` with `error=concurrency_conflict`.
- **FR-LOCKING-004**: Missing `If-Match` header MUST return `400 Bad Request` with `error=missing_required_header`.
- **FR-LOCKING-005**: PATCH metadata updates MUST NOT increment version.

**Idempotency:**
- **FR-IDEMPOTENCY-001**: Same `Idempotency-Key` + same lifecycle action MUST return cached result (`200 OK`) without re-executing.
- **FR-IDEMPOTENCY-002**: Same `Idempotency-Key` + different lifecycle action MUST return `409 Conflict` with `error=idempotency_conflict`.
- **FR-IDEMPOTENCY-003**: Missing `Idempotency-Key` MUST return `400 Bad Request` with `error=validation`.
- **FR-IDEMPOTENCY-004**: Existing create idempotency behavior MUST remain unchanged.

**Audit Trail:**
- **FR-AUDIT-001**: System MUST record every status transition in `payment_order_status_history`.
- **FR-AUDIT-002**: Each history record MUST include `from_status`, `to_status`, `actor_subject`, `idempotency_key_hash`, `correlation_id`, `created_at`.
- **FR-AUDIT-003**: Status history MUST be insert-only (immutable once written).
- **FR-AUDIT-004**: System MUST provide `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history` returning chronological transition list.

**HTTP Protocol:**
- **FR-HTTP-001**: CORS configuration MUST be active in dev and test profiles, disabled in production.
- **FR-HTTP-002**: OPTIONS preflight to any lifecycle endpoint MUST return CORS headers.
- **FR-HTTP-003**: System MUST provide `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` for metadata updates without status change.

**Database:**
- **FR-DB-001**: Flyway migration V4 MUST add lifecycle tracking columns (`authorized_at`, `expires_at`, `captured_at`, `cancelled_at`, `refunded_at`, `version`, `captured_amount_minor`, `cancellation_reason`, `refund_reason`).
- **FR-DB-002**: V4 MUST create `payment_order_status_history` table with PK, FK to `payment_orders`, and index on `(payment_order_id, created_at DESC)`.
- **FR-DB-003**: V4 MUST expand the `chk_payment_orders_status` constraint to include new statuses: CREATED, AUTHORIZED, CAPTURED, CANCELLED, EXPIRED, REFUNDED.
- **FR-DB-004**: Existing V2/V3 constraints and indexes MUST remain unchanged.

**Security:**
- **FR-SEC-001**: System MUST support `merchant:payments:lifecycle` role granting access to all lifecycle actions.
- **FR-SEC-002**: System MUST support `platform:payments:lifecycle` role granting platform override (no ownership check).
- **FR-SEC-003**: System MUST support `platform:payments:audit` role granting read-only access to status history.
- **FR-SEC-004**: Lifecycle actions MUST verify `merchant_id` claim matches path parameter (unless platform override).
- **FR-SEC-005**: Existing roles (`merchant:payments:create`, `merchant:payments:read`, `platform:payments:read`) MUST remain unchanged.
- **FR-SEC-006**: `SecurityConfig` MUST include matchers for lifecycle and history endpoints.

**PSP Mock:**
- **FR-PSP-001**: System MUST define a `PspClient` interface for PSP operations (authorize, capture, void, refund).
- **FR-PSP-002**: A `MockPspClient` implementation MUST always return success.
- **FR-PSP-003**: PSP mock MUST be injected via dependency injection into `PaymentLifecycleService`.
- **FR-PSP-004**: PSP mock MUST NOT simulate failures in this feature.

### Non-Functional Requirements

- **NFR-001**: System MUST respond to lifecycle actions within 500ms under normal load (single user, local development).
- **NFR-002**: System MUST preserve existing test suite passing (regression) — no existing payment order behavior may break.
- **NFR-003**: Concurrent lifecycle requests with the same ETag MUST be serialized correctly (one succeeds, one gets 412).
- **NFR-004**: Status history records MUST be written atomically with the payment order status update (same transaction).

## Quality and Architecture Impact *(mandatory)*

### Tester-Led Risk Notes

- **State Machine Correctness**: 24 possible state × action combinations. Tester should design a decision-table-driven parameterized test covering all valid and invalid transitions.
- **Concurrency Risk**: Optimistic locking prevents lost updates, but testers must verify that stale ETag handling works under realistic concurrency (two threads submitting simultaneous lifecycle actions).
- **Idempotency Risk**: Same key + same action = replay. Same key + different action = conflict. Testers should verify both scenarios for each lifecycle action.
- **Expiration Boundary**: Authorization expiration at exactly 7 days. Testers should test "just before expiration" (capture succeeds) and "just after" (capture fails, order → EXPIRED).
- **Amount Validation**: Partial capture/refund amounts must be validated. Testers should use boundary value analysis — zero amount, negative amount, exactly authorized amount, just over authorized amount.
- **CORS Security**: CORS configuration must be profile-gated. Testers should verify CORS headers are present in dev/test and absent in production.
- **Cache Poisoning Prevention**: `Cache-Control: no-store` prevents financial data from being cached by intermediaries. Testers should verify the header is present on all lifecycle responses.
- **Audit Trail Integrity**: Status history must be append-only and must match the actual payment order state at every transition. Testers should verify history records are consistent with state after each lifecycle action.

### Modulith Impact

- **Module Ownership**: `payment` module (existing).
- **Module API Impact**: New public actions exposed via `PaymentLifecycleService` (authorize, capture, cancel, refund). Internal-only: `PaymentStatusHistoryRepository`, `PspClient` interface.
- **Dependency Impact**: New dependency on a `PspClient` interface (inbound — injected into service). No new module dependencies.
- **Event Impact**: No application events in this feature. Lifecycle transitions are synchronous. Events are deferred to Lesson 17.
- **Module Test Impact**: Existing `PaymentModuleTest` must pass after changes. New module test may verify lifecycle service is available but is not required for this feature.

### Security, Data, and Observability Impact

- **Authentication**: All lifecycle endpoints require a valid JWT token with appropriate role (unchanged authentication mechanism).
- **Authorization**: Two new roles added. Ownership check uses existing `merchant_id` claim pattern. Platform override bypasses ownership check.
- **Validation**: State transition validity, amount range checks, expiration checks, header presence checks are all server-side validations returning structured error responses.
- **Persistence**: New columns on existing `payment_orders` table. New `payment_order_status_history` table. Both in the same transaction boundary.
- **Transaction**: Lifecycle actions are transactional — payment order update and status history insert share the same transaction.
- **Audit**: Status history provides complete audit trail. Correlation ID propagates from request through to audit records.
- **Logging**: Lifecycle actions log at INFO level with correlation ID, merchant ID, payment order ID, and result status.
- **Error Contract**: All errors use `PaymentErrorResponse` with `error`, `message`, `correlationId`, and optional `details`.

### Key Entities

- **PaymentOrder** (extended): Existing entity. New fields: `status` (expanded enum), `authorized_at`, `expires_at`, `captured_at`, `cancelled_at`, `refunded_at`, `version` (optimistic locking), `captured_amount_minor`, `cancellation_reason`, `refund_reason`.
- **PaymentStatusHistory**: New entity. Fields: `status_history_id` (UUID PK), `payment_order_id` (FK to payment_orders), `from_status`, `to_status` (VARCHAR, constrained to valid statuses), `actor_subject` (JWT subject), `reason` (nullable VARCHAR), `idempotency_key_hash`, `correlation_id`, `created_at`.
- **PspClient** (interface): New abstraction. Methods: `authorize(paymentOrderId, amount, currency)`, `capture(paymentOrderId, amount, currency)`, `void(paymentOrderId, authorizationId)`, `refund(paymentOrderId, amount, currency)`. All return a result DTO.
- **MockPspClient** (implementation): New class implementing `PspClient`. All methods return success immediately with a mock transaction reference.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A merchant operator can authorize a CREATED payment order, see it transition to AUTHORIZED, and receive an updated ETag within 500ms of the request completing.
- **SC-002**: A merchant operator can then capture the same order (full or partial), see it transition to CAPTURED, and receive the updated ETag.
- **SC-003**: Two concurrent lifecycle actions on the same payment order with the same ETag result in exactly one success and one 412 Precondition Failed (no lost updates).
- **SC-004**: Replaying a lifecycle action with the same `Idempotency-Key` returns the cached result without re-executing the PSP call or creating duplicate status history records.
- **SC-005**: Using the same `Idempotency-Key` for a different lifecycle action returns 409 Conflict, preventing accidental misuse of idempotency keys.
- **SC-006**: An attempted capture on an expired authorization returns 422 and transitions the order to EXPIRED.
- **SC-007**: Every status transition across all lifecycle actions is recorded in the status history table with complete actor and correlation context.
- **SC-008**: All existing payment order tests (create, read, list, summary, security, HTTP edge) continue to pass after the lifecycle feature is added.
- **SC-009**: OPTIONS requests to lifecycle endpoints return CORS headers when the dev/test profile is active, enabling browser-based API consumers.
- **SC-010**: A platform auditor with `platform:payments:audit` role can retrieve the complete status history of any payment order.

## Assumptions

- Existing payment order foundation (Lessons 06-13) is stable and all existing tests pass.
- Existing `payment_orders` table has sufficient data for adding new columns without data migration (all existing orders are CREATED).
- Existing Keycloak realm supports adding new roles without breaking existing role configurations.
- PSP mock is an interface within the same application (not an external service requiring network calls).
- Authorization expiration is lazy (checked on capture) — a future Lesson 16 may add a scheduled job for automatic expiration.
- Single capture per authorization and single refund per capture are acceptable simplifications for the first lifecycle implementation.
- Frontend scope is limited to displaying lifecycle status and history; no lifecycle action buttons are added to the UI.
- All timestamps are stored in UTC.

## Open Questions / Clarifications

No unresolved clarification blocks `/speckit.plan`. All planning-critical decisions are resolved in the BA Discovery Pack and Gap Analysis:

1. Single capture per authorization (multi-capture deferred to Lesson 15).
2. Lazy expiration only (scheduled job deferred to Lesson 16).
3. PSP always succeeds (failures deferred to Lesson 16).
4. Full authorization only (partial authorization deferred to Lesson 15).
5. PATCH metadata only (does not change status or version).
6. Frontend limited scope (status display only, no lifecycle action buttons).
7. New roles `merchant:payments:lifecycle`, `platform:payments:lifecycle`, `platform:payments:audit` are required.
