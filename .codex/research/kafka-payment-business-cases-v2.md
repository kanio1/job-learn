# Business Case Catalog v2 — classic Kafka cases mapped to this payments lab

Status: ITERATION_2_AWAITING_P0_DECISION
Date: 2026-08-21
Panel: tech-lead, architect, Kafka expert, PostgreSQL 18 administrator, implementation expert, Keycloak/IAM reviewer.
Input: `kafka-event-streaming-proposal.md`, `kafka-event-streaming-proposal-review.md`, ADR 0002, spec `kafka-event-streaming-lab.md`.
Research: Firecrawl MCP (`firecrawl_search`) against Confluent/Conduktor/meshiq/IBM FinServ write-ups, webhook-reliability guides (Hookdeck/KodeKloud), Debezium/outbox engineering posts (Trade Republic, singhajit), idempotent-consumer pattern posts, PostgreSQL 18 release notes (postgresql.org, xata, CrunchyData, Neon), Podman/KRaft compose examples.

Scope: ADR 0002 is **ACCEPTED** (2026-08-23). This catalog remains a **reading list**, not an implementation backlog. Wave 1 delivery is still cases 1+2 (+3 as DLT, not product webhooks).

---

## 1. The 15 classic business cases (payments context)

Legend: **Core** (first slice) · **Later** (approved backlog wave) · **Reshape** (different shape than industry does it) · **Defer** (real case, wrong wave) · **Reject** (non-goal here).

| # | Classic industry case | Meaning in this lab (channels: dashboard REST, hosted Checkout Lab, Mirror Lab evidence) | Verdict / wave | PG 18 tie-in |
|---|---|---|---|---|
| 1 | Payment lifecycle event backbone | `PAYMENT_*` + MERCHANT/SUPPORT/USER actions → outbox → `lab.auditable-actions.v1`, key=`targetId` | **Core** (E2–E3) | Outbox `event_publication` already durable; republish-on-restart heals crashes |
| 2 | Integration proof-of-delivery ("did the capture reach downstream?") | Flagship UC: search `eventlab_processed` by paymentOrderId/correlationId → exactly one processed row, timestamp, consumer group | **Core** (E3, flagship AC) | Index `(target_id, consumed_at DESC)`; `uuidv7()` PK keeps index locality hot |
| 3 | Signed webhook dispatch with retry tiers + DLQ | Industry: merchant webhooks. Here: dispatch via reused `CheckoutLabSignatureService` HMAC against WireMock; delivery log; poison → `lab.event-lab.dlq.v1` | **Reshape** (E3/P3) | Delivery-log table benefits from `uuidv7()` ids; purge job (7d retention) |
| 4 | PSP callback ingestion bus (notify → inbox worker) | Checkout Lab signed notify produced after HMAC accept; worker consumes instead of poller (flag-gated, HTTP contract untouched) | **Later, optional** (E4) | Inbox dedup stays `checkout_event.event_id` unique key |
| 5 | Omnichannel fan-in (POS + e-commerce + wallet analogs) | Many intake surfaces (admin REST commands, Checkout Lab, Mirror Lab evidence POSTs) → one typed contract on one bus; teaches many-producers/one-contract | **Later** (E5+, no new module) | Single `AuditableActionOccurred` envelope; no per-channel topics in wave 1 |
| 6 | Fraud / velocity risk detection | Industry: real-time rules <60s (Confluent FinServ posts). Here: risk flag is a manual PATCH today — auto-flag would be a new product rule; prototype rule in SQL window first | **Defer** (P4; SQL-window prototype allowed earlier as pure SQL lab) | Window functions + skip-scan on `(merchant_id, created_at)` make the SQL prototype fast |
| 7 | Cross-channel reconciliation & anomaly detection | `CheckoutLabReconcileService` + `uk_checkout_anomaly_session_kind` exist; Postgres join is the honest first oracle; Kafka join later | **Later** (E4/E5) | Existing unique constraint is the idempotency oracle; async I/O makes batch reconcile cheaper |
| 8 | Async export via competing consumers (replace DB poller) | Export jobs keep `202 + Location`; competing `@KafkaListener`s replace `PaymentExportJobWorker` polling; rebalance IT included | **Later** (E5) | Job rows already in Postgres; consumer dedup via job-state CAS, not new tables |
| 9 | Audit replication fan-out (second writer to `audit_event`) | Double-write hazard identified in review; audit stays in-process once | **Reject as designed** (Event Lab copy/export only) | `audit_event` untouched; copy lives in `eventlab_processed` |
| 10 | Compacted current-state topic (`payment.state.current`) | Entity-cache teaching demo; OLTP remains oracle; divergence drill in Event Lab | **Defer** (P4) | Compare topic state vs `payment_orders` snapshot query |
| 11 | Delayed actions / SLA escalation via retry topics | Industry myth-buster: Kafka is not a timer; refund dual-control + PIN stays Postgres; schedulers own time | **Reject as timer / Defer concept** (backoff lesson inside E3 retry tier) | `payment_refund_approvals.created_at` + scheduler unchanged |
| 12 | Identity provisioning fan-out (USER_* consumers) | IAM emits USER_*; Mirror Lab identity-copy consumer is new; additive JSON evolution v1→v2 without Schema Registry | **Later** (E5) | No secrets in payloads; Keycloak stays sole identity source |
| 13 | Dispute / chargeback event stream | Mirror Lab disputes + evidence uploads could emit DISPUTE_*; requires new emit path (respect ADR 0001 seed rules) | **Defer** (candidate spec after E5) | Evidence metadata in `afterState` only; no blobs on topics |
| 14 | Ops incident broadcast across instances (WS fan-in) | OpsFeedBroker dies with a second instance; Kafka fan-in only when running two Spring instances becomes a real lab goal | **Defer** (explicit precondition) | `ops_feed_event` + unique `(recipient_subject, event_id)` remain the sink |
| 15 | Behavioral clickstream / analytics streaming | Classic Conduktor-style case; here it degenerates into fake-KPI adjacency | **Reject** (non-goal stands) | — |

