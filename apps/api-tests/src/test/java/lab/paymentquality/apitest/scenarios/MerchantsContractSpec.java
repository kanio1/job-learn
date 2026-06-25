package lab.paymentquality.apitest.scenarios;

import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.merchant.MerchantsApi;
import lab.paymentquality.apitest.api.merchant.dto.CreateMerchantRequest;
import lab.paymentquality.apitest.api.merchant.dto.MerchantResponse;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.UniqueReferences;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import lab.paymentquality.apitest.support.ApiTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Merchant API contract spec — Phases 6D + 6E.
 *
 * <p><strong>Test category:</strong> Contract — verifies that the merchant resource behaves
 * according to its HTTP/REST contract (status codes, response body shape, error shapes,
 * lifecycle transitions).
 *
 * <p><strong>Why API-level and not unit/MockMvc?</strong> Unit tests verify individual layers;
 * API-level contract tests verify the complete HTTP stack — serialization, validation, security,
 * exception mapping, and routing — as seen by a real client. A unit test passing while the
 * JSON serializer produces the wrong field name is a classic false negative that only
 * black-box tests catch.
 *
 * <p><strong>HTTP/REST concepts exercised:</strong>
 * <ul>
 *   <li>201 Created — resource created successfully; body contains canonical representation.</li>
 *   <li>200 OK — resource retrieved or lifecycle transition accepted; idempotent reads.</li>
 *   <li>400 Bad Request with {@code application/problem+json} — bean validation failure or
 *       missing required field; {@code error: "validation"} signals which rule failed.</li>
 *   <li>404 Not Found — resource does not exist; {@code error: "not_found"}.</li>
 *   <li>409 Conflict — duplicate reference ({@code duplicate_merchant_reference}) or invalid
 *       lifecycle transition ({@code invalid_transition}); the request is valid but conflicts
 *       with existing state.</li>
 * </ul>
 *
 * <p><strong>SDET interview topics:</strong>
 * <ul>
 *   <li>Why use seed/reset in @BeforeAll/@AfterAll instead of per-test?</li>
 *   <li>What is the difference between 400 (your request is malformed) and 409 (your request is
 *       valid but conflicts with existing state)?</li>
 *   <li>Why does {@code platform.admin} need seed data (PLATFORM_TENANT in DB) but {@code merchant.denied}
 *       did NOT need it in SecuritySmokeSpec?</li>
 *   <li>What does {@code status: "DRAFT"} tell us about the merchant lifecycle state machine?</li>
 *   <li>Why should each test create its own merchant rather than sharing one across tests?</li>
 *   <li>Why return 200 (not 201) for activate/suspend — they are transitions, not resource creations.</li>
 * </ul>
 *
 * <p><strong>Data isolation strategy:</strong> {@link SeedApi#seed()} loads a deterministic dataset
 * (PLATFORM_TENANT, TENANT_ALPHA, three merchants) into the Testcontainers-managed database before
 * this spec class runs. Each test creates merchants with {@link UniqueReferences#merchantRef(String)}
 * to avoid reference collisions between tests. {@link SeedApi#reset()} cleans up after the class
 * to prevent data bleed into subsequent spec classes.
 */
@ApiTest
@Tag("contract")
@DisplayName("Merchants API — contract")
class MerchantsContractSpec {

    @BeforeAll
    static void seedDatabase() {
        SeedApi.seed();
    }

    @AfterAll
    static void resetDatabase() {
        SeedApi.reset();
    }

    @AfterEach
    void clearContext() {
        Ctx.clear();
    }

    // -------------------------------------------------------------------------
    // CREATE — POST /api/merchants
    // -------------------------------------------------------------------------

    /**
     * Contract: POST /api/merchants → 201 with DRAFT status.
     *
     * <p>HTTP concept: 201 Created is the correct success status for resource creation.
     * The body contains the canonical representation of the created resource.
     *
     * <p>SDET: verify that the initial lifecycle state ({@code DRAFT}) is enforced server-side,
     * regardless of what the client sent. The request body does not contain a {@code status} field;
     * the backend assigns it. This is a state-machine invariant test.
     */
    @Test
    @DisplayName("POST /api/merchants → 201 with DRAFT status and expected body")
    void create_merchant_returns_201_with_draft_status() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        String ref = UniqueReferences.merchantRef("create");
        // Platform-scoped callers MUST supply tenantReference; the backend throws
        // MissingTenantReferenceException (→ 400) if it is absent. TENANT_ALPHA is a
        // standard tenant loaded by SeedApi.seed().
        CreateMerchantRequest req = CreateMerchantRequest.withTenantRef(ref, "Create Test Merchant", "TENANT_ALPHA");

        MerchantResponse body = MerchantsApi.create(req)
                .then()
                .statusCode(201)
                .extract()
                .as(MerchantResponse.class);

        // MerchantReference.from() normalizes to uppercase — assert the normalized form.
        assertThat(body.merchantId()).isNotNull();
        assertThat(body.merchantReference()).isEqualTo(ref.toUpperCase(Locale.ROOT));
        assertThat(body.displayName()).isEqualTo("Create Test Merchant");
        assertThat(body.status()).isEqualTo("DRAFT");
        assertThat(body.createdAt()).isNotNull();
        assertThat(body.updatedAt()).isNotNull();
    }

    /**
     * Contract: POST /api/merchants → read back via GET /api/merchants/{id} → 200.
     *
     * <p>HTTP concept: a successful 201 must return a body from which the client can extract the
     * resource identifier. That identifier must resolve via GET to the same resource.
     * This test verifies the create→read round-trip, which is the minimum viable contract for
     * any RESTful resource.
     *
     * <p>SDET: this is also a write-then-read data consistency check — it catches bugs where
     * the backend creates the resource but returns a different ID, or where the read endpoint
     * applies incorrect tenant filtering that excludes the just-created resource.
     */
    @Test
    @DisplayName("GET /api/merchants/{id} after create → 200 with matching body")
    void read_created_merchant_returns_200() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        String ref = UniqueReferences.merchantRef("read");
        String merchantId = MerchantsApi.create(CreateMerchantRequest.withTenantRef(ref, "Read Test Merchant", "TENANT_ALPHA"))
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");

        MerchantResponse body = MerchantsApi.getById(merchantId)
                .then()
                .statusCode(200)
                .extract()
                .as(MerchantResponse.class);

        assertThat(body.merchantId().toString()).isEqualTo(merchantId);
        assertThat(body.merchantReference()).isEqualTo(ref.toUpperCase(Locale.ROOT));
        assertThat(body.displayName()).isEqualTo("Read Test Merchant");
        assertThat(body.status()).isEqualTo("DRAFT");
    }

    /**
     * Contract: GET /api/merchants → 200 with merchants list.
     *
     * <p>HTTP concept: GET on a collection resource returns a 200 with the collection body.
     * An empty collection is still a 200 — never a 404 (404 means the collection resource
     * itself does not exist, which is a different error).
     *
     * <p>SDET: verifies that after seeding, the platform-scoped caller sees the seeded merchants.
     * The seed loaded 3 merchants (MERCHANT_ALPHA_001, MERCHANT_ALPHA_002, MERCHANT_BETA_001).
     * The list should contain at least those; tests in this class may have added more.
     */
    @Test
    @DisplayName("GET /api/merchants → 200 with non-empty merchants list")
    void list_merchants_returns_200() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        MerchantsApi.list()
                .then()
                .statusCode(200)
                .body("merchants", notNullValue())
                .body("merchants.size()", greaterThanOrEqualTo(3));
    }

    /**
     * Contract: GET /api/merchants?tenantId=TENANT_ALPHA → 200 with merchants for that tenant.
     *
     * <p>HTTP concept: optional query parameters are a standard REST filtering pattern.
     * The platform-scoped caller can scope the list to a single tenant by supplying the tenant
     * reference as a query parameter. This is a distinct capability from "list all" and only
     * effective for platform-scoped callers.
     *
     * <p>SDET: this test exercises a conditional branching path in the controller: when
     * {@code tenantId} is supplied and the caller is platform-scoped, the backend resolves the
     * reference to a tenant UUID and filters the result set. Three seed merchants belong to
     * TENANT_ALPHA, so the assertion is stable against seed data.
     */
    @Test
    @DisplayName("GET /api/merchants?tenantId=TENANT_ALPHA → 200 with merchants for that tenant")
    void list_with_tenant_filter_returns_merchants() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        MerchantsApi.listByTenant("TENANT_ALPHA")
                .then()
                .statusCode(200)
                .body("merchants", notNullValue())
                .body("merchants.size()", greaterThanOrEqualTo(1));
    }

    // -------------------------------------------------------------------------
    // VALIDATION ERROR — 400
    // -------------------------------------------------------------------------

    /**
     * Contract: POST /api/merchants with blank merchantReference → 400 validation error.
     *
     * <p>HTTP concept: 400 Bad Request with {@code application/problem+json} body.
     * The backend's bean validation (Jakarta {@code @NotBlank}) catches this before the
     * service layer executes — no DB write occurs.
     *
     * <p>SDET: this verifies the API-level validation contract, not the domain model.
     * The {@code error: "validation"} discriminator lets clients route to the correct error
     * handler. The absence of a {@code merchantId} in the response confirms no partial creation occurred.
     *
     * <p>Phase 6E: refactored to use {@link ProblemAssert} for richer failure diagnostics.
     * ProblemAssert includes the full response body in failure messages, which is critical for
     * diagnosing API contract regressions quickly.
     */
    @Test
    @DisplayName("POST /api/merchants with blank merchantReference → 400 validation")
    void create_with_blank_reference_returns_400() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        ProblemAssert.assertThat(MerchantsApi.create(CreateMerchantRequest.valid(" ", "Valid Name")))
                .hasStatus(400)
                .hasError(ProblemCodes.VALIDATION);
    }

    /**
     * Contract: POST /api/merchants with too-short displayName → 400 validation error.
     *
     * <p>HTTP concept: same 400 contract — multiple validation rules are enforced in the same
     * request pipeline stage. The {@code details} map contains field-level messages.
     *
     * <p>Phase 6E: refactored to use {@link ProblemAssert}.
     */
    @Test
    @DisplayName("POST /api/merchants with displayName too short (1 char) → 400 validation")
    void create_with_short_display_name_returns_400() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        ProblemAssert.assertThat(MerchantsApi.create(CreateMerchantRequest.valid(UniqueReferences.merchantRef("val"), "A")))
                .hasStatus(400)
                .hasError(ProblemCodes.VALIDATION);
    }

    /**
     * Contract: POST /api/merchants without tenantReference for a platform-scoped caller → 400.
     *
     * <p>HTTP concept: 400 Bad Request — the request is syntactically valid JSON but fails
     * domain-level validation. The backend's {@code MerchantService.resolveAssignedTenantId()}
     * requires platform-scoped callers to supply {@code tenantReference}; tenant-scoped callers
     * derive the tenant ID from their JWT claim instead.
     *
     * <p>SDET: this tests a conditional validation path, not a bean validation rule.
     * {@code MissingTenantReferenceException} maps to 400 via {@code MerchantExceptionHandler}.
     * The distinction matters in SDET interviews: some 400s are caught at the deserialization
     * layer, others at the service layer — both are correctly reported as 400.
     *
     * <p>SDET interview: what happens if tenant.admin (tenant-scoped) omits tenantReference?
     * Answer: no error — the tenant ID is resolved from the JWT {@code tenant_id} claim.
     */
    @Test
    @DisplayName("POST /api/merchants without tenantReference (platform-scoped caller) → 400")
    void create_without_tenant_ref_for_platform_caller_returns_400() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        // valid() omits tenantReference — valid for tenant-scoped callers but 400 for platform-scoped
        ProblemAssert.assertThat(MerchantsApi.create(CreateMerchantRequest.valid(
                        UniqueReferences.merchantRef("notenant"), "No Tenant Merchant")))
                .hasStatus(400)
                .hasError(ProblemCodes.VALIDATION);
    }

    // -------------------------------------------------------------------------
    // CONFLICT — 409
    // -------------------------------------------------------------------------

    /**
     * Contract: POST /api/merchants with duplicate merchantReference → 409 Conflict.
     *
     * <p>HTTP concept: 409 Conflict is the correct status when the request is structurally valid
     * (passes validation) but conflicts with existing resource state. It differs from 400:
     * the client cannot fix a 409 by reformatting the request — they must use a different reference.
     *
     * <p>SDET: this is also a test of idempotency behavior. POST is NOT idempotent by design here;
     * a repeated identical POST must be rejected. The {@code error: "duplicate_merchant_reference"}
     * discriminator is more specific than just 409 and lets automated clients distinguish this
     * conflict from other 409 reasons (e.g., invalid state transition).
     *
     * <p>Phase 6E: refactored to use {@link ProblemAssert} and {@link ProblemCodes} constant.
     */
    @Test
    @DisplayName("POST /api/merchants with duplicate merchantReference → 409 conflict")
    void create_duplicate_reference_returns_409() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        String ref = UniqueReferences.merchantRef("dup");

        MerchantsApi.create(CreateMerchantRequest.withTenantRef(ref, "First Merchant", "TENANT_ALPHA"))
                .then()
                .statusCode(201);

        ProblemAssert.assertThat(MerchantsApi.create(
                        CreateMerchantRequest.withTenantRef(ref, "Second Merchant", "TENANT_ALPHA")))
                .hasStatus(409)
                .hasError(ProblemCodes.DUPLICATE_MERCHANT_REFERENCE);
    }

    // -------------------------------------------------------------------------
    // NOT FOUND — 404
    // -------------------------------------------------------------------------

    /**
     * Contract: GET /api/merchants/{id} with unknown UUID → 404 Not Found.
     *
     * <p>HTTP concept: 404 means the resource identified by this URL does not exist.
     * The backend masks internal domain exceptions into a standard 404 shape — this is
     * the "masking" pattern for security (do not leak internal IDs to probing clients).
     *
     * <p>SDET: use a freshly generated UUID guaranteed to not exist in the DB. Never hardcode
     * a UUID in a not-found test — a hardcoded UUID could coincidentally exist from a seed or
     * prior test run, turning the 404 expectation into a flaky 200.
     *
     * <p>Phase 6E: refactored to use {@link ProblemAssert} and {@code hasContentTypeProblemJson()}.
     * The {@code handleNotFound} exception handler explicitly sets {@code application/problem+json}
     * content type, making this assertion safe. This is the correct place to verify the
     * problem+json content type contract because not all handlers set it explicitly.
     */
    @Test
    @DisplayName("GET /api/merchants/{id} with unknown UUID → 404 not found")
    void get_unknown_merchant_returns_404() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        ProblemAssert.assertThat(MerchantsApi.getById(UUID.randomUUID().toString()))
                .hasStatus(404)
                .hasContentTypeProblemJson()
                .hasError(ProblemCodes.NOT_FOUND);
    }

    /**
     * Contract: GET /api/merchants/{id} with non-UUID id → 400 validation.
     *
     * <p>HTTP concept: 400 for a malformed path parameter — the ID is syntactically invalid
     * (not a UUID) before any database lookup is attempted.
     *
     * <p>SDET: distinguishes client-side input validation (400) from server-side lookup failure
     * (404). A client sending {@code /api/merchants/not-a-uuid} has a bug in their URL
     * construction; the backend should help them by returning 400 rather than silently
     * returning 404 (which would mislead them into thinking the resource exists but was not found).
     *
     * <p>Phase 6E: refactored to use {@link ProblemAssert}.
     */
    @Test
    @DisplayName("GET /api/merchants/{id} with non-UUID id → 400 malformed ID")
    void get_malformed_id_returns_400() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        ProblemAssert.assertThat(MerchantsApi.getById("not-a-uuid"))
                .hasStatus(400)
                .hasError(ProblemCodes.VALIDATION);
    }

    // -------------------------------------------------------------------------
    // MERCHANT LIFECYCLE — activate / suspend
    // -------------------------------------------------------------------------

    /**
     * Contract: POST /api/merchants/{id}/activate on a DRAFT merchant → 200 ACTIVE.
     *
     * <p>HTTP concept: 200 (not 201) is correct for a state transition — no new resource is
     * created, the existing resource is mutated. The response body reflects the post-transition
     * state. This is a command endpoint (verb in URL), not a resource endpoint.
     *
     * <p>SDET: verifies the DRAFT → ACTIVE transition is the only valid activation path.
     * {@code MerchantStatus.canTransitionTo()} enforces this in domain logic. The test
     * creates its own merchant to guarantee DRAFT state — sharing a pre-activated merchant
     * across tests would couple test execution order and risk flaky failures.
     *
     * <p>SDET interview: why is the state machine enforced in the domain model rather than
     * a service layer conditional? Answer: domain invariants belong in the domain — the
     * {@code Merchant.activate()} method is the single place where the transition rule lives.
     */
    @Test
    @DisplayName("POST /api/merchants/{id}/activate on DRAFT → 200 ACTIVE")
    void activate_draft_merchant_returns_200_with_active_status() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        String merchantId = MerchantsApi.create(
                        CreateMerchantRequest.withTenantRef(
                                UniqueReferences.merchantRef("act"), "Activate Me", "TENANT_ALPHA"))
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");

        MerchantResponse body = MerchantsApi.activate(merchantId)
                .then()
                .statusCode(200)
                .extract()
                .as(MerchantResponse.class);

        assertThat(body.merchantId().toString()).isEqualTo(merchantId);
        assertThat(body.status()).isEqualTo("ACTIVE");
    }

    /**
     * Contract: POST /api/merchants/{id}/suspend on an ACTIVE merchant → 200 SUSPENDED.
     *
     * <p>HTTP concept: same 200-on-transition pattern as activate. The full
     * DRAFT → ACTIVE → SUSPENDED chain is exercised in one test to verify that the lifecycle
     * state machine accepts the complete valid path. Neither DRAFT → SUSPENDED nor
     * SUSPENDED → ACTIVE is tested here; those are invalid-transition tests.
     *
     * <p>SDET: the test owns its merchant from creation, so all three states are
     * deterministic — no dependency on seed data state or other tests.
     */
    @Test
    @DisplayName("POST /api/merchants/{id}/suspend on ACTIVE → 200 SUSPENDED (full lifecycle chain)")
    void suspend_active_merchant_returns_200_with_suspended_status() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        String merchantId = MerchantsApi.create(
                        CreateMerchantRequest.withTenantRef(
                                UniqueReferences.merchantRef("sus"), "Suspend Me", "TENANT_ALPHA"))
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");

        MerchantsApi.activate(merchantId).then().statusCode(200);

        MerchantResponse body = MerchantsApi.suspend(merchantId)
                .then()
                .statusCode(200)
                .extract()
                .as(MerchantResponse.class);

        assertThat(body.merchantId().toString()).isEqualTo(merchantId);
        assertThat(body.status()).isEqualTo("SUSPENDED");
    }

    /**
     * Contract: POST /api/merchants/{id}/activate on an already-ACTIVE merchant → 409.
     *
     * <p>HTTP concept: 409 Conflict with {@code error: "invalid_transition"} — the request is
     * structurally valid but the merchant's current state does not permit activation.
     * This is the same 409 status code as duplicate reference, but a different {@code error}
     * discriminator — showing how fine-grained error codes let clients respond appropriately.
     *
     * <p>SDET: tests the state machine boundary explicitly. The domain rule is
     * {@code ACTIVE.canTransitionTo(ACTIVE) == false}, throwing {@code InvalidTransitionException}.
     * Black-box tests verify this rule survives serialization and handler mapping — unit tests
     * alone cannot catch a missing {@code @ExceptionHandler(InvalidTransitionException.class)}.
     *
     * <p>SDET interview: why is this a 409 and not a 400? Answer: the request body is valid;
     * the problem is the current resource state, not the request format. 409 communicates
     * "retry with different state" rather than "fix your request syntax."
     */
    @Test
    @DisplayName("POST /api/merchants/{id}/activate on ACTIVE → 409 invalid_transition")
    void activate_already_active_merchant_returns_409_invalid_transition() {
        Ctx.set(TestContext.of(Identities.platformAdmin()));

        String merchantId = MerchantsApi.create(
                        CreateMerchantRequest.withTenantRef(
                                UniqueReferences.merchantRef("tran"), "Transition Test", "TENANT_ALPHA"))
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");

        MerchantsApi.activate(merchantId).then().statusCode(200);

        ProblemAssert.assertThat(MerchantsApi.activate(merchantId))
                .hasStatus(409)
                .hasError(ProblemCodes.INVALID_TRANSITION);
    }
}
