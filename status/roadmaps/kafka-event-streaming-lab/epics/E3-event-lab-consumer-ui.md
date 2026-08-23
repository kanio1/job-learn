---
name: epic-e3-event-lab-consumer-ui
parent: kafka-event-streaming-lab
epic: E3
tasks: [KAFKA-T10, KAFKA-T11, KAFKA-T12, KAFKA-T13, KAFKA-T14, KAFKA-T15, KAFKA-T16, KAFKA-T20]
last_updated: 2026-08-23
---

# Epic E3 — Idempotentny konsument + cienki UI + karta dostarczenia

**Cel produktowy:** „czy capture dotarł?” — 1 wiersz processed; poison w DLT.
**Cel dydaktyczny:** grupy, unique constraint, retry/DLT, inject; BFF bez brokera w przeglądarce.

**Nie budujemy:** konsoli Kafki, lag charts, LIVE/PAUSED, ECharts, generatora load.

Gate: E2 DONE. Skills: `eventlab-kafka`, `nuxt-frontend`, `tdd`, `playwright-pom`.

## Story E3-S1 — Tabela processed + konsument

**Task:** `KAFKA-T10`, `KAFKA-T11` · P0

AC:
1. V37+ `eventlab_processed`: unique `(consumer_group, event_id)`; `id UUID DEFAULT uuidv7()`; status/attempts/consumed_at/last_error; indeksy `(target_id)`, `(consumed_at)`; kolumny telescope `topic, partition_no, record_offset, record_key` (nie renderowane jako główny UI).
2. Konsument `eventlab-inspector` (`auto.offset.reset=earliest`): PROCESSED raz; duplicate eventId ⇒ 0 nowych rows; replay ⇒ 1 row/grupa.
3. `RA-KAFKA-020…023`. Flagship: proof-of-delivery per paymentOrderId ≤ 5 s.
4. `audit_event` nadal 1 po retry.

## Story E3-S2 — Retry / DLT / retention

**Task:** `KAFKA-T12` · P1

AC:
1. `@RetryableTopic` tylko eventlab → DLT `lab.event-lab.dlq.v1`; status DEAD + last_error.
2. UI/docs: **dead-letter topic**, nie „DLQ” jako etykieta.
3. `RA-KAFKA-024…026`.
4. Lenses: `kafka-dlq-review` na tym temacie (eksploracja, nie CI).

## Story E3-S3 — Authority + inject

**Task:** `KAFKA-T13` · P0

AC:
1. `platform:event-lab:read` + `platform:event-lab:operate` (Authorities, converter, realm, katalog ról).
2. `POST /api/event-lab/inject/duplicate|poison` (delay opcjonalny): 201 operate / 403 bez operate / problem+json; odczyt cudzego tenanta = maskowany 404.
3. Confirm copy: duplicate „nie utworzy drugiego efektu”; poison „płatność bez zmian, rekord w DLT”.
4. `RA-KAFKA-030…033`; `PW-KAFKA-SEC-001…003` (brak tokena/adresu brokera w network logu).

## Story E3-S4 — BFF + Zod

**Task:** `KAFKA-T14` · P0

AC:
1. `server/api/event-lab/**` GET list/detail + POST inject; whitelist query.
2. Zod koperty read modelu (bez wymogu pokazywania raw Kafka payload w tabeli).
3. Vitest; `PW-KAFKA-API-001…003`.

## Story E3-S5 — Cienka strona + karta payment order

**Task:** `KAFKA-T15`, `KAFKA-T16` · P0

AC:
1. Flaga `NUXT_PUBLIC_EVENT_LAB_ENABLED`; nav sibling Error Lab; CSR-only.
2. `/admin/event-lab`: search `paymentOrderId`/`eventId`; `UTable` (time, action, target, status, group); `UAlert` DLT; inject + ConfirmModal; **6 stanów**.
3. **Karta** na istniejącym detalu payment order: Downstream pending/processed/dead (+ czas).
4. **Zakaz:** heatmapa, partition chip jako główny widget, LIVE follow, fake KPI.
5. `PW-KAFKA-E2E-001…006` (authorize → visible; duplicate → 1; poison → DEAD+banner; forbidden; empty; deep-link).
6. POM: `EventLabPage` + delivery assertions na payment-order page.

## Story E3-S6 — Runbook 45 min

**Task:** `KAFKA-T20` · P1 (docs)

AC:
1. `docs/setup/` linkuje [03-lesson-runbook.md](../03-lesson-runbook.md).
2. Krok Lenses SQL + lab≠prod przy topic-audit jest w runbooku.
