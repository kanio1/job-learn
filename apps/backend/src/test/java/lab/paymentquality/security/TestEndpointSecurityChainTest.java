package lab.paymentquality.security;

import io.restassured.RestAssured;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
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

import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies the specificity of the test-endpoint pass-through SecurityFilterChain.
 * The pass-through chain permits only POST /api/test/reset and POST /api/test/seed.
 * All other paths remain protected by the main JWT chain.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class TestEndpointSecurityChainTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("test_security_chain");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void postTestResetWithoutAuthReturns404NotHandlerAbsent() {
        // Pass-through chain allows the request through to MVC.
        // Controller absent (app.testing.enabled=false by default) → MVC returns 404.
        // This proves the chain is NOT rejecting with 401 before MVC can respond.
        RestAssured.given().port(port)
                .when().post("/api/test/reset")
                .then().statusCode(404);
    }

    @Test
    void postTestSeedWithoutAuthReturns404NotHandlerAbsent() {
        RestAssured.given().port(port)
                .when().post("/api/test/seed")
                .then().statusCode(404);
    }

    @Test
    void getTestResetWithoutAuthReturns401ProtectedByMainChain() {
        // GET /api/test/reset is NOT matched by the POST-only pass-through chain.
        // It falls through to the main JWT chain which requires authentication.
        RestAssured.given().port(port)
                .when().get("/api/test/reset")
                .then().statusCode(401);
    }

    @Test
    void unknownApiPathWithoutAuthReturns401ProtectedByMainChain() {
        // The pass-through chain did not globally permit unknown /api/** paths.
        RestAssured.given().port(port)
                .when().post("/api/does-not-exist")
                .then().statusCode(401);
    }

    @Test
    void passedThroughRequestsCarryCorrelationIdHeader() {
        // CorrelationIdFilter runs after the pass-through chain allows the request.
        RestAssured.given().port(port)
                .when().post("/api/test/reset")
                .then()
                .statusCode(404)
                .header("X-Correlation-ID", notNullValue());
    }
}
