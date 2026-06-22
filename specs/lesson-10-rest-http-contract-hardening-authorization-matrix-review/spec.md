# Review Specification: Post-Implementation Review — REST HTTP Contract Hardening and Authorization Matrix

**Feature Branch**: `lesson-10-rest-http-contract-hardening-authorization-matrix-review`

**Created**: 2026-06-02

**Status**: Draft

**Input**: Post-implementation review of Lesson 10 — REST HTTP Contract Hardening and Authorization Matrix. Review code, tests, Spec Kit artifacts, analysis outputs, and vault evidence against the approved `specs/007-rest-http-contract-hardening-authorization-matrix/` specification.

**Lesson**: 10

**Phase**: 2 — Payment Orders

## Purpose

This is a **review specification**, not a new feature. It defines the scope, questions, severity model, and expected output for a post-implementation review of the completed Lesson 10 implementation.

The review must answer:

> Did the implemented code, tests, Spec Kit artifacts, analysis outputs, and vault evidence satisfy the approved Lesson 10 specification without expanding scope, weakening modular boundaries, duplicating existing coverage, or introducing unsupported payment business behavior?

**This specification does not implement code, change tests, or edit documentation.** It defines what a later review agent must verify and how findings must be reported.

## Actors

- **Spec Kit Reviewer**: Verifies traceability between spec, plan, tasks, and implementation.
- **Senior Backend Architect**: Verifies production code minimality and module boundary preservation.
- **Senior QA Automation Architect**: Verifies test quality, assertion strength, and framework patterns.
- **REST Assured Test Architect**: Verifies REST Assured usage, contract assertions, and error handling.
- **Security Test Architect**: Verifies BOLA/BFLA coverage, authentication/authorization distinction, and token leakage risk.
- **Spring Modulith Reviewer**: Verifies module boundary integrity and `PaymentModuleTest` alignment.
- **Learning Evidence Auditor**: Verifies vault evidence accuracy and governance file consistency.

## In Scope

### 1. Spec Kit Compliance Review

Verify that the implementation artifacts match the approved specification:

| Artifact | Location | What to Verify |
|---|---|---|
| Feature spec | `specs/007-.../spec.md` | All FR-4xx, NFR-4xx, AC, SC are addressed |
| Plan | `specs/007-.../plan.md` | Constitution checks pass, design matches implementation |
| Research | `specs/007-.../research.md` | Characterization decisions R-001 through R-008 are reflected in tests |
| Data model | `specs/007-.../data-model.md` | `SummaryAccessCase` record, matrix rows, HTTP edge cases match implementation |
| Contract | `specs/007-.../contracts/summary-http-edge-api.md` | HTTP edge contract tables match actual test assertions |
| Quickstart | `specs/007-.../quickstart.md` | Verification commands match actual commands run |
| Tasks | `specs/007-.../tasks.md` | T001-T020, T023-T028 marked complete; T021-T022 remain unchecked |
| Requirements checklist | `specs/007-.../checklists/requirements.md` | All checked items have corresponding evidence |

### 2. Code Review

| File | What to Verify |
|---|---|
| `TestJwtSupport.java` | `merchantPaymentReaderTokenWithoutMerchantIdClaim()` exists, is `public static`, uses `tokenWithRoles()` (not `tokenWithRolesAndMerchantId()`), has `merchant:payments:read` role, has NO `merchant_id` claim |
| `PaymentOrderSummaryHttpContractRestAssuredTest.java` | 7 tests pass, Lesson 10 / Batch 10A comments present, route collision test asserts summary shape vs single-order shape, malformed UUID is parameterized with 3 variants, unsupported methods assert 405 + Allow header, unsupported Accept asserts 406, If-None-Match asserts 200 + no ETag |
| `PaymentOrderSummaryAuthorizationMatrixTest.java` | 12 parameterized rows pass, Lesson 10 / Batch 10B comments present, `SummaryAccessCase` record defined, `@ParameterizedTest` + `@MethodSource` used, BOLA/BFLA labels in display names, 401 cases don't assert body, 403 cases distinguish controller vs Spring Security, 200 cases assert content type + totalOrders |
| Production files (`PaymentOrderController.java`, `PaymentExceptionHandler.java`, `SecurityConfig.java`, `PaymentErrorResponse.java`, `KeycloakRealmRoleConverter.java`, `PaymentOrderSummaryResponse.java`, `PaymentOrderListResponse.java`) | No changes unless a real bug was found |

