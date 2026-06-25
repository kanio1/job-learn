# Phase 8Q — Targeted Cleanup Fixes

## Scope

Small pre-commit cleanup from the Phase 8P review. No backend code, new tests, new dependencies,
scenario renames, or business assertion changes.

## Phase 8P Findings Handled

| Finding | Handling |
|---|---|
| `ResponseSpecs.created()` existed but was unused | Applied it to the primary payment-order create happy-path test. Exact `Location` path and exact `ETag: "v0"` assertions remain explicit because they teach resource URI and initial version semantics. |
| `ProblemAssert` top-level JavaDoc said schema validation was deferred | Reworded it to say Phase 8H enabled `matchesProblemSchema()` through REST Assured JSON Schema validation. |
| Main plan baseline said 79/72 passed after Phase 8M | Reworded the current-state baseline to say the repo is current through Phase 8Q. |
| Main plan marked 6B-full as deferred | Clarified that the Keycloak/live-stack pieces were superseded and landed through 6C+ live stack work. |

## Validation

- Offline: `cd apps/api-tests && mvn -q test` — 79 tests passed.
- Live: `cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify` — 72 live specs passed.
