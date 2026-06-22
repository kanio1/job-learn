# Deterministic Seed and Test Isolation Execution Notes

## Wave 1 — Stage 1 module + seed capabilities

Status: `BLOCKED_FIXTURE_CATALOG_AMBIGUITY`

### Prerequisite gate

- `.codex/audit-log-dashboard.md` and `.codex/current-state.md` record the
  `audit-log-dashboard` final checkpoint as `COMPLETE_WITH_OPTIONAL_GAPS`.
- Both status documents record that the next spec had not been started.
- The checked-out branch is `018-rest-security-p1-error-auth-method-hardening`.
- The prerequisite gate is therefore satisfied.

### Blocker

The authoritative fixture catalog is incomplete. The design enumerates six base
payment orders (`...c1` through `...c6`), then requires an additional pagination
and summary block using `...c1xx`. It says that the exact count and UUIDs are
enumerated in `Fixtures`, but `Fixtures` does not exist yet and `design.md` does
not enumerate that block. It also does not define the block's exact
`clientOrderReference` values or fixed per-order fields.

Inventing those identities would violate the request to stop rather than create
random or unauthoritative fixtures. No Wave 1 production code was created.

### Implemented

- No implementation started because the fixture-catalog gate failed before editing.

### Changed files

- `.codex/deterministic-seed-and-test-isolation.md`
- `.codex/current-state.md`

### Validation

Commands:

- `git branch --show-current`
- prerequisite/status inspection of `.codex/audit-log-dashboard.md` and `.codex/current-state.md`
- fixture-catalog inspection of `.kiro/specs/deterministic-seed-and-test-isolation/design.md`
- `git diff --check`

Results:

- branch and prerequisite gate: GREEN
- fixture-catalog completeness: BLOCKED
- compile, test-compile, module tests, and boundary grep: not run because implementation did not start

### Security / production safety

- no seed runner added
- no test endpoints added
- no prod behavior changed
- no secrets exposed
- no Playwright files
- no frontend files

### Module boundaries

No Java module changes were made, so existing module boundaries are unchanged.

### Deferred

- testing module skeleton deferred until the fixture catalog is clarified
- merchant and payment seed capabilities deferred
- Fixtures and DeterministicDataset deferred
- SeedRunner and endpoints deferred to Wave 2
- tests deferred to Wave 3
- tenant seeding deferred to a later explicitly requested wave
- audit seeding deferred

### Next

Wave 1 may resume only after the pagination/summary fixture block is made
authoritative: exact count, UUIDs, client order references, amounts, currencies,
statuses, and versions. Wave 2 may not start. Wave 2 was not started.

## Fixture Catalog Resolution

The Wave 1 blocker is resolved by the following authoritative catalog. This
catalog lives in `.codex/**`; `.kiro/**` remains read-only and unchanged.

### Tenants

| Reference | UUID | Type | Status | Name |
|---|---|---|---|---|
| `PLATFORM_TENANT` | `00000000-0000-0000-0000-0000000000a1` | `PLATFORM` | `ACTIVE` | `Platform Tenant` |
| `TENANT_ALPHA` | `00000000-0000-0000-0000-0000000000a2` | `STANDARD` | `ACTIVE` | `Alpha Tenant` |
| `PLACEHOLDER_TENANT_ID` | `00000000-0000-0000-0000-0000000000a3` | `STANDARD` | `ACTIVE` | `Placeholder Tenant` |

### Merchants

| Reference | UUID | Tenant | Status | Display name |
|---|---|---|---|---|
| `MERCHANT_ALPHA_001` | `00000000-0000-0000-0000-0000000000b1` | `TENANT_ALPHA` | `ACTIVE` | `Alpha Merchant 001` |
| `MERCHANT_ALPHA_002` | `00000000-0000-0000-0000-0000000000b2` | `TENANT_ALPHA` | `ACTIVE` | `Alpha Merchant 002` |
| `MERCHANT_BETA_001` | `00000000-0000-0000-0000-0000000000b3` | `PLATFORM_TENANT` | `ACTIVE` | `Beta Merchant 001` |

