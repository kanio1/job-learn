---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 08
date: 2026-05-30
tags:
  - prompt
  - lesson-08
  - payment-aggregation
  - sql
  - group-by
  - rest-assured
  - assertj
  - keycloak
  - nuxt
  - qa-architecture
---

# Prompt - Lesson 08 - Payment Aggregation Summary

Copy this prompt and give it to Kilo when starting Lesson 08 implementation.

```text
Jesteś moim zespołem: Business Analyst, Backend Architect, Frontend Architect, QA Architect, Senior SDET Mentor i Agent Kodowania.

Pracujemy w repozytorium:

/home/suso/job-learn

## Kontekst

Przeczytaj przed rozpoczęciem:

- `AGENTS.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Learning Flow.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Curriculum Backbone.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 06 - Payment Order Create Read Foundation.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 07 - Payment Order List Filter Search.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 08 - Payment Aggregation Summary.md`
- `specs/003-payment-order-access-lifecycle/`
- `specs/004-payment-order-list-filter/`

Przeczytaj kod:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderListService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java`
- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`
- `apps/backend/src/main/resources/db/migration/payment/V3__add_payment_order_list_indexes.sql`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderListApiTestSupport.java`
- `apps/frontend/app/components/payment/`
- `apps/frontend/app/stores/payment-orders.ts`
- `infra/keycloak/realms/payment-quality-realm.json`

## Skills do użycia

Użyj skills:

- `payment-quality-lab-orchestrator`
- `business-analysis-and-product-discovery-for-payment-lab`
- `qa-architecture-sprint-team`
- `spring-boot4-spring7-backend-architect`
- `postgres18-data-architecture-and-risk`
- `java-rest-api-testing-effective-java-mentor`
- `junit6-assertj-restassured-testcraft`
- `rest-api-security-oauth-testing`
- `nuxt-dashboard-zod-pinia-frontend-engineering`, jeśli dotykasz frontend
- `test-analysis-design-and-data`

## Czego NIE powtarzać

Nie tłumacz ponownie od podstaw:

- `given()` / `when()` / `then()`
- path params, headers i request body basics
- create/read Payment Order z Lesson 06
- list/filter/pagination z Lesson 07
- podstawowy Keycloak/JWT flow

Użyj tych tematów tylko jako prerequisites.

## Cel Lesson 08

Zaprojektuj i zaimplementuj mały read-only vertical slice:

Payment Order Aggregation / Summary

Główne pytanie:

Jak testować agregacje SQL i API summary tak, żeby REST Assured, AssertJ, controlled seed data, DB oracle i EXPLAIN redukowały realne ryzyka błędnych raportów?

## Scope Decision

Domyślna decyzja: Lesson Extension, nie Full Spec Kit.

Uzasadnienie:

- endpoint jest w istniejącym `payment` module,
- nie dodaje nowego REST resource lifecycle,
- nie dodaje nowej roli/security modelu,
- jest read-only extension po Lesson 07.

Jeśli podczas analizy okaże się, że scope wymaga nowej roli, nowego modelu bezpieczeństwa albo nowego modułu, zatrzymaj implementację i przygotuj Light/Full Spec Kit recommendation.

## Scope IN

- Existing payment module only.
- Read-only endpoint: `GET /api/merchants/{merchantId}/payment-orders/summary`.
- Merchant-scoped access.
- Platform reader support.
- Existing status `CREATED` only.
- Aggregates:
  - `totalOrders`
  - `totalAmountMinor`
  - `byCurrency[]`
  - `byStatus[]`
- Optional filters only if still small:
  - `currency`
  - `status`
  - `fromDate`
  - `toDate`
- `X-Correlation-ID` response header.
- REST Assured contract tests.
- Security matrix tests.
- AssertJ aggregate assertions.
- Minimal Nuxt UI summary panel only after backend tests pass, if time/scope allows.

## Scope OUT

- authorize/capture/cancel
- new payment statuses
- PSP integration or PSP mock
- Kafka/webhooks/event pipeline
- full business dashboard
- platform-wide analytics dashboard
- RLS
- `If-Match` / `412`
- rate limiting
- settlement, refunds, reconciliation, fraud metrics
- fake business KPIs not backed by API contract

## Candidate API

