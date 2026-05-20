# Phase 1 Merchant Test Design

## Layers

- Domain tests cover value objects and lifecycle rules.
- Service tests cover orchestration, duplicate pre-checks, database unique-constraint translation, and safe logs.
- Repository tests cover PostgreSQL constraints and stable ordering.
- REST Assured tests cover HTTP contracts and error bodies.
- Security tests cover signed JWT claim profiles and authority separation.
- Integration tests cover Flyway, PostgreSQL 18, lifecycle persistence, and durability across context reload.
- Playwright tests cover create, validation, duplicate feedback, lifecycle, denial, loading, and error states.

## Techniques

- Equivalence Partitioning and Boundary Value Analysis for merchant reference and display name.
- State Transition Testing for `DRAFT`, `ACTIVE`, and `SUSPENDED`.
- Decision table thinking for the authorization matrix.
- Concurrency test for duplicate merchant reference creation.

Detailed test-data rules are in `docs/testing/phase-1-test-data-strategy.md`.

## Remaining Risks

- Real browser login against Keycloak should still be walked manually because the default fast Playwright suite mocks session/API routes.
- Phase 1 does not validate future payment eligibility because payment orders do not exist yet.
