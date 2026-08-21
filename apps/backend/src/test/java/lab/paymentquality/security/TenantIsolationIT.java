package lab.paymentquality.security;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.ValidatableResponse;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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

import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class TenantIsolationIT extends PostgresContainerSupport {

    private static final String TENANT_ALPHA = "TENANT_ALPHA";
    private static final String PLATFORM_TENANT = "PLATFORM_TENANT";
    private static final String PLACEHOLDER_TENANT = "PLACEHOLDER_TENANT_ID";
    private static final String SUSPENDED_TENANT = "TENANT_SUSPENDED_IT";

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("tenant_isolation_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void tenantScopedReadOwnTenantMerchantReturns200() {
        String merchantId = createMerchantAsTenant(TENANT_ALPHA, "OWN", "Own Tenant Merchant");

        requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .when().get("/api/merchants/{id}", merchantId)
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", not(equalTo("")))
                .body("merchantId", equalTo(merchantId));
    }

    @Test
    void tenantScopedReadOtherTenantMerchantReturnsMasked404ProblemJson() {
        String merchantId = createMerchantAsPlatform(PLACEHOLDER_TENANT, "READ-OTHER", "Other Tenant Merchant");

        requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .when().get("/api/merchants/{id}", merchantId)
                .then()
                .statusCode(404)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header("X-Correlation-ID", not(equalTo("")))
                .body("error", equalTo("not_found"));
    }

    @Test
    void tenantScopedActivateOtherTenantMerchantReturns403WithoutForeignTenantLeak() {
        String merchantId = createMerchantAsPlatform(PLACEHOLDER_TENANT, "WRITE-OTHER", "Other Write Merchant");

        requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .header("If-Match", "\"v0\"")
                .when().post("/api/merchants/{id}/activate", merchantId)
                .then()
                .statusCode(403)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header("X-Correlation-ID", not(equalTo("")))
                .body("error", equalTo("forbidden"))
                .body("details", equalTo("Access denied"))
                .body("details", not(containsString(PLACEHOLDER_TENANT)));
    }

    @Test
    void platformScopedCreateWithoutTenantReferenceReturns400ProblemJson() {
        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("MISSING-TENANT"), "Missing Tenant"))
                .when().post("/api/merchants")
                .then()
                .statusCode(400)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header("X-Correlation-ID", not(equalTo("")))
                .body("error", equalTo("validation"));
    }

    @Test
    void platformScopedCreateWithValidTenantReferencePersistsTargetTenantId() {
        String merchantId = createMerchantAsPlatform(TENANT_ALPHA, "PLATFORM-VALID", "Platform Valid Tenant");

        assertThat(merchantTenantId(merchantId)).isEqualTo(tenantId(TENANT_ALPHA));
    }

    @Test
    void tenantScopedCreateIgnoresBodyTenantReferenceAndPersistsCallerTenantId() {
        String merchantId = createMerchantAsTenant(PLATFORM_TENANT, "TENANT-IGNORE", "Tenant Ignores Body");

        assertThat(merchantTenantId(merchantId)).isEqualTo(tenantId(TENANT_ALPHA));
        assertThat(merchantTenantId(merchantId)).isNotEqualTo(tenantId(PLATFORM_TENANT));
    }

    @Test
    void jwtWithoutTenantClaimReturns403ProblemJson() {
        requestWithToken(port, TestJwtSupport.tokenWithoutTenantClaim())
                .when().get("/api/merchants")
                .then()
                .statusCode(403)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header("X-Correlation-ID", not(equalTo("")))
                .body("error", equalTo("forbidden"));
    }

    @Test
    void suspendedTenantJwtReturns403ProblemJson() {
        ensureSuspendedTenant();
        String token = TestJwtSupport.tokenWithRolesAndTenantId(
                "suspended.tenant",
                List.of("merchants:read"),
                SUSPENDED_TENANT);

        requestWithToken(port, token)
                .when().get("/api/merchants")
                .then()
                .statusCode(403)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header("X-Correlation-ID", not(equalTo("")))
                .body("error", equalTo("forbidden"));
    }

    @Test
    void platformScopedListWithUnknownTenantFilterReturnsEmptyList() {
        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .queryParam("tenantId", "DOES_NOT_EXIST")
                .when().get("/api/merchants")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", not(equalTo("")))
                .body("content", empty());
    }

    private String createMerchantAsTenant(String bodyTenantReference, String label, String displayName) {
        return createMerchant(TestJwtSupport.tenantAdminToken(), bodyTenantReference, label, displayName)
                .statusCode(201)
                .header("X-Correlation-ID", not(equalTo("")))
                .extract().path("merchantId");
    }

    private String createMerchantAsPlatform(String targetTenantReference, String label, String displayName) {
        return createMerchant(TestJwtSupport.platformAdminToken(), targetTenantReference, label, displayName)
                .statusCode(201)
                .header("X-Correlation-ID", not(equalTo("")))
                .extract().path("merchantId");
    }

    private ValidatableResponse createMerchant(
            String token,
            String tenantReference,
            String label,
            String displayName) {
        return requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference(label), displayName, tenantReference))
                .when().post("/api/merchants")
                .then();
    }

    private Map<String, Object> createMerchantBody(String reference, String displayName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchantReference", reference);
        body.put("displayName", displayName);
        return body;
    }

    private Map<String, Object> createMerchantBody(String reference, String displayName, String tenantReference) {
        Map<String, Object> body = createMerchantBody(reference, displayName);
        body.put("tenantReference", tenantReference);
        return body;
    }

    private UUID tenantId(String tenantReference) {
        return jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM tenants WHERE tenant_reference = ?",
                UUID.class,
                tenantReference);
    }

    private UUID merchantTenantId(String merchantId) {
        return jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM merchants WHERE merchant_id = ?::uuid",
                UUID.class,
                merchantId);
    }

    private void ensureSuspendedTenant() {
        jdbcTemplate.update("""
                INSERT INTO tenants (tenant_id, tenant_reference, name, status, tenant_type, created_at)
                VALUES (gen_random_uuid(), ?, 'Suspended Tenant IT', 'SUSPENDED', 'STANDARD', NOW())
                ON CONFLICT (tenant_reference) DO UPDATE SET status = 'SUSPENDED'
                """, SUSPENDED_TENANT);
    }
}
