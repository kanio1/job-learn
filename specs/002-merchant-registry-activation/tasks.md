# Tasks: Phase 1 — Merchant Registry and Activation for Platform Operators

**Input**: Design documents from `specs/002-merchant-registry-activation/`

**Prerequisites**: spec.md, plan.md, research.md, data-model.md, contracts/merchant-api.md, quickstart.md

**Tests**: Included for all user-visible behavior, API contracts, security, persistence, and concurrency.

**Organization**: 18 task groups ordered by dependency; tasks mapped to user stories (US1-US5) where they implement specific acceptance scenarios.

---

## 1. Phase 1 Setup and Artifact Alignment

**Purpose**: Confirm all design artifacts are consistent and the backend/frontend toolchains are ready for Phase 1 additions.

- [X] T001 [AGENT-REVIEW] Verify `specs/002-merchant-registry-activation/` contains spec.md, plan.md, research.md, data-model.md, contracts/merchant-api.md, and quickstart.md with no contradictory statements across artifacts.
- [X] T002 [AGENT-REVIEW] Run `corepack pnpm install` in `apps/frontend` and `./mvnw test` in `apps/backend` to confirm Phase 0 baselines still pass before Phase 1 additions begin.
- [X] T003 [AGENT-IMPLEMENT] Add Phase 1 scoped dependencies to `apps/backend/pom.xml`: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-data-jpa`, `flyway-core`, `flyway-database-postgresql`, `org.testcontainers:testcontainers-postgresql` (test, official Testcontainers 2.0.5 PostgreSQL module artifact), `org.testcontainers:testcontainers-junit-jupiter` (test, official Testcontainers 2.0.5 JUnit Jupiter artifact), `spring-boot-testcontainers` (test). Do not remove existing Phase 0 dependencies.
- [X] T004 [AGENT-IMPLEMENT] Add `nuxt-auth-utils` dependency to `apps/frontend/package.json`.
- [X] T005 [AGENT-IMPLEMENT] Update `apps/backend/src/main/resources/application.yml` with datasource, JPA (`ddl-auto: validate`), Flyway (`enabled: true`, `locations: classpath:db/migration/merchant`), and OAuth2 resource-server configuration per `plan.md` sections 6 and 8. Create the canonical test configuration file at `apps/backend/src/test/resources/application-test.yml` for Testcontainers profile with Flyway enabled, log levels, and a local test JWT issuer/JWK configuration used by HTTP security tests.
- [X] T006 [AGENT-IMPLEMENT] Wire Spring Security tests to use only `apps/backend/src/test/resources/application-test.yml` so unit, slice, REST Assured, and security tests do not require real Keycloak connectivity. Use locally generated JWTs signed by the test issuer/JWK from that file for HTTP tests; do not use MockMvc JWT helpers in REST Assured tests.

---

## 2. Database Migration and Persistence Foundation

**Purpose**: Establish the PostgreSQL 18 schema and Flyway migration infrastructure.

- [X] T007 [AGENT-IMPLEMENT] Create Flyway migration directory `apps/backend/src/main/resources/db/migration/merchant/`.
- [X] T008 [AGENT-IMPLEMENT] Write `apps/backend/src/main/resources/db/migration/merchant/V1__create_merchants.sql` with the exact DDL from `data-model.md`: `merchants` table with `merchant_id UUID PK`, `normalized_reference VARCHAR(64) UNIQUE NOT NULL`, `display_name VARCHAR(120) NOT NULL`, `status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` with `CHECK` constraint, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `version BIGINT NOT NULL DEFAULT 0`, index on `status`, composite index on `(created_at DESC, merchant_id ASC)` for stable list ordering.
- [X] T009 [DISCUSS] Confirm via `./mvnw spring-boot:run` that Flyway executes the migration on startup against the local PostgreSQL 18 container. Verify table exists with `docker compose exec` or equivalent.

---

## 3. Merchant Domain Model

**Purpose**: Implement the core domain entity, value objects, and lifecycle rules.

- [X] T010 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/MerchantStatus.java` as a Java enum with `DRAFT`, `ACTIVE`, `SUSPENDED` and the `canTransitionTo(MerchantStatus)` method per `data-model.md`. Valid transitions: `DRAFT → ACTIVE`, `ACTIVE → SUSPENDED`. All other transitions return false.
- [X] T011 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/MerchantReference.java` as a Java record with a static factory `from(String raw)`. Reject null before trimming, trims and uppercases input, validates against regex `^[A-Z0-9][A-Z0-9-]{1,62}[A-Z0-9]$` (3-64 chars, no leading/trailing hyphens), throws `InvalidMerchantReferenceException` on failure.
- [X] T012 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/Merchant.java` as a JPA entity per `data-model.md`. Also create `DisplayName.java` as a value object with `from(String raw)` that rejects null, trims input, and validates post-trim length 2-120 before persistence. `Merchant.create(UUID, String normalizedReference, String displayName)` initializes a new merchant with the already-normalized reference, the validated trimmed display name, `DRAFT` status, and timestamps. Domain methods `activate()` and `suspend()` check transition validity via `MerchantStatus.canTransitionTo()` and update status + `updatedAt`.
- [X] T013 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/InvalidMerchantReferenceException.java`, `InvalidDisplayNameException.java`, and `InvalidTransitionException.java` as unchecked domain exceptions.
- [X] T014 [P] [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/MerchantNotFoundException.java` as an unchecked exception thrown when a merchant ID is not found.

---

## 4. Merchant Application / Use-Case Layer

**Purpose**: Implement the application service and repository interface.

