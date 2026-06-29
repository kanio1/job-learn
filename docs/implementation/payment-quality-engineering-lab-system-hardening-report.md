# Payment Quality Engineering Lab — System Hardening Report

## 1. Executive Summary

Six targeted fixes were applied during Phase 2.5 hardening. No new features were built. No Playwright tests, helpers, or POM were created. All quality gates are green.

---

## 2. Source Inputs

1. `docs/implementation/payment-quality-engineering-lab-phase1-phase2-verification-report.md` (primary)
2. `docs/implementation/payment-quality-engineering-lab-phase1-phase2-execution-report.md`
3. Current repository code (highest priority)

---

## 3. Repository Discovery

Inspected:
- All Error Lab BFF triggers (`server/api/error-lab/trigger-*.ts`)
- `error-lab.vue` page (12 scenarios, descriptions, transport)
- `ApiDebugPanel.vue`, `RawJsonViewer.vue`, `HeaderKeyValuePanel.vue`
- `ProblemDetailsCard.vue`, `TenantContextBadge.vue`, `EtagDisplay.vue`
- `admin/merchants/[merchantId].vue` (detail page)
- `admin/support/index.vue` (support search)
- `PaymentOrderLifecycleActions.vue` (capture/refund inputs)
- `backendApi.ts` (BFF forwarding)
- `useApiClient.ts` (client composable)
- `types/api.ts`, `schemas/problem-details.schema.ts`
- BFF proxy routes for payment orders list

---

## 4. System Surfaces Reviewed

| Surface | Status Before | Action |
|---------|--------------|--------|
| Error Lab 304 | Empty `<pre>` for no body; Last-Modified not forwarded | Fixed |
| Error Lab 429 | Description missing retryable/retryAfterSeconds context | Improved |
| Error Lab idempotency-replay | Description unclear on cycle semantics | Improved |
| Error Lab 428 | Description didn't mention requiredHeader | Improved |
| Error Lab 401 | Description didn't mention WWW-Authenticate | Improved |
| RawJsonViewer | Blank `<pre>` on empty content | Fixed |
| ApiDebugPanel | `v-if="response.body != null"` — empty string passes | Fixed |
| ProblemDetailsCard | Mixed `w-20`/`w-28` label widths → misaligned values | Fixed |
| Support search | `canSearch` allowed search without Merchant ID | Fixed |
| trigger-304.get.ts | Missing Last-Modified in forwarded headers | Fixed |
| HeaderKeyValuePanel | Authorization masking correct | No change |
| TenantContextBadge | Suspended banner correct | No change |
| Merchant detail page | Layout and metadata complete | No change |
| PaymentOrderLifecycleActions | Inputs have aria-label | No change |
| backendApi.ts | Full header forwarding list correct | No change |
| useApiClient.ts | extractHeaders covers all Phase 1/2 headers | No change |

---

## 5. Backend Changes

None. Backend was already correct after Phase 1/2 verification.

---

## 6. Nuxt BFF Changes

**`server/api/error-lab/trigger-304.get.ts`**
- Added `Last-Modified` to the header forwarding loop alongside ETag, Cache-Control, Vary, X-Correlation-ID
- Phase 2 (BE-P2-001) adds Last-Modified to GET payment order responses; this ensures the 304 trigger exposes it for learning

---

## 7. Frontend / Nuxt UI Changes

### `app/components/shared/RawJsonViewer.vue`
- Added empty state: shows `No body` italic text when `content` is empty or blank
- `<pre>` only renders when content is non-empty
- Fixes the blank viewer that appeared for 304 (no body) and other empty-body responses

### `app/components/shared/ApiDebugPanel.vue`
- Changed body display from `v-if="response.body != null"` to always rendering `RawJsonViewer` with `response.body ?? ''`
- Empty string body now shows `RawJsonViewer`'s "No body" state instead of being hidden entirely
- Rationale: hiding the panel for 304 was confusing — the body section should always appear and explicitly say "No body"

### `app/components/shared/ProblemDetailsCard.vue`
- Unified all field label `dt` widths to `w-28` (was mixed: `w-20` for standard RFC members, `w-28` for extensions)
- Fixes value column misalignment when extension fields appear alongside standard fields

