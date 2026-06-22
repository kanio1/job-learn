# Requirements Document

## Introduction

This spec defines a **backend-only, test-first, behavior-preserving refactor** of the security/authority layer of the Payment Quality Engineering Lab backend (`apps/backend`, Java 25 / Spring Boot 4 / Spring Framework 7 / Spring Modulith 2.0.6 / Spring Security JWT resource server).

The goal is to apply cleaner patterns and senior-level best practices to four narrow concerns without overengineering:

- **R1 — Typed authority catalog**: replace duplicated authority magic strings with a single compile-time source of truth.
- **R2 — Explicit, test-first converter**: pin the current Keycloak realm-role conversion behavior with characterization tests, then replace the heuristic prefix rule with an explicit allowlist (fail-closed for unknown roles).
- **R5 — Principal claim name**: configure the JWT authentication converter to derive a readable principal name from `preferred_username`.
- **R4 — Token audience / authorized-party validation**: add a token validator so the resource server only accepts tokens intended for this application (the riskiest item, sequenced last).

This refactor introduces **no business behavior changes, no REST contract changes, and no frontend or Playwright work**. Every authority that each endpoint requires today stays identical. The single intentional, documented behavior change is in R2: unknown realm-role names stop producing inert authorities and instead grant nothing (fail-closed).

### Verified Current-State Facts (must not be contradicted)

These facts were confirmed by reading the source and ground the requirements below:

- `shared/security/KeycloakRealmRoleConverter.java` maps each name in the JWT `realm_access.roles` claim to an authority **heuristically**: if a role starts with `merchant:` or `platform:` it passes through unchanged; otherwise it is prefixed with `platform:`. Effect today: `merchants:create` → `platform:merchants:create`; `merchant:*` / `platform:*` roles pass through; and **any unknown role name** (including a future Keycloak composite name like `PLATFORM_ADMIN`) becomes `platform:<name>`.
- `shared/security/SecurityConfig.java` enforces authorities via URL rules (`hasAuthority` / `hasAnyAuthority`) and also enables `@EnableMethodSecurity`.
- `merchant/internal/web/MerchantController.java` **also** enforces the three `platform:merchants:*` authorities via `@PreAuthorize("hasAuthority('...')")`. These authority strings are therefore **duplicated as magic strings** between `SecurityConfig` and `MerchantController`.
- `payment/internal/web/PaymentOrderController.java` does **not** use `@PreAuthorize`; it relies on `SecurityConfig` URL rules plus **programmatic merchant-ownership checks** (`verifyMerchantOwnership`, `isPlatformLifecycle`).
- There is **no existing unit test** for `KeycloakRealmRoleConverter`.
- `application.yml` configures the resource server with only `issuer-uri` + `jwk-set-uri`. There is **no audience / `azp` validation** today; Spring validates issuer + signature + expiry by default.
- The `JwtAuthenticationConverter` uses the default principal claim (`sub`).
- Security tests under `src/test/.../security/` mint JWTs via `TestJwtConfiguration` + `TestJwtSupport`. `TestJwtConfiguration` builds a `NimbusJwtDecoder` validated with `JwtValidators.createDefaultWithIssuer(...)`. These tests must keep passing and must not be weakened.
- The 10 raw realm authority roles are: `merchants:create`, `merchants:read`, `merchants:update-status`, `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, `merchant:payments:lifecycle`, `platform:payments:read`, `platform:payments:lifecycle`, `platform:payments:audit`.

## Glossary

- **Backend**: The `apps/backend` Spring Boot application under refactor.
- **Authority** / **Fine_Grained_Authority**: A Spring Security `GrantedAuthority` string the backend authorizes on, namespaced `platform:*` or `merchant:*` (e.g. `platform:merchants:create`, `merchant:payments:lifecycle`).
- **Raw_Realm_Role**: A role name as it appears in the JWT `realm_access.roles` claim issued by Keycloak (e.g. `merchants:create`, `merchant:payments:read`).
- **Authority_Catalog**: The single typed source of truth (constants class or enum) that holds every fine-grained authority string introduced by R1.
- **Role_Converter**: `KeycloakRealmRoleConverter`, which converts a `Jwt` into a collection of authorities.
- **Known_Role**: One of the 10 documented raw realm roles listed above.
- **Unknown_Role**: Any role name in `realm_access.roles` that is not a Known_Role (includes future Keycloak composite names such as `PLATFORM_ADMIN`).
- **Principal_Name**: The name exposed by the authenticated `Authentication`/`Jwt` principal (the value of the configured principal claim).
- **Token_Validator**: An `OAuth2TokenValidator<Jwt>` added in R4 that asserts a token is intended for this application.
- **azp**: The Keycloak "authorized party" claim carrying the OAuth client id (`payment-quality-dashboard`).
- **aud**: The standard JWT "audience" claim; Keycloak access tokens default `aud` to `account`.
- **Security_Suite**: The existing tests under `src/test/.../security/` that import `TestJwtConfiguration`.
- **Architecture_Tests**: `ModulithArchitectureTest`, `MerchantModuleTest`, `PaymentModuleTest`.
- **Characterization_Test**: A test that pins existing behavior before a refactor, so the refactor can be proven behavior-preserving.

## Personas

- **Backend_Engineer**: Implements the refactor in `apps/backend`, keeps changes small and reviewable, and preserves Spring Modulith boundaries.
- **Security_Reviewer**: Reviews authorization behavior, audience/authorized-party validation, and fail-closed handling of unknown roles.
- **QA_Automation_Engineer**: Authors and maintains characterization and security tests; ensures `./mvnw test` and `./mvnw verify` stay green and that test setup is additive, not weakened.

## In Scope

- A single typed Authority_Catalog as the source of truth for fine-grained authority strings (R1).
- Wiring `SecurityConfig` URL rules and `MerchantController` `@PreAuthorize` expressions to the Authority_Catalog (R1).
- Characterization unit tests for `KeycloakRealmRoleConverter` covering the 10 Known_Roles and Unknown_Role handling (R2).
- Refactoring the Role_Converter from heuristic prefix to an explicit allowlist mapping of the 10 Known_Roles, ignoring Unknown_Roles (R2).
- Configuring the principal claim name to `preferred_username` with a safe fallback (R5).
- Adding a Token_Validator for audience / authorized-party validation, plus additive updates to `TestJwtConfiguration` / `TestJwtSupport` so the Security_Suite stays green (R4).
- Keeping `./mvnw test` and `./mvnw verify` green throughout, including the Security_Suite and Architecture_Tests.

## Out of Scope (Non-Goals)

- Any change to which authority each endpoint requires.
- Any REST contract change (paths, verbs, status codes, headers, request/response bodies).
- Any business-logic change in the merchant or payment modules.
- Any frontend, Nuxt, or Playwright work.
- Removing either enforcement layer (URL rules or `@PreAuthorize`) — keeping both is recommended (see Open Question 4). Collapsing to a single layer is out of scope.
- Extracting URL path patterns / path-pattern constants (only authority strings are catalogued).
- A policy engine, ABAC, or OPA integration.
- Externalized or configuration-driven role mapping.
- A Spring Authorization Server or any token-issuance responsibility.
- A custom permission / annotation framework beyond standard Spring Security.
- Editing the `iam-roles-and-keycloak-login` spec (its needed documentation update is recorded as a note only — see Open Question 5 and Requirement 6).

## Requirements

### Requirement 1: Typed Authority Catalog (R1)

**User Story:** As a Backend_Engineer, I want a single typed source of truth for fine-grained authority strings, so that authorization rules and method annotations cannot drift out of sync through duplicated magic strings.

#### Acceptance Criteria

1. THE Backend SHALL define an Authority_Catalog in one location that declares every fine-grained authority string as a compile-time constant (a class with `public static final String` members, or an enum exposing the authority string).
2. THE Authority_Catalog SHALL contain exactly the fine-grained authority strings currently enforced by the Backend: `platform:merchants:create`, `platform:merchants:read`, `platform:merchants:update-status`, `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:lifecycle`, `platform:payments:read`, `platform:payments:lifecycle`, `platform:payments:audit`.
3. THE SecurityConfig SHALL reference Authority_Catalog constants in every `hasAuthority` and `hasAnyAuthority` URL rule instead of inline string literals.
4. THE MerchantController SHALL reference Authority_Catalog constants in every `@PreAuthorize` expression, using SpEL string concatenation against the compile-time constant (for example `@PreAuthorize("hasAuthority('" + AuthorityCatalog.MERCHANTS_CREATE + "')")`).
5. WHEN the refactor is complete, THE Backend SHALL require the identical authority for every endpoint that the pre-refactor Backend required, with no change to any URL rule mapping or method-security expression outcome.
6. THE Authority_Catalog SHALL reside in a package that both the merchant module, the payment module, and `shared/security` may depend on without violating the Architecture_Tests.
7. WHEN the Authority_Catalog placement is chosen, THE Backend SHALL keep `ModulithArchitectureTest`, `MerchantModuleTest`, and `PaymentModuleTest` passing under `./mvnw verify`.
8. WHERE removing duplicated authority enforcement layers is considered, THE refactor SHALL retain both the URL-rule layer and the `@PreAuthorize` layer (layer removal is out of scope per Open Question 4).

### Requirement 2: Explicit, Test-First Role Converter (R2)

**User Story:** As a Security_Reviewer, I want the realm-role conversion to be an explicit, tested allowlist that ignores unknown roles, so that only documented roles can ever grant authorities and inert authorities are eliminated.

#### Acceptance Criteria

1. THE QA_Automation_Engineer SHALL add Characterization_Tests for `KeycloakRealmRoleConverter` before the converter is refactored.
2. THE Characterization_Tests SHALL assert the current authority produced for each of the 10 Known_Roles: `merchants:create` → `platform:merchants:create`, `merchants:read` → `platform:merchants:read`, `merchants:update-status` → `platform:merchants:update-status`, and `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, `merchant:payments:lifecycle`, `platform:payments:read`, `platform:payments:lifecycle`, `platform:payments:audit` each mapping to an identically named authority.
3. THE Characterization_Tests SHALL document the current Unknown_Role behavior (an Unknown_Role named `X` currently produces authority `platform:X`).
4. WHEN a `realm_access` claim is absent or is not a map, THE Role_Converter SHALL return an empty authority collection.
5. WHEN a `roles` entry under `realm_access` is absent or is not a collection, THE Role_Converter SHALL return an empty authority collection.
6. WHEN the Role_Converter receives a Known_Role, THE Role_Converter SHALL produce the same authority that the pre-refactor converter produced for that role (as enumerated in Acceptance Criterion 2).
7. IF the Role_Converter receives an Unknown_Role, THEN THE Role_Converter SHALL ignore that role and produce no authority for it.
8. THE refactored Role_Converter SHALL derive its produced authorities from the Authority_Catalog (Requirement 1) so the allowlist and the catalog share one source of truth.
9. WHEN the converter refactor is complete, THE Security_Suite SHALL remain green under `./mvnw test` and `./mvnw verify` without weakening any existing assertion.

