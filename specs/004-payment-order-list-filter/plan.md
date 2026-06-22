# Implementation Plan: Payment Order List with Filtering, Pagination, and Sorting

**Branch**: `004-payment-order-create-read` | **Date**: 2026-05-28 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/004-payment-order-list-filter/spec.md`. Extends `003-payment-order-access-lifecycle` with list/filter/pagination/sort on the existing payment order resource.

## 1. Technical Summary

Lesson 07 adds the first collection endpoint to the payment domain: a filtered, paginated, sortable list of payment orders. This is a lesson extension within the existing `payment` Spring Modulith module — no new module, no new tables, no new roles.

The work introduces:

- `GET /api/merchants/{merchantId}/payment-orders` with 10 optional query parameters (`status`, `currency`, `fromDate`, `toDate`, `minAmount`, `maxAmount`, `clientOrderReference`, `page`, `size`, `sort`).
- Dynamic JPA Specification query composition (`JpaSpecificationExecutor<PaymentOrder>`) — the first `Specification<T>` usage in the codebase.
- Paginated response contract: `{ content: [...], page, size, totalElements, totalPages }` with `200 OK` and `X-Correlation-ID`.
- Query parameter validation via Spring `@Valid` on a request record.
- `400 validation` for invalid filters, pagination bounds, and sort values — with stable machine-readable error codes.
- `403 forbidden` for cross-tenant list access (overt refusal, distinct from masked `404` for single-resource reads).
- Flyway migration `V3__add_payment_order_list_indexes.sql` — three composite indexes for filtered/sorted list performance.
- Reusable REST Assured test support: `RequestSpecBuilder` with role pre-configuration, `ResponseSpecification` for success/error contracts, failure-only logging, `Authorization` header masking, typed DTO extraction via `extract().as(TypeRef)`, and custom AssertJ assertions.
- Test layering: REST Assured contract, parameterized tests, security matrix, repository/integration, and specification unit tests.

The backend remains in the same `payment.internal.*` package structure as spec/003. No new module, no new public API types, no `merchant.internal` access.

No frontend changes, Playwright tests, aggregation, lifecycle actions, PSP, Kafka, or other deferred topics are implemented.

## 2. Technical Context

| Area | Decision |
|---|---|
| Language/Version | Java 25 |
| Backend Dependencies | Spring Boot 4.0.6, Spring Framework 7, Spring Modulith 2.0.6, Spring Data JPA, Flyway, Maven 3.9.11 |
| Storage | PostgreSQL 18 — indexes only, no new tables |
| Auth | Keycloak 26.6.1 JWT Resource Server — no new roles, no realm changes |
| Testing | JUnit 6, AssertJ, REST Assured, Testcontainers PostgreSQL |
| Target Platform | Linux server/local lab |
| Project Type | Modular monolith — lesson extension within existing `payment` module |
| Performance Goals | Sub-second list queries for ≤1000 payment orders per merchant in local lab |
| Constraints | Read-only; no new module; no new public API types; no `merchant.internal` access; existing create/read tests must pass |
| Scale/Scope | Single collection endpoint with 10 query params, dynamic JPA queries, 3 new indexes, ~20 automated tests |

No unresolved `NEEDS CLARIFICATION` items remain.

## 3. Constitution Check

### Pre-Design Gate

| Principle | Status | Plan Response |
|---|---|---|
| Tester-Led Product Learning | PASS | 22 backlog topics explicitly mapped to REST Assured (7 methods), AssertJ (5), JUnit (4), SQL (5), Test Design (2). Every FR has a test oracle. Learning delta map is in the spec. |
| Spec-Driven Delivery | PASS | Spec defines business purpose, actors, scope, 5 user stories with acceptance scenarios, edge cases, 29 FRs, 7 NFRs, and 12 SCs. This plan traces FR-201–FR-229 to implementation. |
| Modular Monolith Boundaries | PASS | List behavior lives inside `payment.internal`. No new module. No new public API types. No `merchant.internal` dependency. Architecture verification unchanged. |
| Parallel-Ready Quality Engineering | PASS | Per-test merchant creation with unique payment references. Testcontainers isolation. Seeded test data per test, not shared. No ordered-test dependencies. |
| Security, Data Integrity, and Observability | PASS | Five-actor security matrix for list. Query param validation with stable error codes. ILIKE escaping for SQL injection prevention. `X-Correlation-ID` on all responses. Sort whitelist validation. |

### Post-Design Gate

*Gate deferred until implementation — no research.md, data-model.md, or contracts needed for this lesson extension (no new tables, no new API contract beyond single endpoint).*

## 4. Architecture Decisions

### Module Ownership

`payment` module — unchanged. List behavior is a new read path within the existing module. No new `@ApplicationModule`.

### REST Shape Addition

| Method | Path | Authority | Main Status Codes |
|---|---|---|---|
| `GET` | `/api/merchants/{merchantId}/payment-orders` | `merchant:payments:read` + merchant scope, or `platform:payments:read` | `200`, `400`, `401`, `403` |

Existing endpoints unchanged:

| Method | Path | Authority |
|---|---|---|
| `POST` | `/api/merchants/{merchantId}/payment-orders` | `merchant:payments:create` |
| `GET` | `/api/merchants/{merchantId}/payment-orders/{paymentOrderId}` | `merchant:payments:read` or `platform:payments:read` |

### Controller Decision

The list endpoint is added to the existing `PaymentOrderController` (same `@RequestMapping`) rather than a separate controller class. This keeps all payment-order HTTP behavior in one place and avoids ambiguity in Spring's `@RequestMapping` resolution.

Alternative considered: separate `PaymentOrderListController` class. Rejected because it adds unnecessary class for a single method that shares the same path root.

### Dynamic Query Design

`JpaPaymentOrderRepository` extends `JpaSpecificationExecutor<PaymentOrder>`. A new `PaymentOrderSpecification` utility class builds a `Specification<PaymentOrder>` from a validated `PaymentOrderListRequest` record:

```java
Specification<PaymentOrder> spec = Specification
    .where(PaymentOrderSpecification.hasMerchantId(merchantId))
    .and(PaymentOrderSpecification.hasStatus(request.status()))
    .and(PaymentOrderSpecification.hasCurrency(request.currency()))
    .and(PaymentOrderSpecification.createdBetween(request.fromDate(), request.toDate()))
    .and(PaymentOrderSpecification.amountBetween(request.minAmount(), request.maxAmount()))
    .and(PaymentOrderSpecification.clientOrderReferenceContains(request.clientOrderReference()));
