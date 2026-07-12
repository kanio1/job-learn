---
name: latest-validation
last_updated: 2026-07-12
audited_branch: 001-project-foundation
audited_commit: c6de61f31e7cadc09331269f0f33e70573e4b889
---

# Latest Validation Evidence

## Session 4 (2026-07-12, commit `c6de61f`) — TD-2A fix validation

Ran after fixing TD-2A (see `status/technical-debt/current-baseline.md` TD-2A for full root-cause detail). Two changes: `apps/frontend/playwright.config.ts` (`expect.timeout: 15_000`), `apps/frontend/tests/e2e/merchant-support.ts` + `apps/frontend/tests/e2e/auth-deny.spec.ts` (`mockAuthenticatedSession` now defaults to `roles: ['PLATFORM_ADMIN']`). No backend files touched this session.

### Diagnostic steps (not part of the fix, recorded for reproducibility)
- Temporary probe spec (`tests/e2e/zzprobe.spec.ts`, created and deleted within this session) used to capture console/pageerror/network/DOM state directly — confirmed no thrown JS error, confirmed `vite-plugin-checker-error-overlay` present in the DOM (a red herring — investigated and ruled out as the blocking mechanism; the actual button-click hangs were due to missing RBAC roles, not the overlay), confirmed first-render latency of 4.1–4.6s via `page.getByRole('heading', {name:'Merchants'}).waitFor(...)` timing.
- `git show 8861f84 -- apps/frontend/...` not applicable here (that commit is backend-only); RBAC evidence instead came from `infra/keycloak/realms/payment-quality-realm.json` (`platform.operator` → realm role `PLATFORM_ADMIN`) cross-referenced with `apps/frontend/app/utils/rbacMatrix.ts` and `apps/frontend/app/composables/useAuthorization.ts`.

### `cd apps/frontend && corepack pnpm exec playwright test <8 originally-failing files> --project=chromium` (before any fix)
Result: **21 failed / 61 passed** (of 82 executed) — reproduces session 1's baseline exactly, confirming no drift since then.

### `cd apps/frontend && corepack pnpm exec playwright test <8 files> --project=chromium --workers=1` (diagnostic: contention removed, no fix yet)
Result: **13 failed / 16 passed** — proves 8 of the 21 are pure worker-contention/timing failures (fixed by removing contention alone, before any code change).

### `cd apps/frontend && corepack pnpm exec playwright test <8 files> --project=chromium` (after `expect.timeout: 15_000` only)
Result: **13 failed / 16 passed** — matches the serial (uncontended) count exactly; confirms the timeout increase alone fixes the same 8 tests that contention-removal fixed, with 13 persisting.

### `cd apps/frontend && corepack pnpm exec playwright test tests/e2e/merchant-create.spec.ts tests/e2e/merchant-lifecycle.spec.ts tests/e2e/auth-deny.spec.ts --project=chromium` (after both fixes)
Result: **8 failed / 7 passed** — 2 more fixed by the roles change (`merchant-create.spec.ts` "create-merchant-form data-testid..." and "activate-merchant-button data-testid... for a pending merchant").

### `cd apps/frontend && corepack pnpm exec playwright test <original 8 files> --project=chromium` (both fixes, final targeted check)
Result: **11 failed / 18 passed** (down from 21 failed / 61 passed at session start). 10 net fixed, 0 new failures in this targeted scope.

### `cd apps/frontend && corepack pnpm exec playwright test --project=chromium` (both fixes, full 101-test suite)
Result: **12 failed / 70 passed** (of 82 executed; up from 61 passed at session start). One extra failure vs. the 11-test targeted scope — `payment-status-polling.spec.ts:52` — reappeared under full-82-test 16-worker contention. Re-verified in isolation immediately after: `corepack pnpm exec playwright test tests/e2e/payment-status-polling.spec.ts --project=chromium` → **3/3 passed** in 19.6s (4.7s/4.0s per test, well inside the new 15000ms budget). Classified as residual environmental flakiness under worst-case full-suite load, not a new defect — the `expect.timeout` fix substantially reduces but does not 100% eliminate contention-driven timeouts under the full 16-worker run.

