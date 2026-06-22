package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
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

/**
 * Stabilized learning copy of payment order business-flow tests.
 *
 * This class is kept compilable so it no longer blocks testCompile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MyPaymentOrderBusinessFlowRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("my_payment_business_flow_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("Business flow: creator creates payment order and merchant reader reads the same representation")
    void creatorCreatesPaymentOrderAndMerchantReaderReadsIt() {
        String merchantId = activeMerchant();
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("business-create-read");
        String clientReference = PaymentApiTestSupport.uniquePaymentReference("BUSINESS");

        Response createResponse = paymentCreatorRequest(creatorToken, idempotencyKey, "corr-business-create-001")
                .body(PaymentApiTestSupport.createPaymentOrderBody(12_500, "PLN", clientReference))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId);

        String paymentOrderId = createResponse.then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .header("Location", containsString("/api/merchants/" + merchantId + "/payment-orders/"))
                .header("ETag", startsWith("\"v"))
                .header("X-Correlation-ID", equalTo("corr-business-create-001"))
                .body("paymentOrderId", notNullValue())
                .body("merchantId", equalTo(merchantId))
                .body("amountMinor", equalTo(12_500))
                .body("currency", equalTo("PLN"))
                .body("clientOrderReference", equalTo(clientReference))
                .body("status", equalTo("CREATED"))
                .body("createdAt", notNullValue())
                .body("updatedAt", notNullValue())
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .header("X-Correlation-ID", "corr-business-read-001")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("ETag", startsWith("\"v"))
                .header("X-Correlation-ID", equalTo("corr-business-read-001"))
                .body("paymentOrderId", equalTo(paymentOrderId))
                .body("merchantId", equalTo(merchantId))
                .body("amountMinor", equalTo(12_500))
                .body("currency", equalTo("PLN"))
                .body("clientOrderReference", equalTo(clientReference))
                .body("status", equalTo("CREATED"));
    }

    @Test
    @DisplayName("Business flow: lost response retry returns 200 and the original payment order")
    void retryWithSameIdempotencyKeyReturnsOriginalPaymentOrder() {
        String merchantId = activeMerchant();
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("business-retry");
        String clientReference = PaymentApiTestSupport.uniquePaymentReference("RETRY");
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(5_000, "EUR", clientReference);

        String paymentOrderId = paymentCreatorRequest(creatorToken, idempotencyKey, "corr-business-retry-first")
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        paymentCreatorRequest(creatorToken, idempotencyKey, "corr-business-retry-second")
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("ETag", startsWith("\"v"))
                .header("X-Correlation-ID", equalTo("corr-business-retry-second"))
                .body("paymentOrderId", equalTo(paymentOrderId))
                .body("merchantId", equalTo(merchantId))
                .body("amountMinor", equalTo(5_000))
                .body("currency", equalTo("EUR"))
                .body("clientOrderReference", equalTo(clientReference))
                .body("status", equalTo("CREATED"));
    }

    @Test
    @DisplayName("Business flow: same idempotency key with a different business intent returns 409")
    void sameIdempotencyKeyWithDifferentBodyReturnsConflict() {
        String merchantId = activeMerchant();
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("business-conflict");

        paymentCreatorRequest(creatorToken, idempotencyKey, "corr-business-conflict-first")
                .body(PaymentApiTestSupport.createPaymentOrderBody(1_000, "PLN",
                        PaymentApiTestSupport.uniquePaymentReference("CONFLICT-A")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201);

        paymentCreatorRequest(creatorToken, idempotencyKey, "corr-business-conflict-second")
                .body(PaymentApiTestSupport.createPaymentOrderBody(9_999, "USD",
                        PaymentApiTestSupport.uniquePaymentReference("CONFLICT-B")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-business-conflict-second"))
                .body("error", equalTo("idempotency_conflict"))
                .body("message", notNullValue())
                .body("correlationId", equalTo("corr-business-conflict-second"));
    }

    @Test
    @DisplayName("Business flow: merchant tenant isolation masks cross-tenant payment order reads")
    void merchantReaderFromAnotherMerchantGetsMasked404() {
        String merchantIdA = activeMerchant();
        String merchantIdB = activeMerchant();
        String creatorTokenA = TestJwtSupport.merchantPaymentCreatorToken(merchantIdA);
        String readerTokenB = TestJwtSupport.merchantPaymentReaderToken(merchantIdB);

        String paymentOrderId = paymentCreatorRequest(creatorTokenA,
                PaymentApiTestSupport.uniqueIdempotencyKey("business-tenant"), "corr-business-tenant-create")
                .body(PaymentApiTestSupport.createPaymentOrderBody(3_000, "PLN",
                        PaymentApiTestSupport.uniquePaymentReference("TENANT")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantIdA)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, readerTokenB)
                .header("X-Correlation-ID", "corr-business-tenant-read")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantIdA, paymentOrderId)
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-business-tenant-read"))
                .body("error", equalTo("not_found"))
                .body("correlationId", equalTo("corr-business-tenant-read"));
    }

    @Test
    @DisplayName("Business flow: platform reader can read but cannot create merchant payment orders")
    void platformPaymentReaderCanReadButCannotCreate() {
        String merchantId = activeMerchant();
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String platformReaderToken = TestJwtSupport.platformPaymentReaderToken();

        String paymentOrderId = paymentCreatorRequest(creatorToken,
                PaymentApiTestSupport.uniqueIdempotencyKey("business-platform-create"), "corr-business-platform-create")
                .body(PaymentApiTestSupport.createPaymentOrderBody(7_500, "USD",
                        PaymentApiTestSupport.uniquePaymentReference("PLATFORM")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, platformReaderToken)
                .header("X-Correlation-ID", "corr-business-platform-read")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("ETag", startsWith("\"v"))
                .body("paymentOrderId", equalTo(paymentOrderId))
                .body("merchantId", equalTo(merchantId));

        paymentCreatorRequest(platformReaderToken,
                PaymentApiTestSupport.uniqueIdempotencyKey("business-platform-forbidden"), "corr-business-platform-create-denied")
                .body(PaymentApiTestSupport.createPaymentOrderBody(8_000, "PLN",
                        PaymentApiTestSupport.uniquePaymentReference("PLATFORM-DENIED")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(403);
    }

    private String activeMerchant() {
        return PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
    }

    private io.restassured.specification.RequestSpecification paymentCreatorRequest(
            String token, String idempotencyKey, String correlationId) {
        return MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Correlation-ID", correlationId);
    }
}
