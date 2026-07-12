---
name: playwright-phase3-roadmap
origin: POST_KIRO_WORK
audited_branch: 001-project-foundation
audited_commit: fec8e1da46da18e3d141660c5bc0753de2ddabf2
last_updated: 2026-07-12
---

# Roadmap: Playwright Phase 3A / 3B / 3C (SDET learning expansion)

```text
ORIGIN: POST_KIRO_WORK
SOURCE_DOCUMENT: docs/implementation/payment-quality-engineering-lab-phase3-roadmap-execution-report.md
SOURCE_DOCUMENT: docs/analysis/payment-quality-engineering-lab-missing-features-playwright-learning-roadmap.md
SOURCE_DOCUMENT: docs/analysis/playwright-161-http-api-properties-test-strategy.md
RELATED_KIRO_TASKS: none directly — this is a dedicated Playwright/SDET test-authoring roadmap (feature IDs F-A1..F-D7) layered on top of whatever the `.kiro` specs already shipped; it does not implement new product features, only new test capabilities against existing surfaces (Error Lab, merchant/payment lifecycle, audit).
ACCEPTANCE_CRITERIA: see docs/analysis/payment-quality-engineering-lab-missing-features-playwright-learning-roadmap.md §5 (Playwright Capability Coverage Matrix) and §11 (Missing Feature Design Table).
```

## What this is

A staged Playwright test-suite expansion (dated through 2026-07-12 on `001-project-foundation`) driven by a dedicated gap-analysis roadmap document, not by `.kiro/specs/**`. It is organized into named "Feature IDs" (F-A1 API foundation, F-A3 network assertions, F-B4 DOM modal lifecycle, F-C5 sequential route-mock retry, F-D1..F-D7 "expert capability" UI features: ARIA snapshots, visual regression, command palette, PSP redirect simulator, audit diff drawer, payment expiration).

## Status: Phase 3A, 3B, and 3C all reported COMPLETE

- **Phase 3A-1** (API-only foundation): 13 new `APIRequestContext` tests in a new `api-tests` Playwright project (no browser/Keycloak/backend needed) against the standalone `trigger-429` BFF mock endpoint, plus a reusable `tests/api/helpers/assert-api.ts` assertion module (correlation-id, problem+json, no-cache-store, RFC 9457 structure, no-auth-leak).
- **Phase 3A-2** (UI network foundation): 5 new chromium tests across 2 new spec files (F-A3 `page.waitForResponse`/`waitForRequest`, F-D6 console/storage token guard, F-B4 modal DOM lifecycle).
- **Phase 3B-8** (F-C5 sequential retry demo): closes a gap left by earlier 3B-1..3B-7 sub-phases (not detailed in the excerpt read this session — read the full report for those if resuming this roadmap); adds a 503→200 stateful `route.fulfill()` retry-idempotency test. A **pre-existing baseline regression was found and explicitly left unfixed (out of scope)** in `merchant-feedback.spec.ts` — see `status/technical-debt/current-baseline.md`.
- **Phase 3B-Closure-Audit**: of 4 flagged capability gaps, `page.waitForRequest()` was closed; conditional-GET-304, idempotency-replay, and header-only-HEAD assertions remain blocked because the Playwright run in this environment has no backend/Keycloak available — bundled into one follow-up infra phase rather than re-attempted piecemeal.
- **Phase 3C** (3C-Prep through 3C-6): all 6 sub-phases (F-D4 ARIA snapshots, F-D5 visual regression, F-D3 command palette, F-D2 PSP redirect simulator, F-D7 audit diff drawer, F-D1 payment expiration) reported complete and green.
- **Cumulative total per the report**: 101 Playwright tests across 31 spec files, plus 58 backend unit tests added/touched across the F-C5/F-D7/F-D1 phases.

## This session's fresh validation vs. the roadmap's own claims

This session ran `corepack pnpm exec playwright test --project=chromium` fresh (see `status/evidence/latest-validation.md`). Result: **82 passed, 21 failed** (of 101 total listed) — NOT the "all green" state the execution report describes as of 2026-07-12. Failing spec files include `auth-deny.spec.ts`, `foundation.spec.ts`, `merchant-create.spec.ts`, `merchant-feedback.spec.ts` (the pre-existing regression the roadmap itself already flagged as known/unfixed), `merchant-lifecycle.spec.ts`, `payment-orders-panel.spec.ts`, `payment-status-polling.spec.ts`, and `rbac/merchant-risk-flag.spec.ts`. This directly confirms several of the hypotheses in the audit brief (see `status/technical-debt/current-baseline.md` for the itemized, evidence-backed list) — this is a **material regression** relative to the roadmap document's own "all quality gates green" claim, not a pre-existing/accepted gap.

## Next work

1. Investigate the 21 fresh Playwright chromium failures (see `status/technical-debt/current-baseline.md`) — this is the most urgent open item touching this roadmap.
2. The already-known, already-accepted `merchant-feedback.spec.ts` baseline regression should be folded into the same investigation rather than treated as a second, separate issue.
3. The blocked conditional-GET-304 / idempotency-replay / header-only-HEAD Playwright coverage remains genuinely blocked on backend/Keycloak availability in this environment, not a code defect — re-attempt only once a live backend+Keycloak Playwright run is possible (`PLAYWRIGHT_USE_REAL_KEYCLOAK=true` per root `CLAUDE.md`).