### 3. Test Review

| Aspect | What to Verify |
|---|---|
| REST Assured quality | Proper use of `given()`/`when()`/`then()`, `ContentType.JSON`, `statusCode()`, `body()`, `header()` assertions |
| Route collision | Test proves `/summary` returns `totalOrders` (summary shape), NOT `paymentOrderId` (single-order shape) |
| Malformed UUID | Parameterized test covers `not-a-uuid`, `12345`, `null`; asserts `400`, `error=validation`, `must be a valid UUID`, `correlationId` |
| Unsupported methods | PUT, PATCH, DELETE → `405`; `Allow` header contains `GET` |
| Unsupported Accept | `Accept: text/xml` → `406` (characterized Spring MVC behavior) |
| If-None-Match | `If-None-Match` → `200`, no `ETag`, normal body |
| Authorization matrix | 12 rows: 4× `401`, 5× `403` (BFLA), 1× `403` (BOLA), 2× `200` |
| BOLA/BFLA labels | Visible in `@ParameterizedTest` display names or assertion messages |
| Token leakage | No bearer token values in assertion messages, REST Assured logging, or failure output |
| Error contract | `400` → `error=validation`, `message`, `correlationId`; `403` controller → `error=forbidden`; `401` → no body assertion |
| Duplicate coverage | New tests add HTTP edge and matrix value; do not duplicate `PaymentOrderSummaryRestAssuredTest`, `PaymentOrderSummaryBusinessFlowRestAssuredTest`, `PaymentOrderSummarySecurityTest` |

### 4. Security Review

| Aspect | What to Verify |
|---|---|
| Authentication vs Authorization | `401` = Spring Security rejects (no/invalid token); `403` = authenticated but not authorized |
| BOLA | Cross-tenant merchant reader → `403` (row 10) |
| BFLA | Wrong role (create-only, operate-only, denied, read-without-claim, platform-merchant-only) → `403` (rows 5-8, 12) |
| Ownership | `merchant_id` claim matching enforced by controller |
| Platform reader | `platform:payments:read` → `200` for any merchant (row 11) |
| Token leakage | No token values in test output or assertion messages |
| No new roles/claims | Only existing `merchant:payments:read`, `merchant:payments:create`, `merchant:payments:operate`, `platform:payments:read` used |

### 5. HTTP Contract Review

| Aspect | What to Verify |
|---|---|
| Route collision | `/summary` literal route resolves to summary, not `{paymentOrderId}` wildcard |
| Malformed UUID | Non-UUID `merchantId` → `400 validation` via `PaymentExceptionHandler.handleTypeMismatch()` |
| Unsupported methods | `PUT`, `PATCH`, `DELETE` → `405 Method Not Allowed` |
| Unsupported Accept | `Accept: text/xml` → `406 Not Acceptable` (characterized) |
| If-None-Match | Ignored when no `ETag`; response is `200` with normal body |
| No ETag | Summary response does not include `ETag` header |
| JSON content type | `200` responses have `Content-Type: application/json` |
| Correlation ID | `X-Correlation-ID` header present in responses where request reaches application |

### 6. Modular Monolith Review

| Aspect | What to Verify |
|---|---|
| Module ownership | All test code targets existing `payment` module |
| No cross-module leakage | No new dependencies between modules |
| No new public API | Test classes are in `lab.paymentquality.rest` and `lab.paymentquality.security` |
| PaymentModuleTest | Still passes (2/2) |
| No new module | No new Spring Modulith module created |

### 7. Parallel / Data Isolation Review

