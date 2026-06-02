# Feature Specification: REST HTTP Contract Hardening and Authorization Matrix

**Feature Branch**: `007-rest-http-contract-hardening-authorization-matrix`

**Created**: 2026-06-02

**Status**: Draft

**Input**: User description: "REST HTTP Contract Hardening and Authorization Matrix — backend/API quality hardening slice for existing Payment Order Summary endpoint. HTTP edge contract tests and parameterized authorization matrix with BOLA/BFLA labeling. Lesson 10 test implementation."

**Lesson**: 10

**Phase**: 2 — Payment Orders

## Business Purpose *(mandatory)*

This feature is a quality hardening slice, not a new payment business capability. It strengthens the existing Payment Order Summary REST API contract and authorization test coverage so that a Senior QA Automation/SDET learner practices advanced HTTP semantics, mature REST Assured contract testing, Keycloak/JWT role-claim authorization matrices, BOLA/BFLA risk analysis, and stable error contract assertions.

After Lesson 06 established single-resource create/read, Lesson 07 established collection list/filter/search, Lesson 08 established database-backed aggregation with summary tests, and Lesson 09 established the frontend consumer contract, Lesson 10 introduces the next layer of API quality: protocol-level HTTP behavior and explicit security policy coverage.

This feature creates:

- The first HTTP edge contract tests in the payment domain: `Accept` negotiation, unsupported method semantics, malformed path variable handling, route collision guardrails, and conditional header discipline.
- The first parameterized authorization matrix using `@ParameterizedTest` / `@MethodSource` to express BOLA and BFLA cases as readable test rows.
- Explicit characterization of Spring MVC default behavior for HTTP edge cases that were previously untested.
- A stable error contract assertion pattern that verifies status code, content type, error code, and `X-Correlation-ID` where applicable.
- Educational material for BOLA vs BFLA distinction, content negotiation, and route ambiguity guardrails.

No payment processing, PSP integration, lifecycle actions, new endpoints, new roles, new statuses, Kafka, webhooks, complete OAuth/OIDC integration, or complete business dashboard is implemented.

## Clarifications

### Session 2026-06-02

