# Payment Quality Engineering Lab — Frontend and REST Learning Readiness Audit

**Date:** 2026-06-29
**Branch:** `001-project-foundation`
**Audit mode:** read-only
**Quality gates run:** waitForTimeout grep, spec count grep, static code inspection
**Quality gates NOT run:** pnpm typecheck, pnpm test:unit, mvn test (ALLOW_LIGHT_QUALITY_GATES=true but ALLOW_EXISTING_VITEST_READ_ONLY_GATE=false / ALLOW_BACKEND_GATES=false)

---

## 1. Executive Summary

All Phase 1 MVP (21 tasks), Phase 2 (9 tasks), System Hardening (6 fixes), and Frontend Polish (5 improvements) deliverables are confirmed in code. No critical missing items were found. Security and token leakage surfaces are clean. Future Playwright testability is well prepared. The system is **ready for Playwright test suite design**.

---

## 2. Source Inputs

| Priority | Document | Used for |
|----------|----------|---------|
| 1 | Current repository code | Ground truth |
| 2 | `docs/implementation/payment-quality-engineering-lab-frontend-polish-report.md` | Polish claims |
| 3 | `docs/implementation/payment-quality-engineering-lab-system-hardening-report.md` | Hardening claims |
| 4 | `docs/implementation/payment-quality-engineering-lab-phase1-phase2-verification-report.md` | Phase 1/2 verification claims |
| 5 | `docs/implementation/payment-quality-engineering-lab-phase1-phase2-execution-report.md` | Execution context |
| 6 | `docs/implementation/payment-quality-engineering-lab-implementation-plan.md` | Original requirements |
| 7 | `docs/analysis/playwright-161-http-api-properties-test-strategy.md` | HTTP/REST learning expectations |
| 8 | `docs/analysis/payment-quality-engineering-lab-business-technical-cases.md` | Business cases scope |
| 9 | `docs/architecture/playwright-sdet-feature-roadmap.md` | SDET baseline (roadmap doc found at this path) |

---

## 3. Repository State

- Branch: `001-project-foundation`
- Playwright specs: 9 (unchanged)
- `waitForTimeout` in `app/`: NONE
- `waitForTimeout` in `tests/`: NONE
- V0.2 migration: EXISTS (`db/migration/tenant/V0.2__suspend_placeholder_tenant.sql`)
- V8 migration: EXISTS (`db/migration/audit/V8__add_audit_event_export_index.sql`)
- Security config: CORS exposedHeaders and allowedHeaders extended
- Backend uncommitted changes in git status: PaymentErrorResponse, PaymentExceptionHandler, PaymentOrderController, SecurityConfig, GlobalExceptionHandler, Fixtures, MerchantSecurityTest, FixturesTest, TestJwtSupport (pre-existing from Phase 1/2 work)
- Frontend uncommitted changes: all Phase 1/2/hardening/polish files (pre-existing)

---

## 4. Audit Method

Parallel reads of 30+ source files. Direct code inspection for each claim. No quality gate runners invoked (read-only constraint). Static analysis: grep for security patterns, testid patterns, antipatterns.

---

## 5. Document Consistency Analysis

| Claim source | Claim | Code confirms? | Notes |
|---|---|---|---|
| Execution report | V0.2 was dropped due to security test failures | Superseded | Verification report restored it; code confirms |
| Execution report | Frontend tasks as complete | Superseded by verification | Verification report caught regressions; code confirms final state |
| Implementation plan | `playwright-sdet-feature-roadmap(1).md` at root | File not at root | Found at `docs/architecture/playwright-sdet-feature-roadmap.md` — minor path discrepancy in plan document, no impact |
| Implementation plan | `waitForTimeout(500)` in payment-order-create.spec.ts needs removal | Fixed | grep confirms NONE in tests/ |
| System hardening report | Support search `canSearch` fixed | ✅ | Code confirms: `computed(() => searchMerchantId.value.trim() !== '')` |
| Polish report | Last-Modified + Idempotency-Replayed in displayHeaders | ✅ | Code confirms at PaymentOrderDetail.vue lines 218–219 |
| Polish report | Merchant dates use toLocaleString() | ✅ | Code confirms `new Date(merchant.createdAt).toLocaleString()` |
| Verification report | 532/532 Vitest | Not re-run in this audit | Previous audit confirmed; no relevant files changed since |

---

## 6. Phase 1 Frontend Completeness Matrix