### `app/pages/error-lab.vue` — scenario descriptions
Five scenario descriptions were improved:

| Scenario | Improvement |
|----------|-------------|
| 401 | Added: "Inspect the WWW-Authenticate response header — it tells the client which authentication scheme is expected." |
| 428 | Added: "The requiredHeader field in the Problem Details body names the missing header exactly." |
| 429 | Added mention of `retryable: true`, `retryAfterSeconds: 30` in the body; explains why retry-immediately is wrong |
| 304 | Added: "304 is not an error — it means the resource has not changed; no body is returned." |
| idempotency-replay | Rewritten as numbered steps (1st/2nd/3rd click); clarifies "Replay ≠ Conflict" |

### `app/pages/admin/support/index.vue`
- `canSearch` computed changed from `clientOrderReference || merchantId` to `merchantId only`
- Merchant ID field: added `required` prop and `hint="Required — narrows search to a single merchant"` to `UFormField`
- `aria-label` updated to `"Merchant ID (required)"` for accessibility
- Fixes UX inconsistency where Search button was enabled but handler rejected the request

---

## 8. Error Lab Hardening

All five affected scenarios now have accurate, educational descriptions:

- **304**: Makes explicit that 304 is a cache optimization, not an error; no body is normal
- **429**: Makes explicit that retryable/retryAfterSeconds appear in the body alongside the header
- **Idempotency replay**: Distinguishes first-create (201) from replay (200); distinguishes replay from conflict
- **428**: Points learner to `requiredHeader` field in the body
- **401**: Points learner to `WWW-Authenticate` header in the response

---

## 9. Problem Details Hardening

- Field label width unified (`w-28` for all rows)
- All extension fields (correlationId, requiredHeader, retryable, retryAfterSeconds, field-errors) already rendered correctly
- No token leakage paths found

---

## 10. Headers and Debug Panels Hardening

- `HeaderKeyValuePanel` already masks Authorization as `Bearer ••••••••` — no change needed
- `ApiDebugPanel` has double masking via `maskHeaders()` computed — no change needed
- `RawJsonViewer` now shows "No body" for empty responses — correct for 304
- `trigger-304.get.ts` now forwards Last-Modified — consistent with other triggers

---

## 11. Tenant and Suspended State UX

- `TenantContextBadge` correctly shows red UAlert for `PLACEHOLDER_TENANT_ID`
- Suspended banner has correct `data-testid="tenant-suspended-banner"`
- No changes needed

---

## 12. Merchant Detail and Support Search UX

**Merchant detail** (`admin/merchants/[merchantId].vue`): Already complete — ETag, Correlation ID, Status Actions, Response Headers, Payment Orders link, loading/error states. No changes.

**Support search** (`admin/support/index.vue`): Fixed `canSearch` logic so the Search button is disabled until Merchant ID is provided. Field label now shows `required` and a hint. This aligns the button state with the handler's actual requirement.

---

## 13. Future Testability Notes Without Test Implementation

- `getByLabel('Merchant ID (required)')` or `getByLabel('Client Order Reference')` can target the support search inputs
- `getByRole('button', { name: 'Search' })` targets the Search button; it is disabled when Merchant ID is absent
- `data-testid="problem-details-card"` for ProblemDetailsCard; `data-testid="correlation-id-value"`, `data-testid="required-header-value"`, `data-testid="retryable-value"`, `data-testid="field-errors-list"` for extension fields
- `data-testid="error-lab-trigger-304"`, `data-testid="error-lab-trigger-429"`, `data-testid="error-lab-trigger-idempotency-replay"` for Error Lab triggers
- `data-testid="http-headers-panel"` for HeaderKeyValuePanel
- `data-testid="raw-json-viewer"` for RawJsonViewer — future test can assert text "No body" when 304 response
- `data-testid="tenant-suspended-banner"` for suspended state; `data-testid="tenant-context-badge"` for active tenant
- Payment lifecycle buttons: `data-testid="lifecycle-authorize"`, `data-testid="lifecycle-capture"`, `data-testid="lifecycle-cancel"`, `data-testid="lifecycle-refund"`
- Capture/refund inputs: `getByLabel('Capture amount')`, `getByLabel('Refund amount')` (aria-labels present)
- Merchant detail: `data-testid="merchant-name"`, `data-testid="merchant-reference"`, `data-testid="action-activate-merchant"`, `data-testid="action-suspend-merchant"`, `data-testid="merchant-payment-orders-link"`

