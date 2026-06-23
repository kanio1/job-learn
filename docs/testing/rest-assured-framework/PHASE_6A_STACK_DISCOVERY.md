# Phase 6A — Stack and Runtime Discovery

> **Status**: Discovery complete. No live container code yet. Phase 6B will implement
> `ApiStack`, `PostgresSupport`, `KeycloakSupport`, `BackendSupport`, `ApiStackExtension`, and
> `KeycloakTokenFactory` — but only after the decisions documented here are acted on.
>
> This document is the single source of truth for Phase 6B implementors. Read it in full before
> writing a line of container code.

---

## 1. Current Runtime Reality

### Compose services

The compose file (`infra/compose/compose.yml`) defines exactly two services.

| Service name | Image | Default port (host) | Container port | Key config |
|---|---|---|---|---|
| `payment-quality-postgres` | `docker.io/library/postgres:18` | `5432` (`POSTGRES_PORT`) | `5432` | DB `payment_quality_lab`, user `payment_quality` |
| `payment-quality-keycloak` | `quay.io/keycloak/keycloak:26.6.1` | `8081` (`KEYCLOAK_PORT`) | `8080` | `start-dev --import-realm` |

**The backend is NOT a compose service.** It runs separately as a Spring Boot process.

### Postgres

- DB name: `payment_quality_lab`
- User: `payment_quality`
- Password: `payment_quality_dev` (dev only, local `.env`)
- Port: `5432` by default
- Healthcheck: `pg_isready -U payment_quality -d payment_quality_lab`
- Data volume: `payment-quality-postgres-data`

### Keycloak

- Image: `quay.io/keycloak/keycloak:26.6.1` (matches `testcontainers-keycloak:3.7.0` from dasniko, which supports Keycloak 26)
- Host port: `8081`
- Container port: `8080` (Keycloak internal)
- Start mode: `start-dev --import-realm`
- Realm import volume mount:
  `infra/keycloak/realms/payment-quality-realm.json` → `/opt/keycloak/data/import/payment-quality-realm.json`
- Admin credentials: `admin` / `admin` (local dev only, from `.env`)

### Backend

