---
type: lesson
status: ready
project: Payment Quality Engineering Lab
phase: 2
lesson: 10
area: Payment Orders
module: REST HTTP Contract Hardening and Authorization Matrix
date: 2026-06-02
tags:
  - lesson
  - lesson-10
  - payment-quality-lab
  - payment-order
  - rest
  - http
  - rest-assured
  - keycloak
  - security-testing
  - senior-sdet
---

# Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix

> **Status:** READY - implementation complete, all tests pass.
>
> **Navigation:** [[START HERE - Learning Dashboard]] | [[Current Lesson]] | [[Current Sprint]] | [[Lesson Evidence Tracker]]
>
> **Main decision:** Lesson 10 is not a new payment lifecycle sprint. It hardens existing Payment Order REST contracts and security tests so the system teaches senior-level HTTP, REST Assured and authorization thinking before any new business behavior.
>
> **Implementation date:** 2026-06-02
>
> **Test results:** 41 tests pass (7 HTTP edge + 12 authorization matrix + 22 regression)

## 1. Cel Lekcji

Lekcja 10 ma zamknac najwazniejsza luke po Lessons 06-09: mamy juz create/read, list/filter, summary i frontend consumer, ale brakuje swiadomego hardeningu kontraktu HTTP oraz parametryzowanej macierzy authorization dla istniejacych endpointow.

Celem jest dopisanie testow, ktore ucza senior REST/API mindset: content negotiation, unsupported methods, route ambiguity, malformed path variables, conditional headers that should not affect summary, stable error envelope, role/claim matrix oraz rozroznienie BOLA/BFLA.

## 2. Co Budujemy / Co Cwiczymy

Capability edukacyjna:

- Backend-only REST/HTTP contract hardening for existing Payment Order endpoints.
- No new production endpoint unless a test exposes a real bug that must be fixed.
- Primary target: `GET /api/merchants/{merchantId}/payment-orders/summary` because it now connects backend, security, REST Assured and frontend consumer learning.
- Secondary comparison targets: payment order list and single read only when needed to explain 403 vs masked 404.

System batch:

| Batch | Scope | Expected files |
|---|---|---|
| 10A | Summary HTTP edge contract tests | `PaymentOrderSummaryHttpContractRestAssuredTest.java` or extension of summary contract test |
| 10B | Parameterized summary authorization matrix | `PaymentOrderSummaryAuthorizationMatrixTest.java` or refactor of `PaymentOrderSummarySecurityTest` |
| 10C | DB oracle and aggregation diagnostics | repository/service-level summary aggregation test or documented EXPLAIN exercise |
| 10D | Contract documentation/OpenAPI readiness review | vault/spec note only, no Pact/WireMock yet |

Default implementation for Lesson 10 should do **10A + 10B**. Batch 10C is optional if time remains. Batch 10D is documentation/readiness only.

## 3. Learning Delta Wzgledem Poprzednich Lekcji

| Temat | Status |
|---|---|
| `Accept` header and content negotiation | New, must-have HTTP senior topic |
| Unsupported method semantics: `405 Method Not Allowed` vs auth failure | New |
| Malformed path variable and stable `400 validation` response | Extension of Lesson 08 error taxonomy |
| Route collision: `/summary` must never behave like `{paymentOrderId}` | New guardrail from Lesson 08 implementation risk |
| Summary has no `ETag`; conditional headers should not imply cache support | Extension of Lesson 06 headers |
| Parameterized authorization matrix | New JUnit/REST Assured framework maturity topic |
| BOLA vs BFLA for collection/report endpoint | Extension of Lesson 06 security matrix |
| REST Assured request/response specs and reusable error assertions | Extension of Lesson 07 framework architecture |
| Playwright UI denied-state coverage | Prerequisite from Lesson 09, not repeated |

## 4. Mapa Kodu

Existing production code to read before adding tests:

| File | Why it matters |
|---|---|
| `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java` | Owns create/read/list/summary route mappings and merchant ownership checks |
| `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java` | Matcher ordering: `/summary` must appear before `/{paymentOrderId}` wildcard |
| `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java` | Stable payment error envelope for validation/forbidden/not_found/conflict |
| `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentOrderSummaryService.java` | Summary validation and read-only aggregation orchestration |
| `apps/backend/src/main/java/lab/paymentquality/payment/internal/infrastructure/JpaPaymentOrderRepository.java` | JPQL aggregate queries for totals and grouped rows |
| `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java` | Token/claim variants for security matrix |
| `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderSummaryApiTestSupport.java` | Existing summary request spec and deterministic seed oracle |

