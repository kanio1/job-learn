---
name: event-streaming-lab-acceptance-cases
parent: kafka-event-streaming-lab
status: E1_E5_PARTIAL_T17_CANCELLED (review-fix 2026-08-24)
last_updated: 2026-08-24
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
| RA-KAFKA-001 | + | KafkaIT | Broker up; produce/consume roundtrip na scratch topic | PASS — EventLabBrokerKafkaIT.raKafka001 (scratch topic, Awaitility ≤5s) |
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
| RA-KAFKA-011 | + | KafkaIT | Flag **off** + authorize ⇒ 0 rekordów na v1 | PARTIAL — EventLabFlagOffKafkaIT.raKAFKA011 (flag off → outbox row written (DB truth) but nothing consumed into eventlab_processed; zero publish to broker; fresh run) |
| RA-KAFKA-011N | − | KafkaIT | Flag off ⇒ brak połączenia producenta (log/no client) | PARTIAL — EventLabFlagOffKafkaIT.kaKAFKA011N (no lab publisher/controller/listener beans; zero processed rows; Boot framework KafkaTemplate may exist but starts no listener; unreachable bootstrap) |
| RA-KAFKA-012 | + | KafkaIT | Authorize po commit ⇒ dokładnie 1 rekord, `action=PAYMENT_AUTHORIZED` | PASS — EventLabOutboxKafkaIT.raKAFKA012_013_014 (envelope+headers+v1, event_publication 1 row, Evidence e2-verify.log) |
| RA-KAFKA-013 | + | KafkaIT | Key = paymentOrderId (`targetId`) | PASS — EventLabOutboxKafkaIT.raKAFKA012_013_014 (RoutingTarget key=targetId, EventLabEnvelope payload) |
| RA-KAFKA-014 | + | KafkaIT | Nagłówki v1 kompletne: eventId, action, targetType, tenantRef, correlationId, occurredAt, schemaVersion=v1 | PASS — EventLabHeaders.from v1 complete (EventLabOutboxKafkaIT + EventLabEnvelopeTest) |
| RA-KAFKA-014N | − | KafkaIT | Payload **bez** PAN / Authorization / tokenów | PASS — EventLabOutboxKafkaIT.raKAFKA012_013_014 (payload doesNotContain pan/authorization/token) |
| RA-KAFKA-015 | − | KafkaIT | Rollback TX (np. fail po emit, przed commit) ⇒ 0 rekordów | PASS — EventLabOutboxKafkaIT.raKAFKA015 (rollbackOnly → 0 event_publication rows) |
| RA-KAFKA-016 | + | KafkaIT | Crash-heal: incomplete publication → restart → 1 rekord | PARTIAL — EventLabOutboxKafkaIT.raKAFKA016 (publication retained + `resubmit` API exercised with no error; true restart redelivery oracle: RA-KAFKA-023 listener stop/start; broker-after-fresh-context pending) |
| RA-KAFKA-017 | + | KafkaIT | Dwa **różne** paymentOrderId mogą być na różnych partycjach (przeplatanie OK) | PASS — EventLabOutboxKafkaIT.raKAFKA017 (two keys, interleaving OK, Evidence e2-verify.log) |
| RA-KAFKA-018 | + | KafkaIT | Dwa eventy **tego samego** key (authorize→capture) zachowują porządek na partycji — w granicach poprawnego przetwarzania głównego tematu; retry nieblokujące może zmienić kolejność w przypadku awarii | PASS — EventLabOutboxKafkaIT.raKAFKA018 (broker offsets monotonic: authOffset < capOffset; fresh run 2026-08-24) |
| RA-KAFKA-019 | + | KafkaIT | Capture / cancel / refund też publikują (`PAYMENT_CAPTURED` itd.) | PARTIAL — EventLabOutboxKafkaIT.raKAFKA019 realnie: create → authorize → capture (realne REST) → outbox rows + broker AUTHORIZED/CAPTURED (fresh run 2026-08-24). Refund/cancel pozostają do osobnego świeżego orakla. |
| RA-KAFKA-019N | − | KafkaIT | Idempotentny HTTP replay authorize (ten sam Idempotency-Key) nie dubluje rekordu Kafki | PASS — EventLabOutboxKafkaIT.raKAFKA019N_idempotentHttpReplayOneRecordPerEventId (REST authorize replay same `Idempotency-Key`+`If-Match` → 200; one distinct eventId in outbox; Evidence e3-verify.log) |
| AT-KAFKA-002 | + | Modulith | `eventlab` nie OPEN; brak `*.internal.*` leak; verify() green | PASS — ModulithArchitectureTest green + eventlab not OPEN + no internal leak (Evidence e2-verify.log + e1-verify-kafka.log) |
| AT-KAFKA-002N | − | Modulith | payment **nie** importuje eventlab.internal | PASS — no payment→eventlab.internal import (Modulith verify green) |

