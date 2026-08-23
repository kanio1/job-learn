---
name: kafka-event-streaming-lab
origin: POST_KIRO_WORK
status: ACCEPTED_NOT_IMPLEMENTED
category: enhancement
last_updated: 2026-08-23
---

# Spec — Event Streaming Lab (Modulith outbox → Kafka)

Źródła: ADR [0002 ACCEPTED](../adr/0002-kafka-event-streaming.md) · roadmap [status/roadmaps/kafka-event-streaming-lab/](../../status/roadmaps/kafka-event-streaming-lab/) · skill `eventlab-kafka` · lab≠prod [lenses-lab-vs-prod.md](../../.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md).

Research historyczny: [proposal](../research/kafka-event-streaming-proposal.md), [review](../research/kafka-event-streaming-proposal-review.md), [cases v2](../research/kafka-payment-business-cases-v2.md) (czytanka, nie backlog).

## Intent

Zewnętrznić `AuditableActionOccurred` do jednego tematu przez outbox Modulith, żeby uczyć kluczy, grup, at-least-once i DLT na realnych płatnościach — z Lenses jako lunetą, bez klona UI Kafki.

## Scope (fale)

| Wave | Epic | Zawartość |
|---|---|---|
| 0 | E0 | **DONE** — ADR, AGENTS, skills |
| 1 | E1 | Overlay, `--kafka`, KafkaIT support, Lenses `payment-lab` |
| 2 | E2 | `eventId`, `eventlab` externalizer, rollback/crash-heal |
| 3 | E3 | Konsument + DLT + inject + cienki UI + karta payment + runbook 45 min |
| 4 | E4 | Opcjonalnie checkout inbox over Kafka |
| 5 | E5 | Rebalance IT, seed-guard, property koperty |

Brak E6 (observability dashboard).

## Must-preserve

- REST contracts, JWT/BFF, Flyway validate, correlation id.
- Surefire i `--app` bez brokera.
- Playwright bez `page.route` / `routeWebSocket`.
- `eventlab` nie OPEN; audit in-process; ops WS bez Kafki.
- Seeds bez brokera (ADR 0001).
- Host 9092 = lab overlay only.

## Acceptance criteria (pierwszy produktowy slice = E2+E3)

1. Flag off → istniejące suite'y bez kontenera Kafka.
2. Flag on → authorize = 1 rekord na `lab.auditable-actions.v1`, key=paymentOrderId, header `action=PAYMENT_AUTHORIZED`, `schemaVersion=v1`.
3. Replay/duplicate `eventId` → 1 `audit_event`, 1 `eventlab_processed`.
4. Poison → DLT; stan płatności bez zmian.
5. Merchant JWT nie widzi cudzych rekordów; inject 403 bez `operate`.
6. Playwright: authorize → karta/strona Event Lab ≤ 5 s (unikalna referencja).
7. Lenses SQL (środowisko `payment-lab`) pokazuje ten sam klucz — docs, nie CI.
8. `ModulithArchitectureTest` green; rollback TX ⇒ 0 rekordów.

## Open questions (nie blokują E1)

- Pin tagu `apache/kafka` vs Testcontainers 2.0.5 — na T02.
- Jackson 3 vs serializer Boot 4 — na T08.
- Dokładny attach Lenses agenta do sieci overlay (T19).

## Comments

- 2026-08-21: spec z review; P0 required.
- 2026-08-21: iteracja 2 cases/PG18/Podman.
- 2026-08-23: user zaakceptował ADR + cięcie UI/E6; Lenses luneta; lab≠prod RF=1.
