package lab.paymentquality.testing;

import io.restassured.RestAssured;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies that POST /api/test/reset and POST /api/test/seed return 404 (not 401)
 * when app.testing.enabled=false (the default).
 *
 * This proves Wave 2R security decision: the pass-through chain allows the request
 * to reach MVC, and MVC returns absent-handler 404 because TestController is absent.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class TestEndpointsDisabledIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("test_endpoints_disabled_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void postResetWithoutAuthReturns404NotUnauthorized() {
        RestAssured.given().port(port)
                .when().post("/api/test/reset")
                .then()
                .statusCode(404);
    }

    @Test
    void postSeedWithoutAuthReturns404NotUnauthorized() {
        RestAssured.given().port(port)
                .when().post("/api/test/seed")
                .then()
                .statusCode(404);
    }

    @Test
    void postSeedLearningWithoutAuthReturns404NotUnauthorized() {
        RestAssured.given().port(port)
                .when().post("/api/test/seed-learning")
                .then()
                .statusCode(404);
    }

    @Test
    void passedThroughDisabledRequestsCarryCorrelationIdHeader() {
        // CorrelationIdFilter still runs for pass-through requests.
        RestAssured.given().port(port)
                .when().post("/api/test/reset")
                .then()
                .statusCode(404)
                .header("X-Correlation-ID", notNullValue());
    }

    @Test
    void testControllerBeanIsAbsent() {
        // TestController has @ConditionalOnProperty(havingValue="true"), which is false
        // when app.testing.enabled=false (the default).
        assertThatThrownBy(() ->
                applicationContext.getBean("testController")
        ).isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
