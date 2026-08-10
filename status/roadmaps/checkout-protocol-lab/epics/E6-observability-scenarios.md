---
name: epic-e6-observability-scenarios
parent: checkout-protocol-lab
epic: E6
tasks: [CPL-T27, CPL-T28, CPL-T29]
last_updated: 2026-08-09
---

# Epic E6 — Observability & Scenarios

**Cel produktowy:** uczynić protokół **obserwowalnym** i sterowalnym w labie.  
**Cel dydaktyczny:** dump headers/body/ACK; scenariusze negatywne bez prawdziwego chaosu prod.

**Połączenia:** każdy hop E1–E4 zapisuje się do Inspectora; scenariusze sterują stubem/receiverem.

---

## Story E6-S1 — Event Inspector  
**Task:** `CPL-T27` · P1 · FR-13

### Jako / chcę / aby
Jako SDET chcę listę eventów: headers, body, ACK, retries, duplicate.

### Acceptance criteria
- [ ] UI i/lub API: lista po `sessionId`.
- [ ] Pola: eventId, type, signature header, HTTP response code stub←receiver, attempts, process_status, duplicate flag.
- [ ] `data-testid` do PW.
- [ ] Czytelne raw JSON (z JSONB).

### Learning
- `HTTP:` uczysz się czytać realne nagłówki IPN.
- `SQL:` JSONB payload round-trip.
- `KC:` Inspector za JWT dashboard OK (to nie jest notify path).
- `PW:` oracle pomocniczy obok fulfillment API.

### Połączone z
- `checkout_event` table
- E2-S4 attempts
- E7 tests

---

## Story E6-S2 — Scenario engine  
**Task:** `CPL-T28` · P1 · FR-14

### Jako / chcę / aby
Jako tester chcę wymusić scenariusze bez ręcznego hackowania.

### Mechanizm

```http
POST /api/checkout-lab/sessions
Lab-Force-Scenario: return_lie_success
```

lub `?scenario=` na create / simulate.

### Minimalny katalog

| Scenario | Oczekiwane zachowanie |
|---|---|
| `happy_completed` | Approve → completed event → CONFIRMED |
| `user_cancel` | Decline → CANCELLED |
| `return_lie_success` | continueUrl hint=success, **brak** completed event |
| `bad_signature` | stub wysyła zły HMAC → receiver 400 |
| `notify_5xx_retry` | pierwsze notify → 503, potem 202 |
| `ooo_events` | kolejność eventów odwrócona |
| `expired_link` | validity w przeszłości |
| `pay_no_return` | completed bez wizyty return |

### Acceptance criteria
- [ ] Każdy scenario ma co najmniej jeden test (RA i/lub PW).
- [ ] Nieudokumentowany scenario → 400.
- [ ] Scenariusze tylko gdy flaga on.

### Learning
- `HTTP:` kontrolowany chaos statusów.
- `REST:` nagłówek sterujący zachowaniem lab-only (nigdy w prod PSP).
- `PW:` mapowanie scenario → asercje UI/API.

### Połączone z
- E2-S4, E3-S2, E4-S2/S3/S4

---

## Story E6-S3 — Reconcile + anomaly  
**Task:** `CPL-T29` · P2 · FR-15

### Jako / chcę / aby
Jako ops chcę wykryć drift local vs stub.

### Acceptance criteria
- [ ] Job porównuje session status vs ostatni znany stub state / fulfillment.
- [ ] Tworzy rekord anomaly przy mismatch.
- [ ] API/UI lista anomalies.
- [ ] **Bez** Kafki.

### Learning
- `SQL:` lekki job + tabela anomaly.
- `REST:` reconcile to osobny concern od notify hot path.
- Big-tech: defense in depth gdy event zgubiony.

### Połączone z
- E3 refetch mindset
- NFR structured logs
