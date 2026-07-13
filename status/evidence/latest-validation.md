---
name: latest-validation
last_updated: 2026-07-13
audited_branch: 001-project-foundation
audited_commit: 95e35c97d74608bdc3d7925a4f6bb0b46c99a79b
---

# Latest Validation Evidence

## Assurance Closure Wave 1 — validation-02 closure, 2026-07-13

```text
ASSURANCE_CLOSURE_WAVE_1: DONE_VERIFIED
PACKAGE_SEQUENCE: final closure
WORK_PACKAGE_ID: ASSURANCE-CLOSURE-W1-VALIDATION-02
SOURCE_PLAN: Assurance Closure Wave 1
ROOT_CAUSE: generic alert locator matched loading skeleton alerts in focused multi-role validation
DECISION: filter the alert locator by the user-visible permission-denial text; no production contract, role, data count, timeout, retry, or cleanup behavior changed
FILES_CHANGED: apps/frontend/tests/live/auth/multi-role.spec.ts; current status registry
TARGETED_VALIDATION: multi-role 3/3; two-worker isolation 3/3; idempotency 2/2; conditional GET/304 2/2
BROADER_VALIDATION: typecheck GREEN; unit 48 files / 554 tests; complete live 7/7; Chromium 82/82
STATUS: PW-AUTH-01/F-A2, PW-DATA-01/F-A4, PW-IDEM-01, PW-304-01 = DONE_VERIFIED; PW-HEAD-01 = SUPERSEDED_BY_VERIFIED_SOLUTION
REMAINING_GAPS: none in Assurance Closure Wave 1
NEXT_PACKAGE: QA-HARDEN-01
```

### Complete live run — exactly one authorised execution

```bash
cd apps/frontend
corepack pnpm exec playwright test --config playwright.live.config.ts
```

Environment-only credentials were supplied for `platform.admin` and `merchant.manager`; `PLAYWRIGHT_LIVE_RUN_ID=W1VAL02` was used.

- Projects: `live-auth-platform-admin`, `live-auth-merchant-manager`, `live-multi-role`, `live-http-merchant-manager`, `live-parallel-merchant-manager`.
- Discovered/executed/passed: 7/7/7; failed: 0; flaky: 0; skipped: 0; duration: 18.4 s.
- Real-role proof: `platform.admin` / `PLATFORM_ADMIN` / `PLATFORM_TENANT`; `merchant.manager` / `MERCHANT_MANAGER` / `TENANT_ALPHA` / `MERCHANT_ALPHA_001`; same Alpha route allowed for both, platform-only controls absent for manager, and manager sees no foreign-Beta payment table.
- Worker proof: two parallel workers allocate distinct owner-tagged data; no collision or global reset.
- Idempotency proof: BFF POST initial `201` with `Idempotency-Replayed: false`; identical replay `200` with `Idempotency-Replayed: true`; direct BFF GET and filtered list prove exactly one persisted resource.
- Conditional GET proof: BFF `200` returns `ETag`; forwarded `If-None-Match` receives `304` with empty body and `ETag`, `Cache-Control`, `Vary: Authorization` preserved.

### Final standard frontend regression

```bash
cd apps/frontend
corepack pnpm exec playwright test --project=chromium
```

Result: 82 discovered/executed/passed; 0 failed; 0 flaky; duration 55.8 s.

### Backend and standalone API evidence

No `apps/backend/**`, `apps/api-tests/**`, or BFF contract source changed in this validation session. The current evidence is therefore inherited fresh evidence, not a rerun: filtered backend verify = Surefire 465 total / 460 passed / 5 skipped and Failsafe 46/46; `apps/api-tests` = Surefire 79/79 and Failsafe 72/72.

### Plan-completeness reconciliation

- Required total: 296.
- Implemented required: 296 / 296 = 100.00%.
- Verified required: 285 / 296 = 96.28%.
- The verified count increases from 281 by the four now-closed canonical live-assurance items: `F-A2`, `F-A4`, `PW-IDEM-01`, and `PW-304-01`.

---

## Assurance Closure Wave 1 — final validation, 2026-07-13

```text
ASSURANCE_CLOSURE_WAVE_1:
PACKAGE_SEQUENCE: final validation
WORK_PACKAGE_ID: ASSURANCE-CLOSURE-W1-VALIDATION-01
SOURCE_PLAN: Assurance Closure Wave 1
ROOT_CAUSE: a fixed Alpha-tenant count assertion raced a valid parallel idempotency creation
DECISION: correct the assertion; retain all live-package statuses as IMPLEMENTED_UNVERIFIED because the one authorised complete live run was not fully green
FILES_CHANGED: live multi-role spec; PaymentOrderNotesControllerTest mock; current status registry
TARGETED_VALIDATION: auth setup 2/2; multi-role correction project 3/3; two-worker isolation 3/3; idempotency 2/2; conditional GET 2/2; PaymentOrderNotesControllerTest 6/6
BROADER_VALIDATION: frontend typecheck GREEN; frontend unit 48 files / 554 tests; live discovery 7 tests / 6 files; complete live run 6 passed / 1 failed; filtered backend verify exit 0
STATUS: IMPLEMENTED_UNVERIFIED
REMAINING_GAPS: one newly authorised complete live run of corrected source
NEXT_PACKAGE: ASSURANCE-CLOSURE-W1-VALIDATION-02
```

### Complete live run — exactly one authorised execution

Command:

```bash
cd apps/frontend
corepack pnpm exec playwright test --config playwright.live.config.ts
```

