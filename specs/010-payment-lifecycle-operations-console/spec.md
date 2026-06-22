# Feature Specification: Payment Lifecycle Operations Console

**Feature Branch**: `010-payment-lifecycle-operations-console`

**Created**: 2026-06-05

**Status**: Draft

**Input**: User description: `specs/010-payment-lifecycle-operations-console/prompt-phase-1-specify.md` plus `BA_DISCOVERY_PACK.md`

## Business Purpose *(mandatory)*

This feature turns the payment lifecycle foundation from feature 009 into usable application behavior for operators, readers, auditors, and support users. Feature 009 introduced lifecycle state, lifecycle mutations, metadata PATCH, conditional updates, and status history. This feature focuses on the operations console experience that lets a user inspect a payment order, understand lifecycle facts, choose only valid lifecycle actions, submit those actions safely, and update metadata without implying a status transition.

The business outcome is a realistic PayU-like operational surface for lifecycle work without adding new payment business semantics. The learning outcome is that the application boundary now exposes state-aware UI behavior, stale-state handling, role-aware affordances, safe error feedback, and header forwarding/generation responsibilities as product requirements rather than test-framework work.

### Lesson 14 / Feature 009 Context

This feature builds on the Lesson 14 / Feature 009 lifecycle foundation. The relevant existing application concepts are the lifecycle REST surface, lifecycle application service, lifecycle domain state, lifecycle status vocabulary, status history/audit model, PSP mock boundary, lifecycle HTTP error mapping, security/CORS support, frontend lifecycle schemas, payment detail component, Nuxt proxy boundary, and lifecycle proxy routes.

### Co nowego technicznie dochodzi

Najważniejsze nowe obszary exposed by this application feature are:

- Specific HTTP headers: `ETag`, `If-Match`, `Idempotency-Key`, `X-Correlation-ID`, `Cache-Control`, `Vary`, `Authorization`, and CORS headers such as `Access-Control-Allow-Origin`, `Access-Control-Allow-Headers`, and `Access-Control-Expose-Headers`.
- Specific response codes and error categories: `200`, `400`, `401`, `403`, `404`, `409`, `412`, `415`, `422`, optional `406`, and `OPTIONS 200` for CORS preflight behavior.
- Business-technical flows: create order -> read version marker -> submit lifecycle action with conditional update, authorize -> capture -> history, stale version -> `412` -> refresh state, retry-safe action with idempotency key, forbidden action by wrong role, platform operator vs merchant operator, and frontend -> Nuxt proxy -> backend -> database/history visibility.
- Modern application boundary behavior: API auth failures remain backend `401`/`403` categories, browser login/redirect behavior belongs to the app layer, proxy requests preserve lifecycle headers and backend error shape, and history/audit makes database effects visible without exposing secrets.

## Actors *(mandatory)*

- **Merchant Payment Operator**: Manages lifecycle actions for payment orders that belong to their merchant when they have lifecycle permission and matching merchant ownership.
- **Merchant Reader**: Opens payment details and lifecycle history for their merchant without performing lifecycle or metadata mutations.
- **Platform Payment Operator**: Manages lifecycle actions across merchants when platform lifecycle permission is present.
- **Platform Auditor**: Inspects lifecycle history across merchants without mutation rights.
- **Support/Risk Analyst**: Uses payment detail and history to explain what happened to a payment order using the read or audit permissions available in the current realm.

## Scope *(mandatory)*

### In Scope

