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
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class UserManagementKeycloakAdminIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("user_mgmt_it");

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
        registry.add("payment-quality.keycloak.admin.client-secret", () -> "test-admin-secret");
    }

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
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