- Q: Is this a new business capability? → A: No. This is a quality hardening slice over the existing Payment Order Summary endpoint. No new payment business functionality is introduced.
- Q: Does this slice add new endpoints? → A: No. All tests target the existing `GET /api/merchants/{merchantId}/payment-orders/summary` endpoint. Production code changes are allowed only when contract tests expose a real bug or ambiguous behavior that must be stabilized.
- Q: What is the primary target endpoint? → A: `GET /api/merchants/{merchantId}/payment-orders/summary` — the summary endpoint introduced in Lesson 08.
- Q: Why summary and not list or create/read? → A: Summary connects backend, security, REST Assured, and frontend consumer learning from Lessons 06-09. It is the most recently implemented endpoint and has the richest HTTP edge surface to harden.
- Q: Does this slice add new roles or claims? → A: No. All authorization matrix rows reuse existing `merchant:payments:read`, `merchant:payments:create`, `merchant:payments:operate`, and `platform:payments:read` authorities.
- Q: What happens when `Accept` is not `application/json`? → A: The behavior must be explicitly tested or characterized. If Spring MVC returns `406 Not Acceptable`, the test asserts that. If Spring MVC returns `200` with JSON regardless, the test documents that. The key is that the behavior is known and stable, not silently misleading.
- Q: What happens when `PUT`, `PATCH`, or `DELETE` is sent to the summary URI? → A: The behavior must be explicitly tested or characterized. Spring MVC should return `405 Method Not Allowed` for methods not mapped. The test asserts the actual behavior and confirms no mutation surface is exposed.
- Q: What happens when `merchantId` is not a valid UUID? → A: The existing `PaymentExceptionHandler.handleTypeMismatch()` returns `400` with `error=validation` and message `Invalid merchantId: must be a valid UUID`. The test asserts this stable error contract.
- Q: Does the `/summary` literal route collide with `{paymentOrderId}` wildcard? → A: No. `SecurityConfig` places the summary matcher before the wildcard GET matcher. The controller maps `@GetMapping("/summary")` as a literal route. The test verifies that a request to `/summary` returns the summary response shape, not a single payment order read response.
- Q: Does summary support `ETag` or conditional caching? → A: No. Summary is a transient aggregation with no single representation version. The test verifies that `If-None-Match` does not enable cache semantics and that no `ETag` is returned.
- Q: What is the difference between BOLA and BFLA in this context? → A: BFLA (Broken Function Level Authorization) is when a caller uses a function without the required role — for example, a create-only token trying to read summary. BOLA (Broken Object Level Authorization) is when a caller uses the right function against the wrong object/tenant — for example, a merchant reader with `merchant_id=A` trying to read merchant B's summary.
- Q: Why does cross-tenant summary return `403` instead of masked `404`? → A: Summary is a collection/report endpoint scoped by merchant path. Refusing access with `403` is overt and consistent with list behavior from Lesson 07. Masked `404` is used for single-resource read to avoid enumerating payment order IDs.
- Q: Does `merchant:payments:create` grant summary access? → A: No. Only `merchant:payments:read` and `platform:payments:read` can access summary.
- Q: Does `merchant:payments:operate` grant summary access? → A: No. The operate role is preserved for future lifecycle actions.
- Q: Can `platform:payments:read` access any merchant's summary? → A: Yes. A platform payment reader can view any merchant's summary for support/investigation.
- Q: What about a merchant reader token without a `merchant_id` claim? → A: The controller enforces `merchant_id` claim matching. A token without `merchant_id` claim attempting merchant-scoped summary access receives `403`.
- Q: Are new payment statuses added for the authorization matrix? → A: No. Only `CREATED` exists. The matrix tests authorization policy, not status transitions.
- Q: Does this slice require a Flyway migration? → A: No. No schema changes are expected.
- Q: Does this slice require frontend changes? → A: No. Frontend changes are required only if a backend contract fix demands consumer alignment.
- Q: What about OpenAPI, Pact, or WireMock? → A: Out of scope for this slice. The current highest-value gap is direct REST/HTTP and authorization hardening with REST Assured.
- Q: What about `If-Match` / `412` optimistic concurrency? → A: Out of scope. That belongs to future lifecycle/update operations.

## Actors *(mandatory)*

- **Unauthenticated Caller**: No token. Expected `401 Unauthorized`. Already defined in `003`.
- **Invalid Bearer Token Caller**: Token with invalid issuer, invalid signature, or expired. Expected `401 Unauthorized`. Already defined in `003`.
- **Denied Authenticated Caller**: Authenticated but without any payment read authority. Expected `403 Forbidden`. Already defined in `003`.
- **Merchant Payment Reader**: Authenticated user with `merchant:payments:read` authority scoped to one merchant via `merchant_id` claim. Views payment order summary belonging to their merchant. Expected `200 OK`. Already defined in `003`.
- **Cross-Tenant Merchant Payment Reader**: Authenticated merchant reader for merchant A who attempts to view merchant B's summary. Receives `403 Forbidden` as BOLA prevention. Already defined in `003`.
- **Merchant Payment Creator**: Authenticated user with `merchant:payments:create` only. Cannot view summary. Receives `403 Forbidden` as BFLA prevention. Already defined in `003`.
- **Merchant Payment Operator**: Authenticated user with `merchant:payments:operate` only. Cannot view summary. Receives `403 Forbidden` as BFLA prevention. Already defined in `003`.
- **Platform Payment Reader**: Authenticated user with `platform:payments:read` authority. Views any merchant's payment order summary for support/investigation purposes. Expected `200 OK`. Already defined in `003`.
- **Platform Merchant-Only Actor**: Authenticated user with platform merchant role but not platform payment read role. Cannot view summary. Receives `403 Forbidden`. Already defined in `003`.
- **Merchant Read Token Without `merchant_id` Claim**: Authenticated token with `merchant:payments:read` role but missing `merchant_id` claim. Cannot access merchant-scoped summary. Receives `403 Forbidden`. New actor for Lesson 10 matrix.

