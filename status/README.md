# Status Tracking System — Payment Quality Engineering Lab

This directory is the **tool-neutral execution layer** restoring the project's original planning model after `.codex/**` and various `docs/**` reports had begun to drift into a de facto (but undocumented) source of truth. It is read by, and updated by, Claude Code CLI, Codex CLI, OpenCode, and any future coding agent working in this repository.

## The model

```text
.kiro/specs/**        = IMMUTABLE PLANNING SOURCE
                         requirements.md -> what the system must do
                         design.md       -> how it was designed to work
                         tasks.md        -> the original, complete implementation map
                         Never edited during implementation. Never re-derived elsewhere.

status/**             = MUTABLE EXECUTION TRUTH  (this directory)
                         What is actually implemented, verified, partial, blocked,
                         superseded, or deferred — right now, independent of any
                         .kiro checkbox. Cites .kiro task IDs by exact reference;
                         never restates or redefines requirements/design itself.

.codex/**              = HISTORICAL EXECUTION EVIDENCE
                         Wave-by-wave execution notes written during implementation
                         waves before this status/** system existed. Preserved as-is,
                         read as evidence, never rewritten to match new conclusions.

docs/implementation/** = DETAILED EXECUTION REPORTS
                         Dated, narrative reports for POST_KIRO_WORK roadmaps
                         (MVP/Phase 1-2, System Hardening, Frontend Polish,
                         Playwright Phase 3A/3B/3C). Treated as evidence, not as
                         a second source of Kiro task status.

code / tests           = GROUND TRUTH FOR "DOES IT ACTUALLY WORK"
                         The final arbiter when documents disagree.
```

## Why this exists

`.kiro/specs/**` is intentionally read-only during implementation (see `AGENTS.md`, `.codex/README.md`): agents were told never to check or uncheck `.kiro/tasks.md` boxes, and to track real progress in `.codex/**` instead. That worked, but produced two problems over time: (1) `.codex/**` mixes wave-execution narrative with status in a format that's hard to query across seven specs at once, and (2) several `docs/**` reports (e.g. `docs/specs-analysis/**/README.md`) went stale and now contradict the actual, verified state of the code. `status/**` exists to give every agent one place to check current, per-task execution status without needing to re-derive it from a 1000-line `.codex/current-state.md` or trust a possibly-stale narrative doc.

## Rules for every agent (Claude Code, Codex CLI, OpenCode, future agents)

1. **Before starting work on any task**, read, in this order: the relevant `.kiro/specs/{spec}/{requirements,design,tasks}.md`, then `status/index.md`, then `status/specs/{spec}.md`. Confirm the exact Kiro task ID you intend to work on and its stated acceptance criteria.
2. **Never modify `.kiro/**` during implementation.** If you believe a requirement or design decision is wrong, say so in `status/specs/{spec}.md` section 4/5 (gaps/deviations) — do not edit the Kiro source.
3. **After every task**, update the corresponding leaf row in `status/specs/{spec}.md`: execution status, implementation evidence (file/class/method/endpoint/migration), test evidence, and the commit SHA you verified against. Then roll that up into the parent task's status (parents are a computed rollup of their children, never assigned independently) and refresh `status/index.md`'s summary tables.
4. **Cite a commit SHA and real test results** for every status change. Do not use `DONE_VERIFIED` unless you have fresh, credible test evidence — code existing without a passing test is `IMPLEMENTED_UNVERIFIED`, not `DONE_VERIFIED`.
5. **Never mark a task done without evidence.** "The code looks right" is not evidence; a named test class/method or manual verification step with an observed result is.
6. **Do not create a second requirements/design map inside `status/**`.** Sections 1 (Original Kiro intent) and 2 (Current implementation summary) are short orientation summaries only — the `.kiro` documents remain the only normative requirements/design text.
7. **Never delete entries for superseded or optional tasks.** Use `SUPERSEDED`/`OPTIONAL_SKIPPED_ACCEPTABLE`/`DEFERRED` and say why; the ledger row stays so the original Kiro task is never silently dropped from view.
8. **A new task with no Kiro origin must be tagged `POST_KIRO_WORK`** (see `status/roadmaps/**` for the existing examples) with `SOURCE_DOCUMENT`, `RATIONALE`, `RELATED_KIRO_TASKS` (if any), and its own `ACCEPTANCE_CRITERIA`. It must never be used to replace or hide an unfinished Kiro task.

## Update protocol (repeat this every session)

**Before implementation:**
1. Read the Kiro spec (`requirements.md`, `design.md`, relevant part of `tasks.md`).
2. Read `status/index.md`.
3. Read `status/specs/{spec}.md`.
4. Confirm the exact task ID and its acceptance criteria.
5. Set that task's row to `IN_PROGRESS` in `status/specs/{spec}.md`.

**After implementation:**
1. List the files you changed.
2. List which acceptance criteria are now met.
3. Run the required tests (respecting the repository's standing exclusions — see root `CLAUDE.md`/`AGENTS.md` for `restkit/**` and `paymentsupport/**`).
4. Update the leaf task's status with real evidence.
5. Update the parent task's rollup status.
6. Update `status/index.md`.
7. Update `status/evidence/latest-validation.md` if you ran a broader validation pass.
8. Record the commit SHA.
9. Confirm `.kiro/**` is unchanged (`git status --short -- .kiro` should be empty).

If tests were not run, the status must be `IMPLEMENTED_UNVERIFIED`, never `DONE_VERIFIED`.

**New non-Kiro work** must carry:
```text
ORIGIN: POST_KIRO_WORK
RATIONALE: <why this work exists>
SOURCE_DOCUMENT: <path>
RELATED_KIRO_TASKS: <spec/task-id, or "none">
ACCEPTANCE_CRITERIA: <what "done" means for this item>
```
It must never be used to replace or obscure an unfinished Kiro task — see `status/roadmaps/**` for the standing examples (MVP Phase 1/2, System Hardening + Frontend Polish, Playwright Phase 3A/B/C).

## Directory contents

- `status/index.md` — repository snapshot, Kiro coverage table (must read 100%), execution summary, active work, validation baseline, post-Kiro roadmap pointers.
- `status/specs/*.md` — one file per `.kiro/specs/*` directory; complete leaf-task ledger with independent, evidence-based execution status per task.
- `status/roadmaps/*.md` (and subfolders such as `checkout-protocol-lab/`) — later work programs (`POST_KIRO_WORK`) that are not direct Kiro tasks.
- `status/technical-debt/current-baseline.md` — only confirmed, evidence-backed problems (red tests, contract drift, stale docs) — not general improvement ideas.
- `status/evidence/latest-validation.md` — the most recent full validation run: commands, results, what wasn't run and why.

## What this system deliberately does not do

- It does not re-litigate whether a `.kiro` requirement was a good idea — that belongs in `status/specs/{spec}.md` section 4/5 as a flagged gap/deviation, not a rewrite.
- It does not trust an unchecked `.kiro` checkbox as evidence of anything. Several specs (`tenant-model-and-isolation`, `user-management`, `audit-log-dashboard`, `deterministic-seed-and-test-isolation`) are fully implemented and verified in code with **every** box still unchecked — this is the expected, by-design state of the `.kiro`-is-read-only workflow, not a defect.
- It does not trust a checked `.kiro` checkbox as proof either — `backend-authority-refactor` has all 28 boxes checked, and this session still independently re-verified each of its 23 leaf tasks against current code before assigning execution status.
