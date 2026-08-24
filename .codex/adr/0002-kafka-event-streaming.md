# ADR 0002 — Kafka jako event externalization na outboxie Modulith (Event Streaming Lab)

Status: ACCEPTED (2026-08-23, user — plan po Lenses: luneta, nie drugi UI)
Date: 2026-08-21
Accepted: 2026-08-23
Deciders: user + task force (tech-lead, test-architect, PM/BA, Kafka, PostgreSQL, frontend)
Input: `.codex/research/kafka-event-streaming-proposal.md`, review `.codex/research/kafka-event-streaming-proposal-review.md`, Lenses-as-telescope cut (2026-08-23)

## Context

Lab ma już trwały model zdarzeń: `event_publication` (shared V6) jako transakcyjny outbox, emity `AuditableActionOccurred` w 6 modułach, in-process konsumentów (`audit_event`, ops WS/notifications). Brakuje lekcji brokera: klucze/partycje, grupy, at-least-once, retry/DLT, replay. `AGENTS.md` listował "No Kafka" jako active non-goal — **wycofane na rzecz scoped overlay**.

Lenses Community Edition + MCP są zainstalowane lokalnie jako **luneta operatorska**. Nie są produktem Event Lab i nie zastępują Testcontainers.

## Decision (accepted)

1. Kafka wchodzi jako **lab overlay** na istniejący outbox, nie jako drugie source of truth ani split mikroserwisowy.
2. **Externalizacja przez `spring-modulith-events-kafka` (Modulith 2.0.6)** skonfigurowaną programistycznie **wewnątrz nowego modułu `eventlab`** (flaga `app.event-lab.enabled`, profil `kafka`). Żadnych adnotacji `@Externalized` w module `shared`; domyślne aplikacje nigdy nie publikują.
3. Jeden temat startowy `lab.auditable-actions.v1`: 3 partycje, RF1, key=`targetId`, nagłówki `eventId/action/targetType/tenantRef/correlationId/occurredAt/schemaVersion=v1`.
4. Stabilny **`eventId` (UUID)** dodany do rekordu zdarzenia; reuse w OpsFeedBroker frames (dedup) i w tabeli idempotencji.
5. Konsumenci labowi idempotentni: V37+ `eventlab_processed`, unique `(consumer_group, event_id)`; retry nieblokujące `@RetryableTopic` tylko w eventlab; DLT `lab.event-lab.dlq.v1` (UI: dead-letter topic). **Ordering gwarantowany tylko dla poprawnego przetwarzania głównego tematu**: nieblokujący retry może zmienić kolejność zdarzeń tego samego `targetId` przy awarii (udokumentowane w RA-018, katalogu acceptance).
6. Infrastruktura opcjonalna: overlay `infra/compose/compose.kafka.yml` (`apache/kafka` 4.x KRaft, PLAINTEXT), tryb `--kafka` w dev-stack; testy = Failsafe `*KafkaIT` na `org.testcontainers:testcontainers-kafka` + Awaitility. **Bez AKHQ/kafka-ui** — Lenses jest lunetą.
7. Audit pozostaje in-process (jeden zapis); ops WS zostaje bez Kafki; webhooks produktowe dalej non-goal.
8. **UI Event Lab jest cienki:** wyszukiwarka proof-of-delivery + karta na detalu payment order + inject chaosu. Przeglądarka rekordów, lag, heatmapy = Lenses, nie Nuxt.
9. **Jedna Kafka labu na hoście 9092.** Lenses CE demo nie publikuje 9092 równolegle. Lenses HQ dostaje środowisko `payment-lab` wskazujące na overlay.

## Alternatives

- **CDC/Debezium z Postgres:** realistyczny produkcyjnie, ale infrastrukturo ciężki i uczy innego lekcjału; odrzucone na teraz.
- **`@Externalized` na rekordzie w `shared`:** wiąże OPEN moduł z brokerem; odrzucone.
- **Namastack/JobRunr outbox (Modulith 2.1):** poza pinem 2.0.6; odrzucone.
- **Kafka jako command bus / replace REST:** odrzucone.
- **Event Lab jako drugi Lenses (konsola, ECharts, LIVE/PAUSED):** odrzucone 2026-08-23 — Lenses już to robi.
- **Brak Kafki:** odrzucone przez akceptację tego ADR.

## Iteration 2 addenda (2026-08-21, business-case research)

1. Katalog 15 cases: [.codex/research/kafka-payment-business-cases-v2.md](../research/kafka-payment-business-cases-v2.md) — **czytanka, nie backlog implementacji**. Wave 1 = cases 1+2 (+3 reshaped jako DLT, nie produktowe webhooks).
2. PG18: `eventlab_processed.id UUID PRIMARY KEY DEFAULT uuidv7()`; `eventId` zostaje Java UUID v4.
3. Runtime: compose overlay, single-node KRaft, PLAINTEXT, RF1, auto-create OFF, temat tworzy `--kafka`.
4. Flagship AC: proof-of-delivery per paymentOrderId ≤ 5 s.

## Iteration 3 addenda (2026-08-23, Lenses telescope)

1. Lenses = luneta. Framing lab≠prod (RF=1 critical w `kafka-topic-audit` to prawda operatorska, nie ticket produktowy): `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md`.
2. Split RBAC: `platform:event-lab:read` + `platform:event-lab:operate`.
3. Brak epiku E6 observability / ECharts / generator load w tym roadmapie (`.codex/out-of-scope/kafka-e6-observability-dashboard.md`).
4. Lekcja 45 min: `status/roadmaps/kafka-event-streaming-lab/03-lesson-runbook.md`.
5. Termin kanoniczny: **dead-letter topic (DLT)**; „DLQ” tylko jako alias w docs.

## Consequences

- Nowy moduł `eventlab` (nie OPEN), folder migracji `db/migration/eventlab`, rezerwacja **V37+**.
- Domyślny compose/Surefire/POM bez zmian; broker tylko w `--kafka` i `*KafkaIT`.
- `AGENTS.md` / `CLAUDE.md` / skills: Kafka tylko w `eventlab` / approved overlay.
- Retention: purge completed publications i `eventlab_processed` (domyślnie 7 dni).
- Broker PLAINTEXT = brak auth na poziomie Kafki; izolacja w konsumentach i HTTP.

## Validation gate

E0 (docs) jest wykonane razem z tą akceptacją. E1 (broker overlay) może startować. Zero kodu aplikacji w E0.