## Scope *(mandatory)*

### In Scope

**Batch 10A: HTTP Edge Contract Hardening for Summary**

- Route collision guardrail: `/summary` literal route must resolve to the summary endpoint, not to the wildcard `{paymentOrderId}` read route.
- Malformed `merchantId` path variable: non-UUID values must return a stable `400 validation` response via `PaymentExceptionHandler.handleTypeMismatch()`.
- Unsupported methods: `PUT`, `PATCH`, and `DELETE` on the summary URI must not expose a mutation surface. Behavior must be explicitly tested or characterized (expected `405 Method Not Allowed`).
- Unsupported or non-JSON `Accept` header: behavior must be explicitly tested or characterized. If Spring MVC returns `406 Not Acceptable`, assert it. If it returns `200` with JSON regardless, document it.
- Conditional header discipline: `If-None-Match` must not imply summary cache support. Summary must not return `ETag`.
- Normal `Accept: application/json` requests must continue to return `200 OK` with `application/json` content type and the existing summary response shape.
- Error responses must preserve the existing error contract: status code, `error` code, `message`, and `X-Correlation-ID` where the request reaches the application.

**Batch 10B: Parameterized Authorization Matrix for Summary**

- Unauthenticated request → `401`.
- Token with invalid issuer → `401`.
- Token with invalid signature → `401`.
- Expired token → `401`.
- Authenticated token with no useful payment role (denied) → `403`.
- Merchant token with `merchant:payments:create` only → `403` (BFLA).
- Merchant token with `merchant:payments:operate` only → `403` (BFLA).
- Merchant read token without `merchant_id` claim → `403` (BFLA).
- Merchant read token for the same merchant → `200`.
- Merchant read token for a different merchant → `403` (BOLA).
- Platform payment reader token → `200`.
- Platform merchant-only role token → `403` (BFLA).

The matrix must clearly distinguish:
- **BFLA**: Role grants or denies access to the summary function.
- **BOLA**: A merchant reader tries to access another merchant's summary.

**Batch 10C (Optional): Aggregation Diagnostic**

- Repository/service-level aggregation diagnostic test or short `EXPLAIN` learning note.
- Only if batches 10A and 10B are green and small.

### Out of Scope

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
- New endpoints, roles, claims, or payment business functionality.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - HTTP Edge Contract Hardening (Priority: P1)

A Senior QA Automation/SDET learner verifies that the existing Payment Order Summary endpoint behaves correctly for HTTP protocol edge cases: content negotiation, unsupported methods, malformed path variables, route ambiguity, and conditional headers.

**Why this priority**: HTTP edge semantics are a senior REST must-have. The existing tests cover happy path, filters, validation, and security, but do not cover `Accept` negotiation, unsupported methods, route collision, or conditional header discipline.

**Independent Test**: Can be fully tested by sending HTTP edge requests to the existing summary endpoint and asserting or characterizing the response status, content type, error code, and headers.

**Acceptance Scenarios**:

1. **Given** the summary endpoint exists, **When** a client sends `GET /api/merchants/{merchantId}/payment-orders/summary` with `Accept: application/json`, **Then** the system returns `200 OK` with `Content-Type: application/json` and the summary response shape.
2. **Given** the summary endpoint exists, **When** a client sends `GET /api/merchants/{merchantId}/payment-orders/summary` with `Accept: text/xml`, **Then** the system returns `406 Not Acceptable` or `200 OK` with JSON — the behavior is explicitly characterized.
3. **Given** the summary endpoint exists, **When** a client sends `PUT /api/merchants/{merchantId}/payment-orders/summary`, **Then** the system returns `405 Method Not Allowed` or the behavior is explicitly characterized as no mutation surface.
4. **Given** the summary endpoint exists, **When** a client sends `PATCH /api/merchants/{merchantId}/payment-orders/summary`, **Then** the system returns `405 Method Not Allowed` or the behavior is explicitly characterized.
5. **Given** the summary endpoint exists, **When** a client sends `DELETE /api/merchants/{merchantId}/payment-orders/summary`, **Then** the system returns `405 Method Not Allowed` or the behavior is explicitly characterized.
6. **Given** the summary endpoint exists, **When** a client sends `GET /api/merchants/not-a-uuid/payment-orders/summary`, **Then** the system returns `400 Bad Request` with `error=validation` and message containing `must be a valid UUID`.
7. **Given** the summary endpoint exists, **When** a client sends `GET /api/merchants/{merchantId}/payment-orders/summary`, **Then** the response contains the summary response shape (`totalOrders`, `totalAmountMinor`, `byCurrency`, `byStatus`), not the single payment order read shape (`paymentOrderId`, `amountMinor`, `currency`, `status`).
8. **Given** the summary endpoint exists and does not support `ETag`, **When** a client sends `GET /api/merchants/{merchantId}/payment-orders/summary` with `If-None-Match: "some-etag"`, **Then** the system returns `200 OK` with the summary response and no `ETag` header.

