# Method classes (ISTQB → TypeScript)

Import these from `specs/` (directly or through a named `combinations/` row).
They are **oracles and rows**, not a second test runner.

Do **not** call `POST /api/test/seed-learning` or `/api/test/etl/payments/*` from here.

| Folder | Technique | Combination |
|---|---|---|
| `ep-bva/` | partitions + on/off BVA | used by `CreateUcEpRest` |
| `decision-table/` | who × resource, key × body, If-Match × akcja | `IsolationDtUc`, idempotency, `IfMatchActionMatrix` |
| `state/` | legal / illegal edges | `LifecycleStDt`, `IllegalStDt`, `DualControlStDt` |
| `use-case/` | actor steps + oracle | guest, create order, create merchant |
| `error-guessing/` | overlay, IPv6, two logouts | — |
| `metamorphic/` | relacje dwóch wykonań | `MetamorphicListFilter`, `SummaryInclusion` |
| `combinations/` | why techniques pair | import these from specs |

Reachability starts with the configured live specs, not this catalog. Run
`corepack pnpm exec playwright test --config playwright.pom.config.ts --list`,
then follow imports from the listed `tests-pom/specs/**/*.spec.ts` files into
`methods/`; an artifact without a path back to such a spec is deleted. Current
retained artifacts state their technique and oracle in their file JSDoc.

Copy map for learner: same folder names, prefix `My`.

JSDoc on each file: what it does, what changes between rows, e2e vs rest, seed world.
