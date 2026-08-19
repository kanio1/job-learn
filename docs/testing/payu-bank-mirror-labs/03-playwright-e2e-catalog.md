# 03 — Katalog Playwright E2E (UI)

Warstwa: Chromium. **POM** = `tests-pom`, zero fulfill. Mocked `tests/e2e` **nie istnieje** — `page.route` / abort / fulfill / HAR to `designed`, nie `existing-pom`.  
Pokrycie: `existing-pom` | `designed`.

Selektor bazowy: `data-testid`. Idle overlay: `session-lab-idle-lock` / `session-lab-idle-unlock`.  
SSR: `/error-lab` tylko przez sidebar po SPA `/admin/*`.  
Hosted: `/psp/checkout/{id}` ma `layout: false` — **nie** dashboard overlay.

---

## A. Hub, flaga, nawigacja

### PW-MRL-E2E-001 — Hub mirror-lab z kartami trzech światów

| | |
|---|---|
| Pokrycie | existing-pom (`mirror-lab.spec.ts`) |
| Prio | P0 |
| Auth | Keycloak mock |
| Kroki | `/admin/merchants` → `nav-link-mirror-lab` |
| Asercje | tekst „Three identity worlds”; `mirror-lab-card-session` visible |

### PW-MRL-E2E-002 — Hub → Session / Visual / Network / Bank

| | |
|---|---|
| Pokrycie | existing-pom (nawigacja w beforeEach visual/network) + existing-pom bank |
| Prio | P0 |
| Asercje | URL-e `/admin/session-lab`, `/admin/visual-lab`, `/admin/network-lab`, `/admin/mirror-lab/bank` |

### PW-MRL-E2E-003 — Flaga FE off: nav ukryty, deep link 404

| | |
|---|---|
| Pokrycie | existing-pom `mirror-lab-flag-off.spec.ts` · `playwright.mirror-flag-off.config.ts` (:3012) · GAP-T01 |
| Prio | P0 |
| Preconditions | osobny project `NUXT_PUBLIC_MIRROR_LAB_ENABLED=false` |
| Asercje | `nav-link-mirror-lab` count 0; `/admin/mirror-lab` i `/admin/session-lab` 404; BFF GET csrf 404 |

### PW-MRL-E2E-004 — Widget ukryty gdy flaga off; widoczny gdy on

| | |
|---|---|
| Pokrycie | existing-pom `mirror-lab.spec.ts` · `widget iframe is same-origin` (`widget-session-id`). Hub/session flag-off: existing-pom GAP-T01. Widget path off: still designed |
| Prio | P1 |
| Kroki | hub CPL → Widget iframe |
| Asercje | `widget-session-id` visible gdy flag on |

### PW-MRL-E2E-005 — Unauth `/admin/mirror-lab` → login

| | |
|---|---|
| Pokrycie | existing-pom analog (`session-guest` na session-lab / merchants) |
| Prio | P0 |
| Designed | to samo dla `/admin/mirror-lab` i `/admin/mirror-lab/bank` |

---

## B. Session Lab

### PW-MRL-E2E-010 — Cookie policy: nuxt-session HttpOnly

| | |
|---|---|
| Pokrycie | existing-pom |
| Suite | POM |
| Kroki | login → `/admin/session-lab` |
| Asercje | `session-lab-cookie-policy` zawiera `nuxt-session` i `httpOnly`; `context.cookies()` HttpOnly true |
| Nie asertuje | SameSite, Secure vs prawdziwe cookie, Domain Keycloak |
| FR | S01 · P0 |

### PW-MRL-E2E-013 — SameSite Lax z `context.cookies()`

| | |
|---|---|
| Pokrycie | existing-pom `expectSessionCookieSameSiteLax` |
| Asercje | `nuxt-session.sameSite` Lax/lax; **nie** z JSON policy |
| FR | S01 · P1 |

### PW-MRL-E2E-014 — Policy JSON `secure:false` vs TLS cookie — **designed**

