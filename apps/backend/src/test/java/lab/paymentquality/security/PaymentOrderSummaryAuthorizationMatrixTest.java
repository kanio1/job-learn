package lab.paymentquality.security;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Lesson 10 parameterized authorization matrix for GET /payment-orders/summary.
 *
 * Verifies authentication, role authorization, and merchant ownership through
 * 12 matrix rows covering 401, 403, and 200 outcomes with explicit BOLA/BFLA labeling.
 *
 * Batch 10B: Parameterized Authorization Matrix
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Payment Order Summary Authorization Matrix")
class PaymentOrderSummaryAuthorizationMatrixTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_summary_auth_matrix_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    record SummaryAccessCase(
            String displayName,
            String bolaBflaLabel,
            int expectedStatus
    ) {}

    static Stream<Arguments> summaryAccessMatrix() {
        return Stream.of(
                // Authentication failures (401)
                Arguments.of("unauthenticated request returns 401", "", 401, "UNAUTHENTICATED"),
                Arguments.of("invalid issuer token returns 401", "", 401, "INVALID_ISSUER"),
                Arguments.of("invalid signature token returns 401", "", 401, "INVALID_SIGNATURE"),
                Arguments.of("expired token returns 401", "", 401, "EXPIRED"),
                // Authorization failures (403) - BFLA
                Arguments.of("denied token returns 403 [BFLA]", "BFLA", 403, "DENIED"),
                Arguments.of("merchant create-only returns 403 [BFLA]", "BFLA", 403, "CREATE_ONLY"),
                Arguments.of("merchant operate-only returns 403 [BFLA]", "BFLA", 403, "OPERATE_ONLY"),
                Arguments.of("merchant read without merchant_id claim returns 403 [BFLA]", "BFLA", 403, "READ_NO_CLAIM"),
                // Success (200)
                Arguments.of("merchant read own merchant returns 200", "", 200, "OWN_MERCHANT"),
                // Authorization failure (403) - BOLA
                Arguments.of("merchant read other merchant returns 403 [BOLA]", "BOLA", 403, "CROSS_TENANT"),
                // Success (200)
                Arguments.of("platform payment reader returns 200", "", 200, "PLATFORM_READER"),
                // Authorization failure (403) - BFLA
                Arguments.of("platform merchant-only returns 403 [BFLA]", "BFLA", 403, "PLATFORM_MERCHANT_ONLY")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("summaryAccessMatrix")
    @DisplayName("summary access matrix enforces authentication, authorization, and ownership")
    void summaryAccessMatrixEnforcesAuthenticationAuthorizationAndOwnership(
            String displayName,
            String bolaBflaLabel,
            int expectedStatus,
            String tokenType) {

        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String otherMerchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));

        String token = switch (tokenType) {
            case "UNAUTHENTICATED" -> null;
            case "INVALID_ISSUER" -> TestJwtSupport.invalidIssuerToken();
            case "INVALID_SIGNATURE" -> TestJwtSupport.invalidSignatureToken();
            case "EXPIRED" -> TestJwtSupport.expiredToken();
            case "DENIED" -> TestJwtSupport.deniedToken();
            case "CREATE_ONLY" -> TestJwtSupport.merchantPaymentCreatorToken(merchantId);
            case "OPERATE_ONLY" -> TestJwtSupport.merchantPaymentOperatorToken(merchantId);
            case "READ_NO_CLAIM" -> TestJwtSupport.merchantPaymentReaderTokenWithoutMerchantIdClaim();
            case "OWN_MERCHANT" -> TestJwtSupport.merchantPaymentReaderToken(merchantId);
            case "CROSS_TENANT" -> TestJwtSupport.merchantPaymentReaderToken(otherMerchantId);
            case "PLATFORM_READER" -> TestJwtSupport.platformPaymentReaderToken();
            case "PLATFORM_MERCHANT_ONLY" -> TestJwtSupport.platformOperatorToken();
            default -> throw new IllegalArgumentException("Unknown token type: " + tokenType);
        };

        String targetMerchantId = "CROSS_TENANT".equals(tokenType) ? merchantId : merchantId;

        var request = token == null
                ? MerchantApiTestSupport.publicRequest(port)
                : MerchantApiTestSupport.requestWithToken(port, token);

        var response = request
                .accept(ContentType.JSON)
                .header("X-Correlation-ID", "corr-l10-matrix-" + tokenType.toLowerCase())
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", targetMerchantId)
                .then()
                .statusCode(expectedStatus);

        if (expectedStatus == 200) {
            response.contentType(ContentType.JSON)
                    .body("totalOrders", notNullValue());
        } else if (expectedStatus == 403
                && ("READ_NO_CLAIM".equals(tokenType) || "CROSS_TENANT".equals(tokenType))) {
            // Controller-level 403 has JSON body via PaymentExceptionHandler
            response.contentType(ContentType.JSON)
                    .body("error", equalTo("forbidden"));
        }
        // Spring Security 403/401 responses have no body - no content-type assertion
    }
}
