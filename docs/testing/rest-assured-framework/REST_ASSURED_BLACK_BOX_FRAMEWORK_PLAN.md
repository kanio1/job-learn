# REST Assured Black-Box API Test Framework — Plan

> Living plan for the new `apps/api-tests/` module. This file is the human-facing
> companion to the binding spec `FRAMEWORK-extended-http-rest-restassured.md` (repo root).
> Where the two ever disagree, the spec wins; this file records *current repo reality*,
> the phased build order, and the learning coverage.
>
> No secrets, DSNs, tokens, or credentials belong in this file. Environment-variable
> placeholders only.

---

## 1. Purpose

Build a professional, **black-box** REST Assured 6 test framework that exercises the
`payment-quality-lab` backend through HTTP only. It is both a test suite and a **learning
artifact** for Senior QA Automation / SDET practice: it must teach the REST Assured surface,
HTTP/REST contract thinking, and clean Java 25 test architecture.

Black-box invariants (hard):

- Test the backend through HTTP only.
- No imports of `lab.paymentquality.*` backend application classes.
- No `@SpringBootTest`, no MockMvc, no backend DTOs on the test classpath.
- Do **not** reuse or extend the legacy `restkit/` or `testsupport/` packages — they are
  historical context / anti-pattern reference only.
- Local DTOs/records on the test side, in `api/*/dto`.
- Specs built once as immutable templates via `RequestSpecBuilder` / `ResponseSpecBuilder`;
  no global mutable `RestAssured.baseURI` / `RestAssured.requestSpecification`.

---

## 2. Current repository findings (Current Run Findings)

Verified on branch **`001-project-foundation`** (note: `CLAUDE.md` and `.codex/current-state.md`
still reference the older `018-…` branch; the working branch is `001-project-foundation`).

| Question | Finding |
|---|---|
| Does `apps/api-tests/` exist? | **No.** Created by this work. |
| Does the spec's "skeleton already exists" claim hold? | **No.** The spec (§intro) says signatures + TODO already exist; they do not. Phase 1 creates the module from scratch. |
| Existing REST Assured tests | `apps/backend/src/test/java/lab/paymentquality/rest/*` (in-process, Spring-context, `@ActiveProfiles("test")`) and legacy `restkit/` contract/smoke/spec tests. All in-process — **not** black-box. |
| Helpers that MUST NOT be reused | `…/restkit/**` and `…/testsupport/**` (incl. `testsupport/restkit/**`). Read-only reference. |
| Backend stack | Spring Boot 4.0.6, Java 25, Spring Modulith 2.0.6, PostgreSQL 18 (Flyway), Keycloak resource server. |
| Backend test libs (repo truth) | REST Assured **6.0.0**, JUnit **6.0.3** (`junit-bom`), Testcontainers **2.0.5**, AssertJ 3.27.7, Awaitility 4.3.0, Hamcrest 3.0 — all in local `~/.m2`. `json-schema-validator` is **not** cached (needs network → deferred to Phase 4). |
| Build tooling | Maven 3.9.11, JDK 25.0.3, compiler/surefire/failsafe/enforcer plugins cached. |

### Backend endpoint reality (confirmed in `…/main/java`)

All endpoints in the spec's §18 table exist in code:

- `GET /api/status` — public (`StatusController`, `SecurityConfig` `permitAll`).
- `POST/GET /api/merchants`, `GET /api/merchants/{id}`, `…/activate`, `…/suspend` (`MerchantController`).
- Full payment lifecycle under `/api/merchants/{merchantId}/payment-orders/**`
  (create, list, summary, get, HEAD, PATCH, history, authorize/capture/cancel/refund) — `PaymentOrderController`.
- `/api/audit`, `/api/audit/{id}` (`AuditController`), `/api/users/**` (`UserManagementController`).
- `POST /api/test/seed` and `POST /api/test/reset` — `TestController`, guarded by
  `@ConditionalOnProperty(app.testing.enabled=true)` + `@Profile("!prod")`, `permitAll` in `SecurityConfig`.

Deterministic seed (`testing/internal/seed/*`): fixed UUIDs (`…0000b1/b2/b3` merchants,
`…0000a1/a2` tenants) — usable as stable references once the seed profile is active.

