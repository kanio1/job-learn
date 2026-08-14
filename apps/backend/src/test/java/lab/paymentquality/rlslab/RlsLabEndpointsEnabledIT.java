package lab.paymentquality.rlslab;

import io.restassured.RestAssured;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rls-lab.enabled=true"})
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class RlsLabEndpointsEnabledIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("rls_lab_endpoints_enabled_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
        registry.add("app.rls-lab.datasource.url", postgres::getJdbcUrl);
        registry.add("app.rls-lab.datasource.username", () -> "rls_lab_app");
        registry.add("app.rls-lab.datasource.password", () -> "rls_lab_app");
        registry.add("app.rls-lab.bypass.url", postgres::getJdbcUrl);
        registry.add("app.rls-lab.bypass.username", () -> "rls_lab_bypass");
        registry.add("app.rls-lab.bypass.password", () -> "rls_lab_bypass");
    }

    @LocalServerPort
    int port;

    @Test
    void itemsReturns200ForPlatform() {
        RestAssured.given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().get("/api/rls-lab/items")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("items.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void compareReturnsLeakContrastForPlatform() {
        RestAssured.given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().get("/api/rls-lab/compare")
                .then()
                .statusCode(200)
                .body("restrictedWithoutTenantGuc", equalTo(0));
    }
}