```

All `where`/`and` methods return `Optional<Specification>` that coalesce to `null` (no-op) when the parameter is absent.

### Query Parameter Validation

A `PaymentOrderListRequest` record with `@Valid` annotations validates query parameters at the controller boundary:

```java
public record PaymentOrderListRequest(
    @Pattern(regexp = "CREATED") String status,
    @Pattern(regexp = "PLN|EUR|USD") String currency,
    String fromDate,
    String toDate,
    @PositiveOrZero Long minAmount,
    @PositiveOrZero Long maxAmount,
    String clientOrderReference,
    @PositiveOrZero Integer page,
    @Min(1) @Max(100) Integer size,
    @Pattern(regexp = "createdAt,(asc|desc)") String sort
) {}
```

Default values are applied at the controller method level (`@RequestParam(defaultValue = "0")`), not in the record.

The existing `PaymentExceptionHandler` is extended with method-argument validation error handling for query parameters.

### Security Integration

`SecurityConfig.java` — new matcher added before the single-resource `GET` matcher:

```java
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders")
    .hasAnyAuthority("merchant:payments:read", "platform:payments:read")
```

The single-resource matcher (`/api/merchants/*/payment-orders/*`) remains unchanged and appears AFTER the list matcher for correct resolution order.

`merchant_id` claim enforcement in the controller — identical pattern to existing `createPaymentOrder` and `getPaymentOrder`.

### Transaction Strategy

List queries run as `@Transactional(readOnly = true)`. The list service uses Spring Data's `Page<T>` return type, which automatically executes a `COUNT` query for `totalElements`.

### No ETag for List

Unlike single-resource create/read, the list endpoint does NOT return `ETag` headers. `ETag` in spec/003 was a compatibility point for lifecycle concurrency (`If-Match`). A collection list has no single representation version — pagination is transient.

## 5. Repository Structure Changes

Only additions within existing packages:

```text
apps/backend/
├── src/main/java/lab/paymentquality/payment/internal/
│   ├── application/
│   │   └── PaymentOrderListService.java              # NEW: @Transactional(readOnly=true), dynamic query
│   ├── infrastructure/
│   │   ├── JpaPaymentOrderRepository.java             # MODIFY: extends JpaSpecificationExecutor<PaymentOrder>
│   │   └── PaymentOrderSpecification.java             # NEW: static Specification builders
│   └── web/
│       ├── PaymentOrderController.java                # MODIFY: add listPaymentOrders() method
│       ├── PaymentOrderListRequest.java               # NEW: record with @Valid query param validation
│       ├── PaymentOrderListResponse.java              # NEW: record { content, page, size, totalElements, totalPages }
│       ├── PaymentOrderExceptionHandler.java          # MODIFY: add query-param validation handler
│       └── PaymentOrderListMapper.java                # NEW: Page<PaymentOrder> → PaymentOrderListResponse

apps/backend/src/main/resources/db/migration/payment/
└── V3__add_payment_order_list_indexes.sql             # NEW: three composite indexes

apps/backend/src/test/java/lab/paymentquality/
├── rest/
│   ├── PaymentOrderListRestAssuredTest.java           # NEW: ~12 contract tests
│   ├── PaymentOrderListParameterizedTest.java          # NEW: ~6 @ParameterizedTest filter combinations
│   ├── PaymentOrderListSecurityTest.java              # NEW: 5-actor matrix for list
├── payment/internal/infrastructure/
│   ├── PaymentOrderListRepositoryTest.java            # NEW: Specification + index tests
│   └── PaymentOrderSpecificationTest.java             # NEW: unit tests for each Specification builder
└── testsupport/
    ├── PaymentOrderListApiTestSupport.java             # NEW: seed data helper, RequestSpecBuilder
    ├── PaymentOrderAssertions.java                     # NEW: custom AssertJ for list responses
    └── RestAssuredLoggingConfig.java                   # NEW: failure-only logging + Authorization masking
```

### Files NOT Changed

- `PaymentOrderService.java`, `PaymentOrder.java`, `PaymentAmount.java`, `CurrencyCode.java` — unchanged.
- `KeycloakRealmRoleConverter.java` — unchanged (no new roles).
- `infra/keycloak/realms/payment-quality-realm.json` — unchanged.
- `TestJwtSupport.java` — all required tokens already exist.
- `PaymentModuleTest.java` — unchanged (no new module dependencies).
- `MerchantPaymentEligibility.java`, `MerchantPaymentEligibilityService.java` — unchanged.

## 6. Database Plan

### Migration V3

```sql
-- V3__add_payment_order_list_indexes.sql

CREATE INDEX idx_payment_orders_merchant_status
    ON payment_orders(merchant_id, status);

CREATE INDEX idx_payment_orders_merchant_created
    ON payment_orders(merchant_id, created_at DESC);

CREATE INDEX idx_payment_orders_merchant_currency
    ON payment_orders(merchant_id, currency);
```

### Index Justification

| Index | Query Pattern | Filter |
|---|---|---|
| `merchant_id, status` | `WHERE merchant_id = ? AND status = ?` | Status filter (most common) |
| `merchant_id, created_at DESC` | `WHERE merchant_id = ? ORDER BY created_at DESC` | Default sort + date range |
| `merchant_id, currency` | `WHERE merchant_id = ? AND currency = ?` | Currency filter |

All indexes are merchant-scoped (tenant isolation). The `merchant_id` leading column ensures all list queries benefit from index scans, not seq scans.

### No New Tables

No new tables, columns, FKs, or check constraints. The `payment_orders` schema from `V2__create_payment_orders.sql` is sufficient.

### Query Safety

- `clientOrderReference` ILIKE parameter uses JDBC `?` parameter binding — no SQL injection.
- `sort` parameter is whitelist-validated (`createdAt,asc` or `createdAt,desc` only) — no dynamic column interpolation.
- All numeric params (`page`, `size`, `minAmount`, `maxAmount`) use parameter binding.
- Date params use `java.time.LocalDate` → `java.sql.Date` via Spring conversion.

## 7. REST API Plan

### New Endpoint

```
GET /api/merchants/{merchantId}/payment-orders
```

**Query Parameters** (all optional):

| Param | Type | Default | Validation |
|---|---|---|---|
| `status` | String | none | `CREATED` only |
| `currency` | String | none | `PLN`, `EUR`, `USD` |
| `fromDate` | ISO date | none | Valid date, parsed as start-of-day |
| `toDate` | ISO date | none | Valid date, parsed as end-of-day |
| `minAmount` | Long | none | `>= 0` |
| `maxAmount` | Long | none | `>= 0` |
| `clientOrderReference` | String | none | Trimmed, ILIKE-escaped |
| `page` | int | 0 | `>= 0` |
| `size` | int | 20 | `1..100` |
| `sort` | String | `createdAt,desc` | `createdAt,asc` or `createdAt,desc` |

**Response `200 OK`:**

```json
{
  "content": [
    {
      "paymentOrderId": "uuid",
      "merchantId": "uuid",
      "amountMinor": 12500,
      "currency": "PLN",
      "status": "CREATED",
      "clientOrderReference": "PAY-001",
      "createdAt": "2026-05-28T18:00:00Z",
      "updatedAt": "2026-05-28T18:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

Response headers:
- `X-Correlation-ID` (propagated or generated)
- `Content-Type: application/json`

**Error Responses:**

| Condition | Status | Error Code |
|---|---|---|
| Invalid `page` (negative) | `400` | `validation` |
| Invalid `size` (0 or >100) | `400` | `validation` |
| Invalid `status` | `400` | `validation` |
| Invalid `currency` | `400` | `validation` |
| Malformed date | `400` | `validation` |
| Invalid `sort` | `400` | `validation` |
| Unauthenticated | `401` | — |
| No `merchant:payments:read` or `platform:payments:read` | `403` | `forbidden` |
| Cross-tenant (merchant reader for different merchant) | `403` | `forbidden` |

### Existing Endpoints Unchanged

POST create and GET single-resource are untouched. All existing tests must pass.

## 8. Security Plan

### SecurityConfig Addition

New matcher (MUST appear before the single-resource GET matcher):

```java
.requestMatchers(HttpMethod.GET, "/api/merchants/*/payment-orders")
    .hasAnyAuthority("merchant:payments:read", "platform:payments:read")