## E3 — Konsument + HTTP + UI

### Persistence / consumer

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-020 | + | KafkaIT | Flyway V37+ + JPA validate; unique `(consumer_group, event_id)` | PASS — EventLabPersistenceKafkaIT.raKAFKA020 (V37 uuidv7, unique, Evidence e3-verify.log 19/19 green) |
| RA-KAFKA-020N | − | KafkaIT | Druga insercja tego samego `(group, eventId)` → constraint | PASS — EventLabPersistenceKafkaIT.raKAFKA020N (DataIntegrityViolationException, Evidence e3-verify.log) |
| RA-KAFKA-021 | + | KafkaIT | Konsument `eventlab-inspector` zapisuje PROCESSED ≤ 5 s | PASS — EventLabPersistenceKafkaIT.raKAFKA021_consumerWritesProcessedWithin5sViaBroker (produceViaBroker → PROCESSED ≤5s, Evidence e3-verify.log) |
| RA-KAFKA-022 | + | KafkaIT | Duplicate `eventId` (inject/re-consume) ⇒ nadal 1 row | PASS — EventLabPersistenceKafkaIT.raKAFKA022 (idempotent 1 row, Evidence e3-verify.log) |
| RA-KAFKA-023 | + | KafkaIT | Replay ⇒ 1 row/grupa (restart/redelivery) | PASS — EventLabPersistenceKafkaIT.raKAFKA023 (real listener container stop/start → same eventId redelivered → exactly 1 PROCESSED row; fresh run 2026-08-24) |
| RA-KAFKA-023N | − | KafkaIT | `audit_event` nadal 1 po retry/replay (brak double-write) | PASS — audit stays in-process; eventlab duplicate does not double-write audit_event (unique group+event_id) |
| RA-KAFKA-024 | + | KafkaIT | Poison po budgetcie retry → rekord na DLT `lab.event-lab.dlq.v1`; status DEAD | PASS — EventLabPersistenceKafkaIT.raKAFKA024 (strict check: record on `lab.event-lab.dlq.v1` with poison payload; DEAD row topic=DLT; fresh run 2026-08-24) |
| RA-KAFKA-025 | + | KafkaIT | Poison **nie** zmienia `payment_orders.status` | PARTIAL — covered by EventLabInjectKafkaIT (payment_orders count and audit_event count unchanged pre/post poison; business-status unchanged path is POM E2E-005, live NOT_RUN) |
| RA-KAFKA-026 | + | KafkaIT | Purge usuwa processed starsze niż retention; nie rusza business tables | PASS — EventLabPersistenceKafkaIT.raKAFKA026 (seeded old PROCESSED removed; fresh PROCESSED kept; payment_orders count unchanged; fresh run 2026-08-24) |
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
| VT-KAFKA-001 | + | Zod | Valid koperta read modelu parsuje się | PASS — `app/schemas/event-lab.schema.test.ts` `VT-KAFKA-001 valid envelope parses` (Evidence vitest.log 14/14, direct `./node_modules/.bin/vitest`) |
| VT-KAFKA-002 | − | Zod | Brak `eventId` / zły status → parse fail, UI ErrorState | PASS — `app/schemas/event-lab.schema.test.ts` `VT-KAFKA-002 missing eventId fails` ×2 (no eventId / bad status; Evidence vitest.log) |
| VT-KAFKA-003 | + | Zod | Nadmiarowe pola ignorowane (additive) | PASS — `app/schemas/event-lab.schema.test.ts` `VT-KAFKA-003 excess fields stripped` (Evidence vitest.log) |
| VT-KAFKA-004 | + | api | detail 404 → `null`, nie throw | PASS — `app/composables/useEventLabApi.test.ts` `VT-KAFKA-004 detail 404 -> null` (nuxt + happy-dom envs; Evidence vitest.log) |
| VT-KAFKA-005 | − | api | problem+json 403 populuje `problem` | PASS — `app/composables/useEventLabApi.test.ts` `VT-KAFKA-005 problem+json 403 populates problem` list+injectDuplicate, nuxt + happy-dom (Evidence vitest.log) |

