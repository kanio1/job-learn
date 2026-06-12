package lab.paymentquality.restkit.contract.create;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.startsWith;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
public class PaymentOrderReadContractRestKitTest  extends PostgresContainerSupport {
    
    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_order_create_contract_restkit_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;


    @Test
    void readPaymentOrderReturns200EtagNoStoreVaryAndBody() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("read-payment-order");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String reference = PaymentReferences.unique("read-payment-order");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String idempotencyKey = IdempotencyKeys.forScenario("read-payment-order");
        String createCorrelationId = CorrelationIds.forScenario("read-payment-order-create");


        String paymentOrderId = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                idempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.LOCATION, containsString("/payment-orders/"))
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(createCorrelationId))
            .body("paymentOrderId", notNullValue())
            .extract()
            .path("paymentOrderId");

        Response response =  paymentOrderApi.readOrder(merchantId, paymentOrderId, readerToken)
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("paymentOrderId", equalTo(paymentOrderId))
            .body("merchantId", equalTo(merchantId))
            .body("amountMinor", equalTo(12500))
            .body("currency", equalTo("PLN"))
            .body("clientOrderReference", equalTo(reference))
            .extract()
            .response();

            HeaderAssertions.assertSensitivePaymentReadHeaders(response);

        }

    @Test
    void headPaymentOrderReturns200HeadersAndNoBody() {
    MerchantApi merchantApi = new MerchantApi(port);
    PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

    String merchantId = merchantApi.createActiveMerchantAndReturnId("head-payment-order");
    String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
    String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

    String reference = PaymentReferences.unique("head-payment-order");
    CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

    String idempotencyKey = IdempotencyKeys.forScenario("head-payment-order");
    String createCorrelationId = CorrelationIds.forScenario("head-payment-order-create");

    String paymentOrderId = paymentOrderApi.createOrder(
            merchantId,
            creatorToken,
            payload,
            idempotencyKey,
            createCorrelationId
        )
        .statusCode(201)
        .contentType(ContentType.JSON)
        .header(ApiHeaders.LOCATION, containsString("/payment-orders/"))
        .header(ApiHeaders.ETAG, startsWith("\"v"))
        .body("paymentOrderId", notNullValue())
        .extract()
        .path("paymentOrderId");

    Response response = paymentOrderApi.headOrder(merchantId, paymentOrderId, readerToken)
        .statusCode(200)
        .extract()
        .response();

    HeaderAssertions.assertVersionEtag(response);
    HeaderAssertions.assertNoStore(response);
    HeaderAssertions.assertVaryContainsAuthorization(response);

    assertThat(response.asString())
        .as("HEAD response should not contain response body")
        .isBlank();
    
    }

    @Test
    void optionsPaymentOrdereReturns204AllowAcceptPatchAndCorrelationId() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("options-payment-order");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String reference = PaymentReferences.unique("options-payment-order");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(1300, reference);

        String idempotencyKey = IdempotencyKeys.forScenario("options-payment-order");
        String createCorrelationId = CorrelationIds.forScenario("options-payment-order-create");

        String paymentOrderId = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                idempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("paymentOrderId", notNullValue())
            .extract()
            .path("paymentOrderId");

        paymentOrderApi.optionsOrder(merchantId, paymentOrderId)
            .statusCode(204)
            .header(ApiHeaders.ALLOW, allOf(
                containsString("GET"),
                containsString("HEAD"),
                containsString("PATCH"),
                containsString("OPTIONS")
            ))
            .header(ApiHeaders.ACCEPT_PATCH, equalTo("application/merge-patch+json"))
            .header(ApiHeaders.X_CORRELATION_ID, notNullValue());
    }

    @Test
    void deletePaymentOrderReturns405AllowAndProblem() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("delete-payment-order-method-not-allowed");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String reference = PaymentReferences.unique("delete-payment-order-method-not-allowed");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String idempotencyKey = IdempotencyKeys.forScenario("delete-payment-order-method-not-allowed");
        String createCorrelationId = CorrelationIds.forScenario("delete-payment-order-method-not-allowed-create");

        String paymentOrderId = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                idempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("paymentOrderId", notNullValue())
            .extract()
            .path("paymentOrderId");

        Response response = paymentOrderApi.deleteOrder(merchantId, paymentOrderId, readerToken)
            .spec(PaymentErrorSpecs.methodNotAllowed())
            .header(ApiHeaders.ALLOW, allOf(
                containsString("GET"),
                containsString("HEAD"),
                containsString("PATCH"),
                containsString("OPTIONS")
            ))
            .extract()
            .response();
        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "method_not_allowed");
    }

    @Test
    void getPaymentOrderWithAcceptXmlReturns406Problem() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("accept-xml-not-acceptable");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String reference = PaymentReferences.unique("accept-xml-not-acceptable");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        String idempotencyKey = IdempotencyKeys.forScenario("accept-xml-not-acceptable");
        String createCorrelationId = CorrelationIds.forScenario("accept-xml-not-acceptable-create");

        String paymentOrderId = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                idempotencyKey,
                createCorrelationId
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .body("paymentOrderId", notNullValue())
            .extract()
            .path("paymentOrderId");
            
        Response response = paymentOrderApi.readOrderWithAccept(merchantId, paymentOrderId, readerToken, "application/xml")
            .spec(PaymentErrorSpecs.notAcceptable())
            .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "not_acceptable");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
    }

    @Test
        void postPaymentOrderWithTextPlainContentTypeReturns415Problem() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("text-plain-unsupported-media-type");
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String idempotencyKey = IdempotencyKeys.forScenario("text-plain-unsupported-media-type");
        String correlationId = CorrelationIds.forScenario("text-plain-unsupported-media-type");
 
        Response response = paymentOrderApi.createOrderWithRawBodyAndContentType(merchantId, 
            token,
            "this is not json",
            "text/plain",
            idempotencyKey,
            correlationId
        )
            .spec(PaymentErrorSpecs.unsupportedMediaType())
            .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "unsupported_media_type");
        HeaderAssertions.assertNoStore(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
        HeaderAssertions.assertAcceptPatchMergePatchJson(response);
    }
}
