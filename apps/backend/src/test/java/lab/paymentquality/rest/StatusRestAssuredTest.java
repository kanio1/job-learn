package lab.paymentquality.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class StatusRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("status_rest_test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    private int port;

    @Test
    void statusEndpointSupportsFoundationOnlyHttpSmokeCheck() {
        given()
                .port(port)
        .when()
                .get("/api/status")
        .then()
                .statusCode(200)
                .body("application", equalTo("payment-quality-lab"))
                .body("phase", equalTo("foundation"))
                .body("status", equalTo("UP"));
    }
}
