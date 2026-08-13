# 05 — Playwright API: hosted, notify, bookings, ops, CORS

Kontynuacja `PW-API-###`. Target A = Spring `:8080` (`needs-backend`), o ile nie oznaczono Target B (BFF).

Helper `simulateCompleted(sessionId)`: GET hosted → token → POST simulate `Lab-Simulate-Token` + `{outcome: COMPLETED}`.

Envelope notify (bajty podpisu = **dokładnie** ten raw string):

```json
{"id":"evt_1","type":"checkout.session.completed","created":"2026-01-01T00:00:00Z","data":{"sessionId":"<uuid>","status":"COMPLETED","amountMinor":1999,"currency":"PLN","extOrderId":"ORD-001"}}
```

`Lab-Signature: t=<epochSeconds>,v1=<hmac-sha256-hex>` nad `"{t}." + rawBody`. Tolerancja timestampu: **300 s**.

---

## 5.1 Hosted GET — public

Path: `GET /api/checkout-lab/hosted/sessions/{sessionId}`  
**Bez** lab Bearer. DTO **bez** `notifyUrl`, `redirectUri`, `correlationId`.  
Token: `simulateToken` + `simulateTokenExpiresAt` tylko gdy status ∈ {CREATED, PENDING} i nie expired.

### PW-API-100 — GET hosted 200 + token, bez notifyUrl, bez Bearer

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | — |
| Status | **200** |
| Body | `sessionId, extOrderId, status=CREATED, amountMinor, currency, validityUntil, continueUrl, simulateToken` (64+ hex), `simulateTokenExpiresAt` |
| Body must-not | `notifyUrl` |
| Auth | public |

### PW-API-101 — GET hosted z lab Bearer nadal 200 (token ignorowany jako wymóg)

| | |
|---|---|
| Pokrycie | designed |
| Headers | `Authorization: Bearer lab.*` (zbędny) |
| Status | **200** ten sam DTO |

### PW-API-102 — GET unknown → 404 problem

| | |
|---|---|
| Pokrycie | designed |
| Status | **404** |
| Body | `error: not_found`, `instance` = `/api/checkout-lab/hosted/sessions/{id}` |

### PW-API-103 — GET po COMPLETED: token null

| | |
|---|---|
| Pokrycie | designed |
| Preconditions | simulate COMPLETED |
| Status | **200** |
| Body | `status: COMPLETED`; `simulateToken` null; `simulateTokenExpiresAt` null |

### PW-API-104 — GET gdy EXPIRED: token null

| | |
|---|---|
| Pokrycie | designed |
| Preconditions | clock past validity **lub** scenario EXPIRED_LINK + simulate 409 |
| Body | `simulateToken` null |

### PW-API-105 — BFF GET hosted (Target B) bez dashboard session

| | |
|---|---|
| Pokrycie | designed |
| Path | `GET /api/checkout-lab/hosted/sessions/{id}` na `:3000` |
| Auth | brak cookie |
| Status | **200** (BFF `requireDashboardSession: false`) |

---

## 5.2 POST simulate — public + capability token

Path: `POST /api/checkout-lab/hosted/sessions/{sessionId}/simulate`  
Header **wymagany:** `Lab-Simulate-Token`.  
Body: `{ "outcome": "<alias>" }`.  
Sukces: **200** hosted DTO.

Aliasy: `COMPLETED`/`APPROVED` → COMPLETED; `CANCELED`/`CANCELLED`/`DECLINED` → CANCELED; `PENDING` → PENDING.

### PW-API-110 — Brak tokenu → 403 missing_simulate_token

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | `Content-Type: application/json` (bez Lab-Simulate-Token) |
| Body | `{"outcome":"COMPLETED"}` |
| Status | **403** |
| Content-Type | `application/problem+json` |
| Body | `error: missing_simulate_token`, `code: MISSING_SIMULATE_TOKEN`, `status: 403` |
| Learning | **nie** 401 — nie mylić z Keycloak / lab Bearer |

### PW-API-111 — Śmieciowy token → 403 invalid_simulate_token

| | |
|---|---|
| Pokrycie | designed (unit SignatureService ma tamper) |
| Headers | `Lab-Simulate-Token: deadbeef` |
| Status | **403** |
| Body | `error: invalid_simulate_token`, `code: INVALID_SIMULATE_TOKEN` |

### PW-API-112 — Token innej sesji → 403 invalid

| | |
|---|---|
| Pokrycie | designed |
| Headers | token z GET session A na POST session B |
| Status | **403** `invalid_simulate_token` |

