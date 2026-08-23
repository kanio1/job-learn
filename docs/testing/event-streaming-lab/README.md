---
name: event-streaming-lab-tests
parent: kafka-event-streaming-lab
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-23
---

# Test catalog — Event Streaming Lab (Kafka)

Mapa dla spec [kafka-event-streaming-lab](../../../.codex/specs/kafka-event-streaming-lab.md). Live `--kafka`; zakaz `page.route`/`routeWebSocket`; unikalne referencje; Awaitility ≤10 s; skip `restkit/`+`paymentsupport/`.

Skill: `eventlab-kafka`. Lenses MCP **nie** jest oracle CI.

Pełna macierz +/−: [01-acceptance-cases.md](./01-acceptance-cases.md). Prompt implementacyjny: [.codex/prompts/kafka-event-streaming-implement.md](../../../.codex/prompts/kafka-event-streaming-implement.md).

## Narzędzie

| Warstwa | Narzędzie |
|---|---|
| Oracle brokera / konsumenta | Java Testcontainers Kafka + Failsafe `*KafkaIT` |
| TS | opcjonalny `tools/kafka-probe` |
| UI/BFF | Playwright 1.61 live POM |
| Operator | Lenses UI/SQL; skills `kafka-consumer-lag` / `kafka-dlq-review` po E3; `kafka-topic-audit` tylko z [lab≠prod](../../../.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md) |

## Business cases

| ID | Case | Epik |
|---|---|---|
| BC-KAFKA-01 | Proof-of-delivery per paymentOrderId | E3 |
| BC-KAFKA-02 | Porządek per klucz | E1/E2 |
| BC-KAFKA-03 | Crash-heal at-least-once | E2 |
| BC-KAFKA-04 | Idempotencja (duplicate/replay) | E3 |
| BC-KAFKA-05 | Poison → DLT, biznes bez zmian | E3 |
| BC-KAFKA-06 | Tenant + inject RBAC | E3 |
| BC-KAFKA-07 | Checkout HMAC + inbox over Kafka | E4 opcjonalnie |
| BC-KAFKA-08 | Seeds/flag-off bez brokera | E5 |
| BC-KAFKA-09 | Telescope: Lenses widzi lab topic (docs/runbook, nie CI) | E1-S5 |

## Use cases

| ID | Use case | Aktor |
|---|---|---|
| UC-KAFKA-01 | Authorize → rekord Event Lab / karta payment ≤ 5 s | operator |
| UC-KAFKA-03 | Search paymentOrderId → 1 processed | operator |
| UC-KAFKA-04 | Inject duplicate → 1 row; poison → DEAD + DLT banner | operator |
| UC-KAFKA-05 | Merchant: pusta lista/404; inject 403 | merchant |
| UC-KAFKA-06 | Lenses SQL po kluczu (runbook) | learner |
| UC-KAFKA-07 | `kafka-topic-audit` na labie → RF=1 sklasyfikowane lab≠prod | learner |

UC-KAFKA-02 (drawer key/headers jako główny UI) — **obcięte**; metadane Kafka w Lenses.

## Macierz

### AT

| ID | Co |
|---|---|
| AT-KAFKA-001 | Kontekst bez brokera |
| AT-KAFKA-002 | Modulith; eventlab nie OPEN |
| AT-KAFKA-003 | Final sweep |

### RA `*KafkaIT`

| ID | Asertacja | Story |
|---|---|---|
| RA-KAFKA-001…003 | Smoke; `--kafka` idempotentny; 3p RF1 | E1 |
| RA-KAFKA-010…016 | eventId; flag off; publish/key/headers/rollback; crash-heal | E2 |
| RA-KAFKA-020…026 | schema; consume; duplicate; replay; poison DLT; purge | E3 |
| RA-KAFKA-030…033 | Inject 201/403/404 | E3-S3 |
| RA-KAFKA-040…042 | Checkout (E4) | E4 |
| RA-KAFKA-050…052 | Rebalance; seed-guard; property | E5 |

### PW live POM

| ID | Scenariusz |
|---|---|
| PW-KAFKA-E2E-001 | Authorize UI → rekord (poll ≤5 s) |
| PW-KAFKA-E2E-002 | Capture API → karta downstream na payment order |
| PW-KAFKA-E2E-003 | Search + detal status/group (bez wymogu raw payload) |
| PW-KAFKA-E2E-004 | Inject duplicate ⇒ 1 row |
| PW-KAFKA-E2E-005 | Inject poison ⇒ DEAD + DLT banner |
| PW-KAFKA-E2E-006 | 6 stanów + forbidden |
| PW-KAFKA-API-001…003 | BFF list; forbidden read; detail 404 |
| PW-KAFKA-SEC-001…003 | Tenant mask; inject 403; brak brokera w network log |

### Vitest

Zod read modelu; 404→null; whitelist query.

## Zakazy

Mock brokera w testach domenowych · `page.route`/`routeWebSocket` · globalny stan tematu bez unique ref · `Thread.sleep` · broker w Surefire · kafkajs w `apps/frontend` · Lenses MCP w CI · lag/ECharts E2E.
