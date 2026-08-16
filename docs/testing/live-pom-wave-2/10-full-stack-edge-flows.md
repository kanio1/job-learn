# 10 — Druga iteracja: pełny stos i reverse proxy (BA + TA)

Iteracja 1 ([09](09-core-domain-flows.md)) opisuje **domenę** na hoście `:8080` / `:3000`.  
Ta iteracja dokłada **brzeg**: Caddy, trzy vhosty, TLS `:8443`, OIDC split, nagłówki `X-Forwarded-*`, CORS, cookie `Secure`, replay tych samych UC tenant×merchant×user **przez proxy**.

Techniki: ISTQB FL (DT warstw, ST sesji TLS, EP originów, error guessing na sticky issuer). **As-built.**

Sąsiedzi (nie kopiować 1:1): [wave-3 README](../wave-3-compose-tls-pom/README.md), [RFC 09 stack/TLS](../rls-filters-composition-lab/09-wave-b-stack-tls-catalog.md), [tls-lab.md](../../setup/tls-lab.md), [run-stack-and-pom.md](../../setup/run-stack-and-pom.md).  
CPL przez HTTPS: [CPL 09](../checkout-protocol-lab/09-protocol-flow-simulations.md) § BC-CPL-EDGE (wskaźnik niżej).

Nie ma czwartego vhosta `psp.` — hosted checkout idzie **tym samym** `app.` (`/psp/checkout/{id}`).

---

## Topologia (cztery tryby, nie mieszaj)

| Tryb | Caddy | Spring / Nuxt | Przeglądarka | Issuer JWT `iss` |
|---|---|---|---|---|
| `dev-stack.sh` | brak | host | `http://localhost:3000` | `http://localhost:8081/realms/payment-quality` |
| `--app` | **brak** | kontenery | `http://127.0.0.1:3000` | ten sam HTTP |
| `--tls` | `Caddyfile` → `host.docker.internal` | host | `https://app.payment-quality.local:8443` | `https://auth.payment-quality.local:8443/realms/payment-quality` |
| `--full` | `Caddyfile.full` → kontenery | kontenery | ten sam HTTPS | ten sam HTTPS; BFF token/JWKS **HTTP** do `payment-quality-keycloak:8080` |

`/etc/hosts`: `127.0.0.1 app. api. auth.payment-quality.local`. Port **8443** (rootless nie binduje 443).

```text
Przeglądarka
  │ HTTPS :8443
  ▼
Caddy (trzy serwery TLS, ten sam cert mkcert)
  ├─ app.  → Nuxt :3000     (+ --full: GET /__oidc* = 404)
  ├─ api.  → Spring :8080   (forward-headers-strategy: none)
  └─ auth. → Keycloak :8080 (X-Frame-Options SAMEORIGIN)
```

Dashboard w UI **nie** woła `api.` z przeglądarki: same-origin `app.` + BFF dokłada Bearer. Bruno/kontrakt **może** bić `api.` wprost.

---

## Słownik brzegu

| Symbol | Znaczenie |
|---|---|
| `APP` | `https://app.payment-quality.local:8443` |
| `API` | `https://api.payment-quality.local:8443` |
| `AUTH` | `https://auth.payment-quality.local:8443` |
| `ALPHA_001` | jak w [09](09-core-domain-flows.md) `…b1` |
| Edge headers (Caddy `security.caddy`) | `X-Content-Type-Options: nosniff`; `Referrer-Policy: strict-origin-when-cross-origin`; `Permissions-Policy` camera/mic/geo empty; **brak** `Server`; HSTS `max-age=300`; body **5MB** |
| Frame | `app.` i `api.`: `X-Frame-Options: DENY`; `auth.`: `SAMEORIGIN` |
| Spring Location | zawsze **względny** `/api/...` — nie ufa `X-Forwarded-*` |

JWT na `--full` / `--tls`:

```http
POST https://auth.payment-quality.local:8443/realms/payment-quality/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=payment-quality-dashboard&username={persona}&password={persona}
```

`iss` w tokenie **musi** być URI, którego używa Spring (`tls-lab`). Token z `http://localhost:8081` na `API` HTTPS → **401** (mieszanie trybów).

---

## BC-EDGE-00 — Smoke trzech vhostów

**Cel:** operator wie, że brzeg żyje, zanim testuje domenę.

### MS — status przez Caddy

```http
GET https://api.payment-quality.local:8443/api/status
```

→ **200** JSON (jak `:8080`).  
UI: `GET https://app…:8443/` → 200/302, nie 502.  
Auth discovery:

```http
GET https://auth.payment-quality.local:8443/realms/payment-quality/.well-known/openid-configuration
```

→ `issuer` = `https://auth.payment-quality.local:8443/realms/payment-quality`.

