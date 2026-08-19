# Payment Quality Engineering Lab

Learning-oriented payment platform for Java/Spring backend work, REST API testing, security testing, frontend contract consumption, and SDET practice.

It is a modular monolith with a JWT-protected Spring API, a Nuxt dashboard that talks to the API through a Nitro BFF, and local PostgreSQL + Keycloak. Product APIs sit next to flag-gated learning labs (checkout protocol, PayU/bank mirrors, RLS, HTTP errors, SQL/ETL).

## Current scope

In scope:

- Merchant registry: create, list, retrieve, activate, suspend, tenant ownership, risk flag
- Payment orders (merchant-scoped): create, read, list/filter, summary, CSV export, async export jobs, authorize / capture / cancel / refund, dual-control refunds, metadata PATCH, history, evidence, internal notes
- HTTP contracts on payment orders: `ETag` / `If-Match`, `Idempotency-Key`, conditional GET, HEAD, OPTIONS, `X-Correlation-ID`, `application/problem+json`
- IAM with local Keycloak roles (`platform:*` and `merchant:*`), tenant isolation, user management, tenant settings
- Audit log list, detail, JSON export, and before/after state
- Nuxt operator dashboard for merchants, payments, users, audit, support, and the learning labs
- Deterministic seed (contract world, ~104 payments) and a separate data-learning seed (10 000 payments) plus a payment ETL lab
- Authenticated OpenAPI document at `GET /v3/api-docs` (Swagger UI off; disabled in prod)

Out of scope:

- Top-level `POST /payments`
- Real PSP / card / PAN / PCI / 3DS
- Kafka, webhooks, outbox, settlement, payout, KYC
- Production OAuth/OIDC completion (local Keycloak only)
- Fake KPI / business dashboards
- Spark, Airflow, Iceberg, or a warehouse stack — the ETL lab is OLTP source → staging → target in Postgres

## Stack

| Layer | Versions in this repo |
|---|---|
| Backend | Java 25, Maven Wrapper 3.9.11, Spring Boot 4.0.6 / Spring Framework 7, Spring Modulith 2.0.6 |
| Persistence | PostgreSQL 18, Flyway, JPA `ddl-auto: validate` |
| Security | Spring Security JWT resource server; Keycloak 26.6.1 realm roles → `platform:*` / `merchant:*` |
| API docs | springdoc-openapi 3.1.0 (`GET /v3/api-docs`) |
| Backend tests | JUnit 6.0.3, AssertJ, Mockito, REST Assured 6.0.0, jqwik 1.9.2, Testcontainers 2.0.5, WireMock 3.13.2 |
| Frontend | Nuxt 4.4.6, Nuxt UI 4.7.1, Vue 3, TypeScript 6.0.3, Pinia 3.0.4, Zod 4.4.3, Tailwind CSS 4, `nuxt-auth-utils` |
| Frontend tests | Vitest, fast-check, Playwright 1.61.0, @axe-core/playwright |
| Package manager | pnpm 11.18.0 via Corepack |
| Local infra | Postgres 18 + Keycloak 26.6.1 via Compose; optional Caddy/mkcert HTTPS overlay |

Node for the frontend follows Nuxt 4: `^22.12.0 || ^24.11.0 || >=26.0.0`.

## Repository map

```text
apps/backend/          Spring Boot modular monolith (Java 25)
apps/frontend/         Nuxt 4 dashboard, Nitro BFF, Playwright
apps/api-tests/        Standalone black-box REST Assured (no Spring imports)
infra/compose/         Postgres + Keycloak; optional app/TLS overlays
infra/keycloak/        Local realm import (`payment-quality`)
infra/caddy/           HTTPS reverse-proxy overlay
scripts/               `dev-stack.sh` and TLS/OIDC oracles
docs/setup/            Local stack, TLS, Bruno/Postman
docs/testing/          Test catalogs (POM, TLS, OpenAPI, labs)
docs/data-learning/    SQL / ETL learning exercises
docs/architecture/     Modular monolith and OpenAPI ownership notes
.codex/                Live specs, tickets, ADRs, guides
.agents/skills/        Engineering process skills
knowledge-vault/       Obsidian learning system
status/                Execution registry and validation evidence
```

Historical specs live under `specs/` and `.kiro/specs/`. New work is tracked in `.codex/` — see `docs/agents/issue-tracker.md`. Do not introduce `.kilocode/` as a project-organization target; use `.kilo/` where Kilo config is referenced.

## Backend modules

Spring Modulith modules under `lab.paymentquality.*`:

| Module | Role |
|---|---|
| `merchant` | Merchant registry and eligibility |
| `payment` | Payment orders, lifecycle, evidence, notes, export, dual-control refunds |
| `tenant` | Tenant registry, context, settings |
| `iam` | User management against local Keycloak admin |
| `audit` | Audit events from domain activity |
| `testing` | Seed / reset / learning dataset / payment ETL (flag-gated, never prod) |
| `checkoutlab` | Redirect + notify checkout protocol lab |
| `mirrorlab` | PayU / bank-style mirror flows |
| `rlslab` | PostgreSQL row-level security lab |

Flyway owns schema under `apps/backend/src/main/resources/db/migration/{tenant,merchant,payment,shared,audit,checkoutlab,mirrorlab,rlslab,testing}`.

Labs and `/api/test/**` are off unless explicitly enabled (`app.checkout-lab.enabled`, `app.mirror-lab.enabled`, `app.rls-lab.enabled`, `app.testing.enabled`). They are excluded from the OpenAPI document.

### Product API

```text
GET    /api/status
GET    /v3/api-docs

POST   /api/merchants
GET    /api/merchants
GET    /api/merchants/{id}
POST   /api/merchants/{id}/activate
POST   /api/merchants/{id}/suspend
PATCH  /api/merchants/{id}/risk-flag

POST   /api/merchants/{merchantId}/payment-orders
GET    /api/merchants/{merchantId}/payment-orders
GET    /api/merchants/{merchantId}/payment-orders/summary
GET    /api/merchants/{merchantId}/payment-orders/export
HEAD   /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
GET    /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
PATCH  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
GET    /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history
POST   /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize|capture|cancel|refund
POST/GET  .../evidence
GET/POST  .../notes
POST/GET  .../refund-approvals
POST/GET  .../export-jobs

GET/PATCH /api/tenants/current/settings
GET/POST/PATCH /api/users
POST   /api/users/{id}/roles
GET    /api/audit
GET    /api/audit/export.json
GET    /api/audit/{id}
POST   /api/payment-ops/expiration-sweep
```

`GET /api/status` stays public and does not expose secrets, database, Keycloak, or business identifiers:

```json
{"application":"payment-quality-lab","phase":"foundation","status":"UP"}
```

### Test and learning endpoints

Enabled only with `APP_TESTING_ENABLED=true` (and never on `prod`):

```text
POST /api/test/reset
POST /api/test/seed              # contract world (~104 payments)
POST /api/test/seed-learning     # SMALL learning world (10 000 payments)
POST /api/test/etl/payments/full|incremental|rebuild
```

Checkout / mirror / RLS labs live under `/api/checkout-lab/**`, `/api/mirror-lab/**`, and `/api/rls-lab/**`. How to use the two seed worlds and the ETL lab: [`.codex/guides/data-learning-interview-program.md`](.codex/guides/data-learning-interview-program.md) and [`docs/data-learning/etl-migration/`](docs/data-learning/etl-migration/01-source-target-map.md).

## Frontend

Nuxt dashboard with Keycloak Authorization Code + PKCE (`nuxt-auth-utils`). Browser code never holds access tokens: Nitro `server/api/**` reads the sealed session and forwards `Authorization: Bearer …` to Spring.

Operator routes:

- `/admin/merchants` and nested payment-order pages
- `/admin/users`, `/admin/audit`, `/admin/support`, `/admin/tenant/settings`
- Learning: `/error-lab`, `/admin/checkout-lab`, `/admin/mirror-lab`, `/admin/rls-lab`, plus session / visual / network labs

The browser client validates JSON with Zod before render and surfaces HTTP status, forwarded headers (`ETag`, `Location`, `Vary`, `X-Correlation-ID`, …), and `application/problem+json`.

## What we test

### Backend (`apps/backend`)

`./mvnw test` runs `*Test.java` (Surefire). `./mvnw verify` also runs `*IT.java` (Failsafe). Spring-context tests use `@ActiveProfiles("test")`. DB tests extend `PostgresContainerSupport`; Flyway owns schema. HTTP security tests use locally signed JWTs (`TestJwtConfiguration`), not live Keycloak.

| Layer | Where | Focus |
|---|---|---|
| Domain / service | module `internal` tests | lifecycle, validation, idempotency, tenant rules |
| Property | jqwik | realm/seed alignment and converters |
| Persistence | `*RepositoryTest`, Failsafe ITs | Flyway schema, JPA, RLS, learning seed/ETL |
| REST Assured | `lab.paymentquality.rest` | merchant/payment HTTP contracts, labs, OpenAPI, TLS/CORS |
| Security | `lab.paymentquality.security` | authorities, tenant isolation, lab/test endpoint chains |
| Modulith | `ModulithArchitectureTest`, `*ModuleTest` | public vs `internal` package boundaries |

Broad Codex/agent validation skips `lab.paymentquality.restkit/**` and `lab.paymentquality.paymentsupport/**` (learner copies such as `My*` / `Lesson*` unless the task is about those files).

