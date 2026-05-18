# Tasks: Phase 0 - Project Foundation & Running Skeleton

**Input**: `.specify/memory/constitution.md`, `specs/001-project-foundation/spec.md`, `specs/001-project-foundation/plan.md`

**Scope**: Build the runnable foundation only. Do not implement payment business functionality, `POST /payments`, Kafka, PSP integration, complete OAuth/OIDC business flows, complete dashboards, or payment domain persistence.

**Task Format**: `- [ ] T### [P?] [LABEL] Description`

**Parallel Rule**: `[P]` tasks touch independent files or independent test/data scopes. Baseline work must preserve order-independent tests, no shared mutable test assumptions, future worker-aware Playwright, and safe parallel REST/integration/E2E expansion.

---

## 1. Repository Foundation

**Purpose**: Establish the final monorepo shape and shared project metadata required by FR-001, FR-002, AC-001, and the constitution.

- [ ] T001 [AGENT-IMPLEMENT] Create the planned top-level folders `apps/backend`, `apps/frontend`, `infra/compose`, `infra/keycloak/realms`, `docs/setup`, `docs/testing`, `docs/architecture`, and the categorized single-vault learning structure under `knowledge-vault/01 Project - Payment Quality Engineering Lab`, `knowledge-vault/02 Technical Learning`, `knowledge-vault/03 Business Product and Testing Thinking`, and `knowledge-vault/04 Interview Capital`; verify no payment business folders such as `apps/backend/.../payment` are created.
- [ ] T002 [AGENT-IMPLEMENT] Create root `.gitignore` covering Java/Maven build output, Node/Nuxt output, Playwright reports, local `.env` files, IDE files, OS files, and Docker/runtime temporary files while keeping `.env.example` files trackable.
- [ ] T003 [AGENT-IMPLEMENT] Create root `README.md` with project purpose, Phase 0 scope, top-level directory map, prerequisites, setup overview, baseline verification commands, current Kilo project configuration location `.kilo/`, instruction not to create `.kilocode/` as a new Phase 0 target, and explicit non-goals.
- [ ] T004 [AGENT-IMPLEMENT] Create root `AGENTS.md` with project operating model: agents implement product/infrastructure, user focuses on tester learning, risk analysis, test design, automation, and quality review.
- [ ] T005 [P] [AGENT-IMPLEMENT] Add root documentation pointers to `infra/compose/.env.example` and list app-level non-secret environment categories for `SPRING_PROFILES_ACTIVE`, `APP_POSTGRES_HOST`, `APP_POSTGRES_PORT`, `NUXT_PUBLIC_API_BASE_URL`, and `NUXT_PUBLIC_KEYCLOAK_URL`; verification: `infra/compose/.env.example` is the concrete local service example file and no real secrets are committed.
- [ ] T006 [TESTER-ANALYZE] Review the repository map in `README.md` and record Phase 0 setup risks: unclear ownership, missing commands, confusing non-goals, or folders that imply business features.
- [ ] T007 [AGENT-IMPLEMENT] Before backend, frontend, or infrastructure dependency scaffolding, verify official coordinates, exact usable versions, and compatibility assumptions for Java JDK 25, Maven 3.9.11, Spring Boot 4+, Spring Framework 7+, Spring Modulith 2.0.6, JUnit 6, Nuxt 4, TypeScript 6, Playwright 1.60, PostgreSQL 18, and Keycloak 26.6.1; record selected versions and any justified substitutions in the relevant README/docs before editing dependency files.

---

## 2. Backend Foundation

**Purpose**: Create a Java 25, Maven 3.9.11, Spring Boot 4+/Spring Framework 7+ backend skeleton with one technical status behavior. Covers FR-003, FR-004, AC-002, AC-005, SC-004.

