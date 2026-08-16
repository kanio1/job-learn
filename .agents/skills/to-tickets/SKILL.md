---
name: to-tickets
description: Break a plan, spec, or conversation into tracer-bullet tickets with blocking edges. Use when splitting work into vertical slices for implementation.
disable-model-invocation: true
---

# To Tickets

Break work into **tracer-bullet** tickets — narrow complete paths, each declaring what **blocks** it.

## Process

### 1. Gather context

Use the conversation and any path/spec the user named.

### 2. Explore if needed

Use glossary language. Look for prefactors ("make the change easy, then make the easy change").

### 3. Draft vertical slices

- Each slice cuts through the needed layers (schema, API, UI, tests) — not one horizontal layer
- Demoable or verifiable alone
- Fits one fresh context window
- Prefactors first

**Wide refactors** (rename a column, shared type) are expand → migrate in batches → contract. Do not force them into a fake tracer bullet.

Each ticket lists **Blocked by**.

### 4. Quiz the user

For each ticket: title, blocked by, what it delivers. Ask granularity, edges, merge/split. Iterate until approved.

### 5. Publish locally

Write one file per ticket under `.codex/tickets/<feature-slug>/<NN>-<slug>.md`, numbered from `01`, blockers first. Tracker: `docs/agents/issue-tracker.md`.

```markdown
# <NN> — <Title>

**What to build:** end-to-end behaviour from the user's perspective.

**Blocked by:** numbers/titles, or "None — can start immediately".

**Seams:** REST Assured / Playwright REST / Playwright E2E / unit

**Status:** ready-for-agent

- [ ] Acceptance criterion 1
- [ ] Acceptance criterion 2
```

Do not put stale file paths in tickets. Do not close parent specs. Do not implement until the user asks.

Next implementation step is `implement` on a ticket whose blockers are done.