### Base payment orders

| UUID | Merchant | Client order reference | Status | Currency | Amount minor | Version |
|---|---|---|---|---|---:|---:|
| `00000000-0000-0000-0000-0000000000c1` | `MERCHANT_ALPHA_001` | `SEED-ALPHA-001-CREATED` | `CREATED` | `PLN` | 1100 | 0 |
| `00000000-0000-0000-0000-0000000000c2` | `MERCHANT_ALPHA_001` | `SEED-ALPHA-001-AUTHORIZED` | `AUTHORIZED` | `EUR` | 2200 | 1 |
| `00000000-0000-0000-0000-0000000000c3` | `MERCHANT_ALPHA_001` | `SEED-ALPHA-001-CAPTURED` | `CAPTURED` | `USD` | 3300 | 2 |
| `00000000-0000-0000-0000-0000000000c4` | `MERCHANT_ALPHA_002` | `SEED-ALPHA-002-CANCELLED` | `CANCELLED` | `PLN` | 4400 | 1 |
| `00000000-0000-0000-0000-0000000000c5` | `MERCHANT_ALPHA_002` | `SEED-ALPHA-002-REFUNDED` | `REFUNDED` | `EUR` | 5500 | 3 |
| `00000000-0000-0000-0000-0000000000c6` | `MERCHANT_BETA_001` | `SEED-BETA-001-CREATED` | `CREATED` | `PLN` | 6600 | 0 |

Base records use fixed UTC creation instants from `2026-01-15T09:30:00Z`
through `2026-01-15T09:35:00Z`, in catalog order. Their terminal fields follow
the same deterministic status rules as the expansion block.

### Pagination / summary block

For every integer `n` from 101 through 198 inclusive, one order is owned by
`MERCHANT_ALPHA_001` with UUID
`00000000-0000-0000-0000-00000000c{n}`, client reference
`SEED-ALPHA-001-C{n}`, amount 1000, and creation time
`2026-01-15T10:00:00Z + (n - 101) minutes`.

- Currency by `(n - 101) % 3`: 0 = `PLN`, 1 = `EUR`, 2 = `USD`.
- Status/version by `(n - 101) % 5`: 0 = `CREATED`/0,
  1 = `AUTHORIZED`/1, 2 = `CAPTURED`/2, 3 = `CANCELLED`/1,
  4 = `REFUNDED`/3.
- `AUTHORIZED`: authorization at +1 minute; expiry at authorization +7 days.
- `CAPTURED`: authorization at +1 minute; capture at +2 minutes; full amount captured.
- `CANCELLED`: cancellation at +1 minute with reason `seed-cancelled`.
- `REFUNDED`: authorization at +1, capture at +2, refund at +3 minutes;
  full amount captured/refunded with reason `seed-refunded`.
- `updatedAt` is the terminal timestamp, or `createdAt` for `CREATED`.

Expected `MERCHANT_ALPHA_001` summary: 101 orders; statuses `CREATED` 21,
`AUTHORIZED` 21, `CAPTURED` 21, `CANCELLED` 19, `REFUNDED` 19;
currencies `PLN` 34, `EUR` 34, `USD` 33.

## Wave 1R — Fixture Catalog Resolution and Tenant Capability Pull-In

Status: `COMPLETED`

### Tenant capability pull-in

`tenant-model-and-isolation` is complete, `merchants.tenant_id` is `NOT NULL`
and FK-constrained, while the tenant migration creates its initial UUIDs with
`gen_random_uuid()`. Tenant seeding was therefore pulled into Wave 1R so every
merchant can reference a fixed, existing tenant UUID and repeated dataset seeds
converge to the same ownership model.

### Implemented

