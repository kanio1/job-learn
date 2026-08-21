# ADR 0002 — Kafka jako event externalization na outboxie Modulith (Event Streaming Lab)

Status: PROPOSED (nie akceptowany — wymaga decyzji użytkownika; do czasu akceptacji "No Kafka" pozostaje non-goalem w AGENTS.md)
Date: 2026-08-21
Deciders: user + task force (tech-lead, test-architect, PM/BA, Kafka, PostgreSQL, frontend)
Input: `.codex/research/kafka-event-streaming-proposal.md`, review `.codex/research/kafka-event-streaming-proposal-review.md`

## Context

Lab ma już trwały model zdarzeń: `event_publication` (shared V6) jako transakcyjny outbox, emity `AuditableActionOccurred` w 6 modułach, in-process konsumentów (`audit_event`, ops WS/notifications). Brakuje lekcji brokera: klucze/partycje, grupy, at-least-once, retry/DLQ, replay. `AGENTS.md` listuje "No Kafka" jako active non-goal.

## Decision (proposed)

1. Kafka wchodzi jako **lab overlay** na istniejący outbox, nie jako drugie source of truth ani split mikroserwisowy.
2. **Externalizacja przez `spring-modulith-events-kafka` (Modulith 2.0.6)** skonfigurowaną programistycznie **wewnątrz nowego modułu `eventlab`** (flaga `app.event-lab.enabled`, profil `kafka`). Żadnych adnotacji `@Externalized` w module `shared`; domyślne aplikacje nigdy nie publikują.
3. Jeden temat startowy `lab.auditable-actions.v1`: 3 partycje, RF1, key=`targetId`, nagłówki `eventId/action/targetType/tenantRef/correlationId/occurredAt/schemaVersion=v1`.
4. Stabilny **`eventId` (UUID)** dodany do rekordu zdarzenia; reuse w OpsFeedBroker frames (dedup) i w tabeli idempotencji.
5. Konsumenci labowi idempotentni: V37 `eventlab_processed`, unique `(consumer_group, event_id)`; retry nieblokujące `@RetryableTopic` tylko w eventlab; DLT `lab.event-lab.dlq.v1`.
6. Infrastruktura opcjonalna: overlay `infra/compose/compose.kafka.yml` (`apache/kafka` 4.x KRaft, PLAINTEXT), tryb `--kafka` w dev-stack; testy = Failsafe `*KafkaIT` na `org.testcontainers:testcontainers-kafka` + Awaitility.
7. Audit pozostaje in-process (jeden zapis); ops WS zostaje bez Kafki w pierwszej fali; webhooks produktowe dalej non-goal.

## Alternatives

- **CDC/Debezium z Postgres:** realistyczny produkcyjnie, ale infrastrukturo ciężki i uczy innego lekcjału; odrzucone na teraz (research iteracji 2: Trade Republic/singhajit potwierdzają koszt dodatkowego slotu replikacji i ruchu WAL).
- **`@Externalized` na rekordzie w `shared`:** prostsze, ale wiąże OPEN moduł z brokerem i ryzykuje publikację domyślną; odrzucone.
- **Namastack/JobRunr outbox (Modulith 2.1):** wersja poza pinem repo; odrzucone.
- **Kafka jako command bus / replace REST:** odrzucone — REST+JWT+ETag pozostaje interfejsem komend.
- **Brak Kafki (status quo):** legalna opcja — wtedy ten ADR zostaje PROPOSED/odrzucony, a research jest materiałem dydaktycznym.

## Iteration 2 addenda (2026-08-21, business-case research — Firecrawl)

1. **Katalog klasycznych cases:** 15 kanonicznych przypadków przemysłowych zmapowanych na lab; verdicty Core/Later/Reshape/Defer/Reject w [kafka-payment-business-cases-v2.md](../research/kafka-payment-business-cases-v2.md). Fala 1 bez zmian: lifecycle backbone + proof-of-delivery (+ reshaped webhook dispatch).
2. **PostgreSQL 18 synergies przyjęte do V37:** `eventlab_processed.id UUID PRIMARY KEY DEFAULT uuidv7()` (D-1); VIRTUAL generated column `event_day` tylko do prezentacji UI (bez indeksu); skip-scan jako bonus planera, nie obietnica projektowa; PG18 OAuth-at-Postgres (Keycloak validator) zapisany jako osobny follow-up lab, poza falami Kafki (D-6).
3. **Topologia runtime: Podman Compose rootless, single-node KRaft combined, PLAINTEXT, RF1 + 3 partycje** (D-2/D-3): dokumentacja prowadzi przez `podman compose -f infra/compose/compose.yml -f infra/compose/compose.kafka.yml --env-file infra/compose/.env up -d`; porty >1024 (`9092` host / `19092` sieć compose); auto-create OFF — temat tworzy jawnie tryb `--kafka` w `dev-stack.sh`; zero artefaktów Kubernetes; brak rozproszonego klastra (multi-broker to osobne ćwiczenie później).
4. **Flagship acceptance path:** proof-of-delivery per paymentOrderId/correlationId (dokładnie jeden wiersz processed, timestamp, grupa konumenta, ≤5 s) — podnosi priorytet UC-KAFKA-03 z review (PM-1) do rangi głównego AC pierwszego slice'a (D-4).

## Consequences

- Nowy moduł `eventlab` (nie OPEN), folder migracji `db/migration/eventlab`, rezerwacja wersji **V37+**.
- Domyślny compose/Surefire/POM bez zmian; broker tylko w trybie `--kafka` i IT.
- Retention: zaplanowane czyszczenie completed publications i `eventlab_processed` (domyślnie 7 dni).
- Broker PLAINTEXT = brak auth na poziomie Kafki; izolacja tenantów w konsumentach i HTTP (JWT); SCRAM jako późniejszy profil.
- Edycje dokumentów governance (AGENTS.md, review-checklist, skills, CONTEXT.md glossary) są częścią E0 i wykonują się dopiero po akceptacji.

## Validation gate

E0 nie zmienia kodu aplikacji. Pierwsza fala techniczna (E1) startuje tylko po: akceptacji tego ADR + usunięciu "No Kafka" z `AGENTS.md` przez użytkownika.
