# Phase 0 Tester Orientation Pack

## What Exists

- A monorepo structure for backend, frontend, infrastructure, specs, docs, and knowledge notes
- A Spring Boot backend with `GET /api/status`
- A Nuxt dashboard shell with Nuxt UI, Zod, Pinia, TypeScript, and Playwright foundation
- Docker Compose configuration for PostgreSQL 18 and Keycloak 26.6.1
- Baseline backend, frontend, REST, architecture, and Playwright test locations
- Spring Modulith architecture verification

## What Is Intentionally Absent

- Payment business use cases
- `POST /payments`
- Payment persistence
- Kafka
- PSP integration or PSP mocks
- Complete OAuth/OIDC application integration
- Complete dashboards

## Run Backend

```bash
cd apps/backend
./mvnw test
./mvnw spring-boot:run
```

Check:

```text
GET http://localhost:8080/api/status
```

The response should contain `payment-quality-lab`, `foundation`, and `UP` only.

## Run Frontend

```bash
cd apps/frontend
pnpm install
pnpm dev
pnpm typecheck
pnpm build
corepack pnpm exec playwright test
```

If `pnpm` is not installed as a shell command, use Corepack: `corepack pnpm <command>`.

The root page should show `Payment Quality Engineering Lab` and state that dashboard areas are future phases.

## Run Infrastructure

```bash
cp infra/compose/.env.example infra/compose/.env
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

PostgreSQL and Keycloak are local supporting services only. They do not imply application auth is complete.

## Why Spring Modulith Matters To Testers

Spring Modulith turns architecture rules into executable checks. The current architecture test is small because there are no business modules yet, but it creates a quality gate so future modules cannot silently grow illegal dependencies.

Tester questions for later phases:
- Which module owns this behavior?
- What is the public API?
- What is internal and forbidden to other modules?
- Is direct invocation acceptable or should an event reduce coupling?
- Does this behavior deserve an `@ApplicationModuleTest`?

## Exploratory Testing Charters

- Setup reproducibility: follow docs from a clean checkout and note missing prerequisites.
- Documentation accuracy: compare commands with actual scripts and files.
- Port conflicts: change local ports and confirm docs explain how.
- Failure messages: start services with missing `.env` or wrong ports and inspect actionability.
- Non-goal audit: search for accidental payment, PSP, Kafka, full auth, or complete dashboard behavior.
- Test isolation review: inspect tests for order dependence or shared mutable state.

## FR-to-Check Mapping

- FR-001: Can you identify every top-level project area?
- FR-002: Can you follow setup and verification docs?
- FR-003: Does backend status run without payment data?
- FR-004: Are future module locations and rules explained?
- FR-005: Does Modulith architecture verification exist?
- FR-006: Does the frontend shell run and show future dashboard placeholders?
- FR-007: Are PostgreSQL and Keycloak configured locally without business auth?
- FR-008: Are baseline verification commands documented?
- FR-009: Are unit, module, integration, REST, and browser test areas present or documented?
- FR-010: Are parallel-readiness conventions visible?
- FR-011: Does documentation explain what can be tested and what remains risky?
- FR-012: Can Obsidian milestone notes capture Phase 0?
- FR-013: Are payment workflows, PSP, Kafka, and full OAuth/OIDC absent?
