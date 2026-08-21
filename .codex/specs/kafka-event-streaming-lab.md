---
name: kafka-event-streaming-lab
origin: POST_KIRO_WORK
status: PROPOSED_NOT_APPROVED
category: enhancement
last_updated: 2026-08-21
---

# Spec — Event Streaming Lab (Modulith outbox → Kafka)

Źródła: research [.codex/research/kafka-event-streaming-proposal.md](../research/kafka-event-streaming-proposal.md) · review task force [.codex/research/kafka-event-streaming-proposal-review.md](../research/kafka-event-streaming-proposal-review.md) · ADR [0002](../adr/0002-kafka-event-streaming.md) (PROPOSED) · roadmap wykonawczy [status/roadmaps/kafka-event-streaming-lab/](../../status/roadmaps/kafka-event-streaming-lab/) · katalog testów [docs/testing/event-streaming-lab/](../../docs/testing/event-streaming-lab/).

## Intent

Zewnętrznić istniejące `AuditableActionOccurred` do jednego wersjonowanego tematu Kafka przez transakcyjny outbox Modulith, tak aby uczyć kluczy/partycji, grup konumentów, at-least-once + idempotencji i DLQ na realnym payment labie — bez ruszania PostgreSQL jako source of truth i bez rozbijania monolitu.

## Scope (fale)

| Wave | Epic | Zawartość |
|---|---|---|
| 0 | E0 | Governance: akceptacja ADR 0002, edycje AGENTS/checklist/skills/glossary |
| 1 | E1 | `compose.kafka.yml` overlay, `dev-stack.sh --kafka`, `KafkaContainerSupport`, Awaitility, temat v1 |
| 2 | E2 | Stabilny `eventId`, `spring-modulith-events-kafka` w `eventlab`, programistyczna externalizacja, IT rollback/publish |
| 3 | E3 | Konsument idempotentny + V37 `eventlab_processed`, retry/DLQ, inject API, BFF+Zod+`/admin/event-lab`, POM + live E2E |
| 4 | E4 | (opcjonalnie) checkout inbox over Kafka |
| 5 | E5 | Hardening: rebalance IT, retention purge, seed-guard, glossary, property test envelope |

## Must-preserve

- REST contracts, 412 vs 409, JWT/BFF, Flyway-only schema (`ddl-auto: validate`), `X-Correlation-ID`.
- Domyślny stack i Surefire bez brokera; wyłączenia `restkit/` + `paymentsupport/` wg AGENTS.md.
- Live Playwright: bez `page.route` / `routeWebSocket`; auth przez storageState; unikalne referencje zamiast sleepów.
- Granice Modulith: `eventlab` nie jest OPEN; brak importów `*.internal.*` między modułami domenowymi; `ModulithArchitectureTest` green.
- Audit pisany raz (in-process); learning/deterministic seeds nie wymagają brokera (ADR 0001).

## Acceptance criteria (pierwszy slice)

1. Flag off → istniejące suite'y zielone bez kontenera Kafka.
2. Flag on + broker → authorize publikuje dokładnie 1 rekord na `lab.auditable-actions.v1`, key=paymentOrderId, header `action=PAYMENT_AUTHORIZED`, `schemaVersion=v1`.
3. Replay tego samego `eventId` → zero nowych wierszy `audit_event` i zero drugich wierszy `eventlab_processed`.
4. Poison payload → po budgetcie retry trafia do `lab.event-lab.dlq.v1`; stan płatności w Postgres bez zmian.
5. Merchant-scoped JWT nie widzy cudzych rekordów Event Lab (maskowane 404/pusta lista); inject 403 bez uprawnienia.
6. Live Playwright: authorize przez UI/API → rekord widoczny w `/admin/event-lab` ≤ 5 s (unikalna referencja).
7. `ModulithArchitectureTest` + modułowe testy green; brak publikacji przy rollbacku TX.

## Open questions (przeniesione z propozycji §9 + review)

Dokładny tag `apache/kafka` vs Testcontainers 2.0.5 — pin na T01. Jackson 3 (`tools.jackson`) vs serializator Boot 4 — potwierdzić przy mapperze. Podział tematów — decyzja P4. `--kafka` jako trzeci tryb dev-stack (rekomendacja: tak, obok `--app`/`--full`).

## Comments

- 2026-08-21: spec utworzony z review task force; implementacja dopiero po akceptacji P0 przez użytkownika.
- 2026-08-21 (iteracja 2): katalog 15 klasycznych business cases + decyzje D-1…D-6 w [kafka-payment-business-cases-v2.md](../research/kafka-payment-business-cases-v2.md); przyjęte do zakresu: V37 `eventlab_processed.id DEFAULT uuidv7()` (PG18), runtime Podman Compose rootless (single-node KRaft, PLAINTEXT, auto-create OFF, temat tworzony przez `dev-stack.sh --kafka`), flagship AC = proof-of-delivery per paymentOrderId ≤ 5 s; PG18 OAuth-at-Postgres jako osobny follow-up (poza falami Kafki).
