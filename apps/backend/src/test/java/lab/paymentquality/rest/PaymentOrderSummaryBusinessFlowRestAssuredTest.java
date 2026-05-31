package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.payment.internal.web.PaymentOrderSummaryResponse;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
import lab.paymentquality.testsupport.PaymentOrderSummaryApiTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.assertj.core.api.SoftAssertions;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Lesson 08 business-flow tests for summary endpoint.
 *
 * Validates end-to-end aggregation correctness against explicit oracle data,
 * response observability headers, and cross-tenant denial behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Payment Order Summary Business Flow Tests")
class PaymentOrderSummaryBusinessFlowRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_summary_business_flow_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("business flow: seeded orders produce deterministic summary oracle")
    void seededOrdersProduceDeterministicSummaryOracle() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        var seed = PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);
        var expected = PaymentOrderSummaryApiTestSupport.expectedFor(seed);

        PaymentOrderSummaryResponse response = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-flow-summary")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-l08-flow-summary"))
                .header("ETag", org.hamcrest.Matchers.nullValue())
                .extract().as(PaymentOrderSummaryResponse.class);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(response.totalOrders()).as("totalOrders").isEqualTo(expected.totalOrders());
        softly.assertThat(response.totalAmountMinor()).as("totalAmountMinor").isEqualTo(expected.totalAmountMinor());
        softly.assertThat(response.byCurrency())
                .extracting(PaymentOrderSummaryResponse.CurrencySummary::currency,
                        PaymentOrderSummaryResponse.CurrencySummary::orderCount,
                        PaymentOrderSummaryResponse.CurrencySummary::totalAmountMinor)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("EUR", 1L, 3_000L),
                        org.assertj.core.groups.Tuple.tuple("PLN", 2L, 3_000L),
                        org.assertj.core.groups.Tuple.tuple("USD", 1L, 4_000L)
                );
        softly.assertThat(response.byStatus())
                .extracting(PaymentOrderSummaryResponse.StatusSummary::status,
                        PaymentOrderSummaryResponse.StatusSummary::orderCount,
                        PaymentOrderSummaryResponse.StatusSummary::totalAmountMinor)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("CREATED", 4L, 10_000L));
        softly.assertAll();
    }

    @Test
    @DisplayName("business flow: cross-tenant merchant summary access is blocked with 403")
    void crossTenantMerchantSummaryAccessIsBlocked() {
        String merchantA = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String merchantB = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));

        String creatorA = TestJwtSupport.merchantPaymentCreatorToken(merchantA);
        String readerB = TestJwtSupport.merchantPaymentReaderToken(merchantB);

        PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantA, creatorA);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerB, "corr-l08-flow-cross-tenant")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantA)
                .then()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-l08-flow-cross-tenant"))
                .body("error", equalTo("forbidden"));
    }

    @Test
    @DisplayName("business flow: platform reader can summarize selected merchant")
    void platformReaderCanSummarizeSelectedMerchant() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String platformReader = TestJwtSupport.platformPaymentReaderToken();

        PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);

        PaymentOrderSummaryResponse response = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, platformReader, "corr-l08-flow-platform")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-l08-flow-platform"))
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(response.totalOrders()).isEqualTo(4);
        assertThat(response.totalAmountMinor()).isEqualTo(10_000);
    }
}
