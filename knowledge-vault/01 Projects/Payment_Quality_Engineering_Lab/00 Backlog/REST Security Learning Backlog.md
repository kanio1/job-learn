# REST Security Business Cases from Claude-BugHunter Reverse Engineering

This backlog translates public bug-hunting taxonomy into defensive local-lab learning tasks for the Payment Quality Engineering Lab. It is not a pentest backlog and not an exploit backlog.

## P0 — Must-have for next implementation

### P0-01 - Strict metadata PATCH top-level fields

- Learning goal: Understand mass assignment risk in `PATCH`.
- REST concepts: `PATCH`, `Content-Type: application/merge-patch+json`, `If-Match`, `400`, `application/problem+json`.
- Future implementation idea: For `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`, reject unknown top-level fields outside `metadata`.
- Future Rest Assured test idea: Send `status` next to `metadata` and assert `400` plus unchanged payment status.

### P0-02 - Create payment unknown JSON field policy

- Learning goal: Make write DTO drift visible.
- REST concepts: `POST`, JSON binding, `Idempotency-Key`, `201`, `400`, `Location`.
- Future implementation idea: Decide and implement strict unknown-field rejection for `POST /api/merchants/{merchantId}/payment-orders`.
- Future Rest Assured test idea: Send extra `capturedAmountMinor` on create and assert the documented policy.

### P0-03 - Cross-tenant read masking with no-store

- Learning goal: Test BOLA/IDOR defensively.
- REST concepts: `GET`, `404`, `Cache-Control: no-store`, `Vary: Authorization`, problem details.
- Future implementation idea: Ensure Merchant A reading Merchant B payment detail returns the documented masked response and consistent cache headers.
- Future Rest Assured test idea: Create under Merchant B, read with Merchant A token, assert masked response and no leaked fields.

### P0-04 - Cross-tenant mutation is blocked before side effects

- Learning goal: Separate ownership failure from lifecycle/domain failure.
- REST concepts: lifecycle `POST`, `403` or masked `404`, `If-Match`, `Idempotency-Key`, no side effects.
- Future implementation idea: Ensure ownership mismatch is checked before idempotency reservation, PSP mock call, domain mutation, and history entry.
- Future Rest Assured test idea: Merchant A attempts capture on Merchant B order and assert unchanged status/history.

### P0-05 - HEAD tenant isolation

- Learning goal: Understand existence leakage through headers.
- REST concepts: `HEAD`, `404`, no body, no `ETag` for inaccessible resources, `Cache-Control`.
- Future implementation idea: Align `HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` with `GET` ownership rules.
- Future Rest Assured test idea: Cross-tenant `HEAD` returns the same masked status as `GET`, no body, no `ETag`.

### P0-06 - Reader cannot mutate lifecycle

- Learning goal: Practice BFLA role separation.
- REST concepts: `POST /capture`, `403`, route authorization, persistence oracle.
- Future implementation idea: Keep `merchant:payments:read` separate from lifecycle authorities for all mutation endpoints.
- Future Rest Assured test idea: Merchant reader attempts capture/cancel/refund and receives `403` with no state change.

### P0-07 - Auditor can read history but not mutate

- Learning goal: Model least privilege for audit actors.
- REST concepts: `GET /history`, lifecycle `POST`, `PATCH`, `200`, `403`, `Cache-Control`.
- Future implementation idea: Keep `platform:payments:audit` read-only for history and deny lifecycle/metadata mutations.
- Future Rest Assured test idea: Same auditor token succeeds on history and fails on capture.

### P0-08 - CORS preflight for Idempotency-Key and If-Match

- Learning goal: Distinguish browser preflight from business authorization.
- REST concepts: `OPTIONS`, `Access-Control-Allow-Headers`, `Access-Control-Allow-Methods`, no bearer token.
- Future implementation idea: Keep dev/test CORS policy aligned with payment-required headers.
- Future Rest Assured test idea: Preflight with `Idempotency-Key` and `If-Match` succeeds for known origin without auth.

### P0-09 - Access-Control-Expose-Headers includes contract headers

