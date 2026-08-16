# 09 — Wave B: stos lokalny, TLS overlay, live POM (katalog warstw)

Dokumentacja **task-force** (PM + test architect + test analyst).  
Cel: mapa przypadków **odblokowanych przez Wave B** (2026-08-13): jeden entrypoint HTTP, overlay HTTPS (Caddy/mkcert, host **8443** na rootless Podman), seed kompatybilny z `rls_lab_item`, live POM Wave A, REST-SSL-PROXY-01 część 1.

To **nie** jest nowa domena płatności. Oracles zostają: UI + BFF `:3000` / TLS origin + Spring; SQL tylko JDBC w RA.

Źródła: `scripts/dev-stack.sh`, `scripts/tls-lab-certs.sh`, `infra/compose/compose.tls.yml`, `application-tls-lab.yml`, `playwright.pom.config.ts`, `playwright.pom.tls.config.ts`, `PaymentOrderForwardedHeadersRestAssuredTest`.  
Setup: [docs/setup/local-infra.md](../../setup/local-infra.md), [docs/setup/tls-lab.md](../../setup/tls-lab.md).

Prefiksy: `STK-RFC-*` (smoke/infra), `RA-RFC-03x` (proxy/TLS HTTP), `PW-RFC-E2E-05x` (POM TLS / composition), `UC-RFC-05x` (business), `EG-RFC-05x` (error guessing).

---

## Piramida Wave B

```text
        Live POM TLS (login PKCE + filtr + RLS hub)     existing-pom
           Live POM HTTP (filtry, hard-controls, RLS)  existing-pom DONE_VERIFIED
              Playwright FE flag-off :3010             existing-pw
                 RA Location + issuer + CORS                     existing-ra
                    Smoke stack / Caddy / seed         existing-setup
                       Modulith + Flyway V17–V18       existing (Wave A)
```

Canonical HTTP: Nuxt `http://localhost:3000`, Spring `http://localhost:8080`, Keycloak `http://localhost:8081` (`reuseExistingServer: true` w POM).  
Canonical TLS lab: `https://app.payment-quality.local:8443` (`CADDY_HTTPS_PORT`, nie uprzywilejowane 443).

---

## A. Infra / smoke (`STK-RFC`)

### STK-RFC-001 — HTTP stack ready

| | |
|---|---|
| Pokrycie | existing-setup `scripts/dev-stack.sh` |
| Prio | P0 |
| Warstwa | compose + host Spring/Nuxt |
| Kroki | skrypt → wait Postgres `pg_isready` + realm + `GET /api/status` + Nuxt `:3000` |
| Asercje | status 200 JSON `UP`; Keycloak `/realms/payment-quality`; Nuxt 200/302 |
| Uczy | Jeden entrypoint zamiast czterech terminali. |

### STK-RFC-002 — seed MERCHANT_ALPHA_001

| | |
|---|---|
| Pokrycie | existing-setup (`dev,seed` w skrypcie) + existing-pom create 201 |
| Prio | P0 |
| Warstwa | Spring profile seed |
| Asercje | `POST` payment-order Alpha → **201** (nie 403); RLS rows a1/a2 po reseede |
| Uczy | Flyway nie wstawia merchantów; `rls_lab_item` FK blokował `tenants.clear()` — seed kasuje i wstawia lab rows. |

### STK-RFC-003 — stop vs down

| | |
|---|---|
| Pokrycie | existing-setup |
| Prio | P1 |
| Asercje | `--stop` gasi 8080/3000, compose zostaje; `--down` compose down (volume Postgres zostaje) |
| Uczy | `--stop` nie jest `compose down` (`DOWN=0` w bashu jest nonempty — komunikat musi rozróżniać). |

### STK-RFC-004 — TLS overlay ready

| | |
|---|---|
| Pokrycie | existing-setup `scripts/dev-stack.sh --tls` |
| Prio | P0 |
| Kroki | `tls-lab-certs.sh`; Caddy 8443→443; Spring `dev,tls-lab,seed`; Nuxt `0.0.0.0:3000` |
| Asercje | `curl -sk --resolve app…:8443:127.0.0.1` → 200/302 (nie 502) |
| Uczy | Rootless Podman nie binduje :80/:443. HTTP stack: Nuxt `127.0.0.1`. TLS: `0.0.0.0` (LAN-visible) bo Caddy pasta nie dosięga loopback. |

