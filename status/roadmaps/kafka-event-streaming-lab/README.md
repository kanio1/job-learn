---
name: kafka-event-streaming-lab
origin: POST_KIRO_WORK
status: E0_DONE_CODE_NOT_STARTED
related_gate: ADR 0002 ACCEPTED 2026-08-23 — implementacja od E1 (`KAFKA-T02`)
last_updated: 2026-08-23
---

# Milestone — Event Streaming Lab (Modulith outbox → Kafka)

Wykonawczy backlog dla spec [.codex/specs/kafka-event-streaming-lab.md](../../../.codex/specs/kafka-event-streaming-lab.md). To **nie** jest nowy produkt sprzedajny ani split mikroserwisowy — to lekcja: transakcyjny outbox → broker → idempotentny konsument → **dowód dostarczenia** na płatności.

Lenses UI + MCP są **lunetą** (SQL, lag, DLT). Nuxt nie klonuje Lenses.

Sąsiedzi: [playwright-ops-wave-2](../playwright-ops-wave-2/) (WS feed **zostaje bez Kafki**), [playwright-merchant-360](../playwright-merchant-360/).

Skill kodowania: `.agents/skills/eventlab-kafka`. Lab≠prod: [references/lenses-lab-vs-prod.md](../../../.agents/skills/eventlab-kafka/references/lenses-lab-vs-prod.md).

**Implement goal (kolejność + acceptance):** [.codex/prompts/kafka-event-streaming-implement.md](../../../.codex/prompts/kafka-event-streaming-implement.md) · macierz ID: [docs/testing/event-streaming-lab/01-acceptance-cases.md](../../../docs/testing/event-streaming-lab/01-acceptance-cases.md).

## Jak czytać

| Plik | Rola |
|---|---|
| [README.md](./README.md) | Indeks, fale, granice |
| [00-context-requirements.md](./00-context-requirements.md) | Cel, FR/NFR, non-goals, słownik |
| [01-infra-kafka-stack.md](./01-infra-kafka-stack.md) | Overlay, Testcontainers, Flyway, flagi, cienki UI |
| [02-lenses-telescope.md](./02-lenses-telescope.md) | Lenses vs lab broker, port 9092, MCP |
| [03-lesson-runbook.md](./03-lesson-runbook.md) | Lekcja 45 min (capture → Lenses SQL → Event Lab → inject) |
| [task-board.md](./task-board.md) | `KAFKA-T00`… kolejność |
| [epics/](./epics/) | E0–E5 stories + AC |

## Fale

0. **E0 Governance** — **DONE** (ADR ACCEPTED, AGENTS/skills zaktualizowane).
1. **E1 Infra** — overlay + `--kafka` + `KafkaContainerSupport` + Lenses środowisko `payment-lab` (bez kafka-ui).
2. **E2 Bridge** — `eventId`, externalizacja w `eventlab`, IT rollback/publish/crash-heal.
3. **E3 Thin Event Lab** — processed + DLT + inject + BFF + wyszukiwarka + karta na payment order + runbook. **Nie** konsola Kafki.
4. **E4 Checkout over Kafka** — opcjonalny drugi konsument.
5. **E5 Hardening** — rebalance IT, seed-guard, property koperty.

**Nie ma E6.** Observability/ECharts/generator load = [out of scope](../../../.codex/out-of-scope/kafka-e6-observability-dashboard.md).

## Granice (MUST)

- PostgreSQL = source of truth; Kafka = projekcja. Brak CDC.
- Domyślny stack i Surefire **bez** brokera.
- Live Playwright: bez `page.route` / `routeWebSocket`; unikalne referencje; ops WS specs bez Kafki.
- Flyway V37+ tylko eventlab; JPA validate.
- Browser nigdy nie mówi po Kafce.
- Bez fake KPI / lag charts / AKHQ.
- Audit raz (in-process); seeds bez brokera (ADR 0001).
- Host **9092** = lab overlay. Lenses CE demo nie binduje 9092.

## Oracle testowe

**Primary: Java** Testcontainers Kafka (`*KafkaIT`). TypeScript: opcjonalny `tools/kafka-probe`. Playwright: UI/BFF. Lenses MCP: eksploracja, nie CI.
