package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.payment.internal.web.PaymentOrderSummaryResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Lesson 08 contract tests for GET /payment-orders/summary.
 *
 * Verifies summary response contract (status, headers, payload shape),
 * filter behavior, and validation errors for PaymentOrderController +
 * PaymentOrderSummaryService + JpaPaymentOrderRepository integration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Payment Order Summary REST Assured Contract Tests")
class PaymentOrderSummaryRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_summary_contract_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("empty merchant summary returns 200 with zero totals and empty groups")
    void emptyMerchantSummaryReturnsZeroTotals() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderSummaryResponse response = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-empty")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-l08-empty"))
                .header("ETag", org.hamcrest.Matchers.nullValue())
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(response.totalOrders()).isZero();
        assertThat(response.totalAmountMinor()).isZero();
        assertThat(response.byCurrency()).isEmpty();
        assertThat(response.byStatus()).isEmpty();
    }

    @Test
    @DisplayName("seeded merchant summary returns exact totals and grouped rows")
    void seededMerchantSummaryReturnsExactTotals() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        var seed = PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);
        var expected = PaymentOrderSummaryApiTestSupport.expectedFor(seed);

        PaymentOrderSummaryResponse response = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-seeded")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", equalTo("corr-l08-seeded"))
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(response.totalOrders()).isEqualTo(expected.totalOrders());
        assertThat(response.totalAmountMinor()).isEqualTo(expected.totalAmountMinor());
        assertThat(response.byCurrency())
                .usingElementComparator(PaymentOrderSummaryApiTestSupport.currencyComparator())
                .containsExactlyInAnyOrderElementsOf(expected.byCurrency());
        assertThat(response.byStatus())
                .usingElementComparator(PaymentOrderSummaryApiTestSupport.statusComparator())
                .containsExactlyInAnyOrderElementsOf(expected.byStatus());
    }

    @Test
    @DisplayName("currency filter changes summary population")
    void currencyFilterChangesSummaryPopulation() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);

        PaymentOrderSummaryResponse response = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-currency")
                .queryParam("currency", "PLN")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(response.totalOrders()).isEqualTo(2);
        assertThat(response.totalAmountMinor()).isEqualTo(3_000);
        assertThat(response.byCurrency())
                .containsExactly(new PaymentOrderSummaryResponse.CurrencySummary("PLN", 2, 3_000));
        assertThat(response.byStatus())
                .containsExactly(new PaymentOrderSummaryResponse.StatusSummary("CREATED", 2, 3_000));
    }

    @Test
    @DisplayName("status filter CREATED returns matching summary")
    void statusFilterCreatedReturnsMatchingSummary() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);

        PaymentOrderSummaryResponse response = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-status")
                .queryParam("status", "CREATED")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(response.totalOrders()).isEqualTo(4);
        assertThat(response.totalAmountMinor()).isEqualTo(10_000);
        assertThat(response.byStatus())
                .containsExactly(new PaymentOrderSummaryResponse.StatusSummary("CREATED", 4, 10_000));
    }

    @Test
    @DisplayName("fromDate and toDate filter population correctly")
    void fromDateAndToDateFilterPopulationCorrectly() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);

        String today = PaymentOrderSummaryApiTestSupport.isoToday();
        String tomorrow = PaymentOrderSummaryApiTestSupport.isoTomorrow();
        String yesterday = PaymentOrderSummaryApiTestSupport.isoYesterday();

        PaymentOrderSummaryResponse todayResponse = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-date-today")
                .queryParam("fromDate", today)
                .queryParam("toDate", tomorrow)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(todayResponse.totalOrders()).isEqualTo(4);
        assertThat(todayResponse.totalAmountMinor()).isEqualTo(10_000);

        PaymentOrderSummaryResponse futureEmpty = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-date-future")
                .queryParam("fromDate", tomorrow)
                .queryParam("toDate", tomorrow)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(futureEmpty.totalOrders()).isZero();
        assertThat(futureEmpty.totalAmountMinor()).isZero();

        PaymentOrderSummaryResponse yesterdayToYesterday = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-date-yesterday")
                .queryParam("fromDate", yesterday)
                .queryParam("toDate", yesterday)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(yesterdayToYesterday.totalOrders()).isZero();
        assertThat(yesterdayToYesterday.totalAmountMinor()).isZero();
    }

    @Test
    @DisplayName("fromDate after toDate returns 200 with empty summary")
    void fromDateAfterToDateReturnsEmptySummary() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String creatorToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        PaymentOrderSummaryApiTestSupport.seedDefaultDataset(port, merchantId, creatorToken);

        PaymentOrderSummaryResponse response = PaymentOrderSummaryApiTestSupport
                .summaryReaderRequest(port, readerToken, "corr-l08-date-inverted")
                .queryParam("fromDate", PaymentOrderSummaryApiTestSupport.isoTomorrow())
                .queryParam("toDate", PaymentOrderSummaryApiTestSupport.isoYesterday())
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderSummaryResponse.class);

        assertThat(response.totalOrders()).isZero();
        assertThat(response.totalAmountMinor()).isZero();
        assertThat(response.byCurrency()).isEmpty();
        assertThat(response.byStatus()).isEmpty();
    }

    @Test
    @DisplayName("invalid currency returns 400 validation")
    void invalidCurrencyReturns400Validation() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l08-invalid-currency")
                .queryParam("currency", "GBP")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("message", equalTo("currency must be PLN, EUR, or USD"));
    }

    @Test
    @DisplayName("invalid status returns 400 validation")
    void invalidStatusReturns400Validation() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l08-invalid-status")
                .queryParam("status", "INVALID")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("message", equalTo("status must be CREATED"));
    }

    @Test
    @DisplayName("invalid date format returns 400 validation")
    void invalidDateFormatReturns400Validation() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l08-invalid-date")
                .queryParam("fromDate", "2026/05/30")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(400)
                .contentType(ContentType.JSON)
                .body("error", equalTo("validation"))
                .body("message", org.hamcrest.Matchers.containsString("Invalid date format"));
    }

    @Test
    @DisplayName("response contains X-Correlation-ID")
    void responseContainsCorrelationId() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port, MerchantApiTestSupport.operatorRequest(port));
        String readerToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderSummaryApiTestSupport.summaryReaderRequest(port, readerToken, "corr-l08-header")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders/summary", merchantId)
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", equalTo("corr-l08-header"))
                .body("totalOrders", notNullValue());
    }
}
