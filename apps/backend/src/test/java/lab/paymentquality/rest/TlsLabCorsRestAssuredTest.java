package lab.paymentquality.rest;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
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
import static org.hamcrest.Matchers.equalTo;

/**
 * RA-RFC-033: HTTPS dashboard origins sit beside {@code http://localhost:3000},
 * not instead of it. Spring Security rejects a foreign Origin on preflight with 403
 * (Invalid CORS request) rather than 200 without ACAO.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("RA-RFC-033 additive CORS origins")
class TlsLabCorsRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("tls_lab_cors");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void preflightAllowsHttpLocalhost3000() {
        assertAllowOrigin("http://localhost:3000");
    }

    @Test
    void preflightAllowsHttpsAppWith8443() {
        assertAllowOrigin("https://app.payment-quality.local:8443");
    }

    @Test
    void preflightAllowsHttpsAppWithoutPort() {
        assertAllowOrigin("https://app.payment-quality.local");
    }

    @Test
    void preflightRejectsForeignOrigin() {
        given().port(port)
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
                .when().options("/api/merchants")
                .then()
                .statusCode(403);
    }

    private void assertAllowOrigin(String origin) {
        given().port(port)
                .header("Origin", origin)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization")
                .when().options("/api/merchants")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo(origin));
    }
}
