---
name: playwright-cli
description: >
  Drive a live browser with Microsoft @playwright/cli (playwright-cli): snapshot,
  click, fill, screenshot against the lab dashboard or a URL. Use when exploring
  UI, discovering locators, checking what the page actually shows, or the user
  mentions playwright-cli / Playwright CLI. Do not use to write tests-pom, run
  the 1.61 suite (that is tdd + playwright-pom), or as a Playwright MCP replacement.
---

# Playwright CLI (this lab)

Token-efficient browser control for coding agents. Adapted from [microsoft/playwright-cli](https://github.com/microsoft/playwright-cli) (Apache-2.0). The package lives in `tools/playwright-cli` so it does **not** share `node_modules` with `apps/frontend` (`@playwright/test` **1.61.0**).

Pinned: `@playwright/cli` **0.1.18**. Do not bump it as a side effect of frontend work. Do not bump `@playwright/test`.

## Invoke (repo root)

```bash
corepack pnpm --dir tools/playwright-cli exec playwright-cli <subcommand>
```

If that fails: `corepack pnpm --dir tools/playwright-cli install`, then `corepack pnpm --dir tools/playwright-cli exec playwright-cli install-browser chromium`.

Session name for this repo: `-s=job-learn`. Dashboard: `http://localhost:3000`. Chromium only unless the user asks otherwise.

## Workflow

1. `open` the URL (add `--headed` only when the user needs to see the window).
2. `snapshot` (or `find "visible text"`) and use **refs** (`e12`) for the next action.
3. `click` / `fill` / `press` / `check`. Prefer refs; otherwise Playwright locators (`getByRole`, `getByLabel`, `getByTestId`) — not CSS/XPath.
4. `snapshot` again after navigation or a mutation.
5. `close` the named session when finished (`-s=job-learn close`). Use `close-all` only if you opened extras.

```bash
corepack pnpm --dir tools/playwright-cli exec playwright-cli -s=job-learn open http://localhost:3000
corepack pnpm --dir tools/playwright-cli exec playwright-cli -s=job-learn snapshot
corepack pnpm --dir tools/playwright-cli exec playwright-cli -s=job-learn click e15
corepack pnpm --dir tools/playwright-cli exec playwright-cli -s=job-learn close
```

`playwright-cli --help` is the command list. Do not paste a password. If Keycloak login is required: `--headed` and ask the user, or `state-load` a **local** storageState they already have. Never commit `.playwright-cli/`, `state-save` files, or `tests-pom/.auth/*.json`.

## Lab rules while driving the app

- Live product oracle: do **not** `route` / `unroute` against the lab dashboard or BFF.
- Do not treat CLI exploration as a tests-pom spec. After locator discovery, write the test with `playwright-pom`.
- Do not run the POM suite through this CLI. From `apps/frontend`: `corepack pnpm exec playwright test --config playwright.pom.config.ts`.

## Compose

| Job | Skill |
|---|---|
| Write / place live POM tests | `playwright-pom` + `tdd` |
| Locator / auth / flake review of a diff | `playwright-sdet-review` |
| Product Vue / Nitro | `nuxt-frontend` |

## When not to use

- Authoring or running `tests-pom` / `tests-pom-learner` (`tdd`, `playwright-pom`).
- Replacing `@playwright/test` 1.61.0 or adding `@playwright/cli` under `apps/frontend`.
- Playwright MCP (`@playwright/mcp` in `.mcp.json`) — this CLI is the agent browser path.
- Cloud grids, Firefox/WebKit, or mobile emulation unless the user asks.
- Global `npm install -g @playwright/cli` — this lab keeps the tool in `tools/playwright-cli`.