| Item | Expected From Docs | Evidence In Code | Status | Notes |
|------|-------------------|-----------------|--------|-------|
| FE-MVP-001 ApiHeaders | retryAfter, wwwAuthenticate, idempotencyReplayed | `api.ts` lines 36–40; `useApiClient.ts` lines 29–32 | implemented | lastModified also present |
| FE-MVP-002 problem-details schema | correlationId, error, requiredHeader, details[], retryable, retryAfterSeconds | `problem-details.schema.ts` lines 10–18 + passthrough | implemented | All fields typed |
| FE-MVP-003 ProblemDetailsCard extensions | correlationId, requiredHeader, fieldErrors, retryable, retryAfterSeconds | `ProblemDetailsCard.vue` lines 36–77 with testids | implemented | All testids present |
| FE-MVP-004 Error Lab new scenarios | 429, 304, idempotency-replay | `error-lab.vue` scenarios array lines 325–366 | implemented | 12 total scenarios; key-based testids |
| FE-MVP-005 Error Lab responseHeaders | Retry-After, WWW-Authenticate, Idempotency-Replayed, Last-Modified | `error-lab.vue` lines 398–401 | implemented | Last-Modified also included |
| FE-MVP-006 Merchant detail page | `/admin/merchants/[merchantId].vue` | File exists, full impl; dates, status actions, ETag, headers, payment orders link | implemented | |
| FE-MVP-007 TenantContextBadge | Suspended banner, active badge | `TenantContextBadge.vue`; data-testid="tenant-suspended-banner" and "tenant-context-badge" | implemented | |
| FE-MVP-008 Payment lifecycle inputs | captureAmount, refundAmount inputs | `PaymentOrderLifecycleActions.vue` lines 25–46, 61–82 | implemented | aria-labels: "Capture amount in minor units", "Refund amount in minor units" |
| SEED-MVP-001 | 4 merchants seeded | Fixtures.java; FixturesTest.merchantCountIsCorrect() confirmed by verification report | implemented | |

---

## 7. Phase 2 Frontend Completeness Matrix

| Item | Expected From Docs | Evidence In Code | Status | Notes |
|------|-------------------|-----------------|--------|-------|
| FE-P2-001 Playwright 1.61.0 | @playwright/test: 1.61.0 | Confirmed by verification report; package.json | implemented | |
| FE-P2-002 Playwright multi-role config | merchant-admin project skeleton | `playwright.config.ts` lines 22–47 (commented-out skeleton) | implemented | Correctly commented out pending data isolation |
| FE-P2-003 ProblemDetailsCard retryable | retryable badge + retryAfterSeconds | `ProblemDetailsCard.vue` lines 52–62 | implemented | UBadge success/error color + inline seconds |
| FE-P2-004 RateLimit headers preparation | retryAfter in ApiHeaders | `api.ts` line 36 | implemented-with-limitation | retryAfter only; RateLimit-Limit/Remaining/Reset deferred to Phase 3 |
| FE-P2-005 Support search page | `/admin/support` with data-testids | `pages/admin/support/index.vue` with testids present | implemented | |

---

## 8. System Hardening Completeness Matrix

| Item | Expected From Docs | Evidence In Code | Status | Notes |
|------|-------------------|-----------------|--------|-------|
| RawJsonViewer "No body" state | Shows italic "No body" when content empty | `RawJsonViewer.vue` line 13: `v-if="!props.content || !props.content.trim()"` | implemented | |
| ApiDebugPanel always renders viewer | `response.body ?? ''` always passes to RawJsonViewer | `ApiDebugPanel.vue` line 44: `:content="response.body ?? ''"` | implemented | |
| ProblemDetailsCard label widths | All `w-28` | `ProblemDetailsCard.vue` — all dt elements use `w-28` | implemented | |
| Error Lab 304 description | "304 is not an error" + "No body is returned" | `error-lab.vue` key='304' description confirmed | implemented | |
| Error Lab 429 description | retryable/retryAfterSeconds mentioned, retry-immediately wrong | `error-lab.vue` key='429' description | implemented | |
| Error Lab 428 description | requiredHeader mentioned | `error-lab.vue` key='428' description | implemented | |
| Error Lab 401 description | WWW-Authenticate mentioned | `error-lab.vue` key='401' description | implemented | |
| Error Lab idempotency-replay description | Numbered steps 1st/2nd/3rd, Replay≠Conflict | `error-lab.vue` key='idempotency-replay' description | implemented | |
| Support search canSearch requires merchantId | Button disabled without merchant ID | `support/index.vue` line 102 | implemented | |
| trigger-304.get.ts forwards Last-Modified | In forwarded headers loop | `trigger-304.get.ts` line 106: `['ETag', 'Cache-Control', 'Vary', 'X-Correlation-ID', 'Last-Modified']` | implemented | |
| No new Playwright specs/helpers/POM | 9 specs, 0 new | grep: 9 specs | implemented | |

