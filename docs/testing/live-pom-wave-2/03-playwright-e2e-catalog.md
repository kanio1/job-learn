# 03 — Katalog Playwright E2E (UI)

Warstwa: Chromium live POM (`tests-pom`). Zero `fulfill`.  
Każdy wiersz **existing-pom** = jeden konkretny `test('…')`. `designed` = nadal brak specu.

---

## A. Sesja i gość — `chromium-guest` / `chromium-admin`

### PW-W2-E2E-001 — Unauth `/admin/merchants` → login

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `session-guest.spec.ts` · `unauthenticated visit to merchants lands on login` |
| Project | `chromium-guest` · `@security` |
| Kroki | `goto('/admin/merchants')` |
| Asercje | URL `/login?redirectTo=`; `LoginPage.expectLoaded()` |

### PW-W2-E2E-002 — Unauth `/admin/session-lab` → login

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `session-guest.spec.ts` · `unauthenticated visit to session lab lands on login` |
| Kroki | `goto('/admin/session-lab')` |
| Asercje | jak 001 |

### PW-W2-E2E-003 — Unauth `/admin/users`, `/payments`, `/error-lab`, `/checkout-lab`

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |

### PW-W2-E2E-010 — Logout → login → ponowny admin zablokowany

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `session.spec.ts` · `logout returns to login and blocks admin again` |
| Kroki | `merchants.goto` → `userMenu.signOut()` (`logout-control` + menuitem Sign out) → `goto('/admin/merchants')` |
| Asercje | URL `/login` po sign-out **i** po drugim goto |

### PW-W2-E2E-011 — Cookie HttpOnly + brak JWT w storage

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `session.spec.ts` · `session cookie is HttpOnly and storageState has no JWT` |
| Asercje | `expectSessionCookieHttpOnly`; `expectNoTokenInBrowserStorage`; `expectNoJwtInStorageStateFile(platform-admin.json)` tylko `origins` |

### PW-W2-E2E-012 — Idle unlock → `/login` (`page.clock`, bez `waitForTimeout`)

| | |
|---|---|
| Pokrycie | existing-pom (Session Lab, nie clear-cookies mid-journey) |
| Spec | `session-lab.spec.ts` · `idle lock uses page.clock without waitForTimeout` |
| Kroki | `clock.install` → session-lab → `fastForward(121_000)` → `session-lab-idle-unlock` |
| Asercje | `session-lab-idle-lock` visible; URL `/login` |
| Brak | ponowny `goto /admin/merchants` po unlock → nadal designed (MRL E2E-022) |

---

## B. Merchants — `chromium-admin` · `merchants.spec.ts`

### PW-W2-E2E-020 — Unikalny merchant w detail po API create

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `creates a unique merchant that appears in the registry` |
| HTTP | `POST /api/merchants` 201 (`tenantReference=TENANT_ALPHA`) |
| Kroki | `createMerchant` → `merchantDetail.gotoMerchant(id)` |
| Asercje | `merchant-reference` = unique; brak JWT w Web Storage |

### PW-W2-E2E-021 — Draft → Active → Suspended

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `activates a DRAFT merchant then suspends it` |
| Kroki | API create → detail Draft → `activate()` → Active → `suspend()` → Suspended |

### PW-W2-E2E-022 — Persist GET po create

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `UI create persist: unique merchant GET after API create and reload` |
| Oracle | `assertPersistedMerchant` → `GET /api/merchants/{id}` 200 |

### PW-W2-E2E-023 — Pusty formularz: Zod, zero POST

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `empty create merchant form shows field errors and does not POST` |
| Kroki | open create → submit puste |
| Asercje | „Reference must be at least 3 characters”; „Name must be at least 2 characters”; listener POST `/api/merchants` nie odpalił |

### PW-W2-E2E-024 — UI create z polem tenant

| | |
|---|---|
| Pokrycie | blocked GAP-W2-01 |
| Prio | P1 |

### PW-W2-E2E-025 — 409 duplikat w formularzu UI

