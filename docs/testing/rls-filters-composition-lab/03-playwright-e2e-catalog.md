# 03 — Katalog Playwright E2E (UI)

Warstwa: Chromium. **mocked** = `tests/e2e`. **POM** = `tests-pom`, zero fulfill.

Cel edukacyjny przy każdym TC: 2–4 zdania.

---

## A. RLS Lab

### PW-RFC-E2E-001 — Hub RLS z nav

| | |
|---|---|
| Pokrycie | existing-pw (`rls-lab.spec.ts`) |
| Prio | P0 |
| Kroki | `/admin/merchants` → `nav-link-rls-lab` |
| Asercje | tekst „Java WHERE is not RLS”; `rls-lab-items-table` |
| Uczy | Lab izolacji DB jest osobną powierzchnią, nie rewrite merchants. |

### PW-RFC-E2E-002 — Merchant widzi tylko Alpha

| | |
|---|---|
| Pokrycie | existing-pom (`rls-lab.spec.ts`) **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P0 |
| Asercje | „Alpha secret” visible; „Other tenant secret” count 0; compare panel 0 |
| Uczy | Ten sam UI, inny JWT `tenant_id` → inny zbiór wierszy (RLS + `SET LOCAL app.tenant_id`). |

### PW-RFC-E2E-003 — Probe obcego UUID → 404

| | |
|---|---|
| Pokrycie | existing-pom **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P0 |
| Kroki | fill `rls-lab-probe-id` = `…a2`, click Load |
| Asercje | `problem-details-card` status 404; `problem-error` = `not_found` |
| Uczy | BOLA: brak wiersza w RLS to 404, nie 403 z Java WHERE (nie zdradzamy istnienia). |

### PW-RFC-E2E-004 — Platform compare: restricted bez GUC = 0

| | |
|---|---|
| Pokrycie | existing-pom (`rls-lab.spec.ts`) **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P0 |
| Kroki | platform.admin → `/admin/rls-lab` → Load compare |
| Asercje | `rls-lab-compare-restricted-no-tenant` = `0`; `rls-lab-compare-unprotected` > 0; BFF `restrictedWithoutTenantGuc === 0` |
| JSON | `bypassRoleCount`, `restrictedWithoutTenantGuc`, `unprotected` (nie `rlsWithGuc`) |
| Uczy | Restricted role bez `SET LOCAL app.tenant_id` widzi 0. Platforma czyta przez rolę `BYPASSRLS`, nie przez GUC. |

### PW-RFC-E2E-005 — Merchant compare ukryty

| | |
|---|---|
| Pokrycie | existing-pw (MERCHANT_MANAGER) + existing-pom |
| Prio | P1 |
| Asercje | brak `rls-lab-compare-panel`; BFF `GET /api/rls-lab/compare` **403** (brak `platform:payments:read`; body **nie** jest `rls_forbidden`) |
| Uczy | UI chowa panel (`canReadPlatformPayments`). API odrzuca authority zanim serwis rzuci `rls_forbidden`. |

### PW-RFC-E2E-006 — FE flag off

| | |
|---|---|
| Pokrycie | existing-pw `playwright.rls-flag-off.config.ts` |
| Prio | P1 |
| Asercje | `nav-link-rls-lab` count 0; `/admin/rls-lab` bez table (client 404 copy); BFF `/api/rls-lab/items` 404 |

---

## B. Filtry i paginacja

### PW-RFC-E2E-020 — date + status + reference

| | |
|---|---|
| Pokrycie | existing-pom (`payments-filters.spec.ts`) **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P0 |
| Oracle | `BffClient.listPaymentOrders` te same query |
| Uczy | URL jest kontraktem filtra; oracle to API, nie innerText sam. |

### PW-RFC-E2E-021 — min/max amount

| | |
|---|---|
| Pokrycie | existing-pw + existing-pom |
| Prio | P0 |
| Uczy | Amount w minor units; UI number input vs query int. |

### PW-RFC-E2E-022 — pairwise status × currency

| | |
|---|---|
| Pokrycie | existing-pom (`payments-filters.spec.ts` CREATED × PLN vs EUR) **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P1 |
| Oracle | `BffClient.listPaymentOrders` `{ status: 'CREATED', currency: 'PLN' }` |
| Uwaga | Backend list `status` Pattern = tylko `CREATED`. |

### PW-RFC-E2E-023 — Apply z page 2 → page 0

| | |
|---|---|
| Pokrycie | existing-pw + existing-pom (`?page=1` then Apply) **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P0 |
| Asercje | URL bez `page=1`; BFF list `page` = `0` lub absent |
| Uczy | Stale page index to check-then-act w UI: filtr bez resetu pokazuje pustą stronę. |

### PW-RFC-E2E-024 — UPagination next → page=1

| | |
|---|---|
| Pokrycie | existing-pw |
| Prio | P1 |
| Uczy | Widget 1-based, backend 0-based. Off-by-one w testach paginacji. |

### PW-RFC-E2E-025 — Clear filters

| | |
|---|---|
| Pokrycie | existing-pw analog `payment-date-filter.spec.ts` |
| Prio | P1 |

### PW-RFC-E2E-044 — BFF composition origin 3000

| | |
|---|---|
| Pokrycie | existing-pom waitForRequest port `3000` |
| Prio | P1 |
| Uczy | Dashboard nie woła Spring `:8080` z przeglądarki; API composition w Nitro. |

---

## C. Trudne kontrolki

### PW-RFC-E2E-030 — USelect nie jest native `<select>`

| | |
|---|---|
| Pokrycie | existing-pom **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P1 |
| Uczy | `page.selectOption` pada na Nuxt UI; użyj label + `getByRole('option')`. URL `status=CREATED` po Apply. |

### PW-RFC-E2E-031 — badge data-status

| | |
|---|---|
| Pokrycie | existing-pw + existing-pom |
| Prio | P1 |
| Asercje | `payment-status-badge` w wierszu z `clientOrderReference`, nie `status-badge.first()` |
| Uczy | Kolor nie jest oracle; `data-status` + visible label. Globalny `status-badge` łapie dashboard. |

### PW-RFC-E2E-032 — ConfirmModal dismiss

| | |
|---|---|
| Pokrycie | existing-pom `payments-hard-controls.spec.ts` **DONE_VERIFIED** live HTTP 2026-08-13 |
| Prio | P0 |
| Kroki | open Cancel drawer → submit → heading `Confirm Cancel` → klik `confirm-action-dismiss` |
| Asercje | heading `Confirm Cancel` count 0; brak POST `.../cancel`; GET status `CREATED` |
| Locator | `data-testid="confirm-action-dismiss"` (nie `getByRole('button', { name: 'Cancel' })` — to jest przycisk w drawerze) |
| Uczy | Check-then-act: UI confirm nie jest `page.on('dialog')`. Dismiss = brak zmiany w API. Etykieta „Go back” jest copy; oracle to testid. |

### PW-RFC-E2E-033 — 1-based vs 0-based

| | |
|---|---|
| Pokrycie | existing-pw (to samo co 024) |
| Prio | P1 |
