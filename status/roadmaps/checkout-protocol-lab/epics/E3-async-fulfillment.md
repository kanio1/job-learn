---
name: epic-e3-async-fulfillment
parent: checkout-protocol-lab
epic: E3
tasks: [CPL-T11, CPL-T12, CPL-T13]
last_updated: 2026-08-09
---

# Epic E3 — Async Fulfillment (worker · refetch · state machine)

**Cel produktowy:** fulfillment Bookero-like dopiero po przetworzeniu eventów.  
**Cel dydaktyczny:** async bez Kafki; out-of-order → **re-fetch**, nie ślepy delta apply.

**Połączenia:** inbox z E2; `GET session` z E1-S4; UI thank-you z E4 czyta fulfillment.

---

## Story E3-S1 — Inbox worker  
**Task:** `CPL-T11` · P0 · FR-07 (część)

### Jako / chcę / aby
Jako system chcę asynchroniczne przetwarzanie eventów w-process.

### Acceptance criteria
- [ ] `@Scheduled` (lub poller) gdy `app.checkout-lab.enabled=true`.
- [ ] Wybór wierszy `RECEIVED` → `PROCESSING` → `DONE`/`FAILED`.
- [ ] Współbieżność: `SELECT … FOR UPDATE SKIP LOCKED` (Postgres).
- [ ] ACK (202) już zwrócony **przed** ciężką pracą (udowodnione testem timing/order).

### Szkic SQL pollera

```sql
SELECT id FROM checkout_event
 WHERE process_status = 'RECEIVED'
 ORDER BY received_at
 LIMIT 10
 FOR UPDATE SKIP LOCKED;
```

### Learning
- `SQL:` SKIP LOCKED = multi-worker bez brokera.
- `HTTP:` oddzielenie ACK od side-effect (Stripe/Airbnb mindset).
- `MOD:` worker w module `checkoutlab.internal`, nie w payment.

### Połączone z
- tabela `checkout_event`
- NFR-01 (ACK szybki)

---

## Story E3-S2 — Refetch before fulfill  
**Task:** `CPL-T12` · P0

### Jako / chcę / aby
Jako processor chcę aktualny status session przed zmianą fulfillment.

### Acceptance criteria
- [ ] Przed transition: `GET`/repository load **aktualnego** `checkout_session`.
- [ ] Out-of-order: np. `canceled` po `completed` według reguł SM — nie psują stanu (test OOO).
- [ ] Nie stosuj ślepo pól z eventu, jeśli session mówi inaczej (event = sygnał, session = truth po reconcile lokalnym).

### Learning
- `REST:` retrieve jako oracle przed mutacją.
- `HTTP:` event może być spóźniony; status zasobu wygrywa.
- `SQL:` wersjonowanie / `version` optimistic lock na session (opcjonalnie).

### Połączone z
- E1-S4
- E6-S2 scenario `ooo_events`

---

## Story E3-S3 — Fulfillment state machine  
**Task:** `CPL-T13` · P0 · FR-07

### Jako / chcę / aby
Jako produkt Bookero-like chcę stany rezerwacji/płatności.

### Stany

```text
AWAITING_PAYMENT ──► CONFIRMED   (session COMPLETED + event ok)
                 ├──► CANCELLED  (session CANCELED)
                 └──► EXPIRED    (validity / scenario)
```

### Acceptance criteria
- [ ] `CONFIRMED` **tylko** gdy session `COMPLETED` i event zweryfikowany.
- [ ] Wizyta `continueUrl` **nie** zmienia SM.
- [ ] Przejścia udokumentowane w kodzie + teście tabelarycznym.
- [ ] `source_event_id` ustawione przy CONFIRMED.

### Learning
- `REST:` oddziel session status (PSP) od fulfillment (merchant business).
- `SQL:` UNIQUE(session_id) na fulfillment — jeden rekord biznesowy.
- `PW:` UI czyta fulfillment, nie hint z return URL.

### Połączone z
- E4-S2 lie return
- E4-S4 pay_no_return
- E5 booking cash path (cash → CONFIRMED bez PSP)
