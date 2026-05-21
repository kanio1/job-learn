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

import static org.hamcrest.Matchers.equalTo;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.publicRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;

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
        var merchant = merchantService.create(uniqueMerchantReference("AUTH"), "Auth Merchant");
        String id = merchant.getMerchantId().toString();

        assertMerchantEndpointsRequire401(null, id);
        assertMerchantEndpointsRequire401("not-a-jwt", id);
        assertMerchantEndpointsRequire401(TestJwtSupport.expiredToken(), id);
        assertMerchantEndpointsRequire401(TestJwtSupport.invalidIssuerToken(), id);
        assertMerchantEndpointsRequire401(TestJwtSupport.invalidSignatureToken(), id);

        publicRequest(port).when().get("/api/status").then().statusCode(200);
        requestWithToken(port, TestJwtSupport.expiredToken()).when().get("/api/status").then().statusCode(200);
    }

    @Test
    void deniedIdentityReturns403ForMerchantEndpointsButStatusRemainsPublic() {
        String token = TestJwtSupport.deniedToken();
        var merchant = merchantService.create(uniqueMerchantReference("DENY"), "Denied Merchant");
        String id = merchant.getMerchantId().toString();

        publicRequest(port).when().get("/api/status").then().statusCode(200);
        requestWithToken(port, token).when().get("/api/merchants").then().statusCode(403);
        requestWithToken(port, token).when().get("/api/merchants/{id}", id).then().statusCode(403);
        requestWithToken(port, token).contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("DENIED"), "Denied Merchant"))
                .when().post("/api/merchants").then().statusCode(403);
        requestWithToken(port, token).when().post("/api/merchants/{id}/activate", id).then().statusCode(403);
        requestWithToken(port, token).when().post("/api/merchants/{id}/suspend", id).then().statusCode(403);
    }

    @Test
    void partialAuthoritiesAreSeparatedAcrossEndpoints() {
        String readOnly = TestJwtSupport.tokenWithRoles("read.only", List.of("merchants:read"));
        String createOnly = TestJwtSupport.tokenWithRoles("create.only", List.of("merchants:create"));
        String updateOnly = TestJwtSupport.tokenWithRoles("update.only", List.of("merchants:update-status"));

        var merchant = merchantService.create(uniqueMerchantReference("SEC"), "Security Merchant");
        String id = merchant.getMerchantId().toString();

        requestWithToken(port, readOnly).when().get("/api/merchants").then().statusCode(200);
        requestWithToken(port, readOnly).when().get("/api/merchants/{id}", id).then().statusCode(200);
        requestWithToken(port, readOnly).contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("NOCREATE"), "No Create"))
                .when().post("/api/merchants").then().statusCode(403);
        requestWithToken(port, readOnly).when().post("/api/merchants/{id}/activate", id).then().statusCode(403);
        requestWithToken(port, readOnly).when().post("/api/merchants/{id}/suspend", id).then().statusCode(403);

        requestWithToken(port, createOnly).contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("CREATE"), "Create Only"))
                .when().post("/api/merchants").then().statusCode(201)
                .body("status", equalTo("DRAFT"));
        requestWithToken(port, createOnly).when().get("/api/merchants").then().statusCode(403);
        requestWithToken(port, createOnly).when().get("/api/merchants/{id}", id).then().statusCode(403);
        requestWithToken(port, createOnly).when().post("/api/merchants/{id}/activate", id).then().statusCode(403);
        requestWithToken(port, createOnly).when().post("/api/merchants/{id}/suspend", id).then().statusCode(403);

        requestWithToken(port, updateOnly).when().post("/api/merchants/{id}/activate", id).then().statusCode(200);
        requestWithToken(port, updateOnly).when().post("/api/merchants/{id}/suspend", id).then().statusCode(200);
        requestWithToken(port, updateOnly).when().get("/api/merchants").then().statusCode(403);
        requestWithToken(port, updateOnly).when().get("/api/merchants/{id}", id).then().statusCode(403);
        requestWithToken(port, updateOnly).contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("UPD"), "Update Only"))
                .when().post("/api/merchants").then().statusCode(403);
    }

    @Test
    void fullPlatformOperatorCanUseAllMerchantEndpoints() {
        String token = TestJwtSupport.platformOperatorToken();
        String id = requestWithToken(port, token).contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("FULL"), "Full Operator"))
        .when().post("/api/merchants")
        .then()
                .statusCode(201)
                .extract().path("merchantId");

        requestWithToken(port, token).when().get("/api/merchants").then().statusCode(200);
        requestWithToken(port, token).when().get("/api/merchants/{id}", id).then().statusCode(200);
        requestWithToken(port, token).when().post("/api/merchants/{id}/activate", id).then().statusCode(200);
        requestWithToken(port, token).when().post("/api/merchants/{id}/suspend", id).then().statusCode(200);
    }

    private void assertMerchantEndpointsRequire401(String token, String id) {
        var list = publicRequest(port);
        var getById = publicRequest(port);
        var create = publicRequest(port).contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("NOAUTH"), "No Auth"));
        var activate = publicRequest(port);
        var suspend = publicRequest(port);
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
}