With environment-only `PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD`, `PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD`, and `PLAYWRIGHT_LIVE_RUN_ID=W1VAL01`.

- Projects: `live-auth-platform-admin`, `live-auth-merchant-manager`, `live-multi-role`, `live-http-merchant-manager`, `live-parallel-merchant-manager`.
- Discovered/executed: 7/7; passed: 6; failed: 1; flaky: 0; skipped: 0; duration: 22.4 s.
- Passed: both real-Keycloak setup tests; both two-worker tests; `BFF preserves payment creation and idempotency replay semantics through persistence`; `BFF preserves backend conditional GET 304 with an empty body`.
- Failed: `real roles see their own payment route while merchant manager is denied foreign-tenant data` at a literal `101 order(s)` expectation. The live idempotency project concurrently and validly created one Alpha order, so the page did not contain the stale fixed total.
- Correction: `tests/live/auth/multi-role.spec.ts` now asserts a non-empty `N order(s) across` tenant-visible result; it preserves table, RBAC navigation, token-storage, and foreign-tenant denial checks. Exact rerun with both auth dependencies: 3/3 passed. No second complete run was executed.

### Focused and broader validation

- Backend: `./mvnw compile` GREEN; `./mvnw test-compile` GREEN; focused eligibility/scope/timestamp/security/HTTP classes GREEN; after a `PaymentMerchantScopeVerifier` injection caused `PaymentOrderNotesControllerTest`'s `@WebMvcTest` to lack a bean, adding its `@MockitoBean` made that exact test 6/6 GREEN. Filtered `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` exited 0: Surefire 465 total / 460 passed / 0 failed / 0 errors / 5 skipped; Failsafe 46 total / 46 passed / 0 failed / 0 errors / 0 skipped.
- Frontend: `corepack pnpm typecheck` GREEN; `corepack pnpm test:unit` GREEN — 48 files / 554 tests; live Playwright list GREEN — 7 tests in 6 files, no zero-test required project.
- `PW-HEAD-01`: remains `SUPERSEDED_BY_VERIFIED_SOLUTION`; fresh `apps/api-tests` Failsafe `HttpMethodSemanticsContractSpec` covers backend HEAD 200, empty body, `ETag`, `Vary: Authorization`, `Cache-Control: no-store`, and correlation ID. No distinct BFF HEAD route was found.

---

## Assurance Closure Wave 1 — Packages 2–6 checkpoint (2026-07-13)

```text
ASSURANCE_CLOSURE_WAVE_1: PARTIAL — live closure requires a new run, but the two permitted complete live-assurance runs have been consumed
PACKAGE_SEQUENCE: 2/8 through 6/8
WORK_PACKAGE_ID: PW-AUTH-01, PW-DATA-01, PW-IDEM-01, PW-304-01, PW-HEAD-01
SOURCE_PLAN: docs/architecture/playwright-sdet-feature-roadmap.md; live Nuxt BFF and backend contracts
ROOT_CAUSE: (1) Keycloak PKCE state is host-bound, so live tests must use localhost rather than 127.0.0.1; (2) the real merchant.manager claim is the seeded natural reference MERCHANT_ALPHA_001, not a UUID; (3) exact replay bodies exposed Java Instant nanoseconds versus PostgreSQL microseconds; (4) the current nested BFF payment-order POST route still needs a final runtime proof after route-placement investigation.
DECISION: retain the existing BFF route shape; add a separate live-only Playwright configuration, real env-supplied Keycloak storage states, worker-owned retained data, and exact BFF HTTP contracts. Do not create a UI HEAD flow. PW-HEAD-01 is superseded by the fresh standalone black-box HEAD proof.
FILES_CHANGED: apps/backend/src/main/java/lab/paymentquality/{merchant/MerchantPaymentEligibilityService.java,merchant/internal/application/MerchantPaymentEligibilityAdapter.java,payment/internal/application/PaymentMerchantScopeVerifier.java,payment/internal/domain/PaymentOrder.java,payment/internal/web/PaymentOrderController.java}; apps/backend/src/test/java/lab/paymentquality/{merchant/internal/application/MerchantPaymentEligibilityAdapterTest.java,payment/internal/application/PaymentMerchantScopeVerifierTest.java,payment/internal/domain/PaymentOrderTimestampPrecisionTest.java,security/PaymentOrderSecurityTest.java}; apps/frontend/{playwright.live.config.ts,server/utils/backendApi.ts,server/api/merchants/[merchantId]/payment-orders/index.post.ts,tests/live/**}
TARGETED_VALIDATION: real Keycloak setup projects 2/2 GREEN; worker-owned parallel allocation tests 2/2 GREEN in the first live run; backend focused payment-scope/timestamp suite GREEN; frontend typecheck GREEN; live config discovery GREEN (7 tests).
BROADER_VALIDATION: live assurance run 1 proved the two setup projects, one conditional-GET path, and two parallel allocations; live assurance run 2 exposed one stale first-page multi-role assertion, a temporary route-shadowing experiment, and timestamp precision in idempotency replay. The assertion and precision causes were corrected, and the route experiment was reverted. A third complete run is prohibited by the phase repetition limit, so none of PW-AUTH-01, PW-DATA-01, PW-IDEM-01, or PW-304-01 is closed.
STATUS: PW-AUTH-01 = PARTIAL_UNVERIFIED; F-A2 = PARTIAL_UNVERIFIED; PW-DATA-01 = PARTIAL_UNVERIFIED; F-A4 = PARTIAL_UNVERIFIED; PW-IDEM-01 = PARTIAL_UNVERIFIED; PW-304-01 = PARTIAL_UNVERIFIED; PW-HEAD-01 = SUPERSEDED_BY_VERIFIED_SOLUTION.
REMAINING_GAPS: one controlled live validation run of the current source is required before any DONE_VERIFIED status; the retained-data model deliberately has no DELETE/global-reset proof because this API has no safe worker-owned deletion endpoint.
NEXT_PACKAGE: DOC-STATUS-01 partial reconciliation, then ASSURANCE-CLOSURE-W1-VALIDATION-01 when a new live-run allowance is explicitly granted.
```

