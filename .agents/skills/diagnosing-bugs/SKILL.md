---
name: diagnosing-bugs
description: Disciplined diagnosis loop for hard bugs, flakes, and performance regressions. Use when the user says diagnose or debug, or reports something broken, throwing, failing, or slow.
---

# Diagnosing Bugs

Skip phases only when explicitly justified. Redact secrets as `<REDACTED>`.

Read `.codex/CONTEXT.md` if present.

## Phase 1 — Build a feedback loop

This is the skill. No hypothesis before a **tight** command that can go **red on this bug**.

Try in this order:

1. Failing test at the right seam (REST Assured, Playwright REST, Playwright E2E, unit)
2. HTTP replay against the running API (`SPRING_PROFILES_ACTIVE=dev`)
3. Playwright headed/trace against the dashboard
4. Narrow Maven test: `./mvnw -Dtest=Class#method test` from `apps/backend`
5. Bisection harness between two commits
6. Differential: old vs new response headers/body

Tighten: faster, sharper assertion on the **user's symptom**, deterministic (unique refs, no shared seed).

Flakes: raise reproduction rate until debuggable. A 50% flake is usable; 1% is not.

Phase 1 is done when you have **already run** one command that is red-capable, deterministic (or high-rate), fast, and agent-runnable. Show invocation + redacted output.

## Phase 2 — Reproduce + minimise

Confirm the loop shows the **user's** failure. Shrink inputs until every remaining element is load-bearing.

## Phase 3 — Hypothesise

Write 3–5 ranked, falsifiable hypotheses. Show them before testing.

> If X is the cause, then changing Y makes the bug disappear / changing Z makes it worse.

## Phase 4 — Instrument

One variable at a time. Prefer debugger, then tagged logs `[DEBUG-a4f2]`. Never log everything. For perf: measure first.

## Phase 5 — Fix + regression

If a correct seam exists, follow `tdd`: failing regression at that seam, then fix, then re-run the original Phase 1 loop.

If no correct seam exists, that is the finding — flag it for `codebase-design`.

## Phase 6 — Cleanup

- [ ] Original loop is green
- [ ] Regression test exists or missing seam is documented
- [ ] `[DEBUG-...]` removed
- [ ] Throwaways deleted
- [ ] Winning hypothesis stated for the commit message (commit only if the user asks)
