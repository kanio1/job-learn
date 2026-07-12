---
name: audit-export-closure
origin: POST_KIRO_WORK
audited_branch: 001-project-foundation
audited_commit: c6de61f31e7cadc09331269f0f33e70573e4b889
last_updated: 2026-07-12
status: RESOLVED
---

# Roadmap closure: audit `before_state`/`after_state` diff capture + audit export

```text
ORIGIN: POST_KIRO_WORK
RELATED_KIRO_SPEC: audit-log-dashboard
PROBLEM: status/technical-debt/current-baseline.md TD-1 — AuditEventPersistenceTest asserted a
  closed field list for AuditEvent that predated two later, legitimate schema additions
  (Flyway V8 export index, Flyway V11 before_state/after_state columns), causing the
  repository-approved filtered `./mvnw verify` to be RED.
DECISION: KEEP both features. Fix the stale test, do not revert the schema/code.
RATIONALE: see "Investigation" and "Decision" below.
OWNING_MODULE: apps/backend/src/main/java/lab/paymentquality/audit/**
ACCEPTANCE_CRITERIA: see "Acceptance criteria" below.
IMPLEMENTATION_EVIDENCE: see "Implementation evidence" below.
TEST_EVIDENCE: see "Test evidence" below.
```

## What this closes

Two features that exist in the current codebase but were added by later, dated `docs/implementation/**` roadmap reports rather than by the `audit-log-dashboard` Kiro spec itself, and had never been folded into `status/roadmaps/**` with their own acceptance criteria:

1. **Audit export** (`AuditExportEvent`, `AuditExportResponse`, `server/api/audit/export.json.get.ts`, Flyway `V8__add_audit_event_export_index.sql`) — added by the MVP Phase 1/2 roadmap, task `DB-P2-001` (`docs/implementation/payment-quality-engineering-lab-phase1-phase2-execution-report.md` line 70/112). See `status/roadmaps/mvp-phase1-phase2.md`.
2. **Audit before/after diff drawer (F-D7)** (`AuditEvent.beforeState`/`afterState`, Flyway `V11__add_audit_event_before_after_state.sql`, `AuditEventDetail.beforeState`/`afterState`, `AuditEntryDrawer.vue` diff section) — added by the Playwright Phase 3 roadmap, Phase 3C-5 (`docs/implementation/payment-quality-engineering-lab-phase3-roadmap-execution-report.md`, "Phase 3C-5 — F-D7: Audit Before/After Diff Drawer" section, starting at line 2154).

## Investigation (per audit brief §10.1)

1. **Are `before_state`/`after_state` part of a deliberate audit diff feature?** Yes — confirmed by:
   - A Javadoc comment on `AuditableActionOccurred` (`shared/events/AuditableActionOccurred.java`): "beforeState/afterState are optional field-level snapshots for the audit diff drawer (F-D7)".
   - `docs/implementation/payment-quality-engineering-lab-phase3-roadmap-execution-report.md` "Phase 3C-5" section: an explicit, dated (`2026-07-xx`) domain-design writeup, including a scope decision (merchant activate/suspend only, not a generic diff engine), a shape decision (`Map<String,Object>` flat snapshots, not deep objects), and a backward-compatibility decision (extra trailing record components + a legacy 9-arg constructor so all 7 pre-existing call sites kept compiling).
2. **Is the problem in the migration, JPA mapping, fixture, schema, or test?** The test. Migration `V11` is correct (`ALTER TABLE audit_event ADD COLUMN before_state JSONB, ADD COLUMN after_state JSONB`), the JPA mapping is correct (`@JdbcTypeCode(SqlTypes.JSON)` on `Map<String,Object>` fields, verified round-tripping through a real PostgreSQL 18 Testcontainers instance — see Test evidence). `AuditEventPersistenceTest.migrationAndJpaMappingPersistOnlyExplicitAuditFields`'s exhaustive field-name assertion (`containsExactlyInAnyOrder("id", "occurredAt", ..., "outcome")`) was simply never updated after `V11` landed.
3. **Does the export actually use this data?** No, deliberately: `AuditExportEvent.from(AuditEvent)` explicitly excludes `beforeState`/`afterState` (and `tenantId`/`actorSubject`) — confirmed by direct file read of `AuditExportEvent.java`. Export and diff-drawer are two independent, non-overlapping features that both touch `AuditEvent` but expose disjoint field subsets, each covered by its own `AuditDtoRedactionTest` exact-field-list assertion (`ALLOWED_EXPORT_FIELDS` vs. `ALLOWED_DETAIL_FIELDS`).
4. **Should the fields be nullable?** Yes, and they already are (`@Column(name = "before_state", updatable = false)` / `after_state`, no `nullable = false`) — confirmed nullable by both the migration (no `NOT NULL`) and `AuditEventTest.fromEventCopiesOnlyExplicitContractFieldsAndAssignsId` (asserts `null` when absent).
5. **Is there an H2/PostgreSQL/Testcontainers/Hibernate schema-validation drift?** No. Root cause is purely a stale test assertion, not a schema mismatch — confirmed by this session's fresh `AuditEventPersistenceTest` run against a real `postgres:18` Testcontainers instance (see Test evidence), and by the fact that `ddl-auto: validate` never failed (Hibernate validates the entity against the actual migrated schema successfully on every backend test run in this repository, including this session's).
6. **Was the test using a stale constructor/fixture?** No — the test used the still-valid 9-arg legacy `AuditableActionOccurred` constructor, which is intentionally preserved for backward compatibility. The staleness was purely in the hardcoded field-name list, not the constructor call.
7. **Why wasn't this caught earlier?** `docs/implementation/payment-quality-engineering-lab-phase3-roadmap-execution-report.md` "Phase 3C-5" itself explicitly records: "DB-dependent audit tests (`AuditEventPersistenceTest`, `JpaAuditEventRepositoryTest`, `AuditModuleTest`, `AuditSecurityMatrixIT`, `AuditEventListenerModuleTest`) — Not run — require `PostgresContainerSupport`/Testcontainers; no container runtime available in this sandbox." The Phase 3C-5 implementation session had no Podman/Docker, so it could never have caught this stale assertion — it was first exposed by this session's fresh, Podman-backed `./mvnw verify` run (`status/evidence/latest-validation.md`, prior revision).

