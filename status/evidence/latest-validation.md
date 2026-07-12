---
name: latest-validation
last_updated: 2026-07-12
audited_branch: 001-project-foundation
audited_commit: fec8e1da46da18e3d141660c5bc0753de2ddabf2
---

# Latest Validation Evidence

Date: 2026-07-12
Branch: `001-project-foundation`
HEAD: `fec8e1da46da18e3d141660c5bc0753de2ddabf2`
Environment: local (Podman-backed Testcontainers per project convention), Node/pnpm via corepack

This is the one fresh, independently-run validation pass for this audit session. All commands below were run directly against the current worktree with no code changes made before or during the run. Where a result contradicts an older `.codex/**` or `docs/implementation/**` claim, that is called out explicitly — the fresher result here is authoritative for **current** status; the older claim remains valid as a historical record of what was true when it was written.

## Backend

### `cd apps/backend && ./mvnw compile`
Result: **GREEN** (exit 0). Only pre-existing deprecation warnings (`sun.misc.Unsafe` via Guice, unrelated to project code).

### `cd apps/backend && ./mvnw test-compile`
Result: **GREEN** (exit 0).

### `cd apps/backend && ./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify`
Result: **RED** (exit 1) — one test failure, full suite otherwise green.

```
Tests run: 462, Failures: 1, Errors: 0, Skipped: 5
```

Failing test:
```
AuditEventPersistenceTest.migrationAndJpaMappingPersistOnlyExplicitAuditFields
Expected fields: [id, occurredAt, actorSubject, actorDisplay, action, targetType, targetId, tenantId, correlationId, outcome]
Actual fields:   [id, occurredAt, actorSubject, actorDisplay, action, targetType, targetId, tenantId, correlationId, outcome, beforeState, afterState]
```

Classification: **FAILED_REGRESSION**, tracked as `status/technical-debt/current-baseline.md` TD-1. This is a genuine current build failure on the repository's own approved filtered `verify` command (`AGENTS.md`/root `CLAUDE.md` exclusion rules for `restkit/**` and `paymentsupport/**` were honored; no other suite was excluded).

Excluded suites (per `AGENTS.md`/root `CLAUDE.md` standing rule, not run and not counted as evidence either way):
- `apps/backend/src/test/java/lab/paymentquality/restkit/`
- `apps/backend/src/test/java/lab/paymentquality/paymentsupport/`

## Frontend

### `cd apps/frontend && corepack pnpm typecheck`
Result: **GREEN** (exit 0). `nuxt typecheck` clean.

### `cd apps/frontend && corepack pnpm test:unit`
Result: **GREEN** (exit 0).
```
Test Files  46 passed (46)
     Tests  532 passed (532)
```

### `cd apps/frontend && corepack pnpm exec playwright test --list`
Result: **GREEN** (exit 0) — listing only, no execution.
```
Total: 101 tests in 31 files
```

### `cd apps/frontend && corepack pnpm exec playwright test --project=chromium`
Result: **RED** (exit 1).
```
82 tests executed (of 101 listed — the remainder belong to the auth-setup/api-tests projects not selected by --project=chromium)
21 failed
61 passed
```

Full per-file breakdown, root-cause hypotheses confirmed/rejected, and classification: see `status/technical-debt/current-baseline.md` TD-2. Summary of failing spec files: `auth-deny.spec.ts` (2), `foundation.spec.ts` (1), `merchant-create.spec.ts` (6), `merchant-feedback.spec.ts` (2 — pre-existing, already known per `docs/implementation/payment-quality-engineering-lab-phase3-roadmap-execution-report.md`), `merchant-lifecycle.spec.ts` (3), `payment-orders-panel.spec.ts` (3), `payment-status-polling.spec.ts` (2), `rbac/merchant-risk-flag.spec.ts` (2).

## Tests not executed and why

| Suite | Reason |
|---|---|
| `apps/backend/src/test/java/lab/paymentquality/restkit/**` | Excluded per standing repository rule (`AGENTS.md`, root `CLAUDE.md`) unless explicitly requested |
| `apps/backend/src/test/java/lab/paymentquality/paymentsupport/**` | Same standing exclusion rule |
| Playwright `auth-setup` / `api-tests` projects | `--project=chromium` was used per this audit's own validation-command list (section 15); the 19-test gap between `--list` (101) and the executed 82 corresponds to tests belonging to these other projects |
| Playwright with real Keycloak (`PLAYWRIGHT_USE_REAL_KEYCLOAK=true`) | Not requested; default mocked-session mode was used, consistent with root `CLAUDE.md`'s documented default |

## Error classification summary

- 1 backend test failure: **FAILED_REGRESSION** (TD-1) — code-level mismatch between an entity's actual persisted fields and a test's explicit exclusion assertion; root cause identified (an undocumented audit-export feature added `beforeState`/`afterState` columns/fields after the test was written).
- 21 frontend Playwright failures: mostly **stale locator / stale page copy** in a cluster of older spec files (`foundation.spec.ts`, `merchant-create.spec.ts`, `auth-deny.spec.ts`, `merchant-lifecycle.spec.ts`, `payment-orders-panel.spec.ts`) plus 2 **independently confirmed missing-testid** issues (`payment-status-polling.spec.ts`, `rbac/merchant-risk-flag.spec.ts`) plus 1 **known, previously-accepted regression** (`merchant-feedback.spec.ts`). None trace to a PENDING-vs-DRAFT merchant-status mismatch (that specific audit-brief hypothesis is not confirmed by this evidence). Full detail in `status/technical-debt/current-baseline.md`.
- No test failed due to missing infrastructure (Podman/Testcontainers/Keycloak were available) in this run.
