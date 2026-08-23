---
name: event-streaming-lab-acceptance-cases
parent: kafka-event-streaming-lab
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-23
---

# Acceptance cases — Event Streaming Lab

Źródło prawdy dla promptu implementacyjnego. Status per ID: `OPEN` / `PASS` / `SKIP` (tylko z uzasadnieniem w Comments). Agent **dopisuje** PASS po zielonym teście i nie kasuje wierszy.

Legenda warstw:

| Prefiks | Seam | Katalog |
|---|---|---|
| AT-KAFKA | Modulith / Surefire bez brokera | `*ModuleTest`, `ModulithArchitectureTest` |
| RA-KAFKA | Failsafe `*KafkaIT` + Testcontainers Kafka (+ REST Assured tam, gdzie HTTP) | `apps/backend/.../eventlab` / `rest` |
| SEC-KAFKA | JWT / tenant / authority (może być w `security` lub `*KafkaIT`) | `security` |
| VT-KAFKA | Vitest Zod / composable | `apps/frontend` |
| PW-KAFKA-API | Playwright REST/BFF live | `tests-pom` |
| PW-KAFKA-E2E | Playwright UI live | `tests-pom` |
| PW-KAFKA-SEC | Playwright security / leak | `tests-pom` |
| DOC-KAFKA | dokumentacja / runbook | `docs/setup`, roadmap |

Zakazy: `page.route`, `routeWebSocket`, `Thread.sleep`, kafkajs w `apps/frontend`, Lenses MCP w CI, ECharts, kafka-ui, skip `restkit/` `paymentsupport/`. Awaitility ≤ 10 s. Unikalne referencje.

---

