# api-tests — Black-Box REST Assured 6 Framework

Standalone Maven module. Tests the `payment-quality-lab` backend through HTTP only.

No `@SpringBootTest`. No MockMvc. No backend DTOs. No `restkit/` or `testsupport/` imports.

---

## Quick start

```bash
# Offline unit tests (no network, no containers):
cd apps/api-tests && mvn -q test

# Live integration tests (Phase 7+, requires backend image + Podman/Docker):
export BACKEND_IMAGE=payment-quality/backend:local
cd apps/api-tests && mvn -q verify
```

---

## Layer map

```
core/http        HTTP plumbing: Headers, ContentTypes, ApiConfig, RestAssuredSetup,
                 RequestSpecs, ResponseSpecs, AuthFilter, CorrelationFilter
core/auth        API actors: Identity, Identities, TokenFactory
core/context     Per-thread test context: TestContext, Ctx
core/problem     Error contract: ProblemDetail, ProblemCodes, ProblemAssert
core/data        Data helpers: CorrelationIds, IdempotencyKeys, UniqueReferences, ETag, Versioned<T>
core/stack       Testcontainers stack (Phase 6 — deferred)
api/*            Per-resource thin clients: StatusApi, MerchantsApi, PaymentOrdersApi (Phase 6)
support/         JUnit extensions, lifecycle hooks (Phase 6)
scenarios/       Live *Spec tests (Phase 7+)
```

---

## What runs offline (mvn test)

| Test class | Category | Phase |
|---|---|---|
| `CoreHttpWiringTest` | Framework wiring | 2+3 |
| `ContextAndFilterWiringTest` | Auth/correlation infrastructure | 3 |
| `ProblemAssertTest` | Problem/error contract support | 4 |
| `DataHelpersTest` | Test data helper | 5 |

---

## What is deferred

| Item | Phase | Reason |
|---|---|---|
| `json-schema-validator` dep + `matchesProblemSchema()` | 4+ | Not in local `~/.m2`; needs network |
| Testcontainers stack (`core/stack/`) | 6 | Requires backend Docker image + Podman socket |
| `KeycloakTokenFactory` | 6 | Requires live Keycloak container |
| API clients (`api/`) | 6 | Blocked by stack |
| Live `*Spec` scenarios | 7+ | Blocked by stack + clients |
| `ScopedValue<TestContext>` context | 6 | Requires `InvocationInterceptor` in `ScopedContextExtension` |
| `ConcurrencyHarness` | 7+ | Requires parallel live tests |

---

## Environment variables

| Variable | Default | Used by |
|---|---|---|
| `API_BASE_URI` | `http://localhost:8080` | `ApiConfig.fromEnvironment()` |
| `api.base.uri` (system property) | `http://localhost:8080` | `ApiConfig.fromEnvironment()` |
| `BACKEND_IMAGE` | — | Testcontainers stack (Phase 6) |

Do NOT commit real credentials, tokens, or DSNs.

---

## SDET learning topics covered

- `RequestSpecBuilder.build()` — immutable template vs mutable `given()` copy
- `ResponseSpecBuilder` — reusable response contracts (Vary, Cache-Control, ETag)
- REST Assured `Filter` interface — `AuthFilter`, `CorrelationFilter`
- `application/problem+json` parser registration
- `EncoderConfig` charset fix for `application/merge-patch+json`
- `JsonConfig` + `BIG_DECIMAL` for financial amounts
- `AbstractAssert<S, A>` custom assertion — `ProblemAssert`
- `ThreadLocal` test context passing (→ `ScopedValue` in Phase 6)
- Correlation ID strategy — one ID per test, visible in backend logs
- Idempotency key generation — safe for retry and replay testing
- ETag value object — prevents unquoted-value bugs
- `Versioned<T>` — makes ETag contract explicit in return types