`PW-HEAD-01` superseding evidence is `apps/api-tests/.../HttpMethodSemanticsContractSpec.head_existing_payment_order_returns_200_with_etag_and_no_body`, included in the fresh Failsafe 72/72 baseline. It calls the same backend resource endpoint and verifies `200`, `ETag: "v0"`, `Vary: Authorization`, `Cache-Control: no-store`, `X-Correlation-ID`, and an empty body. A Nuxt/APIRequestContext duplicate would add no distinct BFF route: the BFF has no independently supported HEAD endpoint and the requested UI flow would be artificial.

## Assurance Closure Wave 1 — Package 1 (2026-07-13)

```text
ASSURANCE_CLOSURE_WAVE_1: IN_PROGRESS
PACKAGE_SEQUENCE: 1/8
WORK_PACKAGE_ID: VAL-API-01
SOURCE_PLAN: REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md
ROOT_CAUSE: apps/api-tests MerchantResponse omitted the current riskFlagged response field
DECISION: keep a standalone black-box DTO and add only the missing boolean field
FILES_CHANGED: apps/api-tests/src/test/java/lab/paymentquality/apitest/api/merchant/dto/MerchantResponse.java
TARGETED_VALIDATION: MerchantsContractSpec + TenantIsolationContractSpec GREEN
BROADER_VALIDATION: compile GREEN; test-compile GREEN; Surefire 79/79; Failsafe 72/72; BUILD SUCCESS
STATUS: VAL-API-01 = DONE_VERIFIED; VAL-API-01A = DONE_VERIFIED
REMAINING_GAPS: PW-AUTH-01, PW-DATA-01, PW-IDEM-01, PW-304-01, PW-HEAD-01, DOC-STATUS-01
NEXT_PACKAGE: merchant-claim-alignment prerequisite for PW-AUTH-01
```

The first live invocation omitted `BACKEND_IMAGE` and failed before scenario execution; it is classified as `ENVIRONMENT_FAILURE` and is not closure evidence. The corrected invocation used rootless Podman socket `/run/user/1000/podman/podman.sock`, `BACKEND_IMAGE=payment-quality/backend:local`, and a freshly built backend image.

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

---

## Multi-package Session 8 (2026-07-13, HEAD `95e35c9`) — partial implementation / environment block

MULTI_PACKAGE_SESSION: session 8
EXECUTION_TOOL: Codex CLI
ENVIRONMENT: sandbox denies network and Unix-domain socket connections

### Package 1 — TD-2E-1 / TD-2E closure

ROOT_CAUSE: duplicate merchant responses used legacy `{ error, message }` JSON in both the E2E mock and backend handler; the client renders duplicate feedback from `ProblemDetails.detail`.
CLASSIFICATION: BACKEND_ERROR_CONTRACT_DEFECT plus stale mock contract.
DECISION: emit and preserve `409 application/problem+json` with `type`, `title`, `status`, `detail`, and `error`; preserve strict client parsing.
FILES_CHANGED: `MerchantExceptionHandler.java`, `MerchantRestAssuredTest.java`, `server/utils/backendApi.ts`, `merchant-support.ts`, `useApiClient.property.test.ts`.
TARGETED_TESTS: frontend `useApiClient` focused test passed within the full unit suite.
BROADER_VALIDATION: `corepack pnpm typecheck` GREEN; `corepack pnpm test:unit` GREEN — 46 files, 550 tests. Backend `./mvnw compile` and `./mvnw test-compile` GREEN.
FULL_BASELINE: BLOCKED_ENVIRONMENT — Playwright Nuxt webServer cannot bind localhost; backend `MerchantRestAssuredTest` cannot connect to Podman/Docker through Testcontainers.
STATUS: IMPLEMENTED_UNVERIFIED.
REMAINING_GAPS: exact duplicate E2E, full merchant-create spec, Chromium baseline, and REST integration assertion require socket-enabled environment.
NEXT_PACKAGE: TD-2F.

### Package 2 — TD-2F

ROOT_CAUSE: frontend action gating contradicted the proven domain state machine by offering `SUSPENDED → ACTIVE`.
CLASSIFICATION: FRONTEND_ACTION_GATING_DEFECT and stale test expectation.
DECISION: retain terminal `SUSPENDED`; expose activation only for `DRAFT`; assert no activation control in the E2E test.
FILES_CHANGED: `MerchantTable.vue`, `pages/admin/merchants/[merchantId]/index.vue`, `merchant-lifecycle.spec.ts`.
TARGETED_TESTS: `MerchantStatusTest` and `MerchantTest` GREEN — 17 tests.
BROADER_VALIDATION: frontend typecheck/unit GREEN — 46 files, 550 tests.
FULL_BASELINE: BLOCKED_ENVIRONMENT — Chromium cannot start.
STATUS: IMPLEMENTED_UNVERIFIED.
REMAINING_GAPS: targeted lifecycle E2E and full Chromium.
NEXT_PACKAGE: TD-2D.

### Package 3 — TD-2D

