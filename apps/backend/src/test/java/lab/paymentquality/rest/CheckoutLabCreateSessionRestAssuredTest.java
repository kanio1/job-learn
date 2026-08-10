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
class CheckoutLabCreateSessionRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_create_session_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void postSessionWithLabBearerReturns302LocationAndPersistsRows() {
        String labToken = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String correlationId = "corr-t06-create-session";

        String location = given()
                .port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + labToken)
                .header("X-Correlation-ID", correlationId)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-123",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return?sid={sessionId}",
                          "notifyUrl": "http://localhost:8080/api/checkout-lab/notify",
                          "validitySeconds": 900
                        }
                        """)
                .when()
                .post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302)
                .header("X-Correlation-ID", equalTo(correlationId))
                .header("Location", startsWith("http://localhost:3000/psp/checkout/"))
                .body("status", equalTo("CREATED"))
                .body("redirectUri", startsWith("http://localhost:3000/psp/checkout/"))
                .extract()
                .header("Location");

        assertThat(location).isNotBlank();
        String sessionId = location.substring(location.lastIndexOf('/') + 1);

        Integer sessionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_session WHERE session_id = ?::uuid AND ext_order_id = ?",
                Integer.class,
                sessionId,
                "BOOK-123");
        Integer fulfillmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_fulfillment WHERE session_id = ?::uuid AND status = 'AWAITING_PAYMENT'",
                Integer.class,
                sessionId);

        assertThat(sessionCount).isEqualTo(1);
        assertThat(fulfillmentCount).isEqualTo(1);

        String redirectUri = jdbcTemplate.queryForObject(
                "SELECT redirect_uri FROM checkout_session WHERE session_id = ?::uuid",
                String.class,
                sessionId);
        assertThat(redirectUri).isEqualTo(location);
    }

    @Test
    void postSessionWithoutLabBearerReturns401() {
        given()
                .port(port)
                .redirects().follow(false)
                .header("X-Correlation-ID", "corr-t06-unauthorized")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-401",
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
                .statusCode(401);
    }

    @Test
    void postSessionWithInvalidCurrencyReturnsProblemDetails() {
        String labToken = CheckoutLabTestSupport.obtainLabAccessToken(port);

        given()
                .port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + labToken)
                .header("X-Correlation-ID", "corr-t06-invalid-currency")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-XYZ",
                          "amountMinor": 1999,
                          "currency": "GBP",
                          "continueUrl": "http://localhost:3000/checkout-lab/return",
                          "notifyUrl": "http://localhost:8080/api/checkout-lab/notify",
                          "validitySeconds": 900
                        }
                        """)
                .when()
                .post("/api/checkout-lab/sessions")
                .then()
                .statusCode(400)
                .header("X-Correlation-ID", equalTo("corr-t06-invalid-currency"))
                .contentType("application/problem+json")
                .body("error", equalTo("validation"));
    }
}
