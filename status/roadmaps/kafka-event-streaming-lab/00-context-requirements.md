---
name: kafka-event-streaming-lab-context
parent: kafka-event-streaming-lab
last_updated: 2026-08-23
---

# 00 — Context & Requirements

## Cel biznesowy

Operator odpowiada na **„czy ten capture dotarł do downstream?”** — dokładnie jeden wiersz processed per `(consumer_group, event_id)`, timestamp, grupa, ≤ 5 s.

Lenses pokazuje *ten sam* rekord na topiku (SQL po kluczu). Dashboard nie dubluje przeglądarki rekordów.

## Actors

| Aktor | Cel | Uprawnienia |
|---|---|---|
| Platform operator | Dowód dostarczenia; inject chaosu; Lenses SQL | `platform:event-lab:read` + `operate` |
| Platform reader | Lista/detal Event Lab, bez inject | `platform:event-lab:read` |
| Merchant manager | Cudze rekordy maskowane; inject 403 | JWT merchant-scoped |
| Support / SDET | Authorize → asercja 1 wiersza | istniejące role + lab flags |

Keycloak: żadnych principalów Kafki w realmie.

## Business workflow (pierwszy slice)

- **Trigger:** udane authorize/capture/cancel/refund.
- **Main:** TX + event → outbox → audit/ops in-process → externalizer → `lab.auditable-actions.v1` key=paymentOrderId → `eventlab-inspector` → karta na payment order + `/admin/event-lab`.
- **Telescope:** `SELECT * FROM lab.auditable-actions.v1` w Lenses (środowisko `payment-lab`).
- **Failure:** poison → retry → DLT; wiersz płatności bez zmian.
- **Non-goals slice'u:** CRUD webhooks, SLA timery, Streams, wymiana WS, lag dashboard.

## Business rules

1. Source of truth = PostgreSQL; Kafka = projekcja.
2. At-least-once; efekt exactly-once per (group, eventId) przez unique constraint.
3. Ordering tylko per partition key.
4. Tenant: filtr konsumenta + JWT; broker PLAINTEXT.
5. Zero PAN/tokenów w payload.
6. Czas = `@Scheduled`, nie Kafka.
7. Seeds działają bez brokera.
8. **lab ≠ prod:** RF=1 / PLAINTEXT nie są ticketami produktowymi ([lenses-lab-vs-prod.md](../../../.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md)).

## FR / NFR

| ID | Wymaganie |
|---|---|
| FR-KAFKA-01 | Stabilny `eventId` UUID; reuse w ops frames |
| FR-KAFKA-02 | Externalizacja tylko `app.event-lab.enabled=true` + profil `kafka` |
| FR-KAFKA-03 | Temat `lab.auditable-actions.v1`: 3 partycje, RF1, key=`targetId`, nagłówki v1 |
| FR-KAFKA-04 | Konsument: unique `(consumer_group, event_id)`, status PROCESSED/RETRYING/DEAD |
| FR-KAFKA-05 | Inject duplicate/poison (delay opcjonalny); `operate` |
| FR-KAFKA-06 | Cienkie `/admin/event-lab`: szukaj paymentOrderId, 6 stanów, DLT banner; **bez** raw payload table |
| FR-KAFKA-07 | Karta downstream na detalu payment order (pending/processed/dead) |
| FR-KAFKA-08 | Lenses środowisko `payment-lab` na overlay; CE demo nie zajmuje host 9092 |
| FR-KAFKA-09 | Retention purge (default 7 dni) |
| FR-KAFKA-10 | Split `platform:event-lab:read` vs `operate` |
| NFR-01 | Rekord widoczny ≤ 5 s |
| NFR-02 | Surefire/domyślny stack: zero brokera |
| NFR-03 | Modulith green; eventlab nie OPEN |

## Non-goals

PSP/PAN/PCI · split · fake KPI · Kafka command bus · wymiana Ops WS · Schema Registry/Streams/SCRAM wave 1 · Namastack · seed flood · produktowe Merchant Webhooks · **E6 observability / ECharts / load generator** · **AKHQ/kafka-ui** · **konsola Kafki w Nuxt** · **Lenses MCP jako CI**.

15 industrial cases = czytanka (`.codex/research/kafka-payment-business-cases-v2.md`), nie backlog.

## Słownik

Application event · Outbox (`event_publication`) · Externalization · Topic v1 · eventId · Consumer group · Replay · Poison pill · **Dead-letter topic (DLT)** (DLQ = alias) · Offset reset · Telescope (Lenses) · lab≠prod.
