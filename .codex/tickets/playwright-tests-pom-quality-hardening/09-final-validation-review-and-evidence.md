# 09 — Final validation, independent review, and evidence

**What to build:** One auditable closure record proves that all planned hardening work is compiled, discovered, linted, behaviorally verified and independently reviewed without secrets or false PASS claims.

**Blocked by:** 01–08 — all implementation tickets.

**Seams:** TypeScript/lint; Playwright discovery; Playwright REST/BFF; Playwright E2E; review/evidence

**Status:** ready-for-agent
**Category:** enhancement

## Implementation guidance

- Reconcile every acceptance checkbox against fresh command evidence.
- Run static gates, every active config's discovery, targeted overlays and the full main live suite.
- Event Lab runtime evidence uses the Kafka stack. TLS/RLS/Mirror smoke runs use their correct overlays.
- Classify failures as in-scope regression, pre-existing out-of-scope, environment failure or suspected flake; never relabel a red run as green.
- Run final standards/spec/layer reviews and a net-simplification review.
- Write one dated evidence document with branch, commit/working-tree identity, commands, counts, NOT_RUN items and remaining risks. Never include credentials, tokens or storage state.

## Immutable acceptance IDs

- `T09-A01` — Frontend product typecheck and strict POM typecheck are green.
- `T09-A02` — POM-only lint has zero errors, no warning regression, and a documented net reduction.
- `T09-A03` — Full frontend lint is green or its unchanged pre-existing out-of-scope failure is isolated with evidence; POM scope itself is green.
- `T09-A04` — `git diff --check` is green.
- `T09-A05` — Main, visual, TLS, RLS flag-off, RLS Spring-off, Mirror flag-off and learner discovery commands are reconciled; no live spec is orphaned.
- `T09-A06` — Targeted tests for all modified journeys are green.
- `T09-A07` — One full main live POM run is green with passed/failed/flaky/skipped counts.
- `T09-A08` — Event Lab live Kafka tests and required overlay smoke tests are green; missing environment is recorded as `NOT_RUN` and prevents full DONE.
- `T09-A09` — No `page.route`, `route.fulfill`, `waitForTimeout`, `test.only`, `as any`, committed `.auth`, password fallback or mutating polling remains.
- `T09-A10` — Final `code-review`, `playwright-sdet-review`, `rest-api-test-design` and `ponytail-review` have no open P0/P1.
- `T09-A11` — Every finding F-01–F-14 is marked VERIFIED, NOT_APPLICABLE with evidence, or explicitly still open; only VERIFIED/valid NOT_APPLICABLE permits completion.
- `T09-A12` — The fixed evidence report contains dated checkpoints, no secrets, and links the spec, tickets and exact commands.

## Validation and verification

Apply the goal's complete validation ladder and completion audit. Stop a wave on the first in-scope red result; focused red-green repair precedes resuming broader verification.

## Comments

### 2026-08-26 — TLS ETag closure

- `T09-A08` / `T09-A11` lifecycle blocker verified: Caddy no longer transforms a versioned REST ETag. Fresh `chromium-tls-manager` is **5/5**, including authorize → capture.
- The browser test proves the edge contract (`ETag: "vN"`, `Cache-Control: no-transform`, no content coding) and that each mutation forwards the exact prior ETag as `If-Match`.
- Focused backend gates are green: payment conditional 1/1, merchant 3/3, tenant settings 7/7, support REST 12/12. POM typecheck and whitespace check are green.
- Scoped standards/spec/Spring/REST Assured/Playwright/POM/ponytail review found no P0/P1. Decision and residual scope are recorded in ADR 0003 and the dated evidence report.
