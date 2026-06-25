# Phase 8O — JUnit Tags and Selective Run Workflow

## Scope

Add lightweight JUnit tags to live REST Assured scenario specs and document selective Maven runs.

No backend code, business tests, assertions, class names, or test structure changed.

## Maven / JUnit Discovery

`apps/api-tests` uses:

- `maven-surefire-plugin` 3.5.4 for offline `*Test.java`;
- `maven-failsafe-plugin` 3.5.4 for live `*Spec.java`;
- JUnit Jupiter from the JUnit 6.0.3 BOM.

No POM change was needed. Surefire/Failsafe already support JUnit Platform tag filtering through:

- `-Dgroups=<tag>`;
- `-DexcludedGroups=<tag>`.

Default commands remain unchanged:

```bash
cd apps/api-tests && mvn -q test
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify
```

## Tag Taxonomy

| Tag | Purpose |
|---|---|
| `smoke` | Fast health/security confidence checks |
| `contract` | Resource contract and response semantics |
| `security` | Auth, BOLA/BFLA, tenant/merchant boundaries |
| `lifecycle` | Payment state transitions, history, refund flow |
| `idempotency` | Create/lifecycle replay and conflict behavior |
| `audit` | Async audit/event visibility |
| `schema` | JSON schema drift checks |
| `http` | Method/content-negotiation/merge-patch semantics |
| `concurrency` | Race and optimistic-locking behavior |
| `regression` | Previously discovered bug fixes |

## Tag Placement

Class-level tags were used where the whole class has one clear risk area:

- `StatusSpec` -> `smoke`;
- `SecuritySmokeSpec` -> `smoke`, `security`;
- `MerchantsContractSpec` -> `contract`;
- `PaymentSummaryContractSpec` -> `contract`;
- `AuditContractSpec` -> `audit`;
- `TenantIsolationContractSpec` -> `security`;
- `LifecycleIdempotencyContractSpec` -> `idempotency`, `lifecycle`;
- `PartialRefundContractSpec` -> `contract`, `lifecycle`;
- `PatchMetadataContractSpec` -> `contract`, `http`;
- `HttpMethodSemanticsContractSpec` -> `http`;
- `JsonSchemaContractSpec` -> `schema`.

`PaymentOrdersContractSpec` keeps one class-level `contract` tag and uses method-level tags for
mixed concerns: `security`, `idempotency`, `lifecycle`, `concurrency`, and `regression`.

## Documented Selective Commands

| Command | Phase 8O status |
|---|---|
| `mvn verify -Dgroups=smoke` | verified; 3 live tests |
| `mvn verify -Dgroups=security` | documented only |
| `mvn verify -Dgroups=contract` | documented only |
| `mvn verify -DexcludedGroups=concurrency` | documented only |

```bash
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -Dgroups=smoke
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -Dgroups=security
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -Dgroups=contract
cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -DexcludedGroups=concurrency
```

Tagged `mvn verify` applies the filter to both Surefire and Failsafe. Because offline `*Test`
classes are intentionally untagged, the smoke subset reports zero Surefire tests and three live
Failsafe tests.

## Validation

- Offline: `cd apps/api-tests && mvn -q test` — 79 tests passed.
- Selective live: `cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify -Dgroups=smoke` — 3 live tests passed.
- Full live: `cd apps/api-tests && BACKEND_IMAGE=payment-quality/backend:local mvn verify` — 72 live tests passed.