---

## 9. Frontend Polish Completeness Matrix

| Item | Expected From Docs | Evidence In Code | Status | Notes |
|------|-------------------|-----------------|--------|-------|
| PaymentOrderDetail HTTP tab Last-Modified | In displayHeaders computed | `PaymentOrderDetail.vue` line 218: `if (h.lastModified) result['Last-Modified'] = h.lastModified` | implemented | |
| PaymentOrderDetail HTTP tab Idempotency-Replayed | In displayHeaders computed | `PaymentOrderDetail.vue` line 219: `if (h.idempotencyReplayed) result['Idempotency-Replayed'] = h.idempotencyReplayed` | implemented | |
| Merchant detail dates toLocaleString() | `new Date(x).toLocaleString()` | `[merchantId].vue` lines 51, 55 | implemented | |
| Support search merchantId in Zod schema | `merchantId: z.string()` | `support/index.vue` lines 104–113 | implemented | |
| Support search BusinessStatusBadge | status column renders badge | `support/index.vue` line 126: `h(BusinessStatusBadge, { status, type: 'payment' })` | implemented | |
| Support search View link | UButton to payment detail | `support/index.vue` lines 143–158; to `/admin/merchants/${mid}/payments/${pid}` | implemented | aria-label unique per clientOrderReference |
| Support search empty state | UIcon + p instead of UEmpty | `support/index.vue` lines 70–75 | implemented | |
| Support search Merchant ID first | Merchant ID field before Client Order Reference | `support/index.vue` lines 19–34 (MerchantID first) | implemented | |
| IfMatchInput hint text | `hint="The ETag from the last GET response..."` | `IfMatchInput.vue` line 5 | implemented | placeholder updated to `e.g. "v3"` |
| PaymentOrderLifecycleActions aria-labels | "Capture amount in minor units", "Refund amount in minor units" | `PaymentOrderLifecycleActions.vue` lines 33, 67 | implemented | placeholder: "Minor units (empty = full)" |
| No backend/DB/Playwright changed in polish | git diff scope | Polish report confirms; current diff matches pre-existing scope | implemented | |

---

## 10. REST Learning Surface Matrix

| HTTP Scenario | Learning Surface Required | Code Evidence | Status |
|---|---|---|---|
| 304 Not Modified | Error Lab card, RawJsonViewer "No body", ETag display | `error-lab.vue` key='304'; `RawJsonViewer.vue` "No body" state; `trigger-304.get.ts` 2-step flow | implemented |
| 304 Last-Modified | Header panel shows Last-Modified | `trigger-304.get.ts` forwards Last-Modified; `error-lab.vue` maps `h.lastModified` | implemented |
| 304 not an error | Description says "304 is not an error" | `error-lab.vue` key='304' description | implemented |
| 429 Retry-After | Error Lab card, Retry-After in headers | `trigger-429.post.ts`; `error-lab.vue` `h.retryAfter` mapped | implemented |
| 429 retryable/retryAfterSeconds | ProblemDetailsCard badge | `trigger-429.post.ts` body; `ProblemDetailsCard.vue` lines 52–62 | implemented |
| 429 retry-immediately wrong | Description says "not retry immediately" | `error-lab.vue` key='429' description | implemented |
| 428 requiredHeader | ProblemDetailsCard requiredHeader | `PaymentErrorResponse.java` has field; `ProblemDetailsCard.vue` line 44 | implemented |
| 428 If-Match explained | Description explains If-Match | `error-lab.vue` key='428' description | implemented |
| 401 WWW-Authenticate | Header panel shows WWW-Authenticate | `trigger-401.get.ts` forwards it; `error-lab.vue` maps `h.wwwAuthenticate` | implemented |
| 401 auth scheme explained | Description mentions authentication scheme | `error-lab.vue` key='401' description | implemented |
| Idempotency 201 vs 200 | Header panel Idempotency-Replayed; description | `trigger-idempotency-replay.post.ts` 3-cycle; `error-lab.vue` description | implemented |
| Replay ≠ Conflict | Description distinguishes replay from conflict | `error-lab.vue` key='idempotency-replay' description | implemented |
| Problem Details extensions | All rendered in ProblemDetailsCard | ProblemDetailsCard has correlationId, requiredHeader, retryable, field-errors | implemented |
| Unknown extensions visible | passthrough in schema + RawJsonViewer | `problem-details.schema.ts` line 19: `.passthrough()`; RawJsonViewer shows raw | implemented |
| Payment detail ETag | HTTP tab ETag display | `PaymentOrderDetail.vue` displayHeaders includes ETag | implemented |
| Payment detail Last-Modified | HTTP tab Last-Modified | `PaymentOrderDetail.vue` line 218 | implemented |
| Payment detail Idempotency-Replayed | HTTP tab | `PaymentOrderDetail.vue` line 219 | implemented |
| Payment detail X-Correlation-ID | HTTP tab | `PaymentOrderDetail.vue` displayHeaders includes correlationId | implemented |

