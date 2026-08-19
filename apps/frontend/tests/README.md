# Frontend tests

- **Playwright (product):** [`../tests-pom`](../tests-pom) — live Keycloak + Nuxt BFF + Spring + Postgres. `corepack pnpm test:e2e`.
- **Vitest:** [`unit/`](unit/) and colocated `app/**/*.test.ts`. `corepack pnpm test:unit`.

There is no mocked Playwright suite on the default runner.
