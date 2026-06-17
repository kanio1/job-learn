# Payment Quality Engineering Lab - Codex CLI Guide

## Project Identity

Payment Quality Engineering Lab is a learning-oriented payment platform used to practice Java/Spring backend engineering, REST API testing, security testing, frontend contract consumption, and SDET review skills.

Implementation is done by the main Codex CLI session. Helper agents and skills are for mapping, planning, mentoring, test design, and review only.

## Current Phase

The checked-out branch is in the Payment Orders / lifecycle / HTTP contract hardening phase. The codebase is beyond Phase 1 merchant-only assumptions.

Implemented domain scope:

- Merchant Registry: create, list, retrieve, activate, suspend.
- Payment Orders: merchant-scoped create/read/list/summary.
- Payment Lifecycle: authorize, capture, cancel, refund, metadata PATCH, status history, ETag/If-Match and idempotency hardening.

## Active Non-Goals

- No `POST /payments` top-level API.
- No real PSP provider integration or PSP failure modeling beyond the existing local mock boundary.
- No Kafka, webhooks, outbox, settlement, payout, reconciliation, KYC, card/PAN/PCI, 3DS, or microservice split.
- No complete OAuth/OIDC production integration; local Keycloak is for development and tests.
- No fake KPI dashboard or broad business dashboard.
- No introducing `.kilocode/` as the project organization target; use `.kilo/` where Kilo project config is referenced.

## Tech Stack

- Backend: Java 25, Maven Wrapper 3.9.11, Spring Boot 4.0.6 / Spring Framework 7, Spring Modulith 2.0.6.
- Persistence: PostgreSQL 18, Flyway migrations, JPA with `ddl-auto: validate`.
- Security: Spring Security JWT resource server, Keycloak realm roles mapped to `platform:*` and `merchant:*` authorities.
- Tests: JUnit 6, AssertJ, Mockito, REST Assured 6, Testcontainers PostgreSQL, Spring Modulith tests.
- Frontend: Nuxt 4 app directory, Nuxt UI, TypeScript 6, Pinia, Zod, `nuxt-auth-utils`, Playwright 1.60.

## Backend Commands

Run from `apps/backend`:

```bash
./mvnw test
./mvnw verify
./mvnw spring-boot:run
```

`./mvnw test` runs `*Test.java`. `./mvnw verify` also runs `*IT.java` through Failsafe.

## Frontend Commands

Run from `apps/frontend`:

```bash
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm build
corepack pnpm exec playwright test
```

Use `pnpm` directly only if it is already available in the shell.

## Infrastructure Commands

Run from the repository root:

```bash
cp infra/compose/.env.example infra/compose/.env
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

Keycloak imports `infra/keycloak/realms/payment-quality-realm.json`. Local admin is `admin` / `admin`.

## API Surface

Public:

- `GET /api/status`

Merchant:

- `POST /api/merchants`
- `GET /api/merchants`
- `GET /api/merchants/{id}`
- `POST /api/merchants/{id}/activate`
- `POST /api/merchants/{id}/suspend`

Payment orders:

- `POST /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `GET /api/merchants/{merchantId}/payment-orders/summary`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund`

## Testing Expectations

- Keep Spring-context tests on `@ActiveProfiles("test")`.
- DB-dependent tests extend `PostgresContainerSupport`; Flyway owns schema.
- Repository tests use Spring/Testcontainers patterns already present in the repo.
- REST Assured tests belong in `apps/backend/src/test/java/lab/paymentquality/rest`.
- Security tests belong in `security` and import `TestJwtConfiguration`.
- Architecture/module boundaries are checked by `ModulithArchitectureTest`, `MerchantModuleTest`, and `PaymentModuleTest`.
- Playwright tests live under `apps/frontend/tests` and use the existing auth setup/storage state pattern.
- Ignore learner copies such as `My*` and `Lesson*` unless the task explicitly concerns learning files.

## Implementation Rules

- Keep changes scoped to the current Spec Kit task or explicitly requested small step.
- Preserve Spring Modulith boundaries: public module APIs under module root packages, implementation under `internal`.
- Do not make the payment module depend on `merchant.internal`.
- Keep REST contracts stable unless the spec requires a contract change.
- Use Flyway for schema changes and keep JPA mappings consistent with migrations.
- Do not add dependencies without explicit need and approval.

## Frontend / UI/UX Work

For frontend/UI/UX design and review, follow:
- `.kiro/steering/frontend-nuxt-ui.md` — Nuxt UI Dashboard patterns, component map, a11y/testability baseline
- `.kiro/steering/modern-web-guidance.md` — invoke with `#modern-web-guidance` for advisory web platform guidance

Before any frontend-heavy spec (user-management, audit-log-dashboard, etc.), run the
review gate: `docs/ai/modern-web-guidance-spec-review-gate.md`.
- Do not modify production source or existing tests for Codex setup tasks.

## Review Rules

- Review behavior first: status codes, headers, validation, persistence effects, security authorities, tenant/merchant ownership, and lifecycle transitions.
- Check `Idempotency-Key`, `If-Match`, `ETag`, `X-Correlation-ID`, `Cache-Control`, and `Vary` only where the current code/spec implements them.
- Confirm REST Assured assertions verify response body, headers, and database state where relevant.
- Confirm Playwright tests use stable locators, isolated data, deterministic auth/session setup, and clear UI states.
- Flag scope creep into PSP, Kafka, settlement, reconciliation, KYC, or fake dashboard metrics.

## Safety Rules

- Do not revert user changes.
- Do not run destructive git commands unless explicitly requested.
- Do not commit secrets, tokens, real credentials, or private Keycloak/client material.
- Do not edit `.kilo` or create `.kilocode` unless the user explicitly asks.
- For this Codex setup, only edit support files requested by the user; do not touch application source or existing tests.
