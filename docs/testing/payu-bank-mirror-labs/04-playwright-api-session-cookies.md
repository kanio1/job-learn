# 04 — REST: Spring, Nitro BFF, cookies, CSRF, PayU HTTP, bank

Klient: Playwright `APIRequestContext` i/lub REST Assured. Browser nieobowiązkowy.

**Target A:** `http://127.0.0.1:8080` — Spring + Postgres (`needs-backend`).  
**Target B:** `http://127.0.0.1:3000` — Nitro BFF (sesja `nuxt-session` albo 401).

O ile nie napisano: Target A. Problem+json: pola `type`, `title`, `status`, `detail`, `error`, `correlationId`.  
401 lab Bearer (CPL): puste body. BFF flag off: Nitro **404**.

---

## 4.1 Flag / health / modulith

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-001 | GET `/api/mirror-lab/statements?format=csv` + JWT, `app.mirror-lab.enabled=false` | **404** + `X-Correlation-ID` | existing-it DisabledIT | C-03 | P0 |
| PW-MRL-API-002 | bean `mirrorLabController` przy flag off | `NoSuchBeanDefinitionException` | existing-it | C-03 | P0 |
| PW-MRL-API-003 | GET statements flag on + JWT | **200** CSV | existing-it EnabledIT | — | P0 |
| PW-MRL-API-004 | Target B `GET /api/mirror-lab/statements` gdy FE flag **false** | Nitro **404** (bez Spring) | existing-pom `mirror-lab-flag-off.spec.ts` | C-03 | P0 |
| PW-MRL-API-005 | Target B `GET /api/session-lab/csrf` flag false | 404 | existing-pom `mirror-lab-flag-off.spec.ts` | C-03 | P0 |
| PW-MRL-API-006 | Target B `GET /api/network-lab/slow` flag false | 404 | designed | C-03 | P0 |
| PW-MRL-API-007 | Target B `GET /api/mirror-lab/tpp/accounts` flag false | 404 (nawet z tokenem) | designed | C-03 | P0 |
| PW-MRL-API-008 | GET `/api/status` | 200 existing platform | existing-ra analog | — | — |
| PW-MRL-API-009 | Modulith: `mirrorlab` nie importuje `*.internal` innych modułów | green | existing (`ModulithArchitectureTest`) | — | P0 |

---

## 4.2 Session BFF — CSRF, devices, cookie

Wszystkie session-lab: `requireMirrorLabSession` = flaga **i** `requireUserSession`.

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-010 | GET `/api/session-lab/csrf` z sesją | 200 `{ token }`; `Set-Cookie: mrl-csrf` **nie** HttpOnly | designed | S06 | P1 |
| PW-MRL-API-011 | POST `/api/session-lab/csrf-demo` sesja, **bez** `X-Csrf-Token` | 403 `csrf_failed` problem+json | existing-pom (UI) / designed request | S06 | P1 |
| PW-MRL-API-012 | POST csrf-demo zły header | 403 `csrf_failed` | existing-pom `session-lab.spec.ts` | S06 | P1 |
| PW-MRL-API-013 | POST csrf-demo header == cookie | 200 `{ status: ok }` | existing-pom `session-lab.spec.ts` | S06 | P1 |
| PW-MRL-API-014 | POST merchant payment-order Bearer, **bez** CSRF | istniejący 201/200 — CSRF **nie** na merchant BFF | designed kontrast | S06 | P1 |
| PW-MRL-API-015 | GET csrf **bez** sesji | 401 | existing-pom `session-guest.spec.ts` (POST csrf-demo 401; ten sam `requireUserSession`) | S04 | P0 |
| PW-MRL-API-020 | GET `/api/session-lab/devices` z sesją | 200 lista | existing-pom `session.spec.ts` | S05 | P1 |
| PW-MRL-API-021 | POST devices | 200 rekord | existing-pom (UI `loadDevices` w session-lab) | S05 | P1 |
| PW-MRL-API-022 | POST `devices/{id}/revoke` | 200 `{ revoked: true }`; GET lista bez id | existing-pom `session.spec.ts` | S05 | P1 |
| PW-MRL-API-023 | GET `/api/session-lab/cookie-policy` | 200 opis HttpOnly `nuxt-session` (**statyczny** `secure: false`) | designed | S01 | P1 |
| PW-MRL-API-024 | POST `/api/session-lab/end-session` z sesją | 200 `{ ended, endSessionUrl }`; URL `client_id` + `post_logout_redirect_uri`; **bez** `id_token_hint` | existing-pom `session.spec.ts` | S04b | P0 |
| PW-MRL-API-025 | POST end-session bez sesji | 401 | designed | S04b | P0 |

## Cookies (request)

| ID | Call | Expect | Pokrycie | FR |
|---|---|---|---|---|
| PW-MRL-API-030 | BFF GET bank/statements z Cookie sesji | 200 (flag on, backend up) | designed | S01 |
| PW-MRL-API-031 | BFF GET statements bez Cookie | 401 | designed | S04 |
| PW-MRL-API-032 | Hosted GET `/psp/checkout/{id}` bez Cookie | 200 HTML public | existing-pom | S02 |
| PW-MRL-API-033 | GET `/api/checkout-lab/hosted/sessions/{id}` bez Keycloak | 200 JSON (CPL) | existing-ra | S02 |

