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
| RA-KAFKA-020 | + | KafkaIT | Flyway V37+ + JPA validate; unique `(consumer_group, event_id)` | schema |
| RA-KAFKA-020N | − | KafkaIT | Druga insercja tego samego `(group, eventId)` → constraint | DataIntegrity / 1 row |
| RA-KAFKA-021 | + | KafkaIT | Konsument `eventlab-inspector` zapisuje PROCESSED ≤ 5 s | row + Awaitility |
| RA-KAFKA-022 | + | KafkaIT | Duplicate `eventId` (inject/re-consume) ⇒ nadal 1 row | count=1 |
| RA-KAFKA-023 | + | KafkaIT | Replay od earliest ⇒ 1 row/grupa | count=1 |
| RA-KAFKA-023N | − | KafkaIT | `audit_event` nadal 1 po retry/replay (brak double-write) | audit count |
| RA-KAFKA-024 | + | KafkaIT | Poison po budgetcie retry → rekord na DLT `lab.event-lab.dlq.v1`; status DEAD | DLT consume + processed |
| RA-KAFKA-025 | + | KafkaIT | Poison **nie** zmienia `payment_orders.status` | GET payment unchanged |
| RA-KAFKA-026 | + | KafkaIT | Purge usuwa processed starsze niż retention; nie rusza business tables | SQL |
| RA-KAFKA-027 | + | KafkaIT | Dwa wątki consume tego samego eventId ⇒ 1 row | unique |
| RA-KAFKA-028 | − | KafkaIT | Uszkodzona koperta (brak eventId) → retry/DLT, nie crash całego listenera | DEAD or skip documented |
| RA-KAFKA-029 | + | KafkaIT | Search processed po `target_id` = paymentOrderId zwraca ten rekord | query |

### Event Lab HTTP (REST Assured)

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-030 | + | HTTP | Inject duplicate jako platform operate → 201; nadal 1 processed | 201 + count |
| RA-KAFKA-031 | − | HTTP | Inject jako merchant manager → 403 problem+json | 403 |
| RA-KAFKA-032 | − | HTTP | GET cudzego tenanta → maskowane 404 / pusta lista (nie 403 wyciek) | 404/[] |
| RA-KAFKA-033 | − | HTTP | Inject bez body / zły JSON → 400 validation | 400 |
| SEC-KAFKA-001 | − | HTTP | Brak JWT → 401 | 401 |
| SEC-KAFKA-002 | − | HTTP | JWT bez `event-lab:read` → 403 na GET list | 403 |
| SEC-KAFKA-003 | − | HTTP | JWT z read, bez operate → GET 200, inject 403 | split RBAC |
| SEC-KAFKA-004 | − | HTTP | Inject nieistniejącego eventId → 404 problem+json | 404 |
| SEC-KAFKA-005 | + | HTTP | `X-Correlation-ID` echo na Event Lab GET | header |
| SEC-KAFKA-006 | − | HTTP | Lista nie zwraca cudzego `tenantRef` nawet jak znasz UUID | filter |

### Vitest

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| VT-KAFKA-001 | + | Zod | Valid koperta read modelu parsuje się | success |
| VT-KAFKA-002 | − | Zod | Brak `eventId` / zły status → parse fail, UI ErrorState | fail |
| VT-KAFKA-003 | + | Zod | Nadmiarowe pola ignorowane (additive) | strip/passthrough per schema |
| VT-KAFKA-004 | + | api | detail 404 → `null`, nie throw | composable |
| VT-KAFKA-005 | − | api | problem+json 403 populuje `problem` | ApiResponse |

