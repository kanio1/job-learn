---
name: epic-e4-rbac-concurrency
parent: playwright-merchant-360
epic: E4
tasks: [PW-M360-T12, PW-M360-T13]
last_updated: 2026-08-20
---

# Epic E4 — RBAC kolumn + concurrency merchant

**Cel produktowy:** viewer nie widzi mutacji; dwa operatorzy nie nadpisują merchanta po cichu.  
**Cel dydaktyczny:** `storageState` per rola; dwa contexty; 412 vs 409.

---

## Story E4-S1 — Role-aware columns

**Task:** `PW-M360-T12` · P0

### Jako / chcę / aby

Jako readonly chcę widzieć registry bez checkboxów, Activate, Suspend, Import.

### Business case

`BC-M360-30` — ERP RBAC: UI hide **oraz** API 403. Lekcja interview: „UI hides DELETE ≠ security”.

### Use case

`UC-M360-30` — Ten sam `MerchantsListPage`, `test.use({ storageState: readonly })`.

### Acceptance criteria

- [ ] `READ_ONLY_USER`: 0 Activate/Suspend/Create/Import/bulk; kolumna Tenant może być; Risk toggle 0.
- [ ] `SUPPORT_AGENT`: brak create/import/activate; audit link OK.
- [ ] `PLATFORM_ADMIN`: Tenant column + wszystkie akcje.
- [ ] `TENANT_ADMIN`: bez Tenant column (jeden tenant).
- [ ] Page object **bez** `if (role)` — asercje w specach.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-SEC-010 | E2E | readonly: `getByRole('button', { name: /Activate\|Suspend\|Create merchant/ })` count 0 |
| PW-M360-SEC-011 | E2E | support: Create count 0; tabela widoczna |
| PW-M360-SEC-012 | E2E | admin: Create + Tenant header widoczne |
| PW-M360-SEC-013 | E2E | tenant.admin: brak kolumny Tenant |
| PW-M360-SEC-014 | E2E | manager: registry alert (istniejący) |
| PW-M360-API-040 | PW REST | readonly POST `/activate` → 403 problem |
| RA-M360-040 | RA | macierz GET/POST per authority (uzupełnić istniejące security tests) |

---

## Story E4-S2 — Merchant ETag / If-Match

**Task:** `PW-M360-T13` · P0

### Jako / chcę / aby

Jako operator chcę dostać komunikat „zmieniono przez kogoś innego” gdy drugi admin zdążył suspend, zamiast cichego 200 na stale UI.

### Business case

`BC-M360-31` — `merchants.version` już jest (`@Version`). HTTP jak płatności: `ETag: "v{n}"`, stale **412**, brak **428**. Nie 409.

### Use case

`UC-M360-31` — Dwa contexty platform (lub platform + tenant.admin na tym samym merchancie): A ładuje, B suspend, A activate z starym If-Match → 412; UI Reload.

### Acceptance criteria

- [ ] GET `/{id}` i list item: ETag (list może nie mieć per-row — minimum GET detail + mutacje).
- [ ] POST activate/suspend, PATCH risk-flag wymagają If-Match.
- [ ] BFF forward `If-Match` / `ETag` (allowlist już jest).
- [ ] UI: problem card + Reload; slideover odświeża GET.
- [ ] REST Assured analog payment version tests.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| RA-M360-050 | RA | GET detail ma ETag `"v{n}"` |
| RA-M360-051 | RA | activate bez If-Match → 428 |
| RA-M360-052 | RA | If-Match `"v99"` → 412 |
| RA-M360-053 | RA | happy If-Match → 200, ETag inkrement |
| RA-M360-054 | RA | risk-flag If-Match |
| RA-M360-055 | RA | tenant isolation unchanged |
| PW-M360-SEC-020 | E2E | dwa contexty: 412 + toast/dialog Reload |
| PW-M360-API-041 | PW REST | BFF activate stale ETag 412 |

Wzorzec dwóch ról: [Playwright auth — testing multiple roles together](https://playwright.dev/docs/auth#testing-multiple-roles-together). Lab już: dual-control, RLS.
