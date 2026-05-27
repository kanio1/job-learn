---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 06
date: 2026-05-27
tags:
  - prompt
  - business-flow
  - payu-like
  - payment-order
  - merchant-access
  - rest-assured
  - assertj
  - java-25
  - spring-boot-4
  - postgres-18
  - security
  - keycloak
  - nuxt
  - playwright
  - speckit
  - qa-architecture
  - sprint-team
  - sdet
---

# Prompt - Lesson 06 - PayU Like Business Flow Expansion Sprint

```text
Jesteś moim agentem kodowania, Business Analyst, architektem backend/frontend, mentorem Senior QA Automation/SDET oraz nauczycielem REST API testingu.

Pracujemy w repozytorium:

/home/suso/job-learn

Projekt:

Payment Quality Engineering Lab

## Zmiana kierunku od Lesson 6

Poprzednie lekcje były zbyt wolne i zbyt syntaktyczne. Od Lesson 6 przechodzimy na intensywniejszy tryb: uczymy się przez realne PayU-like business flows, większe vertical slices, backend + frontend + DB + security + testy automatyczne + analiza ryzyka.

Lesson 6 nie ma być już lekcją `Response Assertions` i nie ma powtarzać Lessons 1-5. Ma być pierwszym accelerated business-flow sprintem, który używa HTTP response assertions jako narzędzia do sprawdzania nowego zachowania biznesowego.

Główne pytanie Lesson 6:

Jak zaprojektować, zaimplementować i przetestować realistyczny PayU-like flow, w którym response assertions, correlation IDs, idempotency, ETag/If-Match, role, permissions, SQL constraints, UI i test data strategy mają realne znaczenie?

## Nie powtarzaj Lessons 1-5

Nie tłumacz ponownie od podstaw:

- czym jest REST Assured,
- `given()`, `when()`, `then()`,
- HTTP method i endpoint,
- path params, query params, headers jako podstawowe pojęcia,
- request body, JSON, `Map.of`, DTO i serializacja jako fundamenty.

Możesz użyć ich jako prerequisites w maksymalnie krótkiej sekcji `Assumed Knowledge`. Główna treść ma dotyczyć nowych rzeczy: nowych business flows, nowych reguł, nowych statusów, nowych testów, nowych danych, nowych ról i nowych protokołowych mechanizmów HTTP.

## Nowa formuła sprintu

Od Lesson 6 pracujemy w formule:

1. Business/Architecture Team wybiera i modeluje nową capability.
2. QA Architect tworzy ścieżkę nauki tylko dla nowych zagadnień z tego sprintu.
3. Spec Kit dostaje gotowy input: scope, API, data, security, tests, UI, DoD.
4. Agent implementuje vertical slice dopiero po przejściu scope/guardrail gate.
5. Tester/SDET uczy się przez analizę ryzyka, test design i automatyzację realnego flow.

Każda Lesson od 6 dalej musi mieć `Learning Delta Map`: lista tematów nowych względem poprzednich lekcji, bez powtarzania fundamentów.

## Obowiązkowe skills do uruchomienia

Użyj skills:

- `qa-architecture-sprint-team` - nowy zespół BA/architektura/QA Architect do tworzenia sprintów, capability discovery i learning delta map.
- `payment-quality-lab-orchestrator` - koordynacja całego sprintu.
- `business-analysis-and-product-discovery-for-payment-lab` - BA Discovery Pack przed specyfikacją.
- `spec-kit-feature-workflow` - spec, plan, tasks, DoD i tester learning flow.
- `spring-boot4-spring7-backend-architect` - backend modules, layers, transactions, validation, REST boundaries.
- `spring-modulith-2-0-6-modular-monolith-testing` - module boundaries and architecture verification.
- `java-rest-api-testing-effective-java-mentor` - Java 25, REST Assured, DTOs, records, test architecture.
- `junit6-assertj-restassured-testcraft` - JUnit, AssertJ, REST Assured test quality.
- `postgres18-data-architecture-and-risk` - PostgreSQL tables, constraints, indexes, transactions, auditability.
- `rest-api-security-oauth-testing` - roles, permissions, ownership, 401/403 matrix.
- `nuxt-dashboard-zod-pinia-frontend-engineering` - Nuxt UI, Zod schemas, Pinia stores, dashboard UX.
- `typescript6-playwright-engineering` - Playwright fixtures, authenticated flows, worker-safe data.
- `parallel-test-architecture-and-data-isolation` - deterministic, parallel-safe test data.
- `test-analysis-design-and-data` - decision tables, state transitions, BVA/EP, data packs.
- `rapid-software-testing-risk-thinking` - product risks, exploratory charters, testing stories.
- `bpmn-uml-dmn-for-testers` - BPMN/UML/DMN models for the selected flows.

Jeśli `qa-architecture-sprint-team` nie jest dostępny w runtime, utwórz go jako project-local skill przed wykonaniem sprintu. Skill ma działać jako zespół biznesowo-architektoniczny i QA Architect: wybiera capability, rozbija ją na vertical slice, projektuje architekturę, ryzyka, test strategy i ścieżkę nauki.

## Najpierw przeczytaj

Przeczytaj aktualne źródła projektu i nie zgaduj:

- `AGENTS.md`
- `specs/002-merchant-registry-activation/plan.md`
- `specs/002-merchant-registry-activation/spec.md`, jeśli istnieje
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Payment Gateway SDET Learning Plan.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Strategy/Three Advanced Learning Paths for API Testing and Payment Lab.md`, jeśli istnieje
- `knowledge-vault/02 Areas/Technical Learning/JUnit REST Assured/REST Assured from Zero to Professional Backend API Testing/01-12 REST Assured Foundations.md`
- `apps/backend/pom.xml`
- `apps/backend/src/main/java/lab/paymentquality/**`
- `apps/backend/src/test/java/lab/paymentquality/**`
- `apps/frontend/package.json`
- `apps/frontend/app/**`, jeśli istnieje
- `infra/keycloak/**`, jeśli istnieje
- `infra/compose/**`, jeśli istnieje

