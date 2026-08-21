---
name: event-streaming-lab-tests
parent: kafka-event-streaming-lab
status: DESIGNED_NOT_STARTED
last_updated: 2026-08-21
---

# Test catalog — Event Streaming Lab (Kafka)

Mapa biznes→test dla spec [kafka-event-streaming-lab](../../../.codex/specs/kafka-event-streaming-lab.md). Zasady wspólne: live stack (`--kafka`), brak `page.route`/`routeWebSocket`, unikalne referencje zamiast sleepów, Awaitility ≤10 s, wyłączenia `restkit/`+`paymentsupport/` wg AGENTS.md.

## Narzędzie do testowania Kafki — decyzja (TS vs Java)

| Warstwa | Narzędzie | Dlaczego |
|---|---|---|
| **Primary oracle: broker + bridge + konsumenci** | **Java**: `org.testcontainers:testcontainers-kafka` 2.0.5 (`org.testcontainers.kafka.KafkaContainer`, obraz `apache/kafka`) + spring-kafka test kit + Awaitility; Failsafe `*KafkaIT` | SUT jest po stronie JVM (Modulith externalizer, `@KafkaListener`); repo ma już wzorzec kontenerów (Postgres) i Failsafe; jeden runtime kontenerów = mniej flake'u |
| TypeScript — czy możliwe? | Tak technicznie: `kafkajs` + `@testcontainers/kafka` (black-box read tematu) | Ale jako CI oracle to zła warstwa: duplikuje asercje Java IT, dodaje drugi runtime kontenerów; browser/BFF nie może mówić po Kafce (reguła bezpieczeństwa), więc Playwright nie jest oraclum protokołu |
| **TypeScript — gdzie ma sens** | Opcjonalny read-only probe `tools/kafka-probe` (kafkajs CLI: list topics / tail v1 z key+headers) poza `apps/frontend` | Legalny punkt styku TS dla learnera; zero wpływu na deps/typecheck frontendu |
| Powierzchnia użytkownika | Playwright 1.61 live POM (`tests-pom`) na żywym stacku | Spójne z Ops Wave 2; asercje po widocznych efektach UI/BFF |
| Eksploracja ręczna | AKHQ/kafka-ui tylko w overlayu compose | Nauka operatora, nie CI |

**Werdykt:** najlepsze narzędzie dla tego SUT = Testcontainers Kafka w Javie; TS możliwy, ale jako probe dydaktyczny, nie oracle.

## Business cases (BC)

| ID | Business case | Epik |
|---|---|---|
| BC-KAFKA-01 | Dowód dostarczenia lifecycle paymentu do downstream (proof-of-delivery per paymentOrderId) | E3 |
| BC-KAFKA-02 | Porządek zdarzeń jednej płatności zachowany mimo współbieżności innych | E1/E2 |
| BC-KAFKA-03 | Awaria konsumenta nie gubi danych (at-least-once + crash-heal) | E2 |
| BC-KAFKA-04 | Duplikat dostarczenia nie tworzy duplikatu efektu (idempotencja) | E3 |
| BC-KAFKA-05 | Trująca wiadomość trafia do DLQ bez wpływu na stan biznesowy | E3 |
| BC-KAFKA-06 | Izolacja tenantów na odczycie labu; inject tylko platform | E3 |
| BC-KAFKA-07 | Integracja checkout (PSP-like) przez broker bez utraty HMAC i dedupu | E4 |
| BC-KAFKA-08 | Seeds/lab domyślnie bez brokera; retencja porządkuje dane | E5 |

## Use cases (UC)

| ID | Use case | Aktor |
|---|---|---|
| UC-KAFKA-01 | Authorize → rekord w Event Lab ≤ 5 s | operator |
| UC-KAFKA-02 | Filtr group/action/outcome → otwarcie detalu (key/headers/payload/status) | operator |
| UC-KAFKA-03 | Szukka per paymentOrderId → dokładnie 1 processed row + timestamp/grupa | support/integracje |
| UC-KAFKA-04 | Inject duplicate → nadal 1 row; inject poison → DEAD + banner DLQ | operator |
| UC-KAFKA-05 | Merchant manager: pusta lista/404 cudze; inject 403 | merchant |
| UC-KAFKA-06 | Tail tematu probe'em TS podczas lokalnego labu | learner |

