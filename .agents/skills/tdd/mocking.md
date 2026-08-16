# When to Mock

Mock at **system boundaries** only:

- External PSP/HTTP clients (the local mock boundary already in this repo)
- Time / randomness
- Mail or other I/O you do not own

Do not mock:

- Your own application services, repositories, or Modulith internals for REST Assured tests
- Vue components behind Playwright E2E (drive the UI)
- BFF handlers behind Playwright REST (hit the real BFF)

## This lab

- REST Assured + Testcontainers PostgreSQL is the default for HTTP contracts. Do not mock the DB there.
- Security tests use `TestJwtConfiguration` — that is a boundary adapter, not an internal mock.
- Playwright E2E may mock **network** only when the test is explicitly about UI states under backend-unavailable / offline; say so in the test title.
- Playwright live HTTP tests use real Keycloak/backend. Do not stub them into passing.

## Designing for mockability

Accept boundary dependencies; do not construct them inside domain logic. Prefer a small typed client per external operation over a generic `fetch(url)` that tests must branch on.
