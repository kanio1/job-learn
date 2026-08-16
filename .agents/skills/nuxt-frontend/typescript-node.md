# TypeScript 6 and Node

## Node

Nuxt 4 requires **Node.js 22.x or newer**, even numbered (22, 24, …). This lab’s documented range: `^22.12.0 || ^24.11.0 || >=26.0.0`.

- Run frontend with that Node, then `corepack pnpm …`.
- `package.json` is `"type": "module"` — ESM. Prefer `node:` specifiers if you must use Node APIs in Nitro (`node:fs`). Avoid `fromNodeMiddleware`.
- Official Nuxt docs list Bun/Deno as create/run options. **This lab does not** — stay on Node + Corepack pnpm.
- Do not put secrets in `runtimeConfig.public`. Do not set `future.compatibilityVersion: 5`.

Nitro, the Vue app, shared code, and `nuxt.config.ts` are **different TS projects**. Nuxt generates `.nuxt/tsconfig.app.json`, `.nuxt/tsconfig.server.json`, `.nuxt/tsconfig.shared.json`, and `.nuxt/tsconfig.node.json`. Root `apps/frontend/tsconfig.json` only extends `.nuxt/tsconfig.json` — do not hand-edit compiler options there; extend via `nuxt.config.ts` if needed.

`nuxt.config.ts` already sets `typescript.typeCheck` unless `NUXT_TYPECHECK=false`. After Vue/TS edits:

```bash
corepack pnpm typecheck
```

## TypeScript 6.0.3 (do not bump to 7)

TypeScript 6 is the last JS-implemented compiler and the bridge to native TS 7. This repo stays on **6.0.3**. Do not add `@typescript/native-preview` or `--stableTypeOrdering` unless the user asks.

Nuxt generates tsconfig; do not fight it with a second `strict: false` unless you are mid-migration (we are not).

Language defaults to follow in **new** files:

- ESM `import` / `export`. No `import … assert { type: "json" }` — use `with` if an import attribute is required.
- No TypeScript `module Foo {}` namespaces (use `namespace` only if an existing file already does; prefer modules).
- Do not set `moduleResolution: node` / `node10` in a new config.
- Do not add `baseUrl` for path aliases; Nuxt already provides `~/` and `#imports`.
- Match neighboring callback style (arrow vs method). TS 6 infers `this`-less **methods** as well as arrows — do not rewrite methods “for TS 6”.
- Do not use `Temporal` or `Map.getOrInsert` (`esnext` libs) or `RegExp.escape` (`es2025` lib) in product code unless the runtime (browser + this Node range) is already proven in the app. Prefer `Date` / existing helpers.

Auth code already decodes JWT payloads **without** Node `Buffer`. Keep that.

## Vue vs Nitro types

Augment Vue-only types under `app/`. Nitro-only under `server/`. Types and isomorphic utils used by **both** under `shared/` (`shared/types/auth.d.ts` is the example). A `declare module` in `app/` does not apply to Nitro handlers.

Auto-imports work in `app/` (composables, components). Server files should explicitly import h3 helpers if the file is not already consistent with neighbors — copy the nearest handler.