```

### Controller-Level Enforcement

The `listPaymentOrders()` method enforces `merchant_id` claim matching for non-platform readers:

```java
boolean isPlatformReader = authentication.getAuthorities().stream()
    .anyMatch(a -> a.getAuthority().equals("platform:payments:read"));

if (!isPlatformReader) {
    String jwtMerchantId = jwt.getClaimAsString("merchant_id");
    if (jwtMerchantId == null || !merchantId.toString().equals(jwtMerchantId)) {
        throw new AccessDeniedException("Merchant scope mismatch");
    }
}
```

### Security Matrix

| Actor | List Own | List Other | Expected |
|---|---|---|---|
| Unauthenticated | — | — | `401` |
| Denied identity | — | — | `403` |
| `merchant:payments:create` (no read) | — | — | `403` |
| `merchant:payments:read` + matching `merchant_id` | ✅ | ❌ | `200` / `403` |
| `merchant:payments:read` + mismatched `merchant_id` | ❌ | ❌ | `403` |
| `platform:payments:read` | ✅ | ✅ | `200` |
| `merchant:payments:operate` | — | — | `403` |

### Log Safety

- Query parameters logged at DEBUG level only.
- No `Authorization` headers in test logs (REST Assured filter blacklist).
- `clientOrderReference` never logged at INFO or above.

## 9. Testing Strategy

### Test Layers

| Layer | Test Class | What It Verifies | Count |
|---|---|---|---|
| REST Assured contract | `PaymentOrderListRestAssuredTest` | HTTP status, response shape, headers, pagination metadata, filter correctness | ~12 tests |
| REST Assured parameterized | `PaymentOrderListParameterizedTest` | Filter combinations via `@CsvSource` | ~6 tests |
| REST Assured security | `PaymentOrderListSecurityTest` | 5-actor matrix, cross-tenant refusal, role isolation | ~7 tests |
| Repository/integration | `PaymentOrderListRepositoryTest` | JpaSpecificationExecutor, indexes, pagination, empty result set | ~5 tests |
| Specification unit | `PaymentOrderSpecificationTest` | Each `Specification` builder produces correct WHERE clause | ~6 tests |

### Test Data Strategy

- Each test class creates one merchant + 5-10 payment orders with varied statuses/currencies/amounts/dates via `PaymentOrderListApiTestSupport`.
- Unique payment references per test (`PAY-{testMethod}-{uuid}`).
- No shared mutable fixtures. No cleanup needed (immutable payment orders).
- Testcontainers isolation — each test class gets its own PostgreSQL container.

### New REST Assured Patterns (7 methods — zero prior usage)

| Method | Where Used |
|---|---|
| `queryParam("status", "CREATED")` | All list contract tests |
| `accept(ContentType.JSON)` | All list contract tests |
| `extract().as(PaymentOrderListResponse.class)` | Typed extraction tests |
| `new RequestSpecBuilder().addHeader("Authorization", token).build()` | `PaymentOrderListApiTestSupport` |
| `new ResponseSpecBuilder().expectStatusCode(200).build()` | Shared success/error specs |
| `.log().ifValidationFails()` | `RestAssuredLoggingConfig` |
| `RestAssured.filters(new ResponseLoggingFilter(...))` | `RestAssuredLoggingConfig` (Authorization blacklist) |

### New AssertJ Patterns

- `assertThat(list).extracting("status").containsOnly("CREATED")`
- `assertThat(list).filteredOn("currency", "PLN").hasSize(3)`
- `assertThat(list).extracting("status", "currency").contains(tuple("CREATED", "PLN"))`
- `assertThat(response).usingRecursiveComparison().ignoringFields("createdAt").isEqualTo(expected)`
- `SoftAssertions` for multi-field response validation

### New JUnit Patterns

- `@ParameterizedTest` + `@CsvSource` — 6 filter combinations
- `@Nested class ListFiltering { }` — group filter tests
- `@Tag("list")` — CI categorization
- `@DisplayName("list filtered by status returns only CREATED orders")`

### Key Test Scenarios

1. Empty list (merchant with no orders) → 200, content=[], totalElements=0
2. No filters → all orders, default pagination
3. Single filter (status) → only matching orders
4. Multi-filter (status + currency + amount range) → AND conjunction
5. Date range filtering → inclusive boundaries
6. Pagination → correct page/size/totalElements/totalPages
7. Last page → correct count, totalPages
8. Page beyond data → empty content, totalElements unchanged
9. Sort ascending vs descending → correct order
10. Invalid params → 400 with stable error codes
11. Cross-tenant → 403 (not 404)
12. Platform reader → 200 cross-merchant

## 10. Verification Commands

```bash
cd apps/backend

