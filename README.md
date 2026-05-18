# Payment Quality Engineering Lab

Phase 0 establishes the project foundation and running skeleton for a learning-oriented payment quality engineering lab. It creates the monorepo shape, backend skeleton, frontend skeleton, local infrastructure configuration, test architecture, and tester-facing documentation without implementing payment business behavior.

## Phase 0 Scope

In scope:
- Runnable Spring Boot backend foundation under `apps/backend`
- Runnable Nuxt dashboard foundation under `apps/frontend`
- Local PostgreSQL 18 and Keycloak 26.6.1 infrastructure configuration under `infra`
- Baseline test structure for backend, REST, Testcontainers, WireMock, and Playwright growth
- Documentation and Obsidian-compatible milestone notes

Out of scope:
- Payment business use cases
- `POST /payments`
- Kafka
- PSP integration or PSP mock flows
- Complete OAuth/OIDC application integration
- Complete merchant, admin, risk, operations, or reconciliation dashboards
- Payment persistence or domain entities

## Repository Map

```text
apps/backend/          Java 25 Spring Boot 4 backend foundation
apps/frontend/         Nuxt 4 dashboard foundation
infra/compose/         Local PostgreSQL and Keycloak compose setup
infra/keycloak/        Keycloak Phase 0 notes and future realm import area
specs/                 Spec Kit feature artifacts
docs/setup/            Setup and tester orientation documentation
docs/testing/          Test architecture and quality baseline documentation
docs/architecture/     Modular monolith architecture notes
knowledge-vault/       Obsidian-compatible learning milestone notes
.kilo/                 Current Kilo project configuration location
.specify/              Spec Kit memory and templates
```

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
- Playwright: `@playwright/test 1.60.0`
- PostgreSQL image: `postgres:18`
- Keycloak image: `keycloak/keycloak:26.6.1`

## Local Environment

The concrete local service environment example is `infra/compose/.env.example`.

Application-level non-secret variables documented for later use:
- `SPRING_PROFILES_ACTIVE`
- `APP_POSTGRES_HOST`
- `APP_POSTGRES_PORT`
- `NUXT_PUBLIC_API_BASE_URL`
- `NUXT_PUBLIC_KEYCLOAK_URL`

Do not commit real secrets. Phase 0 does not require production credentials, business realm variables, or full application auth variables.

## Backend Commands

From `apps/backend`:

```bash
./mvnw test
./mvnw spring-boot:run
```

The backend exposes only a technical status endpoint:

```text
GET /api/status
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
```

If `pnpm` is not installed as a shell command, use Corepack: `corepack pnpm <command>`.

The frontend is a foundation dashboard shell only. Dashboard areas are placeholders for later phases.

## Infrastructure Commands

From the repository root:

```bash
cp infra/compose/.env.example infra/compose/.env
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

See `docs/setup/local-infra.md` for details.

## Baseline Verification

- Backend: `apps/backend ./mvnw test`
- Frontend: `apps/frontend pnpm typecheck`, `pnpm build`, `corepack pnpm exec playwright test`
- Infrastructure: Docker Compose startup from `docs/setup/local-infra.md`
- Documentation: follow the Tester Orientation Pack in `docs/setup/phase-0-tester-orientation-pack.md`

## Tester Focus

Phase 0 testing focuses on setup reproducibility, skeleton observability, documentation accuracy, non-goal enforcement, module-boundary readiness, and parallel-test readiness. It does not test payment behavior because no payment behavior exists.
