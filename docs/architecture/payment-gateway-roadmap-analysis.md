# Payment Gateway Roadmap Analysis

Data analizy: 2026-05-21

Repozytorium: `kanio1/job-learn`

Główna gałąź źródłowa: `001-project-foundation`

Aktualny vertical slice: Merchant Registry and Activation

Cel: Payment Quality Engineering Lab dla rozwoju kompetencji Senior QA Automation/SDET

## 1. Executive Summary

Rozwój `job-learn` w stronę edukacyjnego systemu PayU-like / PSP / payment gateway jest bardzo dobrym kierunkiem dla nauki Senior QA Automation/SDET.

Warunek: projekt ma pozostać systemem-laboratorium do nauki ryzyk, danych, stanów, kontraktów, asynchroniczności i bezpieczeństwa, a nie próbą zbudowania produkcyjnego operatora płatności.

Aktualny stan repo jest dobrym fundamentem:

- istnieje merchant registry,
- istnieje REST API,
- istnieje walidacja, domena, JPA, Flyway i PostgreSQL,
- istnieje Keycloak/JWT resource server,
- istnieją REST Assured, security tests i Testcontainers,
- istnieje Nuxt dashboard i Playwright,
- istnieje `knowledge-vault`,
- nie ma jeszcze płatności, PSP, Kafki, GraphQL, gRPC, refundów, settlementu ani reconciliation.

To jest właściwy moment na określenie kierunku, ale nie jest jeszcze właściwy moment na dodanie wszystkich technologii naraz.

Najważniejsza rekomendacja:

- iść w kierunku PayU-like lab,
- zacząć od `Payment Order REST API`,
- utrzymać modular monolith first,
- dodawać technologie tylko wtedy, gdy pojawia się realne ryzyko testowe,
- nie zaczynać od Kafki, GraphQL ani gRPC,
- nie rozbijać systemu na mikroserwisy,
- nie udawać produkcyjnego operatora płatności.

Interpretacja PayU-like lab jest teraz jawnie ograniczona do kontrolowanego learning clone: Payment Order REST API/idempotency/lifecycle, security/ownership/tenant isolation oraz reliability/data integrity/audit/webhook/event evolution. Szczegolowa mapa scope znajduje sie w `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Strategy/Three Advanced Learning Paths for API Testing and Payment Lab.md`.

## 2. Czy Kierunek PayU-Like Ma Sens Dla Nauki Senior QA Automation?

Ten kierunek ma bardzo duży sens, ponieważ payment gateway daje naturalne, realistyczne problemy testowe.

| Obszar nauki | Dlaczego PSP/payment gateway jest dobrym labem |
|---|---|
| REST API | Tworzenie płatności, statusy, refundy, błędy, idempotencja i autoryzacja |
| Dane | Transakcje, constrainty, indeksy, audyt i historia statusów |
| Stany | `CREATED`, `AUTHORIZED`, `CAPTURED`, `FAILED`, `REFUNDED` oraz przejścia zabronione |
| Asynchroniczność | Webhooki, eventy, retry, DLQ i eventual consistency |
| Kontrakty | REST contracts, GraphQL schema, Kafka event schema i gRPC proto |
| Security | Role, tenant isolation, merchant ownership, token scopes i denial paths |
| Test data | Merchant-specific data, worker-safe IDs i idempotency keys |
| Observability | Correlation id, logs, traces, metrics i event timeline |
| Ryzyka finansowe | Double charge, lost event, duplicate webhook, stale status i wrong refund |
| Rekrutacja | Pozwala opowiadać o realnych problemach senior QA/SDET |

Perspektywa architekta systemów płatniczych: dobry wybór, bo płatności są domeną o wysokiej wartości błędu.

Perspektywa senior backend Java/Spring engineer: dobry wybór, bo wymusza czystą domenę, transakcje, moduły, API i integracje.

Perspektywa senior QA automation architect: bardzo dobry wybór, bo umożliwia pełną piramidę testów plus testy kontraktowe i event-driven.

Perspektywa test architect dla systemów regulowanych: dobry wybór, jeśli od początku projekt uczy auditability, traceability, least privilege i danych testowych.

