package lab.paymentquality.shared.security;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.Tag;
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

import static io.restassured.RestAssured.given;

/**
 * End-to-end security test verifying that a token with a wrong {@code azp} claim
 * is rejected with HTTP 401 on a protected endpoint.
 *
 * <p>Mirrors the setup of {@link lab.paymentquality.security.MerchantSecurityTest}.
 *
 * <p><b>Validates: Requirements 5.2, 5.3</b>
 */
@Tag("Feature: backend-authority-refactor")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class AuthorizedPartyValidatorSecurityTest extends PostgresContainerSupport {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer postgres = newPostgresContainer("azp_security_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    /**
     * A request with a wrong-azp token on a protected endpoint must return 401.
     * This is identical to how invalid-issuer and expired tokens are rejected.
     *
     * <p><b>Validates: Requirements 5.2, 5.3</b>
     */
    @Test
    void wrongAzpToken_onProtectedEndpoint_returns401() {
        given()
                .port(port)
                .auth().oauth2(TestJwtSupport.tokenWithWrongAuthorizedParty())
        .when()
                .get("/api/merchants")
        .then()
                .statusCode(401);
    }
}
