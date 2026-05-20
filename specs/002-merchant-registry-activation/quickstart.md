# Quickstart: Phase 1 Merchant Registry

**Date**: 2026-05-18

This quickstart assumes Phase 0 foundation is working (backend, frontend, infrastructure).

## Prerequisites

- Java JDK 25
- Docker with Docker Compose
- Node.js `^22.12.0 || ^24.11.0 || >=26.0.0`
- pnpm (or Corepack: `corepack pnpm`)
- Bash-compatible shell

## 1. Start Local Infrastructure

```bash
# From repository root
cp infra/compose/.env.example infra/compose/.env
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

PostgreSQL 18 on `localhost:5432`. Keycloak 26.6.1 on `http://localhost:8081`.

Phase 1 adds a Keycloak realm import. See `infra/keycloak/realms/` for the realm configuration file that creates the `payment-quality` realm with platform-operator roles and users.

## 2. Start Backend

```bash
cd apps/backend
./mvnw spring-boot:run
```

Backend on `http://localhost:8080`.

On first startup, Flyway runs the `V1__create_merchants.sql` migration against the local PostgreSQL, creating the `merchants` table.

### Verify

```bash
# Public status endpoint
curl http://localhost:8080/api/status
# → {"application":"payment-quality-lab","phase":"foundation","status":"UP"}

# Merchant endpoint (denied without token)
curl http://localhost:8080/api/merchants
# → 401 Unauthorized
```

## 3. Start Frontend

```bash
cd apps/frontend
corepack pnpm install
corepack pnpm dev
```

Frontend on `http://localhost:3000`.

### Login Journey

1. Open `http://localhost:3000`. You will be redirected to Keycloak login (unauthenticated).
2. Log in as `platform.operator` / `platform.operator` (local lab credentials).
3. After successful login, you are redirected to `http://localhost:3000/admin/merchants`.
4. You see the merchant registry with empty state.

## 4. Local Test Identities

| Username | Password | Keycloak Realm Roles |
|---|---|---|
| `platform.operator` | `platform.operator` | `merchants:create`, `merchants:read`, `merchants:update-status` |
| `merchant.denied` | `merchant.denied` | _(none — for 403 denial-path tests)_ |

Both users are created by the realm import file. Passwords are for local lab use only.

## 5. Run Tests

### Backend

```bash
cd apps/backend
./mvnw test          # unit, architecture, context-load
./mvnw verify        # integration tests with Testcontainers (Failsafe, *IT)
```

### Frontend

```bash
cd apps/frontend
corepack pnpm exec playwright test
```

Playwright tests include authenticated dashboards journeys and security denial-path scenarios.

## 6. Stop Everything

```bash
# From repository root
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

## Key Files

| Area | File |
|---|---|
| Backend pom.xml | `apps/backend/pom.xml` |
| Application config | `apps/backend/src/main/resources/application.yml` |
| Flyway migration | `apps/backend/src/main/resources/db/migration/merchant/V1__create_merchants.sql` |
| Keycloak realm | `infra/keycloak/realms/payment-quality-realm.json` |
| Frontend auth store | `apps/frontend/app/stores/auth.ts` |
| Merchant page | `apps/frontend/app/pages/admin/merchants.vue` |
| Auth middleware | `apps/frontend/app/middleware/auth.global.ts` |
| Zod schemas | `apps/frontend/app/schemas/merchant.schema.ts` |
| Playwright tests | `apps/frontend/tests/e2e/merchant-create.spec.ts`, `apps/frontend/tests/e2e/merchant-lifecycle.spec.ts`, `apps/frontend/tests/e2e/auth-deny.spec.ts`, `apps/frontend/tests/e2e/merchant-feedback.spec.ts` |
