# Implementation Plan: Phase 0 - Project Foundation & Running Skeleton

**Branch**: `001-project-foundation` | **Date**: 2026-05-18 | **Spec**: `specs/001-project-foundation/spec.md`

**Input**: Feature specification from `specs/001-project-foundation/spec.md`

## 1. Technical Summary

Phase 0 establishes the Payment Quality Engineering Lab as a runnable, testable monorepo skeleton. The implementation creates a Spring Boot 4+ backend on Java 25 with Maven 3.9.11, Maven Wrapper, Spring Framework 7+, Spring Modulith 2.0.6, Bean Validation, REST foundation behavior, and baseline automated checks. It also initializes a Nuxt 4 frontend from the Nuxt Dashboard Template with Nuxt UI, TypeScript 6, Zod, Pinia via `@pinia/nuxt`, pnpm, and Playwright 1.60 structure.

The phase includes local infrastructure configuration for PostgreSQL 18 and Keycloak 26.6.1 through Docker Compose under `infra/compose`, with Keycloak configuration assets under `infra/keycloak`. It deliberately avoids payment workflows, payment persistence, PSP integration, Kafka, complete OAuth/OIDC application integration, and real dashboard business functionality.

The main output is a coherent foundation that new contributors can run and testers can verify. The implementation should prove that backend, frontend, infrastructure configuration, documentation, and future testing areas exist and are aligned with the constitution.

## 2. Architecture Decisions

- **Monorepo layout**: Use `apps/backend`, `apps/frontend`, `infra`, `specs`, `docs`, `knowledge-vault`, `.kilo`, and `.specify` so product code, infrastructure, specs, documentation, current Kilo project configuration, and learning outputs remain separated. Phase 0 must not introduce a new `.kilocode/` project-configuration target; existing generated or historical files may remain untouched, but implementation should not deepen that dependency.
- **Backend architecture**: Start with a Spring Boot modular-monolith foundation using Spring Modulith 2.0.6 from day one. Do not create fake business modules before payment behavior exists.
- **Initial backend module stance**: Create a foundation-level application shell and a narrow technical status area. Reserve future module locations and naming conventions, but defer explicit business module shells such as `payment`, `merchant`, `settlement`, or `reconciliation` until a real feature owns behavior.
- **Status endpoint**: Add a minimal non-business status/readiness endpoint, for example `GET /api/status`, returning application name, phase, and readiness state without database secrets, identity details, or payment data.
- **Spring Security**: Include Spring Security only if dependency alignment with future OAuth/OIDC work is low-risk and the skeleton remains accessible for local Phase 0 verification. If included, configure only safe defaults for the technical status endpoint and document that full auth is deferred.
- **Database stance**: Plan PostgreSQL 18 support through dependencies and local Compose. Do not create payment domain tables or migrations in Phase 0. Application startup should not require business schema.
- **Identity stance**: Provide Keycloak 26.6.1 local infrastructure as a visible baseline. Do not wire full login, role, permission, token lifecycle, or frontend route protection in Phase 0.
- **Frontend architecture**: Keep the Nuxt Dashboard Template as the UI base. Limit project-specific changes to naming, a foundation landing/dashboard placeholder, and setup needed for Zod, Pinia, TypeScript, and Playwright.
- **Testing architecture**: Establish layered test folders and naming conventions for unit, architecture, integration, REST API, and browser tests. Keep tests order-independent and data-light.
- **Kafka exclusion**: Kafka is explicitly excluded from Phase 0. Future event-driven learning may begin with in-process Spring Modulith events before external brokers are justified.

## 3. Repository Structure

Planned source layout:

```text
/home/suso/job-learn/
├── apps/
│   ├── backend/
│   │   ├── .mvn/
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── pom.xml
│   │   ├── README.md
│   │   └── src/
│   │       ├── main/
│   │       │   ├── java/
│   │       │   │   └── lab/paymentquality/
│   │       │   │       ├── PaymentQualityApplication.java
│   │       │   │       ├── foundation/
│   │       │   │       │   └── status/
│   │       │   │       └── shared/
│   │       │   │           └── web/
│   │       │   └── resources/
│   │       │       └── application.yml
│   │       └── test/
│   │           ├── java/
│   │           │   └── lab/paymentquality/
│   │           │       ├── architecture/
│   │           │       ├── foundation/
│   │           │       ├── integration/
│   │           │       └── rest/
│   │           └── resources/
│   └── frontend/
│       ├── app/
│       ├── components/
│       ├── pages/
│       ├── stores/
│       ├── schemas/
│       ├── tests/
│       │   └── e2e/
│       ├── nuxt.config.ts
│       ├── package.json
│       ├── pnpm-lock.yaml
│       ├── playwright.config.ts
│       └── README.md
├── infra/
│   ├── compose/
│   │   ├── compose.yml
│   │   └── .env.example
│   └── keycloak/
│       ├── realms/
│       └── README.md
├── specs/
│   └── 001-project-foundation/
│       ├── spec.md
│       └── plan.md
├── docs/
│   ├── setup/
│   ├── testing/
│   └── architecture/
├── knowledge-vault/
│   ├── 01 Project - Payment Quality Engineering Lab/
│   │   └── 01 Phase 0 - Foundation/
│   ├── 02 Technical Learning/
│   │   ├── Spring Modulith/
│   │   ├── Testing Architecture/
│   │   └── Infrastructure/
│   ├── 03 Business Product and Testing Thinking/
│   └── 04 Interview Capital/
├── .kilo/
├── .specify/
├── README.md
├── AGENTS.md
└── .gitignore
```

Structure decision: keep a two-application monorepo with independent backend and frontend toolchains, plus shared local infrastructure. The backend Maven Wrapper lives inside `apps/backend` because the backend is the Maven project. The frontend pnpm workspace may remain local to `apps/frontend` in Phase 0 unless a later phase introduces shared frontend packages.

Configuration-directory decision: use the current Kilo project configuration location `.kilo/` where Phase 0 documentation references agent or command configuration. Do not move existing configuration directories during Phase 0 artifact correction or implementation, and do not create `.kilocode/` as a new target for project organization.

## 4. Backend Plan

### Backend Setup

- Before creating dependency files, verify official coordinates, exact usable versions, and compatibility assumptions for Java JDK 25, Maven 3.9.11, Spring Boot 4+, Spring Framework 7+, Spring Modulith 2.0.6, JUnit 6, Nuxt 4, TypeScript 6, Playwright 1.60, PostgreSQL 18, and Keycloak 26.6.1. Record selected versions and any justified substitutions before dependency scaffolding begins.
- Initialize `apps/backend` as a Maven project using Maven Wrapper and a Maven 3.9.11 wrapper distribution.
- Set Java compiler release to 25 and fail fast when the wrong JDK is used.
- Use Spring Boot 4+ dependency management so Spring Framework 7+ versions are aligned by the platform.
- Add dependencies for Spring Web, Bean Validation, Spring Test, Spring Modulith 2.0.6, JUnit 6, AssertJ, Mockito, REST Assured, Testcontainers, PostgreSQL JDBC, and optionally Spring Security.
- Keep WireMock out of production dependencies. If included now, include it only as a test-scoped future-ready dependency or document it as a later addition.
- Keep application configuration minimal: application name, server port convention, profile placeholders, and non-secret local environment variables.

### Initial Packages And Modules

Use `lab.paymentquality` as the root package.

- `lab.paymentquality`: Spring Boot application entry point.
- `lab.paymentquality.foundation.status`: technical status endpoint and DTOs.
- `lab.paymentquality.shared.web`: cross-cutting web concerns only when needed, such as technical error response conventions. Avoid generic utility dumping.
- `lab.paymentquality.architecture` under tests: Modulith architecture verification.
- `lab.paymentquality.integration` under tests: future Spring integration and Testcontainers tests.
- `lab.paymentquality.rest` under tests: future REST Assured tests.

Phase 0 should not create explicit business module shells such as `payment`, `merchant`, `psp`, `refund`, `settlement`, or `reconciliation`. Those names should be documented as intended future modules, not implemented packages, until behavior exists. This avoids fake empty overengineering while still allowing Spring Modulith to enforce the application shape from day one.

### Minimal Backend Behavior