### PW-API-113 — Happy COMPLETED z tokenem z GET

| | |
|---|---|
| Pokrycie | existing-ra (helper `simulateCompleted`) |
| Headers | `Lab-Simulate-Token: <from GET>`; `Content-Type: application/json` |
| Body | `{"outcome":"COMPLETED"}` |
| Status | **200** |
| Body | `status: COMPLETED`; token często null |

### PW-API-114 — Alias APPROVED

| | |
|---|---|
| Pokrycie | designed |
| Body | `{"outcome":"APPROVED"}` |
| Status | **200** `COMPLETED` |

### PW-API-115 — Alias DECLINED / CANCELLED / CANCELED

| | |
|---|---|
| Pokrycie | designed |
| Body | trzy przebiegi (osobne sesje) |
| Status | **200** `CANCELED` (session spelling) |
| Oracle | po workerze fulfillment **CANCELLED** |

### PW-API-116 — PENDING nie emituje notify

| | |
|---|---|
| Pokrycie | designed |
| Body | `{"outcome":"PENDING"}` |
| Status | **200** `PENDING` |
| Oracle | 0 eventów; GET hosted nadal wydaje nowy token (status PENDING) |

### PW-API-117 — Unknown outcome → 400 validation

| | |
|---|---|
| Pokrycie | designed |
| Body | `{"outcome":"CAPTURED"}` |
| Status | **400** |
| Body | `error: validation` (`IllegalArgumentException`) |

### PW-API-118 — Blank outcome → 400 bean validation

| | |
|---|---|
| Pokrycie | designed |
| Body | `{"outcome":""}` lub omit |
| Status | **400**; details field `outcome` |

### PW-API-119 — Simulate na COMPLETED → 200 noop

| | |
|---|---|
| Pokrycie | designed |
| Kroki | COMPLETED raz, potem znowu (token z pierwszego GET może być nieważny — GET ponownie: token null; użyj **starego** tokenu z pamięci) |
| Status | **200** bez drugiego event **lub** 403 jeśli token już nie wydawany i stary wygasł |
| Uwaga | rozdziel: (a) drugi simulate **z tokenem sprzed terminal** w oknie TTL → 200 unchanged, 1 event; (b) GET po terminal nie daje tokenu |

### PW-API-120 — BFF forward Lab-Simulate-Token (Target B)

| | |
|---|---|
| Pokrycie | designed |
| Path | `POST :3000/api/checkout-lab/hosted/sessions/{id}/simulate` |
| Headers | `Lab-Simulate-Token` z GET BFF |
| Status | **200** |
| Uwaga | BFF `checkoutLabApi` musi przekazać header 1:1 |

### PW-API-121 — lowercase outcome `completed`

| | |
|---|---|
| Pokrycie | designed |
| Body | `{"outcome":"completed"}` |
| Status | **200** (`toUpperCase` w `toStatus`) |

---

## 5.3 Hosted fulfillment — public oracle

Path: `GET /api/checkout-lab/hosted/sessions/{sessionId}/fulfillment`

### PW-API-130 — Simulate expired → 409 expired_link, 0 eventów

| | |
|---|---|
| Pokrycie | existing-ra (clock) |
| Kroki | create → GET token → `POST /clock` `{instant}` po `validityUntil` → simulate COMPLETED z **starym** tokenem |
| Status | **409** |
| Body | `error: expired_link`, `code: EXPIRED_LINK` |
| Oracle | 0 eventów; session EXPIRED; fulfillment EXPIRED |

### PW-API-131 — GET hosted fulfillment AWAITING

| | |
|---|---|
| Pokrycie | designed |
| Status | **200** `{status: AWAITING_PAYMENT}` |
| Auth | public |

### PW-API-132 — GET hosted fulfillment unknown session → 404 **empty**

| | |
|---|---|
| Pokrycie | designed |
| Status | **404** |
| Body | puste (nie lookup session — `findBySessionId` orElse null) |
| Kontrast | Bearer GET session fulfillment unknown session → 404 **problem** |

### PW-API-133 — Public fulfillment po workerze CONFIRMED

| | |
|---|---|
| Pokrycie | designed |
| Status | **200** `CONFIRMED` |
| Learning | return page oracle |

---

## 5.4 POST `/api/checkout-lab/notify` — HMAC public

Auth: **nie** JWT, **nie** lab Bearer (ignorowane). Wymagany HMAC.

