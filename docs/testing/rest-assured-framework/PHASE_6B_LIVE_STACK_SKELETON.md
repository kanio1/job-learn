# Phase 6B — Live Stack Skeleton

> **Status**: Complete. `StatusSpec` passes. `mvn verify` exits BUILD SUCCESS.

---

## What was implemented

- `PostgresSupport` — wraps `PostgreSQLContainer("postgres:18")` on a shared TC network.
- `BackendSupport` — wraps `GenericContainer` for the backend OCI image; wires DB and Keycloak env vars.
- `ApiStack` — dual-mode singleton: `TESTCONTAINERS` (default) or `EXTERNAL` (when `API_BASE_URI` is set).
- `ApiStackExtension` — JUnit `BeforeAllCallback`; stores stack in root extension context as `CloseableResource`.
- `ApiTest` — composed annotation: `@ExtendWith(ApiStackExtension.class)` + `@Tag("live")`.
- `StatusSpec` — first live spec: `GET /api/status → 200` with body assertions.

## Validation

```bash
# Offline (unit wiring tests)
cd apps/api-tests && mvn -q test          # 70 tests, BUILD SUCCESS

# Live (requires BACKEND_IMAGE)
cd apps/api-tests
BACKEND_IMAGE=payment-quality/backend:local mvn verify   # 1 spec, BUILD SUCCESS
```

---

## Root cause discovered during Phase 6B

### Problem

Backend container exited before `/api/status` was reachable. Failsafe reported only a TC stack
trace — no Spring Boot logs visible.

### Diagnostics added

`BackendSupport.start()` now attaches a log consumer and wraps `container.start()` in a try-catch:

```java
.withLogConsumer(frame -> System.err.print("[BACKEND] " + frame.getUtf8String()))
```

```java
public void start() {
    try {
        container.start();
    } catch (Exception e) {
        System.err.println("=== BACKEND CONTAINER FAILED TO START ===");
        System.err.println(container.getLogs());
        throw e;
    }
}
```

### Actual root cause: `corsConfigurationSource` bean missing

With logging enabled, the backend log showed:

```
No bean named 'corsConfigurationSource' available
Error creating bean with name 'filterChain' defined in SecurityConfig.class:
  Factory method 'filterChain' threw exception with message:
  No bean named 'corsConfigurationSource' available
```

**Cause**: `SecurityConfig.corsConfigurationSource()` carries `@Profile({"dev", "test"})`, but
`filterChain()` calls it directly (CGLIB-proxied call → bean lookup in context). When the TC
container runs without any active Spring profile, the bean is absent and the application context
fails to start.

**Fix**: Activate the `dev` profile in the backend container by adding an env var:

```java
.withEnv("SPRING_PROFILES_ACTIVE", "dev")
```

Added to `BackendSupport` constructor alongside the existing env vars. No image rebuild required.

### Backend-side note (out of scope for Phase 6B)

`SecurityConfig.corsConfigurationSource()` should not be profile-gated when it is referenced
unconditionally in `filterChain()`. The `@Profile({"dev", "test"})` restriction is a latent backend
bug: the production image fails to start without an active profile. The CORS bean should either:
- be promoted to default (no profile restriction), or
- be referenced conditionally in `filterChain()`.

This is tracked as a known issue; fixing it requires a backend change and OCI image rebuild.

---

## Environment variable reference

| Variable | Where set | Purpose |
|---|---|---|
| `BACKEND_IMAGE` | caller / CI | Required in `TESTCONTAINERS` mode; Docker image name for backend container |
| `API_BASE_URI` | caller | Switches `ApiStack` to `EXTERNAL` mode; no containers started |
| `DB_URL` | `BackendSupport` | Internal JDBC URL via `postgres-db` network alias |
| `DB_USER` | `BackendSupport` | Postgres username (`payment_quality`) |
| `DB_PASSWORD` | `BackendSupport` | Postgres password (`payment_quality_dev`) |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` | `BackendSupport` | Placeholder Keycloak issuer (not fetched at startup when `jwk-set-uri` is also set) |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | `BackendSupport` | Placeholder JWK set URI (not fetched; no authenticated requests in 6B-lite) |
| `APP_TESTING_ENABLED` | `BackendSupport` | Enables `/api/test/seed` and `/api/test/reset` endpoints |
| `SPRING_PROFILES_ACTIVE` | `BackendSupport` | **Set to `dev`** — activates `corsConfigurationSource` bean in `SecurityConfig` |

---

## Deferred to Phase 6B-full / Phase 7+

| Item | Blocker |
|---|---|
| `KeycloakSupport` | Not needed for public-endpoint specs; required for authenticated specs |
| `KeycloakTokenFactory` | Blocked by `KeycloakSupport` |
| Fix `Identities` role names for real tokens | Placeholder names don't match realm |
| Fix `SecurityConfig.corsConfigurationSource` profile restriction | Backend change + image rebuild |
| `MerchantsApi` / `PaymentOrdersApi` | Blocked by auth + Phase 7 |
| `SeedApi` | Phase 7+ |