ROOT_CAUSE: stale test expected a mocked Keycloak page, although global middleware redirects unauthenticated users to the application-owned login route.
CLASSIFICATION: STALE_TEST_EXPECTATION.
DECISION: assert `/login?redirectTo=%2Fadmin%2Fmerchants`, visible login control, and absence of merchant data.
FILES_CHANGED: `auth-deny.spec.ts`.
TARGETED_TESTS: frontend typecheck/unit GREEN — 46 files, 550 tests.
FULL_BASELINE: BLOCKED_ENVIRONMENT — Chromium cannot start.
STATUS: IMPLEMENTED_UNVERIFIED.
REMAINING_GAPS: targeted auth-deny E2E and full Chromium.
NEXT_PACKAGE: TD-2G.

### Package 4 — TD-2G

REPRODUCTION_COUNT: 0.
CLASSIFICATION: BLOCKED_ENVIRONMENT.
ROOT_CAUSE: not evaluated; Nuxt/Playwright cannot open localhost sockets in this sandbox.
FILES_CHANGED: none.
REPEATED_TARGETED_RESULT: not run.
FULL_CHROMIUM: not run.
RESIDUAL_RISK: historical contention-sensitive `payment-status-polling.spec.ts:52` remains unresolved.
STATUS: BLOCKED_ENVIRONMENT.
NEXT_PACKAGE: jqwik tries.

### Package 6 — jqwik tries 30 → 100

TEST: `KeycloakRealmRoleConverterTest`.
TRIES_BEFORE: 30 on four properties.
TRIES_AFTER: 100 on all four properties.
RATIONALE: the frozen backend-authority-refactor requirements specify at least 100 iterations; the pure deterministic converter has no external dependencies.
FILES_CHANGED: `KeycloakRealmRoleConverterTest.java`.
FOCUSED_RUN_1: GREEN — 18 tests; all four jqwik properties report 100 checks.
FOCUSED_RUN_2: GREEN — 18 tests; all four jqwik properties report 100 checks.
COMPILE: GREEN.
TEST_COMPILE: GREEN.
STATUS: DONE_VERIFIED.
NEXT_PACKAGE: TD-2 browser validation when socket-enabled environment is available.

### Environment failures

- `corepack pnpm exec playwright test ...`: Nuxt webServer exits before execution because the sandbox prevents localhost socket binding.
- `./mvnw -Dtest='MerchantRestAssuredTest' test`: Testcontainers cannot connect to configured Podman/Docker Unix sockets; test count is 1 error before any test method executes.
- These are environment failures, not assertions. No filtered backend `verify`, Chromium run, TD-2 closure, or final repository validation is represented as green.

### Final validation attempt