## Macierz testów

### Architektura / moduły (AT)

| ID | Co | Gdzie |
|---|---|---|
| AT-KAFKA-001 | Kontekst startuje bez brokera (flagi domyślne) | Surefire |
| AT-KAFKA-002 | Modulith verify; eventlab nie OPEN; brak internal imports | `ModulithArchitectureTest` rozszerzenia |
| AT-KAFKA-003 | Final boundary sweep eventlab | Failsafe |

### REST Assured + Testcontainers Kafka (RA) — Failsafe `*KafkaIT`

| ID | Asertacja | Story |
|---|---|---|
| RA-KAFKA-001…003 | Broker smoke; tryb --kafka idempotentny; topologia (3 partycje, RF1) | E1-S1..S4 |
| RA-KAFKA-010 | eventId stabilny/unikalny; reuse w ops frames | E2-S1 |
| RA-KAFKA-011 | Flag off ⇒ zero rekordów po authorize | E2-S2 |
| RA-KAFKA-012…015 | Publish-after-commit; key=paymentOrderId; nagłówki kompletne; rollback ⇒ 0 | E2-S3 |
| RA-KAFKA-016 | Crash-heal: incomplete publication → republish po restarcie, dokładnie raz | E2-S4 |
| RA-KAFKA-020…023 | V37 schema/validate; consume PROCESSED; duplicate ⇒ 1 row; replay ⇒ 1 row/grupa | E3-S1 |
| RA-KAFKA-024…026 | Poison→DLT ≤ budget; business row unchanged; retention purge | E3-S2 |
| RA-KAFKA-030…033 | Inject 201 admin / 403 merchant / problem+json; maskowane 404 cross-tenant | E3-S3 |
| RA-KAFKA-040…042 | Checkout: notify→inbox; duplicate⇒DUPLICATE; broker down⇒@Scheduled działa | E4-S1 |
| RA-KAFKA-050…052 | Rebalance (2 instancje grupy, zero duplikatów); seed-guard flag-off; purge granice | E5 |

### Playwright live POM (PW) — `tests-pom`

| ID | Scenariusz | Uwagi |
|---|---|---|
| PW-KAFKA-E2E-001 | Authorize (UI) → rekord z unikalną referencją w `/admin/event-lab` (`expect.poll` ≤5 s) | flaga on, stack --kafka |
| PW-KAFKA-E2E-002 | Capture przez BffClient → timeline row (wzór PW-OPS-E2E-120) | preconditions API |
| PW-KAFKA-E2E-003 | Detal USlideover: key/headers/payload/status zgodny z kopertą v1 | Zod-before-render |
| PW-KAFKA-E2E-004 | Inject duplicate ⇒ nadal 1 row (unikalna referencja) | ConfirmModal pattern |
| PW-KAFKA-E2E-005 | Inject poison ⇒ DEAD + UAlert DLQ banner | bez czekania na realny błąd |
| PW-KAFKA-E2E-006 | Stany: loading/empty/filtered-empty/error/not-found deep-link | sześć stanów jak audit |
| PW-KAFKA-API-001…003 | BFF list po lifecycle POST; forbidden bez authority; detail 404→null | `$fetch.raw` oracles |
| PW-KAFKA-SEC-001…003 | Merchant-scoped: pusta lista/404 cudze; inject 403; brak tokena/adresu brokera w network log | RBAC matrix |

### Vitest (unit)

Schematy Zod koperty v1 (valid/invalid/nadmiarowe pola), `useEventLabApi` mapping (detail 404→null), whitelist query proxy.

## Zakazy

Mockowanie brokera w testach domenowych · `page.route`/`routeWebSocket`/MSW · asercje na globalnym stanie tematu (brak unique group per test) · `Thread.sleep` · broker w Surefire/domyślnym compose · kafkajs w `apps/frontend/package.json`.