Sukces nowego: **202** `{duplicate:false, eventId}`.  
Duplikat: **200** `{duplicate:true, eventId}`.

### PW-API-200 — Happy 202 → worker CONFIRMED

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | `Lab-Signature: t=...,v1=...`; `Lab-Event-Id: evt_happy`; `Content-Type: application/json` |
| Body | raw envelope z `data.sessionId` |
| Status | **202** |
| Body | `duplicate: false`, `eventId` |
| Oracle | poll GET fulfillment → CONFIRMED; event processStatus DONE |

### PW-API-201 — Bez JWT (i bez Bearer) — HMAC wystarcza

| | |
|---|---|
| Pokrycie | existing-ra |
| Status | **202** (nie 401) |

### PW-API-202 — Lab Bearer na notify, zły HMAC → nadal 400

| | |
|---|---|
| Pokrycie | designed |
| Headers | `Authorization: Bearer lab.*` + zły/brak signature |
| Status | **400** `invalid_signature` |
| Learning | Bearer nie zastępuje HMAC |

### PW-API-203 — Brak Lab-Signature → 400 invalid_signature

| | |
|---|---|
| Pokrycie | designed |
| Status | **400** |
| Body | `error: invalid_signature`, `code: INVALID_SIGNATURE` |
| Oracle | 0 wierszy event |
| Learning | **do not retry** (jak Stripe 400, nie 401) |

### PW-API-204 — Zły v1 (tamper body po podpisie)

| | |
|---|---|
| Pokrycie | existing-ra (`BAD_SIGNATURE` outbound) + designed inbound |
| Kroki | podpisz body A, wyślij body B |
| Status | **400** |
| Oracle | 0 insert |

### PW-API-205 — Timestamp poza tolerancją (abs(now-t) ponad 300s)

| | |
|---|---|
| Pokrycie | designed |
| Headers | `t` = now-301 **oraz** now+301 (dwa TC) |
| Status | **400** `invalid_signature` |

### PW-API-206 — Timestamp na granicy 300s

| | |
|---|---|
| Pokrycie | designed |
| Headers | `t` = now-300, now+300 |
| Status | **202** (jeśli `≤ tolerance`; potwierdzić impl `|delta| <= 300`) |

### PW-API-207 — Malformed Lab-Signature

| | |
|---|---|
| Pokrycie | designed |
| Values | `v1=abc` bez t; `t=foo,v1=bar`; pusty string; `Bearer xyz` |
| Status | **400** `invalid_signature` |

### PW-API-208 — Duplicate eventId → 200, fulfillment nie podwójny

| | |
|---|---|
| Pokrycie | existing-ra |
| Kroki | ten sam signed envelope dwa razy |
| Status | 1st **202**, 2nd **200** `{duplicate:true}` |
| Oracle | 1 event; fulfillment CONFIRMED raz |

### PW-API-209 — Lab-Event-Id header vs body `id`

| | |
|---|---|
| Pokrycie | designed |
| Headers | `Lab-Event-Id: from-header` |
| Body | `"id":"from-body"` |
| Status | **202** |
| Body | `eventId` = **header** (jeśli impl: header optional else body id) |
| Uwaga | zanotować faktyczną precedencję z `receive(raw, eventId, signature)` |

### PW-API-210 — Brak event id (header i body)

| | |
|---|---|
| Pokrycie | designed |
| Status | **400** `invalid_signature` (brak id traktowany jak zły request) |

### PW-API-211 — Brak data.sessionId

| | |
|---|---|
| Pokrycie | designed |
| Status | **400** `invalid_signature` |

### PW-API-212 — Non-JSON body

| | |
|---|---|
| Pokrycie | designed |
| Body | `not-json` z poprawnym HMAC nad tymi bajtami |
| Status | **400** |

### PW-API-213 — NOTIFY_5XX_RETRY: 503 potem 202

| | |
|---|---|
| Pokrycie | existing-ra |
| Preconditions | create z `Lab-Force-Scenario: NOTIFY_5XX_RETRY` + simulate COMPLETED (notifier sam retry) **lub** dwa POST notify |
| Status | pierwsze inbound **503** `transient_error` / `TRANSIENT_ERROR`; retry **202** |
| Deliveries | 503, 202 |
| Oracle | CONFIRMED |
| Learning | **retry** przy 503 |

### PW-API-214 — 400 nie tworzy RECEIVED

| | |
|---|---|
| Pokrycie | designed (implikacja 203/204) |
| GET events | `[]` |

---