- [ ] T008 [AGENT-IMPLEMENT] Initialize `apps/backend` as a Maven project with Maven Wrapper files `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, and `mvnw.cmd` configured for Maven 3.9.11.
- [ ] T009 [AGENT-IMPLEMENT] Create `apps/backend/pom.xml` with Java release 25, fail-fast compiler configuration, Spring Boot 4+ dependency management, and Spring Framework 7+ alignment through the selected Spring Boot platform.
- [ ] T010 [AGENT-IMPLEMENT] Add backend dependencies in `apps/backend/pom.xml`: Spring Web, Bean Validation, Spring Test, Spring Modulith 2.0.6, JUnit 6, AssertJ, Mockito, REST Assured, Testcontainers, PostgreSQL JDBC, and test-scoped WireMock only if compatible; document any version substitution in `apps/backend/README.md`.
- [ ] T011 [DISCUSS] If Spring Security is included in `apps/backend/pom.xml`, record the Phase 0 decision in `apps/backend/README.md`: `GET /api/status` remains accessible locally and complete OAuth/OIDC is deferred.
- [ ] T012 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/PaymentQualityApplication.java` as the Spring Boot entry point under root package `lab.paymentquality`.
- [ ] T013 [AGENT-IMPLEMENT] Create `apps/backend/src/main/resources/application.yml` with application name `payment-quality-lab`, server port convention, non-secret local profile placeholders, and no required business schema or payment data configuration.
- [ ] T014 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/foundation/status/StatusResponse.java` returning stable fields `application`, `phase`, and `status` without secrets, database details, Keycloak details, or payment concepts.
- [ ] T015 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/foundation/status/StatusController.java` exposing `GET /api/status` with response values equivalent to `payment-quality-lab`, `foundation`, and `UP`.
- [ ] T016 [P] [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/shared/web/package-info.java` documenting the narrow purpose of future cross-cutting web conventions; do not add generic utilities or unused implementation classes.
- [ ] T017 [AGENT-IMPLEMENT] Create `apps/backend/README.md` covering Java 25, Maven Wrapper usage, run command, test command, package conventions, status endpoint behavior, Spring Modulith presence, and Phase 0 non-goals.
- [ ] T018 [AGENT-REVIEW] Verify backend startup scope: `apps/backend` contains no payment, merchant, settlement, reconciliation, PSP, Kafka, OAuth business-flow, or domain entity implementation.

---

## 3. Spring Modulith 2.0.6 Foundation

**Purpose**: Make modular-monolith direction visible without fake business modules. Covers FR-004, FR-005, AC-008, NFR-004.

- [ ] T019 [AGENT-IMPLEMENT] Configure Spring Modulith 2.0.6 dependencies in `apps/backend/pom.xml` so architecture verification can run in default backend tests.
- [ ] T020 [AGENT-IMPLEMENT] Create `apps/backend/src/test/java/lab/paymentquality/architecture/ModulithArchitectureTest.java` using `ApplicationModules.of(PaymentQualityApplication.class).verify()`.
- [ ] T021 [AGENT-IMPLEMENT] Add a source-level package convention note at `apps/backend/src/main/java/lab/paymentquality/package-info.java` stating that future application modules are introduced only with real behavior and that empty payment/merchant module shells are out of Phase 0 scope.
- [ ] T022 [AGENT-IMPLEMENT] Create `docs/architecture/modular-monolith-foundation.md` explaining root package `lab.paymentquality`, current foundation/status stance, future module candidates, public API/internal boundary expectations, event deferral, and why `@ApplicationModuleTest` is deferred.
- [ ] T023 [TESTER-ANALYZE] Read `docs/architecture/modular-monolith-foundation.md` and identify tester-visible risks: hidden coupling, fake module shells, ambiguous public APIs, event overuse, or missing architecture checks.
- [ ] T024 [AGENT-REVIEW] Run or document verification for `ApplicationModules.verify()` as part of `./mvnw test`; acceptance: architecture test passes and no fake business module packages are present.

---

## 4. Frontend Foundation

**Purpose**: Initialize the Nuxt 4 dashboard frontend with Nuxt UI, TypeScript 6, Zod, Pinia, project shell branding, and Playwright 1.60 structure. Covers FR-006, AC-003, SC-004.

- [ ] T025 [AGENT-IMPLEMENT] Initialize `apps/frontend` from the official Nuxt Dashboard Template using the selected documented command/source/version or reference from T007; preserve template layout primitives and Nuxt UI conventions that support a running shell, and record the source plus initialization approach in `apps/frontend/README.md`.
- [ ] T026 [AGENT-IMPLEMENT] Configure `apps/frontend/package.json` for pnpm scripts, Nuxt 4, Nuxt UI, TypeScript 6 baseline, Zod, `@pinia/nuxt`, and Playwright 1.60.
- [ ] T027 [AGENT-IMPLEMENT] Configure `apps/frontend/nuxt.config.ts` to enable Nuxt UI and Pinia via `@pinia/nuxt`, with no payment API client or auth route guards.
- [ ] T028 [AGENT-IMPLEMENT] Create or customize the foundation landing/dashboard route in `apps/frontend/app/pages/index.vue` (Nuxt 4 sources from `app/`) so it displays `Payment Quality Engineering Lab` and states that merchant, admin, payment operations, risk/review, and reconciliation dashboards are future phases.
- [ ] T029 [P] [AGENT-IMPLEMENT] Create `apps/frontend/app/schemas/app-shell.schema.ts` (Nuxt 4 sources from `app/`) with a minimal Zod schema for shell/config validation only; do not create payment domain schemas.
- [ ] T030 [P] [AGENT-IMPLEMENT] Create `apps/frontend/app/stores/app-shell.ts` (Nuxt 4 sources from `app/`) as a minimal Pinia shell/UI state convention only if useful; do not create payment, merchant, risk, or reconciliation state.
- Note: root-level `apps/frontend/pages/`, `apps/frontend/schemas/`, and `apps/frontend/stores/` README marker folders may remain as convention pointers; implemented Nuxt 4 source files live under `apps/frontend/app/`.
- [ ] T031 [P] [AGENT-IMPLEMENT] Remove or relabel Nuxt Dashboard Template demo content that could be mistaken for implemented payment, merchant, risk, reconciliation, admin, or auth behavior.
- [ ] T032 [AGENT-IMPLEMENT] Create `apps/frontend/playwright.config.ts` with Playwright 1.60 baseline, deterministic base URL conventions, parallel-friendly defaults, and no shared mutable user/payment data.
- [ ] T033 [AGENT-IMPLEMENT] Create `apps/frontend/tests/e2e/foundation.spec.ts` as a non-business smoke test that starts/uses the Nuxt dashboard shell, verifies Nuxt UI-backed foundation content renders, and checks visible project identity or placeholder messaging using resilient locators.
- [ ] T034 [AGENT-IMPLEMENT] Create `apps/frontend/README.md` covering pnpm install/dev, typecheck, Playwright foundation command, template-vs-project content, deferred auth, deferred dashboards, and parallel-ready E2E conventions.
- [ ] T035 [TESTER-ANALYZE] Review the frontend shell for accidental business behavior or misleading demo data; acceptance: no payment creation, payment list, real login, role-based dashboard, or Keycloak-backed route guard exists.

---

## 5. Infrastructure Foundation

**Purpose**: Provide local PostgreSQL 18 and Keycloak 26.6.1 readiness without business integration. Covers FR-007, AC-004, NFR-008.

- [ ] T036 [AGENT-IMPLEMENT] Create `infra/compose/.env.example` as the concrete local service environment example with non-secret values for `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_PORT`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, and `KEYCLOAK_PORT`; document that production secrets, business realm variables, and application auth variables are intentionally deferred.
- [ ] T037 [AGENT-IMPLEMENT] Create `infra/compose/compose.yml` defining PostgreSQL 18 service `payment-quality-postgres` with local-development credentials sourced from `.env`.
- [ ] T038 [AGENT-IMPLEMENT] Extend `infra/compose/compose.yml` with Keycloak 26.6.1 service `payment-quality-keycloak`, explicit port mapping, startup command suitable for local dev, and documented database choice.
- [ ] T039 [AGENT-IMPLEMENT] Create `infra/keycloak/README.md` explaining Phase 0 Keycloak 26.6.1 local startup/configuration conventions, realm import location `infra/keycloak/realms`, and that complete OAuth/OIDC business flows, business realms, roles, and application integration are deferred; if no realm import exists, add a placeholder note explaining why.
- [ ] T040 [AGENT-IMPLEMENT] Create `docs/setup/local-infra.md` with Docker Compose startup, readiness checks, service URLs, ports, shutdown, troubleshooting for port conflicts, and non-production credential warning.
- [ ] T041 [TESTER-ANALYZE] Review `docs/setup/local-infra.md` for local setup risks: port conflicts, unclear `.env` copy step, Keycloak readiness timing, PostgreSQL data persistence, and confusion between identity infrastructure and application auth.
- [ ] T042 [AGENT-REVIEW] Verify infrastructure has no Kafka service, PSP mock service, payment database schema, migration, or production secret material.

---

## 6. Testing Foundation

**Purpose**: Establish baseline verification and future test locations for unit, module, integration, REST API, Testcontainers, WireMock, and Playwright while preserving parallel-readiness. Covers FR-008, FR-009, FR-010, AC-005, AC-006, AC-007.

- [ ] T043 [AGENT-IMPLEMENT] Configure Maven Surefire in `apps/backend/pom.xml` so default `./mvnw test` runs unit, slice, context smoke, REST/status if included, and Modulith architecture tests deterministically.
- [ ] T044 [AGENT-IMPLEMENT] Create `apps/backend/src/test/java/lab/paymentquality/PaymentQualityApplicationTests.java` as a Spring context smoke test with no database, Keycloak, payment, or shared mutable data dependency.
- [ ] T045 [AGENT-IMPLEMENT] Create `apps/backend/src/test/java/lab/paymentquality/foundation/status/StatusControllerTest.java` verifying `GET /api/status` or controller behavior returns stable foundation values and exposes no secrets/payment concepts.
- [ ] T046 [AGENT-IMPLEMENT] Include REST Assured in the backend testing foundation by creating `apps/backend/src/test/java/lab/paymentquality/rest/StatusRestAssuredTest.java` for a minimal technical status smoke/contract check if reliable in Phase 0, or create `apps/backend/src/test/java/lab/paymentquality/rest/README.md` documenting REST Assured conventions and why a full-server test is deferred; acceptance: no business payment API testing is added.
- [ ] T047 [P] [AGENT-IMPLEMENT] Create `apps/backend/src/test/java/lab/paymentquality/integration/README.md` documenting future Spring integration and Testcontainers conventions: unique data namespaces, transactions/rollback, worker-safe containers, and no hidden singleton mutable state.
- [ ] T048 [P] [AGENT-IMPLEMENT] Create `apps/backend/src/test/java/lab/paymentquality/wiremock/README.md` or equivalent documented test area for future external service stubs; acceptance: no PSP mock behavior is implemented.
- [ ] T049 [AGENT-IMPLEMENT] Configure or document backend integration test naming conventions such as `*IT` and when `./mvnw verify` will run Failsafe/Testcontainers checks.
- [ ] T050 [AGENT-IMPLEMENT] Configure frontend dev/build/typecheck/lint/test scripts in `apps/frontend/package.json` using template-appropriate commands; verification commands must prove the Nuxt dashboard shell builds or starts successfully, Nuxt UI remains present, and repeated runs are deterministic.
- [ ] T051 [AGENT-IMPLEMENT] Ensure `apps/frontend/tests/e2e` uses Playwright conventions that support future worker-aware accounts/data and avoids a single shared mutable user, payment record, or auth state.
- [ ] T052 [AGENT-IMPLEMENT] Create `docs/testing/phase-0-quality-baseline.md` with the technical baseline: current backend/frontend/infra verification commands, test layer map, future unit/module/integration/REST/E2E locations, and Testcontainers/WireMock conventions.
- [ ] T053 [TESTER-DESIGN] Add a clearly marked tester-owned section in `docs/testing/phase-0-quality-baseline.md` defining order-independent test strategy, data-isolation readiness, no shared mutable assumptions, future worker-aware Playwright fixtures, and safe parallel REST/integration/E2E testing.
- [ ] T054 [TESTER-AUTOMATE] Verify baseline backend checks from `apps/backend` with `./mvnw test`; record failures as implementation defects, not tester workarounds.
- [ ] T055 [TESTER-AUTOMATE] Verify baseline frontend checks from `apps/frontend` with `pnpm install`, `pnpm typecheck`, and `corepack pnpm exec playwright test` once dependencies are installed.
- [ ] T056 [AGENT-REVIEW] Confirm all baseline tests are repeatable, order-independent, and do not require payment workflows, Kafka, PSP services, complete OAuth/OIDC, or shared business data.

---

## 7. Documentation and Tester Orientation

**Purpose**: Make the skeleton understandable and usable from a tester perspective. Covers FR-002, FR-011, AC-009, SC-003.

- [ ] T057 [AGENT-IMPLEMENT] Update root `README.md` with exact backend, frontend, infrastructure, and baseline verification commands selected during implementation.
- [ ] T058 [AGENT-IMPLEMENT] Ensure `apps/backend/README.md`, `apps/frontend/README.md`, and `docs/setup/local-infra.md` all cross-link to root `README.md` and to Phase 0 non-goals.
- [ ] T059 [AGENT-EXPLAIN] Create `docs/setup/phase-0-tester-orientation-pack.md` after implementation, explaining what exists, what is absent, how to run backend/frontend/infra, how to run baseline verification, and how to interpret `GET /api/status` and the frontend placeholder.
- [ ] T060 [AGENT-EXPLAIN] Add to `docs/setup/phase-0-tester-orientation-pack.md` a plain-language explanation of Spring Modulith architecture verification and why testers should care about module boundaries before payment features exist.
- [ ] T061 [TESTER-ANALYZE] Add Phase 0 exploratory testing charters to `docs/setup/phase-0-tester-orientation-pack.md`: setup reproducibility, documentation accuracy, port conflicts, failure messages, no accidental payment behavior, and test isolation review.
- [ ] T062 [TESTER-DESIGN] Add a tester checklist to `docs/setup/phase-0-tester-orientation-pack.md` mapping FR-001 through FR-013 to observable checks or review questions.
- [ ] T063 [DISCUSS] Review the version validation record from T007 and any implementation-time substitutions or compatibility compromises for Java JDK 25, Maven 3.9.11, Spring Boot 4+, Spring Framework 7+, Spring Modulith 2.0.6, JUnit 6, TypeScript 6, Nuxt 4, Playwright 1.60, PostgreSQL 18, or Keycloak 26.6.1; document the final decision in the relevant README or docs file.
- [ ] T064 [AGENT-REVIEW] Verify documentation accuracy by following documented commands from a clean checkout perspective; acceptance: docs do not reference nonexistent scripts or business features.

---

## 8. Obsidian / Learning Outputs

**Purpose**: Capture Phase 0 as the first learning milestone and connect architecture/testing lessons to the knowledge vault. Covers FR-012, AC-010, SC-007.

- [ ] T065 [AGENT-IMPLEMENT] Create `knowledge-vault/01 Project - Payment Quality Engineering Lab/00 Phase 0 - Foundation/Phase 0 - Project Foundation and Running Skeleton.md` as the Phase 0 hub with links to root README, backend README, frontend README, local infrastructure docs, modular-monolith docs, quality baseline docs, moved technical learning notes, infrastructure note, and interview story.
- [ ] T066 [AGENT-EXPLAIN] Create `knowledge-vault/02 Technical Learning/Spring Modulith/Architecture - Modular Monolith with Spring Modulith.md` summarizing current module stance, future module candidates, dependency/event rules, and why fake business modules are avoided.
- [ ] T067 [AGENT-EXPLAIN] Create `knowledge-vault/02 Technical Learning/Spring Modulith/Architecture Test - ApplicationModules.verify.md` explaining what the architecture test verifies now, what it does not verify yet, and how future `@ApplicationModuleTest` work will fit.
- [ ] T068 [AGENT-EXPLAIN] Create `knowledge-vault/02 Technical Learning/Testing Architecture/Testing - Phase 0 Quality Baseline.md` summarizing backend, frontend, infrastructure, REST, Testcontainers, WireMock, and Playwright testing foundations.
- [ ] T069 [TESTER-DESIGN] Create `knowledge-vault/02 Technical Learning/Testing Architecture/Testing - Parallel Readiness Principles.md` with worker-safe data naming, isolated fixtures, transaction/container options, REST parallel concerns, and Playwright worker-aware strategy.
- [ ] T070 [AGENT-EXPLAIN] Create `knowledge-vault/02 Technical Learning/Infrastructure/Infrastructure - Local PostgreSQL 18 and Keycloak 26.6.1.md` explaining local service purpose, ports, startup, deferred auth, and non-production assumptions.
- [ ] T071 [TESTER-ANALYZE] Create `knowledge-vault/04 Interview Capital/Interview Story - Why Foundation Before Payment Features.md` as a concise learning note connecting Phase 0 scope control to testability, modularity, and future quality engineering credibility.
- [ ] T072 [AGENT-REVIEW] Verify Obsidian notes use repository-relative links where practical, avoid claiming implemented payment behavior, align with actual commands and files, and preserve separation between project/feature knowledge, technical learning, business/product/testing thinking, and interview capital.

---

## Final Verification and Phase 0 Closeout

- [ ] T073 [AGENT-REVIEW] Run backend verification from `apps/backend` with `./mvnw test`; acceptance: Java 25/Maven Wrapper tests pass, including Spring context, status behavior through controller/slice or REST Assured coverage, and Modulith architecture verification.
- [ ] T074 [AGENT-REVIEW] Run frontend verification from `apps/frontend` with documented pnpm commands; acceptance: install, typecheck/build or template-equivalent shell verification, Nuxt UI/dashboard foundation rendering, and Playwright foundation smoke test pass or any environment-only blocker is documented.
- [ ] T075 [AGENT-REVIEW] Start local infrastructure with the documented Docker Compose command from `docs/setup/local-infra.md`; acceptance: PostgreSQL 18 and Keycloak 26.6.1 services start or report actionable local configuration errors.
- [ ] T076 [AGENT-REVIEW] Perform non-goal audit across repository paths; acceptance: no `POST /payments`, payment persistence, PSP integration, Kafka service, complete OAuth/OIDC business flow, or complete dashboard feature exists.
- [ ] T077 [AGENT-REVIEW] Perform parallel-readiness audit; acceptance: tests and docs preserve order-independent strategy, data isolation readiness, no shared mutable test assumptions, future worker-aware Playwright, and safe future REST/integration/E2E parallelization.
- [ ] T078 [TESTER-ANALYZE] Execute the Tester Orientation Pack walkthrough and record remaining risks or confusing areas as follow-up issues or notes, not as hidden implementation assumptions.
- [ ] T079 [DISCUSS] Hold Phase 0 readiness review against AC-001 through AC-011 and SC-001 through SC-007; decision options: accept foundation, accept with documented follow-ups, or block business feature work until critical foundation gaps are fixed.
- [ ] T080 [AGENT-REVIEW] Verify repository foundation against `plan.md` section 3 and constitution principles I, II, and IV; acceptance: all top-level areas are discoverable, `.kilo/` is the documented current Kilo configuration location, `.kilocode/` is not introduced as a new Phase 0 target, and no non-goal feature area is implemented.

---

## Dependencies and Execution Order

- Repository foundation tasks T001-T006 establish the folder map and shared docs; T007 is the early version-validation gate and must complete before backend, frontend, or infrastructure dependency files are edited.
- Backend foundation tasks T008-T018 must complete after T007 and before Modulith tasks T019-T024 and backend test tasks T043-T049 can be fully verified.
- Frontend tasks T025-T035 can run after T001 and may proceed in parallel with backend and infrastructure tasks if dependency installation is independent.
- Infrastructure tasks T036-T042 can run after T001 and should complete before final local environment verification T075.
- Testing foundation tasks T043-T056 depend on the relevant backend/frontend skeletons but documentation-only convention tasks T047, T048, T052, and T053 can proceed once paths exist.
- Documentation tasks T057-T064 should be finalized after implementation commands and versions are known.
- Obsidian tasks T065-T072 should be finalized after the documentation files they link to exist.
- Final verification tasks T073-T080 run last and gate Phase 0 closeout.

## Parallel Opportunities

- T005, T016, T029, T030, T031, T047, and T048 are explicitly parallel-safe when their parent folders exist.
- Backend implementation T012-T015 should be sequential within the backend status slice to avoid file conflicts.
- Frontend setup T025-T028 should be sequential until the template and Nuxt config are stable; shell schemas/stores/demo cleanup may then run in parallel.
- Infrastructure compose work T036-T038 should be sequential because all edit `infra/compose` behavior.
- Tester analysis/design tasks can proceed after their referenced implementation or documentation artifact exists.
