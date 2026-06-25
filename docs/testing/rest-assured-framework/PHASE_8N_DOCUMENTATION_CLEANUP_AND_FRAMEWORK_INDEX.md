# Phase 8N — Documentation Cleanup and Framework Index

## Scope

Docs-only cleanup after Phases 8J–8M.

No backend code, test assertions, API facades, or scenario behavior changed.

## Changes

### `apps/api-tests/README.md`

Replaced stale deferred-phase content with a concise current index:

- module purpose and hard black-box rules;
- offline and live validation commands;
- current baseline counts;
- framework navigation table;
- test-suite map;
- Phase 8A–8N summary;
- practical navigation notes for `RequestSpecs`, `ResponseSpecs`, `ProblemAssert`, and `Seeds`.

Decision: the README is the best quick-entry point for future SDETs because it sits next to
`pom.xml` and is the first file a developer sees inside the module.

### `scenarios/package-info.java`

Replaced the old Phase 7 deferred/nested package description with the current flat scenario map:

- smoke/status;
- security smoke;
- merchant contract;
- payment order contract and lifecycle;
- lifecycle idempotency;
- refund;
- PATCH;
- method semantics;
- schema;
- audit;
- summary;
- tenant isolation.

### `REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md`

Cleaned stale current-state language:

- no longer says `apps/api-tests` is missing;
- records current baseline as 79 offline tests and 72 live specs after Phase 8M;
- no longer says JSON schema validation, stack, Keycloak, API clients, filters, or live specs are deferred;
- adds Phase 8N to the phase table.

## Validation

- Offline: `cd apps/api-tests && mvn -q test` — 79 tests passed.
- Live: `cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify` — 72 tests passed.
