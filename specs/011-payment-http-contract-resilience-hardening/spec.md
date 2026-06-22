# Feature Specification: Payment HTTP Contract Resilience Hardening

**Feature Branch**: `011-payment-http-contract-resilience-hardening`

**Created**: 2026-06-06

**Status**: Draft

## Business Purpose

This feature hardens the backend HTTP contract for payment order lifecycle operations so that operators can safely act on the version of a payment order they actually loaded. The main risk addressed is lost update: two users or retries can observe the same payment version, then submit conflicting lifecycle or metadata mutations.

The feature does not add new payment business behavior. It strengthens the modular-monolith backend boundary by enforcing conditional updates, making error responses machine-readable through problem details, preventing sensitive payment resource caching, and documenting the future Rest Assured scenarios needed for the next phase.

## Scope

### In Scope

- Real `If-Match: "v{version}"` parsing and enforcement for lifecycle and metadata mutation endpoints.
- `428 Precondition Required` for missing `If-Match` on conditional payment mutations.
- `400 Bad Request` for malformed `If-Match` syntax.
- `412 Precondition Failed` for stale payment order versions before domain mutation.
- `application/problem+json` payment error bodies with compatibility fields retained.
- `Cache-Control: no-store` for payment resource success and error responses.
- `Vary` headers that distinguish authenticated reads from conditional mutations.
- Consistent `X-Correlation-ID` on success and error responses.
- `HEAD` contract for payment order detail metadata.
- `OPTIONS` contract for payment order detail and lifecycle action endpoints.
- `Accept-Patch: application/merge-patch+json` for metadata patch capability.
- Idempotency scope hardening across merchant, operation/action, payment order, and payload fingerprint.
- Documentation and corrected test-support seams for future Rest Assured scenarios, without adding executable test scenarios in this phase.

### Out of Scope

- No new automated tests in this phase.
- No executable Rest Assured scenario tests.
- No JUnit tests.
- No Playwright tests.
- No integration tests.
- No Kafka.
- No PSP provider integration.
- No PSP mock flows beyond the existing boundary.
- No gateway service.
- No rate limiting.
- No webhooks.
- No settlement, payout, or reconciliation capability.
- No microservice split.

## Functional Requirements

- **FR-011-001**: Lifecycle mutation endpoints MUST require `If-Match` and compare it with the current persisted payment order version before any lifecycle mutation is executed.
- **FR-011-002**: Metadata PATCH MUST require `If-Match` and compare it with the current persisted payment order version before metadata is changed.
- **FR-011-003**: Missing `If-Match` on conditional payment mutations MUST return `428 Precondition Required`.
- **FR-011-004**: Malformed `If-Match` values MUST return `400 Bad Request`.
- **FR-011-005**: Stale `If-Match` values MUST return `412 Precondition Failed` and MUST NOT run PSP calls or domain mutation methods.
- **FR-011-006**: Payment errors SHOULD use `application/problem+json` and MUST include `type`, `title`, `status`, `detail`, `code`, and `correlationId`.
- **FR-011-007**: Existing payment error compatibility fields such as `error`, `message`, and `details` MUST remain available.
- **FR-011-008**: Payment resource responses MUST include `Cache-Control: no-store`.
- **FR-011-009**: Payment resource read responses SHOULD include `Vary: Authorization`.
- **FR-011-010**: Conditional mutation responses SHOULD include `Vary: Authorization, If-Match`.
- **FR-011-011**: `X-Correlation-ID` MUST be echoed when supplied and generated when absent.
- **FR-011-012**: Problem bodies MUST use the same `correlationId` value as the response header.
- **FR-011-013**: `HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` MUST apply the same read authorization and tenant rules as GET detail.
- **FR-011-014**: Payment order detail `OPTIONS` MUST advertise `Allow: GET, HEAD, PATCH, OPTIONS` and `Accept-Patch: application/merge-patch+json`.
- **FR-011-015**: Lifecycle action `OPTIONS` MUST advertise `Allow: POST, OPTIONS`.
- **FR-011-016**: Unsupported `Content-Type` for request bodies MUST return `415 Unsupported Media Type`.
- **FR-011-017**: Unsupported `Accept` on JSON-only payment endpoints MUST return `406 Not Acceptable`.
- **FR-011-018**: Idempotency records MUST be scoped so the same merchant key cannot blur create, authorize, capture, cancel, refund, payment order identity, or payload fingerprint.
- **FR-011-019**: A repeated lifecycle request with the same merchant, payment order, action, idempotency key, and payload fingerprint SHOULD be treated as an idempotent replay before stale `If-Match` rejection is applied.
- **FR-011-020**: Payment order detail responses SHOULD include lifecycle facts needed by the operations console: timestamps, captured/refunded amounts, reasons, metadata, and current version marker via `ETag`.
- **FR-011-021**: The application boundary SHOULD preserve backend `ETag`, `Cache-Control`, `Vary`, `X-Correlation-ID`, `Location`, `Allow`, and `Accept-Patch` headers where the browser/client needs the REST contract.

