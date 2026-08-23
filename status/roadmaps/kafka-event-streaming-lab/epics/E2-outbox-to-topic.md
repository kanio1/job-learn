---
name: epic-e2-outbox-to-topic
parent: kafka-event-streaming-lab
epic: E2
tasks: [KAFKA-T06, KAFKA-T07, KAFKA-T08, KAFKA-T09]
last_updated: 2026-08-23
---

# Epic E2 — Outbox → topic (serce lekcji)

**Cel produktowy:** lifecycle paymentu jest na `lab.auditable-actions.v1`.
**Cel dydaktyczny:** after-commit externalization; key=`targetId`; rollback ⇒ 0 rekordów; crash-heal.

Gate: E1 DONE. Skill: `eventlab-kafka` + `spring-modulith`.

## Story E2-S1 — Stabilny eventId

**Task:** `KAFKA-T06` · P0

AC:
1. `AuditableActionOccurred.eventId` UUID w jedynej fabryce; przeciążona fabryka kompatybilna.
2. OpsFeedBroker reuse eventId; istniejące specs ops zielone.
3. `RA-KAFKA-010`.
4. Brak migracji kolumnowych na starych tabelach.

## Story E2-S2 — Moduł eventlab + externalizacja

**Task:** `KAFKA-T07` · P0

AC:
1. Dep `spring-modulith-events-kafka`; moduł NIE OPEN.
2. Bean pod `@Profile("kafka")` + `@ConditionalOnProperty(app.event-lab.enabled=true)`.
3. `AT-KAFKA-002`; `RA-KAFKA-011` flag off ⇒ 0 rekordów.
4. Surefire nie próbuje łączyć z brokerem.

## Story E2-S3 — Koperta v1

**Task:** `KAFKA-T08` · P0

AC:
1. JSON + `schemaVersion=v1`; nagłówki: eventId, action, targetType, tenantRef, correlationId, occurredAt, schemaVersion.
2. Key = `targetId`.
3. `RA-KAFKA-012…015`.
4. Serializacja zgodna z istniejącym lazy-Jackson `EventSerializer`.

## Story E2-S4 — Crash-heal

**Task:** `KAFKA-T09` · P1

AC:
1. `RA-KAFKA-016`: incomplete publication → restart → rekord raz.

Po E2: w Lenses widać temat (E1-S5). Nuxt nadal niepotrzebny.
