# Feature Specification: Merchant Registry and Activation for Platform Operators

**Feature Branch**: `002-merchant-registry-activation`

**Created**: 2026-05-18

**Status**: Draft

**Input**: User description: "Define Phase 1 for the Payment Quality Engineering Lab: Merchant Registry and Activation for Platform Operators. Use the accepted BA Discovery Pack as the product-discovery basis."

## Business Purpose *(mandatory)*

Phase 1 establishes the first real business domain capability for the Payment Quality Engineering Lab: a merchant registry and lifecycle foundation managed by authenticated platform operators.

This phase comes before Create Payment Order because future payment orders must belong to a real merchant ownership boundary rather than a hardcoded or fake merchant assumption. The platform must be able to recognize, store, activate, suspend, and retrieve merchants as valid business participants before payment activity begins.

This feature creates:

- The first real domain boundary in the product.
- Ownership context for future payment capabilities.
- Meaningful persistence, validation, and duplicate-conflict behavior.
- A realistic admin/operator user journey in the dashboard foundation.
- The first narrow, functional authentication and authorization boundary for business data.
- Rich test-design material for validation, lifecycle, security, persistence, modularity, and parallel-safe data generation.

## Actors *(mandatory)*

- **Platform Operator**: Internal authenticated user who manages merchant records, creates merchants, reviews the merchant list, activates draft merchants, and suspends active merchants.
- **Unauthenticated User**: User without a valid session or token. May access only the public technical status capability and must not access merchant management behavior.
- **Merchant Human Operator**: Future actor preserved for later merchant-facing access. Not implemented in Phase 1.
- **Merchant Machine-to-Machine API Client**: Future actor preserved for later payment API use cases. Not implemented in Phase 1.
- **Operations Analyst**: Future actor preserved for later support and investigation workflows. Not implemented in Phase 1.
- **Admin/Security Reviewer**: Stakeholder who reviews access boundaries, role assumptions, and auditability. A complete admin/security platform is not implemented in Phase 1.

## Scope *(mandatory)*

### In Scope

- Create a merchant record with a stable, unique merchant reference and display name.
- Validate merchant references as trimmed, case-insensitive business identifiers using 3-64 uppercase letters, numbers, and hyphens after normalization.
- Validate display names as trimmed human-readable names from 2-120 characters.
- Assign newly created merchants the initial status `DRAFT`.
- Retrieve a merchant by internal ID.
- List merchants for platform-operator review using a stable newest-first order and a first page of up to 50 merchants.
- Activate a draft merchant through the lifecycle transition `DRAFT -> ACTIVE`.
- Suspend an active merchant through the lifecycle transition `ACTIVE -> SUSPENDED`.
- Reject invalid lifecycle transitions with clear, testable outcomes.
- Store merchant records using durable platform persistence.
- Protect merchant management behavior so only authorized platform operators can use it.
- Functionally use the configured Keycloak 26.6.1 identity service for the narrow platform-operator login and authorization boundary.
- Keep the existing technical status capability public.
- Provide a minimal dashboard journey at `/admin/merchants` for platform operators.
- Provide an empty state, create action, merchant table, validation feedback, duplicate-conflict feedback, success feedback, loading state, and error state.
- Treat suspend as mandatory in Phase 1 so the minimal lifecycle is complete.
- Preserve parallel-safe merchant test data conventions using unique merchant references such as `MERCH-{testRunId}-{workerId}-{uuid}`.
- Preserve the future dependency that payment orders will require a real merchant ownership boundary.

### Out of Scope

- Payment order creation.
- Payment status read model.
- PSP integration or PSP mock flows.
- Kafka, external messaging, or event broker integration.
- Refunds.
- Settlement.
- Reconciliation.
- KYC or complete onboarding product.
- Merchant machine-to-machine credentials.
- Client Credentials Flow.
- Complete merchant self-service portal.
- Complete admin platform.
- Country or currency rules.
- Settlement account configuration.
- Pricing or routing rules.
- Merchant contact email.
- Merchant default currency.
- Merchant country code.
- Merchant reference mutation after creation.
- Separate merchant detail page unless a later clarification explicitly justifies it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create Draft Merchant (Priority: P1)