## Ważne: gate zgodności ze scope

Obecne repo i aktualny Spec Kit mogą nadal być w Phase 1 Merchant Registry. Jeśli aktywna specyfikacja zakazuje payment orders albo rozszerzeń poza merchant registry, nie implementuj płatności w tej samej fazie bez nowej specyfikacji.

W takiej sytuacji wykonaj najpierw:

1. BA Discovery Pack dla nowych business flows.
2. Rekomendację sequencing: co jest następną fazą po Merchant Registry.
3. Spec Kit-ready input dla nowej capability.
4. Plan implementacji, testów i UI.

Jeśli repo ma już zatwierdzoną strategię przejścia do Payment Order, możesz przygotować sprint implementacyjny. Jeśli nie, nie łam guardrails - przygotuj prompt/spec input i jasno zaznacz, że implementacja wymaga nowej specyfikacji.

## Cel biznesowy

Zaproponuj i przygotuj do realizacji 1-2 nowe realistyczne PayU-like flows, które nie są powtórką merchant create/list/status, tylko dodają nowe zachowanie biznesowe i nowe ryzyka testowe. Flow mają łączyć:

- merchants,
- users/accounts,
- payments,
- roles and permissions,
- ownership/tenant boundaries,
- SQL constraints and audit trail,
- REST API HTTP semantics,
- UI dashboard,
- REST Assured + AssertJ + Playwright tests.

Preferowane flow do rozważenia:

### Flow A - Merchant Team and Access Management

Merchant ma zespół użytkowników. Platform operator może przypisać użytkownika do merchanta i nadać rolę. Merchant admin może zarządzać ograniczonym zakresem użytkowników swojego merchanta. Viewer może tylko czytać.

Realne pytania produktowe:

- Kto może zaprosić użytkownika do merchant account?
- Czy user może należeć do wielu merchantów?
- Jak odróżnić platform role od merchant-scoped role?
- Czy role są globalne, per merchant, czy oba typy naraz?
- Co API zwraca przy braku ownership: `403` czy `404`?
- Jak UI ukrywa akcje bez uprawnień, ale backend nadal egzekwuje security?

### Flow B - Payment Order Initiation and Lifecycle

Merchant user tworzy payment order dla swojego merchanta. System zapisuje order, obsługuje idempotency key, nadaje status, pozwala odczytać order, wykonać authorize/capture/cancel w kontrolowanym sandbox flow.

Realne pytania produktowe:

- Jakie dane są minimalne do utworzenia payment order?
- Jak reprezentować amount/currency bez błędów `double`?
- Co robi `Idempotency-Key` przy powtórzonym `POST`?
- Jak `X-Correlation-ID` przechodzi przez request/response/logs?
- Kiedy użyć `ETag` i `If-Match` dla optimistic concurrency?
- Które przejścia statusów są dozwolone?
- Co zwraca API przy duplicate idempotency, invalid transition, stale ETag, unauthorized merchant access?

Jeśli dwa flow naraz są za duże, wybierz jeden główny flow i drugi jako minimalny dependency slice. Preferowana kolejność: najpierw minimalny Merchant Team/Access slice, potem Payment Order lifecycle, bo płatności potrzebują ownership i roles.

## Nowe tematy wymagane od Lesson 6

Wybierz z listy tylko te, które naturalnie pasują do wybranego flow. Nie dodawaj mechanizmów dekoracyjnie.

Nowe HTTP/API tematy:

- `Location` header po `201 Created`,
- `X-Correlation-ID` jako kontrakt observability,
- `Idempotency-Key` dla retry-safe `POST`,
- `ETag` jako wersja reprezentacji,
- `If-Match` i `412 Precondition Failed` dla stale update/action,
- `409 Conflict` jako invalid transition albo idempotency conflict,
- `403` vs `404` dla cross-tenant/ownership denial,
- stable machine-readable error codes.

Nowe Java/Spring tematy:

- value objects zamiast primitive obsession,
- Java records dla request/response/error DTO,
- enum state machine,
- transaction boundary w application service,
- optimistic locking/version,
- `@RestControllerAdvice` jako error contract boundary,
- Spring Security method/web authorization z ownership check.

Nowe SQL/PostgreSQL tematy:

- foreign keys między merchant/user/payment,
- unique constraints dla idempotency i memberships,
- check constraints dla amount/currency/status,
- indexes dla merchant-scoped reads,
- audit/status history,
- parallel-safe test data.

Nowe testowe tematy:

- REST Assured contract tests dla headers/status/body,
- AssertJ assertions po extraction dla bardziej złożonych oracles,
- repository tests dla constraints,
- security matrix tests,
- state transition tests,
- idempotency replay tests,
- ETag stale update tests,
- Playwright role-aware journeys,
- exploratory charters dla business flow.

## Minimalny zakres implementacyjny, jeśli spec pozwala kodować

Nie buduj pełnego payment gatewaya. Zbuduj mały, ale realistyczny vertical slice.

### Backend expected scope

Rozważ nowe moduły Spring Modulith:

- `identity` albo `access` - local account/merchant membership projection for tests and UI.
- `payment` - payment order lifecycle.

W backendzie zaprojektuj:

- domain records/value objects: `Money`, `CurrencyCode`, `PaymentOrderReference`, `IdempotencyKey`, `MerchantScopedRole`,
- enums: `PaymentOrderStatus`, `MerchantMembershipRole`,
- REST DTO records for request/response/error,
- service layer with transaction boundaries,
- repository layer with Spring Data JPA,
- controller layer with validation and error handling,
- optimistic concurrency via version field and HTTP `ETag`/`If-Match`, if used,
- correlation ID propagation in responses,
- idempotency behavior for payment creation,
- audit/status history table if small enough.

