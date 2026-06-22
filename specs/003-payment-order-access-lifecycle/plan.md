# Implementation Plan: Payment Order Access, Idempotent Creation, And Minimal Create/Read Lifecycle Foundation

**Branch**: `004-payment-order-create-read` | **Date**: 2026-05-27 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/003-payment-order-access-lifecycle/spec.md`

## 1. Technical Summary

Phase 2 introduces the first payment-domain resource for the Payment Quality Engineering Lab: merchant-scoped payment orders that can be created idempotently and read through a minimal ownership-aware API and dashboard journey.

The work introduces:

- A new `payment` Spring Modulith module that owns payment order create/read behavior, idempotency records, and status history.
- A small public merchant API boundary so payment can check active merchant eligibility without depending on `merchant.internal`.
- PostgreSQL-backed payment persistence through Flyway migrations for `payment_orders`, `idempotency_records`, and `payment_order_status_history`.
- Idempotent `POST /api/merchants/{merchantId}/payment-orders` with `Idempotency-Key`, request fingerprinting, `201 Created` for first success, `200 OK` for same-key same-fingerprint replay, and `409 idempotency_conflict` for same-key different-fingerprint reuse.
- Ownership-aware `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` with merchant-scoped read and platform cross-merchant read support.
- Strong ETag response headers on create and read as a compatibility point for the later lifecycle slice.
- `X-Correlation-ID` propagation or generation on all payment requests and storage of the create correlation ID in status history.
- Keycloak/test JWT extensions for `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate` as planned unused role, and `platform:payments:read`.
- A minimal Nuxt 4 dashboard payment journey inside merchant context: create form, detail view, validation feedback, loading/error states, and role-aware visibility.
- Test layering for domain validation, application service orchestration, JPA/Testcontainers persistence, REST Assured contracts, security matrix, Spring Modulith architecture verification, concurrency/idempotency, and Playwright journey checks.
- Parallel-safe payment references and idempotency keys using `PAY-{testRunId}-{workerId}-{uuid}` and `idem-{testRunId}-{workerId}-{uuid}`.

The backend remains a modular monolith built on Java 25, Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6, Maven 3.9.11, PostgreSQL 18, and Keycloak 26.6.1. The frontend remains Nuxt 4.4.6, Nuxt UI 4.7.1, TypeScript 6, Zod, Pinia, and Playwright 1.60.

No lifecycle actions (`authorize`, `capture`, `cancel`), `If-Match`, `412`, status transitions beyond `CREATED`, PSP integration, card data, Kafka, webhooks, refunds, settlement, reconciliation, GraphQL, gRPC, Client Credentials Flow, full Merchant Team Management, payment list/search, complete admin portal, or complete self-service portal behavior is implemented.

## 2. Technical Context

| Area | Decision |
|---|---|
| Language/Version | Java 25; TypeScript 6 for frontend |
| Backend Dependencies | Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6, Spring Security Resource Server, Spring Data JPA, Flyway, Maven 3.9.11 |
| Frontend Dependencies | Nuxt 4.4.6, Nuxt UI 4.7.1, Pinia, Zod, `nuxt-auth-utils`, Playwright 1.60 |
| Storage | PostgreSQL 18 with Flyway migrations and JPA/Hibernate |
| Auth | Keycloak 26.6.1 JWT Resource Server; local signed JWT test support |
| Testing | JUnit 6, AssertJ, Mockito, REST Assured, Testcontainers PostgreSQL, Spring Modulith tests, Playwright |
| Target Platform | Linux server/local lab |
| Project Type | Modular monolith web service with backend and frontend apps |
| Performance Goals | Deterministic local-lab behavior; idempotent create must be reliable under near-simultaneous duplicate requests |
| Constraints | Create/read only; atomic persistence; no payment lifecycle actions; no PSP/card/Kafka/webhook behavior; no dependency on `merchant.internal` |
| Scale/Scope | First payment slice for learner lab; merchant-scoped create/read, platform read, and minimal dashboard journey |

No unresolved `NEEDS CLARIFICATION` items remain.

## 3. Constitution Check

### Pre-Design Gate

| Principle | Status | Plan Response |
|---|---|---|
| Tester-Led Product Learning | PASS | Tester-facing risks are explicit: idempotency, money precision, ownership, cross-tenant leakage, concurrency, atomicity, error contract stability, and UI scope creep. Testing strategy separates agent implementation, tester analysis, tester design, tester automation, and review. |
| Spec-Driven Delivery | PASS | The spec contains business purpose, actors, scope, functional and non-functional requirements, acceptance criteria, assumptions, and success criteria. This plan traces FR-001 through FR-034 to implementation and test coverage. |
| Modular Monolith Boundaries | PASS | `payment` owns payment behavior. Merchant eligibility is exposed through a merchant public API boundary. Payment must not import `merchant.internal`. Architecture verification and `@ApplicationModuleTest` are planned. |
| Parallel-Ready Quality Engineering | PASS | Namespaced payment references/idempotency keys, per-test merchants, Testcontainers, transaction rollbacks, and concurrency tests are planned. |
| Security, Data Integrity, and Observability | PASS | JWT authorities, merchant scope, masked `404`, `403`, `401`, `409`, validation, DB constraints, transaction boundaries, ETags, correlation IDs, and status history audit are defined. |

### Post-Design Gate

| Principle | Status | Evidence |
|---|---|---|
| Tester-Led Product Learning | PASS | `research.md`, `data-model.md`, API contract, quickstart, testing strategy, and FR traceability table expose test conditions and learning value. |
| Spec-Driven Delivery | PASS | Research resolves idempotency fingerprint, ETag, REST shape, authority mapping, and transaction unknowns. No clarification markers remain. |
| Modular Monolith Boundaries | PASS | Plan adds root merchant public API and `payment.internal.*` implementation boundary. Architecture tests must verify absence of forbidden imports. |
| Parallel-Ready Quality Engineering | PASS | Data model and quickstart define namespacing. Testing strategy includes same-key concurrency and isolated Testcontainers use. |
| Security, Data Integrity, and Observability | PASS | Contract defines status codes and errors. Data model defines constraints and audit. Security plan defines role and merchant scope behavior. |

## 4. Architecture Decisions

### Payment Module Ownership

The `payment` module owns payment order creation, idempotency records, payment order retrieval, status history, and first-slice payment validation. It exposes behavior through REST endpoints only in this phase. It does not expose public payment API types for other modules yet.

### Merchant Public API Boundary

Phase 2 adds a root-package public merchant boundary so payment can ask whether a merchant is eligible for payment creation:

```text
lab.paymentquality.merchant
├── MerchantPaymentEligibility.java
└── MerchantPaymentEligibilityService.java
```

The implementation stays inside `merchant.internal.application` and may delegate to existing merchant repository/domain classes. The payment module depends on this public API only. It must not import `lab.paymentquality.merchant.internal.*`.

Suggested public contract:

```java
public interface MerchantPaymentEligibilityService {
    Optional<MerchantPaymentEligibility> findEligibility(UUID merchantId);
}