A platform operator logs in, opens the merchant administration area, and creates a merchant with a unique merchant reference and display name. The merchant appears in the registry with status `DRAFT`.

**Why this priority**: Creating a merchant is the core capability that establishes the first business domain object and future payment ownership boundary.

**Independent Test**: Can be fully tested by signing in as a platform operator, opening the merchant registry, creating a merchant with unique valid data, and confirming the merchant appears in the list with status `DRAFT`.

**Acceptance Scenarios**:

1. **Given** an authenticated platform operator is on the merchant registry page with no merchants, **When** they create a merchant with a unique reference and display name, **Then** the merchant is stored and appears in the list with status `DRAFT`.
2. **Given** an authenticated platform operator submits a blank merchant reference or blank display name, **When** they attempt to create a merchant, **Then** the merchant is not created and field-level validation feedback is shown.
3. **Given** a merchant already exists with a merchant reference, **When** an authenticated platform operator tries to create another merchant with the same reference, **Then** the second merchant is not created and duplicate-conflict feedback is shown.
4. **Given** a merchant is created, **When** the operator reviews or retrieves it, **Then** the merchant has an internal ID, creation time, and update time.
5. **Given** two create attempts use the same normalized merchant reference at nearly the same time, **When** both attempts are processed, **Then** only one merchant is created and the other attempt receives duplicate-conflict feedback.

---

### User Story 2 - Activate Merchant (Priority: P2)

A platform operator activates a draft merchant so the merchant becomes a valid business participant for future payment capabilities.

**Why this priority**: Activation introduces the lifecycle rule future payment order creation will depend on, while staying below payment behavior.

**Independent Test**: Can be tested by creating or locating a `DRAFT` merchant, activating it, and confirming the status changes to `ACTIVE` while invalid activation attempts are rejected.

**Acceptance Scenarios**:

1. **Given** a merchant exists in `DRAFT`, **When** an authenticated platform operator activates the merchant, **Then** the merchant status becomes `ACTIVE`, its update time changes, and the updated status is visible in the registry.
2. **Given** a merchant already exists in `ACTIVE`, **When** an authenticated platform operator attempts to activate it again, **Then** the system rejects the transition and keeps the merchant in `ACTIVE`.
3. **Given** a merchant exists in `SUSPENDED`, **When** an authenticated platform operator attempts to activate it, **Then** the system rejects the transition and keeps the merchant in `SUSPENDED`.

---

### User Story 3 - Suspend Active Merchant (Priority: P3)

A platform operator suspends an active merchant so future payment activity can be blocked for that merchant when payment capabilities are added later.

**Why this priority**: Suspension completes the minimal Phase 1 lifecycle and creates useful state-transition and future eligibility test material.

**Independent Test**: Can be tested by creating and activating a merchant, suspending it, and confirming the merchant appears as `SUSPENDED` while invalid suspension attempts are rejected.

**Acceptance Scenarios**:

1. **Given** a merchant exists in `ACTIVE`, **When** an authenticated platform operator suspends the merchant, **Then** the merchant status becomes `SUSPENDED`, its update time changes, and the updated status is visible in the registry.
2. **Given** a merchant exists in `DRAFT`, **When** an authenticated platform operator attempts to suspend it, **Then** the system rejects the transition and keeps the merchant in `DRAFT`.
3. **Given** a merchant exists in `SUSPENDED`, **When** an authenticated platform operator attempts to suspend it again, **Then** the system rejects the transition and keeps the merchant in `SUSPENDED`.

---

### User Story 4 - Review Merchant Registry (Priority: P4)

A platform operator reviews known merchants in a minimal registry view and can retrieve merchant information by ID when needed.

