---
name: playwright-phase3-roadmap
origin: POST_KIRO_WORK
audited_branch: 001-project-foundation
audited_commit: c6de61f31e7cadc09331269f0f33e70573e4b889
last_updated: 2026-07-13
---

# Roadmap: Playwright Phase 3A / 3B / 3C (SDET learning expansion)

## Assurance Closure Wave 1 checkpoint

- `VAL-API-01`: **DONE_VERIFIED** on 2026-07-13 — standalone Surefire 79/79 and live Failsafe 72/72 after the isolated `VAL-API-01A` DTO contract-drift correction.
- `PW-AUTH-01` / `F-A2`: **DONE_VERIFIED** — real `PLATFORM_ADMIN` (`platform.admin`, `PLATFORM_TENANT`) and `MERCHANT_MANAGER` (`merchant.manager`, `TENANT_ALPHA`, `MERCHANT_ALPHA_001`) setup projects, same-route authorization, admin-control absence, token-storage guard, and foreign-tenant payment denial passed in the complete 7/7 live run.
- `PW-DATA-01` / `F-A4`: **DONE_VERIFIED** — worker-scoped allocation uses `runId + workerIndex + test identity`; the complete live run's two-worker proof passed with zero collisions and no global reset. The safe cleanup strategy is deterministic retention in the session-owned ephemeral database because no worker-owned DELETE/reset API exists.
- `PW-IDEM-01`: **DONE_VERIFIED** — complete live BFF proof verifies 201 initial creation, 200 replay, `Idempotency-Replayed` false/true, equal response semantics, direct GET persistence, and one matching filtered-list resource.
- `PW-304-01`: **DONE_VERIFIED** — complete live BFF proof verifies 200 initial GET, `ETag`, forwarded `If-None-Match`, 304, empty body, `Cache-Control`, and `Vary: Authorization`.
- `PW-HEAD-01`: **SUPERSEDED_BY_VERIFIED_SOLUTION** — fresh standalone `HttpMethodSemanticsContractSpec` Failsafe evidence verifies the same resource's 200/ETag/cache headers/empty body contract. There is no distinct BFF HEAD route or meaningful UI interaction to justify duplicate Playwright coverage.
- Assurance Closure Wave 1 is **DONE_VERIFIED**. Next queue: `QA-HARDEN-01`, `SEED-PROP-01`, `REST-ADVANCED`; do not start it without a new user request.

```text
ORIGIN: POST_KIRO_WORK
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-phase3-roadmap-execution-report.md
SOURCE_DOCUMENT: docs/analysis/payment-quality-engineering-lab-missing-features-playwright-learning-roadmap.md
SOURCE_DOCUMENT: docs/analysis/playwright-161-http-api-properties-test-strategy.md
RELATED_KIRO_TASKS: none directly — this is a dedicated Playwright/SDET test-authoring roadmap (feature IDs F-A1..F-D7) layered on top of whatever the `.kiro` specs already shipped; it does not implement new product features, only new test capabilities against existing surfaces (Error Lab, merchant/payment lifecycle, audit).
ACCEPTANCE_CRITERIA: see docs/analysis/payment-quality-engineering-lab-missing-features-playwright-learning-roadmap.md §5 (Playwright Capability Coverage Matrix) and §11 (Missing Feature Design Table).
```

## What this is

A staged Playwright test-suite expansion (dated through 2026-07-12 on `001-project-foundation`) driven by a dedicated gap-analysis roadmap document, not by `.kiro/specs/**`. It is organized into named "Feature IDs" (F-A1 API foundation, F-A3 network assertions, F-B4 DOM modal lifecycle, F-C5 sequential route-mock retry, F-D1..F-D7 "expert capability" UI features: ARIA snapshots, visual regression, command palette, PSP redirect simulator, audit diff drawer, payment expiration).

## Status: Phase 3A, 3B, and 3C all reported COMPLETE

- **Phase 3A-1** (API-only foundation): 13 new `APIRequestContext` tests in a new `api-tests` Playwright project (no browser/Keycloak/backend needed) against the standalone `trigger-429` BFF mock endpoint, plus a reusable `tests/api/helpers/assert-api.ts` assertion module (correlation-id, problem+json, no-cache-store, RFC 9457 structure, no-auth-leak).
- **Phase 3A-2** (UI network foundation): 5 new chromium tests across 2 new spec files (F-A3 `page.waitForResponse`/`waitForRequest`, F-D6 console/storage token guard, F-B4 modal DOM lifecycle).
- **Phase 3B-8** (F-C5 sequential retry demo): closes a gap left by earlier 3B-1..3B-7 sub-phases (not detailed in the excerpt read this session — read the full report for those if resuming this roadmap); adds a 503→200 stateful `route.fulfill()` retry-idempotency test. A **pre-existing baseline regression was found and explicitly left unfixed (out of scope)** in `merchant-feedback.spec.ts` — see `status/technical-debt/current-baseline.md`.
- **Phase 3B-Closure-Audit**: of 4 flagged capability gaps, `page.waitForRequest()` was closed; conditional-GET-304, idempotency-replay, and header-only-HEAD assertions remain blocked because the Playwright run in this environment has no backend/Keycloak available — bundled into one follow-up infra phase rather than re-attempted piecemeal.
- **Phase 3C** (3C-Prep through 3C-6): all 6 sub-phases (F-D4 ARIA snapshots, F-D5 visual regression, F-D3 command palette, F-D2 PSP redirect simulator, F-D7 audit diff drawer, F-D1 payment expiration) reported complete and green.
- **Cumulative total per the report**: 101 Playwright tests across 31 spec files, plus 58 backend unit tests added/touched across the F-C5/F-D7/F-D1 phases.

