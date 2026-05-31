---
type: tracker
status: active
project: Payment Quality Engineering Lab
area: Learning Governance
date: 2026-05-31
tags:
  - lesson-evidence
  - learning-delta
  - sdet
  - qa-architecture
---

# Lesson Evidence Tracker

Cel: każda lekcja lub sprint ma mieć dowód, że temat został przerobiony w kodzie, testach, analizie albo świadomie odłożony.

> **Navigation:** [[START HERE - Learning Dashboard]] | [[Learning Progress Board]] | [[Home]]
>
> **Current Lesson:** Lesson 09 (READY - frontend consumer and contract alignment implemented)

## What NOT To Touch Yet

These are explicitly deferred or out of scope. Do NOT study, implement, or research them now.

| Topic | Status | When Allowed |
|---|---|---|
| Payment lifecycle (authorize/capture/cancel) | DEFERRED | Spec Kit 004+ |
| PSP integration | DEFERRED | Spec Kit 005+ |
| Kafka, webhooks, event pipeline | DEFERRED | Sprint 10+ |
| GraphQL, gRPC | DEFERRED | Sprint 13+ |
| Performance/load testing | DEFERRED | Sprint 13b |
| Observability beyond correlation ID | DEFERRED | Sprint 12b |
| JSON Schema / OpenAPI validation | DEFERRED | Sprint 10b |
| Contract testing (Pact/WireMock) | DEFERRED | Sprint 10+ |
| Complete OAuth/OIDC | GUARDRAIL | Phase 0 — never in current scope |
| Complete business dashboards | GUARDRAIL | Phase 0 — never in current scope |
| `If-Match` / `412` / optimistic concurrency | DEFERRED | Spec Kit 004+ (when lifecycle actions exist) |
| RLS (Row-Level Security) | DEFERRED | Sprint 9 extension |

## Lesson 07 — Payment Order List, Filter, Search + RA Framework Architecture

**Data:** 2026-05-28
**Status:** Ready — code implemented, tests passing