Count check: 2 Core · 6 Later/optional · 2 Reshape/Rejection-with-teaching · 4 Defer · 1 Reject-clean. Wave-1 delivery surface stays exactly as approved in review: cases **1 + 2 (+3 reshaped)**.

## 2. PostgreSQL 18 × Kafka synergies (adopted into V37 design)

| Synergy | Decision | Why (verified against PG18 sources) |
|---|---|---|
| `uuidv7()` | `eventlab_processed.id UUID PRIMARY KEY DEFAULT uuidv7()` in migration V37 | Native PG18 function; time-ordered UUIDs keep B-tree inserts locality-warm for high-volume consume loops |
| eventId generation | **Stays Java-side `UUID.randomUUID()` (v4)** on `AuditableActionOccurred`; do **not** pull a UUIDv7 lib into the backend | Uniqueness is the dedup requirement; ordering belongs to `occurredAt` + partition key. No new dependency without approval |
| Virtual generated columns | `event_day date GENERATED ALWAYS AS ((consumed_at AT TIME ZONE 'UTC')::date) STORED`… **correction: use VIRTUAL** for UI grouping/display only | PG18 computes VIRTUAL on read — zero write cost; **cannot be indexed in 18**, so real indexes stay on `(consumer_group, status, consumed_at)` and `(target_id)` |
| B-tree skip scan | Bonus, not a promise: DLQ view filtered by `status` alone may skip-scan the composite index | Verify with `EXPLAIN` during E3; never build a story on planner behavior |
| Temporal constraints (`WITHOUT OVERLAPS`) | Not applicable to eventlab; recorded as an adjacent-lab idea (monitoring windows) only | Discipline: no schema feature without a real constraint problem |
| OAuth 2.0 authentication (`pg_hba oauth` + Keycloak validator) | **Follow-up lab idea outside Kafka waves**: PG18 delegates DB authn to local Keycloak 26 (community validators exist, e.g. cloudnative-pg `postgres-keycloak-oauth-validator`) | Completes the Keycloak story at the database edge; separate spec if user wants it |
| Async I/O | Zero action; noted because long retention purges and reconcile batches get cheaper | Background performance, not a design input |

## 3. Runtime topology — Podman Compose, no distributed systems, no Kubernetes

