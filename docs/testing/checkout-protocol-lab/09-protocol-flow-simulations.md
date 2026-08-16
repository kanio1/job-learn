# 09 — Symulacje protokołu CPL (BA + test architect)

Hop-by-hop **use cases** i **test cases** Checkout Protocol Lab: pozytyw, negatyw, kombinacje scenariuszy.  
To **nie** jest `payment_orders`. JWT dashboardu tutaj **nie** działa (401). Operator: [live-pom 09](../live-pom-wave-2/09-core-domain-flows.md).

Target A: `http://127.0.0.1:8080`. Target B: BFF `http://127.0.0.1:3000`.  
`--full` / TLS: Target A′ = `https://api.payment-quality.local:8443`, hosted/continueUrl na `https://app.payment-quality.local:8443` — [live-pom 10 BC-EDGE-07](../live-pom-wave-2/10-full-stack-edge-flows.md).  
`POST /sessions`: `followRedirects: false`. Oracle pieniędzy = **fulfillment / events**, nigdy sam `continueUrl?status=`.

Indeks UC: [07](07-istqb-decision-state-usecase.md). Katalogi HTTP: [04](04-playwright-api-auth-sessions.md), [05](05-playwright-api-hosted-notify-ops.md).

---

## Tożsamości CPL (trzy światy)

| Świat | Auth | Gdzie |
|---|---|---|
| Lab merchant | `Authorization: Bearer lab.*` z OAuth stub | `/api/checkout-lab/sessions`, bookings, ops |
| Hosted płatnik | public GET + `Lab-Simulate-Token` | `/hosted/sessions/{id}`, `/simulate` |
| PSP stub notify | HMAC `Lab-Signature` + `Lab-Event-Id` | `POST /api/checkout-lab/notify` |

Dashboard Keycloak cookie otwiera **hub UI**; sam protokół sesji idzie lab Bearer (BFF wstrzykuje secret).

---

## BC-CPL-00 — Health i flaga

### MS — health on

```http
GET /api/checkout-lab/health
```

→ **200** `{ "status": "UP" }` (PW-API-001).

### EXC — flaga off

Profil `app.checkout-lab.enabled=false` → **404** (PW-API-002 / IT). UI hub skip E2E-064.

---

## BC-CPL-01 — OAuth stub

### UC-OAuth MS (PW-API-010)

```http
POST /api/checkout-lab/oauth/token
Content-Type: application/x-www-form-urlencoded
X-Correlation-ID: cpl-oauth-01

grant_type=client_credentials&client_id={labClient}&client_secret={labSecret}
```

→ **200** `{ "token_type": "Bearer", "expires_in": 3600, "access_token": "lab.…" }`. Echo correlation.

### EXC

| ID | Zmiana | HTTP | Body |
|---|---|---|---|
| PW-API-011 | zły secret | **401** | puste |
| PW-API-012 | `Content-Type: application/json` | **401** | puste (nie 415) |
| PW-API-016 | `grant_type` brak | **401**/walidacja wg RA | puste lub problem |

Dalsze EP: [04](04-playwright-api-auth-sessions.md) §4.2.

---

## BC-CPL-02 / UC-01 — Happy ONLINE (book → hosted → approve → notify → return)

**Aktorzy:** tester (dashboard) + płatnik (hosted tab).  
**Pre:** lab włączony; Nuxt+Spring.

### Krok 1 — Booking UI (ONLINE)

UI: `/admin/checkout-lab` → booking → mode ONLINE → submit (`checkout-booking-submit`).  
BFF analog:

```http
POST /api/checkout-lab/bookings
Authorization: Bearer lab.*
Content-Type: application/json

{
  "mode": "ONLINE",
  "extOrderId": "ORD-HAPPY-001",
  "amountMinor": 1999,
  "currency": "PLN"
}
```

