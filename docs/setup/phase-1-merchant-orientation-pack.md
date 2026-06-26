# Phase 1 Merchant Orientation Pack

## What Exists

- Merchant create, list, retrieve, activate, and suspend backend endpoints.
- PostgreSQL persistence owned by Flyway migration `V1__create_merchants.sql`.
- JWT resource-server authorization using Keycloak realm roles.
- Nuxt `/admin/merchants` dashboard rendered inside the Nuxt UI dashboard shell with create/list/lifecycle actions.

## What Is Absent

- Payment orders, PSPs, Kafka, refunds, settlement, reconciliation, KYC, Client Credentials Flow, merchant self-service, country/currency rules, and payment dashboards.

## Run

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
cd apps/backend && SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
cd apps/frontend && corepack pnpm dev
```

Login through `/login` as `platform.operator`. The dashboard redirects to Keycloak through `/auth/keycloak` using OIDC Authorization Code Flow with PKCE for the public `payment-quality-dashboard` client. Use `merchant.denied` to explore the insufficient-authority path.

The authenticated app renders one dashboard sidebar, one content panel, and a user/session menu in the sidebar footer using a light theme with the Public Sans font.

Unauthenticated requests to `/admin/merchants` redirect to Keycloak (HTTP 401 at the API layer). Authenticated identities without merchant authorities see a deterministic 403 denial state. Invalid lifecycle transitions (e.g., suspending a DRAFT merchant) return 409 conflict responses. The back-end enforces the same HTTP boundaries via the resource-server security configuration.

If the merchant page reports that the backend service is unavailable, verify the backend is running on `http://localhost:8080` before treating it as an auth failure. If the Nuxt frontend port 3000 is already occupied (for example from a previous dev session), stop any leftover Nuxt dev processes on that port before restarting; the Playwright dev server also binds port 3000.

## Merchant Lifecycle

```text
DRAFT -> ACTIVE -> SUSPENDED
```

All other transitions are invalid and should produce conflict feedback.

## Exploratory Charters

- Create merchants around reference/display-name boundaries.
- Try duplicate references with case and whitespace differences.
- Exercise activate/suspend invalid transitions.
- Compare unauthenticated, denied, partial-authority, and full-operator behavior.
- Restart services and confirm persisted merchants remain visible.
- Inspect logs for correlation ID and absence of tokens/passwords.
