# Requirements Document

## Introduction

This feature makes tenant isolation **real and enforced** in the Payment Quality Engineering Lab backend. It is the direct follow-up to the `iam-roles-and-keycloak-login` spec, which introduced a `tenant_id` JWT claim and a `TENANT_ADMIN` composite role but deliberately left `tenant_id` **informational only** (its Requirement 4.7: the backend applies no tenant-scoped authorization rule). This spec closes that security gap.

This is a **brownfield enhancement**. The user has explicitly chosen the **enterprise-grade model**: a tenant is modeled as a **full database entity** (the pattern used by platforms such as Stripe, Adyen, and Braintree), persisted via Flyway with foreign-key constraints, for maximum educational value around multi-tenant authorization and isolation testing. Today there is **no tenant concept anywhere** in the backend: there are zero `tenant` references in the Flyway migrations or schema, the `Merchant` entity has no tenant field, and authorization is **merchant-scoped, not tenant-scoped**.

Verified current-state facts that this spec MUST NOT contradict:

- The `merchants` table (`db/migration/merchant/V1__create_merchants.sql`) has columns `merchant_id` (UUID PK), `normalized_reference`, `display_name`, `status` (values `DRAFT`, `ACTIVE`, `SUSPENDED`), `created_at`, `updated_at`, `version`. There is no tenant column.
- `MerchantController` enforces `platform:merchants:create|read|update-status` via both `SecurityFilterChain` URL rules and `@PreAuthorize`. It performs **no** tenant filtering: any holder of `platform:merchants:read` currently sees all merchants.
- `PaymentOrderController.verifyMerchantOwnership(...)` and its read paths compare the JWT `merchant_id` claim against the path `merchantId`. Holders of `platform:*` payment authorities bypass merchant scope and see everything; merchant-scoped users are limited to their single `merchant_id`. Cross-scope **reads** are masked as `404` (`PaymentOrderNotFoundException`); cross-scope **writes** raise `AccessDeniedException` → `403`.
- The `iam-roles` Test User Catalog assigns literal string `tenant_id` user attributes: `PLATFORM_TENANT` for platform-scoped roles, `TENANT_ALPHA` for tenant-scoped roles, and the shared `PLACEHOLDER_TENANT_ID` for legacy users. The `merchant.manager` user additionally carries `merchant_id = MERCHANT_ALPHA_001`.
- The realm import is `infra/keycloak/realms/payment-quality-realm.json`. Flyway migration locations are `classpath:db/migration/merchant` and `classpath:db/migration/payment`. JPA runs with `ddl-auto: validate`, so the schema is owned by Flyway and JPA mappings must match it exactly.

The enforcement model added here is **additive**: it layers a tenant-boundary check **on top of** the existing authority and merchant-scope checks, analogous to the existing merchant-ownership check. No fine-grained authority required by any endpoint changes, and the existing REST contracts, headers, status codes, and the existing merchant-scope behavior for merchant-scoped users are preserved.

Tenant is treated as a first-class domain concept. This spec introduces a dedicated Spring Modulith module `tenant` (package `lab.paymentquality.tenant`) that owns the Tenant entity and exposes a PUBLIC module API for tenant lookup and resolution; the `merchant` module depends only on that PUBLIC API. Principal classification is **data-driven** via a `tenant_type` column on the `tenants` table (values `PLATFORM` and `STANDARD`): a principal is platform-scoped if and only if its resolved Tenant has `tenant_type = PLATFORM`. Platform-initiated merchant creation uses an **additive, optional** `tenantReference` request-body field (required only for platform-scoped principals, ignored for tenant-scoped principals); this is the single controlled contract addition in this spec.

## Glossary

