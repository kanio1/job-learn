package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
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
import java.util.UUID;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class SupportCaseRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("support_case_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-OPS-110 POST 201 Location + ETag v0 status NEW")
    void postCreatesCaseWithLocationAndEtagV0() {
        String merchantId = createMerchantOnAlpha("RA110");

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(createCaseBody(merchantId, "Card chargeback intake"))
                .when()
                .post("/api/support/cases")
                .then()
                .statusCode(201)
                .header("Location", startsWith("/api/support/cases/"))
                .header("ETag", equalTo("\"v0\""))
                .body("caseId", notNullValue())
                .body("caseReference", startsWith("INC-"))
                .body("status", equalTo("NEW"))
                .body("merchantId", equalTo(merchantId))
                .body("title", equalTo("Card chargeback intake"))
                .body("priority", equalTo("NORMAL"));
    }

    @Test
    @DisplayName("RA-OPS-111 PATCH NEW→IN_PROGRESS If-Match 200")
    void patchNewToInProgressWithIfMatchIs200() {
        CreatedCase created = createCase("RA111");

        String nextEtag = requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", created.etag())
                .body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"))
                .header("ETag", not(equalTo(created.etag())))
                .extract()
                .header("ETag");
        assertThat(nextEtag).isEqualTo("\"v1\"");
    }

    @Test
    @DisplayName("RA-OPS-112 PATCH NEW→RESOLVED 409 illegal_transition")
    void patchNewToResolvedIs409() {
        CreatedCase created = createCase("RA112");

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", created.etag())
                .body(Map.of("status", "RESOLVED"))
                .when()
                .patch("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(409)
                .body("error", equalTo("illegal_transition"));

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .when()
                .get("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(200)
                .body("status", equalTo("NEW"));
    }

    @Test
    @DisplayName("RA-OPS-113 PATCH stale If-Match 412")
    void staleIfMatchPatchIs412() {
        CreatedCase created = createCase("RA113");

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", created.etag())
                .body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(200);

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", created.etag())
                .body(Map.of("status", "WAITING"))
                .when()
                .patch("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(412)
                .body("error", equalTo("case_version_mismatch"));

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .when()
                .get("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @DisplayName("RA-OPS-114 PATCH without If-Match 428")
    void patchWithoutIfMatchIs428() {
        CreatedCase created = createCase("RA114");

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(428)
                .body("error", equalTo("precondition_required"));

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .when()
                .get("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(200)
                .body("status", equalTo("NEW"));
    }

    @Test
    @DisplayName("RA-OPS-115 tenant isolation 404")
    void foreignTenantGetIs404() {
        String merchantId = createMerchantOnPlaceholder("RA115");
        String caseId = requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(createCaseBody(merchantId, "Foreign tenant case"))
                .when()
                .post("/api/support/cases")
                .then()
                .statusCode(201)
                .extract()
                .path("caseId");

        String tenantReader = TestJwtSupport.tokenWithRolesAndTenantId(
                "tenant.admin",
                List.of("platform:support:read"),
                "TENANT_ALPHA");
        requestWithToken(port, tenantReader)
                .when()
                .get("/api/support/cases/{id}", caseId)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    @DisplayName("RA-OPS-116 readonly POST 403")
    void readonlyPostIs403() {
        String merchantId = createMerchantOnAlpha("RA116");
        String readonly = TestJwtSupport.tokenWithRolesAndTenantId(
                "readonly.user",
                List.of("platform:support:read"),
                "PLATFORM_TENANT");

        requestWithToken(port, readonly)
                .contentType(ContentType.JSON)
                .body(createCaseBody(merchantId, "Readonly must not create"))
                .when()
                .post("/api/support/cases")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("RA-OPS-117 manager POST 403")
    void merchantManagerPostIs403() {
        String merchantId = createMerchantOnAlpha("RA117");
        String manager = TestJwtSupport.tokenWithRolesAndTenantId(
                "merchant.manager",
                List.of("merchant:payments:create", "merchant:payments:read", "merchant:payments:lifecycle"),
                "TENANT_ALPHA");

        requestWithToken(port, manager)
                .contentType(ContentType.JSON)
                .body(createCaseBody(merchantId, "Manager must not create"))
                .when()
                .post("/api/support/cases")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("RA-OPS-118 duplicate case_reference 409")
    void duplicateCaseReferenceIs409() {
        String merchantId = createMerchantOnAlpha("RA118");
        String reference = "INC-DUP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, Object> body = createCaseBody(merchantId, "First duplicate");
        body.put("caseReference", reference);

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/support/cases")
                .then()
                .statusCode(201)
                .body("caseReference", equalTo(reference));

        Map<String, Object> duplicate = createCaseBody(merchantId, "Second duplicate");
        duplicate.put("caseReference", reference);
        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(duplicate)
                .when()
                .post("/api/support/cases")
                .then()
                .statusCode(409)
                .body("error", equalTo("duplicate_case_reference"));
    }

    @Test
    @DisplayName("RA-OPS-119 WAITING→IN_PROGRESS 200")
    void waitingToInProgressIs200() {
        CreatedCase created = createCase("RA119");
        String inProgress = move(created.caseId(), created.etag(), "IN_PROGRESS");
        String waiting = move(created.caseId(), inProgress, "WAITING");

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", waiting)
                .body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"));
    }

    @Test
    @DisplayName("RA-OPS-120 GET list filter status IN_PROGRESS")
    void listFilterStatusInProgress() {
        CreatedCase created = createCase("RA120");
        move(created.caseId(), created.etag(), "IN_PROGRESS");
        createCase("RA120B");

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .when()
                .get("/api/support/cases?status=IN_PROGRESS")
                .then()
                .statusCode(200)
                .body("content.caseId", org.hamcrest.Matchers.hasItem(created.caseId()))
                .body("content.status", org.hamcrest.Matchers.everyItem(equalTo("IN_PROGRESS")));
    }

    @Test
    @DisplayName("RA-OPS-121 RESOLVED→IN_PROGRESS 409")
    void resolvedToInProgressIs409() {
        CreatedCase created = createCase("RA121");
        String inProgress = move(created.caseId(), created.etag(), "IN_PROGRESS");
        String waiting = move(created.caseId(), inProgress, "WAITING");
        String resolved = move(created.caseId(), waiting, "RESOLVED");

        requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .header("If-Match", resolved)
                .body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/api/support/cases/{id}", created.caseId())
                .then()
                .statusCode(409)
                .body("error", equalTo("illegal_transition"));
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
                .body("status", equalTo(status))
                .extract()
                .header("ETag");
    }

    private CreatedCase createCase(String label) {
        String merchantId = createMerchantOnAlpha(label);
        Response response = requestWithToken(port, TestJwtSupport.supportOperateToken())
                .contentType(ContentType.JSON)
                .body(createCaseBody(merchantId, "Case " + label))
                .when()
                .post("/api/support/cases");
        response.then().statusCode(201);
        return new CreatedCase(response.path("caseId"), response.getHeader("ETag"), merchantId);
    }

    private String createMerchantOnAlpha(String label) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference(label), "Support " + label))
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

    private static Map<String, Object> createCaseBody(String merchantId, String title) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("merchantId", merchantId);
        body.put("title", title);
        body.put("priority", "NORMAL");
        return body;
    }

    private record CreatedCase(String caseId, String etag, String merchantId) {
    }
}
