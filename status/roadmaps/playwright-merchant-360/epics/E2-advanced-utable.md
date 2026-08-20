---
name: epic-e2-advanced-utable
parent: playwright-merchant-360
epic: E2
tasks: [PW-M360-T06, PW-M360-T07, PW-M360-T08, PW-M360-T09]
last_updated: 2026-08-20
---

# Epic E2 — Advanced UTable (Merchant + Payments)

**Cel produktowy:** ERP tabela: search, status, sort, page, selection, bulk, empty/loading.  
**Cel dydaktyczny:** Playwright tabela (`columnheader`, `row`, `filter`), `waitForResponse`, URL persistence.

**Połączenia:** [MerchantTable.vue](../../../apps/frontend/app/components/merchant/MerchantTable.vue), [PaymentOrderListTable.vue](../../../apps/frontend/app/components/payment/PaymentOrderListTable.vue), Nuxt UI Table (TanStack).

Gate: E1 DONE na `--app`.

---

## Story E2-S1 — Toolbar + URL state

**Task:** `PW-M360-T06` · P0

### Jako / chcę / aby

Jako platform admin chcę `q`, status, (tenant) w toolbarze zapisywane w `?q=&status=&page=&sort=`, aby Back wracał do tego samego wyniku.

### Business case

`BC-M360-10` — Operator porównuje dwa filtry i wraca przyciskiem Back (ERP muscle memory).

### Use case

`UC-M360-10` — Ustaw filtr ACTIVE, Apply, wejdź w detail, Back → ACTIVE nadal.

### Acceptance criteria

- [ ] Filtry **nie** są tylko `computed` na 50 wierszach — każdy Apply = GET.
- [ ] `UDashboardToolbar` / istniejący pasek; włączony przycisk Display kolumn może poczekać (nice-to-have P2).
- [ ] Platform: `USelect` tenant (label accessible).
- [ ] `aria-label="Merchant registry"` zostaje.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-020 | E2E | click `columnheader` name `/Updated\|Modified\|Updated at/` → `waitForResponse` GET `/api/merchants` z `sort=updatedAt` |
| PW-M360-E2E-021 | E2E | `getByRole('row').filter({ hasText: uniqueRef })` widoczny po `q=` |
| PW-M360-E2E-022 | E2E | status ACTIVE + search → wiersze tylko ACTIVE; GET query zawiera oba |
| PW-M360-E2E-030 | E2E | Apply z `?page=1` analog Wave A: nowy filter resetuje page 0 |
| PW-M360-E2E-031 | E2E | goto detail, `page.goBack()`, URL i tabela ten sam filter |
| PW-M360-API-010 | PW REST | ten sam query przez `BffClient` — `totalElements` zgodne z UI caption |

Locatory: **nie** `nth` jako tożsamość wiersza. `nth` tylko na header row (`rows.nth(0)`).

---

## Story E2-S2 — Sort headers (manual)

**Task:** `PW-M360-T06` · P0

### Jako / chcę / aby

Jako operator chcę kliknąć nagłówek Updated / Display name / Status i dostać **serwerowy** porządek.

### Use case

`UC-M360-11` — Toggle asc/desc; drugi klik odwraca; `waitForResponse` 200.

### Acceptance criteria

- [ ] `v-model:sorting` → query `sort=`; **manualSorting** (nie sortuj `content` w przeglądarce jako wyrocznia).
- [ ] Kolumny: displayName, status, updatedAt/createdAt. **Brak Revenue.**
- [ ] Przycisk w `columnheader` ma accessible name kolumny.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-023 | E2E | dwa kliknięcia Updated: `sort` desc potem asc w URL/request |
| PW-M360-E2E-024 | E2E | `allTextContents` displayName monotonic po sort name |

---

## Story E2-S3 — Selection + bulk activate

**Task:** `PW-M360-T07` · P1

### Jako / chcę / aby

Jako platform admin chcę zaznaczyć DRAFT i Activate selected, aby onboarding wielu merchantów nie był klikaniem po jednym.

### Business case

`BC-M360-11` — Bulk to sekwencja istniejących POST `/activate` (bez nowego bulk SQL w Fali 2). Per-row error (np. już ACTIVE) w toście; reszta przechodzi.

### Use case

`UC-M360-12` — Zaznacz 2 DRAFT (unikalne refs), bulk activate, oba ACTIVE w tabeli i GET.

### Acceptance criteria

- [ ] Checkbox kolumna; `aria-label="Select {merchantReference}"`.
- [ ] `v-model:row-selection` (nie tylko `tableApi` — [nuxt/ui#5408](https://github.com/nuxt/ui/issues/5408)).
- [ ] Bulk tylko gdy `canUpdateMerchantStatus`; readonly count 0.
- [ ] `waitForResponse` POST activate per row.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-040 | E2E | select 2 DRAFT → Activate selected → 2× POST 200 |
| PW-M360-E2E-041 | E2E | mixed DRAFT+ACTIVE → ACTIVE wiersz błąd, DRAFT ok |
| PW-M360-E2E-042 | E2E | readonly: 0 checkbox / 0 bulk button |
| RA-M360-019 | RA | activate nadal 409 przy nielegalnym przejściu (regresja) |

---

## Story E2-S4 — Empty / loading / 403

**Task:** `PW-M360-T08` · P0

### Use case

`UC-M360-13` — `q` bez trafień → empty „No merchants match”. Loading przy GET. Manager na `/admin/merchants` → alert authority (istniejący wzorzec).

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-050 | E2E | unique `q` bez hit → empty state |
| PW-M360-E2E-051 | E2E | `role=status` loading name podczas GET (nie asercja na 500 mock) |
| PW-M360-E2E-052 | E2E | merchant.manager registry 403 UI (istniejący deny) |

500: Error Lab / nie `route.fulfill`.

---

## Story E2-S5 — Payment table amount + status

**Task:** `PW-M360-T09` · P0

### Jako / chcę / aby

Jako merchant manager na `MERCHANT-Wn` chcę sort Amount i filtr CAPTURED na żywych orderach.

### Test IDs

| ID | Warstwa | Scenariusz |
|---|---|---|
| PW-M360-E2E-025 | E2E | `columnheader` Amount → `sort=amountMinor` w GET |
| PW-M360-E2E-026 | E2E | status CAPTURED Apply → wiersze + BFF |
| PW-M360-E2E-027 | E2E | regresja PW-RFC-E2E-020 (date+status+ref) nadal zielona |
| PW-M360-E2E-028 | E2E | pagination UPagination vs `page` 0-based |

Worker isolation: mutacje na `MERCHANT-Wn`, nie Alpha seed.
