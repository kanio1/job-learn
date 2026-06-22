package lab.paymentquality.restkit.contract.create;

import static io.restassured.RestAssured.port;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.startsWith;

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
import lab.paymentquality.restkit.assertions.HeaderAssertions;
import lab.paymentquality.restkit.spec.PaymentAsyncSpec;
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
import net.jqwik.api.Disabled;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
public class PaymentOrderAsyncContractRestKitTest extends PostgresContainerSupport{

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_order_create_contract_restkit_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Disabled("Enable when lifecycle capture supports Prefer: respond-async and returns 202 Accepted")
    @Test
    void asyncCapturePaymentOrderReturns202AcceptedWithOperationLocationAndRetryAfter() {
        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("async-capture-accepted");
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String operatorToken = TestJwtSupport.merchantPaymentOperatorToken(merchantId);

        String reference = PaymentReferences.unique("async-capture-accepted");
        CreatePaymentOrderPayload payload = CreatePaymentOrderPayload.pln(12500, reference);

        ExtractableResponse<Response> created = paymentOrderApi.createOrder(
                merchantId,
                creatorToken,
                payload,
                IdempotencyKeys.forScenario("async-capture-accepted-create"),
                CorrelationIds.forScenario("async-capture-accepted-create")
            )
            .statusCode(201)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("paymentOrderId", notNullValue())
            .body("status", equalTo("CREATED"))
            .extract();

        String paymentOrderId = created.path("paymentOrderId");
        String createdEtag = created.header(ApiHeaders.ETAG);

        ExtractableResponse<Response> authorized = paymentOrderApi.authorizeOrder(
                merchantId,
                paymentOrderId,
                operatorToken,
                Map.of("reason", "prepare-async-capture"),
                IdempotencyKeys.forScenario("async-capture-accepted-authorize"),
                createdEtag,
                CorrelationIds.forScenario("async-capture-accepted-authorize")
            )
            .statusCode(200)
            .contentType(ContentType.JSON)
            .header(ApiHeaders.ETAG, startsWith("\"v"))
            .body("status", equalTo("AUTHORIZED"))
            .extract();

        String authorizedEtag = authorized.header(ApiHeaders.ETAG);

        Response response = paymentOrderApi.captureOrderAsync(
            merchantId,
            paymentOrderId,
            operatorToken,
            Map.of(
                "amountMinor", 12500,
                "reason", "async-capture-requested"
            ),
            IdempotencyKeys.forScenario("async-capture-accepted-capture"),
            authorizedEtag,
            CorrelationIds.forScenario("async-capture-accepted-capture")
        )
        .spec(PaymentAsyncSpec.acceptedAsyncOperation())
        .header(ApiHeaders.X_CORRELATION_ID, notNullValue())
        .extract()
        .response();

    HeaderAssertions.assertLocationPointsToOperation(response);
    HeaderAssertions.assertRetryAfterIsValid(response);
    HeaderAssertions.assertSensitiveResponseIsNotCacheable(response);
    HeaderAssertions.assertVaryContainsAuthorization(response);
    HeaderAssertions.assertVaryContainsIfMatch(response);
    HeaderAssertions.assertVaryContainsIdempotencyKey(response);

    assertThat(response.asString())
        .as("202 Accepted should not pretend that the capture has already completed")
        .doesNotContain("\"CAPTURED\"");

    }
}
