# 04 — Playwright API: OAuth, health, sessions

Klient: Playwright `APIRequestContext` (`request` fixture lub `request.newContext({ baseURL })`).  
**Target A (protokół):** `http://127.0.0.1:8080` — wymaga Spring + Postgres (`needs-backend`).  
**Target B (BFF):** `http://127.0.0.1:3000` — Nuxt proxy; lab Bearer wstrzykuje serwer (browser nie widzi secret).

O ile nie napisano inaczej: Target A. `followRedirects: false` na `POST /sessions`.

Problem+json pola: `type`, `title`, `status`, `detail`, `instance`, `code`, `correlationId`, `error`, `message`, `details[]`.  
401 lab Bearer / OAuth: **puste body**, nie problem+json.

Helper: `obtainLabToken()` = PW-API-001.  
Happy session body:

```json
{
  "extOrderId": "ORD-001",
  "amountMinor": 1999,
  "currency": "PLN",
  "continueUrl": "http://localhost:3000/checkout-lab/return",
  "notifyUrl": "http://localhost:8080/api/checkout-lab/notify",
  "validitySeconds": 900
}
```

---

## 4.1 Health

### PW-API-001 — GET health public 200

| Pole | Wartość |
|---|---|
| Pokrycie | existing-ra |
| Method / path | `GET /api/checkout-lab/health` |
| Headers | — |
| Body | — |
| Status | **200** |
| Response body | `{ "status": "UP" }` |
| Response headers | `Content-Type: application/json` |
| Auth | brak |

### PW-API-002 — Health z flagą off → 404

| Pole | Wartość |
|---|---|
| Pokrycie | existing-ra (`CheckoutLabEndpointsDisabledIT`) |
| Method / path | `GET /api/checkout-lab/health` |
| Status | **404** |
| Uwaga | osobny profil `app.checkout-lab.enabled=false`; echo `X-Correlation-ID` jeśli wysłany |

---

## 4.2 OAuth stub — `POST /api/checkout-lab/oauth/token`

Wymagane: `Content-Type: application/x-www-form-urlencoded`.  
Sukces: `{ access_token, token_type: "Bearer", expires_in: 3600 }`, token prefix `lab.`.

### PW-API-010 — Happy client_credentials

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | `Content-Type: application/x-www-form-urlencoded`; opcjonalnie `X-Correlation-ID: corr-010` |
| Body (form) | `grant_type=client_credentials&client_id=<id>&client_secret=<secret>` |
| Status | **200** |
| Body | `token_type=Bearer`, `expires_in=3600`, `access_token` starts with `lab.` |
| Headers out | echo `X-Correlation-ID` jeśli podany (SecurityChain) |

### PW-API-011 — Zły client_secret

| | |
|---|---|
| Pokrycie | existing-ra |
| Body | poprawny grant, zły secret |
| Status | **401** |
| Body | puste |
| Content-Type | nie problem+json |

### PW-API-012 — JSON Content-Type → 401 nie 415

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | `Content-Type: application/json` |
| Body | `{"grant_type":"client_credentials",...}` |
| Status | **401** |
| Learning | błąd składni/credentials traktowany jednolicie |

### PW-API-013 — Brak grant_type / zły grant

| | |
|---|---|
| Pokrycie | designed |
| Body | `grant_type=authorization_code` **lub** brak grant |
| Status | **401** empty |

### PW-API-014 — Brak client_id lub client_secret

| | |
|---|---|
| Pokrycie | designed |
| Partycje | missing id; missing secret; both blank |
| Status | **401** empty |

### PW-API-015 — GET /oauth/token (zła metoda na łańcuchu)

| | |
|---|---|
| Pokrycie | existing-ra (SecurityChain: GET → 401 main chain) |
| Method | `GET /api/checkout-lab/oauth/token` |
| Status | **401** (główny security chain, nie stub POST) |

### PW-API-016 — Lab token odrzucony przez Keycloak decoder (payment API)