**Why this priority**: Listing and retrieval make the registry usable for operators and provide a foundation for future payment lookup and support workflows.

**Independent Test**: Can be tested by creating multiple merchants with unique references, listing merchants, and retrieving a selected merchant by ID.

**Acceptance Scenarios**:

1. **Given** no merchants exist, **When** an authenticated platform operator opens the merchant registry, **Then** an empty state explains that no merchants have been registered yet.
2. **Given** one or more merchants exist, **When** an authenticated platform operator opens the merchant registry, **Then** the list shows merchant reference, display name, status, and creation time for each visible merchant in stable newest-first order.
3. **Given** a merchant exists, **When** an authenticated platform operator retrieves that merchant by ID, **Then** the merchant details returned match the stored merchant identity and lifecycle status.
4. **Given** a merchant ID does not exist, **When** an authenticated platform operator retrieves that merchant by ID, **Then** the system reports that the merchant was not found without creating or altering merchant data.
5. **Given** a merchant ID is malformed, **When** an authenticated platform operator requests that merchant, **Then** the system rejects the request without creating or altering merchant data.
6. **Given** merchants have been created and the platform is restarted normally in the local lab, **When** an authenticated platform operator opens the merchant registry, **Then** previously created merchants remain visible.
7. **Given** the merchant registry is loading, **When** the platform has not returned the list yet, **Then** the operator sees a loading state rather than stale or misleading data.
8. **Given** the merchant registry cannot load because of a service failure, **When** the operator opens the page, **Then** the operator sees an error state that does not imply merchant data was changed.

---

### User Story 5 - Enforce Merchant Access Boundary (Priority: P5)

The platform protects merchant management behavior so unauthenticated users and users without platform-operator authority cannot create, list, activate, or suspend merchants.

**Why this priority**: Merchant data is business data, and Phase 1 is the first functional security boundary for the product.

**Independent Test**: Can be tested by attempting merchant management actions without authentication, with insufficient authority, and with platform-operator authority.

**Acceptance Scenarios**:

1. **Given** a user is unauthenticated, **When** they attempt to access merchant management behavior, **Then** access is denied and no merchant data is disclosed or changed.
2. **Given** an authenticated user lacks the required platform-operator authority, **When** they attempt to create, list, activate, or suspend merchants, **Then** access is denied and no merchant data is changed.
3. **Given** an authenticated platform operator has merchant read authority, **When** they list or retrieve merchants, **Then** they can view merchant records.
4. **Given** an authenticated platform operator has merchant create authority, **When** they submit valid merchant creation data, **Then** the merchant can be created.
5. **Given** an authenticated platform operator has merchant status-update authority, **When** they perform a valid lifecycle transition, **Then** the merchant status is updated.
6. **Given** any user accesses the technical status capability, **When** the platform is running, **Then** the status capability remains publicly available and does not expose merchant data.

---

### Edge Cases

