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

import static org.hamcrest.Matchers.*;

/**
 * Minimal contract tests for MVP + Phase 2 HTTP headers:
 * BE-MVP-001: If-None-Match → 304 Not Modified
 * BE-MVP-002: Idempotency-Replayed header
 * BE-MVP-003: requiredHeader in 428 Problem Details
 * BE-P2-001:  Last-Modified on GET payment order
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class PaymentOrderHttpContractMvpTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("po_http_mvp");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    // BE-MVP-002 — Idempotency-Replayed header
    @Test
    void idempotencyReplayedFalseOnFirstCreate() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("idem-false"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "MVP-IDEM-01"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .header("Idempotency-Replayed", equalTo("false"));
    }

    @Test
    void idempotencyReplayedTrueOnReplay() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String token = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String key = PaymentApiTestSupport.uniqueIdempotencyKey("idem-true");
        Map<String, Object> body = PaymentApiTestSupport.createPaymentOrderBody(1000, "PLN", "MVP-IDEM-02");

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", key)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then().statusCode(201);

        MerchantApiTestSupport.requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", key)
                .body(body)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .header("Idempotency-Replayed", equalTo("true"));
    }

    // BE-MVP-001 — If-None-Match → 304
    @Test
    void conditionalGetReturns304WhenETagMatches() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("cond-get"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(999, "PLN", "MVP-COND-01"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then().statusCode(201)
                .extract().path("paymentOrderId");

        String etag = MerchantApiTestSupport.requestWithToken(port, readerToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{id}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .header("ETag", notNullValue())
                .extract().header("ETag");

        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .header("If-None-Match", etag)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{id}", merchantId, paymentOrderId)
                .then()
                .statusCode(304)
                .header("ETag", equalTo(etag));
    }

    // BE-P2-001 — Last-Modified on GET payment order
    @Test
    void getPaymentOrderReturnsLastModifiedHeader() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("last-mod"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(500, "EUR", "MVP-LMOD-01"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then().statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{id}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .header("Last-Modified", notNullValue());
    }

    // BE-MVP-003 — requiredHeader in 428 Problem Details
    @Test
    void authorizeWithoutIfMatchReturns428WithRequiredHeader() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String lifecycleToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("req-hdr"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(1200, "PLN", "MVP-428-01"))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then().statusCode(201)
                .extract().path("paymentOrderId");

        MerchantApiTestSupport.requestWithToken(port, lifecycleToken)
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("req-hdr-auth"))
                .body("{}")
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{id}/authorize", merchantId, paymentOrderId)
                .then()
                .statusCode(428)
                .body("requiredHeader", equalTo("If-Match"))
                .body("error", equalTo("precondition_required"));
    }
}