| | |
|---|---|
| Pokrycie | existing-ra |
| Kroki | token z 010 → `Authorization: Bearer lab.*` na `GET /api/merchants` (lub inny JWT resource) |
| Status | **401** |
| Learning | `lab.*` ≠ Keycloak JWT |

---

## 4.3 POST `/api/checkout-lab/sessions` — 302

Auth: `Authorization: Bearer <lab token>`.  
Sukces: **302 Found**, `Location: {hostedBase}/psp/checkout/{sessionId}`, body `{sessionId, redirectUri, status: CREATED}`.

### PW-API-020 — Happy 302 + persist

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | `Authorization: Bearer lab.*`; `Content-Type: application/json`; `X-Correlation-ID: corr-020` |
| Body | happy JSON wyżej |
| Status | **302** |
| Headers out | `Location` kończy się `/psp/checkout/{uuid}`; `X-Correlation-ID: corr-020` |
| Body | `sessionId` uuid, `redirectUri` = Location, `status: CREATED` |
| Oracle | GET session 200; fulfillment AWAITING_PAYMENT |

### PW-API-021 — Brak Bearer → 401 empty

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | tylko Content-Type |
| Status | **401** |
| Body | puste |

### PW-API-022 — Keycloak JWT zamiast lab Bearer → 401

| | |
|---|---|
| Pokrycie | designed |
| Headers | `Authorization: Bearer <KC access token>` |
| Status | **401** |
| Learning | filtr lab nie dekoduje realm JWT |

### PW-API-023 — OPTIONS bez Bearer (preflight)

| | |
|---|---|
| Pokrycie | existing-ra |
| Method | `OPTIONS /api/checkout-lab/sessions` |
| Headers | `Origin: http://localhost:3000`; `Access-Control-Request-Method: POST`; `Access-Control-Request-Headers: authorization,idempotency-key,lab-force-scenario` |
| Status | **nie 401** (200/204) |
| Headers out | `Access-Control-Allow-Origin`; Allow-Headers zawiera Authorization |

### PW-API-024 — Idempotency replay

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | ten sam `Idempotency-Key: key-024`, identyczny body, dwa POST |
| Status | oba **302** |
| Headers out | drugi: `Idempotency-Replayed: true` |
| Body | ten sam `sessionId` |
| Oracle | 1 wiersz session w DB |

### PW-API-025 — Idempotency conflict 409

| | |
|---|---|
| Pokrycie | existing-ra |
| Headers | ten sam key, **inny** `amountMinor` (lub extOrderId/currency/continueUrl/notifyUrl) |
| Status | **409** |
| Content-Type | `application/problem+json` |
| Body | `error: idempotency_conflict`, `code: IDEMPOTENCY_CONFLICT`, `status: 409`, `instance` zawiera `/api/checkout-lab/sessions` |

### PW-API-026 — Zmiana tylko validitySeconds → replay (fingerprint bez tego pola)

| | |
|---|---|
| Pokrycie | designed |
| Body 2 | ten sam fingerprint, `validitySeconds: 60` vs 900 |
| Status | **302** replay, **nie** 409 |
| Headers out | `Idempotency-Replayed: true` |

### PW-API-027 — Brak Idempotency-Key → dwa różne sessionId

| | |
|---|---|
| Pokrycie | designed |
| Status | dwa 302, różne UUID |

### PW-API-028 — Generowanie correlation gdy brak headera

| | |
|---|---|
| Pokrycie | designed |
| Headers | Bearer, bez X-Correlation-ID |
| Headers out | `X-Correlation-ID` obecny (UUID) |
| GET session | `correlationId` w JSON = ten header |

### PW-API-029 — Unknown Lab-Force-Scenario → 400 validation

| | |
|---|---|
| Pokrycie | designed |
| Headers | `Lab-Force-Scenario: not_a_real_scenario` |
| Status | **400** |
| Body | `error: validation`, `code: VALIDATION` |

### PW-API-030 — EXPIRED_LINK na create: validity w przeszłości