| | |
|---|---|
| Pokrycie | designed (409 jest `BffClient`, E2E-026) |
| Prio | P1 |

### PW-W2-E2E-026 — Duplikat reference 409 z BFF (bez UI)

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `duplicate merchant reference is 409 from the BFF` |
| HTTP | drugi `POST /api/merchants` ten sam `reference` → 409 |

---

## C. UX / a11y

### PW-W2-E2E-030 — Ctrl+K → Error Lab + ARIA snapshot

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `command-palette.spec.ts` · `Ctrl+K palette navigates to Error Lab` · `@a11y` `@ux` |
| Kroki | merchants loaded → `commandPalette.openWithKeyboard()` → `toMatchAriaSnapshot()` → search `Error Lab` → `getByRole('option', { name: 'Error Lab' }).click()` |
| Snapshot | `specs/command-palette.spec.ts-snapshots/Ctrl-K-palette-navigates-to-Error-Lab-1.aria.yml` |
| Asercje | URL `/error-lab`; `errorLab.expectLoaded()` |

### PW-W2-E2E-031 — Palette → Checkout Lab / Merchants / Support

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |

### PW-W2-E2E-032 — ARIA snapshot login / forbidden

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |

---

## D. Notatki i risk

### PW-W2-E2E-040 — Admin notatka na orderze managera

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `internal-notes.spec.ts` (**nie** `payments-*.spec.ts`) · `platform admin sees notes form and can submit on a live order` · `@ux` |
| Project | `chromium-admin`; drugi `BffClient` z `merchant-manager.json` |
| HTTP | manager `POST .../payment-orders` 201 → admin `POST .../notes` **201 albo 403** |
| Oracle 201 | `payment-note-body` widoczny; `listNotes` zawiera `body` |
| Oracle 403 | `alert` lub `error-state` (realm bez `platform:payments:notes:*`) |

### PW-W2-E2E-041 — Manager nie widzi formularza notes

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |

### PW-W2-E2E-050 — Risk toggle + badge na unikalnym merchancie

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `merchant-risk.spec.ts` · `risk toggle on a unique merchant shows the list badge` · `@ux` |
| Kroki | API create → activate → `merchant-risk-toggle` → `waitForResponse` PATCH `/risk-flag` |
| Oracle 200 | flagged; lista + filter placeholder `Filter merchants...` → `expectRiskBadgeFor(displayName)` |
| Oracle 403 | nadal unflagged |

---

## E. Checkout live POM — `checkout-lab.spec.ts`

Pełny CPL: [../checkout-protocol-lab/03-playwright-e2e-catalog.md](../checkout-protocol-lab/03-playwright-e2e-catalog.md). Skip gdy brak `nav-link-checkout-lab`.

### PW-W2-E2E-060 — ONLINE hosted + fulfillment CONFIRMED

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `hub opens booking; online pay uses hosted tab and fulfillment oracle` |
| Kroki | hub → `checkout-lab-open-booking` → unique `extOrderId` → submit → nowa karta Approve → Return |
| Asercje | `checkout-open-hosted`; `fulfillment-status` AWAITING potem CONFIRMED (45s); hint `toContainText('success')`; inspector `inspector-process-status` |

### PW-W2-E2E-061 — Lie return

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `lie return keeps fulfillment unconfirmed` |
| Kroki | booking ONLINE → `goto /checkout-lab/return?sessionId=&status=success` **bez** Approve |
| Asercje | hint `success`; fulfillment **nie** CONFIRMED (`AWAITING_PAYMENT|UNKNOWN`) |

### PW-W2-E2E-062 — CASH → CONFIRMED, bez hosted

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `cash booking confirms fulfillment without hosted checkout` · `@ux` |
| Kroki | `chooseMode('CASH')` (select, nie prefix `CASH-`) → submit |
| Asercje | CONFIRMED; `checkout-open-hosted` count 0 |

### PW-W2-E2E-063 — Decline → fulfillment CANCELLED

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `hosted decline leaves fulfillment cancelled` · `@ux` |
| Kroki | ONLINE → hosted Decline → Return |
| Asercje | hint **zawiera** `failure` (query może być `success,failure`); fulfillment `CANCELLED` |

