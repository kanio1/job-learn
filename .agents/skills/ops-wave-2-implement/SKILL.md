---
name: ops-wave-2-implement
description: >
  Implement Playwright Ops Wave 2 slices PW-OPS-T01–T22 on the live stack.
  Use when the user says PW-OPS-T*, Ops Wave 2 implement, or enables this
  skill as a Custom Mode. Do not use for Merchant 360, CPL, or unrelated bugs.
disable-model-invocation: true
---

# Ops Wave 2 — implement one slice

Grok 4.6. Stable prefix. Do not paste catalogs into chat.

## Every turn

1. `.codex/ops-wave-2-slice.md` — Next task id only.
2. `status/roadmaps/playwright-ops-wave-2/task-board.md` — that row + dependencies.
3. Matching epic under `status/roadmaps/playwright-ops-wave-2/epics/`.
4. Layer skills: `tdd` then `spring-modulith` / `nuxt-frontend` / `playwright-pom`.

## Non-negotiables

- Live stack only. No `page.route` / `route.fulfill` / `routeWebSocket` / HAR / MSW in `tests-pom`.
- Optimistic lock = **412** + `If-Match`. **409** = duplicate / illegal case transition / self-approve.
- Flyway versions increase with implementation: V31 contact, V32 support_cases, V33 challenges, V34 ops notifications. Gap V26–V30. No `CONCURRENTLY`.
- Support Kanban ≠ payment Kanban. Pin `@nuxt/ui` 4.7.1. No `UPinInput separator`.
- No `.kiro/**`. No `tests-pom-learner`. Skip `restkit/` and `paymentsupport/`. No commit unless asked.
- One `PW-OPS-Txx` per user message unless a `/goal` names a fala.

## After the slice

Update `.codex/ops-wave-2-slice.md` and the task-board row. Reply: closed id, files, commands, Next id.