| | |
|---|---|
| Pokrycie | designed / docs-only drift (GAP-SESS-04) |
| Asercje | HTTP POM: JSON `secure: false` zgodne z cookie. TLS: cookie `Secure=true`, JSON nadal false — oracle = `context.cookies()`, nie UI |
| FR | S01 · P1 |

### PW-MRL-E2E-011 — document.cookie nie zawiera nuxt-session

| | |
|---|---|
| Pokrycie | existing-pom (`session-lab-js-cookies` empty/HttpOnly) |
| FR | S01 · P0 |

### PW-MRL-E2E-012 — Brak JWT w Web Storage po login

| | |
|---|---|
| Pokrycie | existing-pom `expectNoTokenInBrowserStorage` |
| FR | S08 · P0 |

### PW-MRL-E2E-020 — Idle lock po clock.fastForward(121_000)

| | |
|---|---|
| Pokrycie | existing-pom |
| Suite | POM |
| Kroki | `clock.install` → session-lab → `fastForward(121_000)` |
| Asercje | `session-lab-idle-lock` visible |
| FR | S03 · P0 |

### PW-MRL-E2E-021 — Unlock wylogowuje na `/login`

| | |
|---|---|
| Pokrycie | existing-pom |
| Kroki | po lock click `session-lab-idle-unlock` |
| Asercje | URL `/login` (nie `/admin/merchants`) |
| FR | S03/S04a · P0 |
| Uwaga review | Unlock **musi** `session.clear()`; sam `to="/login"` zostawia sesję. To **nie** jest Keycloak `end_session`. |

### PW-MRL-E2E-026 — End OIDC session (`session-lab-end-oidc`)

| | |
|---|---|
| Pokrycie | existing-pom `session.spec.ts` · FR-S04b |
| Kroki | klik `session-lab-end-oidc` → POST `/api/session-lab/end-session` → `endSessionUrl` |
| Asercje | query `client_id` + `post_logout_redirect_uri`; brak `id_token_hint`; `nuxt-session` znika |
| FR | S04b · P0 |

### PW-MRL-E2E-027 — Menu Sign out **nie** woła `end_session`

| | |
|---|---|
| Pokrycie | existing-pom `session.spec.ts` (kontrast E2E-010 / MRL-021) |
| Kroki | Sign out z `/admin/merchants`; `waitForRequest` do `/protocol/openid-connect/logout` count 0 |
| FR | S04a · P1 |

### PW-MRL-E2E-028 — Cookie `nuxt-session` &lt; 4 KB

| | |
|---|---|
| Pokrycie | existing-pom `expectSessionCookieUnderUaLimit` |
| Asercje | długość value cookie poniżej 4096; sesja bez `id_token` |
| FR | S08 + limit UA · P0 |

