package lab.paymentquality.rest;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtSupport;
import lab.paymentquality.testsupport.TlsLabIssuerJwtTestConfiguration;
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

import static io.restassured.RestAssured.given;

/**
 * RA-RFC-032: resource-server {@code issuer-uri} is an exact string.
 * Does not start Keycloak or Caddy — the decoder uses the test RSA key with the
 * TLS-lab issuer value from {@code application-tls-lab.yml}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TlsLabIssuerJwtTestConfiguration.class)
@Testcontainers
@DisplayName("RA-RFC-032 TLS-lab issuer vs HTTP issuer")
class TlsLabIssuerMismatchRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("tls_lab_issuer");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void httpsTlsLabIssuerIsAccepted() {
        given().port(port)
                .header("Authorization", "Bearer "
                        + TestJwtSupport.platformAdminTokenForIssuer(TestJwtSupport.TLS_LAB_ISSUER))
                .when().get("/api/merchants")
                .then()
                .statusCode(200);
    }

    @Test
    void httpKeycloakIssuerIsRejectedWhenResourceServerExpectsTlsLabIss() {
        given().port(port)
                .header("Authorization", "Bearer "
                        + TestJwtSupport.platformAdminTokenForIssuer(
                        "http://localhost:8081/realms/payment-quality"))
                .when().get("/api/merchants")
                .then()
                .statusCode(401);
    }

    @Test
    void defaultTestIssuerIsRejectedWhenResourceServerExpectsTlsLabIss() {
        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().get("/api/merchants")
                .then()
                .statusCode(401);
    }
}
