package lab.paymentquality.security;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
import lab.paymentquality.testsupport.PaymentOrderSummaryApiTestSupport;
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

import static org.hamcrest.Matchers.equalTo;

/**
 * Lesson 08 security tests for GET /payment-orders/summary.
 *
 * Verifies authentication, role authorization and tenant ownership
 * enforced by SecurityConfig + PaymentOrderController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Payment Order Summary Security Tests")
class PaymentOrderSummarySecurityTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_summary_security_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("unauthenticated summary request returns 401")
    void unauthenticatedSummaryReturns401() {
        MerchantApiTestSupport.publicRequest(port)
                .accept(ContentType.JSON)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", "00000000-0000-0000-0000-000000000001")
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("denied identity summary request returns 403")
    void deniedIdentitySummaryReturns403() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, TestJwtSupport.deniedToken(), "corr-l08-sec-denied")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("merchant create only role cannot access summary")
    void merchantCreateOnlyCannotAccessSummary() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, creatorToken, "corr-l08-sec-create-only")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("merchant operate only role cannot access summary")
    void merchantOperateOnlyCannotAccessSummary() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String operatorToken = TestJwtSupport.merchantPaymentOperatorToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, operatorToken, "corr-l08-sec-operate-only")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("merchant reader can access own merchant summary")
    void merchantReaderCanAccessOwnMerchantSummary() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l08-sec-own")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("totalOrders", equalTo(0));
    }

    @Test
    @DisplayName("merchant reader cannot access other merchant summary")
    void merchantReaderCannotAccessOtherMerchantSummary() {
        String merchantA = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String merchantB = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerA = TestJwtSupport.merchantPaymentReaderToken(merchantA);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerA, "corr-l08-sec-cross-tenant")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantB)
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"));
    }

    @Test
    @DisplayName("platform reader can access selected merchant summary")
    void platformReaderCanAccessSelectedMerchantSummary() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String platformReader = TestJwtSupport.platformPaymentReaderToken();

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, platformReader, "corr-l08-sec-platform")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("totalAmountMinor", equalTo(0));
    }
}
