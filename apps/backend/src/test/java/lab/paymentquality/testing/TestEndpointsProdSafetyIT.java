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

// Feature: deterministic-seed-and-test-isolation, Property 4: Production safety is total and deterministic.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.testing.enabled=true"})
@ActiveProfiles({"test", "prod"})
@Import(TestJwtConfiguration.class)
@Testcontainers
class TestEndpointsProdSafetyIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("test_endpoints_prod_safety_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void postResetReturns404EvenWhenFlagForcedTrue() {
        // TestController is guarded by @Profile("!prod"). With the prod profile active,
        // the controller bean is absent even when app.testing.enabled=true.
        RestAssured.given().port(port)
                .when().post("/api/test/reset")
                .then()
                .statusCode(404);
    }

    @Test
    void postSeedReturns404EvenWhenFlagForcedTrue() {
        RestAssured.given().port(port)
                .when().post("/api/test/seed")
                .then()
                .statusCode(404);
    }

    @Test
    void postSeedLearningReturns404EvenWhenFlagForcedTrue() {
        RestAssured.given().port(port)
                .when().post("/api/test/seed-learning")
                .then()
                .statusCode(404);
    }

    @Test
    void postEtlFullReturns404EvenWhenFlagForcedTrue() {
        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/full")
                .then()
                .statusCode(404);
    }

    @Test
    void postEtlIncrementalReturns404EvenWhenFlagForcedTrue() {
        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/incremental")
                .then()
                .statusCode(404);
    }

    @Test
    void postEtlRebuildReturns404EvenWhenFlagForcedTrue() {
        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/rebuild")
                .then()
                .statusCode(404);
    }

    @Test
    void testControllerBeanIsAbsentUnderProdProfile() {
        // @Profile("!prod") prevents registration regardless of app.testing.enabled.
        assertThatThrownBy(() ->
                applicationContext.getBean("testController")
        ).isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void seedRunnerBeanIsAbsentUnderProdProfile() {
        // SeedRunner is @Profile("seed & !prod"). With prod active, it is never registered.
        assertThatThrownBy(() ->
                applicationContext.getBean("seedRunner")
        ).isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