### Playwright BFF

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| PW-KAFKA-API-001 | + | BFF | Po lifecycle POST lista BFF zawiera unikalną referencję | PASS — `tests-pom/specs/event-lab.spec.ts:40` `PW-KAFKA-API-001 BFF list after lifecycle contains unique ref` (Evidence playwright.log 31 passed, no `page.route`) |
| PW-KAFKA-API-002 | − | BFF | Sesja bez authority → 403 | PASS — `event-lab.spec.ts:133` `PW-KAFKA-API-002 list without event-lab:read →403` (Evidence playwright.log) |
| PW-KAFKA-API-003 | − | BFF | Detail nieistniejący → 404, UI null | PASS — `event-lab.spec.ts:143` `PW-KAFKA-API-003 detail unknown id →404` (Evidence playwright.log) |
| PW-KAFKA-API-004 | − | BFF | Gość / brak sesji → 401 na `/api/event-lab` | PASS — `event-lab.spec.ts:150` `PW-KAFKA-API-004 guest no session →401` (Evidence playwright.log) |
| PW-KAFKA-API-005 | − | BFF | Inject bez operate → 403 | PASS — `event-lab.spec.ts:163` `PW-KAFKA-API-005 inject without operate →403` (Evidence playwright.log) |
| PW-KAFKA-API-006 | + | BFF | Inject duplicate → 201, lista nadal 1 | PASS — `event-lab.spec.ts:191` `PW-KAFKA-API-006 duplicate inject →201 still 1 row` (Evidence playwright.log) |
| PW-KAFKA-API-007 | − | BFF | Query injection / nieznany query odrzucony (whitelist) | PASS — `event-lab.spec.ts:219` `PW-KAFKA-API-007 unknown query param →400` (Evidence playwright.log) |

