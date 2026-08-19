package lab.paymentquality.iam;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lab.paymentquality.testsupport.KeycloakContainerSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class UserManagementKeycloakAdminIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = startPostgres("user_mgmt_it");

    static KeycloakContainerSupport keycloak;

    @LocalServerPort
    int port;

    @BeforeAll
    static void startKeycloak() {
        keycloak = new KeycloakContainerSupport();
        keycloak.start();
    }

    @AfterAll
    static void stopKeycloak() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", keycloak::getIssuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", keycloak::getJwksUri);
        registry.add("payment-quality.keycloak.admin.base-url", keycloak::getBaseUrl);
        registry.add("payment-quality.keycloak.admin.realm", () -> "payment-quality");
        registry.add("payment-quality.keycloak.admin.client-id", () -> "payment-quality-admin");
        registry.add("payment-quality.keycloak.admin.client-secret", () -> "dev-admin-client-secret");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void diagnosticRawKeycloakAdminApiPersistsAttributes() {
        String uniqueUsername = "diag-" + UUID.randomUUID().toString().substring(0, 8);
        String email = uniqueUsername + "@example.com";

        String adminToken = getAdminToken();

        String userId = given()
            .baseUri(keycloak.getBaseUrl())
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + adminToken)
            .body("""
                {
                    "username": "%s",
                    "email": "%s",
                    "enabled": true,
                    "attributes": {
                        "tenant_id": ["TENANT_ALPHA"]
                    }
                }
                """.formatted(uniqueUsername, email))
        .when()
            .post("/admin/realms/payment-quality/users")
        .then()
            .statusCode(201)
            .extract().header("Location");

        String userIdFromLocation = userId.substring(userId.lastIndexOf('/') + 1);

        String rawUserJson = given()
            .baseUri(keycloak.getBaseUrl())
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/admin/realms/payment-quality/users/" + userIdFromLocation)
        .then()
            .statusCode(200)
            .extract().asString();

        org.junit.jupiter.api.Assertions.assertTrue(
            rawUserJson.contains("tenant_id"),
            "Raw Keycloak GET should contain tenant_id attribute. Raw JSON: " + rawUserJson
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            rawUserJson.contains("TENANT_ALPHA"),
            "Raw Keycloak GET should contain TENANT_ALPHA value. Raw JSON: " + rawUserJson
        );
    }

    private String getAdminToken() {
        return given()
            .baseUri(keycloak.getBaseUrl())
            .contentType("application/x-www-form-urlencoded")
            .formParam("grant_type", "client_credentials")
            .formParam("client_id", "payment-quality-admin")
            .formParam("client_secret", "dev-admin-client-secret")
        .when()
            .post("/realms/payment-quality/protocol/openid-connect/token")
        .then()
            .statusCode(200)
            .extract().path("access_token");
    }

    @Test
    void createAndManageUserThroughRealKeycloak() {
        String uniqueUsername = "testuser-" + UUID.randomUUID().toString().substring(0, 8);
        String email = uniqueUsername + "@example.com";
        String tempPassword = "TempPass123!";

        // Create user
        String userId = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
            .body("""
                {
                    "username": "%s",
                    "email": "%s",
                    "temporaryPassword": "%s",
                    "tenantId": "TENANT_ALPHA",
                    "roles": ["TENANT_ADMIN"]
                }
                """.formatted(uniqueUsername, email, tempPassword))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("username", equalTo(uniqueUsername))
            .body("email", equalTo(email))
            .body("enabled", equalTo(true))
            .body("tenantId", equalTo("TENANT_ALPHA"))
            .body("roles[0]", equalTo("TENANT_ADMIN"))
            .extract().path("id");

        String adminToken = getAdminToken();
        given()
            .baseUri(keycloak.getBaseUrl())
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/admin/realms/payment-quality/users/{id}", userId)
        .then()
            .statusCode(200)
            .body("attributes.tenant_id[0]", equalTo("TENANT_ALPHA"))
            .body("enabled", equalTo(true));

        // Get user
        given()
            .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
        .when()
            .get("/api/users/{id}", userId)
        .then()
            .statusCode(200)
            .body("id", equalTo(userId))
            .body("username", equalTo(uniqueUsername))
            .body("enabled", equalTo(true));

        // Update user (disable)
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
            .body("""
                {
                    "enabled": false
                }
                """)
        .when()
            .patch("/api/users/{id}", userId)
        .then()
            .statusCode(200)
            .body("enabled", equalTo(false));

        // Verify user is disabled
        given()
            .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
        .when()
            .get("/api/users/{id}", userId)
        .then()
            .statusCode(200)
            .body("enabled", equalTo(false));

        // Assign additional role
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
            .body("""
                {
                    "assign": ["MERCHANT_MANAGER"],
                    "remove": []
                }
                """)
        .when()
            .post("/api/users/{id}/roles", userId)
        .then()
            .statusCode(200)
            .body("roles", notNullValue());

        given()
            .baseUri(keycloak.getBaseUrl())
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/admin/realms/payment-quality/users/{id}/role-mappings/realm", userId)
        .then()
            .statusCode(200)
            .body("name", hasItem("MERCHANT_MANAGER"));

        // Remove role
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + TestJwtSupport.platformUserAdminToken())
            .body("""
                {
                    "assign": [],
                    "remove": ["MERCHANT_MANAGER"]
                }
                """)
        .when()
            .post("/api/users/{id}/roles", userId)
        .then()
            .statusCode(200);

        given()
            .baseUri(keycloak.getBaseUrl())
            .header("Authorization", "Bearer " + adminToken)
        .when()
            .get("/admin/realms/payment-quality/users/{id}/role-mappings/realm", userId)
        .then()
            .statusCode(200)
            .body("name", not(hasItem("MERCHANT_MANAGER")));
    }

    @Test
    void tenantScopedUserCreation() {
        String uniqueUsername = "tenantuser-" + UUID.randomUUID().toString().substring(0, 8);
        String email = uniqueUsername + "@example.com";

        // Tenant admin creates user - should auto-assign to their tenant
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + TestJwtSupport.tenantUserAdminToken())
            .body("""
                {
                    "username": "%s",
                    "email": "%s",
                    "temporaryPassword": "TempPass123!",
                    "tenantId": "TENANT_BETA",
                    "roles": ["READ_ONLY_USER"]
                }
                """.formatted(uniqueUsername, email))
        .when()
            .post("/api/users")
        .then()
            .statusCode(201)
            .body("tenantId", equalTo("TENANT_ALPHA")); // Should be TENANT_ALPHA, not TENANT_BETA
    }
}
