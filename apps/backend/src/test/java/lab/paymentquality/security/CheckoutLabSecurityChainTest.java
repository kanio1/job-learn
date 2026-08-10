package lab.paymentquality.security;

import io.restassured.RestAssured;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * Verifies checkout-lab public pass-through SecurityFilterChain when
 * app.checkout-lab.enabled=true. Handlers are absent in CPL-T04 — 404 proves
 * JWT is not required; 401 would mean the main resource-server chain blocked the request.
 */
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
class CheckoutLabSecurityChainTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_security_chain");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void postOAuthTokenWithoutAuthReturns200WhenHandlerPresent() {
        RestAssured.given().port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body("grant_type=client_credentials&client_id=checkout-lab-merchant&client_secret=test-oauth-secret")
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(200)
                .body("access_token", startsWith("lab."))
                .body("token_type", equalTo("Bearer"));
    }

    @Test
    void postNotifyWithoutAuthReturns404NotJwtUnauthorized() {
        RestAssured.given().port(port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"eventId\":\"evt-1\"}")
                .when().post("/api/checkout-lab/notify")
                .then()
                .statusCode(404);
    }

    @Test
    void getOAuthTokenWithoutAuthReturns401ProtectedByMainChain() {
        RestAssured.given().port(port)
                .when().get("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(401);
    }

    @Test
    void postCheckoutLabSessionsWithoutAuthReturns401FromLabBearerFilter() {
        RestAssured.given().port(port)
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
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(401);
    }

    @Test
    void passedThroughOAuthTokenRequestCarriesCorrelationIdHeader() {
        RestAssured.given().port(port)
                .header("X-Correlation-ID", "corr-checkout-lab-oauth")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body("grant_type=client_credentials&client_id=checkout-lab-merchant&client_secret=test-oauth-secret")
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", equalTo("corr-checkout-lab-oauth"));
    }
}
