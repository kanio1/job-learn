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

## 2. Current repository state

Verified on branch **`001-project-foundation`** (note: `CLAUDE.md` and `.codex/current-state.md`
still reference the older `018-…` branch; the working branch is `001-project-foundation`).

| Question | Finding |
|---|---|
| Does `apps/api-tests/` exist? | **Yes.** It is the active standalone black-box REST Assured module. |
| Current validation baseline | 79 offline tests and 72 live specs pass; current through Phase 8S. |
| Existing REST Assured coverage | `apps/api-tests` owns the black-box suite. Older backend in-process REST tests and legacy `restkit/` tests remain historical/reference context only. |
| Helpers that MUST NOT be reused | `…/restkit/**` and `…/testsupport/**` (incl. `testsupport/restkit/**`). Read-only reference. |
| Backend stack | Spring Boot 4.0.6, Java 25, Spring Modulith 2.0.6, PostgreSQL 18 (Flyway), Keycloak resource server. |
| Backend test libs (repo truth) | REST Assured **6.0.0**, JUnit **6.0.3** (`junit-bom`), Testcontainers **2.0.5**, AssertJ 3.27.7, Awaitility 4.3.0, Hamcrest 3.0, and REST Assured JSON schema validator 6.0.0. |
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

### How the backend is driven black-box

The live suite runs the backend image under Testcontainers with PostgreSQL 18 + Keycloak 26 on
a shared network, seeds deterministic data through the test endpoint, and waits for
`GET /api/status == 200`. Tokens are minted from the imported Keycloak test realm and injected
by REST Assured filters from the current `TestContext`.

Offline `mvn test` still validates framework helpers only; live `mvn verify` runs the black-box
`*Spec` classes.

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

