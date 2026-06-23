# Phase 6B-lite — Implementation Checklist

Companion to `REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` and `PHASE_6A_STACK_DISCOVERY.md`.
Scope: **Testcontainers deps + stack skeleton + first live spec** (unauthenticated only).

Legend: `[x]` done this run · `[ ]` deferred to Phase 6B-full or later

---

## Current Run Findings

| Question | Finding |
|---|---|
| TC 2.0.5 artifact IDs | **Changed from TC 1.x**: `postgresql` → `testcontainers-postgresql`; `junit-jupiter` → `testcontainers-junit-jupiter`. All three JARs in `~/.m2`. |
| `dasniko/testcontainers-keycloak` in `~/.m2`? | **No.** Deferred to Phase 6B-full. |
| `PostgreSQLContainer` generic? | **No.** TC 2.0.5 defines `PostgreSQLContainer extends JdbcDatabaseContainer<PostgreSQLContainer>` — no type parameter on the class itself. Use raw type. |
| Backend OCI image present? | **No** — `mvn verify` skipped; user must build image before running live specs. |
| `mvn test` (offline) still passes? | **Yes** — 4 test classes, 70 methods, no containers, no network. EXIT:0 confirmed on review run. |
| Spring Boot JWT startup behavior | Both `issuer-uri` and `jwk-set-uri` configured. Spring Boot prefers explicit `jwk-set-uri` for the decoder; JWKS endpoint is NOT fetched at startup — only on authenticated requests. Placeholder Keycloak URLs are safe for 6B-lite (public endpoint only). |

---

## Live Validation Status

| Run | Date | Result | Blocker |
|---|---|---|---|
| Phase 6B-lite implementation | 2026-06-23 | `mvn test` ✅ 70/70 · `mvn verify` ⛔ skipped | Backend OCI image absent |
| Phase 6B-lite review run | 2026-06-23 | `mvn test` ✅ 70/70 · `mvn verify` ⛔ skipped | Backend OCI image absent |

### Exact blocker

```
podman images | grep payment-quality   → no output (image does not exist)
```

### Exact next fix — build the backend image

```bash
# Fedora rootless Podman setup (once per session):
systemctl --user enable --now podman.socket
export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
export TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true

# Build the backend OCI image (from apps/backend):
cd apps/backend
./mvnw spring-boot:build-image -DskipTests \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local

# Run live specs (from apps/api-tests):
cd apps/api-tests
export BACKEND_IMAGE=payment-quality/backend:local
mvn verify
```

Expected result after image build: `StatusSpec` runs via Failsafe, backend + postgres
containers start, `GET /api/status` → 200, test passes.

---

## Phase 6B-lite scope

### 1. Testcontainers dependencies (`pom.xml`)

- [x] Uncomment `org.testcontainers:testcontainers` (BOM-managed)
- [x] Uncomment `org.testcontainers:testcontainers-postgresql` (TC 2.0.5 artifact ID, BOM-managed)
- [x] Uncomment `org.testcontainers:testcontainers-junit-jupiter` (TC 2.0.5 artifact ID, BOM-managed)
- [x] Keep `com.github.dasniko:testcontainers-keycloak` commented — not in `~/.m2`, deferred to 6B-full
- [x] Keep `org.awaitility:awaitility` commented — deferred to 6B-full
- [x] Fix in-comment artifact IDs to TC 2.0.x names

### 2. Stack skeleton (`core/stack/`)

- [x] `PostgresSupport` — wraps raw `PostgreSQLContainer("postgres:18")`
  - [x] Joins shared `Network` with alias `postgres-db`
  - [x] `jdbcUrl()` (host-mapped, for test JVM)
  - [x] `internalJdbcUrl()` (`jdbc:postgresql://postgres-db:5432/...` for backend container)
  - [x] `username()` / `password()` — reads from `payment_quality` defaults
- [x] `BackendSupport` — wraps `GenericContainer(BACKEND_IMAGE)`
  - [x] Joins shared TC `Network`
  - [x] `DB_URL` → `postgres.internalJdbcUrl()` (container-to-container via TC network)
  - [x] `DB_USER`, `DB_PASSWORD` from `PostgresSupport`
  - [x] Placeholder `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` / `JWK_SET_URI`
  - [x] `APP_TESTING_ENABLED=true`
  - [x] Wait strategy: `Wait.forHttp("/api/status").forStatusCode(200)`, 120 s timeout
  - [x] `baseUri()` → host-mapped URL for REST Assured
