package lab.paymentquality.restkit.contract.create;

import static io.restassured.RestAssured.head;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ResponseOptions;
import lab.paymentquality.restkit.assertions.HeaderAssertions;
import lab.paymentquality.restkit.assertions.ProblemDetailsAssertions;
import lab.paymentquality.restkit.spec.PaymentErrorSpecs;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import lab.paymentquality.testsupport.restkit.client.MerchantApi;
import lab.paymentquality.testsupport.restkit.client.PaymentOrderApi;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;
import lab.paymentquality.testsupport.restkit.core.CorrelationIds;
import lab.paymentquality.testsupport.restkit.idempotency.IdempotencyKeys;
import lab.paymentquality.testsupport.restkit.payload.CreatePaymentOrderPayload;
import lab.paymentquality.testsupport.restkit.payload.PaymentReferences;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
public class PaymentOrderLifecycleContractRestKitTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_order_create_contract_restkit_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
        void authorizePaymentOrderWithCurrentIfMatchReturns200NewEtagAndAuthorizedStatus() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("authorize-current-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("authorize-current-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String createIdempotencyKey = IdempotencyKeys.forScenario("authorize-current-if-match-create");
        String createCorrelationId = CorrelationIds.forScenario("authorize-current-if-match-create");

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                createIdempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");
        String currentEtag = created.header(ApiHeaders.ETAG);

        String authorizeIdempotencyKey = IdempotencyKeys.forScenario("authorize-current-if-match-command");
        String authorizeCorrelationId = CorrelationIds.forScenario("authorize-current-if-match-command");

        Response response = paymentOrderApi.authorizeOrder(
            merchantId,
            paymentOrderId,
            lifecycleToken,
            Map.of("reason", "customer-authenticated"),
            authorizeIdempotencyKey,
            currentEtag,
            authorizeCorrelationId
        )
            .statusCode(200)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(authorizeCorrelationId))
            .body("paymentOrderId", equalTo(paymentOrderId))
            .body("merchantId", equalTo(merchantId))
            .body("status", equalTo("AUTHORIZED"))
            .body("authorizedAt", notNullValue())
            .extract()
            .response();

        HeaderAssertions.assertSensitivePaymentMutationHeaders(response);

        assertThat(response.header(ApiHeaders.ETAG))
            .as("Lifecycle mutation should return a new ETag after successful authorization")
            .isNotEqualTo(currentEtag);
    }

    @Test
    void authorizePaymentOrderWithoutIfMatchReturns428Problem() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("authorize-missing-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("authorize-missing-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String createIdempotencyKey = IdempotencyKeys.forScenario("authorize-missing-if-match-create");
        String createCorrelationId = CorrelationIds.forScenario("authorize-missing-if-match-create");

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                createIdempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");
        String authorizeIdempotencyKey = IdempotencyKeys.forScenario("authorize-missing-if-match-command");
        String authorizeCorrelationId = CorrelationIds.forScenario("authorize-missing-if-match-command");

        Response response = paymentOrderApi.authorizeOrderWithoutIfMatch(
                merchantId,
                paymentOrderId,
                lifecycleToken,
                Map.of("reason", "missing-if-match-negative-test"),
                authorizeIdempotencyKey,
                authorizeCorrelationId
            )
            .spec(PaymentErrorSpecs.preconditionRequired())
            .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "precondition_required");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsIfMatch(response);
    }

    @Test
    void authorizePaymentOrderWithMalformedIfMatchReturns400Problem() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("authorize-malformed-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("authorize-malformed-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String createIdempotencyKey = IdempotencyKeys.forScenario("authorize-malformed-if-match-create");
        String createCorrelationId = CorrelationIds.forScenario("authorize-malformed-if-match-create");

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
            merchantId,
            creatorToken,
            payload,
            createIdempotencyKey,
            createCorrelationId
        )
        .statusCode(201)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .body("paymentOrderId", notNullValue())
        .body("status", equalTo("CREATED"))
        .extract();

        String paymentOrderId = created.path("paymentOrderId");

        String authorizeIdempotencyKey = IdempotencyKeys.forScenario("authorize-malformed-if-match-command");
        String authorizeCorrelationId = CorrelationIds.forScenario("authorize-malformed-if-match-command");

        String malformedIfMatch = "v0";

        Response response = paymentOrderApi.authorizeOrder(merchantId,
            paymentOrderId,
            lifecycleToken,
            Map.of("reason", "test-malformed-if-match"),
            authorizeIdempotencyKey,
            malformedIfMatch,
            authorizeCorrelationId
        )
        .spec(PaymentErrorSpecs.malformedIfMatch())
        .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
        .extract()
        .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "malformed_if_match");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsIfMatch(response);

    }

    @Test
    void capturePaymentOrderWithStaleIfMatchReturns412Problem() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("capture-stale-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("capture-stale-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String createIdempotencyKey = IdempotencyKeys.forScenario("capture-stale-if-match-create");
        String createCorrelationId = CorrelationIds.forScenario("capture-stale-if-match-create");

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                createIdempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");
        String staleEtag = created.header(ApiHeaders.ETAG);
        String authorizeIdempotencyKey = IdempotencyKeys.forScenario("capture-stale-if-match-authorize");
        String authorizeCorrelationId = CorrelationIds.forScenario("capture-stale-if-match-authorize");

        ExtractableResponse<Response> authorized = paymentOrderApi.authorizeOrder(
            merchantId,
            paymentOrderId,
            lifecycleToken,
            Map.of("reason", "prepare-stale-etag-test"),
            authorizeIdempotencyKey,
            staleEtag,
            authorizeCorrelationId
        )
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .body("status", equalTo("AUTHORIZED"))
        .extract();

        String freshTag = authorized.header(ApiHeaders.ETAG);

        assertThat(freshTag)
            .as("Successful authorize should return a new Etag")
            .isNotEqualTo(staleEtag);
        
        String captureIdempotencyKey = IdempotencyKeys.forScenario("capture-stale-if-match-authorize");
        String captureCorrelationId = CorrelationIds.forScenario("capture-stale-if-match-authorize");

        Response response = paymentOrderApi.captureOrder(
            merchantId,
            paymentOrderId,
            lifecycleToken,
            Map.of(
                "amountMinor", 12500,
                "reason", "capture-with-stale-etag"
            ),
            captureIdempotencyKey,
            staleEtag,
            captureCorrelationId
        )
        .spec(PaymentErrorSpecs.preconditionFailed())
        .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
        .extract()
        .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "payment_order_version_mismatch");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsIfMatch(response);

    }

    @Test
    void paymentOrderLifecycleCreatedAuthorizedCapturedRefundedUsesFreshEtagAtEveryStep() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("full-lifecycle");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("full-lifecycle");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String createIdempotencyKey = IdempotencyKeys.forScenario("full-lifecycle-create");
        String createCorrelationId = CorrelationIds.forScenario("full-lifecycle-create");

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                createIdempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("merchantId", equalTo(merchantId))
            .body("amountMinor", equalTo(12500))
            .body("currency", equalTo("PLN"))
            .body("clientOrderReference", equalTo(reference))
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");
        String createdEtag = created.header(ApiHeaders.ETAG);
        String authorizeCorrelationId = CorrelationIds.forScenario("full-lifecycle-authorize");

        Response authorizeResponse = paymentOrderApi.authorizeOrder(
            merchantId,
            paymentOrderId,
            lifecycleToken,
            Map.of(
                "reason", "customer-authenticated"),
            IdempotencyKeys.forScenario("full-lifecycle-authorize"),
            createdEtag,
            authorizeCorrelationId
        )
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .header(ApiHeaders.X_CORRELATION_ID, equalTo(authorizeCorrelationId))
        .body("paymentOrderId", equalTo(paymentOrderId))
        .body("status", equalTo("AUTHORIZED"))
        .body("authorizedAt", notNullValue())
        .extract()
        .response();

        HeaderAssertions.assertSensitivePaymentMutationHeaders(authorizeResponse);
        String authorizedEtag = authorizeResponse.header(ApiHeaders.ETAG);

        assertThat(authorizedEtag)
            .as("Authorize should return a new ETag").isNotEqualTo(createdEtag);

        String captureCorrelationId = CorrelationIds.forScenario("full-lifecycle-capture");

        Response captureResponse = paymentOrderApi.captureOrder(
            merchantId,
            paymentOrderId,
            lifecycleToken, 
            Map.of(
                "amountMinor", 12500,
                "reason", "merchant-captures-authorized-payment"
            ), 
            IdempotencyKeys.forScenario("full-lifecycle-capture"),
            authorizedEtag,
            captureCorrelationId
        )
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .header(ApiHeaders.X_CORRELATION_ID, equalTo(captureCorrelationId))
        .body("paymentOrderId", equalTo(paymentOrderId))
        .body("status", equalTo("CAPTURED"))
        .body("capturedAmountMinor", equalTo(12500))
        .body("capturedAt", notNullValue())
        .extract()
        .response();

        HeaderAssertions.assertSensitivePaymentMutationHeaders(captureResponse);
        String capturedEtag = captureResponse.header(ApiHeaders.ETAG);

        assertThat(capturedEtag)
            .as("capture should return a new Etag").isNotEqualTo(authorizedEtag);

        String refundCorrelationId = CorrelationIds.forScenario("full-lifecycle-refund");

        Response refundResponse = paymentOrderApi.refundOrder(
            merchantId,
            paymentOrderId,
            lifecycleToken,
            Map.of(
                "amountMinor", 12500,
                "reason", "customer-refund"
            ),
            IdempotencyKeys.forScenario("full-lifecycle-refund"),
            capturedEtag,
            refundCorrelationId
        )
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .header(ApiHeaders.X_CORRELATION_ID, equalTo(refundCorrelationId))
        .body("paymentOrderId", equalTo(paymentOrderId))
        .body("status", equalTo("REFUNDED"))
        .body("refundedAmountMinor", equalTo(12500))
        .body("refundedAt", notNullValue())
        .extract()
        .response();

        HeaderAssertions.assertSensitivePaymentMutationHeaders(refundResponse);

        String refundedEtag = refundResponse.header(ApiHeaders.ETAG);

        assertThat(refundedEtag)
            .as("Refund should return a new ETag")
            .isNotEqualTo(capturedEtag);

        assertThat(List.of(createdEtag, authorizedEtag, capturedEtag, refundedEtag))
            .as("Every lifecycle mutation should move the resource to a new ETag version")
            .doesNotHaveDuplicates();
    }

    @Test
    void patchPaymentOrderMetadataWithCurrentIfMatchReturns200UpdatedMetadataAndNewEtag() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("patch-metadata-current-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("patch-metadata-current-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String createIdempotencyKey = IdempotencyKeys.forScenario("patch-metadata-current-if-match-create");
        String createCorrelationId = CorrelationIds.forScenario("patch-metadata-current-if-match-create");

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
            merchantId,
            creatorToken,
            payload,
            createIdempotencyKey,
            createCorrelationId
        )
        .statusCode(201)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .body("paymentOrderId", notNullValue())
        .body("merchantId", equalTo(merchantId))
        .body("status", equalTo("CREATED"))
        .extract();

        String paymentOrderId = created.path("paymentOrderId");
        String currentEtag = created.header(ApiHeaders.ETAG);

        String patchCorrelationId = CorrelationIds.forScenario("patch-metadata-current-if-match-create");

        Map<String, Object> metadataPatch = Map.of(
            "metadata", Map.of(
                "channel", "web",
                "riskProfile", "low",
                "lesson", "21"
            )
        );

        Response response = paymentOrderApi.patchOrderMetadata(
            merchantId,
            paymentOrderId,
            lifecycleToken,
            metadataPatch,
            currentEtag,
            patchCorrelationId
        )
        .statusCode(200)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .header(ApiHeaders.X_CORRELATION_ID, equalTo(patchCorrelationId))
        .body("paymentOrderId", equalTo(paymentOrderId))
        .body("merchantId", equalTo(merchantId))
        .body("status", equalTo("CREATED"))
        .body("metadata.channel", equalTo("web"))
        .body("metadata.riskProfile", equalTo("low"))
        .body("metadata.lesson", equalTo("21"))
        .extract()
        .response();

        HeaderAssertions.assertSensitivePaymentMutationHeaders(response);
        String patchedEtag = response.header(ApiHeaders.ETAG);

        assertThat(patchedEtag)
        .as("PATCH metadata should return a new ETag after changing the resource")
        .isNotEqualTo(currentEtag);
        
    }

    @Test
    void patchPaymentOrderMetadataWithoutIfMatchReturns428Problem() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("patch-metadata-missing-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("patch-metadata-missing-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                IdempotencyKeys.forScenario("patch-metadata-missing-if-match-create"),
                CorrelationIds.forScenario("patch-metadata-missing-if-match-create")
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");

        Map<String, Object> metadataPatch = Map.of(
            "metadata", Map.of(
                "channel", "web",
                "lesson", "22"
            )
        );

        Response response = paymentOrderApi.patchOrdeerMetadataWithoutIfMatch(
            merchantId,
            paymentOrderId,
            lifecycleToken,
            metadataPatch,
            CorrelationIds.forScenario("patch-metadata-missing-if-match")
        )
        .spec(PaymentErrorSpecs.preconditionRequired())
        .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
        .extract()
        .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "precondition_required");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsIfMatch(response);

    }

    @Test
    void patchPaymentOrderMetadataWithMalformedIfMatchReturns400Problem() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("patch-metadata-malformed-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("patch-metadata-malformed-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                IdempotencyKeys.forScenario("patch-metadata-malformed-if-match-create"),
                CorrelationIds.forScenario("patch-metadata-malformed-if-match-create")
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");

        Map<String, Object> metadataPatch = Map.of(
        "metadata", Map.of(
            "channel", "web",
            "lesson", "22"
            )
        );
        String malformedIfMatch = "v0";

        Response response = paymentOrderApi.patchOrderMetadata(
                merchantId,
                paymentOrderId,
                lifecycleToken,
                metadataPatch,
                malformedIfMatch,
                CorrelationIds.forScenario("patch-metadata-malformed-if-match")
            )
            .spec(PaymentErrorSpecs.malformedIfMatch())
            .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "malformed_if_match");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsIfMatch(response);
    }

    @Test
    void patchPaymentOrderMetadataWithStaleIfMatchReturns412Problem() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("patch-metadata-stale-if-match");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String reference = PaymentReferences.unique("patch-metadata-stale-if-match");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                IdempotencyKeys.forScenario("patch-metadata-stale-if-match-create"),
                CorrelationIds.forScenario("patch-metadata-stale-if-match-create")
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");
        String staleEtag = created.header(ApiHeaders.ETAG);

        Map<String, Object> firstPatch = Map.of(
            "metadata", Map.of(
                "channel", "web",
                "revision", "first"
            )
        );

        ExtractableResponse<Response> firstPatched = paymentOrderApi.patchOrderMetadata(
                merchantId,
                paymentOrderId,
                lifecycleToken,
                firstPatch,
                staleEtag,
                CorrelationIds.forScenario("patch-metadata-stale-if-match-first")
            )
            .statusCode(200)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("metadata.channel", equalTo("web"))
            .body("metadata.revision", equalTo("first"))
            .extract();

        String freshEtag = firstPatched.header(ApiHeaders.ETAG);

        assertThat(freshEtag)
            .as("First PATCH should return a new ETag")
            .isNotEqualTo(staleEtag);

        Map<String, Object> secondPatch = Map.of(
            "metadata", Map.of(
                "channel", "mobile",
                "revision", "second"
            )
        );

        Response response = paymentOrderApi.patchOrderMetadata(
                merchantId,
                paymentOrderId,
                lifecycleToken,
                secondPatch,
                staleEtag,
                CorrelationIds.forScenario("patch-metadata-stale-if-match-second")
            )
            .spec(PaymentErrorSpecs.preconditionFailed())
            .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "payment_order_version_mismatch");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsIfMatch(response);
}


}