- **Backend_API**: The Payment Quality Lab Spring Boot REST API exposed under `/api/*`, including its Spring Security authorization rules and service layer.
- **Tenant**: A persisted top-level account boundary that owns one or more Merchants. A Tenant is a real database entity with a UUID primary key.
- **Tenant_Id**: The UUID primary key of a Tenant record (`tenant_id`).
- **Tenant_Reference**: A stable, human-readable natural-key string that uniquely identifies a Tenant record (for example `TENANT_ALPHA`). The Tenant_Reference is the value carried by the JWT `tenant_id` claim.
- **Tenant_Status**: The lifecycle state of a Tenant. Allowed values are `ACTIVE` and `SUSPENDED`.
- **Tenant_Type**: The classification of a Tenant recorded in the `tenants.tenant_type` column. Allowed values are `PLATFORM` and `STANDARD`. A Tenant_Type of `PLATFORM` marks the platform-administration boundary; `STANDARD` marks an ordinary tenant.
- **Tenant_Module**: The Spring Modulith module introduced by this spec (package `lab.paymentquality.tenant`) that owns the Tenant entity and exposes a PUBLIC module API for tenant lookup and resolution.
- **Tenant_Claim**: The `tenant_id` claim present in the JWT access token, holding a Tenant_Reference string (introduced by the `iam-roles-and-keycloak-login` spec).
- **Merchant**: The existing merchant entity (`merchants` table). Each Merchant belongs to exactly one Tenant after this spec.
- **Merchant_Tenant_Association**: The foreign-key relationship from a Merchant to its owning Tenant (`merchants.tenant_id`).
- **Payment_Order**: The existing merchant-scoped payment order entity. A Payment_Order derives its Tenant transitively through its owning Merchant; it carries no Tenant column of its own.
- **Tenant_Migration**: The Flyway migration(s) introduced by this spec that create the `tenants` table, add the `merchants.tenant_id` foreign-key column, seed required Tenant records, and backfill existing Merchants.
- **Default_Tenant**: A seeded Tenant whose Tenant_Reference equals the legacy literal `PLACEHOLDER_TENANT_ID`, used to backfill Merchants that existed before this spec.
- **Authenticated_Principal**: The Spring Security authentication produced from a validated JWT, carrying granted authorities, the optional `merchant_id` claim, and the optional Tenant_Claim.
- **Platform_Scoped_Principal**: An Authenticated_Principal whose resolved Tenant has Tenant_Type `PLATFORM`. A Platform_Scoped_Principal is entitled to cross-tenant visibility and is not restricted to a single Tenant.
- **Tenant_Scoped_Principal**: An Authenticated_Principal whose resolved Tenant has Tenant_Type `STANDARD`. A Tenant_Scoped_Principal is restricted to the single Tenant identified by its Tenant_Claim.
- **Tenant_Boundary_Check**: The additive authorization rule introduced by this spec that, for a Tenant_Scoped_Principal, permits access only to resources owned by the Tenant matching the principal's Tenant_Claim.
- **Masked_Not_Found**: The existing behavior of returning a `404 Not Found` with the standard `not_found` problem shape for a resource that exists but is not visible to the principal, so that existence is not disclosed.
- **Fine_Grained_Authority**: An existing low-level permission string enforced by the Backend_API (for example `platform:merchants:read`, `merchant:payments:read`, `platform:payments:read`). The complete set is unchanged by this spec.
- **Keycloak_Realm**: The `payment-quality` Keycloak realm defined by `infra/keycloak/realms/payment-quality-realm.json`.

## Personas

- **Platform_Operator**: A platform-level user who manages merchants across all tenants and reads platform-wide data. Holds the `PLATFORM_ADMIN` composite role and is a Platform_Scoped_Principal.
- **Tenant_Admin_User**: A user who administers the merchants of a single tenant. Holds the `TENANT_ADMIN` composite role and is a Tenant_Scoped_Principal bound to one Tenant.
- **Merchant_User**: A user scoped to a single merchant who creates and operates payment orders within that merchant, which itself belongs to one Tenant.
- **Read_Only_Viewer**: A user who only views lists and details with no write capability, scoped to a single Tenant.
- **QA_Automation_Engineer**: Writes future REST Assured and Playwright tests; needs deterministic tenant fixtures, an explicit isolation matrix, and predictable cross-tenant outcomes (403 for writes, masked 404 for reads).

## In Scope