Existing tests to avoid duplicating blindly:

| File | Existing coverage |
|---|---|
| `PaymentOrderSummaryRestAssuredTest.java` | Happy/empty/filter/validation/correlation/no ETag summary contract |
| `PaymentOrderSummaryBusinessFlowRestAssuredTest.java` | Deterministic aggregation oracle and platform/cross-tenant behavior |
| `PaymentOrderSummarySecurityTest.java` | Hand-coded summary 401/403/own/platform matrix |
| `PaymentOrderListRestAssuredTest.java` | List filters, pagination, sort, validation |
| `PaymentOrderSecurityTest.java` | Create/read role and tenant isolation matrix |

## 5. Architecture Walkthrough

Module owner remains `payment`. Lesson 10 should not create a new module, new role, new endpoint or new payment status.

Recommended backend design posture:

| Concern | Decision |
|---|---|
| Transaction boundary | Existing read-only summary service remains unchanged unless a bug is found |
| Security boundary | Backend is source of truth; UI hiding from Lesson 09 is not security |
| Error boundary | Payment-specific controller advice should keep error shape stable where Spring MVC routes reach the controller |
| Route boundary | `/summary` is a literal collection/report route; it must not be swallowed by `{paymentOrderId}` |
| Spec Kit | Not required. Adding tests/deepening existing endpoint is a lesson extension |

## 6. HTTP I REST API

Primary endpoint:

```http
GET /api/merchants/{merchantId}/payment-orders/summary
Authorization: Bearer <token>
Accept: application/json
X-Correlation-ID: corr-l10-example
```

HTTP risks to test or explicitly characterize:

| Risk | Test intent | Expected posture |
|---|---|---|
| Unsupported `Accept` | Client asks for non-JSON representation | API should not silently return a misleading contract |
| Unsupported method | Client sends `PUT`, `PATCH`, `DELETE` to summary | API surface remains intentionally narrow |
| Malformed `merchantId` | Path variable is not UUID | Stable `400 validation` instead of 500 |
| `/summary` route ambiguity | Literal summary path competes with `{paymentOrderId}` | Summary response shape wins |
| Conditional header on summary | Client sends `If-None-Match` to endpoint with no `ETag` | Summary remains normal `200` and does not pretend cache semantics |
| Missing or malformed token | Resource server failure | `401`, not payment-domain error |
| Wrong role or wrong merchant claim | Authenticated but not allowed | `403 forbidden` for summary/list report endpoints |

Do not add `If-Match` / `412` behavior here. That belongs to future lifecycle/update operations.

## 7. Java 25 I Java Code Reading

Java focus for Lesson 10:

- JUnit parameterized tests using `@ParameterizedTest` / `@MethodSource` for role matrix rows.
- Records for expected matrix rows if useful, for example `record SummaryAccessCase(String name, String token, String targetMerchantId, int expectedStatus)`.
- Immutable test data setup via `List.of(...)` and no shared mutable scenario state.
- Small helper methods, no large DSL unless repetition proves it is needed.

Avoid over-engineering a framework. The goal is to teach matrix thinking, not create a generic test engine.

## 8. SQL, PostgreSQL I Flyway

No migration expected.

Optional 10C practice:

- Add a repository/service-level aggregation test if REST failures are hard to diagnose.
- Verify empty aggregate, multi-currency totals and date boundaries closer to the DB/service layer.
- Run or document `EXPLAIN` manually only as a diagnostic exercise, not as a brittle automated assertion.

## 9. Security I Tenant Isolation

Lesson 10 should make the security model more explicit:

| Actor/token case | Target merchant | Expected |
|---|---|---|
| unauthenticated | any | `401` |
| invalid issuer | any | `401` |
| invalid signature | any | `401` |
| expired token | any | `401` |
| denied token | any | `403` |
| `merchant:payments:create` only | own merchant | `403` |
| `merchant:payments:operate` only | own merchant | `403` |
| `merchant:payments:read` without `merchant_id` | target merchant | `403` |
| `merchant:payments:read` with matching `merchant_id` | own merchant | `200` |
| `merchant:payments:read` with mismatched `merchant_id` | other merchant | `403` |
| `platform:payments:read` | selected merchant | `200` |
| platform merchant-only roles | selected merchant | `403` |

