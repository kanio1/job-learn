package lab.paymentquality.testsupport;

import static org.mockito.ArgumentMatchers.contains;

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
import lab.paymentquality.testsupport.restkit.client.MerchantApi;
import lab.paymentquality.testsupport.restkit.client.PaymentOrderApi;
import lab.paymentquality.testsupport.restkit.core.ApiHeaders;
import lab.paymentquality.testsupport.restkit.core.CorrelationIds;
import lab.paymentquality.testsupport.restkit.idempotency.IdempotencyKeys;
import lab.paymentquality.testsupport.restkit.payload.CreatePaymentOrderPayload;
import lab.paymentquality.testsupport.restkit.payload.PaymentReferences;

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
}
