# Phase 1 Test Data Strategy

Merchant references must be unique per test scenario:

```text
MERCH-{testRunId}-{workerId}-{uuid}
```

No test relies on a global shared merchant. Tests create their own data or mock API responses at the layer under test.

## Layer Strategy

- Domain tests: no database data.
- Service tests: mocked repository data scoped to each test method.
- `@DataJpaTest`: transaction rollback and class-scoped PostgreSQL Testcontainer.
- REST/security tests: class-scoped PostgreSQL Testcontainer and unique references per method.
- Integration tests: class-scoped PostgreSQL Testcontainer with explicit durability scenario.
- Playwright: unique references per scenario; default fast suite mocks Nuxt API routes.

Phase 1 Playwright runs with `fullyParallel: false` while Keycloak/session isolation remains new. Data generation is still worker-safe so frontend parallelism can be re-enabled later.
