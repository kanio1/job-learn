# Infrastructure - Local PostgreSQL 18 and Keycloak 26.6.1

Phase 0 defines local supporting services.

## Services

- PostgreSQL 18: local database baseline
- Keycloak 26.6.1: local identity provider baseline

Compose uses fully qualified images: `docker.io/library/postgres:18` and `quay.io/keycloak/keycloak:26.6.1`.

## Commands

```bash
cp infra/compose/.env.example infra/compose/.env
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

## Deferred

- Business schemas
- Payment data
- Business realms and roles
- Application OAuth/OIDC integration
- Production security hardening

## Testing Relevance

Future persistence and security tests can start from known local service names, ports, and environment conventions.
