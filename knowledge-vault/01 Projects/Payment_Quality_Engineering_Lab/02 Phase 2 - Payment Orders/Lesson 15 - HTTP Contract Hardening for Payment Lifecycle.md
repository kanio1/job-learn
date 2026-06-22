# Lesson 15 - HTTP Contract Hardening for Payment Lifecycle

## Lesson Goal

This lesson explains why feature branch `011-payment-http-contract-resilience-hardening` strengthens the backend HTTP contract before adding the next Rest Assured test phase.

The main design idea is simple: payment lifecycle mutations should only run when the caller proves that they are acting on the current payment order version.

## Key Classes

- `PaymentOrderController` exposes create, detail, list, summary, lifecycle, metadata PATCH, history, `HEAD`, and `OPTIONS` payment endpoints.
- `PaymentEtag` owns the `"v{version}"` ETag parsing and formatting rule.
- `PaymentVersionPrecondition` compares parsed `If-Match` with `PaymentOrder.getVersion()` before mutation.
- `PaymentOrderVersionMismatchException` maps a stale payment version to `412 Precondition Failed`.
- `PaymentHttpHeaders` centralizes `Cache-Control`, `Vary`, `Accept-Patch`, and correlation response headers.
- `PaymentExceptionHandler` returns problem-details-compatible payment errors while preserving `error`, `message`, and `details` compatibility fields.
- `JpaIdempotencyRecordRepository` scopes idempotency records for create and lifecycle actions.
- Nuxt payment proxy routes preserve the exact quoted `ETag` value, forward `If-Match`, and propagate important backend headers.

## Why If-Match Protects Against Lost Update

Lost update happens when two clients load the same resource version and both try to mutate it. Without a precondition, the second request may accidentally overwrite or advance state based on stale information.

The payment contract uses:

```text
ETag: "v3"
If-Match: "v3"
```

The backend parses the expected version, loads the current `PaymentOrder`, compares it with `order.getVersion()`, and only then executes lifecycle or metadata mutation. If the current version is different, the backend returns `412` and does not run the domain change.

One production-grade nuance matters: a duplicate retry with the same lifecycle idempotency key and the same request fingerprint is recognized before stale-version rejection. If the first request succeeded but the response was lost, the retry should not be treated as a brand-new stale action.

## 409 vs 412 vs 422 vs 428

- `409 Conflict`: the idempotency key or another resource-level conflict makes the request unsafe to treat as the same operation.
- `412 Precondition Failed`: the client sent a valid `If-Match`, but the payment order version changed since the client loaded it.
- `422 Unprocessable Entity`: the request is syntactically valid, but the lifecycle domain rejects it, for example capture from the wrong state.
- `428 Precondition Required`: the endpoint requires `If-Match`, but the client did not send it.

## Why Cache-Control No-Store Matters

Payment resource responses include amount, currency, merchant identity, lifecycle status, metadata, and operational history. These responses are not safe to keep in browser, proxy, or shared intermediary caches.

The contract therefore uses:

```text
Cache-Control: no-store
```

This applies to successful payment data responses and payment resource errors, including masked `404` responses.

## Why CORS Preflight Is Not A Business Request

Browser CORS preflight uses `OPTIONS` to ask whether the actual request may be sent. It does not authorize, capture, cancel, refund, or patch a payment order.

Permitting `OPTIONS` does not mean permitting the business request. Actual `GET`, `HEAD`, `POST`, or `PATCH` requests still go through authentication, authorization, and tenant checks.

## Why Idempotency Scope Must Be Precise

An idempotency key only makes a retry safe when it is scoped to the same operation. The same key used for `authorize` and `capture` must not be treated as one identical request.

Feature 011 scopes create by merchant, key, and `CREATE`. Lifecycle actions are scoped by merchant, payment order, action, and key. The fingerprint also includes request payload facts such as amount and reason.

## Why Correlation ID Must Work On Errors

Support and QA need to connect a user-visible failure with backend logs. If only success responses carry `X-Correlation-ID`, the most important troubleshooting path is missing.

The problem body `correlationId` must match the response header `X-Correlation-ID` so tests, logs, and UI messages all point to the same request.

## Future Rest Assured Test Scenarios

Do not implement these tests in this branch. They are the next automation phase:

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
- lifecycle lost-response retry with same idempotency key -> no duplicate domain mutation
- proxy preserves quoted `ETag` and forwards it as `If-Match`
- metadata PATCH forwards `If-Match` and uses merge-patch media type

## Interview-Grade Testing Heuristic

A senior SDET should not test only status codes. For each payment mutation, test the contract triangle:

- identity: merchant, payment order, action, actor authority,
- safety headers: `If-Match`, `Idempotency-Key`, `X-Correlation-ID`,
- observable result: status code, `ETag`, problem code, persisted lifecycle state, and history entry.

The hardest bug class is not “endpoint returns 500”. The hardest bug class is “endpoint returns a plausible 200 for the wrong business reason”, such as replaying the wrong action, mutating after stale state, or losing the correlation ID on the one failure support needs to debug.

## Mentor Notes For SDET Learning

This branch is intentionally implementation-first and testability-focused. The important learning outcome is not the existence of tests yet, but that the production code now has seams that will be easy to verify later:

- small ETag parser instead of parsing in controller methods,
- explicit precondition exception instead of generic validation,
- centralized headers,
- stable problem details body,
- idempotency scope visible in repository and migration,
- documented future scenarios ready for Rest Assured.
