---
name: index
last_updated: 2026-07-12
---

# Status Index — Payment Quality Engineering Lab

See `status/README.md` for the full model explanation and update protocol before acting on anything below.

## Repository snapshot

| Field | Value |
|---|---|
| Branch | `001-project-foundation` |
| HEAD | `fec8e1da46da18e3d141660c5bc0753de2ddabf2` |
| Last updated | 2026-07-12 |
| Working tree | Clean (`git status --short` empty at audit start and end — no `.kiro`, `.codex`, code, or test files were modified during this audit) |

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

- **Current problem:** none blocking — this session was a status-system restoration audit only (no implementation). The one concrete, unresolved technical issue is `status/technical-debt/current-baseline.md` TD-1 (`AuditEventPersistenceTest` red on the current filtered `verify`) and TD-2 (21 fresh Playwright chromium failures), both discovered fresh in this session.
- **Current phase:** no spec is "in progress." The last *implementation* work recorded anywhere in `.codex/**` or `docs/implementation/**` was the Playwright Phase 3C roadmap (POST_KIRO_WORK, see `status/roadmaps/playwright-phase3-roadmap.md`), which this session's fresh validation shows is **not** fully green despite its own execution report's "all green" claim.
- **Next task:** see `status/technical-debt/current-baseline.md` — TD-1 (backend audit entity/export scope-drift) and TD-2 (Playwright chromium regressions) are the two items with the most concrete, ready-to-act-on evidence. No Kiro spec has a required task blocking either of these.
- **Blockers:** none environmental — Podman/Testcontainers and Keycloak-in-tests were both available and used successfully during this session's fresh backend validation.
- **Decisions required from the user:** (1) whether to fix or formally re-scope the undocumented audit-export feature (`AuditExportEvent`, `before_state`/`after_state` columns) that has no owning spec — see TD-1; (2) whether/when to resume `deterministic-seed-and-test-isolation` Stage 3 (5.1) and Stage 4 (6.1), which are gated on other specs/decisions per their own design.

## Validation baseline

Full detail: `status/evidence/latest-validation.md`. Summary:

| Suite | Result | Detail |
|---|---|---|
| Backend `./mvnw compile` | GREEN | — |
| Backend `./mvnw test-compile` | GREEN | — |
| Backend `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify` | **RED** | 462 tests, 1 failure (`AuditEventPersistenceTest`), 5 skipped — see TD-1 |
| Frontend `corepack pnpm typecheck` | GREEN | — |
| Frontend `corepack pnpm test:unit` | GREEN | 46 files / 532 tests |
| Frontend `playwright test --list` | GREEN | 101 tests / 31 files (listing only) |
| Frontend `playwright test --project=chromium` | **RED** | 82 executed, 61 passed, 21 failed — see TD-2 |
| PostgreSQL / Testcontainers | Available and used successfully | Podman-backed, no environment blockers this session |
| Keycloak | Not exercised (mocked-session default Playwright mode; real-Keycloak backend ITs like `UserManagementKeycloakAdminIT` were not re-run this session, only inspected for existence) | — |
| Known regressions carried forward | `merchant-feedback.spec.ts` (pre-existing, already documented as accepted in the Phase 3 roadmap report) | Not new |

## Post-Kiro roadmaps

These are later work programs layered on top of (not part of) the seven Kiro specs above. See `status/roadmaps/*.md` for full detail:

- `status/roadmaps/mvp-phase1-phase2.md` — HTTP contract hardening (conditional GET/304, idempotency replay, header forwarding), 30 tasks, complete and independently re-verified with 2 regressions found and fixed.
- `status/roadmaps/system-hardening-and-frontend-polish.md` — 11 small UI/UX fixes across two review passes, complete.
- `status/roadmaps/playwright-phase3-roadmap.md` — Playwright/SDET test-suite expansion (Phase 3A/3B/3C, feature IDs F-A1..F-D7), reported complete/green by its own execution report, but **this session's fresh chromium run found 21 failures**, contradicting that report's "all green" claim — see TD-2.

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
[x] .kiro/** unchanged (git status clean for .kiro at both start and end)
[x] .codex/** unchanged (git status clean for .codex at both start and end)
[x] code and tests unchanged (git status clean for apps/** at both start and end; validation runs were
    read-only executions, no fixes applied)
```
