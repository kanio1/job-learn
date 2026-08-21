# Kontrakt sesji BFF + OIDC (as-built)

Źródło prawdy dla **dwóch logoutów**, limitu cookie i tego, czego TC jeszcze nie asertują.  
Nie implementuje speców — tylko mapuje produkt vs katalogi.

Powiązane: [live-pom-wave-2](live-pom-wave-2/), [payu-bank-mirror-labs](payu-bank-mirror-labs/), [playwright-real-stack-learning](../../status/roadmaps/playwright-real-stack-learning/), [INFRA-BFF-01](../../status/roadmaps/browser-session-visual-network-lab/01-infra-keycloak-bff-security.md).

---

## Model (produkt)

| Warstwa | Gdzie | Co trzyma |
|---|---|---|
| BFF (Nuxt) | HttpOnly cookie `nuxt-session` (sealed) | `user` + `secure.accessToken` |
| Przeglądarka JS | `localStorage` / `sessionStorage` | **bez** JWT |
| IdP | cookies na originie Keycloak (`:8081` / compose) | SSO (`AUTH_SESSION_ID`, …) |
| Postgres 18 | domena merchant/payment | **nie** sesja BFF |

`id_token` **nie** jest zapisywany w cookie (limit UA ~4096 B, RFC 6265 §6.1). Logout RP bez `id_token_hint` używa `client_id` + `post_logout_redirect_uri` (OIDC RP-Initiated Logout).

`GET /api/session-lab/cookie-policy` zwraca **edukacyjny JSON** z `secure: false` i `sameSite: 'lax'`. To **nie** jest odczyt atrybutów z `Set-Cookie`. Na TLS prawdziwe `nuxt-session` ma `Secure=true` (POM `tls-lab`).

---

## Dwa logouty (MUST rozróżniać w UC/TC)

| Ścieżka | UI | Kod | BFF cookie | SSO Keycloak | TC dziś |
|---|---|---|---|---|---|
| A — płytki (named) | **Sign out of dashboard only**, idle Unlock | `auth.logoutShallow()` → `session.clear()` + `/login?logout=shallow` | **puste** `nuxt-session` (as-built; nie RFC delete) | **zostaje** | `session.spec.ts` shallow + idle `session-lab.spec.ts`; oracle `expectSessionCookieCleared` |
| B — głęboki (default) | menu **Sign out**; Session Lab `session-lab-end-oidc` | GET `/api/auth/end-session` (produkt) albo GET `/api/session-lab/end-session` (klasa) → `clearUserSession` + **302** na URL `…/logout?client_id=&post_logout_redirect_uri=`. POST na te same ścieżki zwraca JSON `{ ended, endSessionUrl }` (oracle bez hopu). | puste (`expectSessionCookieCleared`) | hop `end_session` (bez `id_token_hint`) | existing-pom `session.spec.ts` E2E-027 (Sign out) + E2E-013 (Session Lab) |

Idle Unlock = ścieżka A. Default **Sign out** = ścieżka B (lab jest jednym RP). FR historyczne „logout BFF + OIDC tylko w Session Lab” jest **nieaktualne**.

Na `/login` po ścieżce A: alert SSO resume + **Use a different account** (`/auth/keycloak?prompt=login`).

Nazwa lekcji „OIDC revoke” w learning-map = **end_session** (RP logout), nie RFC 7009 token revocation.

Podgląd diagramów: otwórz [diagrams/index.html](diagrams/index.html) w przeglądarce (`corepack pnpm --dir tools/beautiful-mermaid run view`). Źródło Mermaid: [diagrams/dual-depth-logout.mmd](diagrams/dual-depth-logout.mmd).

---

## Use cases

### UC-SESS-01 — Operator Sign out (głęboki, default) — P0

