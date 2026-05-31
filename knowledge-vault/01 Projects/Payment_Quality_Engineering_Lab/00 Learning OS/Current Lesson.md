---
type: learning-os
status: active
date: 2026-05-31
tags:
  - learning-os
  - current-lesson
---

# Current Lesson

> **Active Lesson:** 09 - Payment Orders Frontend Consumer and Contract Alignment
>
> **Status:** READY - frontend consumer slice implemented and verified
>
> **Next:** Review Lesson 09 evidence, then choose a focused follow-up only after the frontend consumer gap is accepted

## NOW: What To Learn

| Priority | Item | Type | Time |
|---|---|---|---|
| 1 | [[Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]] - review implementation evidence and explain the contract split | Review | 1 session |
| 2 | Compare REST Assured backend assertions with Playwright UI assertions | Review | 30 min |
| 3 | Decide the next follow-up: BOLA/BFLA deep dive, DB oracle practice, or contract docs | Planning | 30 min |

## COVERED: Lessons 1-5 (Foundations)

| Lesson | Topic | Status |
|---|---|---|
| 01 | REST API request/response flow | Covered |
| 02 | REST Assured — what it is | Covered |
| 03 | REST Assured foundations (method/endpoint/content-type basics) | Covered |
| 04 | Path params, query params, headers, `Authorization`, `X-Correlation-ID` | Covered |
| 05 | Request body, JSON, `Map.of`, DTO serialization | Covered |

## COVERED: From Lesson 06

| Topic | Evidence | Confidence |
|---|---|---|
| Idempotent payment order creation | `PaymentOrderService`, `PaymentOrderRestAssuredTest` | Strong |
| `Idempotency-Key` and request fingerprint | `IdempotencyKey`, `RequestFingerprint` | Strong |
| HTTP headers: `Location`, `ETag`, `X-Correlation-ID` | `PaymentOrderController`, REST Assured tests | Strong |
| `201` vs replay `200` vs `409 conflict` | Tests and controller | Strong |
| Role × tenant isolation matrix | `PaymentOrderSecurityTest` | Strong |
| Cross-tenant read returns masked `404` | Security tests | Strong |
| DB constraints for amount/currency/status | `V2__create_payment_orders.sql` | Strong |
| Flyway migration as executable DB contract | `V2__create_payment_orders.sql`, repo test | Strong |
| Spring Modulith module boundary | `MerchantPaymentEligibilityService` interface | Strong |
| Frontend as API consumer | `CreatePaymentOrderForm.vue`, server routes | Moderate |
| Test data ownership per test | Per-test merchant creation, unique keys | Strong |
| REST Assured body/header/status assertions | `PaymentOrderRestAssuredTest` | Strong |

## COVERED: From Lesson 07

| Topic | Evidence | Confidence |
|---|---|---|
| Payment order list/filter/search | `PaymentOrderListRestAssuredTest`, `PaymentOrderListService` | Strong |
| `queryParam()` for filters | `PaymentOrderListRestAssuredTest` | Strong |
| Typed extraction with `extract().as(...)` | `PaymentOrderListRestAssuredTest` | Strong |
| `RequestSpecBuilder` / `ResponseSpecBuilder` | `PaymentOrderListApiTestSupport` | Strong |
| Failure-only logging | `RestAssuredLoggingConfig` | Moderate |
| `WHERE`, `ORDER BY`, `LIMIT/OFFSET`, pagination count | `PaymentOrderSpecification`, `PaymentOrderListService` | Strong |
| Cross-tenant list returns `403` | Lesson 07 decision, tests pending as extension | Moderate |

## COVERED: From Lesson 08

| Topic | Evidence | Confidence |
|---|---|---|
| Payment order summary endpoint | `PaymentOrderSummaryService`, `PaymentOrderController` | Strong |
| SQL aggregation: `GROUP BY`, `COUNT`, `SUM` | `JpaPaymentOrderRepository`, summary tests | Strong |
| Summary contract tests | `PaymentOrderSummaryRestAssuredTest` | Strong |
| Summary business-flow oracle | `PaymentOrderSummaryBusinessFlowRestAssuredTest` | Strong |
| Summary security matrix | `PaymentOrderSummarySecurityTest` | Strong |
| Modulith boundary after summary | `PaymentModuleTest` | Strong |
| Package verification after testCompile fix | `./mvnw -DskipTests package` | Strong |

## COVERED: From Lesson 09

