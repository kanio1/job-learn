---
name: epic-e1-protocol-core
parent: checkout-protocol-lab
epic: E1
tasks: [CPL-T04, CPL-T05, CPL-T06, CPL-T07, CPL-T20]
last_updated: 2026-08-09
---

# Epic E1 — Protocol Core (OAuth stub + OrderCreate + retrieve)

**Cel produktowy:** kontrakt „merchant → lab PSP” jak OrderCreate u PayU.  
**Cel dydaktyczny:** form-urlencoded OAuth, **302+Location**, lab Bearer ≠ Keycloak JWT.

**Połączenia:** INFRA-SEC-01 (`CPL-T04`); F-D2 dostanie `redirectUri`; RA `follow(false)`.

---

## Task CPL-T04 — Security chains (wspólny z E2) · P0

Zobacz też E2. W E1 wystarczy permit dla:

- `POST /api/checkout-lab/oauth/token`
- (później) lab-token filter dla `/sessions`

### Learning
- `HTTP:` osobny `SecurityFilterChain` + `@Order` (wzór `/api/test/*`).
- `KC:` **nie** walidujemy JWT Keycloak na tych pathach.

---

## Story E1-S1 — OAuth token stub  
**Task:** `CPL-T05` · P0 · FR-01

### Jako / chcę / aby
Jako integrator chcę `POST /api/checkout-lab/oauth/token` z form-urlencoded, aby dostać lab Bearer.

### Kontrakt

```http
POST /api/checkout-lab/oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials&client_id=checkout-lab-merchant&client_secret=***
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"access_token":"lab.…","token_type":"Bearer","expires_in":3600}
```

Negatywne:
- zły `Content-Type` → 401/415 (wybierz i udokumentuj; preferuj jasny 401 jak PSP-like)
- złe credentials → **401**

### Acceptance criteria
- [ ] Tylko gdy flaga on.
- [ ] Token wystarczy do `POST /sessions` (lab auth, nie KC).
- [ ] RA test: happy + bad secret + (opcjonalnie) missing CT.
- [ ] Token **nie** przechodzi `JwtDecoder` Keycloak (celowe).

### Learning
- `HTTP:` `application/x-www-form-urlencoded` vs JSON.
- `REST:` token endpoint jako osobny zasób protokołu.
- `KC:` kontrast — tu **nie** wołasz Keycloak token URL.

### Połączone z
- `CheckoutLabProperties.oauth*`
- E1-S2 (konsument Bearer)

---

## Story E1-S2 — Create session (302 + Location)  
**Task:** `CPL-T06` · P0 · FR-02

### Jako / chcę / aby
Jako integrator chcę utworzyć checkout session i dostać redirect do hosted page.

### Kontrakt

```http
POST /api/checkout-lab/sessions
Authorization: Bearer <lab-token>
Content-Type: application/json
Idempotency-Key: optional-until-E1-S3
X-Correlation-ID: corr-…
Lab-Force-Scenario: happy_completed   # opcjonalnie E6

{
  "extOrderId": "BOOK-123",
  "amountMinor": 1999,
  "currency": "PLN",
  "continueUrl": "http://localhost:3000/checkout-lab/return?sid={sessionId}",
  "notifyUrl": "http://localhost:8080/api/checkout-lab/notify",
  "validitySeconds": 900
}
```

```http
HTTP/1.1 302 Found
Location: http://localhost:3000/psp/checkout/{sessionId}
X-Correlation-ID: corr-…

{
  "sessionId": "…",
  "redirectUri": "http://localhost:3000/psp/checkout/{sessionId}",
  "status": "CREATED"
}
```

Uwaga: niektóre klienty ignorują body przy 302 — **`Location` jest kanoniczne**; `redirectUri` w JSON dla wygody RA/FE.

### Acceptance criteria
- [ ] Persist `checkout_session` + fulfillment `AWAITING_PAYMENT`.
- [ ] `Location` == `redirectUri`.
- [ ] RA: `redirects().follow(false)` + assert status 302 + header.
- [ ] Bez lab token → 401.
- [ ] Walidacja amount/currency jak payment (Problem Details).

### Szkic RA

```java
given()
    .redirects().follow(false)
    .auth().oauth2(labToken)
    .header("X-Correlation-ID", corr)
    .contentType(JSON)
    .body(payload)
.when()
    .post("/api/checkout-lab/sessions")
.then()
    .statusCode(302)
    .header("Location", startsWith("http://localhost:3000/psp/checkout/"));
```

### Learning
- `HTTP:` **302 + Location** jako kontrakt integracyjny (PayU OrderCreate mindset).
- `REST:` create zwraca zarówno redirect hop, jak i identyfikator zasobu.
- `SQL:` insert session + fulfillment w jednej transakcji.
- `PW:` browser później podąży za `Location` / `href`.

### Połączone z
- E4-S1 hosted page
- E7-S1 / REST-REDIRECT-01
- F-D2 upgrade path

---

## Story E1-S4 — Retrieve session  
**Task:** `CPL-T07` · P0

### Jako / chcę / aby
Jako worker/oracle chcę `GET /api/checkout-lab/sessions/{id}`.

### Acceptance criteria
- [ ] 200 JSON: status, amountMinor, currency, validityUntil, …
- [ ] GET z body → **403** (jeśli w projekcie jest taki wzorzec method semantics) lub 415 — spójnie z labem; preferuj istniejący styl z `apps/api-tests`.
- [ ] Nieistniejące id → 404 Problem Details.
- [ ] Auth: lab Bearer (MVP).

### Learning
- `REST:` retrieve jako source of truth dla refetch (E3-S2).
- `HTTP:` idempotent GET; brak side-effect.

### Połączone z
- E3-S2 refetch
- E7 oracle

---

## Story E1-S3 — Idempotent create + lease  
**Task:** `CPL-T20` · P1 · FR-11

### Jako / chcę / aby
Jako klient chcę bezpieczny retry create bez dwóch session.

### Acceptance criteria
- [ ] Ten sam `Idempotency-Key` + ten sam fingerprint → ta sama session (replay).
- [ ] Ten sam key + inny body → **409** `idempotency_conflict` (jak payment).
- [ ] Concurrent: jeden zwycięzca (DB unique / lease); test concurrent.
- [ ] Osobna tabela lub kolumna w `checkout_session` — **nie** `payment.idempotency_records`.

### Learning
- `REST:` Idempotency-Key semantyka biznesowa.
- `SQL:` unique partial index / reserve-then-complete (wzorzec `PaymentOrderService`).
- `HTTP:` nagłówek vs body fingerprint.

### Połączone z
- `payment/.../PaymentOrderService` (wzorzec, nie współdzielona tabela)
- `V5__harden_payment_http_contract.sql`
