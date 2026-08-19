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
| A — aplikacja | menu Sign out, idle Unlock | `auth.logout()` → `session.clear()` + `/login` | czyszczona | **zostaje** | existing-pom E2E-010, E2E-012, MRL E2E-021 |
| B — RP OIDC | `session-lab-end-oidc` | POST `/api/session-lab/end-session` → `clearUserSession` + URL `…/logout?client_id=&post_logout_redirect_uri=` | czyszczona | hop `end_session` (bez `id_token_hint`) | existing-pom `session.spec.ts` E2E-013 |

Idle Unlock = ścieżka A. FR „logout BFF + OIDC” dotyczy **tylko** ścieżki B.

Nazwa lekcji „OIDC revoke” w learning-map = **end_session** (RP logout), nie RFC 7009 token revocation.

---

## Use cases

### UC-SESS-01 — Operator Sign out (świat aplikacji) — P0

- **Aktor:** platform admin.
- **Precondition:** `nuxt-session` HttpOnly; SSO Keycloak może istnieć.
- **Kroki:** registry → `logout-control` → Sign out → `goto /admin/merchants`.
- **Oracle:** oba razy `/login`; brak JWT w Web Storage.
- **Nie-oracle:** request do Keycloak `end_session` (tego **nie** ma).
- **TC:** PW-W2-E2E-010, UC-W2-02.

### UC-SESS-02 — Idle Unlock (świat aplikacji) — P0

- **Aktor:** platform admin na `/admin/session-lab`.
- **Kroki:** `page.clock` + `fastForward(121_000)` → Unlock.
- **Oracle:** `/login`; sesja BFF pusta.
- **Nie-oracle:** SSO wygaszone.
- **TC:** PW-W2-E2E-012, MRL E2E-022 (ponowny `/admin/merchants` → `/login`), UC-MRL-01.

### UC-SESS-03 — End OIDC session (świat SSO) — P0

- **Aktor:** platform admin na Session Lab.
- **Kroki:** POST `/api/session-lab/end-session` (JSON przez `page.request`, hop przez klik UI). CDP nie oddaje body po `window.location` — JSON i hop są dwoma testami w `session.spec.ts`.
- **Oracle:** body `{ ended: true, endSessionUrl }`; URL (body i hop) zawiera `client_id=` i `post_logout_redirect_uri=`; **brak** `id_token_hint`; po confirm (jeśli jest) `/login`; ponowny `/admin/merchants` → `/login`; Web Storage bez JWT.
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

Dwa contexty, ten sam `storageState`. Click revoke jest w `session.spec.ts` (E2E-122, project `chromium-session`) — **nie** w `session-lab.spec.ts`. Click **nie** jest oraclem HTTP. Designed: A 401 / B nadal 200 (E1-S4, MRL E2E-030).

---

## Test cases (katalog)

| ID | Pokrycie | Oracle |
|---|---|---|
| PW-W2-E2E-010 | existing-pom `session.spec.ts` | ścieżka A |
| PW-W2-E2E-011 | existing-pom | HttpOnly + brak JWT w storage/`origins` |
| PW-W2-E2E-012 | existing-pom | idle → Unlock = A |
| PW-W2-E2E-013 | existing-pom `session.spec.ts` | ścieżka B + query `client_id`, bez `id_token_hint` |
| PW-W2-SEC-001–003 | existing-pom | HttpOnly; token w cookie nie w Web Storage; nie skanować blobu |
| PW-W2-SEC-005 | existing-pom | cookie &lt; 4 KB; brak `id_token` w sesji |
| PW-W2-SEC-006 | existing-pom | `sameSite` `"Lax"` z `context.cookies()` |
| PW-MRL-E2E-010/011 | existing-pom | policy tekst + HttpOnly |
| PW-MRL-E2E-013 | existing-pom | SameSite Lax z prawdziwego cookie |
| PW-MRL-E2E-014 | designed / docs-only | policy JSON `secure:false` **≠** TLS cookie |
| PW-MRL-E2E-026 | existing-pom | `session-lab-end-oidc` |
| PW-MRL-E2E-027 | existing-pom `session.spec.ts` | Sign out **nie** woła `/protocol/openid-connect/logout` |
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
| FR-OIDC | produkt ścieżki B **DONE**; testy **designed** |
| PW-RS-08 | UI/BFF DONE; brak Playwright oracle hopu |
| Lekcja 59 „OIDC revoke” | end_session, nie token revocation |
