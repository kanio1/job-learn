---
name: merchant-360-review
description: >
  Two-axis review of Playwright Merchant 360 WIP (T01–T20) vs HEAD.
  Use when the user asks to review M360 changes, PW-M360, or merchant-360
  task-board DONE. Findings only. Do not use to implement the next slice.
disable-model-invocation: true
---

# Merchant 360 — review (findings only)

Grok 4.6 / Cursor Agent. Follow `code-review` then layer skills. Do not merge Standards vs Spec. Do not implement, commit, or start a new `PW-M360-Txx`.

## Pin the diff

Fixed point: **`HEAD`** (working tree + untracked). Not `main` unless the user says so.

```bash
git rev-parse HEAD
git diff --stat HEAD -- apps/backend apps/frontend status/roadmaps/playwright-merchant-360 .codex/merchant-360-slice.md
git status --short -- apps/backend apps/frontend
```

Include untracked under those trees. **Exclude:** `.cursor/cli.json`, `.cursor/permissions.json`, `docs/testing/ops-wave-2-interaction-lab/`, `status/roadmaps/playwright-ops-wave-2/`, `.kiro/**`.

Empty M360 product diff → stop and say so.

## Spec (read, do not paste)

1. `status/roadmaps/playwright-merchant-360/00-context-requirements.md` (FR + non-goals)
2. `status/roadmaps/playwright-merchant-360/task-board.md` + `.codex/merchant-360-slice.md`
3. Epic for the finding (`epics/E1`…`E7`) — not all catalogs
4. IDs only as needed: `docs/testing/merchant-360-erp-lab/05-traceability.md`, `08-acceptance-tests.md`, `09-agent-tests-pom-plan.md`

## Every turn

1. `code-review` (two axes, under 400 words each)
2. Layer files actually in the diff:
   - `apps/backend/**` production → `java-spring-review` + `spring-modulith`
   - `**/rest/**` or new HTTP → `rest-api-test-design`
   - Vue/Nitro → `nuxt-frontend`
   - `tests-pom/**` → `playwright-sdet-review` (placement already `playwright-pom`)

Hot checks (cite file + spec line):

- Optimistic lock **412** + `If-Match`; **409** only duplicate/idempotency
- No `page.route` / fulfill in `tests-pom`
- Merchant list page DTO `content`/`totalElements`; payment `status` full enum + `sort=amountMinor`
- Unique reference **global**; no fake Revenue; no Nuxt UI bump; no new Keycloak roles
- Flyway owns schema, no `CONCURRENTLY`; JPA validate; Modulith (no `merchant.internal` from payment; tenant public API only)
- Live POM: unique data, `getByRole`, exact BFF path in `waitForBff`

## Output

```markdown
## Standards
## Spec
## Layer notes (Spring / REST Assured / Playwright)
```

Per axis: finding count + worst issue. Severity: blocker / should-fix / nit. Quote spec or `AGENTS.md`. Findings only — no patch unless the user says review-and-fix.
