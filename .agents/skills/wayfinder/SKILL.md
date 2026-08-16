---
name: wayfinder
description: Plan work too big for one session as a map of decision tickets under .codex/wayfinder, resolving them one at a time until the route is clear.
disable-model-invocation: true
---

# Wayfinder

A loose idea is too big for one session and wrapped in fog. Chart a **shared map** of **decision tickets**, then resolve them one at a time. Produce **decisions, not deliverables**, until the way is clear.

Tracker: `docs/agents/issue-tracker.md`.

## Plan, don't do

Each ticket resolves a **decision**. When the map is clear, hand off to `to-spec` → `to-tickets` → `implement`. Do not loop the map straight into `implement` unless the effort turned out small.

Never resolve more than one ticket per session, except parallel `research` tickets.

Refer to maps and tickets **by title**, not bare numbers.

## Map body

`.codex/wayfinder/<effort>/MAP.md`:

```markdown
## Destination
## Notes
## Decisions so far
## Not yet specified
## Out of scope
```

Open tickets are **not** listed on the map — they live as child files.

## Ticket types

- **Research** (AFK) → `research` skill (Firecrawl + primary sources)
- **Prototype** (HITL) → `prototype`
- **Grilling** (HITL) → `grilling` (and `grill-with-docs` if terms shift)
- **Task** — manual work that unblocks a decision (often `wizard`)

## Fog vs ticket

Ticket when the **question** is already sharp. **Not yet specified** when you cannot phrase it yet. Out of scope is beyond the destination — close those tickets, do not keep them on the frontier.

## Chart the map

1. Grill to name the destination (fixes scope).
2. Grill breadth-first for the first takeable decisions. If there is no fog, stop — use `grill-with-docs` → `to-spec` instead.
3. Write `MAP.md`. Create tickets you can specify now; wire `Blocked by` in a second pass.
4. Fire `research` tickets in parallel.
5. Stop. Charting does not resolve tickets.

## Work through the map

1. Load `MAP.md` only.
2. Take the named ticket, or the first frontier ticket. Claim it (`Status: claimed`).
3. Resolve with the matching skill.
4. Write `## Answer`, set `resolved`, gist it onto Decisions so far.
5. Graduate fog into new tickets; rule mis-scoped tickets out of scope.

Lab non-goals (PSP, Kafka, settlement, KYC, top-level `POST /payments`) belong in **Out of scope** unless the user redraws the destination.