Teaching point:

- BFLA: wrong function/role, for example create-only token trying to read summary.
- BOLA: right function but wrong object/tenant, for example merchant reader trying another merchant summary.

## 10. REST Assured Learning Path

Candidate test tasks:

| Test | Purpose | Main assertion |
|---|---|---|
| summaryRejectsOrCharacterizesUnsupportedAccept | HTTP negotiation | status + no misleading body assumption |
| unsupportedMethodsDoNotExposeSummaryMutationSurface | method semantics | `405` or documented Spring behavior after characterization |
| malformedMerchantIdReturnsValidationError | error contract | `400`, `error=validation`, message mentions UUID |
| summaryLiteralRouteWinsOverPaymentOrderIdWildcard | route collision guardrail | `200` summary shape, no single-order fields |
| ifNoneMatchDoesNotEnableSummaryCaching | cache/conditional semantics | `200`, no `ETag` |
| summaryAuthorizationMatrix | BOLA/BFLA matrix | parameterized status and optional error code |

If actual Spring behavior differs from the expected posture, first document the current behavior and only change production code when the behavior is a real product/API risk.

## 11. Assertion Strategy

| Risk | Best oracle |
|---|---|
| HTTP protocol behavior | REST Assured status/header/content-type assertions |
| Error envelope drift | REST Assured body assertions plus reusable helper |
| Role policy drift | Parameterized REST Assured security matrix |
| Aggregation arithmetic | REST Assured typed extraction plus AssertJ controlled seed oracle |
| DB aggregate query defect | Repository/service test or manual SQL diagnostic |
| UI denied-state regression | Playwright only when UI behavior changes |

## 12. Test Data Ownership

Rules:

- Create merchants per test or per parameterized case group.
- Use unique merchant ids and idempotency keys through existing helpers.
- Do not rely on test order.
- Do not share mutable static token/merchant state between cases unless it is immutable and safe.
- Avoid logging `Authorization` headers or raw tokens.

## 13. Pytania Do Samodzielnej Odpowiedzi

1. Dlaczego `Accept` jest innym kontraktem niz `Content-Type`?
2. Kiedy API powinno zwrocic `401`, a kiedy `403`?
3. Dlaczego summary cross-tenant zwraca `403`, a single payment order read moze maskowac jako `404`?
4. Jak test wykrywa, ze `/summary` nie zostalo potraktowane jak `{paymentOrderId}`?
5. Dlaczego summary nie powinno nagle wspierac `ETag` tylko dlatego, ze detail endpoint go ma?
6. Co jest BOLA, a co BFLA w payment order summary?
7. Kiedy parameterized test poprawia czytelnosc, a kiedy tworzy zbyt magiczna macierz?
8. Dlaczego OpenAPI/Pact/WireMock sa odlozone mimo ze sa wazne dla senior SDET?
9. Jak odroznic app bug od test bug przy `406`/`415`/`405`?
10. Co powinno byc sprawdzone w REST Assured, a czego nie przenosic do Playwright?

## 14. Zadania Praktyczne

| Zadanie | Files | Command | Expected |
|---|---|---|---|
| Add HTTP edge tests for summary | `PaymentOrderSummaryHttpContractRestAssuredTest.java` | `./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test` | HTTP semantics characterized and green |
| Add parameterized authorization matrix | `PaymentOrderSummaryAuthorizationMatrixTest.java` or security test refactor | `./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test` | Matrix proves BOLA/BFLA statuses |
| Reuse safe request/error helpers | `PaymentOrderSummaryApiTestSupport.java` or small local helpers | targeted test command | Less duplication without hiding behavior |
| Optional DB oracle test | repository/service test | targeted test command | Aggregation diagnostics improved |
| Update evidence | vault tracker/current lesson/current sprint | n/a | Lesson 10 evidence captured |

## 15. Mini Interview Prep

**Q: What did Lesson 10 add if it did not add product behavior?**

A: It hardened the existing REST API contract. Instead of adding lifecycle features prematurely, it tested HTTP edge semantics, route ambiguity, stable errors and a parameterized authorization matrix for an existing summary endpoint. This is important because senior API testing is not only happy-path JSON assertions; it also protects protocol behavior and security policy from drift.

**Q: How do you distinguish BOLA and BFLA in this API?**

