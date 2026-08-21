---
name: kafka-event-streaming-lab-context
parent: kafka-event-streaming-lab
last_updated: 2026-08-21
---

# 00 — Context & Requirements

## Cel biznesowy

Operatorzy i systemy merchantowskie powinni widzieć **te same fakty lifecycle** co dashboard — asynchronicznie, replayowalnie i niezależnie od pamięci jednej instancji Springa. Realny use-case wspierający (flagship): **"Czy ten capture dotarł do downstream?"** — dowód dostarczenia (proof-of-delivery) per paymentOrderId/correlationId z timestampem i grupą konumenta.

Nie rozwiązując tego: nie uczymy consumer groups, poison pills ani replayu; feed ops umiera na drugiej instancji; checkout notify zostaje poller-only.

## Actors

| Aktor | Cel | Uprawnienia |
|---|---|---|
| Platform operator | Dowieść dostarczenia zdarzenia; inspect DLQ; inject chaosu | `platform:event-lab:operate` (nowe, addytywne do PLATFORM_ADMIN/PLATFORM_OPERATOR) |
| Merchant manager | (później) podpisane webhooks statusów | Tylko własne tenant/merchant rekordy; inject 403 |
| Support agent | Inbox działa jak dziś; zero duplikatów notyfikacji | Bez dostępu do Event Lab operate |
| SDET / learner | Drive authorize przez REST/UI, asercja rekordu + idempotencji | Wszystkie pięć ról kompozytowych bez zmian |

Keycloak: żadnych principalów Kafki w realmie; broker ≠ OIDC.

## Business workflow (pierwszy slice)

- **Trigger:** udane `POST .../authorize` (lub capture/cancel/refund) na merchant-scoped payment order.
- **Main path:** row update + `PAYMENT_*` event → outbox (`event_publication`) → in-process audit/ops (bez zmian) → externalizer → `lab.auditable-actions.v1` key=paymentOrderId → konsument eventlab zapisuje processed (unique eventId) → widoczne w `/admin/event-lab` ≤ 5 s.
- **Alternate:** externalizer down → publication incomplete → republish-on-restart leczy (crash-heal IT).
- **Failure:** poison payload → retry backoff → DLT; business row w Postgres bez zmian.
- **Non-goals slice'u:** CRUD subskrypccji merchant, SLA timery, Streams windows, wymiana WS.

## Business rules

1. Source of truth = PostgreSQL command side; Kafka = projekcja/integracja.
2. At-least-once wszędzie; "exactly-once" = efekt per-(group,eventId) przez unique constraint.
3. Ordering tylko per partition key (paymentOrderId/merchantId/caseId/userId); globalny brak gwarancji.
4. Izolacja tenantów: filtr w konsumentach + JWT na HTTP/WS; broker PLAINTEXT bez ACL (uczciwie udokumentowane).
5. Confidentiality jak kontrakt audytu: zero PAN/tokenów/raw Authorization w payload i nagłówkach.
6. Czas należy do schedulerów (`@Scheduled`); Kafka retry ≠ zegar SLA.
7. Seeds (deterministic + learning) działają bez brokera i niczego nie publikują.

## FR / NFR

| ID | Wymaganie |
|---|---|
| FR-KAFKA-01 | Stabilny `eventId` UUID na każdym `AuditableActionOccurred`; reuse w ops frames |
| FR-KAFKA-02 | Externalizacja wyłącznie przy fladze `app.event-lab.enabled=true` + profil `kafka` |
| FR-KAFKA-03 | Temat `lab.auditable-actions.v1`: 3 partycje, RF1, key=`targetId`, nagłówki v1 |
| FR-KAFKA-04 | Konsument labowy: dedup `(consumer_group, event_id)`, status PROCESSED/RETRYING/DEAD |
| FR-KAFKA-05 | Inject API: duplicate / poison / delay (wzorcowane na ops inject), guarded authority |
| FR-KAFKA-06 | `/admin/event-lab`: lista rekordów, filtr group/action/outcome, drawer detali, tabela DLQ |
| FR-KAFKA-07 | Retention purge completed publications + processed (domyślnie 7 dni, konfigurowalne) |
| NFR-01 | Rekord w UI ≤ 5 s od komendy (lokalny stack) |
| NFR-02 | Surefire/domyślny stack: zero kontaktu z brokerem |
| NFR-03 | `ModulithArchitectureTest` green; eventlab nie OPEN; brak importów internal cross-module |

## Non-goals (carried + tightened)

PSP/PAN/PCI · microservice split · produkcyjne OIDC · fake KPI/Grafana na Overview · Kafka jako command bus · wymiana Ops WS w slice 1 · Schema Registry/Kafka Streams/SCRAM w P1–P2 · Namastack/JobRunr (Modulith 2.1) · seed flood przez domain services (ADR 0001) · produktowa zakładka Merchant→Webhooks (dopóki osobny spec nie wycofa non-goal).

## Słownik (do .codex/CONTEXT.md w E0)

Application event · Outbox (`event_publication`) · Externalization · Topic v1 (koperta JSON + schemaVersion) · eventId (stabilny UUID) · Consumer group · Replay · Poison pill · DLQ · Offset reset.
