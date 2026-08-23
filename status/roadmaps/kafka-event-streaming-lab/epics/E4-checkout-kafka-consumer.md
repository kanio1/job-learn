---
name: epic-e4-checkout-kafka-consumer
parent: kafka-event-streaming-lab
epic: E4
tasks: [KAFKA-T17]
last_updated: 2026-08-23
---

# Epic E4 — Checkout inbox over Kafka (opcjonalny)

**Cel produktowy:** podpisany notify HTTP zostaje wejściem; przetwarzanie inboxu *może* iść przez broker (wzorzec PSP callback).
**Cel dydaktyczny:** integracja zewnętrzna; dedup `checkout_event.event_id`; flaga worker vs consumer.

Gate: E3 DONE. **Można pominąć** bez szkody dla curriculum. Skill: `eventlab-kafka` (konsument w `checkoutlab` lub cienki adapter — **nie** nowy OPEN moduł).

## Story E4-S1 — Produce po HMAC accept

**Task:** `KAFKA-T17` · P2

AC:
1. HTTP + HMAC bez zmian kontraktu; produce po commit, key=`sessionId`.
2. Konsument vs `@Scheduled` flagą; duplicate → `process_status=DUPLICATE`.
3. `RA-KAFKA-040…042`; CPL testy zielone bez brokera.
4. Non-goals: produktowe Merchant Webhooks.

Nie wymaga nowego UI Kafki — Checkout Lab już ma inbox.
