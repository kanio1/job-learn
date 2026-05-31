---
type: learning-os
status: active
date: 2026-05-30
tags:
  - learning-os
  - current-lesson
---

# Current Lesson

> **Active Lesson:** 08 — Payment Aggregation Summary
>
> **Status:** PLANNED — scope and prompt ready, implementation not started
>
> **Next:** Implement read-only summary endpoint and aggregation tests

## NOW: What To Learn

| Priority | Item | Type | Time |
|---|---|---|---|
| 1 | [[Lesson 08 - Payment Aggregation Summary]] — understand scope, API, SQL and test strategy | Study | 1 session |
| 2 | [[Prompt - Lesson 08 - Payment Aggregation Summary]] — use as implementation prompt | Execution | 1 sprint |
| 3 | SQL aggregation: `GROUP BY`, `COUNT`, `SUM`, `EXPLAIN` on `payment_orders` | Practice | 1 session |
| 4 | Aggregation test oracle with controlled seed data | Practice | 1 session |
| 5 | Security matrix for summary endpoint | Practice | 30 min |
| 6 | Optional minimal Nuxt summary panel after backend is green | Extension | 1 session |

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
| Aggregation oracle design | Lesson 08 plan | Build controlled seed data and expected totals |
| `GROUP BY` / `SUM` / `EXPLAIN` | Lesson 08 plan | Implement summary endpoint and SQL exercises |

## NEEDS PRACTICE: Exercises

| # | Exercise | Time |
|---|---|---|
| 1 | Read Lesson 08 and explain why it is not a lifecycle sprint | 15 min |
| 2 | Write expected totals for the controlled aggregation dataset | 20 min |
| 3 | Draft REST Assured assertions for `byCurrency` and `byStatus` | 30 min |
| 4 | Fill summary security matrix before coding | 20 min |
| 5 | Run Lesson 07 list tests as baseline before summary implementation | 20 min |
| 6 | Run one `EXPLAIN` for a merchant-scoped aggregation query | 30 min |

## DEFERRED: Do NOT Study Now

| Topic | When |
|---|---|
| Payment lifecycle (authorize/capture/cancel) | Spec Kit 004+ |
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

- [x] Lesson 08 note exists: [[Lesson 08 - Payment Aggregation Summary]]
- [x] Lesson 08 prompt exists: [[Prompt - Lesson 08 - Payment Aggregation Summary]]
- [x] Lesson 06 and Lesson 07 evidence captured
- [x] Lesson 08 scope guardrails captured
- [ ] Production code evidence captured for Lesson 08
- [ ] Test code evidence captured for Lesson 08
- [ ] Verification commands pass for Lesson 08
- [ ] Competency matrix updated after implementation evidence exists
- [ ] Interview answer finalized after implementation evidence exists
- [ ] Lesson 08 implementation completed
- [ ] Lesson 08 evidence captured

## Navigation

- [[Current Learning Flow]] — process and flow
- [[Current Sprint]] — sprint status
- [[Curriculum Backbone]] — technology ↔ lesson map
- [[Lesson Evidence Tracker]] — detailed evidence
- [[Learning Progress Board]] — overall progress
