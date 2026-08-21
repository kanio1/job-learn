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

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantImportRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_import_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RA-M360-060 preview 200 counts and does not insert merchants")
    void previewDoesNotInsertMerchants() {
        String reference = uniqueMerchantReference("IMP060");
        String csv = csvRow(reference, "Import Preview Merchant", "TENANT_ALPHA");
        int before = merchantCount();

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .multiPart("file", "valid.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .when().post("/api/merchants/import/preview")
                .then()
                .statusCode(200)
                .body("validCount", equalTo(1))
                .body("rejectedCount", equalTo(0))
                .body("previewId", org.hamcrest.Matchers.notNullValue());

        assertThat(merchantCount()).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from merchants where normalized_reference = ?",
                Integer.class,
                reference)).isZero();
    }

    @Test
    @DisplayName("RA-M360-061 readonly cannot preview import")
    void readonlyPreviewIs403() {
        String csv = csvRow(uniqueMerchantReference("IMP061"), "Readonly Import", "TENANT_ALPHA");
        String readOnly = TestJwtSupport.tokenWithRolesAndTenantId(
                "read.only.import", java.util.List.of("merchants:read"), "TENANT_ALPHA");

        requestWithToken(port, readOnly)
                .multiPart("file", "valid.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .when().post("/api/merchants/import/preview")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("RA-M360-062 malformed CSV is 400 problem")
    void malformedCsvIs400() {
        byte[] malformed = "merchantReference,displayName,tenantReference\n\"unclosed".getBytes(StandardCharsets.UTF_8);

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .multiPart("file", "malformed.csv", malformed, "text/csv")
                .when().post("/api/merchants/import/preview")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("error", equalTo("validation"));
    }

    @Test
    @DisplayName("RA-M360-063 commit inserts merchants; second commit is 409")
    void commitInsertsThenSecondCommitConflicts() {
        String reference = uniqueMerchantReference("IMP063");
        String csv = csvRow(reference, "Import Commit Merchant", "TENANT_ALPHA");
        int before = merchantCount();

        String previewId = requestWithToken(port, TestJwtSupport.platformAdminToken())
                .multiPart("file", "valid.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .when().post("/api/merchants/import/preview")
                .then()
                .statusCode(200)
                .extract()
                .path("previewId");

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("previewId", previewId))
                .when().post("/api/merchants/import/commit")
                .then()
                .statusCode(200)
                .body("createdCount", equalTo(1));

        assertThat(merchantCount()).isEqualTo(before + 1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from merchants where normalized_reference = ?",
                Integer.class,
                reference)).isEqualTo(1);

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("previewId", previewId))
                .when().post("/api/merchants/import/commit")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("error", equalTo("import_already_committed"));
    }

    @Test
    @DisplayName("duplicate existing UK is rejected on preview")
    void duplicateExistingReferenceIsRejected() {
        String reference = uniqueMerchantReference("IMPDUP");
        Map<String, Object> body = new LinkedHashMap<>(createMerchantBody(reference, "Existing Import"));
        body.put("tenantReference", "TENANT_ALPHA");
        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/merchants")
                .then()
                .statusCode(201);

        String csv = csvRow(reference, "Duplicate Import", "TENANT_ALPHA");
        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .multiPart("file", "duplicate.csv", csv.getBytes(StandardCharsets.UTF_8), "text/csv")
                .when().post("/api/merchants/import/preview")
                .then()
                .statusCode(200)
                .body("validCount", equalTo(0))
                .body("rejectedCount", greaterThanOrEqualTo(1))
                .body("rows[0].reason", equalTo("duplicate_merchant_reference"));
    }

    private int merchantCount() {
        Integer count = jdbcTemplate.queryForObject("select count(*) from merchants", Integer.class);
        return count == null ? 0 : count;
    }

    private static String csvRow(String reference, String displayName, String tenantReference) {
        return "merchantReference,displayName,tenantReference\n"
                + reference + "," + displayName + "," + tenantReference + "\n";
    }
}