Oracle setup: `scripts/tls-lab-cert-oracle.sh` (`curl --cacert`, bez `-k` po `mkcert -install`).  
TC: STK-RFC-010, UC-W3-10.

### EXC-EDGE-00a — `--app` i HTTPS

`curl https://app.payment-quality.local:8443` → connection refused. **Nie** defect Caddy — Caddy nie startuje. UC operatora: [run-stack](../../setup/run-stack-and-pom.md).

### EXC-EDGE-00b — zły vhost

| Request | Oczekiwanie |
|---|---|
| `GET https://auth…:8443/api/status` | nie Spring (Keycloak HTML/404) |
| `GET https://api…:8443/admin/merchants` | Spring 401/404 — **nie** Nuxt login |
| `GET https://app…:8443/api/status` | BFF/Nuxt proxy do Spring → **200** (same-origin dashboard) |

### EXC-EDGE-00c — `--full` `/__oidc*` z przeglądarki

```http
GET https://app.payment-quality.local:8443/__oidc/openid-configuration
```

→ **404** (Caddy `handle`). Rewrite Nitro jest **loopback-only** (`Host` localhost). Zapobiega wyciekowi `payment-quality-keycloak` DNS. TC: UC-W3-10.

---

## BC-EDGE-01 — Nagłówki proxy vs kontrakt Location

**Cel:** wrogi lub „caddy-shaped” `X-Forwarded-*` nie przepisuje `Location` na evil host.

Spring: `server.forward-headers-strategy: none`.

### MS — Caddy-shaped (RA-RFC-031)

```http
POST https://api.payment-quality.local:8443/api/merchants/{ALPHA_001}/payment-orders
Authorization: Bearer {merchant.manager tls-iss}
Content-Type: application/json
Idempotency-Key: edge-loc-001
Host: api.payment-quality.local:8443
```

Caddy i tak doda `X-Forwarded-For`, `X-Forwarded-Proto: https`, `X-Forwarded-Host`.  
→ **201**, `Location: /api/merchants/{ALPHA_001}/payment-orders/{uuid}` (**względny**, nie `https://api…`).  
`Vary: Authorization`, `Cache-Control` sensitive.  
TC: `PaymentOrderForwardedHeadersRestAssuredTest`, `tls-lab-location-oracle.sh`, UC-W3-09.

### EXC — hostile (RA-RFC-030)

Ten sam POST na Spring **bez** zaufania proxy (test random port):

```http
Host: evil.example
X-Forwarded-Host: evil.example
X-Forwarded-Proto: https
X-Forwarded-Port: 443
Forwarded: for=1.2.3.4;host=evil.example;proto=https
```

→ **201**, `Location` starts with `/api/merchants/`, **nie** `https://evil.example`.

**Nie** włączaj `forward-headers-strategy: framework` w tej fali (Wave 3 non-goal).

---

## BC-EDGE-02 — CORS (origin EP)

Allowed (dev/test bean): `http://localhost:3000`, `https://app.payment-quality.local`, `https://app.payment-quality.local:8443`.

Preflight:

```http
OPTIONS /api/merchants/{ALPHA_001}/payment-orders
Origin: https://app.payment-quality.local:8443
Access-Control-Request-Method: POST
Access-Control-Request-Headers: authorization,content-type,idempotency-key,if-match,x-correlation-id
```

→ **nie 401**; `Access-Control-Allow-Origin` = ten Origin; Allow-Headers zawiera `Idempotency-Key`, `If-Match`.  
`Origin: https://evil.example` → **403** Invalid CORS (nie 200 bez ACAO).  
TC: `TlsLabCorsRestAssuredTest` RA-RFC-033.

Dashboard same-origin (`app.` → BFF → Spring) **nie** potrzebuje CORS. CORS boli, gdy SPA na `app.` bije **wprost** `api.` (Bruno w przeglądarce, nie Node).

---

## BC-EDGE-03 — Security headers i ramki

### MS — app / api

```http
GET https://app.payment-quality.local:8443/login
```

Oracle (Caddy): `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, HSTS `max-age=300`, brak `Server`.  
To samo na `GET https://api…/api/status`.

### EXC — widget / iframe (error guessing)

Hosted `/psp/checkout/{id}` też jest na **`app.`** → **DENY**. Osadzenie checkoutu w iframe merchanta **nie** zadziała. Lab uczy hosted **full page** / nowa karta (`waitForEvent('page')`). Nie projektuj `psp.` vhost w tej iteracji.

### ALT — Keycloak login iframe

`auth.` ma `X-Frame-Options: SAMEORIGIN` — login KC może być w ramce **tego samego** originu auth, nie w `app.`. PKCE dashboard = **top-level redirect** na `AUTH`, nie iframe w Nuxt.

