# Prompt — Merchant 360 review (Grok 4.6 + Cursor, high cache)

WIP review of **T01–T20** vs `HEAD` (working tree + untracked). Overlay: [`.codex/merchant-360-slice.md`](../merchant-360-slice.md) claims **DONE**. Agent loop: skill `merchant-360-review` → `code-review` + layer skills.

Same cache rules as [merchant-360-implement.md](merchant-360-implement.md): new chat, Custom Mode, paste once, no catalog dumps.

## Setup

1. **New** Agent chat. Grok 4.6 High. Do not reuse the implement thread (fat tool logs).
2. `/merchant-360-review` → **Use as Mode** (Alt+Enter).
3. Do not `@` `docs/testing/merchant-360-erp-lab/` as a folder.

## First message (paste once)

```text
Merchant 360 review. Follow skill merchant-360-review then code-review.

Fixed point: HEAD (uncommitted + untracked under apps/backend, apps/frontend, status/roadmaps/playwright-merchant-360, .codex/merchant-360-slice.md). Exclude .cursor/cli.json, permissions, Ops Wave 2, .kiro.

Spec: status/roadmaps/playwright-merchant-360/00-context-requirements.md, task-board, epics E1–E7 as needed, catalog 05/08/09 only for ID gaps.

Findings only. Two axes unmerged (Standards vs Spec, each <400 words), then layer notes (java-spring-review, rest-api-test-design, nuxt-frontend, playwright-sdet-review). Cite file + spec line. Blocker / should-fix / nit. Do not implement, commit, or start a new Txx.

Scope: PW-M360-T01–T20 as claimed DONE on the task-board.
```

## Follow-ups (same chat)

```text
Expand Playwright layer only. No product patch.
```

```text
Expand Spring/RA If-Match 412 vs 409 only.
```

```text
review-and-fix: blockers only, one PR-sized patch, then stop.
```

## Do not

- Ask the reviewer to re-run the full live POM suite unless you add that line.
- Paste FR tables or mermaid.
- Mix Ops Wave 2 into this review.
