# Payment Quality Engineering Lab

Payment Quality Engineering Lab is a learning-oriented full-stack payment platform. The current scope has moved beyond the historical Phase 0/Phase 1 merchant-only foundation.

## Current Scope

In scope:
- Merchant registry with create, list, retrieve, activate, suspend, and tenant ownership
- Payment orders with create/read/list/summary, lifecycle transitions, metadata/history, `ETag`/`If-Match`, idempotency, conditional GET, and HEAD contracts
- IAM and local Keycloak roles, tenant isolation, user management, and audit-log capabilities
- PostgreSQL 18 persistence with Flyway-owned merchant schema
- Keycloak-backed local operator login and JWT resource-server authorization
- Nuxt merchant/payment, user-management, and audit dashboard routes
- Unit, repository, REST Assured, security, Testcontainers, mocked Chromium, and separate live-Keycloak Playwright assurance coverage
- Tester-facing Phase 1 setup, auth, data, and test-design documentation

Out of scope:
- `POST /payments`
- Kafka
- PSP integration or PSP mock flows
- PSP integration, Kafka, settlement, reconciliation, KYC, Client Credentials Flow
- Complete merchant self-service, admin, risk, operations, or reconciliation dashboards

## Repository Map

```text
apps/backend/          Java 25 Spring Boot 4 backend and merchant module
apps/frontend/         Nuxt 4 dashboard with /admin/merchants
infra/compose/         Local PostgreSQL and Keycloak compose setup
infra/keycloak/        Keycloak local realm import for Phase 1
specs/                 Spec Kit feature artifacts
docs/setup/            Setup and tester orientation documentation
docs/testing/          Test architecture and quality baseline documentation
docs/architecture/     Modular monolith architecture notes
knowledge-vault/       Structured Obsidian learning system
.kilo/                 Current Kilo project configuration location
.specify/              Spec Kit memory and templates
```

The Obsidian vault remains one existing learning system. Use the established top-level structure:
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/` for project and feature knowledge
- `knowledge-vault/02 Areas/` for long-lived learning areas, including technical learning, business/product/testing thinking, and interview capital
- `knowledge-vault/03 Resources/` for reusable external materials such as books, official docs, papers, repositories, and attachments
- `knowledge-vault/04 Archives/`, `05 Templates/`, `06 MOCs/`, and `07 Dashboards/` for archived material, templates, maps of content, and dashboards

The Phase 0 hub is `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Phase 0 - Foundation/Phase 0 - Project Foundation and Running Skeleton.md`. The Phase 1 hub is `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/01 Phase 1 - Merchant Registry/Phase 1 - Merchant Registry and Activation.md`.

Do not introduce `.kilocode/` as a new Phase 0 project-organization target. Historical or generated files may exist, but current Phase 0 documentation and implementation should use `.kilo/` for Kilo project configuration references.

## Prerequisites

- Java JDK 25
- Docker with Docker Compose
- Node.js compatible with Nuxt 4 tooling, currently Node `^22.12.0 || ^24.11.0 || >=26.0.0`
- pnpm
- Bash-compatible shell for `apps/backend/mvnw`

## Version Validation Record

Validated on 2026-05-18 before dependency scaffolding:
- Java JDK 25: configured via Maven compiler release `25`
- Maven Wrapper: Maven `3.9.11`
- Spring Boot: `4.0.6`
- Spring Framework: aligned by Spring Boot `4.0.6`
- Spring Modulith: `2.0.6`
- JUnit: `6.0.3`
- REST Assured: `6.0.0`
- Testcontainers core: `2.0.5`; module-specific `junit-jupiter` and `postgresql` artifacts are deferred because Maven Central still publishes those modules on the `1.21.4` line
- WireMock standalone: `3.13.2`
- Nuxt: `4.4.6`
- Nuxt UI: `4.7.1`
- TypeScript: `6.0.3`
- Pinia module: `@pinia/nuxt 0.11.3` with `pinia 3.0.4`
- Zod: `4.4.3`
- Playwright: `@playwright/test 1.61.0`
- PostgreSQL image: `postgres:18`
- Keycloak image: `keycloak/keycloak:26.6.1`

## Local Environment

Day-to-day Podman stack, Playwright POM, and Caddy/HTTPS: [docs/setup/run-stack-and-pom.md](docs/setup/run-stack-and-pom.md).

The concrete local service environment example is `infra/compose/.env.example`.

Application-level non-secret variables:
- `SPRING_PROFILES_ACTIVE`
- `APP_POSTGRES_HOST`
- `APP_POSTGRES_PORT`
- `NUXT_PUBLIC_API_BASE_URL`
- `NUXT_PUBLIC_KEYCLOAK_URL`
- `NUXT_PUBLIC_KEYCLOAK_REALM`
- `NUXT_PUBLIC_KEYCLOAK_CLIENT_ID`

Do not commit real secrets. Phase 0 does not require production credentials, business realm variables, or full application auth variables.

## Backend Commands

From `apps/backend`:

```bash
./mvnw test
./mvnw verify
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

