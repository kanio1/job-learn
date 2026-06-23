# Phase 3–5 — Implementation Checklist

Companion to `REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md`.
Scope: context/auth/correlation skeleton (Phase 3), problem/error contract (Phase 4),
minimal data/value helpers (Phase 5).

Legend: `[x]` done · `[ ]` deferred

---

## Current Run Findings

| Question | Finding |
|---|---|
| Does `apps/api-tests/` exist before this run? | **No.** Created from scratch (Phase 1+2 docs existed but no code). |
| Phase 1+2 code state | **None** — docs described target state; implementation was never committed. |
| All Phases 1–5 implemented in this run? | **Yes.** Module, HTTP layer, context/auth, problem, data helpers all created. |
| `json-schema-validator` in `~/.m2`? | **No.** `matchesProblemSchema()` deferred; throws `UnsupportedOperationException`. |
| JDK version | 25.0.3 |
| Maven version | 3.9.11 |
| REST Assured | 6.0.0 (cached) |
| JUnit | 6.0.3 (cached) |
| AssertJ | 3.27.7 (cached) |
| Backend error shapes | Three variants: `GlobalExceptionHandler` (type/title/status/detail/correlationId/error), `PaymentExceptionHandler` (+ code/message/details[]), `MerchantExceptionHandler` (error/message/details{}) |

---

## Phase 1 — module skeleton

- [x] `apps/api-tests/pom.xml` — standalone Maven module, JDK 25, no Spring parent
  - [x] `junit-bom 6.0.3` + `testcontainers-bom 2.0.5` via `dependencyManagement`
  - [x] Active deps: `rest-assured 6.0.0`, `junit-jupiter`, `assertj-core 3.27.7`
  - [x] Deferred deps documented in-pom with phase labels
  - [x] `maven-surefire-plugin 3.5.4` runs `**/*Test.java`
  - [x] `maven-failsafe-plugin 3.5.4` runs `**/*Spec.java` (integration-test + verify)
  - [x] `maven-enforcer-plugin` pins Java `[25,26)` + Maven `[3.9.11,)`
- [x] `apps/api-tests/README.md`
- [x] `src/test/resources/junit-platform.properties` — parallelism off (single stack)
- [x] Layer `package-info.java` for: `apitest`, `core/http`, `core/auth`, `core/context`,
      `core/problem`, `core/data`, `core/concurrency`, `core/stack`, `api`, `support`, `scenarios`

---

## Phase 2 — core HTTP skeleton

- [x] `core/http/Headers.java` — all 11 constants with Javadoc rationale
- [x] `core/http/ContentTypes.java` — JSON, problem+json, merge-patch+json with charset note
- [x] `core/http/ApiConfig.java` — env/sysprop resolution, `fromStack()` throws until Phase 6
- [x] `core/http/RestAssuredSetup.java` — `install(String baseUri)`:
  - [x] Registers `application/problem+json` → `Parser.JSON`
  - [x] `EncoderConfig.appendDefaultContentCharsetToContentTypeIfUndefined(false)` (merge-patch fix)
  - [x] `LogConfig` log-if-validation-fails + pretty print
  - [x] `JsonConfig` number return type = `BIG_DECIMAL`
  - [x] Builds `BASE` (AuthFilter + CorrelationFilter + ErrorLoggingFilter) and `ANONYMOUS_BASE`
  - [x] Idempotent install (callable multiple times)
- [x] `core/http/RequestSpecs.java` — `base()`, `anonymous()`, `idempotent()`, `conditional()`,
      `lifecycle()`, `mergePatch()` — all return isolated `given().spec(BASE)` copies
- [x] `core/http/ResponseSpecs.java` — `sensitive()`, `problemJson()`, `conditional()`, `created()`
- [x] `CoreHttpWiringTest.java` — non-network unit test, all spec factories + constants

---

## Phase 3 — context/auth/correlation skeleton

