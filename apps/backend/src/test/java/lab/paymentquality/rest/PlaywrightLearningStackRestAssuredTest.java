package lab.paymentquality.rest;

import io.restassured.RestAssured;
import lab.paymentquality.payment.internal.application.PaymentExportJobService;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class PlaywrightLearningStackRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("pw_learning_stack");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    PaymentExportJobService exportJobService;

    @Test
    void evidenceUploadStoresCategoryAndBytesForDownload() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createPaymentOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        String evidenceId = MerchantApiTestSupport.requestWithToken(port, token)
                .multiPart("file", "proof.png", png, "image/png")
                .multiPart("category", "INVOICE")
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence", merchantId, paymentOrderId)
                .then()
                .statusCode(201)
                .body("category", equalTo("INVOICE"))
                .extract().path("evidenceId");

        byte[] downloaded = MerchantApiTestSupport.requestWithToken(port, token)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence/{evidenceId}",
                        merchantId, paymentOrderId, evidenceId)
                .then()
                .statusCode(200)
                .header("Content-Disposition", org.hamcrest.Matchers.containsString("proof.png"))
                .extract().asByteArray();

        org.assertj.core.api.Assertions.assertThat(downloaded).isEqualTo(png);
    }

    @Test
    void asyncExportJobReturns202ThenReadyCsv() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        createPaymentOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String jobId = MerchantApiTestSupport.requestWithToken(port, token)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/export-jobs", merchantId)
                .then()
                .statusCode(202)
                .header("Location", org.hamcrest.Matchers.containsString("/export-jobs/"))
                .header("Retry-After", equalTo("1"))
                .body("status", equalTo("PENDING"))
                .extract().path("jobId");

        exportJobService.processDueJobs(10);

        MerchantApiTestSupport.requestWithToken(port, token)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/export-jobs/{jobId}", merchantId, jobId)
                .then()
                .statusCode(200)
                .body("status", equalTo("READY"));

        String csv = MerchantApiTestSupport.requestWithToken(port, token)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/export-jobs/{jobId}/content", merchantId, jobId)
                .then()
                .statusCode(200)
                .header("Content-Disposition", org.hamcrest.Matchers.containsString("attachment"))
                .extract().asString();

        org.assertj.core.api.Assertions.assertThat(csv).contains("paymentOrderId");
    }

    @Test
    void merchantCannotCreateOrReadAnotherMerchantsExportJob() {
        String merchantA = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String merchantB = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        createPaymentOrder(merchantA);
        String tokenA = TestJwtSupport.merchantPaymentReaderToken(merchantA);
        String tokenB = TestJwtSupport.merchantPaymentReaderToken(merchantB);

        String jobId = MerchantApiTestSupport.requestWithToken(port, tokenA)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/export-jobs", merchantA)
                .then()
                .statusCode(202)
                .extract().path("jobId");

        MerchantApiTestSupport.requestWithToken(port, tokenB)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/export-jobs", merchantA)
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"));

        MerchantApiTestSupport.requestWithToken(port, tokenB)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/export-jobs/{jobId}", merchantA, jobId)
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"));
    }

    @Test
    void exportJobContentBeforeReadyReturnsConflict() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        createPaymentOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        String jobId = MerchantApiTestSupport.requestWithToken(port, token)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/export-jobs", merchantId)
                .then()
                .statusCode(202)
                .extract().path("jobId");

        MerchantApiTestSupport.requestWithToken(port, token)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/export-jobs/{jobId}/content", merchantId, jobId)
                .then()
                .statusCode(409)
                .header("Retry-After", equalTo("1"))
                .body("error", equalTo("export_job_not_ready"));
    }

    @Test
    void refundApprovalRejectedWhenOrderIsNotCaptured() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createPaymentOrder(merchantId);
        String makerToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, makerToken)
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/refund-approvals", merchantId, paymentOrderId)
                .then()
                .statusCode(422)
                .body("error", equalTo("invalid_transition"));
    }

    @Test
    void merchantDirectRefundRequiresDualControl() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createCapturedOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String etag = MerchantApiTestSupport.requestWithToken(port, readerToken)
                .when()
                .get("/api/merchants/{id}/payment-orders/{oid}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract().header("ETag");

        MerchantApiTestSupport.requestWithToken(port, token)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("direct-refund"))
                .header("If-Match", etag)
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/refund", merchantId, paymentOrderId)
                .then()
                .statusCode(409)
                .body("error", equalTo("dual_control_required"));
    }

    @Test
    void makerCannotSelfApproveRefundRequest() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createCapturedOrder(merchantId);
        String makerToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String checkerToken = TestJwtSupport.platformAdminToken();

        String etag = MerchantApiTestSupport.requestWithToken(port, readerToken)
                .when()
                .get("/api/merchants/{id}/payment-orders/{oid}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract().header("ETag");

        String approvalId = MerchantApiTestSupport.requestWithToken(port, makerToken)
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/refund-approvals", merchantId, paymentOrderId)
                .then()
                .statusCode(201)
                .body("status", equalTo("PENDING"))
                .extract().path("approvalId");

        MerchantApiTestSupport.requestWithToken(port, makerToken)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("self"))
                .header("If-Match", etag)
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/refund-approvals/{aid}/approve",
                        merchantId, paymentOrderId, approvalId)
                .then()
                .statusCode(409)
                .body("error", equalTo("dual_control_self_approve"));

        MerchantApiTestSupport.requestWithToken(port, checkerToken)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("checker"))
                .header("If-Match", etag)
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/refund-approvals/{aid}/approve",
                        merchantId, paymentOrderId, approvalId)
                .then()
                .statusCode(200)
                .body("status", equalTo("REFUNDED"));
    }

    @Test
    void expirationSweepRequiresPlatformLifecycle() {
        RestAssured.given().port(port)
                .when()
                .post("/api/payment-ops/expiration-sweep")
                .then()
                .statusCode(401);

        MerchantApiTestSupport.requestWithToken(port, TestJwtSupport.merchantPaymentLifecycleToken(UUID.randomUUID().toString()))
                .when()
                .post("/api/payment-ops/expiration-sweep")
                .then()
                .statusCode(403);

        MerchantApiTestSupport.requestWithToken(port, TestJwtSupport.platformAdminToken())
                .when()
                .post("/api/payment-ops/expiration-sweep")
                .then()
                .statusCode(200)
                .body("expiredCount", notNullValue());
    }

    private String createPaymentOrder(String merchantId) {
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        return MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType("application/json")
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("pwlearn"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(
                        2100, "PLN", PaymentApiTestSupport.uniquePaymentReference("PW")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");
    }

    private String createCapturedOrder(String merchantId) {
        String paymentOrderId = createPaymentOrder(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);
        String etag = MerchantApiTestSupport.requestWithToken(port, readerToken)
                .when()
                .get("/api/merchants/{id}/payment-orders/{oid}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract().header("ETag");
        String afterAuth = MerchantApiTestSupport.requestWithToken(port, token)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("auth"))
                .header("If-Match", etag)
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/authorize", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract().header("ETag");
        MerchantApiTestSupport.requestWithToken(port, token)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("cap"))
                .header("If-Match", afterAuth)
                .contentType("application/json")
                .body(Map.of("amountMinor", 2100))
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/capture", merchantId, paymentOrderId)
                .then()
                .statusCode(200);
        return paymentOrderId;
    }
}