`SecurityConfig` exposes `ETag, Cache-Control, Vary, X-Correlation-ID, Location` via CORS and
applies `no-store` / `Vary` on sensitive resources — matches the spec's `ResponseSpecs` contract.

### How the backend can be driven black-box

The spec's run model (§3) is: build the backend as a Docker image
(`./mvnw spring-boot:build-image`), run it under Testcontainers with PostgreSQL 18 + Keycloak 26
on a shared network, env-driven (`SPRING_PROFILES_ACTIVE=seed`), readiness via
`GET /api/status == 200`. Tokens are minted against Keycloak's host-mapped port while the
backend validates `iss` against the network alias (issuer-mismatch fix, spec §17).

**This is real but heavy and belongs to Phase 3+ (stack/auth).** Phases 1–2 build the module
and the HTTP plumbing without containers, so they compile and run offline.

---

## 3. Interpretation of `FRAMEWORK-extended-http-rest-restassured.md`

The spec is **binding and exhaustive**: it fixes the directory tree, file names, the immutable
`BASE` request spec, `ResponseSpecs`, the problem+json error contract, `ProblemAssert extends
AbstractAssert`, JSON-schema contract tests, Awaitility for async audit, the issuer-mismatch fix,
and a 15-step implementation order. Key non-obvious decisions it mandates:

- **Test side stays on Jackson 2** (not `ObjectMapperType.JACKSON_3`) — RA 6.0.0 bug #1857.
- **Register `application/problem+json` → `Parser.JSON`** or error-body assertions throw.
- **`EncoderConfig.appendDefaultContentCharsetToContentTypeIfUndefined(false)`** so
  `application/merge-patch+json` is sent without a `charset` suffix.
- **`JsonConfig` number return type = `BIG_DECIMAL`** for financial precision.
- **Tenant boundary:** read violation → `404`, write violation → `403`.
- **Collections via `new TypeRef<List<T>>(){}`**, never `List.class`.
- **Idempotency create = 201, replay = 200**; `Versioned<T>` carries body + ETag.

This plan **adopts the spec's package tree** (`lab.paymentquality.apitest.*`) rather than the
prompt's generic `core/stack`-style paths, because the spec is local project truth and already
maps the same responsibilities. The prompt's layer separation is preserved 1:1.

---

## 4. Gap analysis — current repo vs target framework

| Area | Current | Target | Phase |
|---|---|---|---|
| Module | none | `apps/api-tests/` standalone Maven module | 1 ✅ |
| Build | n/a | RA6 + JUnit6 + AssertJ (+TC/Awaitility/JSV later) | 1 ✅ |
| `core/http` | n/a (legacy `restkit/spec`) | immutable `BASE`, `RequestSpecs`, `ResponseSpecs`, parser/charset/number config | 2 ✅ |
| `core/context` + filters | n/a | `Ctx` (ScopedValue), `AuthFilter`, `CorrelationFilter` | 3 |
| `core/stack` | in-process Spring | Testcontainers backend image + PG18 + Keycloak26 | 3 |
| `core/auth` | `TestJwtSupport` (in-proc) | `KeycloakTokenFactory` + `Identities` + realm import | 3 |
| `core/problem` | legacy `ProblemDetailsAssertions` | `ProblemDetail`, `ProblemCodes`, `ProblemAssert extends AbstractAssert` | 4 |
| `resources/schema` | none | `problem.json`, `payment-order.json`, … + `json-schema-validator` dep | 4 |
| `core/data` | legacy generators | `IdempotencyKeys`, `CorrelationIds`, `ETag`, `Versioned<T>`, deterministic seeds | 5 |
| `api/*` clients | legacy `MerchantApi`/`PaymentOrderApi` | thin `StatusApi`/`SeedApi`/`MerchantsApi`/`PaymentOrdersApi`/`AuditApi` | 6 |
| `scenarios/*` | legacy contract tests | smoke → contract → lifecycle → idempotency → tenant → audit `*Spec` | 7+ |

---

## 5. Target architecture — four thin layers

```
LAYER              RESPONSIBILITY                       FORBIDDEN IN SCENARIOS
core/stack         container lifecycle                  given(), pathParam()
core/http          request/response specifications      new RequestSpecBuilder()
api/*              per-resource domain DSL              contentType(), header()
scenarios/*        what we test (end-to-end asserts)    everything above
```