- `testing` Spring Modulith module skeleton with internal seed/web packages and no PUBLIC API
- `TenantSeedCapability`, `TenantSeed`, and internal `TenantSeedService`
- `MerchantSeedCapability`, `MerchantSeed`, and internal `MerchantSeedService`
- `PaymentSeedCapability`, `PaymentOrderSeed`, and internal `PaymentSeedService`
- deterministic entity seed factories local to tenant, merchant, and payment modules
- `Fixtures` with 3 tenants, 3 merchants, 6 base orders, and 98 expansion orders
- one deterministic synthetic `CREATED` history row per payment order
- `DeterministicDataset` transactional reset and seed orchestration

### Changed files

- `apps/backend/src/main/java/lab/paymentquality/testing/package-info.java`
- `apps/backend/src/main/java/lab/paymentquality/testing/internal/seed/Fixtures.java`
- `apps/backend/src/main/java/lab/paymentquality/testing/internal/seed/DeterministicDataset.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantSeed.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/TenantSeedCapability.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/application/TenantSeedService.java`
- `apps/backend/src/main/java/lab/paymentquality/tenant/internal/domain/Tenant.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantSeed.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantSeedCapability.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantSeedService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/domain/Merchant.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/PaymentOrderSeed.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/PaymentSeedCapability.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentSeedService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrder.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/domain/PaymentOrderStatusHistory.java`
- `.codex/deterministic-seed-and-test-isolation.md`
- `.codex/current-state.md`

### Deterministic orchestration

- `reset()`: payment clear, merchant clear, tenant clear.
- `seed()`: payment clear, merchant clear, tenant clear, tenant seed, merchant seed, payment seed.
- Both operations use one transaction at the dataset boundary.
- Payment clear removes idempotency records, status history, then payment orders.

### Validation

Commands:

- `./mvnw compile`
- `./mvnw test-compile`
- `./mvnw -Dtest=ModulithArchitectureTest,TenantModuleTest,MerchantModuleTest,PaymentModuleTest test`
- required testing-module internal-import grep
- cross-module internal-import review
- production-safety route/runner/config scan
- `git diff --check`

Results:

- compile: GREEN
- test-compile: GREEN
- architecture/module tests: GREEN, 7 tests, 0 failures/errors/skips
- initial sandboxed module-test attempt: infrastructure-blocked by Podman socket access;
  repeated with approved local Podman access and passed
- testing-module internal-import grep: no matches
- `ModulithArchitectureTest`: GREEN
- `git diff --check`: GREEN

### Security / production safety

- no seed runner added
- no `/api/test/**` endpoint or controller added
- no security permit rule or profile/config behavior added
- no startup seeding behavior added
- no audit events published by seeding
- no PSP data, idempotency keys, raw payloads, tokens, or secrets added
- no frontend or Playwright files added

### Module boundaries

The `testing` module imports only PUBLIC `TenantSeedCapability`,
`MerchantSeedCapability`, `PaymentSeedCapability`, and their PUBLIC value
records. Each implementation imports only its own module's internals. Spring
Modulith architecture verification is green.

### Deferred

- SeedRunner, endpoints, guards, security rules, and configuration remain Wave 2
- tests specific to Fixtures/Dataset/endpoints remain Wave 3
- audit seeding remains deferred

### Next

Wave 2 may start only when explicitly requested. Wave 2 was not started.

## Wave 2 — Stage 1 runner + endpoints + guards

Status: `BLOCKED_SECURITY_CONFIG_AMBIGUITY`

### Prerequisite gate

- Wave 1R is `COMPLETED` in this file and `.codex/current-state.md`.
- The `testing` module skeleton, `MerchantSeedCapability`,
  `PaymentSeedCapability`, `Fixtures`, and `DeterministicDataset` exist.
- No Wave 2 runner, controller, response DTO, production flag override, or test
  endpoint security rule existed before this attempt.

### Blocker

