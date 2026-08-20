---
name: playwright-pom
description: >
  Place and change Playwright 1.61.0 + TypeScript 6 Page Object Model tests in this lab:
  live tests-pom (App facade, fixtures, BffClient, factories), mocked tests/e2e, learner
  copies. Use when adding or editing page objects, fixtures, Playwright specs, storage
  state, or POM design patterns. Do not use as a generic Playwright tutorial, a review-only
  pass (that is playwright-sdet-review), product Nuxt placement (nuxt-frontend), or
  REST Assured design (rest-api-test-design).
---

# Playwright POM (this lab)

Build Playwright the way `apps/frontend/tests-pom` already does. The agent already knows Playwright; this skill is the **lab arrangement**.

Pinned from `apps/frontend/package.json` — do not bump:

| Piece | Version |
|---|---|
| Playwright | 1.61.0 |
| TypeScript | 6.0.3 |
| Node | 22+ even LTS |

Canon: `apps/frontend/tests-pom/`. Learner copies live in `tests-pom-learner/` (`My*` files). Do not import page objects from `tests-pom` into the learner tree.

## Compose

| Job | Skill |
|---|---|
| Red-green at the Playwright seam | `tdd` then this skill |
| Live browser / locator discovery | `playwright-cli` |
| Locator / auth / flake review | `playwright-sdet-review` |
| Product Vue / Nitro | `nuxt-frontend` |
| TypeScript evidence (`as unknown as T`) | Oxlint anti-slop — `corepack pnpm lint` in `apps/frontend` |
| ISTQB method classes | `docs/testing/playwright-method-playbook/` |

## Where the test lives

| Suite | Path | Network | Auth |
|---|---|---|---|
| Live POM (product) | `tests-pom/` | real BFF + Spring; **no** `page.route` / `route.fulfill` | Keycloak `storageState` |
| Learner | `tests-pom-learner/` | same as live | own `.auth/` |
| Vitest | `tests/unit/`, colocated `*.test.ts` | none | none |

Do not add a second POM tree. Do not call `POST /api/test/seed-learning` or `/api/test/etl/payments/*`.

## Workflow

1. Name the **screen or journey**. One class per route in `tests-pom/pages/`. Shared widgets go in `tests-pom/pages/components/`.
2. Import `test` / `expect` from `tests-pom/fixtures/index.ts`, not `@playwright/test`.
3. Drive the UI through `app.<page>.<intent>()` (`App` facade). Preconditions that mutate data go through `BffClient` + `data/factories.ts`, not extra clicks.
4. Locators: `getByRole` → `getByLabel` → `getByPlaceholder` → `getByTestId`. No CSS/XPath unless a third-party widget has no accessible name.
5. Specs describe **what** the actor should see. Page objects describe **how** to operate the screen. No `if (role === …)` in a page class.
6. After TS edits: `corepack pnpm lint` then the agreed Playwright file from `apps/frontend`.

Patterns and anti-patterns: [patterns.md](patterns.md). Lab rules of the tree: `apps/frontend/tests-pom/README.md`.

## Defaults (one choice)

- Cienki `BasePage`: `goto`, overlay guard, abstract `expectLoaded()`. No business methods on the base.
- Fixture DI: `{ app, api }`. Guest project must not destructure `api` — use `requireApi(api)` in authenticated specs.
- Assertions: journey outcomes in `specs/`. POM may own `expectLoaded()` and page-specific load/access oracles (`expectAccessDenied`, `expectOpen` on a modal). Do not hide the test’s business claim inside the page class.
- Return `Locator`, not `Promise<Locator>`. Dynamic rows are methods (`rowByReference(text)`), not constructor fields.
- Unique data: `uniqueMerchantReference(testInfo)` / `uniqueOrderReference(testInfo)`.
- Passwords only from env (`PLAYWRIGHT_*_PASSWORD`). Never commit `tests-pom/.auth/*.json`.
- ConfirmModal dismiss: `data-testid="confirm-action-dismiss"` — never a button named Cancel.
- Hosted checkout is a **new tab**, not an iframe.
- Header oracles (`Idempotency-Key`, `If-Match`, `ETag`): `page.waitForRequest` / `waitForBff`, never mocks.

## When not to use

- Reviewing a diff without writing tests (`playwright-sdet-review`).
- Live browser exploration or locator discovery (`playwright-cli`). Do not run the POM suite through that CLI.
- Installing a public 70-guide Playwright skill pack (TestDino, LambdaTest cloud, on-the-fly MCP replacements).
- Screenplay, fat BasePage, god `AppPage`, assertions-only locator bags.
- Cross-browser / mobile / cloud grids — this lab is Chromium + live stack.