public record MerchantPaymentEligibility(
        UUID merchantId,
        String normalizedReference,
        boolean active
) {}
```

The payment service treats `Optional.empty()` as not found for read setup and `active == false` as `409 merchant_not_payment_eligible` for create.

### Backend Package Structure

```text
lab.paymentquality.payment                 # module root, package-info.java only
lab.paymentquality.payment.internal        # implementation boundary
lab.paymentquality.payment.internal.domain
lab.paymentquality.payment.internal.application
lab.paymentquality.payment.internal.infrastructure
lab.paymentquality.payment.internal.web
```

The module root contains `@ApplicationModule(displayName = "Payment Orders")`. All domain entities, JPA repositories, DTOs, controllers, exception handlers, and services remain internal.

### Layering Within The Payment Module

| Layer | Responsibility |
|---|---|
| Domain | `PaymentOrder`, `PaymentStatus`, `PaymentAmount`, `CurrencyCode`, `ClientOrderReference`, `IdempotencyKey`, `RequestFingerprint`, domain exceptions |
| Application | `PaymentOrderService` orchestrates create/read, authorization context checks, merchant eligibility, idempotency, transactions, and replay/conflict decisions |
| Infrastructure | Spring Data JPA repositories for payment orders, idempotency records, and status history |
| Web | REST controller, request/response DTOs, ETag and `Location` response headers, payment-specific exception handler |
| Shared | Existing correlation filter and security configuration; no shared code may depend on payment internals |

Controllers delegate to the application service. Repositories do not contain business decisions. Domain value objects own validation and normalization where useful.

### Persistence

Spring Data JPA persists payment entities. Flyway migration directory follows the modular pattern:

```text
apps/backend/src/main/resources/db/migration/payment/V1__create_payment_orders.sql
```

The database enforces amount, currency, status, idempotency uniqueness, and FK integrity. Application-level validation still produces user-facing error details before database constraints are hit.

### Security Integration

Existing `SecurityConfig` is extended with payment endpoint matchers. Existing `KeycloakRealmRoleConverter` must change from unconditional `platform:` prefixing to mixed mapping:

- Existing Phase 1 realm roles `merchants:create`, `merchants:read`, `merchants:update-status` continue mapping to `platform:merchants:*`.
- New namespaced roles `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, and `platform:payments:read` are preserved as Spring authorities.

