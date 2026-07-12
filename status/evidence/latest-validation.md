---
name: latest-validation
last_updated: 2026-07-12
audited_branch: 001-project-foundation
audited_commit: c6de61f31e7cadc09331269f0f33e70573e4b889
---

# Latest Validation Evidence

## Session 6 (2026-07-12, Codex CLI) — TD-2C completion

`EXECUTION_TOOL: Codex CLI`
`WORK_PACKAGE_ID: TD-2C`
`PARENT_ID: TD-2`
`ORIGIN: REGRESSION_FIX`

Inherited worktree: TD-2B production files (`useMerchantsApi.ts`, `MerchantTable.vue`, `BusinessStatusBadge.vue`, merchant index), tests/fixtures, status records, and the Draft visual snapshot were preserved. No unknown file was found.

Classification: all three failures were `STALE_TEST_EXPECTATION`; loading/error also exposed an `ACCESSIBILITY_DEFECT` corrected minimally in the matching shared state components. No mock defect, backend contract defect, or payment/merchant production-state defect was found.

### `cd apps/frontend && corepack pnpm typecheck`
Result: **GREEN** (exit 0).

### `cd apps/frontend && corepack pnpm test:unit`
Result: **GREEN** — 46 files / 546 tests. `state-and-lifecycle.test.ts` verifies the new loading status and error alert semantics in both test environments.

### `cd apps/frontend && corepack pnpm exec playwright test --list`
Result: **GREEN** — 101 tests in 31 files.

### Exact TD-2C / affected specs
`corepack pnpm exec playwright test tests/e2e/merchant-feedback.spec.ts tests/e2e/payment-orders-panel.spec.ts --project=chromium`

Result: **6/6 passed** including the three TD-2C tests. Loading uses a controlled promise release, error uses two initial 503 responses followed by successful retry, and empty payment data remains a 200 success response.

### Full Chromium after TD-2C
`corepack pnpm exec playwright test --project=chromium`

Result: **76 passed / 6 failed** of 82 Chromium tests. TD-2C removed exactly 3 failures from the prior 72/10 baseline and introduced none. Remaining failures: `merchant-create.spec.ts` four stale `Create` accessible-name assertions (TD-2E), `auth-deny.spec.ts:4` (TD-2D), and `merchant-lifecycle.spec.ts:39` (TD-2F). `payment-status-polling.spec.ts:52` passed in this run but remains historically contention-sensitive.

`BACKEND_VALIDATION: NOT_APPLICABLE — no backend code or contract changed.`

TD-2C is **DONE_VERIFIED**. Next executable task: **TD-2E — align Create merchant action assertions with current accessible names**.

---

## Session 5 (2026-07-12, Codex CLI) — TD-2B completion after interrupted Claude Code CLI handoff

`EXECUTION_TOOL: Codex CLI`
`HANDOFF_FROM: interrupted Claude Code CLI session`
`WORK_PACKAGE_ID: TD-2B`
`PARENT_ID: TD-2`
`ORIGIN: REGRESSION_FIX`

Claude partial work changed the merchant status literals and added tests, but had not updated `status/**`; the schema test duplicated the enum rather than testing the production boundary, and the visual snapshot replacement was incomplete. Codex retained the valid status changes, exported and tested the actual Zod schemas, derived the table type from `MerchantResponse`, regenerated the Draft snapshot, and removed unrelated TD-2F commentary. Backend inspection confirmed `DRAFT` domain and wire values; no backend file changed.

### `cd apps/frontend && corepack pnpm typecheck`
Result: **GREEN** (exit 0).

### `cd apps/frontend && corepack pnpm exec vitest run tests/unit/merchant-list-contract.test.ts app/components/shared/BusinessStatusBadge.test.ts`
Result: **GREEN** — 4 test-file/environment executions, 40/40 tests. Covers all three canonical statuses, rejects `PENDING` and `ARCHIVED`, parses a DRAFT list response, rejects a PENDING list response, and renders `DRAFT -> Draft`.

### `cd apps/frontend && corepack pnpm test:unit`
Result: **GREEN** — 46 files / 546 tests. Existing icon-load warnings did not fail tests.

### `cd apps/frontend && corepack pnpm exec playwright test --list`
Result: **GREEN** — 101 tests in 31 files.

### Targeted Chromium after partial handoff
`corepack pnpm exec playwright test tests/e2e/merchant-create.spec.ts tests/e2e/merchant-lifecycle.spec.ts tests/e2e/rbac/role-visibility.spec.ts tests/e2e/visual-regression.spec.ts --project=chromium`

Result: **16 passed / 5 failed** (21 test executions including auth setup). All TD-2B checks passed: DRAFT activation, DRAFT action visibility/gating, RBAC detail-page mocks, and Draft/Active/Suspended visual assertions. Four failures use the stale exact accessible name `Create`; one expects unsupported `SUSPENDED -> ACTIVE`. Both are outside TD-2B.

### Full Chromium after Codex completion
`corepack pnpm exec playwright test --project=chromium`

Result: **72 passed / 10 failed** (82 Chromium tests; auth setup completed). Historical pre-TD-2B baseline was 70 passed / 12 failed. TD-2B removed 2 failures and introduced none. Remaining failures: four `merchant-create.spec.ts` stale `Create` accessible-name assertions, two `merchant-feedback.spec.ts` stale UI-state assertions, `payment-orders-panel.spec.ts:41` stale empty-copy assertion, `auth-deny.spec.ts:4` stale redirect assertion, `merchant-lifecycle.spec.ts:39` unsupported re-activation expectation (TD-2F), and `payment-status-polling.spec.ts:52` contention-sensitive flake.