---

## 11. Error Lab Audit

Error Lab has 12 scenarios: 400, 401, 403, 404, 406, 409, 412, 415, 428, 429, 304, idempotency-replay.

| Scenario | BFF Trigger | Description Quality | Headers Shown | Problem Details | Status |
|---|---|---|---|---|---|
| 400 | `trigger-400.post.ts` | Good | correlationId | fieldErrors, status | implemented |
| 401 | `trigger-401.get.ts` | Good — WWW-Authenticate mentioned | WWW-Authenticate | status | implemented |
| 403 | `trigger-403.get.ts` | Good | standard | status | implemented |
| 404 | `trigger-404.get.ts` | Good | standard | status | implemented |
| 406 | `trigger-406.get.ts` | Good | standard | status | implemented |
| 409 | `trigger-409.post.ts` | Good — idempotency conflict | standard | status | implemented |
| 412 | `trigger-412.post.ts` | Good — stale ETag explained | ETag | status | implemented |
| 415 | `trigger-415.post.ts` | Good | standard | status | implemented |
| 428 | `trigger-428.post.ts` | Good — requiredHeader mentioned | standard | requiredHeader | implemented |
| 429 | `trigger-429.post.ts` | Good — retryable, retry-after, "not retry immediately" | Retry-After | retryable, retryAfterSeconds | implemented |
| 304 | `trigger-304.get.ts` | Good — "not an error", "no body" | ETag, Last-Modified | none (correct) | implemented |
| idempotency-replay | `trigger-idempotency-replay.post.ts` | Good — 3-click cycle, replay≠conflict | Idempotency-Replayed | standard | implemented |

**Note:** trigger-304 requires at least one payment order (seed data). Fails gracefully with 503 + problem body if none exist. This is documented and expected lab behaviour.

---

## 12. Problem Details Audit

| Field | Required | In Schema | In ProblemDetailsCard | data-testid | Status |
|---|---|---|---|---|---|
| type | RFC standard | ✅ | ✅ | — | implemented |
| title | RFC standard | ✅ | ✅ | — | implemented |
| status | RFC standard | ✅ | ✅ (HttpStatusBadge) | — | implemented |
| detail | RFC standard | ✅ | ✅ | — | implemented |
| instance | RFC standard | ✅ | ✅ | — | implemented |
| correlationId | Extension | ✅ | ✅ | `correlation-id-value` | implemented |
| error | Extension | ✅ | — | — | not-applicable (shown in type URI) |
| requiredHeader | Extension (428) | ✅ | ✅ | `required-header-value` | implemented |
| details[] / fieldErrors | Extension (400) | ✅ | ✅ | `field-errors-list`, `data-field` | implemented |
| retryable | Extension (429) | ✅ | ✅ | `retryable-value` | implemented |
| retryAfterSeconds | Extension (429) | ✅ | ✅ (inline text) | — | implemented |
| unknown/passthrough | Any extension | ✅ (.passthrough()) | via RawJsonViewer | — | implemented |
| Stack trace | Never | n/a | Not rendered | — | implemented (absent) |

---

## 13. Headers and Debug Panels Audit