- Payment order detail lifecycle summary for the six feature 009 statuses: `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, and `REFUNDED`.
- Status badges that make terminal and actionable statuses visually distinct.
- Display of amount, currency, lifecycle timestamps, captured/refunded amounts, cancellation/refund reasons, and current metadata when those fields are present.
- Lifecycle history display loaded from the existing payment history contract and rendered as an oldest-first chronological timeline or list.
- State-aware lifecycle action controls for authorize, capture, cancel, and refund based on current status.
- Confirmation UX for destructive or irreversible lifecycle actions.
- Capture, refund, cancel, and optional authorize reason/amount inputs only where supported by the existing lifecycle contract.
- Application server proxy behavior for forwarding authorization, conditional update, idempotency, and correlation headers on lifecycle routes.
- Metadata update flow that is separate from lifecycle actions and uses current conditional update rules.
- Conservative role and permission UX that avoids implying permission when the application lacks enough role context.
- User-facing success, loading, empty, stale-state, forbidden, validation, invalid-transition, idempotency-conflict, not-found, and backend-unavailable states.

### Out of Scope

- No test suite implementation as feature deliverable.
- No REST Assured framework work.
- No new backend test classes as feature scope.
- No frontend E2E test scope.
- No multi-capture.
- No multi-refund.
- No PSP failure scenarios.
- No PSP provider integration.
- No webhooks.
- No Kafka.
- No scheduled expiration job.
- No rate limiting.
- No HATEOAS redesign.
- No complete business dashboard.
- No fake KPIs or fake operational metrics.
- No complete OAuth/OIDC app integration.
- No new payment lifecycle behavior beyond feature 009 semantics.
- No new payment creation capability, including no `POST /payments` scope.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Inspect Lifecycle Detail (Priority: P1)

An operator or read-capable user opens a payment order detail screen and needs to understand the current lifecycle status and financial facts without reading raw API responses.

**Why this priority**: Inspection is the minimum useful console behavior. Without accurate detail display, any action control or history view is unsafe because the user cannot tell what state they are operating on.

**Independent Test**: Can be verified by opening payment orders in each lifecycle status and confirming the detail view shows the correct status badge, lifecycle fields that exist for that status, amount/currency, and metadata without exposing blank or misleading fields.

**Acceptance Scenarios**:

1. **Given** a payment order in `CREATED` status, **When** a user opens its detail view, **Then** the view shows `CREATED`, amount, currency, metadata, and no lifecycle timestamps that are not present.
2. **Given** a payment order in `AUTHORIZED` status, **When** a user opens its detail view, **Then** the view shows `AUTHORIZED`, amount, currency, authorized timestamp, expiration timestamp when present, and metadata.
3. **Given** a payment order in `CAPTURED` status, **When** a user opens its detail view, **Then** the view shows `CAPTURED`, captured timestamp, captured amount, amount, currency, and metadata.
4. **Given** a payment order in `CANCELLED` status, **When** a user opens its detail view, **Then** the view shows `CANCELLED`, cancelled timestamp and cancellation reason when present, and no available lifecycle mutation action.
5. **Given** a payment order in `REFUNDED` status, **When** a user opens its detail view, **Then** the view shows `REFUNDED`, refunded timestamp, refunded amount, refund reason when present, and no available lifecycle mutation action.

---

### User Story 2 - Review Lifecycle History (Priority: P1)

An operator, auditor, or support user needs to see how the payment reached its current state by reading a chronological status history.

**Why this priority**: Lifecycle state without history is hard to audit or explain. History is also needed after each mutation so users can verify that the observed state change matches the action they performed.

**Independent Test**: Can be verified by opening an order with zero, one, and multiple lifecycle history entries and checking that the history area loads, displays entries oldest-first, shows safe fields, and handles empty/error states.

**Acceptance Scenarios**:

1. **Given** a payment order with no lifecycle transitions beyond creation, **When** a user opens lifecycle history, **Then** the view shows an empty history state rather than an error.
2. **Given** a payment order with authorize then capture transitions, **When** history is rendered, **Then** entries appear oldest-first with from status, to status, action timestamp, and safe actor information when available.
3. **Given** history entries include reason, amount, or PSP reference fields from the backend, **When** history is rendered, **Then** those fields are shown without exposing secrets, raw tokens, or credentials.
4. **Given** the history request is loading, **When** the user views the detail page, **Then** the history area communicates loading separately from the payment detail summary.
5. **Given** the history request fails, **When** the detail page is otherwise available, **Then** the user sees a clear history error state without losing the payment detail summary.

---

### User Story 3 - Execute Valid Lifecycle Actions (Priority: P1)

A lifecycle-capable operator needs to perform only the actions that are valid for the current payment status, with confirmation and clear feedback for the result.

**Why this priority**: The console exists to make feature 009 lifecycle operations usable. The UI must reduce invalid operations instead of exposing every backend action at all times.

**Independent Test**: Can be verified by opening payment orders in each status and confirming the action set matches the state matrix, then performing each valid action and observing detail/history refresh after success.

**Acceptance Scenarios**:

1. **Given** a `CREATED` payment order and a lifecycle-capable operator, **When** the detail view renders action controls, **Then** authorize and cancel are available and capture/refund are not offered.
2. **Given** an `AUTHORIZED` payment order and a lifecycle-capable operator, **When** the detail view renders action controls, **Then** capture and cancel are available and authorize/refund are not offered.
3. **Given** a `CAPTURED` payment order and a lifecycle-capable operator, **When** the detail view renders action controls, **Then** refund is available and authorize/capture/cancel are not offered.
4. **Given** a payment order in `CANCELLED`, `EXPIRED`, or `REFUNDED` status, **When** the detail view renders action controls, **Then** no lifecycle mutation action is offered.
5. **Given** an operator confirms capture, cancel, or refund, **When** the backend accepts the action, **Then** the detail summary and lifecycle history are reloaded and the user sees success feedback tied to the performed action.
6. **Given** a lifecycle action fails, **When** the error is returned, **Then** the user sees failure feedback and the console does not pretend the action succeeded.

---

### User Story 4 - Submit Lifecycle Requests Safely Through the Application Boundary (Priority: P1)

The application must preserve the lifecycle protocol requirements when a user performs an action from the console.

**Why this priority**: Feature 009 relies on authorization, idempotency, conditional updates, and correlation identifiers. If the application boundary drops or invents these incorrectly, the console becomes unsafe even if the backend is correct.

**Independent Test**: Can be verified by performing lifecycle actions through the application boundary and observing that the effective request preserves authorization, conditional update, idempotency, and correlation behavior, and that backend error bodies/statuses are not flattened into false success.

**Acceptance Scenarios**:

1. **Given** a lifecycle mutation request is submitted from the console, **When** the application boundary sends it to the backend, **Then** it forwards the user authorization context.
2. **Given** the detail view has a current order version marker, **When** a lifecycle mutation is submitted, **Then** the current version marker is sent as the conditional update value.
3. **Given** a lifecycle mutation is submitted, **When** an idempotency key is already present from the client flow, **Then** the application boundary forwards it unchanged.
4. **Given** a lifecycle mutation is submitted and no idempotency key is present, **When** the application boundary prepares the backend request, **Then** it generates one for that single mutation attempt.
5. **Given** a correlation identifier is present, **When** a lifecycle or history request is proxied, **Then** the application boundary forwards it; otherwise it uses the application convention for generating one when supported.
6. **Given** the backend returns a lifecycle error, **When** the application boundary responds to the browser, **Then** it preserves the backend status code and error body shape closely enough for the UI to show the correct user-facing state.

---

### User Story 5 - Update Metadata Separately From Lifecycle (Priority: P2)

An operator needs to edit payment metadata such as support notes without changing the payment status or confusing the action with authorize/capture/cancel/refund.

**Why this priority**: Metadata is useful operational context, but mixing it with lifecycle actions would create a misleading product workflow and unnecessary state-machine risk.

**Independent Test**: Can be verified by editing metadata from the detail page, confirming the payment status stays unchanged, and confirming detail refreshes after success.

**Acceptance Scenarios**:

1. **Given** a user with metadata update permission opens payment detail, **When** they choose to edit metadata, **Then** the metadata editor is presented separately from lifecycle action controls.
2. **Given** metadata key/value pairs are submitted with the current version marker, **When** the backend accepts the update, **Then** the detail view refreshes and the lifecycle status remains unchanged.
3. **Given** the metadata update receives a stale-state response, **When** the console handles the response, **Then** it informs the user that the payment changed and reloads current detail before further editing.
4. **Given** the metadata update fails validation, **When** the response is shown, **Then** the user can correct the metadata input without seeing lifecycle-action success feedback.

---

### User Story 6 - Respect Roles and Permissions in the Console (Priority: P2)

A read-only or audit user needs to inspect payment lifecycle information without seeing misleading mutation affordances, while the backend remains the final enforcement point.

**Why this priority**: UI hiding is not authorization, but misleading action controls for read-only users create confusion and encourage failed or unauthorized operations.

**Independent Test**: Can be verified by opening the console with read/audit and lifecycle-capable contexts and checking that controls are hidden or disabled only when role context is known, and forbidden responses are shown as access-denied feedback.

**Acceptance Scenarios**:

1. **Given** the application has role information showing a read-only or audit actor, **When** payment detail renders, **Then** lifecycle mutation controls are hidden or disabled.
2. **Given** the application lacks sufficient role information, **When** payment detail renders, **Then** the console still avoids impossible state-based actions and lets backend authorization decide final permission.
3. **Given** the backend returns forbidden for a lifecycle or metadata mutation, **When** the console handles the response, **Then** the user sees access-denied feedback and no success state.
4. **Given** a read/audit actor can access history, **When** they open payment detail, **Then** they can inspect history without mutation controls.

---

### Edge Cases

- What happens when detail loads but history fails? The detail summary remains usable and the history area shows a scoped error state.
- What happens when history is empty? The console shows an empty history message and does not imply an audit failure.
- What happens when the user attempts an action on stale state? The console shows that the payment changed, reloads detail/history, and does not automatically retry the business action.
- What happens when the backend returns `422 invalid_transition` after the UI offered an action? The console shows the domain error and refreshes current state to remove stale controls.
- What happens when a validation error is returned for capture/refund amount or reason? The console keeps the entered values available for correction and displays the validation feedback.
- What happens when the backend returns `409 idempotency_conflict`? The console shows that the action could not be safely replayed and does not present the result as successful.
- What happens when the payment order is not found? The console shows a not-found state for the detail view and does not render mutation controls.
- What happens when the backend is unavailable? The console shows a backend-unavailable state and does not clear existing user input unless the user leaves the page.
- What happens when the current status is terminal? The console shows terminal status clearly and offers no lifecycle mutation action.
- What happens when actor information exists in history but may be sensitive? The console shows only safe display values returned by the backend and never displays raw tokens or credentials.

## Requirements *(mandatory)*

### Functional Requirements

**Lifecycle Detail Summary:**

- **FR-SUMMARY-001**: The system MUST show the current lifecycle status for a payment order detail view.
- **FR-SUMMARY-002**: The system MUST show a status badge for each supported status: `CREATED`, `AUTHORIZED`, `CAPTURED`, `CANCELLED`, `EXPIRED`, and `REFUNDED`.
- **FR-SUMMARY-003**: The system MUST show amount and currency in the payment detail lifecycle summary.
- **FR-SUMMARY-004**: The system MUST show authorized timestamp and expiration timestamp when those values are present.
- **FR-SUMMARY-005**: The system MUST show captured timestamp and captured amount when those values are present.
- **FR-SUMMARY-006**: The system MUST show cancelled timestamp and cancellation reason when those values are present.
- **FR-SUMMARY-007**: The system MUST show refunded timestamp, refunded amount, and refund reason when those values are present.
- **FR-SUMMARY-008**: The system MUST show current metadata on the payment detail page.

**Lifecycle History:**

- **FR-HISTORY-001**: The system MUST load lifecycle history for the current merchant and payment order from the existing status history contract.
- **FR-HISTORY-002**: The system MUST render lifecycle history as an oldest-first chronological timeline or list.
- **FR-HISTORY-003**: Each rendered history entry MUST show from status, to status, and action timestamp.
- **FR-HISTORY-004**: Each rendered history entry MUST show actor information when the backend returns a safe actor value.
- **FR-HISTORY-005**: Each rendered history entry MUST show reason, amount, and PSP reference when those values are returned by the backend.
- **FR-HISTORY-006**: The system MUST show a clear empty state when history has no entries.
- **FR-HISTORY-007**: The system MUST show separate loading and error states for lifecycle history.

**State-Aware Actions:**

- **FR-ACTION-001**: The system MUST derive available lifecycle actions from the current payment status.
- **FR-ACTION-002**: For `CREATED`, the system MUST offer authorize and cancel to lifecycle-capable operators.
- **FR-ACTION-003**: For `AUTHORIZED`, the system MUST offer capture and cancel to lifecycle-capable operators.
- **FR-ACTION-004**: For `CAPTURED`, the system MUST offer refund to lifecycle-capable operators.
- **FR-ACTION-005**: For `CANCELLED`, `EXPIRED`, and `REFUNDED`, the system MUST offer no lifecycle mutation action.
- **FR-ACTION-006**: The system MUST NOT offer lifecycle controls for impossible state transitions.
- **FR-ACTION-007**: The system MUST require confirmation before capture, cancel, or refund is submitted.
- **FR-ACTION-008**: The system MUST collect capture amount only for capture and use full capture as the default when the amount is omitted.
- **FR-ACTION-009**: The system MUST collect refund amount and refund reason only for refund and use full refund as the default when the amount is omitted.
- **FR-ACTION-010**: The system MUST collect cancellation reason only for cancel when a reason is provided by the operator.
- **FR-ACTION-011**: After a successful lifecycle action, the system MUST reload payment detail and lifecycle history.
- **FR-ACTION-012**: After a failed lifecycle action, the system MUST show a clear user-facing failure message and MUST NOT show action success.

**Application Boundary and Headers:**

- **FR-PROXY-001**: The application boundary MUST forward authorization context for lifecycle detail, history, metadata, and mutation requests.
- **FR-PROXY-002**: For lifecycle mutation requests, the application boundary MUST forward an existing idempotency key or generate one when absent.
- **FR-PROXY-003**: For lifecycle mutation requests, the application boundary MUST send the current payment order version marker as the conditional update value.
- **FR-PROXY-004**: For metadata updates, the application boundary MUST send the current payment order version marker as required by the existing metadata update contract.
- **FR-PROXY-005**: The application boundary MUST forward a correlation identifier when present or generate/use the application convention for one when supported.
- **FR-PROXY-006**: The application boundary MUST preserve backend status codes and lifecycle error body shape closely enough for the UI to distinguish validation, invalid transition, forbidden, not found, stale state, idempotency conflict, and backend unavailable states.

**Metadata Update:**

- **FR-METADATA-001**: The system MUST provide a separate metadata editing flow for key/value metadata.
- **FR-METADATA-002**: Metadata update MUST be visually and behaviorally separate from lifecycle actions.
- **FR-METADATA-003**: Metadata update MUST NOT imply or display a lifecycle status change unless the refreshed detail from the backend has changed for another reason.
- **FR-METADATA-004**: After successful metadata update, the system MUST refresh payment detail.
- **FR-METADATA-005**: Metadata update stale-state handling MUST inform the user that the payment changed and reload current detail before further editing.

**Roles, Permissions, and Error UX:**

- **FR-ROLE-001**: When role information is available, the system MUST hide or disable lifecycle mutation controls for read-only and audit actors.
- **FR-ROLE-002**: When role information is incomplete, the system MUST avoid claiming permission and MUST rely on backend enforcement for final authorization.
- **FR-ROLE-003**: Forbidden backend responses MUST be shown as access-denied feedback.
- **FR-ERROR-001**: The system MUST show a distinct user-facing message for invalid transition errors.
- **FR-ERROR-002**: The system MUST show a distinct user-facing message for validation errors.
- **FR-ERROR-003**: The system MUST show a distinct user-facing message for not-found results.
- **FR-ERROR-004**: The system MUST show a distinct user-facing message for stale version or precondition failure results.
- **FR-ERROR-005**: The system MUST show a distinct user-facing message for idempotency conflict results.
- **FR-ERROR-006**: The system MUST show a distinct user-facing message for backend unavailable or unexpected backend error states.
- **FR-ERROR-007**: On stale-state responses, the system MUST reload detail and history and MUST NOT automatically retry the business action.

### Non-Functional Requirements

- **NFR-001**: The primary lifecycle detail content SHOULD become understandable to a user within 2 seconds under normal local development conditions once backend responses are available.
- **NFR-002**: The action area MUST remain deterministic for the same payment status and role context so users do not see fluctuating controls without a data refresh.
- **NFR-003**: The console MUST avoid displaying sensitive authentication tokens, idempotency key hashes, raw credentials, or other secret values in detail, history, or errors.
- **NFR-004**: User-facing errors MUST preserve enough correlation information for support troubleshooting when the backend provides a safe correlation identifier.
- **NFR-005**: The feature MUST remain application-focused and MUST NOT create automated test implementation as a deliverable.
- **NFR-006**: The feature MUST preserve feature 009 lifecycle semantics and MUST NOT introduce new status transitions.

## Quality and Architecture Impact *(mandatory)*

### Tester-Led Risk Notes

- **Misleading Action Availability**: The highest UX risk is offering actions that the current lifecycle status cannot support. The state-to-action matrix must be visible and deterministic.
- **Stale-State Risk**: A user may act on an old version of a payment order. The console must explain stale state and refresh instead of retrying automatically.
- **Header Boundary Risk**: Lifecycle actions depend on authorization, idempotency, conditional update, and correlation signals. Dropping them at the application boundary can cause false failures or unsafe retries.
- **Role-Affordance Risk**: Hiding controls is not authorization, but showing mutation controls to known read-only actors is misleading. Backend enforcement remains final.
- **History Interpretation Risk**: Unclear ordering makes audit review harder. This spec resolves history ordering as oldest-first chronological display.
- **Metadata Confusion Risk**: Metadata update must not look like a lifecycle transition or be placed in the same confirmation flow as capture/cancel/refund.
- **Error Flattening Risk**: If all backend errors become a generic message, users cannot distinguish invalid transition, stale state, forbidden access, validation, or idempotency conflict.

### Modulith Impact *(required for backend-relevant features; otherwise state N/A)*

- **Module Ownership**: Existing payment lifecycle ownership remains with the payment module from feature 009; this feature does not introduce a new backend module.
- **Module API Impact**: No new payment lifecycle capability is required beyond existing lifecycle detail, mutation, metadata update, and history contracts. Any backend adjustment must only support the application display contract if existing responses omit already-defined lifecycle fields.
- **Dependency Impact**: No new module dependency is intended. Application boundary behavior should consume existing payment lifecycle contracts.
- **Event Impact**: No events, webhooks, Kafka, or asynchronous lifecycle behavior are introduced.
- **Module Test Impact**: No new backend test classes are feature scope; existing module verification remains a later implementation/regression concern, not a deliverable of `/speckit.specify`.

### Security, Data, and Observability Impact

- Authentication context must be forwarded through the application boundary for all protected payment detail, history, metadata, and lifecycle requests.
- Backend authorization remains the final enforcement point; the console only controls user affordances and feedback.
- The console must not display raw bearer tokens, raw credentials, or sensitive internal values.
- Lifecycle history displays safe actor, reason, amount, PSP reference, and timestamp fields only when returned by the backend.
- Stale-state handling must prevent accidental execution of a different business action by avoiding automatic retries.
- Error messages must remain user-facing while preserving safe correlation information for support diagnostics.
- The feature must not implement complete OAuth/OIDC application integration.

### Key Entities *(include if feature involves data)*

- **Payment Order Detail**: The user-facing representation of a payment order, including lifecycle status, amount, currency, lifecycle timestamps, lifecycle amounts/reasons, metadata, and current version marker.
- **Lifecycle Status History Entry**: A chronological record of a payment status transition with from status, to status, timestamp, optional actor, and optional action context returned by the backend.
- **Lifecycle Action Request**: A user-confirmed action submission for authorize, capture, cancel, or refund, including only fields supported by that action and the current payment version marker.
- **Metadata Update Request**: A non-lifecycle update of payment metadata key/value pairs using the current conditional update rules.
- **User Feedback State**: The visible result of loading, success, empty, forbidden, validation, invalid transition, stale state, idempotency conflict, not found, or backend unavailable outcomes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can identify the current lifecycle status and relevant lifecycle facts for a payment order in under 10 seconds without using raw API output.
- **SC-002**: For each of the six lifecycle statuses, the visible action set matches the specified state matrix in 100% of reviewed status cases.
- **SC-003**: A lifecycle-capable operator can perform an allowed action from the detail screen and see refreshed detail plus refreshed history after success.
- **SC-004**: Terminal statuses show zero lifecycle mutation controls.
- **SC-005**: Lifecycle history is presented oldest-first and includes from/to status plus timestamp for every returned history entry.
- **SC-006**: Stale-state responses result in user feedback and a detail/history refresh, with zero automatic retry of the lifecycle business action.
- **SC-007**: Forbidden responses are shown as access-denied feedback and never as successful lifecycle or metadata updates.
- **SC-008**: Metadata can be updated from a separate flow and the displayed lifecycle status does not change as a result of that metadata flow.
- **SC-009**: Users receive distinguishable feedback for invalid transition, validation, forbidden, not found, stale state, idempotency conflict, and backend unavailable outcomes.
- **SC-010**: The completed feature introduces no new lifecycle state, no new lifecycle transition, and no payment business capability beyond feature 009 semantics.

## Assumptions

- Feature 009 lifecycle endpoints, metadata update, history contract, version marker, and error shape exist or are the immediate dependency for this console feature.
- Lifecycle history should be displayed oldest-first because audit and support users read status transitions as a story from original state to current state.
- Capture and refund amount fields default to full available amount when omitted, matching feature 009 behavior, while still allowing the user to provide an amount where supported.
- Cancellation and refund reasons are optional user inputs unless backend validation requires otherwise.
- The application boundary may generate idempotency keys for lifecycle actions when the browser flow does not provide one.
- Role context may be incomplete in the frontend; backend authorization remains authoritative.
- All timestamps displayed by the console are treated as backend-provided instants and should be shown consistently to the user.
- Existing payment order detail navigation already provides merchant ID and payment order ID.
- This specification does not require creating test deliverables, even though acceptance scenarios are included to make requirements verifiable.

## Open Questions / Clarifications

No unresolved clarification blocks `/speckit.plan`. The specification resolves prior discovery ambiguities as follows:

1. History order is oldest-first chronological display.
2. The application boundary forwards an idempotency key when present and generates one when absent for lifecycle mutations.
3. Capture/refund amount is optional with full amount as default; the UI may allow editing where the backend supports it.
4. Metadata editing is a separate detail-page flow rather than a lifecycle action.
5. Role-aware UI is conservative when role context is incomplete and relies on backend enforcement for final authorization.
