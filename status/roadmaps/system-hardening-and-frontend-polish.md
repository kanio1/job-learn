---
name: system-hardening-and-frontend-polish
origin: POST_KIRO_WORK
audited_branch: 001-project-foundation
audited_commit: 95e35c97d74608bdc3d7925a4f6bb0b46c99a79b
last_updated: 2026-07-13
---

# Roadmap: System Hardening (Phase 2.5) + Frontend Polish

```text
ORIGIN: POST_KIRO_WORK
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-system-hardening-report.md
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-frontend-polish-report.md
RELATED_KIRO_TASKS: none — two small, code-review-driven cleanup passes over the `mvp-phase1-phase2` roadmap's own output, not over any `.kiro/specs/**` task.
ACCEPTANCE_CRITERIA: none formally defined; both reports are informal review passes that fixed identified UI/UX rough edges with no new features.
```

## System Hardening (Phase 2.5) — 6 targeted fixes, no new features

Reviewed Error Lab trigger routes, `ApiDebugPanel`, `RawJsonViewer`, `HeaderKeyValuePanel`, `ProblemDetailsCard`, `TenantContextBadge`, `EtagDisplay`, merchant detail page, support search, lifecycle actions, `backendApi.ts`, `useApiClient.ts`. Fixes:
1. Error Lab 304 scenario: empty `<pre>` for no-body responses; `Last-Modified` not forwarded — fixed in `trigger-304.get.ts`.
2. `RawJsonViewer.vue`: blank `<pre>` on empty content — now shows an explicit "No body" state.
3. `ApiDebugPanel.vue`: `v-if="response.body != null"` let an empty string silently pass through and hide the body panel — changed to always render `RawJsonViewer` with `response.body ?? ''`.
4. `ProblemDetailsCard.vue`: mixed `w-20`/`w-28` label widths caused misaligned values — unified.
5. `/admin/support` search: `canSearch` allowed searching with no Merchant ID — fixed.
6. Error Lab scenario descriptions (429/idempotency-replay/428/401) improved for clarity (not correctness bugs).

No backend changes in this pass — backend was already correct per the Phase 1/2 verification report.

## Frontend Polish — 5 targeted improvements, no new features

1. `PaymentOrderDetail.vue` `displayHeaders`: was missing `Last-Modified` and `Idempotency-Replayed` in the HTTP debug tab, even though both headers existed on the wire since Phase 1/2 — fixed.
2. `admin/merchants/[merchantId].vue`: raw ISO date strings shown for `createdAt`/`updatedAt` — changed to `toLocaleString()`, matching `MerchantTable.vue`/`PaymentOrderDetail.vue` conventions.
3. `admin/support/index.vue` results table: no navigation from search results, `merchantId` missing from schema, field-order issues — rewritten.
4. `IfMatchInput.vue`: no hint text explaining its purpose — added.
5. `PaymentOrderLifecycleActions.vue`: capture/refund amount placeholder didn't mention "minor units" — fixed.

## Assurance Closure Wave 2A verification (2026-07-13)

All eleven roadmap items now have focused acceptance-level proof under stable IDs `QA-HARDEN-01.01` through `QA-HARDEN-01.11`, grouped into five packages:

- `01A`: Error Lab 304 status/headers/no-body, RawJsonViewer empty state, ApiDebugPanel body-section retention.
- `01B`: ProblemDetails label alignment and five Error Lab learning descriptions.
- `01C`: Support Search Merchant-ID gating, exact request, typed result/status/date/link and state distinctions.
- `01D`: Payment detail `Last-Modified`/`Idempotency-Replayed` presentation and deterministic merchant date formatting.
- `01E`: If-Match label/hint relationship and capture/refund minor-unit guidance.

Focused tests are `qa-harden-empty-body.test.ts`, `qa-harden-error-presentation.test.ts`, `admin/support/index.test.ts`, `qa-harden-detail-presentation.test.ts`, and `qa-harden-lifecycle-guidance.test.ts`. Ten implementations passed without production change. The alignment test found one confirmed residual defect: the `Field Errors` `dt` alone lacked `w-28`; a one-class correction in `ProblemDetailsCard.vue` closed it. Two red test-construction attempts were classified and corrected as `TEST_DESIGN_DEFECT` (Nitro auto-import mocking and Pinia initialization order), without changing correct production behavior.

Final evidence: frontend typecheck GREEN; unit suite 58 files / 594 tests GREEN; standard Chromium 82/82 GREEN. Live Playwright was not run because no Wave-1 live assurance surface changed. See `status/evidence/latest-validation.md`.

## Status

`QA-HARDEN-01: DONE_VERIFIED` — 11/11 exact requirements have focused evidence and the relevant regression gates are green.

## Next work

None outstanding — these were closed, self-contained cleanup passes. Any new UI rough edges found in future work should get their own dated report rather than reopening these two.