## 5.5 Bookings — lab Bearer

Path: `POST /api/checkout-lab/bookings`  
**Brak** Idempotency-Key na tej ścieżce.  
Body: `{mode, extOrderId, amountMinor, currency, continueUrl?, notifyUrl?, validitySeconds?}`.  
CASH case-insensitive. Inny niepusty mode → ONLINE. Defaults ONLINE: continueUrl return, notifyUrl lab notify, validity 900.

### PW-API-300 — CASH 200 CONFIRMED, brak session

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | `Authorization: Bearer lab.*` |
| Body | `{"mode":"CASH","extOrderId":"CASH-1","amountMinor":1999,"currency":"PLN"}` |
| Status | **200** |
| Body | `mode: CASH`, `fulfillmentStatus: CONFIRMED`, `sessionId: null`, `redirectUri: null`, `validityUntil: null`, `bookingId` uuid |
| Oracle | brak wiersza `checkout_session` (count 0 dla tego booking) |

### PW-API-301 — `mode: cash` (lowercase)

| | |
|---|---|
| Pokrycie | designed |
| Status | **200** CASH (`equalsIgnoreCase`) |

### PW-API-302 — ONLINE 200 AWAITING + redirectUri + validityUntil

| | |
|---|---|
| Pokrycie | designed |
| Body | `{"mode":"ONLINE","extOrderId":"ON-1","amountMinor":1999,"currency":"PLN"}` |
| Status | **200** (nie 302) |
| Body | `fulfillmentStatus: AWAITING_PAYMENT`, `sessionId` uuid, `redirectUri` zawiera `/psp/checkout/`, `validityUntil` ISO |
| Learning | dashboard form ≠ merchant 302 |

### PW-API-303 — ONLINE z Lab-Force-Scenario RETURN_LIE_SUCCESS

| | |
|---|---|
| Pokrycie | designed |
| Headers | Bearer + `Lab-Force-Scenario: RETURN_LIE_SUCCESS` |
| Następnie | simulate COMPLETED |
| Oracle | 0 notify / fulfillment zostaje AWAITING |

### PW-API-304 — POST bookings bez Bearer → 401

| | |
|---|---|
| Pokrycie | designed |
| Status | **401** empty |

### PW-API-305 — Invalid currency na booking

| | |
|---|---|
| Pokrycie | designed |
| Body | `currency: CHF` |
| Status | **400** `validation` |

### PW-API-306 — amount 0 na booking ONLINE

| | |
|---|---|
| Pokrycie | designed |
| Uwaga | record ma `long amountMinor` bez `@Min` — walidacja domain w `createSession` → 400. CASH może iść inną ścieżką — **sprawdzić oba**. |
| Status oczekiwany | **400** (jeśli domain validate); zanotować jeśli CASH omija |

### PW-API-307 — Blank mode → 400 `@NotBlank`

| | |
|---|---|
| Pokrycie | designed |
| Status | **400** details `mode` |

### PW-API-308 — GET `/bookings/{bookingId}` 200

| | |
|---|---|
| Pokrycie | designed |
| Path | `GET /api/checkout-lab/bookings/{bookingId}` |
| Headers | Bearer |
| Status | **200** fulfillment DTO |
| Body | cash: CONFIRMED, sessionId null |

### PW-API-309 — GET booking unknown → 404 empty

| | |
|---|---|
| Pokrycie | designed |
| Status | **404** puste (nie problem) |

### PW-API-310 — GET booking bez Bearer → 401

| | |
|---|---|
| Pokrycie | designed |
| Status | **401** |

### PW-API-311 — BFF POST bookings (Target B) 200 JSON

| | |
|---|---|
| Pokrycie | designed |
| Auth | Nuxt session |
| Status | **200** (follow redirect default OK — to nie 302) |

---

## 5.6 Ops — lab Bearer

### PW-API-400 — POST clock 200

| | |
|---|---|
| Pokrycie | existing-ra (użyty w expiry) |
| Path | `POST /api/checkout-lab/clock` |
| Headers | Bearer; `Content-Type: application/json` |
| Body | `{"instant":"2099-01-01T00:00:00Z"}` |
| Status | **200** `{instant: ...}` |

### PW-API-401 — Clock bez Bearer → 401

| | |
|---|---|
| Pokrycie | designed |
| Status | **401** |

### PW-API-402 — Clock nieparsowalny Instant

