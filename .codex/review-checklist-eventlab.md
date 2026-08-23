# Review checklist — Event Streaming Lab (`eventlab`)

Use with `code-review` + `eventlab-kafka` + `java-spring-review`. Tenant isolation checklist stays `.codex/review-checklist.md`.

## Scope

- Kafka only in `eventlab` / `compose.kafka.yml` / `*KafkaIT`.
- No AKHQ, kafka-ui, ECharts, lag dashboard, Schema Registry, Streams, SCRAM, product webhooks.
- Browser never speaks Kafka. No `kafkajs` in `apps/frontend`.
- Lenses MCP is not CI.

## Module

- `eventlab` is not OPEN.
- No `*.internal.*` imports across modules.
- No `@Externalized` on `shared` types.
- Audit still in-process (one `audit_event` row after retry).
- Ops WS unchanged (not fed from Kafka).

## Persistence

- Flyway `db/migration/eventlab/` only; next free version V37+ (confirm max).
- JPA `ddl-auto: validate`.
- Unique `(consumer_group, event_id)`.

## Tests

- Failsafe `*KafkaIT` + `KafkaContainerSupport`.
- Surefire excludes KafkaIT.
- Playwright: unique refs, `expect.poll`, no `page.route` / `routeWebSocket`.
- Flag off: zero broker contact.

## lab ≠ prod

If a review cites `kafka-topic-audit` RF=1 critical: **not** a product defect. See `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md`.