### PW-MRL-E2E-022 — Po Unlock `/admin/merchants` nadal login

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` Unlock → `/admin/merchants` still `/login` |
| Prio | P0 |
| Kroki | Unlock → `goto /admin/merchants` |
| Asercje | nadal `/login`; brak danych merchant |

### PW-MRL-E2E-023 — Idle overlay na innym `/admin/**` (np. merchants)

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` idle overlay on `/admin/merchants` |
| Prio | P1 |
| Uwaga | overlay jest w `dashboard.vue` — planowo globalny |
| Kroki | clock na `/admin/merchants` |
| Asercje | ten sam `session-lab-idle-lock` |

### PW-MRL-E2E-024 — Hosted checkout **bez** idle overlay

| | |
|---|---|
| Pokrycie | existing-pom (`checkout-lab.spec.ts` hosted `session-lab-idle-lock` count 0) |
| Suite | mocked guest context |
| FR | S02/S03 · P0 |

### PW-MRL-E2E-025 — BVA idle: TTL-1s brak lock

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` · BVA-MRL-010 (`119_000` then `+2000`) |
| Kroki | `fastForward(119_000)` |
| Asercje | overlay count 0; po `+2000` visible |

### PW-MRL-E2E-030 — Dwa contexty, revoke device

| | |
|---|---|
| Pokrycie | existing-pom `session.spec.ts` — POST revoke 200, GET devices nadal 200, revoked id usunięty (as-built: wspólna sesja, nie 401) |
| Designed | product session-kill / 401 na drugim kontekście — poza zakresem |
| FR | S05 · P1 |

### PW-MRL-E2E-040 — CSRF demo bez tokenu → 403 `csrf_failed`

| | |
|---|---|
| Pokrycie | existing-pom `waitForResponse` POST csrf-demo |
| Asercje | status 403; body.error `csrf_failed` |
| FR | S06 · P1 |

### PW-MRL-E2E-041 — CSRF demo z tokenem z GET csrf → 200

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` · `csrf demo with token returns ok` |
| Kroki | GET csrf (ustawia cookie) → POST z `X-Csrf-Token` |
| Asercje | 200 `{ status: ok }` |

### PW-MRL-E2E-042 — Cookie `mrl-csrf` widoczne w `document.cookie` (lekcja non-HttpOnly)

| | |
|---|---|
| Pokrycie | existing-pom `session-lab.spec.ts` · `mrl-csrf is visible on document.cookie after GET csrf` |
| Kontrast | `nuxt-session` niewidoczne |

---

## C. Visual Lab

Kafelki: `visual-tile-merchant-badge`, `visual-tile-payment-badge`, `visual-tile-problem-details`, `visual-tile-hosted-cta`, `visual-tile-idle-lock`, `visual-tile-dark`, `visual-tile-expired`.  
Mask: `tests/e2e/visual-lab-mask.css`. Próg 0.02.

### PW-MRL-E2E-100 — Wejście Visual Lab z huba

| Pokrycie | existing-pom | Prio | P0 |

### PW-MRL-E2E-110–116 — Screenshot każdego kafelka

| Pokrycie | existing-pom (`visual-lab.spec.ts` loop) | FR-V02 | P0 |

### PW-MRL-E2E-120 — Full page + stylePath mask

| Pokrycie | designed · FR-V04 | Prio | P1 |

### PW-MRL-E2E-130 — Dark tile

| Pokrycie | existing-pom (`visual-tile-dark`) | FR-V03 | P1 |

### PW-MRL-E2E-140 — Break visual tagged expected fail

| Pokrycie | existing-pom `@visual-negative` | FR-V05 | P1 |
| Uwaga | nie w default CI (`grepInvert`) |

### PW-MRL-E2E-190 — Anti-case: nie screenshotować list UUID

| Pokrycie | docs-only · FR-V06 |

---

## D. Network Lab UI

### PW-MRL-E2E-200 — Live 503 then 200 (POM, zero fulfill)

| | |
|---|---|
| Pokrycie | existing-pom |
| Kroki | dwa click `network-lab-trigger-503`; `waitForResponse` |
| Asercje | 503 potem 200 |
| FR | N01 · P0 |

### PW-MRL-E2E-201 — Mocked stateful 503→200

| Pokrycie | designed (mocked suite removed) | FR-N01 · P0 |

### PW-MRL-E2E-202 — Trzeci trigger po TTL 10s znowu 503 (izolacja)

| Pokrycie | existing-pom `network-lab.spec.ts` (503→200→503; TTL via GET `retry-window` `remainingMs`) · GAP-T08 |
| Suite | POM | `expect.poll` na peek GET — bez `setTimeout` / `waitForTimeout` |

### PW-MRL-E2E-210 — Abort slow → ErrorState / nie `"status":"ok"`

| Pokrycie | designed (`route.abort` poza live POM) | FR-N02 · P0 |

### PW-MRL-E2E-211 — `context.setOffline(true)`

| Pokrycie | existing-pom `network-lab.spec.ts` setOffline + `error-state` |

### PW-MRL-E2E-220 — Lie body `success` w UI

| Pokrycie | existing-pom (UI contains success) |
| Designed | oracle fulfillment **nie** CONFIRMED (CPL analog) |
| FR | N03 · P0 |

### PW-MRL-E2E-230 — HAR replay bez Cookie/Authorization w fixture

| Pokrycie | designed (mocked suite removed) | FR-N05 · P1 |

---

## E. PayU mirrors UI (delta vs katalog CPL)

Pełne booking/hosted/return: [checkout-protocol-lab/03](../checkout-protocol-lab/03-playwright-e2e-catalog.md). Tu tylko Wave 3.

### PW-MRL-E2E-300 — Hosted `lang=pl` copy

| Pokrycie | designed | `psp-hosted-lang` zawiera Polski | FR-P02 · P1 |

### PW-MRL-E2E-310 — Expired link testid

| Pokrycie | existing-pom (`psp-link-expired` scenariusz EXPIRED_LINK) | FR-P04 · P1 |

### PW-MRL-E2E-320 — Widget iframe Approve (`frameLocator`)

| | |
|---|---|
| Pokrycie | existing-pom |
| Kroki | `/admin/checkout-lab/widget` → fill session id → Load → `contentFrame()` → `psp-approve` |
| Asercje | `psp-outcome` w ramce (approved) **oraz** BFF fulfillment `CONFIRMED` |
| FR | P05 · P1 |

### PW-MRL-E2E-321 — Widget same-origin (POM live)

| Pokrycie | existing-pom `checkout-lab.spec.ts` live session + `contentFrame` Approve → `psp-outcome` + CONFIRMED | P2 |

### PW-MRL-E2E-330 — Grant contrast panel trzy kolumny

| Pokrycie | designed (karta `checkout-lab-grant-contrast` na hubie gdy flag on) | FR-P06 · P1 |

---

## F. Bank-like UI

Merchant lab: `00000000-0000-0000-0000-0000000000b1`.  
Approve: `UInput` `approval-id` — fill native `input`, nie `innerText` / `page.evaluate(fetch)`.

### PW-MRL-E2E-400 — Step-up: submit ≥ 10000 → challenge z kwotą

| Pokrycie | designed | `step-up-challenge` pokazuje amount + merchantId | FR-B01/B06 · P2 |

### PW-MRL-E2E-401 — Step-up confirm → 200 accepted

| Pokrycie | designed | header `X-Lab-Step-Up: confirmed` na BFF | P2 |

### PW-MRL-E2E-402 — Amount 9999 bez challenge

| Pokrycie | designed · BVA | P2 |

### PW-MRL-E2E-410 — Statement CSV download filename

| Pokrycie | existing-pom `mirror-lab.spec.ts` | `waitForEvent('download')` | FR-B02 · P2 |

### PW-MRL-E2E-411 — Statement PDF magic bytes (mocked fulfill)

| Pokrycie | designed (mocked suite removed) | bytes `[0..4] == %PDF-` | P0 review |

### PW-MRL-E2E-412 — Statement PDF magic bytes (żywy BFF)

| Pokrycie | existing-pom `mirror-lab.spec.ts` live BFF download `%PDF-` · GAP-T04 | Suite | POM | P1 |

### PW-MRL-E2E-420 — Dispute open + `setInputFiles` evidence txt

| Pokrycie | designed | `dispute-id` nonempty; upload zamyka spór | FR-B03 · P2 |

### PW-MRL-E2E-421 — Evidence exe → 415 w UI

| Pokrycie | designed | P2 |

### PW-MRL-E2E-430 — Maker nie self-approve; checker Approve z wklejonym id

| | |
|---|---|
| Pokrycie | existing-pom `mirror-lab-rbac.spec.ts` |
| Kroki | manager Create → inputValue id → Approve 403; admin fill id → Approve 200 |
| Zakaz | `page.evaluate` + raw `fetch` |
| FR | B04 · P2 |

### PW-MRL-E2E-431 — Checker ponowne Approve → 409 w `approval-result`

| Pokrycie | designed (RA ma 409; UI nie) | P2 |

### PW-MRL-E2E-440 — Consent grant → TPP via **header** → revoke

| Pokrycie | designed · GAP-T09 | `/consent/mirror-lab`; `X-Lab-Consent-Token` | FR-B05 · P2 |

### PW-MRL-E2E-441 — Revoke → TPP 403 w UI

| Pokrycie | designed | P2 |

---

## G. Learner

Puste / skip: `tests-pom-learner/specs/{session-guest,visual-lab,network-lab}.spec.ts`.  
Copy-from-reference: nie importować page objects z `tests-pom`.
