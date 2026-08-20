---
name: epic-e6-tree-search-charts
parent: playwright-merchant-360
epic: E6
tasks: [PW-M360-T16, PW-M360-T17, PW-M360-T18]
last_updated: 2026-08-20
---

# Epic E6 — Tree, live search, summary charts

**Cel produktowy:** hierarchia tenant→merchant; Ctrl+K znajduje prawdziwe encje; overview/detail charts z `summary`.  
**Cel dydaktyczny:** `tree`/`treeitem`; last-wins na żywym GET (bez `page.route`); test danych nie pikseli.

---

## Story E6-S1 — Org tree

**Task:** `PW-M360-T16` · P1

### Jako / chcę / aby

Jako platform admin rozwijam Tenant Alpha i widzę merchantów (lazy GET).

### Business case

`BC-M360-50` — BSS multi-tenant: drzewo read-model, nie nowa tabela parent_id. Dzieci = query `GET /api/org-tree?parent=tenant:{id}`.

### Use case

`UC-M360-50` — `/admin/merchants` split: tree + tabela. Click Sweden analog: tu „Alpha Tenant” → widać `MERCHANT_ALPHA_001`. Keyboard expand `aria-expanded`.

### Acceptance criteria

- [ ] `GET /api/org-tree` authority merchants read; tenant admin tylko swój węzeł.
- [ ] Lazy: drugi request po expand.
- [ ] Deep-link P2: `?tree=tenant-alpha`.
- [ ] Komponent: `UTree` jeśli 4.7.1, inaczej semantyka ARIA ręcznie.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-100 | E2E | `getByRole('treeitem', { name: /Alpha/ })` click → child merchant visible |
| PW-M360-E2E-101 | E2E | collapse → child hidden; `aria-expanded` |
| PW-M360-E2E-102 | E2E | keyboard expand |
| PW-M360-E2E-103 | E2E | tenant.admin nie widzi PLATFORM_TENANT children obcych |
| PW-M360-E2E-104 | E2E | tree `toMatchAriaSnapshot` fragment |
| RA-M360-070 | RA | org-tree tenant isolation |
| PW-M360-API-050 | PW REST | lazy children 200 |

---

## Story E6-S2 — Command palette encje

**Task:** `PW-M360-T17` · P1

### Jako / chcę / aby

Jako operator Ctrl+K wpisuję fragment reference i skaczę do 360 / płatności.

### Business case

`BC-M360-51` — Dziś palette to tylko nav ([command-palette.spec.ts](../../../apps/frontend/tests-pom/specs/command-palette.spec.ts)). ERP search = `GET /api/search?q=` limit 10 merchants + 10 payment refs.

### Use case

`UC-M360-51` — Ctrl+K, type unique ref, Enter → slideover lub detail.

**Race (korekta):** bez mock delay. Test: wolne wpisywanie + `waitForResponse` ostatniego GET `q=fullRef`; wynik = ten merchant. Out-of-order **non-goal**.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-110 | E2E | Ctrl+K dialog; fill unique; option visible; GET search 200 |
| PW-M360-E2E-111 | E2E | select → URL/panel właściwego id |
| PW-M360-E2E-112 | E2E | last response body zawiera tylko trafienie `q` |
| RA-M360-071 | RA | search tenant isolation; empty q 400 |
| PW-M360-API-051 | PW REST | search BFF |

Regresja: istniejące nav destination tests zostają.

---

## Story E6-S3 — Charts z summary

**Task:** `PW-M360-T18` · P1

### Jako / chcę / aby

Jako merchant manager widzę rozkład statusów z `GET .../summary` (`byStatus.orderCount`, `totalAmountMinor`).

### Business case

`BC-M360-52` — Interview: testujemy dane, legendę, nie `toHaveScreenshot` jako jedyną asercję. Visual opcjonalnie visual-lab.

### Use case

`UC-M360-52` — Payments page: bar/legend „CAPTURED {n}” = `byStatus`.

### Acceptance criteria

- [ ] Zero client-side sum wymyślonych. Karty Overview już z API — chart ten sam payload.
- [ ] Accessible: table/legend text, nie sam kolor.
- [ ] Unovis tylko po akceptacji dependency; default: `UProgress` / lista + SVG minimalny.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-API-052 | PW REST | GET summary 200 Zod |
| PW-M360-E2E-120 | E2E | visible text counts = summary JSON (waitForResponse) |
| PW-M360-E2E-121 | E2E | 403 summary (rola bez payments read) |
