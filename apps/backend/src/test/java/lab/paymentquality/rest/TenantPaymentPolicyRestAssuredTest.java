package lab.paymentquality.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class TenantPaymentPolicyRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("tenant_payment_policy_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("RA-OPS-180 PATCH policy 200 nowy ETag")
    void patchPolicyReturns200AndNewEtagAndPersistsJsonb() {
        assertV36ColumnOnTenantsNotATable();

        String token = TestJwtSupport.platformAdminToken();
        String currentEtag = currentEtag(token);
        Map<String, Object> policy = policy(true, 2500, 42, "AUTOMATIC");

        String newEtag = requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("If-Match", currentEtag)
                .body(settingsBody(policy))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(200)
                .header("ETag", not(equalTo(currentEtag)))
                .header("ETag", org.hamcrest.Matchers.matchesPattern("\"v\\d+\""))
                .body("paymentPolicy.autoCapture", equalTo(true))
                .body("paymentPolicy.maxAutoCaptureMinor", equalTo(2500))
                .body("paymentPolicy.riskThreshold", equalTo(42))
                .body("paymentPolicy.refundPolicy", equalTo("AUTOMATIC"))
                .extract()
                .header("ETag");

        requestWithToken(port, token)
                .when()
                .get("/api/tenants/current/settings")
                .then()
                .statusCode(200)
                .header("ETag", equalTo(newEtag))
                .body("paymentPolicy.autoCapture", equalTo(true))
                .body("paymentPolicy.maxAutoCaptureMinor", equalTo(2500))
                .body("paymentPolicy.riskThreshold", equalTo(42))
                .body("paymentPolicy.refundPolicy", equalTo("AUTOMATIC"));

        assertThat(platformPolicyJson()).contains("\"autoCapture\": true")
                .contains("\"maxAutoCaptureMinor\": 2500")
                .contains("\"riskThreshold\": 42")
                .contains("\"refundPolicy\": \"AUTOMATIC\"");
    }

    @Test
    @DisplayName("RA-OPS-181 stale If-Match 412")
    void staleIfMatchReturns412AndLeavesJsonbUnchanged() {
        String token = TestJwtSupport.platformAdminToken();
        String staleEtag = currentEtag(token);

        requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("If-Match", staleEtag)
                .body(settingsBody(policy(false, 0, 77, "MANUAL")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(200);

        String storedAfterFirst = platformPolicyJson();

        requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("If-Match", staleEtag)
                .body(settingsBody(policy(true, 9, 12, "AUTOMATIC")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(412)
                .contentType("application/problem+json")
                .body("status", equalTo(412));

        assertThat(platformPolicyJson()).isEqualTo(storedAfterFirst);
        assertThat(jsonbInt(storedAfterFirst, "riskThreshold")).isEqualTo(77);
    }

    @Test
    @DisplayName("RA-OPS-182 riskThreshold -1 / 101 400")
    void riskThresholdOutsideInclusiveRangeIs400() {
        String token = TestJwtSupport.platformAdminToken();
        String before = platformPolicyJson();
        String etag = currentEtag(token);

        requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("If-Match", etag)
                .body(settingsBody(policy(false, 0, -1, "MANUAL")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400));

        requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("If-Match", etag)
                .body(settingsBody(policy(false, 0, 101, "MANUAL")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400));

        assertThat(platformPolicyJson()).isEqualTo(before);
    }

    @Test
    @DisplayName("RA-OPS-183 autoCapture true + missing max 400")
    void autoCaptureTrueWithoutMaxIs400() {
        String token = TestJwtSupport.platformAdminToken();
        String before = platformPolicyJson();
        String etag = currentEtag(token);

        Map<String, Object> missingMax = new LinkedHashMap<>();
        missingMax.put("autoCapture", true);
        missingMax.put("riskThreshold", 50);
        missingMax.put("refundPolicy", "MANUAL");

        requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .header("If-Match", etag)
                .body(settingsBody(missingMax))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("status", equalTo(400));

        assertThat(platformPolicyJson()).isEqualTo(before);
    }

    @Test
    @DisplayName("RA-OPS-184 readonly 403")
    void readonlyPatchIs403() {
        String admin = TestJwtSupport.platformAdminToken();
        String etag = currentEtag(admin);
        String before = platformPolicyJson();
        String readonly = TestJwtSupport.tokenWithRolesAndTenantId(
                "readonly.user",
                List.of("READ_ONLY_USER"),
                "PLATFORM_TENANT");

        requestWithToken(port, readonly)
                .contentType(ContentType.JSON)
                .header("If-Match", etag)
                .body(settingsBody(policy(true, 100, 10, "AUTOMATIC")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(403);

        assertThat(platformPolicyJson()).isEqualTo(before);
    }

    @Test
    @DisplayName("RA-OPS-185 tenant isolation")
    void otherTenantCannotReadOrMutateThisTenantPolicy() {
        String platform = TestJwtSupport.platformAdminToken();
        String platformEtag = currentEtag(platform);
        requestWithToken(port, platform)
                .contentType(ContentType.JSON)
                .header("If-Match", platformEtag)
                .body(settingsBody(policy(false, 0, 91, "AUTOMATIC")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(200);

        String platformStored = platformPolicyJson();
        String otherTenant = TestJwtSupport.tokenWithRolesAndTenantId(
                "tenant.alpha.admin",
                List.of("TENANT_ADMIN"),
                "TENANT_ALPHA");

        Integer otherThreshold = requestWithToken(port, otherTenant)
                .when()
                .get("/api/tenants/current/settings")
                .then()
                .statusCode(200)
                .extract()
                .path("paymentPolicy.riskThreshold");
        assertThat(otherThreshold).isNotEqualTo(91);

        String otherEtag = currentEtag(otherTenant);
        requestWithToken(port, otherTenant)
                .contentType(ContentType.JSON)
                .header("If-Match", otherEtag)
                .body(settingsBody(policy(true, 5, 3, "MANUAL")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(200);

        assertThat(platformPolicyJson()).isEqualTo(platformStored);
        assertThat(jsonbInt(platformStored, "riskThreshold")).isEqualTo(91);

        String unknownTenant = TestJwtSupport.tokenWithRolesAndTenantId(
                "ghost.admin",
                List.of("platform:tenant:settings:read", "platform:tenant:settings:update"),
                "UNKNOWN_TENANT");
        requestWithToken(port, unknownTenant)
                .when()
                .get("/api/tenants/current/settings")
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("status", equalTo(403))
                .body("code", equalTo("tenant_access_denied"));
    }

    @Test
    @DisplayName("PATCH policy missing If-Match 428")
    void missingIfMatchReturns428AndLeavesJsonbUnchanged() {
        String token = TestJwtSupport.platformAdminToken();
        String before = platformPolicyJson();

        requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .body(settingsBody(policy(true, 8, 4, "AUTOMATIC")))
                .when()
                .patch("/api/tenants/current/settings")
                .then()
                .statusCode(428)
                .contentType("application/problem+json")
                .body("status", equalTo(428));

        assertThat(platformPolicyJson()).isEqualTo(before);
    }

    private void assertV36ColumnOnTenantsNotATable() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE script = 'V36__tenant_payment_policy.sql'
                """, Integer.class)).isEqualTo(1);

        String dataType = jdbcTemplate.queryForObject("""
                SELECT data_type FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'tenants'
                  AND column_name = 'payment_policy'
                """, String.class);
        assertThat(dataType).isEqualTo("jsonb");

        Integer tables = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'payment_policy'
                """, Integer.class);
        assertThat(tables).isZero();
    }

    private String currentEtag(String token) {
        return requestWithToken(port, token)
                .when()
                .get("/api/tenants/current/settings")
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
    }

    private String platformPolicyJson() {
        return jdbcTemplate.queryForObject("""
                SELECT payment_policy::text FROM tenants
                WHERE tenant_reference = 'PLATFORM_TENANT'
                """, String.class);
    }

    private int jsonbInt(String json, String key) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode value = node.get(key);
            if (value == null || !value.isNumber()) {
                throw new AssertionError("missing numeric " + key + " in " + json);
            }
            return value.asInt();
        } catch (java.io.IOException ex) {
            throw new AssertionError("unreadable payment_policy JSON: " + json, ex);
        }
    }

    private static Map<String, Object> settingsBody(Map<String, Object> paymentPolicy) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timezone", "UTC");
        body.put("paymentPolicy", paymentPolicy);
        return body;
    }

    private static Map<String, Object> policy(
            boolean autoCapture, Integer maxAutoCaptureMinor, int riskThreshold, String refundPolicy) {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("autoCapture", autoCapture);
        if (maxAutoCaptureMinor != null) {
            policy.put("maxAutoCaptureMinor", maxAutoCaptureMinor);
        }
        policy.put("riskThreshold", riskThreshold);
        policy.put("refundPolicy", refundPolicy);
        return policy;
    }
}