- A `tenants` table created via Flyway migration: `tenant_id` (UUID PK), `tenant_reference` (unique natural key), `name`, `status` (`ACTIVE`/`SUSPENDED`), `tenant_type` (`PLATFORM`/`STANDARD`), `created_at`. A real, minimal entity owned by the new Tenant_Module.
- A foreign-key `tenant_id` column on `merchants` associating each Merchant with exactly one Tenant, created via Flyway migration, with backfill of pre-existing Merchants to the Default_Tenant.
- A new Spring Modulith module `tenant` (package `lab.paymentquality.tenant`) that owns the Tenant entity and exposes a PUBLIC module API for tenant lookup and resolution, consumed by the `merchant` module through its published API only.
- Resolution of the JWT Tenant_Claim (a Tenant_Reference string such as `TENANT_ALPHA`) to a persisted Tenant record by the `tenant_reference` natural key, and validation that the claim corresponds to an existing `ACTIVE` Tenant.
- A data-driven rule that distinguishes a Platform_Scoped_Principal from a Tenant_Scoped_Principal using the resolved Tenant's `tenant_type`.
- Enforced Tenant_Boundary_Check on merchant endpoints (list, detail, create, and status-change operations): a Tenant_Scoped_Principal sees and acts only within its own Tenant; cross-tenant reads yield Masked_Not_Found (`404`) and cross-tenant writes yield `403`.
- An additive, optional `tenantReference` field on the create-merchant request body, required for a Platform_Scoped_Principal and ignored for a Tenant_Scoped_Principal, used to assign explicit tenant ownership to platform-created Merchants.
- Retained cross-tenant visibility for a Platform_Scoped_Principal, with an optional `?tenantId=` filter parameter to narrow merchant reads to a single Tenant.
- Transitive tenant isolation on payment order endpoints, enforced through the Merchant_Tenant_Association (a Tenant_Scoped_Principal cannot reach payment orders of a Merchant in another Tenant).
- Preservation of the existing merchant-scope behavior, the existing platform-scope behavior, all REST contracts and headers, and a green `./mvnw test` and `./mvnw verify`.
- Seeding of Tenant records whose Tenant_Reference values match the literals already carried by the existing test users (`TENANT_ALPHA`, `PLATFORM_TENANT`, `PLACEHOLDER_TENANT_ID`), with `PLATFORM_TENANT` seeded as `tenant_type = PLATFORM` and `TENANT_ALPHA` and the Default_Tenant seeded as `tenant_type = STANDARD`.

## Out of Scope (Non-Goals)

- Tenant CRUD / tenant-management UI or API endpoints (deferred to a separate `tenant-management` spec).
- A separate `tenant_id` column directly on `payment_orders` (Tenant is always derived through the Merchant_Tenant_Association).
- Any Playwright or other automated frontend test files (conceptual frontend impact only, consistent with prior specs).
- Changing which Fine_Grained_Authority each endpoint requires (authorities are unchanged; this adds a Tenant_Boundary_Check on top).
- Keycloak realm changes beyond what is strictly required to make the Tenant_Claim reflect real, seeded tenant references (any coupling to the realm import is captured in Resolved Decisions). Modeling tenants as Keycloak groups (the `iam-roles` Decision 4 forward note) is deferred.
- A policy engine, ABAC, or OPA integration.
- PSP integration, Kafka, webhooks, settlement, or any item in the project-wide active non-goals.

## Requirements

### Requirement 1: Tenant Database Entity

**User Story:** As a Platform_Operator, I want tenants persisted as real database records, so that merchant ownership and tenant isolation rest on a verifiable source of truth.

#### Acceptance Criteria