### Requirement 3: Behavior and Contract Preservation (Cross-Cutting)

**User Story:** As a Security_Reviewer, I want a hard guarantee that this refactor changes no externally observable behavior except the documented unknown-role change, so that I can approve it as low-risk.

#### Acceptance Criteria

1. THE Backend SHALL preserve every REST contract unchanged: paths, HTTP methods, status codes, response headers, and request/response body shapes.
2. THE Backend SHALL preserve all merchant and payment business logic unchanged.
3. WHEN any endpoint is called with a token carrying only Known_Roles, THE Backend SHALL grant or deny access identically to the pre-refactor Backend.
4. THE refactor SHALL introduce no frontend, Nuxt, or Playwright change.
5. IF an Unknown_Role would previously have produced an inert `platform:<name>` authority, THEN THE Backend SHALL instead produce no authority for that role, AND this SHALL be the only intended behavior change in this spec.
6. THE Backend SHALL preserve Spring Modulith boundaries and introduce no cross-module internal package imports.

### Requirement 4: Principal Claim Name (R5)

**User Story:** As a Backend_Engineer, I want the authenticated principal to be a readable username, so that logs and authorization context show a human-meaningful identity instead of an opaque subject id.

#### Acceptance Criteria

1. THE Backend SHALL configure the `JwtAuthenticationConverter` to use `preferred_username` as the principal claim name.
2. WHEN a validated token contains a `preferred_username` claim, THE Backend SHALL expose that value as the Principal_Name.
3. IF a validated token does not contain a `preferred_username` claim, THEN THE Backend SHALL fall back to a safe principal value without raising an error.
4. THE principal claim change SHALL NOT alter the authorities derived for any token (authorization behavior stays identical).
5. THE R5 change SHALL be limited to principal-name configuration and SHALL NOT introduce additional claim processing beyond the principal name and its fallback.