| | |
|---|---|
| Pokrycie | designed |
| Body | `{"instant":"not-an-instant"}` |
| Status | **500** (luka: brak handlera `DateTimeParseException`) **lub** 400 jeśli dodany |
| Uwaga | GAP testowy — zanotować faktyczny status; nie uczyć 500 jako „happy” |

### PW-API-403 — POST reset → tylko checkout_* 

| | |
|---|---|
| Pokrycie | existing-ra |
| Path | `POST /api/checkout-lab/reset` |
| Status | **200** `{status: reset}` |
| Oracle | count checkout_session/event/fulfillment/anomaly = 0; tabele merchant/payment **nie** zerowane |

### PW-API-404 — Reset bez Bearer → 401

| | |
|---|---|
| Pokrycie | designed |
| Status | **401** |

### PW-API-405 — POST reconcile created count + dedup

| | |
|---|---|
| Pokrycie | existing-ra (często przez bean; HTTP **designed** jeśli RA woła service) |
| Path | `POST /api/checkout-lab/reconcile` |
| Preconditions | session COMPLETED + fulfillment AWAITING (RETURN_LIE lub skip worker) |
| Status | **200** |
| Body 1st | `{created: 1}` (lub ≥1) |
| Body 2nd | `{created: 0}` |
| Oracle | 1 wiersz anomaly `kind=session_completed_fulfillment_pending` |

### PW-API-406 — GET anomalies 200 lista

| | |
|---|---|
| Pokrycie | designed |
| Path | `GET /api/checkout-lab/anomalies` |
| Headers | Bearer |
| Status | **200** |
| Body | `[{anomalyId, sessionId, kind, detail, detectedAt}]` |

### PW-API-407 — GET anomalies bez Bearer → 401

| | |
|---|---|
| Pokrycie | designed |
| Status | **401** |

### PW-API-408 — Reconcile/clock/anomalies z Keycloak JWT only → 401

| | |
|---|---|
| Pokrycie | designed |
| Headers | Bearer KC |
| Status | **401** |

---

## 5.7 CORS

Allowed origin: `http://localhost:3000`.  
Allowed headers m.in.: `Authorization`, `Idempotency-Key`, `X-Correlation-ID`, `Lab-Signature`, `Lab-Event-Id`, `Lab-Force-Scenario`, `Lab-Simulate-Token`.  
Exposed: `Location`, `Idempotency-Replayed`, `X-Correlation-ID`, lab signature headers.

### PW-API-450 — OPTIONS notify + Lab-Signature

| | |
|---|---|
| Pokrycie | existing-ra |
| Method | `OPTIONS /api/checkout-lab/notify` |
| Headers | `Origin: http://localhost:3000`; `Access-Control-Request-Method: POST`; `Access-Control-Request-Headers: Lab-Signature` |
| Status | 200/204 |
| Headers out | `Access-Control-Allow-Headers` zawiera `Lab-Signature` (case-insensitive) |

### PW-API-451 — OPTIONS simulate + Lab-Simulate-Token

| | |
|---|---|
| Pokrycie | designed |
| Path | `OPTIONS /api/checkout-lab/hosted/sessions/{id}/simulate` |
| ACRH | `Lab-Simulate-Token` |
| Headers out | Allow-Headers zawiera `Lab-Simulate-Token` |

### PW-API-452 — Origin inny niż localhost:3000

| | |
|---|---|
| Pokrycie | designed |
| Headers | `Origin: http://evil.example` |
| Asercja | brak `Allow-Origin: evil` **lub** request CORS fail |

### PW-API-453 — OPTIONS /sessions nie 401

| | |
|---|---|
| Pokrycie | existing-ra |
| Status | nie 401 (`shouldNotFilter` OPTIONS) |

---

## 5.8 Macierz statusów notify vs simulate (ściąga)

| Sytuacja | HTTP | `error` | Retry? |
|---|---|---|---|
| Nowe event | 202 | — | n/a (ACK) |
| Duplikat eventId | 200 | — | n/a |
| Zły HMAC / t / JSON / id | 400 | `invalid_signature` | **NIE** |
| NOTIFY_5XX pierwszy | 503 | `transient_error` | **TAK** |
| Brak simulate token | 403 | `missing_simulate_token` | n/a |
| Zły simulate token | 403 | `invalid_simulate_token` | n/a |
| Link wygasł | 409 | `expired_link` | n/a |
| Idempotency body mismatch | 409 | `idempotency_conflict` | n/a |
| Brak lab Bearer na merchant | 401 | (empty) | n/a |
| Unknown session GET | 404 | `not_found` | n/a |