| Aspect | What to Verify |
|---|---|
| Per-test merchant | Each test creates its own merchant via `PaymentApiTestSupport.createActiveMerchant()` |
| Per-row token | Each parameterized row constructs its own token |
| No shared mutable fixtures | No static mutable state between tests |
| No order dependence | Tests don't depend on execution order |
| Unique container names | Each test class uses unique PostgreSQL container name |

### 8. Learning Evidence Review

| File | What to Verify |
|---|---|
| Lesson 10 vault note | Accurate file paths, test counts (7+12+20+2=41), command results, characterization results |
| Lesson Evidence Tracker | Test evidence, residual risks, interview answer present |
| Current Lesson | Lesson 10 marked complete, Lesson 11 is next |
| Current Sprint | Sprint 11 planned after Lesson 10 |
| Learning Coverage Backlog | HTTP/REST `Accept`, content negotiation, BOLA, BFLA, parameterized tests marked `Evidence Strong` or `Practiced` |
| Requirements checklist | All items checked off |

### 9. Scope Guardrail Review

| Guardrail | What to Verify |
|---|---|
| No `POST /payments` | No payment lifecycle endpoint added |
| No lifecycle action | No authorize, capture, cancel, refund |
| No PSP mock | No PSP integration or mock |
| No Kafka | No Kafka, webhooks, or event pipeline |
| No new status | Only `CREATED` status exists |
| No frontend dashboard | No business dashboard implemented |
| No complete OAuth/OIDC | Only test JWT tokens used |
| No broad RA rewrite | No REST Assured framework architecture overhaul |

## Out of Scope

- Implementing fixes for any findings discovered during review.
- Changing production code unless a CRITICAL or HIGH finding requires it.
- Changing test code unless a CRITICAL or HIGH finding requires it.
- Editing vault evidence or governance files.
- Expanding Lesson 10 into payment lifecycle, PSP, Kafka, or frontend dashboard work.
- OpenAPI, Pact, WireMock, or contract-test tooling.
- Performance/load testing.
- Playwright UI testing (no frontend changes expected).

## Review Finding Severity Model

| Severity | Definition | Examples |
|---|---|---|
| **CRITICAL** | Introduces or approves payment business functionality outside scope; security policy bypass; cross-tenant data exposure; false completion of core requirements; code that breaks existing payment module behavior | New endpoint added; BOLA row missing and marked complete; production change that breaks `PaymentModuleTest`; token values leaked in test output |
| **HIGH** | Missing required authorization matrix row; missing route collision or malformed UUID coverage; incorrect BOLA/BFLA expectation; token leakage in test output; misleading vault evidence; production change not justified by spec | Only 11 matrix rows instead of 12; malformed UUID test missing `null` variant; `401` cases assert `PaymentErrorResponse` body; requirements checklist overclaims |
| **MEDIUM** | Weak assertion quality; duplicated coverage that reduces educational value; incomplete correlation/error contract assertion; unstable Spring MVC characterization; incomplete task-to-evidence traceability; unclear learning evidence | `200` cases don't assert `totalOrders`; `403` cases don't distinguish controller vs Spring Security; `Accept` test doesn't document characterization; T014 doesn't mention duplicate coverage check |
| **LOW** | Naming, formatting, minor wording, non-blocking documentation clarity, or small maintainability concerns | Comment wording; minor naming inconsistency; vault note formatting |

## Required Review Questions

The review must answer these questions with evidence:

1. Does every completed task in `tasks.md` correspond to actual code, test, evidence, or verification output?
2. Are T021-T022 clearly optional and not falsely presented as completed elsewhere?
3. Do the new tests add HTTP edge and authorization matrix value instead of duplicating existing summary/security tests?
4. Is `merchantPaymentReaderTokenWithoutMerchantIdClaim()` correctly named, scoped to tests, and implemented without a `merchant_id` claim while retaining the `merchant:payments:read` role?
5. Does the authorization matrix cover all 12 required rows with expected `401`, `403`, and `200` outcomes?
6. Are BOLA and BFLA labels visible in display names, row labels, assertions, or test output?
7. Do `401` cases correctly distinguish Spring Security authentication rejection from application-level forbidden responses?
8. Do `403` cases assert the correct behavior without overfitting to an unstable body where Spring Security handles the response?
9. Do successful `200` cases assert meaningful summary response shape and JSON content type?
10. Does the route collision test prove `/summary` resolves to summary shape rather than the single payment order read shape?
11. Does malformed UUID coverage include the expanded variants from the remediation task?
12. Are unsupported method and unsupported `Accept` tests aligned with the characterization documented in `research.md`?
13. Does `If-None-Match` testing confirm no cache semantics and no `ETag` without implying future caching support?
14. Do tests avoid logging or exposing bearer token values in assertion messages or failure output?
15. Are test data and tokens isolated enough for parallel execution?
16. Did implementation avoid production changes unless a real contract bug required them?
17. Do vault evidence files accurately reflect exact file paths, command results, test counts, and residual risks?
18. Does the requirements checklist overclaim any requirement that is not actually covered?