1. THE Tenant_Migration SHALL create a `tenants` table with columns `tenant_id` (UUID, primary key), `tenant_reference` (string, not null), `name` (string, not null), `status` (string, not null), `tenant_type` (string, not null), and `created_at` (timestamp, not null).
2. THE Tenant_Migration SHALL define a unique constraint on `tenant_reference` so that each Tenant_Reference identifies at most one Tenant record.
3. THE Tenant_Migration SHALL constrain the `tenants.status` column to the values `ACTIVE` and `SUSPENDED`.
4. THE Tenant_Migration SHALL constrain the `tenants.tenant_type` column to the values `PLATFORM` and `STANDARD`.
5. THE Backend_API SHALL map the Tenant entity to the `tenants` table with JPA mappings that pass startup schema validation while `ddl-auto` is set to `validate`.
6. WHEN the Backend_API starts against a database where the Tenant_Migration has run, THE Backend_API SHALL complete Flyway migration and JPA schema validation without error.

### Requirement 2: Merchant-to-Tenant Association

**User Story:** As a Platform_Operator, I want every merchant linked to exactly one tenant, so that tenant ownership of merchants and their payment orders is unambiguous.

#### Acceptance Criteria

1. THE Tenant_Migration SHALL add a `tenant_id` column to the `merchants` table with a foreign-key constraint referencing `tenants.tenant_id`.
2. THE Tenant_Migration SHALL seed a Default_Tenant record whose `tenant_reference` equals the literal `PLACEHOLDER_TENANT_ID` and whose `tenant_type` equals `STANDARD`.
3. WHEN the Tenant_Migration runs against a database containing Merchants created before this spec, THE Tenant_Migration SHALL set the `tenant_id` of every such Merchant to the Default_Tenant.
4. WHEN the backfill of pre-existing Merchants is complete, THE Tenant_Migration SHALL enforce that the `merchants.tenant_id` column is not null.
5. THE Backend_API SHALL map the Merchant_Tenant_Association so that each Merchant resolves to exactly one owning Tenant.
6. WHEN a Merchant is created through the Backend_API, THE Backend_API SHALL persist the new Merchant with a non-null `tenant_id` referencing an existing Tenant.

### Requirement 3: Tenant Claim Resolution to a Tenant Record

**User Story:** As a QA_Automation_Engineer, I want the literal `tenant_id` claim resolved to a real tenant record, so that the string carried by test users maps deterministically to persisted tenant data.

#### Acceptance Criteria

1. THE Backend_API SHALL resolve the Tenant_Claim string to a Tenant record by matching the claim value against the `tenant_reference` natural key.
2. THE Tenant_Migration SHALL seed Tenant records whose `tenant_reference` values equal each distinct Tenant_Claim literal used by the seeded test users, seeding `PLATFORM_TENANT` with `tenant_type` `PLATFORM` and `TENANT_ALPHA` with `tenant_type` `STANDARD`.
3. WHEN an Authenticated_Principal presents a Tenant_Claim that matches the `tenant_reference` of an existing `ACTIVE` Tenant, THE Backend_API SHALL treat that Tenant as the principal's resolved Tenant.
4. IF an Authenticated_Principal presents a Tenant_Claim that matches no existing Tenant record, THEN THE Backend_API SHALL deny access to tenant-scoped resources and SHALL return `403`.
5. IF a Tenant_Scoped_Principal presents a Tenant_Claim whose resolved Tenant has Tenant_Status `SUSPENDED`, THEN THE Backend_API SHALL deny access to tenant-scoped resources and SHALL return `403`.
6. WHEN a Platform_Scoped_Principal accesses a resource owned by a target Tenant whose Tenant_Status is `SUSPENDED`, THE Backend_API SHALL permit the access so that platform administration and remediation of suspended tenants remain possible.
7. THE Backend_API SHALL exclude the Tenant_Reference and Tenant_Id values of other tenants from any error response body produced by a denied cross-tenant request.

### Requirement 4: Platform-Scoped vs Tenant-Scoped Principal Determination

**User Story:** As a Platform_Operator, I want the system to know which users are platform-wide and which are bound to one tenant, so that isolation applies only to tenant-scoped users while platform operators retain cross-tenant reach.

#### Acceptance Criteria