Merchant-scoped payment identities need a single `merchant_id` JWT claim. Platform payment reader identities do not require `merchant_id` for read.

### REST Shape

The API uses merchant-context paths:

| Method | Path | Authority | Main Status Codes |
|---|---|---|---|
| `POST` | `/api/merchants/{merchantId}/payment-orders` | `merchant:payments:create` + merchant scope | `201`, `200`, `400`, `401`, `403`, `409` |
| `GET` | `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}` | `merchant:payments:read` + merchant scope, or `platform:payments:read` | `200`, `400`, `401`, `403`, `404` |
| `GET` | `/api/status` | public | `200` |

Full contract: [contracts/payment-order-api.md](./contracts/payment-order-api.md).

### Frontend Scope

The Nuxt dashboard adds a minimal payment journey under merchant context. It does not add payment listing, filtering, lifecycle buttons, PSP placeholders, or complete admin/self-service workflows.

## 5. Repository Structure Changes

Additions and modifications under the existing monorepo layout:

```text
apps/backend/
├── src/main/java/lab/paymentquality/
│   ├── merchant/
│   │   ├── MerchantPaymentEligibility.java                  # New public API record
│   │   ├── MerchantPaymentEligibilityService.java           # New public API interface
│   │   └── internal/application/
│   │       └── MerchantPaymentEligibilityAdapter.java       # Implements public eligibility API
│   ├── payment/
│   │   ├── package-info.java                                # @ApplicationModule(displayName = "Payment Orders")
│   │   └── internal/
│   │       ├── domain/
│   │       │   ├── PaymentOrder.java
│   │       │   ├── PaymentStatus.java
│   │       │   ├── IdempotencyRecord.java
│   │       │   ├── PaymentOrderStatusHistory.java
│   │       │   ├── PaymentAmount.java
│   │       │   ├── CurrencyCode.java
│   │       │   ├── ClientOrderReference.java
│   │       │   ├── IdempotencyKey.java
│   │       │   └── RequestFingerprint.java
│   │       ├── application/
│   │       │   ├── PaymentOrderService.java
│   │       │   ├── PaymentCreateResult.java
│   │       │   └── PaymentActorContext.java
│   │       ├── infrastructure/
│   │       │   ├── JpaPaymentOrderRepository.java
│   │       │   ├── JpaIdempotencyRecordRepository.java
│   │       │   └── JpaPaymentOrderStatusHistoryRepository.java
│   │       └── web/
│   │           ├── PaymentOrderController.java
│   │           ├── CreatePaymentOrderRequest.java
│   │           ├── PaymentOrderResponse.java
│   │           ├── PaymentOrderMapper.java
│   │           ├── PaymentExceptionHandler.java
│   │           └── PaymentErrorResponse.java
│   └── shared/security/
│       ├── SecurityConfig.java                              # Modified: payment endpoint rules
│       └── KeycloakRealmRoleConverter.java                  # Modified: mixed role mapping
├── src/main/resources/db/migration/payment/
│   └── V1__create_payment_orders.sql
└── src/test/java/lab/paymentquality/
    ├── architecture/
    │   └── ModulithArchitectureTest.java                    # Existing test verifies payment boundary
    ├── payment/
    │   ├── PaymentModuleTest.java
    │   └── internal/
    │       ├── domain/
    │       │   ├── PaymentAmountTest.java
    │       │   ├── CurrencyCodeTest.java
    │       │   ├── ClientOrderReferenceTest.java
    │       │   └── IdempotencyKeyTest.java
    │       ├── application/
    │       │   ├── PaymentOrderServiceTest.java
    │       │   └── PaymentOrderIdempotencyConcurrencyTest.java
    │       ├── infrastructure/
    │       │   └── JpaPaymentOrderRepositoryTest.java
    │       └── web/
    │           └── PaymentOrderControllerTest.java
    ├── rest/
    │   └── PaymentOrderRestAssuredTest.java
    ├── security/
    │   └── PaymentOrderSecurityTest.java
    └── testsupport/
        ├── PaymentApiTestSupport.java
        └── TestJwtSupport.java                              # Modified: payment tokens and merchant_id claim

apps/frontend/
├── app/
│   ├── components/payment/
│   │   ├── CreatePaymentOrderForm.vue
│   │   ├── PaymentOrderDetail.vue
│   │   └── PaymentStatusBadge.vue
│   ├── pages/admin/merchants/[merchantId]/payments/
│   │   ├── new.vue
│   │   └── [paymentOrderId].vue
│   ├── schemas/payment-order.schema.ts
│   └── stores/payment-orders.ts
├── server/api/merchants/[merchantId]/payment-orders/
│   ├── index.post.ts
│   └── [paymentOrderId].get.ts
└── tests/e2e/
    ├── payment-order-create.spec.ts
    ├── payment-order-read.spec.ts
    └── payment-order-auth-deny.spec.ts

infra/keycloak/realms/
└── payment-quality-realm.json                               # Modified: payment roles, users, merchant_id mapper

specs/003-payment-order-access-lifecycle/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/payment-order-api.md
```