---

### User Story 2 - Parameterized Authorization Matrix (Priority: P1)

A Senior QA Automation/SDET learner verifies that the existing Payment Order Summary endpoint enforces authentication, role authorization, and merchant ownership through a parameterized test matrix that labels BOLA and BFLA cases.

**Why this priority**: The existing security tests are hand-coded and cover the main cases, but do not use parameterized test patterns or explicitly label BOLA/BFLA. A matrix-style test teaches security policy coverage and makes the authorization model visible.

**Independent Test**: Can be fully tested by sending summary requests with different token/claim combinations and asserting the expected status code.

**Acceptance Scenarios**:

1. **Given** an unauthenticated caller, **When** they request summary, **Then** the system returns `401`.
2. **Given** a caller with an invalid issuer token, **When** they request summary, **Then** the system returns `401`.
3. **Given** a caller with an invalid signature token, **When** they request summary, **Then** the system returns `401`.
4. **Given** a caller with an expired token, **When** they request summary, **Then** the system returns `401`.
5. **Given** an authenticated caller with no payment role, **When** they request summary, **Then** the system returns `403` (BFLA).
6. **Given** an authenticated caller with `merchant:payments:create` only, **When** they request summary for their own merchant, **Then** the system returns `403` (BFLA).
7. **Given** an authenticated caller with `merchant:payments:operate` only, **When** they request summary for their own merchant, **Then** the system returns `403` (BFLA).
8. **Given** an authenticated caller with `merchant:payments:read` but no `merchant_id` claim, **When** they request summary, **Then** the system returns `403` (BFLA).
9. **Given** an authenticated merchant reader for merchant A, **When** they request summary for merchant A, **Then** the system returns `200`.
10. **Given** an authenticated merchant reader for merchant A, **When** they request summary for merchant B, **Then** the system returns `403` (BOLA).
11. **Given** an authenticated platform payment reader, **When** they request summary for any merchant, **Then** the system returns `200`.
12. **Given** an authenticated platform merchant-only actor, **When** they request summary, **Then** the system returns `403` (BFLA).

---

### Edge Cases

- Summary with `Accept: */*` → should return `200 OK` with JSON (wildcard accept is permissive).
- Summary with `Accept: application/json, text/xml` → should return `200 OK` with JSON (JSON is acceptable).
- Summary with no `Accept` header → should return `200 OK` with JSON (default behavior).
- Malformed `merchantId` with value `""` (empty string) → `400 validation`.
- Malformed `merchantId` with value `"12345"` (numeric but not UUID) → `400 validation`.
- Malformed `merchantId` with value `"null"` → `400 validation`.
- `HEAD` method on summary URI → characterized behavior (Spring MVC may return `200` with no body for `HEAD` on `GET` mappings).
- `OPTIONS` method on summary URI → characterized behavior (Spring MVC may return `200` with `Allow` header).

## Requirements *(mandatory)*

### Functional Requirements

**HTTP Edge Contract Requirements**

