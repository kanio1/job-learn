---
name: kafka-event-streaming-lab-infra
parent: kafka-event-streaming-lab
last_updated: 2026-08-23
---

# 01 — Infra: broker overlay, Testcontainers, Flyway, cienki UI

Placement: `.agents/skills/eventlab-kafka`.

## Compose overlay

`infra/compose/compose.kafka.yml` — **tylko broker** (żadnego AKHQ/kafka-ui):

- `apache/kafka` 4.x KRaft combined; tag pin vs Testcontainers 2.0.5; bez ZooKeeper.
- PLAINTEXT; dual listeners: host `localhost:9092`, sieć `payment-quality-kafka:19092`.
- Auto-create OFF. Temat tworzy `dev-stack.sh --kafka`.
- **9092 jest kanoniczne dla labu.** Jeśli Lenses CE demo publikuje 9092 — wyłączyć mapping hosta CE (patrz [02-lenses-telescope.md](./02-lenses-telescope.md)).

Start: `docker compose --env-file infra/compose/.env -f infra/compose/compose.yml -f infra/compose/compose.kafka.yml up -d`.

## dev-stack.sh — trzeci tryb

`--kafka` = host-hybrid + overlay + `app.event-lab.enabled=true` + profil `kafka`. **Nie** łączyć z `--app`/`--full`.

## Backend

| Element | Decyzja |
|---|---|
| Moduł | `lab.paymentquality.eventlab` — NIE OPEN |
| Deps | `spring-modulith-events-kafka` z BOM 2.0.6 |
| Externalizacja | `EventExternalizationConfiguration` w eventlab; profil+flaga |
| Konsument | group `eventlab-inspector`; `@RetryableTopic`; DLT `lab.event-lab.dlq.v1`; `auto.offset.reset=earliest` |
| Flyway | `db/migration/eventlab/`; **V37+** (potwierdź max; E11 nie może zająć tej samej wersji) |
| PK | `eventlab_processed.id UUID DEFAULT uuidv7()` |
| Unique | `(consumer_group, event_id)` |
| Read model V37 | `consumer_group, event_id, action, target_type, target_id, tenant_ref, status, attempts, consumed_at, last_error` + `topic, partition_no, record_offset, record_key` (dla Lenses-cross-ref i detal; **nie** jako kolumny tabeli UI) |
| Security | `platform:event-lab:read` + `platform:event-lab:operate` (additive do PLATFORM_ADMIN / OPERATOR) |
| Testy | `KafkaContainerSupport`; Failsafe `*KafkaIT`; Surefire excludes; Awaitility test-scope |

## Frontend (cienki)

| Element | Decyzja |
|---|---|
| Flaga | `NUXT_PUBLIC_EVENT_LAB_ENABLED` |
| Strona | `/admin/event-lab` — search + lista + 6 stanów + DLT banner + inject confirm |
| Karta | na istniejącym detalu payment order: pending / processed / dead |
| Nie | UChip partition jako główny UI, heatmapa, LIVE/PAUSED, raw payload table, ECharts |
| BFF | `server/api/event-lab/**`; Zod; `useEventLabApi` |
| POM | `EventLabPage.ts` + rozszerzenie payment-order page o delivery card |

Payload/headers ogląda się w **Lenses**, nie w Nuxt (opcjonalny zwinięty `<details>` w drawerze jest OK, nie jest AC fali 1).

## Probe TS (opcjonalny)

`tools/kafka-probe` — poza `apps/frontend`. Nie oracle CI.
