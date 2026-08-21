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
import org.springframework.http.MediaType;
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
import java.util.UUID;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorIfMatch;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.publicRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("Merchant list REST Assured contract (PW-M360-T02)")
class MerchantListRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_list_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RA-M360-010 page 0 size 20 returns content and totalElements")
    void raM360_010_pageZeroSizeTwentyReturnsContentAndTotalElements() {
        String batch = uniqueBatch();
        for (int i = 0; i < 21; i++) {
            createAsOperator("MERCH-PG-" + batch + "-" + i, "Page Merchant " + i);
        }

        List<Map<String, Object>> content = operatorRequest(port)
                .queryParam("q", batch)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", equalTo(21))
                .body("totalPages", equalTo(2))
                .extract()
                .path("content");

        assertThat(content).hasSizeLessThanOrEqualTo(20);
        assertThat(content).hasSize(20);
    }

    @Test
    @DisplayName("RA-M360-011 status=ACTIVE returns only ACTIVE merchants")
    void raM360_011_statusActiveReturnsOnlyActive() {
        String batch = uniqueBatch();
        String draftRef = "MERCH-ST-" + batch + "-DRAFT";
        String activeRef = "MERCH-ST-" + batch + "-ACTIVE";
        createAsOperator(draftRef, "Draft " + batch);
        String activeId = createAsOperator(activeRef, "Active " + batch);
        String etag = operatorRequest(port).when().get("/api/merchants/{id}", activeId)
                .then().statusCode(200).extract().header("ETag");
        operatorIfMatch(port, etag).when().post("/api/merchants/{id}/activate", activeId).then().statusCode(200);

        List<Map<String, Object>> content = operatorRequest(port)
                .queryParam("q", batch)
                .queryParam("status", "ACTIVE")
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(200)
                .extract()
                .path("content");

        assertThat(content).isNotEmpty();
        assertThat(content).extracting(row -> row.get("status")).containsOnly("ACTIVE");
        assertThat(content).extracting(row -> row.get("merchantReference")).contains(activeRef).doesNotContain(draftRef);
    }

    @Test
    @DisplayName("RA-M360-012 q unique reference returns one row")
    void raM360_012_qUniqueReferenceReturnsOneRow() {
        String reference = uniqueMerchantReference("QONLY");
        createAsOperator(reference, "Queryable Merchant");
        createAsOperator(uniqueMerchantReference("QOTHER"), "Other Merchant");

        List<Map<String, Object>> content = operatorRequest(port)
                .queryParam("q", reference)
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .extract()
                .path("content");

        assertThat(content).hasSize(1);
        assertThat(content.getFirst().get("merchantReference")).isEqualTo(reference);
    }

    @Test
    @DisplayName("RA-M360-013 sort=displayName,asc is monotonic")
    void raM360_013_sortDisplayNameAscIsMonotonic() {
        String batch = uniqueBatch();
        createAsOperator("MERCH-DN-" + batch + "-Z", "Zzz " + batch);
        createAsOperator("MERCH-DN-" + batch + "-A", "Aaa " + batch);
        createAsOperator("MERCH-DN-" + batch + "-M", "Mmm " + batch);

        List<Map<String, Object>> content = operatorRequest(port)
                .queryParam("q", batch)
                .queryParam("sort", "displayName,asc")
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(200)
                .extract()
                .path("content");

        List<String> names = content.stream()
                .map(row -> (String) row.get("displayName"))
                .toList();
        assertThat(names).containsExactly("Aaa " + batch, "Mmm " + batch, "Zzz " + batch);
    }

    @Test
    @DisplayName("RA-M360-014 size=101 returns 400 problem+json")
    void raM360_014_size101Returns400ProblemJson() {
        operatorRequest(port)
                .queryParam("size", 101)
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("RA-M360-015 sort=revenue,desc returns 400 problem+json")
    void raM360_015_illegalSortReturns400ProblemJson() {
        operatorRequest(port)
                .queryParam("sort", "revenue,desc")
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("invalid status returns 400 problem+json")
    void invalidStatusReturns400ProblemJson() {
        operatorRequest(port)
                .queryParam("status", "NOPE")
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("RA-M360-016 tenant.admin does not see Beta merchants")
    void raM360_016_tenantAdminDoesNotSeeBeta() {
        ensureTenantBeta();
        String batch = uniqueBatch();
        String alphaRef = "MERCH-ISO-" + batch + "-A";
        String betaRef = "MERCH-ISO-" + batch + "-B";
        createAsOperator(alphaRef, "Alpha Isolated " + batch);
        createAsPlatform("TENANT_BETA", betaRef, "Beta Isolated " + batch);

        List<Map<String, Object>> content = requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .queryParam("q", batch)
                .queryParam("tenantId", "TENANT_BETA")
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(200)
                .extract()
                .path("content");

        assertThat(content).extracting(row -> row.get("merchantReference")).contains(alphaRef).doesNotContain(betaRef);
    }

    @Test
    @DisplayName("RA-M360-017 platform tenantId=TENANT_ALPHA returns only Alpha")
    void raM360_017_platformTenantIdFilterReturnsOnlyAlpha() {
        ensureTenantBeta();
        String batch = uniqueBatch();
        String alphaRef = "MERCH-PLAT-" + batch + "-A";
        String betaRef = "MERCH-PLAT-" + batch + "-B";
        createAsPlatform("TENANT_ALPHA", alphaRef, "Platform Alpha " + batch);
        createAsPlatform("TENANT_BETA", betaRef, "Platform Beta " + batch);

        List<Map<String, Object>> content = requestWithToken(port, TestJwtSupport.platformAdminToken())
                .queryParam("q", batch)
                .queryParam("tenantId", "TENANT_ALPHA")
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(200)
                .extract()
                .path("content");

        assertThat(content).extracting(row -> row.get("merchantReference")).contains(alphaRef).doesNotContain(betaRef);
    }

    @Test
    @DisplayName("RA-M360-018 unauthenticated 401; readonly GET 200")
    void raM360_018_unauthenticated401AndReadonlyGet200() {
        publicRequest(port)
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(401);

        String readOnly = TestJwtSupport.tokenWithRolesAndTenantId(
                "read.only", List.of("merchants:read"), "TENANT_ALPHA");
        requestWithToken(port, readOnly)
                .when()
                .get("/api/merchants")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("content", org.hamcrest.Matchers.notNullValue());
    }

    private String createAsOperator(String reference, String displayName) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(reference, displayName))
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");
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

    private static String uniqueBatch() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