### PW-W2-E2E-064 — Flag off → skip

| | |
|---|---|
| Pokrycie | existing-pom `requireCheckoutLab` |
| Kroki | merchants → jeśli `nav-link-checkout-lab` count 0 → `test.skip` |

### PW-W2-E2E-065 — EXPIRED_LINK na hosted (`psp-link-expired`)

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `mirror-lab.spec.ts` · `expired hosted checkout exposes test id` |
| Kroki | booking scenario `EXPIRED_LINK` → open hosted tab |
| Asercje | `psp-link-expired` visible |

---

## F. Support IDOR + Error Lab

### PW-W2-E2E-070 — Manager: brak nav; deep-link Beta → problem

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `support.spec.ts` · `merchant manager support search on Beta is denied` · `@security` |
| Project | `chromium-manager` |
| Kroki | merchants → `nav-link-support` count 0 → `support.goto` → `search(merchantBetaId)` |
| Asercje | `problem-details-card`; tabela `Support search results` count 0 |

### PW-W2-E2E-071 — Admin Support wyszukuje Beta

| | |
|---|---|
| Pokrycie | designed |
| Prio | P1 |

### PW-W2-E2E-080 — Error Lab 400

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `Error Lab 400 shows problem+json from the real backend` |
| HTTP | `waitForResponse` `.../error-lab/trigger-400` status 400 |
| Asercje | `problem-details-card` badge 400; brak `Authorization` w body odpowiedzi |

### PW-W2-E2E-081 — Error Lab 401 (nie mock 429)

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `Error Lab 401 is a live unauthorized response, not a 429 mock` |
| HTTP | `trigger-401` → 401; przycisk `error-lab-trigger-429` **visible**, nie klikany |

### PW-W2-E2E-082 — Error Lab 412

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `Error Lab 412 is a live stale If-Match from the backend` · `@security` |
| HTTP | `trigger-412` → 412 + problem badge |

### PW-W2-E2E-083 — Error Lab 403 / 404 / 406 / 409 / 415 / 428 / 304

| | |
|---|---|
| Pokrycie | designed (triggery UI: `error-lab-trigger-{status}` w `error-lab.vue`) |
| Prio | P1 |
| Poza | 429 = BFF mock, nie POM |

---

## G. Payment orders (manager) — `chromium-manager`

Szczegóły filtrów/kontrolek: [../rls-filters-composition-lab/03-playwright-e2e-catalog.md](../rls-filters-composition-lab/03-playwright-e2e-catalog.md).

### PW-W2-E2E-090 — Create z Idempotency-Key → detail

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `payments-create.spec.ts` · `creates a payment order with Idempotency-Key and lands on detail` |
| HTTP | POST `/api/merchants/{alpha}/payment-orders` header `Idempotency-Key` = wartość z formularza |
| Asercje | URL `/admin/merchants/{alpha}/payments/{uuid}`; reference na detail |

### PW-W2-E2E-091 — Replay ten sam klucz 200; mismatch 409

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `replaying the same Idempotency-Key returns the same order; mismatch is 409` |
| HTTP | 201 → replay 200 ten sam `paymentOrderId` → inny body ten sam key → 409 `idempotency_conflict` |

### PW-W2-E2E-092 — Authorize + capture z If-Match

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `payments-lifecycle.spec.ts` · `authorize then capture from the payment detail drawer with If-Match` |
| HTTP | GET detail ETag → POST authorize `If-Match` + `Idempotency-Key` → capture `If-Match` z drawera |
| Asercje | Created → Authorized → Captured |

### PW-W2-E2E-093 — Stale If-Match 412, stan CREATED

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `stale If-Match on authorize shows 412 problem+json in the drawer` |
| Kroki | fill If-Match `"stale-etag"` → submit authorize |
| Asercje | HTTP 412; problem badge; `GET` status `CREATED`; API authorize stale też 412 |

### PW-W2-E2E-094 — Cancel z ConfirmModal (submit)

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `cancel from CREATED uses ConfirmModal` |
| Asercje | status detail `Cancelled` |