The existing primary `SecurityFilterChain` ends with
`.anyRequest().authenticated()`. A conditional `/api/test/**` `permitAll` rule
can be added safely for `!prod && app.testing.enabled=true`, but when the flag is
false there is intentionally no permit rule. An unauthenticated request to the
absent controller is then rejected by Spring Security with `401` before MVC can
resolve the missing handler as `404`.

The following possible workarounds conflict with the active-wave rules:

- permitting `/api/test/**` while the flag is false creates a live special
  security rule outside the required `flag=true` condition;
- registering a disabled placeholder handler violates the requirement that the
  route and handler mapping be absent;
- changing the global catch-all to permit unmatched requests changes existing
  security behavior and broadens the production attack surface;
- returning a special `404` from an authentication entry point adds special
  disabled-route behavior and still exposes path-specific security handling.

No safe interpretation simultaneously guarantees an unauthenticated disabled
`404`, no disabled-path security rule, no handler, and the unchanged authenticated
catch-all. Per the prompt, implementation stopped rather than adding an unsafe
unconditional rule.

### Implemented

- No Wave 2 production implementation was started.
- `SeedRunner`, `TestController`, `TestOperationResponse`, prod configuration,
  and security rules remain unchanged/deferred.

### Changed files

- `.codex/deterministic-seed-and-test-isolation.md`
- `.codex/current-state.md`

### Validation

Commands/checks:

- prerequisite and required-file inspection
- `SecurityConfig` and all `SecurityFilterChain` definitions inspection
- application/profile configuration inspection
- existing correlation filter inspection
- `git diff --check`

Results:

- Wave 1 prerequisite: GREEN
- conditional enabled permit feasibility: GREEN
- disabled unauthenticated 404 under the current catch-all: BLOCKED
- compile/test-compile/module tests: not run because production code was not changed

### Production safety

- seed runner profile gate: not implemented
- controller flag gate: not implemented
- prod fail-safe: not implemented
- prod flag override: not implemented
- disabled endpoint expected to be 404: unresolved by the contradictory security constraints
- prod endpoint expected to be 404: can be protected by profile absence, but Wave 2 was not partially implemented

### Security / confidentiality

- no unconditional permit rule was added
- no endpoint, token, credential, secret, payload, or sensitive log was added
- existing merchant, payment, tenant, IAM, and audit security behavior is unchanged

### Deferred

- all Wave 2 implementation is deferred pending an explicit security decision
- integration tests for startup seed, endpoints, disabled 404, and prod 404 remain Wave 3
- jqwik properties remain Wave 3 or optional follow-up
- no frontend or Playwright work

### Decision needed before resume

Choose one explicit contract adjustment:

1. Allow a narrowly scoped non-prod `/api/test/**` pass-through/permit rule even
   when the flag is false, relying on controller absence to produce `404`; or
2. Require authentication when disabled and accept the current unauthenticated
   `401` while authenticated requests receive `404`; or
3. Approve a path-specific disabled `404` security entry point despite it being
   special route behavior.

### Next

Wave 3 may not start. Wave 3 was not started.

## Wave 2R — Checkpoint Repair after Security Decision

Status: `COMPLETED`

### Why this section exists

Wave 2 was previously blocked because the main `SecurityFilterChain` returned `401` before MVC could produce absent-handler `404` for disabled test endpoints.

The explicit security decision was to add a narrow pass-through `SecurityFilterChain` for exactly:

- `POST /api/test/reset`
- `POST /api/test/seed`

This pass-through chain is not the feature flag. It only lets MVC produce the required `404` when the controller is absent.

### Security decision documented

The previous Wave 2 blocker was resolved by approving a narrow pass-through SecurityFilterChain for exactly POST /api/test/reset and POST /api/test/seed.

This pass-through chain is not the feature flag.

The feature flag is controller registration.

When app.testing.enabled=false, the TestController bean is absent. The pass-through chain allows the request to reach MVC, and MVC returns absent-handler 404.

When app.testing.enabled=true in non-prod, the TestController bean exists and returns 200.