## Expected Review Output Structure

The review agent must produce findings ordered by severity, with file and line references where possible.

### Required Output Sections

1. **Executive Verdict**: `PASS`, `PASS WITH FINDINGS`, or `FAIL`.
2. **Findings** (ordered by severity): Each finding must include file/line reference, evidence, impact, and recommended fix.
3. **Spec Kit Traceability Matrix**: Requirement/task/artifact → implementation evidence.
4. **Test Coverage Matrix**: HTTP edge cases and authorization rows → test methods or parameterized rows.
5. **Scope Guardrail Assessment**: Explicit confirmation that no out-of-scope payment behavior was introduced.
6. **Security Assessment**: BOLA/BFLA, authentication/authorization, ownership, and token leakage risks.
7. **Architecture Assessment**: Spring Modulith/module-boundary impact and production-code minimality.
8. **Learning Evidence Assessment**: Vault and governance files versus actual implementation and verification.
9. **Verification Commands**: Commands to rerun with expected results based on current evidence.
10. **Residual Risks and Follow-up Tasks**: Including whether optional T021-T022 should remain deferred.

## Verification Commands

The review must instruct the reviewer to run or request evidence for these commands from `apps/backend`:

```bash
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest,PaymentOrderSummaryAuthorizationMatrixTest,PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest,PaymentModuleTest test
./mvnw -DskipTests package
```

Frontend verification is not expected unless the review discovers frontend file changes:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## Known Implementation Claims to Validate

The implementation claims the following outcomes. The review must independently verify each claim:

| # | Claim | Verification Method |
|---|---|---|
| 1 | `merchantPaymentReaderTokenWithoutMerchantIdClaim()` added in `TestJwtSupport.java` | Read source, verify method signature, verify no `merchant_id` claim |
| 2 | `PaymentOrderSummaryHttpContractRestAssuredTest.java` created with 7 tests | Read source, count test methods, verify Lesson 10 / Batch 10A comments |
| 3 | `PaymentOrderSummaryAuthorizationMatrixTest.java` created with 12 parameterized rows | Read source, count `Arguments.of()` entries, verify Lesson 10 / Batch 10B comments |
| 4 | HTTP contract: 7/7 pass | Run command or verify evidence |
| 5 | Auth matrix: 12/12 pass | Run command or verify evidence |
| 6 | Regression: 20/20 pass | Run command or verify evidence |
| 7 | Modulith: 2/2 pass | Run command or verify evidence |
| 8 | Combined: 41 tests, 0 failures | Run combined command |
| 9 | Lesson comments present in new test files | Read source files |
| 10 | Vault evidence updated | Read vault files |
| 11 | Tasks T001-T020, T023-T028 marked complete | Read `tasks.md` |
| 12 | T021-T022 not completed (optional) | Read `tasks.md` |
| 13 | No production changes unless bug found | Diff production files against baseline |

## Definition of Done for This Review Specification

1. Review specification is created and covers all 9 review scope areas.
2. Severity model is defined with 4 levels.
3. All 18 required review questions are included.
4. Expected output structure is defined with 10 sections.
5. Verification commands are specified.
6. Known implementation claims are listed for independent verification.
7. Out of scope items are explicitly excluded.
8. Specification does not instruct the reviewer to implement code during the review.
