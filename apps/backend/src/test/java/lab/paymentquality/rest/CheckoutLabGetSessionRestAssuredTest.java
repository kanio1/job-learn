package lab.paymentquality.rest;

import lab.paymentquality.testsupport.CheckoutLabTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

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
class CheckoutLabGetSessionRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_get_session_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void getSessionWithLabBearerReturns200JsonSnapshot() {
        String labToken = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String correlationId = "corr-t07-get-session";
        String location = createSession(labToken, correlationId);
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        given()
                .port(port)
                .header("Authorization", "Bearer " + labToken)
                .header("X-Correlation-ID", correlationId)
                .when()
                .get("/api/checkout-lab/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", equalTo(correlationId))
                .body("sessionId", equalTo(sessionId))
                .body("extOrderId", equalTo("BOOK-GET-1"))
                .body("status", equalTo("CREATED"))
                .body("amountMinor", equalTo(1999))
                .body("currency", equalTo("PLN"))
                .body("continueUrl", equalTo("http://localhost:3000/checkout-lab/return"))
                .body("notifyUrl", equalTo("http://localhost:8080/api/checkout-lab/notify"))
                .body("redirectUri", startsWith("http://localhost:3000/psp/checkout/"))
                .body("correlationId", equalTo(correlationId))
                .body("validityUntil", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void getSessionIsIdempotentAndDoesNotCreateExtraRows() {
        String labToken = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = createSession(labToken, "corr-t07-idempotent");
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        Integer sessionsBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_session", Integer.class);

        String firstStatus = given()
                .port(port)
                .header("Authorization", "Bearer " + labToken)
                .when()
                .get("/api/checkout-lab/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .extract()
                .path("status");

        String secondStatus = given()
                .port(port)
                .header("Authorization", "Bearer " + labToken)
                .when()
                .get("/api/checkout-lab/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .extract()
                .path("status");

        Integer sessionsAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_session", Integer.class);

        assertThat(firstStatus).isEqualTo("CREATED");
        assertThat(secondStatus).isEqualTo(firstStatus);
        assertThat(sessionsAfter).isEqualTo(sessionsBefore);
    }

    @Test
    void getUnknownSessionReturns404ProblemDetails() {
        String labToken = CheckoutLabTestSupport.obtainLabAccessToken(port);
        UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000099");

        given()
                .port(port)
                .header("Authorization", "Bearer " + labToken)
                .header("X-Correlation-ID", "corr-t07-not-found")
                .when()
                .get("/api/checkout-lab/sessions/" + missingId)
                .then()
                .statusCode(404)
                .header("X-Correlation-ID", equalTo("corr-t07-not-found"))
                .contentType("application/problem+json")
                .body("error", equalTo("not_found"));
    }

    @Test
    void getSessionWithoutLabBearerReturns401() {
        UUID sessionId = UUID.randomUUID();

        given()
                .port(port)
                .when()
                .get("/api/checkout-lab/sessions/" + sessionId)
                .then()
                .statusCode(401);
    }

    private String createSession(String labToken, String correlationId) {
        return given()
                .port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + labToken)
                .header("X-Correlation-ID", correlationId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-GET-1",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return",
                          "notifyUrl": "http://localhost:8080/api/checkout-lab/notify",
                          "validitySeconds": 900
                        }
                        """)
                .when()
                .post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302)
                .extract()
                .header("Location");
    }
}
