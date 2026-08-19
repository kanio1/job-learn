# 03 — Katalog Playwright E2E (UI)

Warstwa: Chromium live POM (`tests-pom`). Zero `fulfill`.  
Każdy wiersz **existing-pom** = jeden konkretny `test('…')`. `designed` = nadal brak specu.

Skrypty HTTP + kombinacje person (tenant.admin, ALPHA_002, dual-control): [09](09-core-domain-flows.md). Brzeg Caddy/TLS: [10](10-full-stack-edge-flows.md). Checkout hops: [CPL 09](../checkout-protocol-lab/09-protocol-flow-simulations.md).

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
| Pokrycie | existing-pom |
| Spec | `session-guest.spec.ts` · `unauthenticated admin and lab paths land on login with redirectTo` |
| Prio | P1 |
| Kroki | `goto` `/admin/users`, `/admin/merchants/{alpha}/payments`, `/error-lab`, `/admin/checkout-lab` |
| Asercje | URL `/login?redirectTo=` **równe** ścieżce; `LoginPage.expectLoaded()`. Nie `/psp/checkout` (hosted bez sesji). |

### PW-W2-E2E-010 — Logout **aplikacji** → login → ponowny admin zablokowany

| | |
|---|---|
| Pokrycie | existing-pom |
| Spec | `session.spec.ts` · `logout returns to login and blocks admin again` |
| Kroki | `merchants.goto` → `userMenu.signOut()` (`logout-control` + menuitem Sign out) → `goto('/admin/merchants')` |
| Asercje | URL `/login` po sign-out **i** po drugim goto |
| Nie asertuje | Keycloak `end_session` (to jest E2E-013). Kontrakt: [session-bff-oidc-contract](../session-bff-oidc-contract.md) UC-SESS-01 |

### PW-W2-E2E-013 — End OIDC session (Session Lab)

| | |
|---|---|
| Pokrycie | existing-pom · FR-OIDC / FR-S04b |
| Spec | `session.spec.ts` · hop: `Session Lab end OIDC…`; JSON: `Session Lab end-session JSON…` · `chromium-session` |
| Kroki | JSON: `page.request.post` (to samo cookie, bez hopu CDP). Hop: klik `session-lab-end-oidc` → nawigacja |
| Asercje | POST 200 `{ ended: true, endSessionUrl }` z `page.request`; hop URL `client_id` + `post_logout_redirect_uri`; **brak** `id_token_hint`; po confirm (jeśli jest) `/login`; ponowny `/admin/merchants` → `/login`; brak JWT w Web Storage |
| Uwaga | IdP może pokazać confirm. Realm JSON ma `post.logout.redirect.uris`; stary volume = `provision-keycloak-logout-uris.py`. |

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
| Po unlock | `goto /admin/merchants` nadal `/login` (MRL E2E-022) |

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
| Pokrycie | existing-pom |
| Tytuł | `platform admin create form requires tenant reference and persists` |
| Spec | `merchants.spec.ts` |
| Kroki | open create → `create-merchant-tenant-reference` visible → fill reference/name/`TENANT_ALPHA` → submit |
| Asercje | wiersz listy; `listMerchants` zawiera `merchantReference` |
| Prio | P1 |

### PW-W2-E2E-025 — 409 duplikat w formularzu UI

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `duplicate merchant reference shows 409 on the create form` |
| Spec | `merchants.spec.ts` |
| Kroki | API create → UI ten sam `merchantReference` + tenant → submit |
| Asercje | POST 409; `alert` z `already exists`; pole reference zachowane |
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
| Pokrycie | existing-pom `command-palette.spec.ts` (`test.for` destynacje) |
| Prio | P2 |

### PW-W2-E2E-032 — ARIA snapshot login

| | |
|---|---|
| Pokrycie | existing-pom `session-guest.spec.ts` · `login page matches ARIA snapshot` |
| Prio | P2 |
| Forbidden | poza zakresem (inna persona) |

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
| Pokrycie | existing-pom |
| Spec | `payments-lifecycle.spec.ts` · `merchant manager does not see the internal notes form` |
| Prio | P1 |
| Kroki | manager POST order Alpha → detail |
| Asercje | `payment-note-body` count 0 (capability notes tylko platform) |

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
| Pokrycie | existing-pom |
| Spec | `support-admin.spec.ts` · `platform admin support search on Beta returns results` · `@security` |
| Project | `chromium-admin` (`testMatch` zawiera `support-admin`) |
| Prio | P1 |
| Kroki | `support.goto` → `search(merchantBetaId)` |
| Asercje | `error-state` count 0; tabela `Support search results` (`expectResults()`). `No results` = fail (seed Beta). |