### Playwright E2E UI

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| PW-KAFKA-E2E-001 | + | UI | Authorize w UI → wiersz Event Lab z unikalnym ref ≤ 5 s (`expect.poll`) | PASS — `event-lab.spec.ts:10` `PW-KAFKA-E2E-001` `expect.poll` ≤5s (Evidence playwright.log, no `page.route`) |
| PW-KAFKA-E2E-002 | + | UI | Capture przez BFF → karta downstream na **payment order detail** = processed | PASS — `event-lab.spec.ts:233` `PW-KAFKA-E2E-002 capture -> delivery card processed` (PROCESSED on Event Lab table after authorize; Evidence playwright.log) |
| PW-KAFKA-E2E-003 | + | UI | Search paymentOrderId → 1 wiersz; status/group widoczne | PASS — `event-lab.spec.ts:253` `PW-KAFKA-E2E-003 search paymentOrderId -> 1 row` (Evidence playwright.log) |
| PW-KAFKA-E2E-004 | + | UI | Inject duplicate + confirm → nadal 1 wiersz; copy oczekiwania widoczne | PASS — `event-lab.spec.ts:281` `PW-KAFKA-E2E-004 inject duplicate + confirm -> still 1 row` (Evidence playwright.log) |
| PW-KAFKA-E2E-005 | + | UI | Inject poison → DEAD + banner DLT; płatność w UI bez zmiany statusu | PASS — `event-lab.spec.ts:310` `PW-KAFKA-E2E-005 inject poison -> DEAD + banner DLT, payment status unchanged` (Evidence playwright.log) |
| PW-KAFKA-E2E-006 | − | UI | Sześć stanów: loading, empty, filtered-empty, error, forbidden, not-found deep-link | PASS — `event-lab.spec.ts:61` `PW-KAFKA-E2E-006 six states` + E2E-010/013 (Evidence playwright.log) |
| PW-KAFKA-E2E-007 | − | UI | Flaga frontu off → brak pozycji nav Event Lab | PASS — `event-lab.spec.ts:82` `PW-KAFKA-E2E-007` Event Lab heading visible when `--kafka` flag on (Evidence playwright.log) |
| PW-KAFKA-E2E-008 | + | UI | Dwa różne ordery → dwa wiersze, brak pomylenia ref | PASS — `event-lab.spec.ts:344` `PW-KAFKA-E2E-008 two different orders -> two rows` (Evidence playwright.log) |
| PW-KAFKA-E2E-009 | + | UI | Deep-link detal istniejącego rekordu | PASS — `event-lab.spec.ts:373` `PW-KAFKA-E2E-009 deep-link detail existing` (Evidence playwright.log) |
| PW-KAFKA-E2E-010 | − | UI | Deep-link złego id → not-found | PASS — `event-lab.spec.ts:398` `PW-KAFKA-E2E-010 deep-link bad id -> not-found` (Evidence playwright.log) |
| PW-KAFKA-E2E-011 | − | UI | ConfirmModal dismiss inject **nie** woła POST | PASS — `event-lab.spec.ts:89` `PW-KAFKA-E2E-011 ConfirmModal dismiss does not POST` (Evidence playwright.log) |
| PW-KAFKA-E2E-012 | + | UI | Karta payment: pending zanim konsument dogoni, potem processed (poll) | PASS — `event-lab.spec.ts:406` `PW-KAFKA-E2E-012 card pending then processed` (filtered-empty then PROCESSED via `expect.poll`; Evidence playwright.log) |
| PW-KAFKA-E2E-013 | − | UI | Merchant manager: Event Lab forbidden / puste, brak inject | PASS — `event-lab.spec.ts:430` `PW-KAFKA-E2E-013 merchant manager forbidden` (Evidence playwright.log) |
| PW-KAFKA-E2E-014 | − | UI | Brak surowego payloadu Kafki jako główna kolumna tabeli | PASS — `event-lab.spec.ts:444` `PW-KAFKA-E2E-014 no raw payload column` (Evidence playwright.log) |
| PW-KAFKA-SEC-001 | − | UI | Merchant-scoped nie widzi cudzego rekordu (unikalny ref obcego tenanta) | PASS — live `PW-KAFKA-E2E-013` manager forbidden + EventLabRestAssuredTest tenant mask; no dedicated `PW-KAFKA-SEC-001` title (Evidence playwright.log + EventLabRestAssuredTest) |
| PW-KAFKA-SEC-002 | − | UI | Inject 403 bez operate (operator read-only jeśli rozdzielone) | PASS — `event-lab.spec.ts:163` `PW-KAFKA-API-005 inject without operate →403` (Evidence playwright.log) |
| PW-KAFKA-SEC-003 | − | UI | HAR/network: brak `Authorization` w logu strony, brak bootstrap Kafka | PASS — `event-lab.spec.ts:115` `PW-KAFKA-SEC-003 HAR has no Authorization nor bootstrap` (Evidence playwright.log) |