Glue: `Ctx` (`ScopedValue<TestContext>`) carries identity + `correlationId`. Filters read `Ctx`
and inject `Authorization` + `X-Correlation-ID`. Scenarios never touch auth/correlation headers.

### Proposed package tree (spec §5, adopted)

```
apps/api-tests/
├── pom.xml
├── README.md
└── src/test/
    ├── java/lab/paymentquality/apitest/
    │   ├── core/
    │   │   ├── stack/      (Phase 3)  BackendImage, PostgresSupport, KeycloakSupport, BackendSupport, ApiStack
    │   │   ├── http/       (Phase 2)  ApiConfig, ContentTypes, Headers, RequestSpecs, ResponseSpecs,
    │   │   │                          RestAssuredSetup [, AuthFilter, CorrelationFilter → Phase 3]
    │   │   ├── auth/       (Phase 3)  Identity, Identities, TokenFactory, KeycloakTokenFactory
    │   │   ├── context/    (Phase 3)  TestContext, Ctx
    │   │   ├── problem/    (Phase 4)  ProblemDetail, ProblemCodes, ProblemAssert
    │   │   ├── data/       (Phase 5)  IdempotencyKeys, CorrelationIds, UniqueNames, Seeds
    │   │   └── concurrency/(Phase 5)  ETag, Versioned<T>, ConcurrencyHarness
    │   ├── api/            (Phase 6)  ApiClient, payment/, merchant/, audit/, seed/ (+ dto records)
    │   ├── support/        (Phase 3)  ApiTest, ApiStackExtension, SeedLifecycleExtension, ScopedContextExtension, Eventually
    │   └── scenarios/      (Phase 7+) payment/, tenant/, audit/  (*Spec)
    └── resources/
        ├── junit-platform.properties
        ├── keycloak/payment-quality-realm.json   (Phase 3)
        └── schema/{problem,payment-order,merchant,payment-list}.json  (Phase 4)
```

> Phase 1 materialises the layer packages with `package-info.java` (documented intent).
> Empty leaf dirs are not committed (git ignores them); they appear as their phase lands.

---

## 6. Phased build order

| Phase | Scope | Status |
|---|---|---|
| 0 | Repo + spec review, gap map | ✅ done |
| 1 | Module skeleton: `pom.xml`, `README.md`, `junit-platform.properties`, layer `package-info.java`, plan docs | ✅ done |
| 2 | `core/http`: `Headers`, `ContentTypes`, `ApiConfig`, `RestAssuredSetup`, `RequestSpecs`, `ResponseSpecs` + wiring test | ✅ done |
| 3 | `core/context` (`Ctx`, `TestContext`), `core/auth` (`Identity`, `Identities`, `TokenFactory`), `AuthFilter`, `CorrelationFilter` | ✅ done |
| 4 | `core/problem` (`ProblemDetail`, `ProblemCodes`, `ProblemAssert`), `schema/problem.schema.json` | ✅ done (json-schema-validator dep deferred — not in ~/.m2) |
| 5 | `core/data` (`CorrelationIds`, `IdempotencyKeys`, `UniqueReferences`, `ETag`, `Versioned<T>`) | ✅ done |
| 6A | Stack discovery: runtime reality, image strategy, Keycloak strategy, first endpoint plan | ✅ done |
| 6B-lite | TC deps (core/postgresql/junit-jupiter), `PostgresSupport`, `BackendSupport`, `ApiStack`, `ApiStackExtension`, `ApiTest`, `StatusApi`, `StatusSpec` | ✅ done |
| 6B-full | `KeycloakSupport`, `KeycloakTokenFactory`, update `Identities`, `SeedApi` | deferred |
| 6C | Keycloak 26 TC container, ROPC token factory, `SecuritySmokeSpec` (401 + 403 without seed data) | ✅ done |
| 6D | Local merchant DTOs, `MerchantsApi` facade, `SeedApi`, `MerchantsContractSpec` (8 scenarios: 201/200/400/409/404) | ✅ done |
| 6E | Contract hardening: `activate`/`suspend` API + 5 new contract tests (lifecycle 200, invalid_transition 409, missing tenantRef 400, tenant-filter list), refactor 5 error tests to ProblemAssert, add `DUPLICATE_MERCHANT_REFERENCE`/`INVALID_TRANSITION` to `ProblemCodes` | ✅ done |
| 7A | Payment order contract foundation: `Seeds`, `PaymentOrdersApi` (GET, LIST), `PaymentOrdersContractSpec` (6 specs: GET body, headers, AUTHORIZED status, LIST pagination, 404, 400 malformed); create deferred (Keycloak realm gap) | ✅ done |
| 7B | Keycloak realm fix (`merchant.alpha.creator` user with real merchant UUID), `seededMerchantCreator()` persona, `CreatePaymentOrderRequest` DTO, `PaymentOrdersApi.create()` + `createWithoutIdempotencyKey()`, 3 create specs (201 + 403 scope mismatch + 400 missing Idempotency-Key) | ✅ done |
| 7+ | Tenant isolation, merchant lifecycle boundary, lifecycle (authorize/capture/cancel/refund), PATCH, history, summary | deferred |