- Merchant reference is missing, blank, whitespace-only, shorter than 3 characters, longer than 64 characters, or contains characters other than letters, numbers, and hyphens.
- Display name is missing, blank, whitespace-only, shorter than 2 characters, or longer than 120 characters.
- Merchant reference differs only by case or surrounding whitespace from an existing reference; normalized references are treated as duplicates.
- Two create requests attempt to use the same merchant reference at nearly the same time.
- Merchant ID is malformed or does not exist.
- Activation is requested for an `ACTIVE` or `SUSPENDED` merchant.
- Suspension is requested for a `DRAFT` or `SUSPENDED` merchant.
- A user has read authority but not create or status-update authority.
- A user has create authority but not status-update authority.
- Authentication is missing, expired, malformed, or valid but lacks required authority.
- Merchant list is empty.
- Merchant list contains more than 50 records and only the first stable newest-first page is needed in Phase 1.
- Network or service failure occurs while the operator creates, activates, suspends, or lists merchants.
- Tests run in parallel and generate merchants with unique namespaced references.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow an authenticated platform operator with merchant create authority to create a merchant using `merchantReference` and `displayName`.
- **FR-002**: System MUST assign each created merchant an internal `merchantId` that is distinct from the merchant reference.
- **FR-003**: System MUST assign newly created merchants the initial status `DRAFT`.
- **FR-004**: System MUST require `merchantReference` and reject missing, blank, or invalid references with clear validation feedback.
- **FR-005**: System MUST require `displayName` and reject missing, blank, or invalid display names with clear validation feedback.
- **FR-006**: System MUST accept merchant references only after trimming and normalizing them to uppercase, with 3-64 letters, numbers, or hyphens.
- **FR-007**: System MUST accept display names only after trimming them, with 2-120 characters.
- **FR-008**: System MUST ensure normalized `merchantReference` is unique across merchants.
- **FR-009**: System MUST reject duplicate merchant references with conflict feedback that allows the operator or tester to distinguish duplicates from general validation failures.
- **FR-010**: System MUST keep `merchantReference` stable and non-mutable in Phase 1.
- **FR-011**: System MUST store `createdAt` and `updatedAt` timestamps for each merchant.
- **FR-012**: System MUST change `updatedAt` when a merchant lifecycle transition succeeds.
- **FR-013**: System MUST allow an authenticated platform operator with merchant read authority to retrieve a merchant by `merchantId`.
- **FR-014**: System MUST report a not-found outcome when an authenticated platform operator requests an unknown merchant ID.
- **FR-015**: System MUST reject malformed merchant IDs without creating or altering merchant data.
- **FR-016**: System MUST allow an authenticated platform operator with merchant read authority to list merchants.
- **FR-017**: Merchant list results MUST show at least merchant reference, display name, status, and creation time.
- **FR-018**: Merchant list results MUST use stable newest-first ordering and return an initial page of up to 50 merchants in Phase 1.
- **FR-019**: System MUST allow an authenticated platform operator with merchant status-update authority to activate a merchant only from `DRAFT` to `ACTIVE`.
- **FR-020**: System MUST allow an authenticated platform operator with merchant status-update authority to suspend a merchant only from `ACTIVE` to `SUSPENDED`.
- **FR-021**: System MUST reject lifecycle transitions other than `DRAFT -> ACTIVE` and `ACTIVE -> SUSPENDED` without changing merchant status.
- **FR-022**: System MUST expose merchant management behavior through the Phase 1 operations `POST /api/merchants`, `GET /api/merchants/{id}`, `GET /api/merchants`, `POST /api/merchants/{id}/activate`, and `POST /api/merchants/{id}/suspend`.
- **FR-023**: System MUST keep the existing public technical status behavior available without requiring merchant authentication or disclosing merchant data.
- **FR-024**: System MUST provide a dashboard route `/admin/merchants` for authenticated platform operators.
- **FR-025**: The merchant registry UI MUST show a page title `Merchants`.
- **FR-026**: The merchant registry UI MUST show an empty state when no merchants exist.
- **FR-027**: The merchant registry UI MUST provide a create merchant action and minimal form for merchant reference and display name.
- **FR-028**: The merchant registry UI MUST show merchant rows with merchant reference, display name, status, and creation time when merchants exist.
- **FR-029**: The merchant registry UI MUST provide operator-visible validation feedback, duplicate-conflict feedback, success feedback, loading state, and error state.
- **FR-030**: System MUST deny unauthenticated access to merchant management behavior.
- **FR-031**: System MUST deny authenticated users who lack the relevant authority for create, read, or status-update merchant actions.
- **FR-032**: System MUST distinguish missing authentication from insufficient authorization in externally observable behavior so testers can verify 401-style and 403-style cases.
- **FR-033**: System MUST support the platform-operator authority concepts `platform:merchants:create`, `platform:merchants:read`, and `platform:merchants:update-status`.
- **FR-034**: The platform-operator dashboard sign-in flow MUST use an authorization-code-with-PKCE style browser login through the configured identity service.
- **FR-035**: Merchant management requests MUST carry verifiable access-token authority information so the business boundary can distinguish unauthenticated, insufficient-authority, and authorized platform-operator requests.
- **FR-036**: System MUST not implement payment order creation, payment status read models, PSP behavior, Kafka/events, settlement, reconciliation, Client Credentials Flow, merchant machine-to-machine credentials, complete merchant self-service, or complete admin platform behavior in Phase 1.