- Implement a foundation status endpoint such as `GET /api/status`.
- Response should be stable and testable, for example `{"application":"payment-quality-lab","phase":"foundation","status":"UP"}`.
- Do not expose environment variables, database credentials, Keycloak details, build secrets, or payment concepts.
- If Spring Boot Actuator is considered, prefer deferring it unless it is explicitly needed. A custom technical endpoint is enough for Phase 0 and keeps scope clear.

### Backend Tests

- Add a Spring context smoke test to verify the application starts.
- Add a unit or slice test for the status endpoint behavior.
- Add a Spring Modulith architecture test using `ApplicationModules.of(PaymentQualityApplication.class).verify()`.
- Add a REST Assured test area and either a minimal local-port status endpoint test or a documented placeholder if the implementation chooses not to start the full server in Phase 0.
- Add Testcontainers conventions and dependency setup, but do not require a PostgreSQL container for every baseline test unless a real persistence behavior is introduced.
- Configure Surefire for fast unit, slice, and architecture checks. Reserve Failsafe or naming conventions such as `*IT` for integration tests if they are added.

## 5. Frontend Plan

### Imported Template Functionality

- Initialize `apps/frontend` from the official Nuxt Dashboard Template using the selected documented command, template source, and version or reference available at implementation time. Record the chosen source and initialization approach in `apps/frontend/README.md`.
- Retain Nuxt 4, Nuxt UI, template layout primitives, app shell conventions, and default dashboard structure where useful.
- Preserve template-provided development scripts and visual conventions unless they conflict with project clarity.
- Keep template examples only if they help prove the shell works. Remove or relabel demo content that could be mistaken for implemented payment features.

### Project-Specific Customization

- Rename visible product identity to Payment Quality Engineering Lab.
- Add a project-specific foundation landing or dashboard placeholder stating that payment operations, merchant views, risk review, reconciliation, and admin workflows are future phases.
- Add or retain Zod as the baseline validation library even if Phase 0 only has a small environment/config schema or placeholder schema area.
- Add Pinia via `@pinia/nuxt` and create a minimal store convention only when useful, such as a small app-shell store for non-business UI state. Do not create payment state.
- Confirm TypeScript 6 baseline in `package.json` and lockfile.
- Use pnpm as the package manager and document exact commands.

### Frontend Folder Conventions

- `app/` or Nuxt template root structure: Nuxt application shell.
- `pages/`: route-level views, starting with the foundation dashboard or landing route.
- `components/`: reusable UI components, limited to shell-level components in Phase 0.
- `stores/`: Pinia stores, initially shell-only or empty with README guidance.
- `schemas/`: Zod schemas for config or future API contracts, with no payment domain model yet.
- `tests/e2e/`: Playwright foundation tests.

### Deferred Frontend Work

- No payment creation, payment list, merchant onboarding, PSP callback, refund, settlement, reconciliation, or risk-review dashboard behavior.
- No real login UX, role-based navigation, token refresh, or Keycloak-backed route guards.
- No frontend API client for payment business operations.

## 6. Infrastructure Plan

### Local Compose

- Create `infra/compose/compose.yml` with PostgreSQL 18 and Keycloak 26.6.1 services.
- Use predictable service names such as `payment-quality-postgres` and `payment-quality-keycloak`.
- Expose conventional local ports while documenting how to change them if conflicts occur.
- Keep database credentials local-development only and sourced from an example environment file.
- Configure Keycloak to use a local development database option appropriate for Phase 0. If Keycloak uses its own internal dev database in Phase 0, document that this is not production-like. If it shares PostgreSQL, use a separate database/schema from future application data. Do not create a business realm, business roles, or full application OAuth/OIDC integration unless a minimal deterministic startup placeholder is strictly required and documented as non-business.

### Environment Variables

Create `infra/compose/.env.example` with non-secret examples:

```text
POSTGRES_DB=payment_quality_lab
POSTGRES_USER=payment_quality
POSTGRES_PASSWORD=payment_quality_dev
POSTGRES_PORT=5432
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_PORT=8081
```

Backend and frontend environment conventions should be documented separately where needed, for example:

- `SPRING_PROFILES_ACTIVE=local`
- `APP_POSTGRES_HOST=localhost`
- `APP_POSTGRES_PORT=5432`
- `NUXT_PUBLIC_API_BASE_URL=http://localhost:8080`
- `NUXT_PUBLIC_KEYCLOAK_URL=http://localhost:8081`

Values are examples only. Phase 0 must not require real production secrets.

Environment deliverable: `infra/compose/.env.example` is the concrete example file for local service variables. Root, backend, and frontend documentation should link to it or document app-specific non-secret variables; Phase 0 should not scatter competing `.env.example` files unless a tool requires them.

### Startup Instructions

- Document `docker compose --env-file .env -f infra/compose/compose.yml up -d` or the exact command selected.
- Document service URLs, expected ports, readiness checks, shutdown command, and troubleshooting for port conflicts.
- Keep application auth integration deferred. Infrastructure exists so later security work has a known baseline.

## 7. Testing Strategy

### Backend Test Layers

- **Unit tests**: fast tests for records, services, validators, and controllers when they contain logic. In Phase 0, likely limited to status response behavior.
- **Architecture verification tests**: Spring Modulith `ApplicationModules.verify()` must run as part of the default backend verification.
- **Spring integration tests**: reserved under `src/test/java/.../integration` for context, persistence, and module integration checks. Use `*IT` naming if separated by Failsafe.
- **REST Assured tests**: included in the backend testing foundation under `src/test/java/.../rest` for HTTP-level API checks. Phase 0 may use REST Assured only for a minimal technical smoke or contract-style verification of the foundation status endpoint if reliable and fast; no payment business API testing exists yet.
- **Testcontainers tests**: dependencies and conventions established now; PostgreSQL containers used only when persistence behavior exists or when a foundation connectivity smoke test is intentionally added.
- **WireMock tests**: folder and dependency strategy documented for future PSP and external-service testing. Do not build PSP mocks in Phase 0.

### Frontend Test Layers

- **Type checking**: Nuxt/TypeScript command verifies the frontend compiles under TypeScript 6.
- **Unit/component tests**: optional in Phase 0 unless the template already includes a compatible setup. Do not introduce heavy frontend test infrastructure without a component behavior to test.
- **Playwright foundation tests**: create baseline structure and, if practical, one non-business smoke test that loads the foundation dashboard and checks visible project identity.
- **Future E2E scenarios**: merchant, admin, payment operations, risk-review, reconciliation, and auth flows are deferred.

### Quality Commands

Document and make runnable:

- Backend: `./mvnw test` from `apps/backend`.
- Backend integration later: `./mvnw verify` when Failsafe/Testcontainers checks exist.
- Frontend install: `pnpm install` from `apps/frontend`.
- Frontend dev: `pnpm dev`.
- Frontend checks: `pnpm typecheck` and a template-appropriate lint/test command if present.
- Playwright foundation: `corepack pnpm exec playwright test` once the baseline is configured.
- Infra: Docker Compose startup and shutdown commands.

## 8. Parallel-Readiness Strategy

- Baseline tests must be order-independent and must not rely on payment business data.
- Tests must avoid shared mutable state. If future tests need state, use unique namespacing, generated identifiers, transactions, isolated containers, or per-worker fixtures.
- Backend unit and architecture tests should not mutate global static state.
- Integration tests that use PostgreSQL must eventually isolate data by schema, transaction rollback, truncation strategy, or worker-specific namespaces.
- REST Assured tests must use explicit ports and avoid assumptions that block parallel test forks.
- Testcontainers usage should prefer reusable conventions without hidden singleton state that prevents parallelization.
- Playwright tests should be structured so future worker-aware account and data allocation is possible. Do not hard-code a single mutable user or shared payment record.
- Frontend tests should use resilient locators based on roles, labels, or stable test ids where appropriate, not visual text that will frequently change unless the text is the behavior under test.
- Phase 0 does not need to enable full parallel execution in CI. It must avoid choices that make parallel execution difficult later.

## 9. Spring Modulith Strategy

### Introduction In Phase 0