| | |
|---|---|
| Pokrycie | designed (RA używa clock, niekoniecznie header) |
| Headers | `Lab-Force-Scenario: EXPIRED_LINK` |
| Status | **302** (sesja powstaje) |
| GET session | `status: CREATED` ale `validityUntil` przed now |
| Następny | simulate → 409 (PW-API-130) |

### PW-API-031 — Invalid currency

| | |
|---|---|
| Pokrycie | existing-ra |
| Body | `currency: GBP` (lub `pln` lowercase — domain Set jest case-sensitive) |
| Status | **400** |
| Body | `error: validation` |

### PW-API-032 — Bean validation amount 0

| | |
|---|---|
| Pokrycie | designed |
| Body | `amountMinor: 0` |
| Status | **400** |
| Body | `error: validation`; `details[]` field `amountMinor` |

### PW-API-033 — amount 100_000_001

| | |
|---|---|
| Pokrycie | designed |
| Body | `amountMinor: 100000001` |
| Status | **400** validation |

### PW-API-034 — amount 1 i 100_000_000 (granice ważne)

| | |
|---|---|
| Pokrycie | designed |
| Status | **302** dla obu |

### PW-API-035 — extOrderId blank / 121 znaków

| | |
|---|---|
| Pokrycie | designed |
| Body | `""` lub 121×`A` |
| Status | **400**; details field `extOrderId` |

### PW-API-036 — extOrderId 120 znaków OK

| | |
|---|---|
| Pokrycie | designed |
| Status | **302** |

### PW-API-037 — Brak continueUrl / notifyUrl / validitySeconds

| | |
|---|---|
| Pokrycie | designed |
| Body | omit each (3 cases) |
| Status | **400** validation |

### PW-API-038 — validitySeconds 0

| | |
|---|---|
| Pokrycie | designed |
| Body | `validitySeconds: 0` |
| Status | **400** (`@Min(1)`) |

### PW-API-039 — BFF POST sessions nie followuje 302 na HTML

| | |
|---|---|
| Pokrycie | designed (Target B) |
| Path | `POST http://127.0.0.1:3000/api/checkout-lab/sessions` |
| Auth | Nuxt session cookie (dashboard) |
| Status | **302** (BFF `redirect: 'manual'` / `followRedirects: false`) |
| Body | JSON `{sessionId, redirectUri, status}` **nie** HTML Nuxt |
| Headers out | `Location` hosted URL |

### PW-API-040 — PUT/PATCH/DELETE sessions → 405 (jeśli Spring default)

| | |
|---|---|
| Pokrycie | designed |
| Status | **405** Method Not Allowed (lub 401 jeśli filtr wcześniej — zanotować faktyczny) |
| Uwaga | interview: HTTP method semantics |

---

## 4.4 GET session / events / deliveries / fulfillment (Bearer)

Wszystkie: `Authorization: Bearer lab.*`. 404 sesji = problem+json `not_found`.

### PW-API-050 — GET session 200 snapshot

| | |
|---|---|
| Pokrycie | existing-ra |
| Path | `GET /api/checkout-lab/sessions/{sessionId}` |
| Status | **200** |
| Body | `sessionId, extOrderId, status, amountMinor, currency, validityUntil, continueUrl, notifyUrl, redirectUri, correlationId` |
| Uwaga | merchant GET **zawiera** `notifyUrl` (w przeciwieństwie do hosted) |

### PW-API-051 — GET unknown → 404 problem

| | |
|---|---|
| Pokrycie | existing-ra |
| Path | losowy UUID v4 |
| Status | **404** |
| Body | `error: not_found`, `code: NOT_FOUND`, `instance` = path |

### PW-API-052 — GET bez Bearer → 401

| | |
|---|---|
| Pokrycie | existing-ra |
| Status | **401** empty |

### PW-API-053 — GET dwukrotnie nie tworzy wierszy

| | |
|---|---|
| Pokrycie | existing-ra |
| Oracle | count session = 1 |

### PW-API-054 — GET events pusta lista po create