### STK-RFC-005 — brak sekretów w git

| | |
|---|---|
| Pokrycie | existing-setup `.gitignore` `infra/tls/*.pem` |
| Prio | P0 |
| Asercje | `cert.pem` / `key.pem` nie trackowane; hasła POM tylko z env |
| Uczy | mkcert jest labem, nie produkcyjnym CA. |

### STK-RFC-006 — Nuxt bind: HTTP loopback vs TLS `0.0.0.0`

| | |
|---|---|
| Pokrycie | existing-setup `scripts/dev-stack.sh` |
| Prio | P1 |
| Asercje | bez `--tls`: `--host 127.0.0.1`; z `--tls`: `--host 0.0.0.0` + komunikat LAN-visible |
| Uczy | Caddy pasta nie dosięga `127.0.0.1` hosta. Bind-all to kompromis labu, nie wzorzec produkcyjny. |

### STK-RFC-007 — zaufanie mkcert vs `PLAYWRIGHT_TLS_INSECURE`

| | |
|---|---|
| Pokrycie | existing-setup `playwright.pom.tls.config.ts` + [tls-lab.md](../../setup/tls-lab.md) |
| Prio | P0 |
| Kroki | `ignoreHTTPSErrors` tylko gdy `PLAYWRIGHT_TLS_INSECURE=1`; Node: `NODE_EXTRA_CA_CERTS` z `mkcert -CAROOT` |
| Asercje | bez env i bez `mkcert -install` Chromium pada na cert; z env suite jest zielony **bez** dowodu CA |
| Uczy | `NODE_EXTRA_CA_CERTS` nie zasila Chromium. Oracle certu: `curl --cacert "$(mkcert -CAROOT)/rootCA.pem"`. |

---

## B. REST Assured / HTTP kontrakt (`RA-RFC-03x`)

### RA-RFC-030 — wrogie forwarded headers nie przepisują Location

| | |
|---|---|
| Pokrycie | existing-ra `PaymentOrderForwardedHeadersRestAssuredTest` **DONE_VERIFIED** |
| Prio | P0 |
| Warstwa | Spring `forward-headers-strategy: none` |
| Kroki | POST create + `Host` / `X-Forwarded-Host` / `X-Forwarded-Proto` / `Forwarded` = evil |
| Asercje | `Location` zaczyna się od `/api/merchants/…/payment-orders/`; nie `http`; nie `https://evil.example` |
| Uczy | REST-SSL-PROXY-01 część 1. Stop: nie włączać `framework` bez trusted-proxy. |

### RA-RFC-031 — Location przy nagłówkach Caddy — **existing-ra** (Spring oracle)

| | |
|---|---|
| Pokrycie | existing-ra `createLocationStaysRelativeWhenHeadersMatchCaddyReverseProxyDefaults` |
| Prio | P1 |
| Warstwa | Spring `forward-headers-strategy: none` (bez kontenera Caddy) |
| Kroki | POST create + `X-Forwarded-For` / `X-Forwarded-Proto: https` / `X-Forwarded-Host: api.payment-quality.local:8443` (domyślne `reverse_proxy` Caddy) |
| Asercje | `Location` względny; nie `https://api.payment-quality.local:8443` |
| Uczy | Proxy nie jest drugim kontraktem REST. Porównanie live Caddy vs `:8080` zostaje poza Surefire (wymaga overlay). |

### RA-RFC-032 — issuer TLS vs HTTP — **existing-ra**

| | |
|---|---|
| Pokrycie | existing-ra `TlsLabIssuerMismatchRestAssuredTest` |
| Prio | P0 |
| Warstwa | `JwtDecoder` z `iss` = `https://auth.payment-quality.local:8443/realms/payment-quality` (klucz testowy, bez żywego Keycloak) |
| Asercje | JWT z tym `iss` → 200 na `GET /api/merchants`; `iss` `http://localhost:8081/realms/payment-quality` → 401; domyślny test issuer → 401 |
| Uczy | `issuer-uri` musi być dokładnym stringiem. JWKS w labie zostaje na `http://localhost:8081` (JVM bez mkcert CA). |

