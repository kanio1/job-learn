package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.publicRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Entity search REST Assured contract (PW-M360-T17)")
class EntitySearchRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("entity_search_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RA-M360-071 search tenant isolation; empty q 400")
    void raM360_071_searchTenantIsolationAndEmptyQ400() {
        publicRequest(port)
                .when()
                .get("/api/search")
                .then()
                .statusCode(401);

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .when()
                .get("/api/search")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .queryParam("q", "   ")
                .when()
                .get("/api/search")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        ensureTenantBeta();
        String alphaRef = uniqueMerchantReference("SRCHA");
        String betaRef = uniqueMerchantReference("SRCHB");
        createAsPlatform("TENANT_ALPHA", alphaRef, "Search Alpha " + alphaRef);
        createAsPlatform("TENANT_BETA", betaRef, "Search Beta " + betaRef);

        List<Map<String, Object>> tenantMerchants = requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .queryParam("q", alphaRef)
                .when()
                .get("/api/search")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .path("merchants");

        assertThat(tenantMerchants)
                .extracting(row -> row.get("merchantReference"))
                .containsExactly(alphaRef);

        List<Map<String, Object>> hiddenBeta = requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .queryParam("q", betaRef)
                .when()
                .get("/api/search")
                .then()
                .statusCode(200)
                .extract()
                .path("merchants");

        assertThat(hiddenBeta).isEmpty();
    }

    private void createAsPlatform(String tenantReference, String reference, String displayName) {
        Map<String, Object> body = new LinkedHashMap<>(createMerchantBody(reference, displayName));
        body.put("tenantReference", tenantReference);
        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201);
    }

    private void ensureTenantBeta() {
        jdbcTemplate.update("""
                INSERT INTO tenants (tenant_id, tenant_reference, name, status, tenant_type, created_at)
                VALUES (gen_random_uuid(), 'TENANT_BETA', 'Beta Tenant', 'ACTIVE', 'STANDARD', NOW())
                ON CONFLICT (tenant_reference) DO UPDATE SET status = 'ACTIVE'
                """);
    }
}
