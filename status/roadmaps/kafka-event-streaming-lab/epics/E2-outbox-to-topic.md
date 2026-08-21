---
name: epic-e2-outbox-to-topic
parent: kafka-event-streaming-lab
epic: E2
tasks: [KAFKA-T06, KAFKA-T07, KAFKA-T08, KAFKA-T09]
last_updated: 2026-08-21
---

# Epic E2 — Outbox → topic bridge (serce lekcji)

**Cel produktowy:** każdy lifecycle payment (i pozostałe akcje audytowalne) jest dowodnie opublikowany do wersjonowanego tematu — materiał dowodowy dla integracji downstream.
**Cel dydaktyczny:** transakcyjny outbox vs podwójny zapis; externalization after commit; key=`targetId`; rollback ⇒ brak publikacji; crash-heal przez republish-on-restart.

Gate: E1 DONE.

## Story E2-S1 — Stabilny eventId

**Task:** `KAFKA-T06` · P0

Jako integrator potrzebuję stabilnego `eventId` UUID na każdym zdarzeniu, aby deduplikować dostarczenie po stronie konsumentów.

AC:
1. `AuditableActionOccurred` + pole `eventId` (UUID) nadawane w jedynej fabryce; kompatybilna przeciążona fabryka (wzór `Merchant.create`).
2. OpsFeedBroker frames reuse eventId (koniec `UUID.randomUUID()` per frame); istniejące specs ops (`PW-OPS-E2E-121 duplicate eventId is one row`) dalej zielone.
3. `RA-KAFKA-010`: dwa zdarzenia tej samej akcji mają różne eventId; replay tego samego ma to samo.
4. Brak migracji kolumnowych (pole jedzie w serializowanym payload `event_publication`).

## Story E2-S2 — Moduł eventlab + externalizacja programistyczna

**Task:** `KAFKA-T07` · P0

Jako tech-lead mam externalizację zamkniętą w `eventlab`, aby domyślne aplikacje nigdy nie publikowały, a `shared` nie zna brokera.

AC:
1. Dep `spring-modulith-events-kafka` (BOM Modulith 2.0.6); moduł `lab.paymentquality.eventlab` NIE OPEN; package-info z ograniczonym API.
2. Bean `EventExternalizationConfiguration` (select typu, mapping→koperta v1, headers, routeKey=`targetId`) pod `@Profile("kafka")` + `@ConditionalOnProperty(app.event-lab.enabled=true)`.
3. `AT-KAFKA-002`: `ModulithArchitectureTest` green; `RA-KAFKA-011`: flag off ⇒ zero rekordów na temacie po authorize.
4. Surefire contexts nie tworzą producenta (brak connection attempt w logach testów).

## Story E2-S3 — Kontrakt koperty v1

**Task:** `KAFKA-T08` · P0

Jako konsument czytam przewidywalną kopertę v1 z nagłówkami, abym mógł routować i deduplikować bez parsowania payloadu.

AC:
1. Payload = JSON rekordu + `eventId` + `schemaVersion=v1`; nagłówki: `eventId`, `action`, `targetType`, `tenantRef`, `correlationId`, `occurredAt`, `schemaVersion`.
2. Key = `targetId` (paymentOrderId/merchantId/caseId/userId).
3. `RA-KAFKA-012`: authorize → dokładnie 1 rekord po commit; `RA-KAFKA-013`: key=paymentOrderId; `RA-KAFKA-014`: nagłówki kompletne; `RA-KAFKA-015`: rollback TX ⇒ zero rekordów.
4. Serializacja zgodna z istniejącym lazy-Jackson `EventSerializer` (lekcja Wave 2 audit — JPA slices nie mogą pękać).

## Story E2-S4 — Crash-heal (at-least-once udowodniony)

**Task:** `KAFKA-T09` · P1

Jako learner widzę, że commit bez publikacji (externalizer down) jest leczony przy restarcie, aby zrozumieć prawdziwy at-least-once.

AC:
1. `RA-KAFKA-016`: publication incomplete → restart appki (ten sam DB) → rekord publikowany dokładnie raz dodatkowo; `republish-outstanding-events-on-restart` działa jak dziś.

Nuxt UI: brak. Testy: RA-KAFKA-010…016, AT-KAFKA-002.