In prod, application-prod.yml forces app.testing.enabled=false and TestController is also excluded by @Profile("!prod"), so no operation can execute and the request returns 404.

The global authenticated catch-all remains unchanged for all other API paths.

### Verified implementation

- `SeedRunner`: present (`@Component`, `@Profile("seed & !prod")`, implements `ApplicationRunner`, calls `DeterministicDataset.seed()`).
- `TestController`: present (`@RestController`, `@RequestMapping("/api/test")`, `@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")`, `@Profile("!prod")`).
- `TestOperationResponse`: present, minimal `operation`/`status` record only.
- Default `app.testing.enabled=false`: present in `application.yml`.
- Production `app.testing.enabled=false`: present in `application-prod.yml`.
- Exact POST-only pass-through chain (`@Order(2)`): present in `SecurityConfig`.
- Main JWT chain (`@Order(3)`): unchanged, still ends with `.anyRequest().authenticated()`.

### Changed files already present in code

- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/testing/internal/seed/SeedRunner.java`
- `apps/backend/src/main/java/lab/paymentquality/testing/internal/web/TestController.java`
- `apps/backend/src/main/java/lab/paymentquality/testing/internal/web/TestOperationResponse.java`
- `apps/backend/src/main/resources/application.yml`
- `apps/backend/src/main/resources/application-prod.yml`
- `.codex/deterministic-seed-and-test-isolation.md`
- `.codex/current-state.md`

### Validation

Commands:

- `git branch --show-current` → `018-rest-security-p1-error-auth-method-hardening`
- `git status --short` → only untracked `CLAUDE.md`
- `cd apps/backend && ./mvnw compile`
- `./mvnw test-compile`
- `./mvnw -Dtest=ModulithArchitectureTest test`
- `export DOCKER_HOST=unix://${XDG_RUNTIME_DIR}/podman/podman.sock && export TESTCONTAINERS_RYUK_DISABLED=true && ./mvnw -Dtest=TenantModuleTest,MerchantModuleTest,PaymentModuleTest test`
- static `grep` checks for secrets, `/api/test` paths, and cross-module internals
- `git diff --check`

Results:

- compile: GREEN
- test-compile: GREEN
- `ModulithArchitectureTest`: GREEN, 1 test, 0 failures/errors/skips
- `TenantModuleTest`, `MerchantModuleTest`, `PaymentModuleTest`: GREEN, 6 tests, 0 failures/errors/skips (Podman available)
- security/secrets grep: no matches (no tokens, passwords, PAN/CVV, or raw payloads)
- cross-module internal-import grep: no matches
- `git diff --check`: GREEN

### Environment validation gap

None — container-backed module tests were rerun with Podman and passed (6 tests, all green).

### Security / confidentiality

- no tokens exposed: confirmed
- no passwords exposed: confirmed
- no temporary passwords exposed: confirmed
- no PAN/CVV exposed: confirmed
- no raw Authorization exposed: confirmed
- no raw request/response body exposed: confirmed
- no generic payload exposed: confirmed
- no seeded data dump in endpoint responses: confirmed (`TestOperationResponse` has only `operation` and `status`)

### Production safety

- `SeedRunner` guarded by `seed & !prod`: confirmed (`@Profile("seed & !prod")`)
- `TestController` guarded by `app.testing.enabled=true & !prod`: confirmed (`@ConditionalOnProperty` + `@Profile("!prod")`)
- `app.testing.enabled` default false: confirmed in `application.yml`
- prod override false: confirmed in `application-prod.yml`
- pass-through exact POST-only: confirmed (`PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/test/reset")` and `/api/test/seed` only)
- main chain remains authenticated catch-all: confirmed (`.anyRequest().authenticated()` unchanged at `@Order(3)`)
- production operation cannot execute because controller is absent: confirmed (both `@ConditionalOnProperty` and `@Profile("!prod")` guard the bean)

### Module boundaries