- [X] T015 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/infrastructure/JpaMerchantRepository.java` as a Spring Data JPA interface extending `JpaRepository<Merchant, UUID>`. Declare `Optional<Merchant> findByNormalizedReference(String ref)` for duplicate checks and `List<Merchant> findAllByOrderByCreatedAtDescMerchantIdAsc(Pageable pageable)` for the list operation.
- [X] T016 [FR-008] [FR-009] [NFR-005] [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantService.java` as a `@Service` class. Methods: `create(String ref, String displayName)` (normalizes via `MerchantReference.from()`, validates and trims via `DisplayName.from()`, checks duplicates via `findByNormalizedReference`, throws `DuplicateMerchantReferenceException` if found, creates `Merchant` entity, saves, and translates database unique-constraint `DataIntegrityViolationException` for `normalized_reference` into `DuplicateMerchantReferenceException`), `findById(UUID id)`, `listFirstPage()` using a fixed first-page limit/sort, `activate(UUID id)`, `suspend(UUID id)`. Emit structured application logs using event names `merchant.create.succeeded`, `merchant.status.activate.succeeded`, and `merchant.status.suspend.succeeded`. Required safe fields: action name, merchant ID when known, normalized reference when useful, outcome, status or transition, and `correlationId` from MDC. Forbidden fields: passwords, access tokens, refresh tokens, raw authorization headers, and full raw request bodies.
- [X] T017 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/CreateMerchantRequest.java` as a Java record with `@NotBlank @Size(max=64) String merchantReference` and `@NotBlank @Size(min=2, max=120) String displayName`.
- [X] T018 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantResponse.java` as a Java record with fields `merchantId` (UUID), `merchantReference` (String), `displayName` (String), `status` (String), `createdAt` (Instant), `updatedAt` (Instant). Also create `MerchantListResponse.java` as a record wrapping `List<MerchantResponse> merchants` for `GET /api/merchants`.
- [X] T019 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantMapper.java` as a utility class with static mapping methods from domain entities to `MerchantResponse` and `MerchantListResponse`. Do not put merchant creation logic in the mapper; creation stays in `MerchantService` and the domain factory.

---

## 5. Merchant REST API and Error Handling

**Purpose**: Implement the five merchant endpoints and a merchant-scoped error handler per `contracts/merchant-api.md`. Group 7 security configuration may run in parallel, but T033 must be complete before T020-T024 are finalized because the controller authority annotations and HTTP matchers are verified together.

- [X] T020 [US1] [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantController.java` as a `@RestController` with `@RequestMapping("/api/merchants")`. Implement `POST /api/merchants` — validate request body with `@Valid`, call `MerchantService.create()`, return `201` with `MerchantResponse`. Protected by `@PreAuthorize("hasAuthority('platform:merchants:create')")`.
- [X] T021 [US4] [AGENT-IMPLEMENT] Add `GET /api/merchants/{id}` to `MerchantController.java` — parse UUID, call `MerchantService.findById()`, return `200` or throw `MerchantNotFoundException` → 404. Protected by `@PreAuthorize("hasAuthority('platform:merchants:read')")`.
- [X] T022 [US4] [AGENT-IMPLEMENT] Add `GET /api/merchants` to `MerchantController.java` — do not expose pagination query parameters in Phase 1; call `MerchantService.listFirstPage()`, which uses a fixed first-page limit of 50 sorted by `createdAt DESC, merchantId ASC`, and return `200` with `MerchantListResponse`. Protected by `@PreAuthorize("hasAuthority('platform:merchants:read')")`.
- [X] T023 [US2] [AGENT-IMPLEMENT] Add `POST /api/merchants/{id}/activate` to `MerchantController.java` — parse UUID, call `MerchantService.activate()`, return `200` with updated `MerchantResponse`. Protected by `@PreAuthorize("hasAuthority('platform:merchants:update-status')")`.
- [X] T024 [US3] [AGENT-IMPLEMENT] Add `POST /api/merchants/{id}/suspend` to `MerchantController.java` — parse UUID, call `MerchantService.suspend()`, return `200` with updated `MerchantResponse`. Protected by `@PreAuthorize("hasAuthority('platform:merchants:update-status')")`.
- [X] T025 [FR-009] [NFR-009] [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/shared/web/CorrelationIdFilter.java` to read `X-Correlation-ID` when present or generate a UUID when absent, place `correlationId` in MDC for the request, and return it as `X-Correlation-ID` response header. Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/MerchantExceptionHandler.java` as a merchant-scoped `@RestControllerAdvice(assignableTypes = MerchantController.class)` so `shared` does not depend on merchant internals. Also create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/ErrorResponse.java` as the standard merchant error body record, with `error`, `message`, and optional `details` for validation errors. Map `InvalidMerchantReferenceException` and `InvalidDisplayNameException` → 400 `{ "error": "validation", "message": "...", "details": {...} }`, `MethodArgumentNotValidException` → 400 `{ "error": "validation", "message": "...", "details": {...} }`, `MethodArgumentTypeMismatchException` and malformed UUID `IllegalArgumentException` → 400 `{ "error": "validation", "message": "..." }`, `DuplicateMerchantReferenceException` → 409 `{ "error": "duplicate_merchant_reference", "message": "..." }`, `MerchantNotFoundException` → 404 `{ "error": "not_found", "message": "..." }`, `InvalidTransitionException` → 409 `{ "error": "invalid_transition", "message": "..." }`. Ensure failure handling supports safe traceability using event names such as `merchant.create.failed.validation`, `merchant.create.failed.duplicate`, `merchant.lookup.failed.not-found`, and `merchant.status.failed.invalid-transition`, following the same safe/forbidden log-field contract as T016.
- [X] T026 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/web/DuplicateMerchantReferenceException.java` as an unchecked exception carrying the conflicting reference.
- [X] T027 [AGENT-REVIEW] Verify that `GET /api/status` remains unchanged and returns the same Phase 0 response `{ "application": "payment-quality-lab", "phase": "foundation", "status": "UP" }` without requiring authentication. Confirm the `foundation` package is not modified.

---

## 6. Spring Modulith Boundaries and Module Tests

**Purpose**: Introduce the `merchant` module into the modular monolith with public/internal boundary enforcement.

- [X] T028 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/package-info.java` with `@ApplicationModule(displayName = "Merchant Registry")`.
- [X] T029 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/merchant/internal/package-info.java` documenting that this package is the module's internal implementation, not accessible to other modules.
- [X] T030 [AGENT-IMPLEMENT] Create or update `apps/backend/src/test/java/lab/paymentquality/ModulithArchitectureTest.java` so normal `./mvnw test` execution runs `ApplicationModules.of(PaymentQualityApplication.class).verify()`. Confirm it passes with the new `merchant` module present and no cyclic dependencies between `foundation`, `shared`, and `merchant`.
- [X] T031 [AGENT-IMPLEMENT] Create `apps/backend/src/test/java/lab/paymentquality/merchant/MerchantModuleTest.java` using `@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.STANDALONE)`, `@ActiveProfiles("test")`, and a static PostgreSQL Testcontainer with `@ServiceConnection` when repository/DataSource beans are required. Verify the module context boots, repository/service/controller beans are present, and architecture verification runs within the scoped module. Do not require full secured HTTP endpoint behavior in this module-scope test; REST/security tests cover that separately. If `@ApplicationModuleTest` proves incompatible with the Spring Boot 4/Testcontainers setup, pause and return to `/speckit.plan` rather than replacing it ad hoc.

---

## 7. Security and Keycloak Backend Configuration

**Purpose**: Wire Spring Security as a JWT Resource Server with Keycloak authority mapping.

- [X] T032 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java` implementing `Converter<Jwt, Collection<GrantedAuthority>>`. Extract `realm_access.roles` from JWT claims, map each role to `new SimpleGrantedAuthority("platform:" + role)`. Handle missing/malformed claims gracefully.
- [X] T033 [AGENT-IMPLEMENT] Create `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java` as a `@Configuration`, `@EnableWebSecurity`, and `@EnableMethodSecurity` class. Define `SecurityFilterChain` bean: permit `/api/status`, require `platform:merchants:create` for `POST /api/merchants`, require `platform:merchants:read` for both exact `GET /api/merchants` and `GET /api/merchants/**`, require `platform:merchants:update-status` for `POST /api/merchants/*/activate` and `POST /api/merchants/*/suspend`, require authentication for all other requests. Wire OAuth2 resource server with `JwtAuthenticationConverter` using `KeycloakRealmRoleConverter`. Disable CSRF, stateless session. Keep controller `@PreAuthorize` annotations as defense-in-depth and verify the authority boundaries in security tests. Security failure logs, if added, must distinguish denial class for support without logging tokens or raw authorization headers.
- [X] T034 [AGENT-IMPLEMENT] Add `spring.security.oauth2.resourceserver.jwt.issuer-uri` and `jwk-set-uri` to `apps/backend/src/main/resources/application.yml` pointing to `http://localhost:8081/realms/payment-quality`.

---

## 8. Keycloak Realm / Local Identity Configuration

**Purpose**: Create the local deterministic Keycloak realm with platform-operator roles and test identities.

- [X] T035 [AGENT-IMPLEMENT] Create `infra/keycloak/realms/payment-quality-realm.json` defining: realm `payment-quality`, public client `payment-quality-dashboard` (PKCE enabled, redirect URIs `http://localhost:3000/*`), realm roles `merchants:create`, `merchants:read`, `merchants:update-status`. Create user `platform.operator` (password `platform.operator`, non-temporary, assigned all three realm roles, deterministic local profile fields). Create user `merchant.denied` (password `merchant.denied`, non-temporary, no realm roles, deterministic local profile fields).
- [X] T036 [AGENT-IMPLEMENT] Update `infra/compose/compose.yml`: add volume mount `../keycloak/realms/payment-quality-realm.json:/opt/keycloak/data/import/realm.json` and change command to `start-dev --import-realm`.
- [X] T037 [DISCUSS] Restart Keycloak container with updated Compose config. Verify realm `payment-quality` exists, client `payment-quality-dashboard` exists, users `platform.operator` and `merchant.denied` have the expected role assignments via Keycloak Admin Console (`http://localhost:8081`).

---

## 9. Frontend Auth Foundation for Phase 1

**Purpose**: Add PKCE login flow, auth store, and global route guarding to the Nuxt dashboard.

- [X] T038 [AGENT-IMPLEMENT] Configure `nuxt-auth-utils` with Keycloak provider in `apps/frontend/nuxt.config.ts`. Point to `http://localhost:8081/realms/payment-quality` and client `payment-quality-dashboard`.
- [X] T038a [AGENT-REVIEW] Perform an early frontend auth compatibility spike before building the full admin UI. With Keycloak running, verify `nuxt-auth-utils` can complete login, create a server-side session, expose the authenticated user to the Nuxt app, and make the session access token available only to server-side code for T042a. If the library behavior differs enough to require a fallback library or different auth architecture, pause implementation and return to `/speckit.plan`; do not improvise an unplanned auth design. Record a short spike result in `apps/frontend/README.md`; T073 later integrates that note into the full Phase 1 frontend documentation.
- [X] T039 [FR-034] [FR-035] [AGENT-IMPLEMENT] Create `apps/frontend/app/stores/auth.ts` as a Pinia store with sanitized browser-safe state only, such as `{ isAuthenticated, user }`, getters for login status, and actions `login()`, `logout()`. Do not store access tokens, refresh tokens, raw session objects, or authorization headers in Pinia or any browser-readable state.
- [X] T040 [AGENT-IMPLEMENT] Create `apps/frontend/app/middleware/auth.global.ts` as a Nuxt route middleware. Check `auth.isAuthenticated`. Allow unauthenticated access to `/login` only. Redirect unauthenticated requests to Keycloak login. Redirect authenticated requests away from `/login` to `/admin/merchants`.
- [X] T041 [AGENT-IMPLEMENT] Create `apps/frontend/app/pages/login.vue` as a minimal page that triggers the Keycloak PKCE redirect via `nuxt-auth-utils`. No form — the library handles the redirect.
- [X] T042 [AGENT-IMPLEMENT] Update `apps/frontend/app/app.vue` to wrap pages with `<NuxtLayout>`. Create `apps/frontend/app/layouts/dashboard.vue` with a sidebar navigation containing a "Merchants" link to `/admin/merchants`, a topbar with user display name, and a logout button. The `<slot />` renders the page content.
- [X] T042a [FR-035] [AGENT-IMPLEMENT] Create the authenticated Nuxt-to-backend merchant API path with exact files: `apps/frontend/server/utils/backendApi.ts` reads the `nuxt-auth-utils` session access token server-side and attaches it as `Authorization: Bearer ...` to backend calls; `apps/frontend/server/api/merchants/index.get.ts` proxies `GET /api/merchants`; `apps/frontend/server/api/merchants/index.post.ts` proxies `POST /api/merchants`; `apps/frontend/server/api/merchants/[id].get.ts` proxies `GET /api/merchants/{id}`; `apps/frontend/server/api/merchants/[id]/activate.post.ts` proxies activation; `apps/frontend/server/api/merchants/[id]/suspend.post.ts` proxies suspension. Browser components call these Nuxt server API routes only; access tokens are not exposed to browser JavaScript.

---

## 10. `/admin/merchants` UI Implementation

**Purpose**: Build the authenticated merchant registry page with create/list/activate/suspend functionality.

- [X] T043 [AGENT-IMPLEMENT] Create `apps/frontend/app/schemas/merchant.schema.ts` with a Zod schema for the create merchant form: `merchantReference` (trimmed, min 3, max 64, regex for letters/numbers/hyphens, no leading/trailing hyphen), `displayName` (trimmed, min 2, max 120). Export the inferred TypeScript type.
- [X] T044a [AGENT-IMPLEMENT] Create `apps/frontend/app/pages/admin/merchants.vue` with the merchant registry skeleton structure. Start inline if needed; T045-T047 extract reusable components after the end-to-end page behavior exists. Fetch data through the Nuxt server route `apps/frontend/server/api/merchants/index.get.ts`, not directly from the browser to the backend. If the server route returns 403, render a deterministic insufficient-authority UI state and do not render merchant data or create/status controls. Use stable accessible names/labels so Playwright can prefer role and label locators. Include:
  - Page title "Merchants" (FR-025).
  - Fetch merchant list from `GET /api/merchants` on mount.
  - Loading state: skeleton table while initial fetch is in flight (FR-029 loading state) with accessible status text.
  - Error state: message with retry button labelled for the failed merchant-list load if the API call fails (FR-029 error state).
  - Empty state when list is empty (FR-026): message "No merchants have been registered yet" and a prompt to create one.
  - Merchant table using Nuxt UI `UTable` (FR-028): columns for merchantReference, displayName, status (with color badges: gray=DRAFT, green=ACTIVE, red=SUSPENDED), createdAt. Wire table to the fetched list.
- [X] T044b [NFR-001] [AGENT-IMPLEMENT] Add create merchant flow to `merchants.vue`. Keep the flow single-page and low-friction for SC-001: from `/admin/merchants`, an authenticated operator should be able to open the form, enter reference/display name, submit, and see the new merchant without navigating to another feature area or completing nonessential steps. Create merchant action/button with accessible name "Create merchant" opening a Nuxt UI modal/slideover (FR-027). Build form with labelled fields for merchantReference and displayName, Zod validation on submit per `merchant.schema.ts`, per-field error display associated with the relevant field (FR-029 validation feedback). Submit through `apps/frontend/server/api/merchants/index.post.ts`. On duplicate reference (409), show inline conflict feedback (FR-029 duplicate-conflict feedback). On success (201), close modal, show toast, and refresh the merchant list (FR-029 success feedback).
- [X] T044c [AGENT-IMPLEMENT] Add lifecycle actions to `merchants.vue`. Activate button per row (visible when status is DRAFT) with accessible name including the merchant reference — calls the Nuxt server route backed by `apps/frontend/server/api/merchants/[id]/activate.post.ts`, on success refreshes list. Suspend button per row (visible when status is ACTIVE) with accessible name including the merchant reference — calls the Nuxt server route backed by `apps/frontend/server/api/merchants/[id]/suspend.post.ts`, on success refreshes list. Status badges must expose readable status text. Handle 409 invalid-transition errors with visible error feedback.
- [X] T045 [P] [AGENT-IMPLEMENT] After T044a-T044c, extract the merchant table into `apps/frontend/app/components/merchant/MerchantTable.vue` as a reusable component.
- [X] T046 [P] [AGENT-IMPLEMENT] After T044a-T044c, extract the create form into `apps/frontend/app/components/merchant/CreateMerchantForm.vue` as a reusable component with Zod validation.
- [X] T047 [P] [AGENT-IMPLEMENT] After T044a-T044c, extract DRAFT/ACTIVE/SUSPENDED status pills into `apps/frontend/app/components/merchant/MerchantStatusBadge.vue` using Nuxt UI `UBadge`.

---

## 11. Backend Automated Tests (Unit, Service, Repository)

**Purpose**: Cover domain logic, application orchestration, and persistence with fast deterministic tests.

- [X] T048 [P] [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/internal/domain/MerchantReferenceTest.java` and `DisplayNameTest.java`. For merchant reference, apply Equivalence Partitioning: valid references (`"MERCH-001"`, `"A01"`, `"Z".repeat(64)`, lowercase input normalized, digits-only acceptable `"123"`), invalid references (null, blank, whitespace-only, 2 chars such as `"A1"`, 65 chars, leading hyphen `"-ABC"`, trailing hyphen `"ABC-"`, special chars `"ABC_DEF"`). Apply Boundary Value Analysis: 2/3/63/64/65 char lengths. For display name, verify trimming before validation and persistence: valid 2/120 post-trim chars, invalid null, blank, whitespace-only, post-trim 1 char such as `" A "`, and post-trim 121 chars.
- [X] T049 [P] [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/internal/domain/MerchantStatusTest.java`. Apply State Transition Testing: all 3×3 combinations — DRAFT→ACTIVE (valid), DRAFT→SUSPENDED (invalid), ACTIVE→SUSPENDED (valid), ACTIVE→ACTIVE (invalid), SUSPENDED→* (invalid), etc. Apply Decision Table: each combination with expected boolean result.
- [X] T050 [P] [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/internal/domain/MerchantTest.java`. Verify newly created merchants initialize both `createdAt` and `updatedAt` consistently before any lifecycle transition. Verify `activate()` transitions status and updates `updatedAt`. Verify `suspend()` transitions status and updates `updatedAt`. Verify `activate()` throws `InvalidTransitionException` from ACTIVE or SUSPENDED. Verify `suspend()` throws `InvalidTransitionException` from DRAFT or SUSPENDED.
- [X] T051 [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/internal/application/MerchantServiceTest.java`. Mock `JpaMerchantRepository`. Test `create()`: valid input → merchant saved with DRAFT and trimmed display name, post-trim short display name such as `" A "` → `InvalidDisplayNameException`, duplicate pre-check input → throws `DuplicateMerchantReferenceException`, repository unique-constraint `DataIntegrityViolationException` during save → translated to `DuplicateMerchantReferenceException`. Test `findById()`: existing → returns merchant, unknown → throws `MerchantNotFoundException`. Test `activate()`: DRAFT merchant → status becomes ACTIVE, ACTIVE merchant → throws `InvalidTransitionException`. Test `suspend()`: ACTIVE merchant → status becomes SUSPENDED, DRAFT merchant → throws `InvalidTransitionException`. Use Spring Boot `OutputCaptureExtension` or equivalent where practical to verify representative create/activate/suspend traces include action/outcome/merchant context and omit passwords, tokens, and raw authorization headers.
- [X] T052 [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/internal/web/MerchantControllerTest.java`. Use `@WebMvcTest(MerchantController.class)` with `@MockBean` for `MerchantService`. Test each endpoint with valid input, invalid input, malformed UUID, and service-layer error propagation. Exclude security auto-configuration for this unit slice.
- [X] T053 [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/internal/infrastructure/JpaMerchantRepositoryTest.java`. Use `@DataJpaTest` with `@AutoConfigureTestDatabase(replace = NONE)` and a Testcontainers PostgreSQL container with a unique database name per test class (for example `merchant_repo_test_{uuid}`). Test CRUD: save and find by ID, save and find by normalized reference, save then list all ordered newest-first. Test unique constraint: save two merchants with the same normalized reference → `DataIntegrityViolationException`. Test the `findAllByOrderByCreatedAtDescMerchantIdAsc` query returns correct ordering with ties broken by merchant ID; create ties deterministically by controlling entity timestamps in the test fixture or inserting SQL seed rows with the same `created_at`.
- [X] T054 [NFR-005] [SC-003] [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/internal/application/MerchantServiceDuplicateTest.java` as a concurrency test. Use `ExecutorService` with two threads and a `CountDownLatch` to coordinate two simultaneous create attempts with the same normalized merchant reference via `MerchantService.create()`. Verify exactly one succeeds (returns 201-equivalent merchant) and the other receives `DuplicateMerchantReferenceException`. Confirm the database `UNIQUE` constraint provides the final safety net.

---

## 12. REST Assured and Security API Tests

**Purpose**: Verify HTTP contract, error responses, and authorization boundaries.

- [X] T055 [US4] [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/rest/MerchantRestAssuredTest.java`. Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with a Testcontainers PostgreSQL container using a unique database name per test class (for example `merchant_rest_test_{uuid}`). Test `GET /api/merchants` with a valid locally generated platform-operator JWT signed by the test issuer/JWK: seed 3 merchants, verify list returns them in newest-first order. Test `GET /api/merchants/{id}`: valid ID returns 200 with correct fields, unknown ID returns 404 with error body, malformed UUID returns 400. Do not use MockMvc JWT helpers in REST Assured tests because these tests run over HTTP.
- [X] T056 [US1] [TESTER-AUTOMATE] Extend `MerchantRestAssuredTest.java`: Test `POST /api/merchants` with valid request returns 201 and response body. Test blank reference → 400 with `validation` error and field details. Test blank display name → 400. Test whitespace-surrounded one-character display name such as `" A "` → 400 after service/domain trimming. Test reference too short (2 chars) → 400. Test reference too long (65 chars) → 400. Test duplicate reference (create same ref twice) → 409 with `duplicate_merchant_reference` error. Test leading hyphen → 400. Test trailing hyphen → 400.
- [X] T057 [US2] [TESTER-AUTOMATE] Extend `MerchantRestAssuredTest.java`: Test `POST /api/merchants/{id}/activate` on DRAFT → 200 with status ACTIVE. Test activate on ACTIVE → 409 with `invalid_transition`. Test activate on SUSPENDED → 409. Test activate for unknown ID → 404.
- [X] T058 [US3] [TESTER-AUTOMATE] Extend `MerchantRestAssuredTest.java`: Test `POST /api/merchants/{id}/suspend` on ACTIVE → 200 with status SUSPENDED. Test suspend on DRAFT → 409. Test suspend on SUSPENDED → 409. Test suspend for unknown ID → 404.
- [X] T059 [US5] [FR-030] [FR-031] [FR-032] [FR-033] [NFR-006] [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/security/MerchantSecurityTest.java`. Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with a Testcontainers PostgreSQL container using a unique database name per test class (for example `merchant_security_test_{uuid}`) and locally generated JWTs signed by the test issuer/JWK. Authorization matrix: unauthenticated → all merchant endpoints return 401. Malformed token, expired token, invalid issuer, and invalid signature → 401. `merchant.denied` claim profile (authenticated, no merchant authorities) → all merchant endpoints return 403. `platform.operator` claim profile (all three authorities) → all endpoints return 2xx (where data is valid). Automate partial-authority separation across all five merchant endpoints: read-only can list/retrieve but cannot create/activate/suspend; create-only can create but cannot list/retrieve/activate/suspend; status-update-only can activate/suspend when data state is valid but cannot create/list/retrieve. Verify `/api/status` returns 200 for all auth states. Use output capture where practical to verify representative `merchant.security.denied` traces distinguish denial class without logging tokens or raw authorization headers.
- [X] T060 [TESTER-AUTOMATE] Verify that `MerchantSecurityTest` uses deterministic local JWT claim profiles that mirror `platform.operator` and `merchant.denied` from the realm import (FR-036). The automated test must not require real Keycloak connectivity or dynamic user creation; the manual/local Keycloak realm walkthrough in T086 verifies the actual imported users.

---

## 13. PostgreSQL / Testcontainers Integration Tests

**Purpose**: Prove persistence, migrations, and data durability end-to-end.

- [X] T061 [TESTER-AUTOMATE] Create `apps/backend/src/test/java/lab/paymentquality/merchant/MerchantPersistenceIT.java` (suffixed `*IT` for Failsafe). Use `@SpringBootTest` with `@ServiceConnection` and a static `PostgreSQLContainer` configured with a unique database name per test class (for example `merchant_persistence_test_{uuid}`). Verify Flyway migration runs successfully on a fresh container. Create, retrieve, activate, suspend, and list merchants against the real PostgreSQL 18 database. Verify merchants persist across context reloads within the same class/container.
- [X] T062 [NFR-004] [TESTER-AUTOMATE] Add a durability scenario to `MerchantPersistenceIT.java`: create a merchant, activate it, then force a context restart within the same container. Query the merchant after restart and verify it is still ACTIVE with the same reference and display name.

---

## 14. Playwright Authenticated Merchant Journey Tests

**Purpose**: Verify the complete authenticated frontend user journey end-to-end.

- [X] T063 [TESTER-AUTOMATE] Create `apps/frontend/tests/auth/auth.setup.ts` as a Playwright setup project. Update `apps/frontend/playwright.config.ts` to register the setup project, persist `storageState`, set `fullyParallel: false` for Phase 1 Keycloak-authenticated dashboard tests, and make `merchant-create.spec.ts`, `merchant-lifecycle.spec.ts`, `auth-deny.spec.ts`, and `merchant-feedback.spec.ts` depend on that setup where appropriate. Authenticate as `platform.operator` via Keycloak login flow, save `storageState` to a file, and use this state in merchant tests to avoid re-authentication. Keep generated merchant references worker-safe so frontend parallelism can be enabled in a later phase.
- [X] T064 [US1] [TESTER-AUTOMATE] Create `apps/frontend/tests/e2e/merchant-create.spec.ts` using `storageState` from auth setup. Test: navigate to `/admin/merchants`, verify empty state is visible when no merchants exist. Click create, fill form with unique merchant reference and display name, submit. Verify success toast appears, table refreshes, and the new merchant appears with DRAFT status (SC-007).
- [X] T065 [US1] [TESTER-AUTOMATE] Extend `merchant-create.spec.ts`: Test blank reference — submit without filling reference, verify validation error shown. Test blank display name — verify validation error shown. Test duplicate reference — create a merchant, attempt to create another with the same reference, verify duplicate-conflict feedback shown.
- [X] T066 [US2] [TESTER-AUTOMATE] Create `apps/frontend/tests/e2e/merchant-lifecycle.spec.ts`. Test: create a DRAFT merchant, click activate, verify status changes to ACTIVE with green badge. Then click suspend on the ACTIVE merchant, verify status changes to SUSPENDED with red badge.
- [X] T067 [US5] [FR-030] [FR-031] [FR-032] [TESTER-AUTOMATE] Create `apps/frontend/tests/e2e/auth-deny.spec.ts`. Test: attempt to access `/admin/merchants` without authentication, verify redirect to Keycloak login and no merchant data is rendered. Attempt to access with `merchant.denied` identity (authenticated but no merchant authorities), verify the dashboard shows a deterministic insufficient-authority denial state, such as visible text "You do not have permission to view merchants" or an equivalent 403 page, and no merchant data or create/status controls are rendered.
- [X] T068 [P] [TESTER-AUTOMATE] Create `apps/frontend/tests/e2e/merchant-feedback.spec.ts`. Test loading state (mock slow API), test error state (mock server error), verify each is rendered and recoverable.

---

## 15. Parallel-Safe Test Data Strategy

**Purpose**: Ensure all tests use unique namespaced data and avoid shared mutable state.

- [X] T069 [TESTER-DESIGN] Create `docs/testing/phase-1-test-data-strategy.md` documenting the namespacing pattern for merchant references: `MERCH-{testRunId}-{workerId}-{uuid}`. Document that no test relies on a global shared merchant. Document rollback/cleanup strategy per test layer (transactional for `@DataJpaTest`, container-per-class for `@SpringBootTest`, unique references for REST Assured, unique references per scenario for Playwright). Document the Phase 1 Playwright constraint: Keycloak-authenticated dashboard specs run with `fullyParallel: false` initially to stabilize the first auth setup, while data remains worker-safe so frontend parallelism can be re-enabled in a later phase after auth/session isolation is proven.
- [X] T070 [TESTER-ANALYZE] Review all Phase 1 backend test classes and verify no test assumes a shared merchant record exists before it runs. Verify REST Assured tests use unique references per test method (e.g., `MERCH-{timestamp}-{counter}`). Verify Playwright tests generate unique references per scenario.
- [X] T071 [TESTER-DESIGN] Define the authorization matrix as a formal test design artifact: create `docs/testing/phase-1-auth-matrix.md` with rows for each actor (unauthenticated, `merchant.denied`, `platform.operator` with partial authorities, `platform.operator` with full authorities) × columns for each operation (create, read-by-id, list, activate, suspend, status) × expected HTTP status + behavior.

---

## 16. Documentation and Quickstart Updates

**Purpose**: Update project docs so new contributors and testers can use Phase 1.

- [X] T072 [AGENT-EXPLAIN] Update `apps/backend/README.md` with Phase 1 additions: mention merchant module, PostgreSQL persistence, Flyway migrations, Keycloak resource-server auth, new endpoints.
- [X] T073 [AGENT-EXPLAIN] Update `apps/frontend/README.md` with Phase 1 additions: integrate the T038a spike result, mention `nuxt-auth-utils` Keycloak integration, auth middleware, `/admin/merchants` page, server-side token forwarding, and Playwright authenticated test setup.
- [X] T074 [AGENT-EXPLAIN] Update root `README.md` repository map and Phase 1 scope summary to reference the merchant registry capability, Keycloak functional use, and new `/admin/merchants` route.
- [X] T075 [AGENT-EXPLAIN] Create `docs/setup/keycloak-local-auth.md` documenting: how to start Keycloak with the realm import, realm structure, client configuration, test identities (`platform.operator`, `merchant.denied`), and how to add new test users.
- [X] T076 [AGENT-EXPLAIN] Create `docs/setup/phase-1-merchant-orientation-pack.md` documenting: what exists, what is absent, how to run backend/frontend/infra, how to authenticate, the merchant lifecycle, the API surface, and exploratory testing charters.
- [X] T077 [AGENT-EXPLAIN] Create `docs/testing/phase-1-merchant-test-design.md` documenting: test layers, techniques applied (BVA, EP, state transition, decision tables, auth matrix), parallel-readiness decisions, and what remains untestable in Phase 1. Link to `docs/testing/phase-1-test-data-strategy.md` as the detailed source of truth for test data strategy to avoid drift.

---

## 17. Obsidian Learning Outputs

**Purpose**: Capture Phase 1 learning into the established single-vault structure.

- [X] T078 [P] [AGENT-EXPLAIN] Create `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/01 Phase 1 - Merchant Registry/Phase 1 - Merchant Registry and Activation.md` as the Phase 1 project hub note linking to spec, plan, technical learning notes, and interview capital.
- [X] T079 [P] [AGENT-EXPLAIN] Create `knowledge-vault/02 Areas/Technical Learning/Spring Modulith/Merchant Module Architecture.md` summarizing how the first real business module was introduced, the public/internal boundary, and how `@ApplicationModuleTest` was applied.
- [X] T080 [P] [AGENT-EXPLAIN] Create `knowledge-vault/02 Areas/Business Product and Testing Thinking/Phase 1 Test Design.md` summarizing the test techniques applied, decision tables written, and BVA/EP/state-transition lessons from Phase 1.
- [X] T081 [P] [AGENT-EXPLAIN] Create `knowledge-vault/02 Areas/Interview Capital/Phase 1 - Merchant Before Payments.md` explaining why merchant registry came before payment orders and how this decision improved domain clarity and testability.

---

## 18. Final Verification and Review

**Purpose**: Gate all Phase 1 work before declaring done.

- [X] T082 [AGENT-REVIEW] Run `./mvnw test` from `apps/backend`. Accept: all unit, service, repository, module, and architecture tests pass. Verify output shows no test failures.
- [X] T083 [AGENT-REVIEW] Run `./mvnw verify` from `apps/backend`. Accept: Failsafe picks up `*IT.java` tests. Verify Testcontainers integration tests pass with PostgreSQL 18.
- [X] T084 [AGENT-REVIEW] Run `corepack pnpm typecheck` and `corepack pnpm build` from `apps/frontend`. Accept: TypeScript checks pass, Nuxt build succeeds.
- [X] T085 [AGENT-REVIEW] Run `corepack pnpm exec playwright test` from `apps/frontend`. Accept: all Playwright tests pass, including auth setup, merchant journeys, and denial-path tests.
- [X] T086 [NFR-001] [SC-001] [SC-007] [AGENT-REVIEW] Start local infrastructure with `docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d`. Verify Keycloak realm import creates `platform.operator` and `merchant.denied`. Start backend, verify `GET /api/status` returns 200. Attempt `GET /api/merchants` without token → 401. Authenticate via dashboard, verify full merchant journey per SC-007. Measure SC-001 with a simple stopwatch walkthrough: start timing when the authenticated operator opens `/admin/merchants`, stop when the newly created merchant is visible in the registry, and document whether the flow is practically completable within the NFR-001 target of under 2 minutes. Review successful and failed create/activate/suspend traces plus security denial traces and confirm they support traceability without exposing passwords, tokens, or raw authorization headers. VERIFIED: Flyway migration V1 executed, merchants table schema correct. Keycloak realm imported with both users. GET /api/status → 200 unauthenticated. GET /api/merchants without token → 401. merchant.denied → 403 on all merchant endpoints. platform.operator → full CRUD lifecycle (create DRAFT → activate → ACTIVE → suspend → SUSPENDED). Duplicate reference → 409. Invalid transition → 409. SC-001: create-to-visible flow is under 2 minutes via dashboard. Auth denial traces distinguish denial class without exposing tokens.
- [X] T087 [FR-037] [SC-009] [AGENT-REVIEW] Perform non-goal audit: search for payment order creation, payment status model, PSP integration, Kafka, settlements, reconciliation, Client Credentials Flow, KYC, currency rules. Verify none exist in the Phase 1 implementation.
- [X] T088 [AGENT-REVIEW] Perform parallel-readiness audit: verify no test depends on shared mutable merchant data. Verify all merchant reference tests use unique namespaced references. Verify REST Assured tests create their own data. Verify Playwright tests use unique references per scenario.
- [X] T089 [AGENT-REVIEW] Verify Spring Modulith architecture: confirm `ApplicationModules.of(PaymentQualityApplication.class).verify()` passes. Confirm no `payment`, `psp`, `refund`, `settlement`, or `reconciliation` packages exist.
- [X] T090 [TESTER-ANALYZE] Execute the Phase 1 Tester Orientation Pack walkthrough from `docs/setup/phase-1-merchant-orientation-pack.md`. Record remaining risks, confusing areas, or undocumented behavior.
- [X] T091 [DISCUSS] Hold Phase 1 readiness review. Acceptance: all SC-001 through SC-009 from spec.md are satisfied. Decision: accept, accept with documented follow-ups, or block further phases until gaps are resolved.

---

## Dependencies and Execution Order

### Sequential Dependencies

1. **Group 1 (Setup)**: No dependencies — gate check only.
2. **Group 2 (Database)**: Depends on Group 1 (dependencies added).
3. **Group 3 (Domain Model)**: Depends on Group 2 (table exists for entity mapping review, but pure Java code is independently testable). T010-T011 can start in parallel with T007.
4. **Group 4 (Application Layer)**: Depends on Group 3 (entities and value objects).
5. **Group 5 (REST API)**: Depends on Group 4 (service layer) and Group 7 (security config must exist for `@PreAuthorize`).
6. **Group 6 (Modulith)**: Depends on Group 4 (module packages exist). T030 must run after packages created.
7. **Group 7 (Backend Security)**: Depends on Group 1 (dependencies added). Independent of Groups 2-6.
8. **Group 8 (Keycloak Realm)**: Depends on Group 1 only. Parallel with Groups 2-7.
9. **Group 9 (Frontend Auth)**: Depends on Group 8 (realm must exist for client config). T038a depends on T038 and must complete before T039-T042a. T039-T042a are otherwise independent of backend Groups 2-7.
10. **Group 10 (Admin UI)**: Depends on Group 9 (auth middleware) and Group 5 (API endpoints must exist).
11. **Group 11 (Backend Tests)**: Depends on Groups 3-5 (code under test). T048-T050 can start once domain model exists. T051 depends on service. T052 depends on controller.
12. **Group 12 (REST/Security Tests)**: Depends on Groups 5 and 7 (API + security integration). Requires Group 2 for Testcontainers PostgreSQL. Does not require Group 8 because automated REST/security tests use locally generated JWTs; Group 8 is required for local realm walkthroughs such as T086 and Playwright authentication.
13. **Group 13 (Integration Tests)**: Depends on Groups 2, 3, 4, 5 (full persistence stack). Must have Testcontainers PostgreSQL available.
14. **Group 14 (Playwright Tests)**: Depends on Groups 9 and 10 (frontend auth + UI). Requires Group 8 (realm) and Groups 2, 5 (backend running).
15. **Group 15 (Test Data Strategy)**: Can start in parallel with implementation. T069-T071 are documentation/analysis tasks.
16. **Group 16 (Documentation)**: Depends on all implementation groups completed.
17. **Group 17 (Obsidian)**: Can start once implementation is stable. Parallel with Group 16.
18. **Group 18 (Verification)**: Depends on all preceding groups completed.

### Parallel Opportunities

- Groups 2, 3, 7, 8 can start in parallel after Group 1.
- Within Group 3 (Domain): T010, T011 are independent (different classes).
- Within Group 10 (UI): T045, T046, T047 are independent (different components).
- Within Group 11 (Backend Tests): T048, T049 are fully parallel (different test classes, pure unit).
- Groups 16 and 17 are fully parallel (different files, no code dependencies).
- Within Group 17 (Obsidian): T078, T079, T080, T081 are independent notes.

---

## Parallel Execution Examples

### Early Parallel Window (after Group 1)

```text
Worker 1: Group 2 (DB migration) → Group 3 (Domain model) → Group 4 (Service) → Group 5 (REST API)
Worker 2: Group 7 (Backend security) → continues to SecurityConfig
Worker 3: Group 8 (Keycloak realm) → Group 9 (Frontend auth) → Group 10 (Admin UI)
```

### Test Parallel Window (after implementation)

```text
Worker 1: Group 11 T048-T052 (domain, service, controller unit tests)
Worker 2: Group 11 T053-T054 (repository and concurrency tests) → Group 12 T055-T058 (REST Assured API tests)
Worker 3: Group 12 T059-T060 (security tests)
Worker 4: Group 13 T061-T062 (integration tests)
Worker 1+2: Group 14 (Playwright tests after backend + frontend available)
```

---

**Task Count**: 95 checklist tasks represented; `T044` is intentionally split into `T044a`/`T044b`/`T044c` and there is no standalone `T044` task  
**Major Groups**: 18 groups covering setup, database, domain, application, REST API, Modulith, security, Keycloak, frontend auth, admin UI, backend tests, REST/security tests, integration tests, Playwright tests, test data strategy, documentation, Obsidian outputs, and final verification  

**Artifact Readiness**: Ready for `/speckit.analyze`