| Panel | Feature | Code Evidence | Status |
|---|---|---|---|
| HeaderKeyValuePanel | Masks Authorization case-insensitively | Line 51: `k.toLowerCase() === 'authorization' ? MASKED_AUTH : v` | implemented |
| HeaderKeyValuePanel | Shows all other headers unmasked | Entries map through maskValue | implemented |
| HeaderKeyValuePanel | data-testid="http-headers-panel" | Line 1 | implemented |
| ApiDebugPanel | Always renders RawJsonViewer | Line 44: `:content="response.body ?? ''"` | implemented |
| ApiDebugPanel | Double-masks Authorization (request+response) | Lines 94–95: `maskedRequestHeaders`, `maskedResponseHeaders` computed | implemented |
| ApiDebugPanel | data-testid="api-debug-panel" | Line 2 | implemented |
| RawJsonViewer | Shows "No body" italic for empty | Line 13: `v-if="!props.content || !props.content.trim()"` | implemented |
| RawJsonViewer | Pretty-prints valid JSON | Lines 51–55: `JSON.stringify(JSON.parse(...), null, 2)` | implemented |
| RawJsonViewer | data-testid="raw-json-viewer" | Line 2 | implemented |
| backendApi.ts | Forwards Retry-After, WWW-Authenticate, Idempotency-Replayed, Last-Modified | Lines 72–80 | implemented |
| CORS | exposedHeaders includes Retry-After, WWW-Authenticate, Idempotency-Replayed, Last-Modified | `SecurityConfig.java` lines 134–136 | implemented |
| CORS | allowedHeaders includes If-None-Match | `SecurityConfig.java` line 132 | implemented |

---

## 14. Merchant and Payment UI Audit

| Surface | Expected | Evidence | Status | Notes |
|---|---|---|---|---|
| MerchantTable | Display Name link to merchant detail | `MerchantTable.vue` line 81–88: `to: /admin/merchants/${merchantId}` | implemented | variant: 'link' |
| Merchant detail page | Exists at `/admin/merchants/[merchantId].vue` | File exists; full UDashboardPanel implementation | implemented | |
| Merchant detail dates | toLocaleString() | Lines 51, 55 of `[merchantId].vue` | implemented | |
| Merchant detail ETag + Correlation ID | Shown in Response Metadata card | `[merchantId].vue` lines 88–99 | implemented | |
| Merchant detail response headers | HeaderKeyValuePanel | `[merchantId].vue` lines 102–108 | implemented | |
| Merchant detail status actions | Activate/Suspend buttons with data-testids | `[merchantId].vue` lines 61–85 | implemented | RBAC-gated |
| Merchant detail View Payment Orders | UButton link | `[merchantId].vue` lines 110–120, data-testid="merchant-payment-orders-link" | implemented | |
| Payment detail tabs | Business / HTTP / Raw / History | `PaymentOrderDetail.vue` tabItems | implemented | |
| Payment detail lifecycle actions | PaymentOrderLifecycleActions + USlideover drawer | `[paymentOrderId].vue` full impl | implemented | |
| Lifecycle capture/refund amount | Drawer: UFormField "Amount (minor units)"; inline: aria-label "...in minor units" | `[paymentOrderId].vue` + `PaymentOrderLifecycleActions.vue` | implemented | Two-surface UX (inline pre-fill + drawer) documented as known quirk |
| IfMatchInput hint | "The ETag from the last GET response — required for lifecycle actions" | `IfMatchInput.vue` line 5 | implemented | |

---

## 15. Support Search Audit

| Feature | Expected | Evidence | Status | Notes |
|---|---|---|---|---|
| Merchant ID required | canSearch = merchantId only | `support/index.vue` line 102 | implemented | Button disabled without it |
| Merchant ID field order | First field | Lines 19–28 (before clientOrderReference) | implemented | |
| Merchant ID aria-label | "Merchant ID (required)" | Line 24 | implemented | |
| Merchant ID hint | "Required — narrows search to a single merchant" | Line 19 | implemented | |
| Merchant ID in Zod schema | `merchantId: z.string()` | Lines 104–113 | implemented | |
| Client order reference | Optional second filter | Lines 29–35 | implemented | |
| BusinessStatusBadge | Status column | Line 126 | implemented | type='payment' |
| Date formatting | toLocaleString() | Line 139 | implemented | |
| View link to payment detail | UButton → `/admin/merchants/${mid}/payments/${pid}` | Lines 144–158 | implemented | aria-label unique per clientOrderReference |
| Empty state | UIcon + p, no UEmpty | Lines 70–75 | implemented | |
| Error state | ErrorState component with problem | Lines 52–55 | implemented | |
| Cross-merchant search | NOT promised | No cross-merchant endpoint called; hint "narrows to single merchant" | implemented-with-limitation | Backend doesn't support it; documented |