- Frontend `corepack pnpm exec playwright test --project=chromium`: **BLOCKED_ENVIRONMENT** before discovery/execution — `webServer` exited 1 because the sandbox disallows localhost socket binding.
- Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify`: **BLOCKED_ENVIRONMENT / unreliable baseline** — Surefire reached 346 tests, then reported 171 errors after Testcontainers could not connect to Podman/Docker Unix sockets. Subsequent Mockito `MockMaker` initialization failures occurred in the same invalid environment; no assertion failure was reported (`Failures: 0`). Failsafe did not run.
- Repository safety: `.kiro/**` and `.codex/**` remain unchanged; `git diff --check` is clean. `apps/backend/.jqwik-database` was modified by jqwik during validation and could not be restored because the sandbox mounts `.git` read-only, preventing Git from creating `index.lock`. It is a generated validation artifact, not an intentional package change.

---

## Continuation validation session (2026-07-13, HEAD `95e35c9`)

CONTINUATION_VALIDATION_SESSION: completed
EXECUTION_TOOL: Codex CLI
ENVIRONMENT:
- localhost: GREEN — `LOCALHOST_BIND_OK 127.0.0.1:<ephemeral-port>`
- Podman: GREEN — `podman info` and Testcontainers selected `unix:///run/user/1000/podman/podman.sock`
- Docker: NOT_APPLICABLE — Podman is the selected available runtime
- Testcontainers: GREEN

GENERATED_ARTIFACT_CLEANUP: `apps/backend/.jqwik-database` was confirmed as a tracked, test-generated binary mutation and restored only at that path. Final state: no diff.

### TD-2E-1 / TD-2E

ROOT_CAUSE: legacy duplicate body did not meet the strict Problem Details client contract.
PROBLEM_DETAILS_CONTRACT: `409`; `application/problem+json`; `type=https://api.payment-quality.local/problems/duplicate-merchant-reference`; `title=Merchant already exists`; `status=409`; `detail=A merchant with this reference already exists`; extension `error=duplicate_merchant_reference`; conditional `correlationId`; no `instance` field for this response.
TARGETED_TESTS: frontend typecheck GREEN; frontend unit suite GREEN — 46 files / 550 tests; exact `shows create validation and duplicate feedback` GREEN; `merchant-create.spec.ts` GREEN — 6 Chromium tests; `MerchantRestAssuredTest` GREEN — 5/5.
FULL_BASELINE: Chromium closure run 1 GREEN — 82/82; run 2 GREEN — 82/82.
STATUS: TD-2E-1 = DONE_VERIFIED; TD-2E = DONE_VERIFIED.

### TD-2F

TRANSITION_GRAPH: `DRAFT -> ACTIVE` allowed; `ACTIVE -> SUSPENDED` allowed; `SUSPENDED -> ACTIVE` unsupported; `SUSPENDED` terminal.
TARGETED_TESTS: `MerchantStatusTest,MerchantTest` GREEN — 17/17; frontend typecheck/unit GREEN — 46 files / 550 tests; `merchant-lifecycle.spec.ts` GREEN — 6 Chromium tests.
FULL_BASELINE: Chromium closure run 1 GREEN — 82/82; run 2 GREEN — 82/82.
STATUS: DONE_VERIFIED.

### TD-2D

ROOT_CAUSE: browser URL normalization makes the raw encoded-slash assertion stale; semantic route contract is preserved.
TARGETED_TEST: `auth-deny.spec.ts` GREEN — 2 Chromium tests plus auth setup; pathname `/login`, `redirectTo=/admin/merchants`, visible login control, absent merchant registry.
FULL_BASELINE: Chromium closure run 1 GREEN — 82/82; run 2 GREEN — 82/82.
STATUS: DONE_VERIFIED.

### TD-2G

REPRODUCTION_COUNT: 10 repetitions per polling scenario.
INITIAL_RESULT: 14 passed / 6 failed; all six failures were `manual refresh updates status through a repeated GET response` at the ETag assertion.
ROOT_CAUSE: `response.url().includes(detailPath)` matched supporting `/history` and `/evidence` responses without ETag rather than the repeated payment-detail GET.
DECISION: require GET and exact URL pathname; no retry, sleep, timeout, or worker adjustment.
REPEATED_TARGETED_RESULT: GREEN — 20/20 polling cases (21/21 including auth setup).
FULL_BASELINE: two closure Chromium runs GREEN — 82/82 each.
STATUS: DONE_VERIFIED; residual risk: none observed after controlled repetition and full baselines.

### TD-2 closure and final frontend

PLAYWRIGHT_DISCOVERY: 101 tests in 31 files; Chromium project executes 82 tests.
CHROMIUM_RUN_1: GREEN — 82 passed / 0 failed.
CHROMIUM_RUN_2: GREEN — 82 passed / 0 failed.
FINAL_FRONTEND: typecheck GREEN; unit suite GREEN — 46 files / 550 tests; final Chromium GREEN — 82 passed / 0 failed; flaky = 0.
TD-2: DONE_VERIFIED.

### jqwik and final backend

JQWIK: `KeycloakRealmRoleConverterTest` focused run GREEN — 18/18; four properties each reported `tries = 100`, `checks = 100`.
BACKEND_COMPILE: GREEN.
BACKEND_TEST_COMPILE: GREEN.
FILTERED_VERIFY: BUILD SUCCESS.
SUREFIRE: total 445; passed 440; failed 0; errors 0; skipped 5. Counts exclude unchanged, old report files for the explicitly excluded `restkit/**` and `paymentsupport/**` suites.
FAILSAFE: total 46; passed 46; failed 0; errors 0; skipped 0; flakes 0.
REMAINING_GAPS: none in this continuation scope.
NEXT_EXECUTABLE_TASK: NO_EXECUTABLE_TASK_IN_THIS_CONTINUATION_SCOPE.

---

## TD-3 documentation and status closure (2026-07-13, Codex CLI)

WORK_PACKAGE_ID: TD-3
PARENT_ID: none
ORIGIN: DOCUMENTATION_AND_STATUS_CLEANUP
ROOT_CAUSE: six current `docs/specs-analysis/**/README.md` status headers and the summary table contradicted the canonical `status/specs/*.md` ledgers; four ledgers also still described those headers as stale after correction.
FILES_INSPECTED: `status/index.md`, `status/technical-debt/current-baseline.md`, `status/evidence/latest-validation.md`, `status/roadmaps/playwright-phase3-roadmap.md`, all `status/specs/*.md`, `docs/specs-analysis/*.md`, `README.md`, `CLAUDE.md`, and `AGENTS.md`.
STALE_CURRENT_ENTRIES: `docs/specs-analysis/{02-iam-roles-and-keycloak-login,03-payment-operations-dashboard,04-tenant-model-and-isolation,05-user-management,06-audit-log-dashboard,07-deterministic-seed-and-test-isolation}/README.md`; `docs/specs-analysis/README.md`; stale-header notes in the IAM, tenant, user-management, and deterministic status ledgers.
VALID_HISTORICAL_ENTRIES: dated Sessions 4--7 in `status/index.md`, `status/evidence/latest-validation.md`, and `status/roadmaps/playwright-phase3-roadmap.md`; dated `docs/specs-analysis/COMMIT-REVIEW-e18ebb1.md`; all retain their historical TD-2 states and test counts with a session or commit context.
FILES_CHANGED: the six README files above; `docs/specs-analysis/README.md`; `status/specs/{iam-roles-and-keycloak-login,tenant-model-and-isolation,user-management,deterministic-seed-and-test-isolation,backend-authority-refactor}.md`; `status/technical-debt/current-baseline.md`.
REFERENCE_VALIDATION: all seven referenced `status/specs/*.md` ledger paths exist; no repository Markdown/link/status validator is defined in the inspected package, Maven, task-runner, or scripts configuration.
DOCUMENTATION_VALIDATION: `git diff --check` GREEN. The current source of truth records TD-2 `DONE_VERIFIED`, frontend 46 files / 550 units and 82/82 Chromium, backend filtered verify `BUILD SUCCESS` (Surefire 445 total / 440 passed / 5 skipped; Failsafe 46/46), and four jqwik properties at 100 tries as the **last verified baseline**, not a run of this package.
STATUS: DONE_VERIFIED.
REMAINING_GAPS: none for TD-3.
NEXT_PACKAGE: TD-4.

---

## TD-4 Kiro identifier-reference closure (2026-07-13, Codex CLI)

WORK_PACKAGE_ID: TD-4
PARENT_ID: none
ORIGIN: DOCUMENTATION_AND_DATA_HYGIENE
ROOT_CAUSE: four immutable `.kiro/specs/*/.config.kiro` files reuse `specId` `ddbff980-460a-4eec-ae6b-f004d743fac8` for different specs, while current documentation did not define a canonical disambiguation rule.
DUPLICATE_IDENTIFIERS: `ddbff980-460a-4eec-ae6b-f004d743fac8` in `.kiro/specs/{audit-log-dashboard,deterministic-seed-and-test-isolation,iam-roles-and-keycloak-login,user-management}/.config.kiro`.
CANONICAL_IDENTIFIERS: `audit-log-dashboard`, `deterministic-seed-and-test-isolation`, `iam-roles-and-keycloak-login`, and `user-management`, each defined by its unique slug and `.kiro/specs/{slug}/` path. The remaining unique raw metadata values are inventoried in `status/index.md`.
HISTORICAL_COLLISIONS: the shared UUID is a `HISTORICAL_COLLISION` with no canonical owner; it is not an alias and cannot identify any one of the four specs.
CURRENT_COLLISIONS: none. `status/index.md` is the canonical current registry and prohibits the shared UUID in new status or implementation references.
FILES_CHANGED: `status/index.md`, `status/technical-debt/current-baseline.md`, `status/specs/user-management.md`, `status/evidence/latest-validation.md`.
CODE_OR_CONFIG_CHANGED: no.
VALIDATION: complete identifier inventory over `.kiro`, `.codex`, `status`, `docs`, `apps`, `README.md`, `CLAUDE.md`, and `AGENTS.md`; all seven canonical slug/path owners are explicit; `git diff --check` and `git diff -- .kiro .codex` are required final controls.
DEVIATIONS: immutable `.kiro/**` retains the four-way raw metadata collision. No Kiro file, production code, test fixture, configuration, or public contract was changed.
STATUS: DONE_WITH_DEVIATION.
REMAINING_GAPS: no current identifier ambiguity; the historical immutable collision is documented.
NEXT_PACKAGE: NONE — current tracked execution queue is complete.

---

## Assurance Closure Wave 2A — QA-HARDEN-01A (2026-07-13, Codex CLI)

ASSURANCE_CLOSURE_WAVE_2A: in progress
PACKAGE: QA-HARDEN-01A — 304 and no-body presentation
PLAN_ITEM_IDS: QA-HARDEN-01.01, QA-HARDEN-01.02, QA-HARDEN-01.03
ROOT_CAUSE: the three implemented empty-body improvements had no focused acceptance-level proof; the initial route-test seam incorrectly attempted to mock Nitro-only imports through `mockNuxtImport` and was corrected as `TEST_DESIGN_DEFECT` without changing production.
ACCEPTANCE_CRITERIA: Error Lab returns the exact 304 status, forwards ETag/Last-Modified/Cache-Control/Vary/X-Correlation-ID and returns no body; RawJsonViewer renders `No body` without `pre` for blank input; ApiDebugPanel retains its response-body section for absent/empty body.
TEST_LEVEL: Nuxt server-route test plus Vue component tests.
FILES_CHANGED: `apps/frontend/tests/unit/qa-harden-empty-body.test.ts`; this checkpoint.
FOCUSED_VALIDATION: `vitest run tests/unit/qa-harden-empty-body.test.ts` GREEN — 6 tests per Vitest project, 12/12 total; `raw-json-viewer-round-trip.property.test.ts` GREEN — 12 tests per project, 24/24 total.
BROADER_VALIDATION: frontend `corepack pnpm typecheck` GREEN.
STATUS: DONE_VERIFIED — all three implementations were correct and previously unverified; no production change.
REMAINING_GAPS: QA-HARDEN-01.04 through QA-HARDEN-01.11; SEED-PROP-01.
NEXT_PACKAGE: QA-HARDEN-01B.

---

## Assurance Closure Wave 2A — QA-HARDEN-01B (2026-07-13, Codex CLI)

ASSURANCE_CLOSURE_WAVE_2A: in progress
PACKAGE: QA-HARDEN-01B — Problem presentation and educational descriptions
PLAN_ITEM_IDS: QA-HARDEN-01.04, QA-HARDEN-01.06
ROOT_CAUSE: the focused mixed-field test confirmed that the `Field Errors` label alone lacked `w-28`, contradicting the hardening report's all-label alignment claim; Error Lab descriptions were correct and previously unverified.
ACCEPTANCE_CRITERIA: every standard/extension Problem Details `dt` uses `w-28` and none uses `w-20`; the 401/428/429/304/idempotency-replay descriptions expose their exact learning points.
TEST_LEVEL: Vue component tests.
FILES_CHANGED: `apps/frontend/tests/unit/qa-harden-error-presentation.test.ts`; one class-only correction in `apps/frontend/app/components/shared/ProblemDetailsCard.vue`; this checkpoint.
FOCUSED_VALIDATION: initial focused run — Error Lab GREEN, alignment RED only for `Field Errors`; post-fix focused suite GREEN. Combined focused/existing run GREEN — 4 Vitest files / 32 tests across both projects, including 28 existing ProblemDetails property checks.
BROADER_VALIDATION: frontend `corepack pnpm typecheck` GREEN.
STATUS: DONE_VERIFIED — one `CONFIRMED_PRODUCT_DEFECT` fixed minimally; one implementation correct and previously unverified.
REMAINING_GAPS: QA-HARDEN-01.05, QA-HARDEN-01.07 through QA-HARDEN-01.11; SEED-PROP-01.
NEXT_PACKAGE: QA-HARDEN-01C.

---

## Assurance Closure Wave 2A — QA-HARDEN-01C (2026-07-13, Codex CLI)

ASSURANCE_CLOSURE_WAVE_2A: in progress
PACKAGE: QA-HARDEN-01C — Support Search gating and result contract
PLAN_ITEM_IDS: QA-HARDEN-01.05, QA-HARDEN-01.09
ROOT_CAUSE: both implemented Support Search improvements lacked a focused page-level proof.
ACCEPTANCE_CRITERIA: blank Merchant ID keeps Search disabled and prevents a request even with a client reference; valid inputs are trimmed into the exact merchant-scoped request; results expose text status, formatted date and uniquely named exact detail link; empty and error states remain distinct.
TEST_LEVEL: mounted Nuxt page/component test with real page logic, schema, columns and cell renderers.
FILES_CHANGED: `apps/frontend/app/pages/admin/support/index.test.ts`; this checkpoint.
FOCUSED_VALIDATION: focused page suite GREEN — 4 tests per Vitest project, 8/8 total; existing `BusinessStatusBadge.test.ts` GREEN — 4 tests per project, 8/8 total.
BROADER_VALIDATION: frontend `corepack pnpm typecheck` GREEN.
STATUS: DONE_VERIFIED — both implementations correct and previously unverified; no production change.
REMAINING_GAPS: QA-HARDEN-01.07, QA-HARDEN-01.08, QA-HARDEN-01.10, QA-HARDEN-01.11; SEED-PROP-01.
NEXT_PACKAGE: QA-HARDEN-01D.

---

## Assurance Closure Wave 2A — QA-HARDEN-01D (2026-07-13, Codex CLI)

ASSURANCE_CLOSURE_WAVE_2A: in progress
PACKAGE: QA-HARDEN-01D — detail headers and dates
PLAN_ITEM_IDS: QA-HARDEN-01.07, QA-HARDEN-01.08
ROOT_CAUSE: header and merchant-date display improvements were implemented but had no focused presentation tests.
ACCEPTANCE_CRITERIA: PaymentOrderDetail renders exact `Last-Modified` and `Idempotency-Replayed` names/values; merchant detail formats created/updated values without raw ISO leakage under deterministic offset, DST, backend-precision, null-rejection and invalid-input cases.
TEST_LEVEL: Vue component/page tests.
FILES_CHANGED: `apps/frontend/tests/unit/qa-harden-detail-presentation.test.ts`; this checkpoint.
FOCUSED_VALIDATION: focused suite GREEN — 4 tests per Vitest project, 8/8 total; related history and merchant-list property/contract suites GREEN — 64/64 total across both projects.
BROADER_VALIDATION: frontend `corepack pnpm typecheck` GREEN.
STATUS: DONE_VERIFIED — both implementations correct and previously unverified; no production change.
REMAINING_GAPS: QA-HARDEN-01.10, QA-HARDEN-01.11; SEED-PROP-01.
NEXT_PACKAGE: QA-HARDEN-01E.

---

## Assurance Closure Wave 2A — QA-HARDEN-01E (2026-07-13, Codex CLI)

ASSURANCE_CLOSURE_WAVE_2A: in progress
PACKAGE: QA-HARDEN-01E — lifecycle hints and accessible names
PLAN_ITEM_IDS: QA-HARDEN-01.10, QA-HARDEN-01.11
ROOT_CAUSE: both implemented guidance improvements lacked focused semantic proof; the initial lifecycle setup set Pinia state before `mountSuspended` initialized its store and was corrected as `TEST_DESIGN_DEFECT` using the repository's mount/set/remount convention.
ACCEPTANCE_CRITERIA: If-Match has associated label, exact hint, `aria-describedby`, exact placeholder and update event; capture/refund expose exact minor-unit labels/placeholders/type/min only in supported states and emit the entered amount for the correct action.
TEST_LEVEL: Vue component tests with real Nuxt UI form semantics and Pinia lifecycle state.
FILES_CHANGED: `apps/frontend/tests/unit/qa-harden-lifecycle-guidance.test.ts`; this checkpoint.
FOCUSED_VALIDATION: post-correction focused suite GREEN — 4 tests per Vitest project, 8/8 total; related lifecycle suites GREEN — 152/152 total across both projects.
BROADER_VALIDATION: frontend `corepack pnpm typecheck` GREEN.
STATUS: DONE_VERIFIED — both implementations correct and previously unverified; no production change.
REMAINING_GAPS: frontend full suite/Chromium decision; SEED-PROP-01.
NEXT_PACKAGE: QA-HARDEN-01 final frontend validation, then SEED-PROP-01.

---

## Assurance Closure Wave 2A — SEED-PROP-01 (2026-07-13, Codex CLI)

ASSURANCE_CLOSURE_WAVE_2A: in progress
PACKAGE: SEED-PROP-01 — deterministic seed task 5.1 / Property 6 realm alignment
PLAN_ITEM_IDS: deterministic-seed-and-test-isolation task 5.1
ROOT_CAUSE: the optional realm-alignment property became executable after IAM and user-management completion but had no executable property proof.
ACCEPTANCE_CRITERIA: compare the actual five per-role Realm Test Users with deterministic fixture tenants/merchants; require every realm tenant reference to resolve, every optional realm merchant reference to resolve and belong to that user's tenant, all three expected realm tenant identities to be represented, and every seeded merchant to belong to a seeded tenant. Seed/reset must not mutate Keycloak.
TEST_LEVEL: pure jqwik property over a finite, shrinkable catalog parsed once from the actual realm import; no container or Keycloak mutation per example.
FILES_CHANGED: `apps/backend/src/test/java/lab/paymentquality/testing/internal/seed/RealmAlignmentPropertyTest.java`; `status/specs/deterministic-seed-and-test-isolation.md`; this checkpoint.
PROPERTY: Realm Test User tenant/merchant attributes align with `Fixtures.tenants()`/`Fixtures.merchants()` and merchant ownership.
TRIES: declared `tries = 100`; jqwik selected `EXHAUSTIVE` generation over all five finite catalog entries and completed 5/5 checks, which satisfies task 5.1's explicit “at least 100 iterations or exhaustive over the attribute set” criterion.
RUN_1: GREEN — 4 tests, property exhaustive 5/5, seed `-8479399993197945547`, Maven duration 3.652 s.
RUN_2: GREEN — 4 tests, property exhaustive 5/5, seed `-9067841590121427325`, Maven duration 1.464 s.
SHRINKING: preserved by the finite `Arbitraries.of(...)` generator; no shrink occurred because there was no failure.
FAILURES: 0.
RELATED_VALIDATION: `UserManagementServiceTest` 20, `PaymentOrderSecurityTest` 12, `KeycloakRealmRoleConverterTest` green, `TenantIsolationPropertyTest` green, `DeterministicDatasetTest` 3, and `FixturesTest` 25. Backend compile and test-compile GREEN.
BROADER_VALIDATION: filtered backend verify BUILD SUCCESS — Surefire 469 total / 464 passed / 5 skipped / 0 failed / 0 errors; Failsafe 46/46 / 0 failed / 0 errors / 0 skipped.
STATUS: DONE_VERIFIED — no production workaround, realm mutation, fixed property seed, permissive assertion, or container-per-example design was introduced.
REMAINING_GAPS: conditional Stage 4 task 6.1 remains deferred on its historical Open Question 2; it is not task 5.1 and not optional-plan debt.
NEXT_PACKAGE: Wave 2A final reconciliation and Wave 2B design only.

---

## Assurance Closure Wave 2A — final closure (2026-07-13, Codex CLI)

ASSURANCE_CLOSURE_WAVE_2A: complete
PACKAGE: Wave 2A final validation, completeness reconciliation, and REST-ADVANCED Wave 2B design only
PLAN_ITEM_IDS: QA-HARDEN-01.01–QA-HARDEN-01.11; SEED-PROP-01; REST-REDIRECT-01; REST-MULTIPART-01; REST-SSL-PROXY-01; REST-OPENAPI-DRIFT-01 (design only)
ROOT_CAUSE: eleven implemented required polish items lacked individual acceptance proof, and one now-executable optional seed property lacked executable evidence.
ACCEPTANCE_CRITERIA: every required item has direct focused evidence; property 6 has two deterministic executions plus related/backend regression proof; no REST-ADVANCED implementation occurs.
TEST_LEVEL: Nuxt server-route, Vue component/page, pure jqwik property, full frontend unit/typecheck, standard Chromium, and filtered Maven verify.
FILES_CHANGED: five focused frontend test files; one focused backend property-test file; one minimal `ProblemDetailsCard.vue` class correction; directly related status files. No `.kiro/**` or `.codex/**` file changed.
FOCUSED_VALIDATION: QA-HARDEN packages 01A–01E GREEN, 11/11 requirements; SEED-PROP-01 focused runs GREEN twice, 4/4 each with exhaustive 5/5 property checks.
BROADER_VALIDATION: frontend typecheck GREEN; unit suite GREEN — 58 files / 594 tests; standard Chromium GREEN — 82/82, 0 failed, 0 flaky. Backend compile/test-compile GREEN; filtered verify BUILD SUCCESS — Surefire 469 total / 464 passed / 5 skipped, Failsafe 46/46.
LIVE_PLAYWRIGHT: NOT_RUN — Wave 2A did not change auth storage states, tenant/role behavior, live-data isolation, BFF idempotency, or BFF conditional GET/304.
APPS_API_TESTS: NOT_RERUN — neither the presentation correction nor the pure property test changes an external API contract.
QA_HARDEN_RESULT: 11 verified; 10 already-correct implementations; 1 confirmed product defect; 2 test-design defects; 0 blocked.
PLAN_COMPLETENESS: required total 296; implemented required 296/296 (100.00%); verified required 296/296 (100.00%). Optional total 73; optional verified/deviation before 43/73; after 44/73; remaining 29/73 are explicitly `OPTIONAL_SKIPPED_ACCEPTABLE`; no open optional item remains.
REST_ADVANCED_WAVE_2B_DESIGN: recommended order `REST-MULTIPART-01` → `REST-SSL-PROXY-01` → `REST-REDIRECT-01` → `REST-OPENAPI-DRIFT-01`. Multipart has a real evidence-upload domain and can start with test-framework support. Proxy/SSL must split forwarded-header behavior from real TLS and stop before certificate material without an approved ephemeral trust strategy. Redirect work stops until a business/training redirect target or approved test-only server exists. OpenAPI drift stops until the repository has an agreed canonical spec/generation owner.
STATUS: DONE_VERIFIED.
REMAINING_GAPS: no required-plan verification gap. REST-ADVANCED remains unimplemented by design; conditional deterministic-seed Stage 4 remains deferred.
NEXT_PACKAGE: Assurance Closure Wave 2B only after its scope and stop gates are accepted.
