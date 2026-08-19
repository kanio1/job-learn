# Playwright POM skills vs lab needs; vendored anti-slop

## Answer

Public Playwright “POM skills” are either generic tutorials, cloud-vendor packs, or on-the-fly MCP replacements. None of them encode this lab’s live `tests-pom` arrangement (no `route.fulfill`, App facade, Keycloak storageState, BffClient, ISTQB `methods/`).

The missing lab skill was **implementation placement** (the Nuxt/Spring analogue). Review (`playwright-sdet-review`) and process (`tdd`) already existed. The stub `typescript6-playwright-engineering` was not that skill.

`dmmulroy/anti-slop` is an Oxlint plugin, not a POM skill. It is now vendored under `apps/frontend/tools/oxlint/anti-slop/` with a **lab rule policy** (five error rules; the rest warn). Effect rules are omitted.

## Why it matters here

Agents writing page objects were pointed at a 17-line stub plus a review skill that forbids editing. Public packs would teach Screenplay, fat BasePage, assertions-in-POM, `page.route` in “E2E”, and LambdaTest grids — all in conflict with `tests-pom/README.md`.

## Project impact

- New process skill: `.agents/skills/playwright-pom/`
- Stub `.kiro/skills/typescript6-playwright-engineering` now points at it
- Do **not** `npx skills add testdino-hq/playwright-skill` or LambdaTest / lackeyjb packs
- Frontend lint: `corepack pnpm lint` (Oxlint 1.78.0 + `@oxlint/plugins` 1.78.0)

## Test impact (REST Assured / Playwright REST / Playwright E2E)

- Playwright live POM: follow `playwright-pom` then `tdd`
- Three chained-`as` sites removed so error-tier anti-slop stays green (`users/index.test.ts`, `audit.spec.ts`, `useApiClient.property.test.ts`)
- REST Assured unchanged

## Inventory

### Keep (lab)

| Skill | Role |
|---|---|
| `playwright-pom` | write / place POM |
| `playwright-sdet-review` | review locators, flake, auth |
| `tdd` | red-green |
| `nuxt-frontend` | product Vue/Nitro |
| playbook `04-principal-typescript.md` | named patterns |
| `tests-pom/` | canon |
| `LESSON-08` | human mentoring |

### Redundant to import

| Source | Why skip |
|---|---|
| [testdino-hq/playwright-skill](https://github.com/testdino-hq/playwright-skill) (~70 guides, `npx skills add …/pom`) | Generic; contradicts itself on assertions-in-POM; teaches mocking as default |
| [LambdaTest/agent-skills playwright-skill](https://github.com/LambdaTest/agent-skills/blob/main/playwright-skill/SKILL.md) | Cloud grid + fat BasePage; vendor lock |
| [lackeyjb/playwright-skill](https://github.com/lackeyjb/playwright-skill) | On-the-fly scripts (HN: MCP replacement), not a suite |
| [Jeffallan/claude-skills playwright-expert](https://github.com/Jeffallan/claude-skills/blob/main/skills/playwright-expert/SKILL.md) | Generic E2E |
| Currents “Playwright Best Practices” skill | Same generic surface as TestDino core |
| [padmarajnidagundi POM+MCP](https://github.com/padmarajnidagundi/Playwright-AI-Agent-POM-MCP-Server) | Whole template + MCP, not a lab skill |
| Screenplay skills | Explicitly out of scope in LESSON-08 |
| Upstream `install-anti-slop` skill | One-shot; already vendored |

### Still thin (not this change)

- `parallel-test-architecture-and-data-isolation` — still a stub pointing at missing `.kilocode` refs
- Official Playwright docs still show `expect()` inside page objects — lab exception is `expectLoaded()` plus page-load oracles

## Sources

- [Playwright POM docs](https://playwright.dev/docs/pom) — official helper-class example; assertions inside the page object
- [TestDino POM guide](https://raw.githubusercontent.com/testdino-hq/playwright-skill/main/pom/page-object-model.md) — intent methods, fixtures, component objects
- [TestDino POM vs fixtures vs helpers](https://raw.githubusercontent.com/testdino-hq/playwright-skill/main/pom/pom-vs-fixtures-vs-helpers.md) — hybrid; then puts `expect` on CheckoutPage
- [LambdaTest POM reference](https://raw.githubusercontent.com/LambdaTest/agent-skills/main/playwright-skill/reference/page-object-model.md) — BasePage + cloud
- [dmmulroy/anti-slop](https://github.com/dmmulroy/anti-slop) commit `6d538555cb151d4121ed51a27db81890eacf8ae9` — vendor, not npm
- X keyword search `playwright skill POM OR "page object"` (Latest) — no dedicated POM skill posts; semantic hits were generic skill-design, not Playwright
- This repo: `apps/frontend/tests-pom/README.md`, `docs/testing/playwright-method-playbook/04-principal-typescript.md`

## Uncertainty / follow-up

- Context7 Oxlint docs: `Invalid API key`. Versions taken from anti-slop `package.json` (oxlint / `@oxlint/plugins` **1.78.0**).
- Warn-tier anti-slop is noisy on Zod/`$fetch`/`vi.mock`. Promote a rule to error only after a cleanup wave.
- `~/.claude/skills/typescript6-playwright-engineering` is a pointer; remove later if it still auto-invokes beside `playwright-pom`.
