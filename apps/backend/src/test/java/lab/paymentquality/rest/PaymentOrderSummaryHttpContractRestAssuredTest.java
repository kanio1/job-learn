package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
import lab.paymentquality.testsupport.PaymentOrderSummaryApiTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;

/**
 * Lesson 10 HTTP edge contract tests for GET /payment-orders/summary.
 *
 * Verifies HTTP protocol edge cases: route collision avoidance, malformed path variables,
 * unsupported methods, content negotiation, and conditional header discipline.
 *
 * Batch 10A: HTTP Edge Contract Hardening
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Payment Order Summary HTTP Edge Contract Tests")
class PaymentOrderSummaryHttpContractRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_summary_http_edge_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("summary route returns summary shape, not single payment order read shape")
    void summaryRouteReturnsSummaryShapeNotPaymentOrderReadShape() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l10-route")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-l10-route"))
                .body("totalOrders", notNullValue())
                .body("totalAmountMinor", notNullValue())
                .body("byCurrency", notNullValue())
                .body("byStatus", notNullValue())
                // Summary shape fields present, single-order shape fields absent
                .body("paymentOrderId", nullValue())
                .body("amountMinor", nullValue())
                .body("currency", nullValue())
                .body("status", nullValue());
    }

    @ParameterizedTest(name = "malformed merchantId ''{0}'' returns 400 validation")
    @ValueSource(strings = {"not-a-uuid", "12345", "null"})
    @DisplayName("malformed merchantId returns 400 validation")
    void malformedMerchantIdReturnsValidationError(String malformedMerchantId) {
        String readerToken = TestJwtSupport.merchantPaymentReaderToken("00000000-0000-0000-0000-000000000001");

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l10-malformed")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", malformedMerchantId)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("message", containsString("must be a valid UUID"))
                .body("correlationId", notNullValue());
    }

    @Test
    @DisplayName("unsupported methods do not expose summary mutation surface")
    void unsupportedMethodsDoNotExposeSummaryMutationSurface() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        // PUT should return 405 Method Not Allowed
        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .accept(ContentType.JSON)
                .when()
                .put("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(405)
                .header("Allow", containsString("GET"));

        // PATCH should return 403 Forbidden (requires lifecycle role)
        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .accept(ContentType.JSON)
                .when()
                .patch("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(403);

        // DELETE should return 405 Method Not Allowed
        MerchantApiTestSupport.requestWithToken(port, readerToken)
                .accept(ContentType.JSON)
                .when()
                .delete("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(405)
                .header("Allow", containsString("GET"));
    }

    @Test
    @DisplayName("unsupported Accept header is rejected or explicitly characterized")
    void unsupportedAcceptIsRejectedOrExplicitlyCharacterized() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        // Characterization: Spring MVC with @RestController and no explicit produces
        // returns 406 Not Acceptable when Accept header cannot be satisfied
        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l10-accept")
                .header("Accept", "text/xml")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(406);
    }

    @Test
    @DisplayName("If-None-Match does not enable summary caching")
    void ifNoneMatchDoesNotEnableSummaryCaching() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        // Summary endpoint does not support ETag or conditional caching
        // If-None-Match header should be ignored, returning normal 200 response
        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l10-conditional")
                .header("If-None-Match", "\"some-etag\"")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-l10-conditional"))
                .header("ETag", nullValue())
                .body("totalOrders", notNullValue());
    }
}