→ **200** booking; fulfillment `AWAITING_PAYMENT`. (Defaults: continueUrl return, notifyUrl lab notify, validity 900.)  
Direct session (ten sam efekt protokołu):

```http
POST /api/checkout-lab/sessions
Authorization: Bearer lab.*
Content-Type: application/json
Idempotency-Key: sess-happy-001
X-Correlation-ID: cpl-01

{
  "extOrderId": "ORD-HAPPY-001",
  "amountMinor": 1999,
  "currency": "PLN",
  "continueUrl": "http://localhost:3000/checkout-lab/return",
  "notifyUrl": "http://localhost:8080/api/checkout-lab/notify",
  "validitySeconds": 900
}
```

→ **302**, `Location: …/psp/checkout/{sessionId}`, body `{ sessionId, redirectUri, status: "CREATED" }` (PW-API-020).  
`GET /api/checkout-lab/sessions/{id}` → **200**; fulfillment AWAITING.

### Krok 2 — Hosted GET (public)

```http
GET /api/checkout-lab/hosted/sessions/{sessionId}
```

→ **200**: `simulateToken` (64+ hex), **bez** `notifyUrl` (PW-API-100).  
UI: nowa karta `/psp/checkout/{id}`, `data-testid="psp-approve"`.

### Krok 3 — Simulate COMPLETED

```http
POST /api/checkout-lab/hosted/sessions/{sessionId}/simulate
Lab-Simulate-Token: {simulateToken}
Content-Type: application/json

{ "outcome": "COMPLETED" }
```

→ **200**, session `COMPLETED`. Lab emituje notify (happy).  
UI: klik `psp-approve`.

### EXC tokenu (ten sam krok, negatyw)

| Zmiana | HTTP | `error` | TC |
|---|---|---|---|
| brak `Lab-Simulate-Token` | **403** | `missing_simulate_token` | PW-API-110 |
| zły token | **403** | `invalid_simulate_token` | PW-API-111 designed |
| po `validityUntil` | **409** | `expired_link` | PW-API-130 |

### Krok 4 — Notify HMAC (to, co robi stub; ręczny Bruno)

Raw body (bajty podpisu = ten string):

```json
{"id":"evt_happy_1","type":"checkout.session.completed","created":"2026-01-01T00:00:00Z","data":{"sessionId":"<uuid>","status":"COMPLETED","amountMinor":1999,"currency":"PLN","extOrderId":"ORD-HAPPY-001"}}
```

```http
POST /api/checkout-lab/notify
Content-Type: application/json
Lab-Event-Id: evt_happy_1
Lab-Signature: t={epochSeconds},v1={hmacSha256Hex of "{t}."+rawBody}
```

→ **202**; worker → event DONE; fulfillment **CONFIRMED**. Duplicate ten sam `id` → **200** `{ "duplicate": true }` (PW-API-208).

### EXC notify

| Zmiana | HTTP | `error` | Retry? |
|---|---|---|---|
| zły HMAC / `BAD_SIGNATURE` | **400** | `invalid_signature` | nie |
| `NOTIFY_5XX_RETRY` pierwsze | **503** | `transient_error` | tak |
| timestamp poza ±300 s | **400** | signature | nie |

### Krok 5 — Return (hint)

UI: karta return `continueUrl?status=success`.  
`data-testid="fulfillment-status"` = `CONFIRMED`. Hint success jest **dodatkowy**.  
TC: PW-E2E-050, live POM E2E-060, PW-API-113/200.

### EXC-UC-01 create session

| ID | Zmiana | HTTP | `error` |
|---|---|---|---|
| PW-API-021 | brak Bearer | **401** empty | — |
| PW-API-022 | JWT Keycloak zamiast lab | **401** | designed |
| PW-API-037 | brak `continueUrl` | **400** | `validation` |
| BVA amount 0 / max+1 | **400** | `validation` | 06 BVA-001… |
| PW-API-029 | `Lab-Force-Scenario: GARBAGE` | **400** | `validation` |

