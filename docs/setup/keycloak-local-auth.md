# Local Keycloak Auth

Start local infrastructure:

```bash
cp infra/compose/.env.example infra/compose/.env
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
```

Compose imports `infra/keycloak/realms/payment-quality-realm.json` with `start-dev --import-realm`.

## Realm

- Realm: `payment-quality`
- Public client: `payment-quality-dashboard`
- Redirect URI: `http://localhost:3000/*` and `https://app.payment-quality.local/*` (TLS lab)
- PKCE: `S256`
- Discovery endpoint: `http://localhost:8081/realms/payment-quality/.well-known/openid-configuration`

The Nuxt dashboard keeps the user-facing route `/auth/keycloak`, but implements it with the generic OIDC handler from `nuxt-auth-utils` so the public client uses Authorization Code Flow with PKCE. No Keycloak client secret is required for the local public client.

## Roles

- `merchants:create`
- `merchants:read`
- `merchants:update-status`

The backend maps these to `platform:merchants:create`, `platform:merchants:read`, and `platform:merchants:update-status`.

## Test Users

- `platform.operator` / `platform.operator`: all merchant realm roles, with deterministic local profile fields.
- `merchant.denied` / `merchant.denied`: authenticated but no merchant realm roles, with deterministic local profile fields.

Add new local users by editing the realm import or through the Keycloak Admin Console at `http://localhost:8081`, then document their intended test role.