---

## 4.3 PayU HTTP mirrors (Spring checkoutlab)

GET-with-body: filtr na **GET** z `Content-Length>0` lub `Transfer-Encoding`. RA Apache nie wyśle body — użyj `java.net.http.HttpClient`.

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-100 | GET `/api/checkout-lab/sessions/{id}` + JSON body `{}` + lab Bearer | **403** `get_with_body` problem+json | existing-ra | P01 | P1 |
| PW-MRL-API-101 | GET session **bez** body | 200 snapshot | existing-ra | P01 | P1 |
| PW-MRL-API-102 | POST sessions (ma body) | **nie** 403 get_with_body | designed (regresja filtra) | P01 | P0 |
| PW-MRL-API-103 | Location / `sessionId` z `?lang=` — parser **obcina** query | UUID poprawny | existing-ra `sessionIdFromLocation` | P02 | P0 |
| PW-MRL-API-110 | POST oauth JSON Content-Type | **401** nie 415 | existing-ra analog CPL | P06 | P1 |
| PW-MRL-API-111 | POST oauth form `trusted_merchant` + email + extCustomerId | 200 lab Bearer | existing-ra | P06 | P1 |
| PW-MRL-API-112 | `trusted_merchant` bez extCustomerId | 401 | existing-ra | P06 | P1 |
| PW-MRL-API-120 | POST `/sessions/{id}/refund` po COMPLETED | 200 `status=REFUNDED`; event `checkout.session.refunded` | existing-ra | P03 | P0 |
| PW-MRL-API-121 | drugi POST refund | **409** `refund_not_allowed`; COUNT event **= 1** | existing-ra | P03 | P0 |
| PW-MRL-API-122 | refund gdy CREATED/PENDING/CANCELED | 409 `refund_not_allowed` | designed | P03 | P1 |
| PW-MRL-API-123 | notify refund HMAC invalid | 400 `invalid_signature`; brak side-effect | designed (CPL analog HMAC) | P03 | P1 |
| PW-MRL-API-124 | duplicate notify `eventId` | 200 `{duplicate:true}` | designed CPL analog | P03 | P2 |
| PW-MRL-API-130 | simulate po `validityUntil` | 409 `expired_link` | existing-ra CPL | P04 | P1 |
| PW-MRL-API-131 | POST sessions `language=pl` | Location zawiera `?lang=pl` | existing-ra | P02 | P1 |
| PW-MRL-API-132 | `language=xx` | Location **bez** query lang | designed | P02 | P2 |
| PW-MRL-API-140 | OPTIONS `/api/network-lab/cors-cookie` Origin `http://localhost:3000` | 204; `Allow-Credentials: true`; ACAO origin | existing-pom | N06 | P1 |
| PW-MRL-API-141 | GET cors-cookie inny Origin | 200 JSON; ACAO nadal lab origin | existing-pom | N06 | P1 |

---

## 4.4 Network Nitro

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-150 | POST `/api/network-lab/trigger-503-retry` 1st | 503 + `Retry-After` = TTL s | existing-pom | N01 | P0 |
| PW-MRL-API-151 | 2nd same session key | 200 `{ status: ok }` | existing-pom | N01 | P0 |
| PW-MRL-API-152 | po TTL znowu 1st=503 | GET `/api/network-lab/retry-window` `remainingMs` → 0, potem POST 503 | existing-pom | N01 | P1 |
| PW-MRL-API-153 | GET `/api/network-lab/lie-fulfillment` | lab JSON `success` (nie persistence) | existing-pom | N03 | P2 |
| PW-MRL-API-154 | GET `/api/network-lab/har-replay` | `{ source: har }` | existing-pom | N05 | P1 |

---

## 4.5 Bank-like Spring `/api/mirror-lab`

Auth: JWT `merchant:payments:lifecycle` lub `platform:payments:*` (TPP GET wyjątek).  
Step-up: header `X-Lab-Step-Up: confirmed` (lab, nie ACR).

### High-value

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-200 | POST `/high-value-refunds` amount 10000, brak header | 403 `step_up_required` | existing-ra | B01 | P0 |
| PW-MRL-API-201 | to samo + `X-Lab-Step-Up: confirmed` | 200 `accepted` | existing-ra | B01 | P0 |
| PW-MRL-API-202 | amount 9999 bez header | 200 accepted | designed BVA | B01 | P1 |
| PW-MRL-API-203 | amount 10000 header `yes` (nie `confirmed`) | 403 | designed EP | B01 | P1 |
| PW-MRL-API-204 | BFF POST high-value **bez** flagi | 404 | designed | C-03 | P0 |
| PW-MRL-API-205 | BFF POST high-value bez sesji, flag on | 401 (backendApi) | designed | S04 | P1 |