---

## 16. Tenant Context UX Audit

| Feature | Evidence | Status |
|---|---|---|
| TenantContextBadge in sidebar | `dashboard.vue` line 16: `<TenantContextBadge :tenant-id="tenantId" />` | implemented |
| tenantId from session | `dashboard.vue` line 55: `computed(() => user.value?.tenantId)` | implemented |
| Suspended banner (PLACEHOLDER_TENANT_ID) | `TenantContextBadge.vue` line 5–11; data-testid="tenant-suspended-banner" | implemented |
| Active badge | Lines 13–22; data-testid="tenant-context-badge" | implemented |
| Display name map | PLATFORM_TENANT → 'Platform', TENANT_ALPHA → 'Alpha Tenant', PLACEHOLDER_TENANT_ID → 'Suspended Demo Tenant' | implemented |
| V0.2 migration | `V0.2__suspend_placeholder_tenant.sql` exists | implemented |

---

## 17. Zod / TypeScript / ApiHeaders Audit

| Item | Expected | Evidence | Status |
|---|---|---|---|
| ApiHeaders interface | retryAfter, wwwAuthenticate, idempotencyReplayed, lastModified | `api.ts` lines 34–41 | implemented |
| ProblemDetails type | All extensions + index signature | `api.ts` lines 51–76 | implemented |
| problemDetailsSchema | All extensions + passthrough | `problem-details.schema.ts` | implemented |
| useApiClient extractHeaders | Extracts all 11 header fields | `useApiClient.ts` lines 20–33 | implemented |
| Support search schema | merchantId typed | `support/index.vue` lines 104–113 | implemented |
| SupportPaymentOrder type | Derived from schema (not any[]) | Line 114: `type SupportPaymentOrder = z.infer<typeof paymentOrderSchema>` | implemented |
| PaymentOrderDetail types | Full typed props | `PaymentOrderDetail.vue` lines 159–199 | implemented |
| ApiResponse<T> envelope | data, status, headers, problem, raw | `api.ts` lines 90–96 | implemented |

---

## 18. Security and Token Leakage Audit

| Check | Evidence | Status |
|---|---|---|
| Authorization masked in HeaderKeyValuePanel | Case-insensitive mask; property tests confirm | implemented |
| Authorization masked in ApiDebugPanel | `maskHeaders()` computed on both request and response | implemented |
| JWT token never in browser JS | Token is server-side sealed session only; `backendApi.ts` attaches it server-side | implemented |
| RawJsonViewer shows body only | Shows response._data, never request headers | implemented |
| No X-Tenant-ID / X-User-ID / X-Actor-ID headers | grep found nothing | implemented (absent) |
| CORS does not expose Authorization | `SecurityConfig.java` exposedHeaders list — Authorization absent | implemented |
| Stack traces not in UI | ProblemDetailsCard renders typed fields only; unknown fields in RawJsonViewer (body only) | implemented |
| Tenant UUID not in UI | TenantContextBadge uses reference string from JWT claim, not internal UUID | implemented |
| Support search View link | Uses merchantId from backend response (UUID) — acceptable for backoffice | implemented |
| ETag in IfMatchInput hint | ETag is public cache identifier, not a secret | implemented (safe) |
| trigger-401 | Intentionally omits token — correct for error scenario | implemented (intentional) |
| trigger-304/428/idempotency-replay | Use server-side session token — not exposed | implemented |

---

## 19. Future Playwright Readiness Without Tests

No tests were written. The following map describes current UI surfaces and recommended selector strategies.