### Production code evidence
- `web/PaymentOrderListRequest.java` — record z walidacją query params
- `web/PaymentOrderListResponse.java` — record: content, page, size, totalElements, totalPages
- `web/PaymentOrderListMapper.java` — mapuje Page<PaymentOrder> → DTO
- `infrastructure/PaymentOrderSpecification.java` — 6 static Specification builderów
- `application/PaymentOrderListService.java` — @Transactional(readOnly), dynamic query z addIfNotNull()
- `web/PaymentOrderController.java` — dodany @GetMapping listPaymentOrders z 10 @RequestParam
- `infrastructure/JpaPaymentOrderRepository.java` — rozszerzony o JpaSpecificationExecutor
- `web/PaymentExceptionHandler.java` — dodane handler dla BindException, IllegalArgumentException, DateTimeParseException
- `shared/security/SecurityConfig.java` — dodany matcher GET /api/merchants/*/payment-orders
- `db/migration/payment/V3__add_payment_order_list_indexes.sql` — 2 indeksy IF NOT EXISTS

### Test code evidence
- `rest/PaymentOrderListRestAssuredTest.java` — 10 contract tests (all pass)
- `testsupport/PaymentOrderListApiTestSupport.java` — seed + RequestSpecBuilder + ResponseSpecBuilder
- `testsupport/PaymentOrderAssertions.java` — custom AssertJ: hasOnlyStatus, allAmountsGreaterThan, hasPageMetadata
- `testsupport/RestAssuredLoggingConfig.java` — failure-only logging

### Spec Kit artifacts
- `specs/004-payment-order-list-filter/spec.md` — 29 FR, 12 SC, 5 user stories
- `specs/004-payment-order-list-filter/plan.md` — 14-section implementation plan
- `specs/004-payment-order-list-filter/tasks.md` — 37 tasks across 9 phases

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 07 - Payment Order List Filter Search.md` — 18-section lesson note

### Commands run
- `./mvnw -Dtest=PaymentOrderListRestAssuredTest test` — 10/10 pass
- `./mvnw -Dtest="PaymentOrder*" test` — 45/45 pass (35 existing + 10 new)
- `./mvnw -Dtest=PaymentModuleTest test` — Modulith architecture pass

### Competency updates
22 topics moved from Not Started/Introduced → Practiced:
- RA: queryParam(), accept(), extract().as(), RequestSpecBuilder, ResponseSpecBuilder, log().ifValidationFails()
- AssertJ: extracting, filteredOn, tuple, usingRecursiveComparison, SoftAssertions
- JUnit: @ParameterizedTest, @CsvSource, @Nested, @Tag, @DisplayName
- SQL: WHERE, ORDER BY, LIMIT, indexes, COUNT, JpaSpecificationExecutor
- Test Design: decision tables, negative-path first

### Open risks
- Cross-tenant list security tests not implemented (T022-T023 pending)
- @ParameterizedTest not yet used (planned for extension)
- Authorization masking in logs (simplified config — full masking deferred)

### Interview answer EN
Created (5 questions in Lesson 07 §15):
1. Cross-tenant list 403 vs 404
2. JpaSpecificationExecutor dynamic query
3. "Other specification must not be null" fix
4. extract().path() vs extract().as(Class)
5. merchant:payments:create doesn't grant list access

### Next sprint handoff
Lesson 08: Payment Aggregation (GROUP BY, COUNT per status, EXPLAIN)

## Lesson 08 — Payment Aggregation Summary

**Data:** 2026-05-30
**Status:** Ready - summary backend and summary REST/security/business-flow tests pass

### Prompt
- `../Learning Prompts/Prompt - Lesson 08 - Payment Aggregation Summary.md`

### Business capability
Read-only merchant-scoped payment order summary over existing `payment_orders`: total orders, total amount in minor units, breakdown by currency and breakdown by status.

### Learning delta
- `GROUP BY`, `COUNT`, `SUM(amount_minor)`
- aggregation response DTOs and repository projections
- controlled seed dataset as aggregation oracle
- AssertJ `tuple()` / `SoftAssertions` for grouped totals
- `EXPLAIN` as SQL diagnostics
- summary security matrix reusing `merchant:payments:read` / `platform:payments:read`

### Production code evidence
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryRequest.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`

### Test code evidence
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java` - 10 contract tests for empty summary, seeded totals, filters, validation and correlation header.
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryBusinessFlowRestAssuredTest.java` - 3 business-flow tests for deterministic aggregate oracle, cross-tenant denial and platform reader access.
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummarySecurityTest.java` - 7 security tests for unauthenticated, denied, create-only, operate-only, own merchant, cross-tenant and platform reader access.
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderSummaryApiTestSupport.java` - deterministic seed data, expected aggregate calculation and reusable request support.

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 08 - Payment Aggregation Summary.md`

### Commands run
- `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` - passed, 20 tests.
- `./mvnw -DskipTests package` - passed.
- `./mvnw -Dtest=PaymentModuleTest test` - passed, 2 tests.

### Verification blocker
- Resolved before Lesson 09 planning. Prior `testCompile` blocker in `MyPaymentOrderBusinessFlowRestAssuredTest.java` no longer blocks package/module verification.

### Guardrails
- Do not add authorize/capture/cancel lifecycle actions.
- Do not add PSP integration or PSP mock flows.
- Do not add Kafka/webhooks.
- Do not add new payment statuses only to make aggregation more interesting.
- Do not build a complete business dashboard.
- Do not add test implementation in this system-only slice.

### Interview answer EN
Current:
> Lesson 8 extends payment order list/read behavior with a read-only summary endpoint. The backend system slice is implemented with DB-side aggregation, summary-specific security matcher ordering, and tenant ownership enforcement. Test automation for summary is intentionally deferred to a separate tester slice.

### Descriptive lesson materials (2026-05-30)

Created 5 focused descriptive lessons in the Technical Learning vault, each covering a different aspect of Lesson 08:

| Area | File |
|---|---|
| Java | `02 Areas/Technical Learning/Java 25 For SDET/Lesson 08 - Java Records, Read-Only Services, and Input Validation.md` |
| REST testing | `02 Areas/Technical Learning/JUnit REST Assured/Lesson 08 - Aggregation Contract, Security, and Business Flow Tests.md` |
| SQL | `02 Areas/Technical Learning/PostgreSQL and SQL From Zero/Lesson 08 - GROUP BY COUNT SUM Null Semantics in Aggregation Queries.md` |
| HTTP/API | `02 Areas/Technical Learning/REST API From Zero/Lesson 08 - Summary Endpoint Contract, Status Codes, and Error Taxonomy.md` |
| Business logic | `02 Phase 2 - Payment Orders/Lesson 08 - Business Logic, Decision Tables, and Risk Notes.md` |

Each lesson follows the Learning OS 11-section template (goal → code map → concepts → walkthrough → delta vs L07 → mistakes → exercises → questions → testing → next links), with direct references to real production and test files from Lesson 08.

### Next lesson/sprint handoff
Lesson 09 should close the frontend consumer gap for existing payment list/summary APIs before any payment lifecycle work.

## Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment

**Data:** 2026-05-31
**Status:** Ready - frontend consumer slice implemented and verified

### Prompt
- `../Learning Prompts/Prompt - Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment.md`

### Business capability
Typed Nuxt Dashboard consumer for existing merchant-scoped payment order list and summary endpoints: summary cards, list table, empty/loading/forbidden states, and frontend tests.

### Learning delta
- Nuxt server proxy for existing GET endpoints with query params and access token forwarding.
- Zod response schemas for backend contract consumption.
- Typed Pinia store instead of `any` for payment responses.
- Playwright UI state tests for happy, empty and forbidden states.
- Consumer-driven contract thinking without adding Pact/WireMock yet.
- Clear split between REST Assured backend contract assertions and Playwright UI assertions.

### Skills expected
- `payment-quality-lab-orchestrator`
- `business-analysis-and-product-discovery-for-payment-lab`
- `qa-architecture-sprint-team`
- `nuxt-dashboard-zod-pinia-frontend-engineering`
- `typescript6-playwright-engineering`
- `rest-api-security-oauth-testing`
- `junit6-assertj-restassured-testcraft`
- `parallel-test-architecture-and-data-isolation`
- `test-analysis-design-and-data`

### Production code evidence
- `apps/frontend/app/schemas/payment-order.schema.ts` - Zod schemas and inferred types for create/read/list/summary/backend error responses.
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/index.get.ts` - Nuxt server proxy for backend list endpoint with query params and access token forwarding.
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/summary.get.ts` - Nuxt server proxy for backend summary endpoint.
- `apps/frontend/app/stores/payment-orders.ts` - typed Pinia state/actions for list, summary, current order, last created order, loading/error/forbidden state.
- `apps/frontend/app/components/payment/PaymentOrderSummaryCards.vue` - summary cards for existing backend totals and breakdowns.
- `apps/frontend/app/components/payment/PaymentOrderListTable.vue` - payment order list table and empty state.
- `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` - merchant-scoped payments panel with loading, empty, forbidden and backend-unavailable states.
- `apps/frontend/app/pages/admin/merchants/index.vue` - merchant registry page moved to index route so nested payment panel routes resolve correctly.
- `apps/frontend/app/components/payment/CreatePaymentOrderForm.vue` - create response now parsed with `paymentOrderResponseSchema` before updating typed store state.

### Test code evidence
- `apps/frontend/tests/e2e/payment-orders-panel.spec.ts` - 4 Playwright tests for summary/list rendering, empty merchant state, forbidden no-data state and backend-unavailable state.

### Vault notes
- `02 Phase 2 - Payment Orders/Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment.md`

### Spec Kit artifacts
- `specs/006-payment-orders-frontend-consumer/spec.md` - light lesson-extension specification for frontend consumer scope.
- `specs/006-payment-orders-frontend-consumer/plan.md` - implementation plan and guardrails.
- `specs/006-payment-orders-frontend-consumer/tasks.md` - completed task checklist for the frontend consumer slice.

### Commands run
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` - passed, 20 tests.
- `cd apps/backend && ./mvnw -DskipTests package` - passed.
- `cd apps/backend && ./mvnw -Dtest=PaymentModuleTest test` - passed, 2 tests.
- `cd apps/backend && ./mvnw -Dtest=PaymentOrderListRestAssuredTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` - command completed without failure report; output was truncated by tool capture.
- `cd apps/frontend && corepack pnpm typecheck` - passed.
- `cd apps/frontend && corepack pnpm test:e2e -- payment-orders-panel.spec.ts` - passed, 4 tests.

### Open risks
- Playwright tests route-mock Nuxt API responses; they verify UI states, not live backend integration.
- The combined backend list/summary regression command output was truncated by tool capture, although no failure report was returned.
- Keycloak setup docs may still be stale for payment roles/users; no new roles/users were added for Lesson 09.

### Interview answer EN
> Lesson 9 turns existing payment order list and summary REST contracts into a typed Nuxt Dashboard consumer. It deliberately avoids new lifecycle behavior and focuses on frontend contract alignment: Zod schemas, typed Pinia state, role-aware UI states, Playwright tests, and REST Assured regression as backend truth.

### Next lesson/sprint handoff
After Lesson 09, choose between BOLA/BFLA deep dive, DB oracle/EXPLAIN practice, or formal Spec Kit for payment lifecycle only if the guardrails are updated.

## Template Dla Nowej Lekcji/Sprintu

```text
## Lesson NN - Title

Status:
Prompt:
Business capability:
Learning delta:
Skills expected:
Skills actually used:
Production code evidence:
Test code evidence:
Vault notes:
Spec Kit artifacts:
Commands run:
Competency matrix updates:
Open risks:
Interview answer EN:
Next lesson/sprint handoff:
```

## Lesson 01-05 Summary

| Lesson | Status | Evidence | Notes |
|---:|---|---|---|
| 01 | Introduced | REST API request/response flow prompt and vault note | Foundation; no need to repeat in Lesson 6 |
| 02 | Introduced | REST Assured entry prompt | Foundation; no need to repeat in Lesson 6 |
| 03 | Practiced | REST Assured foundations doc | HTTP method/endpoint/content-type basics |
| 04 | Practiced | Lesson 4 prompt + lesson-pack expansion | Path/query/header basics, `Authorization`, `X-Correlation-ID` context |
| 05 | Practiced | Lesson 5 prompt + lesson-pack expansion | request body, JSON, `Map.of`, DTO, serialization |

## Lesson 06 - Payment Order Create/Read Foundation

Status: `Ready`

Prompt: `../Learning Prompts/Prompt - Lesson 06 - PayU Like Business Flow Expansion Sprint.md` and current interactive prompt for Payment Order create/read lesson.

Business capability: Payment Order create/read foundation with idempotent creation, merchant-scoped access, platform read access, PostgreSQL/Flyway persistence, Keycloak role/claim model, minimal frontend consumer and REST Assured tests.

Learning delta:

- no repetition of `given/when/then`, path params, basic headers and request body basics,
- first payment-specific REST resource,
- `Idempotency-Key` and request fingerprint,
- `Location`, `ETag`, `X-Correlation-ID`, `201`, replay `200`, `403`, masked `404`, `409`,
- role authorization plus `merchant_id` ownership,
- SQL constraints for amount, currency, status and idempotency uniqueness,
- Flyway migration as executable DB contract,
- REST Assured contract tests for headers/body/status/error code,
- security matrix tests for create/read access,
- frontend as API consumer preserving stable idempotency key.

Skills expected:

- `qa-architecture-sprint-team`,
- `obsidian-learning-os`,
- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `postgres18-data-architecture-and-risk`,
- `spring-modulith-2-0-6-modular-monolith-testing`.

Skills actually used:

- `qa-architecture-sprint-team`,
- `obsidian-learning-os`,
- `java-rest-api-testing-effective-java-mentor`,
- `junit6-assertj-restassured-testcraft`,
- `postgres18-data-architecture-and-risk`,
- `spring-modulith-2-0-6-modular-monolith-testing`.

Production code evidence:

- `apps/backend/src/main/java/lab/paymentquality/payment/`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibility.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/MerchantPaymentEligibilityService.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantPaymentEligibilityAdapter.java`
- `apps/backend/src/main/resources/db/migration/payment/V2__create_payment_orders.sql`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/frontend/app/components/payment/`
- `apps/frontend/server/api/merchants/[merchantId]/payment-orders/`
- `infra/keycloak/realms/payment-quality-realm.json`

Test code evidence:

- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderServiceTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/application/PaymentOrderIdempotencyConcurrencyTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepositoryTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/PaymentModuleTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentApiTestSupport.java`

Vault notes:

- `../02 Phase 2 - Payment Orders/Phase 2 - Payment Orders.md`
- `../02 Phase 2 - Payment Orders/Lesson 06 - Payment Order Create Read Foundation.md`
- `../Learning Governance/Expert Gap Analysis - Senior SDET Coverage.md`

Spec Kit artifacts:

- `specs/003-payment-order-access-lifecycle/spec.md`
- `specs/003-payment-order-access-lifecycle/plan.md`
- `specs/003-payment-order-access-lifecycle/data-model.md`
- `specs/003-payment-order-access-lifecycle/contracts/payment-order-api.md`
- `specs/003-payment-order-access-lifecycle/quickstart.md`
- `specs/003-payment-order-access-lifecycle/tasks.md`

Commands run:

- `./mvnw test -q` in `apps/backend` - passed.
- `corepack pnpm typecheck` in `apps/frontend` - passed.

Competency matrix updates: updated after Payment Order create/read scope materialized.

Open risks:

- current lesson note is ready, but REST Assured foundation pack can still be extended with a cross-link instead of duplicating content,
- future lifecycle topics remain deferred: authorize/capture/cancel, `If-Match`, `412`, PSP integration, Kafka, webhooks and settlement,
- frontend E2E is optional verification and was not run during this lesson note capture.

Interview answer EN:

> In Lesson 6 I moved from syntax-driven REST Assured practice to product-risk-driven API testing. I can explain and test an idempotent Payment Order create/read API, including retry behavior, tenant isolation, role and claim checks, database constraints, Flyway migrations, HTTP headers and REST Assured contract assertions.

Next lesson/sprint handoff: deepen REST Assured reusable response/error specifications or continue with payment lifecycle only after a new Spec Kit scope explicitly allows transitions and optimistic concurrency.
