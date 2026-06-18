package lab.paymentquality.restkit.contract.create;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;



@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
public class PaymentOrderCreateContractRestKitTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_order_create_contract_restkit_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void createPaymentOrderReturns201LocationEtagCorrelationIdAndBody() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("create-contract");
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String reference = PaymentReferences.unique("create-contract");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String idempotencyKey = IdempotencyKeys.forScenario("create-contract");
        String correlationId = CorrelationIds.forScenario("create-contract");

        paymentOrderApi.createOrder(merchantId, token, payload, idempotencyKey, correlationId)
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.LOCATION, containsString("/payment-orders/"))
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(correlationId))
            .body("paymentOrderId", notNullValue())
            .body("merchantId", equalTo(merchantId))
            .body("amountMinor", equalTo(12500))
            .body("currency", equalTo("PLN"))
            .body("clientOrderReference", equalTo(reference));                                               
    }

    @Test
    void createPaymentOrderReturns200WhenReplayTheSameIdempotencyKey() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("idempotency-test");
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String reference = PaymentReferences.unique("idempotency-test");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(2000, reference);

        String idempotencyKey = IdempotencyKeys.forScenario("idempotency-test");
        String correlationId = CorrelationIds.forScenario("idempotency-test");

        String firstPaymentOrderId = paymentOrderApi.createOrder(merchantId, token, payload, idempotencyKey, correlationId)
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.LOCATION, containsString("/payment-orders/"))
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(correlationId))
            .body("paymentOrderId", notNullValue())
            .body("merchantId", equalTo(merchantId))
            .body("amountMinor", equalTo(2000))
            .body("currency", equalTo("PLN"))
            .body("clientOrderReference", equalTo(reference))
            .extract()
            .path("paymenOrderId");

        String secondPaymentOrderId = paymentOrderApi.createOrder(merchantId, token, payload, idempotencyKey, correlationId)
            .statusCode(200)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(correlationId))
            .body("paymentOrderId", notNullValue())
            .body("merchantId", equalTo(merchantId))
            .body("amountMinor", equalTo(2000))
            .body("currency", equalTo("PLN"))
            .body("clientOrderReference", equalTo(reference))
            .extract()
            .path("paymenOrderId");
        
        assertThat(secondPaymentOrderId)
            .as("Replay with the same Idempotency-Key and same payload should return the same paymentOrderId")
            .isEqualTo(firstPaymentOrderId);
    }

    @Test
    void createPaymentOrderReturns409WhenTheSameIdemKeyAndDifferentBody() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("conflict-idemKey");
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String firstReference = PaymentReferences.unique("conflict-idemKey-first");
        String secondReference = PaymentReferences.unique("conflict-idemKey-second");

        CreatePaymentOrderPayload firstPayload = CreatePaymentOrderPayload.pln(2000, firstReference);
        CreatePaymentOrderPayload differentPayload = CreatePaymentOrderPayload.pln(3000, secondReference);

        String idempotencyKey = IdempotencyKeys.forScenario("conflict-idemKey");
        String firstCorrelationId = CorrelationIds.forScenario("conflict-idemKey-first");
        String conflictCorrelationId = CorrelationIds.forScenario("conflict-idemKey-first");

        String firstPaymentOrderId = paymentOrderApi.createOrder(merchantId, token, firstPayload, idempotencyKey, firstCorrelationId)
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.LOCATION, containsString("/payment-orders/"))
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(firstCorrelationId))
            .body("paymentOrderId", notNullValue())
            .body("merchantId", equalTo(merchantId))
            .body("amountMinor", equalTo(2000))
            .body("currency", equalTo("PLN"))
            .body("clientOrderReference", equalTo(firstReference))
            .extract()
            .path("paymentOrderId");

        Response conflictResponse = paymentOrderApi.createOrder(merchantId, token, differentPayload, idempotencyKey, conflictCorrelationId)
            .spec(PaymentErrorSpecs.idempotencyConflict())
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(conflictCorrelationId))
            .body("correlationId", equalTo(conflictCorrelationId))
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(conflictResponse);
        ProblemDetailsAssertions.assertProblemError(conflictResponse, "idempotency_conflict");
        
    }

    @Test
    void createPaymentOrderVaryContainsAuthorizationAndIdempotencyKeyBecauseReplayDependsOnKey() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("create-vary-idempotency-key");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String reference = PaymentReferences.unique("create-vary-idempotency-key");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String idempotencyKey = IdempotencyKeys.forScenario("create-vary-idempotency-key");
        String correlationId = CorrelationIds.forScenario("create-vary-idempotency-key");

        Response response = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                idempotencyKey,
                correlationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.LOCATION, notNullValue())
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract()
            .response();

        HeaderAssertions.assertSensitiveResponseIsNotCacheable(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsIdempotencyKey(response);
    }

    @Test
    void notAcceptableProblemResponseVaryContainsAcceptBecauseResponseDependsOnRequestedMediaType() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("vary-accept-not-acceptable");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String reference = PaymentReferences.unique("vary-accept-not-acceptable");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                IdempotencyKeys.forScenario("vary-accept-not-acceptable-create"),
                CorrelationIds.forScenario("vary-accept-not-acceptable-create")
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .extract();

        String paymentOrderId = created.path("paymentOrderId");

        Response response = paymentOrderApi.readOrderWithAccept(
                merchantId,
                paymentOrderId,
                readerToken,
                "application/xml"
            )
            .spec(PaymentErrorSpecs.notAcceptable())
            .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "not_acceptable");
        HeaderAssertions.assertSensitiveResponseIsNotCacheable(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertVaryContainsAccept(response);
    }
}
