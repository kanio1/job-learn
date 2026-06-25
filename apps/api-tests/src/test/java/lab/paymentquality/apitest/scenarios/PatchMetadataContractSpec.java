package lab.paymentquality.apitest.scenarios;

import io.restassured.response.Response;
import lab.paymentquality.apitest.api.SeedApi;
import lab.paymentquality.apitest.api.payment.PaymentOrdersApi;
import lab.paymentquality.apitest.api.payment.dto.CreatePaymentOrderRequest;
import lab.paymentquality.apitest.api.payment.dto.PatchMetadataRequest;
import lab.paymentquality.apitest.core.auth.Identities;
import lab.paymentquality.apitest.core.context.Ctx;
import lab.paymentquality.apitest.core.context.TestContext;
import lab.paymentquality.apitest.core.data.IdempotencyKeys;
import lab.paymentquality.apitest.core.data.Seeds;
import lab.paymentquality.apitest.core.data.UniqueReferences;
import lab.paymentquality.apitest.core.http.Headers;
import lab.paymentquality.apitest.core.problem.ProblemAssert;
import lab.paymentquality.apitest.core.problem.ProblemCodes;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8E — JSON Merge Patch contract for
 * {@code PATCH /api/merchants/{merchantId}/payment-orders/{paymentOrderId}}.
 *
 * <p>Covers four high-value scenarios:
 * <ol>
 *   <li>Valid merge-patch returns 200 with incremented ETag (happy path)</li>
 *   <li>Missing {@code If-Match} → 428 {@code precondition_required}</li>
 *   <li>Wrong {@code Content-Type} → 415 {@code unsupported_media_type} + {@code Accept-Patch}</li>
 *   <li>Unknown top-level field → 400 {@code unknown_top_level_field} with field details</li>
 *   <li>Stale {@code If-Match} → 412 {@code payment_order_version_mismatch}</li>
 * </ol>
 *
 * <p><strong>Backend contract source:</strong>
 * <ul>
 *   <li>{@code PaymentOrderController.updateMetadata()} — endpoint mapping and guard order</li>
 *   <li>{@code MetadataPatchRequest} — only {@code metadata} field allowed; extras → 400</li>
 *   <li>{@code PaymentEtag.requireVersion()} — 428 if null/blank; 412 if stale</li>
 *   <li>{@code PaymentExceptionHandler} — all error → problem+json mappings</li>
 *   <li>{@code SecurityConfig} line 77: {@code PATCH .../payment-orders/*} requires
 *       {@code merchant:payments:lifecycle} or {@code platform:payments:lifecycle}</li>
 * </ul>
 *
 * <p><strong>Guard order in controller</strong> (note: differs from lifecycle POSTs):
 * <ol>
 *   <li>Security filter: authority check ({@code merchant:payments:lifecycle})</li>
 *   <li>{@code verifyMerchantOwnership()} — 403 if JWT {@code merchant_id} ≠ path {@code merchantId}</li>
 *   <li>{@code request.requireOnlyMetadataTopLevelField()} — 400 if unknown top-level fields</li>
 *   <li>{@code PaymentEtag.requireVersion(ifMatch)} — 428 if null; parses "v{N}" → version long</li>
 *   <li>{@code paymentLifecycleService.updateMetadata()} — 412 if version stale; stores metadata JSON</li>
 * </ol>
 *
 * <p><strong>SDET learning — JSON Merge Patch (RFC 7396) vs JSON Patch (RFC 6902):</strong>
 * Merge Patch sends the desired end-state of the changed sub-document; the server replaces that
 * sub-document with the value received. JSON Patch sends a list of operations (add, remove,
 * replace, …) applied atomically. This API uses Merge Patch because the only patchable
 * sub-document is {@code metadata} (a flat {@code Map<String, String>}) — there is no benefit
 * in expressing it as operations when a simple replacement is sufficient.
 *
 * <p><strong>SDET learning — content-type negotiation at the dispatcher layer:</strong>
 * Spring MVC rejects unsupported {@code Content-Type} values with 415 at handler selection
 * time, before the controller method body executes. This means the 415 short-circuits all
 * controller-level guards (ownership, unknown-field, ETag). Testers must send a valid
 * {@code Content-Type} to reach any business-logic guard.
 *
 * <p>All tests use {@link Identities#seededMerchantCreator()} — the Keycloak user
 * {@code merchant.alpha.creator} whose JWT carries {@code merchant_id = MERCHANT_ALPHA_001_ID}
 * and the {@code merchant:payments:lifecycle} (and {@code merchant:payments:create}) authority.
 */
@Tag("contract")
@Tag("http")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PatchMetadataContractSpec {

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

    /**
     * Valid merge-patch with correct {@code If-Match} returns 200 and increments the ETag.
     *
     * <p>A freshly created order starts at version 0 ({@code ETag: "v0"}). A PATCH that passes
     * all guards increments the JPA {@code @Version} counter: response carries {@code ETag: "v1"}.
     * The order {@code status} field does not change — PATCH is a metadata-only operation.
     *
     * <p>Assertions:
     * <ul>
     *   <li>200 OK</li>
     *   <li>{@code ETag: "v1"} — incremented by 1</li>
     *   <li>{@code Vary: If-Match} — correct caching directive</li>
     *   <li>{@code Cache-Control: no-store} — sensitive resource</li>
     *   <li>{@code status: "CREATED"} — state machine not advanced</li>
     * </ul>
     *
     * <p>Business risk verified: metadata updates are version-tracked so concurrent patchers
     * cannot silently overwrite each other's changes; each caller must re-read the current ETag.
     */
    @Test
    @Order(1)
    void valid_metadata_patch_returns_200_and_increments_etag() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        String ref = UniqueReferences.paymentRef("patch-happy");
        String createKey = IdempotencyKeys.generate("patch-create");
        Response created = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(1000L, "PLN", ref),
                createKey);
        assertThat(created.statusCode()).as("create must succeed").isEqualTo(201);
        String paymentOrderId = created.jsonPath().getString("paymentOrderId");

        PatchMetadataRequest patch = new PatchMetadataRequest(Map.of("env", "test", "phase", "8e"));
        Response patchResponse = PaymentOrdersApi.patch(
                Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId, "\"v0\"", patch);

        assertThat(patchResponse.statusCode()).as("expect 200 OK").isEqualTo(200);
        assertThat(patchResponse.header(Headers.ETAG)).as("ETag incremented to v1").isEqualTo("\"v1\"");
        assertThat(patchResponse.header(Headers.VARY)).as("Vary: If-Match").containsIgnoringCase("If-Match");
        assertThat(patchResponse.header(Headers.CACHE_CONTROL)).as("no-store").containsIgnoringCase("no-store");
        assertThat(patchResponse.jsonPath().getString("status")).as("status unchanged by PATCH").isEqualTo("CREATED");
    }

    /**
     * PATCH without {@code If-Match} returns 428 Precondition Required.
     *
     * <p>{@code If-Match} is declared {@code required = false} at the Spring MVC layer so that
     * Spring does not emit a generic 400 for missing headers. The controller reads the header,
     * then {@code PaymentEtag.requireVersion(null)} throws {@code PaymentPreconditionRequiredException}
     * → mapped to 428 by {@code PaymentExceptionHandler.handlePreconditionRequired()}.
     *
     * <p>428 (RFC 6585 §3) means "the server requires a precondition that the client did not send".
     * Compare with 412 ("the precondition was sent but did not match the current resource state").
     *
     * <p>The {@code Vary: If-Match} response header on the 428 signals to intermediate caches
     * that the response is conditional on that header.
     */
    @Test
    @Order(2)
    void patch_without_if_match_returns_428() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));
        PatchMetadataRequest patch = new PatchMetadataRequest(Map.of("env", "test"));

        Response response = PaymentOrdersApi.patchWithoutIfMatch(
                Seeds.MERCHANT_ALPHA_001_ID, Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID, patch);

        ProblemAssert.assertThat(response)
                .hasStatus(428)
                .hasError(ProblemCodes.PRECONDITION_REQUIRED)
                .hasContentTypeProblemJson()
                .hasCorrelationId()
                .hasNoStore()
                .varyContains("If-Match");
    }

    /**
     * PATCH with {@code Content-Type: text/plain} returns 415 and an {@code Accept-Patch} header.
     *
     * <p>Spring MVC rejects the request at handler selection time
     * ({@code HttpMediaTypeNotSupportedException}) — before the controller method is invoked.
     * The 415 response carries {@code Accept-Patch: application/merge-patch+json} (RFC 5789 §3.1)
     * to advertise the supported patch content-type to clients that sent the wrong one.
     *
     * <p>SDET learning: content-type negotiation fires BEFORE merchant ownership, unknown-field,
     * and ETag checks. No DB access occurs on a 415 path. The test uses a seeded order ID but
     * any valid-looking UUID would produce the same result.
     */
    @Test
    @Order(3)
    void patch_with_wrong_content_type_returns_415_and_accept_patch_header() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response response = PaymentOrdersApi.patchWithWrongContentType(
                Seeds.MERCHANT_ALPHA_001_ID, Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID, "\"v0\"");

        ProblemAssert.assertThat(response)
                .hasStatus(415)
                .hasError(ProblemCodes.UNSUPPORTED_MEDIA_TYPE)
                .hasContentTypeProblemJson();
        assertThat(response.header(Headers.ACCEPT_PATCH))
                .as("Accept-Patch header advertises merge-patch")
                .isEqualTo("application/merge-patch+json");
    }

    /**
     * PATCH with an unknown top-level field returns 400 with field-level error details.
     *
     * <p>Body {@code {"metadata":{},"unknownField":"forbidden"}} contains a field outside the
     * backend's known set. {@code MetadataPatchRequest} captures it via {@code @JsonAnySetter},
     * then {@code requireOnlyMetadataTopLevelField()} throws
     * {@code UnknownMetadataPatchFieldException} → 400 {@code unknown_top_level_field}.
     *
     * <p>The 400 body includes a {@code details} array with one entry per rejected field name.
     *
     * <p>Guard ordering: this check (step 2 in the controller) fires BEFORE
     * {@code PaymentEtag.requireVersion()} (step 3). A body with an unknown field returns 400
     * regardless of the {@code If-Match} value — even a stale ETag does not change the outcome.
     */
    @Test
    @Order(4)
    void patch_with_unknown_top_level_field_returns_400_with_field_details() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        Response response = PaymentOrdersApi.patchWithUnknownField(
                Seeds.MERCHANT_ALPHA_001_ID, Seeds.PAYMENT_ORDER_ALPHA_001_CREATED_ID, "\"v0\"");

        ProblemAssert.assertThat(response)
                .hasStatus(400)
                .hasError(ProblemCodes.UNKNOWN_TOP_LEVEL_FIELD)
                .hasContentTypeProblemJson()
                .hasCorrelationId()
                .hasFieldError("unknownField")
                .hasNoStore();
    }

    /**
     * PATCH with a stale {@code If-Match} returns 412 {@code payment_order_version_mismatch}.
     *
     * <p>The first PATCH uses the fresh {@code ETag: "v0"} from create and succeeds, moving the
     * payment order version to {@code "v1"}. The second PATCH deliberately reuses the stale
     * original {@code "v0"} ETag with an otherwise valid merge-patch body. Because the body has
     * only the allowed {@code metadata} top-level field, the request reaches
     * {@code paymentLifecycleService.updateMetadata()}, where the version precondition fails.
     *
     * <p>This confirms the guard order discovered in Phase 8E: unknown-field validation happens
     * before ETag parsing, but a structurally valid merge patch reaches the stale-version guard and
     * maps to 412 rather than 400 or 428.
     */
    @Test
    @Order(5)
    void patch_with_stale_if_match_returns_412_payment_order_version_mismatch() {
        Ctx.set(TestContext.of(Identities.seededMerchantCreator()));

        String ref = UniqueReferences.paymentRef("patch-stale-etag");
        Response created = PaymentOrdersApi.create(
                Seeds.MERCHANT_ALPHA_001_ID,
                CreatePaymentOrderRequest.valid(1_250L, "PLN", ref),
                IdempotencyKeys.generate("patch-stale-create"));
        assertThat(created.statusCode()).as("create must succeed").isEqualTo(201);
        String paymentOrderId = created.jsonPath().getString("paymentOrderId");
        String staleEtag = created.header(Headers.ETAG);
        assertThat(staleEtag).as("freshly created order starts at v0").isEqualTo("\"v0\"");

        PatchMetadataRequest firstPatch = new PatchMetadataRequest(Map.of("phase", "8j", "step", "fresh"));
        Response firstPatchResponse = PaymentOrdersApi.patch(
                Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId, staleEtag, firstPatch);
        assertThat(firstPatchResponse.statusCode()).as("first PATCH with current ETag succeeds").isEqualTo(200);
        assertThat(firstPatchResponse.header(Headers.ETAG)).as("PATCH increments ETag to v1").isEqualTo("\"v1\"");

        PatchMetadataRequest secondPatch = new PatchMetadataRequest(Map.of("phase", "8j", "step", "stale"));
        Response stalePatchResponse = PaymentOrdersApi.patch(
                Seeds.MERCHANT_ALPHA_001_ID, paymentOrderId, staleEtag, secondPatch);

        ProblemAssert.assertThat(stalePatchResponse)
                .hasStatus(412)
                .hasError(ProblemCodes.PAYMENT_ORDER_VERSION_MISMATCH)
                .hasContentTypeProblemJson()
                .hasCorrelationId()
                .hasNoStore()
                .varyContains("If-Match")
                .matchesProblemSchema();
    }
}
