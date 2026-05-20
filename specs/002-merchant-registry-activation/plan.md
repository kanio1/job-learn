# Implementation Plan: Merchant Registry and Activation for Platform Operators

**Branch**: `002-merchant-registry-activation` | **Date**: 2026-05-18 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-merchant-registry-activation/spec.md`

## 1. Technical Summary

Phase 1 establishes the first real business domain capability for the Payment Quality Engineering Lab: a merchant registry and lifecycle foundation managed by authenticated platform operators through a protected REST API and a minimal Nuxt 4 dashboard at `/admin/merchants`.

The work introduces:

- A new `merchant` Spring Modulith module with domain entity, JPA repository, application service, and REST controller
- PostgreSQL-backed persistence with Flyway migrations and Spring Data JPA
- Flyway-based schema migration creating the `merchants` table with uniqueness constraint on normalized merchant references
- Spring Security Resource Server integration with Keycloak 26.6.1 for JWT-based authentication and platform-operator authorization
- A Keycloak realm import defining the local `payment-quality` realm with deterministic platform operator and denial-path identities
- A Nuxt PKCE OAuth2 login flow, authenticated dashboard layout, and the `/admin/merchants` page with merchant CRUD and lifecycle actions
- Auth middleware for route guarding and a Pinia auth store for sanitized browser-safe auth state
- Test layering: domain unit tests, application service tests, JPA repository tests, Spring Modulith `@ApplicationModuleTest`, REST Assured API tests with token injection, Testcontainers PostgreSQL integration tests, and Playwright authenticated E2E journeys
- Parallel-safe merchant test data using namespaced references (`MERCH-{testRunId}-{workerId}-{uuid}`)
- Architecture verification enforcing the `merchant` module boundary — no payment, PSP, or Kafka dependencies ever cross into it

The backend remains a modular monolith built on Java 25, Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6, Maven 3.9.11, PostgreSQL 18, and Keycloak 26.6.1. The frontend continues on Nuxt 4.4.6, Nuxt UI 4.7.1, TypeScript 6, Zod, Pinia, and Playwright 1.60.

No payment order creation, PSP integration, Kafka, settlement, reconciliation, Client Credentials Flow, KYC, currency rules, or self-service merchant portal is implemented.

## 2. Architecture Decisions

### Merchant Module Ownership

The `merchant` module is the first real business module beyond the Phase 0 technical `foundation` module. It owns merchant lifecycle, validation, and future eligibility concepts. Phase 1 exposes that behavior only through REST endpoints; future payment capabilities may depend on merchant eligibility through a root-package public API when a concrete cross-module use case exists, never by directly accessing internal packages or data.

### Backend Package Structure

```
lab.paymentquality.merchant         ← module root (module declaration only in Phase 1)
lab.paymentquality.merchant.internal ← internal: domain, repo, DTOs
```

The `merchant` root package contains the module's `package-info.java` with `@ApplicationModule(displayName = "Merchant Registry")`. Internal sub-packages (`domain`, `infrastructure`, `web`) are inaccessible to other modules. No cross-module public API types are introduced in Phase 1.

### Layering within the Module

- **Domain**: `Merchant` JPA entity, `MerchantStatus` enum with transition logic, `MerchantReference` value object
- **Application**: `MerchantService` under `merchant.internal.application` — orchestrates creation, activation, suspension, and duplicate checking
- **Infrastructure**: `JpaMerchantRepository` (Spring Data JPA) interface
- **Web**: `MerchantController` — REST endpoints, `MerchantRequest`/`MerchantResponse` DTOs, validation

No separate `api` and `spi` sub-modules are introduced. The module root is reserved as the future public API boundary, while Phase 1 keeps implementation types internal. Spring Modulith enforces this at compile/test time.

### Persistence

Spring Data JPA with Hibernate. Flyway for versioned SQL migrations. The `merchants` table stores a `normalized_reference` column (uppercase, trimmed) with a database `UNIQUE` constraint. A `version` column provides optimistic locking for concurrent update scenarios. The `flyway` migration directory follows the modular pattern: `db/migration/merchant/V1__create_merchants.sql`.

### Security Integration

Spring Security acts as a Resource Server validating JWT access tokens. A custom `KeycloakRealmRoleConverter` extracts Keycloak realm roles from `realm_access.roles`, for example `merchants:create`, and maps them to Spring authorities such as `SimpleGrantedAuthority("platform:merchants:create")`. Endpoint authorization uses `@PreAuthorize("hasAuthority('platform:merchants:create')")`. Missing auth → `401`, wrong authority → `403`.

`GET /api/status` is explicitly permitted for all requests. All merchant endpoints require authentication plus the matching authority.

### Frontend Auth & Routing

`nuxt-auth-utils` with its built-in Keycloak provider handles the Authorization Code Flow with PKCE using the Phase 1 public Keycloak client. The Nuxt server mediates the exchange and stores session state so access tokens are not exposed to browser JavaScript. A global auth middleware redirects unauthenticated users to Keycloak login. After login, the user lands on `/admin/merchants`. A Pinia `auth` store holds only sanitized browser-safe auth state.

### Testing Strategy

- **Unit**: `MerchantReference` validation, `MerchantStatus` transitions, `MerchantService` logic (mocked repository)
- **Repository**: `@DataJpaTest` with Testcontainers PostgreSQL, verifying Flyway migrations and constraint behavior
- **Application module**: `@ApplicationModuleTest` bootstrapping only the merchant module
- **REST API**: REST Assured with test-issued JWT tokens, covering all status codes, validation errors, and authorization denial paths
- **Security**: Dedicated HTTP tests using locally generated signed JWTs for 401/403/200 scenarios; missing-authentication, malformed-authentication, and insufficient-authority behavior must be verified at the HTTP boundary
- **Playwright**: Authenticated browser journeys covering login, create merchant, activate, suspend, form validation, empty state, duplicate feedback
- **Architecture**: `ApplicationModules.verify()` runs on every test execution, now catching illegal coupling to or from the merchant module
- **Observability**: `CorrelationIdFilter` generates or propagates `X-Correlation-ID`; focused filter-level coverage (header generation, MDC propagation, response header) is desirable if it fits naturally during implementation

Test data uses namespaced references. No test relies on a globally shared merchant record. Playwright tests use `storageState` for authenticated sessions.

## 3. Repository Structure Changes

Additions under existing Phase 0 monorepo layout (no reorganization):

```text
apps/backend/
├── pom.xml                                          # Modified: add security, JPA, Flyway, org.testcontainers:testcontainers-postgresql, org.testcontainers:testcontainers-junit-jupiter, spring-boot-testcontainers
├── src/main/java/lab/paymentquality/
│   ├── merchant/
│   │   ├── package-info.java                        # @ApplicationModule(displayName = "Merchant Registry")
│   │   └── internal/
│   │       ├── domain/
│   │       │   ├── Merchant.java                    # JPA entity
│   │       │   ├── MerchantStatus.java              # Enum: DRAFT, ACTIVE, SUSPENDED
│   │       │   └── MerchantReference.java           # Value object with validation
│   │       ├── application/
│   │       │   └── MerchantService.java             # Application service
│   │       ├── infrastructure/
│   │       │   └── JpaMerchantRepository.java       # Spring Data JPA interface
│   │       └── web/
│   │           ├── MerchantController.java          # REST controller
│   │           ├── CreateMerchantRequest.java        # DTO
│   │           ├── MerchantResponse.java             # DTO
│   │           └── MerchantMapper.java              # Entity ↔ DTO
│   └── shared/
│       └── security/
│           ├── SecurityConfig.java                  # SecurityFilterChain + JWT config
│           └── KeycloakRealmRoleConverter.java      # Custom authority converter
├── src/main/resources/
│   ├── application.yml                              # Modified: datasource, flyway, security properties
│   └── db/migration/merchant/
│       └── V1__create_merchants.sql                 # Flyway migration
├── src/test/java/lab/paymentquality/
│   ├── merchant/
│   │   ├── MerchantModuleTest.java                  # @ApplicationModuleTest
│   │   ├── internal/
│   │       ├── domain/
│   │       │   ├── MerchantReferenceTest.java       # Unit: validation rules
│   │       │   └── MerchantStatusTest.java          # Unit: transition logic
│   │       ├── application/
│   │       │   └── MerchantServiceTest.java         # Application service with mocks
│   │       ├── infrastructure/
│   │       │   └── JpaMerchantRepositoryTest.java   # @DataJpaTest with Testcontainers
│   │       └── web/
│   │           └── MerchantControllerTest.java      # Controller unit test
│   ├── rest/
│   │   └── MerchantRestAssuredTest.java             # REST Assured with auth
│   └── security/
│       └── MerchantSecurityTest.java               # 401/403/200 authorization matrix