Perspektywa mentora: dobry wybór tylko wtedy, gdy każda funkcja ma learning outcome, a nie tylko nową technologię.

Główne ryzyka:

| Ryzyko | Jak je kontrolować |
|---|---|
| Zbyt duży scope | Każda faza musi mieć jedno główne ryzyko edukacyjne |
| Kafka za wcześnie | Najpierw outbox/event model w DB, potem Kafka |
| GraphQL bez sensu | Tylko dla dashboard read model/reporting, nie jako kopia REST |
| gRPC na siłę | Tylko dla internal simulatorów |
| Przerost architektury | Modular monolith first |
| Udawany fintech | Używać realistycznych, ale uproszczonych procesów |
| Brak fundamentów | Przed eventami opanować REST, SQL, stany, idempotencję i security |

## 3. Co Dodać, A Czego Nie Dodawać Jeszcze

| Kategoria | Elementy | Uzasadnienie |
|---|---|---|
| Must-have | `Payment Order`, status lifecycle, idempotency key, merchant eligibility, REST status lookup, domain tests, REST Assured tests | Daje fundament płatności bez rozproszonego chaosu |
| Must-have | Payment status history / event log w DB | Uczy audytu, stanów i SQL |
| Must-have | Security ownership matrix | Uczy tenant isolation i denial-path testing |
| Should-have | Webhook subscriptions i outbound deliveries | Uczy async HTTP, retry i idempotencję odbiorcy |
| Should-have | Refund podstawowy | Uczy powiązania stanów, kwot i constraintów |
| Should-have | Reporting read model | Uczy SQL aggregation, pagination i filtering |
| Advanced | Kafka event pipeline | Uczy ordering, retries, DLQ i schema evolution |
| Advanced | GraphQL dashboard API | Uczy read models, query testing i field-level auth |
| Advanced | gRPC risk/authorization simulator | Uczy proto contracts, deadlines i internal API testing |
| Advanced | Reconciliation, settlement, ledger-lite | Uczy różnic danych, batch processing i finansowej integralności |
| Out of scope | Realne karty, PAN, PCI, 3DS, real PSP integration | Zbyt duże, ryzykowne i niepotrzebne edukacyjnie teraz |
| Out of scope | Mikroserwisy od początku | Zabiją naukę domeny i testowania przez operational complexity |
| Out of scope | Pełny PayU clone | Celem jest laboratorium, nie operator płatności |

## 4. REST Vs GraphQL Vs Kafka Vs gRPC

| Technologia | Gdzie pasuje | Czego uczy | Jakie testy umożliwia | Kiedy za wcześnie | Typowe błędy |
|---|---|---|---|---|---|
| REST | Public/payment-facing API: merchants, payment orders, status, refunds, webhook config | HTTP semantics, status codes, validation, auth i idempotency | REST Assured, security matrix, BVA/EP i contract tests | Przed jasnym modelem domeny | Endpointy CRUD bez procesu, brak idempotencji, złe statusy HTTP |
| GraphQL | Dashboard read model, admin overview, reporting, merchant/payment projections | Schema design, selective reads, field-level auth i N+1 | GraphQL query tests, schema compatibility i authorization tests | Gdy nie ma danych do agregacji | GraphQL jako kopia REST, mutacje bez potrzeby, brak limitów |
| Kafka | Payment events, webhook dispatch, audit stream, reconciliation pipeline | Eventual consistency, ordering, retries, DLQ i schema evolution | Testcontainers Kafka, event contract tests i consumer idempotency tests | Przed stabilnym event model i outbox | Eventy jako RPC, brak partition key, brak idempotencji |
| gRPC | Internal risk scoring mock, authorization simulator, currency rates, settlement calculator | Proto contracts, deadlines, status codes i service-to-service testing | Contract/integration tests, mock server tests i timeout tests | Gdy nie ma internal boundary | Public gRPC dla UI, brak timeoutów, REST mental model w proto |

Najważniejsza kolejność:

1. REST jako podstawowy kanał zachowania.
2. PostgreSQL event/status history jako źródło prawdy.
3. Webhooki jako pierwszy async boundary.
4. Kafka dopiero po zrozumieniu outbox/eventów.
5. GraphQL dopiero, gdy dashboard ma realne agregacje.
6. gRPC dopiero, gdy istnieje sensowny internal simulator.

## 5. Proponowana Architektura Modułowa

Docelowo projekt powinien pozostać w modelu Spring Modulith modular monolith first, z symulowanymi zewnętrznymi usługami tam, gdzie to daje wartość testową.

Nie należy rozbijać wszystkiego na mikroserwisy. Moduły powinny powstawać dopiero, gdy mają realne zachowanie.

| Moduł | Odpowiedzialność | API/dane/eventy | Testy | Ryzyka | Obsidian |
|---|---|---|---|---|---|
| `merchant` | Rejestr i lifecycle merchantów | `merchants`, `MerchantActivated` | Domain, REST, security, repository | Zły status merchant przed payment | Merchant eligibility model |
| `payment` | Payment order i lifecycle | `payment_orders`, `payment_events` | State transition, REST, idempotency | Double charge, invalid transition | Payment lifecycle |
| `checkout` | Payment session / redirect flow | `payment_sessions` | REST + Playwright | Session expiry, wrong merchant | Checkout journey |
| `paymentmethod` | Symulowane metody płatności | `payment_methods`, tokenized mock refs | Domain + API | Udawanie realnych kart | Payment method simulation |
| `authorization` | Symulacja autoryzacji płatności | `payment_attempts`, `PaymentAuthorized` | Integration, failure matrix | Flaky external decision | Authorization simulation |
| `risk` | Mock risk review/scoring | Risk decision, reason codes | Decision tables, gRPC later | False approve/decline | Risk review testing |
| `webhook` | Outbound merchant notifications | `webhook_subscriptions`, `webhook_deliveries` | Retry, idempotency, WireMock | Duplicate/lost notification | Webhook reliability |
| `refund` | Refund request/lifecycle | `refunds`, `RefundCompleted` | State + amount tests | Refund większy niż captured amount | Refund model |
| `settlement` | Batch closing / payable amounts | `settlement_batches/items` | SQL, batch tests | Wrong totals | Settlement basics |
| `reconciliation` | Compare expected vs observed records | `reconciliation_records` | Data quality tests | Missing/mismatched record | Reconciliation stories |
| `audit` | Immutable business audit trail | `audit_log` | Append-only tests | Missing traceability | Auditability |
| `notification` | Email/admin notification mock | Notification records | Async tests | Noisy or missing notifications | Notification vs webhook |
| `reporting` | Dashboard read models | Projections/views | SQL/GraphQL tests | Wrong aggregation/authorization | Reporting read models |

Moduły, które powinny zostać w modular monolith:

- `merchant`,
- `payment`,
- `checkout`,
- `refund`,
- `webhook`,
- `settlement`,
- `reconciliation`,
- `audit`,
- `reporting`.

Elementy, które można symulować jako osobny mock/service:

- risk scoring service,
- payment authorization simulator,
- currency/rates service,
- settlement calculator,
- external merchant webhook endpoint.

## 6. Roadmapa Faz

