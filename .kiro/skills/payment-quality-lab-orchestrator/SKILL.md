---
name: payment-quality-lab-orchestrator
description: Use when coordinating the full Payment Quality Engineering Lab so that implementation, tester-focused learning, business analysis, spec work, Spring Modulith design, Obsidian, risk thinking, and parallel-ready quality engineering remain aligned.
---

# Payment Quality Engineering Lab Orchestrator

## Use when
- starting or reviewing a project phase,
- coordinating multiple specialist skills,
- turning a broad learning/product goal into an execution path,
- deciding what becomes Business Analysis, a `.codex` spec/tickets, implementation or tester learning.

## Project posture
- **Business Analysis clarifies substantial product capabilities before a written spec.**
- **New work uses engineering process skills**: grill → (wayfinder if foggy) → `to-spec` → `to-tickets` → `implement`/`tdd`.
- Tracker: local markdown under `.codex/` (`docs/agents/issue-tracker.md`).
- **User learns through quality engineering**:
  - requirements analysis,
  - risk analysis,
  - test design,
  - test data design,
  - REST/API testing,
  - Spring integration and module tests,
  - Playwright,
  - parallel-ready framework strategy,
  - interview-quality explanation.

## Product Discovery Gate
Before a substantial new feature goes to `to-spec`, route through:
- `business-analysis-and-product-discovery-for-payment-lab`

Expected output:
- BA Discovery Pack,
- business goal, actors, workflow, business rules, data needs,
- candidate acceptance criteria,
- ambiguities/open questions,
- feature sequencing recommendation.

This gate may be skipped only for:
- tiny non-product technical work,
- documentation-only adjustments,
- obvious refactors with no product ambiguity.

## Standard task labels
- `[IMPLEMENT]`
- `[EXPLAIN]`
- `[TESTER-ANALYZE]`
- `[TESTER-DESIGN]`
- `[TESTER-AUTOMATE]`
- `[REVIEW]`
- `[DISCUSS]`

## Engineering process skills

Canonical copies live in `.agents/skills/`. Route building/review/test-writing through them:

| Intent | Process skill |
|---|---|
| Align before building | `grill-me` / `grill-with-docs` |
| Foggy multi-session work | `wayfinder` |
| Incoming bugs/requests | `triage` |
| Write spec / tickets | `to-spec` / `to-tickets` |
| Implement test-first | `implement` → `tdd` |
| Logic/UI question | `prototype` |
| Primary-source reading | `research` |
| Human-only setup | `wizard` |
| Learn over sessions | `teach` |
| REST Assured / Playwright REST / Playwright E2E tests | `tdd` + domain test skills |
| Review a diff | `code-review` + layer review skills |
| Hard bug | `diagnosing-bugs` |

Index: `.agents/skills/README.md`.

## When Not to Use
Do not use this for a narrow local question with an obvious specialist skill.