apps/frontend/
├── package.json                                     # Modified: add nuxt-auth-utils
├── nuxt.config.ts                                   # Modified: auth module config
├── app/
│   ├── app.vue                                      # Modified: add NuxtLayout wrapping
│   ├── middleware/
│   │   └── auth.global.ts                           # Global auth guard
│   ├── layouts/
│   │   └── dashboard.vue                            # Dashboard shell with sidebar
│   ├── pages/
│   │   ├── login.vue                                # PKCE redirect trigger
│   │   └── admin/
│   │       └── merchants.vue                        # Merchant registry page
│   ├── schemas/
│   │   └── merchant.schema.ts                       # Zod: merchant form validation
│   ├── stores/
│   │   └── auth.ts                                  # Pinia auth store
│   └── components/
│       └── merchant/
│           ├── MerchantTable.vue                    # Table with status badges
│           ├── CreateMerchantForm.vue               # Create form with validation
│           └── MerchantStatusBadge.vue              # Status pill component
├── tests/e2e/
│   ├── merchant-create.spec.ts                      # Playwright: create, form validation, duplicate feedback
│   ├── merchant-lifecycle.spec.ts                   # Playwright: activate and suspend journey
│   ├── auth-deny.spec.ts                            # Playwright: unauthenticated and unauthorized denial
│   └── merchant-feedback.spec.ts                    # Playwright: loading and error feedback
└── tests/
    └── auth/
        └── auth.setup.ts                            # Playwright auth fixture setup