# Full test suite
./mvnw test

# List contract tests
./mvnw -Dtest=PaymentOrderListRestAssuredTest test

# Parameterized filter tests
./mvnw -Dtest=PaymentOrderListParameterizedTest test

# Security tests
./mvnw -Dtest=PaymentOrderListSecurityTest test

# Repository/integration tests
./mvnw -Dtest=PaymentOrderListRepositoryTest test

# Specification unit tests
./mvnw -Dtest=PaymentOrderSpecificationTest test

# All payment order tests (existing + new)
./mvnw -Dtest="PaymentOrder*" test

# Spring Modulith architecture verification
./mvnw -Dtest=PaymentModuleTest test

# Frontend typecheck (unchanged)
cd ../frontend && corepack pnpm typecheck
```

## 11. Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `JpaSpecificationExecutor` breaks existing repository queries | Existing tests fail | Extend interface, don't replace. Run full `PaymentOrder*` test suite. |
| ILIKE escaping incomplete | SQL injection via `clientOrderReference` | Parameter binding. Dedicated test with `%`, `_`, `'` characters. |
| Sort whitelist bypass | SQL injection via `sort` param | Validate against enum before passing to `Sort.by()`. Test with `sort=invalid;DROP TABLE`. |
| Security matcher order wrong | List endpoint returns 401/403 incorrectly | Matcher order: list → single-resource. Test security matrix for both endpoints. |
| Index migration breaks existing tests | `V2__` tests fail | `V3` is additive, no schema changes. Flyway runs migrations in order. |
| `merchant:payments:create` accidentally gets list access | Authorization boundary leak | Explicit security test verifies create-only token gets 403 for list. |
| Query param default values don't apply | Controller method signature mismatch | `@RequestParam(defaultValue = "0")` on each param. Test with no query params. |
| `totalElements` incorrect | Pagination metadata wrong | Repository test seeds known count, asserts `totalElements` matches. |
| `Size` 0 accepted as valid | Undefined behavior | `@Min(1)` on size field. Test `size=0` → 400. |
| Cross-tenant list returns 404 (copy-paste from single-resource) | Information leak | Explicit test: merchant A reader on merchant B list → 403, not 404. |