- Add Spring Modulith 2.0.6 dependencies from the first backend implementation.
- Use the root package and package organization so Spring Modulith can discover application modules.
- Add an architecture verification test equivalent to `ApplicationModules.of(PaymentQualityApplication.class).verify()`.
- Treat the architecture test as a default quality gate, not optional documentation.

### What Gets Verified Now

- The current application package structure is valid for Modulith analysis.
- There are no cyclic module dependencies.
- No code reaches into another module's internal package because no business modules exist yet.
- The foundation status area remains technical and does not create fake payment-module coupling.

### Future Module Expansion

Future modules should be introduced only when a feature creates real behavior. Expected future module candidates include:

- `payment`: payment initiation and lifecycle ownership.
- `merchant`: merchant-facing configuration and ownership.
- `psp`: payment service provider integration boundary.
- `webhook`: inbound provider callback handling.
- `refund`: refund use cases.
- `settlement`: settlement and payout preparation.
- `reconciliation`: comparison between internal and external records.
- `riskreview`: manual or automated risk decision support.
- `audit`: audit trail or observability-related history when justified.

When each module appears, the plan or feature spec must define public API, internal packages, allowed dependencies, event impact, and module tests.

### ApplicationModuleTest Fit

- Do not add `@ApplicationModuleTest` without a real module behavior to test.
- Reserve module test conventions now so future module tests can validate a module alone, with direct dependencies, or with the full dependency tree depending on risk.
- Use module tests later for domain behavior, module interaction, and event-driven reactions.

### Documentation Generation

- Defer generated Modulith documentation until the first meaningful business module exists or until Phase 0 implementation can generate useful diagrams without noise.
- In Phase 0, create human-written architecture notes explaining the modular-monolith direction and the purpose of `ApplicationModules.verify()`.
- Later phases may generate module canvases or component diagrams into `docs/architecture/modules` and link them from Obsidian notes.

## 10. Documentation and Learning Outputs

### Repository Documentation

- Root `README.md`: project purpose, Phase 0 scope, top-level directory map, prerequisites, setup overview, baseline commands, and explicit non-goals.
- `apps/backend/README.md`: backend prerequisites, Maven Wrapper usage, Java 25 requirement, run command, test command, package/module conventions, and Modulith architecture check.
- `apps/frontend/README.md`: pnpm usage, Nuxt dev command, TypeScript check, Playwright foundation command, template vs project-specific content.
- `infra/compose/README.md` or `docs/setup/local-infra.md`: PostgreSQL 18 and Keycloak 26.6.1 startup, environment variables, ports, shutdown, troubleshooting, and auth deferral notice.
- `docs/testing/phase-0-quality-baseline.md`: test layers, current commands, future test areas, parallel-readiness principles, Testcontainers and WireMock conventions.
- `docs/architecture/modular-monolith-foundation.md`: Spring Modulith strategy, current module stance, future module candidates, architecture verification explanation.

### Tester Orientation Pack

After implementation, create a Phase 0 Tester Orientation Pack covering:

- What exists and what is intentionally absent.
- How to run backend, frontend, and local infrastructure.
- How to run baseline verification.
- How to interpret the status endpoint and frontend placeholder.
- Where future unit, integration, REST, module, Playwright, Testcontainers, and WireMock tests belong.
- What Spring Modulith verifies now and why testers should care.
- Phase 0 exploratory testing charters: setup reproducibility, docs accuracy, port conflicts, failure messages, no accidental payment behavior, and test isolation review.

### Obsidian-Compatible Notes

Create or reserve notes under the categorized single-vault learning structure:

- `knowledge-vault/01 Project - Payment Quality Engineering Lab/01 Phase 0 - Foundation/Phase 0 - Project Foundation and Running Skeleton.md`
- `knowledge-vault/02 Technical Learning/Spring Modulith/Architecture - Modular Monolith with Spring Modulith.md`
- `knowledge-vault/02 Technical Learning/Spring Modulith/Architecture Test - ApplicationModules.verify.md`
- `knowledge-vault/02 Technical Learning/Testing Architecture/Testing - Phase 0 Quality Baseline.md`
- `knowledge-vault/02 Technical Learning/Testing Architecture/Testing - Parallel Readiness Principles.md`
- `knowledge-vault/02 Technical Learning/Infrastructure/Infrastructure - Local PostgreSQL 18 and Keycloak 26.6.1.md`
- `knowledge-vault/03 Business Product and Testing Thinking/README.md`
- `knowledge-vault/04 Interview Capital/Interview Story - Why Foundation Before Payment Features.md`