- Learning goal: Understand why browser clients need response headers.
- REST concepts: `Access-Control-Expose-Headers`, `ETag`, `Location`, `X-Correlation-ID`, `Allow`, `Accept-Patch`.
- Future implementation idea: Add or verify expose headers for browser-visible payment contract metadata.
- Future Rest Assured test idea: CORS request from known origin exposes `ETag` and `X-Correlation-ID`.

### P0-10 - Payment problem errors always use no-store

- Learning goal: Treat error responses as sensitive.
- REST concepts: `application/problem+json`, `Cache-Control: no-store`, `Vary`, `X-Correlation-ID`.
- Future implementation idea: Ensure all payment-scoped errors use centralized sensitive error headers.
- Future Rest Assured test idea: Trigger malformed `If-Match` and assert problem body plus no-store.

### P0-11 - Stale If-Match on capture returns 412

- Learning goal: Test lost-update protection.
- REST concepts: `ETag`, `If-Match`, `412 Precondition Failed`, `Vary: Authorization, If-Match`.
- Future implementation idea: Ensure stale version is rejected before capture mutation and history creation.
- Future Rest Assured test idea: Read ETag, advance version, capture with stale ETag, assert `412`.

### P0-12 - Idempotency conflict is distinct from replay

- Learning goal: Separate retry safety from conflicting business intent.
- REST concepts: `Idempotency-Key`, fingerprint, `200` replay, `409 Conflict`.
- Future implementation idea: Keep create/lifecycle idempotency fingerprint stable and return conflict for different payload under same key.
- Future Rest Assured test idea: Same key/same payload returns replay; same key/different payload returns `409`.

## P1 — Useful after the core HTTP contract

### P1-01 - X-HTTP-Method-Override rejected

- Learning goal: Prevent method tampering confusion.
- REST concepts: method semantics, `X-HTTP-Method-Override`, `400` or ignored header, no mutation.
- Future implementation idea: Add a shared defensive filter or document ignored override behavior.
- Future Rest Assured test idea: Send override header and assert it cannot convert a denied method into a mutation.

### P1-02 - TRACE disabled and does not echo secrets

- Learning goal: Understand unsafe methods.
- REST concepts: `TRACE`, `405`, header echo risk.
- Future implementation idea: Verify server behavior; add explicit block only if needed.
- Future Rest Assured test idea: Send TRACE with fake `Authorization` and assert token is not echoed.

### P1-03 - 405 includes Allow

- Learning goal: Test method contract discovery.
- REST concepts: `405 Method Not Allowed`, `Allow`, problem details.
- Future implementation idea: Keep payment exception handling for unsupported methods and set `Allow`.
- Future Rest Assured test idea: `PUT` payment detail returns `405` and `Allow: GET, HEAD, PATCH, OPTIONS`.

### P1-04 - OPTIONS advertises Accept-Patch

- Learning goal: Learn capability discovery through HTTP.
- REST concepts: `OPTIONS`, `Allow`, `Accept-Patch`, `204 No Content`.
- Future implementation idea: Keep detail `OPTIONS` explicit and stable.
- Future Rest Assured test idea: Assert `OPTIONS` detail has exact `Allow`, `Accept-Patch`, and empty body.

### P1-05 - PATCH unsupported media type returns 415

- Learning goal: Treat media type as contract.
- REST concepts: `Content-Type`, `415`, `Accept-Patch`, problem details.
- Future implementation idea: Narrow or document metadata PATCH media type support.
- Future Rest Assured test idea: Send `text/plain` PATCH and assert `415`, `Accept-Patch`, unchanged metadata.

### P1-06 - Host does not affect Location

- Learning goal: Learn header trust boundaries.
- REST concepts: `Host`, `Location`, `201 Created`.
- Future implementation idea: Keep create payment `Location` relative or configured from trusted base URL.
- Future Rest Assured test idea: Create with hostile `Host`; assert `Location` does not contain it.

### P1-07 - Forwarded headers ignored in local/test profile

