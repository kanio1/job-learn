# GOAL — Implement Event Streaming Lab (E1→E5) with full KafkaIT + Playwright coverage

Paste this as the **implementation goal**. One sequential program: overlay → outbox bridge → thin Event Lab → optional checkout → hardening. Every slice is **TDD**. Every new behaviour gets **positive and negative** tests on the named seam. After each slice, **update docs and mark cases PASS**.

Do **not** treat this as a single giant PR of UI. Walk `KAFKA-T02` … `KAFKA-T18` in board order. Finish a ticket (code + tests + docs) before the next.

---

## 0. Identity

You are implementing **Event Streaming Lab** in Payment Quality Engineering Lab.

- ADR **0002 ACCEPTED**: `.codex/adr/0002-kafka-event-streaming.md`
- Spec: `.codex/specs/kafka-event-streaming-lab.md`
- Board: `status/roadmaps/kafka-event-streaming-lab/task-board.md` (E0 DONE; start **KAFKA-T02**)
- Epics: `status/roadmaps/kafka-event-streaming-lab/epics/`
- Cases: `docs/testing/event-streaming-lab/01-acceptance-cases.md` ← **this is the acceptance backlog; keep it current**
- Catalog index: `docs/testing/event-streaming-lab/README.md`
- Skill: `.agents/skills/eventlab-kafka` then `spring-modulith` / `nuxt-frontend` / `tdd` / `playwright-pom`
- lab≠prod: `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md`
- Out of scope product: `.codex/out-of-scope/kafka-e6-observability-dashboard.md`

E0 (`KAFKA-T00`, `T01`) is already DONE. Do not re-litigate the ADR.

---

## 1. Non-negotiable constraints

1. PostgreSQL remains source of truth. Kafka is **externalization** of `AuditableActionOccurred` via existing outbox `event_publication`.
2. Module `lab.paymentquality.eventlab` is **not OPEN**. No `@Externalized` on `shared`. No Kafka listeners in `audit` / `payment` / `ops`.
3. Audit stays **in-process** (one `audit_event` row after retry). Ops WebSocket stays **without Kafka**.
4. Browser **never** speaks Kafka. No `kafkajs` in `apps/frontend`. Tokens never in JS.
5. Default stack, Surefire, `--app` / `--full` **stay broker-free**. Kafka only: overlay `compose.kafka.yml`, `dev-stack.sh --kafka`, Failsafe `*KafkaIT`.
6. Host **9092** belongs to the **lab overlay**. Do not bind Lenses CE demo Kafka on 9092. No AKHQ/kafka-ui. No ECharts. No lag dashboard. No Schema Registry / Streams / SCRAM / EOS in this milestone.
7. UI is **thin**: `/admin/event-lab` search + six states + DLT banner + inject confirm + **delivery card on payment order detail**. Kafka payload browser = Lenses, not Nuxt.
8. Canonical DLT name in UI/docs: **dead-letter topic**. Topic: `lab.event-lab.dlq.v1`.
9. Flyway `db/migration/eventlab/`, next free version **V37+** (confirm max first). JPA `ddl-auto: validate`. Unique `(consumer_group, event_id)`.
10. Tests: no `page.route` / `routeWebSocket` / `Thread.sleep`. Unique merchant/payment refs. Awaitility ≤ 10 s. Skip `restkit/` and `paymentsupport/`. Ignore `My*` / `Lesson*`. Lenses MCP is **not** CI.
11. Do not edit `.kiro/**`. Do not commit secrets. Passwords only via env.
12. `kafka-topic-audit` on this cluster **will** scream RF=1 critical. That is lab≠prod, not a ticket to set RF=3.

Pinned versions: Java 25, Boot 4.0.6, Modulith 2.0.6, Testcontainers 2.0.5 (`org.testcontainers.kafka.KafkaContainer`, image `apache/kafka`), Playwright 1.61.0, Nuxt 4.4.6. Do not bump.

---

## 2. How you work (every ticket)

```
RED  → write the failing acceptance test(s) for this ticket from 01-acceptance-cases.md
GREEN → minimal production code
DOCS → mark those IDs PASS; note new IDs you had to add; update task-board row; current-state one liner
```

Seams:

| Behaviour | Seam | Where |
|---|---|---|
| Topic topology, produce/consume, consumer idempotency, DLT, ordering, crash-heal, rebalance | Failsafe `*KafkaIT` | `apps/backend/src/test/java/lab/paymentquality/` (`eventlab` + `rest` as needed) |
| Event Lab HTTP (list/detail/inject), JWT, tenant mask | REST Assured + TestJwt | `rest` and/or `security` |
| Architecture / flag-off context | Surefire module tests | `ModulithArchitectureTest`, `*ModuleTest` |
| Zod / composable | Vitest | `apps/frontend` colocated |
| BFF HTTP | Playwright REST live | `tests-pom` + `BffClient` |
| Operator UI | Playwright E2E live POM | `tests-pom/pages/EventLabPage.ts` + payment-order page |

Highest seam that can prove the behaviour wins. Do **not** duplicate the HTTP status matrix in Playwright if REST Assured already owns it — Playwright then covers **visible states** and live wiring.

After each epic (E1, E2, E3, E5):

- `apps/backend`: `./mvnw test` (must stay green **without** broker) and targeted `./mvnw verify` for new `*KafkaIT` (still skip restkit/paymentsupport).
- From E3: `corepack pnpm typecheck && corepack pnpm lint` in `apps/frontend`; live POM specs that exist.

Update:

- `docs/testing/event-streaming-lab/01-acceptance-cases.md` (PASS)
- `status/roadmaps/kafka-event-streaming-lab/task-board.md`
- `.codex/current-state.md` (next ticket)
- `status/index.md` when a whole epic closes

---

## 3. Sequence (do not skip)

### Wave E1 — broker (`KAFKA-T02` … `T05`, `T19`)

Implement:

- `infra/compose/compose.kafka.yml` (KRaft combined, PLAINTEXT, `localhost:9092` / `payment-quality-kafka:19092`, auto-create OFF, **no UI container**).
- `KafkaContainerSupport` singleton; Surefire excludes `**/*KafkaIT*.java`; Awaitility test scope.
- `scripts/dev-stack.sh --kafka` creates `lab.auditable-actions.v1` (3 partitions, RF1) idempotently.
- Docs: 9092 vs Lenses CE; pointer to lab≠prod.
- `KAFKA-T19`: document how Lenses environment `payment-lab` attaches to the overlay; do not fight CE 9092. SQL smoke is **docs** until E2 produces real records.

Must go green: `RA-KAFKA-001`, `001N`, `002`, `003`, `003N`, `AT-KAFKA-001`, `001N`, `DOC-KAFKA-001`.

### Wave E2 — outbox bridge (`T06` … `T09`)

Implement:

- Stable `eventId` UUID on `AuditableActionOccurred` + factory; OpsFeedBroker reuses it (existing ops tests stay green).
- Module `eventlab` + `spring-modulith-events-kafka` + `EventExternalizationConfiguration` (`@Profile("kafka")` + `app.event-lab.enabled`).
- Envelope v1: key=`targetId`; headers listed in the spec.
- Crash-heal IT (`republish-outstanding-events-on-restart`).

Must go green: `RA-KAFKA-010`–`019` + `010N` `011N` `014N` `015` `019N`, `AT-KAFKA-002` `002N`.

Negative that must exist: rollback ⇒ 0 records; flag off ⇒ 0 records and no producer; no secrets in payload; HTTP idempotent authorize does not double-publish.

### Wave E3 — consumer + thin UI (`T10` … `T16`, `T20`) — **largest test wave**

Backend:

- `eventlab_processed` V37+ (`uuidv7()` PK, unique group+event_id, status PROCESSED/RETRYING/DEAD).
- Consumer group `eventlab-inspector`, `auto.offset.reset=earliest`.
- `@RetryableTopic` → DLT `lab.event-lab.dlq.v1`.
- Authorities: `platform:event-lab:read` and `platform:event-lab:operate` (additive to PLATFORM_ADMIN / OPERATOR).
- HTTP: list/detail + `POST /api/event-lab/inject/duplicate|poison` (delay optional). problem+json. Tenant mask 404. Correlation-ID.

Frontend:

- BFF `server/api/event-lab/**`, Zod, `useEventLabApi`.
- Page `/admin/event-lab` behind `NUXT_PUBLIC_EVENT_LAB_ENABLED`.
- Payment-order **delivery card** (pending / processed / dead).
- POM: `EventLabPage` (`openRecord`, `expectRecordVisible`, `injectDuplicate`, `injectPoison`, state oracles). Fixtures `{ app, api }`. Unique refs from factories.
- Live stack: `--kafka`. **No mocks.**

Docs: `KAFKA-T20` runbook 45 min in `docs/setup/` linking `03-lesson-runbook.md`.

Must go green: **all E3 rows** in `01-acceptance-cases.md` (RA-KAFKA-020–038 family, SEC-KAFKA-*, VT-KAFKA-*, PW-KAFKA-API-*, PW-KAFKA-E2E-001–014, PW-KAFKA-SEC-*).

UI negatives that are mandatory (do not ship E3 without them):

- empty / filtered-empty / error / forbidden / not-found deep-link
- flag off hides nav
- ConfirmModal dismiss does not POST
- merchant manager cannot inject and does not see foreign rows
- poison does not change payment status in UI
- no raw Kafka payload column
- network log has no broker bootstrap and Authorization stays masked

### Wave E4 — optional (`T17`)

Only after E3 is green. HMAC HTTP contract unchanged. Consumer vs `@Scheduled` flag. Cases `RA-KAFKA-040`–`042N`. Existing checkout tests stay green **without** broker.

### Wave E5 — hardening (`T18`)

- Two listeners, one group: exactly-once effect via unique constraint (`RA-KAFKA-050`). Do not assert partition assignment.
- Seed/reset + DataLearningDataset with flag off: zero records (`RA-KAFKA-051`).
- jqwik envelope property (`RA-KAFKA-052`).
- `AT-KAFKA-003`.
- Mark remaining DOC IDs PASS. Wrap-up in `status/index.md`.

---

## 4. Coverage bar (definition of “duże pokrycie”)

You are **not** done with a wave if you only have the happy path.

Minimum counts (create extra IDs in `01-acceptance-cases.md` if you discover more; never delete):

| Wave | KafkaIT / RA / SEC (backend) | Playwright API + E2E + SEC | Vitest |
|---|---|---|---|
| E1 | ≥ 6 (incl. empty consume, no auto-create, surefire exclude) | 0 (no UI yet) | 0 |
| E2 | ≥ 12 (incl. rollback, flag-off, secret absence, key order, idempotent HTTP) | 0 | 0 |
| E3 | ≥ 20 HTTP+consumer (incl. 401/403/404/400, tenant mask, unique, DLT, payment unchanged) | ≥ 18 (E2E+API+SEC, six UI states, flag-off, dismiss, merchant forbidden) | ≥ 5 |
| E4 | ≥ 4 | 0 unless checkout UI already exists | 0 |
| E5 | ≥ 4 | 0 | jqwik counts as backend |

If a positive case exists, add the matching negative (auth, validation, isolation, or “does not change payment”).

---

## 5. Documentation you must write as you go

Per ticket:

1. Flip board status `OPEN` → `DONE`.
2. Flip case IDs to `PASS` in `01-acceptance-cases.md`.
3. If you add a test not in the table, **append a new ID** (do not reuse).
4. Short note in `.codex/current-state.md`: what landed, next `KAFKA-Txx`.
5. E1/E3: operator docs (`docs/setup/`) — how to run `--kafka`, where Lenses is, RF=1 disclaimer.
6. E3: runbook 45 min (capture → Lenses SQL → Event Lab → duplicate → poison).

Do not claim Lenses SQL as a CI PASS. That is DOC/runbook only (`BC-KAFKA-09`).

---

## 6. Done when

- Task board: T02–T16, T19, T20, T18 `DONE`; T17 done or explicitly skipped with reason; T-E6 remains CANCELLED.
- `01-acceptance-cases.md`: no OPEN IDs for shipped waves.
- `./mvnw test` green without Kafka.
- New `*KafkaIT` green under Failsafe.
- Live Playwright Event Lab + delivery-card specs green on `--kafka`.
- Frontend typecheck + lint green.
- Modulith architecture green.
- No kafka-ui, no ECharts, no broker in the browser.

Start now at **KAFKA-T02**. First tests: `RA-KAFKA-001` and `RA-KAFKA-001N`.