> **Local dev profile required.** The `dev` profile activates CORS for the Nuxt frontend on `http://localhost:3000`. Running without `-Dspring-boot.run.profiles=dev` or `SPRING_PROFILES_ACTIVE=dev` fails at startup with a missing `corsConfigurationSource` bean. The `test` profile is for the test suite only. Do not disable security or CORS as a workaround.

The backend exposes the public technical status endpoint and secured merchant endpoints:

```text
GET /api/status
POST /api/merchants
GET /api/merchants
GET /api/merchants/{id}
POST /api/merchants/{id}/activate
POST /api/merchants/{id}/suspend
```

Expected response shape:

```json
{"application":"payment-quality-lab","phase":"foundation","status":"UP"}
```

## Frontend Commands

From `apps/frontend`:

```bash
pnpm install
pnpm dev
pnpm typecheck
pnpm build
corepack pnpm exec playwright test
corepack pnpm exec playwright test --config playwright.live.config.ts
```

If `pnpm` is not installed as a shell command, use Corepack: `corepack pnpm <command>`.

The standard Chromium suite uses mocked sessions. `playwright.live.config.ts` is a separate assurance project: it requires a running local backend/Keycloak and environment-supplied test-user passwords; runtime storage states are ignored and must never be committed.

## Infrastructure Commands

From the repository root:

```bash
cp infra/compose/.env.example infra/compose/.env
scripts/dev-stack.sh
scripts/dev-stack.sh --stop
scripts/dev-stack.sh --down
```

Compose-only (Postgres + Keycloak, no apps):

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

HTTPS overlay (mkcert + Caddy): see [docs/setup/tls-lab.md](docs/setup/tls-lab.md) and `scripts/dev-stack.sh --tls`.

See `docs/setup/local-infra.md` for details.

## Baseline Verification

- Backend: `apps/backend ./mvnw test`, `./mvnw verify` (Codex broad validation excludes `restkit/**` and `paymentsupport/**` unless explicitly requested)
- Standalone REST Assured: `apps/api-tests` baseline is Surefire 79/79 and Failsafe 72/72 (2026-07-13)
- Frontend: `apps/frontend corepack pnpm typecheck`, `corepack pnpm test:unit`, `corepack pnpm exec playwright test`; standard Chromium closure baseline is 82/82 (2026-07-13)
- Live assurance: run only with explicitly supplied local test credentials and the `playwright.live.config.ts` or `playwright.pom.config.ts` project; see `status/evidence/latest-validation.md` for its current validation state
- Infrastructure: `scripts/dev-stack.sh` or Docker Compose from `docs/setup/local-infra.md`; TLS overlay in `docs/setup/tls-lab.md`
- Documentation: follow the Tester Orientation Pack in `docs/setup/phase-0-tester-orientation-pack.md`

## Tester Focus

Testing focuses on merchant/payment contracts, lifecycle transitions, IAM and tenant boundaries, audit/user-management flows, persistence effects, frontend feedback states, and parallel-safe test data.