- testing module imports only PUBLIC seed capabilities: confirmed
- no testing import of tenant/merchant/payment/audit internals: confirmed (grep: no matches)

### Deferred

- Wave 3R tests not started.
- Endpoint runtime behavior 200/404 will be proven in Wave 3R.
- No frontend.
- No Playwright.
- No `.kiro/**` changes.

### Next

Wave 3R may start only when explicitly requested.
Wave 3R was not started.

## Wave 3R — Validation Test Suite

Status: `COMPLETED`

### Production fix driven by tests

`PaymentSeedService.seed()` was changed to use `entityManager.createNativeQuery()` INSERT instead of `entityManager.persist()` or `entityManager.merge()`:

- `persist()` throws `PersistentObjectException: Detached entity passed to persist` for entities with an assigned UUID `@Id` and a non-null `@Version` (Hibernate detects non-null version as indicator of a pre-existing entity).
- `merge()` throws `StaleObjectStateException: Row was already updated or deleted` because Hibernate performs a SELECT, finds no existing row, and concludes the row was deleted by another transaction.
- Native SQL INSERT bypasses JPA entity-state detection entirely and inserts exact `version` values required by the fixture catalog.

This is the only Wave 1R/Wave 2R production file changed by Wave 3R.

### Test classes created

**Surefire unit tests (`*Test.java`):**

- `apps/backend/src/test/java/lab/paymentquality/testing/internal/seed/FixturesTest.java` — 24 tests: tenant/merchant/payment UUID/reference/count/uniqueness assertions; MERCHANT_ALPHA_001 101 orders; deterministic status/currency formula; expansion UUID pattern; fixture call stability.
- `apps/backend/src/test/java/lab/paymentquality/testing/internal/seed/DeterministicDatasetTest.java` — 3 tests: reset/seed delegation order via Mockito `InOrder`; dataset depends only on public capability interfaces.
- `apps/backend/src/test/java/lab/paymentquality/testing/internal/web/TestOperationResponseTest.java` — 6 tests: exactly 2 JSON fields (operation/status); no forbidden fields (token, password, PAN, CVV, merchantId, paymentOrderId, authorization, payload, etc.); record components; field name correctness.
- `apps/backend/src/test/java/lab/paymentquality/testing/TestingModuleTest.java` — 4 tests: `ApplicationModules.verify()`; module present; no explicit named interfaces; no domain module depends on testing.
- `apps/backend/src/test/java/lab/paymentquality/security/TestEndpointSecurityChainTest.java` — 5 tests: POST /api/test/reset → 404 (pass-through, no controller); POST /api/test/seed → 404; GET /api/test/reset → 401 (not in pass-through chain); POST /api/does-not-exist → 401; pass-through 404 carries X-Correlation-ID.

**Failsafe integration tests (`*IT.java`):**

- `apps/backend/src/test/java/lab/paymentquality/testing/SeedProfileStartupIT.java` — 8 tests: context loads with "seed" profile; SeedRunner populates 3 tenants, 3 merchants, 104 payment_orders, 104 history rows on startup; deterministic UUIDs a1-a3, b1-b3 present; MERCHANT_ALPHA_001 has 101 orders; re-seeding converges to same state.
- `apps/backend/src/test/java/lab/paymentquality/testing/TestEndpointsEnabledIT.java` — 9 tests: POST /api/test/seed → 200 with operation/status body and no forbidden fields; POST /api/test/reset → 200; seed loads 104 orders; MERCHANT_ALPHA_001 has 101 orders; reset clears all data; seed-after-reset restores state; idempotency; deterministic UUIDs c1/c101; MERCHANT_ALPHA_001 status counts 21/21/21/19/19.
- `apps/backend/src/test/java/lab/paymentquality/testing/TestEndpointsDisabledIT.java` — 4 tests: POST /api/test/reset → 404 (not 401) when enabled=false; POST /api/test/seed → 404; pass-through 404 carries X-Correlation-ID; TestController bean absent.
- `apps/backend/src/test/java/lab/paymentquality/testing/TestEndpointsProdSafetyIT.java` — 4 tests: POST /api/test/reset → 404 with prod profile even when enabled=true; POST /api/test/seed → 404; TestController bean absent; SeedRunner bean absent.

