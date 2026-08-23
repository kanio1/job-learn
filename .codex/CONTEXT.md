# Glossary

Terms resolved in grilling. No implementation, no file paths.

## DeterministicDataset

The small, precise seed world used by API tests, Playwright, and REST Assured. Fixed identities (including the 104 payment orders). History for those payments is a single `CREATED` row per order. Invoked by the existing test seed operation. Do not change this contract to make learning data easier.

## DataLearningDataset

A sibling seed world for SQL, data quality, ETL, reconciliation, and later warehouse work. Same OLTP schema, replace-not-overlay: when loaded, it wipes business tables and loads the learning population. Never mixed with the 104 payment fixtures in one database state. Never loaded on application startup or in prod.

## DataLearningProfile

Size of the learning world. `SMALL` is the first shipped profile (about 10 000 payments and matching satellite tables). `MEDIUM` and `SCALE` are named but not built.

## DataLearningTruth

Exact expected counts and key aggregates for one seeded `DataLearningProfile`. The reconciliation oracle: expected vs actual, not “about 6000 captured”.

## Known doors

The existing lab tenant and merchant identities (`TENANT_ALPHA`, `PLATFORM_TENANT`, `PLACEHOLDER_TENANT`, `MERCHANT_ALPHA_001`, and the other fixture merchants). After a learning seed they still exist so Keycloak and the dashboard keep working. Most learning payments hang on `TENANT_ALPHA` / `MERCHANT_ALPHA_001`.

## Synthetic crowd

Extra `LEARN_*` tenants and merchants created only for the learning world, used for tenant skew and isolation exercises. They do not replace known doors.

## Business anomaly

A row that is legal under CHECK, FK, and UNIQUE but interesting for quality work (stale `AUTHORIZED`, missing checkout fulfillment, 503 ACK, retries, risk-flagged or suspended tenant). Lives in OLTP.

## Technical defect

A row that would violate OLTP constraints (bad currency, broken timestamp, duplicate business id). Not inserted into OLTP. Reserved for a later raw/staging dataset.

## Event Streaming Lab (ADR 0002)

Kafka overlay on the existing Modulith outbox. Module `eventlab` (not OPEN). PostgreSQL remains source of truth.

## Outbox (`event_publication`)

Shared Flyway V6 table. After-commit dispatcher. Kafka is **externalization** of selected `AuditableActionOccurred` events, not a second outbox.

## Topic v1

`lab.auditable-actions.v1` — JSON envelope, `schemaVersion=v1`, 3 partitions, RF1, key=`targetId`. Lab-shaped topology (not production HA).

## eventId

Stable UUID on `AuditableActionOccurred`. Dedup key for consumers and Ops feed frames.

## Consumer group

Lab group `eventlab-inspector`. Idempotence = unique `(consumer_group, event_id)` on `eventlab_processed`.

## Replay

Re-read from earliest / reset offsets. Same `eventId` must not create a second processed row.

## Poison pill

Injected payload that fails deserialization/validation after retry budget.

## Dead-letter topic (DLT)

Canonical name for `lab.event-lab.dlq.v1`. „DLQ” is an informal alias only — do not use it as the UI label.

## Telescope (Lenses)

Lenses UI/MCP look at the **lab** broker. Not the Event Lab product. Not a CI oracle.

## lab ≠ prod

Single-node KRaft, RF=1, PLAINTEXT are intentional. `kafka-topic-audit` reporting RF=1 as critical is an operator truth and a **false product lesson** unless framed this way. See `.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md`.
