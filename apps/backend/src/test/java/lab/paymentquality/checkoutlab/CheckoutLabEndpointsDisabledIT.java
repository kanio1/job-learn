package lab.paymentquality.checkoutlab;

import io.restassured.RestAssured;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies checkout-lab web beans are absent when app.checkout-lab.enabled=false (the default).
 *
 * <p>Unauthenticated requests still receive 401 from the main JWT chain until CPL-T04 adds a
 * pass-through matcher (same progression as testing endpoints before Wave 2R). With a valid JWT,
 * MVC returns 404 because no handler is registered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class CheckoutLabEndpointsDisabledIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_endpoints_disabled_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void getHealthWhenDisabledReturns404WithAuth() {
        RestAssured.given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
                .when().get("/api/checkout-lab/health")
                .then()
                .statusCode(404);
    }

    @Test
    void disabledRequestsCarryCorrelationIdHeader() {
        RestAssured.given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
                .when().get("/api/checkout-lab/health")
                .then()
                .statusCode(404)
                .header("X-Correlation-ID", notNullValue());
    }

    @Test
    void checkoutLabHealthControllerBeanIsAbsent() {
        assertThatThrownBy(() ->
                applicationContext.getBean("checkoutLabHealthController")
        ).isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
