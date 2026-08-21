---
name: kafka-event-streaming-lab-infra
parent: kafka-event-streaming-lab
last_updated: 2026-08-21
---

# 01 — Infra: broker overlay, Testcontainers, Flyway, flagi

## Compose overlay (opcjonalny, domyślny stack bez zmian)

`infra/compose/compose.kafka.yml` — tylko serwis brokera + opcjonalne UI operatora:

- Obraz `apache/kafka` 4.x KRaft combined (dokładny tag pinowany na T01 vs Testcontainers 2.0.5; bez ZooKeeper — Kafka 4 go usunęła).
- PLAINTEXT; dual listeners: host `localhost:9092`, sieć compose `payment-quality-kafka:19092` (advertised listeners).
- `AKHQ`/`kafka-ui` wyłącznie w tym overlay (nauka operatora), nigdy w `--app`.
- Uruchomienie: `docker compose --env-file infra/compose/.env -f infra/compose/compose.yml -f infra/compose/compose.kafka.yml up -d`.

## dev-stack.sh — trzeci tryb

`--kafka` = obecny tryb host-hybrid + broker z overlay + `app.event-lab.enabled=true` dla Springa. **Nie** łączymy z `--app`/`--full` (live POM Wave 2 zostaje Kafka-free). Hasła/parametry tylko przez env.

## Backend

| Element | Decyzja |
|---|---|
| Moduł | `lab.paymentquality.eventlab` — NIE OPEN; publiczne API minimalne (DTO read modelu), reszta w `internal` |
| Deps | `spring-modulith-events-kafka` (+ przechodnio spring-kafka) — wersja z BOM Modulith 2.0.6; żadnych Namastack/JobRunr |
| Externalizacja | Bean `EventExternalizationConfiguration` w eventlab: select po typie `AuditableActionOccurred`, mapping → koperta v1, headers wg FR-KAFKA-03, routeKey=`targetId`; bean pod `@Profile("kafka")` + `@ConditionalOnProperty(app.event-lab.enabled)` |
| Konsumenci | `@KafkaListener` group `eventlab-inspector` (idempotentny zapis processed) i późniejszy webhook-dispatcher; retry `@RetryableTopic` tylko tutaj; DLT `lab.event-lab.dlq.v1`; `auto.offset.reset=earliest` |
| eventId | Nowe pole UUID na rekordzie + kompatybilna fabryka (nadużywane przez ops frames) |
| Flyway | Nowy folder `db/migration/eventlab`, globalnie **V37** (`V37__create_eventlab_processed.sql`) — V36 zajęte przez tenant; JPA validate |
| Retention | `@Scheduled` purge w eventlab: completed publications + `eventlab_processed` starsze niż `app.event-lab.retention-days` (default 7) |
| Security | Nowe authority `platform:event-lab:operate` addytywnie do PLATFORM_ADMIN/PLATFORM_OPERATOR (wzór `platform:ops:inject`: Authorities + converter allowlist + realm JSON + test katalogu) |
| Testy IT | `KafkaContainerSupport` (singleton container, obraz jak compose) przy `PostgresContainerSupport`; klasyczne `*KafkaIT` w Failsafe; Awaitility pinowana (test scope); surefire excludes `**/*KafkaIT*.java` |

## Frontend

| Element | Decyzja |
|---|---|
| Flaga | `NUXT_PUBLIC_EVENT_LAB_ENABLED` (wzór labów mirror/rls) |
| Strona | `/admin/event-lab` — sibling Error Lab w nav; CSR-only jak pozostałe admin pages |
| Komponenty Nuxt UI | `UTable` (timeline rekordów), `USlideover` (detal: key/headers/payload/status), `UBadge` (outcome/action), `UChip` (partition/offset), `USelect` (group/action/outcome filtry), `UButton`+ConfirmModal (inject), `UAlert` (banner DLQ), stany loading/empty/filtered-empty/error/forbidden |
| Dane | `server/api/event-lab/**` proxy (whitelist query), Zod `app/schemas/event-lab.schema.ts` before-render, composable `useEventLabApi` (kształt `useAuditApi`; detail 404→null) |
| POM | `tests-pom/pages/EventLabPage.ts` (+ widget drawer w `pages/components/` jeśli reuse); fixtures `{ app, api }`; metody `BffClient.eventLab*`; unikalne referencje z `data/factories.ts` |

## Narzędzie TS (opcjonalne, poza apps/frontend)

`tools/kafka-probe` — read-only CLI (kafkajs): list topics, tail `lab.auditable-actions.v1` z key/headers. Do nauki lokalnej; nie jest oracle CI; zero zmian w `apps/frontend/package.json`.

## Kolejność wersji Flyway (stan 2026-08-21)

Max globalnie: **V36** (`tenant/V36__tenant_payment_policy.sql`). Rezerwacja eventu labu: **V37+**. Kolizja = fail fast na starcie Flyway.
