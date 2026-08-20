---
name: playwright-merchant-360-learning-map
parent: playwright-merchant-360
last_updated: 2026-08-20
---

# Mapa lekcja TypeScript / Playwright → story

Curriculum senior SDET na **żywym** stosie. Świadomie **bez** lekcji `page.route` (zakaz laby).

| Lekcja | Feature | Story | Test IDs |
|---|---|---|---|
| `getByRole('columnheader')` + click sort | Server sort | E2-S2 | PW-M360-E2E-020 |
| `locator.filter({ hasText })` / `nth` ostrożnie | Wiersz merchanta | E2-S2 | PW-M360-E2E-021 |
| `waitForResponse` + method/path | Sort/filter = prawdziwy GET | E2-S3 | PW-M360-E2E-022, PW-M360-API-010 |
| Query string + `Back` | Persistence filtrów | E2-S4 | PW-M360-E2E-030 |
| Selection + bulk | TanStack row-selection | E2-S5 | PW-M360-E2E-040 |
| Empty / loading | `UEmpty` / `LoadingState` | E2-S6 | PW-M360-E2E-050/051 |
| `getByRole('dialog')` + Escape | Slideover / Reka focus | E3-S1 | PW-M360-E2E-060 |
| `toMatchAriaSnapshot` | Form + slideover YAML | E3-S2 | PW-M360-E2E-061 |
| EP/BVA/DT na Zod | Create merchant | E3-S3 | PW-M360-E2E-070, RA-M360-030, [06](../../../docs/testing/merchant-360-erp-lab/06-istqb-ep-bva.md) |
| `test.use({ storageState })` | RBAC kolumny | E4-S1 | PW-M360-SEC-010 |
| UI hide ≠ 403 | Readonly POST activate | E4-S2 | PW-M360-API-040, RA-M360-040 |
| Dwa `browser.newContext` | Stale If-Match 412 | E4-S3 | PW-M360-SEC-020 |
| `setInputFiles` | Import CSV | E5-S1 | PW-M360-E2E-080 |
| `dragTo` **plus** menu Move | Kanban | E5-S3 | PW-M360-E2E-090 |
| `role=tree` / `treeitem` / `aria-expanded` | Org tree | E6-S1 | PW-M360-E2E-100 |
| Ctrl+K + last response | Entity search live | E6-S2 | PW-M360-E2E-110 |
| Dane summary, nie piksele | Charts | E6-S3 | PW-M360-E2E-120, PW-M360-API-050 |
| `page.clock` | Calendar expiresAt | E7-S1 | PW-M360-E2E-130 |
| `contenteditable` (jeśli Editor 4.7.1) | Notes | E7-S4 | PW-M360-E2E-140 |
| Discriminated unions / `satisfies` / Zod | TS 6 w schema + columns | E1–E2 | typecheck + oxlint |

Warstwa E2E vs REST i docelowe `methods/`: [09-agent-tests-pom-plan](../../../docs/testing/merchant-360-erp-lab/09-agent-tests-pom-plan.md).

Poza mapą (non-goal): GraphQL, WS, WebAuthn, visual pixel chart jako jedyna asercja, native `page.on('dialog')`.
