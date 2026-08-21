package lab.paymentquality.rest;

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
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "payment.refund-challenge.expose-pin=false")
@Import(TestJwtConfiguration.class)
@Testcontainers
class RefundChallengePinHiddenRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("refund_challenge_pin_hidden");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("201 pin is absent when expose-pin is false (prod mapping)")
    void pinAbsentWhenNotExposed() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(
                port, MerchantApiTestSupport.operatorRequest(port));
        String creator = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creator)
                .contentType("application/json")
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("hide-create"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(
                        100_001, "PLN", PaymentApiTestSupport.uniquePaymentReference("HID")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract()
                .path("paymentOrderId");
        String maker = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);
        String reader = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String etag = MerchantApiTestSupport.requestWithToken(port, reader)
                .when()
                .get("/api/merchants/{id}/payment-orders/{oid}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
        String afterAuth = MerchantApiTestSupport.requestWithToken(port, maker)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("hide-auth"))
                .header("If-Match", etag)
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/authorize", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
        MerchantApiTestSupport.requestWithToken(port, maker)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("hide-cap"))
                .header("If-Match", afterAuth)
                .contentType("application/json")
                .body(Map.of("amountMinor", 100_001))
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/capture", merchantId, paymentOrderId)
                .then()
                .statusCode(200);
        MerchantApiTestSupport.requestWithToken(port, maker)
                .contentType("application/json")
                .body(Map.of("amountMinor", 100_001))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-approvals", merchantId, paymentOrderId)
                .then()
                .statusCode(201);

        MerchantApiTestSupport.requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges", merchantId, paymentOrderId)
                .then()
                .statusCode(201)
                .body("pin", nullValue());
    }
}
