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

/**
 * Verifies checkout-lab public paths stay behind JWT when the lab flag is off.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class CheckoutLabSecurityChainDisabledTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_security_chain_disabled");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void postOAuthTokenWithoutAuthReturns401WhenLabDisabled() {
        RestAssured.given().port(port)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body("grant_type=client_credentials&client_id=checkout-lab-merchant&client_secret=change-me")
                .when().post("/api/checkout-lab/oauth/token")
                .then()
                .statusCode(401);
    }
}
