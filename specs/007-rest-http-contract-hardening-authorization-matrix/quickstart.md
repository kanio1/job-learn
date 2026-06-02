# Quickstart: REST HTTP Contract Hardening and Authorization Matrix

**Feature**: Lesson 10 — REST HTTP Contract Hardening and Authorization Matrix
**Date**: 2026-06-02

## Prerequisites

- Java 25 JDK installed.
- Maven Wrapper (`./mvnw`) available in `apps/backend/`.
- Docker running (for Testcontainers PostgreSQL).
- Lessons 06-09 implemented and passing.

## Step 1: Characterize Spring MVC Defaults

Before writing assertions, run exploratory requests against the existing summary endpoint to document actual behavior:

```bash
cd apps/backend

# Start the application (if not already running)
# Or use a test to send exploratory requests

# Characterize unsupported Accept
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer <token>" \
  -H "Accept: text/xml" \
  http://localhost:8080/api/merchants/<merchantId>/payment-orders/summary

# Characterize unsupported method
curl -s -o /dev/null -w "%{http_code}" \
  -X PUT \
  -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/merchants/<merchantId>/payment-orders/summary

# Characterize malformed UUID
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer <token>" \
  http://localhost:8080/api/merchants/not-a-uuid/payment-orders/summary
```

Document results in `research.md` before locking test assertions.

## Step 2: Implement HTTP Edge Contract Tests

Create `PaymentOrderSummaryHttpContractRestAssuredTest.java` with 5 tests:

1. `summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape`
2. `malformedMerchantIdReturnsValidationError`
3. `unsupportedMethodsDoNotExposeSummaryMutationSurface`
4. `unsupportedAcceptIsRejectedOrExplicitlyCharacterized`
5. `ifNoneMatchDoesNotEnableSummaryCaching`

Run:

```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test
```

## Step 3: Implement Authorization Matrix Tests

Create `PaymentOrderSummaryAuthorizationMatrixTest.java` with `@ParameterizedTest` / `@MethodSource`:

1. 12 matrix rows covering 401, 403, 200.
2. BOLA and BFLA labels in display names.
3. Per-case token and merchant construction.

Run:

```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test
```

## Step 4: Regression Verification

Run existing tests to ensure no regression:

```bash
cd apps/backend
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
./mvnw -Dtest=PaymentModuleTest test
./mvnw -DskipTests package
```

## Step 5: Optional Aggregation Diagnostic (Batch 10C)

If time remains and batches 10A+10B are green:

- Add a repository/service-level aggregation test, or
- Run `EXPLAIN ANALYZE` on the summary query and document index usage.

## Step 6: Update Vault Evidence

After implementation:

1. Update `Lesson 10 - REST HTTP Contract Hardening and Authorization Matrix.md` with actual files and command results.
2. Update `Lesson Evidence Tracker.md` with test evidence and residual risks.
3. Update `Current Lesson.md` and `Current Sprint.md`.
4. Update `Learning Coverage Backlog.md` for HTTP/REST, REST Assured, JUnit, Security and Assertion Strategy statuses.

## Verification Commands Summary

```bash
cd apps/backend

# New tests
./mvnw -Dtest=PaymentOrderSummaryHttpContractRestAssuredTest test
./mvnw -Dtest=PaymentOrderSummaryAuthorizationMatrixTest test

# Regression
./mvnw -Dtest=PaymentOrderSummaryRestAssuredTest,PaymentOrderSummaryBusinessFlowRestAssuredTest,PaymentOrderSummarySecurityTest test
./mvnw -Dtest=PaymentModuleTest test

# Package
./mvnw -DskipTests package
```

Frontend only if frontend files change:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm test:e2e -- payment-orders-panel.spec.ts
```