- **FR-401**: System MUST return `200 OK` with `Content-Type: application/json` and the summary response shape when a valid `GET` request with `Accept: application/json` is received.
- **FR-402**: System MUST return the summary response shape (`totalOrders`, `totalAmountMinor`, `byCurrency`, `byStatus`) for the `/summary` route and MUST NOT return the single payment order read shape (`paymentOrderId`, `amountMinor`, `currency`, `status`).
- **FR-403**: System MUST return `400 Bad Request` with `error=validation` and message containing `must be a valid UUID` when `merchantId` path variable is not a valid UUID.
- **FR-404**: System MUST NOT expose a mutation surface for `PUT`, `PATCH`, or `DELETE` methods on the summary URI. The behavior MUST be explicitly tested or characterized.
- **FR-405**: Content negotiation behavior for unsupported `Accept` headers MUST be explicitly tested or characterized. The system MUST NOT silently return a misleading contract.
- **FR-406**: System MUST NOT return `ETag` on summary responses.
- **FR-407**: System MUST NOT enable caching semantics for summary when `If-None-Match` header is present.
- **FR-408**: System MUST preserve the existing error contract for validation failures: `400` status, `error=validation`, `message`, and `correlationId` in the response body.
- **FR-409**: System MUST include `X-Correlation-ID` header in summary responses where the request reaches the application.

**Authorization Matrix Requirements**

- **FR-410**: System MUST return `401 Unauthorized` for unauthenticated summary requests.
- **FR-411**: System MUST return `401 Unauthorized` for requests with invalid issuer tokens.
- **FR-412**: System MUST return `401 Unauthorized` for requests with invalid signature tokens.
- **FR-413**: System MUST return `401 Unauthorized` for requests with expired tokens.
- **FR-414**: System MUST return `403 Forbidden` for authenticated callers without `merchant:payments:read` or `platform:payments:read` authority (BFLA).
- **FR-415**: System MUST return `403 Forbidden` for callers with `merchant:payments:create` only (BFLA).
- **FR-416**: System MUST return `403 Forbidden` for callers with `merchant:payments:operate` only (BFLA).
- **FR-417**: System MUST return `403 Forbidden` for callers with `merchant:payments:read` but no `merchant_id` claim (BFLA).
- **FR-418**: System MUST return `200 OK` for callers with `merchant:payments:read` and matching `merchant_id` claim.
- **FR-419**: System MUST return `403 Forbidden` for callers with `merchant:payments:read` and mismatched `merchant_id` claim (BOLA).
- **FR-420**: System MUST return `200 OK` for callers with `platform:payments:read` for any merchant.
- **FR-421**: System MUST return `403 Forbidden` for callers with platform merchant-only roles but not `platform:payments:read` (BFLA).
- **FR-422**: The authorization matrix MUST label BOLA and BFLA cases clearly in test names, display names, or matrix row labels.
- **FR-423**: Test failure output MUST NOT expose bearer token values.
- **FR-424**: The implementation MUST NOT introduce new payment business functionality.

### Non-Functional Requirements

- **NFR-401**: HTTP edge contract tests MUST characterize Spring MVC default behavior before changing production code. If the default behavior is acceptable, the test asserts it. If it is a product risk, production code is changed only after characterization.
- **NFR-402**: Authorization matrix tests MUST use `@ParameterizedTest` / `@MethodSource` for readability and maintainability.
- **NFR-403**: Test classes MUST NOT duplicate existing test coverage from `PaymentOrderSummaryRestAssuredTest`, `PaymentOrderSummaryBusinessFlowRestAssuredTest`, or `PaymentOrderSummarySecurityTest` unless the new test adds HTTP edge or matrix labeling value.
- **NFR-404**: Test data MUST be created per test or per parameterized case group. No shared mutable fixtures.
- **NFR-405**: The feature MUST NOT require a new Flyway migration.
- **NFR-406**: The feature MUST NOT require frontend changes unless a backend contract fix demands consumer alignment.

## Quality and Architecture Impact *(mandatory)*

### Tester-Led Risk Notes

