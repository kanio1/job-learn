# Repository Context

Payment Quality Engineering Lab is a modular monolith learning project for payment API quality engineering and SDET practice.

## Current Implementation

- Backend modules: `lab.paymentquality.merchant` and `lab.paymentquality.payment`, verified with Spring Modulith tests.
- Merchant Registry: create, list, retrieve, activate, suspend.
- Payment Orders: merchant-scoped create, read, list/filter, summary, lifecycle history, authorize, capture, cancel, refund, metadata PATCH.
- Status endpoint: public `GET /api/status`.
- Frontend: Nuxt dashboard at `/admin/merchants` with nested payment order pages under `/admin/merchants/{merchantId}/payments`.
- Security: JWT resource server; Keycloak realm roles map to `platform:*` and `merchant:*` authorities.
- Persistence: PostgreSQL 18, Flyway migrations in `apps/backend/src/main/resources/db/migration/{merchant,payment}`, JPA `ddl-auto: validate`.

## Test Layers

- Unit/domain/service tests with JUnit, AssertJ, and Mockito.
- Repository and integration tests with Spring Boot and Testcontainers PostgreSQL.
- REST Assured API tests in `apps/backend/src/test/java/lab/paymentquality/rest`.
- Security tests in `apps/backend/src/test/java/lab/paymentquality/security`.
- Spring Modulith architecture/module tests.
- Playwright tests in `apps/frontend/tests`.

## Commands

- Backend test: `cd apps/backend && ./mvnw test`
- Backend verify: `cd apps/backend && ./mvnw verify`
- Frontend typecheck: `cd apps/frontend && corepack pnpm typecheck`
- Frontend build: `cd apps/frontend && corepack pnpm build`
- Playwright: `cd apps/frontend && corepack pnpm exec playwright test`
- Infra up: `docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d`

## Active Non-Goals

No top-level `POST /payments`, real PSP integration, PSP failure modeling, Kafka, webhooks, settlement, payout, reconciliation, KYC, card/PCI/3DS, microservice split, fake KPI dashboard, or production OAuth/OIDC completion.

## Where To Look

- Specs: `specs/`
- Backend docs: `apps/backend/README.md`
- Frontend docs: `apps/frontend/README.md`
- Setup docs: `docs/setup/`
- Testing docs: `docs/testing/`
- Learning notes: `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/`
