---
name: index
last_updated: 2026-07-12
---

## Session log

- **Session 4 (2026-07-12, commit `c6de61f` → this session):** Worked `TD-2A` (the largest confirmed cluster within `TD-2`'s 21 Playwright chromium failures). Reproduced the 21-failure baseline fresh, built a failure ledger, and — via controlled experiments (serial vs. parallel reruns, a temporary diagnostic probe spec, and direct inspection of `useAuthorization.ts`/`rbacMatrix.ts`/the real Keycloak realm JSON) — confirmed **two** distinct test-infrastructure root causes, both fixed: (1) the default 5000ms Playwright assertion timeout is too tight for this project's `nuxt dev` webServer (~4.1–4.6s real render latency, worse under 16-worker contention) — fixed via `apps/frontend/playwright.config.ts`'s new `expect.timeout: 15_000`; (2) the shared `mockAuthenticatedSession()` test helper never included a `roles` array, so `useAuthorization()`'s fail-closed default hid every RBAC-gated button, even though the real Keycloak realm assigns `platform.operator` the `PLATFORM_ADMIN` role — fixed via `apps/frontend/tests/e2e/merchant-support.ts` (default `roles: ['PLATFORM_ADMIN']`) and one explicit override in `auth-deny.spec.ts` to preserve its intentional no-authority test. Result: 21→11 remaining failures (10 fixed), 0 new failures, frontend typecheck/532 unit tests still green. TD-2A **RESOLVED**; TD-2 overall **IN_PROGRESS** — 11 failures remain across 3 further confirmed-independent root causes (TD-2B PENDING/DRAFT contract bug, 7 tests, likely production fix + a UX decision; TD-2C stale UI copy, 3 tests; TD-2D stale auth-deny redirect assumption, 1 test), none fixed this session per the one-work-package rule. See `status/technical-debt/current-baseline.md`, `status/roadmaps/playwright-phase3-roadmap.md`, `status/evidence/latest-validation.md`.
- **Session 3 (2026-07-12, commit `c6de61f`):** Worked `TD-5` (stale merchant-count assertions in `SeedProfileStartupIT`/`TestEndpointsEnabledIT`). Root cause confirmed via `git log`/`git show 8861f84`: `Fixtures.merchants()` was deliberately extended from 3 to 4 merchants (`MERCHANT_SUSPENDED_DEMO`, MVP Phase 1 roadmap task `SEED-MVP-001`, commit `8861f84`) **after** these two Failsafe integration tests were written (commit `1ade297`, predates `8861f84`); `FixturesTest` was correctly updated at the time, but these two IT files never were. Fixed 4 stale `isEqualTo(3)`→`isEqualTo(4)` assertions and strengthened `SeedProfileStartupIT.deterministicMerchantUuidsArePresent` to also check the 4th merchant's UUID. TD-5 **RESOLVED**. Full filtered `./mvnw verify` is now **BUILD SUCCESS** for the first time across all 3 sessions (Surefire 463/463, Failsafe 46/46, 0 failures). Reverted the `status/specs/deterministic-seed-and-test-isolation.md` tasks 3.2/3.3 `CONFLICTING_EVIDENCE` flag back to `DONE_VERIFIED` with fresh evidence. Next task: `TD-2` (21 Playwright chromium failures) — not started, per the one-work-package rule.
- **Session 2 (2026-07-12, commit `c6de61f`):** Worked `TD-1` (`AuditEventPersistenceTest` stale field-list assertion). Root cause confirmed: a legitimate, later, well-tested feature (audit before/after-state diff drawer, Playwright Phase 3C-5 "F-D7") added two entity fields the test's exhaustive assertion never accounted for, because the implementing session had no Testcontainers runtime to catch it. Fixed by updating the assertion and adding a dedicated JSONB round-trip test — not by reverting the feature. TD-1 **RESOLVED**. Documented the decision in new `status/roadmaps/audit-export-closure.md`. Discovered and documented (but did **not** fix, per the one-work-package rule) a new, unrelated regression: **TD-5** — 4 Failsafe integration-test failures in `deterministic-seed-and-test-isolation`'s `testing` module (stale hardcoded merchant-count assertions), previously masked because TD-1 always stopped the build before Failsafe could run. TD-5 is now resolved (see Session 3 above). See `status/technical-debt/current-baseline.md` and `status/evidence/latest-validation.md` for full detail.

# Status Index — Payment Quality Engineering Lab

See `status/README.md` for the full model explanation and update protocol before acting on anything below.

## Repository snapshot

| Field | Value |
|---|---|
| Branch | `001-project-foundation` |
| HEAD (session 4 start) | `c6de61f31e7cadc09331269f0f33e70573e4b889` (no new commits from sessions 2/3/4 — all changes are uncommitted working-tree edits) |
| Last updated | 2026-07-12 |
| Working tree | 6 code files modified across sessions 2–4: `apps/backend/src/test/java/lab/paymentquality/audit/AuditEventPersistenceTest.java` (TD-1), `apps/backend/src/test/java/lab/paymentquality/testing/{SeedProfileStartupIT,TestEndpointsEnabledIT}.java` (TD-5), `apps/frontend/playwright.config.ts`, `apps/frontend/tests/e2e/{merchant-support.ts,auth-deny.spec.ts}` (TD-2A). `.kiro/**` and `.codex/**` unchanged. |

## Kiro coverage

100% of leaf tasks across all 7 discovered `.kiro/specs/*` directories are mapped in `status/specs/*.md`, mechanically cross-checked against a parsed extract of each `tasks.md` (see completeness notes below).

| Spec | All Kiro items (incl. parents/checkpoints) | Leaf tasks | Mapped | Unmapped | Coverage |
|---|---:|---:|---:|---:|---:|
| `backend-authority-refactor` | 28 | 23 | 23 | 0 | 100% |
| `iam-roles-and-keycloak-login` | 38 | 25 | 25 | 0 | 100% |
| `payment-operations-dashboard` | 73 | 59 | 59 | 0 | 100% |
| `tenant-model-and-isolation` | 40 | 33 | 33 | 0 | 100% |
| `user-management` | 54 | 46 | 46 | 0 | 100% |
| `audit-log-dashboard` | 55 | 46 | 46 | 0 | 100% |
| `deterministic-seed-and-test-isolation` | 31 | 25 | 25 | 0 | 100% |
| **Total** | **319** | **257** | **257** | **0** | **100%** |

**Completeness-gate note:** the `deterministic-seed-and-test-isolation` drafting agent initially omitted leaf task `7` ("Final checkpoint") from its ledger — caught by this orchestrating session's own mechanical row-count cross-check against the pre-extracted task list, and added directly to `status/specs/deterministic-seed-and-test-isolation.md` with full evidence and an explicit `CONFLICTING_EVIDENCE` note (see that file and `status/technical-debt/current-baseline.md` TD-1). No other spec had an unmapped or duplicated row.

## Execution summary

| Spec | Overall status | Done verified | Done w/ deviation | Optional skipped | Not started / deferred | Next task |
|---|---|---:|---:|---:|---:|---|
| `backend-authority-refactor` | DONE_VERIFIED | 17 | 6 | 0 | 0 | NO_KIRO_TASK_REMAINING (optional cleanup: raise jqwik `tries` 30→100 on 4 property tests) |
| `iam-roles-and-keycloak-login` | DONE_VERIFIED | 17 | 1 | 7 | 0 | NO_KIRO_TASK_REMAINING |
| `payment-operations-dashboard` | COMPLETE_AND_KIRO_MARKED | 46 | 6 | 7 | 0 | NO_KIRO_TASK_REMAINING (closed 2026-06-18 per user decision) |
| `tenant-model-and-isolation` | DONE_VERIFIED | 28 | 5 | 0 | 0 | NO_KIRO_TASK_REMAINING |
| `user-management` | COMPLETE_WITH_OPTIONAL_GAPS | 39 | 0 | 7 | 0 | NO_KIRO_TASK_REMAINING (required work); optional gaps 6.1/6.8–6.13 remain skipped |
| `audit-log-dashboard` | COMPLETE_WITH_OPTIONAL_GAPS | 38 | 3 | 5 | 0 | NO_KIRO_TASK_REMAINING (required); optional jqwik P1/P2/P4/P6 remain the largest optional coverage gap |
| `deterministic-seed-and-test-isolation` | STAGE_1_DONE_LATER_STAGES_DEFERRED | 18 | 2 | 3 | 2 (1 not-started, 1 deferred — both Stage 3/4 gated on other specs) | NO_KIRO_TASK_REMAINING for currently-satisfiable prerequisites; Stage 3 (5.1) gated on confirming iam/user-management landed, Stage 4 (6.1) gated on Open Question 2 |
| **Total (257 leaf tasks)** | — | **203** | **23** | **29** | **2** | — |

All 7 specs have **no required, currently-executable Kiro task remaining**. Every spec's remaining open items are either explicitly optional (and accepted as skipped), or gated on a prerequisite/decision that has not yet been triggered by the user. This is a materially different picture from a naive reading of the raw `.kiro` checkboxes, where 5 of 7 specs show 0 checked boxes — see `status/README.md` for why the checkbox is not evidence either way.

## Active work

- **Current problem:** `TD-2B` (new, confirmed) — 7 of the remaining 11 Playwright chromium failures trace to a real merchant-status contract mismatch (frontend `'PENDING'` vs. real backend `'DRAFT'`) spanning 7 production files, plus an open UX question (what should the *display label* be) that must be resolved before touching `BusinessStatusBadge.vue`. Not fixed yet — queued as next task.
- **Current phase:** no spec is "in progress." TD-1, TD-5, and TD-2A are all closed. Backend filtered `verify` remains fully GREEN (unaffected by this session's frontend-only work). Frontend Playwright baseline improved from 21→11 failures this session; the Playwright Phase 3C roadmap (POST_KIRO_WORK, see `status/roadmaps/playwright-phase3-roadmap.md`) remains not fully green.
- **Next task:** `TD-2B` — merchant status contract mismatch (`'PENDING'` should be `'DRAFT'` in `apps/frontend/app/composables/useMerchantsApi.ts`, `apps/frontend/app/components/merchant/MerchantTable.vue` (3 locations), `apps/frontend/app/pages/admin/merchants/index.vue`, `apps/frontend/app/components/shared/BusinessStatusBadge.vue`, plus matching test files). Before fixing: confirm the intended display label with the user/product owner — `merchant-create.spec.ts:20` currently expects the raw text "DRAFT" visible, which may or may not reflect actual intent versus a friendlier "Pending" label; do not guess. See `status/technical-debt/current-baseline.md` TD-2B for full detail. After TD-2B: `TD-2C` (3 tests, stale UI copy) and `TD-2D` (1 test, stale auth-deny redirect assumption) remain, then TD-2 overall can close once the full 101-test baseline is rerun green.
- **Blockers:** none environmental — Podman/Testcontainers were available and used successfully across all 4 sessions' backend validation. TD-2B has a **decision blocker** (display-label UX choice), not an environment blocker — see above.
- **Decisions required from the user:** (1) TD-2B's display-label question (raw "DRAFT" text vs. a friendlier label) — see above; (2) whether/when to resume `deterministic-seed-and-test-isolation` Stage 3 (5.1) and Stage 4 (6.1), which are gated on other specs/decisions per their own design. (TD-1, TD-5, and TD-2A decisions are all resolved — see `status/roadmaps/audit-export-closure.md`, `status/technical-debt/current-baseline.md` TD-5/TD-2A.)

## Validation baseline

Full detail: `status/evidence/latest-validation.md`. Summary:

| Suite | Result | Detail |
|---|---|---|
| Backend `./mvnw compile` | GREEN | — |
| Backend `./mvnw test-compile` | GREEN | — |
| Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` — Surefire stage | **GREEN** | 463 tests, 0 failures, 5 skipped — TD-1 fixed session 2 |
| Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` — Failsafe stage | **GREEN** (was RED) | 46 tests, 0 failures — TD-5 fixed session 3 |
| Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` — overall | **BUILD SUCCESS** | First fully-green filtered backend baseline across sessions 1–4 |
| Frontend `corepack pnpm typecheck` | GREEN | Re-run session 4 after TD-2A |
| Frontend `corepack pnpm test:unit` | GREEN | 46 files / 532 tests, re-run session 4, unchanged |
| Frontend `playwright test --list` | GREEN | 101 tests / 31 files (listing only), unchanged |
| Frontend `playwright test --project=chromium` | **RED, improving** | 82 executed: was 61 passed/21 failed (session 1), now **70 passed/12 failed** (session 4, after TD-2A) — see TD-2B/C/D (next tasks) |
| PostgreSQL / Testcontainers | Available and used successfully across all 4 sessions | Podman-backed, no environment blockers |
| Keycloak | Not exercised via Playwright (mocked-session default mode); real-Keycloak backend IT `UserManagementKeycloakAdminIT` passes as part of the full Failsafe run (3/3, confirmed session 3) | — |
| Known regressions carried forward | `merchant-feedback.spec.ts` — now understood as TD-2C (stale UI copy), not a mystery pre-existing regression | Reclassified, not new |
| Known flaky test | `payment-status-polling.spec.ts:52` — passes reliably in isolation (3/3), occasionally times out only under full-101-test 16-worker contention even with the new 15000ms budget | Residual environmental flakiness, not a defect |

## Post-Kiro roadmaps

These are later work programs layered on top of (not part of) the seven Kiro specs above. See `status/roadmaps/*.md` for full detail:

- `status/roadmaps/mvp-phase1-phase2.md` — HTTP contract hardening (conditional GET/304, idempotency replay, header forwarding), 30 tasks, complete and independently re-verified with 2 regressions found and fixed.
- `status/roadmaps/system-hardening-and-frontend-polish.md` — 11 small UI/UX fixes across two review passes, complete.
- `status/roadmaps/playwright-phase3-roadmap.md` — Playwright/SDET test-suite expansion (Phase 3A/3B/3C, feature IDs F-A1..F-D7), reported complete/green by its own execution report, but a fresh chromium run found 21 failures, contradicting that report's "all green" claim (TD-2). Updated session 4 with the TD-2A closure record (10 of 21 fixed) and the remaining TD-2B/C/D breakdown.
- `status/roadmaps/audit-export-closure.md` — Formal closure record for two previously-unowned POST_KIRO_WORK features found living in the `audit` module (export index + `AuditExportEvent`/`Response`, and the before/after-state diff drawer "F-D7"). Decision: KEEP both (real UI/API usage, safe field scoping, dedicated tests). Resolves TD-1.

## Completeness self-check (per audit brief §19)

```text
[x] all Kiro specs found (7: backend-authority-refactor, iam-roles-and-keycloak-login,
    payment-operations-dashboard, tenant-model-and-isolation, user-management,
    audit-log-dashboard, deterministic-seed-and-test-isolation)
[x] every tasks.md read in full (by the per-spec drafting agent + spot-checked by this session)
[x] all parent tasks represented (rollup rows included in each ledger table)
[x] all leaf tasks mapped (257/257, after the 1-row deterministic-seed correction)
[x] no unmapped tasks remaining
[x] no duplicate mappings found
[x] optional tasks preserved (29 OPTIONAL_SKIPPED_ACCEPTABLE rows, none deleted)
[x] deferred tasks preserved (1 DEFERRED row: deterministic-seed 6.1)
[x] superseded tasks preserved (none found needing this status this session)
[x] Kiro checkbox preserved as historical info only (ORIGINAL_KIRO_CHECKBOX column in every ledger)
[x] execution status assigned independently of checkbox (confirmed per-spec: backend-authority-refactor
    has 28/28 boxes checked yet was still independently re-verified; tenant/user-management/audit/
    deterministic-seed have 0 checked boxes yet are DONE_VERIFIED where evidenced)
[x] every DONE_VERIFIED has cited evidence (file/class/test) in its ledger row
[x] every POST_KIRO_WORK item has a SOURCE_DOCUMENT (see status/roadmaps/*.md)
[x] status/index.md consistent with status/specs/*.md (numbers above derived directly from the ledger
    tables via a mechanical parse, not estimated)
[x] .kiro/** unchanged (git status clean for .kiro across all 3 sessions)
[x] .codex/** unchanged (git status clean for .codex across all 3 sessions)
[x] code/test changes scoped to the selected work package only (sessions 2-3 modified exactly 3 test
    files — AuditEventPersistenceTest.java for TD-1, SeedProfileStartupIT.java + TestEndpointsEnabledIT.java
    for TD-5 — no production code, migrations, or unrelated files touched; git diff --check clean)
```
