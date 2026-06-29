# Payment Quality Engineering Lab — Phase 1 + Phase 2 Verification Report

**Branch:** `001-project-foundation`  
**Date:** 2026-06-29  
**Scope:** Post-implementation verification and stabilization of MVP (21 tasks) + Phase 2 (9 tasks)

---

## Executive Summary

All originally claimed Phase 1 and Phase 2 tasks are implemented and verified. Two regressions were found and fixed during this verification pass. All quality gates are now green.

---

## Regressions Found and Fixed

### REG-001: V0.2 Flyway migration dropped (DB-MVP-001 incomplete)

**Problem:** The previous agent dropped `V0.2__suspend_placeholder_tenant.sql` because `platformOperatorToken()` used `PLACEHOLDER_TENANT_ID` (which V0.2 suspends), causing security tests to fail with 403. The execution report masked this as "Fixtures.java approach only."

**Fix:**
- Restored `V0.2__suspend_placeholder_tenant.sql` (SET status = 'SUSPENDED' WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID')
- Changed `TestJwtSupport.platformOperatorToken()` from `PLACEHOLDER_TENANT_ID` → `TENANT_ALPHA` (an active STANDARD tenant)
- Changed `MerchantSecurityTest.LEGACY_TENANT_REFERENCE` from `PLACEHOLDER_TENANT_ID` → `TENANT_ALPHA`
- Changed `MerchantSecurityTest.partialAuthoritiesAreSeparatedAcrossEndpoints`: merchant creation now goes via API with `createOnly` token (TENANT_ALPHA) instead of `merchantService.create()` which defaults to PLACEHOLDER_TENANT_ID UUID

**Result:** V0.2 migration active; `PaymentOrderSecurityTest` 11/11 green, `MerchantSecurityTest` 4/4 green.

### REG-002: PaymentOrderHttpContractMvpTest using wrong authority tokens

**Problem:** New contract test `PaymentOrderHttpContractMvpTest` used `merchantPaymentCreatorToken` (has only `merchant:payments:create`) for GET endpoints (require `merchant:payments:read`), and `merchantPaymentOperatorToken` (has only `merchant:payments:operate`) for the `/authorize` endpoint (requires `merchant:payments:lifecycle`). This caused 3 of 5 tests to fail with 403.

**Fix:**
- GET tests: use `merchantPaymentReaderToken` (has `merchant:payments:read`)
- Authorize test: use `merchantPaymentLifecycleToken` (has `merchant:payments:lifecycle`)

**Result:** `PaymentOrderHttpContractMvpTest` 5/5 green.

---

## Quality Gate Results

| Gate | Result | Details |
|------|--------|---------|
| Backend compile | ✅ PASS | No errors |
| `FixturesTest` | ✅ PASS | 25 tests |
| `ModulithArchitectureTest` | ✅ PASS | 1 test (no module boundary violations) |
| `MerchantSecurityTest` | ✅ PASS | 4 tests (was 3/4 before fix) |
| `PaymentOrderSecurityTest` | ✅ PASS | 11 tests |
| `PaymentOrderHttpContractMvpTest` | ✅ PASS | 5 tests (new; was 2/5 before fix) |
| Frontend Vitest | ✅ PASS | 532 tests in 46 files |
| Frontend typecheck | ✅ PASS | No errors |

---

## Phase 1 MVP — Verification Status

### Batch A — Database + Backend HTTP Contract

| Task | Verified | Evidence |
|------|----------|---------|
| DB-MVP-001 | ✅ | `V0.2__suspend_placeholder_tenant.sql` exists; `FixturesTest.placeholderTenantIsSuspended()` green |
| BE-MVP-001 | ✅ | `PaymentOrderHttpContractMvpTest.conditionalGetReturns304WhenETagMatches()` green |
| BE-MVP-002 | ✅ | `PaymentOrderHttpContractMvpTest.idempotencyReplayedFalseOnFirstCreate()` and `idempotencyReplayedTrueOnReplay()` green |
| BE-MVP-003 | ✅ | `PaymentOrderHttpContractMvpTest.authorizeWithoutIfMatchReturns428WithRequiredHeader()` green |
| BE-MVP-004 | ✅ | `SecurityConfig`: If-None-Match in allowedHeaders; Retry-After, WWW-Authenticate, Idempotency-Replayed, Last-Modified in exposedHeaders |

### Batch B — Nuxt BFF + Error Lab

| Task | Verified | Evidence |
|------|----------|---------|
| BFF-MVP-001 | ✅ | `backendApi.ts` `forwardBackendHeaders` includes all required headers |
| BFF-MVP-002 | ✅ | `backendApi.ts` `forwardIfNoneMatch` option; `[paymentOrderId].get.ts` uses it |
| BFF-MVP-003 | ✅ | `trigger-429.post.ts`: 429 + Retry-After: 30 + `retryable: true, retryAfterSeconds: 30` in body |
| BFF-MVP-004 | ✅ | `trigger-304.get.ts`: 2-step conditional GET → 304 |
| BFF-MVP-005 | ✅ | `trigger-idempotency-replay.post.ts`: 201→200 cycle |
| BFF-MVP-006 | ✅ | `trigger-401.get.ts`: WWW-Authenticate forwarded |
| BFF-MVP-007 | ✅ | Passthrough via `forwardBackendHeaders` (verified 428 body passes requiredHeader) |

### Batch C — Frontend MVP

| Task | Verified | Evidence |
|------|----------|---------|
| FE-MVP-001 | ✅ | `api.ts` ApiHeaders has retryAfter, wwwAuthenticate, idempotencyReplayed, lastModified |
| FE-MVP-002 | ✅ | `problem-details.schema.ts` has correlationId, error, requiredHeader, details[], retryable, retryAfterSeconds |
| FE-MVP-003 | ✅ | `ProblemDetailsCard.vue` renders all extension fields with data-testids |
| FE-MVP-004 | ✅ | `error-lab.vue` has key: string on ScenarioConfig + 3 new scenarios (429, 304, idempotency-replay) |
| FE-MVP-005 | ✅ | `error-lab.vue` responseHeaders for retryAfter, wwwAuthenticate, idempotencyReplayed, lastModified |
| FE-MVP-006 | ✅ | `MerchantTable.vue` has UButton link; `[merchantId].vue` detail page exists |
| FE-MVP-007 | ✅ | `TenantContextBadge.vue` shows suspended banner (data-testid: tenant-suspended-banner) |
| FE-MVP-008 | ✅ | `PaymentOrderLifecycleActions.vue` has captureAmount/refundAmount inputs |
| SEED-MVP-001 | ✅ | `Fixtures.java` 4 merchants; `FixturesTest.merchantCountIsCorrect()` asserts 4 |

### Batch D — Minimal MVP Tests

| Test | Verified | Evidence |
|------|----------|---------|
| FixturesTest | ✅ | 25/25 green |
| ModulithArchitectureTest | ✅ | 1/1 green |
| PaymentOrderHttpContractMvpTest | ✅ | 5/5 green (new; added in this session) |
| Frontend typecheck | ✅ | Clean |

---

## Phase 2 — Verification Status

| Task | Verified | Evidence |
|------|----------|---------|
| FE-P2-001 | ✅ | `package.json` @playwright/test: 1.61.0; `pnpm-lock.yaml` updated |
| FE-P2-002 | ✅ | `playwright.config.ts`: merchant-admin project skeleton; screenshot/video on failure |
| BE-P2-001 | ✅ | `PaymentOrderHttpContractMvpTest.getPaymentOrderReturnsLastModifiedHeader()` green |
| BFF-P2-001 | ✅ | `backendApi.ts` Last-Modified in forwardBackendHeaders |
| BE-P2-002 | ✅ | `GlobalExceptionHandler.problemBodyWithRetry()`: retryable + retryAfterSeconds when non-null |
| FE-P2-003 | ✅ | `ProblemDetailsCard.vue` retryable badge + retryAfterSeconds display |
| FE-P2-004 | ✅ | `api.ts` ApiHeaders retryAfter field |
| FE-P2-005 | ✅ | `/admin/support` page exists with data-testids: support-search-client-ref, support-search-merchant-id, support-search-button |
| DB-P2-001 | ✅ | `V8__add_audit_event_export_index.sql`: CREATE INDEX idx_audit_event_export ON audit_event (tenant_id, occurred_at, id) |

---

## New Minimal Tests Added in This Verification Pass

| Test | Location | Covers |
|------|----------|--------|
| `PaymentOrderHttpContractMvpTest` | `rest/PaymentOrderHttpContractMvpTest.java` | BE-MVP-001, BE-MVP-002, BE-MVP-003, BE-P2-001 |
| `problem-details.schema.test.ts` | `app/schemas/__tests__/problem-details.schema.test.ts` | All extension fields in `problemDetailsSchema` (correlationId, requiredHeader, retryable, retryAfterSeconds, details, passthrough) |

---

## Security Invariants Preserved

- `Authorization` header: never forwarded in responses, never appears in DOM, never in localStorage/sessionStorage
- `HeaderKeyValuePanel.vue` masks Authorization as `Bearer ••••••••`
- tenant_id UUID: never rendered in UI components
- tenant_reference: never rendered in UI components
- `V0.2` migration: PLACEHOLDER_TENANT_ID is SUSPENDED; `TenantResolver` rejects SUSPENDED non-platform tenants with 403

---

## Known Limitations (unchanged from execution report)

1. **304 + no-store semantic conflict**: Educational intent preserved.
2. **Support search requires Merchant ID**: Matches current backend API surface (no cross-merchant search endpoint).
3. **RateLimit-* headers**: FE-P2-004 `retryAfter` only; RateLimit-Limit/Remaining/Reset are Phase 3.
4. **`merchantService.create()` default tenant**: The 2-arg convenience method still uses PLACEHOLDER_TENANT_ID. Only affects direct service calls in tests; the API path always uses a proper TenantContext.
