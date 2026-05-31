package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class PaymentOrderRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void createPaymentOrderReturns201WithHeaders() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(12500, "PLN", "PAY-001");
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("create");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-Correlation-ID", "corr-create-001")
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .header("Location", containsString("/payment-orders/"))
                .header("ETag", startsWith("\"po-"))
                .header("X-Correlation-ID", equalTo("corr-create-001"))
                .body("paymentOrderId", notNullValue())
                .body("merchantId", equalTo(merchantId))
                .body("amountMinor", equalTo(12500))
                .body("currency", equalTo("PLN"))
                .body("status", equalTo("CREATED"))
                .body("clientOrderReference", equalTo("PAY-001"));
    }

    @Test
    void idempotentReplayReturns200WithSameId() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(5000, "EUR", "PAY-002");
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("replay");

        String firstId = MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .body("paymentOrderId", equalTo(firstId));
    }

    @Test
    void idempotencyConflictReturns409() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("conflict");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-003"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201);

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentApiTestSupport.createPaymentOrderBody(9999, "EUR", "PAY-999"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(409)
                .body("error", equalTo("idempotency_conflict"));
    }

    @Test
    void validationErrorReturns400() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(0, "PLN", "PAY-004");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("val"))
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));
    }

    @Test
    void missingIdempotencyKeyReturns400() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-005");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(400);
    }

    @Test
    void nonActiveMerchantReturns409() {
        String merchantId = MerchantApiTestSupport.operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(MerchantApiTestSupport.createMerchantBody(
                        MerchantApiTestSupport.uniqueMerchantReference("INACTIVE"), "Inactive Merchant"))
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201)
                .extract().path("merchantId");

        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("inactive"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-006"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(409)
                .body("error", equalTo("merchant_not_payment_eligible"));
    }

    @Test
    void readPaymentOrderReturns200WithEtag() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("read");

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentApiTestSupport.createPaymentOrderBody(7500, "USD", "PAY-007"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .header("ETag", startsWith("\"po-"))
                .body("paymentOrderId", equalTo(paymentOrderId))
                .body("status", equalTo("CREATED"));
    }

    @Test
    void crossTenantReadReturns404() {
        String merchantIdA = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String creatorTokenA = TestJwtSupport.merchantPaymentCreatorToken(merchantIdA);
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("cross");

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorTokenA)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "PAY-008"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantIdA)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        String merchantIdB = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String readerTokenB = TestJwtSupport.merchantPaymentReaderToken(merchantIdB);

        MerchantApiTestSupport.requestWithToken(port, readerTokenB)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantIdA, paymentOrderId)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void platformReaderCanReadCrossMerchant() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String platformToken = TestJwtSupport.platformPaymentReaderToken();
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("platform");

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentApiTestSupport.createPaymentOrderBody(2000, "EUR", "PAY-009"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, platformToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .body("paymentOrderId", equalTo(paymentOrderId));
    }

    @Test
    void platformReaderGets404WhenNestedMerchantPathDoesNotOwnOrder() {
        String merchantIdA = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String merchantIdB = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantIdA);
        String platformToken = TestJwtSupport.platformPaymentReaderToken();

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("platform-wrong-path"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(2000, "EUR", "PAY-WRONG-PATH"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantIdA)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, platformToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}", merchantIdB, paymentOrderId)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void etagIsSameOnCreateReplayAndRead() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(5000, "PLN", "PAY-ETAG-001");
        String idempotencyKey = PaymentApiTestSupport.uniqueIdempotencyKey("etag-consistency");

        var createResult = MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201);

        String createEtag = createResult.extract().header("ETag");
        String paymentOrderId = createResult.extract().path("paymentOrderId");

        String replayEtag = MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().header("ETag");

        String readEtag = MerchantApiTestSupport.requestWithToken(port, readerToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}",
                        merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract().header("ETag");

        assertThat(replayEtag).isEqualTo(createEtag);
        assertThat(readEtag).isEqualTo(createEtag);
    }
}