infra/
├── keycloak/
│   └── realms/
│       └── payment-quality-realm.json               # Realm: roles, clients, users
└── compose/
    └── compose.yml                                  # Modified: realm import volume

docs/
├── setup/
│   ├── phase-1-merchant-orientation-pack.md          # Tester orientation
│   └── keycloak-local-auth.md                       # Keycloak setup guide
└── testing/
    └── phase-1-merchant-test-design.md               # Test design notes

knowledge-vault/
├── 01 Projects/Payment_Quality_Engineering_Lab/
│   └── 01 Phase 1 - Merchant Registry/              # Phase 1 project hub note
└── 02 Areas/
    ├── Technical Learning/Spring Modulith/
    │   └── Merchant Module Architecture.md
    └── Business Product and Testing Thinking/
        └── Phase 1 Test Design.md
```

## 4. Backend Merchant Module Plan

### Application Service Operations

Phase 1 exposes merchant behavior through REST endpoints only. The `MerchantService` is an internal application service in `lab.paymentquality.merchant.internal.application` used by the REST controller; no cross-module public merchant API is introduced until a later payment capability needs merchant eligibility or ownership lookups.

| Operation | Method | Description |
|---|---|---|
| `create(merchantReference, displayName)` | — | Validates, normalizes reference, assigns UUID, persists with `DRAFT` status |
| `findById(merchantId)` | — | Returns merchant or empty |
| `listFirstPage()` | — | Returns first page of up to 50, newest-first with deterministic tie-breaker |
| `activate(merchantId)` | — | Transition `DRAFT → ACTIVE`, updates timestamp |
| `suspend(merchantId)` | — | Transition `ACTIVE → SUSPENDED`, updates timestamp |

The `MerchantService` application class is the sole coordinator. Controllers delegate to it. No business logic lives in controllers or repositories. Display names are trimmed server-side before validation/persistence so API clients cannot bypass the trimming rule by skipping frontend validation. Database unique-constraint failures for `normalized_reference` are translated to the same duplicate-conflict outcome as application-level duplicate pre-checks.

### Internal Domain Model

- `Merchant`: JPA entity annotated with `@Entity`, `@Table(name = "merchants")`. Fields: `merchantId` (`UUID`, `@Id`), `normalizedReference` (`String`, `@Column(unique = true)`), `displayName` (`String`), `status` (`@Enumerated(STRING)`), `createdAt`, `updatedAt` (`Instant`), `version` (`@Version`). The entity exposes domain methods: `activate()`, `suspend()`, which encapsulate the transition rules via `MerchantStatus.canTransitionTo()`. No setters for business fields — state changes go through domain methods.
- `MerchantStatus`: Java enum implementing the state machine. Method `canTransitionTo(MerchantStatus target)` encodes the valid transition rules. Only `DRAFT → ACTIVE` and `ACTIVE → SUSPENDED` are valid.
- `MerchantReference`: Java record with a static factory `from(String raw)`. Rejects null, trims and uppercases input, then validates via regex: `^[A-Z0-9][A-Z0-9-]{1,62}[A-Z0-9]$` (uppercase, letters/numbers/hyphens, 3-64 chars, no leading/trailing hyphen). Invalid input throws a domain exception (`InvalidMerchantReferenceException`), which the web layer maps to `400` with validation details.
- `DisplayName`: Java record with a static factory `from(String raw)`. Rejects null, trims input, validates post-trim length 2-120, and returns the trimmed value. Invalid input throws `InvalidDisplayNameException`, which the web layer maps to `400` with validation details.

### Repository Boundary

`JpaMerchantRepository` extends `JpaRepository<Merchant, UUID>`. Declarative methods:

- `Optional<Merchant> findByNormalizedReference(String ref)` — duplicate check
- `List<Merchant> findAllByOrderByCreatedAtDescMerchantIdAsc(Pageable pageable)` — first-page list with deterministic tie-breaker; `Pageable` is used only as an internal limit/sort mechanism, not exposed as Phase 1 API pagination

Spring Data JPA handles the query derivation. No custom SQL needed for Phase 1.

### DTOs and Mapping

- `CreateMerchantRequest`: record with `@NotBlank @Size(max=64) merchantReference`, `@NotBlank @Size(min=2, max=120) displayName`. Validation happens at the controller layer via `@Valid`. The application layer also validates via `MerchantReference.from()` and `DisplayName.from()` so post-trim display-name length is enforced for non-browser API clients.
- `MerchantListResponse`: record wrapping `List<MerchantResponse> merchants` for the first-page list endpoint.
- `MerchantResponse`: record mirroring entity fields for API output. Mapped by `MerchantMapper.toResponse(Merchant)`.
- `ErrorResponse`: record with `error` (code) and `message` (human-readable) fields. Used for 400, 404, 409 responses.

### Lifecycle Transition Handling

`MerchantService.activate(merchantId)`:
1. Load merchant by ID (or throw `MerchantNotFoundException` → 404)
2. Check transition validity via `merchant.getStatus().canTransitionTo(ACTIVE)` (false → throw `InvalidTransitionException` → 409)
3. Call `merchant.activate()` — sets status to `ACTIVE`, updates `updatedAt` to `now()`
4. Save and flush

Same pattern for `suspend()`. The entity own the transition rules — service only orchestrates.

### Error Handling

A merchant-scoped `@RestControllerAdvice(assignableTypes = MerchantController.class)` in `lab.paymentquality.merchant.internal.web` handles merchant domain exceptions. It must not live in `shared` because shared code cannot depend on merchant internals:

| Exception | HTTP Status | Error Code |
|---|---|---|
| `InvalidMerchantReferenceException` | 400 | `validation` |
| `InvalidDisplayNameException` | 400 | `validation` |
| `MethodArgumentTypeMismatchException` for malformed UUID path variables | 400 | `validation` |
| `DuplicateMerchantReferenceException` | 409 | `duplicate_merchant_reference` |
| `MerchantNotFoundException` | 404 | `not_found` |
| `InvalidTransitionException` | 409 | `invalid_transition` |
| `MethodArgumentNotValidException` | 400 | `validation` |
| `AccessDeniedException` | 403 | (Spring default) |
| `AuthenticationException` | 401 | (Spring default) |

## 5. Merchant Domain Model

### Merchant Entity

```java
@Entity
@Table(name = "merchants")
public class Merchant {
    @Id
    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "normalized_reference", length = 64, nullable = false, unique = true)
    private String normalizedReference;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MerchantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    // No-arg constructor for JPA
    protected Merchant() {}

    // Factory method
    public static Merchant create(UUID merchantId, String normalizedReference, String displayName) {
        var m = new Merchant();
        m.merchantId = merchantId;
        m.normalizedReference = normalizedReference;
        m.displayName = displayName;
        m.status = MerchantStatus.DRAFT;
        m.createdAt = Instant.now();
        m.updatedAt = m.createdAt;
        return m;
    }

    public void activate() {
        if (!status.canTransitionTo(MerchantStatus.ACTIVE)) {
            throw new InvalidTransitionException(status, MerchantStatus.ACTIVE);
        }
        this.status = MerchantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        if (!status.canTransitionTo(MerchantStatus.SUSPENDED)) {
            throw new InvalidTransitionException(status, MerchantStatus.SUSPENDED);
        }
        this.status = MerchantStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    // Getters...
}
```

### Responsibility Distribution

| Layer | Responsibility |
|---|---|
| `MerchantReference` (domain value object) | Input normalization (trim + uppercase), regex validation |
| `Merchant` (domain entity) | Lifecycle transitions, state encapsulation, `activate()`/`suspend()` |
| `MerchantService` (application) | Orchestration: create → normalize → check duplicate → persist; activate/suspend → load → validate → save |
| `MerchantController` (web) | HTTP mapping, `@Valid` input validation, authority check delegation |
| `JpaMerchantRepository` (infrastructure) | Data access, duplicate lookup, list query |

## 6. PostgreSQL 18 Plan

### Database Migration

Flyway manages the schema. Migration file at `src/main/resources/db/migration/merchant/V1__create_merchants.sql`:

```sql
CREATE TABLE merchants (
    merchant_id          UUID PRIMARY KEY,
    normalized_reference VARCHAR(64) NOT NULL,
    display_name         VARCHAR(120) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version              BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_merchants_status CHECK (status IN ('DRAFT', 'ACTIVE', 'SUSPENDED')),
    CONSTRAINT uk_merchants_normalized_reference UNIQUE (normalized_reference)
);

