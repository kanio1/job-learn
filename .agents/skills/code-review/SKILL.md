---
name: code-review
description: Two-axis review of the diff since a fixed point — Standards vs Spec — then layer-specific REST Assured / Playwright / Spring review. Use when reviewing a branch, PR, WIP diff, or when asked to review since a commit.
---

# Code Review

Two-axis review of `HEAD` vs a fixed point, then lab layer reviews. Do not merge the axes.

## 1. Pin the fixed point

Use what the user named (SHA, branch, tag, `main`). If missing, ask.

```bash
git rev-parse <fixed-point>
git diff <fixed-point>...HEAD
git log <fixed-point>..HEAD --oneline
```

Fail here if the ref is bad or the diff is empty.

## 2. Spec source (in order)

1. Path the user passed
2. `.codex/specs/`, `.codex/tickets/`, `docs/testing/**`, `status/roadmaps/**`
3. Historical prior art only: `specs/`, `.kiro/specs/**`
4. If none: Spec axis reports "no spec available"

## 3. Standards sources

- `AGENTS.md`
- `.kiro/steering/tech.md`, `structure.md`, `rest-api-contracts.md`, `testing-strategy.md`, `frontend-nuxt-ui.md`
- `.cursor/rules/maven-shell-approvals.mdc` for how backend tests are run

Repo standards override the smell baseline below. Smells are judgement calls. Skip anything tooling already enforces.

Smell baseline (Fowler): Mysterious Name, Duplicated Code, Feature Envy, Data Clumps, Primitive Obsession, Repeated Switches, Shotgun Surgery, Divergent Change, Speculative Generality, Message Chains, Middle Man, Refused Bequest.

Lab-specific standards to always check:

- Modulith: no `payment` → `merchant.internal`; public API at module root
- Flyway owns schema; JPA validates
- REST contract stability (status, problem+json, headers only where implemented)
- Unique test data; no secrets in tests
- No PSP/settlement/KYC/top-level `POST /payments` scope creep; Kafka only in `eventlab` / overlay (ADR 0002). Flag a Kafka UI clone or lag dashboard — Lenses is the telescope.

## 4. Parallel axes

Run Standards and Spec as separate passes (sub-agents if available) so neither pollutes the other.

**Standards** — per hunk: documented-standard breach (cite file + rule) vs baseline smell (name it, quote hunk). Under 400 words.

**Spec** — (a) missing/partial requirements, (b) scope creep, (c) implemented but wrong. Quote the spec line. Under 400 words.

## 5. Layer reviews (only for files in the diff)

| Diff touches | Also follow |
|---|---|
| `apps/backend/**` production | `java-spring-review` (placement rules: `spring-modulith`) |
| `apps/backend/**/rest/**` or new HTTP behavior | `rest-api-test-design` |
| `apps/frontend/tests/**`, `tests-pom/**` | `playwright-pom` (placement) then `playwright-sdet-review` |
| Vue/Nuxt product files | `nuxt-frontend` then `.kiro/steering/frontend-nuxt-ui.md` |

These layer reviews add findings; they do not rerank Standards vs Spec.

## 6. Aggregate

Present:

```markdown
## Standards
## Spec
## Layer notes (Spring / REST Assured / Playwright)
```

Do not merge or pick a winner across axes. End with counts per axis and the worst issue **within each axis**.

This skill may implement fixes only when the user asked for a review-and-fix pass; default is findings only.

## When not to use

- Reviewing a single layer in isolation (use `java-spring-review` / `playwright-sdet-review` / `rest-api-test-design` instead).
- Implementing features; this skill is findings-only unless the user asked for a review-and-fix pass.
