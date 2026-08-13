package lab.paymentquality.rest;

import lab.paymentquality.testsupport.CheckoutLabTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.checkout-lab.enabled=true",
                "app.checkout-lab.oauth-client-secret=test-oauth-secret",
                "app.checkout-lab.hmac-secret=test-hmac-secret",
                "app.checkout-lab.hosted-checkout-base-url=http://localhost:3000"
        })
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class CheckoutLabRefundNotifyRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_refund_notify_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void refundAfterCompletedEmitsRefundedNotifyEvent() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String notifyUrl = "http://localhost:" + port + "/api/checkout-lab/notify";
        String location = CheckoutLabTestSupport.createSession(port, token, "BOOK-REFUND", notifyUrl);
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);
        CheckoutLabTestSupport.simulateCompleted(port, sessionId);

        given().port(port)
                .header("Authorization", "Bearer " + token)
                .when().post("/api/checkout-lab/sessions/" + sessionId + "/refund")
                .then()
                .statusCode(200)
                .body("sessionId", equalTo(sessionId))
                .body("status", equalTo("REFUNDED"));

        given().port(port)
                .header("Authorization", "Bearer " + token)
                .when().post("/api/checkout-lab/sessions/" + sessionId + "/refund")
                .then()
                .statusCode(409);

        Integer refundEvents = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_event WHERE session_id = CAST(? AS uuid) AND event_type = 'checkout.session.refunded'",
                Integer.class,
                sessionId);
        assertThat(refundEvents).isEqualTo(1);
    }
}