Decisions (brave but bounded):

1. **Rootless Podman Compose is the documented runtime** for the overlay: `podman compose -f infra/compose/compose.yml -f infra/compose/compose.kafka.yml --env-file infra/compose/.env up -d`. Docker Compose keeps working (same file semantics); docs lead with Podman because the lab machines run it. Ports stay unprivileged (>1024): `9092` host, `19092` in-network — no root requirement, no K8s manifests anywhere in the repo.
2. **Single-node KRaft combined node**, official `apache/kafka` image, PLAINTEXT, no ZooKeeper (removed in Kafka 4). Candidate tag pinned at T02 against Testcontainers 2.0.5 compatibility (open question preserved); dual advertised listeners: `localhost:9092` (host tools, kafka-probe) and `payment-quality-kafka:19092` (compose network).
3. **Explicit topic creation beats auto-create**: `dev-stack.sh --kafka` runs a one-shot `kafka-topics.sh --create` (3 partitions, RF1) plus healthcheck `kafka-broker-api-versions --bootstrap-server localhost:9092`. Auto-create disabled on the broker so learners see topology as code.
4. **AKHQ/kafka-ui only on this overlay** (operator learning), never in the default or `--app` POM path.
5. **Single broker is the honest lab shape**: RF1 + 3 partitions teach keys/ordering/groups/DLQ; replication-factor lessons are deferred until multi-broker is a deliberate exercise (compose scale-out possible later without code change).

## 4. Decisions log (iteration 2, "be brave" items with rationale)

| ID | Decision | Bravery rationale | Guardrail kept |
|---|---|---|---|
| D-1 | Adopt PG18 `uuidv7()` as V37 PK default; keep event `eventId` Java-v4 | Modern platform synergy without a new Java dep | Flyway-only schema, `ddl-auto: validate` passes |
| D-2 | Lead runtime docs with Podman Compose; forbid K8s artifacts in this lab | Matches actual machines; removes "distributed systems" gravity | Default stack unchanged; `--kafka` third mode |
| D-3 | Auto-create OFF; topic created explicitly by `--kafka` mode | Topology-as-code beats convenience for learning | One topic v1 until case-driven split (P4) |
| D-4 | Case 2 (proof-of-delivery) is the flagship acceptance path | Real integration-support job-to-be-done, not badge theatre | ≤5 s NFR, unique-reference assertions |
| D-5 | Case 6 fraud: allow a pure-SQL window prototype early, Kafka Streams stays P4+ | De-risks the "new product rule" question honestly | Risk-flag remains manual PATCH until its own spec |
| D-6 | PG18 OAuth-at-Postgres recorded as separate follow-up lab (not a Kafka wave) | Captures today's research before it evaporates | Scope isolation: Kafka epic stays Kafka |

## 5. Sources (Firecrawl, 2026-08-21)

- FinServ Kafka patterns: meshiq "Apache Kafka in Financial Services"; Conduktor "Kafka in Financial Services"; mimacom "Data Streaming for Financial Services"; Confluent "Real-time streaming prevents fraud"; Kai Wähner FinServ top-10.
- Webhook reliability: Hookdeck DLQ guide; KodeKloud webhook-system notes; didit.me retry/DLQ strategies; Transactional Outbox series (Level Up Coding).
- Outbox vs CDC: Trade Republic Engineering "Streaming Outbox Events … with Debezium"; HN "Push-based outbox with logical replication"; singhajit Debezium outbox impact.
- Idempotent consumer: Conduktor "Building Idempotent Consumers"; TrinityLogic consumer idempotency; Confluent EOS deep-dive (context for "teach acks, not EOS").
- PostgreSQL 18: postgresql.org Release 18 notes + announcement; xata features deep-dive; CrunchyData async-I/O post; Neon OAuth + skip-scan pages; cloudnative-pg Keycloak OAuth validator.
- Runtime: Apache Kafka KRaft compose examples; Instaclustr KRaft-on-Docker tutorial; streamthoughts kraft-single-node-stack.yml.

Operational note: `firecrawl_search` returned HTTP 429 when called in rapid bursts; batch queries with short backoff (credits were not the limit).