- HTTP edge behavior depends on Spring MVC defaults. The tester must characterize actual behavior before locking assertions. For example, Spring MVC may return `406` for unsupported `Accept`, or it may return `200` with JSON if content negotiation is permissive.
- Route collision between `/summary` and `/{paymentOrderId}` is a real risk if matcher ordering in `SecurityConfig` or controller mapping changes. The test must verify that `/summary` resolves to the summary endpoint.
- Malformed UUID handling depends on `MethodArgumentTypeMismatchException` being caught by `PaymentExceptionHandler`. If the exception handler is removed or changed, the error contract breaks.
- The authorization matrix must distinguish BOLA from BFLA in test labels. A flat list of `403` cases without labeling loses the educational value.
- `TestJwtSupport` currently does not have a method to create a merchant reader token without `merchant_id` claim. A new helper method or inline token construction may be needed.
- Token values must not appear in test failure output. REST Assured logging and assertion messages must be checked for token leakage.

### Modulith Impact *(required for backend-relevant features; otherwise state N/A)*

- **Module Ownership**: `payment` module — unchanged. All test code targets existing `payment` module endpoints.
- **Module API Impact**: No new public API types. Test classes are in `lab.paymentquality.rest` and `lab.paymentquality.security` packages.
- **Dependency Impact**: No new module dependencies.
- **Event Impact**: No events.
- **Module Test Impact**: `PaymentModuleTest` architecture verification must still pass.

### Security, Data, and Observability Impact

- **Authentication**: Existing JWT Resource Server — no changes.
- **Authorization**: Reuses existing authorities. No new roles. The authorization matrix tests verify existing policy.
- **Ownership**: `merchant_id` claim enforcement — no changes. The matrix tests verify existing enforcement.
- **Validation**: Existing `PaymentExceptionHandler` — no changes unless a bug is found.
- **Persistence**: No new tables, no writes. Read-only tests.
- **Transactions**: No changes.
- **Audit**: No audit trail changes.
- **Logging**: `X-Correlation-ID` propagated. No sensitive data in test output.
- **Error Contract**: Existing `PaymentExceptionHandler` pattern. Tests verify stability.

### Key Entities *(include if feature involves data)*

- **Payment Order** (existing): Source of aggregation data. No new fields or status values.
- **Summary Response** (existing): Read-only projection. No changes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-401**: New summary HTTP edge contract tests compile and pass.
- **SC-402**: New summary authorization matrix tests compile and pass.
- **SC-403**: The summary route collision risk with `{paymentOrderId}` is covered by a test.
- **SC-404**: Malformed path, unsupported method, unsupported accept, and conditional header behavior are either asserted or explicitly characterized.
- **SC-405**: The authorization matrix covers `401`, `403`, and `200` outcomes.
- **SC-406**: BOLA and BFLA are visible in test names, display names, or matrix row labels.
- **SC-407**: No new payment business capability is introduced.
- **SC-408**: No new endpoint, role, claim, status, PSP integration, Kafka integration, or business dashboard is introduced.
- **SC-409**: Existing summary/list/security regression tests remain green.
- **SC-410**: `PaymentModuleTest` remains green.
- **SC-411**: Vault lesson evidence is updated after implementation in later Spec Kit steps.

## Assumptions

- Existing `PaymentOrderController` route mappings remain unchanged.
- Existing `SecurityConfig` matcher ordering remains: `/summary` before `/{paymentOrderId}` wildcard.
- Existing `PaymentExceptionHandler.handleTypeMismatch()` handles malformed UUID path variables.
- Spring MVC returns `405 Method Not Allowed` for methods not mapped to a controller handler.
- Spring MVC content negotiation behavior for `Accept` headers is consistent and characterizable.
- `TestJwtSupport` provides sufficient token construction methods for most matrix rows. A new method for merchant reader without `merchant_id` claim may be needed.
- The existing `PaymentOrderSummaryApiTestSupport.summaryReaderRequest()` helper is reusable for matrix tests.
- No database migration is needed.
- No frontend change is needed.

## Backend / Module Impact