- **Aktor:** platform admin.
- **Precondition:** `nuxt-session` HttpOnly; SSO Keycloak może istnieć.
- **Kroki:** registry → `logout-control` → **Sign out** (exact) → Keycloak `end_session` (confirm jeśli jest) → `/login` → opcjonalnie Continue to Keycloak.
- **Oracle:** hop `/protocol/openid-connect/logout` z `client_id` + `post_logout_redirect_uri`, **brak** `id_token_hint`; `/login`; brak JWT w Web Storage; `nuxt-session` puste; następny Continue pokazuje formularz Keycloak **Sign in to your account** (nie ciche SSO).
- **Nie-oracle:** Sign out **nie** może zostawić SSO (to jest stary kontrakt E2E-027).
- **TC:** PW-MRL-E2E-027 (przepisany), PW-W2-E2E-010 (powrót na `/login` + blokada admin).

### UC-SESS-01a — Sign out of dashboard only (płytki, named) — P0

- **Aktor:** platform admin.
- **Kroki:** registry → `logout-control` → **Sign out of dashboard only** → `/login?logout=shallow`.
- **Oracle:** **zero** requestów do Keycloak `end_session`; alert `login-sso-resume-notice`; `expectSessionCookieCleared`; **Use a different account** → authorize z `prompt=login` i formularz Sign in.
- **TC:** `session.spec.ts` · `Sign out of dashboard only keeps Keycloak SSO and explains resume`.

### UC-SESS-02 — Idle Unlock (świat aplikacji) — P0

- **Aktor:** platform admin na `/admin/session-lab`.
- **Kroki:** `page.clock` + `fastForward(121_000)` → Unlock.
- **Oracle:** `/login`; sesja BFF pusta.
- **Nie-oracle:** SSO wygaszone.
- **TC:** PW-W2-E2E-012, MRL E2E-022 (ponowny `/admin/merchants` → `/login`), UC-MRL-01.

### UC-SESS-03 — End OIDC session (świat SSO) — P0

- **Aktor:** platform admin na Session Lab.
- **Kroki:** POST `/api/session-lab/end-session` (JSON przez `page.request`, bez hopu). Hop: GET `/api/session-lab/end-session` (302) przez klik UI. CDP nie oddaje body po nawigacji — JSON i hop są dwoma testami w `session.spec.ts`.
- **Oracle:** body `{ ended: true, endSessionUrl }`; URL (body i hop) zawiera `client_id=` i `post_logout_redirect_uri=`; **brak** `id_token_hint`; po confirm (jeśli jest) `/login`; ponowny `/admin/merchants` → `/login`; Web Storage bez JWT; `expectSessionCookieCleared`.
- **Uwaga lab:** Keycloak **może** pokazać confirm (brak `id_token_hint`). Realm JSON ma additive `post.logout.redirect.uris` (HTTP `:3000/login` + HTTPS app vhost). Istniejący volume: `python3 scripts/provision-keycloak-logout-uris.py` (`--import-realm` nie aktualizuje).
- **TC:** PW-W2-E2E-013, PW-MRL-E2E-026, PW-MRL-API-024 — existing-pom `session.spec.ts`.

### UC-SESS-04 — Cookie poniżej limitu UA — P0

- **Aktor:** po OIDC callback.
- **Oracle:** `nuxt-session` value length + overhead **&lt; 4096**; sealed session **nie** zawiera drugiego JWT (`id_token`). Testy **nie** dekodują sealed blobu do ról; asercja rozmiaru + kontrakt `SecureSessionData` (tylko `accessToken`).
- **TC:** PW-W2-SEC-005, PW-MRL-E2E-028 — existing-pom (`expectSessionCookieUnderUaLimit`).

### UC-SESS-05 — Atrybuty cookie z Playwright, nie z policy JSON — P1

- **Kroki:** `page.context().cookies()` dla `nuxt-session`.
- **Oracle HTTP:** `httpOnly=true`, Playwright `sameSite: "Lax"`, `secure=false`, `path=/`.
- **Oracle TLS:** `secure=true` (E2E-056). Policy JSON nadal `secure: false` — **nie** używać jako oracle TLS.
- **TC:** PW-W2-SEC-006, PW-MRL-E2E-013 — existing-pom (`expectSessionCookieSameSiteLax`; TLS Secure = E2E-056).

### UC-SESS-06 — Guest / hosted — P0

