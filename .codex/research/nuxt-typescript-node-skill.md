# How to author a Nuxt 4 + TypeScript 6 + Node skill for this lab

## Answer

Author one **model-invoked** process skill at `.agents/skills/nuxt-frontend/` (Agent Skills spec: `name` + `description`, progressive disclosure, SKILL.md under 500 lines, extra files one level deep — no `references/` folder needed at this size). Pin **this repo’s versions**, not “latest Nuxt”: Nuxt 4.4.6, TypeScript 6.0.3, Vue 3.5, Nuxt UI 4.7.1, Zod 4.4.3, Pinia 3, pnpm 11, Node **22+ even LTS** (`^22.12.0 || ^24.11.0 || >=26.0.0`).

The skill is for **placing production frontend and Nitro BFF code**. Playwright stays with `tdd` / `playwright-sdet-review`. Visual dashboard taste stays with `nuxt-dashboard-zod-pinia-frontend-engineering`.

## Why it matters here

Agents already know Vue. They do not know this lab’s BFF (`server/api` + `backendApi`), `$fetch.raw`, Zod-before-render, token never in the browser, `/admin/**` CSR, or TypeScript 6 defaults that Nuxt already generates. Those facts belong in a skill so `implement` does not re-learn them from steering docs every time.

## Project impact

- Canonical copy: `.agents/skills/nuxt-frontend/`
- Consumed via symlinks from `.cursor/skills/` and `.opencode/skills/`
- Composed by `implement`; reviewed later with `playwright-sdet-review` (tests) and `.kiro/steering/frontend-nuxt-ui.md` (UI)

## Test impact (REST Assured / Playwright REST / Playwright E2E)

HTTP/UI seams stay with `tdd`. This skill only owns product placement and `corepack pnpm typecheck` / Vitest for colocated unit tests. Do not move Playwright coverage here.

## Sources

- [Nuxt 4 installation](https://nuxt.com/docs/4.x/getting-started/installation) — Node.js 22.x or newer, even numbered versions
- [Nuxt 4 `server/`](https://nuxt.com/docs/4.x/directory-structure/server) — `defineEventHandler`, method suffixes, do not mix Vue and Nitro, `#server` alias
- [Nuxt 4 TypeScript](https://nuxt.com/docs/4.x/guide/concepts/typescript) — `nuxt typecheck`, project references (`tsconfig.app` / `tsconfig.server`), do not hand-edit root `tsconfig.json`
- [Announcing TypeScript 6.0](https://devblogs.microsoft.com/typescript/announcing-typescript-6-0/) — last JS-based compiler; `strict` default true; deprecations (`import assert`, `moduleResolution node`, `baseUrl`); do not jump to TS 7 native preview
- This repo: `apps/frontend/package.json`, `nuxt.config.ts`, `server/utils/backendApi.ts`, `app/composables/useApiClient.ts`, `.kiro/steering/frontend-nuxt-ui.md`, `specs/002-merchant-registry-activation/quickstart.md`

## Uncertainty / follow-up

- **Context7** still returns `Invalid API key`. Facts are from Firecrawl + `package.json`.
- Playwright in `package.json` is **1.61.0**; some older README lines still say 1.60. Skill pins 1.61.0.
- Live Nuxt HTML is 4.x docs for this tree’s 4.4.6 — do not upgrade to Nuxt 5.
