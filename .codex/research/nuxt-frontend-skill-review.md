# Review of `nuxt-frontend` against Nuxt 4 / TypeScript 6 / ofetch docs

## Answer

The skill is the right shape (lab arrangement, pinned versions, compose, one-level extras). Most Nuxt 4 / Nitro / ofetch claims hold. Two factual mistakes should be fixed before agents rely on `typescript-node.md` and `layout.md`: the TypeScript 6 `this`-less inference note is inverted, and dual-context types belong in `shared/types/` (already used in this repo), not `app/types`.

Context7 remained `Invalid API key` after `mcp_auth`. Claims below are from Firecrawl scrapes of official pages plus `apps/frontend`.

## Why it matters here

Agents will follow this skill when placing Vue/Nitro code. A wrong type-location rule will fight Nuxt 4 project references. A wrong TS 6 inference rule will generate noisy style nits.

## Project impact

Canonical skill: `.agents/skills/nuxt-frontend/`. Review findings applied: `shared/` placement, TS 6 inference/libs, Nuxt 5 flag + Bun/Deno lab default, BFF method-suffix vs `readBody`, no Zod on the proxy.

## Test impact (REST Assured / Playwright REST / Playwright E2E)

None. Playwright stays with `tdd` / `playwright-sdet-review`.

## Sources

- [Nuxt 4 installation](https://nuxt.com/docs/4.x/getting-started/installation) — Node 22.x or newer, even numbered; also documents Bun/Deno as create/run options
- [Nuxt 4 `server/`](https://nuxt.com/docs/4.x/directory-structure/server) — no Vue/Nitro mix; method suffix; `readBody` on GET → 405; `#server` alias since 4.3; `fromNodeMiddleware` is legacy
- [Nuxt 4 TypeScript](https://nuxt.com/docs/4.x/guide/concepts/typescript) — `nuxt typecheck`; `tsconfig.app` / `server` / `node` / `shared`; do not hand-edit root `tsconfig.json`
- [Nuxt 4 `shared/`](https://nuxt.com/docs/4.x/directory-structure/shared) — types needed in both bundles go in `shared/types/`
- [Nuxt 4 upgrade](https://nuxt.com/docs/4.x/getting-started/upgrade) — default `srcDir` is `app/`; Nuxt 5 is in development via `future.compatibilityVersion: 5`
- [Nuxt `$fetch`](https://nuxt.com/docs/4.x/api/utils/dollarfetch) — ofetch; SSR cookie forwarding caveats
- [ofetch raw response](https://github.com/unjs/ofetch#-access-to-raw-response) — `ofetch.raw` / `$fetch.raw` keeps `headers` and `_data`
- [Announcing TypeScript 6.0](https://devblogs.microsoft.com/typescript/announcing-typescript-6-0/) — last JS compiler; deprecations; lib placement of Temporal / getOrInsert / `RegExp.escape`; `this`-less method inference
- [Nuxt UI Empty](https://ui.nuxt.com/docs/components/empty) — `UEmpty` exists; `loading` prop is 4.10+ (lab is `@nuxt/ui` 4.7.1)
- This repo: `apps/frontend/package.json`, `nuxt.config.ts`, `server/utils/backendApi.ts`, `shared/types/auth.d.ts`

## Uncertainty / follow-up

- Context7: still `Invalid API key` (needs `ctx7sk…`) after successful `mcp_auth`.
- Live Nuxt UI docs are ahead of 4.7.1 (`Empty.loading` is 4.10+).
- Microsoft encourages trying TS 7 native preview after TS 6; lab forbids it — keep that as an explicit lab default, not an upstream fact.