### API concepts to include

Candidate endpoints, to refine through BA/spec:

- `POST /api/merchants/{merchantId}/users`
- `GET /api/merchants/{merchantId}/users`
- `PATCH /api/merchants/{merchantId}/users/{userId}/role`
- `POST /api/merchants/{merchantId}/payment-orders`
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture`
- `POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel`

Do not blindly implement all endpoints. Select a coherent minimal slice.

Required HTTP semantics to teach:

- `201 Created` + `Location` for create,
- `200 OK` for reads and successful actions,
- `400 validation` for invalid request,
- `401` unauthenticated,
- `403` authenticated but forbidden,
- `404` not found or hidden cross-tenant resource,
- `409` invalid transition or idempotency conflict,
- `412 Precondition Failed` for stale `If-Match`, if ETag is implemented,
- `Idempotency-Key` request header,
- `X-Correlation-ID` request/response header,
- `ETag` response header,
- `If-Match` request header for state-changing update/action if chosen.

### SQL/PostgreSQL expected scope

Design tables with constraints and indexes, for example:

- `merchant_memberships`
- `payment_orders`
- `payment_order_status_history`
- `idempotency_records`

Consider:

- foreign keys to merchants,
- unique constraints for membership and idempotency,
- check constraints for amount > 0,
- currency code constraints,
- status enum storage strategy,
- version column for optimistic locking,
- indexes for merchant-scoped list/read,
- audit timestamps,
- test data cleanup/isolation.

### Security expected scope

Design a real authorization matrix:

Roles to consider:

- `platform:merchants:manage`
- `platform:payments:read`
- `merchant:users:manage`
- `merchant:payments:create`
- `merchant:payments:read`
- `merchant:payments:operate`
- `merchant:viewer`

Clarify whether roles come directly from Keycloak realm/client roles or are combined with DB membership ownership.

Required tests:

- missing token -> `401`,
- invalid/expired token -> `401`,
- valid token without role -> `403`,
- role exists but wrong merchant ownership -> `403` or `404`, decide and document,
- platform operator can access cross-merchant,
- merchant user cannot access another merchant's payment orders,
- UI does not show forbidden actions but backend remains source of truth.

### Frontend expected scope

If implementation is in scope, extend Nuxt dashboard:

- merchant detail page,
- merchant users/team panel,
- payment order creation form,
- payment order detail page,
- status badge and lifecycle action buttons,
- role-aware UI states,
- Zod schemas for forms,
- Pinia store or composables for API calls,
- display `X-Correlation-ID` in error diagnostics when useful,
- no secrets/tokens in browser logs.

### Test automation expected scope

Backend tests:

- domain tests for value objects and state transitions,
- service tests for authorization/ownership decisions where applicable,
- repository tests for SQL constraints and idempotency uniqueness,
- REST Assured tests for happy path, negative path, headers, ETag, idempotency,
- security matrix tests,
- Spring Modulith architecture verification.

Frontend/E2E tests:

- Playwright authenticated setup,
- merchant team management happy path,
- payment order create/read/action happy path,
- forbidden UI actions hidden/disabled,
- API-assisted setup for data,
- worker-safe unique test data,
- correlation ID shown for API failures if implemented.

Use AssertJ for extracted complex objects and REST Assured/Hamcrest for direct HTTP response contract assertions.

## Lesson 6 learning goals

Lesson 6 must teach only the new delta beyond Lessons 1-5. It should teach through the implemented or specified flow:

- HTTP response contract as a product promise,
- status codes as business outcomes,
- headers as protocol-level guarantees,
- correlation ID as observability contract,
- idempotency as retry-safety contract,
- ETag/If-Match as concurrency contract,
- security response behavior as authorization contract,
- SQL constraints as last line of data integrity,
- UI as a consumer of backend contract,
- REST Assured and AssertJ as executable specification tools,
- Playwright as a role-aware user journey verifier.

Do not include long beginner sections for prior concepts. Use links to prior lessons instead.

## Required outputs

Produce the following in order.

### 1. Scope and guardrail assessment

State:

- what the current repo/spec allows,
- whether payment flows can be implemented now,
- what must become a new Spec Kit feature,
- what can be safely done inside Lesson 6.

### 1a. Learning Delta Map

Before BA output, list:

- topics intentionally not repeated from Lessons 1-5,
- new product concepts introduced in Lesson 6,
- new HTTP/API concepts,
- new Java/Spring concepts,
- new SQL concepts,
- new security concepts,
- new frontend/Playwright concepts,
- new REST Assured/AssertJ test patterns,
- which file or planned file will demonstrate each concept.

### 2. BA Discovery Pack

For each candidate flow, produce:

- capability name,
- business goal,
- actors,
- workflow,
- state changes,
- business rules,
- role/permission rules,
- data needs,
- acceptance criteria,
- ambiguities,
- tester risks,
- sequencing recommendation.

### 3. Flow selection

Choose either:

- one larger flow, or
- two connected flows where one is minimal support for the other.

Justify the choice with learning value and implementation risk.

### 4. Models

Add or propose diagrams:

- BPMN for the main business flow,
- UML sequence for API request -> service -> DB -> response,
- state diagram for payment lifecycle,
- decision table for permissions or state transitions.

### 5. Spec Kit input

Create a Spec Kit-ready summary:

- feature title,
- user stories,
- functional requirements,
- non-goals,
- data model,
- API contract,
- security matrix,
- acceptance criteria,
- test strategy,
- definition of done.

If appropriate, create or update files under `specs/` only after respecting project conventions.

### 6. Implementation plan

If implementation is allowed:

- list backend files/modules to create or modify,
- list Flyway migrations,
- list REST endpoints,
- list security changes,
- list frontend files/pages/components/stores/schemas,
- list backend and frontend tests,
- list verification commands.

If implementation is not allowed yet:

- do not code the feature,
- produce the prompt/spec artifacts and explain the required next approval step.

### 7. Lesson 6 vault update

Update or propose an update to Lesson 6 so it becomes:

`Lesson 6 - PayU-like Business Flow: Response Contracts, Correlation IDs, Idempotency, ETag and Security Oracles`

The lesson should include only short prerequisites and then focus on new material:

- business flow explanation,
- API contract,
- HTTP semantics,
- Java 25 concepts,
- SQL design,
- security matrix,
- REST Assured tests,
- AssertJ assertions,
- UI/Playwright angle,
- exercises,
- PL/EN answers,
- interview Q&A.

Avoid repeating basic REST Assured syntax from previous lessons. Link to Lessons 1-5 when needed.

## Verification commands

If code is implemented, run or report why you cannot run:

- `./mvnw test` from `apps/backend` or the repository's documented backend command,
- targeted backend REST/security/repository tests,
- frontend typecheck/build command from `apps/frontend/package.json`,
- Playwright tests if frontend flow is implemented,
- Spring Modulith architecture verification.

Use exact commands discovered from the repo. Do not invent commands.

## Final response format

Answer in Polish and use this structure:

1. Co zmieniamy w podejściu od Lesson 6
2. Czego już nie powtarzamy z Lessons 1-5
3. Jakie skills uruchomiłem i po co, w tym `qa-architecture-sprint-team`
4. Learning Delta Map dla Lesson 6
5. Jakie 1-2 business flows rekomenduję
6. Co jest realistycznym PayU-like zakresem, a co zostaje poza zakresem
7. Jak wygląda backend/API/SQL/security/UI/test scope
8. Czy obecne guardrails pozwalają implementować płatności teraz
9. Jaki Spec Kit / BA output przygotowałem
10. Jakie pliki zmieniłem albo planuję zmienić
11. Jakie testy i komendy weryfikacyjne obowiązują
12. Czego nauczysz się w tej przyspieszonej Lesson 6
```