CREATE INDEX idx_merchants_status ON merchants (status);
CREATE INDEX idx_merchants_created_at ON merchants (created_at DESC, merchant_id ASC);
```

### Design Decisions

- **`merchantId` is a UUID generated by the application**, not by the database. This ensures the ID is available for the response without a round-trip.
- **`normalized_reference` stores the canonical uppercase form.** The raw input is normalized by `MerchantReference.from()` before persistence. No raw `merchant_reference` column is stored — all reads return the normalized form. If later phases need original-case display, a column can be added.
- **The `UNIQUE` constraint on `normalized_reference` provides database-level duplicate enforcement.** Application-level duplicate checking (`findByNormalizedReference`) provides a nice error message; the constraint is the safety net for concurrent inserts.
- **`created_at DESC, merchant_id ASC` composite index** supports the spec's stable newest-first ordering with deterministic tie-breaker.
- **`version` column enables optimistic locking.** If two platform operators update the same merchant simultaneously, the second update detects the version mismatch and fails.
- **`CHECK` constraint enforces the status enum at the database level.** Application logic in `MerchantStatus` is the primary enforcement; the constraint is defense-in-depth.
- **No generated or functional indexes** are needed for Phase 1 access patterns. The two indexes cover the spec's list-by-creation and future filter-by-status needs.

### Flyway Configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration/merchant
    baseline-on-migrate: false
```

