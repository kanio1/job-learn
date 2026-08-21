---
name: kafka-event-streaming-lab
origin: POST_KIRO_WORK
status: DESIGNED_NOT_STARTED
related_gate: ADR 0002 PROPOSED — implementacja dopiero po akceptacji P0 przez użytkownika
last_updated: 2026-08-21
---

# Milestone — Event Streaming Lab (Modulith outbox → Kafka)

Wykonawczy backlog dla spec [.codex/specs/kafka-event-streaming-lab.md](../../../.codex/specs/kafka-event-streaming-lab.md). To **nie** jest nowy produkt sprzedajny ani split mikroserwisowy — to realistyczny lesson: transakcyjny outbox → broker → idempotentny konsument → widoczny efekt w dashboardzie, na istniejącym Merchant/Payment/Audit/Ops świecie.

Sąsiedzi: [playwright-ops-wave-2](../playwright-ops-wave-2/) (WS feed zostaje bez Kafki), [playwright-merchant-360](../playwright-merchant-360/). Zero kodu aplikacji w tej sesji dokumentacyjnej.

Katalog testów (BC/UC/RA/PW): [docs/testing/event-streaming-lab/](../../../docs/testing/event-streaming-lab/).
Research + review: [.codex/research/kafka-event-streaming-proposal.md](../../../.codex/research/kafka-event-streaming-proposal.md) · [review task force](../../../.codex/research/kafka-event-streaming-proposal-review.md) · [ADR 0002](../../../.codex/adr/0002-kafka-event-streaming.md).

## Jak czytać

| Plik | Rola |
|---|---|
| [README.md](./README.md) | Indeks, fale, granice |
| [00-context-requirements.md](./00-context-requirements.md) | Cel biznesowy, FR/NFR, non-goals, słownik |
| [01-infra-kafka-stack.md](./01-infra-kafka-stack.md) | Broker overlay, Testcontainers, Flyway V37+, flagi/profil, moduł eventlab |
| [task-board.md](./task-board.md) | `KAFKA-T00`… kolejność implementacji |
| [epics/](./epics/) | E0–E5: stories, AC, komponenty Nuxt UI, ID testów |

## Fale (kolejność implementacji)

0. **E0 Governance** — akceptacja ADR 0002 przez użytkownika; edycje AGENTS/checklist/skills/glossary; zero kodu.
1. **E1 Infra** — compose overlay + `--kafka`, `KafkaContainerSupport`, Awaitility, temat v1 (3 partycje).
2. **E2 Bridge** — stabilny `eventId`, externalizacja programistyczna w eventlab, IT rollback/publish/crash-heal.
3. **E3 Event Lab** — konsument idempotentny + V37, retry/DLQ, inject API + uprawnienie, BFF+Zod+strona `/admin/event-lab`, POM + live E2E.
4. **E4 Checkout over Kafka** — opcjonalny drugi konsument (inbox), bez ruszania HMAC HTTP.
5. **E5 Hardening** — rebalance IT, retention purge, seed-guard, property test koperty, glossary wrap-up.

## Granice (MUST)

- PostgreSQL pozostaje source of truth; Kafka = projekcja/integracja. Brak CDC/Debezium w tej fazie.
- Domyślny stack (`dev-stack.sh` bez flag) i Surefire **bez** brokera; wyłączenia `restkit/`/`paymentsupport/` wg AGENTS.md.
- Live Playwright: bez `page.route` / `routeWebSocket`; auth storageState; unikalne referencje; ops WS specs bez Kafki.
- Flyway V37+ tylko dla eventlab; JPA `ddl-auto: validate`.
- Browser/Nuxt nigdy nie mówi po protokole Kafki — tylko BFF REST.
- Bez fake KPI/lag-charts na Overview; Event Lab to osobna, flagowana strona.
- Audit pisany raz (in-process listener); learning/deterministic seeds nie wymagają brokera (ADR 0001).

## Narzędzie testowe Kafka (decyzja)

**Primary oracle: Java** — `org.testcontainers:testcontainers-kafka` 2.0.5 (`org.testcontainers.kafka.KafkaContainer`, obraz `apache/kafka`) + spring-kafka test + Awaitility, bo SUT (externalizer, konsumenci) jest po stronie JVM. **TypeScript**: możliwy (kafkajs + @testcontainers/kafka), ale jako CI oracle to zła warstwa — wchodzi jako opcjonalny read-only probe `tools/kafka-probe` do nauki lokalnej. Playwright pokrywa powierzchnię użytkownika (UI/BFF) na żywym stacku. Pełne uzasadnienie: [docs/testing/event-streaming-lab/README.md](../../../docs/testing/event-streaming-lab/README.md).