### Non-Functional Requirements

- **NFR-001**: Platform operators SHOULD be able to complete the create-merchant task from the registry page in under 2 minutes during normal local lab use.
- **NFR-002**: Merchant create, list, retrieve, activate, and suspend behavior MUST provide deterministic outcomes suitable for repeated automated verification.
- **NFR-003**: Automated tests MUST be able to create merchants with unique namespaced references without relying on one global shared merchant.
- **NFR-004**: Merchant data MUST remain durable across normal application restarts in the local lab environment.
- **NFR-005**: Duplicate-reference handling MUST remain reliable when two requests attempt to create the same merchant reference close together.
- **NFR-006**: Security behavior MUST be testable for missing authentication, invalid authentication, insufficient authority, and valid platform-operator authority.
- **NFR-007**: The dashboard journey MUST expose stable, accessible labels or controls so automated browser tests can exercise the business flow without relying on fragile visual-only selectors.
- **NFR-008**: The feature MUST preserve module-boundary clarity so future payment capabilities can depend on merchant eligibility through an explicit merchant boundary rather than direct internal data coupling.
- **NFR-009**: Error and feedback messages MUST be clear enough for a platform operator to understand whether the problem is validation, duplicate reference, missing access, insufficient authority, not found, or invalid lifecycle transition.

## Quality and Architecture Impact *(mandatory)*

### Tester-Led Risk Notes

- Validation risk: merchant reference and display name rules must be specific enough to avoid ambiguous tests.
- Duplicate risk: unique merchant reference must be enforced consistently, including near-simultaneous attempts.
- Lifecycle risk: invalid transitions must not silently succeed or mutate state.
- Security risk: merchant business data must not remain anonymous after Phase 1.
- Authorization risk: read, create, and status-update authority must be separable enough to test 403 behavior.
- Persistence risk: the feature introduces the first real business data, so storage and restart behavior become visible quality concerns.
- UI risk: the dashboard must not imply complete admin, merchant self-service, payment operations, or auth platform functionality.
- Parallel-test risk: shared merchant references or global fixtures would create order-dependent tests.
- Scope-creep risk: adding payment order, currency/country, settlement, KYC, machine credentials, or PSP behavior would blur the Phase 1 milestone.

### Modulith Impact *(required for backend-relevant features; otherwise state N/A)*

- **Module Ownership**: Merchant is the owning business module for merchant registry, lifecycle, validation, and eligibility concepts.
- **Module API Impact**: The merchant module should expose only the merchant behaviors needed by external callers and future payment capabilities: create, read/list, activate, suspend, and later eligibility checks. Internal persistence and lifecycle implementation details should remain private to the module.
- **Dependency Impact**: Future payment capabilities may depend on merchant eligibility through an explicit merchant boundary. The merchant module must not depend on future payment modules.
- **Event Impact**: No external broker or asynchronous event flow is part of Phase 1. Lifecycle changes may be modeled directly for now; any event-based integration is deferred until a real downstream need exists.
- **Module Test Impact**: Architecture verification must continue to run, and merchant module tests should verify lifecycle behavior, public boundary behavior, and absence of forbidden payment/PSP/Kafka dependencies.

### Security, Data, and Observability Impact