### RA-RFC-033 — CORS addytywny — **existing-ra**

| | |
|---|---|
| Pokrycie | existing-ra `TlsLabCorsRestAssuredTest` |
| Prio | P1 |
| Warstwa | `SecurityConfig` origins |
| Asercje | `Origin: http://localhost:3000`, `https://app.payment-quality.local:8443`, `https://app.payment-quality.local` → 200 + ACAO; `https://evil.example` → **403** (Spring Security Invalid CORS request, nie 200 bez ACAO) |
| Uczy | HTTPS **obok** HTTP, nie zamiast (regresja testów HTTP). |

### RA-RFC-034 — Vary / Cache-Control za proxy — **existing-setup**

| | |
|---|---|
| Pokrycie | existing-setup `scripts/tls-lab-location-oracle.sh` |
| Prio | P2 |
| Asercje | GET payment-order przez Caddy nadal `Vary: Authorization`, `Cache-Control` jak na `:8080` |

### RA-RFC-035 — live Keycloak create Alpha — **existing-pom** (oracle BFF)

| | |
|---|---|
| Pokrycie | existing-pom (nie osobny RA na żywym KC) |
| Prio | P0 |
| Asercje | `merchant.manager` + UUID Alpha → 201; Beta UUID → 403 |
| Uczy | Claim natural `MERCHANT_ALPHA_001` ≠ TestJwt UUID. RA Testcontainers tego nie ćwiczy. |

---

## C. Playwright E2E / POM

### C1. HTTP live (Wave A zamknięcie) — existing-pom DONE_VERIFIED

| ID | Przypadek | Spec | Prio |
|---|---|---|---|
| PW-RFC-E2E-002 | Merchant tylko Alpha RLS | `rls-lab.spec.ts` | P0 |
| PW-RFC-E2E-003 | Probe obcego UUID 404 `not_found` | `rls-lab.spec.ts` | P0 |
| PW-RFC-E2E-004 | Platform compare `restrictedWithoutTenantGuc=0` | `rls-lab.spec.ts` | P0 |
| PW-RFC-E2E-020 | date + status + reference vs BFF | `payments-filters.spec.ts` | P0 |
| PW-RFC-E2E-021 | min/max amount | `payments-filters.spec.ts` | P0 |
| PW-RFC-E2E-022 | CREATED × PLN vs EUR | `payments-filters.spec.ts` | P1 |
| PW-RFC-E2E-023 | Apply z `?page=1` → page 0 | `payments-filters.spec.ts` | P0 |
| PW-RFC-E2E-030 | USelect ≠ native select | `payments-hard-controls.spec.ts` | P1 |
| PW-RFC-E2E-031 | badge `data-status` na wierszu | `payments-hard-controls.spec.ts` | P1 |
| PW-RFC-E2E-032 | ConfirmModal `confirm-action-dismiss`, brak POST cancel | `payments-hard-controls.spec.ts` | P0 |

Hasła: `PLAYWRIGHT_PLATFORM_ADMIN_PASSWORD`, `PLAYWRIGHT_MERCHANT_MANAGER_PASSWORD`. Zero `fulfill`. Szczegóły kroków: [03](03-playwright-e2e-catalog.md).

### C2. TLS live — existing-pom (wąski zestaw)

### PW-RFC-E2E-050 — Platform hub RLS na HTTPS

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` **DONE_VERIFIED** |
| Prio | P0 |
| Asercje | `page.url()` protocol `https:`; `/admin/rls-lab`; hub załadowany |
| Uczy | PKCE redirect URI `https://app…:8443/auth/keycloak` musi być w realm. |