1. THE Backend_API SHALL classify an Authenticated_Principal as a Platform_Scoped_Principal IF AND ONLY IF the principal's resolved Tenant has Tenant_Type `PLATFORM`, and SHALL otherwise classify the principal as a Tenant_Scoped_Principal.
2. WHERE an Authenticated_Principal is classified as a Platform_Scoped_Principal, THE Backend_API SHALL NOT restrict that principal to a single Tenant for merchant or payment-order access.
3. WHERE an Authenticated_Principal is classified as a Tenant_Scoped_Principal, THE Backend_API SHALL restrict that principal to the single Tenant resolved from its Tenant_Claim.
4. THE Backend_API SHALL derive the classification from the resolved Tenant's Tenant_Type only and SHALL NOT derive the classification from any Fine_Grained_Authority held by the principal.
5. IF an Authenticated_Principal presents no Tenant_Claim, THEN THE Backend_API SHALL deny access to tenant-scoped resources and SHALL return `403`.
6. THE Backend_API SHALL preserve, for a Platform_Scoped_Principal, the existing cross-merchant visibility behavior on payment-order endpoints unchanged.

### Requirement 5: Tenant-Boundary Authorization on Merchant Reads

**User Story:** As a Tenant_Admin_User, I want to see only the merchants of my own tenant, so that I cannot view another tenant's merchants.

#### Acceptance Criteria

1. WHEN a Tenant_Scoped_Principal requests the merchant list, THE Backend_API SHALL return only Merchants whose owning Tenant matches the principal's resolved Tenant.
2. WHEN a Tenant_Scoped_Principal requests a merchant by identifier that belongs to the principal's resolved Tenant, THE Backend_API SHALL return that Merchant with status `200`.
3. IF a Tenant_Scoped_Principal requests a merchant by identifier that belongs to a different Tenant, THEN THE Backend_API SHALL return Masked_Not_Found with status `404`.
4. IF a Tenant_Scoped_Principal requests a merchant identifier that does not exist, THEN THE Backend_API SHALL return Masked_Not_Found with status `404` using the same response shape as criterion 3.
5. WHEN a Platform_Scoped_Principal requests the merchant list without a tenant filter, THE Backend_API SHALL return Merchants across all Tenants.
6. THE Backend_API SHALL require the same Fine_Grained_Authority for each merchant read operation as it requires today and SHALL apply the Tenant_Boundary_Check only after the authority check passes.

### Requirement 6: Tenant-Boundary Authorization on Merchant Writes

**User Story:** As a Tenant_Admin_User, I want my merchant create and status-change actions confined to my own tenant, so that I cannot create or modify merchants outside my tenant boundary.

#### Acceptance Criteria

1. WHEN a Tenant_Scoped_Principal creates a Merchant, THE Backend_API SHALL assign the new Merchant to the principal's resolved Tenant and SHALL ignore any `tenantReference` field present in the create-merchant request body.
2. WHEN a Platform_Scoped_Principal creates a Merchant and supplies a `tenantReference` field that matches the `tenant_reference` of an existing Tenant, THE Backend_API SHALL assign the new Merchant to that Tenant.
3. IF a Platform_Scoped_Principal creates a Merchant and the `tenantReference` field is absent, THEN THE Backend_API SHALL reject the request and SHALL return `400`.
4. IF a Platform_Scoped_Principal creates a Merchant and the supplied `tenantReference` field matches no existing Tenant, THEN THE Backend_API SHALL reject the request and SHALL return `400`.
5. WHEN a Tenant_Scoped_Principal requests a status change on a Merchant that belongs to the principal's resolved Tenant, THE Backend_API SHALL apply the existing merchant status-change behavior unchanged.
6. IF a Tenant_Scoped_Principal requests a status change on a Merchant that belongs to a different Tenant, THEN THE Backend_API SHALL deny the request and SHALL return `403`.
7. THE Backend_API SHALL require the same Fine_Grained_Authority for each merchant write operation as it requires today and SHALL apply the Tenant_Boundary_Check only after the authority check passes.

### Requirement 7: Optional Tenant Filter for Platform-Scoped Reads

**User Story:** As a Platform_Operator, I want to optionally narrow the merchant list to one tenant, so that I can inspect a single tenant's merchants without losing my platform-wide reach.