---

## BC-EDGE-04 — Limit body 5MB (Caddy + Spring)

Oba: Caddy `request_body max_size 5MB` i Spring `multipart max-file-size/max-request-size 5MB`.

### EXC — za duży evidence

```http
POST https://api…/api/merchants/{ALPHA_001}/payment-orders/{id}/evidence
Authorization: Bearer {manager}
Content-Type: multipart/form-data
```

Plik > 5MB → **413** na Caddy **albo** 400 Spring (zależnie kto odetnie pierwszy). Wave 4 multipart oracles na `:8080` (bez Caddy) zostają kanonem rozmiaru części. Przez `API` dodaj asercję: Caddy 413 ≠ problem+json Spring.

---

## BC-EDGE-05 — Sesja TLS (cookie, PKCE, issuer)

Powtórka UC-W2-01/02 **na APP**, nie na `:3000`.

### MS — login PKCE

1. Gość `https://app…:8443/admin/merchants` → `/login?redirectTo=`.
2. OIDC authorize na `AUTH` (`redirect_uri=https://app…:8443/auth/keycloak`).
3. Po callback: registry; cookie `nuxt-session` z flagą **`Secure`** gdy `NUXT_SESSION_COOKIE_SECURE=true`.
4. Brak `eyJ` w Web Storage.

TC: PW-RFC-E2E-050, 055, 056, UC-W3-08, SEC-RFC-001 (`redirect_uri=https://evil.example/callback` → błąd Keycloak).

### EXC-EDGE-05a — sticky issuer

Token ROPC z HTTP Keycloak (`iss=http://localhost:8081/...`) na:

```http
GET https://api.payment-quality.local:8443/api/merchants
Authorization: Bearer {http-iss token}
```

→ **401**. Oracle startu: `keycloak-issuer-oracle.sh`. GAP: nie zostawiać kontenera `--full` przy `--app`.

### EXC-EDGE-05b — BFF IPv4 vs OIDC hostname

HTTP POM: Node `BffClient` = `127.0.0.1:3000`; browser = `localhost` (OIDC). TLS POM: `MAP *.payment-quality.local 127.0.0.1` w Chromium. Pomyłka `::1` → ECONNREFUSED (EG-W2-02).

---

## BC-EDGE-06 — Replay domeny przez `api.` (tenant × merchant × user)

Te same oracles co [09 BC-OP-02…07](09-core-domain-flows.md), zmiana **tylko** hosta i `iss`.

Base:

```http
Host: api.payment-quality.local:8443
Authorization: Bearer {token with tls iss}
```

| # | Persona | Request | Oracle (bez zmiany vs :8080) |
|---|---|---|---|
| 1 | gość | `GET {API}/api/merchants` | **401** problem+json |
| 2 | `platform.admin` | `POST {API}/api/merchants` body `merchantReference`+`displayName` **bez** `tenantReference` | **400** |
| 3 | `platform.admin` | ten sam + `"tenantReference":"TENANT_ALPHA"` | **201**, `Location` względny |
| 4 | `tenant.admin` | `GET {API}/api/merchants/{BETA_001}` | **404** |
| 5 | `merchant.manager` | `POST {API}/api/merchants/{ALPHA_001}/payment-orders` + Idempotency-Key + `{amountMinor,currency,clientOrderReference}` | **201** + względny Location |
| 6 | `merchant.manager` | create na `{ALPHA_002}` | **403** |
| 7 | `platform.admin` | create order Alpha | **403** |
| 8 | `merchant.manager` | authorize `If-Match: "v0"` | **200** AUTHORIZED |
| 9 | `merchant.manager` | `If-Match: "v99"` | **412**, CREATED |
| 10 | `merchant.manager` | `POST …/refund` po capture | **409** `dual_control_required` |
| 11 | `platform.admin` | `POST …/refund` | **200** override |
| 12 | `merchant.denied` | `GET {API}/api/merchants` | **403** |

UI analog (same-origin `APP`, nie `API`): E2E-001, 020, 090–094, 070; TLS: PW-RFC-E2E-051…054 (filtr, authorize/capture, dismiss cancel, probe 404).

**DT warstw:** ten sam status kodu na `:8080` i na `API`. Różnica = TLS + HSTS + Forwarded **ignorowane**. Jeśli kody się rozjadą → defect brzegu, nie domeny.

---

## BC-EDGE-07 — CPL przez `app.` / `api.` (nie mylić z payment_orders)

Na `--full` Spring ma `hosted-checkout-base-url=https://app.payment-quality.local:8443`.

### MS — create session (lab Bearer, nie KC JWT)

```http
POST https://api.payment-quality.local:8443/api/checkout-lab/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id={lab}&client_secret={lab}
```