### Test Data Isolation

Testcontainers provides a fresh PostgreSQL 18 container per test class. Each test class gets a unique database name: `.withDatabaseName("merchant_test_" + UUID.randomUUID().toString().substring(0, 8))`. Flyway runs the migration at context startup. `@Transactional` with rollback isolates test methods within the class. No global shared test database.

## 7. REST API Plan

Full API contracts are documented in [contracts/merchant-api.md](./contracts/merchant-api.md). Summary:

| Method | Path | Authority | Status Codes |
|---|---|---|---|
| `POST` | `/api/merchants` | `platform:merchants:create` | `201`, `400`, `409` |
| `GET` | `/api/merchants/{id}` | `platform:merchants:read` | `200`, `400`, `404` |
| `GET` | `/api/merchants` | `platform:merchants:read` | `200` |
| `POST` | `/api/merchants/{id}/activate` | `platform:merchants:update-status` | `200`, `400`, `404`, `409` |
| `POST` | `/api/merchants/{id}/suspend` | `platform:merchants:update-status` | `200`, `400`, `404`, `409` |
| `GET` | `/api/status` | _public_ | `200` |

All protected endpoints return `401` for missing/invalid tokens and `403` for tokens lacking the required authority. A merchant-scoped `@RestControllerAdvice` maps merchant domain exceptions to the error response contract (`{ "error": "...", "message": "..." }`) without creating a shared-to-merchant dependency.

Duplicate merchant references return `409` with error code `duplicate_merchant_reference`. Invalid lifecycle transitions return `409` with `invalid_transition`. Validation failures return `400` with `validation` and field-level `details`.

## 8. Security and Keycloak Plan

### Backend: Spring Security Resource Server

Dependencies added to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

Application configuration:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081/realms/payment-quality
          jwk-set-uri: http://localhost:8081/realms/payment-quality/protocol/openid-connect/certs
```

Security configuration:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/status").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/merchants").hasAuthority("platform:merchants:create")
                .requestMatchers(HttpMethod.GET, "/api/merchants").hasAuthority("platform:merchants:read")
                .requestMatchers(HttpMethod.GET, "/api/merchants/**").hasAuthority("platform:merchants:read")
                .requestMatchers(HttpMethod.POST, "/api/merchants/*/activate").hasAuthority("platform:merchants:update-status")
                .requestMatchers(HttpMethod.POST, "/api/merchants/*/suspend").hasAuthority("platform:merchants:update-status")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
```

The custom `KeycloakRealmRoleConverter` extracts `realm_access.roles` from the JWT and maps them:

```java
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        var realmAccessClaim = jwt.getClaims().get("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return List.of();
        }

        var rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(role -> new SimpleGrantedAuthority("platform:" + role))
            .collect(Collectors.toList());
    }
}
```

Keycloak realm roles (e.g., `merchants:create`) are mapped to Spring Security authorities with prefix `platform:`.

### Keycloak: Realm Import

A new realm file at `infra/keycloak/realms/payment-quality-realm.json` defines:

- **Realm**: `payment-quality`
- **Client**: `payment-quality-dashboard` (public client, PKCE, redirect to `http://localhost:3000/*`)
- **Realm roles**: `merchants:create`, `merchants:read`, `merchants:update-status`
- **Users**:
  - `platform.operator` — password `platform.operator`, realm roles: `merchants:create`, `merchants:read`, `merchants:update-status`
  - `merchant.denied` — password `merchant.denied`, no realm roles