```http
GET /api/merchants/{merchantId}/payment-orders/summary
Accept: application/json
Authorization: Bearer <token>
```

Response:

```json
{
  "totalOrders": 3,
  "totalAmountMinor": 6000,
  "byCurrency": [
    { "currency": "PLN", "orderCount": 2, "totalAmountMinor": 3000 },
    { "currency": "EUR", "orderCount": 1, "totalAmountMinor": 3000 }
  ],
  "byStatus": [
    { "status": "CREATED", "orderCount": 3, "totalAmountMinor": 6000 }
  ]
}
```

## Backend Implementation Requirements

Implement minimal, readable code:

1. API DTO records:
   - `PaymentOrderSummaryResponse`
   - nested or separate `CurrencySummary`
   - nested or separate `StatusSummary`
2. Repository/service query using SQL/JPA projection with `GROUP BY`, `COUNT`, `SUM`.
3. `PaymentOrderSummaryService` with `@Transactional(readOnly = true)`.
4. Controller endpoint in `PaymentOrderController`.
5. Security consistent with Lesson 07 list endpoint:
   - `merchant:payments:read` own merchant -> `200`
   - `platform:payments:read` selected merchant -> `200`
   - cross-tenant merchant reader -> `403`
   - creator-only and operate-only -> `403`
6. Add Flyway index migration only if justified by actual query shape.
7. Preserve Spring Modulith boundaries.

## Frontend Requirements, Only If Backend Is Green

Minimal Nuxt scope:

1. Add server proxy:
   - `GET /api/merchants/{merchantId}/payment-orders/summary`
2. Add Zod/TS type for summary response.
3. Add small `PaymentOrderSummaryCards.vue`.
4. Add or prepare `/admin/merchants/[merchantId]/payments` as merchant-scoped payments panel.
5. Use dashboard layout and Nuxt UI components.
6. Do not add full dashboard, lifecycle buttons, fake PSP metrics, settlement or fraud KPIs.

## Required Tests

Backend REST Assured:

- `summaryForEmptyMerchantReturnsZeroTotals`
- `summaryForSeededMerchantReturnsCountsAndSums`
- `summaryGroupsByCurrency`
- `summaryGroupsByStatus`
- `summaryFilteredByCurrencyAffectsTotals`, if filters are implemented
- `summaryResponseIncludesCorrelationId`
- `invalidSummaryFilterReturns400`, if filters are implemented

Security:

- unauthenticated -> `401`
- denied identity -> `403`
- creator without read -> `403`
- operate-only -> `403`
- merchant reader own merchant -> `200`
- merchant reader other merchant -> `403`
- platform reader selected merchant -> `200`

Assertions:

- Use typed extraction: `extract().as(PaymentOrderSummaryResponse.class)`.
- Use AssertJ `tuple()` for grouped rows.
- Use `SoftAssertions` when asserting many totals in one response.
- Keep expected totals explicit and readable.

Test data:

- Create a fresh active merchant per test.
- Seed a deterministic set:
  - PLN 1000
  - PLN 2000
  - EUR 3000
  - USD 4000
- Expected totals:
  - total orders = 4
  - total amount = 10000
  - PLN = 2 / 3000
  - EUR = 1 / 3000
  - USD = 1 / 4000
  - CREATED = 4 / 10000

## Learning Note / Evidence Requirements

After code and tests pass:

1. Update `Lesson 08 - Payment Aggregation Summary.md` from planned to ready.
2. Add production/test evidence to `Lesson Evidence Tracker.md`.
3. Update `Current Lesson.md` and `Current Sprint.md` if Lesson 08 becomes active/completed.
4. Update `Learning Coverage Backlog.md` statuses only for topics with real evidence.
5. Include short interview answers in English.

## Verification

Run from `apps/backend`:

```bash
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummarySecurityTest test
./mvnw -Dtest="PaymentOrder*" test
./mvnw -Dtest=PaymentModuleTest test
```

If frontend changed, run from `apps/frontend`:

```bash
corepack pnpm typecheck
```

## Final Output

Zwróć:

1. Scope decision actually used.
2. Changed files.
3. Test coverage summary.
4. Commands run and results.
5. Guardrails preserved.
6. What remains deferred.
```