- Merchant management is protected business behavior for platform operators.
- The existing technical status behavior remains public and must not reveal merchant data.
- The dashboard sign-in journey uses the configured Keycloak 26.6.1 identity boundary for platform operators.
- The browser login model is OAuth 2.1 Authorization Code Flow with PKCE.
- Merchant management requests are protected by JWT access-token validation at the service boundary.
- Merchant management access is controlled by create, read, and status-update authority concepts.
- Missing authentication and insufficient authorization must be externally distinguishable for testing.
- Merchant records require durable storage with a uniqueness guarantee for merchant reference.
- Merchant references should support parallel-safe namespacing, for example `MERCH-{testRunId}-{workerId}-{uuid}`.
- Merchant lifecycle changes should update the merchant's updated timestamp.
- Operator-facing failures should be visible as validation, duplicate conflict, not found, invalid transition, missing authentication, insufficient authority, or general service error.
- Logs or audit-friendly records should allow support and testing to trace merchant creation and status transitions without exposing secrets.

### Key Entities *(include if feature involves data)*

- **Merchant**: A business participant recognized by the platform before payment activity begins. Key attributes are `merchantId`, `merchantReference`, `displayName`, `status`, `createdAt`, and `updatedAt`.
- **Merchant Status**: Lifecycle state for a merchant. Phase 1 states are `DRAFT`, `ACTIVE`, and `SUSPENDED`.
- **Platform Operator Authority**: Permission concept controlling whether an authenticated platform operator can create merchants, read merchants, or update merchant status.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A platform operator can create a valid draft merchant from the dashboard in under 2 minutes during a normal local lab walkthrough.
- **SC-002**: 100% of created merchants have a unique internal ID, stable merchant reference, display name, status, creation time, and update time visible through merchant retrieval or registry review.
- **SC-003**: Duplicate merchant reference attempts are rejected consistently in both single-request and near-simultaneous request scenarios.
- **SC-004**: 100% of invalid lifecycle transitions leave the merchant in its previous status and produce a clear operator/tester-visible rejection.
- **SC-005**: An unauthenticated user cannot create, list, retrieve, activate, or suspend merchants, while the public technical status capability remains accessible.
- **SC-006**: A user without the required authority cannot perform merchant actions outside their granted authority, and tests can distinguish missing authentication from insufficient authorization.
- **SC-007**: The merchant registry journey can be verified end-to-end: sign in, open `Admin > Merchants`, observe empty state, create merchant, see `DRAFT`, activate merchant, see `ACTIVE`, suspend merchant, and see `SUSPENDED`.
- **SC-008**: Automated tests can run repeatedly with unique merchant references and do not depend on one shared global merchant.
- **SC-009**: Phase 1 completion introduces no payment order creation, payment status read model, PSP integration, Kafka/events, settlement, reconciliation, Client Credentials Flow, or complete admin/self-service portal behavior.

## Assumptions

- Platform operators are internal users, not merchant users.
- The initial Phase 1 dashboard is intended for desktop or standard responsive dashboard use, not a specialized mobile flow.
- Merchant reference comparison trims and normalizes references to uppercase, so references that differ only by case or surrounding whitespace are duplicates.
- Merchant list ordering is stable newest-first for Phase 1.
- Merchant deletion is intentionally absent in Phase 1.
- A separate merchant detail page is not required for Phase 1 unless later clarification justifies it.
- Client Credentials Flow waits until merchant machine-to-machine payment API use cases exist.
- Country, currency, settlement, KYC, pricing, routing, and merchant contact fields are deferred to later product capabilities.

## Open Questions for `/speckit.clarify`

- Confirm whether the default normalized merchant reference rule, 3-64 uppercase letters/numbers/hyphens, should remain unchanged.
- Confirm whether the default display name rule, 2-120 trimmed characters, should remain unchanged.
- Confirm whether the Phase 1 merchant list first page of 50 newest-first records is sufficient.
- Confirm the local platform-operator identity setup for the first login journey.
- Confirm whether `platform:merchants:update-status` should cover both activate and suspend, or whether later phases should split these authorities.
