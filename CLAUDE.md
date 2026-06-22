# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Payment Quality Engineering Lab — a learning-oriented payment platform for practicing Java/Spring backend engineering, REST API testing, security testing, frontend contract consumption, and SDET skills.

## Commands

### Backend (from `apps/backend`)

```bash
./mvnw test           # runs *Test.java (unit, integration, module, security, REST Assured)
./mvnw verify         # also runs *IT.java via Failsafe
./mvnw spring-boot:run
```

**Always skip these suites** when running backend tests (unless explicitly asked):
- `apps/backend/src/test/java/lab/paymentquality/restkit/`
- `apps/backend/src/test/java/lab/paymentquality/paymentsupport/`

### Frontend (from `apps/frontend`)

```bash
corepack pnpm install
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm build
corepack pnpm test:unit          # Vitest unit/component/property tests
corepack pnpm exec playwright test
```

### Infrastructure (from repo root)

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

Copy `infra/compose/.env.example` → `infra/compose/.env` on first use.

## Backend Architecture

Spring Modulith modular monolith. Root package: `lab.paymentquality`.

### Module map

```
shared      — OPEN; cross-cutting: SecurityConfig, Authorities, CorrelationIdFilter, GlobalExceptionHandler
foundation  — standalone; GET /api/status (public)
tenant      — owns Tenant JPA entity; exposes PUBLIC API (TenantResolver, TenantContext, TenantReference)
merchant    — depends on tenant PUBLIC API and shared; owns Merchant entity and all merchant endpoints
payment     — depends on merchant PUBLIC API; owns PaymentOrder entity and all payment endpoints
iam         — Keycloak admin integration; user management
audit       — event-driven audit log; listens to AuditableActionOccurred domain events
testing     — shared test fixtures exposed as a module for test scope
```

**Hard dependency rule**: no module imports `*.internal.*` from another module. `merchant` → `tenant` public only. `payment` → `merchant` public only. Enforced by `ModulithArchitectureTest`.

### Persistence

- PostgreSQL 18, Flyway-owned schema, JPA with `ddl-auto: validate`.
- Migration locations: `classpath:db/migration/tenant`, `.../merchant`, `.../payment`.
- DB-dependent tests extend `PostgresContainerSupport` (spins up a postgres:18 container with a unique DB name per test class).

### Security

- JWT resource server; Keycloak realm roles are prefixed to `platform:` Spring authorities by `KeycloakRealmRoleConverter`.
- Authority constants live in `Authorities.java`.
- Security tests use locally generated signed JWTs via `TestJwtConfiguration` and `TestJwtSupport` — no live Keycloak required.
- `TestJwtSupport` provides: `platformAdminToken()`, `tenantAdminToken()`, `tokenWithRolesAndTenantId(...)`, `tokenWithRolesTenantIdAndMerchantId(...)`, `tokenWithoutTenantClaim()`.

### API surface

```
GET  /api/status                                                     (public)
POST /api/merchants
GET  /api/merchants
GET  /api/merchants/{id}
POST /api/merchants/{id}/activate
POST /api/merchants/{id}/suspend
POST /api/merchants/{merchantId}/payment-orders
GET  /api/merchants/{merchantId}/payment-orders
GET  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
HEAD /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}
GET  /api/merchants/{merchantId}/payment-orders/summary
GET  /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/history
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/authorize
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/capture
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/cancel
POST /api/merchants/{merchantId}/payment-orders/{paymentOrderId}/refund
```

## Frontend Architecture

Nuxt 4 app-directory layout. All browser-to-backend traffic is proxied through `server/api/**` routes; tokens never reach browser JS.

### Key layers

- `app/middleware/auth.global.ts` — protects all `/admin/**` routes.
- `app/stores/auth.ts` — exposes only `isAuthenticated` and sanitized `user`; no tokens.
- `server/utils/backendApi.ts` — reads sealed session `secure.accessToken` server-side and forwards as `Authorization: Bearer`.
- `app/types/api.ts` — `ApiResponse<T>` envelope (`{ data, status, headers, problem, raw }`).
- `app/composables/useApiClient.ts` — wraps `$fetch.raw`, validates body against a Zod schema, populates `problem` for `application/problem+json` responses.
- Domain composables (`useMerchantsApi`, `usePaymentOrdersApi`, `usePaymentLifecycleApi`, `useUsersApi`) delegate transport to `useApiClient`.
- `app/composables/useAuthorization.ts` — derives capability booleans (`canManageUsers`, `canAssignRoles`, etc.) from `rbacMatrix`.
- `app/components/shared/` — reusable protocol/state components (`ApiDebugPanel`, `ProblemDetailsCard`, `HeaderKeyValuePanel`, `RawJsonViewer`, `EtagDisplay`, `IfMatchInput`, etc.). `HeaderKeyValuePanel` and `ApiDebugPanel` always mask `Authorization` as `Bearer ••••••••`.

### Auth flow

`/login` → Keycloak PKCE Authorization Code Flow via `server/routes/auth/keycloak.get.ts` (uses `defineOAuthOidcEventHandler`) → sealed server-side session stores `secure.accessToken`. The frontend Playwright default suite mocks Nuxt session/API routes; set `PLAYWRIGHT_USE_REAL_KEYCLOAK=true` for live Keycloak.

## Testing Conventions

- Spring context tests use `@ActiveProfiles("test")`.
- REST Assured tests go in `apps/backend/src/test/java/lab/paymentquality/rest/`.
- Security tests go in `apps/backend/src/test/java/lab/paymentquality/security/` and import `TestJwtConfiguration`.
- Architecture boundary tests: `ModulithArchitectureTest`, `MerchantModuleTest`, `TenantModuleTest`, `PaymentModuleTest`.
- Frontend unit/property tests: Vitest + fast-check, colocated with source. Property tests are tagged `Feature: <spec>, Property {n}: ...` and run ≥ 100 iterations.
- Playwright specs live in `apps/frontend/tests/`.
- Ignore `My*` and `Lesson*` test files — they are learner practice copies.

## Active Implementation Context

The current branch (`018-rest-security-p1-error-auth-method-hardening`) continues the `tenant-model-and-isolation` spec. Progress is tracked in `.codex/current-state.md`. Specs are read-only under `.kiro/specs/`; update `.codex/current-state.md` (not `.kiro` files) to record progress.

Recommended reading order when resuming tenant work:
1. `.codex/README.md`
2. `.codex/current-state.md`
3. `.codex/tenant-model-and-isolation.md`
4. `.kiro/specs/tenant-model-and-isolation/requirements.md`
5. `.kiro/specs/tenant-model-and-isolation/design.md`
6. `.kiro/specs/tenant-model-and-isolation/tasks.md`

## Scope Guardrails

Out of scope for all phases:
- `POST /payments` top-level API, real PSP integration, PSP failure modeling
- Kafka, webhooks, outbox, settlement, payout, reconciliation, KYC, card/PAN/PCI, 3DS
- Microservice split, fake KPI/business dashboards
- Production OAuth/OIDC; Keycloak is local-dev and test only
