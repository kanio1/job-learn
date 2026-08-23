# Payment Quality Engineering Lab - Codex CLI Guide

## Project Identity

Payment Quality Engineering Lab is a learning-oriented payment platform used to practice Java/Spring backend engineering, REST API testing, security testing, frontend contract consumption, and SDET review skills.

Implementation is done by the main Codex CLI session. Domain helper skills are for mapping, planning, mentoring, test design, and review. Engineering process skills in `.agents/skills/` are used by that session when building, reviewing, or writing REST Assured, Playwright E2E, and Playwright REST tests. Tracker: `docs/agents/issue-tracker.md`. Index: `.agents/skills/README.md`.

## Codex Execution Overlay

`AGENTS.md` is the single primary Codex agent instruction file for this repository. Do not create or keep additional `AGENTS-overlay.md` files.

`.codex/**` is the mutable Codex execution layer. Use it for continuation notes, execution status, review checklists, and prompts. OpenCode also uses `.codex/**` as the shared execution overlay; `opencode.jsonc` loads the same instruction files.

`.kiro/**` is historical prior art. Codex may read `.kiro/specs/**` for old requirements, but must not check or uncheck tasks or rewrite those plans. Track new work in `.codex/` (`docs/agents/issue-tracker.md`).

Recommended read order for current execution work:

1. `AGENTS.md`
2. `.agents/skills/README.md` — engineering process skills
3. `docs/agents/issue-tracker.md` — local markdown tracker
4. `.codex/README.md`
5. `.codex/current-state.md`
6. `status/evidence/latest-validation.md`
7. relevant current implementation and tests
8. Event Streaming Lab: `.agents/skills/eventlab-kafka` then `status/roadmaps/kafka-event-streaming-lab/` (next `KAFKA-T02`)

## Agent skills

### Issue tracker

Local markdown under `.codex/`. See `docs/agents/issue-tracker.md`. This lab does not use GitHub Issues or Linear as the work tracker.

### Triage labels

`Status:` line on those files. See `docs/agents/triage-labels.md`.

### Domain docs

Repo map in root `CONTEXT.md`; glossary in `.codex/CONTEXT.md`; ADRs in `.codex/adr/`. See `docs/agents/domain.md`.

## Current Phase

The checked-out branch is in the Payment Orders / lifecycle / HTTP contract hardening phase. The codebase is beyond Phase 1 merchant-only assumptions.

For assurance work, verify the actual branch with `git branch --show-current`; the current execution registry is `status/index.md`. Do not rely on the historical `018-rest-security-p1-error-auth-method-hardening` continuation reference.

**Event Streaming Lab (ADR 0002 ACCEPTED 2026-08-23):** docs-ready, **code not started**. Next implementation is E1 (`KAFKA-T02`). Follow `.agents/skills/eventlab-kafka` and `status/roadmaps/kafka-event-streaming-lab/`. Lenses is a telescope, not a product UI.

Implemented domain scope:

- Merchant Registry: create, list, retrieve, activate, suspend.
- Payment Orders: merchant-scoped create/read/list/summary.
- Payment Lifecycle: authorize, capture, cancel, refund, metadata PATCH, status history, ETag/If-Match and idempotency hardening.
- IAM, tenant isolation, user management, and audit log: implemented current domain capabilities.

## Active Non-Goals

- No `POST /payments` top-level API.
- No real PSP provider integration or PSP failure modeling beyond the existing local mock boundary.
- Kafka **only** in module `eventlab` / overlay `compose.kafka.yml` / Failsafe `*KafkaIT` (ADR 0002). Still out: Kafka as command bus, product merchant webhooks, Schema Registry / Streams / SCRAM / EOS in wave 1, AKHQ/kafka-ui, lag dashboards/ECharts, Event Lab as a Kafka console, second host broker on 9092 beside the lab overlay.
- No settlement, payout, reconciliation-as-product, KYC, card/PAN/PCI, 3DS, or microservice split.
- No complete OAuth/OIDC production integration; local Keycloak is for development and tests.
- No fake KPI dashboard or broad business dashboard.
- Do not recreate `.kilo/` or `.kilocode/`. Skills live in `.agents/skills/` (Cursor: `.cursor/skills/` symlinks).

Lenses CE + MCP (`Lenses` in `~/.grok/config.toml`) are **operator playground / luneta**. Framing lab≠prod (RF=1 is not a product bug): `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md`.

## Tech Stack

- Backend: Java 25, Maven Wrapper 3.9.11, Spring Boot 4.0.6 / Spring Framework 7, Spring Modulith 2.0.6.
- Persistence: PostgreSQL 18, Flyway migrations, JPA with `ddl-auto: validate`.
- Security: Spring Security JWT resource server, Keycloak realm roles mapped to `platform:*` and `merchant:*` authorities.
- Tests: JUnit 6, AssertJ, Mockito, REST Assured 6, Testcontainers PostgreSQL, Spring Modulith tests.
- Frontend: Nuxt 4 app directory, Nuxt UI, TypeScript 6, Pinia, Zod, `nuxt-auth-utils`, Playwright 1.61.0.

## Backend Commands

Run from `apps/backend`:

```bash
./mvnw test
./mvnw verify
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

The `dev` profile is required for local startup — it activates the CORS bean needed by the Nuxt frontend on `http://localhost:3000`. Running without it fails at startup.

`./mvnw test` runs `*Test.java`. `./mvnw verify` also runs `*IT.java` through Failsafe.

Codex backend validation must skip tests under:

- `apps/backend/src/test/java/lab/paymentquality/restkit/`
- `apps/backend/src/test/java/lab/paymentquality/paymentsupport/`

Treat these as excluded suites even when a task generally asks to run backend tests, unless the user explicitly asks to include `restkit/` or `paymentsupport/`.

## Frontend Commands

Run from `apps/frontend`:

```bash
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm lint
corepack pnpm build
corepack pnpm test:e2e
corepack pnpm exec playwright test --config playwright.pom.config.ts
```

Agent live browser (not the 1.61 test runner) — skill `playwright-cli`, from repo root:

```bash
corepack pnpm --dir tools/playwright-cli exec playwright-cli --help
```

Use `pnpm` directly only if it is already available in the shell. Live POM uses real Keycloak storage states and must receive passwords only through environment variables; never track generated state files.

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
- Do not run backend tests from `apps/backend/src/test/java/lab/paymentquality/restkit/` or `apps/backend/src/test/java/lab/paymentquality/paymentsupport/` unless the user explicitly asks for those suites. This is a standing rule for Codex validation, including broad backend test requests.
- Security tests belong in `security` and import `TestJwtConfiguration`.
- Architecture/module boundaries are checked by `ModulithArchitectureTest`, `MerchantModuleTest`, and `PaymentModuleTest`.
- Playwright E2E and BFF REST live under `apps/frontend/tests-pom` (real Keycloak + stack). Vitest is `apps/frontend/tests/unit` and colocated `app/**/*.test.ts`.
- When writing tests test-first, follow `.agents/skills/tdd`. When changing Spring/Java production code, follow `.agents/skills/spring-modulith`. When changing `eventlab` / Kafka overlay / Event Lab UI, follow `.agents/skills/eventlab-kafka` (then `spring-modulith` or `nuxt-frontend`). When changing Nuxt/TypeScript/Nitro production code, follow `.agents/skills/nuxt-frontend`. When writing Playwright POM / live `tests-pom` tests, follow `.agents/skills/playwright-pom`. When reviewing a diff, follow `.agents/skills/code-review` plus the layer skills `java-spring-review`, `rest-api-test-design`, and `playwright-sdet-review`. Operator Kafka inspection uses Lenses skills (`kafka-topic-audit`, `kafka-consumer-lag`, `kafka-dlq-review`) **after** `eventlab-kafka/references/lenses-lab-vs-prod.md`.
- Ignore learner copies such as `My*` and `Lesson*` unless the task explicitly concerns learning files.

## Implementation Rules

- Keep changes scoped to the current `.codex` spec/ticket or explicitly requested small step.
- Preserve Spring Modulith boundaries: public module APIs under module root packages, implementation under `internal`.
- Do not make the payment module depend on `merchant.internal`.
- For tenant isolation work, `merchant` may import only public tenant API from `lab.paymentquality.tenant.*` and must never import `lab.paymentquality.tenant.internal.*`.
- Do not modify payment module source files for tenant isolation work unless the user explicitly changes scope.
- Keep REST contracts stable unless the spec requires a contract change.
- Use Flyway for schema changes and keep JPA mappings consistent with migrations.
- Do not add dependencies without explicit need and approval.
- Kafka code belongs in `lab.paymentquality.eventlab` (not OPEN). Do not put producers/listeners in `shared` or rewrite `audit` onto the broker. Browser never speaks Kafka; no `kafkajs` in `apps/frontend`.
- Follow `status/index.md` for the current execution queue. Event Streaming Lab implementation starts at `KAFKA-T02` after E0 (done). Do not begin the listed Wave 2 packages until the active assurance validation item is closed or the user explicitly reprioritises it.

## Parallel implementation

### Delegation (parent agent)

- Delegate at most three implementation tasks concurrently.
- Assign explicit file and module ownership before delegation; never delegate overlapping files.
- Each implementation agent works in its own Git worktree.
- Integration, final verification and review are performed by the parent agent;
  merging worktree branches locally is part of integration.
- Never push without explicit user instruction.

### Worker rules

- Work only inside paths explicitly assigned in the task.
- If a required change belongs to another session, report the dependency.
- Do not edit files owned by another task.
- Do not run git push, merge, rebase, reset --hard or git clean.
- Do not modify .env files or real credentials.
- Do not change dependency versions unless explicitly requested.
- Run the module-specific formatter, static checks and tests.
- Finish with a clean, reviewable commit.
- Report changed files, commands executed, test results and commit SHA.

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
- Flag scope creep into PSP, settlement, KYC, fake dashboard metrics, Kafka **outside** `eventlab`/overlay, or a Kafka UI clone (Lenses already covers operator view).

## Safety Rules

- Do not revert user changes.
- Do not run destructive git commands unless explicitly requested.
- Do not commit secrets, tokens, real credentials, or private Keycloak/client material.
- Do not recreate `.kilo/` or `.kilocode/`.
- For this Codex setup, only edit support files requested by the user; do not touch application source or existing tests.
