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
import java.util.Map;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorIfMatch;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantContactPatchRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_contact_patch_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-OPS-050 PATCH contactPhone If-Match v0 → 200 ETag v1")
    void patchContactPhoneIfMatchIncrementsEtag() {
        Response created = createMerchant("RA050C");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        String nextEtag = operatorIfMatch(port, etag)
                .contentType(ContentType.JSON)
                .body(Map.of("contactPhone", "+48111111111"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .body("contactPhone", equalTo("+48111111111"))
                .header("ETag", not(equalTo(etag)))
                .extract().header("ETag");
        assertThat(nextEtag).isEqualTo("\"v1\"");
    }

    @Test
    @DisplayName("RA-OPS-051 PATCH contact without If-Match → 428")
    void patchContactWithoutIfMatchIs428() {
        Response created = createMerchant("RA051C");
        created.then().statusCode(201);
        String id = created.path("merchantId");

        operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(Map.of("contactPhone", "+48222222222"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(428)
                .body("error", equalTo("precondition_required"));

        operatorRequest(port)
                .when().get("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .body("contactPhone", nullValue());
    }

    @Test
    @DisplayName("RA-OPS-052 stale If-Match PATCH contact → 412 DB unchanged")
    void staleIfMatchPatchContactIs412AndDbUnchanged() {
        Response created = createMerchant("RA052C");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        operatorIfMatch(port, etag)
                .contentType(ContentType.JSON)
                .body(Map.of("contactPhone", "+48111111111"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(200);

        operatorIfMatch(port, etag)
                .contentType(ContentType.JSON)
                .body(Map.of("contactAddress", "Should not persist"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(412)
                .body("error", equalTo("merchant_version_mismatch"));

        operatorRequest(port)
                .when().get("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .body("contactPhone", equalTo("+48111111111"))
                .body("contactAddress", nullValue());
    }

    @Test
    @DisplayName("RA-OPS-053 malformed If-Match PATCH contact → 400")
    void malformedIfMatchPatchContactIs400() {
        Response created = createMerchant("RA053C");
        created.then().statusCode(201);
        String id = created.path("merchantId");

        operatorIfMatch(port, "not-an-etag")
                .contentType(ContentType.JSON)
                .body(Map.of("contactPhone", "+48333333333"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(400)
                .body("error", equalTo("malformed_if_match"));
    }

    @Test
    @DisplayName("RA-OPS-054 tenant admin PATCH foreign merchant → 403/404 not 412")
    void tenantIsolationPatchContact() {
        Map<String, Object> body = new LinkedHashMap<>(createMerchantBody(
                uniqueMerchantReference("RA054C"), "RA-OPS-054C"));
        body.put("tenantReference", "PLACEHOLDER_TENANT_ID");
        String id = requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/merchants")
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");

        requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .header("If-Match", "\"v0\"")
                .contentType(ContentType.JSON)
                .body(Map.of("contactPhone", "+48444444444"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(403)
                .body("error", equalTo("forbidden"))
                .body("error", not(equalTo("merchant_version_mismatch")));
    }

    @Test
    @DisplayName("RA-OPS-055 readonly PATCH contact → 403")
    void readonlyPatchContactIs403() {
        Response created = createMerchant("RA055C");
        created.then().statusCode(201);
        String id = created.path("merchantId");

        String readonly = TestJwtSupport.tokenWithRoles("readonly.user", java.util.List.of("merchants:read"));
        requestWithToken(port, readonly)
                .header("If-Match", "\"v0\"")
                .contentType(ContentType.JSON)
                .body(Map.of("contactPhone", "+48555555555"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(403);

        operatorRequest(port)
                .when().get("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .body("contactPhone", nullValue());
    }

    @Test
    @DisplayName("PATCH omit contactPhone leaves previous value; null clears")
    void omitLeavesValueNullClears() {
        Response created = createMerchant("RAOMIT");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        String v1 = operatorIfMatch(port, etag)
                .contentType(ContentType.JSON)
                .body(Map.of("contactPhone", "+48666666666", "contactAddress", "Street 1"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .extract().header("ETag");

        String v2 = operatorIfMatch(port, v1)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "Renamed Omit"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .body("contactPhone", equalTo("+48666666666"))
                .body("contactAddress", equalTo("Street 1"))
                .extract().header("ETag");

        Map<String, Object> clearPhone = new LinkedHashMap<>();
        clearPhone.put("contactPhone", null);
        operatorIfMatch(port, v2)
                .contentType(ContentType.JSON)
                .body(clearPhone)
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .body("contactPhone", nullValue())
                .body("contactAddress", equalTo("Street 1"));
    }

    private Response createMerchant(String label) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference(label), "Original " + label))
                .when().post("/api/merchants");
    }
}