- [x] `core/auth/TokenFactory.java` — `@FunctionalInterface`, `placeholder()`, `noOp()` static factories
- [x] `core/auth/Identity.java` — `anonymous()`, `of(name, factory)`, `of(name, roles, tenantId, factory)`
  - [x] `token()` delegates to factory; throws for anonymous
  - [x] `roles()` returns `List.copyOf` — immutable
  - [x] toString safe (no token)
- [x] `core/auth/Identities.java` — Object Mother: `ANONYMOUS`, `platformAdmin()`, `tenantAdmin(tenantId)`,
      `merchantReader(merchantId)`, `paymentCreator(merchantId)`, `paymentLifecycleOperator(merchantId)`
  - [x] All non-anonymous use `TokenFactory.placeholder()` — documented as Phase 6 TODO
- [x] `core/context/TestContext.java` — record: correlationId + identity + scenarioName
  - [x] `of(Identity)`, `of(correlationId, Identity)`, `of(corrId, Identity, scenario)`, `anonymous()`
- [x] `core/context/Ctx.java` — `ThreadLocal`-based holder, `set/current/currentOrNull/clear/isSet`
  - [x] `ScopedValue` deferred to Phase 6 — documented in Javadoc + package-info
- [x] `core/http/AuthFilter.java` — reads `Ctx.currentOrNull()`, skips if null or anonymous
  - [x] Token NOT logged
  - [x] Added to `BASE` (not to `ANONYMOUS_BASE`)
- [x] `core/http/CorrelationFilter.java` — reads `Ctx.currentOrNull()`, falls back to `CorrelationIds.generate()`
  - [x] Does not overwrite explicitly set header
  - [x] Added to both `BASE` and `ANONYMOUS_BASE`

---

## Phase 4 — problem/error contract

- [x] `core/problem/ProblemDetail.java` — local record covering all three backend error shapes
  - [x] No Spring imports, no backend DTO imports
- [x] `core/problem/ProblemCodes.java` — 13 constants, confirmed against actual exception handlers
- [x] `core/problem/ProblemAssert.java` — extends `AbstractAssert<ProblemAssert, Response>`:
  - [x] `hasStatus(int)`
  - [x] `hasContentTypeProblemJson()`
  - [x] `hasError(String)`
  - [x] `hasCorrelationId()` (presence check)
  - [x] `hasCorrelationId(String)` (exact match)
  - [x] `hasCorrelationIdConsistent()` (body == header)
  - [x] `hasPathContaining(String)` (checks `detail` field)
  - [x] `hasMessageContaining(String)` (checks `detail` or `message`)
  - [x] `hasNoStore()`
  - [x] `varyContains(String)` (case-insensitive)
  - [x] `hasFieldError(String)` (checks `details[].field`)
  - [x] `matchesProblemSchema()` — throws `UnsupportedOperationException` (deferred, needs json-schema-validator)
- [x] `src/test/resources/schema/problem.schema.json` — draft-07, `additionalProperties: true`
  - [x] Covers all three backend error shapes, permissive until shapes stabilise

---

## Phase 5 — minimal data/value helpers

- [x] `core/data/CorrelationIds.java` — `generate(prefix)`, `generate()`, `isValid()`, sanitize
- [x] `core/data/IdempotencyKeys.java` — `generate(scenario)`, `generate()`, sanitize
- [x] `core/data/UniqueReferences.java` — `merchantRef()`, `merchantRef(label)`, `paymentRef(label)`,
      `of(prefix)`, `uuid()`
- [x] `core/data/ETag.java` — `of(raw)`, `raw()`, `version()`, `isQuoted()`, `equals/hashCode`
  - [x] Validates `"vN"` pattern via `Pattern`
  - [x] `version()` throws for non-`"vN"` ETags
- [x] `core/data/Versioned.java` — `record Versioned<T>(T body, ETag etag)`, `of(body, rawEtag)`, `of(body, ETag)`
  - [x] `Objects.requireNonNull` on both fields via compact constructor