#### Acceptance Criteria

1. WHERE a Platform_Scoped_Principal supplies a `tenantId` filter parameter on the merchant list request that matches the `tenant_reference` of an existing Tenant, THE Backend_API SHALL return only Merchants owned by that Tenant.
2. WHERE a Platform_Scoped_Principal supplies no `tenantId` filter parameter on the merchant list request, THE Backend_API SHALL return Merchants across all Tenants.
3. IF a Platform_Scoped_Principal supplies a `tenantId` filter parameter that matches no existing Tenant, THEN THE Backend_API SHALL return an empty merchant list with status `200`.
4. WHERE a Tenant_Scoped_Principal supplies a `tenantId` filter parameter, THE Backend_API SHALL ignore the parameter and SHALL return only Merchants owned by the principal's resolved Tenant.

### Requirement 8: Transitive Tenant Isolation on Payment Orders

**User Story:** As a Tenant_Admin_User, I want tenant isolation to extend to payment orders through their merchant, so that I cannot reach payment orders belonging to another tenant's merchant.

#### Acceptance Criteria

1. WHEN a Tenant_Scoped_Principal requests a payment-order read on a Merchant that belongs to the principal's resolved Tenant, THE Backend_API SHALL apply the existing merchant-scope behavior unchanged.
2. IF a Tenant_Scoped_Principal requests a payment-order read on a Merchant that belongs to a different Tenant, THEN THE Backend_API SHALL return Masked_Not_Found with status `404`.
3. IF a Tenant_Scoped_Principal requests a payment-order write or lifecycle action on a Merchant that belongs to a different Tenant, THEN THE Backend_API SHALL deny the request and SHALL return `403`.
4. THE Backend_API SHALL derive the Tenant of a Payment_Order solely from the Merchant_Tenant_Association and SHALL NOT read any tenant column on the Payment_Order.
5. THE Backend_API SHALL require the same Fine_Grained_Authority for each payment-order operation as it requires today and SHALL apply the Tenant_Boundary_Check only after the authority check and the existing merchant-scope check.

### Requirement 9: Preservation of Existing Behavior and Build Health

**User Story:** As a QA_Automation_Engineer, I want the existing merchant-scope and platform-scope flows and the existing test suite to keep passing, so that the additive tenant check does not regress current behavior.

#### Acceptance Criteria

1. THE Backend_API SHALL preserve every existing REST contract, HTTP status code, response header, and problem-detail shape for requests that do not cross a tenant boundary.
2. WHEN a merchant-scoped Merchant_User accesses payment orders of its own Merchant within its own Tenant, THE Backend_API SHALL preserve the existing merchant-scope outcome unchanged.
3. WHEN a Platform_Scoped_Principal accesses payment-order endpoints, THE Backend_API SHALL preserve the existing platform-scope cross-merchant outcome unchanged.
4. WHEN the existing backend test suite is executed with `./mvnw test` and `./mvnw verify`, THE Backend_API SHALL satisfy the existing tests after the Tenant_Migration and seeded tenant data are applied.
5. THE Backend_API SHALL preserve the Spring Modulith module boundaries, with no module importing another module's `internal` packages on account of the Tenant entity.

### Requirement 10: Tenant Isolation Verification Matrix

**User Story:** As a QA_Automation_Engineer, I want an explicit matrix of cross-tenant outcomes, so that I can write deterministic isolation tests for each principal class and operation.

#### Acceptance Criteria

1. THE requirements document SHALL record an isolation matrix mapping each principal class and operation to its expected cross-tenant outcome.
2. THE Backend_API SHALL produce, for a Tenant_Scoped_Principal acting outside its Tenant, exactly the outcomes recorded in the isolation matrix (`404` Masked_Not_Found for reads, `403` for writes and lifecycle actions).
3. THE Backend_API SHALL produce, for a Platform_Scoped_Principal, the cross-tenant-visible outcomes recorded in the isolation matrix.

#### Tenant Isolation Matrix

