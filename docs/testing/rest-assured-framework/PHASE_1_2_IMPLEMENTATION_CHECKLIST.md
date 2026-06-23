# Phase 1 & 2 — Implementation Checklist

Companion to `REST_ASSURED_BLACK_BOX_FRAMEWORK_PLAN.md` and the binding spec
`FRAMEWORK-extended-http-rest-restassured.md`. Scope: **module skeleton** (Phase 1) and
**core HTTP plumbing** (Phase 2). No containers, no live tests — everything here compiles and
runs offline.

Legend: `[x]` done this run · `[ ]` deferred to a later phase.

---

## Phase 1 — module / directory skeleton

- [x] Create `apps/api-tests/` as a **standalone** Maven module (no Spring parent, black-box).
- [x] `apps/api-tests/pom.xml`
  - [x] JDK 25 (`maven.compiler.release=25`, `-parameters`).
  - [x] `junit-bom 6.0.3` + `testcontainers-bom 2.0.5` via `dependencyManagement`.
  - [x] Active deps (cached, offline-safe): `rest-assured 6.0.0`, `junit-jupiter`, `assertj-core 3.27.7`.
  - [x] Deferred deps documented in-pom with phase labels (TC, Awaitility, `json-schema-validator`, Keycloak-TC).
  - [x] `maven-surefire-plugin 3.5.4` runs `**/*Test.java`; `maven-failsafe-plugin 3.5.4` runs `**/*Spec.java` (integration-test + verify).
  - [x] `maven-enforcer-plugin` pins Java `[25,26)` + Maven `[3.9.11,)` (mirrors backend).
- [x] `apps/api-tests/README.md` — black-box purpose, run model, layer map, what is/isn't here yet.
- [x] `apps/api-tests/src/test/resources/junit-platform.properties` — single-stack note; parallelism off for now.
- [x] Layer `package-info.java` skeleton documenting responsibility + phase of each layer:
  - [x] `apitest` (root), `core/stack`, `core/http`, `core/auth`, `core/context`, `core/problem`,
        `core/data`, `core/concurrency`, `api`, `support`, `scenarios`.
- [x] Plan docs created/updated under `docs/testing/rest-assured-framework/`.
- [x] No business-heavy tests added (only a non-network wiring test, see Phase 2).

---

## Phase 2 — core HTTP skeleton (`core/http`)

- [x] `Headers.java` — constants only: `Idempotency-Key`, `If-Match`, `If-None-Match`, `ETag`,
      `X-Correlation-ID`, `Vary`, `Cache-Control`, `Accept-Patch`, `Allow`, `Content-Type`, `Location`, `Retry-After`.
- [x] `ContentTypes.java` — `application/json`, `application/problem+json`, `application/merge-patch+json`.
- [x] `ApiConfig.java` — base URI resolution from env/system property (`API_BASE_URI`,
      default `http://localhost:8080`). `fromStack()` documented as a Phase-3 addition (not faked).
- [x] `RestAssuredSetup.java` — `install(baseUri)`:
  - [x] Builds immutable `BASE` + `ANONYMOUS` templates via `RequestSpecBuilder.build()`.
  - [x] `RestAssured.registerParser("application/problem+json", Parser.JSON)` (else error-body asserts throw).
  - [x] `EncoderConfig.appendDefaultContentCharsetToContentTypeIfUndefined(false)` (merge-patch charset fix).
  - [x] `LogConfig.enableLoggingOfRequestAndResponseIfValidationFails(ALL)` + pretty print (safe failure-only logging).
  - [x] `JsonConfig` number return type = `BIG_DECIMAL` (financial precision; documented decision).
- [x] `RequestSpecs.java` — immutable `BASE`/`ANONYMOUS` consumed via `given().spec(...)`:
  - [x] `base()`, `anonymous()`, `idempotent(key)`, `conditional(ifMatch)`, `lifecycle(ifMatch,key)`, `mergePatch(ifMatch)`.
  - [x] No global mutable RA state; every method returns an isolated copy of the template.
  - [x] `AuthFilter`/`CorrelationFilter` wiring marked as Phase 3 (BASE is filter-free until then; `ANONYMOUS` will *stay* filter-free by design).
- [x] `ResponseSpecs.java` — reusable response contracts:
  - [x] `sensitive()` (X-Correlation-ID present, `Cache-Control: no-store`, `Vary: Authorization`).
  - [x] `problemJson()` (= sensitive + `application/problem+json` + body `error/status/correlationId`).
  - [x] `conditional()` (Vary contains `If-Match`), `created()` (Vary contains `Idempotency-Key` + ETag `"v\d+"`).
- [x] `CoreHttpWiringTest.java` — **non-network** unit test: `install()` then assert every spec
      factory builds without throwing; assert header/content-type constants. Proves the module compiles and wires.

### Decisions recorded in Phase 2

- **Jackson 2 on the test side** (not `JACKSON_3`) — RA 6.0.0 `jsonPath()` bug; keeps test/app mappers separate. (no code yet; documented for Phase 6 DTO deserialization.)
- **JSON numbers = `BIG_DECIMAL`** — amounts are minor-unit integers but money math must not touch float.
- **problem+json parser registration is mandatory** and lives in `install()` so every error assertion works.
- **Two templates, not one** — `BASE` (gains auth filter in Phase 3) vs `ANONYMOUS` (`/api/status`, seed/reset) so we never have to *remove* a filter.

---

## Validation (run after this phase)

```bash
cd apps/api-tests && mvn -q test
```

Expected: compiles under JDK 25, `CoreHttpWiringTest` passes, no containers started, no network
for dependencies (all in `~/.m2`). If `mvn` attempts to download a build plugin not cached and the
sandbox is offline, re-run with network enabled or pre-warm the plugin.

---

## Explicitly NOT in Phase 1–2

Filters, `Ctx`/`TestContext`, Testcontainers stack, Keycloak token minting, `ProblemAssert`,
JSON-schema + `json-schema-validator` dep, data generators, `Versioned<T>`/`ETag`/concurrency
harness, `api/*` clients, and all live `*Spec` scenarios. No payment tests until endpoints are
driven through the real stack (Phase 7).
