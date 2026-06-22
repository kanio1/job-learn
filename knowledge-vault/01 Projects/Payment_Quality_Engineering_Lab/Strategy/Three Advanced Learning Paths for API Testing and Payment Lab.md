---
type: strategy-discovery
status: ready
project: Payment Quality Engineering Lab
date: 2026-05-22
tags:
  - strategy
  - discovery
  - business-analysis
  - senior-qa-automation
  - sdet
  - rest-api-testing
  - payment-quality-lab
---

# Three Advanced Learning Paths for API Testing and Payment Lab

## Cel notatki

Ta notatka proponuje trzy alternatywne, ale kompatybilne sciezki rozwoju Payment Quality Engineering Lab. Celem nie jest szybkie dodanie technologii, tylko zbudowanie realistycznych scenariuszy testowych, ktore da sie wyjasnic na rozmowie Senior QA Automation/SDET.

## Przeanalizowany kontekst

Przed synteza przeanalizowano:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
- `docs/architecture/payment-gateway-roadmap-analysis.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/README.md`
- `knowledge-vault/02 Areas/Technical Learning/REST API From Zero/Merchant Request and Response Flow.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/README.md`
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/README.md`
- `knowledge-vault/02 Areas/Technical Learning/Backend Testing Review/README.md`
- `knowledge-vault/02 Areas/Technical Learning/PostgreSQL and SQL From Zero/README.md`
- `knowledge-vault/02 Areas/Technical Learning/Security and Authorization Testing/README.md`
- `knowledge-vault/02 Areas/Technical Learning/Spring Boot Spring MVC/README.md`
- `knowledge-vault/02 Areas/Technical Learning/Spring Data JPA and Flyway/README.md`
- aktualny kod Merchant Registry na poziomie orientacji: REST Assured tests, security tests, JWT support, controller, service, domain entity, Flyway migration i `SecurityConfig`
- aktualny Spec Kit plan Phase 1 Merchant Registry and Activation

## Aktualny punkt startowy

Obecny fundament jest dobry do dalszej nauki, ale trzeba go nazywac precyzyjnie.

Mamy juz:

- Merchant Registry jako pierwszy biznesowy vertical slice.
- REST API dla merchantow: create, get, list, activate, suspend.
- Spring Security JWT resource server i role/authorities.
- Testy 401, 403, partial authorities, invalid token, expired token.
- REST Assured tests dla happy path, validation, duplicate, not found, malformed UUID i invalid transitions.
- PostgreSQL/Flyway/JPA z `merchants`, unique constraint, check constraint, indeksami i `version` column.
- Nuxt dashboard i selektywne Playwright journeys.
- Vault learning notes dla REST, REST Assured, SQL, Security, Spring MVC, JPA/Flyway.

Nie mamy jeszcze:

- Payment Order.
- Realnego payment resource z ownership/tenant isolation.
- `Idempotency-Key` i kontraktu retry dla create payment.
- Webhookow, outbox/event history, Kafki.
- GraphQL, gRPC.
- PSP mock flow, realnych kart, PAN, PCI, settlement, reconciliation.

## Guardrails

- Modular monolith first.
- Najpierw Business Analysis Discovery Pack, potem Spec Kit input, dopiero potem implementacja.
- Nie dodawac `POST /payments` ani Payment Order bez osobnego discovery/specification.
- Nie dodawac Kafki przed DB event history/outbox i realnym event model.
- Nie dodawac GraphQL jako kopii REST.
- Nie dodawac gRPC bez realnej internal boundary.
- Nie dodawac realnych kart, PAN, PCI, 3DS ani real PSP integration.
- Nie robic wszystkiego przez Playwright.
- Nie dawac future `payment` code dostepu do `merchant.internal`.

## Kontrolowany PayU-like Clone Scope

W tym projekcie "klon PayU" oznacza **edukacyjny PayU-like learning clone**, a nie probe odtworzenia PayU jako produktu, operatora platnosci albo rzeczywistego PSP. Nazwa ma pomagac w wyborze realistycznych ryzyk testowych, ale zakres ma pozostac ograniczony do trzech sciezek opisanych w tej notatce.

### Co klonujemy koncepcyjnie

Klonujemy tylko te fragmenty myslenia o bramce platniczej, ktore ucza Senior QA Automation/SDET:

- merchant jako wlasciciel lub kontekst zasobow platniczych,
- payment order jako zasob REST reprezentujacy zamiar platnosci,
- idempotentne tworzenie zasobu, zeby uniknac double charge risk,
- status/lifecycle platnosci jako zrodlo state transition testing,
- role, permissions, ownership i tenant isolation,
- PostgreSQL jako zrodlo integralnosci: FK, unique, check constraints, indexes, transactions, optimistic locking,
- correlation IDs, request IDs, audit/status history i traceability,
- webhook/retry/outbox jako pozniejsza sciezka reliability,
- kontrakty HTTP: methods, headers, status codes, content negotiation i versioning.

### Czego nie klonujemy

Nie klonujemy elementow, ktore zwiekszaja scope bez proporcjonalnej wartosci edukacyjnej na tym etapie:

- realnych kart, PAN, PCI, 3DS ani tokenizacji kart,
- realnej integracji z bankiem, acquirerem, PSP albo schematem kartowym,
- pelnego checkoutu konsumenckiego,
- pelnego merchant panelu self-service,
- chargebackow, settlementu, reconciliation i ledgerow jako najblizszego kroku,
- mikroserwisow,
- Kafki przed outbox/event history,
- GraphQL jako kopii REST,
- gRPC bez internal simulator boundary.

### Mapa PayU-like capability do trzech sciezek

| PayU-like capability | Wchodzi do scope? | Sciezka | Po co testowo |
|---|---|---|---|
| Merchant Registry | Juz istnieje jako fundament | Prerequisite | Role, lifecycle, unique data, REST baseline. |
| Payment Order create/read | Tak, jako pierwszy nowy product slice | Sciezka 1 | REST contract, `201`, `Location`, validation, idempotency. |
| Payment amount/currency | Tak, minimalnie | Sciezka 1 | EP/BVA, domain value objects, DB check constraints. |
| Payment lifecycle/status | Tak, stopniowo | Sciezka 1 | State transition testing, optimistic locking, invalid transitions. |
| Idempotency key | Tak, early | Sciezka 1 | Retry-safe create, duplicate charge prevention, unique constraint. |
| Role-based access | Tak, juz zaczete | Sciezka 2 | 401/403 matrix, authority separation. |
| Ownership/tenant isolation | Tak, po Payment Order | Sciezka 2 | Merchant A cannot read or mutate Merchant B data. |
| JWT/OAuth/Keycloak learning | Tak, kontrolowanie | Sciezka 2 | Token validity, expiry, issuer, roles, no token leakage. |
| Correlation ID/request ID | Tak | Sciezka 3 | Diagnosability and traceability. |
| Payment status history/audit | Tak, po minimalnym lifecycle | Sciezka 3 | Append-only evidence, SQL timeline, transaction atomicity. |
| Webhook subscription/delivery | Pozniej | Sciezka 3 | Async HTTP, retries, duplicate delivery, WireMock scenarios. |
| Outbox | Pozniej, przed Kafka | Sciezka 3 | Reliable event publication and eventual consistency. |
| Kafka | Deferred advanced | Sciezka 3 later | Ordering, DLQ, schema evolution, idempotent consumers. |
| GraphQL dashboard read model | Deferred advanced | Later outside core three-path start | Read model queries, field auth, N+1 only when reporting exists. |
| gRPC risk/auth simulator | Deferred advanced | Later outside core three-path start | Internal contract, deadlines, unavailable service. |
| Real PSP/cards/PAN/PCI | No | Out of scope | Too risky and unnecessary for current learning goals. |

### PayU-like narrative for interviews

The interview-safe story is:

> I did not build a production PayU clone. I built a controlled PayU-like payment quality lab focused on backend API testing risks: payment order creation, idempotency, JWT/RBAC, tenant isolation, PostgreSQL constraints, state transitions, auditability, correlation IDs, retries and later webhook/outbox event evolution. I deliberately deferred real card data, PCI, PSP integrations, microservices and Kafka until the domain model justified them.

### Jak to wplywa na kolejnosc pracy

Ten PayU-like scope nie dodaje czwartej sciezki. On doprecyzowuje, ze wszystkie przyszle funkcje maja przechodzic przez filtr:

1. Czy funkcja wzmacnia Payment Order/idempotency/lifecycle?
2. Czy funkcja wzmacnia security/ownership/tenant isolation?
3. Czy funkcja wzmacnia reliability/data integrity/audit/webhook/event evolution?
4. Czy funkcja jest za wczesna, bo wymaga PSP, kart, settlementu, Kafki, GraphQL albo gRPC bez fundamentow?

Jesli odpowiedz na pierwsze trzy pytania brzmi "nie", funkcja nie powinna wejsc do najblizszego scope, nawet jesli brzmi atrakcyjnie jako element PayU clone.

## Sciezka 1: Payment Order REST API, Idempotency And Lifecycle

### 1. Nazwa sciezki

Payment Order REST API, Idempotency And Lifecycle.

### 2. Jednozdaniowa idea

Zbudowac minimalny, kontrolowany payment resource, ktory pozwala cwiczyc prawdziwe REST API testing, kontrakty, idempotencje, walidacje danych, statusy i lifecycle bez PSP, kart i asynchronicznego chaosu.

### 3. Dlaczego ma sens biznesowo

Payment Order jest naturalnym nastepnym krokiem po Merchant Registry, bo aktywny merchant musi miec zasob, ktory reprezentuje zamiar platnosci. Bez Payment Order lab pozostaje systemem administracyjnym, a nie payment labem.

### 4. Dlaczego ma sens edukacyjnie dla Senior QA Automation/SDET

Ta sciezka laczy wszystkie fundamenty rozmowy rekrutacyjnej: HTTP methods, status codes, request/response contract, REST Assured, validation, idempotency, PostgreSQL constraints, transaction boundary, negative tests, security i test data design.

### 5. Funkcje aplikacji krok po kroku

1. BA Discovery Pack dla `Payment Order REST API`.
2. Minimalny model domenowy: `PaymentOrder`, `MoneyAmount`, `CurrencyCode`, `PaymentStatus`, `IdempotencyKey`.
3. `POST /api/payment-orders` dla utworzenia ordera przez uprawnionego callera.
4. `GET /api/payment-orders/{id}` dla odczytu statusu i kontraktu response.
5. `GET /api/payment-orders?merchantId=&status=&limit=` jako kontrolowana lista, dopiero po single-resource flow.
6. Minimalny lifecycle: `CREATED -> AUTHORIZED -> CAPTURED` albo `CREATED -> FAILED`, ale tylko gdy jest juz gotowy state-transition discovery.
7. `PATCH /api/payment-orders/{id}` albo command endpoint dla status transition tylko jesli spec zdecyduje, ze status update jest zewnetrznie widoczna komenda.
8. `DELETE` tylko jako pozniejszy temat anulowania/cancel, nie jako fizyczne usuwanie payment data.

### 6. HTTP methods do cwiczenia

| Method | Sens w tej sciezce |
|---|---|
| `POST` | Create payment order, idempotent retry create, future status command. |
| `GET` | Read payment by ID, list/filter payments. |
| `PUT` | Raczej deferred; moze miec sens dla replace webhook config, nie dla payment create. |
| `PATCH` | Status transition lub metadata update tylko po jasnym kontrakcie optimistic locking. |
| `DELETE` | Deferred; ewentualnie cancel semantics, nie hard delete. |

### 7. HTTP headers do cwiczenia

| Header | Czego uczy |
|---|---|
| `Authorization` | Bearer JWT, 401 vs 403. |
| `Content-Type` | JSON request body contract. |
| `Accept` | Content negotiation basics and 406/415 discussion later. |
| `Location` | `201 Created` powinno wskazac nowy resource. |
| `Idempotency-Key` | Duplicate create protection and retry-safe contract. |
| `X-Correlation-ID` | Traceability create/read/status transition. |
| `X-Request-ID` | Client request identity, possible relation to correlation ID. |
| `ETag` | Later optimistic locking for update/status transition. |
| `If-Match` | Later stale update rejection. |
| API version headers | Later contract evolution, e.g. `Accept: application/vnd.payment-lab.v1+json`. |

### 8. Status codes do cwiczenia

| Status | Przyklad |
|---|---|
| `200` | `GET /api/payment-orders/{id}` returns current state. |
| `201` | New payment order created. |
| `202` | Deferred until async authorization/webhook exists. |
| `204` | Later cancel/update with no body, if product semantics need it. |
| `400` | Malformed UUID, invalid JSON, invalid amount format. |
| `401` | Missing, invalid, expired token. |
| `403` | Token valid, missing permission. |
| `404` | Payment order not found or hidden by ownership policy. |
| `409` | Duplicate conflicting idempotency key, invalid state transition. |
| `412` | Later `If-Match` stale version failure. |
| `422` | Business-valid JSON but semantically unprocessable, if spec chooses this instead of 400/409 for business validation. |
| `429` | Deferred rate limit/retry exercise. |
| `500` | Not a target assertion except generic error hygiene and no sensitive leak. |
| `503` | Deferred integration dependency unavailable. |

### 9. Security risks do cwiczenia

- Missing/invalid/expired token returns `401`.
- Valid token without `payments:create` or `payments:read` returns `403`.
- Platform operator vs future merchant actor separation.
- Active merchant eligibility: only active merchants can create payment orders.
- Ownership is introduced carefully: caller for Merchant A must not read Payment Order for Merchant B.
- Tenant isolation becomes real only when payment orders are owned by merchants.
- Forbidden action: read-only caller cannot create; create-only caller cannot list/read.

### 10. PostgreSQL/data risks do cwiczenia

- FK from `payment_orders.merchant_id` to `merchants.merchant_id`.
- `CHECK amount_minor > 0` and supported currency constraint or reference table.
- Unique constraint on `(merchant_id, idempotency_key)`.
- Indexes for `(merchant_id, created_at DESC)`, `status`, and event timeline later.
- Transaction boundary: create payment order and initial history/event record atomically.
- Optimistic locking through `version` for status transitions.
- Isolation/concurrency: two concurrent creates with same idempotency key.
- Audit/status history: deferred until lifecycle creates meaningful timeline.

### 11. Test design techniques do cwiczenia

- EP: valid/invalid currency, valid/invalid merchant status, valid/invalid idempotency key.
- BVA: amount `0`, minimum positive, maximum supported, scale/decimal precision if represented in major units.
- Decision tables: merchant status x role x amount/currency x idempotency situation.
- State transition: created, authorized, captured, failed, forbidden transitions.
- Pairwise: currency x amount class x merchant status x actor role.
- Exploratory charters: duplicate creates, response consistency, status confusion, error contract clarity.

### 12. Test data patterns do cwiczenia

- Unique payment references and idempotency keys per test/worker.
- API-assisted setup: create merchant, activate merchant, then create payment order.
- Worker-safe data namespacing similar to current merchant references.
- Prefer isolation over cleanup for immutable payment records.
- Purposeful reuse of idempotency key only inside idempotency tests.
- Separate data for happy path, duplicate path, authorization path and ownership path.

### 13. Test layers potrzebne

- Domain tests for money, currency, status transitions and idempotency key validation.
- Service/application tests for merchant eligibility and orchestration.
- Repository tests for FK, unique idempotency, amount check and indexes where relevant.
- REST Assured tests for HTTP contract, status codes, headers and response bodies.
- Security tests for role matrix and 401/403.
- Spring Modulith/module tests for future `payment` module boundary.
- Playwright only for one critical dashboard journey after API stabilizes.

### 14. Przykladowy realistyczny REST Assured scenario test bez implementacji

Scenario: active merchant creates payment order and retries safely.

1. Test creates a unique merchant through existing API or support fixture.
2. Test activates merchant.
3. Test sends `POST /api/payment-orders` with `Authorization`, `Content-Type: application/json`, `Accept: application/json`, `Idempotency-Key` and `X-Correlation-ID`.
4. Expected response is `201`, `Location` header points to `/api/payment-orders/{id}`, body contains payment ID, merchant ID, amount, currency, status `CREATED` and correlation ID is propagated.
5. Test sends the same request with the same `Idempotency-Key`.
6. Expected response is stable according to spec: either `200`/`201` with the same payment ID and body, not a duplicate record.
7. Test sends same `Idempotency-Key` with different amount.
8. Expected response is `409` with a clear idempotency conflict error.
9. Test follows `GET /api/payment-orders/{id}` and verifies the same state.

### 15. Interview stories ta sciezka zbuduje

- I designed REST API tests around payment creation, not around controllers.
- I used idempotency keys to prevent duplicate charge risk.
- I separated API validation, domain invariants and database constraints.
- I tested HTTP contract, DB safety net and security matrix at different layers.
- I explained why payment create returns `201`, duplicate business conflict returns `409`, missing token returns `401`, and forbidden role returns `403`.

### 16. Ryzyka zbyt trudne albo za wczesne

- Idempotency can become complex if the response replay contract is overdesigned.
- Lifecycle can explode if refund, authorization, settlement and PSP mocks enter too early.
- Ownership can be ambiguous until actors and tenant model are clearly specified.
- Optimistic locking should not be added to every endpoint without a stale-update use case.

### 17. Pierwszy maly krok

Prepare BA Discovery Pack for `Payment Order REST API` with explicit non-goals: no PSP, no cards, no Kafka, no webhook, no refund, no settlement, no GraphQL, no gRPC.

### 18. Deferred

- PSP/mock authorization flow.
- Webhooks and retries.
- Outbox and Kafka.
- Refunds, settlement, reconciliation.
- GraphQL read model.
- gRPC risk/authorization simulator.
- Full merchant self-service and Client Credentials Flow.

## Sciezka 2: Security, Ownership And Tenant Isolation Matrix

### 1. Nazwa sciezki

Security, Ownership And Tenant Isolation Matrix.

### 2. Jednozdaniowa idea

Rozwinac lab w kierunku zaawansowanego security testing przez role, scopes/authorities, ownership, tenant isolation i denial-path design, ale dopiero na zasobach, ktore faktycznie maja wlasciciela.

### 3. Dlaczego ma sens biznesowo

Payment gateway musi chronic dane merchantow i platnosci przed dostepem innych merchantow, operatorow bez uprawnien i tokenow o blednym zakresie. Najwiekszy blad security w takim labie to nie brak logowania, tylko cross-tenant data exposure.

### 4. Dlaczego ma sens edukacyjnie dla Senior QA Automation/SDET

Ta sciezka buduje umiejetnosc tworzenia authorization matrix, rozrozniania authentication/authorization/ownership, projektowania 401/403/404, pracy z JWT claims i wyjasniania testow security jako kontraktu biznesowego.

### 5. Funkcje aplikacji krok po kroku

1. Security discovery note: actors, roles, permissions, ownership model.
2. Rozszerzenie obecnej role matrix z Merchant Registry o przyszle payment endpoints.
3. Tenant/merchant identity concept dla payment resources, bez pelnego M2M jeszcze.
4. Payment Order ownership rule: payment belongs to one merchant.
5. Read/list filtering by caller ownership.
6. Denial behavior decision: cross-tenant read returns `403` or `404` according to spec.
7. Token claim mapping review: subject, roles, merchant/tenant claim when needed.
8. Later Keycloak-backed identities for multiple merchant tenants.
9. Later Client Credentials Flow only when merchant-to-API use case exists.

### 6. HTTP methods do cwiczenia

| Method | Sens w tej sciezce |
|---|---|
| `GET` | Read/list owned resources and verify data isolation. |
| `POST` | Create resources under correct ownership. |
| `PATCH` | Update owned metadata/status with role and ownership checks. |
| `PUT` | Replace configuration owned by merchant, e.g. future webhook subscription config. |
| `DELETE` | Disable/delete owned config, not financial records. |

### 7. HTTP headers do cwiczenia

| Header | Czego uczy |
|---|---|
| `Authorization` | JWT bearer token as security boundary. |
| `Content-Type` | Request contract for protected commands. |
| `Accept` | Error response content negotiation. |
| `X-Correlation-ID` | Trace denied requests without leaking secrets. |
| `X-Request-ID` | Client-visible diagnosis for security denials. |
| `ETag` | Versioned reads for owned mutable resources. |
| `If-Match` | Prevent stale ownership/config updates. |
| API version headers | Security contract compatibility between versions. |
| `Location` | Created owned resource should not reveal another tenant. |
| `Retry-After` | Later rate-limit or brute-force protection. |

### 8. Status codes do cwiczenia

| Status | Przyklad |
|---|---|
| `200` | Authorized read of owned resource. |
| `201` | Authorized create under owned merchant/tenant. |
| `202` | Deferred async security-sensitive operation. |
| `204` | Disable owned config without response body. |
| `400` | Missing malformed path/body, invalid tenant claim shape. |
| `401` | Missing, expired, invalid signature, invalid issuer token. |
| `403` | Authenticated but missing authority or forbidden ownership. |
| `404` | Unknown resource or deliberate not-found masking for cross-tenant access. |
| `409` | Ownership conflict or incompatible state for action. |
| `412` | Stale `If-Match` on owned mutable resource. |
| `422` | Semantically invalid security-related command if spec chooses it. |
| `429` | Too many denied attempts or rate-limited client. |
| `500` | Verify no sensitive error leak. |
| `503` | Deferred identity provider/dependency unavailable behavior. |

### 9. Security risks do cwiczenia

- Authentication: no token, malformed token, invalid signature, invalid issuer, expired token.
- Authorization: role missing for endpoint.
- Role drift: token has role that should not grant access.
- Ownership: merchant A cannot read/update merchant B payment.
- Tenant isolation: list endpoint never leaks rows from another tenant.
- Forbidden action: read-only actor cannot mutate; create-only actor cannot read if policy says so.
- Token expiry and clock assumptions.
- Error body leak: denial response must not reveal sensitive resource details.

### 10. PostgreSQL/data risks do cwiczenia

- Every owned resource carries `merchant_id` or explicit tenant ownership column.
- FK prevents orphan payment/order/config records.
- Indexes support tenant-scoped list queries.
- Unique constraints should be tenant-scoped when business keys can repeat across tenants.
- Transaction boundary must not create resource before ownership is validated.
- Audit trail for denied or sensitive actions, but no token logging.
- Isolation: parallel tests for multiple tenants must not share data accidentally.

### 11. Test design techniques do cwiczenia

- Decision tables: endpoint x role x owner x token state x expected status.
- Pairwise: role x resource owner x method x state.
- EP: valid owner, other owner, missing owner, invalid claim.
- BVA: token expiry around now, list limits around tenant filtering.
- State transition: ownership-sensitive status updates only in allowed states.
- Exploratory charters: horizontal privilege escalation, confused-deputy behavior, error message leakage.

### 12. Test data patterns do cwiczenia

- Two or more merchants/tenants per test scenario.
- Unique owned resources per tenant and per worker.
- API-assisted setup with privileged operator only for fixture creation.
- Security actor tokens generated with explicit roles and merchant/tenant claims.
- No shared global tenant fixtures.
- Cleanup usually unnecessary if test data is namespaced and immutable enough.

### 13. Test layers potrzebne

- Security tests at HTTP boundary for 401/403/404 decisions.
- REST Assured tests for list isolation and response contracts.
- Service tests for ownership predicates and authorization-adjacent business rules.
- Repository tests for tenant-scoped queries.
- Module tests for public API boundary between `merchant` and future `payment`.
- Domain tests only where ownership rule is part of domain model.
- Playwright selectively for visible unauthorized dashboard behavior, not full security matrix.

### 14. Przykladowy realistyczny REST Assured scenario test bez implementacji

Scenario: merchant actor cannot read another merchant's payment order.

1. Test creates Merchant A and Merchant B through setup API or fixtures.
2. Test creates one payment order for Merchant A and one payment order for Merchant B.
3. Test obtains token for actor associated with Merchant A and `payments:read` authority.
4. Test sends `GET /api/payment-orders/{merchantBPaymentId}` with Merchant A token and `X-Correlation-ID`.
5. Expected response is either `403` or masked `404`, depending on documented product security decision.
6. Test sends `GET /api/payment-orders?merchantId=merchantB` with Merchant A token.
7. Expected response does not include Merchant B data; error or empty/filtered response follows spec.
8. Test verifies Merchant A can still read its own payment order with `200`.

### 15. Interview stories ta sciezka zbuduje

- I built an authorization matrix that separates authentication, role authorization and ownership.
- I tested cross-tenant isolation at API and data-query levels.
- I can explain when to return `403` versus masked `404`.
- I used multiple test identities and worker-safe tenant data.
- I reviewed JWT claims and Spring authorities without exposing tokens to logs.

### 16. Ryzyka zbyt trudne albo za wczesne

- Full OAuth/OIDC/Keycloak operations can distract from API security testing.
- Client Credentials Flow is premature before merchant M2M API exists.
- Ownership tests are fake if there is no owned payment resource yet.
- Too many roles too early will make the matrix noisy and unlearnable.

### 17. Pierwszy maly krok

Create a security discovery note and authorization matrix template for future Payment Order, explicitly marking which parts current Merchant Registry already covers and which require real payment ownership.

### 18. Deferred

- Full Keycloak client management flows.
- Client Credentials Flow.
- Fine-grained scopes beyond simple authorities.
- Field-level auth and GraphQL security.
- Rate limiting and brute-force protection.
- Complete audit investigation UI.

## Sciezka 3: Reliability, Data Integrity, Audit Trail, Webhooks And Event Evolution

### 1. Nazwa sciezki

Reliability, Data Integrity, Audit Trail, Webhooks And Event Evolution.

### 2. Jednozdaniowa idea

Rozwijac lab od synchronicznego REST w kierunku diagnozowalnosci, historii zdarzen, retry, webhookow i dopiero pozniej Kafki, tak aby kazdy krok mial realne ryzyko testowe.

### 3. Dlaczego ma sens biznesowo

W platnosciach samo utworzenie zasobu nie wystarcza: system musi pokazac, co sie stalo, kiedy, z jakim correlation ID, czy merchant zostal powiadomiony i czy retry nie spowodowal duplikatu.

### 4. Dlaczego ma sens edukacyjnie dla Senior QA Automation/SDET

Ta sciezka buduje zaawansowane kompetencje: observability-aware testing, auditability, SQL event timeline, transaction boundaries, async HTTP, retries, idempotent receiver, eventual consistency i pozniejszy event-driven testing.

### 5. Funkcje aplikacji krok po kroku

1. Correlation ID contract review for existing REST endpoints.
2. Payment status history table once Payment Order exists.
3. Audit trail for state-changing business actions.
4. Outbox table only when there is a real downstream event consumer.
5. Webhook subscription config for merchant notifications.
6. Webhook delivery attempts with status, response code, next retry time and attempt count.
7. Retry policy with `Retry-After` only where client-facing retry semantics are needed.
8. Event schema/version notes in DB/outbox.
9. Kafka only after outbox/event model and webhook/event semantics are stable.

### 6. HTTP methods do cwiczenia

| Method | Sens w tej sciezce |
|---|---|
| `GET` | Read audit/status timeline, webhook deliveries, retry status. |
| `POST` | Create webhook subscription, trigger retry command, emit business command. |
| `PUT` | Replace webhook subscription configuration. |
| `PATCH` | Enable/disable webhook subscription or update retry policy. |
| `DELETE` | Disable subscription, not erase audit/event history. |

### 7. HTTP headers do cwiczenia

| Header | Czego uczy |
|---|---|
| `Authorization` | Protected audit/webhook/admin operations. |
| `Content-Type` | Webhook config and event delivery payload contracts. |
| `Accept` | Versioned event/timeline response representation. |
| `X-Correlation-ID` | End-to-end trace across command, event, audit, webhook. |
| `X-Request-ID` | Client-side request identity and diagnostics. |
| `Idempotency-Key` | Retry-safe command and webhook receiver semantics. |
| `Retry-After` | Retry advice after `429`/`503` or delivery backoff exposure. |
| `ETag` | Versioned webhook subscription config. |
| `If-Match` | Prevent stale config update. |
| API version headers | Event/timeline contract evolution. |

### 8. Status codes do cwiczenia

| Status | Przyklad |
|---|---|
| `200` | Read timeline, subscription, delivery status. |
| `201` | Create webhook subscription. |
| `202` | Accept retry/delivery command for asynchronous processing. |
| `204` | Disable subscription or update with no body. |
| `400` | Invalid URL, invalid event type, malformed request. |
| `401` | Missing/invalid token. |
| `403` | Missing webhook/admin/audit authority or ownership violation. |
| `404` | Unknown subscription/delivery/event. |
| `409` | Retry not allowed in current delivery state. |
| `412` | Stale `If-Match` on subscription update. |
| `422` | URL syntactically valid but unacceptable by business policy, if chosen. |
| `429` | Too many retry requests or rate-limited delivery attempts. |
| `500` | Verify generic internal error hygiene. |
| `503` | External callback unavailable or dispatcher dependency unavailable. |

### 9. Security risks do cwiczenia

- Audit and webhook endpoints require strong authorization.
- Merchant A cannot view Merchant B's webhook deliveries.
- Tokens must not be logged in correlation/audit data.
- Webhook URL ownership/config update must be protected.
- Retry commands must not allow unauthorized re-delivery.
- Token expiry and forbidden action remain part of every protected endpoint.

### 10. PostgreSQL/data risks do cwiczenia

- Append-only status history and audit log.
- FK from events/audit/webhook deliveries to payment order and merchant.
- Unique event IDs and unique delivery attempt numbers.
- Indexes for timeline queries: `(payment_order_id, occurred_at)`.
- Transaction: state change and event/audit record written atomically.
- Outbox state transition: pending, published, failed, dead-letter candidate.
- Isolation: concurrent retries must not create duplicate delivery attempts.
- Audit payload must avoid sensitive data.

### 11. Test design techniques do cwiczenia

- State transition: delivery pending, sent, failed, retry scheduled, exhausted.
- Decision tables: response code x retry policy x attempt count x next state.
- EP: 2xx callback, 4xx non-retryable, 5xx retryable, timeout retryable.
- BVA: max retry attempts, retry delay thresholds, payload size limits.
- Pairwise: event type x subscription state x delivery result x ownership.
- Exploratory charters: lost event, duplicate event, out-of-order timeline, bad correlation ID, sensitive data in logs/audit.

### 12. Test data patterns do cwiczenia

- Unique correlation IDs per scenario.
- API-assisted setup for merchant, payment, subscription and event.
- Worker-safe event and delivery records.
- Avoid cleanup for audit/history; prefer namespaced data and time-window filtering.
- Idempotency keys for retryable commands.
- WireMock or equivalent later for outbound HTTP callback behavior.

### 13. Test layers potrzebne

- Domain tests for retry policy and event state machine.
- Service tests for atomic status/event/audit orchestration.
- Repository tests for append-only history, indexes, uniqueness and FK constraints.
- REST Assured tests for timeline/audit/webhook management API.
- Security tests for ownership and role matrix.
- Module/integration tests for outbox and webhook module boundaries.
- WireMock/Testcontainers later for outbound HTTP.
- Kafka tests only after DB outbox is stable.
- Playwright only for dashboard visibility of timeline/delivery status, not retry matrix.

### 14. Przykladowy realistyczny REST Assured scenario test bez implementacji

Scenario: payment state change creates traceable status history and webhook delivery candidate.

1. Test creates active merchant and payment order.
2. Test sends status transition command with `X-Correlation-ID`.
3. Expected command response is `200` or `202`, depending on synchronous/asynchronous design.
4. Test reads `GET /api/payment-orders/{id}/timeline` and verifies status event exists with the same correlation ID.
5. Test reads webhook delivery list for that payment/event and verifies one pending delivery candidate exists.
6. If delivery is later simulated as failed with `503`, test verifies retry is scheduled with attempt count incremented and no duplicate event is created.
7. Test verifies unauthorized merchant cannot read the timeline or delivery.

### 15. Interview stories ta sciezka zbuduje

- I tested traceability with correlation IDs across API, DB history and delivery records.
- I designed tests for retry behavior without jumping directly to Kafka.
- I explained transactional outbox as a reliability pattern and why Kafka came later.
- I verified auditability and no-sensitive-data principles.
- I tested eventual consistency with deterministic polling and clear oracles.

### 16. Ryzyka zbyt trudne albo za wczesne

- Webhooks before Payment Order/status events would be decorative.
- Kafka before outbox teaches tooling, not event-driven design.
- Async tests can become flaky without deterministic state and polling strategy.
- Audit log can become huge scope if compliance UI and retention policies enter early.

### 17. Pierwszy maly krok

After Payment Order exists, define a tiny `payment_status_history` or audit/status timeline discovery note: what event is recorded, when, with which correlation ID, and what is intentionally not audited.

### 18. Deferred

- Kafka topics, consumers and DLQ.
- Full webhook retry scheduler.
- Complex observability stack with traces/metrics dashboards.
- Settlement/reconciliation.
- GraphQL reporting read model.
- gRPC internal risk/authorization simulator.

## Macierz porownawcza

| Sciezka | Wartosc edukacyjna | Wartosc produktowa | Trudnosc | Ryzyko scope creep | Najlepsza jako nastepna? | Dlaczego |
|---|---|---|---|---|---|---|
| Payment Order REST API, Idempotency And Lifecycle | Bardzo wysoka | Bardzo wysoka | Srednia do wysokiej | Srednie | Tak | Daje pierwszy realny payment resource i laczy REST Assured, HTTP, SQL, idempotency, security i test design bez asynchronicznego chaosu. |
| Security, Ownership And Tenant Isolation Matrix | Bardzo wysoka | Wysoka | Srednia | Srednie | Jako rownolegly discovery, nie pierwszy implementation slice | Obecne role/401/403 juz istnieja, ale prawdziwe ownership wymaga zasobu paymentowego. Najpierw matryca, potem implementacja na Payment Order. |
| Reliability, Data Integrity, Audit Trail, Webhooks And Event Evolution | Bardzo wysoka | Wysoka | Wysoka | Wysokie | Nie jako pierwsza implementacja | To najlepsza pozniejsza sciezka po Payment Order i minimalnym lifecycle; za wczesnie bez status history/event model. |

## Rekomendowana kolejnosc na najblizsze tygodnie

1. Najpierw przerobic `REST API From Zero/Merchant Request and Response Flow.md`, jezeli nie jest jeszcze pewnie opanowane.
2. Potem przerobic `REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`.
3. Rownolegle czytac podstawy z `PostgreSQL and SQL From Zero`, `Spring Boot Spring MVC`, `Spring Data JPA and Flyway` i `Security and Authorization Testing` na przykladach Merchant Registry.
4. Uzyc `Backend Testing Review` checklist do samodzielnego review obecnych `MerchantRestAssuredTest` i `MerchantSecurityTest`.
5. Potem przerobic `REST Assured from Zero to Professional Backend API Testing/13-22 Professional Practice After Refactoring.md`.
6. Przygotowac BA Discovery Pack dla `Payment Order REST API`.
7. W BA Discovery Pack jawnie zapisac non-goals: no PSP, no cards/PAN/PCI, no webhook, no Kafka, no GraphQL, no gRPC, no refund, no settlement.
8. Po discovery przygotowac Spec Kit input dla minimalnego `Payment Order REST API` capability.
9. Dopiero potem implementowac minimalny vertical slice: create payment order for active merchant, read payment order, idempotency key, DB constraints, REST Assured/security/repository/domain tests.
10. Po minimalnym vertical slice dodac notatki: request/response flow, idempotency testing story, SQL constraints story, authorization matrix story.
11. Nastepnie przejsc do security/ownership path na realnym Payment Order: multi-merchant data, read/list isolation, `403` vs masked `404` decision.
12. Dopiero po tym przejsc do reliability path: payment status history, audit/correlation, outbox/webhooks, a Kafka dopiero po stabilnym event model.

## Najwazniejsze headers/methods/status codes do utrwalenia

Methods:

- `GET`: read resource, list/filter, timeline/status lookup.
- `POST`: create command, idempotent create, retry command.
- `PUT`: replace owned configuration, mainly future webhook config.
- `PATCH`: partial update/status/config change with optimistic locking where justified.
- `DELETE`: disable/cancel semantics only where product rules allow, not hard-delete financial history.

Headers:

- `Authorization`
- `Content-Type`
- `Accept`
- `Location`
- `Idempotency-Key`
- `X-Correlation-ID`
- `X-Request-ID`
- `ETag`
- `If-Match`
- `Retry-After`
- API version headers

Status codes:

- `200`, `201`, `202`, `204`
- `400`, `401`, `403`, `404`, `409`, `412`, `422`, `429`
- `500`, `503` as error-hygiene/reliability topics, not happy-path targets

## Najwazniejsze security, data i test-design ryzyka

Security:

- Authentication vs authorization.
- JWT validity, expiry, issuer and signature.
- Role/authority matrix.
- Ownership and tenant isolation.
- Forbidden action.
- Error response leakage.
- No token or secret logging.

Data/PostgreSQL:

- Unique constraints for business keys and idempotency keys.
- FK integrity between merchant and payment resources.
- Check constraints for amount/status.
- Indexes for list, ownership and timeline queries.
- Transactions for atomic business change plus event/history/audit.
- Optimistic locking and stale updates.
- Append-only audit/history discipline.
- Parallel-safe test data.

Test design:

- EP and BVA for amount, currency, idempotency key, request validation.
- Decision tables for role x ownership x state x method.
- State transition testing for payment lifecycle and delivery lifecycle.
- Pairwise for controlled combinations.
- Exploratory charters for duplicate charge, data leak, lost event, retry storm, stale status and bad observability.

## Decyzja strategiczna

Najlepszy pierwszy kierunek to Sciezka 1 jako product implementation candidate, ale tylko po lekcjach i BA Discovery Pack. Sciezka 2 powinna zaczac sie rownolegle jako discovery/matrix, bo pomoze dobrze zaprojektowac Payment Order ownership. Sciezka 3 jest bardzo wartosciowa, ale powinna wejsc dopiero po minimalnym Payment Order i poczatkowym lifecycle/status history.
