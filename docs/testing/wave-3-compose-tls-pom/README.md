# Wave 3 — TLS depth, Live POM Wave 2, pełny HTTPS w compose

Dokumentacja **task-force**. Wave 3 zamyka luki **designed** z Wave B (katalog 09) i Live POM Wave 2 (gość / Error Lab / IDOR) oraz dodaje topologię **Spring + Nuxt w compose** za Caddy.

To **nie** zamyka `REST-MULTIPART-01` ani `REST-OPENAPI-DRIFT-01`. QUERY / `@Retryable` / versioning / RLS na `merchants` i `payment_orders` zostają docs-only.

Kroki UI/HTTP: [live-pom-wave-2](../live-pom-wave-2/) (03/04/05/07). TLS/stos: [09-wave-b-stack-tls-catalog.md](../rls-filters-composition-lab/09-wave-b-stack-tls-catalog.md).

## Cztery entrypointy

| Tryb | Komenda | Spring / Nuxt | Caddy | POM |
|---|---|---|---|---|
| HTTP DX | `scripts/dev-stack.sh` | host, hot reload | brak | `playwright.pom.config.ts` `:3000` |
| HTTP compose | `scripts/dev-stack.sh --app` | kontenery | brak | ten sam HTTP config + `PLAYWRIGHT_SKIP_WEBSERVER=1` |
| TLS hybrid | `scripts/dev-stack.sh --tls` | host | `host.docker.internal` | `playwright.pom.tls.config.ts` |
| Full compose | `scripts/dev-stack.sh --full` | kontenery | `payment-quality-frontend` / `backend` | ten sam TLS config |

Canonical HTTP: `http://127.0.0.1:3000`. Canonical TLS: `https://app.payment-quality.local:8443` (rootless, nie :443). Keycloak HTTP issuer: `http://localhost:8081` (oracle przy starcie). HTTPS issuer: `https://auth.payment-quality.local:8443`.

`--full` OIDC: przeglądarka HTTPS (`issuer` / `authorization_endpoint` na `auth…:8443`); BFF token/JWKS/userinfo HTTP do `payment-quality-keycloak:8080` przez Nitro `/__oidc/openid-configuration` **tylko z loopback**. Caddy `GET /__oidc*` → 404. **Bez** `extra_hosts` hairpin przez Caddy. Hybrid `--tls` zostaje: host Nuxt discovery `http://localhost:8081/...`.

`--full` na pasta: oracle Location porównuje Caddy z `docker exec payment-quality-backend curl http://127.0.0.1:8080` gdy host `:8080` jest martwy. TLS POM: `PLAYWRIGHT_SKIP_WEBSERVER=1` (drukowane przez `dev-stack.sh --full`) — **nie** startować host `pnpm dev`. `ignoreHTTPSErrors` tylko przy `PLAYWRIGHT_TLS_INSECURE=1`. CA = STK-007 / `mkcert -install`.

## Use cases (Wave 3)

Format: aktor · precondition · kroki · oracle.

### UC-W3-01 — Gość na strzeżonych labach — P1

- **Aktor:** `chromium-guest`.
- **Kroki:** `/admin/users`, lista płatności Alpha, `/error-lab`, `/admin/checkout-lab`.
- **Oracle:** `/login?redirectTo=` równe ścieżce (nie hosted PSP `/psp/checkout`).
- **TC:** PW-W2-E2E-003. Kontrast: CPL hosted bez sesji nadal 200 UI.

### UC-W3-02 — Powrót po OIDC na `redirectTo` — P1

- **Aktor:** gość → platform admin (hasło tylko z env).
- **Kroki:** `/admin/users` → login Keycloak → callback.
- **Oracle:** heading `Users` (nie merchants hub).
- **TC:** PW-W2-SEC-011.

### UC-W3-03 — Gość nie woła BFF merchantów — P1

- **Kroki:** `page.request` GET i POST `/api/merchants` bez cookie.
- **Oracle:** 401. Nie destructure `api` w guest project.
- **TC:** PW-W2-SEC-030.

### UC-W3-04 — Admin Support vs manager IDOR — P1

- **Aktor:** platform admin (kontrast E2E-070 manager).
- **Kroki:** `/admin/support` → search `merchantBetaId`.
- **Oracle:** brak `error-state`; tabela `Support search results` (seed Beta; `No results` = fail).
- **TC:** PW-W2-E2E-071.

### UC-W3-05 — Error Lab żywe kody (nie 429) — P1

- **Kroki:** canary 401 = click UI + `waitForResponse` + `problem.expectVisible`; pozostałe = `page.request.fetch('/api/error-lab/trigger-{status}')`. Admin: 401/403/404/406/415. Manager: 400/409/412/428/304.
- **Oracle:** dokładny status + 4xx problem+json (304 bez body); **429 nie wołać**. Manager na 403 → 503 `lab_unavailable` (aktor ma create). 304 bez seed Alpha → 503, nie alias 304. If-Match stale = `"v99"` (`\"v{n}\"`, nie `"stale-etag"` → 400 malformed).
- **TC:** PW-W2-E2E-080…083.

### UC-W3-06 — Admin nie tworzy payment-order — P1

- **Kroki:** `BffClient` ze storageState admina, POST order Alpha.
- **Oracle:** 403. POST merchant bez `tenantReference` → 400 (GAP-W2-01). GET missing UUID → 404.
- **TC:** PW-W2-API-011 / 003 / 004.

### UC-W3-07 — Manager bez formularza notatek — P1