Compose service updated with realm import:

```yaml
payment-quality-keycloak:
  volumes:
    - ../keycloak/realms/payment-quality-realm.json:/opt/keycloak/data/import/realm.json
  command:
    - start-dev
    - --import-realm
```

### Frontend: Auth Integration

`nuxt-auth-utils` with Keycloak provider handles the Authorization Code Flow with PKCE using the public `payment-quality-dashboard` client. Configuration in `nuxt.config.ts` references the Keycloak realm. The library manages the server-mediated session cookie, token refresh, and login/logout redirects without exposing access tokens to browser JavaScript. A global auth middleware (`middleware/auth.global.ts`) protects all routes except `/login`. After successful login, the user is redirected to `/admin/merchants`.

Merchant dashboard calls use concrete Nuxt server-side API handlers. `apps/frontend/server/utils/backendApi.ts` reads the `nuxt-auth-utils` session access token server-side and attaches it as a bearer token to backend `/api/merchants` calls. Browser components call only the Nuxt server API routes under `apps/frontend/server/api/merchants/`; the backend still receives a JWT bearer token and enforces authorities, while browser JavaScript does not receive the access token directly.

The Pinia `auth` store exposes only sanitized browser-safe state such as `isAuthenticated`, `user`, `login()`, and `logout()`. It must never store access tokens, refresh tokens, raw session objects, or authorization headers. Token reading is limited to Nuxt server-side handlers such as `apps/frontend/server/utils/backendApi.ts`.

## 9. Frontend Nuxt 4 Plan

### Route Structure

| Route | Component | Auth Required |
|---|---|---|
| `/login` | `pages/login.vue` | No (redirects if already authenticated) |
| `/admin/merchants` | `pages/admin/merchants.vue` | Yes |

### Layout

A new `layouts/dashboard.vue` provides the shell: sidebar with navigation (Merchants as sole active item in Phase 1), topbar with user info and logout, and a `<slot />` for page content. The existing `app.vue` wraps pages with `<NuxtLayout>`.

### `/admin/merchants` Page

This single-page component handles the full merchant registry journey:

- **Empty state**: Shown when the merchant list is empty. Displays a prompt to create the first merchant.
- **Merchant table**: Uses Nuxt UI `UTable` component. Columns: Reference, Display Name, Status (badge-colored: gray for DRAFT, green for ACTIVE, red for SUSPENDED), Created At.
- **Create action**: A Nuxt UI modal or slideover with a form. Fields: merchant reference, display name. Shows per-field validation errors. Shows toast on success. Shows inline error on duplicate reference conflict.
- **Activate action**: Button per row (visible for DRAFT merchants). Confirms and transitions to ACTIVE.
- **Suspend action**: Button per row (visible for ACTIVE merchants). Confirms and transitions to SUSPENDED.
- **Loading state**: Skeleton table while data loads.
- **Error state**: Generic error message with retry if the API call fails.

### Zod Validation

`schemas/merchant.schema.ts`:

```typescript
export const createMerchantSchema = z.object({
  merchantReference: z.string()
    .trim()
    .min(3, 'Reference must be at least 3 characters')
    .max(64, 'Reference must be at most 64 characters')
    .regex(/^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$/, 'Invalid reference format'),
  displayName: z.string()
    .trim()
    .min(2, 'Name must be at least 2 characters')
    .max(120, 'Name must be at most 120 characters'),
})
```

### Pinia Auth Store

`stores/auth.ts` holds only sanitized browser-safe state such as `{ isAuthenticated, user }`. It is initialized from Nuxt session-derived user information on app load, but it never stores raw session objects, access tokens, refresh tokens, or authorization headers. It exposes `login()` and `logout()` actions that delegate to `nuxt-auth-utils` composables.

### Playwright Accessibility

Components use semantic HTML and Nuxt UI primitives with accessible labels. Test locators target `getByRole`, `getByLabel`, and `getByText` patterns. No `data-testid` attributes unless a specific interaction cannot be targeted by role/label.

## 10. Testing Strategy

### Backend Test Layers