**Structure Decision**: Use the existing backend/frontend monorepo layout. Add one backend business module (`payment`), one merchant public API boundary, payment-specific frontend components/routes/server handlers, and no new top-level application.

## 6. Backend Payment Module Plan

### Application Service Operations

| Operation | Description |
|---|---|
| `create(merchantId, command, idempotencyKey, actor, correlationId)` | Validates request, enforces merchant scope, checks active merchant eligibility, handles idempotency, persists order/history/record atomically, returns create result with `created` or `replayed` marker |
| `findForMerchant(merchantId, paymentOrderId, actor)` | Enforces `merchant:payments:read` and merchant scope, returns order or masked `not_found` |
| `findForPlatform(merchantId, paymentOrderId, actor)` | Enforces `platform:payments:read`, reads any merchant payment order using target merchant ID context |

The application service is the transaction boundary for create. Read operations may be read-only transactions.

### Idempotent Creation Flow

1. Web layer validates path UUIDs, body shape, and required `Idempotency-Key`.
2. Security layer requires `merchant:payments:create`.
3. Application layer verifies JWT `merchant_id` matches path `merchantId`.
4. Application layer calls merchant public API for eligibility.
5. Non-active merchant returns `409 merchant_not_payment_eligible`.
6. Service builds canonical request fingerprint from operation, merchant ID, amount, currency, and trimmed client reference.
7. Service hashes the idempotency key and fingerprint with SHA-256 hex.
8. Existing idempotency record with same key and same fingerprint returns replay result with existing order.
9. Existing idempotency record with different fingerprint throws `IdempotencyConflictException`.
10. New request persists payment order, initial status history, and idempotency record in one transaction.
11. Concurrent insert conflict reloads the committed record and applies same-fingerprint/different-fingerprint decision.

### Internal Domain Model

- `PaymentOrder`: JPA entity with UUID ID, merchant ID, client reference, amount minor, currency, status, timestamps, and JPA version. Factory method creates status `CREATED` only.
- `PaymentStatus`: enum with only `CREATED` in this slice. No transition methods beyond creation are needed now.
- `IdempotencyRecord`: JPA entity holding merchant ID, idempotency key hash, request fingerprint hash, linked payment order, and timestamps.
- `PaymentOrderStatusHistory`: JPA entity for append-only status history. Creation entry uses `fromStatus = null`, `toStatus = CREATED`.
- `PaymentAmount`: value object validating `1..100_000_000` minor units.
- `CurrencyCode`: value object validating `PLN`, `EUR`, `USD` only.
- `ClientOrderReference`: value object trimming and validating non-blank max 120.
- `IdempotencyKey`: value object validating non-blank printable ASCII max 128 and producing SHA-256 hash.
- `RequestFingerprint`: canonical representation and SHA-256 hash of create material fields.

### Repository Boundary

`JpaPaymentOrderRepository`:

- `Optional<PaymentOrder> findByMerchantIdAndPaymentOrderId(UUID merchantId, UUID paymentOrderId)`
- `Optional<PaymentOrder> findByPaymentOrderId(UUID paymentOrderId)` for platform read with merchant ID verification

`JpaIdempotencyRecordRepository`:

- `Optional<IdempotencyRecord> findByMerchantIdAndIdempotencyKeyHash(UUID merchantId, String keyHash)`
- Unique constraint handles concurrent inserts.

`JpaPaymentOrderStatusHistoryRepository`:

- Save initial status history.
- Optional test query by payment order ID for verification.

### DTOs And Mapping

- `CreatePaymentOrderRequest`: `amountMinor`, `currency`, `clientOrderReference` with Bean Validation annotations.
- `PaymentOrderResponse`: payment order ID, merchant ID, client order reference, amount minor, currency, status, createdAt, updatedAt.
- `PaymentErrorResponse`: stable error code, message, optional field details, correlation ID.
- `PaymentOrderMapper`: entity-to-response mapping. It does not include idempotency hashes or status history internals.

### Error Handling

A payment-scoped `@RestControllerAdvice(assignableTypes = PaymentOrderController.class)` maps payment exceptions:

| Exception | HTTP Status | Error Code |
|---|---:|---|
| Invalid amount/currency/reference/idempotency key/correlation ID | `400` | `validation` |
| `MethodArgumentTypeMismatchException` for malformed UUID | `400` | `validation` |
| `MerchantNotPaymentEligibleException` | `409` | `merchant_not_payment_eligible` |
| `IdempotencyConflictException` | `409` | `idempotency_conflict` |
| `PaymentOrderNotFoundException` | `404` | `not_found` |
| Cross-tenant merchant read | `404` | `not_found` |
| `AccessDeniedException` | `403` | `forbidden` or Spring default with equivalent status |
| Authentication failure | `401` | Spring Resource Server response |

## 7. Payment Domain Model

Full model details and SQL draft are in [data-model.md](./data-model.md).

### Responsibility Distribution

| Layer | Responsibility |
|---|---|
| `PaymentAmount` | Money precision and BVA boundaries |
| `CurrencyCode` | Supported currency EP rules |
| `ClientOrderReference` | Trim, blank, and max-length validation |
| `IdempotencyKey` | Header validation and key hashing |
| `RequestFingerprint` | Canonical fingerprint derivation |
| `PaymentOrder` | Initial resource creation and current status |
| `IdempotencyRecord` | Replay/conflict comparison data |
| `PaymentOrderStatusHistory` | Creation audit trail |
| `PaymentOrderService` | Transaction, merchant eligibility, idempotency race handling, ownership decisions |
| `PaymentOrderController` | HTTP mapping, status codes, response headers |

### Lifecycle Scope

Only `CREATED` exists in this slice. No method, endpoint, UI control, or test should imply authorization, capture, cancel, `If-Match`, `412`, or invalid transition behavior.

## 8. PostgreSQL 18 Plan

### Migration

Create `apps/backend/src/main/resources/db/migration/payment/V1__create_payment_orders.sql` with:

- `payment_orders`
- `idempotency_records`
- `payment_order_status_history`
- FK constraints to `merchants` and payment orders
- amount/currency/status check constraints
- unique `(merchant_id, idempotency_key_hash)` idempotency constraint
- indexes for merchant-context reads and status-history inspection

### Design Decisions

- Payment order IDs are UUIDs generated by the application.
- Amount is stored as `BIGINT` minor units, never decimal/floating point.
- Currency is stored as `VARCHAR(3)` with a database check constraint.
- Status is stored as `VARCHAR(20)` with only `CREATED` allowed.
- `version` supports ETag generation and future optimistic locking.
- Idempotency key hash and fingerprint hash use lowercase SHA-256 hex.
- Initial status history is append-only and linked to the payment order.
- The FK to `merchants` is a data-integrity relationship, not a Java dependency on merchant internals.

### Transaction And Concurrency

The create service commits payment order, idempotency record, and status history in one transaction. Same-key concurrency is controlled by the idempotency unique constraint. Tests must prove exactly one payment order is created when near-simultaneous requests use the same key.

### Test Data Isolation

- Repository tests use Testcontainers PostgreSQL and Flyway migrations.
- Each REST/security test creates or resolves its own active merchant.
- Namespaced client references and idempotency keys avoid shared mutable collisions.
- Tests intentionally reuse idempotency keys only within replay/conflict scenarios.

## 9. REST API Plan

Full API contract is documented in [contracts/payment-order-api.md](./contracts/payment-order-api.md).

### Endpoint Summary

| Method | Path | Authority | Notes |
|---|---|---|---|
| `POST` | `/api/merchants/{merchantId}/payment-orders` | `merchant:payments:create` | Requires merchant scope, active merchant, and `Idempotency-Key` |
| `GET` | `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}` | `merchant:payments:read` or `platform:payments:read` | Merchant read is scoped; platform read is cross-merchant |

### Response Headers

- First create: `Location`, `ETag`, `X-Correlation-ID`.
- Replay create: `ETag`, `X-Correlation-ID`.
- Read: `ETag`, `X-Correlation-ID`.

### Error Contract

Errors use stable codes: `validation`, `forbidden`, `not_found`, `merchant_not_payment_eligible`, and `idempotency_conflict`. Missing/invalid authentication remains handled by Spring Resource Server with `401`.

## 10. Security And Keycloak Plan

### Backend Endpoint Authorization

Extend `SecurityConfig`:

```java
.requestMatchers(HttpMethod.POST, "/api/merchants/*/payment-orders")
    .hasAuthority("merchant:payments:create")
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders/*")
    .hasAnyAuthority("merchant:payments:read", "platform:payments:read")
```