- **Production code changes**: None expected by default. Production code changes are allowed only when contract tests expose a real bug or ambiguous behavior that must be stabilized.
- **Potential production touchpoints** (only if tests expose a bug):
  - `PaymentOrderController.java` — route mapping or ownership enforcement.
  - `PaymentExceptionHandler.java` — error contract stability.
  - `SecurityConfig.java` — matcher ordering.
- **Module boundary**: No new module, no new public API types.

## Database Impact

- No schema changes expected.
- No new Flyway migration expected.
- Optional batch 10C may include a repository/service-level aggregation diagnostic test or `EXPLAIN` learning note.

## API Impact

- No new endpoints.
- No changes to existing endpoint contracts.
- HTTP edge behavior is characterized and tested, not changed (unless a bug is found).

## Security Impact

- No new roles, claims, or authorities.
- Authorization matrix tests verify existing security policy.
- BOLA and BFLA cases are explicitly labeled.
- Token values are not exposed in test output.

## Frontend Impact

- No frontend changes expected.
- Frontend changes are required only if a backend contract fix demands consumer alignment.

## Test Strategy Impact

### New Test Classes

- `PaymentOrderSummaryHttpContractRestAssuredTest` — HTTP edge contract tests for summary.
- `PaymentOrderSummaryAuthorizationMatrixTest` — Parameterized authorization matrix tests for summary.

### Test Design Techniques

- **Equivalence Partitioning**: HTTP edge cases partitioned by method, accept header, path variable format, and route.
- **Boundary Value Analysis**: Malformed UUID boundary (empty string, numeric, null, valid UUID).
- **Decision Table Testing**: Authorization matrix as a decision table with actor × resource × expected status.
- **Parameterized Testing**: `@ParameterizedTest` / `@MethodSource` for authorization matrix rows.

### Assertion Strategy

| Risk | Best Oracle |
|---|---|
| HTTP protocol behavior | REST Assured status/header/content-type assertions |
| Error envelope drift | REST Assured body assertions for `error`, `message`, `correlationId` |
| Role policy drift | Parameterized REST Assured security matrix with BOLA/BFLA labels |
| Route collision | REST Assured response shape assertion (summary vs single order) |
| Token leakage | Manual review of test output and assertion messages |

### Existing Tests Not Duplicated

- `PaymentOrderSummaryRestAssuredTest` — happy path, filters, validation, correlation, no ETag.
- `PaymentOrderSummaryBusinessFlowRestAssuredTest` — deterministic oracle, cross-tenant, platform reader.
- `PaymentOrderSummarySecurityTest` — hand-coded 401/403/own/platform matrix.
- `PaymentOrderListRestAssuredTest` — list filters, pagination, sort, validation.
- `PaymentOrderSecurityTest` — create/read role and tenant isolation matrix.

### Candidate Test Names

HTTP contract tests:

- `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape`
- `malformedMerchantIdReturnsValidationError`
- `unsupportedMethodsDoNotExposeSummaryMutationSurface`
- `unsupportedAcceptIsRejectedOrExplicitlyCharacterized`
- `ifNoneMatchDoesNotEnableSummaryCaching`

Authorization matrix test:

- `summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership`

## Parallel / Data Isolation Impact

- Each test or parameterized case group creates its own merchant and token.
- No shared mutable fixtures.
- No test ordering dependency.
- Token values are not logged or exposed in failure messages.
- Each test uses unique merchant IDs and idempotency keys through existing helpers.

## Risks

| # | Risk | Impact | Mitigation |
|---|---|---|---|
| R-401 | Spring MVC default behavior for unsupported `Accept` differs from expectation | Test asserts wrong status | Characterize actual behavior first, then lock assertion |
| R-402 | Spring MVC default behavior for unsupported methods differs from `405` | Test asserts wrong status | Characterize actual behavior first, then lock assertion |
| R-403 | Route collision between `/summary` and `/{paymentOrderId}` if matcher ordering changes | Summary returns single order shape | Test verifies response shape, not just status code |
| R-404 | `TestJwtSupport` lacks method for merchant reader without `merchant_id` claim | Matrix row cannot be expressed | Add new helper method or inline token construction |
| R-405 | Token values leak into test failure output | Security risk | Review REST Assured logging config and assertion messages |
| R-406 | Authorization matrix becomes unreadable DSL | Test maintainability suffers | Keep matrix rows simple, use `@DisplayName` for readability |
| R-407 | Production code change introduces regression | Existing tests break | Run full regression suite after any production change |
| R-408 | `PaymentExceptionHandler` does not catch `MethodArgumentTypeMismatchException` for summary path | Malformed UUID returns `500` instead of `400` | Verify handler covers summary controller; fix if needed |

