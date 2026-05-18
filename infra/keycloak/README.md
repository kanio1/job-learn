# Keycloak Phase 0 Baseline

Phase 0 provides local Keycloak 26.6.1 infrastructure only.

## What Exists

- Docker Compose service `payment-quality-keycloak`
- Local admin credentials sourced from `infra/compose/.env`
- Reserved realm import location: `infra/keycloak/realms`

## What Is Deferred

- Full application OAuth/OIDC integration
- Business realm model
- Merchant/admin/risk roles
- Frontend route guards
- Backend resource-server validation
- Token lifecycle, refresh, revocation, and logout behavior

## Realm Imports

No realm import is provided in Phase 0. The `realms` directory is reserved so a future security specification can add a deterministic local realm when the application actually needs auth behavior.

Do not add business roles or clients here without a security-focused feature specification.
