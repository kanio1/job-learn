package lab.paymentquality.mirrorlab;

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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.mirror-lab.enabled=true"
        })
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MirrorLabEndpointsEnabledIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("mirror_lab_endpoints_enabled_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void statementsCsvReturns200WithDisposition() {
        RestAssured.given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().get("/api/mirror-lab/statements?format=csv")
                .then()
                .statusCode(200)
                .header("Content-Disposition", notNullValue())
                .header("X-Correlation-ID", notNullValue())
                .body(org.hamcrest.Matchers.containsString("amount"));
    }

    @Test
    void statementsPdfStartsWithPdfMagic() {
        byte[] body = RestAssured.given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().get("/api/mirror-lab/statements?format=pdf")
                .then()
                .statusCode(200)
                .header("Content-Disposition", notNullValue())
                .extract()
                .asByteArray();
        org.assertj.core.api.Assertions.assertThat(new String(body, 0, 8)).startsWith("%PDF-1.4");
    }
}