### PW-W2-E2E-095 — Filtry date/status/reference w query

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `payments-lifecycle.spec.ts` (ostatni test) + `payments-filters.spec.ts` |
| Oracle | URL query = `BffClient.listPaymentOrders` |

### PW-W2-E2E-096 — USelect + `data-status`; ConfirmModal dismiss bez POST cancel

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `payments-hard-controls.spec.ts` |
| Asercje | `select[name=status]` count 0; badge `data-status=CREATED`; dismiss → GET nadal CREATED |

### PW-W2-E2E-097 — Upload evidence; export CSV bez tokenu w pliku

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `payments-evidence-export.spec.ts` |
| Asercje | `evidence-file-name` = `sample-evidence.txt`; download `.csv`; treść bez `eyJ` / Bearer |

---

## H. RBAC i platforma

### PW-W2-E2E-100 — Alpha OK; manager Beta + Users deny

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `auth-rbac.spec.ts` · `real roles see Alpha payments; merchant manager is denied Beta and Users` |
| Project | `chromium-rbac` (dwa `browser.newContext`) |
| Asercje | oba Alpha `order(s) across`; manager Users/Audit nav hidden; Beta alert permission; `payment-orders-table` not visible; `users.expectForbidden()` |

### PW-W2-E2E-110 — Users table (admin)

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `users.spec.ts` · `platform admin sees the users table and page summary` |
| Oracle | `api.listUsers()` 200 + copy `Page N · … user(s) shown` |

### PW-W2-E2E-111 — Audit filtr + export JSON + drawer

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `audit.spec.ts` |
| HTTP | GET `/api/audit/export.json`; download `.json` bez tokenu; `audit-entry-drawer` |

### PW-W2-E2E-112 — Tenant settings PATCH z If-Match z GET ETag

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `tenant-settings.spec.ts` · `PATCH tenant settings forwards GET ETag as If-Match` |
| HTTP | GET `/api/tenants/current/settings` ETag → PATCH ten sam `If-Match` |

---

## I. Laby (wskaźniki — pełne katalogi MRL/RFC)

### PW-W2-E2E-120 — Session Lab cookie policy vs `document.cookie`

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` |
| Katalog | [../payu-bank-mirror-labs/03-playwright-e2e-catalog.md](../payu-bank-mirror-labs/03-playwright-e2e-catalog.md) E2E-010/011 |

### PW-W2-E2E-121 — CSRF demo 403 `csrf_failed`

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` · `csrf demo without token returns 403 csrf_failed` |
| HTTP | POST `/api/session-lab/csrf-demo` → 403 `error=csrf_failed` |

### PW-W2-E2E-122 — Dwa contexty: revoke device

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` · `two contexts sharing storageState can revoke a device` |
| Asercje | oba `session-lab-device-list`; click `session-lab-revoke-*` (brak asercji 401 na drugim — MRL designed) |

### PW-W2-E2E-123 — Network Lab 503 → 200 bez `fulfill`

| | |
|---|---|
| Pokrycie | existing-pom `network-lab.spec.ts` |
| HTTP | dwa POST `/api/network-lab/trigger-503-retry`: 503 potem 200 |

### PW-W2-E2E-124 — Mirror hub + CSV statements; widget same-origin

| | |
|---|---|
| Pokrycie | existing-pom `mirror-lab.spec.ts` |
| Asercje | download `statement*.csv`; `widget-session-id` po `openWidget()` |

### PW-W2-E2E-125 — RLS Alpha vs probe 404; admin compare

| | |
|---|---|
| Pokrycie | existing-pom `rls-lab.spec.ts` |
| Katalog | RFC E2E-002–005 |

### PW-W2-E2E-126 — TLS origin `:8443` RLS + filtr amount

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` · `playwright.pom.tls.config.ts` |
| Katalog | [../rls-filters-composition-lab/09-wave-b-stack-tls-catalog.md](../rls-filters-composition-lab/09-wave-b-stack-tls-catalog.md) |
