# Phase 0 Quality Baseline

Phase 0 creates a testable skeleton before payment behavior exists.

## Technical Verification Commands

Backend from `apps/backend`:

```bash
./mvnw test
```

Frontend from `apps/frontend`:

```bash
pnpm install
pnpm typecheck
pnpm build
corepack pnpm exec playwright test
```

Infrastructure from repository root:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

## Test Layer Map

- Unit/slice tests: backend controller/status behavior and future isolated logic
- Architecture tests: Spring Modulith `ApplicationModules.verify()`
- Integration tests: future Spring/Testcontainers checks under `apps/backend/src/test/java/lab/paymentquality/integration`
- REST tests: foundation-only REST Assured status smoke now; future API tests later
- WireMock tests: future external-service stubs under `apps/backend/src/test/java/lab/paymentquality/wiremock`
- Playwright tests: frontend foundation shell smoke under `apps/frontend/tests/e2e`

## REST Assured Scope

REST Assured is present to prove the backend can host HTTP-level tests. Phase 0 uses it only for the technical status endpoint. There are no payment API tests because there is no payment API.

## Testcontainers Scope

Testcontainers dependencies and conventions exist now. PostgreSQL containers should be added only when persistence behavior exists or a future foundation connectivity test is explicitly justified.

## WireMock Scope

WireMock is reserved for future external-service tests. Phase 0 must not implement PSP stubs or provider callback behavior.

## Tester-Owned Parallel-Readiness Strategy

Tests should be safe to run alone, repeatedly, and eventually in parallel.

Principles:
- Avoid shared mutable state.
- Avoid reliance on test order.
- Use unique data namespaces when future data exists.
- Use worker-aware Playwright fixtures for future users and resources.
- Avoid one global mutable user, merchant, payment, or auth state.
- Prefer dynamic ports for WireMock and explicit ports for REST checks.
- Use cleanup-by-owner, transaction rollback, isolated schemas, or ephemeral containers based on test layer risk.

Example future namespacing pattern:

```text
testRunId = 20260518-abcdef
workerId = pw-0
externalReference = PAY-{workerId}-{scenarioId}-{uuid}
idempotencyKey = IDEM-{workerId}-{uuid}
```

## Non-Goal Audit Checklist

- No `POST /payments`
- No payment persistence
- No Kafka service
- No PSP mock flow
- No complete OAuth/OIDC flow
- No complete business dashboard
