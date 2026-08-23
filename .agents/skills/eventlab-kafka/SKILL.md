---
name: eventlab-kafka
description: >-
  Place and change the Event Streaming Lab (ADR 0002): Spring Modulith Kafka
  externalization in module eventlab, compose overlay, Testcontainers KafkaIT,
  thin Event Lab UI/BFF, Lenses as telescope. Use when adding or editing
  eventlab, compose.kafka.yml, Kafka listeners, eventlab_processed, inject API,
  /admin/event-lab, or Lenses-to-lab wiring. Do not use for a Kafka UI clone,
  lag dashboards, Schema Registry, Streams, or product webhooks.
---

# Event Streaming Lab (this repo)

Kafka exists **only** as a lab overlay on the existing Modulith outbox. PostgreSQL stays the source of truth. Browser never speaks Kafka.

Canonical plan: `status/roadmaps/kafka-event-streaming-lab/`. Spec: `.codex/specs/kafka-event-streaming-lab.md`. ADR: `.codex/adr/0002-kafka-event-streaming.md` (ACCEPTED).

## Compose

| Job | Skill |
|---|---|
| Java/Spring placement in `eventlab` | this skill, then `spring-modulith` |
| Nuxt/BFF Event Lab page + payment delivery card | `nuxt-frontend` |
| REST Assured / KafkaIT design | `tdd` + `rest-api-test-design` + this skill |
| Playwright live POM | `tdd` + `playwright-pom` |
| Operator view of **our** topic (SQL, lag, DLT) | Lenses UI + user skills `kafka-topic-audit`, `kafka-consumer-lag`, `kafka-dlq-review` — **after** reading [lenses-lab-vs-prod.md](references/lenses-lab-vs-prod.md) |
| Review | `code-review` → `java-spring-review` / `playwright-sdet-review` |

Do not build a second Lenses. Do not add `kafkajs` to `apps/frontend`.

## Workflow

1. Confirm the task is on `status/roadmaps/kafka-event-streaming-lab/task-board.md` (`KAFKA-T*`). Do not invent E6 observability, kafka-ui, ECharts, or Schema Registry.
2. Own module is **`eventlab`** (`lab.paymentquality.eventlab`, not OPEN). Do not put listeners or producers in `shared`, `payment`, or `audit`.
3. Externalize with `spring-modulith-events-kafka` (BOM Modulith 2.0.6) via `EventExternalizationConfiguration` under `@Profile("kafka")` + `app.event-lab.enabled=true`. Never `@Externalized` on types in `shared`.
4. One topic wave 1: `lab.auditable-actions.v1` (3 partitions, RF1, key=`targetId`). DLT: `lab.event-lab.dlq.v1` — UI/docs say **dead-letter topic (DLT)**; DLQ is informal alias only.
5. Tests: Failsafe `*KafkaIT` + `KafkaContainerSupport` (image matches compose). Surefire stays broker-free. Playwright asserts visible proof-of-delivery, not protocol.
6. Operator telescope: Lenses looks at the **lab** broker. Host port 9092 is the lab overlay. Lenses CE demo Kafka is a separate playground — see [lenses-lab-vs-prod.md](references/lenses-lab-vs-prod.md).

## Lab mappings

| Vocabulary | Here |
|---|---|
| Outbox | Existing `event_publication` (shared V6). Do not add a second outbox table |
| Externalization | After-commit publish of `AuditableActionOccurred` from `eventlab` |
| Proof-of-delivery | One `eventlab_processed` row per `(consumer_group, event_id)` searchable by paymentOrderId |
| Inject | `POST /api/event-lab/inject/*` — duplicate / poison (delay optional). Authority `platform:event-lab:operate` |
| Read HTTP | `platform:event-lab:read` |
| UI | Thin `/admin/event-lab` + delivery card on payment order. Kafka metadata lives in Lenses |
| Oracle | Java Testcontainers. Lenses MCP is **not** CI |

## UI that is in scope

- Search by `paymentOrderId` / `eventId`; six states (loading / empty / filtered-empty / error / forbidden / not-found).
- Inject duplicate/poison with expected-result copy.
- DLT banner when status is DEAD.
- Payment-order detail: downstream processed / pending / dead.

## UI that is out of scope

Lag heatmaps, ECharts, LIVE/PAUSED follow, partition chips as primary UI, raw payload table, AKHQ/kafka-ui, WebSocket fan-in from Kafka, fake Overview KPI.

## Defaults

- `dev-stack.sh --kafka` is a **third** mode; do not mix with `--app` / `--full`.
- Auto-create OFF; `--kafka` creates the topic explicitly.
- `auto.offset.reset=earliest` on the lab group.
- PLAINTEXT broker: JWT still guards HTTP. Tenant filter is in consumers + HTTP, not Kafka ACLs.
- Flyway `db/migration/eventlab/` — next free version **V37+** (confirm max before writing).
- Audit stays in-process (one `audit_event` row). Ops WS stays in-process (ADR decision 7).

## When not to use

- Merchant/payment REST without the overlay (`spring-modulith`).
- Product merchant webhooks, Streams, EOS, SCRAM, compacted changelog, clickstream.
- Treating Lenses CE demo topics (`telecom_italia_data`, …) as payment-lab data.