The application service still enforces merchant scope and platform-reader behavior. Endpoint authorization alone is not enough.

### Authority Conversion

Update `KeycloakRealmRoleConverter`:

| Realm Role | Spring Authority |
|---|---|
| `merchants:create` | `platform:merchants:create` |
| `merchants:read` | `platform:merchants:read` |
| `merchants:update-status` | `platform:merchants:update-status` |
| `merchant:payments:create` | `merchant:payments:create` |
| `merchant:payments:read` | `merchant:payments:read` |
| `merchant:payments:operate` | `merchant:payments:operate` |
| `platform:payments:read` | `platform:payments:read` |

### Merchant Scope Claim

Merchant payment users include a single `merchant_id` JWT claim. The application service compares it to the path merchant ID for create and merchant read. Missing or mismatched merchant scope denies merchant-scoped operations. Cross-tenant single-resource read returns masked `404 not_found`.

### Keycloak Realm Import

Modify `infra/keycloak/realms/payment-quality-realm.json`:

- Add realm roles: `merchant:payments:create`, `merchant:payments:read`, `merchant:payments:operate`, `platform:payments:read`.
- Add users: `merchant.payment.creator`, `merchant.payment.reader`, `merchant.payment.operator`, `platform.payment.reader`.
- Add a mapper/attribute for merchant users to include `merchant_id` in the access token.
- Preserve existing Phase 1 users and roles.

### Denial Rules

- Missing/invalid/expired token: `401`.
- Authenticated without required role: `403`.
- Merchant reader wrong merchant: masked `404`.
- `merchant:payments:operate` alone: `403` for create and read.
- `platform:payments:read`: read any merchant payment order, cannot create.

### Logging And Secret Safety

Logs must not include tokens, authorization headers, raw idempotency keys, or sensitive session data. Correlation IDs and payment order IDs are acceptable diagnostics.

## 11. Frontend Nuxt 4 Plan

### Route Structure

| Route | Purpose | Auth |
|---|---|---|
| `/admin/merchants/[merchantId]/payments/new` | Merchant-context create payment order form | Auth required |
| `/admin/merchants/[merchantId]/payments/[paymentOrderId]` | Payment order detail | Auth required |

The existing merchant dashboard may link to the create route for active merchants when the sanitized auth state indicates create permission. Backend remains the enforcement source.

### Components

- `CreatePaymentOrderForm.vue`: fields for amount minor, currency, client order reference, submit state, field-level errors, success feedback.
- `PaymentOrderDetail.vue`: status, amount, currency, reference, timestamps, ETag/correlation metadata when helpful for learning.
- `PaymentStatusBadge.vue`: displays `CREATED` only.

### Zod Validation

`payment-order.schema.ts` validates:

- `amountMinor`: integer `1..100_000_000`.
- `currency`: enum `PLN`, `EUR`, `USD`.
- `clientOrderReference`: trimmed non-blank max 120.

The browser may generate an idempotency key for form submission, but the server API handler must set the actual backend `Idempotency-Key` header. The browser must not receive access tokens.

### Server API Handlers

Nuxt server handlers under `server/api/merchants/[merchantId]/payment-orders/` call the backend using existing server-side token mediation. They forward or generate correlation IDs and translate backend errors into UI-friendly feedback without hiding machine-readable codes.

### Pinia State

A `payment-orders` store may hold transient create/read state, loading flags, error feedback, and the last created order. It must not store tokens, raw sessions, authorization headers, or raw idempotency hashes.

### Playwright Accessibility

Use role/label/text locators. Controls must expose accessible labels for amount, currency, client order reference, submit, and detail status. Avoid `data-testid` unless a Nuxt UI interaction cannot be reliably located semantically.

## 12. Testing Strategy

### Backend Test Layers

| Layer | Test Class | Purpose |
|---|---|---|
| Domain unit | `PaymentAmountTest` | BVA: `0`, `1`, `100_000_000`, `100_000_001`, negative |
| Domain unit | `CurrencyCodeTest` | EP: `PLN`/`EUR`/`USD`, unsupported uppercase, lowercase, malformed |
| Domain unit | `ClientOrderReferenceTest` | Blank, trim, max length, namespaced values |
| Domain unit | `IdempotencyKeyTest` | Missing/blank/max length/control characters/hash determinism |
| Application | `PaymentOrderServiceTest` | Merchant eligibility, replay, conflict, ownership, status history creation |
| Concurrency | `PaymentOrderIdempotencyConcurrencyTest` | Same-key near-simultaneous requests create exactly one order |
| Repository | `JpaPaymentOrderRepositoryTest` | Flyway migration, constraints, FK behavior, unique idempotency constraint |
| Module | `PaymentModuleTest` | Payment module starts and only depends on allowed public APIs |
| REST API | `PaymentOrderRestAssuredTest` | Contract status codes, headers, bodies, validation, idempotency |
| Security | `PaymentOrderSecurityTest` | `401`, `403`, masked `404`, platform reader, operate-only denial |
| Architecture | `ModulithArchitectureTest` | No forbidden dependency on `merchant.internal`; module verification passes |

