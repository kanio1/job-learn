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

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "app.mirror-lab.enabled=true" })
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MirrorLabRestAssuredTest extends PostgresContainerSupport {

    private static final UUID MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("mirror_lab_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void highValueWithoutStepUpReturns403() {
        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"amountMinor\":10000,\"merchantId\":\"" + MERCHANT_ID + "\"}")
                .when().post("/api/mirror-lab/high-value-refunds")
                .then()
                .statusCode(403)
                .body("error", equalTo("step_up_required"));
    }

    @Test
    void highValueWithStepUpReturns200() {
        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .header("X-Lab-Step-Up", "confirmed")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"amountMinor\":10000,\"merchantId\":\"" + MERCHANT_ID + "\"}")
                .when().post("/api/mirror-lab/high-value-refunds")
                .then()
                .statusCode(200)
                .body("status", equalTo("accepted"));
    }

    @Test
    void disputeEvidenceRejectsUnsupportedTypeAndOversizedFile() {
        String disputeId = given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"merchantId\":\"" + MERCHANT_ID + "\"}")
                .when().post("/api/mirror-lab/disputes")
                .then()
                .statusCode(200)
                .extract()
                .path("disputeId");

        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .multiPart("file", "malware.exe", new byte[] { 1, 2, 3 }, "application/octet-stream")
                .when().post("/api/mirror-lab/disputes/" + disputeId + "/evidence")
                .then()
                .statusCode(415)
                .body("error", equalTo("unsupported_evidence_content_type"));

        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .multiPart("file", "huge.txt", new byte[2 * 1024 * 1024 + 1], "text/plain")
                .when().post("/api/mirror-lab/disputes/" + disputeId + "/evidence")
                .then()
                .statusCode(413)
                .body("error", equalTo("evidence_file_too_large"));

        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .multiPart("file", "note.txt", "ok".getBytes(), "text/plain")
                .when().post("/api/mirror-lab/disputes/" + disputeId + "/evidence")
                .then()
                .statusCode(200)
                .body("status", equalTo("CLOSED"));
    }

    @Test
    void makerCannotSelfApproveButOtherRoleCan() {
        String makerToken = TestJwtSupport.merchantPaymentLifecycleToken(MERCHANT_ID.toString());
        String checkerToken = TestJwtSupport.platformAdminToken();

        String approvalId = given().port(port)
                .header("Authorization", "Bearer " + makerToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"amountMinor\":500,\"merchantId\":\"" + MERCHANT_ID + "\"}")
                .when().post("/api/mirror-lab/refund-approvals")
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING_APPROVAL"))
                .extract()
                .path("approvalId");

        given().port(port)
                .header("Authorization", "Bearer " + makerToken)
                .when().post("/api/mirror-lab/refund-approvals/" + approvalId + "/approve")
                .then()
                .statusCode(403)
                .body("error", equalTo("self_approve_forbidden"));

        given().port(port)
                .header("Authorization", "Bearer " + checkerToken)
                .when().post("/api/mirror-lab/refund-approvals/" + approvalId + "/approve")
                .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        given().port(port)
                .header("Authorization", "Bearer " + checkerToken)
                .when().post("/api/mirror-lab/refund-approvals/" + approvalId + "/approve")
                .then()
                .statusCode(409)
                .body("error", equalTo("approval_not_pending"));
    }

    @Test
    void highValueApprovalIncludesStepUpUntil() {
        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"amountMinor\":10000,\"merchantId\":\"" + MERCHANT_ID + "\"}")
                .when().post("/api/mirror-lab/refund-approvals")
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING_APPROVAL"))
                .body("stepUpUntil", org.hamcrest.Matchers.notNullValue());
    }

    @Test
    void consentGrantThenTppThenRevoke() {
        var granted = given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().post("/api/mirror-lab/consents")
                .then()
                .statusCode(200)
                .extract();
        String accessToken = granted.path("accessToken");
        String consentId = granted.path("consentId");

        given().port(port)
                .when().get("/api/mirror-lab/tpp/accounts?token=" + accessToken)
                .then()
                .statusCode(200);

        given().port(port)
                .header("X-Lab-Consent-Token", accessToken)
                .when().get("/api/mirror-lab/tpp/accounts")
                .then()
                .statusCode(200);

        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.merchantPaymentLifecycleToken(MERCHANT_ID.toString()))
                .when().post("/api/mirror-lab/consents/" + consentId + "/revoke")
                .then()
                .statusCode(403)
                .body("error", equalTo("consent_owner_mismatch"));

        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().post("/api/mirror-lab/consents/" + consentId + "/revoke")
                .then()
                .statusCode(200);

        given().port(port)
                .when().get("/api/mirror-lab/tpp/accounts?token=" + accessToken)
                .then()
                .statusCode(403)
                .body("error", equalTo("consent_denied"));
    }
}