---

## Tests added

- [x] `CoreHttpWiringTest` — 14 tests (Phase 2+3 wiring)
- [x] `ContextAndFilterWiringTest` — 16 tests (Phase 3)
- [x] `ProblemAssertTest` — 16 tests (Phase 4, uses `ResponseBuilder` — no network)
- [x] `DataHelpersTest` — 23 tests (Phase 5)

---

## Validation command

```bash
cd apps/api-tests && mvn -q test
```

Expected: 4 test classes, ~69 test methods, no containers, no network for dependencies.

---

## Deferred work

| Item | Phase | Why deferred |
|---|---|---|
| `json-schema-validator` dep | 4+ | Not in `~/.m2`, needs network download |
| `ProblemAssert.matchesProblemSchema()` | 4+ | Blocked by above |
| `core/stack/` — Testcontainers | 6 | Requires backend Docker image + Podman |
| `KeycloakTokenFactory` | 6 | Requires live Keycloak |
| `ScopedContextExtension` + `ScopedValue` | 6 | Requires `InvocationInterceptor` wiring |
| `ApiTest` meta-annotation + extensions | 6 | Blocked by stack |
| `StatusApi`, `SeedApi`, `MerchantsApi`, `PaymentOrdersApi` | 6 | Blocked by stack |
| `ConcurrencyHarness` | 7+ | Requires parallel live tests |
| `PaymentLifecycleSpec`, `TenantIsolationSpec`, `AuditTrailSpec` | 7+ | Blocked by stack + clients |

---

## SDET learning coverage (this batch)

| Concept | Where taught |
|---|---|
| `RequestSpecBuilder` immutable template vs mutable `given()` copy | `RequestSpecs` Javadoc + `CoreHttpWiringTest` |
| `ResponseSpecBuilder` reusable contracts | `ResponseSpecs` + `CoreHttpWiringTest` |
| `Filter` interface — auth/correlation cross-cutting concern | `AuthFilter`, `CorrelationFilter` |
| `ThreadLocal` for per-test context passing | `Ctx` Javadoc + `ContextAndFilterWiringTest` |
| `AbstractAssert<S,A>` custom assertion | `ProblemAssert` + `ProblemAssertTest` |
| Object Mother pattern for personas | `Identities` + `ContextAndFilterWiringTest` |
| Strategy pattern — `TokenFactory` | `TokenFactory` + `Identity` |
| Value Object — `ETag` | `ETag` + `DataHelpersTest` |
| Result wrapper — `Versioned<T>` | `Versioned` + `DataHelpersTest` |
| Correlation ID strategy | `CorrelationIds` + `CorrelationFilter` + `TestContext` |
| `ResponseBuilder` for unit-testing assertions without network | `ProblemAssertTest` |

---

## Risks / review notes

- `MerchantExceptionHandler` returns `{error, message, details}` — no `correlationId` in body.
  `ProblemAssert.hasCorrelationId()` will fail on merchant 4xx responses until the handler is
  updated to include `correlationId`. Noted in `ProblemDetail` Javadoc.
- `ProblemCodes.MERCHANT_NOT_ELIGIBLE` is derived from `PaymentExceptionHandler` code —
  verify against live backend in Phase 7 as the first contract test.
- `Identities.platformAdmin()` etc. produce `placeholder-*` tokens that will cause 401 against a
  live backend. Replace factory references in Phase 6 when `KeycloakTokenFactory` is wired.

---

## Next phase (suggested prompt)

Phase 6: Add `json-schema-validator` (requires network), implement `core/stack/` with
Testcontainers (`PostgresSupport`, `KeycloakSupport`, `BackendSupport`, `ApiStack`), implement
`ApiTest` meta-annotation + `ApiStackExtension` + `SeedLifecycleExtension`, add `KeycloakTokenFactory`,
update `Identities` to use it, add `StatusApi` + `SeedApi` stubs, add first smoke `*Spec`.
