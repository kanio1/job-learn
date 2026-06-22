---
type: prompt
status: ready
project: Payment Quality Engineering Lab
lesson: 10
date: 2026-05-31
tags:
  - prompt
  - speckit
  - lesson-10
  - payment-order
  - backend
  - rest
  - http-contract
  - rest-assured
  - keycloak
  - authorization-matrix
  - security-testing
  - qa-architecture
---

# Prompt - Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix

Copy this prompt and use it as the input for the first Spec Kit command, `/speckit.specify`, for Lesson 10.

```text
You are my cross-functional specification team: Business Analyst, Backend Architect, QA Architect, Security Test Architect, Senior REST Assured Mentor, and Spec Kit Agent.

We are working in this repository:

/home/suso/job-learn

Run the first Spec Kit step: `/speckit.specify`.

Create a feature specification for Lesson 10:

REST HTTP Contract Hardening and Authorization Matrix

## Context To Read First

Read these project and learning files before writing the specification:

- `AGENTS.md`
- `specs/005-payment-order-summary/spec.md`
- `specs/005-payment-order-summary/plan.md`
- `specs/005-payment-order-summary/contracts/payment-order-summary-api.md`
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
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix.md`

Inspect these code files only to understand the current implementation and test baseline. Do not implement code during `/speckit.specify`:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentErrorResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryBusinessFlowRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummarySecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/PaymentOrderSummaryApiTestSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/testsupport/MerchantApiTestSupport.java`

## Skills To Use

Use these skills while creating the specification:

- `payment-quality-lab-orchestrator`
- `qa-architecture-sprint-team`
- `spec-kit-feature-workflow`
- `spring-boot4-spring7-backend-architect`
- `java-rest-api-testing-effective-java-mentor`
- `junit6-assertj-restassured-testcraft`
- `rest-api-security-oauth-testing`
- `test-analysis-design-and-data`
- `parallel-test-architecture-and-data-isolation`
- `spring-modulith-2-0-6-modular-monolith-testing`
- `obsidian-learning-os`

## Specification Goal

Create a Spec Kit feature specification for a backend/API quality hardening slice that strengthens the existing Payment Order Summary REST contract and authorization test coverage without introducing new payment business behavior.

Primary learning question:

How can the existing Payment Order Summary REST API be hardened so that a Senior QA Automation/SDET learner practices advanced HTTP semantics, mature REST Assured contract testing, Keycloak/JWT role-claim authorization matrices, BOLA/BFLA risk analysis, and stable error contract assertions without adding new payment business functionality?

## Feature Name

REST HTTP Contract Hardening and Authorization Matrix

Suggested Spec Kit slug:

`rest-http-contract-hardening-authorization-matrix`

## Existing API Under Test

Primary target:

- `GET /api/merchants/{merchantId}/payment-orders/summary`

Secondary context only:

- Existing payment order create/read/list tests from Lessons 06 and 07.
- Existing summary implementation from Lesson 08.
- Existing frontend consumer alignment from Lesson 09.

## What Not To Re-Explain

Treat these topics as prerequisites and do not re-teach them in the spec:

- Basic REST Assured `given()` / `when()` / `then()` flow.
- Basic path parameters, query parameters, headers, and request bodies.
- Payment Order create/read behavior from Lesson 06.
- Payment Order list/filter/pagination behavior from Lesson 07.
- SQL aggregation behavior from Lesson 08.
- Nuxt/Zod/Pinia frontend consumer behavior from Lesson 09.
- Basic Keycloak/OIDC login flow.

## Scope Decision

This is a Spec Kit specification for a quality and contract hardening slice.

It should not be framed as a new business capability. It should be framed as an explicit testing, security, and HTTP contract specification around an already implemented read-only summary endpoint.

The specification must preserve these decisions:

- No new payment resource lifecycle.
- No `POST /payments`.
- No new endpoint by default.
- No new payment status.
- No new roles or claims.
- No Kafka.
- No PSP integration or PSP mock flow.
- No complete OAuth/OIDC application integration.
- No complete business dashboard.
- Production code changes are allowed only when the contract tests expose a real bug or ambiguous behavior that must be stabilized.

If the analysis discovers that the desired outcome requires a lifecycle action, PSP flow, Kafka, new role model, OpenAPI/Pact/WireMock tooling, or a new endpoint, the spec must capture that as an out-of-scope recommendation instead of silently expanding the feature.

## In Scope

Specify batch 10A: HTTP edge contract hardening for summary.

The specification must cover these behaviors:

- The `/summary` literal route must resolve to the summary endpoint, not to the wildcard `{paymentOrderId}` read route.
- Malformed `merchantId` path values must return a stable validation-style `400` response when the request reaches application validation.
- Unsupported methods such as `PUT`, `PATCH`, and `DELETE` must not expose a summary mutation surface.
- Unsupported or non-JSON `Accept` behavior must be explicitly tested or characterized.
- `If-None-Match` must not imply summary cache support.
- The summary response must not expose an `ETag` unless the implementation deliberately introduces cache semantics, which is out of scope for this slice.
- Normal `Accept: application/json` requests must continue to return JSON.
- Error responses should preserve the existing error contract and correlation behavior where applicable.

Specify batch 10B: Parameterized authorization matrix for summary.

The specification must cover these identity/token cases:

- Unauthenticated request.
- Token with invalid issuer.
- Token with invalid signature.
- Expired token.
- Authenticated token with no useful payment role.
- Merchant token with create-only role.
- Merchant token with operate-only role.
- Merchant read token without `merchant_id` claim.
- Merchant read token for the same merchant.
- Merchant read token for a different merchant.
- Platform payment reader token.
- Platform merchant-only role token.

The matrix must clearly distinguish:

- BFLA: role grants or denies access to the summary function.
- BOLA: a merchant reader tries to access another merchant's summary.

Specify optional batch 10C only as a learning extension, not as a required implementation task:

- Repository/service-level aggregation diagnostic test, or
- Short `EXPLAIN` learning note for how PostgreSQL serves the summary query.

## Out Of Scope

The specification must explicitly exclude:

- Authorize, capture, cancel, refund, settlement, reconciliation, and any other payment lifecycle action.
- New payment statuses.
- PSP integration or PSP mock flows.
- Kafka, webhooks, events, or asynchronous pipeline work.
- Row-level security implementation.
- `If-Match` / `412 Precondition Failed` optimistic concurrency.
- New cache behavior, cache invalidation rules, or `ETag` support.
- Complete OAuth/OIDC application integration.
- OpenAPI validation, Pact, WireMock, or contract-test tooling expansion in this slice.
- Broad REST Assured framework rewrite.
- Frontend changes unless a backend contract fix requires a small type/schema alignment.
- Complete business dashboards or fake analytics.

## Actors And Security Expectations

The specification must include an actor and authorization matrix with expected status codes.

Actors:

- Unauthenticated caller: no token, expected `401`.
- Invalid bearer token caller: invalid issuer, signature, or expiration, expected `401`.
- Denied authenticated caller: authenticated but without summary permission, expected `403`.
- Merchant payment reader: has `merchant:payments:read` and matching `merchant_id`, expected `200`.
- Cross-tenant merchant payment reader: has `merchant:payments:read` but mismatched `merchant_id`, expected `403` as BOLA prevention.
- Merchant create-only actor: has `merchant:payments:create` only, expected `403` as BFLA prevention.
- Merchant operate-only actor: has operation-oriented role only, expected `403` as BFLA prevention.
- Platform payment reader: has `platform:payments:read`, expected `200` for any merchant.
- Platform merchant-only actor: has platform merchant role but not platform payment read role, expected `403`.

## Required Spec Sections

The `/speckit.specify` output must create a specification containing at least these sections:

1. Business purpose: why this is a quality hardening slice and what learner capability it develops.
2. Existing behavior baseline: what already exists from Lessons 06-09.
3. In scope / out of scope.
4. Actors and authorization matrix.
5. Functional requirements for HTTP edge behavior.
6. Functional requirements for authentication, authorization, BOLA, and BFLA coverage.
7. Error contract expectations, including status code, content type where body is expected, error code, and `X-Correlation-ID` where applicable.
8. Test strategy impact: REST Assured contract tests, security matrix tests, parameterized JUnit rows, and minimal helper changes.
9. Backend impact: expected to be test-first; production changes only if current behavior violates the agreed contract.
10. Database impact: no schema change expected.
11. Frontend impact: no frontend change expected by default.
12. Parallel/data isolation impact: per-test merchant and token setup, no shared mutable fixtures.
13. Risks and ambiguities.
14. Acceptance criteria.
15. Definition of Done.

## Candidate Functional Requirements

Use these as starting requirements and refine them into formal Spec Kit language:

- FR-001: The summary route must return the summary response shape and must not be confused with the single payment order read route.
- FR-002: A malformed `merchantId` must produce a stable `400` validation-style error when the request reaches the application.
- FR-003: Unsupported methods on the summary URI must not create or imply mutation behavior.
- FR-004: Content negotiation behavior for unsupported `Accept` headers must be explicitly tested or characterized.
- FR-005: A normal JSON request must return `200 OK`, `application/json`, and the existing summary response shape.
- FR-006: `If-None-Match` must not enable caching semantics for summary.
- FR-007: Summary responses must not include `ETag` unless a future cache feature explicitly adds it.
- FR-008: Unauthenticated and invalid-token requests must be rejected with `401`.
- FR-009: Authenticated callers without payment summary read permission must be rejected with `403`.
- FR-010: Merchant payment readers may access only their own merchant summary.
- FR-011: Merchant payment readers must receive `403` when accessing another merchant's summary.
- FR-012: Platform payment readers may access any merchant summary.
- FR-013: The authorization matrix must label BOLA and BFLA cases clearly.
- FR-014: Test failure output must not expose bearer token values.
- FR-015: The implementation must not introduce new payment business functionality.

## Candidate Acceptance Criteria

The spec must include acceptance criteria equivalent to these:

1. New summary HTTP contract tests compile and pass.
2. New summary authorization matrix tests compile and pass.
3. The summary route collision risk with `{paymentOrderId}` is covered by a test.
4. Malformed path, unsupported method, unsupported accept, and cache-header behavior are either asserted or explicitly characterized.
5. The authorization matrix covers `401`, `403`, and `200` outcomes.
6. BOLA and BFLA are visible in test names, display names, or matrix row labels.
7. No new payment business capability is introduced.
8. No new endpoint, role, claim, status, PSP integration, Kafka integration, or business dashboard is introduced.
9. Existing summary/list/security regression tests remain green.
10. `PaymentModuleTest` remains green.
11. Vault lesson evidence is updated after implementation in later Spec Kit steps.

## Candidate Test Names

The spec may recommend these test names, but should not implement them during `/speckit.specify`:

HTTP contract tests:

- `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape`
- `malformedMerchantIdReturnsValidationError`
- `unsupportedMethodsDoNotExposeSummaryMutationSurface`
- `unsupportedAcceptIsRejectedOrExplicitlyCharacterized`
- `ifNoneMatchDoesNotEnableSummaryCaching`

Authorization matrix test:

- `summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership`

Preferred test class names:

- `PaymentOrderSummaryHttpContractRestAssuredTest`
- `PaymentOrderSummaryAuthorizationMatrixTest`

## Clarifications To Preserve

Include these as explicit decisions in the spec unless the current implementation proves otherwise:

- This is a quality hardening slice, not a new business feature.
- Cross-tenant merchant summary access is `403`, not `404`, because this endpoint is scoped by merchant path and the lesson is explicitly teaching BOLA prevention.
- `merchant:payments:create` does not grant summary read access.
- Operation-oriented merchant roles do not grant summary read access.
- `platform:payments:read` grants summary read access across merchants.
- Merchant read tokens require a `merchant_id` claim for merchant-scoped access.
- Summary does not support `ETag` or conditional caching in this slice.
- No database migration is expected.
- No frontend change is expected.

## Verification Guidance For Later Spec Kit Steps

The specification should prepare later `/speckit.plan` and `/speckit.tasks` steps to use these backend verification commands after implementation:

```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

