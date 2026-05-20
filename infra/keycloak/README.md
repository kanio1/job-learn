# Keycloak Local Auth Baseline

Phase 1 provides deterministic local Keycloak 26.6.1 identity configuration for the merchant registry operator journey.

## What Exists

- Docker Compose service `payment-quality-keycloak`
- Realm import `infra/keycloak/realms/payment-quality-realm.json`
- Realm `payment-quality`
- Public PKCE client `payment-quality-dashboard`
- Roles `merchants:create`, `merchants:read`, `merchants:update-status`
- Users `platform.operator` and `merchant.denied`

## Usage

Start with:

```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

Compose runs Keycloak with `start-dev --import-realm`. See `docs/setup/keycloak-local-auth.md` for the tester walkthrough.

## Still Deferred

- Production identity hardening
- Token refresh/revocation policy
- Merchant machine-to-machine credentials
- Client Credentials Flow
