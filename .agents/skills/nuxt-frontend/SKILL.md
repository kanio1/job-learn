---
name: nuxt-frontend
description: >-
  Place and change Nuxt 4.4.6 / TypeScript 6.0.3 / Node 22+ (Nitro BFF) frontend
  code in this lab: app/ vs server/, server/api proxy, $fetch.raw, Zod-before-render,
  Pinia only for shared state, Nuxt UI first. Use when adding or editing Vue pages,
  components, composables, schemas, stores, or Nitro handlers. Do not use as a generic
  Nuxt tutorial, Playwright framework skill (that is playwright-pom / playwright-sdet-review),
  or a dashboard-taste pass (that is nuxt-dashboard-zod-pinia-frontend-engineering).
---

# Nuxt frontend (this lab)

Build Vue/Nitro the way this repo already does. The agent already knows Nuxt; this skill is the **lab arrangement**.

Pinned from `apps/frontend/package.json` — do not bump:

| Piece | Version |
|---|---|
| Nuxt | 4.4.6 (`app/` directory) |
| Vue | 3.5.x |
| TypeScript | 6.0.3 (`vue-tsc` via `nuxt typecheck`) |
| @nuxt/ui | 4.7.1 |
| Pinia | 3.0.4 / `@pinia/nuxt` 0.11.3 |
| Zod | 4.4.3 |
| pnpm | 11 (Corepack) |
| Playwright | 1.61.0 (tests — not this skill) |
| Node | 22+ even LTS (`^22.12.0 \|\| ^24.11.0 \|\| >=26.0.0`) |

`package.json` is `"type": "module"`. Use even Node versions. Nuxt’s installer also shows Bun/Deno; **this lab stays on Node + Corepack pnpm**.

## Compose

| Job | Skill |
|---|---|
| Red-green at UI / BFF HTTP seams | `tdd` then `implement` |
| Playwright POM / fixtures | `playwright-pom` |
| Playwright locators / auth / flake | `playwright-sdet-review` |
| Dashboard visual taste, density, a11y polish | `nuxt-dashboard-zod-pinia-frontend-engineering` + `.kiro/steering/frontend-nuxt-ui.md` |
| Deep-module vocabulary | `codebase-design` |
| Version-sensitive fact check | `research` + `official-docs-and-versioned-research` |

## Workflow

1. Name the **screen or BFF route**. Pages live under `app/pages`. Proxy handlers live under `server/api` with a method suffix (`index.get.ts`, `authorize.post.ts`).
2. Browser never calls Spring. Add or reuse a Nitro handler that uses `backendApi` from `server/utils/backendApi.ts`.
3. Client transport goes through `useApiClient` (`$fetch.raw` on `/api/**`) and a Zod schema in `app/schemas/`. On parse failure: `ErrorState`, no unvalidated data.
4. Pinia only when two surfaces share state (see [layout.md](layout.md)). Otherwise composable + local state.
5. Prefer existing Nuxt UI / `app/components/shared/` widgets. Do not invent KPI tiles.
6. After TS/Vue changes: `corepack pnpm typecheck` and `corepack pnpm lint` in `apps/frontend`. Then the agreed `tdd` seam.

Package map: [layout.md](layout.md). BFF and headers: [bff.md](bff.md). Language/runtime: [typescript-node.md](typescript-node.md).

## Lab mappings

| Vocabulary | Here |
|---|---|
| External seam | Nuxt BFF `/api/**` → Spring `/api/**` |
| App context | `app/` (pages, components, composables, schemas, stores) |
| Shared context | `shared/` — types/utils used by both Vue and Nitro (`shared/types/auth.d.ts`) |
| Server context | `server/` (Nitro / h3). Do not import Vue composables here |
| Adapter | `backendApi` + sealed `nuxt-auth-utils` session (`secure.accessToken`) |
| Contract oracle | Zod schemas — do not widen them to match loose prose |

## Defaults (one choice)

- `/admin/**` is CSR (`routeRules` `ssr: false`). Do not flip that for dashboard screens.
- Token stays in the sealed server session. Never in Pinia, `localStorage`, DOM, or debug panels (mask `Authorization`).
- Forward only the allowlist in `forwardBackendHeaders` (`ETag`, `Location`, `Vary`, `Cache-Control`, `X-Correlation-ID`, …). Never forward `Authorization`.
- Commands from `apps/frontend` via Corepack: `corepack pnpm typecheck`, `corepack pnpm lint`, `corepack pnpm test:unit`, `corepack pnpm dev`.

## When not to use

- Backend Java/Spring (`spring-modulith`).
- Playwright suite design (`tdd`, `playwright-pom`, `playwright-sdet-review`). After TS edits in `apps/frontend`: `corepack pnpm lint` (vendored anti-slop).
- Inventing a SPA that talks to `localhost:8080` from the browser.
- Upgrading to Nuxt 5, setting `future.compatibilityVersion: 5`, or adding TypeScript 7 native preview.