| Faza | Cel biznesowy | Cel edukacyjny | Technologie | Implementować | Nie implementować jeszcze | Testy i ryzyka | Vault/interview |
|---|---|---|---|---|---|---|---|
| Phase 2: Payment Order REST API | Merchant może utworzyć payment order | REST, DTO, validation, idempotency basics | Spring MVC, JPA, Flyway, REST Assured | `payment_orders`, `POST /api/payment-orders`, `GET status` | Kafka, PSP, card auth | Validation, merchant active check, duplicate idempotency key | “How I test payment creation safely” |
| Phase 3: Payment Lifecycle State Machine | Payment przechodzi przez kontrolowane stany | State transition testing | Java domain, JPA optimistic locking | `CREATED -> AUTHORIZED -> CAPTURED/FAILED` | Refunds, settlement | Invalid transitions, concurrency | “State machine testing in payments” |
| Phase 4: PostgreSQL Querying and Reporting Basics | Operator widzi listy i agregacje | SQL od zera | PostgreSQL, indexes | Filters, pagination, simple reports | GraphQL | Query correctness, index awareness | SQL learning notes |
| Phase 5: Webhook Receiver and Outbound Notifications | Merchant dostaje status update | Async HTTP, retry | REST, WireMock/Testcontainers | Subscriptions, deliveries, retry status | Kafka | Duplicate deliveries, timeout, 5xx retry | Webhook testing story |
| Phase 6: Kafka Event Pipeline | Eventy płatności idą przez stream | Event-driven testing | Kafka, Testcontainers, outbox | Topics, producers, consumers, DLQ | Microservices | Ordering, poison messages, idempotent consumers | Kafka payment events |
| Phase 7: GraphQL Dashboard Read Model | Dashboard pobiera złożone widoki | Schema/query testing | Spring GraphQL | Queries for payments, merchants, reports | GraphQL mutations for payments | Field auth, N+1, schema evolution | GraphQL vs REST |
| Phase 8: gRPC Risk/Authorization Simulator | Internal service decyduje o auth/risk | Contract testing gRPC | Protobuf, grpc-java | Risk score / auth simulator | Public gRPC | Deadlines, error mapping, retries | gRPC tester notes |
| Phase 9: Refunds and Reconciliation | Refund i porównanie records | Financial data integrity | SQL, batch jobs | Refunds, reconciliation records | Chargebacks | Amount constraints, mismatch detection | Refund/recon interview story |
| Phase 10: Observability, Audit, Reliability | System jest diagnozowalny | Production-like QA thinking | Logs, metrics, traces, audit | Correlation, audit log, failure scenarios | Full compliance platform | Observability assertions, chaos-lite | Regulated systems testing |

## 7. Strategia Testów

| Typ testu | Gdzie stosować | Przykład | Ryzyko wykrywa | Teraz czy później |
|---|---|---|---|---|
| Unit tests | Value objects, status enums, amount rules | Invalid currency/amount | Błędne reguły domenowe | Must-have |
| Domain tests | Payment lifecycle, refund lifecycle | `AUTHORIZED -> CREATED` forbidden | Nielegalne przejścia | Must-have Phase 2/3 |
| Application service tests | Orchestration | Create payment for suspended merchant | Brak reguł między modułami | Must-have |
| Module tests | Spring Modulith module boundary | Payment nie dotyka merchant internals | Coupling | Must-have |
| Repository tests | JPA/Flyway/constraints | Duplicate idempotency key | Niespójność DB | Must-have |
| REST Assured | Public REST behavior | Create payment, get status | API contract regression | Must-have |
| Security tests | Role/ownership matrix | Merchant A cannot read merchant B payment | Tenant breach | Must-have |
| State transition tests | Payments/refunds | Captured payment cannot fail | Financial state corruption | Must-have |
| BVA/EP | Amount, display names, references | Amount 0, min, max | Walidacja brzegów | Must-have |
| Decision tables | Risk/payment authorization | Amount/currency/merchant status decisions | Wrong decision combinations | Should-have |
| Pairwise | Payment method/currency/status/channel | Combinations across dimensions | Combinatorial gaps | Should-have |
| Property-based | Money/status invariants | Total refunded <= captured amount | Hidden edge cases | Later |
| Mutation testing | Domain and validation rules | Kill mutants in amount/status logic | Weak assertions | Later |
| SQL/data quality tests | Reports/reconciliation | Sum totals by merchant/date | Wrong aggregation | Should-have |
| Idempotency tests | Create payment/refund/webhook | Same key returns same result | Duplicate charge | Must-have |
| Kafka tests | Producer/consumer/outbox | Event eventually consumed once | Lost/duplicate events | Later |
| Retry/DLQ tests | Webhook/Kafka | Poison event goes to DLQ | Infinite retry/loss | Later |
| GraphQL tests | Dashboard queries | Field hidden without role | Data leak/N+1/schema break | Later |
| gRPC contract tests | Risk/auth simulator | Deadline exceeded mapping | Unstable internal integration | Later |
| Playwright E2E | Critical journeys | Create merchant/payment, inspect status | Broken user flow | Must-have selectively |
| Observability assertions | Correlation/log/audit | Request id appears in response/audit | Poor diagnosability | Should-have |

