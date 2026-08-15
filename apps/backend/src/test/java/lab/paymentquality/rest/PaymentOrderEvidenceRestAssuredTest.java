package lab.paymentquality.rest;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderEvidenceRepository;
import lab.paymentquality.testsupport.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class PaymentOrderEvidenceRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("po_evidence_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JpaPaymentOrderEvidenceRepository evidenceRepository;

    @Test
    void successfulEvidenceUploadReturnsMetadataAndPersistsRecord() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createPaymentOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        String evidenceId = MerchantApiTestSupport.requestWithToken(port, token)
                .multiPart("file", "refund-proof.txt", "refund approved".getBytes(StandardCharsets.UTF_8), "text/plain")
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence", merchantId, paymentOrderId)
                .then()
                .statusCode(201)
                .header("Location", containsString("/evidence/"))
                .header("X-Correlation-ID", notNullValue())
                .body("evidenceId", notNullValue())
                .body("paymentOrderId", equalTo(paymentOrderId))
                .body("originalFilename", equalTo("refund-proof.txt"))
                .body("contentType", equalTo("text/plain"))
                .body("sizeBytes", equalTo(15))
                .body("storageKey", nullValue())
                .extract().path("evidenceId");

        assertThat(evidenceRepository.findAll())
                .singleElement()
                .satisfies(evidence -> {
                    assertThat(evidence.getEvidenceId().toString()).isEqualTo(evidenceId);
                    assertThat(evidence.getOriginalFilename()).isEqualTo("refund-proof.txt");
                    assertThat(evidence.getStorageKey()).doesNotContain("refund-proof.txt");
                });
    }

    @Test
    void unsupportedEvidenceContentTypeReturnsProblemDetails() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createPaymentOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, token)
                .multiPart("file", "script.sh", "rm -rf /".getBytes(StandardCharsets.UTF_8), "application/x-sh")
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence", merchantId, paymentOrderId)
                .then()
                .statusCode(415)
                .contentType("application/problem+json")
                .body("error", equalTo("unsupported_evidence_content_type"))
                .body("detail", equalTo("Evidence content type is not supported"));
    }

    @Test
    void emptyEvidenceFileIsRejected() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createPaymentOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, token)
                .multiPart("file", "empty.txt", new byte[0], "text/plain")
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence", merchantId, paymentOrderId)
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("error", equalTo("empty_evidence_file"));
    }

    @Test
    void truncatedMultipartReturnsProblemDetailsWithoutServerError() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createPaymentOrder(merchantId);
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);
        String boundary = "----Wave4DeclaredBoundary";
        String truncated = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"proof.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "hello";

        MerchantApiTestSupport.requestWithToken(port, token)
                .config(RestAssured.config().encoderConfig(
                        EncoderConfig.encoderConfig()
                                .encodeContentTypeAs("multipart/form-data", ContentType.TEXT)))
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(truncated)
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence", merchantId, paymentOrderId)
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("error", equalTo("validation"));
    }

    @Test
    void merchantCannotUploadEvidenceForAnotherMerchantPaymentOrder() {
        String merchantA = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String merchantB = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String paymentOrderId = createPaymentOrder(merchantB);
        String merchantAToken = TestJwtSupport.merchantPaymentLifecycleToken(merchantA);

        MerchantApiTestSupport.requestWithToken(port, merchantAToken)
                .multiPart("file", "support-note.txt", "case note".getBytes(StandardCharsets.UTF_8), "text/plain")
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence", merchantB, paymentOrderId)
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("error", equalTo("forbidden"));
    }

    private String createPaymentOrder(String merchantId) {
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        return MerchantApiTestSupport.requestWithToken(port, creatorToken)
                .contentType("application/json")
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("evidence"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(
                        1200, "PLN", PaymentApiTestSupport.uniquePaymentReference("EVIDENCE")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract().path("paymentOrderId");
    }
}
