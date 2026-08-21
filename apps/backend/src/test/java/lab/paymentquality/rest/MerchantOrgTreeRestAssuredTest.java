package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
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
@DisplayName("Org tree REST Assured contract (PW-M360-T16)")
class MerchantOrgTreeRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_org_tree_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-M360-070 org-tree tenant isolation and lazy children")
    void raM360_070_orgTreeTenantIsolationAndLazyChildren() {
        String alphaRef = uniqueMerchantReference("TREEA");
        String platformRef = uniqueMerchantReference("TREEP");
        createAsPlatform("TENANT_ALPHA", alphaRef, "Tree Alpha " + alphaRef);
        createAsPlatform("PLATFORM_TENANT", platformRef, "Tree Platform " + platformRef);

        List<Map<String, Object>> platformRoots = requestWithToken(port, TestJwtSupport.platformAdminToken())
                .when()
                .get("/api/org-tree")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .path("nodes");

        assertThat(platformRoots)
                .extracting(node -> node.get("label"))
                .contains("Alpha Tenant", "Platform Tenant");
        assertThat(platformRoots)
                .extracting(node -> node.get("reference"))
                .contains("TENANT_ALPHA", "PLATFORM_TENANT");
        assertThat(platformRoots).allSatisfy(node -> {
            assertThat(node.get("type")).isEqualTo("TENANT");
            assertThat(node.get("lazy")).isEqualTo(true);
            assertThat(node).doesNotContainKey("children");
        });

        String alphaParent = parentId(platformRoots, "TENANT_ALPHA");
        String platformParent = parentId(platformRoots, "PLATFORM_TENANT");

        List<Map<String, Object>> alphaChildren = requestWithToken(port, TestJwtSupport.platformAdminToken())
                .queryParam("parent", alphaParent)
                .when()
                .get("/api/org-tree")
                .then()
                .statusCode(200)
                .extract()
                .path("nodes");

        assertThat(alphaChildren)
                .extracting(node -> node.get("reference"))
                .contains(alphaRef)
                .doesNotContain(platformRef);

        List<Map<String, Object>> tenantRoots = requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .when()
                .get("/api/org-tree")
                .then()
                .statusCode(200)
                .extract()
                .path("nodes");

        assertThat(tenantRoots)
                .extracting(node -> node.get("reference"))
                .containsExactly("TENANT_ALPHA");
        assertThat(tenantRoots)
                .extracting(node -> node.get("label"))
                .containsExactly("Alpha Tenant");

        List<Map<String, Object>> tenantChildren = requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .queryParam("parent", alphaParent)
                .when()
                .get("/api/org-tree")
                .then()
                .statusCode(200)
                .extract()
                .path("nodes");

        assertThat(tenantChildren)
                .extracting(node -> node.get("reference"))
                .contains(alphaRef)
                .doesNotContain(platformRef);

        requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .queryParam("parent", platformParent)
                .when()
                .get("/api/org-tree")
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"));
    }

    @Test
    @DisplayName("unauthenticated org-tree is 401")
    void unauthenticatedOrgTreeIs401() {
        publicRequest(port)
                .when()
                .get("/api/org-tree")
                .then()
                .statusCode(401);
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

    private static String parentId(List<Map<String, Object>> nodes, String tenantReference) {
        return nodes.stream()
                .filter(node -> tenantReference.equals(node.get("reference")))
                .map(node -> (String) node.get("id"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing tenant node " + tenantReference));
    }
}