| Operation (required authority unchanged) | Tenant_Scoped_Principal, same tenant | Tenant_Scoped_Principal, other tenant | Platform_Scoped_Principal |
|---|---|---|---|
| List merchants (`platform:merchants:read`) | Only own-tenant merchants | (not applicable — list is filtered) | All tenants (optional `?tenantId=` narrows) |
| Get merchant by id (`platform:merchants:read`) | `200` | `404` Masked_Not_Found | `200` for any tenant |
| Create merchant (`platform:merchants:create`) | Created in own tenant (any `tenantReference` in body ignored) | (not applicable — bound to own tenant) | Created in the Tenant named by the required `tenantReference` body field; absent or unresolved `tenantReference` → `400` |
| Activate/suspend merchant (`platform:merchants:update-status`) | Applies status change | `403` | Applies status change for any tenant |
| Read payment orders (`merchant:payments:read` / `platform:payments:read`) | Existing merchant-scope outcome | `404` Masked_Not_Found | Existing platform cross-merchant outcome |
| Payment lifecycle / write (`*:payments:lifecycle`, create) | Existing merchant-scope outcome | `403` | Existing platform outcome |

Note: "other tenant" outcomes apply only to a Tenant_Scoped_Principal. A request that is rejected by the existing authority check or merchant-scope check keeps its current status code; the Tenant_Boundary_Check is evaluated only after those checks pass.

## Resolved Decisions

Each decision records the original open question for traceability, followed by the resolution and a short rationale.

### Decision 1: Tenant_Claim-to-record mapping mechanism

**Original question:** This document proposes a `tenant_reference` natural-key column on `tenants` whose value equals the JWT `tenant_id` literal (for example `TENANT_ALPHA`), with the seeded test references resolved at request time. The alternative is to make the JWT `tenant_id` claim carry the Tenant UUID directly (which would require changing the seeded user attributes and the realm import). Confirm the `tenant_reference` natural-key approach versus switching claims to UUIDs.

**Resolved:** Use the natural key `tenant_reference`. The JWT `tenant_id` claim carries a stable business identifier (for example `TENANT_ALPHA`) that is resolved at request time against the unique `tenants.tenant_reference` column. The claim never carries the internal surrogate primary key. **Rationale:** external tokens must not leak internal surrogate primary keys, and the split teaches the distinction between a natural key (the externally meaningful `tenant_reference`) and a surrogate key (the internal `tenant_id` UUID).

### Decision 2: Platform-scoped vs tenant-scoped classification rule

**Original question:** Requirement 4 mandates a single documented rule but does not fix it. Candidate rules: (a) treat the sentinel Tenant_Reference `PLATFORM_TENANT` as the platform-scoped marker; (b) classify by authority; (c) an explicit boolean/role marker. Note the tension from `iam-roles`: `READ_ONLY_USER` holds `platform:payments:read` yet carries `tenant_id = TENANT_ALPHA`, so an authority-only rule would misclassify it. Select the classification rule.

**Resolved:** Classify data-driven via the `tenant_type` column (`PLATFORM` / `STANDARD`). A principal is a Platform_Scoped_Principal if and only if its resolved Tenant has `tenant_type = PLATFORM`; otherwise it is a Tenant_Scoped_Principal. The seed sets `PLATFORM_TENANT` → `PLATFORM`, and both `TENANT_ALPHA` and the Default_Tenant (`PLACEHOLDER_TENANT_ID`) → `STANDARD`. **Rationale:** explicit, data-driven authorization that does not overload authorities. An authority-based rule would misclassify `READ_ONLY_USER` (which holds `platform:payments:read` but carries `tenant_id = TENANT_ALPHA` → `STANDARD` → tenant-scoped). The rule requires no Keycloak realm change.

### Decision 3: Reads — 403 vs Masked_Not_Found

**Original question:** This document follows the existing `PaymentOrderController` pattern — cross-tenant reads return `404` Masked_Not_Found and cross-tenant writes return `403`. Confirm this split, or require `403` uniformly for cross-tenant access.

