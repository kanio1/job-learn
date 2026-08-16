---
name: teach
description: Teach a concept over multiple sessions in a stateful workspace under .codex/teach. Use when the user wants to learn (REST Assured, Playwright, lifecycle, HTTP contracts), not when they want production code implemented.
disable-model-invocation: true
---

# Teach

Stateful teaching. Do not implement product features here — use `implement` / `tdd` for that. Durable vault notes still go through `obsidian-learning-os` when the user wants them in the knowledge vault.

## Workspace

`.codex/teach/<topic-slug>/`:

- `MISSION.md` — why they are learning this (SDET / this lab)
- `RESOURCES.md` — high-trust primary sources only
- `NOTES.md` — teaching preferences
- `lessons/0001-<slug>.md` — one tightly scoped lesson each
- `learning-records/0001-<slug>.md` — non-obvious insights
- `reference/` — cheat sheets (markdown)

Create the directory lazily.

## Philosophy

- Knowledge from primary sources (`research` / Firecrawl / official-docs), not parametric memory for versions.
- Skills via short lessons with a tight feedback loop (a failing-then-passing test in **this** repo is better than a quiz).
- Storage strength: retrieval, spacing — not a fluency dump.

Each lesson: one win, in the zone of proximal development, tied to the mission, cites a primary source, reminds them to ask follow-ups. Prefer exercises against real seams: REST Assured class, Playwright E2E, Playwright REST.

## First session

If `MISSION.md` is empty, grill why they want this (interview? current payment-order gap? HTTP headers?). Then one lesson, not a curriculum dump.

## Compose

- `implementation-learning-loop` — explain a **real diff** after `implement`
- `java-rest-api-testing-effective-java-mentor` / `junit6-assertj-restassured-testcraft` / `typescript6-playwright-engineering` — domain depth inside a lesson
- `research` — gather sources into `RESOURCES.md`
