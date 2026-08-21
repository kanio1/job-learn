package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorIfMatch;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    void createReadListActivateAndSuspendMerchant() {
        String reference = uniqueMerchantReference("FLOW");

        String id = createMerchant(reference, "Flow Merchant")
                .then()
                .statusCode(201)
                .header("ETag", notNullValue())
                .body("merchantReference", equalTo(reference))
                .body("displayName", equalTo("Flow Merchant"))
                .body("status", equalTo("DRAFT"))
                .extract().path("merchantId");

        String etag = operatorRequest(port)
        .when().get("/api/merchants/{id}", id)
        .then()
                .statusCode(200)
                .body("merchantId", equalTo(id))
                .body("status", equalTo("DRAFT"))
                .extract().header("ETag");

        operatorRequest(port)
        .when().get("/api/merchants")
        .then()
                .statusCode(200)
                .body("content.merchantReference", hasItem(reference));

        String afterActivate = operatorIfMatch(port, etag)
        .when().post("/api/merchants/{id}/activate", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"))
                .extract().header("ETag");

        operatorIfMatch(port, afterActivate)
        .when().post("/api/merchants/{id}/suspend", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));
    }

    @Test
    void listReturnSeededMErchantNewestFirst() {
        String prefix = uniqueMerchantReference("ORDER");
        String first = prefix + "-A";
        String second = prefix + "-B";
        String third = prefix + "-C";

        createMerchant(first, "First Merchant").then().statusCode(201);
        createMerchant(second, "Second Merchant").then().statusCode(201);
        createMerchant(third, "Third Merchant").then().statusCode(201);

        List<Map<String, Object>> merchants = operatorRequest(port)
                .when().get("/api/merchants")
                .then().statusCode(200)
                .extract().path("content");

        List<String> orderedReferences = merchants.stream()
                .map(row -> (String) row.get("merchantReference"))
                .filter(reference -> reference.startsWith(prefix))
                .toList();
        
        assertThat(orderedReferences).containsExactly(third, second, first);
    }

    @Test
    void createValidationAndDuplicateErrors() {
        String reference = uniqueMerchantReference("DUP");
        createMerchant(reference, "Duplicate Merchant").then().statusCode(201);

        createMerchant(reference, "Duplicate Merchant")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("type", equalTo("https://api.payment-quality.local/problems/duplicate-merchant-reference"))
                .body("title", equalTo("Merchant already exists"))
                .body("status", equalTo(409))
                .body("detail", equalTo("A merchant with this reference already exists"))
                .body("error", equalTo("duplicate_merchant_reference"));

        createMerchant("AB", "Short Reference")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        createMerchant(" ", "Blank Reference")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("details.merchantReference", notNullValue());

        createMerchant("A".repeat(65), "Long Reference")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        createMerchant("-ABC", "Bad Reference")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        createMerchant("ABC-", "Bad Reference")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));

        createMerchant(uniqueMerchantReference("BLANKNAME"), " ")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("details.displayName", notNullValue());

        createMerchant(uniqueMerchantReference("BADNAME"), " A ")
                .then()
                .statusCode(400)
                .body("error", equalTo("validation"));
    }

    @Test
    void notFoundMalformedAndInvalidTransitionErrors() {
        Response created = createMerchant(uniqueMerchantReference("ERR"), "Error Merchant");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        operatorRequest(port)
        .when().get("/api/merchants/not-a-uuid")
        .then()
                .statusCode(400)
                .body("error", equalTo("validation"))
                .body("message", equalTo("Malformed merchant ID"));

        operatorRequest(port)
        .when().get("/api/merchants/{id}", UUID.randomUUID())
        .then()
                .statusCode(404)
                .contentType("application/problem+json")
                .body("type", equalTo("https://api.payment-quality.local/problems/not-found"))
                .body("title", equalTo("Not Found"))
                .body("status", equalTo(404))
                .body("error", equalTo("not_found"));

        String afterActivate = operatorIfMatch(port, etag)
        .when().post("/api/merchants/{id}/activate", id)
        .then().statusCode(200)
                .extract().header("ETag");

        operatorIfMatch(port, afterActivate)
        .when().post("/api/merchants/{id}/activate", id)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        String afterSuspend = operatorIfMatch(port, afterActivate)
        .when().post("/api/merchants/{id}/suspend", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"))
                .extract().header("ETag");

        operatorIfMatch(port, afterSuspend)
        .when().post("/api/merchants/{id}/activate", id)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        operatorIfMatch(port, "\"v0\"")
        .when().post("/api/merchants/{id}/activate", UUID.randomUUID())
        .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void suspendValidAndInvalidTransitions() {
        Response draftCreated = createMerchant(uniqueMerchantReference("DRAFT"), "Draft Merchant");
        draftCreated.then().statusCode(201);
        String draftId = draftCreated.path("merchantId");
        String draftEtag = draftCreated.getHeader("ETag");
        Response activeCreated = createMerchant(uniqueMerchantReference("ACTIVE"), "Active Merchant");
        activeCreated.then().statusCode(201);
        String activeId = activeCreated.path("merchantId");
        String activeEtag = activeCreated.getHeader("ETag");

        operatorIfMatch(port, draftEtag)
        .when().post("/api/merchants/{id}/suspend", draftId)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        String afterActivate = operatorIfMatch(port, activeEtag)
        .when().post("/api/merchants/{id}/activate", activeId)
        .then().statusCode(200)
                .extract().header("ETag");

        String afterSuspend = operatorIfMatch(port, afterActivate)
        .when().post("/api/merchants/{id}/suspend", activeId)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"))
                .extract().header("ETag");

        operatorIfMatch(port, afterSuspend)
        .when().post("/api/merchants/{id}/suspend", activeId)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        operatorIfMatch(port, "\"v0\"")
        .when().post("/api/merchants/{id}/suspend", UUID.randomUUID())
        .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    @DisplayName("RA-M360-019 activate already ACTIVE merchant is 409 invalid_transition")
    void activateAlreadyActiveReturns409() {
        Response created = createMerchant(uniqueMerchantReference("RA019"), "RA-M360-019");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        String afterActivate = operatorIfMatch(port, etag)
        .when().post("/api/merchants/{id}/activate", id)
        .then().statusCode(200)
                .extract().header("ETag");

        operatorIfMatch(port, afterActivate)
                .when().post("/api/merchants/{id}/activate", id)
                .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));
    }

    @ParameterizedTest(name = "RA-M360-030 merchantReference length {0} → HTTP {1}")
    @CsvSource({
            "2, 400",
            "3, 201",
            "64, 201",
            "65, 400"
    })
    @DisplayName("RA-M360-030 merchantReference length BVA")
    void merchantReferenceLengthBounds(int length, int expectedStatus) {
        int status = -1;
        Response response = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            String reference = merchantReferenceOfLength(length);
            response = createMerchant(reference, "RA-M360-030");
            status = response.statusCode();
            if (expectedStatus == 201 && status == 409) {
                continue;
            }
            break;
        }
        assertThat(status).isEqualTo(expectedStatus);
        if (expectedStatus == 400) {
            response.then().body("error", equalTo("validation"));
        } else {
            response.then().body("merchantId", notNullValue());
        }
    }

    private static String merchantReferenceOfLength(int length) {
        if (length <= 0) {
            return "";
        }
        String seed = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String padded = (seed + "X".repeat(Math.max(length, 1))).substring(0, length);
        char[] chars = padded.toCharArray();
        if (!Character.isLetterOrDigit(chars[0])) {
            chars[0] = 'A';
        }
        if (!Character.isLetterOrDigit(chars[chars.length - 1])) {
            chars[chars.length - 1] = 'Z';
        }
        return new String(chars);
    }

    private Response createMerchant(String reference, String displayName) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(reference, displayName))
        .when().post("/api/merchants");
    }
}