### PW-RFC-E2E-051 — Merchant filtr amount na HTTPS

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` **DONE_VERIFIED** |
| Prio | P0 |
| Asercje | create 201; Apply min/max; URL `minAmount`; reference widoczna |
| Uczy | BFF cookie na origin TLS; APIRequestContext musi rozwiązać hostname (preload DNS / `--host-resolver-rules`). |

### PW-RFC-E2E-052 — lifecycle + If-Match na HTTPS — **existing-pom**

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` (HTTP: `payments-lifecycle.spec.ts`) |
| Prio | P1 |
| Spec | `tls-lab.spec.ts` · `merchant manager authorizes then captures over the TLS origin` |
| Kroki | create 201 → drawer authorize z If-Match `"v99"` → 412, GET CREATED → reload → authorize + capture |
| Asercje | 412 nie 400; potem Authorized / Captured; `https:` |

### PW-RFC-E2E-053 — ConfirmModal dismiss na HTTPS — **existing-pom**

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` |
| Prio | P1 |
| Spec | `tls-lab.spec.ts` · `merchant manager dismissing ConfirmModal does not cancel over TLS` |
| Asercje | `confirm-action-dismiss`; heading Confirm znika; brak POST `/cancel`; GET `CREATED` |

### PW-RFC-E2E-054 — dual-role RLS na HTTPS — **existing-pom**

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` (HTTP: E2E-002/004) |
| Prio | P1 |
| Asercje | admin: `restrictedWithoutTenantGuc=0`, unprotected>0 |

### PW-RFC-E2E-055 — brak JWT w Web Storage na HTTPS — **existing-pom**

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` (`expectNoTokenInBrowserStorage`) |
| Prio | P0 |
| Asercje | `expectNoTokenInBrowserStorage` po loginie TLS |
| Uczy | Sesja sealed cookie; access token w secure partition. Rozmiar cookie / brak `id_token`: [session-bff-oidc-contract](../session-bff-oidc-contract.md) SEC-005 designed. |

### PW-RFC-E2E-056 — cookie Secure na origin TLS — **existing-pom**

| | |
|---|---|
| Pokrycie | existing-pom `tls-lab.spec.ts` (`NUXT_SESSION_COOKIE_SECURE`) |
| Prio | P2 |
| Asercje | session cookie z `https://app…:8443` ma `Secure` (porównaj HTTP lab bez Secure) |

### C3. Flag composition

### PW-RFC-E2E-006 — FE flag off — existing-pw

| | |
|---|---|
| Pokrycie | existing-pw `playwright.rls-flag-off.config.ts` (Nuxt **:3010**, nie reuse :3000) |
| Prio | P1 |
| Asercje | `nav-link-rls-lab` 0; strona bez table; BFF `/api/rls-lab/items` **404** |

### PW-RFC-E2E-060 — FE on + Spring RLS off — **existing-pom**

| | |
|---|---|
| Pokrycie | existing-pom `playwright.pom.rls-spring-off.config.ts` (`PLAYWRIGHT_RLS_SPRING_OFF=1`) |
| Prio | P1 |
| Skip | bez env — nie failuje default HTTP POM; **nie** na `--full` |
| Asercje | UI hub ładuje się; BFF/Spring item list **404** (brak controllera) |
| Uczy | Flaga UI ≠ flaga Spring. Zmierz raz na stosie, nie zgaduj statusu BFF. |

### PW-RFC-E2E-061 — FE off + Spring on — **designed**

| | |
|---|---|
| Pokrycie | designed |
| Prio | P2 |
| Asercje | UI 404 copy; `GET http://localhost:8080/api/rls-lab/items` z JWT **200** |
| Uczy | Wyłączenie dashboardu nie wyłącza Postgres RLS ani API. |

---

## D. Business / use case (`UC-RFC-05x`)

Język domeny. Warstwa wykonania: live POM HTTP (existing) lub TLS (existing-pom Wave 3; E2E-061 zostaje designed P2).

### UC-RFC-050 — Alpha widzi tylko swoje dane

Merchant manager: lista płatności i RLS „Alpha secret”; obcy UUID → 404 nie 403.  
Pokrycie: existing-pom E2E-002/003.

### UC-RFC-051 — Filtr jest kontraktem URL i API

Date/status/reference/amount w URL = `BffClient.listPaymentOrders`. Kolor badge nie jest oracle (`data-status` + label).  
Pokrycie: existing-pom E2E-020/021/022/031.

