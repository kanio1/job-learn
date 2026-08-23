---
name: kafka-event-streaming-lab-tasks
origin: POST_KIRO_WORK
status: E0_DONE_CODE_NOT_STARTED
last_updated: 2026-08-23
---

# Task board — kolejność implementacji

Statusy: `OPEN` / `DONE` / `CANCELLED`. Implementacja od `KAFKA-T02`. Skill: `eventlab-kafka`.

| ID | Fala | Epic | Treść | Testy | Status |
|---|---|---|---|---|---|
| KAFKA-T00 | 0 | E0 | Katalog + review + ADR + BC/UC | — | DONE |
| KAFKA-T01 | 0 | E0 | ADR ACCEPTED + AGENTS/CLAUDE/skills + glossary + lab≠prod | — | DONE (2026-08-23) |
| KAFKA-T02 | 1 | E1 | `compose.kafka.yml` (bez kafka-ui) + pin tagu + docs 9092 vs CE | RA-KAFKA-001, 001N | DONE (2026-08-23) |
| KAFKA-T03 | 1 | E1 | `dev-stack.sh --kafka` + create topic (3p RF1) + health | RA-KAFKA-002, 003, 003N | DONE (2026-08-23) |
| KAFKA-T04 | 1 | E1 | `KafkaContainerSupport` + Awaitility + surefire exclude `*KafkaIT*` | AT-KAFKA-001 | DONE (2026-08-23) |
| KAFKA-T05 | 1 | E1 | Temat `lab.auditable-actions.v1` + nota lab≠prod RF=1 | RA-KAFKA-003 | DONE (2026-08-23) |
| KAFKA-T19 | 1 | E1 | Lenses env `payment-lab` na overlay; CE bez host 9092 | docs smoke SQL (po E2) | OPEN |
| KAFKA-T06 | 2 | E2 | Stabilny `eventId` + reuse OpsFeedBroker | RA-KAFKA-010 | DONE (2026-08-23) |
| KAFKA-T07 | 2 | E2 | Dep events-kafka + moduł eventlab + bean externalizacji | AT-KAFKA-002, RA-KAFKA-011 | DONE (2026-08-23) |
| KAFKA-T08 | 2 | E2 | Koperta v1: mapping, nagłówki, key=`targetId` | RA-KAFKA-012…015 | DONE (2026-08-23) |
| KAFKA-T09 | 2 | E2 | Crash-heal IT | RA-KAFKA-016 | DONE (2026-08-23) |
| KAFKA-T10 | 3 | E3 | V37+ `eventlab_processed` (uuidv7, unique group+event_id) | RA-KAFKA-020 | OPEN |
| KAFKA-T11 | 3 | E3 | Konsument `eventlab-inspector` (flagship PoD ≤5 s) | RA-KAFKA-021…023 | OPEN |
| KAFKA-T12 | 3 | E3 | Retry + DLT `lab.event-lab.dlq.v1` + purge | RA-KAFKA-024…026 | OPEN |
| KAFKA-T13 | 3 | E3 | Authorities read+operate + inject duplicate/poison | RA-KAFKA-030…033 | OPEN |
| KAFKA-T14 | 3 | E3 | BFF + Zod + `useEventLabApi` | PW-KAFKA-API-001…003 | OPEN |
| KAFKA-T15 | 3 | E3 | Cienkie `/admin/event-lab` + karta downstream na payment order | PW-KAFKA-E2E-001…006 | OPEN |
| KAFKA-T16 | 3 | E3 | POM EventLabPage + delivery card | wspiera PW-KAFKA-* | OPEN |
| KAFKA-T20 | 3 | E3 | Runbook 45 min w `docs/setup/` | docs | OPEN |
| KAFKA-T17 | 4 | E4 | (opcjonalnie) checkout inbox over Kafka | RA-KAFKA-040…042 | OPEN |
| KAFKA-T18 | 5 | E5 | Rebalance IT + seed-guard + jqwik koperty + wrap-up | RA-KAFKA-050…052, AT-KAFKA-003 | OPEN |
| KAFKA-T-E6 | — | — | Observability dashboard / ECharts / load topic | — | CANCELLED |

## Zależności

```text
T01 DONE
T02..T05, T19 (fala 1; T19 SQL smoke po T08)
T05 ──► T06..T09 (fala 2)
T09 ──► T10..T16, T20 (fala 3; T13 ∥ T10/T11)
T16 ──► T17 (opcjonalna), T18
```

## Definition of Done (per fala)

- Backend: exclusions `restkit/` + `paymentsupport/`; `*KafkaIT` zielone.
- Frontend E3+: `corepack pnpm typecheck && corepack pnpm lint` + wskazane PW na `--kafka`.
- `.kiro/**` nietknięte; status w `.codex/current-state.md` / `status/index.md`.
- Brak kafka-ui, ECharts, Lenses MCP w CI.