### Frontend Test Layers

| Test | File | Purpose |
|---|---|---|
| Create journey | `payment-order-create.spec.ts` | Login, active merchant context, create order, detail shows `CREATED` |
| Read journey | `payment-order-read.spec.ts` | Reader can view own merchant order; platform reader can view cross-merchant order if setup supports it |
| Auth denial | `payment-order-auth-deny.spec.ts` | Create action hidden for missing authority; direct backend denial remains `403` |

### Test Design Techniques

| Technique | Application |
|---|---|
| Boundary Value Analysis | Amount and length constraints |
| Equivalence Partitioning | Currency, idempotency key, client reference, token classes |
| Decision Tables | Actor x operation x merchant scope x expected status |
| State Modeling | Payment status starts and remains `CREATED` in this slice |
| Concurrency Testing | Same idempotency key, same fingerprint race |
| Negative Testing | Missing auth, invalid token, malformed IDs, wrong merchant, non-active merchant |
| Exploratory Charters | UI feedback, retry behavior, refresh/detail navigation, local restart durability |

### Requirement Traceability

| Requirement | Implementation Area | Planned Test Coverage |
|---|---|---|
| FR-001 | Create endpoint, service, merchant eligibility | REST create success, service create test |
| FR-002 | `PaymentOrder` UUID ID | Domain/entity and REST response assertions |
| FR-003 | `PaymentStatus.CREATED` | Domain, repository, REST body, UI badge |
| FR-004 | Merchant public eligibility API | Service and REST `409 merchant_not_payment_eligible` |
| FR-005 | `Idempotency-Key` validation | Domain/header validation and REST `400` |
| FR-006 | First create headers/body | REST Assured contract test |
| FR-007 | Replay same key/fingerprint | REST and service idempotency tests |
| FR-008 | Conflict same key/different fingerprint | REST and service conflict tests |
| FR-009 | Amount range | Domain BVA, REST validation, UI validation |
| FR-010 | Currency set | Domain EP, REST validation, UI validation |
| FR-011 | Timestamps | Repository and REST response assertions |
| FR-012 | Merchant-scoped read | REST read success and security tests |
| FR-013 | Cross-tenant masked `404` | Security matrix tests |
| FR-014 | Unknown ID `404` | REST not-found tests |
| FR-015 | Malformed ID `400` | REST validation tests |
| FR-016 | Missing role `403` | Security matrix tests |
| FR-017 | Missing/invalid/expired token `401` | Security matrix tests |
| FR-018 | Distinguish `401` vs `403` | Security matrix tests |
| FR-019 | Stable error codes | REST contract tests |
| FR-020 | Initial status history | Repository/service audit tests |
| FR-021 | Atomic persistence | Service transaction and repository tests |
| FR-022 | ETag on create/read | REST contract tests |
| FR-023 | Correlation ID propagation | REST/header and status history tests |
| FR-024 | Modulith boundaries | `ApplicationModules.verify()` and import checks |
| FR-025 | Payment authorities and operate unused | Keycloak/test JWT/security tests |
| FR-026 | No lifecycle actions | Route absence checks, UI absence checks, review checklist |
| FR-027 | Dashboard create/detail route | Playwright create/detail journey |
| FR-028 | UI form/detail/status/action visibility | Component and Playwright tests |
| FR-029 | UI validation/success/loading/error | Playwright and component-level checks if available |
| FR-030 | Deny unauthenticated payment behavior | Security and Playwright denial tests |
| FR-031 | Deny missing authority | Security and Playwright denial tests |
| FR-032 | Local identities and planned operate role | Realm import and test JWT support tests |
| FR-033 | Platform reader cross-merchant read | Security and REST tests |
| FR-034 | `platform.payment.reader` test identity | Realm import/test JWT support verification |

## 13. Spring Modulith Strategy

### Module Introduction

The `payment` module is introduced as a direct sub-package of `lab.paymentquality`:

```java
@ApplicationModule(displayName = "Payment Orders")
package lab.paymentquality.payment;
```

### Public Vs Internal Boundary

- Public payment module root: module declaration only in this slice.
- Internal payment implementation: all entities, repositories, services, controllers, DTOs, exceptions.
- Public merchant root: new eligibility API used by payment.
- Forbidden: any import from `lab.paymentquality.merchant.internal` inside payment.

