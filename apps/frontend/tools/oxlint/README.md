# Oxlint + vendored anti-slop (lab)

TypeScript hygiene gate for `apps/frontend`. This is **not** a Playwright/POM skill.

Upstream: [dmmulroy/anti-slop](https://github.com/dmmulroy/anti-slop) (MIT). Vendored at `anti-slop/` from commit in `anti-slop/UPSTREAM-COMMIT`. Effect rules are **not** copied — this lab does not use Effect.

Do not treat the plugin as an npm dependency. Edit `oxlint.config.ts` for lab policy. Re-copy `anti-slop/` only when bumping upstream.

## Commands

From `apps/frontend`:

```bash
corepack pnpm lint
```

`--deny-warnings` is **not** on. Remaining warns are honest seams (see below), not a merge blocker.

## Lab policy vs upstream defaults

Upstream enables every generic rule as `error`. That would fail Zod/BFF/`$fetch` seams and existing Vitest `vi.mock` files.

| Rule | Lab | Why |
|---|---|---|
| `no-chained-type-assertions` | error | Fabricated evidence (`as unknown as T`) |
| `no-widen-then-assert` | error | Widen then `as` back |
| `no-reflect-apply` / `no-reflect-get` | error | Untyped reflection |
| `no-object-parameters` | error | TS `object` type on inputs |
| `require-safety-comment-for-type-assertion` | warn | Remaining Vue form unions / test doubles; do not spam `// SAFETY:` |
| `no-shape-in-symbol-names` | error | Cleaned |
| `no-conditional-empty-object-spread` | warn | Filter query omit in Vue pages |
| `no-known-value-widening` | warn | Header maps typed as `Record<string, string>` |
| `no-module-mocking` | off in `*.test.ts` / `*.spec.ts` | Vue SFC tests mock composables; live POM does not mock |
| `no-runtime-typeof` | warn, `allowInTypeGuards: true` | Prefer named predicates (`isNonEmptyString`) |
| `no-unknown-parameters` | warn | `useApiClient.request` body is the parse boundary |
| `no-unknown-returns` | off in `server/api/**` | Nitro proxies Spring JSON; client Zod is the oracle |
| `no-unsafe-dictionary-type` | warn | Problem+json extensions; query bags |

Promote a warn rule to error only after the existing files that trip it are cleaned.

## Out of scope

- Playwright locators, POM, `page.route` — `.agents/skills/playwright-pom`
- Product Nuxt placement — `.agents/skills/nuxt-frontend`
- Java/Spring — this tool does not run there
