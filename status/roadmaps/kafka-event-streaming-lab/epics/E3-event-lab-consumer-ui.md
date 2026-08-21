---
name: epic-e3-event-lab-consumer-ui
parent: kafka-event-streaming-lab
epic: E3
tasks: [KAFKA-T10, KAFKA-T11, KAFKA-T12, KAFKA-T13, KAFKA-T14, KAFKA-T15, KAFKA-T16]
last_updated: 2026-08-21
---

# Epic E3 — Event Lab: konsument idempotentny + UI

**Cel produktowy:** operator odpowiada na "czy capture dotarł do downstream?" — szuka per paymentOrderId i widzi dokładnie jeden przetworzony rekord (grupa, timestamp, status); poison widać w DLQ.
**Cel dydaktyczny:** consumer groups, idempotencja przez unique constraint, retry/DLQ, inject chaosu jak w ops; pełny pion BFF→Zod→Nuxt UI na live stacku.

Gate: E2 DONE.

## Story E3-S1 — Tabela processed + konsument

**Taski:** `KAFKA-T10`, `KAFKA-T11` · P0

Jako eventlab zapisuję każde dostarczone zdarzenie raz na grupę, aby at-least-once stał się efektywnie exactly-once per grupa.

AC:
1. V37 `eventlab_processed`: unique `(consumer_group, event_id)`; kolumny wg PG-2 (status/attempts/consumed_at/last_error); indeksy `(target_id)`, `(consumed_at)`; JPA validate.
2. Konsument `eventlab-inspector` (`auto.offset.reset=earliest`): PROCESSED raz; duplicate eventId ⇒ zero nowych rows; replay po resecie offsetów ⇒ nadal 1 row/grupa.
3. `RA-KAFKA-020…023` (schema/validate, consume, duplicate, replay).
4. Audit_event dalej pisany wyłącznie in-process (liczba wierszy = 1 po retry konumenta).

## Story E3-S2 — Retry / DLQ / retention

**Task:** `KAFKA-T12` · P1

Jako learner widzę poison pill w DLQ po budgetcie retry, a stare dane same znikają.

AC:
1. `@RetryableTopic` (backoff index, tylko eventlab) → DLT `lab.event-lab.dlq.v1`; status DEAD + last_error w processed.
2. `RA-KAFKA-024`: poison → DLT ≤ budget; `RA-KAFKA-025`: business row płatności bez zmian; `RA-KAFKA-026`: purge usuwa starsze niż retention-days (default 7), nie dotyka business tables.

## Story E3-S3 — Authority + inject API

**Task:** `KAFKA-T13` · P0

Jako platform operator injectuję duplicate/poison/delay do labu, aby ćwiczyć semantykę dostarczenia bez czekania na prawdziwe awarie.

AC:
1. Authority `platform:event-lab:operate` addytywnie (Authorities + converter allowlist + realm JSON + test katalogu ról); brak zmian istniejących authority.
2. `POST /api/event-lab/inject/*` (wzór ops inject): 201 admin / 403 merchant roles / problem+json; maskowane 404 cross-tenant odczyty.
3. `RA-KAFKA-030…033`; security matrix PW-KAFKA-SEC-001…003 (w tym: brak tokena/adresu brokera w network logu przeglądarki).

## Story E3-S4 — BFF + Zod + composable

**Task:** `KAFKA-T14` · P0

Jako frontend mam walidowany read model Event Lab przez BFF, aby przeglądarka nigdy nie widziała brokera.

AC:
1. `server/api/event-lab/**` GET list/detail + POST inject proxy; whitelist query; forwarding correlation.
2. `app/schemas/event-lab.schema.ts` (koperta v1, paginacja); `useEventLabApi` jak `useAuditApi` (validate-all, detail 404→null).
3. Vitest schema/api; `PW-KAFKA-API-001…003` (BFF list po lifecycle POST; forbidden; 404).

## Story E3-S5 — Strona /admin/event-lab (Nuxt UI)

**Taski:** `KAFKA-T15`, `KAFKA-T16` · P0

Jako operator przeszukuję rekordy po paymentOrderId i otwieram detal, aby udowodnić dostarczenie downstream.

AC:
1. Flaga `NUXT_PUBLIC_EVENT_LAB_ENABLED`; nav sibling Error Lab; CSR-only.
2. Komponenty: `UTable` timeline (time, action badge, targetType+ref, tenantRef, group, status), `USlideover` detal (key/headers/payload/status/attempts), `USelect` filtry (group/action/outcome), `UButton`+ConfirmModal inject, `UAlert` DLQ banner, `UChip` partition/offset.
3. Sześć stanów: loading / empty / filtered-empty / error / forbidden / not-found-detal.
4. Zero fake KPI/lag charts; brak surowego payloadu w tabeli (dopiero drawer).
5. `PW-KAFKA-E2E-001…006`: authorize przez UI/API → rekord z unikalną referencją widoczny ≤5 s (`expect.poll`); duplicate inject ⇒ nadal 1 row; poison ⇒ DEAD + banner; forbidden state; filtr outcome; deep-link detal.
6. POM: `EventLabPage.ts` (`openRecord(ref)`, `expectRecordVisible(ref)`, `injectDuplicate(ref)`), fixtures `{ app, api }`, `BffClient.eventLab*`.