Spec §19 ordering (stack → http → context → auth → support → seed → payment → problem → schema →
**first green `PaymentLifecycleSpec`** → rest) governs Phases 3–7.

---

## 7. REST Assured learning coverage matrix

| Level | API / concept | Phase landed |
|---|---|---|
| L1 | `given/when/then`, `RequestSpecification`, `Response`, `ContentType`, `.header/.body/.pathParam/.queryParam`, `.extract().as()`, `.jsonPath()` | 6–7 (via clients/scenarios) |
| L2 | `RequestSpecBuilder`, `ResponseSpecBuilder`, `ResponseSpecification`, reusable specs, `Header(s)`, `TypeRef`, `matchesJsonSchemaInClasspath`, custom AssertJ | **2** (specs), 4 (schema/AssertJ), 5 (`TypeRef`) |
| L3 | `Filter`/`OrderedFilter`, safe `ErrorLoggingFilter`, `RestAssuredConfig`, `LogConfig`, `EncoderConfig`, `JsonConfig`/`JsonPathConfig`, parser registration | **2** (config + parser + encoder + json), 3 (filters) |
| L3+ deferred | `RedirectConfig`, `DecoderConfig`, `ObjectMapperConfig`, `ResponseBuilder`, SSL/proxy/URL-encoding | backlog (spec §22) |

Phase 2 deliberately teaches the **framework/config** layer (immutable templates, parser
registration, charset + number-format pitfalls) before any scenario exists.

---

## 8. HTTP/REST coverage matrix (target; exercised in later phases)

`201+Location`, `200 replay`, `400/401/403/404(masked)/405/406/409/412/415/422/428/429`,
`Idempotency-Key`, `ETag`, `If-Match`/`If-None-Match`, `Cache-Control`, `Vary`, `X-Correlation-ID`,
`Retry-After`, `Accept`/`Content-Type`/`Accept-Patch`/`Allow`, `application/problem+json`,
`application/merge-patch+json`, JSON-schema, content negotiation, conditional GET + `304`,
rate limiting, async `202` + status resource, `HEAD`/`OPTIONS`/`PATCH`, method semantics,
pagination/filter/sort, versioning, OpenAPI drift, redirects, multipart (settlement, later).
`500` only as unexpected-failure signal, never a business oracle.

Phase 2 encodes the **header contract** (`Headers`, `ResponseSpecs.sensitive/created/conditional`)
and the **problem+json** plumbing so later phases just assert against it.

---

## 9. Test taxonomy

Every scenario declares one primary category: Smoke · Sanity · Contract · Business flow ·
Security matrix · Negative HTTP · Validation · State transition · Idempotency · Conditional/ETag ·
Concurrency · DB verification · Audit/observability · Async/eventual-consistency · Regression ·
API-assisted setup. First live targets (Phase 7): **Smoke** (`/api/status`), **Contract**
(merchant create headers/body), **Negative HTTP** (problem+json 4xx).

---

## 10. Design patterns — use / avoid

**Use:** Builder (request DTOs, specs), Object Mother (standard fixtures), Factory Method
(identities/idempotency/correlation/specs), Facade (per-resource API clients), Strategy
(`TokenFactory`), Value Objects (`ETag`, `CorrelationId`, `IdempotencyKey`, `MerchantId`),
Result wrapper (`Versioned<T>`), custom AssertJ (`ProblemAssert`), thin Adapter over RA.

**Avoid:** abstract-factory theatre, deep inheritance, generic base classes that hide intent,
one giant `ApiClient`, static mutable util dumps, premature generalization, global mutable
`RestAssured.baseURI`.

