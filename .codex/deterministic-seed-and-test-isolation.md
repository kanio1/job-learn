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
