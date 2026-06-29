# Payment Quality Engineering Lab — Frontend Polish Report

## 1. Executive Summary

Five targeted frontend improvements applied in this pass. No backend, DB, Playwright tests, helpers, or POM were created or modified. All quality gates green.

---

## 2. Source Inputs

1. `docs/implementation/payment-quality-engineering-lab-system-hardening-report.md` (primary)
2. `docs/implementation/payment-quality-engineering-lab-phase1-phase2-verification-report.md`
3. Current frontend code (highest priority)

---

## 3. Repository Discovery

Inspected:
- `PaymentOrderDetail.vue` (tabs: Business, HTTP, Raw, History)
- `PaymentOrderLifecycleActions.vue` (inline capture/refund inputs)
- `[paymentOrderId].vue` (lifecycle drawer with `UFormField` amount input)
- `admin/merchants/[merchantId].vue` (merchant detail page)
- `admin/support/index.vue` (support search page)
- `IfMatchInput.vue` / `IdempotencyKeyInput.vue`
- `MerchantTable.vue` (merchant list with UButton link to detail)
- `dashboard.vue` layout (sidebar with TenantContextBadge)
- `error-lab.vue` (already improved in hardening pass)
- `ProblemDetailsCard.vue`, `HeaderKeyValuePanel.vue`, `ApiDebugPanel.vue`, `RawJsonViewer.vue` (all hardened in previous pass)

---

## 4. Frontend Surfaces Reviewed

| Surface | Issue Found | Action |
|---------|------------|--------|
| `PaymentOrderDetail.vue` `displayHeaders` | Missing `Last-Modified` and `Idempotency-Replayed` | Fixed |
| `admin/merchants/[merchantId].vue` dates | Raw ISO strings shown | Fixed |
| `admin/support/index.vue` results table | No navigation; `merchantId` not in schema; `UEmpty` usage; Merchant ID field order | Rewritten |
| `IfMatchInput.vue` | No hint explaining purpose | Fixed |
| `PaymentOrderLifecycleActions.vue` inline inputs | Placeholder "Amount (empty = full)" misses "minor units" context | Fixed |
| Error Lab descriptions | Already improved in hardening pass | No change |
| `RawJsonViewer`, `ApiDebugPanel`, `ProblemDetailsCard` | Already corrected in hardening pass | No change |
| `HeaderKeyValuePanel` | Token masking correct | No change |
| `TenantContextBadge` | Suspended banner correct | No change |
| `MerchantTable` | UButton link to merchant detail correct | No change |
| Lifecycle drawer amount input | `UFormField` label "Amount (minor units)" present | No change |

---

## 5. Error Lab Improvements

Already completed in System Hardening pass. No additional changes needed.

---

## 6. Problem Details Improvements

Already completed in Phase 1/2 and System Hardening pass. `w-28` field widths unified. No additional changes.

---

## 7. Headers and Debug Panels Improvements

**`PaymentOrderDetail.vue` — `displayHeaders`:**
Added `Last-Modified` and `Idempotency-Replayed` to the HTTP tab's `displayHeaders` computed. These headers were introduced in Phase 1/2 (BE-MVP-002, BE-P2-001) but not surfaced in the payment order HTTP debug tab.

---

## 8. Merchant Detail Improvements

**`admin/merchants/[merchantId].vue` — dates:**
Changed `{{ merchant.createdAt }}` and `{{ merchant.updatedAt }}` from raw ISO strings to `{{ new Date(merchant.createdAt).toLocaleString() }}`. Consistent with how `MerchantTable.vue` and `PaymentOrderDetail.vue` display dates.

---

## 9. Merchant List Navigation Improvements

`MerchantTable.vue` already has a UButton link (variant: link) on the Display Name column pointing to `/admin/merchants/{merchantId}`. No changes needed.

---

## 10. Tenant Context UX Improvements

`TenantContextBadge.vue` and the suspended banner are already correct. No changes needed.

---

## 11. Support Search Improvements

**`admin/support/index.vue` — full rewrite (content-preserving):**

Changes:
1. **Field order**: Merchant ID field moved before Client Order Reference — clearer that Merchant ID is the primary required filter
2. **`merchantId` added to Zod schema**: Was relying on `.passthrough()` for `merchantId`; now explicitly typed for TypeScript safety
3. **`SupportPaymentOrder` type**: Derived from schema instead of `any[]`
4. **Status column**: Now renders `BusinessStatusBadge` (colored + labeled) instead of raw string
5. **Created column**: Formats date with `toLocaleString()` instead of raw ISO string
6. **View column**: Added navigation `UButton` (variant: ghost, icon: external-link) linking to `/admin/merchants/{merchantId}/payments/{paymentOrderId}` — operators can open the full payment order detail from search results
7. **Empty state**: Replaced `UEmpty` (potential availability issue) with inline `UIcon + p` empty state consistent with Nuxt UI patterns
8. **Guard removed**: Removed redundant `if (!mid)` guard in `handleSearch` (now unreachable since `canSearch` requires Merchant ID)

---

## 12. Payment Lifecycle UI Improvements

**`IfMatchInput.vue` — hint text:**
Added `hint="The ETag from the last GET response — required for lifecycle actions"`. Teaches operators and learners why this field is required and where the value comes from. Placeholder updated from "ETag value for conditional update" to `e.g. "v3"`.