## Architecture Notes

- `PaymentEtag` owns the HTTP `"v{version}"` parsing and formatting contract.
- `PaymentVersionPrecondition` compares the parsed expected version with the current `PaymentOrder.getVersion()` before mutation.
- `PaymentOrderVersionMismatchException` represents a stale conditional update and maps to `412`.
- `PaymentHttpHeaders` centralizes payment resource cache, vary, and correlation response headers.
- `PaymentExceptionHandler` now emits problem-details-compatible bodies while preserving old error fields.
- `JpaIdempotencyRecordRepository` and migration `V5__harden_payment_http_contract.sql` scope idempotency by create operation or lifecycle order/action.
- Nuxt payment proxy routes preserve quoted backend `ETag` values as version markers and forward `If-Match` for metadata PATCH.

## Risk Notes

- Lost update risk is addressed by comparing `If-Match` before mutation rather than relying only on JPA optimistic locking after mutation intent has already been accepted.
- Sensitive payment data should not be stored by browser, proxy, or shared intermediary caches; payment resource responses therefore use `Cache-Control: no-store`.
- CORS preflight is not a business request. `OPTIONS` may be permitted for preflight/contract discovery, but actual lifecycle and metadata requests remain authenticated and authorized.
- Idempotency keys are unsafe if scoped only by merchant and key. The scope must include operation/action and payment order where relevant, while the fingerprint covers payload differences such as amount and reason.
- Idempotent lifecycle replay is checked before stale-version rejection for the same key/scope/fingerprint. A duplicate retry after a lost response should not become a false `412` just because the first successful request advanced the version.

## Future Rest Assured Test Scenarios

Automated tests are intentionally deferred to the next phase.

- stale `If-Match` -> `412`
- missing `If-Match` -> `428`
- malformed `If-Match` -> `400`
- payment detail -> `Cache-Control: no-store`
- error response -> `application/problem+json`
- correlationId header equals body
- `HEAD` returns headers and no body
- `OPTIONS` returns `Allow` and `Accept-Patch`
- unsupported `Content-Type` -> `415`
- unsupported `Accept` -> `406`
- idempotency conflict -> `409`
- lifecycle retry with same idempotency key and original `If-Match` after lost response -> documented replay behavior
- frontend/proxy preserves quoted `ETag` as `If-Match`
- metadata PATCH forwards `If-Match` and returns updated payment detail facts

## Acceptance

- The backend compiles.
- No automated tests were added in this phase.
- No microservice, Kafka, PSP integration, webhook, or rate-limiting capability was introduced.
- `If-Match` is enforced before payment lifecycle or metadata mutation.
- Sensitive payment responses use `Cache-Control: no-store`.
- Payment errors use `application/problem+json` with correlation information.
- The implementation remains inside the existing Spring Modulith modular monolith.