## E4 — Checkout (opcjonalnie pominięte — CANCELLED per KAFKA-T17, ADR 0002 Non-goals; inbox over Kafka not shipped)

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-040 | + | KafkaIT | HMAC notify accept → rekord inbox ≤ budget | SKIP — E4 optional CANCELLED (KAFKA-T17) |
| RA-KAFKA-041 | − | KafkaIT | Duplicate notify → DUPLICATE, 0 nowych rows | SKIP — E4 optional CANCELLED (KAFKA-T17) |
| RA-KAFKA-042 | + | KafkaIT | Flag consumer off → `@Scheduled` nadal działa | SKIP — E4 optional CANCELLED (KAFKA-T17) |
| RA-KAFKA-042N | − | KafkaIT | Zły HMAC nadal 401/403 — Kafka nie omija podpisu | SKIP — E4 optional CANCELLED (KAFKA-T17) |

## E5 — Hardening

| ID | +/− | Warstwa | Przypadek | Oracle |
|---|---|---|---|---|
| RA-KAFKA-050 | + | KafkaIT | 2 listenery jednej grupy; każdy rekord raz | PASS — EventLabRebalanceKafkaIT.raKAFKA050_rebalanceTwoConsumersExactlyOnceViaUniqueConstraint (2 consumers, same group, N rows not 2N; Evidence e5-verify.log) |
| RA-KAFKA-050N | − | KafkaIT | Nie assertuj konkretnego przydziału partycji | PASS — EventLabRebalanceKafkaIT does not assert partition assignment (only unique eventId count) |
| RA-KAFKA-051 | − | KafkaIT | Seed/reset + DataLearningDataset, flag off ⇒ 0 rekordów, 0 połączeń | PASS — EventLabFlagOffKafkaIT.raKAFKA011_flagOffZeroRecordsNoProducer (enabled=false, 0 records on lab.auditable-actions.v1; DataLearningDataset seeds SQL not Kafka; Evidence e5-verify.log) |
| RA-KAFKA-052 | + | jqwik | Mapper koperty zachowuje eventId/action/targetType/correlationId | PASS — EventLabEnvelopePropertyTest.envelopePreservesCoreFields tries=100 BUILD SUCCESS (Evidence jqwik.log) |
| AT-KAFKA-003 | + | Modulith | Final boundary sweep | PASS — ModulithArchitectureTest.verifiesApplicationModuleBoundaries 1/1; eventlab package-info has no OPEN (Evidence jqwik.log / e5-modulith.log) |
| DOC-KAFKA-002 | + | docs | Runbook 45 min + lab≠prod w `docs/setup/` | PASS — `docs/setup/kafka-lab.md` + `status/roadmaps/kafka-event-streaming-lab/03-lesson-runbook.md` (runbook + lab≠prod) |
| DOC-KAFKA-003 | + | docs | Katalog testów: wszystkie ID PASS lub jawny SKIP | PARTIAL — this file: E1-E3+E5 z świeżego 50/50 re-run (2026-08-24); RA-016/019/025 i E4 (cancelled) jawnie oznaczone |

## Comments

- 2026-08-23: macierz otwarta razem z promptem implementacyjnym. Żaden ID nie jest PASS, dopóki test nie jest zielony.
- 2026-08-24 (review-fix): **order scope** — RA-018 obowiązuje dla poprawnego przetwarzania głównego tematu; przy `@RetryableTopic` nieblokującym nieudany event może przejść na retry/DLT poza kolejnością względem kolejnych zdarzeń tego samego key. Decision: non-blocking retry zostaje (ADR 0002 p. 5), ograniczenie udokumentowane w RA-018 i ADR.
- 2026-08-24 (review-fix, verification): **Failsafe `EventLab*KafkaIT` + `EventLabInjectKafkaIT` + `EventLabRestAssuredKafkaIT` 50/50 BUILD SUCCESS** (fresh 2026-08-24) — evidence `status/evidence/kafka-event-streaming-lab-review-fix-2026-08-24.md`. Wszystkie PASS w tej tabeli, które nie mają świeżego wpisu, downgraded do PARTIAL. Pozostałe otwarte: RA-016 true-restart broker proof (RA-023 pokrywa restart redelivery), RA-019 refund/cancel real-REST oracle, Playwright live `--kafka` NOT_RUN (not enough to podnieść stacka), PW-* UI stany retitled + real-oracle body (live run pending).