| Topic | Evidence | Confidence |
|---|---|---|
| Nuxt server proxy for payment list/summary | `apps/frontend/server/api/merchants/[merchantId]/payment-orders/*.get.ts` | Strong |
| Zod response schemas for backend consumer contracts | `apps/frontend/app/schemas/payment-order.schema.ts` | Strong |
| Typed Pinia payment order state | `apps/frontend/app/stores/payment-orders.ts` | Strong |
| Merchant-scoped payments panel | `apps/frontend/app/pages/admin/merchants/[merchantId]/payments/index.vue` | Strong |
| Summary/list UI without fake lifecycle actions | `PaymentOrderSummaryCards.vue`, `PaymentOrderListTable.vue` | Strong |
| Playwright happy/empty/forbidden/backend-unavailable UI states | `apps/frontend/tests/e2e/payment-orders-panel.spec.ts` | Strong |
| Backend list/summary regression guardrails for frontend consumer | REST Assured summary/list/security/business-flow commands | Strong |

## INTRODUCED: Seen But Not Yet Mastered

| Topic | Where | What You Still Need |
|---|---|---|
| Assertion strategy (RA body vs AssertJ vs DB query) | Lesson 06 §12a | Practice making the decision yourself |
| Database verification as test layer | Lesson 06 §12b | Write tests that probe DB directly |
| Idempotency concurrency/race conditions | `PaymentOrderIdempotencyConcurrencyTest` | Write your own concurrency scenario |
| `ETag` / `If-Match` / `412` | Lesson 06 headers discussion | Not yet implemented — deferred |
| AssertJ `extracting`, `filteredOn`, `tuple` | Limited in existing tests | Write your own complex extractions |
| REST Assured `RequestSpecification` reuse | `MerchantApiTestSupport` | Create your own spec builders |
| Business-readable test names (`@DisplayName`) | Concept introduced | Add `@DisplayName` to 3 tests |
| Negative-path first methodology | Concept introduced | Write negative test before happy path |
| Frontend consumer contract | Lesson 09 implementation | Practice explaining the backend contract vs UI consumer split without reading notes |
| Playwright UI state coverage | `payment-orders-panel.spec.ts` | Add future tests only when new UI behavior exists |
| Consumer-driven contract thinking | Lesson 09 evidence | Practice choosing REST Assured vs Playwright assertions for new cases |

## NEEDS PRACTICE: Exercises

| # | Exercise | Time |
|---|---|---|
| 1 | Explain why Lesson 09 did not add authorize/capture/cancel | 15 min |
| 2 | Explain how Zod protects a Nuxt consumer from backend response drift | 20 min |
| 3 | Walk through the forbidden UI test and identify what it does not prove | 20 min |
| 4 | Compare summary REST Assured assertions with payment panel Playwright assertions | 30 min |
| 5 | Review route collision fix: why `merchants/index.vue` was needed for nested routes | 20 min |

## DEFERRED: Do NOT Study Now

| Topic | When |
|---|---|
| Payment lifecycle (authorize/capture/cancel) | Future Spec Kit after frontend consumer gap is closed |
| PSP integration | Spec Kit 005+ |
| Kafka, webhooks, event pipeline | Sprint 10+ |
| GraphQL, gRPC | Sprint 13+ |
| Performance/load testing | Sprint 13b |
| JSON Schema / OpenAPI validation | Sprint 10b |
| Contract testing (Pact/WireMock) | Sprint 10+ |
| `If-Match` / `412` / optimistic concurrency | Spec Kit 004+ |
| RLS (Row-Level Security) | Sprint 9 extension |
| Complete OAuth/OIDC | Phase 0 guardrail — never |
| Complete business dashboards | Phase 0 guardrail — never |

## Evidence Checklist

- [x] Lesson 08 production evidence captured
- [x] Lesson 08 REST/security/business-flow tests exist and pass
- [x] Lesson 08 package and Modulith verification pass
- [x] Lesson 09 note exists: [[Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]]
- [x] Lesson 09 prompt exists: [[Prompt - Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]]
- [x] Lesson 09 frontend implementation completed
- [x] Lesson 09 Playwright tests completed
- [x] Lesson 09 frontend typecheck passes
- [x] Lesson 09 backend regression guardrails captured
- [x] Lesson 09 evidence captured after implementation

## Navigation

- [[Current Learning Flow]] — process and flow
- [[Current Sprint]] — sprint status
- [[Curriculum Backbone]] — technology ↔ lesson map
- [[Lesson Evidence Tracker]] — detailed evidence
- [[Learning Progress Board]] — overall progress
