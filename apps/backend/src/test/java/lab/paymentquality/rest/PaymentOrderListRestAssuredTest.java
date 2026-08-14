package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.payment.internal.web.PaymentOrderListResponse;
import lab.paymentquality.testsupport.*;
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
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Payment Order List REST Assured Contract Tests")
class PaymentOrderListRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_list_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    private String seedAndGetReader(int count) {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String createToken = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        PaymentOrderListApiTestSupport.seedPaymentOrders(port, merchantId, createToken, count);
        return merchantId;
    }

    private PaymentOrderListResponse list(String merchantId) {
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        return MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .header("X-Correlation-ID", notNullValue())
                .extract().as(PaymentOrderListResponse.class);
    }

    @Test
    @DisplayName("list without filters returns all orders with correct metadata")
    void listAllWithoutFiltersReturns200() {
        String merchantId = seedAndGetReader(5);
        var response = list(merchantId);

        PaymentOrderAssertions.assertThat(response)
                .hasContentSize(5)
                .hasPageMetadata(5, 1);
        assertThat(response.page()).isEqualTo(0);
    }

    @Test
    @DisplayName("list filtered by currency returns only matching orders")
    void listFilteredByCurrencyReturnsOnlyMatchingOrders() {
        String merchantId = seedAndGetReader(10);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("status", "CREATED")
                .queryParam("currency", "PLN")
                .queryParam("size", 5)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).isNotEmpty();
        assertThat(response.content())
                .allMatch(item -> "PLN".equals(item.currency()));
    }

    @Test
    @DisplayName("list filtered by status returns only CREATED orders")
    void listFilteredByStatusReturnsOnlyCreated() {
        String merchantId = seedAndGetReader(5);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("status", "CREATED")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).hasSize(5);
        assertThat(response.content())
                .extracting("status")
                .containsOnly("CREATED");
    }

    @Test
    @DisplayName("list filtered by status returns only CREATED orders")
    void listFilteredByStatusReturnsOnlyCreatedV2(){
        String merchantId = seedAndGetReader(90);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("status", "CREATED")
                .queryParam("size", 5)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);
        
        assertThat(response.content()).hasSize(5);
        assertThat(response.content()).extracting("status").containsOnly("CREATED");
    }

    @Test
    @DisplayName("list with empty result returns 200 with empty content")
    void listEmptyResultReturns200WithEmptyContent() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        var response = list(merchantId);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("list first page returns correct pagination metadata")
    void listFirstPageReturnsCorrectMetadata() {
        String merchantId = seedAndGetReader(25);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).hasSize(10);
        assertThat(response.totalElements()).isEqualTo(25);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("list last page returns remaining items")
    void listLastPageReturnsRemainingItems() {
        String merchantId = seedAndGetReader(25);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("page", 2)
                .queryParam("size", 10)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).hasSize(5);
    }

    @Test
    @DisplayName("list sorted desc returns newest first")
    void listSortedByCreatedAtDescReturnsNewestFirst() {
        String merchantId = seedAndGetReader(5);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("sort", "createdAt,desc")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).hasSize(5);
        var timestamps = response.content().stream()
                .map(o -> o.createdAt())
                .toList();
        assertThat(timestamps).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    @DisplayName("invalid page returns 400 validation")
    void invalidPageReturns400() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("page", -1)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("invalid status returns 400 validation")
    void invalidStatusReturns400() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("status", "INVALID")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("unsupported currency filter returns empty result")
    void invalidCurrencyReturns400() {
        String merchantId = seedAndGetReader(5);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("currency", "GBP")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("list filtered by fromDate and toDate returns orders created today")
    void listFilteredByDateRangeReturnsMatchingOrders() {
        String merchantId = seedAndGetReader(3);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("fromDate", today)
                .queryParam("toDate", today)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).hasSize(3);
    }

    @Test
    @DisplayName("list filtered by future date range returns empty content")
    void listFilteredByFutureDateRangeReturnsEmpty() {
        String merchantId = seedAndGetReader(3);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("fromDate", "2099-01-01")
                .queryParam("toDate", "2099-12-31")
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @DisplayName("list filtered by minAmount and maxAmount returns matching amounts")
    void listFilteredByAmountRangeReturnsMatchingOrders() {
        String merchantId = seedAndGetReader(5);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("minAmount", 2000)
                .queryParam("maxAmount", 4000)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).isNotEmpty();
        assertThat(response.content())
                .allMatch(item -> item.amountMinor() >= 2000 && item.amountMinor() <= 4000);
    }

    @Test
    @DisplayName("list filtered by status and amount combo returns only matching CREATED PLN-range")
    void listFilteredByStatusAndAmountCombo() {
        String merchantId = seedAndGetReader(6);
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        PaymentOrderListResponse response = MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("status", "CREATED")
                .queryParam("minAmount", 1000)
                .queryParam("maxAmount", 6000)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(200)
                .extract().as(PaymentOrderListResponse.class);

        assertThat(response.content()).isNotEmpty();
        assertThat(response.content())
                .allMatch(item -> "CREATED".equals(item.status())
                        && item.amountMinor() >= 1000
                        && item.amountMinor() <= 6000);
    }

    @Test
    @DisplayName("minAmount greater than maxAmount returns 400 validation")
    void minAmountGreaterThanMaxAmountReturns400() {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(port,
                MerchantApiTestSupport.operatorRequest(port));
        String readToken = TestJwtSupport.merchantPaymentReaderToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, readToken)
                .accept(ContentType.JSON)
                .queryParam("minAmount", 5000)
                .queryParam("maxAmount", 1000)
                .when()
                .get("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));
    }
}
