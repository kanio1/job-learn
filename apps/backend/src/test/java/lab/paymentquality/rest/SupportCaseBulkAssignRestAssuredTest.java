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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class SupportCaseBulkAssignRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("support_bulk_assign_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-OPS-150 4 ids: 1 RESOLVED + 1 foreign tenant + 2 OK → HTTP 200 succeeded=2 failed=2")
    void mixedBulkAssignReturns200() {
        String alphaMerchant = createMerchantOnAlpha("RA150A");
        String okOne = createCase(alphaMerchant, "OK one");
        String okTwo = createCase(alphaMerchant, "OK two");
        String resolvedId = createCase(alphaMerchant, "Resolved");
        resolve(resolvedId);

        String placeholderMerchant = createMerchantOnPlaceholder("RA150P");
        String foreignId = createCase(placeholderMerchant, "Foreign");

        String tenantOperator = TestJwtSupport.tokenWithRolesAndTenantId(
                "tenant.support",
                List.of("platform:support:read", "platform:support:operate"),
                "TENANT_ALPHA");

        requestWithToken(port, tenantOperator)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "caseIds", List.of(okOne, resolvedId, foreignId, okTwo),
                        "assigneeSubject", "support.agent"))
                .when()
                .post("/api/support/cases/bulk-assign")
                .then()
                .statusCode(200)
                .body("succeeded", equalTo(2))
                .body("failed", hasSize(2));
    }

    @Test
    @DisplayName("RA-OPS-151 empty list 400")
    void emptyBulkAssignIs400() {
        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(Map.of("caseIds", List.of(), "assigneeSubject", "support.agent"))
                .when()
                .post("/api/support/cases/bulk-assign")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("RA-OPS-152 >100 ids 400")
    void tooManyIdsIs400() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            ids.add(UUID.randomUUID().toString());
        }
        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(Map.of("caseIds", ids, "assigneeSubject", "support.agent"))
                .when()
                .post("/api/support/cases/bulk-assign")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("RA-OPS-153 readonly 403")
    void readonlyBulkAssignIs403() {
        String readonly = TestJwtSupport.tokenWithRolesAndTenantId(
                "readonly.user",
                List.of("platform:support:read"),
                "PLATFORM_TENANT");
        requestWithToken(port, readonly)
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "caseIds", List.of(UUID.randomUUID().toString()),
                        "assigneeSubject", "support.agent"))
                .when()
                .post("/api/support/cases/bulk-assign")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("RA-OPS-154 no-op same assignee counts succeeded")
    void sameAssigneeNoOpCountsSucceeded() {
        String merchantId = createMerchantOnAlpha("RA154");
        String caseId = createCase(merchantId, "No-op assign");
        String etag = requestWithToken(port, TestJwtSupport.supportOperateToken())
                .when()
                .get("/api/support/cases/{id}", caseId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", etag)
                .body(Map.of("assigneeSubject", "support.agent"))
                .when()
                .patch("/api/support/cases/{id}", caseId)
                .then()
                .statusCode(200);

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(Map.of("caseIds", List.of(caseId), "assigneeSubject", "support.agent"))
                .when()
                .post("/api/support/cases/bulk-assign")
                .then()
                .statusCode(200)
                .body("succeeded", equalTo(1))
                .body("failed", hasSize(0));
    }

    private void resolve(String caseId) {
        String etag = requestWithToken(port, TestJwtSupport.supportOperateToken())
                .when()
                .get("/api/support/cases/{id}", caseId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
        etag = move(caseId, etag, "IN_PROGRESS");
        etag = move(caseId, etag, "WAITING");
        move(caseId, etag, "RESOLVED");
    }

    private String move(String caseId, String etag, String status) {
        return requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", etag)
                .body(Map.of("status", status))
                .when()
                .patch("/api/support/cases/{id}", caseId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
    }

    private String createCase(String merchantId, String title) {
        return requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(Map.of("merchantId", merchantId, "title", title, "priority", "NORMAL"))
                .when()
                .post("/api/support/cases")
                .then()
                .statusCode(201)
                .extract()
                .path("caseId");
    }

    private String createMerchantOnAlpha(String label) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference(label), "Bulk " + label))
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");
    }

    private String createMerchantOnPlaceholder(String label) {
        Map<String, Object> body = new LinkedHashMap<>(createMerchantBody(
                uniqueMerchantReference(label), "Placeholder " + label));
        body.put("tenantReference", "PLACEHOLDER_TENANT_ID");
        return requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/merchants")
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");
    }
}
