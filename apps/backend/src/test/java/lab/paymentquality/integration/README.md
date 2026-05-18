# Backend Integration Test Conventions

This folder is reserved for future Spring integration and Testcontainers tests.

Phase 0 does not require a PostgreSQL container because no persistence behavior exists yet.

Future tests should:
- Use `*IT` naming when they belong in `./mvnw verify`.
- Avoid hidden singleton mutable state.
- Use unique data namespaces such as `testRunId`, `workerId`, and scenario identifiers.
- Prefer transaction rollback, isolated schemas, cleanup-by-owner, or container-per-suite strategies based on the test layer.
- Never delete broad shared data from a database used by other tests.