### Requirement 5: Token Audience / Authorized-Party Validation (R4)

**User Story:** As a Security_Reviewer, I want the resource server to reject tokens not intended for this application, so that tokens minted for other clients cannot be replayed against this Backend.

#### Acceptance Criteria

1. THE Backend SHALL register an `OAuth2TokenValidator<Jwt>` (the Token_Validator) on the resource-server JWT decoder.
2. WHEN a token satisfies the configured audience / authorized-party rule, THE Token_Validator SHALL accept the token (subject to the existing issuer, signature, and expiry checks).
3. IF a token does not satisfy the configured audience / authorized-party rule, THEN THE Token_Validator SHALL reject the token so the request is treated as unauthorized.
4. THE Token_Validator SHALL be composed with the existing default validators (issuer + timestamp) rather than replacing them.
5. THE QA_Automation_Engineer SHALL update `TestJwtConfiguration` / `TestJwtSupport` additively so existing Security_Suite tokens satisfy the Token_Validator, without weakening any existing security assertion.
6. WHEN the Token_Validator is active, THE Security_Suite SHALL remain green under `./mvnw test` and `./mvnw verify`.
7. THE R4 work SHALL be sequenced after R1, R2, and R5 and SHALL be identified as the highest-risk item in this spec.
8. WHERE the audience-validation strategy is undecided, THE Backend SHALL resolve Open Question 2 (validate `azp` equals `payment-quality-dashboard`, or add a Keycloak audience protocol mapper and validate `aud`) before implementing the Token_Validator.

### Requirement 6: Cross-Spec Impact Record (Cross-Cutting)

**User Story:** As a Security_Reviewer, I want the cross-spec effect of the converter change recorded, so that the `iam-roles-and-keycloak-login` spec stays accurate without this spec editing it.

#### Acceptance Criteria

1. THE Backend_Engineer SHALL record that Requirement 2 removes the "inert authority" behavior that the `iam-roles-and-keycloak-login` spec relies on (its design.md Decision 1, discrepancy A, and Property 1 & Property 9 wording assume Unknown_Roles become inert `platform:<name>` authorities).
2. THE record SHALL be captured as a note / open question in this spec only and SHALL NOT modify the `iam-roles-and-keycloak-login` spec files.
3. WHEN R2 changes Unknown_Role handling, THE record SHALL flag that the `iam-roles-and-keycloak-login` documentation needs a small follow-up update to describe ignored (not inert) unknown roles.

## Open Questions

1. **Authority_Catalog placement vs Modulith boundaries (R1).** Where should the Authority_Catalog live so that `shared/security`, the merchant module, and the payment module can all reference it without tripping `ModulithArchitectureTest` / `MerchantModuleTest` / `PaymentModuleTest`? Candidate: a shared published package (e.g. under `shared/security` or a dedicated shared authority package). The exact placement must be confirmed against the Architecture_Tests during design.

2. **R4 azp-vs-audience-mapper decision and realm coupling.** Keycloak access tokens default `aud` to `account`, while `azp` carries the client id `payment-quality-dashboard`. Option A: validate `azp == payment-quality-dashboard` (no realm change). Option B: add a Keycloak audience protocol mapper to the realm and validate `aud` contains the resource-server audience. Option B couples to `infra/keycloak/realms/payment-quality-realm.json` (which the `iam-roles-and-keycloak-login` spec also edits) — this coupling must be flagged and decided before implementation.

3. **R4 test-token / profile strategy.** Should the Token_Validator be active in all profiles (including `test`), with `TestJwtConfiguration` / `TestJwtSupport` updated additively so test tokens carry the required `azp`/`aud`? Or should it be profile-scoped? The decision must keep the Security_Suite green without weakening assertions.

4. **Double enforcement (URL rules + `@PreAuthorize`).** Recommendation: keep both layers; R1 de-risks the duplication by binding both to the Authority_Catalog. Removing a layer is marked out of scope. Confirm this recommendation stands.

5. **Cross-spec documentation follow-up.** R2 invalidates the inert-authority assumption in `iam-roles-and-keycloak-login` (design.md Decision 1, discrepancy A, Property 1, Property 9). Confirm that the follow-up documentation update to that spec is handled separately (not in this spec) per Requirement 6.