### UC-RFC-052 — Stale page po filtrze

Apply z `?page=1` resetuje listę (page 0 / brak page). Pusta strona to defekt check-then-act.  
Pokrycie: existing-pom E2E-023.

### UC-RFC-053 — Anulowanie wymaga potwierdzenia

Dismiss ≠ cancel. Locator: `confirm-action-dismiss` (copy „Go back”). Brak POST `/cancel`; status `CREATED`.  
Pokrycie: existing-pom E2E-032 HTTP; E2E-053 HTTPS.

### UC-RFC-054 — Operator pracuje na HTTPS origin

Po PKCE użytkownik jest na `https://app…:8443`; token nie w `localStorage`; cookie `Secure` (E2E-056). Hybrid `--tls` albo `--full`.  
Pokrycie: existing-pom E2E-050–056. Mapka UC: [wave-3](../wave-3-compose-tls-pom/) UC-W3-08.

### UC-RFC-055 — Natural merchant claim

`merchant.manager` tworzy order na Alpha (201), nie na Beta (403).  
Pokrycie: existing-pom (create w filtrach) + designed RA-035 jako osobny RA na żywym KC.

### UC-RFC-056 — Platforma widzi lekcję wycieku, merchant nie

Merchant: brak panelu; BFF compare 403 (RA-014, nie `rls_forbidden`).  
Admin: `restrictedWithoutTenantGuc=0`, `unprotected` > 0 (E2E-004, RA-019).  
Spoof `platform:payments:read` + `TENANT_ALPHA`: 403 `rls_forbidden` (RA-018) — liczby wycieku nie wychodzą.  
Pokrycie: existing-pom E2E-004/005 + existing-ra RA-014/018/019.

---

## E. ISTQB — DT / ST / EG (Wave B)

### DT-RFC-02 — Origin × flaga × Location

| Origin | Flaga FE | Flaga Spring | Forwarded hostile | Oczekiwanie |
|---|---|---|---|---|
| HTTP :3000/:8080 | on | on (dev) | tak | Location względny; POM HTTP green |
| HTTPS :8443 | on | on (tls-lab) | Caddy X-Forwarded | Location względny; PKCE OK |
| HTTP | off | on | — | UI 404; Spring items 200 z JWT |
| HTTP | on | off | — | UI hub; API 404 |
| HTTP | on | on | — | `iss` localhost JWT działa; tls-lab issuer odrzuca |

Pokrycie: existing-ra RA-030/031/032/033 (wiersz 1 issuer); POM TLS (wiersz 2); E2E-060 existing-pom; E2E-061 designed P2.

### ST-RFC-050 — PKCE callback

`/login` → `/auth/keycloak` → Keycloak → `/auth/keycloak?code=` → `/admin/merchants`.  
Middleware omija **tylko** `/auth/keycloak` (nie cały `/auth/`).  
Błąd DNS/CA na token endpoint → pętla `/login?redirectTo=/auth/keycloak?code=`.  
Pokrycie: existing-setup (preload DNS + `NODE_EXTRA_CA_CERTS`) + existing-pom TLS setup + `auth.global.ts`.

### EG-RFC-050 — Privileged 443

Założenie planu „Caddy :443” pada na rootless (`ip_unprivileged_port_start`). Lab = **8443**. Nie failować TC na bind 443.

### EG-RFC-051 — localhost vs 127.0.0.1 cookies

BFF `baseURL` 127.0.0.1 + login na `localhost` → 401. POM używa `http://localhost:3000`.

### EG-RFC-052 — Vue Boolean prop ukrywa Cancel

`defineProps({ canRunLifecycle: Boolean })` bez przekazania z parenta rzutuje na `false`. Capability tylko z `useAuthorization()`. Unittest mockuje composable, nie prop.

Pokrycie: existing (brak Boolean prop); POM E2E-032; unit `state-and-lifecycle.test.ts`.

### EG-RFC-053 — vue-tsc overlay kradnie click

`$fetch` typed routes → overlay intercepts pointer. RLS page używa `ofetch`. POM **failuje**, gdy overlay jest w DOM (`assertNoDevErrorOverlay`) — nie klika go.

