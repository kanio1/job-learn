package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.DisplayName;
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

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class Lesson06RestAssuredLearningTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("lesson06_rest_assured_learning_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("REST basics: public status endpoint returns the foundation contract")
    void publicStatusEndpointReturnsFoundationContract() {
        MerchantApiTestSupport.publicRequest(port)
                // when() marks the HTTP action: from this point REST Assured sends the request.
                .when()
                .get("/api/status")
                // then() starts the oracle: what must be true about the HTTP response.
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("application", equalTo("payment-quality-lab"))
                .body("phase", equalTo("foundation"))
                .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("REST contract: creating a merchant returns 201 and a DRAFT resource representation")
    void createMerchantReturnsCreatedDraftContract() {
        String reference = MerchantApiTestSupport.uniqueMerchantReference("LEARN");

        MerchantApiTestSupport.operatorRequest(port)
                // contentType(JSON) documents that the request body is JSON, not form data or plain text.
                .contentType(ContentType.JSON)
                .body(MerchantApiTestSupport.createMerchantBody(reference, "Learning Merchant"))
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("merchantId", notNullValue())
                .body("merchantReference", equalTo(reference))
                .body("displayName", equalTo("Learning Merchant"))
                .body("status", equalTo("DRAFT"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue());
    }

    @Test
    @DisplayName("Error contract: invalid merchant reference returns validation details")
    void invalidMerchantReferenceReturnsValidationDetails() {
        MerchantApiTestSupport.operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(MerchantApiTestSupport.createMerchantBody(" ", "Invalid Merchant"))
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                // Stable machine-readable error codes are safer for clients than free text messages.
                .body("error", equalTo("validation"))
                .body("details.merchantReference", notNullValue());
    }

    @Test
    @DisplayName("Payment contract: create payment order returns protocol headers and business body")
    void createPaymentOrderReturnsHeadersAndBodyContract() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                // Idempotency-Key makes this POST retry-safe from the client's perspective.
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("learn-create"))
                // X-Correlation-ID lets client, logs and tests connect one request/response path.
                .header("X-Correlation-ID", "lesson06-create-payment")
                .body(PaymentApiTestSupport.createPaymentOrderBody(12500, "PLN", "PAY-LEARN-001"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .header("Location", containsString("/api/merchants/" + merchantId + "/payment-orders/"))
                .header("ETag", startsWith("\"po-"))
                .header("X-Correlation-ID", equalTo("lesson06-create-payment"))
                .body("paymentOrderId", notNullValue())
                .body("merchantId", equalTo(merchantId))
                .body("amountMinor", equalTo(12500))
                .body("currency", equalTo("PLN"))
                .body("clientOrderReference", equalTo("PAY-LEARN-001"))
                .body("status", equalTo("CREATED"));
    }

    @Test
    @DisplayName("Idempotency: replay with the same key and body returns 200 and the same payment order")
    void idempotentReplayReturnsSamePaymentOrder() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("learn-replay");
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(5000, "EUR", "PAY-LEARN-002");

        String firstPaymentOrderId = MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                // extract() is useful when a later HTTP call must reuse a value from the response.
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .body("paymentOrderId", equalTo(firstPaymentOrderId))
                .body("status", equalTo("CREATED"));
    }

    @Test
    @DisplayName("Security boundary: unauthenticated payment order create returns 401")
    void unauthenticatedPaymentOrderCreateReturns401() {
        MerchantApiTestSupport.publicRequest(port)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("learn-unauthenticated"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-LEARN-003"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", "00000000-0000-0000-0000-000000000001")
                .then()
                .statusCode(401);
    }
}