---

## BC-CPL-03 / UC-02 — CASH

```http
POST /api/checkout-lab/bookings
Authorization: Bearer lab.*
Content-Type: application/json

{
  "mode": "CASH",
  "extOrderId": "ORD-CASH-001",
  "amountMinor": 500,
  "currency": "PLN"
}
```

→ **200**, fulfillment **CONFIRMED**, **brak** hosted tab.  
UI: select mode CASH (E2E-062 / PW-E2E-011). Mockowany PW używał prefixu `CASH-*` — live POM używa selecta.  
`mode: "cash"` (lowercase) → ONLINE/CASH case-insensitive wg kontrolera; EP-050.

**Negatyw:** Bearer na bookings brak → **401** empty (PW-API-304 designed).

---

## BC-CPL-04 / UC-03 — Lie return (`continueUrl` kłamie)

**Cel biznesowy:** sklep nie księguje po samym query `status=success`.

### MS UI (existing)

1. Booking ONLINE (jak UC-01 krok 1).
2. **Nie** klikaj Approve.
3. Otwórz `continueUrl` z `status=success` (albo return page z hintem).
4. Oracle: `fulfillment-status` ∈ `AWAITING_PAYMENT` \| `UNKNOWN` — **nie** `CONFIRMED`.
5. TC: PW-E2E-040, E2E-061.

### MS API header (designed PW-API-071)

```http
POST /api/checkout-lab/sessions
Authorization: Bearer lab.*
Content-Type: application/json
Lab-Force-Scenario: RETURN_LIE_SUCCESS

{ …happy JSON… }
```

Potem simulate `COMPLETED` jak UC-01.  
Oracle: **0** eventów notify; GET fulfillment nadal AWAITING; return hint może mówić success.

---

## BC-CPL-05 / UC-04 — Decline / USER_CANCEL

UI: `psp-decline` (testid analogiczny do approve) → return.  
Fulfillment **CANCELLED**; hint zawiera `failure` (query może być tablicą `success,failure` — GAP-W2-03, asercja `toContainText('failure')`).  
Session status może być `CANCELED` ≠ fulfillment `CANCELLED`.

```http
POST /api/checkout-lab/hosted/sessions/{id}/simulate
Lab-Simulate-Token: {token}
Content-Type: application/json

{ "outcome": "CANCELED" }
```

Aliasy Decline: PW-API-115. Header `USER_CANCEL` jest **no-op** (GAP-03) — Decline i tak canceluje.  
TC: E2E-063, PW-E2E-022/042 existing-pom.

---

## BC-CPL-06 / UC-05 — PAY_NO_RETURN (designed)

**Cel:** płatnik zapłacił, zamknął kartę, nie wrócił do sklepu — merchant i tak ma CONFIRMED.

1. Jak UC-01 kroki 1–3 (Approve / simulate COMPLETED + notify 202).
2. **Nie** `goto` return / `continueUrl`.
3. Oracle:

```http
GET /api/checkout-lab/hosted/sessions/{id}/fulfillment
```

lub GET booking fulfillment → **CONFIRMED**.  
TC: PW-E2E-043, PW-API-075 **designed** (GAP-02). Header `PAY_NO_RETURN` nie zmienia logiki (to happy minus return).

---

## BC-CPL-07 / UC-06 — Expired link

```http
POST /api/checkout-lab/sessions
Authorization: Bearer lab.*
Lab-Force-Scenario: EXPIRED_LINK
Content-Type: application/json

{ …happy JSON… }
```

albo `POST /api/checkout-lab/clock` w przyszłość + simulate.

Simulate → **409** `expired_link`. UI: `psp-link-expired` (E2E-065). Approve zablokowane: częściowo designed (PW-E2E-024).

---

## BC-CPL-08 — Idempotencja sesji („już utworzone”)

Fingerprint = `extOrderId|amountMinor|currency|continueUrl|notifyUrl` (**bez** `validitySeconds`). Hash = SHA-256(**tylko** klucz).