- **Kroki:** detail orderu Alpha.
- **Oracle:** `payment-note-body` count 0 (`canCreatePaymentNote` / `canReadPaymentNotes` tylko platform).
- **TC:** PW-W2-E2E-041.

### UC-W3-08 — Operator na HTTPS (hybrid albo `--full`) — P0

- **Kroki:** PKCE na `https://app…:8443`; RLS hub; filtr amount; authorize/capture; dismiss Cancel; probe obcego UUID.
- **Oracle:** `https:`; brak JWT w Web Storage; cookie `Secure` gdy `NUXT_SESSION_COOKIE_SECURE`; Keycloak odrzuca `redirect_uri=https://evil.example/callback` (strona błędu, nie exploit).
- **TC:** PW-RFC-E2E-050…056, SEC-RFC-001. UC-RFC-054.

### UC-W3-09 — Location za Caddy jest względny — P1

- **Kroki:** POST create order przez `https://api…:8443` (Bearer password grant).
- **Oracle:** `Location` `/api/merchants/…/payment-orders/{id}` (nie absolute URL); GET `Vary: Authorization` + `Cache-Control`.
- **TC:** RA-RFC-031 live / RA-RFC-034 · `scripts/tls-lab-location-oracle.sh`.

### UC-W3-10 — Cały stos w jednej sieci compose — P1

- **Kroki:** `scripts/dev-stack.sh --full`; `curl --cacert` na app/api/auth.
- **Oracle:** nie 502; `GET /api/status` przez Caddy 200. JWKS Spring = `http://payment-quality-keycloak:8080/…/certs`; issuer HTTPS. BFF discovery rewrite: token endpoint HTTP, nie `:8443`.
- **TC:** STK-RFC-010, STK-RFC-007.

## Indeks ID → spec

| ID | Pokrycie | Spec / skrypt | Tytuł `test()` / oracle |
|---|---|---|---|
| STK-RFC-010 | existing-setup | `dev-stack.sh --full` | trzy vhosty HTTPS, nie 502 |
| STK-RFC-007 | existing-setup | `tls-lab-cert-oracle.sh` | `curl --cacert`, bez `-k` |
| RA-RFC-031 live | existing-setup | `tls-lab-location-oracle.sh` | Location względny przez Caddy |
| RA-RFC-034 | existing-setup | ten sam skrypt | Vary / Cache-Control |
| PW-RFC-E2E-050 | existing-pom | `tls-lab.spec.ts` | `platform admin reaches RLS lab hub over the TLS origin` |
| PW-RFC-E2E-051 | existing-pom | `tls-lab.spec.ts` | `merchant manager applies a payment amount filter over the TLS origin` |
| PW-RFC-E2E-052 | existing-pom | `tls-lab.spec.ts` | `merchant manager authorizes then captures over the TLS origin` (`If-Match` `"v99"` → 412) |
| PW-RFC-E2E-053 | existing-pom | `tls-lab.spec.ts` | `merchant manager dismissing ConfirmModal does not cancel over TLS` |
| PW-RFC-E2E-054 | existing-pom | `tls-lab.spec.ts` | admin compare + manager Alpha-only / probe 404 |
| PW-RFC-E2E-055 | existing-pom | `tls-lab.spec.ts` | `expectNoTokenInBrowserStorage` |
| PW-RFC-E2E-056 | existing-pom | `tls-lab.spec.ts` | `platform admin has a Secure session cookie on the TLS origin` |
| PW-RFC-E2E-060 | existing-pom skip | `rls-lab-spring-off.spec.ts` | skip bez `PLAYWRIGHT_RLS_SPRING_OFF=1`; **nie** `--full` |
| PW-RFC-E2E-061 | designed P2 | — | FE off + Spring on |
| SEC-RFC-001 | existing-pom | `tls-lab.spec.ts` | `platform admin Keycloak rejects a redirect URI outside the realm` |
| PW-W2-E2E-003 | existing-pom | `session-guest.spec.ts` | `unauthenticated admin and lab paths land on login with redirectTo` |
| PW-W2-SEC-011 | existing-pom | `session-guest.spec.ts` | `login with redirectTo returns to the intended admin path` |
| PW-W2-SEC-030 | existing-pom | `session-guest.spec.ts` | `guest BFF merchants GET and POST return 401` |
| PW-W2-E2E-083 | existing-pom | `error-lab.spec.ts` + `error-lab-manager.spec.ts` | dokładne statusy; canary UI 401 |
| PW-W2-E2E-071 | existing-pom | `support-admin.spec.ts` | `platform admin support search on Beta returns results` |
| PW-W2-API-003 | existing-pom | `admin-bff.spec.ts` | `POST merchant without tenantReference is 400` |
| PW-W2-API-004 | existing-pom | `admin-bff.spec.ts` | `GET unknown merchant is 404` |
| PW-W2-API-011 | existing-pom | `admin-bff.spec.ts` | `platform admin POST payment-order is 403` |
| PW-W2-E2E-041 | existing-pom | `payments-lifecycle.spec.ts` | `merchant manager does not see the internal notes form` |

## Zostaje designed (nie Wave 3)

E2E-025 UI 409, E2E-031/032 palette/ARIA, MRL E2E-022 idle re-goto, SEC-031 CSRF happy, E2E-061 FE off + Spring on, GAP-W2-01 pole tenanta w UI, `trigger-429`.

## Poza Wave 3

MULTIPART, OpenAPI drift, mockowane `tests/e2e`, privileged :443, `forward-headers-strategy: framework`.