Frontend verification should be required only if frontend files change:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## Required Output From `/speckit.specify`

Create a new Spec Kit feature specification for Lesson 10. The spec must be precise enough that `/speckit.plan` and `/speckit.tasks` can later generate a small backend/API test hardening implementation plan.

Do not write implementation code in this first Spec Kit step.

Do not broaden the feature beyond the contract hardening and authorization matrix scope.

Before finishing, verify that the generated specification:

- Respects `AGENTS.md` and Phase 0 guardrails.
- Does not add payment business functionality.
- Keeps the summary endpoint read-only.
- Keeps BOLA and BFLA risks explicit.
- Defines observable HTTP and security outcomes.
- Captures open questions only where a real ambiguity remains.
```

## Post-Implementation Review Prompt

Copy this prompt and use it as the input for `/speckit.specify` when you want a separate Spec Kit review specification for the completed Lesson 10 implementation.

```text
You are my post-implementation review team: Spec Kit Reviewer, Senior Backend Architect, Senior QA Automation Architect, REST Assured Test Architect, Security Test Architect, Spring Modulith Reviewer, and Learning Evidence Auditor.

We are working in this repository:

/home/suso/job-learn

Run the first Spec Kit step: `/speckit.specify`.

Create a review specification for the completed Lesson 10 implementation:

Post-Implementation Review - REST HTTP Contract Hardening and Authorization Matrix

Suggested Spec Kit slug:

`lesson-10-rest-http-contract-hardening-authorization-matrix-review`

## Review Mission

Create a precise review specification that will guide a code, test, Spec Kit, and learning-evidence review of the completed Lesson 10 implementation.

The review must answer:

Did the implemented code, tests, Spec Kit artifacts, analysis outputs, and vault evidence satisfy the approved Lesson 10 specification without expanding scope, weakening modular boundaries, duplicating existing coverage, or introducing unsupported payment business behavior?

This is a review specification only. Do not implement code, do not change tests, and do not edit documentation during `/speckit.specify`.

## Context To Read First

Read these project guardrails and current Spec Kit pointers first:

- `AGENTS.md`
- `.kilocode/rules/specify-rules.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/spec.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/plan.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/research.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/data-model.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/contracts/summary-http-edge-api.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/quickstart.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/tasks.md`
- `specs/007-rest-http-contract-hardening-authorization-matrix/checklists/requirements.md`

Read these implemented code and test files:

- `apps/backend/src/test/java/lab/paymentquality/testsupport/TestJwtSupport.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryHttpContractRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummaryAuthorizationMatrixTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderSummaryBusinessFlowRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSummarySecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/rest/PaymentOrderListRestAssuredTest.java`
- `apps/backend/src/test/java/lab/paymentquality/security/PaymentOrderSecurityTest.java`
- `apps/backend/src/test/java/lab/paymentquality/payment/PaymentModuleTest.java`

