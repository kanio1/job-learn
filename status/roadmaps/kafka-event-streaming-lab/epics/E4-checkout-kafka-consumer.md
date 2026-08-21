---
name: epic-e4-checkout-kafka-consumer
parent: kafka-event-streaming-lab
epic: E4
tasks: [KAFKA-T17]
last_updated: 2026-08-21
---

# Epic E4 — Checkout inbox over Kafka (opcjonalny drugi konsument)

**Cel produktowy:** checkout lab dostaje realną integrację asynchroniczną: podpisany notify HTTP pozostaje wejściem, a przetwarzanie do inboxu może jechać przez broker — jak PSP callback → internal pipeline w prawdziwej platformie.
**Cel dydaktyczny:** external integration pattern; dedup na naturalnym kluczu `checkout_event.event_id`; flagowany wybór worker-vs-consumer bez utraty HMAC.

Gate: E3 DONE · Status: OPCJONALNA (można pominąć bez szkody dla curriculum).

## Story E4-S1 — Produce po HMAC accept

**Task:** `KAFKA-T17` · P2

Jako checkout lab publikuję zdarzenie notify do tematu labowego po akceptacji HMAC, aby worker mógł je skonsumować asynchronicznie.

AC:
1. Wejście HTTP + HMAC bez zmian (`CheckoutLabNotifier`/`CheckoutLabSignatureService` nietknięte kontraktowo); produce po commit, key=`sessionId`.
2. Konsument vs `@Scheduled` wybierany flagą; dedup przez istniejący unique `event_id` → process_status DUPLICATE (bez drugiego rzędu).
3. `RA-KAFKA-040`: notify → rekord w inboxie ≤ budget; `RA-KAFKA-041`: duplicate notify ⇒ DUPLICATE, zero nowych rows; `RA-KAFKA-042`: broker down ⇒ ścieżka @Scheduled dalej działa (flag off = dziś).
4. Istniejące CPL testy zielone bez brokera (domyślnie flag off).

Non-goals: produktowe Merchant Webhooks (osobny spec gdy user wycofa non-goal), request/reply topics.