### EG-RFC-055 — `PLAYWRIGHT_TLS_INSECURE=1` nie jest oracle CA

Zielone 4/4 TLS POM z `ignoreHTTPSErrors` nie znaczy „przeglądarka ufa mkcert”. Domyślnie insecure jest **off**. Hatch tylko gdy brak `mkcert -install`.

### EG-RFC-054 — QUERY / versioning / Retryable

Świadomie **docs-only**. Nie projektować TC na nieistniejący `RequestMethod.QUERY`. Anty: CPL GET+body 403 (`EG-RFC-043` w [07](07-istqb-decision-state-usecase.md)).

---

## F. Bezpieczeństwo (lab, nie playbook ataku)

| ID | Przypadek | Pokrycie | Prio |
|---|---|---|---|
| SEC-RFC-001 | Redirect URI spoza realm odrzucony przez Keycloak | existing-pom `tls-lab.spec.ts` | P1 |
| SEC-RFC-002 | HTTP localhost redirect nadal w realm (regresja) | existing-setup + existing-pom HTTP | P0 |
| SEC-RFC-003 | Nie ufać `X-Forwarded-*` | existing-ra RA-030/031 | P0 |
| SEC-RFC-004 | Nie testować „RLS na payment_orders” — nie ma go | docs-only / zakaz | — |
| SEC-RFC-005 | Guard sesji: tylko `/auth/keycloak`, nie `/auth/*` | existing `auth.global.ts` (`isOidcHandlerPath`) | P0 |

---

## G. Poza Wave B (nie dodawać TC jako „brak implementacji”)

- `REST-MULTIPART-01` — Wave 4 (`apps/api-tests` evidence). See [wave-4-rest-contract-gates](../wave-4-rest-contract-gates/README.md).
- `REST-OPENAPI-DRIFT-01` — Wave 4 decision recorded; springdoc/CI = Wave 5. See [openapi-ownership.md](../../architecture/openapi-ownership.md).
- RLS na `merchants` / `payment_orders`.
- Kafka, PSP. Konteneryzacja Spring/Nuxt = Wave 3 `--full` (nie zastępuje HTTP DX).
- Uprzywilejowane :443 bez sysctl.

---

## H. Rekomendowana kolejność pozostałych **designed**

Wave 3 zamknęło RA-RFC-031 live, E2E-052–056, E2E-060, STK-007, SEC-RFC-001. Zostaje:

1. PW-RFC-E2E-061 FE off + Spring on (P2).
2. `REST-MULTIPART-01` / OpenAPI **decision** — Wave 4. OpenAPI **tooling** — Wave 5.

RA-RFC-032 (issuer) i RA-RFC-033 (CORS) są **existing-ra**.

Nie blokować HTTP POM na TLS: HTTP stack jest obowiązkowy, HTTPS jest labem lokalnym.

---

## I. Wave 3 (TLS depth + `--full`)

Mapa: [wave-3-compose-tls-pom](../wave-3-compose-tls-pom/README.md). Hybrid `--tls` zostaje. `--full` = Caddy → serwisy compose; OIDC split front HTTPS / back HTTP (nie hairpin `extra_hosts`).

| ID | Pokrycie Wave 3 |
|---|---|
| RA-RFC-031 live | `scripts/tls-lab-location-oracle.sh` (Caddy vs host `:8080` albo `docker exec`) |
| RA-RFC-034 | ten sam skrypt (Vary / Cache-Control) |
| PW-RFC-E2E-052…055 | `tls-lab.spec.ts` |
| PW-RFC-E2E-056 | `nuxt-session` `Secure` gdy `NUXT_SESSION_COOKIE_SECURE=true` |
| PW-RFC-E2E-060 | `playwright.pom.rls-spring-off.config.ts` |
| STK-RFC-007 | `scripts/tls-lab-cert-oracle.sh` |
| SEC-RFC-001 | `tls-lab.spec.ts` (Keycloak `invalid redirect_uri`) |
| STK-RFC-010 | `scripts/dev-stack.sh --full` |

Kafka / MULTIPART / OpenAPI / RLS na tabelach produkcyjnych — nadal poza.
