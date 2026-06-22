package lab.paymentquality.restkit.contract.create;

import static io.restassured.RestAssured.port;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.startsWith;

import org.junit.jupiter.api.Disabled;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
public class PaymentRateLimitContractRestKitTest extends PostgresContainerSupport {


    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_order_create_contract_restkit_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void secondReadWithinRateLimitWindowReturns429ProblemWithRetryAfter() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("rate-limit-second-read");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String reference = PaymentReferences.unique("rate-limit-second-read");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                IdempotencyKeys.forScenario("rate-limit-second-read-create"),
                CorrelationIds.forScenario("rate-limit-second-read-create")
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .extract();

        String paymentOrderId = created.path("paymentOrderId");

        paymentOrderApi.readOrder(
                merchantId,
                paymentOrderId,
                readerToken
            )
            .statusCode(200)
            .contentType(ContentType.JSON);

        Response response = paymentOrderApi.readOrder(
                merchantId,
                paymentOrderId,
                readerToken
            )
            .spec(PaymentErrorSpecs.tooManyRequests())
            .header(ApiHeaders.RETRY_AFTER, notNullValue())
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "too_many_requests");
        HeaderAssertions.assertRetryAfterIsValid(response);
        HeaderAssertions.assertSensitiveResponseIsNotCacheable(response);
        HeaderAssertions.assertVaryContainsAuthorization(response);
    }

}