- [x] `ApiStack` — singleton coordinator
  - [x] Resolves `StackMode.EXTERNAL` when `API_BASE_URI` env var is set
  - [x] Resolves `StackMode.TESTCONTAINERS` otherwise — requires `BACKEND_IMAGE` env var
  - [x] Creates `Network.newNetwork()`, starts Postgres then Backend in order
  - [x] Clear `IllegalStateException` message when `BACKEND_IMAGE` is missing
  - [x] `stop()` tears down Backend → Postgres → Network in order
  - [x] `baseUri()`, `stackMode()` accessors

### 3. Support layer

- [x] `ApiStackExtension` — implements `BeforeAllCallback`
  - [x] Calls `ApiStack.get()` then `RestAssuredSetup.install(stack.baseUri())`
  - [x] Stores `StackResource` in root extension store (singleton across spec classes)
  - [x] `StackResource implements CloseableResource` → calls `stack.stop()` on session close
- [x] `ApiTest` — meta-annotation
  - [x] `@ExtendWith(ApiStackExtension.class)`
  - [x] `@Tag("live")`
  - [x] Javadoc: only for `*Spec.java` (Failsafe); never on `*Test.java`

### 4. API client

- [x] `StatusApi` — thin client for `GET /api/status`
  - [x] `status() → Response` using `RequestSpecs.anonymous()`
  - [x] No auth header (public endpoint)

### 5. First live spec

- [x] `scenarios/StatusSpec` annotated `@ApiTest`
  - [x] `get_status_returns_200_with_expected_body()` — asserts `200`, `application/json`,
        `application=payment-quality-lab`, `status=UP`, `phase=foundation`

---

## Validation

### Offline (always, no containers)

```bash
cd apps/api-tests && mvn -q test
```

Expected: BUILD SUCCESS, 4 test classes, ~69 methods, no containers.

### Live (requires backend image + Podman/Docker)

```bash
# Step 1: build backend OCI image
cd apps/backend
./mvnw spring-boot:build-image -DskipTests \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local

# Step 2: run live specs (Failsafe, *Spec.java via mvn verify)
cd apps/api-tests
export BACKEND_IMAGE=payment-quality/backend:local
mvn verify
```

Podman (Fedora, rootless):
```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
export TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true
```

External mode (compose + backend already running):
```bash
docker compose --env-file infra/compose/.env -f infra/compose/compose.yml up -d
cd apps/backend && ./mvnw spring-boot:run &
# Then in api-tests:
export API_BASE_URI=http://localhost:8080
cd apps/api-tests && mvn verify
```

---

## Deferred to Phase 6B-full

| Item | Blocker |
|---|---|
| `KeycloakSupport` — wraps `dasniko/testcontainers-keycloak` | Not in `~/.m2`; needs network download |
| `KeycloakTokenFactory` — ROPC token minting | Blocked by `KeycloakSupport` |
| Update `Identities` to use real Keycloak users | Blocked by `KeycloakTokenFactory` |
| `SeedApi` — `POST /api/test/seed` and reset | Requires `APP_TESTING_ENABLED=true` + auth |
| Authenticated specs (`MerchantsSpec`, `PaymentLifecycleSpec`, etc.) | Blocked by real tokens |

---

## Key design decisions recorded

- **TC 2.0.x artifact IDs differ from 1.x** — update any docs referencing old names.
- **Shared TC `Network`** — only way for backend container to reach postgres container. The `PostgreSQLContainer.getJdbcUrl()` returns the host-mapped URL (test JVM only); `internalJdbcUrl()` uses the network alias for container-to-container connectivity.
- **Placeholder Keycloak URLs** — Spring Boot JWT auto-config with explicit `jwk-set-uri` does not eagerly validate Keycloak at startup. Safe until authenticated specs are added.
- **Root extension store** — `ApiStackExtension` stores the stack at root scope so all `@ApiTest` classes share one container lifecycle. Class-scope would start containers per spec class.
- **`mvn test` stays offline** — `StatusSpec` is a `*Spec.java` (Failsafe), never picked up by Surefire. New stack classes compile cleanly without starting containers.