## Task Breakdown

### Recommended Task Groups for `/speckit.plan` and `/speckit.tasks`

1. **HTTP Edge Contract Tests** (Batch 10A)
   - Characterize Spring MVC behavior for unsupported `Accept`, unsupported methods, and conditional headers.
   - Implement `PaymentOrderSummaryHttpContractRestAssuredTest` with 5 tests.
   - Verify route collision guardrail.
   - Verify malformed UUID error contract.

2. **Parameterized Authorization Matrix** (Batch 10B)
   - Add `merchantPaymentReaderTokenWithoutMerchantIdClaim()` to `TestJwtSupport` if needed.
   - Implement `PaymentOrderSummaryAuthorizationMatrixTest` with `@ParameterizedTest` / `@MethodSource`.
   - Label BOLA and BFLA cases in display names.
   - Verify all 12 matrix rows.

3. **Regression Verification**
   - Run existing summary/list/security regression tests.
   - Run `PaymentModuleTest`.
   - Run `./mvnw -DskipTests package`.

4. **Optional Aggregation Diagnostic** (Batch 10C)
   - Repository/service-level aggregation test or `EXPLAIN` learning note.

5. **Vault Evidence Update**
   - Update Lesson 10 note with actual files and command results.
   - Update Lesson Evidence Tracker.
   - Update Current Lesson and Current Sprint.
   - Update Learning Coverage Backlog.

## Definition of Done

1. New summary HTTP edge contract tests compile and pass.
2. New summary authorization matrix tests compile and pass.
3. The summary route collision risk with `{paymentOrderId}` is covered by a test.
4. Malformed path, unsupported method, unsupported accept, and conditional header behavior are either asserted or explicitly characterized.
5. The authorization matrix covers `401`, `403`, and `200` outcomes.
6. BOLA and BFLA are visible in test names, display names, or matrix row labels.
7. No new payment business capability is introduced.
8. No new endpoint, role, claim, status, PSP integration, Kafka integration, or business dashboard is introduced.
9. Existing summary/list/security regression tests remain green.
10. `PaymentModuleTest` remains green.
11. `./mvnw -DskipTests package` succeeds.
12. Vault lesson evidence is updated after implementation.

## Verification Commands

Run from `apps/backend` after implementation:

```bash
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

Frontend verification only if frontend files change:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```

## Authorization Matrix Reference

| # | Actor / Principal | Role / Scope | Target Merchant | Expected HTTP | BOLA/BFLA Label |
|---|---|---|---|---|---|
| 1 | Unauthenticated caller | none | any | `401` | — |
| 2 | Invalid issuer token | any | any | `401` | — |
| 3 | Invalid signature token | any | any | `401` | — |
| 4 | Expired token | any | any | `401` | — |
| 5 | Denied authenticated caller | none | any | `403` | BFLA |
| 6 | Merchant create-only | `merchant:payments:create` | own | `403` | BFLA |
| 7 | Merchant operate-only | `merchant:payments:operate` | own | `403` | BFLA |
| 8 | Merchant read without `merchant_id` | `merchant:payments:read` | any | `403` | BFLA |
| 9 | Merchant read own | `merchant:payments:read` + matching `merchant_id` | own | `200` | — |
| 10 | Merchant read other | `merchant:payments:read` + mismatched `merchant_id` | other | `403` | BOLA |
| 11 | Platform payment reader | `platform:payments:read` | any | `200` | — |
| 12 | Platform merchant-only | platform merchant role, no payment read | any | `403` | BFLA |