| # | Headers / body | HTTP | Headers out | TC |
|---|---|---|---|---|
| 1 | nowy key | **302** nowy sessionId | `Idempotency-Replayed: false`/brak | PW-API-020 |
| 2 | ten sam key + ten sam fp | **302** to samo id | `Idempotency-Replayed: true` | PW-API-024 existing-ra |
| 3 | ten sam key + inny `amountMinor` | **409** | `error: idempotency_conflict` | PW-API-025 |
| 4 | ten sam key + tylko inny `validitySeconds` | **302** replay | Replayed | PW-API-026 **designed** (GAP-07) |
| 5 | brak key | **302** zawsze nowy id | — | PW-API-027 |

To **nie** jest replay `payment_orders` (tam drugi POST = **200**). Tu create session zawsze semantyka redirect **302**.

---

## BC-CPL-09 — Duplicate notify vs simulate noop

| Przypadek | Wynik | TC |
|---|---|---|
| Drugi POST notify ten sam `Lab-Event-Id` / body `id` | **200** `{duplicate:true}`, 0 drugi inbox | PW-API-208 |
| Simulate COMPLETED na już COMPLETED | noop, brak drugiego notify | PW-API-114 |
| `OOO_EVENTS` | enum **bez** reorder | **blocked** GAP-01, PW-API-076 |

---

## BC-CPL-10 — Kombinacje mode × currency × scenario (pairwise)

Pełna tablica: [07 PWISE](07-istqb-decision-state-usecase.md). Minimum do symulacji myślowej:

| Para | Oracle |
|---|---|
| ONLINE × PLN × HAPPY | CONFIRMED + event DONE |
| ONLINE × EUR × RETURN_LIE | AWAITING + hint success |
| ONLINE × USD × BAD_SIGNATURE | 400, 0 event |
| ONLINE × PLN × 5XX | 503 potem 202 CONFIRMED |
| ONLINE × EUR × EXPIRED | 409 |
| ONLINE × PLN × PAY_NO_RETURN | CONFIRMED bez return (**designed**) |
| CASH × PLN | CONFIRMED, brak hosted |
| ONLINE × USD × OOO | blocked |

Nie mnożyć amount × language × validity w tej samej fali.

---

## BC-CPL-EDGE — HTTPS / Caddy (iteracja 2)

Pełny brzeg (vhosty, DENY, `/__oidc*`, mixed `iss`): [live-pom 10 BC-EDGE-07](../live-pom-wave-2/10-full-stack-edge-flows.md).

Na `--full` `Location` sesji = `https://app.payment-quality.local:8443/psp/checkout/{id}`.  
`continueUrl` / `notifyUrl` w body też na `app.` / `api.` HTTPS.  
JWT Keycloak na `/api/checkout-lab/sessions` nadal **401**. Hosted UI: nowa karta, nie iframe (`X-Frame-Options: DENY`).

---

## Mapowanie UC-01…10

| UC | BC tutaj | Pokrycie skrót |
|---|---|---|
| UC-01 | BC-CPL-02 | existing PW/RA/POM |
| UC-02 | BC-CPL-03 | existing-pom select; RA bookings |
| UC-03 | BC-CPL-04 | UI existing; API 071 designed |
| UC-04 | BC-CPL-05 | existing-pom |
| UC-05 | BC-CPL-06 | designed |
| UC-06 | BC-CPL-07 | RA + POM expired testid |
| UC-07 inspector | 05 PW-E2E-060… | cienkie PW |
| UC-08 unauth | hub login vs hosted public | E2E-003 / PW-E2E-020 |
| UC-09 reconcile | PW-API-405 | existing-ra |
| UC-10 unknown scenario | PW-API-029 | designed/P1 |
| UC-01 na `--full` | BC-CPL-EDGE | Location `APP`; RA test zostaje localhost |