### `cd apps/frontend && corepack pnpm typecheck` (after both fixes)
Result: **GREEN** (exit 0).

### `cd apps/frontend && corepack pnpm test:unit` (after both fixes)
Result: **GREEN** — 46 files / 532 tests, unchanged from session 1.

TD-2A is **RESOLVED**. TD-2 overall remains **IN_PROGRESS** — 11 failures remain across 3 further independent, confirmed root causes (TD-2B: 7 tests, PENDING/DRAFT contract mismatch, needs production-code fix + a UX decision on display label; TD-2C: 3 tests, stale UI copy; TD-2D: 1 test, stale auth-deny redirect assumption). None of these three were touched this session — see `status/technical-debt/current-baseline.md` for full detail and `status/index.md` for the queued next task.

---

## Session 3 (2026-07-12, commit `c6de61f`) — TD-5 fix validation

Ran after fixing the 4 stale merchant-count assertions in `SeedProfileStartupIT`/`TestEndpointsEnabledIT` (see `status/technical-debt/current-baseline.md` TD-5 and `status/specs/deterministic-seed-and-test-isolation.md` §3, tasks 3.2/3.3). No frontend files touched; frontend results remain carried over unchanged from session 1.

### `cd apps/backend && ./mvnw -q test-compile`
Result: **GREEN** (exit 0). (Needed before the targeted Failsafe run below — a first attempt without recompiling ran against stale pre-edit `.class` files and still showed the old `expected: 3` failures; this was caught immediately from the failure text, corrected, and is noted here so a future session doesn't hit the same trap.)

### `cd apps/backend && ./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' -Dit.test='SeedProfileStartupIT,TestEndpointsEnabledIT,TestEndpointsDisabledIT,TestEndpointsProdSafetyIT' failsafe:integration-test failsafe:verify`
Result: **GREEN** (exit 0). `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`. Breakdown: `SeedProfileStartupIT` 8/8, `TestEndpointsEnabledIT` 9/9, `TestEndpointsDisabledIT` 4/4, `TestEndpointsProdSafetyIT` 4/4.

### `cd apps/backend && ./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' -Dtest='FixturesTest,DeterministicDatasetTest,TestingModuleTest,TestOperationResponseTest,TestEndpointSecurityChainTest' test`
Result: **GREEN** (exit 0). `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`. Breakdown: `FixturesTest` 25/25, `DeterministicDatasetTest` 3/3, `TestingModuleTest` 4/4, `TestOperationResponseTest` 6/6, `TestEndpointSecurityChainTest` 5/5.

### `cd apps/backend && ./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify`
Result: **GREEN — BUILD SUCCESS** (exit 0). This is the first fully-green repository-approved filtered `verify` run across sessions 1–3.
- Surefire (unit tests): `Tests run: 463, Failures: 0, Errors: 0, Skipped: 5`.
- Failsafe (integration tests): `Tests run: 46, Failures: 0, Errors: 0, Skipped: 0`.

TD-5 is **RESOLVED**. Both TD-1 (session 2) and TD-5 (this session) are now closed; the full filtered backend baseline is green for the first time in this status-tracking system's recorded history.

---

## Session 2 (2026-07-12, commit `c6de61f`) — TD-1 fix validation

Ran after fixing `AuditEventPersistenceTest`'s stale field-list assertion (see `status/roadmaps/audit-export-closure.md`).

### `cd apps/backend && ./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' -Dtest='AuditEventPersistenceTest,AuditDtoRedactionTest,AuditModuleTest,AuditControllerTest,AuditEventListenerModuleTest,JpaAuditEventRepositoryTest,AuditEventTest,AuditQueryTest' test`
Result: **GREEN** (exit 0). `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0`.

### `cd apps/backend && ./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify`
Result: **RED** (exit 1) — but for a different reason than session 1. **Superseded by Session 3 above — the Failsafe failures below are now fixed.** Breakdown:
- **Surefire (unit test) stage: GREEN.** `Tests run: 463, Failures: 0, Errors: 0, Skipped: 5`. This is the stage TD-1 was blocking; it is now fully green (462 session-1 tests + 1 new test added by the TD-1 fix).
- **Failsafe (integration test) stage: RED.** `Tests run: 46, Failures: 4, Errors: 0, Skipped: 0`. This is the **first time Failsafe has run to completion** in either session — every previous filtered `verify` attempt failed at the Surefire stage first (TD-1), which stops the Maven lifecycle before `integration-test`/`verify` phases execute Failsafe at all. The 4 failures are a newly-exposed, unrelated regression — see `status/technical-debt/current-baseline.md` TD-5 (RESOLVED in Session 3):
  ```
  SeedProfileStartupIT.reSeedingProducesSameState:98        expected: 3 but was: 4
  SeedProfileStartupIT.seedRunnerPopulatedMerchantsOnStartup:55  expected: 3 but was: 4
  TestEndpointsEnabledIT.seedAfterResetRestoresFullDeterministicState:135  expected: 3 but was: 4
  TestEndpointsEnabledIT.seedLoads104PaymentOrdersIntoDatabase:101  expected: 3 but was: 4
  ```
  Root cause: `Fixtures.merchants()` seeds 4 merchants (a `MERCHANT_SUSPENDED_DEMO` demo merchant was added by the MVP Phase 1 roadmap's `SEED-MVP-001` task) but these 4 `deterministic-seed-and-test-isolation` Wave-3R integration tests still hardcode an expected count of 3. Not fixed in Session 2 (different root cause/module than TD-1; one-work-package-per-session rule) — fixed in Session 3.

No frontend files were touched this session; frontend results below are carried over unchanged from session 1 (2026-07-12, commit `fec8e1d`) and were not re-run.

---

## Session 1 (2026-07-12, commit `fec8e1d`) — original status-restoration audit

Date: 2026-07-12
Branch: `001-project-foundation`
HEAD: `fec8e1da46da18e3d141660c5bc0753de2ddabf2`
Environment: local (Podman-backed Testcontainers per project convention), Node/pnpm via corepack

This is the one fresh, independently-run validation pass for this audit session. All commands below were run directly against the current worktree with no code changes made before or during the run. Where a result contradicts an older `.codex/**` or `docs/implementation/**` claim, that is called out explicitly — the fresher result here is authoritative for **current** status; the older claim remains valid as a historical record of what was true when it was written.

### Backend

#### `cd apps/backend && ./mvnw compile`
Result: **GREEN** (exit 0). Only pre-existing deprecation warnings (`sun.misc.Unsafe` via Guice, unrelated to project code).

#### `cd apps/backend && ./mvnw test-compile`
Result: **GREEN** (exit 0).

#### `cd apps/backend && ./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify`
Result: **RED** (exit 1) — one test failure, full suite otherwise green. **Superseded by Session 2 above — this specific failure is now fixed.**

```
Tests run: 462, Failures: 1, Errors: 0, Skipped: 5
```

Failing test:
```
AuditEventPersistenceTest.migrationAndJpaMappingPersistOnlyExplicitAuditFields
Expected fields: [id, occurredAt, actorSubject, actorDisplay, action, targetType, targetId, tenantId, correlationId, outcome]
Actual fields:   [id, occurredAt, actorSubject, actorDisplay, action, targetType, targetId, tenantId, correlationId, outcome, beforeState, afterState]
```

Classification: **FAILED_REGRESSION**, tracked as `status/technical-debt/current-baseline.md` TD-1 (now RESOLVED — see Session 2).

Excluded suites (per `AGENTS.md`/root `CLAUDE.md` standing rule, not run and not counted as evidence either way):
- `apps/backend/src/test/java/lab/paymentquality/restkit/`
- `apps/backend/src/test/java/lab/paymentquality/paymentsupport/`

### Frontend

#### `cd apps/frontend && corepack pnpm typecheck`
Result: **GREEN** (exit 0). `nuxt typecheck` clean.

#### `cd apps/frontend && corepack pnpm test:unit`
Result: **GREEN** (exit 0).
```
Test Files  46 passed (46)
     Tests  532 passed (532)
```

#### `cd apps/frontend && corepack pnpm exec playwright test --list`
Result: **GREEN** (exit 0) — listing only, no execution.
```
Total: 101 tests in 31 files
```

#### `cd apps/frontend && corepack pnpm exec playwright test --project=chromium`
Result: **RED** (exit 1).
```
82 tests executed (of 101 listed — the remainder belong to the auth-setup/api-tests projects not selected by --project=chromium)
21 failed
61 passed
```

Full per-file breakdown, root-cause hypotheses confirmed/rejected, and classification: see `status/technical-debt/current-baseline.md` TD-2 (not yet worked this session — carried over unchanged). Summary of failing spec files: `auth-deny.spec.ts` (2), `foundation.spec.ts` (1), `merchant-create.spec.ts` (6), `merchant-feedback.spec.ts` (2 — pre-existing, already known per `docs/implementation/payment-quality-engineering-lab-phase3-roadmap-execution-report.md`), `merchant-lifecycle.spec.ts` (3), `payment-orders-panel.spec.ts` (3), `payment-status-polling.spec.ts` (2), `rbac/merchant-risk-flag.spec.ts` (2). That same report also independently names a likely root cause for a large share of these: `useMerchantsApi.ts`'s Zod status enum (`PENDING`/`ACTIVE`/`SUSPENDED`) does not match the real backend `MerchantStatus` enum (`DRAFT`/`ACTIVE`/`SUSPENDED`) — see report line ~2222 ("Known Baseline Findings"). Not verified or acted on this session; a strong lead for whoever picks up TD-2 next.

### Tests not executed and why

| Suite | Reason |
|---|---|
| `apps/backend/src/test/java/lab/paymentquality/restkit/**` | Excluded per standing repository rule (`AGENTS.md`, root `CLAUDE.md`) unless explicitly requested |
| `apps/backend/src/test/java/lab/paymentquality/paymentsupport/**` | Same standing exclusion rule |
| Playwright `auth-setup` / `api-tests` projects | `--project=chromium` was used per this audit's own validation-command list; the 19-test gap between `--list` (101) and the executed 82 corresponds to tests belonging to these other projects |
| Playwright with real Keycloak (`PLAYWRIGHT_USE_REAL_KEYCLOAK=true`) | Not requested; default mocked-session mode was used, consistent with root `CLAUDE.md`'s documented default |

### Error classification summary (session 1, carried over)

- 1 backend test failure: **FAILED_REGRESSION** (TD-1) — code-level mismatch between an entity's actual persisted fields and a test's explicit exclusion assertion; root cause identified and **fixed in Session 2** (a legitimate, later feature addition that the test was never updated to reflect).
- 21 frontend Playwright failures: mostly **stale locator / stale page copy** in a cluster of older spec files (`foundation.spec.ts`, `merchant-create.spec.ts`, `auth-deny.spec.ts`, `merchant-lifecycle.spec.ts`, `payment-orders-panel.spec.ts`) plus 2 **independently confirmed missing-testid** issues (`payment-status-polling.spec.ts`, `rbac/merchant-risk-flag.spec.ts`) plus 1 **known, previously-accepted regression** (`merchant-feedback.spec.ts`). None trace to a PENDING-vs-DRAFT merchant-status mismatch as a UI text issue (that specific audit-brief hypothesis is not confirmed at the UI-copy level) — though a related but distinct schema-validation-level `DRAFT`-vs-`PENDING` mismatch was found by the Phase 3C-5 report (see above) and may explain some of these. Not fixed this session. Full detail in `status/technical-debt/current-baseline.md`.
- No test failed due to missing infrastructure (Podman/Testcontainers/Keycloak were available) in this run.
- **New in Session 2**: 4 backend Failsafe integration-test failures (TD-5), previously masked by TD-1, now the top of the queue.
