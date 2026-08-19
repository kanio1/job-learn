# Method classes (ISTQB → TypeScript)

Import these from `specs/`. They are **oracles and rows**, not a second test runner.

Do **not** call `POST /api/test/seed-learning` or `/api/test/etl/payments/*` from here.

| Folder | Technique | Combination |
|---|---|---|
| `ep-bva/` | partitions + on/off BVA | used by `CreateUcEpRest` |
| `decision-table/` | who × resource, key × body, If-Match × akcja | `IsolationDtUc`, idempotency, `IfMatchActionMatrix` |
| `state/` | legal / illegal edges | `LifecycleStDt`, `IllegalStDt`, `DualControlStDt` |
| `use-case/` | actor steps + oracle | guest, create order, create merchant |
| `pairwise/` | checkout mode × outcome | **other world** (CPL) |
| `error-guessing/` | overlay, IPv6, two logouts | — |
| `metamorphic/` | relacje dwóch wykonań | `MetamorphicListFilter` |
| `combinations/` | why techniques pair | import these from specs |

Copy map for learner: same folder names, prefix `My`.

JSDoc on each file: what it does, what changes between rows, e2e vs rest, seed world.