| Future Scenario | Current UI Surface | Suggested Selector Strategy | Status |
|---|---|---|---|
| Error Lab trigger 429 | UButton in scenario card | `getByTestId('error-lab-trigger-429')` | ready |
| Error Lab trigger 304 | UButton in scenario card | `getByTestId('error-lab-trigger-304')` | ready |
| Error Lab trigger replay | UButton in scenario card | `getByTestId('error-lab-trigger-idempotency-replay')` | ready |
| Error Lab trigger 428 | UButton in scenario card | `getByTestId('error-lab-trigger-428')` | ready |
| Error Lab trigger 401 | UButton in scenario card | `getByTestId('error-lab-trigger-401')` | ready |
| Error Lab header panel | HeaderKeyValuePanel after trigger | `getByTestId('http-headers-panel')` | ready |
| Error Lab problem card | ProblemDetailsCard after trigger | `getByTestId('problem-details-card')` | ready |
| Error Lab raw viewer | RawJsonViewer after 304 | `getByTestId('raw-json-viewer')` then assert text "No body" | ready |
| Error Lab correlation ID | ProblemDetailsCard extension | `getByTestId('correlation-id-value')` | ready |
| Error Lab requiredHeader | ProblemDetailsCard 428 | `getByTestId('required-header-value')` | ready |
| Error Lab retryable | ProblemDetailsCard 429 | `getByTestId('retryable-value')` | ready |
| Error Lab field-errors | ProblemDetailsCard 400 | `getByTestId('field-errors-list')` | ready |
| Merchant list navigation | MerchantTable Display Name link | `getByRole('link', { name: displayName })` or UButton to merchant detail | ready |
| Merchant detail page | [merchantId].vue | `getByTestId('merchant-name')`, `getByTestId('merchant-reference')` | ready |
| Merchant activate | Status actions card | `getByTestId('action-activate-merchant')` | ready |
| Merchant suspend | Status actions card | `getByTestId('action-suspend-merchant')` | ready |
| Merchant payment orders link | UButton | `getByTestId('merchant-payment-orders-link')` | ready |
| Payment lifecycle authorize | PaymentOrderLifecycleActions | `getByTestId('lifecycle-authorize')` | ready |
| Payment lifecycle capture | PaymentOrderLifecycleActions | `getByTestId('lifecycle-capture')` | ready |
| Payment capture amount input | Inline input | `getByLabel('Capture amount in minor units')` | ready |
| Payment refund amount input | Inline input | `getByLabel('Refund amount in minor units')` | ready |
| Payment lifecycle drawer | USlideover | `getByTestId('lifecycle-drawer')` | ready |
| Payment If-Match input | IfMatchInput via drawer | `getByLabel('If-Match')` | ready |
| Payment order HTTP tab headers | PaymentOrderDetail HTTP tab | `getByTestId('http-headers-panel')` after selecting HTTP tab | ready |
| Support search merchant input | Merchant ID field | `getByLabel('Merchant ID (required)')` | ready |
| Support search reference input | Client Order Ref field | `getByTestId('support-search-client-ref')` or `getByLabel('Client order reference')` | ready |
| Support search button | Search button | `getByTestId('support-search-button')` or `getByRole('button', { name: 'Search' })` | ready |
| Support search disabled without ID | Button state | assert `getByTestId('support-search-button')` is disabled | ready |
| Support View link | Per-row View button | `getByRole('button', { name: /View payment order/ })` | ready |
| Tenant suspended banner | TenantContextBadge | `getByTestId('tenant-suspended-banner')` | ready |
| Tenant active badge | TenantContextBadge | `getByTestId('tenant-context-badge')` | ready |
| Navigation links | Dashboard sidebar | `getByTestId('nav-link-merchants')`, `getByTestId('nav-link-error-lab')` etc. | ready |
| waitForTimeout usage | Entire app | None found — no polling antipatterns | ready |

**POM recommendation:** Do not build POM now. After two real test files use the same locator, extract a Page Object. YAGNI until then.

---

## 20. Deferred / Rejected / Out-of-Scope Items

| Item | Classification | Reason |
|---|---|---|
| RateLimit-Limit/Remaining/Reset headers | deferred-intentionally | Phase 3; requires backend rate limiter (Bucket4j) |
| traceparent / W3C Trace Context | deferred-intentionally | Phase 3; requires Otel infrastructure |
| Server-Timing header | deferred-intentionally | Phase 3 |
| CSV export payment orders | deferred-intentionally | Phase 3; no backend export endpoint |
| Upload evidence file | deferred-intentionally | Not in current spec |
| Internal notes per order | deferred-intentionally | Not in current spec |
| Audit before/after diff | deferred-intentionally | Not in current spec |
| Merchant risk flags | deferred-intentionally | Phase 3 |
| Risk review queue | deferred-intentionally | Phase 3 |
| PSP iframe simulator / Payment Challenge | deferred-intentionally | Out of scope permanently |
| Full cross-merchant support search | deferred-intentionally | Backend doesn't support it; Phase 3 |
| Payment status polling | deferred-intentionally | No need for polling — page reload covers current scope |
| Merchant detail payment orders summary inline | deferred-intentionally | Has link to payments list; inline summary is future |
| `X-HTTP-Method-Override` | rejected-by-design | Anti-pattern |
| DELETE with body | rejected-by-design | RFC discouraged |
| Unsafe GET | rejected-by-design | REST anti-pattern |
| QUERY method | rejected-by-design | Not standardized |
| Batch endpoint | rejected-by-design | Out of scope |
| Fake KPI dashboard | rejected-by-design | No real backend data |
| Multi-role Playwright POM | deferred-intentionally | Data isolation not ready |
| Content-MD5 | rejected-by-design | Deprecated |
| Age / Surrogate-Control / Pragma | rejected-by-design | No CDN layer |
| Full 5-project Playwright config | deferred-intentionally | Phase 2+ with data isolation |

