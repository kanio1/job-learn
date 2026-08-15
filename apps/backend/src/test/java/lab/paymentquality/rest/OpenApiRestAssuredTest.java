package lab.paymentquality.rest;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
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

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Wave 5 — OpenAPI exposure and path filter (OA-EXP-01…03, OA-FLT, OA-PUB, OA-INT).
 *
 * <p>Labs and {@code /api/test} are enabled so the exclude filter is proven against registered
 * controllers, not against absent beans.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.checkout-lab.enabled=true",
                "app.checkout-lab.oauth-client-secret=test-oauth-secret",
                "app.checkout-lab.hmac-secret=test-hmac-secret",
                "app.checkout-lab.hosted-checkout-base-url=http://localhost:3000",
                "app.mirror-lab.enabled=true",
                "app.rls-lab.enabled=true",
                "app.testing.enabled=true"
        })
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class OpenApiRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("openapi_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void anonymousApiDocsReturns401ProblemJson() {
        given().port(port)
                .when().get("/v3/api-docs")
                .then()
                .statusCode(401)
                .contentType(containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .body("error", equalTo("unauthorized"));
    }

    @Test
    void authenticatedApiDocsReturnsPublicPathsAndExcludesLabs() {
        Map<String, Object> paths = given().port(port)
                .auth().oauth2(TestJwtSupport.platformOperatorToken())
                .when().get("/v3/api-docs")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getMap("paths");

        assertThat(paths).isNotNull();
        assertThat(paths.keySet())
                .anyMatch(path -> path.equals("/api/status") || path.startsWith("/api/status"))
                .anyMatch(path -> path.startsWith("/api/merchants"))
                .anyMatch(path -> path.contains("/payment-orders"))
                .anyMatch(path -> path.contains("/evidence"))
                .anyMatch(path -> path.startsWith("/api/users"))
                .anyMatch(path -> path.startsWith("/api/audit"))
                .anyMatch(path -> path.contains("/tenants/current/settings"));

        assertThat(paths.keySet())
                .noneMatch(path -> path.startsWith("/api/checkout-lab"))
                .noneMatch(path -> path.startsWith("/api/mirror-lab"))
                .noneMatch(path -> path.startsWith("/api/rls-lab"))
                .noneMatch(path -> path.startsWith("/api/test"));
    }

    @Test
    void openApiDocumentDoesNotLeakInternalPackageNames() {
        String body = given().port(port)
                .auth().oauth2(TestJwtSupport.platformOperatorToken())
                .when().get("/v3/api-docs")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertThat(body).doesNotContain(".internal");
    }

    @Test
    void authenticatedSwaggerUiReturns404() {
        given().port(port)
                .auth().oauth2(TestJwtSupport.platformOperatorToken())
                .when().get("/swagger-ui.html")
                .then()
                .statusCode(404);

        given().port(port)
                .auth().oauth2(TestJwtSupport.platformOperatorToken())
                .when().get("/swagger-ui/index.html")
                .then()
                .statusCode(404);
    }
}
