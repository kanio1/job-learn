---
name: kafka-event-streaming-lab-tasks
origin: POST_KIRO_WORK
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-21
---

# Task board — kolejność implementacji

Statusy: `OPEN` / `DONE`. Wave 0 (E0) startuje wyłącznie po akceptacji ADR 0002 przez użytkownika.

| ID | Fala | Epic | Treść | Testy (projektowane) | Status |
|---|---|---|---|---|---|
| KAFKA-T00 | 0 | E0 | Ten katalog + review + ADR PROPOSED + katalog BC/UC | — | DONE (docs) |
| KAFKA-T01 | 0 | E0 | Akceptacja ADR 0002 (user) + edycje AGENTS.md/review-checklist/skills ("Kafka only in eventlab") + glossary .codex/CONTEXT.md | — | OPEN |
| KAFKA-T02 | 1 | E1 | `compose.kafka.yml` overlay + pin tagu `apache/kafka` + docs setup (runtime: Podman Compose rootless; auto-create OFF) | RA-KAFKA-001 (broker-up smoke IT) | OPEN |
| KAFKA-T03 | 1 | E1 | `dev-stack.sh --kafka` (trzeci tryb) + jawne tworzenie tematu (3 partycje, RF1) + health check | RA-KAFKA-002, RA-KAFKA-003 (topologia) | OPEN |
| KAFKA-T04 | 1 | E1 | `KafkaContainerSupport` (singleton) + dep Awaitility + surefire excludes `*KafkaIT*` | AT-KAFKA-001 (kontekst bez brokera startuje) | OPEN |
| KAFKA-T05 | 1 | E1 | Temat `lab.auditable-actions.v1` (3 partycje, RF1) + polityka tworzenia | RA-KAFKA-003 (topologia) | OPEN |
| KAFKA-T06 | 2 | E2 | Stabilny `eventId` UUID na rekordzie + fabryka + reuse w OpsFeedBroker frames | RA-KAFKA-010 (eventId stabilny), istniejące ops specs green | OPEN |
| KAFKA-T07 | 2 | E2 | Dep `spring-modulith-events-kafka` + moduł `eventlab` (package-info, nie OPEN) + config bean externalizacji (profile+flag) | AT-KAFKA-002 (Modulith verify), RA-KAFKA-011 (flag off ⇒ zero rekordów) | OPEN |
| KAFKA-T08 | 2 | E2 | Koperta v1: mapping, nagłówki, routeKey=`targetId` | RA-KAFKA-012…015 (publish-after-commit, key, headers, brak publikacji przy rollbacku) | OPEN |
| KAFKA-T09 | 2 | E2 | Crash-heal IT: commit bez publikacji → republish-on-restart | RA-KAFKA-016 | OPEN |
| KAFKA-T10 | 3 | E3 | V37 `eventlab_processed` (unique consumer_group+event_id, `id UUID DEFAULT uuidv7()` PG18, VIRTUAL `event_day`) + encja/repo + validate | RA-KAFKA-020 (schema/validate) | OPEN |
| KAFKA-T11 | 3 | E3 | Konsument idempotentny `eventlab-inspector` (flagship: proof-of-delivery per paymentOrderId ≤ 5 s) + statusy PROCESSED/RETRYING/DEAD | RA-KAFKA-021…023 (consume, duplicate eventId ⇒ 1 row, replay) | OPEN |
| KAFKA-T12 | 3 | E3 | Retry/DLQ `@RetryableTopic` + DLT `lab.event-lab.dlq.v1` + retention purge | RA-KAFKA-024…026 (poison→DLQ, business row unchanged, purge) | OPEN |
| KAFKA-T13 | 3 | E3 | Authority `platform:event-lab:operate` (Authorities+converter+realm+catalog test) + inject API duplicate/poison/delay | RA-KAFKA-030…033 (201 admin/403 readonly/404 mask, wzór PW-OPS-API-020) | OPEN |
| KAFKA-T14 | 3 | E3 | BFF proxy `server/api/event-lab/**` + Zod schema + `useEventLabApi` | Vitest schema/api; PW-KAFKA-API-001…003 | OPEN |
| KAFKA-T15 | 3 | E3 | Strona `/admin/event-lab` (UTable/USlideover/USelect/UAlert, 6 stanów) + nav flaga | PW-KAFKA-E2E-001…006, PW-KAFKA-SEC-001…003 | OPEN |
| KAFKA-T16 | 3 | E3 | POM `EventLabPage` (+drawer component) + fixtures/BffClient | wspiera wszystkie PW-KAFKA-E2E | OPEN |
| KAFKA-T17 | 4 | E4 | (opcjonalnie) checkout inbox over Kafka: produce po HMAC accept, worker consume lub @Scheduled (flaga), dedup event_id | RA-KAFKA-040…042; istniejące CPL testy green bez brokera | OPEN |
| KAFKA-T18 | 5 | E5 | Hardening: rebalance IT (druga instancja grupy), seed-guard IT, property test koperty (jqwik), wrap-up glossary/docs | RA-KAFKA-050…052, AT-KAFKA-003 | OPEN |

## Zależności

```text
T01 ──► T02..T05 (fala 1)
T05 ──► T06..T09 (fala 2; T06 niezależny od T07/T08, ale przed T11)
T09 ──► T10..T16 (fala 3; T13 równoległe do T10/T11)
T16 ──► T17 (opcjonalna), T18 (zamknięcie)
```

## Iteracja 2 (2026-08-21) — business cases & decyzje

Katalog 15 klasycznych cases przemysłowych (FinServ backbone, proof-of-delivery, webhook dispatch, PSP callback bus, omnichannel fan-in, fraud/velocity, reconciliation, competing consumers, audit replication, compacted state, delayed actions, identity fan-out, disputes, ops broadcast, clickstream) z verdictami i decyzjami D-1…D-6: [.codex/research/kafka-payment-business-cases-v2.md](../../../.codex/research/kafka-payment-business-cases-v2.md). Fala 1 bez zmian zakresu (cases 1+2, +3 reshaped); nowe elementy w tablicy: `uuidv7()` w T10, Podman-first + auto-create OFF w T02/T03, flagship proof-of-delivery w T11. PG18 OAuth-at-Postgres = osobny follow-up poza tą roadmapą.

## Definition of Done (per fala)

- Filtered backend validation wg AGENTS.md (exclusions `restkit/`, `paymentsupport/`) + nowe `*KafkaIT` zielone lokalnie (Podman).
- `corepack pnpm typecheck && corepack pnpm lint` + wskazane specy PW zielone na żywym stacku (`--kafka`).
- `.kiro/**` nietknięte; wpisy statusu w `.codex/current-state.md` / `status/index.md`.