---

## 14. Security Review

- Authorization header value never in DOM or debug panels — masked as `Bearer ••••••••` in both `HeaderKeyValuePanel` and `ApiDebugPanel`
- No tenant_id or tenant_reference exposed in UI components
- `trigger-304`, `trigger-idempotency-replay`, `trigger-428` use session-bound access tokens — not exposed to client
- CORS `exposedHeaders` does not include `Authorization`
- Support search passes only allow-listed query params to backend (`clientOrderReference`, `status`, etc.)
- No X-Tenant-ID, X-User-ID, X-Role, X-Permissions headers added

---

## 15. Quality Gates Run

| Gate | Scope |
|------|-------|
| Frontend typecheck | All frontend source |
| Frontend Vitest | 532 tests (46 files) |
| No new Playwright specs/helpers/POM | Verified via find + count |
| No waitForTimeout | Verified via grep |
| Backend compile | Not run — no backend changes |

---

## 16. Quality Gates Results

| Gate | Result |
|------|--------|
| Frontend typecheck | ✅ PASS |
| Frontend Vitest | ✅ 532/532 |
| No new Playwright tests | ✅ Confirmed (9 pre-existing, 0 new) |
| No waitForTimeout | ✅ None found |

---

## 17. Files Changed

| File | Change |
|------|--------|
| `app/components/shared/RawJsonViewer.vue` | Added "No body" empty state |
| `app/components/shared/ApiDebugPanel.vue` | Always render RawJsonViewer with `?? ''`; remove `!= null` guard |
| `app/components/shared/ProblemDetailsCard.vue` | Unified label width `w-20` → `w-28` |
| `app/pages/error-lab.vue` | Improved descriptions for 401, 428, 429, 304, idempotency-replay |
| `app/pages/admin/support/index.vue` | Fixed `canSearch`, added required/hint to Merchant ID field |
| `server/api/error-lab/trigger-304.get.ts` | Added Last-Modified to forwarded headers |

---

## 18. What Was Intentionally Not Built

- No Playwright tests, helpers, POM, fixtures, smoke specs
- No Phase 3 features
- No custom design system
- No CSV export
- No traceparent / Server-Timing headers
- No full support search backend (no cross-merchant payment search endpoint exists)
- No RateLimit-Limit/Remaining/Reset headers (Phase 3)
- No security test fixes (120 pre-existing failures unchanged)
- No new backend endpoints

---

## 19. Remaining Risks

1. **Support search requires merchant UUID**: Users must know the merchant ID to search. No autocomplete/lookup mechanism. Acceptable for current scope.
2. **trigger-304 requires seed data**: The 304 trigger fails gracefully (503 + problem body) if no payment orders exist. This is expected lab behavior.
3. **idempotency-replay module-level state**: `storedIdempotencyKey` is shared in-process. In production-like setups with multiple Nitro workers, concurrent users could interfere. Acceptable for a learning lab.
4. **RawJsonViewer "No body" vs empty JSON**: If a response body is a valid JSON `null` literal, `JSON.parse("null")` succeeds and the value is re-stringified as `"null"`. RawJsonViewer shows `"null"` (truthy). This is technically correct but slightly misleading. Low priority.

---

## 20. Next Recommended Work

Phase 3 candidates (when `ALLOW_PHASE_3 = true`):
1. Playwright test suite using the stable data-testids and aria-labels catalogued in Section 13
2. RateLimit-Limit/Remaining/Reset header support in ApiHeaders and HeaderKeyValuePanel
3. Support search autocomplete for merchant ID (from merchants list)
4. Error Lab additional scenarios: 503 (backend unavailable), 409 (conflict on duplicate clientOrderReference)
5. Payment order history tab on merchant detail page