`BACKEND_VALIDATION: NOT_APPLICABLE — backend contract inspected, no backend changes.`

TD-2B is **DONE_VERIFIED**. TD-2 remains **IN_PROGRESS**. Next executable task: **TD-2C — align stale merchant loading/error and payment-empty-state UI-copy expectations with the current UI**.

---

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

## Session 7 (2026-07-12, Codex CLI) — TD-2E accessible-name alignment (PARTIAL)

```text
EXECUTION_TOOL: Codex CLI
WORK_PACKAGE_ID: TD-2E
PARENT_ID: TD-2
ORIGIN: REGRESSION_FIX
BRANCH: 001-project-foundation
HEAD_BEFORE: de6deca0a85e1336e3de85136fdd13cf0915a447
HEAD_AFTER: de6deca0a85e1336e3de85136fdd13cf0915a447
```

### Inherited working tree

- TD-2B production/tests preserved: merchant `DRAFT` status contract, table/badge/schema/API updates, lifecycle and role-visibility test edits.
- TD-2C production/tests preserved: `LoadingState.vue`, `ErrorState.vue`, state/lifecycle unit coverage, `merchant-feedback.spec.ts`, and `payment-orders-panel.spec.ts`.
- Visual snapshot preserved: pending snapshot deletion plus untracked `merchant-badge-draft-chromium-linux.png`; no snapshot regenerated by TD-2E.
- TD-2E changed only `apps/frontend/tests/e2e/merchant-create.spec.ts` (six exact submit-action locator expectations) plus the four status records.

### Accessible-name investigation and reproduction

The current submit control in `CreateMerchantForm.vue` is role `button`, visible text `Create`, enabled-state `aria-label="Create merchant"`, no `aria-labelledby`, and no icon name. Its accessible name is intentionally `Create merchant`; the modal/form heading is also `Create merchant`. Git history shows this label was introduced with the RBAC-aware disabled-state label in commit `421cd26`.

Pre-edit complete spec reproduction: 6 Chromium tests executed, 2 passed / 4 failed. The four exact titles were:

1. `creates a merchant from the empty registry`
2. `shows create validation and duplicate feedback`
3. `validation gating — empty form blocks submission and shows field messages`
4. `validation gating — too-short reference is rejected with field message`

Each first causal error was the exact semantic locator `getByRole('button', { name: 'Create', exact: true })`; its accessibility snapshot showed `button "Create merchant": Create`, enabled and not loading. The root cause is `STALE_TEST_EXPECTATION`, not a product/accessibility/shared-component/locator defect. No browser console or request failure occurred before those clicks; traces were not generated because Playwright retries are disabled, while failure DOM snapshots, screenshots, and videos were inspected.

### Implementation

- Product naming decision: retain `Create merchant`; it is clearer than generic `Create` and already derives from the form's meaningful `aria-label`.
- Playwright: changed six submit-action calls in `tests/e2e/merchant-create.spec.ts` to `getByRole('button', { name: 'Create merchant', exact: true })`.
- Production/helpers/unit tests/snapshots: none changed.
- No CSS locator, `data-testid`, timeout, retry, `waitForTimeout`, skip, or weakened business assertion was introduced.

### Validation

| Command | Result |
|---|---|
| `corepack pnpm typecheck` | GREEN |
| `corepack pnpm test:unit` | GREEN — 46 files / 546 tests (known Nuxt Icon load warnings only) |
| `corepack pnpm exec playwright test --list` | GREEN — 101 tests in 31 files |
| Exact TD-2E grep run | 3 passed / 1 failed: the duplicate-feedback test progressed beyond the corrected locator and failed only at its separate duplicate-error assertion |
| Complete `merchant-create.spec.ts` Chromium | 5 passed / 1 failed (6 Chromium tests); same duplicate-feedback assertion only |
| Full `playwright test --project=chromium` | RED — 82 executed: 78 passed / 4 failed; 19 tests assigned to other projects not executed |
| Backend validation | NOT_APPLICABLE — no backend/API-contract file changed |
| Visual validation | NOT_APPLICABLE — only semantic test expectations changed; no snapshot regenerated |

### Full Chromium comparison and classification

```text
FULL_CHROMIUM_BEFORE: 76 passed / 6 failed / 82 executed (TD-2C session)
FULL_CHROMIUM_AFTER: 78 passed / 4 failed / 82 executed
EXPECTED_REDUCTION: 4 stale accessible-name test failures
ACTUAL_REDUCTION: 2 failing test titles; three accessible-name failures removed, while one title remained red because a different root cause was unmasked
FAILURES_REMOVED:
- creates a merchant from the empty registry
- validation gating — empty form blocks submission and shows field messages
- validation gating — too-short reference is rejected with field message
FAILURES_REMAINING:
- TD-2E-1: merchant-create.spec.ts — shows create validation and duplicate feedback; legacy 409 mock body is not Problem Details
- TD-2D: auth-deny.spec.ts — expects Keycloak login instead of current /login redirect contract
- TD-2F: merchant-lifecycle.spec.ts — unsupported SUSPENDED -> ACTIVE expectation
FLAKY_REMAINING:
- payment-status-polling.spec.ts:52 — manual refresh updates status through a repeated GET response; failed under the 16-worker full baseline, historically passes in isolation
NEW_FAILURES: none; TD-2E-1 was previously masked by the stale locator in an already failing test title
STATUS: TD-2E = PARTIAL
NEXT_EXECUTABLE_TASK: TD-2E-1 — align the duplicate merchant route fixture with the current Problem Details client contract
```

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