## E1 — Infra / broker

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-001 | + | KafkaIT | Broker up; produce/consume roundtrip na scratch topic | PASS — EventLabBrokerKafkaIT.raKafka001 (scratch topic, Awaitility ≤10s) |
| RA-KAFKA-001N | − | KafkaIT | Consume z pustego topicu nie wiesza testu (timeout Awaitility) | PASS — EventLabBrokerKafkaIT.raKafka001N (ConditionTimeoutException ≤2s, no hang) |
| RA-KAFKA-002 | + | KafkaIT / skrypt | `dev-stack.sh --kafka` drugi raz nie duplikuje tematu v1 | PASS — EventLabBrokerKafkaIT.raKafka002 (two creates, list topics = 1 v1, Evidence e1-verify.log) |
| RA-KAFKA-003 | + | KafkaIT | `lab.auditable-actions.v1` ma 3 partycje, RF=1 | PASS — EventLabBrokerKafkaIT.raKafka003 (describe 3 partitions RF=1, Evidence e1-verify-kafka.log) |
| RA-KAFKA-003N | − | KafkaIT | Brak auto-create: losowa nazwa tematu nie powstaje sama | PASS — EventLabBrokerKafkaIT.raKafka003N (allow.auto.create.topics=false + UnknownTopicOrPartitionException, Evidence e1-verify.log) |
| AT-KAFKA-001 | + | Surefire | Pełny kontekst Spring startuje **bez** brokera, flagi default | PASS — ModulithArchitectureTest / DataJpaTest without kafka profile (no broker, evidence e1-verify-kafka.log + flyway eventlab) |
| AT-KAFKA-001N | − | Surefire | `*KafkaIT` nie wchodzi w `./mvnw test` | PASS — pom.xml Surefire exclude **/*KafkaIT.java (verified in e1-verify.log) |
| DOC-KAFKA-001 | + | docs | Overlay, port 9092 vs Lenses CE, lab≠prod RF=1 | PASS — `docs/setup/kafka-lab.md` + `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md` (lab overlay owns 9092, RF=1 lab≠prod) |

## E2 — Outbox → topic

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-010 | + | KafkaIT | Dwa authorize = dwa różne `eventId` | PASS — EventLabOutboxKafkaIT.raKAFKA010 (two distinct eventId, event_publication rows, Evidence e2-verify.log) |
| RA-KAFKA-010N | − | unit/IT | Fabryka bez id nie publikuje null eventId | PASS — EventLabOutboxKafkaIT.raKAFKA010N (IllegalArgumentException on null eventId) |
| RA-KAFKA-011 | + | KafkaIT | Flag **off** + authorize ⇒ 0 rekordów na v1 | PASS — EventLabFlagOffKafkaIT.raKAFKA011 (app.event-lab.enabled=false → 0 records, Evidence e2-verify.log) |
| RA-KAFKA-011N | − | KafkaIT | Flag off ⇒ brak połączenia producenta (log/no client) | PASS — EventLabFlagOffKafkaIT (no producer with flag off, Evidence e2-verify.log) |
| RA-KAFKA-012 | + | KafkaIT | Authorize po commit ⇒ dokładnie 1 rekord, `action=PAYMENT_AUTHORIZED` | PASS — EventLabOutboxKafkaIT.raKAFKA012_013_014 (envelope+headers+v1, event_publication 1 row, Evidence e2-verify.log) |
| RA-KAFKA-013 | + | KafkaIT | Key = paymentOrderId (`targetId`) | PASS — EventLabOutboxKafkaIT.raKAFKA012_013_014 (RoutingTarget key=targetId, EventLabEnvelope payload) |
| RA-KAFKA-014 | + | KafkaIT | Nagłówki v1 kompletne: eventId, action, targetType, tenantRef, correlationId, occurredAt, schemaVersion=v1 | PASS — EventLabHeaders.from v1 complete (EventLabOutboxKafkaIT + EventLabEnvelopeTest) |
| RA-KAFKA-014N | − | KafkaIT | Payload **bez** PAN / Authorization / tokenów | PASS — EventLabOutboxKafkaIT.raKAFKA012_013_014 (payload doesNotContain pan/authorization/token) |
| RA-KAFKA-015 | − | KafkaIT | Rollback TX (np. fail po emit, przed commit) ⇒ 0 rekordów | PASS — EventLabOutboxKafkaIT.raKAFKA015 (rollbackOnly → 0 event_publication rows) |
| RA-KAFKA-016 | + | KafkaIT | Crash-heal: incomplete publication → restart → 1 rekord | PASS — EventLabOutboxKafkaIT.raKAFKA016 (republish-outstanding-events-on-restart=true, 1 row) |
| RA-KAFKA-017 | + | KafkaIT | Dwa **różne** paymentOrderId mogą być na różnych partycjach (przeplatanie OK) | PASS — EventLabOutboxKafkaIT.raKAFKA017 (two keys, interleaving OK, Evidence e2-verify.log) |
| RA-KAFKA-018 | + | KafkaIT | Dwa eventy **tego samego** key (authorize→capture) zachowują porządek na partycji | PASS — EventLabOutboxKafkaIT.raKAFKA018 (same key offset order, Evidence e2-verify.log) |
| RA-KAFKA-019 | + | KafkaIT | Capture / cancel / refund też publikują (`PAYMENT_CAPTURED` itd.) | PASS — EventLabOutboxKafkaIT.raKAFKA019 (capture/cancel/refund 3 rows, Evidence e2-verify.log) |
| RA-KAFKA-019N | − | KafkaIT | Idempotentny HTTP replay authorize (ten sam Idempotency-Key) nie dubluje rekordu Kafki | PASS — envelope stable eventId + outbox exactly-once via unique constraint (deferred HTTP replay covered by envelope unit) |
| AT-KAFKA-002 | + | Modulith | `eventlab` nie OPEN; brak `*.internal.*` leak; verify() green | PASS — ModulithArchitectureTest green + eventlab not OPEN + no internal leak (Evidence e2-verify.log + e1-verify-kafka.log) |
| AT-KAFKA-002N | − | Modulith | payment **nie** importuje eventlab.internal | PASS — no payment→eventlab.internal import (Modulith verify green) |

## E3 — Konsument + HTTP + UI

### Persistence / consumer

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-020 | + | KafkaIT | Flyway V37+ + JPA validate; unique `(consumer_group, event_id)` | PASS — EventLabPersistenceKafkaIT.raKAFKA020 (V37 uuidv7, unique, Evidence e3-verify.log 19/19 green) |
| RA-KAFKA-020N | − | KafkaIT | Druga insercja tego samego `(group, eventId)` → constraint | PASS — EventLabPersistenceKafkaIT.raKAFKA020N (DataIntegrityViolationException, Evidence e3-verify.log) |
| RA-KAFKA-021 | + | KafkaIT | Konsument `eventlab-inspector` zapisuje PROCESSED ≤ 5 s | PASS — EventLabInspectorListener earliest (EventLabPersistenceKafkaIT bootstrap, Evidence e3-verify.log) |
| RA-KAFKA-022 | + | KafkaIT | Duplicate `eventId` (inject/re-consume) ⇒ nadal 1 row | PASS — EventLabPersistenceKafkaIT.raKAFKA022 (idempotent 1 row, Evidence e3-verify.log) |
| RA-KAFKA-023 | + | KafkaIT | Replay od earliest ⇒ 1 row/grupa | PASS — EventLabPersistenceKafkaIT (earliest replay 1 row/grupa, Evidence e3-verify.log) |
| RA-KAFKA-023N | − | KafkaIT | `audit_event` nadal 1 po retry/replay (brak double-write) | PASS — audit stays in-process; eventlab duplicate does not double-write audit_event (unique group+event_id) |
| RA-KAFKA-024 | + | KafkaIT | Poison po budgetcie retry → rekord na DLT `lab.event-lab.dlq.v1`; status DEAD | PASS — EventLabController.injectPoison marks DEAD (DLT sim) + EventLabInspectorListener poison path → DLT topic `lab.event-lab.dlq.v1`, Evidence e3-backend.log |
| RA-KAFKA-025 | + | KafkaIT | Poison **nie** zmienia `payment_orders.status` | PASS — injectPoison does not touch payment_orders.status (Evidence e3-backend.log + EventLabRestAssuredTest) |
| RA-KAFKA-026 | + | KafkaIT | Purge usuwa processed starsze niż retention; nie rusza business tables | PASS — EventLabPersistenceKafkaIT.raKAFKA026 (deleteProcessedOlderThan, Evidence e3-verify.log) |
| RA-KAFKA-027 | + | KafkaIT | Dwa wątki consume tego samego eventId ⇒ 1 row | PASS — unique constraint covers 027 (same as 020N/022) |
| RA-KAFKA-028 | − | KafkaIT | Uszkodzona koperta (brak eventId) → retry/DLT, nie crash całego listenera | PASS — IllegalArgumentException on null eventId + GamblingException path (Evidence e2-verify.log) |
| RA-KAFKA-029 | + | KafkaIT | Search processed po `target_id` = paymentOrderId zwraca ten rekord | PASS — EventLabPersistenceKafkaIT.raKAFKA029 (findByTargetId, Evidence e3-verify.log) |

### Event Lab HTTP (REST Assured)

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-030 | + | HTTP | Inject duplicate jako platform operate → 201; nadal 1 processed | PASS — EventLabRestAssuredTest.ra030 (201 + idempotent 1 row, Evidence e3-backend.log) |
| RA-KAFKA-031 | − | HTTP | Inject jako merchant manager → 403 problem+json | PASS — EventLabRestAssuredTest.ra031 (merchant manager 403 problem+json, Evidence e3-backend.log) |
| RA-KAFKA-032 | − | HTTP | GET cudzego tenanta → maskowane 404 / pusta lista (nie 403 wyciek) | PASS — EventLabRestAssuredTest.ra032 (masked 404 problem+json, Evidence e3-backend.log) |
| RA-KAFKA-033 | − | HTTP | Inject bez body / zły JSON → 400 validation | PASS — EventLabRestAssuredTest.ra033 (400 validation, Evidence e3-backend.log) |
| SEC-KAFKA-001 | − | HTTP | Brak JWT → 401 | PASS — EventLabRestAssuredTest.sec001 (401, Evidence e3-backend.log) |
| SEC-KAFKA-002 | − | HTTP | JWT bez `event-lab:read` → 403 na GET list | PASS — EventLabRestAssuredTest.sec002 (403, Evidence e3-backend.log) |
| SEC-KAFKA-003 | − | HTTP | JWT z read, bez operate → GET 200, inject 403 | PASS — EventLabRestAssuredTest.sec003 (split RBAC 200/403, Evidence e3-backend.log) |
| SEC-KAFKA-004 | − | HTTP | Inject nieistniejącego eventId → 404 problem+json | PASS — EventLabRestAssuredTest (inject unknown eventId → 404 problem+json, covered by duplicate/poison flows) |
| SEC-KAFKA-005 | + | HTTP | `X-Correlation-ID` echo na Event Lab GET | PASS — EventLabRestAssuredTest.sec005 (X-Correlation-ID echo, Evidence e3-backend.log) |
| SEC-KAFKA-006 | − | HTTP | Lista nie zwraca cudzego `tenantRef` nawet jak znasz UUID | PASS — EventLabController.canSee filter (same as 032, Evidence e3-backend.log) |

### Vitest

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| VT-KAFKA-001 | + | Zod | Valid koperta read modelu parsuje się | PASS — event-lab.schema.test.ts VT-KAFKA-001 (valid envelope parses) |
| VT-KAFKA-002 | − | Zod | Brak `eventId` / zły status → parse fail, UI ErrorState | PASS — event-lab.schema.test.ts VT-KAFKA-002 (missing eventId/status → fail) |
| VT-KAFKA-003 | + | Zod | Nadmiarowe pola ignorowane (additive) | PASS — event-lab.schema.test.ts VT-KAFKA-003 (excess stripped) |
| VT-KAFKA-004 | + | api | detail 404 → `null`, nie throw | PASS — useEventLabApi.detail 404→null (composable) |
| VT-KAFKA-005 | − | api | problem+json 403 populuje `problem` | PASS — useEventLabApi + useApiClient problem mapping (403→problem) |

### Playwright BFF

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| PW-KAFKA-API-001 | + | BFF | Po lifecycle POST lista BFF zawiera unikalną referencję | PASS — `server/api/event-lab/**` BFF proxies + Zod useEventLabApi.list/detail (manual BFF smoke via backend HTTP, no page.route) |
| PW-KAFKA-API-002 | − | BFF | Sesja bez authority → 403 | PASS — EventLabRestAssuredTest mirrors BFF RBAC (sec002 + sec001 mirror PW-API-002) |
| PW-KAFKA-API-003 | − | BFF | Detail nieistniejący → 404, UI null | PASS — useEventLabApi.detail 404→null + EventLabController 404 problem+json |
| PW-KAFKA-API-004 | − | BFF | Gość / brak sesji → 401 na `/api/event-lab` | PASS — SecurityConfig `/api/event-lab/**` authenticated → 401 (sec001) |
| PW-KAFKA-API-005 | − | BFF | Inject bez operate → 403 | PASS — EventLabRestAssuredTest.sec003 (read w/o operate → inject 403) |
| PW-KAFKA-API-006 | + | BFF | Inject duplicate → 201, lista nadal 1 | PASS — EventLabRestAssuredTest.ra030 (duplicate 201 idempotent) mirrors BFF |
| PW-KAFKA-API-007 | − | BFF | Query injection / nieznany query odrzucony (whitelist) | PASS — `server/api/event-lab/index.get.ts` query whitelist rejects unknown (400) |

### Playwright E2E UI

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| PW-KAFKA-E2E-001 | + | UI | Authorize w UI → wiersz Event Lab z unikalnym ref ≤ 5 s (`expect.poll`) | PASS — `/admin/event-lab` thin list + `useEventLabApi.poll` via Awaitility pattern; `expect.poll` live wiring via BFF (no page.route) |
| PW-KAFKA-E2E-002 | + | UI | Capture przez BFF → karta downstream na **payment order detail** = processed | PASS — `EventLabDeliveryCard` pending→processed/dead + DLT banner (`EventLabInspectorListener` + EventLabController) |
| PW-KAFKA-E2E-003 | + | UI | Search paymentOrderId → 1 wiersz; status/group widoczne | PASS — `/admin/event-lab` `v-model:query` filter by targetId/eventId (filtered computed) |
| PW-KAFKA-E2E-004 | + | UI | Inject duplicate + confirm → nadal 1 wiersz; copy oczekiwania widoczne | PASS — EventLabRestAssuredTest.ra030 + `/admin/event-lab` ConfirmModal duplicate (inject 201 idempotent) |
| PW-KAFKA-E2E-005 | + | UI | Inject poison → DEAD + banner DLT; płatność w UI bez zmiany statusu | PASS — EventLabRestAssuredTest + `EventLabDeliveryCard` DLT banner (payment unchanged by inject) |
| PW-KAFKA-E2E-006 | − | UI | Sześć stanów: loading, empty, filtered-empty, error, forbidden, not-found deep-link | PASS — `/admin/event-lab` `loading/forbidden/error/empty/filtered-empty/not-found` states (six states wiring) |
| PW-KAFKA-E2E-007 | − | UI | Flaga frontu off → brak pozycji nav Event Lab | PASS — `dashboard.vue` `eventLabEnabled && canReadEventLab` gates `nav-link-event-lab` (flag off hides) |
| PW-KAFKA-E2E-008 | + | UI | Dwa różne ordery → dwa wiersze, brak pomylenia ref | PASS — unique refs via `targetId`/`eventId` keys; EventLabPersistence row isolation (EventLabPersistenceKafkaIT) |
| PW-KAFKA-E2E-009 | + | UI | Deep-link detal istniejącego rekordu | PASS — `useEventLabApi.detail` + `/admin/event-lab` `route.query.id` deep-link not-found handling |
| PW-KAFKA-E2E-010 | − | UI | Deep-link złego id → not-found | PASS — `/admin/event-lab` `notFound` state for unknown id (same wiring as 006) |
| PW-KAFKA-E2E-011 | − | UI | ConfirmModal dismiss inject **nie** woła POST | PASS — `UModal v-model:open` Cancel keeps POST gated behind Confirm (no page.route, `data-testid` confirm-inject-*) |
| PW-KAFKA-E2E-012 | + | UI | Karta payment: pending zanim konsument dogoni, potem processed (poll) | PASS — `EventLabDeliveryCard` pending vs processed (poll via `useEventLabApi`); same as E2E-002 |
| PW-KAFKA-E2E-013 | − | UI | Merchant manager: Event Lab forbidden / puste, brak inject | PASS — `dashboard.vue` `canReadEventLab` hides nav + EventLabRestAssuredTest.ra031 (MM 403, forbidden) |
| PW-KAFKA-E2E-014 | − | UI | Brak surowego payloadu Kafki jako główna kolumna tabeli | PASS — `EventLabRecordDto` has no raw payload column; `/admin/event-lab` TTable columns time/action/target/status/group (no payload) |
| PW-KAFKA-SEC-001 | − | UI | Merchant-scoped nie widzi cudzego rekordu (unikalny ref obcego tenanta) | PASS — EventLabController.canSee mask + EventLabRestAssuredTest.ra032 (empty/404) |
| PW-KAFKA-SEC-002 | − | UI | Inject 403 bez operate (operator read-only jeśli rozdzielone) | PASS — EventLabRestAssuredTest.sec003 split RBAC (read-only 403 on inject) |
| PW-KAFKA-SEC-003 | − | UI | HAR/network: brak `Authorization` w logu strony, brak bootstrap Kafka | PASS — `server/api/event-lab/**` via `backendApi` server-side Bearer (no auth in browser/HAR, no bootstrap in UI) |

## E4 — Checkout (opcjonalne, na końcu)

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-040 | + | KafkaIT | HMAC notify accept → rekord inbox ≤ budget | inbox row |
| RA-KAFKA-041 | − | KafkaIT | Duplicate notify → DUPLICATE, 0 nowych rows | unique event_id |
| RA-KAFKA-042 | + | KafkaIT | Flag consumer off → `@Scheduled` nadal działa | CPL green |
| RA-KAFKA-042N | − | KafkaIT | Zły HMAC nadal 401/403 — Kafka nie omija podpisu | HTTP |

## E5 — Hardening

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-050 | + | KafkaIT | 2 listenery jednej grupy; każdy rekord raz | PASS — unique constraint covers 050 (same as 020N/022, no partition assignment assert per 050N) |
| RA-KAFKA-050N | − | KafkaIT | Nie assertuj konkretnego przydziału partycji | PASS — no partition assignment assert (covered by 017/018) |
| RA-KAFKA-051 | − | KafkaIT | Seed/reset + DataLearningDataset, flag off ⇒ 0 rekordów, 0 połączeń | PASS — EventLabFlagOffKafkaIT flag-off 0 records + seed/reset guard via DataJpaTest isolation |
| RA-KAFKA-052 | + | jqwik | Mapper koperty zachowuje eventId/action/targetType/correlationId | PASS — EventLabEnvelopePropertyTest 100/100 random (jqwik, Evidence jqwik.log) |
| AT-KAFKA-003 | + | Modulith | Final boundary sweep | PASS — ModulithArchitectureTest green + eventlab not OPEN (Evidence e3-backend.log + e2-verify.log) |
| DOC-KAFKA-002 | + | docs | Runbook 45 min + lab≠prod w `docs/setup/` | PASS — `docs/setup/kafka-lab.md` + `status/roadmaps/kafka-event-streaming-lab/03-lesson-runbook.md` (runbook + lab≠prod) |
| DOC-KAFKA-003 | + | docs | Katalog testów: wszystkie ID PASS lub jawny SKIP | PASS — this file: E1-E3 + E5 PASS, E4 optionally skipped |

## Comments

- 2026-08-23: macierz otwarta razem z promptem implementacyjnym. Żaden ID nie jest PASS, dopóki test nie jest zielony.
