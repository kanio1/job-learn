---
name: merchant-360-implement
description: >
  Implement Playwright Merchant 360 slices PW-M360-T01–T20 on the live stack.
  Use when the user says PW-M360-T*, Merchant 360 implement, or enables this
  skill as a Custom Mode for the milestone. Do not use for Ops Wave 2, CPL,
  or unrelated bugs.
disable-model-invocation: true
---

# Merchant 360 — implement one slice

Grok 4.6 / Cursor Agent. This skill is the **stable prefix**. Do not paste catalogs into chat. Read files with tools.

## Every turn (identical order)

1. `.codex/merchant-360-slice.md` — Next task id only.
2. `status/roadmaps/playwright-merchant-360/task-board.md` — that row + dependencies.
3. Matching epic under `status/roadmaps/playwright-merchant-360/epics/`.
4. Layer files listed on that epic (infra / catalog IDs). Do **not** open all 00–09 catalogs.
5. Then the layer skills below. Do not re-read this skill.

| Change | Read skill |
|---|---|
| Any test-first work | `tdd` then `implement` |
| Java / Flyway / REST Assured | `spring-modulith`, `rest-api-test-design` |
| Nuxt / Nitro / Zod | `nuxt-frontend` |
| `tests-pom` | `playwright-pom` (plan: `docs/testing/merchant-360-erp-lab/09-agent-tests-pom-plan.md`) |

Canonical FR/non-goals: `status/roadmaps/playwright-merchant-360/00-context-requirements.md` — only if the epic does not already state the FR.

## Non-negotiables

- Live stack only. No `page.route` / `route.fulfill` / HAR / MSW in `tests-pom`.
- Optimistic lock = **412** + `If-Match`. **409** = duplicate / idempotency only.
- No CRM entities (Customers/Deals). No fake Revenue/KPI. No Nuxt UI bump. No new Keycloak roles.
- Unique merchant reference stays **global**. Worker data: `uniqueMerchantReference` / `MERCHANT-W{n}`. Never Alpha ~104 seed as owner.
- Flyway owns schema. No `CREATE INDEX CONCURRENTLY`. JPA `ddl-auto: validate`.
- Modulith: public API at module root; no `*.internal` leaks; no `crm` module.
- Do not edit `.kiro/**`. Do not write `tests-pom-learner/`. Do not call `seed-learning` / ETL test APIs.
- Do not run `restkit/` or `paymentsupport/` tests.
- Do not commit unless the user asks.
- One `PW-M360-Txx` per user message. Stop when that slice’s AC are green or blocked. Do not start the next T.

## TDD seams (do not invert)

1. REST Assured red → Spring green (`apps/backend`, `./mvnw -Dtest=Class#method test`).
2. Playwright REST (`BffClient`) only after the Spring contract exists.
3. Live POM E2E last. 1 happy UI + HTTP observe; EP/BVA/DT matrices stay RA unless 09 says e2e.
4. Two-session tests = `PW-M360-SEC-*` via `browser.newContext({ storageState })`.

POM: `test`/`expect` from `tests-pom/fixtures/index.ts`. Locators: `getByRole` → `getByLabel` → `getByTestId`. Exact BFF pathname in `waitForBff` (never prefix-match `/history`).

## Verify (scoped)

- Backend: `working_directory` `/home/suso/job-learn/apps/backend`; command starts with `./mvnw`. No `all` / `full_network` on first try.
- Frontend TS: `corepack pnpm typecheck` and `corepack pnpm lint` from `apps/frontend` when Vue/TS changed.
- Live POM: only the spec named in the epic; stack must already be `scripts/dev-stack.sh --app` (or host DX). Passwords from env only.

## After the slice

Update **only**:

- `.codex/merchant-360-slice.md` (Last closed / Next / verify commands)
- the matching row in `status/roadmaps/playwright-merchant-360/task-board.md`

Reply with: closed id, files, commands run, Next id. Then stop.