### Architecture Verification

`ApplicationModules.of(PaymentQualityApplication.class).verify()` must pass with modules `foundation`, `shared`, `merchant`, and `payment`. Tests must catch:

1. Payment importing merchant internals.
2. Merchant depending on payment.
3. Shared depending on payment or merchant internals.
4. Cycles between business modules.

### `@ApplicationModuleTest`

`PaymentModuleTest` boots the payment module with required collaborators and test profile. It verifies the module context starts, payment beans are available, and module boundaries are respected. Full secured HTTP behavior remains in REST/security tests.

## 14. Documentation And Learning Outputs

| Deliverable | Path | Purpose |
|---|---|---|
| Feature plan | `specs/003-payment-order-access-lifecycle/plan.md` | Implementation architecture and task input |
| Research | `specs/003-payment-order-access-lifecycle/research.md` | Idempotency, fingerprint, ETag, role mapping decisions |
| Data model | `specs/003-payment-order-access-lifecycle/data-model.md` | Entity, SQL, validation, transaction model |
| API contract | `specs/003-payment-order-access-lifecycle/contracts/payment-order-api.md` | REST request/response/status/header contract |
| Quickstart | `specs/003-payment-order-access-lifecycle/quickstart.md` | Local learner walkthrough after implementation |
| Lesson 6 docs | Existing vault/docs lesson locations | Connect idempotency, money precision, ownership, and concurrency testing to learning flow |

Implementation tasks should add tester-facing docs only where they reflect implemented behavior and must avoid fake lifecycle completeness.

## 15. Risks And Mitigations

| Risk | Mitigation |
|---|---|
| Existing role converter breaks namespaced payment authorities | Update converter to preserve roles containing `:` while keeping Phase 1 role mapping tests |
| Payment accidentally imports `merchant.internal` | Add public merchant eligibility API and architecture tests |
| Same-key concurrent create produces duplicate orders | Use DB unique constraint and concurrency tests with near-simultaneous requests |
| Request fingerprint false conflict | Use canonical JSON excluding volatile headers/tokens/correlation IDs |
| UI implies lifecycle behavior | Do not add authorize/capture/cancel buttons, disabled placeholders, or PSP language |
| Cross-tenant leakage through status code | Explicitly map wrong-merchant read to masked `404 not_found` |
| Full team management scope creep | Use a single `merchant_id` claim only for this slice |
| ETag interpreted as conditional update support | Document that `If-Match` and `412` are absent until lifecycle slice |
| Keycloak local identities drift | Realm import and test JWT support both define payment roles/users deterministically |

## 16. Definition Of Done

Phase 2 planning is implementation-ready when:

- `payment` module is introduced with Spring Modulith boundaries.
- Merchant public eligibility API exists and payment does not depend on `merchant.internal`.
- Flyway migration creates payment tables with constraints and indexes.
- `POST /api/merchants/{merchantId}/payment-orders` returns `201` for first valid create.
- Same idempotency key and same fingerprint returns `200` with same payment order ID.
- Same idempotency key and different fingerprint returns `409 idempotency_conflict`.
- Non-active merchant creation returns `409 merchant_not_payment_eligible`.
- Amount/currency/client reference/idempotency key validation returns `400 validation`.
- `GET /api/merchants/{merchantId}/payment-orders/{paymentOrderId}` returns `200` for own merchant reader.
- Wrong merchant reader gets masked `404 not_found`.
- Platform payment reader can read any merchant payment order.
- Missing authentication returns `401`; missing authority returns `403`.
- `merchant:payments:operate` is registered but grants no create/read access.
- Create/read responses include `ETag` and `X-Correlation-ID`; first create includes `Location`.
- Initial status history row is persisted with correlation ID and actor subject.
- Backend unit, application, repository, REST, security, concurrency, and Modulith tests pass.
- Frontend typecheck and Playwright payment create/read/auth-denial journeys pass.
- `GET /api/status` remains public and payment-data-free.
- No lifecycle actions, `If-Match`, `412`, PSP, cards, Kafka, webhooks, refunds, settlement, reconciliation, list/search, GraphQL, or gRPC behavior exists.

## 17. Complexity Tracking

No constitution violations are introduced. No exception is required.

## 18. Implementation Readiness Assessment

The specification is clarified, and this plan resolves the material technical unknowns: idempotency scope, fingerprint derivation, ETag format, merchant public boundary, authority mapping, transaction model, API shape, and test data isolation.

The feature is ready for `/speckit.tasks`. It is not ready for direct implementation until tasks are generated and preserve the scope guardrails from this plan.
