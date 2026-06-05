# Research: Payment Lifecycle Operations Console

## Decision: Keep Feature 010 Application-Only

**Decision**: Implement the feature through Nuxt payment detail UI, Pinia/Zod state contracts, and Nuxt server proxy behavior. Backend work is limited to checking and, only if necessary, exposing fields already required by the application display contract.

**Rationale**: The feature exists to make Feature 009 lifecycle behavior usable by operators, readers, auditors, and support users. The highest risks are display accuracy, action affordances, stale-state handling, proxy header preservation, and error feedback, not new lifecycle domain behavior.

**Alternatives considered**:
- Add new backend lifecycle semantics: rejected because the spec explicitly preserves Feature 009 semantics and prohibits new transitions.
- Add test-framework deliverables: rejected because this is not a REST Assured or E2E framework feature.
- Build a full dashboard: rejected because the scope is one detail/history/action surface.

## Decision: Reuse Existing Lifecycle API Contracts

**Decision**: Consume Feature 009 detail, history, lifecycle mutation, and metadata update contracts from existing routes under `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}`.

**Rationale**: Existing routes already model the lifecycle surface. Feature 010 should not introduce parallel endpoints, HATEOAS redesign, PSP integration, Kafka/webhooks, or payment creation scope.

**Alternatives considered**:
- Add a dedicated operations-console backend endpoint: rejected unless existing responses cannot satisfy the display contract, because it would duplicate lifecycle representation.
- Add a frontend-only fake data layer: rejected because the console must show real backend lifecycle facts and errors.

## Decision: Treat Current Version Marker as Application State

**Decision**: The application must retain the backend version marker from detail loading and submit it as `If-Match` for lifecycle and metadata mutations.

**Rationale**: Stale-state behavior is a product requirement. The console cannot safely execute actions without preserving conditional update semantics through the Nuxt boundary.

**Alternatives considered**:
- Let the backend infer current version: rejected because it removes stale-state detection from the user workflow.
- Auto-retry on stale state: rejected because it can execute a business action against a state the user did not review.

## Decision: Generate Per-Attempt Idempotency Keys at the Application Boundary

**Decision**: Forward an existing `Idempotency-Key` when present; otherwise generate one for a single lifecycle mutation attempt in the Nuxt server proxy.

**Rationale**: The browser flow may not own idempotency-key generation, but the backend lifecycle contract requires retry-safety. Generating at the proxy keeps the request protocol valid while preserving conflict behavior.

**Alternatives considered**:
- Require manual browser-provided keys for every action: rejected as unnecessary UI complexity.
- Reuse a stable key per order/action: rejected because it risks accidental `409` conflicts or false replay behavior across different action bodies.

## Decision: Preserve Backend Error Shape Through Nuxt Proxy

**Decision**: Nuxt proxy routes must preserve status codes and error body shape closely enough for the UI to distinguish validation, invalid transition, forbidden, not found, stale state, idempotency conflict, and backend unavailable states.

**Rationale**: Error flattening is a core feature risk. Operators need different guidance for `403`, `409`, `412`, `422`, `404`, and unavailable backend outcomes.

**Alternatives considered**:
- Convert all errors to generic UI text: rejected because it violates `FR-PROXY-006` and `FR-ERROR-*` requirements.
- Display raw backend errors verbatim: rejected because safe user-facing text and secret avoidance are required.

## Decision: Render History Oldest-First With Scoped Loading/Error State

**Decision**: Lifecycle history is loaded separately from detail and rendered oldest-first, with empty, loading, and error states scoped to the history area.

**Rationale**: Audit/support users read lifecycle transitions as a story. A history failure must not destroy an otherwise useful detail summary.

**Alternatives considered**:
- Newest-first activity feed: rejected by the resolved specification assumption.
- Block detail rendering until history loads: rejected because detail remains useful when history fails.

## Decision: Keep Metadata Editing Separate From Lifecycle Actions

**Decision**: Metadata update UI must be visually and behaviorally separate from authorize/capture/cancel/refund actions and use the current version marker with metadata-specific error feedback.

**Rationale**: Metadata changes must not imply lifecycle transitions or share destructive action confirmation flows.

**Alternatives considered**:
- Add metadata fields into lifecycle confirmation dialogs: rejected because it creates product confusion.
- Treat metadata save as a lifecycle action: rejected because Feature 009 semantics do not define metadata as a status transition.