- Learning goal: Understand proxy trust.
- REST concepts: `X-Forwarded-Host`, `X-Forwarded-Proto`, deployment profiles.
- Future implementation idea: Document and verify forwarded headers are ignored unless a trusted proxy profile is enabled.
- Future Rest Assured test idea: Send forwarded headers and assert no link/header trusts them.

### P1-08 - Vary policy matrix

- Learning goal: Connect authorization and caching.
- REST concepts: `Vary: Authorization`, `Vary: Authorization, If-Match`, `Vary: Authorization, Idempotency-Key`.
- Future implementation idea: Define expected `Vary` per endpoint family and centralize header constants.
- Future Rest Assured test idea: Assert `Vary` on detail, create replay, stale precondition, and masked 404.

### P1-09 - JWT missing merchant_id matrix

- Learning goal: Separate role from ownership claim.
- REST concepts: JWT claims, `403`, masked `404`, problem details.
- Future implementation idea: Document and align behavior for merchant role without `merchant_id`.
- Future Rest Assured test idea: Merchant reader without `merchant_id` cannot access list/detail/history.

### P1-10 - Error bodies do not leak internals

- Learning goal: Test safe error shape.
- REST concepts: malformed JSON, validation, problem details, absence assertions.
- Future implementation idea: Keep payment exception handling explicit and safe.
- Future Rest Assured test idea: Malformed JSON response does not include Java exception class, stack trace, raw token, or raw request body.

## P2 — Future advanced topics

### P2-01 - Double capture race

- Learning goal: Understand race-condition testing after basic contract is stable.
- REST concepts: concurrent lifecycle `POST`, `If-Match`, idempotency, optimistic locking, `200` versus `412`.
- Future implementation idea: Use current version checks plus database optimistic locking to ensure one winning capture.
- Future Rest Assured test idea: Parallel capture attempts result in only one state/history mutation.

### P2-02 - Cancel after capture and refund after cancel

- Learning goal: Exercise lifecycle state machine negatives.
- REST concepts: `422 Unprocessable Entity`, problem details, current `ETag`.
- Future implementation idea: Keep invalid transitions in the domain model and stable problem code mapping.
- Future Rest Assured test idea: Captured order cannot be cancelled; cancelled order cannot be refunded.

### P2-03 - Invalid issuer and future audience validation

- Learning goal: Understand OAuth resource-server token validation.
- REST concepts: `401`, `WWW-Authenticate`, issuer, audience.
- Future implementation idea: Keep issuer validation; add audience validation only after expected audience is specified.
- Future Rest Assured test idea: Invalid issuer/audience tokens return `401` before controller logic.

### P2-04 - Actuator exposure policy

- Learning goal: Treat operational endpoints as API surface.
- REST concepts: `/actuator/**`, exposure allow-list, `404`, `401`, `403`.
- Future implementation idea: Do not add Actuator until a narrow profile-specific exposure policy exists.
- Future Rest Assured test idea: `/actuator/env` is absent or protected under local/test policy.

### P2-05 - OpenAPI exposure policy

- Learning goal: Test generated documentation as a security boundary.
- REST concepts: `/v3/api-docs`, `/swagger-ui/**`, profile-specific docs.
- Future implementation idea: Add OpenAPI only with explicit path filtering and profile rules.
- Future Rest Assured test idea: Docs expose only intended public paths and no internal/admin endpoints.

### P2-06 - Rest Assured redacting logging filter

- Learning goal: Produce safe evidence.
- REST concepts: `Authorization`, `Cookie`, `Set-Cookie`, failure logging, response snapshots.
- Future implementation idea: Replace raw failure logging with a redacting Rest Assured filter/helper.
- Future Rest Assured test idea: Characterize the redactor with fake tokens and assert sensitive values are masked.

### P2-07 - Safe reproduction template

- Learning goal: Write useful bug reports without exploit-style instructions.
- REST concepts: method, endpoint, headers, status, problem code, correlation ID.
- Future implementation idea: Add a vault template for local-lab reproduction notes.
- Future Rest Assured test idea: Generate a sanitized evidence note for one negative scenario.
