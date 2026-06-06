# Test Strategy

Keep the strategy practical and aligned with the current code.

## Existing Layers

- Unit/domain tests: value objects, lifecycle state rules, services, and small mappers where useful.
- Repository tests: Spring Boot + Testcontainers PostgreSQL 18, Flyway enabled, `ddl-auto: validate`.
- REST Assured tests: HTTP contract for status, merchant, payment order create/read/list/summary/lifecycle behavior.
- Security tests: JWT resource-server behavior, role matrix, merchant ownership, forbidden/unauthorized cases.
- Spring Modulith tests: module boundary and module bootstrapping checks.
- Integration tests: `*IT.java` through Maven Failsafe where persistence durability is the focus.
- Playwright tests: Nuxt dashboard merchant and payment-order journeys with existing auth setup and route mocks where appropriate.

## What Good Tests Check

- Business state changes, not only method calls.
- HTTP status, content type, stable error code, important headers, and response body.
- Database effects for uniqueness, idempotency, lifecycle history, and version-sensitive mutations.
- Authorization boundaries: unauthenticated, denied identity, partial authority, correct role, wrong merchant, platform role.
- Parallel-safe data using unique references and keys.
- UI states: loading, empty, validation, duplicate/conflict, forbidden, backend unavailable, success, stale state where applicable.

## What Not To Test Yet

- Top-level `POST /payments`.
- Real PSP provider behavior or PSP failure scenarios.
- Kafka, webhooks, outbox, settlement, payout, reconciliation, KYC, card/PAN/PCI, 3DS.
- Client Credentials Flow or production OAuth/OIDC completeness.
- Payment status read models beyond the implemented payment order/list/summary/history APIs.
- Fake dashboard KPIs or broad operational dashboards.

## Verification Order

For support-file-only changes, review the diff and skip application test runs unless requested.

For backend behavior changes:

```bash
cd apps/backend
./mvnw test
./mvnw verify
```

For frontend behavior changes:

```bash
cd apps/frontend
corepack pnpm typecheck
corepack pnpm build
corepack pnpm exec playwright test
```