## 12. Implementation Task Breakdown

| # | Task | Layer | Files | Dependencies |
|---|---|---|---|---|
| T001 | Create `PaymentOrderListRequest` record with validation annotations | Web | 1 new | None |
| T002 | Create `PaymentOrderListResponse` record | Web | 1 new | None |
| T003 | Create `PaymentOrderListMapper` | Web | 1 new | T002 |
| T004 | Create `PaymentOrderSpecification` static builder class | Infrastructure | 1 new | None |
| T005 | Extend `JpaPaymentOrderRepository` with `JpaSpecificationExecutor` | Infrastructure | 1 modify | T004 |
| T006 | Create `PaymentOrderListService` | Application | 1 new | T001, T005 |
| T007 | Add `listPaymentOrders()` method to `PaymentOrderController` | Web | 1 modify | T001, T002, T003, T006 |
| T008 | Add query param validation to `PaymentExceptionHandler` | Web | 1 modify | T007 |
| T009 | Add list matcher to `SecurityConfig` | Security | 1 modify | T007 |
| T010 | Create `V3__add_payment_order_list_indexes.sql` | Database | 1 new | None |
| T011 | Create `PaymentOrderListApiTestSupport` | Test support | 1 new | T007 |
| T012 | Create `PaymentOrderAssertions` custom AssertJ | Test support | 1 new | T002 |
| T013 | Create `RestAssuredLoggingConfig` | Test support | 1 new | None |
| T014 | Write `PaymentOrderListRestAssuredTest` | Test | 1 new | T011 |
| T015 | Write `PaymentOrderListParameterizedTest` | Test | 1 new | T014 |
| T016 | Write `PaymentOrderListSecurityTest` | Test | 1 new | T011 |
| T017 | Write `PaymentOrderListRepositoryTest` | Test | 1 new | T005 |
| T018 | Write `PaymentOrderSpecificationTest` | Test | 1 new | T004 |
| T019 | Run full test suite + Modulith verification | Verification | — | T014-T018 |

## 13. Documentation and Learning Outputs

- `knowledge-vault/01 Projects/Payment_Quality_Engineering_Lab/02 Phase 2 - Payment Orders/Lesson 07 - Payment Order List Filter Search.md` — lesson note
- Update `Lesson Evidence Tracker`, `Learning Progress Board`, `Senior SDET Competency Coverage Matrix`
- Run `Prompt - Post Sprint Evidence Update` from vault

## 14. Deferred Scope

| Topic | When |
|---|---|
| `GROUP BY`, aggregation, reporting | Lesson 08 |
| Multi-column sorting | Optional 07b extension |
| Frontend list page (Nuxt) | Optional 07b extension |
| Playwright E2E for list | Sprint 8+ |
| Rate limiting, cache headers | Sprint 12+ |
