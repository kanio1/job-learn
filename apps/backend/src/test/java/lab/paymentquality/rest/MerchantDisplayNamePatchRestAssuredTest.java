package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
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

import java.util.Map;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorIfMatch;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantDisplayNamePatchRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_display_name_patch_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-M360-090 PATCH displayName validation 2–120")
    void patchDisplayNameValidationBounds() {
        Response created = createMerchant("RA090");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");
        String originalName = created.path("displayName");
        String originalRef = created.path("merchantReference");

        operatorIfMatch(port, etag)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "A"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        operatorIfMatch(port, etag)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "x".repeat(121)))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        String renamed = operatorIfMatch(port, etag)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "Ab"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .extract().path("displayName");
        assertThat(renamed).isEqualTo("Ab");

        String after = operatorRequest(port)
                .when().get("/api/merchants/{id}", id)
                .then().statusCode(200)
                .extract().path("merchantReference");
        assertThat(after).isEqualTo(originalRef);
        assertThat(originalName).isNotEqualTo("Ab");
    }

    @Test
    @DisplayName("RA-M360-091 PATCH displayName without If-Match is 428")
    void patchDisplayNameWithoutIfMatchIs428() {
        Response created = createMerchant("RA091");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String originalName = created.path("displayName");

        operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "No Match Name"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(428)
                .body("error", equalTo("precondition_required"));

        String persisted = operatorRequest(port)
                .when().get("/api/merchants/{id}", id)
                .then().statusCode(200)
                .extract().path("displayName");
        assertThat(persisted).isEqualTo(originalName);
    }

    @Test
    @DisplayName("RA-M360-091b stale If-Match PATCH displayName is 412")
    void staleIfMatchPatchDisplayNameIs412() {
        Response created = createMerchant("RA091B");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String originalName = created.path("displayName");

        operatorIfMatch(port, "\"v99\"")
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "Stale Name"))
                .when().patch("/api/merchants/{id}", id)
                .then()
                .statusCode(412)
                .body("error", equalTo("merchant_version_mismatch"));

        String persisted = operatorRequest(port)
                .when().get("/api/merchants/{id}", id)
                .then().statusCode(200)
                .extract().path("displayName");
        assertThat(persisted).isEqualTo(originalName);
    }

    private Response createMerchant(String label) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference(label), "Original " + label))
                .when().post("/api/merchants");
    }
}