```http
POST https://api.payment-quality.local:8443/api/checkout-lab/sessions
Authorization: Bearer lab.*
Content-Type: application/json
Idempotency-Key: edge-cpl-001

{
  "extOrderId": "ORD-EDGE-001",
  "amountMinor": 1999,
  "currency": "PLN",
  "continueUrl": "https://app.payment-quality.local:8443/checkout-lab/return",
  "notifyUrl": "https://api.payment-quality.local:8443/api/checkout-lab/notify",
  "validitySeconds": 900
}
```

→ **302**, `Location` / `redirectUri` zaczyna się od `https://app.payment-quality.local:8443/psp/checkout/{uuid}` (nie `http://localhost:3000`).  
Hosted GET publiczny (płatnik, bez lab Bearer):

```http
GET https://app.payment-quality.local:8443/psp/checkout/{sessionId}
```

albo API `GET {API}/api/checkout-lab/hosted/sessions/{id}` → `simulateToken`.  
Notify HMAC na `{API}/api/checkout-lab/notify` (CORS Allow-Headers zawiera `Lab-Signature`).

### EXC

| Zmiana | Wynik |
|---|---|
| KC JWT zamiast `lab.*` na sessions | **401** empty (PW-API-022) |
| `continueUrl` HTTP przy TLS origin | dozwolone przez walidację URL, ale mixed content w UI — używaj HTTPS `APP` |
| Lie return | query `status=success` na `https://app…/checkout-lab/return` — fulfillment nadal AWAITING ([CPL UC-03](../checkout-protocol-lab/07-istqb-decision-state-usecase.md)) |
| `X-Frame-Options DENY` | iframe widget **blocked** (BC-EDGE-03) |

Pokrycie TLS CPL E2E: **cienkie** (Wave 3 TLS spec = RLS/payments, nie pełny hosted). RA create session na test profile zostaje `localhost:3000`. Live `--full` Location = `APP` — oracle setup, nie nowy P0 POM w tej iteracji docs.

---

## BC-EDGE-08 — Bruno / dwa hosty API

| Tryb | Token URL | API base |
|---|---|---|
| `--app` | `http://localhost:8081/realms/…/token` | `http://127.0.0.1:8080` |
| `--full` | `https://auth…:8443/realms/…/token` | `https://api…:8443` **lub** BFF `https://app…:8443` z cookie |

Nie mieszaj wierszy. Cookie `nuxt-session` na `APP` **nie** autoryzuje surowego `{API}` (inny host; Spring chce Bearer).

---

## Macierz kombinacji (covering, nie 4×6×12)

| Tryb | Vhost | Persona | Flow | Pokrycie |
|---|---|---|---|---|
| `--app` | `:3000` / `:8080` | manager | create+lifecycle | existing-pom 09 |
| `--app` | `:8080` | tenant.admin | GET Beta 404 | existing-ra |
| `--full` | `APP` | admin | PKCE + Secure cookie | existing-pom TLS |
| `--full` | `API` | manager | Location względny | existing-setup oracle |
| `--full` | `AUTH` | admin | reject evil redirect_uri | existing-pom SEC-RFC-001 |
| `--full` | `APP` `/__oidc*` | ktokolwiek | 404 | existing-setup |
| `--tls`/`--full` | `API` | hostile X-Forwarded | Location względny | existing-ra |
| dowolny | `app.` iframe | płatnik | DENY | docs / EG |
| mix `--app` token + `API` | — | dowolny | **401** iss | EG + issuer oracle |

Pairwise currency×amount **nie** mnożyć przez tryb stosu — jeden happy TLS (PW-RFC-E2E-052) wystarcza jako smoke brzegu.

---

## Mapowanie UC / TC

| BC | UC | Główne TC |
|---|---|---|
| EDGE-00 | UC-W3-10, **UC-W2-23** | STK-RFC-010/007 |
| EDGE-01 | UC-W3-09 | RA-RFC-030/031, location oracle |
| EDGE-02 | — | RA-RFC-033 |
| EDGE-03 | EG frame | Caddyfile + snippet |
| EDGE-04 | Wave 4 + Caddy 413 | AT-MP + docs |
| EDGE-05 | UC-W3-08, UC-W2-01 TLS | E2E-050…056 |
| EDGE-06 | UC-W2-08…22 przez `API` | RA + TLS 051–054 |
| EDGE-07 | CPL UC-01 na HTTPS | RA profile `localhost`; live Location `APP` |
| EDGE-08 | Bruno | [bruno-postman-api.md](../../setup/bruno-postman-api.md) |

Indeks Wave 2: [07](07-istqb-decision-state-usecase.md) UC-W2-23. Domena bez proxy: [09](09-core-domain-flows.md).