| | |
|---|---|
| Pokrycie | designed |
| Path | `GET .../sessions/{id}/events` |
| Status | **200** |
| Body | `[]` |

### PW-API-055 — GET events po happy notify+worker

| | |
|---|---|
| Pokrycie | designed (RA happy nie listuje events wprost) |
| Status | **200** |
| Body[0] | `eventType` completed; `processStatus` DONE (po workerze); `ackStatus` 202; `signatureHeader` starts with `t=` |
| Schema | `payload` object; `lastError` null |

### PW-API-056 — GET events unknown session 404 problem

| | |
|---|---|
| Pokrycie | designed |
| Status | **404** `not_found` |

### PW-API-057 — GET deliveries po NOTIFY_5XX_RETRY

| | |
|---|---|
| Pokrycie | existing-ra (log asercje w Protocol test) |
| Path | `GET .../deliveries` |
| Status | **200** |
| Body | attempt 1 `responseStatus: 503`; attempt 2 `202` (kolejność) |

### PW-API-058 — GET deliveries unknown session 404

| | |
|---|---|
| Pokrycie | designed |
| Status | **404** problem |

### PW-API-059 — GET fulfillment Bearer 200 AWAITING po create

| | |
|---|---|
| Pokrycie | designed |
| Path | `GET .../sessions/{id}/fulfillment` |
| Status | **200** |
| Body | `status: AWAITING_PAYMENT`; `fulfillmentId` uuid; `sessionId`; `confirmedAt` null |

### PW-API-060 — GET fulfillment Bearer po worker CONFIRMED

| | |
|---|---|
| Pokrycie | designed |
| Body | `status: CONFIRMED`; `sourceEventId` nie-null; `confirmedAt` ISO |

### PW-API-061 — GET fulfillment gdy sesja jest, fulfillment usunięty — 404 empty

| | |
|---|---|
| Pokrycie | designed |
| Uwaga | trudne bez reset częściowego; jeśli niemożliwe — skip. Kontrakt: `orElseGet 404` **empty**, nie problem, gdy session lookup przeszedł a fulfillment brak |
| Kontrast | unknown session → 404 **problem** (session looked up first) |

### PW-API-062 — GET events/deliveries/fulfillment bez Bearer → 401

| | |
|---|---|
| Pokrycie | designed |
| Paths | trzy GET |
| Status | **401** empty każdy |

---

## 4.5 Scenariusze na create (header) — mostek do 05

| ID | Header `Lab-Force-Scenario` | Oczekiwane po łańcuchu | Pokrycie |
|---|---|---|---|
| PW-API-070 | (brak) / `HAPPY_COMPLETED` | notify po COMPLETED simulate | existing-ra |
| PW-API-071 | `RETURN_LIE_SUCCESS` | simulate COMPLETED → **0** eventów, fulfillment AWAITING | designed |
| PW-API-072 | `BAD_SIGNATURE` | delivery 400, 0 inbox insert | existing-ra |
| PW-API-073 | `NOTIFY_5XX_RETRY` | 503 potem 202 | existing-ra |
| PW-API-074 | `USER_CANCEL` | brak specjalnej logiki; Decline nadal CANCELED | designed (dokumentuj no-op) |
| PW-API-075 | `PAY_NO_RETURN` | jak happy; fulfillment CONFIRMED bez GET return | designed |
| PW-API-076 | `OOO_EVENTS` | **blocked** GAP-01 — dziś jak happy | blocked |
| PW-API-077 | lowercase `expired_link` | `valueOf(upper)` → działa jak EXPIRED_LINK | designed |

---

## Checklist asercji REST (każdy TC 04)

1. Status code.  
2. `Content-Type` (`application/json` / `application/problem+json` / brak przy 401).  
3. Headers: `Location`, `Idempotency-Replayed`, `X-Correlation-ID`.  
4. Body `error` + `code` + `instance` na błędach.  
5. Efekt: liczba sesji/eventów/fulfillment status.
