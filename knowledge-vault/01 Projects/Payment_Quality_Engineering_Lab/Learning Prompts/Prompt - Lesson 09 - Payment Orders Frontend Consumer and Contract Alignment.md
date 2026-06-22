---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 09
date: 2026-05-31
tags:
  - prompt
  - lesson-09
  - payment-order
  - frontend
  - nuxt
  - zod
  - pinia
  - playwright
  - rest-assured
  - keycloak
  - qa-architecture
---

# Prompt - Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment

Copy this prompt and give it to Kilo when starting Lesson 09 implementation.

```text
Jestes moim zespołem: Business Analyst, Frontend Architect, Backend Architect, QA Architect, Senior SDET Mentor i Agent Kodowania.

Pracujemy w repozytorium:

/home/suso/job-learn

## Kontekst

Przeczytaj przed rozpoczeciem:

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
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment.md`
- `specs/004-payment-order-list-filter/`
- `specs/005-payment-order-summary/`

Przeczytaj kod:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryBusinessFlowRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummarySecurityTest.java`
- `apps/frontend/app/schemas/payment-order.schema.ts`
- `apps/frontend/app/stores/payment-orders.ts`
- `apps/frontend/app/components/payment/`
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/`
- `apps/frontend/tests/e2e/payment-order-create.spec.ts`
- `apps/frontend/tests/e2e/payment-order-read.spec.ts`
- `apps/frontend/tests/e2e/payment-order-auth-deny.spec.ts`
- `infra/keycloak/realms/payment-quality-realm.json`

## Skills do uzycia

Uzyj skills:

- `payment-quality-lab-orchestrator`
- `business-analysis-and-product-discovery-for-payment-lab`
- `qa-architecture-sprint-team`
- `nuxt-dashboard-zod-pinia-frontend-engineering`
- `typescript6-playwright-engineering`
- `rest-api-security-oauth-testing`
- `junit6-assertj-restassured-testcraft`
- `parallel-test-architecture-and-data-isolation`
- `test-analysis-design-and-data`
- `spring-boot4-spring7-backend-architect`, tylko do sprawdzenia backend contract boundaries

## Czego NIE powtarzac

Nie tlumacz ponownie od podstaw:

- `given()` / `when()` / `then()`
- path params, query params, headers i request body basics
- create/read Payment Order z Lesson 06
- list/filter/pagination z Lesson 07
- SQL aggregation z Lesson 08
- podstawowy Keycloak/JWT flow

Uzyj tych tematow tylko jako prerequisites.

## Cel Lesson 09

Zaprojektuj i zaimplementuj maly frontend consumer slice:

Payment Orders Frontend Consumer and Contract Alignment

Glowne pytanie:

Jak zbudowac typed Nuxt Dashboard consumer dla istniejacych list/summary REST APIs tak, zeby UI, Zod, Pinia, Playwright, Keycloak token forwarding i REST Assured regression tests tworzyly spojny system nauki bez dodawania nieistniejacego biznesu?

## Scope Decision

Domyslna decyzja: Lesson Extension, nie Full Spec Kit.

Uzasadnienie:

- backend endpoints juz istnieja,
- nie dodajemy nowego backend resource lifecycle,
- nie dodajemy nowych roles ani claims,
- nie dodajemy lifecycle actions ani PSP,
- rozwijamy frontend jako realnego consumer istniejacych kontraktow.

Jesli analiza wykaze, ze potrzebne sa nowe backend role, nowe statusy, nowy lifecycle, albo nowy model danych, zatrzymaj implementacje i przygotuj Spec Kit recommendation zamiast kodowac.

## Scope IN

- Nuxt server proxy dla:
  - `GET /api/merchants/{merchantId}/payment-orders`
  - `GET /api/merchants/{merchantId}/payment-orders/summary`
- Query params dla listy: `status`, `currency`, `fromDate`, `toDate`, `minAmount`, `maxAmount`, `clientOrderReference`, `page`, `size`, `sort`.
- Query params dla summary: `currency`, `status`, `fromDate`, `toDate`.
- Zod schemas i TS typy dla:
  - payment order response,
  - payment order list response,
  - payment order summary response,
  - backend error response.
