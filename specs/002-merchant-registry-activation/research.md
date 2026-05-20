# Research: Phase 1 — Merchant Registry and Activation

**Date**: 2026-05-18 | **Spec**: [spec.md](./spec.md)

Phase 1 introduces the first real business domain module. Several technical unknowns from Phase 0 must be resolved.

## Decision 1: Keycloak Authority Mapping

**Decision**: Implement a custom `Converter<Jwt, Collection<GrantedAuthority>>` that extracts Keycloak realm roles from `realm_access.roles` and maps them to `platform:*` prefix authorities.

**Rationale**: Spring Security's default `JwtGrantedAuthoritiesConverter` only reads `scope`/`scp` claims. Keycloak's JWT structure nests roles in `realm_access.roles` (JSON array inside an object), which the default converter cannot traverse. A custom converter is the only reliable approach.

**Alternatives**: None viable. Setting `authoritiesClaimName` to `realm_access` doesn't work because the claim is a JSON object, not a flat string list.

**Authority mapping**:
- Keycloak realm role `merchants:create` → Spring `platform:merchants:create`
- Keycloak realm role `merchants:read` → Spring `platform:merchants:read`
- Keycloak realm role `merchants:update-status` → Spring `platform:merchants:update-status`

## Decision 2: Nuxt PKCE OAuth2 Client

**Decision**: Use `nuxt-auth-utils` with its built-in Keycloak provider for an Authorization Code Flow with PKCE using the Phase 1 public Keycloak client.

**Rationale**: `nuxt-auth-utils` is maintained by the Nuxt core team, has a built-in Keycloak provider, supports a server-mediated OAuth session where tokens are not exposed to browser JavaScript, and integrates naturally with Nuxt 4 hybrid rendering. PKCE is used during the authorization code exchange with the public dashboard client.

**Alternatives considered**:
- `keycloak-js` directly: Not Nuxt-aware, no SSR support, manual composable wrapping needed.
- `oidc-client-ts`: Certified OIDC, but Nuxt-agnostic, requires manual integration.
- `@sidebase/nuxt-auth`: Auth.js-based, heavier dependency, Keycloak support via generic OIDC only.

**Caveat**: Phase 1 uses a public dashboard client with PKCE and a server-mediated Nuxt session, not a machine-to-machine client. If a pure browser-token model or merchant machine-to-machine flow is later required, evaluate `oidc-client-ts` or a dedicated confidential client design for that later phase.

## Decision 3: Database Migrations

**Decision**: Use Flyway with `flyway-database-postgresql` module.

**Rationale**: SQL-based migrations are directly readable and testable. Linear versioning avoids dependency-graph complexity. Module-level migration directories (`db/migration/merchant/`) map naturally to the modular monolith. Spring Boot auto-configuration with `spring.flyway.*` requires zero manual wiring. The `flyway-database-postgresql` module provides PostgreSQL 18 dialect support.

**Alternatives considered**:
- Liquibase: XML/YAML changelogs add abstraction; module-level changelogs require `include` chaining, which is harder to trace in a modular monolith.
- Raw JDBC: No migration history tracking; complexity scales poorly with evolution.

## Decision 4: Testcontainers PostgreSQL 18

**Decision**: Use `org.testcontainers:testcontainers-postgresql:2.0.5` (BOM-managed) with `@ServiceConnection` and static containers per test class, plus `@Transactional` rollback within classes.

**Rationale**: Official Testcontainers for Java PostgreSQL module documentation for 2.0.5 lists Maven artifact `org.testcontainers:testcontainers-postgresql:2.0.5`. `@ServiceConnection` auto-wires datasource, Flyway, and JDBC details. Static containers with unique database names per class (`withDatabaseName("merchant_test_" + UUID)`) provide class-level isolation without per-method overhead. `@Transactional` provides method-level rollback within a class.

**Alternatives considered**:
- Per-method containers: Too slow for repeated runs.
- Reusable containers with unique schemas: Complex, shared-container failures cascade.

**Dependencies required**:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

## Decision 5: Spring Modulith @ApplicationModuleTest

**Decision**: Introduce `@ApplicationModuleTest` for the `merchant` module now. It is the first real business module with domain behavior, persistence, and lifecycle transitions.

**Rationale**: The project's architecture docs already state `@ApplicationModuleTest` is deferred "until a real module owns behavior." The merchant module satisfies this. Module-scoped tests will verify the module boots in isolation with its direct dependencies.

**Approach**:
- Use `BootstrapMode.STANDALONE` since merchant has no upstream module dependencies in Phase 1.
- Create `lab.paymentquality.merchant.MerchantModuleTest` using `@ApplicationModuleTest`.
- Verify context loads, repository works, controller responds, and architecture verification passes within the module scope.

## Decision 6: Persistence Layer

**Decision**: Use Spring Data JPA with Hibernate for merchant persistence.

**Rationale**: The merchant entity is straightforward (single table, simple CRUD, lifecycle state transitions). Spring Data JPA provides repository abstraction, declarative query methods, and audit support (`@CreatedDate`, `@LastModifiedDate`) that directly map to the spec's timestamp requirements. It integrates naturally with Flyway migrations and Testcontainers.

**Alternatives considered**:
- jOOQ: Excellent for complex queries, but overkill for Phase 1's simple CRUD.
- Raw JDBC: Too much boilerplate for a learning lab; doesn't teach realistic Spring patterns.

## Decision 7: Duplicate Reference Handling

**Decision**: Store the normalized (uppercase, trimmed) merchant reference in a separate column with a unique database constraint, plus application-level validation.

**Rationale**: The spec requires uniqueness on the normalized form and case-insensitive comparison. A separate `normalized_reference` column with a `UNIQUE` constraint provides database-level enforcement. Application code normalizes input before both validation and persistence. Combined with `@Version` for optimistic locking, this handles near-simultaneous duplicate attempts via constraint violation detection.

## Decision 8: Frontend Route Structure

**Decision**: Use a layout-based approach with a shared dashboard layout wrapping `/admin/*` routes.

**Approach**:
- New `layouts/dashboard.vue` with sidebar navigation and topbar.
- New `middleware/auth.global.ts` for global auth guard.
- New `pages/login.vue` for the Keycloak PKCE redirect.
- New `pages/admin/merchants.vue` for the merchant registry.
- New `stores/auth.ts` Pinia store for sanitized browser-safe auth state only; raw session/token access remains server-side.
- New `schemas/merchant.schema.ts` Zod schema for merchant form validation.

No other administrative routes in Phase 1. The existing `pages/index.vue` becomes a redirect to `/admin/merchants` when authenticated.
