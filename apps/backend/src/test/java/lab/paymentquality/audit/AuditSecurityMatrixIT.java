package lab.paymentquality.audit;

import io.restassured.response.Response;
import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.audit.internal.infrastructure.JpaAuditEventRepository;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static lab.paymentquality.testsupport.TestJwtSupport.tokenWithRolesAndTenantId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class AuditSecurityMatrixIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("audit_security_matrix_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JpaAuditEventRepository repository;

    AuditEvent alphaEvent;
    AuditEvent betaEvent;

    @BeforeEach
    void seedAuditRows() {
        repository.deleteAll();
        alphaEvent = save("TENANT_ALPHA", "MERCHANT_CREATED", "merchant-alpha", "2026-06-19T08:00:00Z");
        betaEvent = save("PLACEHOLDER_TENANT_ID", "USER_UPDATED", "user-beta", "2026-06-19T09:00:00Z");
    }

    @Test
    void platformAdminSeesAllTenantsAndReadsAnyDetailWithoutMutation() {
        long before = repository.count();

        request(platformAdminToken()).get("/api/audit").then()
                .statusCode(200)
                .header("Vary", containsString("Authorization"))
                .header("X-Correlation-ID", notNullValue())
                .body("content", hasSize(2))
                .body("content[0].id", equalTo(betaEvent.getId().toString()));

        request(platformAdminToken()).get("/api/audit/{id}", betaEvent.getId()).then()
                .statusCode(200)
                .body("id", equalTo(betaEvent.getId().toString()))
                .body("actorSubject", equalTo(null));

        assertThat(repository.count()).isEqualTo(before);
    }

    @Test
    void supportAgentSeesAllTenantsAndReadsAnyDetail() {
        request(supportAgentToken()).get("/api/audit").then()
                .statusCode(200)
                .body("content", hasSize(2));

        request(supportAgentToken()).get("/api/audit/{id}", alphaEvent.getId()).then()
                .statusCode(200)
                .body("id", equalTo(alphaEvent.getId().toString()));
    }

    @Test
    void tenantAdminSeesOnlyOwnTenantAndCrossTenantMatchesNonexistentMask() {
        String token = tenantAdminToken();
        String correlationId = "audit-masked-correlation";

        request(token).get("/api/audit").then()
                .statusCode(200)
                .header("Vary", containsString("Authorization"))
                .body("content", hasSize(1))
                .body("content[0].tenantId", equalTo("TENANT_ALPHA"));

        request(token).get("/api/audit/{id}", alphaEvent.getId()).then()
                .statusCode(200)
                .body("id", equalTo(alphaEvent.getId().toString()));

        Response crossTenant = request(token, correlationId)
                .get("/api/audit/{id}", betaEvent.getId());
        Response nonexistent = request(token, correlationId)
                .get("/api/audit/{id}", UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));

        assertMaskedNotFound(crossTenant);
        assertMaskedNotFound(nonexistent);
        assertThat(comparableProblem(crossTenant)).isEqualTo(comparableProblem(nonexistent));
        assertThat(crossTenant.asString()).doesNotContain("PLACEHOLDER_TENANT_ID");
    }

    @Test
    void merchantManagerCannotReadListOrDetail() {
        assertForbidden(merchantManagerToken());
    }

    @Test
    void readOnlyUserCannotReadListOrDetail() {
        assertForbidden(readOnlyUserToken());
    }

    private void assertForbidden(String token) {
        request(token).get("/api/audit").then()
                .statusCode(403)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body("detail", not(containsString("audit:read")));
        request(token).get("/api/audit/{id}", alphaEvent.getId()).then()
                .statusCode(403)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body("detail", not(containsString("audit:read")));
    }

    private static void assertMaskedNotFound(Response response) {
        response.then()
                .statusCode(404)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .header("Vary", containsString("Authorization"))
                .header("X-Correlation-ID", equalTo("audit-masked-correlation"))
                .body("error", equalTo("not_found"));
    }

    private static Map<String, Object> comparableProblem(Response response) {
        Map<String, Object> body = new LinkedHashMap<>(response.jsonPath().getMap("$"));
        body.remove("instance");
        return body;
    }

    private io.restassured.specification.RequestSpecification request(String token) {
        return request(token, null);
    }

    private io.restassured.specification.RequestSpecification request(String token, String correlationId) {
        var request = given().port(port).auth().oauth2(token).accept(MediaType.APPLICATION_JSON_VALUE);
        return correlationId == null ? request : request.header("X-Correlation-ID", correlationId);
    }

    private AuditEvent save(String tenant, String action, String targetId, String occurredAt) {
        return repository.saveAndFlush(AuditEvent.fromEvent(new AuditableActionOccurred(
                Instant.parse(occurredAt), "matrix-actor", "Matrix Operator", action,
                "AUDIT_TARGET", targetId, tenant, "correlation-" + targetId, Outcome.SUCCESS)));
    }

    private static String platformAdminToken() {
        return tokenWithRolesAndTenantId(
                "platform.admin", List.of("platform:audit:read"), "PLATFORM_TENANT");
    }

    private static String supportAgentToken() {
        return tokenWithRolesAndTenantId(
                "support.agent", List.of("platform:audit:read"), "PLATFORM_TENANT");
    }

    private static String tenantAdminToken() {
        return tokenWithRolesAndTenantId(
                "tenant.admin", List.of("tenant:audit:read"), "TENANT_ALPHA");
    }

    private static String merchantManagerToken() {
        return tokenWithRolesAndTenantId(
                "merchant.manager", List.of("merchants:read"), "TENANT_ALPHA");
    }

    private static String readOnlyUserToken() {
        return tokenWithRolesAndTenantId(
                "read.only", List.of("tenant:users:read"), "TENANT_ALPHA");
    }
}
