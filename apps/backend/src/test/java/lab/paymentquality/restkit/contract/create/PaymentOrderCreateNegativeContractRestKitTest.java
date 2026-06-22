package lab.paymentquality.restkit.contract.create;

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

import io.restassured.response.Response;
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
import lab.paymentquality.testsupport.restkit.payload.InvalidPaymentOrderPayloads;
import lab.paymentquality.testsupport.restkit.payload.PaymentReferences;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
public class PaymentOrderCreateNegativeContractRestKitTest extends PostgresContainerSupport{

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_order_create_contract_restkit_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void zeroAmountReturns400ValidationProblem() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("zero-amount-value");
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String reference = PaymentReferences.unique("zero-amount-validation");
        Map<String, Object> invalidPayload = InvalidPaymentOrderPayloads.zeroAmount(reference);

        String idempotencyKey = IdempotencyKeys.forScenario("zereo-amount-validation");
        String correlationId = CorrelationIds.forScenario("zero-amount-validation");

        Response response = paymentOrderApi.createOrderWithBody(merchantId, token, invalidPayload, idempotencyKey, correlationId)
            .spec(PaymentErrorSpecs.validationProblem())
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(correlationId))
            .body("correlationId", equalTo(correlationId))
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "validation");
    }

    @Test
    void malformedJsonReturns400Problem() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("malformed-json");
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String malformedJson = InvalidPaymentOrderPayloads.malformedJson();
        String idempotencyKey = IdempotencyKeys.forScenario("malformed-json");
        String correlationId = CorrelationIds.forScenario("malformed-json");

        Response response = paymentOrderApi.createOrderWithBody(merchantId, token, malformedJson, idempotencyKey, correlationId)
            .spec(PaymentErrorSpecs.badRequestProblem())
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(correlationId))
            .body("correlationId", equalTo(correlationId))
            .extract()
            .response();

        ProblemDetailsAssertions.assertSafeProblem(response);
        ProblemDetailsAssertions.assertProblemError(response, "malformed_json");
    }

    @Test
    void unknownTopLevelFieldReturns400Problem() {

        MerchantApi merchantApi = new MerchantApi(port);
        PaymentOrderApi paymentOrderApi = new PaymentOrderApi(port);

        String merchantId = merchantApi.createActiveMerchantAndReturnId("unknown-top-level-field");
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        String reference = PaymentReferences.unique("unknown-top-level-field");

        Map<String, Object> payloadWithUnknownField = Map.of(
            "amountMinor", 12500,
            "currency", "PLN",
            "clientOrderReference", reference,
            "status", "CAPTURED"
        );

        String idempotencyKey = IdempotencyKeys.forScenario("unknown-top-level-field");
        String correlationId = CorrelationIds.forScenario("unknown-top-level-field");

        Response response = paymentOrderApi.createOrderWithBody(merchantId, token, payloadWithUnknownField, idempotencyKey, correlationId)
            .spec(PaymentErrorSpecs.badRequestProblem())
            .header(ApiHeaders.X_CORRELATION_ID, equalTo(correlationId))
            .body("correlationId", equalTo(correlationId))
            .extract()
            .response();
        
            ProblemDetailsAssertions.assertSafeProblem(response);
            ProblemDetailsAssertions.assertProblemError(response, "unknown_top_level_field");
            ProblemDetailsAssertions.assertValidationDetailsContainField(response, "status");

    }

}