Bez zmian: pusty `storageState` → `/login`; `/psp/checkout` bez `nuxt-session`. Existing.

### UC-SESS-07 — Revoke lab device — P1

Dwa contexty, ten sam `storageState`. HTTP oracle w `session.spec.ts` (E2E-122, project `chromium-session`): POST revoke **200**, GET devices obu kontekstów nadal **200**, revoked id usunięty. As-built **nie** invaliduje `nuxt-session` (brak 401). Product session-kill poza zakresem.

---

## Test cases (katalog)

| ID | Pokrycie | Oracle |
|---|---|---|
| PW-W2-E2E-010 | existing-pom `session.spec.ts` | Sign out (głęboki) → `/login` + blokada admin |
| PW-W2-E2E-011 | existing-pom | HttpOnly + brak JWT w storage/`origins` |
| PW-W2-E2E-012 | existing-pom | idle → Unlock = A (płytki) |
| PW-W2-E2E-013 | existing-pom `session.spec.ts` | ścieżka B Session Lab + query `client_id`, bez `id_token_hint` |
| PW-W2-SEC-001–003 | existing-pom | HttpOnly; token w cookie nie w Web Storage; nie skanować blobu |
| PW-W2-SEC-005 | existing-pom | cookie &lt; 4 KB; brak `id_token` w sesji |
| PW-W2-SEC-006 | existing-pom | `sameSite` `"Lax"` z `context.cookies()` |
| PW-MRL-E2E-010/011 | existing-pom | policy tekst + HttpOnly |
| PW-MRL-E2E-013 | existing-pom | SameSite Lax z prawdziwego cookie |
| PW-MRL-E2E-014 | designed / docs-only | policy JSON `secure:false` **≠** TLS cookie |
| PW-MRL-E2E-026 | existing-pom | `session-lab-end-oidc` |
| PW-MRL-E2E-027 | existing-pom `session.spec.ts` | menu **Sign out** woła `/protocol/openid-connect/logout`; Continue pokazuje formularz Keycloak |
| PW-MRL-E2E-028 | existing-pom | rozmiar cookie |
| PW-MRL-API-023 | existing-pom `session-lab.spec.ts` | GET cookie-policy 200 (edukacja, nie TLS) |
| PW-MRL-API-024 | existing-pom `session.spec.ts` | POST end-session URL |
| PW-RFC-E2E-055/056 | existing-pom TLS | brak JWT w storage; Secure true |

---

## Świadome luki (nie TC bez zmiany produktu / realm)

| ID | Fakt | Dlaczego nie TC teraz |
|---|---|---|
| GAP-SESS-01 | stary volume Keycloak bez `post.logout.redirect.uris` | Additive w realm JSON; E2E-013 asertuje hop + `/login`. Volume: `python3 scripts/provision-keycloak-logout-uris.py` |
| GAP-SESS-02 | Access token nadal w cookie (fat JWT może znów zabić 4 KB) | UC-SESS-04 łapie rozmiar, nie przenosi tokena do store serwerowego |
| GAP-SESS-03 | Brak refresh token / RFC 7009 revoke | non-goal lab |
| GAP-SESS-04 | `cookie-policy` hardcode `secure: false` | produkt; TC-014 dokumentuje drift |
| GAP-SESS-05 | Keycloak nie na Postgres (`KC_DB` absent) | infra lab; INFRA-PG-01 sesja w cookie |
| GAP-SESS-06 | Public client + ROPC (`directAccessGrantsEnabled`) | testy RA/POM; nie dashboard cookie |

---

## Mapowanie starych FR

| Stare ID | Poprawne znaczenie |
|---|---|
| FR-W2-02 | tylko ścieżka A |
| FR-S04 (historyczne „BFF + OIDC”) | rozszczepione: FR-S04a = A, FR-S04b = B |
| FR-OIDC | produkt ścieżki B **DONE**; testy **existing-pom** `session.spec.ts` |
| PW-RS-08 | UI/BFF DONE; brak Playwright oracle hopu |
| Lekcja 59 „OIDC revoke” | end_session, nie token revocation |
