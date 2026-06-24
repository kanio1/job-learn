# Phase 6C — Keycloak Auth Discovery and Minimal Token Factory

> **Status**: Complete. `mvn verify` exits BUILD SUCCESS with 3 live specs (1 status + 2 security smoke).

---

## Summary

Phase 6C wires a real Keycloak 26 container into the Testcontainers stack, implements ROPC-based
token minting, and runs the first authenticated integration specs: 401 (no auth) and 403 (denied user).

---

## Realm Analysis: `payment-quality-realm.json`

### ROPC-eligible client

| Client ID | Type | ROPC enabled |
|---|---|---|
| `payment-quality-dashboard` | public | YES (`directAccessGrantsEnabled: true`) |
| `payment-quality-admin` | confidential | NO (secret is a placeholder env var) |

Token endpoint: `POST /realms/payment-quality/protocol/openid-connect/token`

```
grant_type=password
client_id=payment-quality-dashboard
username=<user>
password=<password>
```

### Enabled users with credentials in realm JSON

These are local dev / test credentials only — not secrets.

| Username / Password | Composite role | Notes |
|---|---|---|
| `platform.admin` / `platform.admin` | `PLATFORM_ADMIN` | All platform operations |
| `tenant.admin` / `tenant.admin` | `TENANT_ADMIN` | Tenant `TENANT_ALPHA` |
| `merchant.manager` / `merchant.manager` | `MERCHANT_MANAGER` | Placeholder tenant/merchant IDs |
| `support.agent` / `support.agent` | `SUPPORT_AGENT` | Read-only cross-tenant |
| `merchant.denied` / `merchant.denied` | (none) | Valid JWT, no roles |
| `platform.payment.reader` / `platform.payment.reader` | `PLATFORM_PAYMENT_READER` | `platform:payments:read` |
| `merchant.payment.lifecycle` / `merchant.payment.lifecycle` | (leaf roles) | `merchant:payments:read/create/lifecycle` |
| `platform.payment.admin` / `platform.payment.admin` | `PLATFORM_PAYMENT_ADMIN` | Broad payment admin |
| `platform.payment.auditor` / `platform.payment.auditor` | `PLATFORM_PAYMENT_AUDITOR` | Audit access |
| `readonly.user` / `readonly.user` | `READ_ONLY_USER` | `merchants:read`, `platform:payments:read` |
| `platform.operator` / `platform.operator` | `PLATFORM_OPERATOR` | Platform operations |

**Disabled users** (cannot mint tokens): `merchant.payment.creator`, `merchant.payment.reader`,
`merchant.payment.operator`.

### Role mapping: composite → leaf roles → Spring Security authorities

The backend's `KeycloakRealmRoleConverter` maps LEAF roles from the JWT's `realm_access.roles`
array to Spring Security `GrantedAuthority`. Keycloak expands composite roles before embedding them
in the JWT — so `PLATFORM_ADMIN` never appears in the token; its leaf roles do.

Selected leaf role → Spring authority mappings:

| Keycloak leaf role | Spring authority |
|---|---|
| `merchants:create` | `platform:merchants:create` |
| `merchants:read` | `platform:merchants:read` |
| `merchants:manage` | `platform:merchants:manage` |
| `merchant:payments:create` | `merchant:payments:create` |
| `merchant:payments:read` | `merchant:payments:read` |
| `merchant:payments:lifecycle` | `merchant:payments:lifecycle` |
| `platform:payments:read` | `platform:payments:read` |

---

## Issuer URL Strategy

The test JVM and the backend container see different hostnames for Keycloak:

| URL | Used for | Why |
|---|---|---|
| `http://localhost:<mapped_port>/realms/...` | `ISSUER_URI`, token endpoint | Test JVM mints tokens here; `iss` claim in JWT matches |
| `http://keycloak:8080/realms/...` | `JWK_SET_URI` | Backend container fetches JWKS via internal TC network |

If `ISSUER_URI` and `JWK_SET_URI` point to the same host-mapped URL:
- The backend container cannot resolve `localhost` to reach Keycloak → JWKS fetch fails → 401 on every authenticated request.

If `JWK_SET_URI` uses the host-mapped URL:
- Container cannot reach `localhost:<host-port>` from inside Docker → connection refused → 401.

The split design (`issuerUri()` = host-mapped, `internalJwksUri()` = internal alias) solves both problems.

### Spring Boot behaviour

`NimbusJwtDecoder.withJwkSetUri(jwkSetUri)` fetches JWKS lazily on the first validated request.
`ISSUER_URI` is used only by `JwtValidators.createDefaultWithIssuer(issuerUri)` to validate the `iss`
claim — no HTTP call is made to ISSUER_URI at startup or validation time.

---

## Safe Authenticated Smoke Test: Why 403 Works Without Seed Data

`GET /api/merchants` requires `hasAuthority('platform:merchants:read')`.

`merchant.denied` has NO roles → Spring Security's `authorizeHttpRequests` filter denies the request
**before** the `MerchantController` method body executes. `TenantResolverService.resolve(jwt)` is
never called — so no database lookup occurs, and no `PLATFORM_TENANT` seed row is needed.

This is the key invariant: `@PreAuthorize` (and `authorizeHttpRequests`) fire in the security filter
chain, which runs before servlet dispatch to the controller.

---

## Files Added / Modified in Phase 6C

| File | Change |
|---|---|
| `src/test/resources/keycloak/payment-quality-realm.json` | Copied from `infra/keycloak/realms/` for classpath mounting |
| `core/stack/KeycloakSupport.java` | New — `GenericContainer` wrapper for Keycloak 26.6.1 |
| `core/auth/KeycloakTokenFactory.java` | New — ROPC token minting with 30s-margin caching |
| `core/auth/KeycloakTokenFactoryTest.java` | New — 9 offline unit tests (constructor guards + CachedToken expiry) |
| `core/auth/Identities.java` | Updated — `install()`, real user/role mappings, `denied()`, `supportAgent()`, `merchantManager()` |
| `core/stack/ApiStack.java` | Updated — starts Keycloak, installs `KeycloakTokenFactory` |
| `core/stack/BackendSupport.java` | Updated — accepts `KeycloakSupport`, uses real issuer/JWKS URLs |
| `scenarios/SecuritySmokeSpec.java` | New — 401 (no auth) and 403 (denied user) |
| `ContextAndFilterWiringTest.java` | Fixed — role assertions updated to real Keycloak composite names |
| `pom.xml` | Added `jackson-databind:2.21.2` for token response parsing |

---

## Validation

```bash
# Offline (unit tests — no containers)
cd apps/api-tests && mvn -q test
# Result: 79 tests, BUILD SUCCESS

# Live (requires Docker image)
cd apps/api-tests
BACKEND_IMAGE=payment-quality/backend:local mvn verify
# Result: 3 IT specs (1 status + 2 security smoke), BUILD SUCCESS
# Startup: Postgres ~3s, Keycloak ~10s, Backend ~20s
```

---

## Deferred to Phase 7+

| Item | Blocker |
|---|---|
| `MerchantsApi` authenticated create/read specs | Requires `PLATFORM_TENANT` seed data in DB |
| `PaymentOrdersApi` specs | Requires merchant + payment seed data |
| Parameterized persona tenant/merchant binding | Requires Phase 7 seed API + real IDs |
| Fix `SecurityConfig.corsConfigurationSource` profile restriction | Backend change + image rebuild |
| `platform.admin` GET /api/merchants → 200 [] | Requires `PLATFORM_TENANT` seed row |