## This session's fresh validation vs. the roadmap's own claims

This session ran `corepack pnpm exec playwright test --project=chromium` fresh (see `status/evidence/latest-validation.md`). Result: **82 passed, 21 failed** (of 101 total listed) — NOT the "all green" state the execution report describes as of 2026-07-12. Failing spec files include `auth-deny.spec.ts`, `foundation.spec.ts`, `merchant-create.spec.ts`, `merchant-feedback.spec.ts` (the pre-existing regression the roadmap itself already flagged as known/unfixed), `merchant-lifecycle.spec.ts`, `payment-orders-panel.spec.ts`, `payment-status-polling.spec.ts`, and `rbac/merchant-risk-flag.spec.ts`. This directly confirms several of the hypotheses in the audit brief (see `status/technical-debt/current-baseline.md` for the itemized, evidence-backed list) — this is a **material regression** relative to the roadmap document's own "all quality gates green" claim, not a pre-existing/accepted gap.

## TD-2A closure (2026-07-12, this session) — 10 of 21 failures fixed

Investigated per `status/technical-debt/current-baseline.md` TD-2's own special-handling protocol (reproduce → group by root cause → fix the largest confirmed cluster only). Found and fixed **two** distinct, confirmed test-infrastructure defects (bundled as "TD-2A" since both were needed to reach a stable baseline for the affected files and both are pure test/config fixes, not product features):

1. **`apps/frontend/playwright.config.ts`** — the project had no `expect.timeout` override, so it used Playwright's 5000ms default. This project's `nuxt dev` webServer takes ~4.1–4.6s to render a fresh `/admin/merchants*` navigation even in isolation, and regularly exceeds 5s under the local machine's default 16-worker parallelism. Added `expect: { timeout: 15_000 }` project-wide (see the inline comment in the file for the full measurement-based justification). One existing spec (`ui/command-palette.spec.ts`) had already independently worked around this per-assertion with an explicit 15000ms override on the same locator — this generalizes that fix instead of repeating it per assertion.
2. **`apps/frontend/tests/e2e/merchant-support.ts`** — `mockAuthenticatedSession()` never included a `roles` array in its mocked `/api/_auth/session` response, even though the real Keycloak realm (`infra/keycloak/realms/payment-quality-realm.json`) assigns the default `platform.operator` user the `PLATFORM_ADMIN` realm role. `useAuthorization()`'s fail-closed default (empty roles → every capability false) hid every RBAC-gated button (Create merchant, Activate, Suspend) for any test using this shared helper. Fixed by defaulting `roles = ['PLATFORM_ADMIN']`; the one call site intentionally testing a no-authority denial path (`auth-deny.spec.ts`) now passes `roles: []` explicitly.

Result: the exact 8 originally-failing spec files went from 21 failed/61 passed → 11 failed/18 passed; the full 101-test suite went from 82 failed-count-basis (21 failed/61 passed of 82 executed) → 12 failed/70 passed (one of the 12, `payment-status-polling.spec.ts:52`, is confirmed flaky under full-suite contention only — passes reliably in isolation). Zero new failures introduced; frontend typecheck and all 532 unit tests remain green. Full evidence: `status/evidence/latest-validation.md` session 4, `status/technical-debt/current-baseline.md` TD-2A.

## TD-2B closure (2026-07-12, Codex CLI)

After an interrupted Claude Code CLI handoff, Codex verified and completed TD-2B. The backend wire contract is `DRAFT`, `ACTIVE`, `SUSPENDED`; frontend Zod, inferred application types, filters, action gating, badges, Playwright mocks, and visual snapshot now use the same values. `DRAFT` renders as `Draft`; `PENDING` is strictly rejected at the API boundary. Full Chromium improved from 70 passed / 12 failed to **72 passed / 10 failed**, with no new failure.

## Next work

## TD-2C closure (2026-07-12, Codex CLI)

TD-2C is `DONE_VERIFIED`. The three stale expectations were replaced with state-transition and semantic contracts; `LoadingState` now has `role=status` and an accessible name, while `ErrorState` has `role=alert` and an accessible name. Affected specs are 6/6 green. Full Chromium improved from 72 passed / 10 failed to **76 passed / 6 failed**, with no snapshot change and no new failure.

## Next work

