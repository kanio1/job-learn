# Status Hygiene Audit

Date: 2026-06-18
Branch: `018-rest-security-p1-error-auth-method-hardening`
Mode: review-only (no `.kiro/**`, no production code, no tests modified)

## Executive Summary

- User-management final status: `COMPLETE_WITH_OPTIONAL_GAPS` — documented in both `.codex/current-state.md` and `.codex/user-management.md` (Wave 11 sections present).
- `.kiro` mutation status: **NOT MODIFIED**. `git status --short -- .kiro` is empty. All 7 specs' task boxes remain in their pre-existing state (intentionally read-only per the Codex/OpenCode workflow).
- Untracked/unexpected files: 1 untracked file (`PaymentOrderSecurityContractRestKitTest.java` in the excluded `restkit/` suite) + 3 modified `restkit/` files — all `UNRELATED_EXISTING_WORK` from concurrent work outside `user-management`.
- Status docs consistency: **CONSISTENT for user-management**. `.codex/current-state.md` and `.codex/user-management.md` both record Wave 11 closure. **INCONSISTENT for `.kiro/README.md`** — stale "Current specs" table lists only `payment-operations-dashboard`.
- Next recommended spec: `audit-log-dashboard` (Spec #4) Wave 0, after explicit user request. `iam-roles-and-keycloak-login` is classified `COMPLETE_AND_KIRO_MARKED` (user decision 2026-06-18): all required implementation verified in code and GREEN; remaining unchecked Kiro boxes are optional tests (skippable per MVP policy) + required tasks implemented but with unchecked boxes.

## Worktree Status

| File | Status | Classification | Notes |
|---|---|---|---|
| `.codex/current-state.md` | M | `EXPECTED_STATUS_DOC` | Wave 11 final-checkpoint section added in prior task |
| `.codex/user-management.md` | M | `EXPECTED_STATUS_DOC` | Wave 11 final-checkpoint section added in prior task |
| `apps/backend/.../restkit/contract/create/PaymentOrderLifecycleContractRestKitTest.java` | M | `UNRELATED_EXISTING_WORK` | Excluded `restkit/` suite; concurrent work; not user-management |
| `apps/backend/.../restkit/spec/PaymentErrorSpecs.java` | M | `UNRELATED_EXISTING_WORK` | Excluded `restkit/` suite; concurrent work |
| `apps/backend/.../restkit/client/PaymentOrderApi.java` | M | `UNRELATED_EXISTING_WORK` | Excluded `restkit/` suite; concurrent work |
| `apps/backend/.../restkit/contract/create/PaymentOrderSecurityContractRestKitTest.java` | ?? | `UNRELATED_EXISTING_WORK` / `NEEDS_USER_DECISION` | Untracked; has compilation errors (missing `HeaderAssertions` methods); blocks unfiltered `test-compile`; belongs to `restkit/` work owner |

No Playwright files created. No `SUSPICIOUS` or `SHOULD_NOT_EXIST` files found.

## Kiro vs Codex Status Matrix

| Spec | `.kiro` task state | `.codex` state | Evidence | Classification |
|---|---|---|---|---|
| `backend-authority-refactor` | 28/28 checked | no codex doc | all boxes checked; converter allowlist + `Authorities` catalog in code | `COMPLETE_AND_KIRO_MARKED` |
| `payment-operations-dashboard` | 56/73 checked (12 unchecked: 7 optional `*` + 4 required-skipped Playwright `[-]` + 1 required-implemented) | no codex doc | task 16.2 (frontend README update) DONE 2026-06-18 — README now documents useApiClient envelope, shared component inventory, Error Lab, HTTP learning surfaces; task 16.1/17 final checkpoint GREEN (typecheck + 468 tests); 5 optional property tests (10.3 P3, 10.4 P12, 11.3 P14/P15, 12.2 lifecycle int, 14.3 P1) already implemented under alternate paths; flaky P6 test fixed (JSON `-0` normalization); 4 Playwright tasks `[-]` SKIPPED per user decision; 14.4/15.2 optional skipped (MVP policy) | `COMPLETE_AND_KIRO_MARKED` (user decision 2026-06-18) |
| `iam-roles-and-keycloak-login` | 22/38 checked (16 unchecked: 8 optional `*`, 8 required) | `.codex/iam-roles-and-keycloak-login.md` (gate review; impl done + GREEN validation) | code confirms required 9.2 (role-aware Overview), 10.1 (merchant action gating), 10.2 (payment action gating), 12.1 (Authorization masking) implemented; task 14 final checkpoint done informally (typecheck + 442 tests GREEN); 16 unchecked = 8 optional tests (skippable) + 8 required tasks implemented in code | `COMPLETE_AND_KIRO_MARKED` (user decision 2026-06-18) |
| `tenant-model-and-isolation` | 0/40 checked | `.codex/tenant-model-and-isolation.md` + `current-state.md` Waves 0–6 complete | code + GREEN filtered tests (321 Surefire + 16 Failsafe); all 6 optional jqwik properties (P1–P6, 10 declarations) implemented in `TenantIsolationPropertyTest`; `TenantModuleTest` 2/2, `TenantIsolationIT` 9/9, `MerchantModuleTest` 2/2 GREEN | `COMPLETE_AND_KIRO_MARKED` (user decision 2026-06-18) |
| `user-management` | 0/54 checked | `.codex/user-management.md` Waves 0–11 + `current-state.md` Wave 11 | code + GREEN tests + real Keycloak IT 3/3; closure = `COMPLETE_WITH_OPTIONAL_GAPS` | `COMPLETE_IN_CODE_BUT_KIRO_UNCHECKED` (closed) |
| `audit-log-dashboard` | 0/55 checked | not started | no code; prereqs #1/#2 hold (no `ApplicationEventPublisher`/`@ApplicationModuleListener` usage yet); dep approval flagged (task 3.2) | `NOT_STARTED` / `NEXT_CANDIDATE` |
| `deterministic-seed-and-test-isolation` | 0/31 checked | not started | no code; Spec #5 staged; Stage 4 depends on audit-log-dashboard | `NOT_STARTED` / `AFTER_AUDIT_OR_STAGED` |

Interpretation notes:
- Unchecked `[ ]` boxes do NOT mean not implemented. Per the Codex/OpenCode workflow, `.kiro/**` is intentionally read-only; implementation status is tracked in `.codex/**` and verified against code/tests.
- `tenant-model-and-isolation` and `user-management` have ALL boxes unchecked but are fully implemented and validated (GREEN). Both are now classified `COMPLETE_AND_KIRO_MARKED` per user decision.
- `iam-roles-and-keycloak-login` required tasks are implemented in code but were never formally closed via a final-checkpoint wave (task 14 box unchecked).

Data hygiene finding (read-only, not fixed): `.config.kiro` specId duplicates — `audit-log-dashboard`, `deterministic-seed-and-test-isolation`, `iam-roles-and-keycloak-login`, and `user-management` all share specId `ddbff980-460a-4eec-ae6b-f004d743fac8`. Only `backend-authority-refactor`, `payment-operations-dashboard`, and `tenant-model-and-isolation` have unique specIds. Not blocking; flagged for awareness.

## User-management Closure Check

- Required waves: Wave 0–10 — all `DONE_VERIFIED`.
- Wave 11 final checkpoint: completed and documented in `.codex/current-state.md` (lines 424–471) and `.codex/user-management.md` (lines 1170–1311).
- Required tasks: 6.6 `IamModuleTest` (4 tests GREEN), 6.7 real Keycloak IT (`UserManagementKeycloakAdminIT` 3/3 GREEN) — both `DONE_VERIFIED`.
- Optional skipped: 6.1 (token provider unit test), 6.8 (realm smoke), 6.9–6.13 (jqwik P1–P4, P6) — all `OPTIONAL_SKIPPED_ACCEPTABLE`, documented.
- Validation: backend `clean test` GREEN (321 Surefire, 0 failures, 5 skipped); `verify` GREEN (16 Failsafe, 0 failures); frontend `typecheck` GREEN; `test:unit` GREEN (38 files, 468 tests).
- Security: no token/secret/temporary-password exposure found (grep showed only legitimate server-side/test references); no local user DB/JPA/repository/Flyway; no `keycloak-admin-client` dependency (thin `RestClient` wrapper confirmed).
- Remaining unrelated work: the `restkit/` compilation issue (untracked `PaymentOrderSecurityContractRestKitTest.java`) is outside user-management scope and belongs to the `restkit/` work owner.

## Stale / Unmarked Status Findings

- `.kiro/README.md`: **STALE**. The "Current specs" table (line 94) lists only `payment-operations-dashboard` with status "requirements.md ✓ · design.md ✓ · tasks.md pending". In reality 7 specs now exist under `.kiro/specs/`, and `payment-operations-dashboard` tasks.md is 56/73 checked (not "pending"). Not modified (read-only per rules).
- `.kiro/specs/*/tasks.md`: `tenant-model-and-isolation` (0/40), `user-management` (0/54), `audit-log-dashboard` (0/55), `deterministic-seed-and-test-isolation` (0/31) all have fully unchecked boxes despite two of them being complete in code. `iam-roles-and-keycloak-login` (22/38) and `payment-operations-dashboard` (56/73) are partially checked. This is the expected read-only state of the Codex/OpenCode workflow; not a defect, but documented for traceability.
- `.codex/current-state.md`: **CONSISTENT** — Wave 11 section present with full closure details.
- `.codex/user-management.md`: **CONSISTENT** — Wave 11 final-checkpoint section present.
- Other: no `.codex/user-management-final-checkpoint.md`, `.codex/user-management-completion-audit.md`, or `.codex/spec-status.md` exist. This audit file is the new consolidated status-hygiene record.

## Recommended `.kiro/README.md` Update

**Do not apply automatically.** User approval required to edit `.kiro/README.md`. Proposed replacement for the "Current specs" table only:

```text
| Spec | Status |
|---|---|
| `backend-authority-refactor` | COMPLETE (tasks 28/28 checked) |
| `payment-operations-dashboard` | COMPLETE — 56/73 boxes checked; required tasks implemented & verified GREEN (12 unchecked = optional tests + Playwright-skipped `[-]` + optional 14.4/15.2) |
| `iam-roles-and-keycloak-login` | COMPLETE — 22/38 boxes checked; required tasks implemented & verified GREEN (8 unchecked = optional tests + required-implemented) |
| `tenant-model-and-isolation` | COMPLETE — Kiro boxes unchecked (0/40); Waves 0–6 verified GREEN including all optional P1–P6 |
| `user-management` | COMPLETE_WITH_OPTIONAL_GAPS — Kiro boxes unchecked (0/54); Waves 0–11 verified GREEN |
| `audit-log-dashboard` | NOT_STARTED — Spec #4, next candidate |
| `deterministic-seed-and-test-isolation` | NOT_STARTED — Spec #5, staged after audit-log-dashboard |
```

## Next Recommended Work

1. **`audit-log-dashboard` (Spec #4) Wave 0 — prerequisite gate** (next candidate, after explicit user request): confirmed Spec #4; Wave 0 is a verification-only hard gate; hard prerequisites #1 (iam-roles) and #2 (tenant) hold; introduces the project's first Spring Modulith `ApplicationEventPublisher`/`@ApplicationModuleListener` usage and first audit Flyway migration. **Task 3.2 requires explicit dependency approval** before adding `spring-modulith-events-api` + `spring-modulith-events-jpa` to `pom.xml`.
2. **`deterministic-seed-and-test-isolation` (Spec #5)**: not started; staged; Stage 4 depends on audit-log-dashboard. Do not start until #4 progresses.

## Actions Needed From User

- Approve editing `.kiro/README.md` to refresh the stale "Current specs" table? (Recommended; proposed table above. Otherwise it stays stale.)
- Approve starting `audit-log-dashboard` (Spec #4) Wave 0? (Next candidate. Note: task 3.2 will need explicit dependency approval for `spring-modulith-events-api`/`spring-modulith-events-jpa`.)
- Resolve the unrelated `restkit/` work? (The untracked `PaymentOrderSecurityContractRestKitTest.java` has compilation errors — missing `HeaderAssertions.assertWwwAuthenticatePresent` / `assertAuthorizationTokenIsNotLeaked` — and blocks unfiltered `./mvnw test` compilation. This belongs to the `restkit/` work owner, not user-management.)
