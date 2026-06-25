# api-tests — Black-Box REST Assured Framework

Standalone Maven module for live HTTP contract testing of `payment-quality-lab`.

Rules: no backend production imports, no `@SpringBootTest`, no MockMvc, no backend DTO reuse.
Scenario specs call API facades; facades own REST Assured request mechanics.

## Run

```bash
# Offline framework/unit tests only
cd apps/api-tests && mvn -q test

# Live black-box API specs; requires local backend image + container runtime
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
```

Current baseline through Phase 8S: `79` offline tests, `72` live tests.

## Selective Live Runs

JUnit tags are optional filters. Default `mvn verify` still runs every live `*Spec`.

```bash
# Smoke only: status + security smoke
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -Dgroups=smoke

# Security boundary specs
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -Dgroups=security

# Contract-oriented specs
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -Dgroups=contract

# Skip race-sensitive concurrency specs during quick local iteration
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -DexcludedGroups=concurrency
```

Maven Surefire/Failsafe 3.5.4 maps `groups` and `excludedGroups` to JUnit Platform tag filters.
Tagged `mvn verify` applies the filter to both Surefire and Failsafe; offline `*Test` classes are
intentionally untagged, so selective live commands may report zero Surefire tests. Use plain
`mvn -q test` for offline framework validation.

## Framework Index

| Area | Location | Purpose |
|---|---|---|
| HTTP specs | `src/test/java/.../core/http` | `RequestSpecs`, `ResponseSpecs`, headers, content types, RA setup, auth/correlation filters |
| Auth/context | `src/test/java/.../core/auth`, `core/context` | Keycloak-backed personas, token factory, `Ctx`/`TestContext` per-test identity and correlation |
| Test data | `src/test/java/.../core/data` | idempotency keys, unique refs, fixed seed IDs, ETag/version helpers |
| Problem/schema | `src/test/java/.../core/problem` | `ProblemAssert`, `ProblemCodes`, `SchemaAssertions` |
| Stack | `src/test/java/.../core/stack`, `support` | Testcontainers backend/Postgres/Keycloak orchestration and JUnit extension |
| API facades | `src/test/java/.../api` | `StatusApi`, `SeedApi`, `MerchantsApi`, `PaymentOrdersApi`, `AuditApi` |
| Scenarios | `src/test/java/.../scenarios` | Live `*Spec` classes grouped by contract/risk area |
| JSON schemas | `src/test/resources/schema` | `problem`, `payment-order`, `payment-summary` schema contracts |
| Keycloak realm | `src/test/resources/keycloak` | Test users, roles, tenant/merchant claims |

## Test-Suite Map

| Suite | Class |
|---|---|
| Smoke/status | `StatusSpec` |
| Security smoke | `SecuritySmokeSpec` |
| Merchant contract | `MerchantsContractSpec` |
| Payment contract and lifecycle happy/negative paths | `PaymentOrdersContractSpec` |
| Lifecycle idempotency | `LifecycleIdempotencyContractSpec` |
| Refund boundaries | `PartialRefundContractSpec` |
| PATCH / JSON Merge Patch | `PatchMetadataContractSpec` |
| HTTP method semantics | `HttpMethodSemanticsContractSpec` |
| JSON schema validation | `JsonSchemaContractSpec` |
| Audit / async eventual consistency | `AuditContractSpec` |
| Payment summary/reporting | `PaymentSummaryContractSpec` |
| Tenant / merchant isolation | `TenantIsolationContractSpec` |

## Phase 8 Index

| Phase | Added |
|---|---|
| 8A | Awaitility-backed async audit event contract |
| 8B | Payment summary/reporting contract |
| 8C | Tenant/merchant isolation matrix |
| 8D | Authorize/capture lifecycle idempotency replay and conflict |
| 8E | PATCH / JSON Merge Patch contract |
| 8F | Partial refund and refund negative boundaries |
| 8G | HEAD/OPTIONS/DELETE/Accept method semantics |
| 8H | JSON schema validation foundation |
| 8I | `ResponseSpecs` cleanup and wider reuse |
| 8J | PATCH stale `If-Match` -> 412 version mismatch |
| 8K | Cancel from AUTHORIZED contract |
| 8L | Refund idempotency replay contract |
| 8M | Tenant admin merchant boundary |
| 8N | Documentation cleanup and this framework index |
| 8O | JUnit tags and selective run workflow |
| 8P | Pre-commit consistency review |
| 8Q | Targeted cleanup fixes from review |
| 8R | Final commit-prep diff review |
| 8S | Final documentation ledger polish |

## Tag Map

| Tag | Purpose | Examples |
|---|---|---|
| `smoke` | Fast health/security confidence | `StatusSpec`, `SecuritySmokeSpec` |
| `contract` | HTTP resource contract and response semantics | `MerchantsContractSpec`, `PaymentOrdersContractSpec`, `PatchMetadataContractSpec` |
| `security` | Auth, BOLA/BFLA, tenant/merchant boundaries | `SecuritySmokeSpec`, `TenantIsolationContractSpec` |
| `lifecycle` | Payment state transitions, history, refund flow | lifecycle methods in `PaymentOrdersContractSpec`, `PartialRefundContractSpec` |
| `idempotency` | Create/lifecycle replay and conflict behavior | `LifecycleIdempotencyContractSpec`, idempotency methods in `PaymentOrdersContractSpec` |
| `audit` | Async audit/event visibility | `AuditContractSpec` |
| `schema` | JSON schema drift checks | `JsonSchemaContractSpec` |
| `http` | Method/content-negotiation/merge-patch semantics | `HttpMethodSemanticsContractSpec`, `PatchMetadataContractSpec` |
| `concurrency` | Race and optimistic-locking behavior | concurrent methods in `PaymentOrdersContractSpec` |
| `regression` | Locked-in fixes for previously found bugs | refund-on-authorized and create-race regression methods |

## Navigation Notes

- Use `RequestSpecs.lifecycle(ifMatch, key)` for lifecycle POSTs requiring both `If-Match` and `Idempotency-Key`.
- Use `ResponseSpecs.sensitive()` for sensitive read responses and `ResponseSpecs.conditional()` for lifecycle/PATCH mutation responses.
- Use `ProblemAssert` for problem+json errors; add schema checks only where structural drift is the target.
- Use deterministic IDs from `Seeds` for seeded cross-tenant/security cases; use API-created orders for lifecycle/idempotency flows.