1. **TD-2E — align Create merchant action assertions with current accessible names.** Four failures in `merchant-create.spec.ts` expect exact name `Create`; the current form exposes `Create merchant`. This is the largest confirmed stable remaining cluster.
2. **TD-2D — stale auth-deny redirect assumption.** `auth-deny.spec.ts:4` still expects the old Keycloak route rather than the current `/login` behavior.
3. **TD-2F — product/lifecycle decision for `SUSPENDED -> ACTIVE`.** `merchant-lifecycle.spec.ts:39` conflicts with the backend state machine and must not be mechanically changed.
4. The blocked conditional-GET-304 / idempotency-replay / header-only-HEAD Playwright coverage remains genuinely blocked on backend/Keycloak availability in this environment, not a code defect — re-attempt only once a live backend+Keycloak Playwright run is possible (`PLAYWRIGHT_USE_REAL_KEYCLOAK=true` per root `CLAUDE.md`).

## TD-2E partial result (2026-07-12, Codex CLI) — Create merchant accessible-name alignment

`CreateMerchantForm.vue` deliberately renders a submit `button` with visible text `Create` and enabled-state `aria-label="Create merchant"`; its intended accessible name is therefore `Create merchant`. Fresh pre-edit Chromium reproduction confirmed that four `merchant-create.spec.ts` tests all timed out before any business action because their exact semantic locator still expected `Create`. Updated only the six affected `getByRole('button', { name: 'Create', exact: true })` calls to exact `Create merchant`, preserving role-based selection, exactness, all validation/create assertions, and the TD-2B `DRAFT` contract. No production code, helper, state component, or visual snapshot changed.

The correction made three of the four test titles pass. The fourth, `shows create validation and duplicate feedback`, now reaches a separately stale duplicate-error route fixture and fails because `{ error, message }` does not match the current Problem Details client contract; the UI correctly shows its generic safe fallback. TD-2E is therefore `PARTIAL`, not `DONE_VERIFIED`. Full Chromium moved from 76 passed / 6 failed to 78 passed / 4 failed: TD-2E-1 duplicate fixture, TD-2D, TD-2F, and historical polling contention remain. Next executable work is TD-2E-1 only.

## Session 8 (2026-07-13, HEAD `95e35c9`) — TD-2 implementation blocked before browser validation

- **TD-2E-1:** Reclassified from `STALE_MOCK_CONTRACT` to a duplicate-error contract defect: the mock and backend handler both emitted legacy JSON, while the client consumes RFC 9457 Problem Details. The duplicate handler now returns `409 application/problem+json` with `type`, `title`, `status`, `detail`, and `error`; `backendApi` preserves backend Problem Details; the E2E mock mirrors the contract. `useApiClient` has a focused duplicate-detail test. Status: `IMPLEMENTED_UNVERIFIED` pending the exact E2E test, full merchant-create spec, and Chromium baseline.
- **TD-2F:** `MerchantStatusTest` and `MerchantTest` prove `SUSPENDED` has no legal outgoing transition. Fixed frontend list/detail action gating to show activation only for `DRAFT`; changed the E2E case to assert no activate control for suspended merchants. Status: `IMPLEMENTED_UNVERIFIED` pending Playwright.
- **TD-2D:** Confirmed middleware redirects unauthenticated visitors to `/login?redirectTo=...`; revised the test from an invalid mocked Keycloak-page expectation to the application-owned login contract and protected-data absence. Status: `IMPLEMENTED_UNVERIFIED` pending Playwright.
- **TD-2G:** No code change. The required repeat-each investigation is `BLOCKED_ENVIRONMENT` because this sandbox denies localhost socket access before Nuxt starts. No sleep, retry, timeout, or worker workaround was added.
- Frontend validation: `corepack pnpm typecheck` GREEN; `corepack pnpm test:unit` GREEN, 46 files / 550 tests. Playwright listing: 82 Chromium tests in 28 files. Chromium execution is blocked before test discovery by the socket policy.

## Continuation validation closure (2026-07-13, HEAD `95e35c9`)

- Environment is now socket-enabled: localhost binding and Podman/Testcontainers were verified.
- **TD-2E-1 / TD-2E: DONE_VERIFIED.** Exact duplicate E2E, merchant-create (6 Chromium tests), frontend typecheck/unit suite, and `MerchantRestAssuredTest` (5/5) are green. Duplicate responses are `409 application/problem+json` with strict Problem Details parsing preserved.
- **TD-2F: DONE_VERIFIED.** `MerchantStatusTest` plus `MerchantTest` are 17/17; UI exposes activation only from `DRAFT`; lifecycle E2E is green.
- **TD-2D: DONE_VERIFIED.** Browser URL normalization made raw `%2F` matching stale; semantic `/login` plus `redirectTo=/admin/merchants`, visible login control, and absent merchant data are green.
- **TD-2G: DONE_VERIFIED.** Measured 6/10 manual failures before correction because `includes(detailPath)` matched supporting `/history` and `/evidence` responses. Exact GET/path matching produced repeat-each=10 green (20 cases), with no sleep/retry/timeout change.
- **TD-2: DONE_VERIFIED.** Full Chromium closure run 1 = 82/82, run 2 = 82/82; final independent Chromium = 82/82. No residual Phase 3 TD-2 failure remains.
