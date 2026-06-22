# Research: Payment Order Access, Idempotent Creation, And Minimal Create/Read Lifecycle Foundation

**Feature**: `003-payment-order-access-lifecycle`  
**Branch**: `004-payment-order-create-read`  
**Date**: 2026-05-27

## Research Scope

This research resolves the technical decisions needed before implementation planning. It focuses on idempotent creation, request fingerprinting, ETag format, merchant eligibility boundaries, JWT authority mapping, transaction behavior, and test isolation.

## Decision 1: Idempotency Scope And Storage

**Decision**: Scope idempotency records by `(merchant_id, idempotency_key_hash)` and link each successful record to exactly one `payment_orders.payment_order_id`.

**Rationale**: The spec treats `Idempotency-Key` as an opaque client-provided retry key for create operations. Scoping by merchant prevents one merchant from colliding with another merchant's retry keys while keeping the uniqueness rule simple and enforceable through PostgreSQL. Storing a SHA-256 hash of the key avoids persisting raw opaque keys that may contain client identifiers. The payment order ID link is enough to reconstruct the create replay response for this first slice.

**Alternatives considered**:

- Global key uniqueness: rejected because it creates unnecessary cross-tenant coupling and test data collisions.
- Scope by actor subject: rejected because retries from equivalent merchant identities should remain merchant-operation scoped, not user-session scoped.
- Store raw idempotency keys only: rejected because hashed storage reduces accidental disclosure risk with little implementation cost.

## Decision 2: Idempotent Create Algorithm

**Decision**: The application service handles create in one transaction: validate request, check merchant eligibility through the merchant public API, derive fingerprint, attempt to reserve the idempotency record, create `payment_orders`, append initial `payment_order_status_history`, and update the idempotency record with the payment order ID. Concurrent same-key races are resolved by the unique constraint and a reload path.

**Rationale**: The spec requires atomic persistence of payment order, idempotency record, and initial status history. The unique constraint is the authoritative concurrency control. If a second request arrives with the same key after the first commits, it loads the existing idempotency record: same fingerprint returns `200 OK`, different fingerprint returns `409 idempotency_conflict`. If a concurrent insert loses to the unique constraint, the failed transaction rolls back, then the service reloads the committed record and applies the same comparison rule.

**Alternatives considered**:

- Application-only lock map: rejected because it fails across restarts and multiple JVMs.
- PostgreSQL advisory locks: rejected for this slice because the unique constraint and retry path are simpler and easier for learners to inspect.
- Store full response snapshot: deferred because the first slice can reconstruct the response from the durable payment order; future lifecycle behavior may revisit snapshot semantics.

## Decision 3: Request Fingerprint Derivation

**Decision**: Derive `request_fingerprint_hash` as SHA-256 over canonical UTF-8 JSON containing:

```json
{
  "operation": "POST /api/merchants/{merchantId}/payment-orders",
  "merchantId": "<uuid>",
  "amountMinor": 12345,
  "currency": "PLN",
  "clientOrderReference": "trimmed-client-reference"
}
```

`X-Correlation-ID`, bearer token, actor subject, request timestamp, raw header order, and whitespace are excluded.

**Rationale**: The fingerprint must distinguish materially different create requests while remaining stable across retries. Correlation IDs and tokens often change during retries and must not break idempotency. Canonical JSON with fixed field names and ordering keeps the hash deterministic and testable.

**Alternatives considered**:

- Hash the raw HTTP body: rejected because insignificant JSON whitespace or field ordering would create false conflicts.
- Include all headers: rejected because volatile headers would make legitimate retries fail.
- Include actor subject: rejected because idempotency is scoped to the merchant operation, while authorization is enforced separately.

## Decision 4: Idempotency Key Validation

**Decision**: Require a non-blank `Idempotency-Key` header with trimmed length `1..128` and printable ASCII characters. Store only `idempotency_key_hash = SHA-256(trimmedKey)`.

**Rationale**: The spec requires missing or blank keys to return `400 validation` and calls out excessively long keys as an edge case. A 128-character limit is enough for UUIDs and typical retry keys while preventing oversized header abuse. Printable ASCII avoids hidden control characters in logs and diagnostics.

**Alternatives considered**:

- No maximum length: rejected because it weakens validation and creates noisy test boundaries.
- UUID-only keys: rejected because the spec treats the key as opaque and client-generated.

## Decision 5: ETag Format

**Decision**: Use a strong opaque ETag derived from payment order ID and JPA version: `"po-<paymentOrderId>-v<version>"`.

**Rationale**: The first slice only exposes ETags; it does not implement `If-Match` or `412`. A version-based strong validator prepares for future lifecycle actions without implying conditional behavior now. The ID is already visible in the URL and response, so including it in an opaque validator does not add meaningful disclosure.

**Alternatives considered**:

- Weak ETag (`W/"..."`): rejected because future `If-Match` semantics require strong validators.
- Timestamp-only ETag: rejected because timestamp precision can vary and is less directly tied to optimistic locking.
- Hash the full JSON response: rejected as unnecessary work for this first slice.

## Decision 6: Merchant Eligibility Boundary

