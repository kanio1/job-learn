package lab.paymentquality.restkit.contract.create;

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
        String operatorToken = TestJwtSupport.merchantPaymentOperatorToken(merchantId);

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
            operatorToken,
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

}
