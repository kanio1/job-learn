package lab.paymentquality.testing;

import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import lab.paymentquality.testing.internal.seed.DeterministicDataset;
import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies that POST /api/test/reset and POST /api/test/seed return 200 when
 * app.testing.enabled=true and the test profile is active.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.testing.enabled=true"})
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class TestEndpointsEnabledIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("test_endpoints_enabled_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DeterministicDataset dataset;

    @BeforeEach
    void resetBeforeEach() {
        dataset.reset();
    }

    @Test
    void postSeedReturns200WithMinimalResponseBody() {
        ValidatableResponse response = RestAssured.given().port(port)
                .when().post("/api/test/seed")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("operation", equalTo("seed"))
                .body("status", equalTo("completed"));

        // Verify no forbidden fields in response
        String json = response.extract().asString();
        assertThat(json).doesNotContainIgnoringCase("tenantId");
        assertThat(json).doesNotContainIgnoringCase("merchantId");
        assertThat(json).doesNotContainIgnoringCase("paymentOrderId");
        assertThat(json).doesNotContainIgnoringCase("accessToken");
        assertThat(json).doesNotContainIgnoringCase("password");
        assertThat(json).doesNotContainIgnoringCase("payload");
    }

    @Test
    void postResetReturns200WithMinimalResponseBody() {
        ValidatableResponse response = RestAssured.given().port(port)
                .when().post("/api/test/reset")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("operation", equalTo("reset"))
                .body("status", equalTo("completed"));

        String json = response.extract().asString();
        assertThat(json).doesNotContainIgnoringCase("password");
        assertThat(json).doesNotContainIgnoringCase("payload");
    }

    @Test
    void seedLoads104PaymentOrdersIntoDatabase() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(104);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_session", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_event", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_fulfillment", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_anomaly", Integer.class)).isEqualTo(0);
    }

    @Test
    void postSeedLearningReturns200WithPaymentTruth() {
        RestAssured.given().port(port)
                .when().post("/api/test/seed-learning")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("operation", equalTo("seed-learning"))
                .body("status", equalTo("completed"))
                .body("truth.tenants", equalTo(5))
                .body("truth.merchants", equalTo(20))
                .body("truth.payments", equalTo(10_000))
                .body("truth.paymentHistoryRows", equalTo(28_000))
                .body("truth.capturedPayments", equalTo(6_000))
                .body("truth.refundedPayments", equalTo(1_200))
                .body("truth.cancelledPayments", equalTo(800))
                .body("truth.authorizedPayments", equalTo(800))
                .body("truth.expiredPayments", equalTo(400))
                .body("truth.createdPayments", equalTo(800))
                .body("truth.tenantAlphaPayments", equalTo(5_500))
                .body("truth.checkoutSessions", equalTo(2_000))
                .body("truth.checkoutEvents", equalTo(5_000))
                .body("truth.checkoutFulfillments", equalTo(1_950))
                .body("truth.checkoutAnomalies", equalTo(50))
                .body("truth.auditEvents", equalTo(10_000))
                .body("truth.publicationEvents", equalTo(10_000))
                .body("truth.failedPublications", equalTo(100));
    }

    @Test
    void postSeedLearningAcceptsExplicitSmallProfile() {
        RestAssured.given().port(port)
                .queryParam("profile", "SMALL")
                .when().post("/api/test/seed-learning")
                .then()
                .statusCode(200)
                .body("operation", equalTo("seed-learning"))
                .body("status", equalTo("completed"))
                .body("truth.tenants", equalTo(5))
                .body("truth.failedPublications", equalTo(100));
    }

    @Test
    void postSeedLearningRejectsUnknownProfile() {
        RestAssured.given().port(port)
                .queryParam("profile", "MEDIUM")
                .when().post("/api/test/seed-learning")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400))
                .body("title", equalTo("Bad Request"))
                .body("error", equalTo("validation"))
                .body("code", equalTo("VALIDATION"))
                .body("detail", equalTo("Learning seed profile must be SMALL"))
                .header("X-Correlation-ID", notNullValue());
    }

    @Test
    void seedAfterLearningSeedStillLoads104PaymentOrders() {
        RestAssured.given().port(port).post("/api/test/seed-learning").then().statusCode(200);
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(104);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_session", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_event", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_fulfillment", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_anomaly", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_publication", Integer.class)).isEqualTo(0);
    }

    @Test
    void seedLoadsMerchantAlpha001With101Orders() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders WHERE merchant_id = ?",
                Integer.class,
                UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
        assertThat(count).isEqualTo(101);
    }

    @Test
    void resetClearsAllMutableData() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(104);

        RestAssured.given().port(port).post("/api/test/reset").then().statusCode(200);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_session", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_publication", Integer.class)).isEqualTo(0);
    }

    @Test
    void resetAfterLearningSeedClearsCheckoutAuditAndPublications() {
        RestAssured.given().port(port).post("/api/test/seed-learning").then().statusCode(200);
        RestAssured.given().port(port).post("/api/test/reset").then().statusCode(200);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_session", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_event", Integer.class)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM event_publication", Integer.class)).isEqualTo(0);
    }

    @Test
    void resetAfterEvidenceUploadReturns200AndClearsEvidenceRows() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);

        String merchantId = "00000000-0000-0000-0000-0000000000b1";
        String paymentOrderId = "00000000-0000-0000-0000-0000000000c1";
        String token = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);

        MerchantApiTestSupport.requestWithToken(port, token)
                .multiPart("file", "reset-proof.txt", "keep".getBytes(), "text/plain")
                .when()
                .post("/api/merchants/{merchantId}/payment-orders/{paymentOrderId}/evidence",
                        merchantId, paymentOrderId)
                .then()
                .statusCode(201);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_order_evidence", Integer.class))
                .isGreaterThan(0);

        RestAssured.given().port(port).post("/api/test/reset").then().statusCode(200);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_order_evidence", Integer.class))
                .isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_order_note", Integer.class))
                .isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(0);
    }

    @Test
    void seedAfterResetRestoresFullDeterministicState() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);
        RestAssured.given().port(port).post("/api/test/reset").then().statusCode(200);
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(104);
    }

    @Test
    void seedIsIdempotent() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);
        Integer ordersAfterFirstSeed = jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class);

        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);
        Integer ordersAfterSecondSeed = jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class);

        assertThat(ordersAfterSecondSeed).isEqualTo(ordersAfterFirstSeed);
        assertThat(ordersAfterSecondSeed).isEqualTo(104);
    }

    @Test
    void seededOrdersHaveDeterministicUuids() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);

        Integer c1 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders WHERE payment_order_id = ?",
                Integer.class, UUID.fromString("00000000-0000-0000-0000-0000000000c1"));
        assertThat(c1).isEqualTo(1);

        Integer c101 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders WHERE payment_order_id = ?",
                Integer.class, UUID.fromString("00000000-0000-0000-0000-00000000c101"));
        assertThat(c101).isEqualTo(1);
    }

    @Test
    void merchantAlpha001StatusCountsMatchExpectedSummaryAfterSeed() {
        RestAssured.given().port(port).post("/api/test/seed").then().statusCode(200);

        UUID alpha001 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

        assertStatusCount(alpha001, "CREATED", 21);
        assertStatusCount(alpha001, "AUTHORIZED", 21);
        assertStatusCount(alpha001, "CAPTURED", 21);
        assertStatusCount(alpha001, "CANCELLED", 19);
        assertStatusCount(alpha001, "REFUNDED", 19);
    }

    private void assertStatusCount(UUID merchantId, String status, int expected) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders WHERE merchant_id = ? AND status = ?",
                Integer.class, merchantId, status);
        assertThat(count).as("Status %s count for merchant %s", status, merchantId).isEqualTo(expected);
    }
}
