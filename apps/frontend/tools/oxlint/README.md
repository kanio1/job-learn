# Oxlint + vendored anti-slop (lab)

TypeScript hygiene gate for `apps/frontend`. This is **not** a Playwright/POM skill.

Upstream: [dmmulroy/anti-slop](https://github.com/dmmulroy/anti-slop) (MIT). Vendored at `anti-slop/` from commit in `anti-slop/UPSTREAM-COMMIT`. Effect rules are **not** copied — this lab does not use Effect.

Do not treat the plugin as an npm dependency. Edit `oxlint.config.ts` for lab policy. Re-copy `anti-slop/` only when bumping upstream.

## Commands

From `apps/frontend`:

```bash
corepack pnpm lint
```

`--deny-warnings` is **not** on. Warn-tier rules are a backlog, not a merge blocker.

## Lab policy vs upstream defaults

Upstream enables every generic rule as `error`. That would fail Zod/BFF/`$fetch` seams and existing Vitest `vi.mock` files.

| Rule | Lab | Why |
|---|---|---|
| `no-chained-type-assertions` | error | Fabricated evidence (`as unknown as T`) |
| `no-widen-then-assert` | error | Widen then `as` back |
| `no-reflect-apply` / `no-reflect-get` | error | Untyped reflection |
| `no-object-parameters` | error | TS `object` type on inputs |
| `require-safety-comment-for-type-assertion` | warn | Existing Vue/BFF/`as` sites; new code should add `// SAFETY:` |
| `no-shape-in-symbol-names` | warn | Existing `paginatedShape` tests |
| `no-conditional-empty-object-spread` | warn | Common Vue/options omit |
| `no-known-value-widening` | warn | Existing handler maps |
| `no-module-mocking` | warn | Existing Vitest `vi.mock` — prefer real seams for **new** tests |
| `no-runtime-typeof` | warn, `allowInTypeGuards: true` | Zod is the BFF/API boundary; type predicates stay |
| `no-unknown-parameters` / `no-unknown-returns` / `no-unknown-type-aliases` | warn | HTTP/`$fetch` bodies start untyped; parse with Zod |
| `no-unsafe-dictionary-type` | warn | `Record<string, unknown>` is current query/metadata |

Promote a warn rule to error only after the existing files that trip it are cleaned.

## Out of scope

- Playwright locators, POM, `page.route` — `.agents/skills/playwright-pom`
- Product Nuxt placement — `.agents/skills/nuxt-frontend`
- Java/Spring — this tool does not run there