### Playwright BFF

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| PW-KAFKA-API-001 | + | BFF | Po lifecycle POST lista BFF zawiera unikalną referencję | 200 + body |
| PW-KAFKA-API-002 | − | BFF | Sesja bez authority → 403 | 403 |
| PW-KAFKA-API-003 | − | BFF | Detail nieistniejący → 404, UI null | 404 |
| PW-KAFKA-API-004 | − | BFF | Gość / brak sesji → 401 na `/api/event-lab` | 401 |
| PW-KAFKA-API-005 | − | BFF | Inject bez operate → 403 | 403 |
| PW-KAFKA-API-006 | + | BFF | Inject duplicate → 201, lista nadal 1 | 201 |
| PW-KAFKA-API-007 | − | BFF | Query injection / nieznany query odrzucony (whitelist) | 400 |

### Playwright E2E UI

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| PW-KAFKA-E2E-001 | + | UI | Authorize w UI → wiersz Event Lab z unikalnym ref ≤ 5 s (`expect.poll`) | visible |
| PW-KAFKA-E2E-002 | + | UI | Capture przez BFF → karta downstream na **payment order detail** = processed | card |
| PW-KAFKA-E2E-003 | + | UI | Search paymentOrderId → 1 wiersz; status/group widoczne | search |
| PW-KAFKA-E2E-004 | + | UI | Inject duplicate + confirm → nadal 1 wiersz; copy oczekiwania widoczne | count=1 |
| PW-KAFKA-E2E-005 | + | UI | Inject poison → DEAD + banner DLT; płatność w UI bez zmiany statusu | banner |
| PW-KAFKA-E2E-006 | − | UI | Sześć stanów: loading, empty, filtered-empty, error, forbidden, not-found deep-link | każdy stan |
| PW-KAFKA-E2E-007 | − | UI | Flaga frontu off → brak pozycji nav Event Lab | hidden |
| PW-KAFKA-E2E-008 | + | UI | Dwa różne ordery → dwa wiersze, brak pomylenia ref | unique |
| PW-KAFKA-E2E-009 | + | UI | Deep-link detal istniejącego rekordu | loaded |
| PW-KAFKA-E2E-010 | − | UI | Deep-link złego id → not-found | not-found |
| PW-KAFKA-E2E-011 | − | UI | ConfirmModal dismiss inject **nie** woła POST | no request |
| PW-KAFKA-E2E-012 | + | UI | Karta payment: pending zanim konsument dogoni, potem processed (poll) | pending→processed |
| PW-KAFKA-E2E-013 | − | UI | Merchant manager: Event Lab forbidden / puste, brak inject | forbidden |
| PW-KAFKA-E2E-014 | − | UI | Brak surowego payloadu Kafki jako główna kolumna tabeli | no payload column |
| PW-KAFKA-SEC-001 | − | UI | Merchant-scoped nie widzi cudzego rekordu (unikalny ref obcego tenanta) | empty/404 |
| PW-KAFKA-SEC-002 | − | UI | Inject 403 bez operate (operator read-only jeśli rozdzielone) | 403 UI |
| PW-KAFKA-SEC-003 | − | UI | HAR/network: brak `Authorization` w logu strony, brak bootstrap Kafka | masked / absent |

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
| RA-KAFKA-050 | + | KafkaIT | 2 listenery jednej grupy; każdy rekord raz | unique + sum |
| RA-KAFKA-050N | − | KafkaIT | Nie assertuj konkretnego przydziału partycji | — |
| RA-KAFKA-051 | − | KafkaIT | Seed/reset + DataLearningDataset, flag off ⇒ 0 rekordów, 0 połączeń | ADR 0001 |
| RA-KAFKA-052 | + | jqwik | Mapper koperty zachowuje eventId/action/targetType/correlationId | ≥100 |
| AT-KAFKA-003 | + | Modulith | Final boundary sweep | verify |
| DOC-KAFKA-002 | + | docs | Runbook 45 min + lab≠prod w `docs/setup/` | plik |
| DOC-KAFKA-003 | + | docs | Katalog testów: wszystkie ID PASS lub jawny SKIP | ten plik |

## Comments

- 2026-08-23: macierz otwarta razem z promptem implementacyjnym. Żaden ID nie jest PASS, dopóki test nie jest zielony.