Zasada: nie każdy scenariusz przez UI. UI ma testować najważniejsze journey i integrację, a większość wariantów powinna żyć w API/domain/repository tests.

## 8. PostgreSQL / SQL Learning Path

Proponowany model danych:

| Tabela | Cel | PK | Kluczowe kolumny | Constrainty/indeksy | SQL do nauki | Ryzyka testowe |
|---|---|---|---|---|---|---|
| `merchants` | Istniejący rejestr | `merchant_id UUID` | `normalized_reference`, `status`, `version` | Unique reference, status check | `SELECT WHERE status` | Duplicate merchant, wrong lifecycle |
| `payment_orders` | Główna płatność | `payment_order_id UUID` | `merchant_id`, `amount_minor`, `currency`, `status`, `idempotency_key` | FK merchant, unique merchant+idempotency, amount > 0 | Joins merchant/payment | Duplicate charge, invalid amount |
| `payment_attempts` | Próby autoryzacji | `attempt_id UUID` | `payment_order_id`, `attempt_no`, `result`, `reason_code` | FK, unique order+attempt_no | Order attempts by time | Lost attempt, wrong retry |
| `payment_events` | Historia stanu | `event_id UUID` | `payment_order_id`, `event_type`, `occurred_at`, `payload` | FK, index order+time | Timeline query | Missing audit/status gap |
| `payment_methods` | Symulowane metody | `payment_method_id UUID` | `type`, `token`, `status` | No PAN, token unique | Filter by type | Leaking sensitive data |
| `refunds` | Zwroty | `refund_id UUID` | `payment_order_id`, `amount_minor`, `status`, `idempotency_key` | Amount > 0, unique key | Sum refunds | Refund over captured |
| `webhook_subscriptions` | Konfiguracja callbacków | `subscription_id UUID` | `merchant_id`, `url`, `event_type`, `enabled` | FK, URL validation | Active subscriptions | Wrong merchant target |
| `webhook_deliveries` | Próby wysyłki | `delivery_id UUID` | `event_id`, `attempt_no`, `status`, `response_code` | FK, unique event+attempt | Retry history | Duplicate/missing delivery |
| `settlement_batches` | Zamknięcie okresu | `batch_id UUID` | `merchant_id`, `period_start`, `period_end`, `status` | No overlapping closed period | Group by period | Wrong settlement window |
| `settlement_items` | Pozycje batcha | `item_id UUID` | `batch_id`, `payment_order_id`, `amount_minor` | FK, unique payment in batch | Sum by batch | Duplicate settlement |
| `reconciliation_records` | Porównanie oczekiwane/otrzymane | `record_id UUID` | `source`, `external_ref`, `amount_minor`, `match_status` | Unique source+external_ref | Find mismatches | Silent mismatch |
| `audit_log` | Ślad audytowy | `audit_id UUID` | `actor`, `action`, `resource_type`, `resource_id`, `correlation_id` | Append-only discipline, indexes | Audit by resource | Missing traceability |

Ścieżka SQL od zera:

1. `SELECT`, `WHERE`, `ORDER BY`, `LIMIT`: listy paymentów i merchantów.
2. `JOIN`: payment z merchantem, refund z paymentem.
3. `GROUP BY`: suma płatności per merchant/currency/status.
4. `constraints`: `CHECK amount_minor > 0`, `UNIQUE idempotency_key`.
5. `indexes`: status lookup, merchant/date reports, event timeline.
6. `transactions`: create payment + event atomically.
7. `isolation`: concurrent idempotent create.
8. `optimistic locking`: status update collision.
9. `idempotency keys`: duplicate request returns stable response.
10. `EXPLAIN`: dlaczego raport może być wolny.

## 9. GraphQL Learning Path

GraphQL ma sens później jako read-model API dla dashboardu, nie jako zamiennik REST do komend płatniczych.

Sensowne queries:

- `merchantOverview(merchantId)`,
- `paymentOrder(id)`,
- `paymentOrders(filter, page)`,
- `paymentStatusTimeline(paymentOrderId)`,
- `settlementReport(merchantId, period)`,
- `reconciliationDifferences(filter)`,
- `webhookDeliveries(paymentOrderId)`.

