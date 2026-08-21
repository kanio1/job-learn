# 03 — Katalog Playwright E2E (live POM)

Warstwa: Chromium `tests-pom`, **zero fulfill**. Stos: `scripts/dev-stack.sh --app` (lub host DX).  
Pokrycie: **designed**. Spec docelowy podany.

Page objects: rozszerzyć `MerchantsListPage`, `PaymentsListPage`; dodać `MerchantSlideover` (component). Locatory: `getByRole` → `getByLabel` → `getByTestId`. Wiersz: `getByRole('row').filter({ hasText: ref })`.

Pliki `tests-pom` i warstwa E2E vs REST: [09-agent-tests-pom-plan.md](09-agent-tests-pom-plan.md). Techniki: [06](06-istqb-ep-bva.md), [07](07-dt-st-uc-mr.md).  
Curriculum: [value-and-learning](../m360-ops-wave-2-value-and-learning.md).

### Czego uczy ta warstwa (M360 E2E)

Żywy Keycloak + BFF. Lekcje: `columnheader` server sort, `waitForResponse` exact path, `storageState` RBAC, dwa contexty 412, `setInputFiles`, `role=tree`, Ctrl+K. **Nie** uczy `page.route`, pixel chart, ani support Kanban (to Wave 2 E2E-110).

---

## A. Kontrakt list / overview

### PW-M360-E2E-001 — Overview count = `totalElements`

| | |
|---|---|
| Pokrycie | designed |
| UC | UC-M360-04 |
| Spec docelowy | `merchants.spec.ts` (zmiana istniejącego overview) |
| Kroki | `goto('/')`; `waitForResponse` GET `/api/merchants` |
| Asercje | Summary Merchants text = `body.totalElements` (nie `content.length`) |

---

## B. Advanced table

### PW-M360-E2E-020 — Sort Updated (server)

| | |
|---|---|
| Spec | `merchants-table.spec.ts` (nowy) |
| Kroki | `merchants.goto`; `getByRole('columnheader', { name: /Updated/ }).click()` |
| Asercje | Promise `waitForResponse` GET `/api/merchants` query `sort=updatedAt`; 200 |

### PW-M360-E2E-021 — Search unique ref

| | |
|---|---|
| Kroki | factory unique merchant via `BffClient`; fill search; Apply |
| Asercje | `row.filter({ hasText: ref })` visible; GET `q=` |

### PW-M360-E2E-022 — Multi-filter status + q

| | |
|---|---|
| Asercje | query `status` + `q`; wszystkie wiersze ACTIVE |

### PW-M360-E2E-023 — Sort toggle asc/desc

| | |
|---|---|
| Asercje | dwa requesty, przeciwne `asc`/`desc` |

### PW-M360-E2E-024 — displayName monotonic

| | |
|---|---|
| Asercje | `allTextContents` posortowane; ostrożnie z seed — użyj 3 własnych merchantów |

### PW-M360-E2E-025 — Payment Amount sort

| | |
|---|---|
| Project | `chromium-manager` worker |
| Asercje | GET `sort=amountMinor` |

### PW-M360-E2E-026 — Payment status CAPTURED

| | |
|---|---|
| Setup | BffClient authorize+capture owned order |
| Asercje | filtr CAPTURED; badge `data-status` |

### PW-M360-E2E-027 — Regresja filtrów Wave A

| | |
|---|---|
| Asercje | istniejący `payments-filters.spec.ts` nadal zielony (nie duplikować 1:1) |

### PW-M360-E2E-028 — Pagination 1-based UI / 0-based API

| | |
|---|---|
| Asercje | jak PW-RFC-E2E-023: Apply z `?page=1` → page 0 |

### PW-M360-E2E-030 — Filter resets page

### PW-M360-E2E-031 — Back persistence

### PW-M360-E2E-032 — Platform tenant filter (admin)

### PW-M360-E2E-040 — Bulk activate 2 DRAFT

`waitForResponse` × POST `/activate`.

### PW-M360-E2E-041 — Bulk mixed statuses

### PW-M360-E2E-042 — Readonly: brak selection/bulk

### PW-M360-E2E-050 — Empty search

### PW-M360-E2E-051 — Loading `role=status`

Nie mock 500. Opcja: wolny prawdziwy GET (duży size) **lub** tylko `toBeVisible` skeleton zanim 200.

### PW-M360-E2E-052 — Manager 403 registry

---

## C. Slideover + form

### PW-M360-E2E-060 — Open 360 + GET detail

### PW-M360-E2E-061 — ARIA snapshot dialog

### PW-M360-E2E-062 — Escape + focus

### PW-M360-E2E-063 — Confirm dismiss, no POST suspend

### PW-M360-E2E-070 — Create unique (regresja)

### PW-M360-E2E-071 — Duplicate 409 UI

### PW-M360-E2E-072 — BVA reference length

### PW-M360-E2E-073 — Create form ARIA (rozszerzenie istniejącego)

---

## D. Security (storageState)

### PW-M360-SEC-010 — Readonly: 0 mutacji UI

`test.use` readonly json.

### PW-M360-SEC-011 — Support: brak Create

### PW-M360-SEC-012 — Admin: Create + Tenant column

### PW-M360-SEC-013 — Tenant admin: brak Tenant column

### PW-M360-SEC-014 — Manager registry deny

### PW-M360-SEC-020 — Dwa contexty, 412

`browser.newContext({ storageState: admin })` ×2; If-Match stale; Reload.

---

## E. Import + Kanban

### PW-M360-E2E-080…085 — CSV preview/commit

`setInputFiles`; fixtures pod `tests-pom/fixtures/import/`.

### PW-M360-E2E-090 — Move menu authorize (P0)

### PW-M360-E2E-091 — dragTo (P1)

### PW-M360-E2E-092 — reload persistence

### PW-M360-E2E-093 — 412 rollback dwa contexty

### PW-M360-E2E-094 — illegal transition

---

## F. Tree / search / charts / later

### PW-M360-E2E-100…104 — tree

### PW-M360-E2E-110…112 — palette live search

### PW-M360-E2E-120…121 — chart = summary JSON

### PW-M360-E2E-130…136 — calendar / timeline / stepper

### PW-M360-E2E-140…141 — Editor (conditional)

### PW-M360-E2E-150…151 — inline PATCH

Szczegóły AC: epiki E5–E7.