---

## 11. Test data / helper strategy

Deterministic-by-default: readable prefixes (`alpha-`, `it-`), per-test correlation/id suffixes,
stable seed UUIDs from the backend's `DeterministicDataset`. Valid-by-default builders; explicit
**invalid** builders only for negative tests, added incrementally. API-test fixtures stay separate
from production fixtures. Reset→seed between tests via `SeedLifecycleExtension` (Phase 3).
Never log tokens, credentials, PII, idempotency keys, or payment data.

---

## 12. API client return-type strategy (documented contract)

| Return type | When |
|---|---|
| `Response` | test inspects raw status before choosing path (negatives, idempotency first-vs-replay). |
| `ValidatableResponse` | client executes, scenario still expresses HTTP expectations (learning/contract). |
| `ExtractableResponse<Response>` | validate baseline contract, then extract body **and** headers. |
| DTO (record) | read-only cases where headers are not part of the tested contract. |
| `Versioned<T>` | body + ETag/version both essential (create, lifecycle). |

No global return type — choose by test intent.

---

## 13. Educational comments / Javadoc strategy

One short class-level Javadoc per scenario: (1) what contract it protects, (2) why API-level
not UI/unit/repo, (3) which HTTP/REST concept, (4) which SDET/interview risk it teaches.
Method-level comments only when a test teaches an important HTTP/SDET decision. Prefer expressive
names and Arrange/Act/Assert over line-by-line noise.

---

## 14. Risks and anti-patterns to watch

- **Issuer mismatch** (Keycloak in TC) → 401 (spec §17): fix in Phase 3, document loudly.
- **Forgetting the problem+json parser** → assertion throws instead of failing (Phase 2 fixes).
- **Charset on merge-patch** → 415 (Phase 2 `EncoderConfig` fixes).
- **Async audit** asserted too early → flaky (always `Eventually`, Phase 4/5).
- **`List.class`** loses generics → use `TypeRef`.
- **Global mutable RA state** across parallel tests → only immutable `BASE` + `.spec()` copies.
- **Pattern theatre / hidden intent** → keep endpoint, status, headers, body visible in scenarios.
- Adding payment tests before the live stack exists → forbidden; clients are compile-safe skeletons until Phase 6/7.

---

## 15. Validation commands

```bash
# Phase 1–2 (offline, no containers): compile + run the wiring unit test
cd apps/api-tests && mvn -q test

# Later phases (require the backend image + Podman/Docker):
(cd apps/backend && ./mvnw spring-boot:build-image \
  -Dspring-boot.build-image.imageName=payment-quality/backend:local)
export BACKEND_IMAGE=payment-quality/backend:local
cd apps/api-tests && mvn -q verify        # runs *Spec via failsafe
```

Podman (Fedora, rootless), Phase 3+:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST="unix://$XDG_RUNTIME_DIR/podman/podman.sock"
export TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true
```

---

## 16. Intentionally deferred

Containers/stack, Keycloak token minting, filters, `ProblemAssert`, JSON-schema validation,
data generators, API clients, and all live `*Spec` scenarios are deferred to Phases 3–7.
`json-schema-validator` dependency is deferred to Phase 4 (not in local `~/.m2`; needs network).
Advanced HTTP backlog (redirects, multipart, SSL/proxy, OpenAPI drift) per spec §22 is later still.

---

## 17. Skills / MCP usage for this run

- **Skill used:** `junit6-assertj-restassured-testcraft` (test-layer choice, oracle clarity,
  assertion discipline, parallel-safe immutable specs). Other relevant skills available but not
  loaded to keep the run focused: `java-rest-api-testing-effective-java-mentor`,
  `parallel-test-architecture-and-data-isolation`, `maven-3-9-11-build-engineering`.
- **MCP:** none required for Phases 1–2 — decisions grounded in local truth (spec, `pom.xml`,
  controllers, `SecurityConfig`, `~/.m2` cache). Context7 reserved for RA6/JUnit6/Testcontainers/
  Keycloak details in Phases 3–4. PostgreSQL MCP not used (read-only only when schema inspection
  becomes useful; no SQL writes ever).
- RA 6 config APIs used in Phase 2 are stable across RA 4/5/6 and already resolve from the repo's
  existing RA 6.0.0 usage, so they were taken from local truth without external lookup.