**Resolved:** Keep the split. Cross-tenant reads return `404` Masked_Not_Found; cross-tenant writes and lifecycle actions return `403`. **Rationale:** consistency with the existing `PaymentOrderController` behavior, and it teaches the masked-not-found non-disclosure pattern (existence of another tenant's resource is not revealed).

### Decision 4: Suspended-tenant behavior scope

**Original question:** Requirement 3 denies access when the principal's own resolved Tenant is `SUSPENDED`. It is undecided whether a `SUSPENDED` target Tenant (for example a platform operator acting on a suspended tenant's merchant) should also be blocked, or whether tenant suspension only gates the acting principal. Define whether tenant suspension blocks inbound access to that tenant's resources as well.

**Resolved:** A Tenant_Scoped_Principal whose own resolved Tenant is `SUSPENDED` is denied with `403`. A Platform_Scoped_Principal retains access to a suspended target Tenant's resources, so that platform administration and remediation remain possible. **Rationale:** realistic suspension semantics — suspension gates the suspended tenant's own users while still allowing platform operators to administer and remediate the suspended tenant.

### Decision 5: Tenant assignment when a Platform_Scoped_Principal creates a merchant

**Original question:** A Tenant_Scoped_Principal's new merchant is assigned to its own resolved Tenant. A Platform_Scoped_Principal has no single tenant. Require an explicit tenant reference on the create request for platform operators, default new platform-created merchants to the Default_Tenant, or defer platform-initiated merchant creation entirely.

**Resolved:** Use explicit tenant assignment. A Tenant_Scoped_Principal create auto-assigns the new Merchant to its own resolved Tenant. A Platform_Scoped_Principal create MUST supply an explicit `tenantReference` in the request body; an absent or unresolved `tenantReference` yields `400`. The `tenantReference` field is an additive request-body field (optional for tenant-scoped principals, required for platform-scoped principals). **Rationale:** explicit ownership is safer and clearer than silent defaulting to a placeholder tenant.

### Decision 6: Flyway migration placement, ordering, and module ownership

**Original question:** Current Flyway locations are `db/migration/merchant` and `db/migration/payment`; Flyway orders versions across all locations. The `tenants` table must be created before the `merchants.tenant_id` foreign key is added. Confirm whether the Tenant_Migration adds a new location with appropriate ordering or places the tenant migrations within the existing `merchant` location, and which Spring Modulith module owns the Tenant entity.

**Resolved:** Introduce a new Spring Modulith module `tenant` (package `lab.paymentquality.tenant`) that exposes a PUBLIC module API for tenant lookup and resolution; the `merchant` module depends on the `tenant` PUBLIC API only and never on its internal packages. A new Flyway location `db/migration/tenant` holds the `tenants` table creation plus seed data. The `merchants.tenant_id` foreign-key column plus the backfill migration live in `db/migration/merchant`, versioned to run AFTER the `tenants` table exists. **Rationale:** Tenant is a first-class domain concept that earns its own module and published API; the split teaches Spring Modulith boundaries and cross-location Flyway version ordering.

### Decision 7: Realm import coupling

**Original question:** Making the Tenant_Claim reflect real seeded tenants assumes the seeded `tenant_reference` values exactly match the literals already present in `payment-quality-realm.json` (`TENANT_ALPHA`, `PLATFORM_TENANT`, `PLACEHOLDER_TENANT_ID`). Confirm the seed values must stay byte-identical to the realm literals, and whether the `iam-roles` Decision 4 forward note (model tenants as Keycloak groups) is adopted now or deferred.

**Resolved:** The seeded `tenant_reference` values stay byte-identical to the realm literals (`TENANT_ALPHA`, `PLATFORM_TENANT`, `PLACEHOLDER_TENANT_ID`). The `iam-roles` Decision 4 forward note (modeling tenants as Keycloak groups) is DEFERRED — this spec enforces isolation at the backend, and the existing per-user `tenant_id` attributes already deliver the claim. **Rationale:** keeping the seed values identical to the realm literals lets the existing test users resolve to real tenants without any realm change, while group-based modeling can be revisited in a later spec.