These notes should be Markdown and Obsidian-friendly, using repository-relative links where practical. The vault remains a single vault, but it must clearly separate project/feature knowledge, technical learning, business/product/testing thinking, and interview capital.

## 11. Risks / Open Questions

- **Spring Boot 4+ artifact availability**: verify current official Spring Boot 4 and Spring Framework 7 coordinates during implementation. If exact released versions differ, document the selected supported version and rationale.
- **JUnit 6 availability and Maven plugin support**: verify current JUnit 6 coordinates and Surefire compatibility. If ecosystem support requires specific plugin versions, pin them explicitly.
- **TypeScript 6 availability with Nuxt 4 tooling**: confirm Nuxt compatibility before pinning. If Nuxt template tooling constrains TypeScript version, document the compatible resolution.
- **Nuxt Dashboard Template shape**: template contents may include demo functionality that resembles business behavior. Remove or relabel demo content to prevent scope confusion.
- **Spring Security inclusion**: including it too early can complicate local verification. If included, keep the Phase 0 endpoint accessible and document that complete auth is deferred.
- **Keycloak database choice**: decide whether Keycloak uses its own dev storage or a PostgreSQL database in Compose. Either choice must be explicit and not confused with application payment data.
- **Generated Modulith docs timing**: generating docs too early may add noise. Prefer human-written notes now unless generated output is demonstrably useful.
- **Monorepo command orchestration**: a root task runner is not required in Phase 0. Avoid adding one unless it reduces documented setup friction without increasing maintenance.

## 12. Definition of Done

Phase 0 is done when all of the following are true:

- Repository layout contains coherent `apps/backend`, `apps/frontend`, `infra`, `specs`, `docs`, and `knowledge-vault` areas.
- Repository documentation identifies `.kilo/` as the current Kilo project configuration location and does not introduce `.kilocode/` as a new Phase 0 organization target.
- Backend is runnable with Java 25 and Maven Wrapper.
- Backend Maven baseline uses Maven Wrapper configured for Maven 3.9.11.
- Backend dependency management is aligned with Spring Boot 4+ and Spring Framework 7+.
- Spring Modulith 2.0.6 is present from the beginning.
- Backend exposes a minimal foundation status/readiness endpoint with no payment behavior.
- Backend compiles and default baseline tests pass.
- Spring Modulith architecture verification exists and passes.
- Initial backend test conventions exist for unit, architecture, integration, REST Assured, Testcontainers, and future WireMock usage.
- Frontend is initialized from the Nuxt Dashboard Template and runs locally.
- Frontend retains Nuxt UI, TypeScript 6 baseline, Zod, Pinia via `@pinia/nuxt`, and pnpm.
- Frontend has a project-specific foundation placeholder without business dashboard functionality.
- Playwright 1.60 baseline structure exists and can host future E2E tests.
- Local PostgreSQL 18 and Keycloak 26.6.1 Compose configuration exists.
- Environment variable examples exist and contain no real secrets.
- Documentation explains setup, verification commands, infrastructure startup, architecture direction, test strategy, and Phase 0 non-goals.
- Tester Orientation Pack content is created or clearly staged for immediate post-implementation completion.
- Obsidian-compatible learning notes exist in the categorized single-vault structure or are explicitly created as part of the implementation closeout.
- No payment business endpoint, payment persistence, PSP mock flow, Kafka setup, complete OAuth/OIDC flow, or complete payment dashboard feature has slipped into Phase 0.

## 13. Implementation Readiness Assessment

The feature is ready for implementation after the early version-validation gate records official coordinates, exact usable versions, compatibility assumptions, and any justified substitutions before dependency scaffolding. The specification is sufficiently scoped, the constitution gates are satisfied, and the technology baseline is explicit.

Recommended next step: run a second `/speckit.analyze` pass against the corrected artifacts, then proceed to `/speckit.implement` only if no blocking findings remain.