**`PaymentOrderLifecycleActions.vue` — inline amount inputs:**
Updated placeholders from "Amount (empty = full)" to "Minor units (empty = full)" and `aria-label` from "Capture amount" to "Capture amount in minor units" (same for refund). Consistent with the drawer's `UFormField` label "Amount (minor units)".

---

## 13. Accessibility Improvements

- `IfMatchInput.vue` hint provides context without relying on color alone
- Support search "View" button has `aria-label="View payment order {clientOrderReference}"` — distinguishable when multiple rows are present
- Status column in support search now uses `BusinessStatusBadge` which uses both color and text label — not color-only
- Empty state uses text + icon, not icon-only

---

## 14. Future Testability Notes Without Test Implementation

- `getByLabel('Merchant ID (required)')` targets the required search input; `getByLabel('Client Order Reference')` targets the optional filter
- `getByRole('button', { name: 'Search' })` — disabled until Merchant ID is provided
- `getByRole('button', { name: /View payment order/ })` — links from search results to detail; each has a unique aria-label from clientOrderReference
- `getByLabel('If-Match')` targets the If-Match input; the hint text is accessible via `aria-describedby`
- `getByLabel('Capture amount in minor units')` / `getByLabel('Refund amount in minor units')` — updated aria-labels
- `PaymentOrderDetail` HTTP tab: future test can assert `Last-Modified` and `Idempotency-Replayed` appear in `data-testid="http-headers-panel"` after a POST lifecycle action
- `data-testid="support-search-merchant-id"`, `data-testid="support-search-client-ref"`, `data-testid="support-search-button"` remain stable

---

## 15. Security and Token Leakage Review

- `Authorization` header masking unchanged — still handled by `HeaderKeyValuePanel` and `ApiDebugPanel`
- Support search "View" link uses `merchantId` from the backend response (UUID, not internal tenant/user id) — acceptable
- `IfMatchInput` hint text references ETag — not a secret, ETag is a public cache identifier
- No tokens, secrets, or internal IDs added to DOM

---

## 16. Files Changed

| File | Change |
|------|--------|
| `app/components/payment/PaymentOrderDetail.vue` | Added `Last-Modified` and `Idempotency-Replayed` to `displayHeaders` |
| `app/pages/admin/merchants/[merchantId].vue` | Format `createdAt`/`updatedAt` with `toLocaleString()` |
| `app/pages/admin/support/index.vue` | Rewrote: `merchantId` in schema, typed rows, status badge, date format, View link, empty state |
| `app/components/shared/IfMatchInput.vue` | Added `hint`, updated `placeholder` |
| `app/components/shared/PaymentOrderLifecycleActions.vue` | Updated inline amount input placeholders and aria-labels |

---

## 17. Quality Gates Run

| Gate | Command |
|------|---------|
| Frontend typecheck | `pnpm typecheck` |
| Frontend Vitest | `pnpm test:unit` |
| waitForTimeout grep | `grep -r "waitForTimeout" apps/frontend/app/` |
| Playwright spec count | `find apps/frontend/tests -name "*.spec.ts"` |
| Backend/DB touched? | `git diff --name-only` |

---

## 18. Quality Gates Results

| Gate | Result |
|------|--------|
| Frontend typecheck | ✅ PASS |
| Frontend Vitest | ✅ 532/532 |
| No `waitForTimeout` | ✅ None found |
| Playwright specs unchanged | ✅ 9 (same as before) |
| Backend/DB touched in this pass | ✅ No (pre-existing diff from Phase 1/2) |

---

## 19. What Was Intentionally Not Built

- No Playwright tests, helpers, POM, fixtures
- No backend changes
- No Nuxt BFF changes
- No DB/Flyway changes
- No Phase 3 features
- No custom design system
- No CSV/export UI
- No full cross-merchant payment search (backend doesn't support it)
- No RateLimit-* header display (Phase 3)
- No PSP integration UI

---

## 20. Remaining Risks

1. **`toLocaleString()` is locale-dependent**: Dates will render differently per browser locale. For a learning/backoffice tool this is acceptable. A future improvement could add a utility formatter with a fixed locale.
2. **Support search "View" link requires `merchantId` in response**: If the backend ever omits `merchantId` from the list response body, the View link won't render (gracefully returns `null`). This is safe — no crash.
3. **Inline lifecycle amount inputs**: The UX has two places to enter capture/refund amount — the inline inputs (which pre-fill the drawer) and the drawer itself. This double-entry pattern could confuse learners. Removing the inline inputs from `PaymentOrderLifecycleActions` and relying solely on the drawer would simplify the UX, but that is a more significant refactor.

---

## 21. Next Recommended Work

1. **Playwright test suite** — all stable `data-testid`, `aria-label`, and accessible names are documented in Section 14; ready to implement tests
2. **Date formatting utility** — extract a `formatDate(iso: string): string` util to keep locale consistent and testable
3. **Merchant detail — payment orders summary card** — could show count/status breakdown inline
4. **Error Lab additional scenarios** — 503 (backend unavailable), 409 (clientOrderReference conflict)
5. **Phase 3 features** — RateLimit-* headers, CSV export, multi-merchant support search
