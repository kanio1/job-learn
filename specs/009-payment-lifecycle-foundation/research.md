# Research: Payment Lifecycle Foundation

## Decision: Keep lifecycle ownership in the existing payment module

**Rationale**: The existing `lab.paymentquality.payment` Spring Modulith module already owns payment order creation, read/list/summary contracts, payment domain objects, idempotency records, status history, and payment REST controllers. Keeping lifecycle behavior inside this module preserves the modular-monolith boundary and avoids a fake module split.

**Alternatives considered**: A separate lifecycle module was rejected because it would create cross-module dependencies around the same aggregate. A shared workflow module was rejected because no other module owns lifecycle behavior today.

## Decision: Extend V2/V3 schema through a V4 Flyway migration

**Rationale**: Existing `payment_orders`, `idempotency_records`, and `payment_order_status_history` tables already exist. Feature 009 should expand status constraints and add missing lifecycle columns/fields without rewriting historical migrations.

**Alternatives considered**: Editing V2 was rejected because it breaks migration history. Creating duplicate lifecycle tables was rejected because it splits one aggregate across unnecessary persistence structures.

## Decision: Use a synchronous state machine over the payment order aggregate

**Rationale**: Lesson 14 focuses on REST/HTTP, state transitions, idempotency, and optimistic locking. Synchronous service methods make state-machine behavior observable through one request/response cycle and keep the feature testable with REST Assured.

**Alternatives considered**: Kafka/webhooks/events were rejected as Lesson 17 scope. A scheduled expiration job was rejected because lazy expiration on capture is sufficient for Lesson 14.

## Decision: Use optimistic locking with `If-Match` and payment order version

**Rationale**: The current `PaymentOrder` entity already has a version field. Binding lifecycle updates to `If-Match` gives testers a concrete lost-update prevention path and maps stale clients to `412 Precondition Failed`.

**Alternatives considered**: Pessimistic locking was rejected as too heavy for the lesson and less visible to API consumers. Last-write-wins was rejected because it is unsafe for financial lifecycle actions.

## Decision: Standardize lifecycle ETags as `"v{version}"`

**Rationale**: The feature spec requires this contract for `If-Match` and response ETags. The existing controller currently emits an ID-prefixed ETag for create/read responses; implementation must reconcile that existing behavior with the lifecycle contract so clients can read an order and use the returned ETag for lifecycle actions.

**Alternatives considered**: Keeping `"po-{id}-v{version}"` was rejected because it conflicts with the feature contract. Accepting both formats may be useful only as a transitional parser, but the response contract should be one clear format.

## Decision: Reuse idempotency key hashing and fingerprint concepts for lifecycle actions

**Rationale**: Existing create idempotency already validates printable ASCII keys, hashes them, and stores request fingerprints. Lifecycle actions need the same retry-safety semantics: same key plus same action returns cached result, while same key plus different action returns `409 Conflict`.

**Alternatives considered**: Per-action key tables were rejected as unnecessary. In-memory idempotency was rejected because it would not survive application restart or support database-level uniqueness.

## Decision: Use an in-process always-success PSP mock

**Rationale**: The feature needs a seam for authorize/capture/void/refund without introducing network calls or failure simulation. An injected interface plus mock implementation preserves testability and keeps PSP failures deferred.

**Alternatives considered**: WireMock or external PSP simulation was rejected as Lesson 16 scope. Directly embedding PSP behavior inside the lifecycle service was rejected because it hides an important test seam.

## Decision: Status history endpoint exposes lifecycle transitions for Lesson 14

**Rationale**: The feature acceptance scenarios expect lifecycle history to explain authorize/capture/cancel/refund transitions. Existing creation-entry support must not confuse the Lesson 14 history contract.

**Alternatives considered**: Returning creation entries was rejected because the spec says a newly created order has an empty lifecycle history. Removing all creation history persistence may be considered during implementation if no existing tests depend on it.

## Decision: CORS is profile-gated to dev/test only

**Rationale**: This gives frontend and REST/HTTP learners a concrete OPTIONS/preflight behavior while preserving the project guardrail that production CORS remains disabled unless separately specified.

**Alternatives considered**: Production CORS was rejected as too broad. No CORS support was rejected because OPTIONS/preflight is an explicit Lesson 14 learning goal.

## Decision: Frontend displays lifecycle status/history only

**Rationale**: The frontend already has payment detail, status badge, schema, store, and proxy route foundations. Displaying expanded statuses and a history timeline supports learning without creating incomplete business dashboards or action workflows.

**Alternatives considered**: Adding lifecycle action buttons was rejected because the spec explicitly excludes UI lifecycle actions. A dashboard/KPI view was rejected by project guardrails.