Glue: `Ctx` (`ThreadLocal<TestContext>`) carries identity + `correlationId`. Filters read `Ctx`
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
| 4 | `core/problem` (`ProblemDetail`, `ProblemCodes`, `ProblemAssert`), `schema/problem.schema.json`; full JSON schema validator activation landed later in Phase 8H | ✅ done |
| 5 | `core/data` (`CorrelationIds`, `IdempotencyKeys`, `UniqueReferences`, `ETag`, `Versioned<T>`) | ✅ done |
| 6A | Stack discovery: runtime reality, image strategy, Keycloak strategy, first endpoint plan | ✅ done |
| 6B-lite | TC deps (core/postgresql/junit-jupiter), `PostgresSupport`, `BackendSupport`, `ApiStack`, `ApiStackExtension`, `ApiTest`, `StatusApi`, `StatusSpec` | ✅ done |
| 6B-full | `KeycloakSupport`, `KeycloakTokenFactory`, update `Identities`, `SeedApi` | superseded/landed via 6C+ live stack work |
| 6C | Keycloak 26 TC container, ROPC token factory, `SecuritySmokeSpec` (401 + 403 without seed data) | ✅ done |
| 6D | Local merchant DTOs, `MerchantsApi` facade, `SeedApi`, `MerchantsContractSpec` (8 scenarios: 201/200/400/409/404) | ✅ done |
| 6E | Contract hardening: `activate`/`suspend` API + 5 new contract tests (lifecycle 200, invalid_transition 409, missing tenantRef 400, tenant-filter list), refactor 5 error tests to ProblemAssert, add `DUPLICATE_MERCHANT_REFERENCE`/`INVALID_TRANSITION` to `ProblemCodes` | ✅ done |
| 7A | Payment order contract foundation: `Seeds`, `PaymentOrdersApi` (GET, LIST), `PaymentOrdersContractSpec` (6 specs: GET body, headers, AUTHORIZED status, LIST pagination, 404, 400 malformed); create deferred (Keycloak realm gap) | ✅ done |
| 7B | Keycloak realm fix (`merchant.alpha.creator` user with real merchant UUID), `seededMerchantCreator()` persona, `CreatePaymentOrderRequest` DTO, `PaymentOrdersApi.create()` + `createWithoutIdempotencyKey()`, 3 create specs (201 + 403 scope mismatch + 400 missing Idempotency-Key) | ✅ done |
| 7C | Idempotency contract: replay (same key + same body → 200) + conflict (same key + different body → 409 `idempotency_conflict`); 2 new specs; `PHASE_7C` doc; Vary observed as `Idempotency-Key` only | ✅ done |
| 7D | ETag / If-Match lifecycle foundation: authorize happy path (→ 200, ETag v0→v1), missing If-Match (→ 428 `precondition_required`), stale If-Match (→ 412 `payment_order_version_mismatch`); `PaymentOrdersApi.authorize()` + `authorizeWithoutIfMatch()`; 2 new `ProblemCodes` | ✅ done |
| 7E | Lifecycle contract foundation — capture + cancel: create→authorize→capture (v0→v1→v2 ETag chain), create→cancel (v0→v1), capture before authorize (422 `invalid_transition`); `PaymentOrdersApi.capture()` + `cancel()` | ✅ done |
| 7F | Refund contract + negative boundaries: full lifecycle refund (v0→v1→v2→v3, REFUNDED, `refundedAmountMinor`), cancel-after-capture (422 `invalid_transition`); `PaymentOrdersApi.refund()`; backend bug documented (refund on non-captured order → 500 NPE instead of 422) | ✅ done |
| 7G | Backend bugfix: `PaymentLifecycleService.refund()` — add `canTransitionTo(REFUNDED)` guard before PSP call (prevents NPE on null `capturedAmountMinor`); image rebuilt via `podman-build-image` profile; regression spec `refund_on_authorized_order_returns_422_invalid_transition` re-enabled | ✅ done |
| 7H | Concurrency race: two concurrent authorizes (different Idempotency-Keys, same `If-Match: "v0"`) → one 200, one 412 (`concurrency_conflict` or `payment_order_version_mismatch`); `CyclicBarrier(2)` + `ExecutorService`; final GET verifies AUTHORIZED + ETag v1; `ProblemCodes.CONCURRENCY_CONFLICT` added | ✅ done |
| 7I | Backend bugfix: `PaymentOrderService.create()` two-phase idempotency write race — `IllegalStateException` (500) replaced by `IdempotencyCreateInProgressException` → 409 `create_in_progress`; guards added to both null-paymentOrderId code paths; 2 new unit tests; black-box regression: concurrent same-key creates → no 500, at least one 201 | ✅ done |
| 7J | History/audit contract: `GET .../history` synchronous, creation entry excluded (`action IS NOT NULL`); `PaymentHistoryResponse` DTO; `PaymentOrdersApi.history()`; 3 specs: full-lifecycle 2-entry chain (AUTHORIZE+CAPTURE), empty list on fresh create, 403 for denied user; audit event endpoint deferred (async, Awaitility needed) | ✅ done |
| 8A | Async audit event contract with Awaitility: `AuditApi` facade, `AuditListResponse` DTO, Awaitility 4.3.0 dep; `authorize_emits_payment_authorized_audit_event` (create→authorize→poll→assert correlationId+fields), `audit_list_returns_403_for_denied_user`; discovered Awaitility thread model (condition runs on `awaitility-thread`, not main) → `Ctx.set()` must be called inside every condition lambda; 2 new live specs; 79+43 all pass | ✅ done |
| 8B | Payment summary / reporting contract: `PaymentSummaryResponse` DTO (with `CurrencySummary` / `StatusSummary` nested records), `PaymentOrdersApi.summary()` + `summaryWithCurrency()`, `PaymentSummaryContractSpec` (3 specs: 200 with exact deterministic aggregates using SoftAssertions, 400 `validation` for unsupported currency, 403 `forbidden` for merchant-scope mismatch); ordering contract verified (SQL `ORDER BY currency/status ASC`); 79+46 all pass | ✅ done |
| 8C | Tenant / merchant isolation security matrix: `TenantIsolationContractSpec` (4 specs); key findings: GET-by-ID cross-merchant → 404 masked (BOLA defence via `PaymentOrderNotFoundException`), GET-list/history cross-merchant → 403 (`AccessDeniedException`), `platform:payments:read` bypasses merchant_id check → 200 cross-merchant; PHASE_8C doc added; 79+50 all pass | ✅ done |
| 8D | Lifecycle idempotency replay contract: `LifecycleIdempotencyContractSpec` (3 specs); key findings: replay check fires BEFORE version check in service; If-Match syntactically required but semantically bypassed on replay; ETag stable (no increment); no duplicate history entry on replay; fingerprint conflict (same key + different body) → 409; `authorizeWithReason()` facade added; PHASE_8D doc added; 79+53 all pass | ✅ done |
| 8E | JSON Merge Patch contract: `PatchMetadataContractSpec` (4 specs); key findings: PATCH accepts both `application/merge-patch+json` and `application/json`; guard order differs from lifecycle — unknown-field check (400) fires BEFORE ETag check (428/412); 415 fires at dispatcher before controller; `Accept-Patch` header on 415; ETag incremented on success (same `lifecycleResponse()` path as lifecycle actions); `PatchMetadataRequest` DTO; `patch()`, `patchWithoutIfMatch()`, `patchWithWrongContentType()`, `patchWithUnknownField()` facades added; PHASE_8E doc added; 79+57 all pass | ✅ done |
| 8F | Partial refund contract: `PartialRefundContractSpec` (3 specs); key findings: `amountMinor` in `RefundRequest` is optional (null → full refund); domain-level validation only (no bean-validation annotations); zero/negative → 422 same code as over-refund (`refund_amount_exceeds_captured`); transaction rollback on domain exception preserves seeded order state; multiple refunds blocked by REFUNDED terminal state; `refundWithAmount()` facade added; `REFUND_AMOUNT_EXCEEDS_CAPTURED` ProblemCode added; PHASE_8F doc added; 79+60 all pass | ✅ done |
| 8G | HTTP method semantics and content-negotiation contract: `HttpMethodSemanticsContractSpec` (4 specs); key findings: HEAD and OPTIONS are explicit controller handlers (not Spring defaults); OPTIONS is `permitAll()`; HEAD returns ETag + Vary:Authorization + no-store with no body; OPTIONS 204 with Allow:GET,HEAD,PATCH,OPTIONS + Accept-Patch:application/merge-patch+json; DELETE → 405 `method_not_allowed` + Allow header (RFC 9110 §15.5.6 mandatory); `Accept: text/xml` → 406 `not_acceptable` with problem+json body regardless of Accept; auth required for HEAD/DELETE (anyRequest.authenticated), not for OPTIONS; `headById()`, `optionsById()` (anonymous), `deleteById()`, `getByIdWithAccept()` facades added; PHASE_8G doc added; 79+64 all pass | ✅ done |
| 8H | JSON Schema contract validation foundation: `json-schema-validator:6.0.0` resolved from Maven Central (2026-06-25) and enabled in `pom.xml` (was deferred since Phase 4); `payment-order.schema.json` + `payment-summary.schema.json` created (draft-07, `additionalProperties: false` to catch field renames/additions; `problem.schema.json` pre-existing); `SchemaAssertions` helper (`matchesProblemSchema / matchesPaymentOrderSchema / matchesPaymentSummarySchema`); `ProblemAssert.matchesProblemSchema()` activated (was `UnsupportedOperationException`); `JsonSchemaContractSpec` (3 tests: payment-order schema, 404 problem schema, summary schema); all transitive deps from `com.github.java-json-tools` confirmed in `~/.m2`; PHASE_8H doc added; 79+67 all pass | ✅ done |
| 8I | ResponseSpecs contract assertion cleanup: discovered that `ResponseSpecs` had a Vary: Authorization bug in `conditional()` and `created()` (both extended `sensitive()` which checks Vary: Authorization, but lifecycle 200 responses carry Vary: If-Match only and 201 responses carry Vary: Idempotency-Key only); fixed by extracting private `noCache()` base (X-Correlation-ID + Cache-Control: no-store) that `conditional()` and `created()` now extend instead of `sensitive()`; added Location header check to `created()`; applied specs to 8 locations across 3 scenario files (6 in `PaymentOrdersContractSpec`: GET security headers + 4 lifecycle happy paths + history; 1 in `PaymentSummaryContractSpec`: summary GET; 1 in `PartialRefundContractSpec`: refund happy path); kept 59 remaining tests explicit where specificity matters or refactoring would reduce readability; no new spec classes added; PHASE_8I doc added; 79+67 all pass | ✅ done |
| 8J | PATCH stale ETag contract: added `PatchMetadataContractSpec.patch_with_stale_if_match_returns_412_payment_order_version_mismatch`; flow creates order (`ETag: "v0"`), successfully PATCHes metadata to increment ETag to `"v1"`, then reuses stale `"v0"` on a second valid merge-patch request; asserts 412 `payment_order_version_mismatch`, `application/problem+json`, `Cache-Control: no-store`, `Vary: If-Match`, correlation ID, and problem schema; confirms structurally valid PATCH reaches version precondition after the Phase 8E unknown-field guard ordering; PHASE_8J doc added; 79+68 all pass | ✅ done |
| 8K | Cancel from AUTHORIZED contract: added `PaymentOrdersContractSpec.create_authorize_cancel_happy_path_returns_200_and_records_history`; flow creates order (`ETag: "v0"`), authorizes to `"v1"`, cancels with `If-Match: "v1"` to `"v2"`; asserts 200, `ResponseSpecs.conditional()`, status `CANCELLED`, `cancelledAt` non-null, `expiresAt` cleared, capture/refund fields null, and synchronous history entries `AUTHORIZE` then `CANCEL`; confirms backend voids PSP authorization before state mutation for previous `AUTHORIZED` orders; PHASE_8K doc added; 79+69 all pass | ✅ done |
| 8L | Refund idempotency replay contract: added `LifecycleIdempotencyContractSpec.refund_replay_returns_stable_200_and_does_not_create_duplicate_history_entry`; flow creates order (`ETag: "v0"`), authorizes to `"v1"`, captures to `"v2"`, full-refunds to `"v3"`, then replays the same refund with the same `Idempotency-Key` and original refund `If-Match: "v2"`; asserts 200, `ResponseSpecs.conditional()`, stable `ETag: "v3"`, stable `REFUNDED` body and `refundedAmountMinor`, and history remains exactly `AUTHORIZE`, `CAPTURE`, `REFUND` with one `REFUND` entry; confirms refund replay check fires before semantic version/state guards while controller still requires syntactically valid `If-Match`; PHASE_8L doc added; 79+70 all pass | ✅ done |
| 8M | Tenant admin merchant boundary contract: added 2 specs in `TenantIsolationContractSpec`; `tenant_admin_reads_and_lists_only_own_tenant_merchants` verifies real `tenant.admin` (`TENANT_ADMIN`, `tenant_id=TENANT_ALPHA`, no `merchant_id`) can read/list TENANT_ALPHA merchants, list excludes `MERCHANT_BETA_001`, and cross-tenant merchant GET is masked as 404; `tenant_admin_without_merchant_id_cannot_read_same_tenant_payment_order` verifies tenant admin cannot directly read even same-tenant payment orders because payment reads require matching `merchant_id` or `platform:payments:read`; confirms merchant administration is tenant-scoped while payment data remains merchant/platform-scoped; PHASE_8M doc added; 79+72 all pass | ✅ done |
| 8N | Documentation cleanup and framework index: refreshed stale `scenarios/package-info.java`, rewrote `apps/api-tests/README.md` as a concise framework index/runbook, updated this plan's stale current-state/deferred text, and added PHASE_8N doc; no tests or assertions changed; 79+72 all pass | ✅ done |
| 8O | JUnit tags and selective run workflow: added compact JUnit tag taxonomy (`smoke`, `contract`, `security`, `lifecycle`, `idempotency`, `audit`, `schema`, `http`, `concurrency`, `regression`) across live scenario specs; used class-level tags where intent is clear and method-level tags only for mixed `PaymentOrdersContractSpec`; documented Surefire/Failsafe 3.5.4 tag filters via `-Dgroups` / `-DexcludedGroups` in README and PHASE_8O doc; no POM change needed and default full-suite behavior remains unchanged; 79+72 all pass | ✅ done |
| 8P | Pre-commit review and consistency audit: review-only pass over the uncommitted 8J-8O work; checked phase-doc references, README commands/counts, tag taxonomy, black-box guardrails, `ResponseSpecs` usage, problem/schema consistency, and stale docs; no files modified; no P0/P1 findings; P2/P3 cleanup items deferred to 8Q/8S; PHASE_8P doc added; 79 offline tests pass | ✅ done |
| 8Q | Targeted cleanup fixes from Phase 8P review: applied `ResponseSpecs.created()` to the primary payment-order create happy-path test while keeping exact `Location` and `"v0"` assertions explicit; refreshed `ProblemAssert` schema-validation JavaDoc; corrected current baseline wording through Phase 8Q; clarified 6B-full as superseded/landed through later live-stack work; PHASE_8Q doc added; 79+72 validation rerun | ✅ done |
| 8R | Final commit-prep checklist and diff consistency review: review-only pass over the 8J-8Q diff; confirmed no code/test blockers, no backend imports, no Spring test/MockMvc usage, plan/README command consistency, and 79 offline tests passing; recommended only documentation ledger polish, handled in 8S | ✅ done |
| 8S | Final documentation ledger polish: added PHASE_8P review note, updated README baseline wording, tightened `scenarios/package-info.java` current-coverage wording, added 8P/8R/8S ledger rows, and created PHASE_8S doc; no behavior changes; 79 offline tests pass | ✅ done |
| 8+ | Advanced HTTP backlog (redirects, multipart, SSL/proxy, OpenAPI drift) | deferred |

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

The core black-box framework, stack, Keycloak token minting, filters, API facades, schemas, and
live scenario suite are implemented through Phase 8S. Remaining deferred work is the advanced
HTTP backlog from spec §22: redirects, multipart, SSL/proxy coverage, OpenAPI drift checks, and
future product capabilities outside the current payment-order scope.

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