- Typed Pinia store zamiast `any` dla list/summary/current order.
- Minimal payments panel:
  - summary cards,
  - list table,
  - empty state,
  - loading state,
  - `403` insufficient authority state,
  - backend unavailable state.
- Playwright tests dla UI states.
- Backend regression commands dla REST Assured summary/list/security.
- Evidence update w vault.

## Scope OUT

- authorize/capture/cancel
- nowe payment statuses
- PSP integration lub PSP mock
- Kafka/webhooks/event pipeline
- refunds, settlement, reconciliation
- complete business dashboard
- fake KPIs, fraud metrics, conversion metrics
- RLS
- `If-Match` / `412`
- new Keycloak roles/users unless existing local docs must be corrected
- Pact/WireMock contract testing
- backend refactor unrelated to frontend consumer

## Frontend Implementation Requirements

Minimal, readable implementation:

1. Extend `apps/frontend/app/schemas/payment-order.schema.ts`:
   - keep create schema,
   - add `paymentOrderResponseSchema`,
   - add `paymentOrderListResponseSchema`,
   - add `paymentOrderSummaryResponseSchema`,
   - export inferred types.
2. Add server proxy routes:
   - `apps/frontend/server/api/merchants/[merchantId]/payment-orders/index.get.ts`
   - `apps/frontend/server/api/merchants/[merchantId]/payment-orders/summary.get.ts`
3. Use the existing session/access token pattern:
   - require session,
   - fail with `missing_access_token` when absent,
   - forward `Authorization: Bearer <accessToken>`.
4. Preserve query params exactly enough for backend filters.
5. Normalize backend errors consistently with existing payment create/read routes.
6. Update `apps/frontend/app/stores/payment-orders.ts`:
   - remove `any` for payment order list/summary/current order,
   - track `loading`, `error`, `summary`, `list`, `currentOrder`, `lastCreatedOrder`,
   - provide reset actions.
7. Add small Nuxt UI components:
   - `PaymentOrderSummaryCards.vue`,
   - `PaymentOrderListTable.vue`.
8. Add page:
   - `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue`.
9. Keep UI aligned with Nuxt UI Dashboard Template.
10. Do not add lifecycle buttons or fake analytics.

## Required Tests

Frontend Playwright:

- `paymentsPanelRendersSummaryAndList`
- `paymentsPanelShowsEmptyStateForNoOrders`
- `paymentsPanelShowsForbiddenStateWithoutPaymentReadRole`
- `paymentsPanelDoesNotRenderPaymentDataOnForbidden`
- `paymentsPanelCanPassCurrencyFilterToSummaryOrList`, if UI exposes filter

Recommended test approach:

- Use route-mocked API responses for UI state coverage.
- Use backend regression tests for API truth.
- Do not duplicate every REST Assured body assertion in Playwright.

Backend regression commands:

- `PaymentOrderListRestAssuredTest`
- `PaymentOrderSummaryRestAssuredTest`
- `PaymentOrderSummaryBusinessFlowRestAssuredTest`
- `PaymentOrderSummarySecurityTest`
- `PaymentModuleTest`

## Acceptance Criteria

1. Merchant payment reader with valid session can load the payments panel for their merchant.
2. The panel renders `totalOrders`, `totalAmountMinor`, `byCurrency`, and `byStatus` from backend summary.
3. The panel renders list rows and pagination metadata from backend list response.
4. Empty merchant renders an explicit empty state, not an error.
5. `403` renders an insufficient-authority state and no payment data.
6. Frontend schemas/types do not use `any` for payment order list/summary/current order.
7. Server proxies forward the backend access token and query params.
8. Existing REST Assured summary/list/security tests remain green.
9. Frontend `typecheck` is green.
10. Playwright tests cover happy, empty and forbidden UI states.

## Verification Commands

Backend:

```bash
cd apps/backend
./mvnw -DskipTests package
./mvnw -Dtest=PaymentModuleTest test
./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
```

Frontend:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## Evidence Update Required

After implementation:

1. Update `Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment.md` with actual files and commands.
2. Update `Lesson Evidence Tracker.md` with production/test evidence.
3. Update `Current Lesson.md` and `Current Sprint.md` if Lesson 09 becomes ready.
4. Record residual risks, especially any skipped Playwright/full-backend integration coverage.
```