**Decision**: Add a public merchant module boundary in `lab.paymentquality.merchant`, such as `MerchantPaymentEligibilityService` and `MerchantPaymentEligibility`, implemented internally by the merchant module. The payment module depends only on this root-package API and never imports `lab.paymentquality.merchant.internal.*`.

**Rationale**: The spec requires payment orders to belong to active merchants but forbids payment from depending on merchant internals. A root-package public API lets Spring Modulith verify the dependency while keeping merchant persistence and domain implementation encapsulated.

**Alternatives considered**:

- Payment repository queries the `merchants` table directly: rejected because it bypasses merchant module ownership.
- Payment imports `merchant.internal.application.MerchantService`: rejected by the explicit guardrail.
- Duplicate merchant status in payment: rejected because it creates stale state and unnecessary synchronization.

## Decision 7: JWT Authority And Merchant Scope Mapping

**Decision**: Update authority conversion to preserve namespaced realm roles (`merchant:*`, `platform:*`) and keep backward-compatible mapping for existing Phase 1 roles (`merchants:*` -> `platform:merchants:*`). Add a single-merchant scope claim such as `merchant_id` for merchant payment users. Platform readers do not require `merchant_id` for cross-merchant read.

**Rationale**: Existing Phase 1 code prefixes every Keycloak realm role with `platform:`. That works for roles like `merchants:create`, but it would incorrectly map `merchant:payments:create` to `platform:merchant:payments:create`. A mixed converter preserves existing behavior while enabling explicit merchant and platform payment authorities. The single-merchant scope claim keeps the first slice simple and avoids implementing full Merchant Team Management.

**Alternatives considered**:

- Rename payment roles to `payments:create` and prefix them: rejected because the clarified spec requires `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, and `platform:payments:read` authority concepts.
- Build full team membership tables: rejected as out of scope.

## Decision 8: REST Endpoint Shape

**Decision**: Use merchant-context endpoints:

- `POST /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`

Platform readers use the same read endpoint with the target merchant ID in the path.

**Rationale**: The spec repeatedly frames the feature as merchant-scoped and includes the edge case where merchant ID in the path does not match caller scope. Keeping merchant ID in the URL makes ownership checks explicit. No list/search endpoint is introduced.

**Alternatives considered**:

- `POST /api/payment-orders` with merchant ID in body: rejected because it hides tenant context in the payload.
- `GET /api/payment-orders/{id}` without merchant ID: rejected because it makes the first-slice ownership oracle less visible.

## Decision 9: PostgreSQL Schema Constraints

**Decision**: Create `payment_orders`, `idempotency_records`, and `payment_order_status_history` with foreign keys to `merchants` and `payment_orders`, check constraints for amount/currency/status, and unique constraints for idempotency.

**Rationale**: Database constraints provide durable defense-in-depth for the payment resource. Amount range and status/currency constraints make money precision and lifecycle scope visible at the data layer. FKs ensure payment orders cannot reference non-existent merchants, while application eligibility checks ensure only active merchants can create them.

**Alternatives considered**:

- Application validation only: rejected because it weakens data integrity and persistence learning value.
- Separate schema per module: rejected because Phase 1 uses one PostgreSQL schema and modular migration directories.

## Decision 10: Status History Semantics

**Decision**: Append exactly one initial status history row on create with `from_status = NULL`, `to_status = 'CREATED'`, `actor_subject`, `correlation_id`, and timestamp.

**Rationale**: The first slice has no lifecycle transition beyond `CREATED`, but it must establish audit trail structure for future lifecycle behavior. `from_status = NULL` accurately models resource creation.

**Alternatives considered**:

- No status history until lifecycle actions: rejected because the spec requires initial history.
- Store only current status on order: insufficient for audit learning and future lifecycle traceability.

## Decision 11: Frontend Scope

**Decision**: Add a minimal merchant-context payment journey: create form for active merchants and payment order detail page. Do not add payment list/filter/search, lifecycle action buttons, PSP placeholders, or platform admin dashboards.

**Rationale**: The UI should consume the backend contract and support the Lesson 6 walkthrough without implying unimplemented payment behavior. Since no list endpoint exists, navigation after create relies on the returned payment order ID and direct detail URL.

**Alternatives considered**:

- Add a full payment dashboard: rejected as complete business dashboard scope creep.
- Add lifecycle buttons disabled in the UI: rejected because it suggests behavior not implemented in this slice.

## Decision 12: Parallel Test Data Strategy

**Decision**: Generate payment references and idempotency keys with explicit namespace components: `PAY-{testRunId}-{workerId}-{uuid}` and `idem-{testRunId}-{workerId}-{uuid}`. Each test creates or selects its own active merchant using merchant test support.

**Rationale**: The feature is specifically used to teach parallel-safe payment testing. Unique references and keys avoid cross-test collisions while still letting tests intentionally reuse the same key inside one scenario.

**Alternatives considered**:

- Shared global active merchant and fixed idempotency key: rejected because it creates order-dependent tests.
- Random-only values without test run/worker metadata: acceptable but less diagnosable when a parallel failure occurs.