| Layer | Test Class | Tech | Purpose |
|---|---|---|---|
| Domain unit | `MerchantReferenceTest` | JUnit 6 + AssertJ | Reference validation: BVA (2-char, 3-char, 63-char, 65-char), EP (valid, invalid chars, leading/trailing hyphens) |
| Domain unit | `MerchantStatusTest` | JUnit 6 + AssertJ | State transitions: all 3×3 combinations, decision table for valid/invalid |
| Application | `MerchantServiceTest` | JUnit 6 + Mockito | Service orchestration with mocked repository: create, activate, suspend, duplicate checking |
| Repository | `JpaMerchantRepositoryTest` | `@DataJpaTest` + Testcontainers | Persistence: CRUD, unique constraint, ordering, Flyway migration verification |
| Module | `MerchantModuleTest` | `@ApplicationModuleTest` + `@ActiveProfiles("test")` + Testcontainers when DataSource beans are required | Module bootstrap: context loads, repository/service/controller beans present, architecture verification within module scope; secured HTTP behavior is covered by REST/security tests |
| REST API | `MerchantRestAssuredTest` | REST Assured + `@SpringBootTest` | Full HTTP stack: status codes, response bodies, validation errors, duplicate conflict |
| Security | `MerchantSecurityTest` | `@SpringBootTest` + token injection | 401/403 matrix: unauthenticated, `merchant.denied` (no authority), `platform.operator` (full access) |
| Architecture | `ModulithArchitectureTest` | Spring Modulith | Existing test extended: verifies merchant module boundary, no payment/PSP/Kafka dependency |

### Frontend Test Layers

| Test | File | Purpose |
|---|---|---|
| Authenticated create journey | `tests/e2e/merchant-create.spec.ts` | Login → empty state → create merchant → verify in table with DRAFT status; blank-field and duplicate-reference feedback |
| Auth denial | `tests/e2e/auth-deny.spec.ts` | Unauthenticated access attempts, denied-operator access attempts |
| Lifecycle journey | `tests/e2e/merchant-lifecycle.spec.ts` | Create draft merchant → activate → verify status → suspend → verify status |
| Feedback states | `tests/e2e/merchant-feedback.spec.ts` | Loading state and service error state are rendered and recoverable |

Playwright uses `storageState` from a setup project to authenticate once per worker. The auth setup file at `tests/auth/auth.setup.ts` performs the Keycloak login flow and saves the session state.

### Test Data Strategy

All test data uses namespaced merchant references: `MERCH-{testRunId}-{workerId}-{uuid}`. No test depends on a globally shared merchant record. REST Assured tests create their own merchants. Playwright tests use unique references per scenario.

### Test Design Techniques Applied

| Technique | Where |
|---|---|
| Equivalence Partitioning | Valid/invalid reference characters, display name lengths |
| Boundary Value Analysis | Reference length 2/3/63/64/65, display name length 1/2/119/120/121 |
| State Transition Testing | All 3×3 MerchantStatus combinations |
| Decision Tables | Authorization matrix (actor × operation × expected status) |
| Negative Testing | Missing auth, wrong auth, expired tokens, malformed IDs |
| Concurrency | Near-simultaneous duplicate reference creation coordinated with `ExecutorService` and `CountDownLatch`, plus repository unique-constraint coverage |
| Exploratory Charters | UI edge cases, restart durability, port conflicts with auth |

### WireMock

Not applicable in Phase 1. No external service calls exist.

## 11. Spring Modulith Strategy

### Module Introduction

The `merchant` module is introduced as a direct sub-package of `lab.paymentquality`. Its `package-info.java` declares:

```java
@ApplicationModule(displayName = "Merchant Registry")
package lab.paymentquality.merchant;
```

### Public vs Internal Boundary

- **Public**: The `lab.paymentquality.merchant` module root contains the module declaration only in Phase 1. No cross-module API types are exposed yet.
- **Internal**: Everything under `lab.paymentquality.merchant.internal`. This includes `merchant.internal.application.MerchantService`, `MerchantResponse`, `Merchant` entity, `JpaMerchantRepository`, `MerchantController`, and domain implementation.
- **Deferred**: A future payment phase may introduce root-package public API types for merchant eligibility or ownership lookups when another module has a concrete dependency.

### Architecture Verification

The existing `ModulithArchitectureTest` continues to run `ApplicationModules.of(PaymentQualityApplication.class).verify()`. With the merchant module added, it now enforces:

1. No cyclic dependencies between `foundation`, `shared`, and `merchant`
2. No code in `foundation` or `shared` accessing `merchant.internal.*`
3. No code in `merchant` accessing `foundation.internal.*` or `shared.internal.*`

### Future Payment Dependency

When the `payment` module is introduced later, it will depend on the merchant module's public API for eligibility checks — never directly on `merchant.internal`. The plan documents this intention so the architecture test catches violations immediately.

### @ApplicationModuleTest

Introduced now with `MerchantModuleTest`. Boots the merchant module in `STANDALONE` mode with the `test` profile and a PostgreSQL Testcontainer via `@ServiceConnection` when repository/DataSource beans are required. Verifies:
- Module context starts
- Repository, service, and controller beans are available
- Architecture verification passes within the scoped context