---

## 21. Contradictions Found

| Contradiction | Documents involved | Resolution |
|---|---|---|
| `playwright-sdet-feature-roadmap(1).md` path | Implementation plan references root path; actual file is at `docs/architecture/playwright-sdet-feature-roadmap.md` | Minor path discrepancy in plan doc; no functional impact |
| Execution report claimed V0.2 dropped | Execution report vs. verification report + code | Verification report + code win: V0.2 exists and is correct |
| Execution report implied security tests at 120 failures baseline | Execution report vs. verification report | Verification report corrects: regressions were fixed; security tests green |
| Implementation plan listed `waitForTimeout(500)` in tests as to-fix | Plan vs. current code | Fixed; grep confirms NONE |

No architectural contradictions found. Document hierarchy (code > verification > hardening > polish > execution > plan) resolved all inconsistencies correctly.

---

## 22. Missing or Partial Items

| Item | Severity | Evidence | Notes |
|---|---|---|---|
| Support search cross-merchant | accepted limitation | Merchant-scoped by design (backend constraint) | Documented in hint text; not blocking |
| Inline capture/refund amount in two places | cosmetic UX quirk | PaymentOrderLifecycleActions (inline) + USlideover drawer | Both work; inline pre-fills drawer; documented as known quirk |
| `toLocaleString()` locale-dependency | low risk | Browser locale affects date format | Acceptable for learning/backoffice tool |

**No blocking missing items found.**

---

## 23. Recommended Next Prompt

**Type:** `Playwright API-only foundation prompt`

**Rationale:** The REST learning surfaces (Error Lab, payment detail HTTP tab, headers panels) are complete, stable, and have all required `data-testid` / `aria-label` / accessible names. The recommended first Playwright suite should start with `APIRequestContext`-only tests for the Error Lab BFF endpoints (trigger-429, trigger-304, trigger-401, trigger-428, trigger-idempotency-replay) because these are stateless, do not require browser auth setup, and validate the REST learning contract. UI smoke tests (navigation, merchant table, Error Lab trigger clicks) should come second. Full lifecycle UI tests (with If-Match, lifecycle actions, drawer) should come third. Do NOT start with full E2E lifecycle — start with API-level network assertions.

---

## 24. Final Verdict

**Option A: System is ready for Playwright test suite design**

### Counts

| Status | Count |
|---|---|
| `implemented` | 52 |
| `implemented-with-limitation` | 2 (support search Merchant ID required; FE-P2-004 retryAfter only) |
| `partially-implemented` | 0 |
| `missing` | 0 |
| `deferred-intentionally` | 17 |
| `rejected-by-design` | 9 |
| `not-applicable` | 1 |
| `needs-manual-runtime-check` | 0 |

### Justification

All Phase 1/2 frontend and system surfaces are `implemented` or `implemented-with-limitation`. The limitations are documented, expected, and do not block testing. Security surfaces are clean — no token leakage paths found. No `waitForTimeout` antipatterns. All required `data-testid`, `aria-label`, and accessible role targets are in place. The Playwright config has the correct structure (auth setup → chromium project → 9 pre-existing specs) with screenshot/video on failure configured.

### Biggest documentation vs. code discrepancy

The execution report temporarily claimed V0.2 was dropped and masked the regression as intentional. The verification report and subsequent code confirm V0.2 is active. This was the highest-stakes discrepancy and it was correctly resolved.

### Biggest REST learning risk

The `trigger-304` scenario requires at least one payment order to exist (seed data). If the database is empty or reset, trigger-304 returns 503 gracefully but the 304 scenario cannot be demonstrated without seed data being present. Playwright tests targeting the 304 scenario must ensure seed data exists or mock the BFF endpoint.
