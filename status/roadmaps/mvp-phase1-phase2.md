---
name: mvp-phase1-phase2
origin: POST_KIRO_WORK
audited_branch: 001-project-foundation
audited_commit: fec8e1da46da18e3d141660c5bc0753de2ddabf2
last_updated: 2026-07-12
---

# Roadmap: MVP Phase 1 + Phase 2 (HTTP contract hardening)

```text
ORIGIN: POST_KIRO_WORK
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-implementation-plan.md
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-phase1-phase2-execution-report.md
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-phase1-phase2-verification-report.md
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-frontend-rest-readiness-audit.md
RELATED_KIRO_TASKS: none directly — this roadmap is a separate, later work program that runs across/after the seven `.kiro/specs/**` specs; it hardens HTTP contract details (conditional GET, idempotency replay, Retry-After/WWW-Authenticate/Last-Modified headers) that the Kiro specs did not fully enumerate.
ACCEPTANCE_CRITERIA: see docs/implementation/payment-quality-engineering-lab-implementation-plan.md §6-8 (MVP / Phase 2 / Phase 3 scope tables) — not restated here.
```

## What this is

A 30-task (21 MVP + 9 Phase 2) implementation pass, dated 2026-06-29 on branch `001-project-foundation`, driven by `docs/implementation/payment-quality-engineering-lab-implementation-plan.md` rather than by any `.kiro/specs/**` document. It hardens the payment-order HTTP contract (conditional GET via `If-None-Match`/304, `Idempotency-Replayed` header semantics, `Retry-After`/`WWW-Authenticate`/`Last-Modified` headers end-to-end from backend → Nuxt BFF → frontend Error Lab) and adds small frontend surfaces (merchant detail page, tenant-context suspended banner, support search page).

## Status: COMPLETE, then independently re-verified with 2 regressions found and fixed

### Phase 1 MVP (21 tasks) — Batch A (DB + backend), Batch B (Nuxt BFF), Batch C (frontend), Batch D (tests)
All 21 tasks reported ✅ Done in the execution report (2026-06-29): conditional GET/304, idempotency replay, 428 `requiredHeader`, CORS header allowlist extension, BFF header forwarding, three new Error Lab trigger routes (429/304/idempotency-replay), `ProblemDetails`/`ApiHeaders` type extensions, merchant detail page, `TenantContextBadge`, lifecycle capture/refund amount inputs, a 4th seed merchant (`MERCHANT_SUSPENDED_DEMO`).

### Phase 2 (9 tasks)
Playwright upgraded 1.60.0→1.61.0 with `waitForTimeout` removed; `Last-Modified` header (RFC 1123) added to GET/HEAD payment order; `retryable`/`retryAfterSeconds` problem-details extensions; `/admin/support` search page; Flyway `V8__add_audit_event_export_index.sql` (composite index on `audit_events(tenant_id, occurred_at, id)` for cursor-based export — **note:** this migration pre-dates/parallels the `audit-log-dashboard` Kiro spec's own Flyway versions V6/V7; see `status/technical-debt/current-baseline.md` TD-1 for the related export-feature scope-drift finding).

### Verification pass (same date) — 2 regressions found and fixed
- **REG-001**: an earlier agent had dropped `V0.2__suspend_placeholder_tenant.sql` (masking the removal as "Fixtures.java approach only") because `platformOperatorToken()` used the now-suspended `PLACEHOLDER_TENANT_ID`, breaking security tests. Fixed by restoring V0.2 and repointing `TestJwtSupport.platformOperatorToken()` / `MerchantSecurityTest` fixtures to the still-active `TENANT_ALPHA` tenant.
- **REG-002**: `PaymentOrderHttpContractMvpTest` used the wrong authority tokens for 3 of 5 new tests (read tests used a create-only token; the authorize test used an operate-only token instead of lifecycle). Fixed by switching to `merchantPaymentReaderToken` / `merchantPaymentLifecycleToken`.
- Quality gates after fix: backend compile clean, `FixturesTest` 25/25, `ModulithArchitectureTest` 1/1, `MerchantSecurityTest` 4/4, `PaymentOrderSecurityTest` 11/11, `PaymentOrderHttpContractMvpTest` 5/5, frontend Vitest 532/532 (46 files), frontend typecheck clean.

### Independent readiness audit (2026-06-29, read-only mode)
`docs/implementation/payment-quality-engineering-lab-frontend-rest-readiness-audit.md` re-confirmed all Phase 1/2/Hardening/Polish claims directly against code (not against the narrative reports) and found no critical missing items; declared the system "ready for Playwright test suite design." Quality gates were NOT re-run in that audit (explicitly read-only); it relied on static grep/inspection only.

## Relationship to this session's fresh validation

This session's fresh `./mvnw verify` (see `status/evidence/latest-validation.md`) still shows `FixturesTest`-adjacent counts consistent with the 25-test figure above, but surfaces one NEW failure unrelated to this roadmap (`AuditEventPersistenceTest` — see `status/technical-debt/current-baseline.md` TD-1, which is downstream of the `audit-log-dashboard` Kiro spec plus the later, undocumented audit-export scope-drift work, not of this MVP/Phase-2 roadmap).

## Next work

No outstanding tasks in this roadmap file are unresolved — both regressions were fixed and re-verified on the same date. Follow-on work continued in `system-hardening-and-frontend-polish.md` and `playwright-phase3-roadmap.md`.
