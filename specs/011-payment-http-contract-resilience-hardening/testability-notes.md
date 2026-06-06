# Testability Notes: Branch 011 Payment HTTP Contract Resilience Hardening

This branch prepares backend behavior for a later Rest Assured phase. It intentionally does not add automated tests.

## Classes To Target Later

- `PaymentOrderController`: success headers, `HEAD`, `OPTIONS`, media type and lifecycle endpoint behavior.
- `PaymentEtag`: accepted and rejected `If-Match` formats.
- `PaymentVersionPrecondition`: stale version rejection before mutation.
- `PaymentLifecycleService`: no domain mutation when precondition fails and idempotency scope behavior when precondition passes.
- `PaymentExceptionHandler`: `application/problem+json`, status mapping, old compatibility fields, and correlation ID in body.
- `PaymentHttpHeaders`: `Cache-Control`, `Vary`, `Accept-Patch`, and `X-Correlation-ID` consistency.
- `JpaIdempotencyRecordRepository`: create vs lifecycle idempotency scope.
- Nuxt payment proxy routes: quoted `ETag` preservation, `If-Match` forwarding, and backend header propagation.

## Future Rest Assured Test Scenarios

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
- lifecycle lost-response retry with same idempotency key and original `If-Match` -> replay/current representation without duplicate mutation
- frontend/proxy preserves quoted `ETag` and forwards it unchanged as `If-Match`
- metadata PATCH forwards `If-Match` and preferred `application/merge-patch+json`

## Automation Constraints

- Do not add Rest Assured classes in this branch.
- Do not add JUnit classes in this branch.
- Do not add Playwright tests in this branch.
- Do not add integration tests in this branch.
- Use these notes as the starting point for the next branch only.
