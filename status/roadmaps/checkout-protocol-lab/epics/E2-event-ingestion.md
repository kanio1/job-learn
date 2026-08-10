---
name: epic-e2-event-ingestion
parent: checkout-protocol-lab
epic: E2
tasks: [CPL-T08, CPL-T09, CPL-T10, CPL-T21]
last_updated: 2026-08-09
---

# Epic E2 — Event Ingestion (signature · inbox · 400/503 · dedup)

**Cel produktowy:** merchant receiver jak notifyUrl PayU — szybki ACK, weryfikacja, kolejka.  
**Cel dydaktyczny:** HMAC na raw body; **400 nie retry / 503 retry**; at-least-once + dedup.

**Połączenia:** stub po Approve (E4); worker (E3); Inspector (E6); **bez Keycloak JWT**.

---

## Story E2-S1 — Stub notifier  
**Task:** `CPL-T08` · P0 · FR-05

### Jako / chcę / aby
Jako lab (PSP-stub) chcę emitować POST na `notifyUrl` po akcji checkout.

### Envelope (przykład)

```http
POST /api/checkout-lab/notify
Content-Type: application/json
Lab-Event-Id: evt_01H…
Lab-Signature: t=1710000000,v1=hexhmac
X-Correlation-ID: corr-…

{
  "id": "evt_01H…",
  "type": "checkout.session.completed",
  "created": 1710000000,
  "data": {
    "sessionId": "…",
    "status": "COMPLETED",
    "amountMinor": 1999,
    "currency": "PLN",
    "extOrderId": "BOOK-123"
  }
}
```

### Signature (Stripe-like mindset)

```text
signed_payload = "{t}." + raw_body_bytes_as_string
v1 = HMAC_SHA256(hmacSecret, signed_payload)   # hex
```

Constant-time compare po stronie receivera.

### Acceptance criteria
- [ ] Emit po Approve/Decline/Pending zgodnie ze scenariuszem.
- [ ] Podpis liczony na **raw** bytes wysyłanego body.
- [ ] `Lab-Event-Id` unikalne per emisja.
- [ ] Log: sessionId, eventId, correlationId.

### Learning
- `HTTP:` custom signature header (analog `OpenPayu-Signature`).
- `REST:` notify to callback, nie „user CRUD”.
- `KC:` stub **nie** dołącza Bearer Keycloak.

### Połączone z
- E4-S1 (trigger)
- E2-S2 (konsument)
- E6-S1 (Inspector zapisuje emisję)

---

## Story E2-S2 — Merchant receiver verify + enqueue  
**Task:** `CPL-T09` · P0 · FR-06 · FR-09

### Jako / chcę / aby
Jako merchant chcę endpoint, który weryfikuje podpis i kolejuje event.

### Macierz odpowiedzi

| Warunek | Status | Retry stubu? |
|---|---|---|
| OK verify + insert inbox | **202** | nie |
| Zły podpis / |Δt| > tolerance | **400** | **nie** |
| Błąd chwilowy (DB down) | **503** | **tak** |
| Duplicate event_id (patrz E2-S3) | **200** | nie |

### Krytyczna implementacja

```text
Filter / Controller:
  1. przeczytaj raw body bytes (przed Jackson)
  2. wyciągnij t,v1 z Lab-Signature
  3. jeśli |now - t| > tolerance → 400
  4. HMAC compare constant-time → fail → 400
  5. INSERT checkout_event (RECEIVED) → 202
  6. NIE spełniaj fulfillment tutaj
```

### Acceptance criteria
- [ ] Security: path bez JWT (CPL-T04).
- [ ] Unit/IT: bad signature → 400, brak wiersza DONE fulfillment.
- [ ] Happy → 202 + wiersz `RECEIVED`.
- [ ] Czas ACK krótki (enqueue only) — NFR-01.

### Learning
- `HTTP:` semantyka **202** vs **200**; **400 vs 503** to lekcja retry policy.
- `SQL:` insert inbox w transakcji krótkiej.
- `KC:` najważniejszy kontrast labu — IPN ≠ OAuth2 Resource Server.

### Połączone z
- `SecurityConfig` (nowy chain)
- E3 worker
- Big-tech practice: accept-then-queue

---

## Story E2-S3 — Duplicate event handling  
**Task:** `CPL-T10` · P0 · FR-08

### Jako / chcę / aby
Jako system chcę safety na at-least-once delivery.

### Acceptance criteria
- [ ] Drugi POST z tym samym `Lab-Event-Id` → **200** `{ "duplicate": true }`.
- [ ] Fulfillment count / status nie zmienia się drugi raz.
- [ ] UNIQUE(`event_id`) egzekwowane na DB (IT).

### Learning
- `HTTP:` 200 na duplicate (ACK bez pracy) — różne szkoły; tu **udokumentowane**.
- `SQL:` UNIQUE jako ostatnia linia obrony race.

### Połączone z
- E2-S4 retries
- E3 (idempotent apply)

---

## Story E2-S4 — Stub retry policy  
**Task:** `CPL-T21` · P1

### Jako / chcę / aby
Jako PSP-stub chcę retry tylko przy transient errors.

### Acceptance criteria
- [ ] Po 400: 0 dalszych prób.
- [ ] Po 503: ≥1 retry (skrócony backoff labowy, np. 100ms/200ms).
- [ ] Inspector pokazuje attempts (E6).
- [ ] Test z stubem force-503 (scenario).

### Learning
- `HTTP:` dlaczego bad signature **nie** wolno retryować w kółko.
- `REST:` klient (stub) respektuje status merchant receivera.