### PW-W2-E2E-080 — Error Lab 400

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `Error Lab 400 is a live backend validation problem (not a 429 mock)` |
| HTTP | `page.request.fetch` `.../error-lab/trigger-400` as **merchant manager** |
| Asercje | status **400** `application/problem+json` + `status` w JSON; `expectNoAuthorizationInNetworkResponse` |

### PW-W2-E2E-081 — Error Lab 401 (nie mock 429)

| | |
|---|---|
| Pokrycie | existing-pom |
| Tytuł | `Error Lab 401 canary is a live unauthorized UI response, not a 429 mock` |
| HTTP | click `error-lab-trigger-401` + `waitForResponse`; przycisk `error-lab-trigger-429` **visible**, nie klikany |
| Asercje | 401 problem+json; `problem.expectVisible` |

### PW-W2-E2E-082 — Error Lab 412

| | |
|---|---|
| Pokrycie | existing-pom `error-lab-manager.spec.ts` (`chromium-manager`) |
| Tytuł | `Error Lab 412 is a live stale If-Match from the backend` · `@security` |
| HTTP | `trigger-412` z If-Match `"v99"` (`\"v{n}\"`); malformed `"stale-etag"` to 400, nie 412 |
| Asercje | status **412** problem+json (brak create → 503 `lab_unavailable`, nie 412) |

### PW-W2-E2E-083 — Error Lab 403 / 404 / 406 / 415 (admin) + 409 / 428 / 304 (manager)

| | |
|---|---|
| Pokrycie | existing-pom `error-lab.spec.ts` (admin) + `error-lab-manager.spec.ts` (create) |
| Prio | P1 |
| Tytuł | admin: remaining statuses without create; manager: 409/428/304 |
| Poza | 429 = BFF mock, nie POM |
| Oracle | dokładny `expect(status).toBe(status)`; 4xx `application/problem+json` + `status` w JSON (oprócz 304); 304 **nie** akceptuje 503 (brak seed = 503 `lab_unavailable`); 403 = prawdziwy token + POST Alpha (2xx create → 503 `lab_unavailable`, nie 201) |

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
| Kroki | fill If-Match `"v99"` (format Spring `\"v{n}\"`) → submit authorize |
| Asercje | HTTP 412; problem badge; `GET` status `CREATED`; API authorize `"v99"` też 412 |

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
| Katalog | [../payu-bank-mirror-labs/03-playwright-e2e-catalog.md](../payu-bank-mirror-labs/03-playwright-e2e-catalog.md) E2E-010/011. OIDC hop / 4 KB: [session-bff-oidc-contract](../session-bff-oidc-contract.md) (designed E2E-013) |

### PW-W2-E2E-121 — CSRF demo 403 `csrf_failed`

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` · `csrf demo without token returns 403 csrf_failed` |
| HTTP | POST `/api/session-lab/csrf-demo` → 403 `error=csrf_failed` |

### PW-W2-E2E-122 — Dwa contexty: revoke device

| | |
|---|---|
| Pokrycie | existing-pom `session.spec.ts` · `two contexts sharing storageState can revoke a device` · project `chromium-session` |
| Asercje | oba `session-lab-device-list`; click Revoke (brak asercji 401 na drugim — MRL designed) |

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

---

## J. Poza Wave 2 (indeks, bez nowych ID)

Live POM ma specy, których ten katalog nie numeruje. Ślad: [playbook 06](../playwright-method-playbook/06-scenario-catalog.md), [real-stack](../../status/roadmaps/playwright-real-stack-learning/).

| Spec | Co asertuje |
|---|---|
| `payments-offline.spec.ts` | banner offline (`context.setOffline`) |
| `payments-expiration.spec.ts` | countdown `expiresAt` na AUTHORIZED |
| `payments-polling.spec.ts` | manual/auto refresh GET, nie mock status |
| `payments-async-export.spec.ts` | export-jobs 202 + poll READY + CSV |
| `payments-conditional.spec.ts` | If-None-Match 304; HEAD 200 + ETag; PATCH bez If-Match 428 |
| `payments-illegal-transitions.spec.ts` | SCN-ILL / SCN-IFM REST |
| `payments-metamorphic.spec.ts` | MR-IDEM / MR-UNIQ / MR-ETAG / MR-FILTER |
| `worker-world.spec.ts` | `MERCHANT-Wn` 201 vs Alpha 403 |
| `readonly-rbac.spec.ts` | lista tak, create/lifecycle/notes nie |
| `tenant-scope.spec.ts` | SCN-ISO + UC-W2-20/21 |
| `visual-lab.spec.ts` / `aria-snapshots.spec.ts` | `PLAYWRIGHT_VISUAL=1` |
| `a11y-axe.spec.ts` | login + registry, brak serious axe | |
