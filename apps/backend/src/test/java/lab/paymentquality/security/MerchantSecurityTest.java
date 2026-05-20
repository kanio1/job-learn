package lab.paymentquality.security;

import io.restassured.http.ContentType;
import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantSecurityTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer<?> postgres = newPostgresContainer("merchant_security_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    MerchantService merchantService;

    @Test
    void unauthenticatedAndInvalidTokensReturn401() {
        var merchant = merchantService.create(uniqueReference("AUTH"), "Auth Merchant");
        String id = merchant.getMerchantId().toString();

        assertMerchantEndpointsRequire401(null, id);
        assertMerchantEndpointsRequire401("not-a-jwt", id);
        assertMerchantEndpointsRequire401(TestJwtSupport.expiredToken(), id);
        assertMerchantEndpointsRequire401(TestJwtSupport.invalidIssuerToken(), id);
        assertMerchantEndpointsRequire401(TestJwtSupport.invalidSignatureToken(), id);

        given().port(port).when().get("/api/status").then().statusCode(200);
        given().port(port).auth().oauth2(TestJwtSupport.expiredToken()).when().get("/api/status").then().statusCode(200);
    }

    @Test
    void deniedIdentityReturns403ForMerchantEndpointsButStatusRemainsPublic() {
        String token = TestJwtSupport.deniedToken();
        var merchant = merchantService.create(uniqueReference("DENY"), "Denied Merchant");
        String id = merchant.getMerchantId().toString();

        given().port(port).when().get("/api/status").then().statusCode(200);
        given().port(port).auth().oauth2(token).when().get("/api/merchants").then().statusCode(403);
        given().port(port).auth().oauth2(token).when().get("/api/merchants/{id}", id).then().statusCode(403);
        given().port(port).auth().oauth2(token).contentType(ContentType.JSON)
                .body(createBody(uniqueReference("DENIED"), "Denied Merchant"))
                .when().post("/api/merchants").then().statusCode(403);
        given().port(port).auth().oauth2(token).when().post("/api/merchants/{id}/activate", id).then().statusCode(403);
        given().port(port).auth().oauth2(token).when().post("/api/merchants/{id}/suspend", id).then().statusCode(403);
    }

    @Test
    void partialAuthoritiesAreSeparatedAcrossEndpoints() {
        String readOnly = TestJwtSupport.tokenWithRoles("read.only", List.of("merchants:read"));
        String createOnly = TestJwtSupport.tokenWithRoles("create.only", List.of("merchants:create"));
        String updateOnly = TestJwtSupport.tokenWithRoles("update.only", List.of("merchants:update-status"));

        var merchant = merchantService.create(uniqueReference("SEC"), "Security Merchant");
        String id = merchant.getMerchantId().toString();

        given().port(port).auth().oauth2(readOnly).when().get("/api/merchants").then().statusCode(200);
        given().port(port).auth().oauth2(readOnly).when().get("/api/merchants/{id}", id).then().statusCode(200);
        given().port(port).auth().oauth2(readOnly).contentType(ContentType.JSON)
                .body(createBody(uniqueReference("NOCREATE"), "No Create"))
                .when().post("/api/merchants").then().statusCode(403);
        given().port(port).auth().oauth2(readOnly).when().post("/api/merchants/{id}/activate", id).then().statusCode(403);
        given().port(port).auth().oauth2(readOnly).when().post("/api/merchants/{id}/suspend", id).then().statusCode(403);

        given().port(port).auth().oauth2(createOnly).contentType(ContentType.JSON)
                .body(createBody(uniqueReference("CREATE"), "Create Only"))
                .when().post("/api/merchants").then().statusCode(201)
                .body("status", equalTo("DRAFT"));
        given().port(port).auth().oauth2(createOnly).when().get("/api/merchants").then().statusCode(403);
        given().port(port).auth().oauth2(createOnly).when().get("/api/merchants/{id}", id).then().statusCode(403);
        given().port(port).auth().oauth2(createOnly).when().post("/api/merchants/{id}/activate", id).then().statusCode(403);
        given().port(port).auth().oauth2(createOnly).when().post("/api/merchants/{id}/suspend", id).then().statusCode(403);

        given().port(port).auth().oauth2(updateOnly).when().post("/api/merchants/{id}/activate", id).then().statusCode(200);
        given().port(port).auth().oauth2(updateOnly).when().post("/api/merchants/{id}/suspend", id).then().statusCode(200);
        given().port(port).auth().oauth2(updateOnly).when().get("/api/merchants").then().statusCode(403);
        given().port(port).auth().oauth2(updateOnly).when().get("/api/merchants/{id}", id).then().statusCode(403);
        given().port(port).auth().oauth2(updateOnly).contentType(ContentType.JSON)
                .body(createBody(uniqueReference("UPD"), "Update Only"))
                .when().post("/api/merchants").then().statusCode(403);
    }

    @Test
    void fullPlatformOperatorCanUseAllMerchantEndpoints() {
        String token = TestJwtSupport.platformOperatorToken();
        String id = given().port(port).auth().oauth2(token).contentType(ContentType.JSON)
                .body(createBody(uniqueReference("FULL"), "Full Operator"))
        .when().post("/api/merchants")
        .then()
                .statusCode(201)
                .extract().path("merchantId");

        given().port(port).auth().oauth2(token).when().get("/api/merchants").then().statusCode(200);
        given().port(port).auth().oauth2(token).when().get("/api/merchants/{id}", id).then().statusCode(200);
        given().port(port).auth().oauth2(token).when().post("/api/merchants/{id}/activate", id).then().statusCode(200);
        given().port(port).auth().oauth2(token).when().post("/api/merchants/{id}/suspend", id).then().statusCode(200);
    }

    private void assertMerchantEndpointsRequire401(String token, String id) {
        var list = given().port(port);
        var getById = given().port(port);
        var create = given().port(port).contentType(ContentType.JSON).body(createBody(uniqueReference("NOAUTH"), "No Auth"));
        var activate = given().port(port);
        var suspend = given().port(port);
        if (token != null) {
            list.auth().oauth2(token);
            getById.auth().oauth2(token);
            create.auth().oauth2(token);
            activate.auth().oauth2(token);
            suspend.auth().oauth2(token);
        }

        list.when().get("/api/merchants").then().statusCode(401);
        getById.when().get("/api/merchants/{id}", id).then().statusCode(401);
        create.when().post("/api/merchants").then().statusCode(401);
        activate.when().post("/api/merchants/{id}/activate", id).then().statusCode(401);
        suspend.when().post("/api/merchants/{id}/suspend", id).then().statusCode(401);
    }

    private static String createBody(String reference, String displayName) {
        return "{\"merchantReference\":\"%s\",\"displayName\":\"%s\"}".formatted(reference, displayName);
    }

    private static String uniqueReference(String label) {
        return "MERCH-" + label + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