Mutations:

- raczej nie na początku,
- komendy finansowe powinny zostać w REST,
- ewentualnie później admin-only mutation typu `retryWebhookDelivery`.

Testować:

- valid query,
- invalid query,
- unauthorized field,
- merchant ownership,
- schema compatibility,
- pagination/filtering,
- N+1 query risk,
- overfetching/underfetching,
- error shape,
- nullability.

Porównanie z REST:

| REST | GraphQL |
|---|---|
| Lepszy do komend i public API | Lepszy do dashboardów i projekcji |
| Prostsza autoryzacja endpointów | Trudniejsza field-level auth |
| Łatwiejsze contract tests HTTP | Lepsze schema evolution tests |
| Naturalny dla `POST /refunds` | Naturalny dla overview/reporting |

## 10. Kafka Learning Path

Kafka powinna wejść dopiero po tym, jak w DB istnieją sensowne eventy i outbox.

Topics edukacyjne:

- `payment.merchant.events`,
- `payment.order.events`,
- `payment.refund.events`,
- `payment.webhook.dispatch`,
- `payment.settlement.events`,
- `payment.audit.events`,
- `payment.dead-letter`.

Event naming:

- czas przeszły,
- biznesowe znaczenie,
- stabilny schema version,
- correlation id,
- aggregate id,
- occurred at.

Przykładowe eventy:

| Event | Producer | Consumer | Testy | Ryzyka |
|---|---|---|---|---|
| `MerchantActivated` | `merchant` | `payment`, `audit`, `reporting` | Event emitted once | Payment enabled for wrong merchant |
| `PaymentOrderCreated` | `payment` | `audit`, `webhook`, `reporting` | Outbox + consumer | Lost creation event |
| `PaymentAuthorized` | `authorization`/`payment` | `webhook`, `settlement` | Ordering by payment id | Captured before authorized |
| `PaymentCaptured` | `payment` | `settlement`, `reporting` | Idempotent consumer | Duplicate settlement |
| `PaymentFailed` | `payment` | `webhook`, `reporting` | Failure reason preserved | Wrong customer status |
| `RefundRequested` | `refund` | `payment`, `audit` | Amount invariant | Refund over captured |
| `RefundCompleted` | `refund` | `settlement`, `webhook` | Consumer retry | Missing merchant notification |
| `WebhookDeliveryRequested` | `webhook` | Webhook dispatcher | Retry/DLQ | Duplicate callbacks |
| `SettlementBatchClosed` | `settlement` | Reporting/audit | Batch totals | Financial mismatch |

Najważniejsze koncepty do ćwiczenia:

- partition key: zwykle `paymentOrderId` lub `merchantId`,
- ordering: gwarantowany tylko w partycji,
- idempotent consumers,
- at-least-once jako realistyczny default,
- exactly-once jako temat do omówienia, nie magiczne rozwiązanie,
- retries i DLQ,
- poison messages,
- consumer lag,
- transactional outbox,
- schema evolution.

## 11. gRPC Learning Path

gRPC ma sens jako internal service-to-service API, nie publiczne API dla UI.

Dobre kandydaty:

| Service | Po co | Przykład request/response | Testy |
|---|---|---|---|
| `RiskScoringService` | Decyzja risk | Amount, merchant, country -> score, decision, reason | Decision table, deadlines |
| `PaymentAuthorizationSimulator` | Symulowana autoryzacja | Payment id, amount, method token -> approved/declined | Success/decline/timeout |
| `CurrencyRateService` | Kursy walut do raportów | Base, target, date -> rate | Contract + stale data |
| `SettlementCalculatorService` | Wyliczenie batch totals | Items -> net/gross/fees | Data integrity |

Co testować:

- proto contract compatibility,
- required/optional semantics,
- error handling przez gRPC status codes,
- deadlines/timeouts,
- retries,
- unavailable service,
- malformed response from mock,
- mapping gRPC error na domenową decyzję,
- contract tests między klientem a serwerem,
- Testcontainers lub mock server dla integracji.

Różnica dla testera:

| REST | gRPC |
|---|---|
| Łatwy ręczny debug HTTP/JSON | Wymaga proto tooling |
| Statusy HTTP | Statusy gRPC |
| Tekstowe payloady | Binary protobuf |
| Public API friendly | Internal low-latency contracts |
| REST Assured | gRPC client tests / contract tests |

## 12. Playwright Learning Path

Nuxt dashboard warto rozwijać, ale UI nie powinno testować wszystkich wariantów domenowych.

Rozsądne ekrany:

- merchant dashboard,
- payment order list,
- payment detail,
- status timeline,
- refund action,
- webhook delivery status,
- settlement report,
- reconciliation differences.

Co testować przez mocki:

- UI loading/error states,
- empty states,
- form validation,
- network failure,
- component-level edge cases.

Co testować z real backend:

- login/session,
- create merchant,
- create payment order,
- payment detail/status,
- refund happy path,
- authorization denial,
- merchant isolation.

Organizacja Playwright:

- fixtures per role,
- API-assisted setup,
- worker-aware test data,
- unique merchant/payment references,
- storage state for auth,
- cleanup only if needed, but prefer isolated data,
- UI assertions for user-visible outcomes,
- API assertions for backend truth.

Kiedy UI, a kiedy API:

| Testować przez UI | Testować przez API |
|---|---|
| Krytyczna ścieżka użytkownika | Walidacja wariantów |
| Auth redirect/session | Status transitions |
| Widoczność statusów | Idempotency |
| Refund action smoke | Amount/currency edge cases |
| Dashboard rendering | Security matrix |

## 13. Obsidian Vault Expansion Plan

Proponowana rozbudowa `knowledge-vault`:

| Notatka/folder | Cel | Lokalizacja | Sekcje | Linkować do |
|---|---|---|---|---|
| `REST API From Zero` | Nauka HTTP/API | `02 Areas/Technical Learning/REST API From Zero/` | Methods, status codes, validation, idempotency | Controllers, REST Assured tests |
| `Java 25 for Payment Systems` | Java przez domenę płatności | `02 Areas/Technical Learning/Java/` | Records, enums, time, money, immutability | Value objects, domain tests |
| `Spring Web MVC` | Request lifecycle | `02 Areas/Technical Learning/Spring/` | Controller, validation, exception handling | Web package |
| `Spring Data JPA and Flyway` | Persistence learning | `02 Areas/Technical Learning/Persistence/` | Entity, repository, migration, locking | JPA tests, migrations |
| `PostgreSQL and SQL from Zero` | SQL krok po kroku | `02 Areas/Technical Learning/PostgreSQL/` | SELECT, JOIN, GROUP BY, indexes, transactions | Migrations, repo tests |
| `Payment Domain Modeling` | Domena płatności | `02 Areas/Business Product and Testing Thinking/` | Order, attempt, status, refund, settlement | Specs and domain classes |
| `GraphQL Testing` | Schema/query testing | `02 Areas/Technical Learning/GraphQL/` | Queries, auth, N+1, schema evolution | Future GraphQL tests |
| `Kafka Testing` | Event-driven testing | `02 Areas/Technical Learning/Kafka/` | Topics, outbox, DLQ, idempotency | Future Kafka tests |
| `gRPC Testing` | Internal contract testing | `02 Areas/Technical Learning/gRPC/` | Proto, deadlines, errors, mocks | Future proto/tests |
| `Playwright TypeScript` | UI automation design | `02 Areas/Technical Learning/Playwright/` | Fixtures, data setup, auth, isolation | E2E tests |
| `Security and Authorization Testing` | Role/ownership matrix | `02 Areas/Technical Learning/Security/` | JWT, scopes, tenant isolation, 401/403 | Security tests |
| `Risk-Based Testing for Payment Systems` | Test thinking | `02 Areas/Business Product and Testing Thinking/` | Product risks, heuristics, charters | Test design docs |
| `Interview Capital - Payment Gateway Stories` | Rekrutacyjne historie | `02 Areas/Interview Capital/` | STAR stories, tradeoffs, failures | Every phase summary |

Dodatkowo każda faza powinna mieć hub:

- `01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Order/`,
- `01 Projects/Payment_Quality_Engineering_Lab/03 Phase 3 - Payment Lifecycle/`,
- `01 Projects/Payment_Quality_Engineering_Lab/04 Phase 4 - SQL Reporting/`.

## 14. Ryzyka I Antywzorce

Najważniejsze antywzorce do unikania:

| Antywzorzec | Dlaczego szkodzi |
|---|---|
| Kafka before database events | Uczysz się narzędzia, nie event-driven design |
| GraphQL jako kopia REST | Brak realnej wartości edukacyjnej |
| gRPC dla UI | Zły boundary i zła historia architektoniczna |
| Mikroserwisy za wcześnie | Operational complexity przed domeną |
| Puste moduły Spring Modulith | Dekoracyjna architektura |
| Payment bez idempotency | Nierealistyczny payment gateway |
| Refund bez constraintów kwotowych | Niebezpieczny model finansowy |
| Brak audit trail | Słaba opowieść o systemach regulowanych |
| Wszystko przez Playwright | Wolne, kruche, złe pokrycie |
| Brak ownership tests | Największe ryzyko security w multi-tenant systemie |
| Realne karty/PAN | Niepotrzebne i niebezpieczne edukacyjnie |
| Full PayU clone | Rozmycie celu nauki |

## 15. Branching Strategy

Analiza i przyszłe prace powinny być bezpieczne dla `001-project-foundation`.

Nie należy zmieniać `001-project-foundation` bez wyraźnej decyzji.

Jeżeli ta analiza ma zostać rozwijana jako osobna praca, rekomendowana gałąź to:

- `learn/payment-gateway-roadmap-analysis`

Dla przyszłych lekcji rekomendowane gałęzie:

- `learn/rest-payment-order`,
- `learn/graphql-dashboard-read-model`,
- `learn/kafka-payment-events`,
- `learn/grpc-risk-simulator`,
- `learn/postgresql-payment-queries`,
- `learn/playwright-payment-dashboard`.

Nie tworzyć PR i nie mergować do `001-project-foundation` bez wyraźnej prośby.

## 16. Pierwsze 3 Konkretne Kroki

1. Utworzyć później gałąź `learn/payment-gateway-roadmap-analysis` tylko dla dokumentacji roadmapy, jeśli decyzją będzie utrwalenie i rozwijanie tej analizy w repo.
2. Przygotować Business Analysis Discovery Pack dla `Payment Order REST API`, zanim powstanie spec implementacyjny.
3. Zacząć implementacyjną naukę od gałęzi `learn/rest-payment-order`, z zakresem: `payment_orders`, idempotency key, merchant active check, status `CREATED`, REST Assured, repository tests, security matrix i notatka w `knowledge-vault`.

## 17. Rekomendacja Końcowa

Warto iść w kierunku edukacyjnego PayU-like / PSP / payment gateway lab.

Warunki:

- modular monolith first,
- każda technologia ma uzasadnienie testowe,
- każda faza uczy jednego głównego ryzyka,
- najpierw REST, domena, SQL, stany, idempotencja i security,
- dopiero później Kafka, GraphQL i gRPC,
- nie budujemy produkcyjnego PSP,
- nie kopiujemy PayU 1:1,
- nie dodajemy płatności bez specyfikacji i test designu.

Rekomendowany start:

- `Payment Order REST API`,
- model `payment_orders`,
- idempotency key,
- lifecycle minimum,
- merchant eligibility,
- REST Assured/security/repository/domain tests,
- SQL learning notes.

Błędem na tym etapie byłoby:

- dodać Kafkę przed modelem eventów,
- dodać GraphQL przed dashboard read model,
- dodać gRPC bez internal service boundary,
- rozbić system na mikroserwisy,
- implementować PSP/card/real payment flows,
- tworzyć duży system bez konsekwentnego `knowledge-vault` i historii rekrutacyjnych.

Najlepsza definicja celu: `job-learn` powinien stać się realistycznym, ale kontrolowanym laboratorium jakości płatności, w którym nauka Senior QA Automation/SDET odbywa się przez ryzyka finansowe, dane, stany, API, eventy, security i obserwowalność.