Full secured HTTP endpoint behavior is covered by REST Assured and security tests rather than the module-scope test.

### Module Documentation

Not generated in Phase 1. The module canvas requires PlantUML rendering and would add build complexity without meaningful benefit while only one business module exists. Human-written architecture notes are sufficient. Generated documentation will be valuable when `payment`, `merchant`, and other modules co-exist.

## 12. Documentation and Learning Outputs

| Deliverable | Path | Purpose |
|---|---|---|
| Keycloak local auth guide | `docs/setup/keycloak-local-auth.md` | Step-by-step for local auth setup, realm import, test identities |
| Phase 1 tester orientation | `docs/setup/phase-1-merchant-orientation-pack.md` | What exists, what is absent, how to run, test charters |
| Test design notes | `docs/testing/phase-1-merchant-test-design.md` | Test conditions, techniques, data strategy, negative cases |
| Phase 1 Obsidian hub | `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/01 Phase 1 - Merchant Registry/` | Project hub note linking to spec, plan, technical learning notes |
| Modulith architecture note | `knowledge-vault/02 Areas/Technical Learning/Spring Modulith/Merchant Module Architecture.md` | How the first business module was introduced |
| Test design note | `knowledge-vault/02 Areas/Business Product and Testing Thinking/Phase 1 Test Design.md` | Learnings from applying BVA, EP, state transition, decision tables |

## 13. Risks and Open Questions

| Risk | Mitigation |
|---|---|
| `nuxt-auth-utils` Keycloak provider compatibility with Nuxt 4.4.6 | Verify provider works with Keycloak 26.6.1 during the early auth spike. If compatibility issues require `oidc-client-ts` or a different auth architecture, pause implementation and return to `/speckit.plan` before changing the design. |
| JWT issuer URI connectivity in local lab | Keycloak runs on `localhost:8081`; ensure `issuer-uri` is resolvable from the Spring Boot process |
| `@ApplicationModuleTest` compatibility with Spring Boot 4 Testcontainers integration | Test early; if evidence during implementation shows `@ApplicationModuleTest` is not viable in the planned shape, pause and return to `/speckit.plan` rather than silently replacing it with `@SpringBootTest` |
| Flyway `locations` not picking up the `merchant` subdirectory | Verify `classpath:db/migration/merchant` is resolved correctly; use `classpath*:db/migration/**` as fallback |
| Deterministic Playwright auth across parallel workers | Use `storageState` + `fullyParallel: false` for Phase 1 to keep the first Keycloak-authenticated dashboard tests stable; keep test data worker-safe so frontend parallelism can be enabled in Phase 2+ |
| Keycloak dev mode data loss on restart affecting test identity persistence | Dev mode with H2 is ephemeral; realm import on each startup ensures identities exist |

## 14. Definition of Done

Phase 1 is done when:

- Backend compiles and `./mvnw test` passes with all unit, application, repository, REST, security, and architecture tests
- `./mvnw verify` passes with Testcontainers PostgreSQL integration tests
- Flyway `V1__create_merchants.sql` migration runs on a fresh PostgreSQL 18 instance
- `POST /api/merchants` creates a merchant with `DRAFT` status and returns `201`
- `GET /api/merchants/{id}` returns `200` for known IDs and `404` for unknown
- `GET /api/merchants` returns up to 50 merchants in newest-first order
- `POST /api/merchants/{id}/activate` transitions `DRAFT → ACTIVE` and rejects invalid transitions with `409`
- `POST /api/merchants/{id}/suspend` transitions `ACTIVE → SUSPENDED` and rejects invalid transitions with `409`
- Unauthenticated requests to merchant endpoints return `401`
- `merchant.denied` requests to merchant endpoints return `403`
- `GET /api/status` remains public and unchanged
- Keycloak realm import creates `platform.operator` and `merchant.denied` users on startup
- Frontend builds and `pnpm typecheck` passes
- Frontend login redirects to Keycloak, authenticates, and lands on `/admin/merchants`
- Merchant registry page shows empty state, create form, table, activate/suspend actions, and feedback states
- Playwright tests cover the complete authenticated merchant journey and auth denial scenarios
- Spring Modulith `ApplicationModules.verify()` passes with the `merchant` module present
- Documentation is updated: root README, tester orientation pack, Keycloak setup, test design notes
- No payment order creation, payment status read model, PSP, Kafka, settlement, reconciliation, or client credentials behavior exists

## 15. Implementation Readiness Assessment

The specification is clarified and all material product decisions are resolved. Tasks were generated and a final cross-artifact analysis found no blocker. The plan covers all required areas.

The feature is ready for `/speckit.implement`.