### Changed files

**Production (minimal Wave 3R fix):**
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentSeedService.java` — `seed()` uses `createNativeQuery()` INSERT instead of `persist()`/`merge()`

**Tests (new):**
- `apps/backend/src/test/java/lab/paymentquality/testing/internal/seed/FixturesTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testing/internal/seed/DeterministicDatasetTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testing/internal/web/TestOperationResponseTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testing/TestingModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/TestEndpointSecurityChainTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testing/SeedProfileStartupIT.java`
- `apps/backend/src/test/java/lab/paymentquality/testing/TestEndpointsEnabledIT.java`
- `apps/backend/src/test/java/lab/paymentquality/testing/TestEndpointsDisabledIT.java`
- `apps/backend/src/test/java/lab/paymentquality/testing/TestEndpointsProdSafetyIT.java`

### Validation

Commands:

```bash
export DOCKER_HOST=unix://${XDG_RUNTIME_DIR}/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
cd apps/backend

# Focused IT run
./mvnw -Dit.test="SeedProfileStartupIT,TestEndpointsEnabledIT,TestEndpointsDisabledIT,TestEndpointsProdSafetyIT" failsafe:integration-test failsafe:verify

# Full filtered verify
./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify
```

Results:

- Focused IT: `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS
- Full filtered Surefire: `Tests run: 411, Failures: 0, Errors: 0, Skipped: 5`
- Full filtered Failsafe: `Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`
- Combined: BUILD SUCCESS

New unit tests (Surefire): FixturesTest 24, DeterministicDatasetTest 3, TestOperationResponseTest 6, TestingModuleTest 4, TestEndpointSecurityChainTest 5 = **42 new unit tests**
New IT tests (Failsafe): SeedProfileStartupIT 8, TestEndpointsEnabledIT 9, TestEndpointsDisabledIT 4, TestEndpointsProdSafetyIT 4 = **25 new IT tests**

### Security / confidentiality

- no tokens exposed: confirmed (`TestOperationResponse` has only `operation` and `status`)
- no passwords exposed: confirmed
- no temporary passwords, PAN/CVV, raw Authorization, request/response body, generic payload: confirmed (verified by `TestOperationResponseTest.noForbiddenFieldsInSerializedResponse`)
- `POST /api/test/**` pass-through does not broaden to GET/PUT/PATCH/DELETE: confirmed (`TestEndpointSecurityChainTest.getRequestToTestResetReturns401`)
- production safety preserved: confirmed (`TestEndpointsProdSafetyIT` all 4 tests green)

### Production safety

- controller absent when `app.testing.enabled=false`: confirmed (`TestEndpointsDisabledIT.testControllerBeanIsAbsent`)
- controller absent under prod profile even with flag forced true: confirmed (`TestEndpointsProdSafetyIT.testControllerBeanIsAbsentUnderProdProfile`)
- SeedRunner absent under prod profile: confirmed (`TestEndpointsProdSafetyIT.seedRunnerBeanIsAbsentUnderProdProfile`)
- pass-through returns 404 (not 401) for disabled endpoint: confirmed (`TestEndpointsDisabledIT` both POST tests)

### Module boundaries

- testing module imports only PUBLIC seed capabilities: confirmed (Wave 2R grep, no new internal imports)
- `.kiro/**` modified: no
- frontend/Playwright: no

### Optional jqwik

Deferred: `OPTIONAL_JQWIK_DEFERRED`. No jqwik property tests were written for the testing module. The property-based test approach is documented here and can be added in a follow-up.

### Next

`deterministic-seed-and-test-isolation` spec is complete. Wave 4 was not started and is not needed unless the user explicitly requests additional work on this spec.
