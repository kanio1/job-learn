---
name: ask-engineering-flow
description: Route which engineering skill or flow to use. Use when the user asks how to start, which skill fits, or mentions grill, spec, tickets, TDD, triage, prototype, research, wizard, or review.
disable-model-invocation: true
---

# Ask Engineering Flow

Pick **one** next skill. Do not start implementing.

## Main flow: idea → ship

1. **`grill-with-docs`** in this repo (paper trail). Follow the plain `grilling` primitive only outside a working directory.
2. If a question needs a runnable answer → **`prototype`** (logic HTML or Nuxt UI variants). `handoff` if it needs a fresh session.
3. Multi-session build? **`to-spec`** then **`to-tickets`**, then **`implement`** per ticket (drives `tdd`, then `code-review`). Small enough? **`implement`** in this window.
4. Tracker is local markdown: `docs/agents/issue-tracker.md` (`.codex/specs`, `.codex/tickets`).

Keep grill → spec → tickets in **one** window. Each `implement` starts fresh from the ticket.

## On-ramps

- Incoming bugs/requests you did **not** create with `to-tickets` → **`triage`**
- Hard bug / flake / regression → **`diagnosing-bugs`**
- Foggy effort bigger than one session → **`grill-with-docs`** + parallel **`research`** until decisions are named; then `to-spec`, not straight to `implement`.
- Unsure of a versioned API fact → **`research`** (Firecrawl + official docs)

## Standalone

| Need | Skill |
|---|---|
| Review a diff | `code-review` |
| Test-first without a spec | `tdd` |
| Human-only clicks (Keycloak, mkcert, secrets) | `wizard` |
| Session must stop | `handoff` |
| Module shape / seam | `codebase-design` |

## Lab constraints

- Standing non-goals: see `AGENTS.md` (Kafka only in `eventlab` overlay; no PSP/settlement/KYC/top-level `POST /payments`)
- Seams: REST Assured, Playwright REST, Playwright E2E
- Do not commit unless the user asks

State the chosen skill and why, then wait for confirmation unless they already asked you to run it.