- NOT in compose.
- Started by: `cd apps/backend && ./mvnw spring-boot:run` or running the JAR.
- Server port: `8080`.
- Default DB connection: `jdbc:postgresql://localhost:5432/payment_quality_lab` (env: `DB_URL`).
- JWT issuer URI: `http://localhost:8081/realms/payment-quality` (env: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`).
- JWK set URI: `http://localhost:8081/realms/payment-quality/protocol/openid-connect/certs` (env: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI`).
- Authorized party claim: `payment-quality-dashboard` (env: `EXPECTED_AZP`).
- Testing seed/reset endpoints guarded by `@ConditionalOnProperty(app.testing.enabled=true)` + `@Profile("!prod")`.

---

## 2. Backend Image Strategy

### Current status

- **No Dockerfile exists** anywhere in the repository. Confirmed by exhaustive `find` — no `Dockerfile`, `*.dockerfile`, or Jib configuration.
- **`spring-boot-maven-plugin` is present** in `apps/backend/pom.xml` (inherits from Spring Boot 4.0.6 parent).

### OCI image build via Spring Boot Maven Plugin

Spring Boot Maven Plugin 3.x (bundled with Spring Boot 4) supports `spring-boot:build-image` using Paketo Buildpacks (no Dockerfile needed).

```bash
# From apps/backend — builds OCI image using Paketo Buildpacks
cd apps/backend
./mvnw spring-boot:build-image -DskipTests \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local
```

Default image name without `-DimageName` override: `payment-quality-backend:0.0.1-SNAPSHOT`
(derived from `artifactId:version`).

**Phase 6B must build this image before running Testcontainers specs.**

### Recommended image name convention

```
payment-quality/backend:local
```

Set via env variable `BACKEND_IMAGE=payment-quality/backend:local` — already documented in
`apps/api-tests/README.md` and `ApiConfig`.

### Maven Failsafe pre-integration-test hook (Phase 6B option)

To automate the image build before `mvn verify`:

```xml
<!-- In apps/api-tests/pom.xml Phase 6B — do NOT add yet -->
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <!-- exec-maven-plugin is simpler since we're in a different module -->
</plugin>
```

Simpler alternative for Phase 6B: document the manual build step and require `BACKEND_IMAGE` to be
set externally. The test suite detects the missing variable and fails fast with a clear message from
`ApiStack`.

---

## 3. Testcontainers Strategy

### Options considered

| Option | Pros | Cons |
|---|---|---|
| **A: Pure Testcontainers** (Postgres + Keycloak + backend as 3 containers) | Self-contained, CI-safe, no external dependency | Requires OCI backend image; image build adds ~90s on first run |
| **B: External stack** (connect to running compose + backend) | Zero image build; fast local dev | Not CI-safe; `mvn verify` fails if stack is not running |
| **C: Hybrid** (TC for Postgres + Keycloak; backend on `localhost:8080`) | Avoids image build; TC handles infra | Tightly couples test to running backend; not CI-safe without backend start |

### Recommended strategy: **dual-mode via `StackMode`**

Implement two modes in `ApiStack`:

- `TESTCONTAINERS` (default): starts Postgres, Keycloak, and backend in TC containers.
  Requires `BACKEND_IMAGE` to be set.
- `EXTERNAL`: reads `API_BASE_URI` from environment. No containers started.
  For local dev loop when the full stack is already running.

```java
// StackMode enum (already committed to core/stack/ as Phase 6A placeholder)
public enum StackMode { TESTCONTAINERS, EXTERNAL }
```

`ApiStack` resolves mode at startup:

```java
if (System.getenv("API_BASE_URI") != null) → EXTERNAL
else → TESTCONTAINERS
```

This gives CI `TESTCONTAINERS` by default and local devs `EXTERNAL` if they export `API_BASE_URI`.

### Container start order

```
1. PostgresSupport.start()      — postgres:18
2. KeycloakSupport.start()      — keycloak:26.6.1 (realm import from classpath)
3. BackendSupport.start()       — payment-quality/backend:local
                                    env DB_URL → postgres TC URL
                                    env SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI → keycloak TC URL
                                    env SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI → keycloak TC JWKS URL
                                    env EXPECTED_AZP=payment-quality-dashboard
                                    env APP_TESTING_ENABLED=true (enables seed/reset endpoints)
```

---

## 4. Keycloak Strategy

### Realm import

- **File**: `infra/keycloak/realms/payment-quality-realm.json`
- **Realm name**: `payment-quality`
- **Must be on the classpath** when Testcontainers-Keycloak starts (copy to
  `apps/api-tests/src/test/resources/keycloak/` or pass as a File to `KeycloakContainer`).

### Issuer URL and the mismatch problem

The backend validates JWT `iss` claim against:
```
http://localhost:8081/realms/payment-quality
```

When Testcontainers starts Keycloak on a random port (e.g., `32771`), the issuer in tokens becomes:
```
http://localhost:32771/realms/payment-quality
```

This will cause **401 Unauthorized** on every authenticated request unless the backend is configured
at startup with the TC Keycloak's actual mapped port.

**Fix**: Pass the TC Keycloak issuer URI as environment variables to the backend container:

```java
backendContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI",
    keycloakSupport.issuerUri());
backendContainer.withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI",
    keycloakSupport.jwksUri());
```

Where `keycloakSupport.issuerUri()` returns the TC-mapped URL, e.g.:
```
http://localhost:32771/realms/payment-quality
```

**The internal container network alias** is also a risk: Keycloak embeds its own base URL in
tokens using the URL it receives in the request. If the backend container makes JWKS requests
to `http://keycloak:8080/realms/payment-quality/...` (internal alias) but the token `iss` is
`http://localhost:32771/realms/payment-quality`, validation fails.

**Resolution**: always use the TC host-mapped URL for both `ISSUER_URI` and `JWK_SET_URI`
passed to the backend. Do not use container-internal aliases for these two properties.

### Keycloak client for token minting

The realm has one PKCE public client for tokens:

```
clientId: payment-quality-dashboard
publicClient: true
directAccessGrantsEnabled: true  ← ROPC flow enabled for test users
```

The `payment-quality-admin` service-account client is for Keycloak admin API only.

**Token minting in `KeycloakTokenFactory`** (Phase 6B):

```
POST http://<tc-host>:<tc-port>/realms/payment-quality/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password
&client_id=payment-quality-dashboard
&username=<username>
&password=<password>
```

### Available test users in realm

| Username | Password | Role | Status | Notes |
|---|---|---|---|---|
| `platform.admin` | `platform.admin` | `PLATFORM_ADMIN` (composite) | enabled | Platform-wide admin |
| `tenant.admin` | `tenant.admin` | `TENANT_ADMIN` (composite) | enabled | `tenant_id=TENANT_ALPHA` |
| `merchant.manager` | `merchant.manager` | `MERCHANT_MANAGER` (composite) | enabled | `merchant_id=MERCHANT_ALPHA_001`, `tenant_id=TENANT_ALPHA` |
| `support.agent` | `support.agent` | `SUPPORT_AGENT` (composite) | enabled | Read-only cross-tenant |
| `merchant.denied` | `merchant.denied` | *(no roles)* | enabled | For 403/permission tests |
| `merchant.payment.creator` | same | `merchant:payments:create` | **disabled** | PLACEHOLDER_MERCHANT_ID |
| `merchant.payment.reader` | same | `merchant:payments:read` | **disabled** | PLACEHOLDER_MERCHANT_ID |
| `merchant.payment.operator` | same | `merchant:payments:operate` | **disabled** | PLACEHOLDER_MERCHANT_ID |

> **Important for Phase 6B**: Several users have `PLACEHOLDER_MERCHANT_ID` and `PLACEHOLDER_TENANT_ID`
> attributes and are disabled. Only `platform.admin`, `tenant.admin`, `merchant.manager`,
> `support.agent`, and `merchant.denied` are enabled. Token minting will fail for disabled users.

### `Identities` role mismatch — must fix in Phase 6B

The current `Identities.java` uses placeholder role names (`platform:admin`, `platform:tenant_admin`)
that do not exist in the realm. These were correct for Phase 5's placeholder token strategy but
**will produce 401/403 when used with real tokens** unless `Identities` is rewritten to bind to
real Keycloak users by username.

**Phase 6B fix**: replace `TokenFactory.placeholder()` in `Identities` with a
`KeycloakTokenFactory` that accepts a username/password pair:

```java
// After fix:
public static Identity platformAdmin() {
    return Identity.of("platform-admin",
            List.of("PLATFORM_ADMIN"),
            null,
            keycloakFactory.forUser("platform.admin", "platform.admin"));
}
```

---

## 5. First Live Endpoint Strategy

### Recommended first test: `GET /api/status`

**Why**: public endpoint, no auth, no DB write, no Keycloak interaction. Proves the backend
container is running, healthy, and the port mapping is correct.

**Security configuration** (`SecurityConfig.java`, `@Order(1)`):

```java
.securityMatcher("/api/status")
.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
```

No `Authorization` header required. No token minting needed.

**Expected response contract**:

```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "application": "payment-quality-lab",
  "phase": "foundation",
  "status": "UP"
}
```

Source: `StatusController.java` returns `StatusResponse("payment-quality-lab", "foundation", "UP")`.

**No `ETag`, no `Vary`, no `Cache-Control`, no `X-Correlation-ID`** in the status response.
The `sensitive()` `ResponseSpec` must NOT be used for status assertions.

**First live spec** (Phase 6B):

```java
// StatusSpec.java — uses RequestSpecs.anonymous()
@ApiTest
class StatusSpec {

    @Test
    void get_status_returns200_with_expectedBody() {
        RequestSpecs.anonymous()
            .when().get("/api/status")
            .then()
            .statusCode(200)
            .body("application", equalTo("payment-quality-lab"))
            .body("status", equalTo("UP"));
    }
}
```

---

## 6. Proposed Phase 6B Class List

### `core/stack/` package

| Class | Role |
|---|---|
| `StackMode` | Enum: `TESTCONTAINERS`, `EXTERNAL`. Committed as Phase 6A placeholder. |
| `ApiStack` | Singleton coordinator. Resolves `StackMode`, starts/stops containers in order. Exposes `baseUri()`. |
| `PostgresSupport` | Wraps TC `PostgreSQLContainer("postgres:18")`. Runs Flyway migrations? No — backend does Flyway on start. Exposes `jdbcUrl()`, `username()`, `password()`. |
| `KeycloakSupport` | Wraps `dasniko/testcontainers-keycloak:3.7.0`. Mounts realm JSON. Exposes `issuerUri()`, `jwksUri()`, `tokenEndpoint()`. |
| `BackendSupport` | Wraps TC `GenericContainer(BACKEND_IMAGE)`. Wired with postgres + keycloak URLs. Exposes `baseUri()`. |
| `ApiStackExtension` | JUnit `BeforeAllCallback + AfterAllCallback`. Stores `ApiStack` in root extension context store (singleton across classes). |
| `KeycloakTokenFactory` | Implements `TokenFactory`. HTTP client call to Keycloak token endpoint. Caches tokens until near-expiry. Does NOT log token values. |

### `support/` package

| Class | Role |
|---|---|
| `ApiTest` | Composed annotation: `@ExtendWith(ApiStackExtension.class)` + `@Tag("live")` |

### `api/` package

| Class | Role |
|---|---|
| `StatusApi` | Thin client for `GET /api/status`. Single method: `status()` → `Response`. No auth needed. |

### Deferred to Phase 7+

- `MerchantsApi`
- `PaymentOrdersApi`
- `SeedApi` (for `POST /api/test/seed` and `POST /api/test/reset`)
- `ConcurrencyHarness`

---

## 7. Environment Variables for Live Specs

| Variable | Default | Purpose |
|---|---|---|
| `API_BASE_URI` | *(none)* | If set, switches `ApiStack` to `EXTERNAL` mode. No containers started. |
| `BACKEND_IMAGE` | *(none)* | Docker image name for backend container. Required in `TESTCONTAINERS` mode. |

No secrets, tokens, passwords, or DSNs belong in this table or in any committed file.
The Keycloak test user credentials (`platform.admin` / `platform.admin`) are hardcoded in the
realm import JSON (not in Java source) and are for local dev only — never commit production credentials.

---

## 8. Validation Commands

### Offline validation (now, anytime)

```bash
# Must pass with 70 tests and BUILD SUCCESS
cd apps/api-tests && mvn -q test
```

### Future live validation (Phase 6B)

```bash
# Step 1: build backend OCI image
cd apps/backend
./mvnw spring-boot:build-image -DskipTests \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local

# Step 2: run live specs (Failsafe, *Spec.java)
cd apps/api-tests
export BACKEND_IMAGE=payment-quality/backend:local
mvn verify

# Alternative: external stack (compose already running, backend on port 8080)
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
cd apps/backend && ./mvnw spring-boot:run &
cd apps/api-tests
export API_BASE_URI=http://localhost:8080
mvn verify
```

### Compose start commands (current infra)

```bash
# Start Postgres + Keycloak
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d

# Check health
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml ps

# Start backend separately (requires postgres + keycloak running)
cd apps/backend
./mvnw spring-boot:run

# Stop all
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml down
```

---

## 9. Explicit Deferred Work

The following are **not implemented in Phase 6A** and must not be assumed to exist:

| Item | Phase | Blocker |
|---|---|---|
| Live `*Spec.java` test | 6B+ | No stack yet |
| `KeycloakTokenFactory` | 6B | No KC container yet |
| `ApiStack` / `PostgresSupport` / `KeycloakSupport` / `BackendSupport` | 6B | No TC deps uncommented |
| `ApiStackExtension` / `ApiTest` annotation | 6B | Blocked by `ApiStack` |
| `StatusApi` thin client | 6B | Blocked by stack (needs `baseUri()`) |
| `SeedApi` | 7+ | Requires `app.testing.enabled=true` backend + seed data design |
| `MerchantsApi` / `PaymentOrdersApi` | 7+ | Blocked by stack + auth |
| `ConcurrencyHarness` | 7+ | Requires parallel live specs |
| `json-schema-validator` dep | 4+ | Not in local `~/.m2`; needs network |
| `ScopedContextExtension` (ScopedValue) | 6+ | Requires `InvocationInterceptor` wiring |
| Fix `Identities` role names for real tokens | 6B | Placeholder names don't match realm |

---

## 10. Key Risks Before Phase 6B

| Risk | Severity | Mitigation |
|---|---|---|
| **No backend Dockerfile / OCI image** | High | Must run `spring-boot:build-image` before TC specs. Document as prerequisite. |
| **Keycloak issuer URL mismatch** | High | Always wire TC-mapped Keycloak URL into backend container env vars. Never use container-internal aliases for issuer/JWKS. |
| **`platform.admin` AZP validation** | Medium | Backend validates `azp=payment-quality-dashboard`. Token must come from that client. ROPC flow confirmed enabled in realm. |
| **Disabled realm users** | Medium | Only 5 of 12 users are enabled. Disabled users will cause 401 on token endpoint. Phase 6B must only reference enabled users. |
| **Flyway on backend start** | Low | Backend runs Flyway migrations at startup. TC postgres starts fresh — Flyway will run cleanly but adds ~2s to first container start. |
| **`RestAssured.registerParser` global state** | Low | `RestAssuredSetup.install()` writes to `RestAssured` global state. Safe for single-threaded Failsafe run; may need isolation for parallel spec execution. |
| **Backend `app.testing.enabled` flag** | Medium | Seed/reset endpoints only active with `APP_TESTING_ENABLED=true`. Must be set in backend container env. |
| **`PAYMENT_QUALITY_KEYCLOAK_ADMIN_CLIENT_SECRET` placeholder** | Low | `payment-quality-admin` client has `secret: ${PAYMENT_QUALITY_KEYCLOAK_ADMIN_CLIENT_SECRET}` — env var substitution in Keycloak import. If not resolved at import time, the admin client may fail. The `payment-quality-dashboard` PKCE client (used for ROPC token minting) does NOT use a secret and is not affected. |