### Statements

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-210 | GET `?format=csv` | 200 `text/csv`; `Content-Disposition` filename statement.csv | existing-it / designed full headers | B02 | P1 |
| PW-MRL-API-211 | GET `?format=pdf` | 200 `application/pdf`; body starts `%PDF-` | designed RA bytes | B02 | P0 |
| PW-MRL-API-212 | BFF GET pdf `arrayBuffer` | bajty nieutf8-zepsute | existing-pom `mirror-lab.spec.ts` live `%PDF-` | B02 | P0 |
| PW-MRL-API-213 | `?format=xml` | 400 albo csv default — **ustalić z kodem** (`statement()` tylko pdf vs else csv) | designed: **200 csv** (else branch) | B02 | P2 |

### Disputes / evidence

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-220 | POST `/disputes` `{ merchantId }` | 200 `disputeId` + status OPEN | existing-ra | B03 | P1 |
| PW-MRL-API-221 | POST evidence `note.txt` text/plain | 200 `CLOSED` | existing-ra | B03 | P1 |
| PW-MRL-API-222 | evidence `malware.exe` octet-stream | 415 `unsupported_evidence_content_type` | existing-ra | B03 | P0 |
| PW-MRL-API-223 | evidence `huge.txt` 2MiB+1 | 413 `evidence_file_too_large` | existing-ra | B03 | P0 |
| PW-MRL-API-224 | dokładnie 2MiB | 200 | designed BVA | B03 | P1 |
| PW-MRL-API-225 | pusty plik | 400 `empty_evidence_file` | designed | B03 | P1 |
| PW-MRL-API-226 | filename `../x.txt` | 400 `invalid_evidence_filename` | designed | B03 | P1 |
| PW-MRL-API-227 | unknown disputeId | 404 `not_found` | designed | B03 | P2 |
| PW-MRL-API-228 | BFF multipart forward Authorization | 200 | designed | B03 | P2 |

### Maker-checker

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-230 | POST `/refund-approvals` amount 500, maker JWT | 200 `PENDING_APPROVAL` | existing-ra | B04 | P0 |
| PW-MRL-API-231 | amount 10000 | 200 + `stepUpUntil` ISO-8601 | existing-ra | B01/B04 | P1 |
| PW-MRL-API-232 | POST `.../approve` ten sam subject | 403 `self_approve_forbidden` | existing-ra | B04 | P0 |
| PW-MRL-API-233 | approve inny subject (platform admin) | 200 `APPROVED` | existing-ra | B04 | P0 |
| PW-MRL-API-234 | ponowne approve | 409 `approval_not_pending` | existing-ra | B04 | P0 |
| PW-MRL-API-235 | approve nieistniejące id | 404 `not_found` | designed | B04 | P2 |
| PW-MRL-API-236 | BFF approve bez sesji | 401 | designed | S04 | P1 |

**Uwaga:** to **nie** zmienia `payment_orders`. Oracle = wiersz `mrl_refund_approvals`, nie status płatności.

### Consent / TPP

TPP: `GET /api/mirror-lab/tpp/accounts` `permitAll`. Token: header `X-Lab-Consent-Token` **albo** query `token` (header wygrywa). Rate limit 30/min/IP → 429 `rate_limit_exceeded`.

| ID | Call | Expect | Pokrycie | FR | Prio |
|---|---|---|---|---|---|
| PW-MRL-API-240 | POST `/consents` JWT | 200 `consentId`, `accessToken`, `GRANTED` | existing-ra | B05 | P0 |
| PW-MRL-API-241 | GET TPP `?token=` | 200 `accounts[].iban` | existing-ra | B05 | P0 |
| PW-MRL-API-242 | GET TPP header `X-Lab-Consent-Token` (bez query) | 200 | existing-ra | B05 | P0 |
| PW-MRL-API-243 | GET TPP bez tokenu | 403 `consent_denied` | designed (controller) | B05 | P1 |
| PW-MRL-API-244 | revoke **innym** JWT | 403 `consent_owner_mismatch` | existing-ra | B05 | P0 |
| PW-MRL-API-245 | revoke właścicielem | 200 `REVOKED` | existing-ra | B05 | P0 |
| PW-MRL-API-246 | TPP po revoke | 403 `consent_denied` | existing-ra | B05 | P0 |
| PW-MRL-API-247 | 31× GET TPP / min / IP | 429 `rate_limit_exceeded` | designed · GAP-T05 | B05 | P1 |
| PW-MRL-API-248 | header zły, query dobry | 403 (header wygrywa) | designed | B05 | P1 |
| PW-MRL-API-249 | BFF TPP **bez** dashboard session, flag on | 200 jeśli header OK | designed (TPP BFF tylko `requireMirrorLab`) | B05 | P1 |
| PW-MRL-API-250 | BFF TPP forwarduje header do Spring, nie query | brak `?token=` w URL backend | designed | B05 | P1 |

---

## Nagłówki do asercji

`X-Correlation-ID` (echo lub wygenerowany), `Content-Type: application/problem+json` na błędach MRL, `Retry-After` na 503 lab, `Content-Disposition` statements, `Access-Control-Allow-Credentials` CORS lab.