Read these production files only to verify that scope and behavior were preserved. Do not request production changes unless a real review finding requires them:

- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderController.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentExceptionHandler.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentErrorResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/SecurityConfig.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/security/KeycloakRealmRoleConverter.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderSummaryResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/web/PaymentOrderListResponse.java`

Read these learning evidence files:

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Lesson Evidence Tracker.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Lesson.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/00 Learning OS/Current Sprint.md`
- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/Learning Governance/Learning Coverage Backlog.md`

## Skills To Use

Use these skills while creating the review specification:

- `payment-quality-lab-orchestrator`
- `spec-kit-feature-workflow`
- `spring-boot4-spring7-backend-architect`
- `spring-modulith-2-0-6-modular-monolith-testing`
- `java-rest-api-testing-effective-java-mentor`
- `junit6-assertj-restassured-testcraft`
- `rest-api-security-oauth-testing`
- `test-analysis-design-and-data`
- `parallel-test-architecture-and-data-isolation`
- `rapid-software-testing-risk-thinking`
- `obsidian-learning-os`

## Known Implementation Summary To Validate

The implementation claims the following outcomes:

- `merchantPaymentReaderTokenWithoutMerchantIdClaim()` was added in `TestJwtSupport.java`.
- `PaymentOrderSummaryHttpContractRestAssuredTest.java` was created with Lesson 10 / Batch 10A HTTP edge contract tests.
- `PaymentOrderSummaryAuthorizationMatrixTest.java` was created with Lesson 10 / Batch 10B parameterized authorization matrix tests.
- HTTP contract verification passed: `./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test` produced 7 passing tests.
- Authorization matrix verification passed: `./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test` produced 12 passing tests.
- Existing summary/security regression suite passed: `./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test` produced 20 passing tests.
- Modulith verification passed: `./mvnw -Dtest=PaymentModuleTest test` produced 2 passing tests.
- Combined suite produced 41 passing tests and 0 failures.
- Lesson comments are present in the new test files:
  - `Lesson 10 HTTP edge contract tests`
  - `Batch 10A: HTTP Edge Contract Hardening`
  - `Lesson 10 parameterized authorization matrix`
  - `Batch 10B: Parameterized Authorization Matrix`
- Vault lesson evidence and governance files were updated.
- Tasks T001-T020 and T023-T028 were marked complete in `tasks.md`.
- Optional aggregation diagnostics T021-T022 were not completed.

The review specification must require these claims to be independently verified, not merely repeated.

## Review Scope

The review must cover:

1. Spec Kit compliance: implementation versus `spec.md`, `plan.md`, `research.md`, `data-model.md`, contract document, quickstart, tasks, and requirements checklist.
2. Code review: correctness, minimality, naming, helper design, readability, and absence of unnecessary production changes.
3. Test review: REST Assured quality, assertions, parameterized matrix design, BOLA/BFLA visibility, duplicate coverage risk, error contract assertions, token leakage risk, and stable failure messages.
4. Security review: authentication versus authorization behavior, BOLA versus BFLA distinction, ownership claim handling, platform reader behavior, invalid/expired token behavior, and no new role/claim model.
5. HTTP contract review: route collision guardrail, malformed UUID behavior, unsupported method behavior, unsupported `Accept` characterization, `If-None-Match` behavior, no `ETag`, JSON content type expectations, and correlation/error contract handling.
6. Modular monolith review: payment module ownership, no cross-module leakage, no new public module API, and `PaymentModuleTest` alignment.
7. Parallel/data isolation review: per-test merchant setup, per-row token setup, no shared mutable fixtures, and no order dependence.
8. Learning evidence review: Lesson 10 vault note, Evidence Tracker, Current Lesson, Current Sprint, Learning Coverage Backlog, and requirements checklist match actual implementation and command results.
9. Scope guardrail review: no `POST /payments`, no lifecycle action, no PSP mock, no Kafka, no new status, no frontend dashboard, no complete OAuth/OIDC app integration, no broad REST Assured framework rewrite.

## Required Review Questions

The review specification must include review questions equivalent to these:

- Does every completed task in `tasks.md` correspond to actual code, test, evidence, or verification output?
- Are T021-T022 clearly optional and not falsely presented as completed elsewhere?
- Do the new tests add HTTP edge and authorization matrix value instead of duplicating existing summary/security tests?
- Is `merchantPaymentReaderTokenWithoutMerchantIdClaim()` correctly named, scoped to tests, and implemented without a `merchant_id` claim while retaining the `merchant:payments:read` role?
- Does the authorization matrix cover all 12 required rows with expected `401`, `403`, and `200` outcomes?
- Are BOLA and BFLA labels visible in display names, row labels, assertions, or test output?
- Do `401` cases correctly distinguish Spring Security authentication rejection from application-level forbidden responses?
- Do `403` cases assert the correct behavior without overfitting to an unstable body where Spring Security handles the response?
- Do successful `200` cases assert meaningful summary response shape and JSON content type?
- Does the route collision test prove `/summary` resolves to summary shape rather than the single payment order read shape?
- Does malformed UUID coverage include the expanded variants from the remediation task?
- Are unsupported method and unsupported `Accept` tests aligned with the characterization documented in `research.md`?
- Does `If-None-Match` testing confirm no cache semantics and no `ETag` without implying future caching support?
- Do tests avoid logging or exposing bearer token values in assertion messages or failure output?
- Are test data and tokens isolated enough for parallel execution?
- Did implementation avoid production changes unless a real contract bug required them?
- Do vault evidence files accurately reflect exact file paths, command results, test counts, and residual risks?
- Does the requirements checklist overclaim any requirement that is not actually covered?

## Review Finding Severity Model

The review specification must define this severity model:

- `CRITICAL`: Introduces or approves payment business functionality outside scope, security policy bypass, cross-tenant data exposure, false completion of core requirements, or code that breaks existing payment module behavior.
- `HIGH`: Missing required authorization matrix row, missing route collision or malformed UUID coverage, incorrect BOLA/BFLA expectation, token leakage in test output, misleading vault evidence, or production change not justified by the spec.
- `MEDIUM`: Weak assertion quality, duplicated coverage that reduces educational value, incomplete correlation/error contract assertion, unstable Spring MVC characterization, incomplete task-to-evidence traceability, or unclear learning evidence.
- `LOW`: Naming, formatting, minor wording, non-blocking documentation clarity, or small maintainability concerns.

## Expected Review Output Structure

The generated review specification must require the later review agent to produce findings first, ordered by severity, with file and line references where possible.

Required output sections for the later review:

1. Executive verdict: PASS, PASS WITH FINDINGS, or FAIL.
2. Findings ordered by severity, each with file/line reference, evidence, impact, and recommended fix.
3. Spec Kit traceability matrix: requirement/task/artifact to implementation evidence.
4. Test coverage matrix: HTTP edge cases and authorization rows to test methods or parameterized rows.
5. Scope guardrail assessment: explicit confirmation that no out-of-scope payment behavior was introduced.
6. Security assessment: BOLA/BFLA, authentication/authorization, ownership, and token leakage risks.
7. Architecture assessment: Spring Modulith/module-boundary impact and production-code minimality.
8. Learning evidence assessment: vault and governance files versus actual implementation and verification.
9. Verification commands to rerun, with expected results based on current evidence.
10. Residual risks and follow-up tasks, including whether optional T021-T022 should remain deferred.

## Required Verification Commands For Later Review

The review specification must instruct later review steps to run or request evidence for these commands from `apps/backend`:

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

## Out Of Scope For The Review Specification

Do not ask the review specification to implement fixes.

Do not broaden Lesson 10 into:

- Payment lifecycle actions.
- New payment statuses.
- PSP integration or PSP mocks.
- Kafka, webhooks, or event pipelines.
- Row-level security implementation.
- `If-Match` / `412 Precondition Failed` optimistic concurrency.
- New cache behavior, `ETag` support, or cache invalidation rules.
- OpenAPI, Pact, WireMock, or contract-test tooling expansion.
- Frontend dashboard implementation.
- Complete OAuth/OIDC application integration.
- Broad REST Assured framework architecture rewrite.

## Required Output From `/speckit.specify`

Create a new Spec Kit review specification for the completed Lesson 10 implementation.

The specification must be precise enough that later `/speckit.plan`, `/speckit.tasks`, or a dedicated review agent can perform a rigorous post-implementation review of code, tests, Spec Kit artifacts, analysis findings, verification evidence, and vault learning documentation.

Before finishing, verify that the generated review specification:

- Respects `AGENTS.md` and Phase 0 guardrails.
- Keeps Lesson 10 framed as a quality hardening and review slice, not a new business feature.
- Requires independent verification of implementation claims.
- Makes BOLA, BFLA, HTTP edge behavior, and test isolation reviewable.
- Defines finding severity and expected review output format.
- Does not instruct the reviewer to implement code during `/speckit.specify`.
```