A: BFLA is about using a function without the required role, such as a create-only token trying to read a summary. BOLA is about using the right function against the wrong merchant object, such as a merchant reader with `merchant_id=A` trying to read merchant B summary.

**Q: Why not jump to Pact, WireMock or OpenAPI now?**

A: The current highest-value gap is still direct REST/HTTP and authorization hardening on our own API. Pact/WireMock/OpenAPI are useful later, but they would add tooling before the learner has fully practiced protocol semantics, error contracts and role matrices with REST Assured.

## 16. Verification Commands

Backend target commands:

```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

Frontend regression only if frontend files change:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## 17. Learning Outcome Checklist

Po tej lekcji umiem:

- [x] Wyjasnic roznice miedzy `Accept` i `Content-Type`.
- [x] Zaprojektowac REST Assured tests dla unsupported methods, malformed UUID i route ambiguity.
- [x] Zrobic parameterized authorization matrix dla BOLA/BFLA.
- [x] Uzasadnic `401` vs `403` vs masked `404` w istniejacym Payment Order API.
- [x] Wyjasnic, dlaczego summary ma no `ETag` i nie wspiera conditional requests.
- [x] Rozpoznac, kiedy REST Assured wystarcza, a kiedy potrzebny jest DB oracle lub Playwright.

## 18. Implementation Evidence

**Date:** 2026-06-02

### Production code changes
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java` — added `merchantPaymentReaderTokenWithoutMerchantIdClaim()` factory method for authorization matrix row 8

### Test code evidence
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java` — 7 tests (Batch 10A: HTTP Edge Contract Hardening)
  - `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape` — route collision guardrail
  - `malformedMerchantIdReturnsValidationError` — parameterized with 3 variants: `not-a-uuid`, `12345`, `null`
  - `unsupportedMethodsDoNotExposeSummaryMutationSurface` — PUT, PATCH, DELETE → 405
  - `unsupportedAcceptIsRejectedOrExplicitlyCharacterized` — `Accept: text/xml` → 406
  - `ifNoneMatchDoesNotEnableSummaryCaching` — `If-None-Match` ignored, no ETag
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java` — 12 parameterized tests (Batch 10B: Authorization Matrix)
  - 4 authentication failures (401): unauthenticated, invalid issuer, invalid signature, expired
  - 5 BFLA failures (403): denied, create-only, operate-only, read without claim, platform merchant-only
  - 1 BOLA failure (403): cross-tenant merchant reader
  - 2 success cases (200): own merchant reader, platform payment reader

### Characterization results
- Spring MVC `@RestController` without explicit `produces` returns `406 Not Acceptable` for `Accept: text/xml`
- Spring MVC returns `405 Method Not Allowed` with `Allow: GET, HEAD` for unmapped methods
- Spring MVC ignores `If-None-Match` when response has no `ETag`
- `PaymentExceptionHandler.handleTypeMismatch()` returns `400 validation` for malformed UUID path variables

### Commands run
```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test  # 7/7 pass
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test       # 12/12 pass
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test  # 20/20 pass
./mvnw -Dtest=PaymentModuleTest test                                 # 2/2 pass
./mvnw -DskipTests package                                           # BUILD SUCCESS
```

### Spec Kit artifacts
- `specs/007-rest-http-contract-hardening-authorization-matrix/spec.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/plan.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/tasks.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/research.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/data-model.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/contracts/summary-http-edge-api.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/checklists/requirements.md`

### Interview answer EN
> Lesson 10 hardened the existing Payment Order Summary REST API without adding new business functionality. I implemented HTTP edge contract tests covering route collision, malformed UUID validation, unsupported methods (405), content negotiation (406), and conditional header discipline (If-None-Match ignored). I also created a parameterized authorization matrix with 12 test cases explicitly labeling BOLA (cross-tenant access) and BFLA (wrong role) scenarios. This demonstrates senior-level thinking: testing protocol behavior and security policy, not just happy-path JSON assertions.

## 19. Powiazane Notatki W Vault

- [[Lesson 06 - Payment Order Create Read Foundation]]
- [[Lesson 07 - Payment Order List Filter Search]]
- [[Lesson 08 - Payment Aggregation Summary]]
- [[Lesson 09 - Payment Orders Frontend Consumer and Contract Alignment]]
- [[Prompt - Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix]]
- [[Learning Coverage Backlog]]
- [[Senior SDET Competency Coverage Matrix]]
- [[Lesson Evidence Tracker]]