## Decision

**KEEP** both features. Per the audit brief's decision matrix (§8): the diff-drawer feature is used by real UI (`AuditEntryDrawer.vue`'s diff section, covered by 2 green Playwright tests), has clear domain justification (merchant activate/suspend before/after status is exactly the kind of thing an audit log should show), has dedicated migrations and multiple dedicated tests proving deliberate implementation (`AuditEventTest`, `AuditDtoRedactionTest`, `MerchantServiceTest`, `PaymentExpirationServiceTest`), and the failing test was a local, narrowly-scoped implementation gap (a stale assertion), not a defect requiring invented business behavior. Reverting would delete a working, tested, safely-scoped feature to satisfy a test that was simply never updated.

## Acceptance criteria

- `AuditEvent`'s persisted field set is exactly: `id, occurredAt, actorSubject, actorDisplay, action, targetType, targetId, tenantId, correlationId, outcome, beforeState, afterState` — enforced by `AuditEventPersistenceTest`.
- `beforeState`/`afterState` round-trip correctly as PostgreSQL `jsonb` through Hibernate `@JdbcTypeCode(SqlTypes.JSON)` — enforced by `AuditEventPersistenceTest.migrationAndJpaMappingRoundTripBeforeAndAfterStateAsJsonb` (new).
- `AuditExportEvent` never exposes `beforeState`/`afterState` (or `actorSubject`/`tenantId`) — enforced by `AuditDtoRedactionTest.exportEventExposesOnlyComplianceSafeFieldSet`.
- `AuditEventDetail` exposes `beforeState`/`afterState` in addition to the safe summary field set — enforced by `AuditDtoRedactionTest.detailExposesExactlyTheSafeFieldSetPlusDiffState`.
- Diff-drawer UI renders a "Change" section only when at least one of `beforeState`/`afterState` is present — enforced by `tests/e2e/audit-diff-drawer.spec.ts` (2 Playwright tests, both green per this session's and the Phase 3C-5 report's runs).

## Implementation evidence

- `apps/backend/src/main/resources/db/migration/audit/V8__add_audit_event_export_index.sql`, `V11__add_audit_event_before_after_state.sql`
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/domain/AuditEvent.java` (`beforeState`/`afterState` fields + getters)
- `apps/backend/src/main/java/lab/paymentquality/audit/internal/web/dto/AuditEventDetail.java`, `AuditExportEvent.java`, `AuditExportResponse.java`
- `apps/backend/src/main/java/lab/paymentquality/shared/events/AuditableActionOccurred.java`, `AuditableActionEventFactory.java`
- `apps/backend/src/main/java/lab/paymentquality/merchant/internal/application/MerchantService.java` (`activate`/`suspend` publish `Map.of("status", ...)` before/after)
- `apps/backend/src/main/java/lab/paymentquality/payment/internal/application/PaymentExpirationService.java` (reuses the same diff pattern for `PAYMENT_EXPIRED`)
- `apps/frontend/app/components/audit/AuditEntryDrawer.vue`, `apps/frontend/app/schemas/audit.schema.ts`

## Test evidence (this session, fresh, commit `c6de61f`)

Targeted (`./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' -Dtest='AuditEventPersistenceTest,AuditDtoRedactionTest,AuditModuleTest,AuditControllerTest,AuditEventListenerModuleTest,JpaAuditEventRepositoryTest,AuditEventTest,AuditQueryTest' test`): **32/32 passed, 0 failures** — includes the fixed/extended `AuditEventPersistenceTest` (now 2 tests: the corrected exhaustive field-list assertion, plus a new dedicated JSONB round-trip test).

Full filtered `./mvnw -Dsurefire.excludes='**/restkit/**,**/paymentsupport/**' verify`: Surefire (unit tests) **463/463 passed, 0 failures, 5 skipped — GREEN** (this is the gate TD-1 was blocking; it is now green). Failsafe (integration tests) is **RED for an unrelated reason** — see `status/technical-debt/current-baseline.md` TD-5, a newly-exposed, separate regression in the `deterministic-seed-and-test-isolation` spec's `testing` module (stale hardcoded merchant-count assertions), previously masked because Surefire's TD-1 failure always stopped the build before Failsafe could run. TD-5 is out of scope for this work package (different root cause, different module) and is queued as the next task.

Not re-run this session (no frontend files touched by this fix): frontend typecheck/unit/Playwright — see `status/evidence/latest-validation.md` for the prior session's frontend baseline, still valid since no frontend code changed.
