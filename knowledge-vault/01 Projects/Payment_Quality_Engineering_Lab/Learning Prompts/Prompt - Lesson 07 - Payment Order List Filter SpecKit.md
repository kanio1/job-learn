---
type: prompt
status: ready
date: 2026-05-28
lesson: 07
tags:
  - prompt
  - speckit
  - lesson-07
  - payment-order-list
  - learning-os
---

# Prompt - Lesson 07 - Payment Order List Filter + Framework Architecture

Copy this entire prompt and give it to Kilo to execute `/speckit.specify` for Lesson 07.

```text
Jesteś moim zespołem: Business Analyst, Architekt Backend, QA Architect i Agent Kodowania.
Pracujemy w repozytorium /home/suso/job-learn na branchu 004-payment-order-create-read.

## Kontekst

Przeczytaj przed rozpoczęciem:

- `specs/003-payment-order-access-lifecycle/spec.md` — obecna specyfikacja Payment Order (create/read)
- `specs/003-payment-order-access-lifecycle/plan.md` — plan architektoniczny
- `specs/003-payment-order-access-lifecycle/data-model.md` — model danych
- `specs/003-payment-order-access-lifecycle/contracts/payment-order-api.md` — API contract
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/START HERE - Learning Dashboard.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Learning Flow.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Curriculum Backbone.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 06 - Payment Order Create Read Foundation.md`

Przeczytaj kod:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java`
- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentApiTestSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`

## Cel

Uruchom `/speckit.specify` aby stworzyć specyfikację dla Lesson 07 — Payment Order List z filtrowaniem, paginacją i sortowaniem.

## Feature

**Payment Order List with Advanced Filtering, Pagination, and Sorting**

Pierwszy krok od "create/read pojedynczy" do zarządzania wieloma payment orderami. Merchant reader potrzebuje przeglądać, filtrować i sortować swoje payment ordery.

## Actorzy

- **Merchant Payment Reader** — `merchant:payments:read` + matching `merchant_id` claim — widzi tylko swoje ordery
- **Platform Payment Reader** — `platform:payments:read` — widzi dowolne ordery (support/investigation)
- **Cross-Tenant Actor** — merchant A próbuje listować ordery merchanta B → `403 forbidden` (jawna odmowa, nie maskowane `404`)
- **Unauthenticated User** — `401`
- **Denied Identity** — `403`

## Scope

### IN Scope

- `GET /api/merchants/{merchantId}/payment-orders` — lista z filtrowaniem
- Query params: `status`, `currency`, `fromDate`, `toDate`, `minAmount`, `maxAmount`, `clientOrderReference` (partial match ILIKE)
- Paginacja: `page` (default 0), `size` (default 20, min 1, max 100)
- Sortowanie: `sort` (default `createdAt,desc`)
- Response: `{ content: [...], page, size, totalElements, totalPages }` z `200 OK` + `X-Correlation-ID`
- Walidacja query params: `400 validation` dla `?page=-1`, `?size=1001`, `?status=INVALID`, `?currency=GBP`
- Tylko status `CREATED` w pierwszym slicie (jedyny istniejący status)
- Waluty: `PLN`, `EUR`, `USD`
- Dynamiczne `Specification<PaymentOrder>` w repozytorium (`JpaSpecificationExecutor`)
- Migracja: `V3__add_payment_order_list_indexes.sql` — indeksy dla filtrowania
- Security: `merchant:payments:read` dla merchant scope, `platform:payments:read` dla cross-merchant
- `merchant_id` claim wymagany dla merchant reader — cross-tenant lista → `403`

### OUT of Scope (explicit)

- GROUP BY, COUNT per status, agregacje — to Lesson 08
- Lifecycle actions (authorize, capture, cancel) — to Spec Kit 004+
- Frontend list page (Nuxt) — opcjonalne rozszerzenie
- Playwright E2E — zbyt dużo
- Eksport CSV/PDF
- Wyszukiwanie pełnotekstowe
- Sortowanie wielokolumnowe
- Rate limiting
- Cache headers

## New REST Assured Methods (NIGDY nie użyte w projekcie)

Te 7 metod NIE występuje w żadnym istniejącym teście REST Assured:

1. `queryParam("status", "CREATED")` — przekazywanie parametrów zapytania
2. `accept(ContentType.JSON)` — jawne content negotiation
3. `extract().as(PaymentOrderListResponse.class)` — typowana deserializacja (obecnie tylko `extract().path()`)
4. `new RequestSpecBuilder().addHeader(...).build()` — programowe budowanie request spec
5. `new ResponseSpecBuilder().expectStatusCode(200).build()` — reużywalne response specs
6. `.log().ifValidationFails()` — logowanie tylko gdy test pada
7. `RestAssured.filters(new ResponseLoggingFilter(...))` z blacklistą `Authorization` — maskowanie sekretów

## New AssertJ / JUnit / SQL Patterns

- AssertJ: `extracting()`, `filteredOn()`, `tuple()`, `usingRecursiveComparison()`, `SoftAssertions`
- JUnit: `@ParameterizedTest` + `@CsvSource`, `@Nested`, `@Tag`, `@DisplayName`
- SQL: `WHERE` warunkowe, `ORDER BY`, `LIMIT/OFFSET`, `COUNT(*)` dla paginacji, indeksy, `Specification<T>` dynamic query

## Business Rules

1. Lista zwraca tylko ordery należące do `{merchantId}` z path
2. Empty filter = wszystkie ordery merchanta
3. Multi-filter = AND wszystkich podanych filtrów
4. `clientOrderReference` = partial match (ILIKE `%value%`)
5. `fromDate` / `toDate` filtrują po `createdAt`
6. `minAmount` / `maxAmount` filtrują po `amountMinor` (inclusive)
7. Sort default: `createdAt DESC`
8. Page default: 0, size default: 20
9. Brak owner authorization → `403` (nie `404` dla listy)
10. `merchant:payments:create` NIE daje prawa do listy

## Guardrails (z AGENTS.md i Phase 0)

- ❌ Nie dodawaj nowego modułu Spring Modulith — lista w istniejącym `payment`
- ❌ Nie dodawaj lifecycle actions (authorize/capture/cancel)
- ❌ Nie dodawaj PSP integration
- ❌ Nie dodawaj Kafka, GraphQL, gRPC
- ✅ Nowy endpoint w istniejącym module = lesson extension
- ✅ `merchant_id` claim enforcement obowiązkowy

## Wymagany output `/speckit.specify`

1. `specs/004-payment-order-list-filter/spec.md` z:

   - Business Purpose — dlaczego lista, co odblokowuje
   - Actors — 5 aktorów z rolami
   - In Scope / Out of Scope — dokładna lista
   - Functional Requirements — FR-001 do FR-020+
   - API Contract — `GET /api/merchants/{merchantId}/payment-orders` z wszystkimi query params i response
   - Security Matrix — tabela aktor × operacja
   - Database Impact — nowa migracja, indeksy
   - Test Strategy — 5 warstw testów
   - Acceptance Criteria — minimum 15 AC
   - Risks — minimum 5 ryzyk
   - Definition of Done

2. Spec musi zawierać `Clarifications` section z decyzjami:

   - Cross-tenant list → `403` (nie `404`)
   - `merchant:payments:create` nie daje read
   - Default page=0, size=20, max size=100
   - `clientOrderReference` = partial match ILIKE
   - Tylko status `CREATED` (jedyny istniejący)
   - Sort default `createdAt,desc`
   - `platform:payments:read` może listować cross-merchant

## Verification

Po stworzeniu spec, sprawdź:

- [ ] Spec nie powtarza Lekcji 06 (create/read pojedynczy)
- [ ] Spec respektuje AGENTS.md guardrails
- [ ] Wszystkie 7 nowych metod REST Assured ma pokrycie w test strategy
- [ ] Wszystkie 5 nowych wzorców AssertJ ma pokrycie
- [ ] Security matrix pokrywa cross-tenant list
- [ ] Walidacja query params ma testy
- [ ] Paginacja ma edge case testy (pusta strona, ostatnia strona, oversized size)
- [ ] Indeksy są w migracji
- [ ] `JpaSpecificationExecutor` jest użyte

Użyj skills:
- `qa-architecture-sprint-team` — BA + Architecture + QA Architecture
- `spec-kit-feature-workflow` — Spec Kit workflow
- `spring-boot4-spring7-backend-architect` — Backend architecture
- `postgres18-data-architecture-and-risk` — DB indexes + query plan
- `java-rest-api-testing-effective-java-mentor` — Java 25 + REST Assured design
- `junit6-assertj-restassured-testcraft` — Test quality + assertion patterns
```
