package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer<?> postgres = newPostgresContainer("merchant_rest_test");

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
                .body("merchantReference", equalTo(reference))
                .body("displayName", equalTo("Flow Merchant"))
                .body("status", equalTo("DRAFT"))
                .extract().path("merchantId");

        operatorRequest(port)
        .when().get("/api/merchants/{id}", id)
        .then()
                .statusCode(200)
                .body("merchantId", equalTo(id))
                .body("status", equalTo("DRAFT"));

        operatorRequest(port)
        .when().get("/api/merchants")
        .then()
                .statusCode(200)
                .body("merchants.merchantReference", hasItem(reference));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/activate", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("ACTIVE"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/suspend", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));
    }

    @Test
    void listReturnsSeededMerchantsNewestFirst() {
        String prefix = uniqueMerchantReference("ORDER");
        String first = prefix + "-A";
        String second = prefix + "-B";
        String third = prefix + "-C";

        createMerchant(first, "First Merchant").then().statusCode(201);
        createMerchant(second, "Second Merchant").then().statusCode(201);
        createMerchant(third, "Third Merchant").then().statusCode(201);

        List<Map<String, Object>> merchants = operatorRequest(port)
        .when().get("/api/merchants")
        .then()
                .statusCode(200)
                .extract().path("merchants");

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
        String id = createMerchant(uniqueMerchantReference("ERR"), "Error Merchant")
                .then().statusCode(201).extract().path("merchantId");

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
                .body("error", equalTo("not_found"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/activate", id)
        .then().statusCode(200);

        operatorRequest(port)
        .when().post("/api/merchants/{id}/activate", id)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/suspend", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/activate", id)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/activate", UUID.randomUUID())
        .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void suspendValidAndInvalidTransitions() {
        String draftId = createMerchant(uniqueMerchantReference("DRAFT"), "Draft Merchant")
                .then().statusCode(201).extract().path("merchantId");
        String activeId = createMerchant(uniqueMerchantReference("ACTIVE"), "Active Merchant")
                .then().statusCode(201).extract().path("merchantId");

        operatorRequest(port)
        .when().post("/api/merchants/{id}/suspend", draftId)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/activate", activeId)
        .then().statusCode(200);

        operatorRequest(port)
        .when().post("/api/merchants/{id}/suspend", activeId)
        .then()
                .statusCode(200)
                .body("status", equalTo("SUSPENDED"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/suspend", activeId)
        .then()
                .statusCode(409)
                .body("error", equalTo("invalid_transition"));

        operatorRequest(port)
        .when().post("/api/merchants/{id}/suspend", UUID.randomUUID())
        .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    private io.restassured.response.Response createMerchant(String reference, String displayName) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(reference, displayName))
        .when().post("/api/merchants");
    }
}