### Frontend (`apps/frontend`)

| Layer | Command / config | Focus |
|---|---|---|
| Unit / component / property | `corepack pnpm test:unit` (Vitest + fast-check) | Zod contracts, RBAC, HTTP presentation, colocated UI states |
| Live POM + BFF REST | `playwright.pom.config.ts` (`tests-pom`, + TLS / RLS-off variants) | real Keycloak, UI journeys, BFF 304/HEAD/idempotency |

Live Playwright needs a running backend/Keycloak and passwords from the environment. Do not commit storage-state files.

### Black-box API (`apps/api-tests`)

Standalone REST Assured: no Spring Boot, no backend DTO reuse. Offline framework tests with `mvn test`; live specs with `BACKEND_IMAGE=… mvn verify`. Covers merchant/payment contracts, isolation, multipart evidence, OpenAPI, HTTP method semantics.

### Tester focus

Merchant and payment contracts, lifecycle transitions, IAM and tenant boundaries, audit and user-management flows, persistence effects, frontend loading/empty/error/forbidden states, parallel-safe test data, and (when labs are on) checkout / mirror / RLS / ETL behaviour.

## Prerequisites

- JDK 25
- Docker or Podman with Compose
- Node compatible with Nuxt 4 (see stack table)
- Corepack / pnpm
- Bash for `apps/backend/mvnw` and `scripts/dev-stack.sh`

## Local environment

Day-to-day Podman stack, Playwright POM, and Caddy/HTTPS: [docs/setup/run-stack-and-pom.md](docs/setup/run-stack-and-pom.md). Bruno/Postman against Spring (`Bearer` JWT): [docs/setup/bruno-postman-api.md](docs/setup/bruno-postman-api.md). TLS overlay: [docs/setup/tls-lab.md](docs/setup/tls-lab.md).

Copy `infra/compose/.env.example` to `infra/compose/.env`. Do not commit real secrets, tokens, or generated Keycloak/Playwright state.

Non-secret application variables:

- `SPRING_PROFILES_ACTIVE`
- `APP_TESTING_ENABLED`
- `APP_POSTGRES_HOST` / `APP_POSTGRES_PORT`
- `NUXT_PUBLIC_API_BASE_URL`
- `NUXT_PUBLIC_KEYCLOAK_URL` / `NUXT_PUBLIC_KEYCLOAK_REALM` / `NUXT_PUBLIC_KEYCLOAK_CLIENT_ID`

Keycloak imports `infra/keycloak/realms/payment-quality-realm.json`. Local admin is `admin` / `admin`.

## Commands

### Infrastructure

From the repository root:

```bash
cp infra/compose/.env.example infra/compose/.env
scripts/dev-stack.sh            # Postgres + Keycloak in Compose; Spring + Nuxt on the host
scripts/dev-stack.sh --app      # full HTTP stack in containers (POM / TS learning)
scripts/dev-stack.sh --tls      # Caddy HTTPS → host apps
scripts/dev-stack.sh --full     # Caddy HTTPS → containerized apps
scripts/dev-stack.sh --stop
scripts/dev-stack.sh --down
```

Compose-only (Postgres + Keycloak):

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

### Backend

From `apps/backend`:

```bash
./mvnw test
./mvnw verify
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The `dev` profile is required for local startup — it activates CORS for the Nuxt app on `http://localhost:3000`. Without it the context fails on a missing `corsConfigurationSource` bean. `test` is for the test suite only.

Learning seed / ETL also needs test endpoints:

```bash
SPRING_PROFILES_ACTIVE=dev APP_TESTING_ENABLED=true ./mvnw spring-boot:run
```

### Frontend

From `apps/frontend`:

```bash
corepack pnpm install
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm test:unit
corepack pnpm build
corepack pnpm test:e2e
corepack pnpm exec playwright test --config playwright.pom.config.ts
```

Use `pnpm` directly only if it is already on `PATH`.

### Black-box API tests

From `apps/api-tests`:

```bash
mvn test
BACKEND_IMAGE=payment-quality/backend:local mvn verify
```

Latest recorded validation snapshots live in `status/evidence/latest-validation.md` (they lag the working tree; treat them as evidence, not as this README's source of truth).

## Where to look next

- Repo map / glossary: `CONTEXT.md`, `.codex/CONTEXT.md`
- Agent instructions: `AGENTS.md`
- Backend notes: `apps/backend/README.md`
- Frontend notes: `apps/frontend/README.md`
- Setup: `docs/setup/`
- Testing catalogs: `docs/testing/`
- Learning vault: `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/`
- Historical Phase 0/1 hubs in that vault remain useful orientation, not a description of current product scope
