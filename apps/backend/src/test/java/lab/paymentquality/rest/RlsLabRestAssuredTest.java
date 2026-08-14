package lab.paymentquality.rest;

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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.rls-lab.enabled=true"})
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
@DisplayName("RLS Lab REST Assured")
class RlsLabRestAssuredTest extends PostgresContainerSupport {

    private static final UUID OTHER_ITEM = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID ALPHA_ITEM = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("rls_lab_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
        registry.add("app.rls-lab.datasource.url", postgres::getJdbcUrl);
        registry.add("app.rls-lab.datasource.username", () -> "rls_lab_app");
        registry.add("app.rls-lab.datasource.password", () -> "rls_lab_app");
        registry.add("app.rls-lab.bypass.url", postgres::getJdbcUrl);
        registry.add("app.rls-lab.bypass.username", () -> "rls_lab_bypass");
        registry.add("app.rls-lab.bypass.password", () -> "rls_lab_bypass");
    }

    @LocalServerPort
    int port;

    private String alphaToken() {
        return TestJwtSupport.tokenWithRolesAndTenantId(
                "merchant.manager",
                List.of("merchant:payments:read"),
                "TENANT_ALPHA");
    }

    @Test
    void alphaJwtListsOnlyAlphaRow() {
        given().port(port)
                .header("Authorization", "Bearer " + alphaToken())
                .when().get("/api/rls-lab/items")
                .then()
                .statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].itemId", equalTo(ALPHA_ITEM.toString()))
                .body("items[0].label", equalTo("Alpha secret"));
    }

    @Test
    void alphaJwtCannotReadOtherTenantItem() {
        given().port(port)
                .header("Authorization", "Bearer " + alphaToken())
                .when().get("/api/rls-lab/items/{id}", OTHER_ITEM)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    @Test
    void platformCompareShowsLeakContrast() {
        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.platformAdminToken())
                .when().get("/api/rls-lab/compare")
                .then()
                .statusCode(200)
                .body("bypassRoleCount", equalTo(2))
                .body("restrictedWithoutTenantGuc", equalTo(0))
                .body("unprotected", equalTo(2));
    }

    @Test
    void supportAgentWithPlatformReadCanCompare() {
        given().port(port)
                .header("Authorization", "Bearer " + TestJwtSupport.supportAgentToken())
                .when().get("/api/rls-lab/compare")
                .then()
                .statusCode(200)
                .body("restrictedWithoutTenantGuc", equalTo(0));
    }

    @Test
    void merchantWithoutPlatformReadIsForbiddenByMethodSecurity() {
        given().port(port)
                .header("Authorization", "Bearer " + alphaToken())
                .when().get("/api/rls-lab/compare")
                .then()
                .statusCode(403);
    }

    @Test
    void platformReadOnMerchantTenantIsRlsForbidden() {
        String spoofed = TestJwtSupport.tokenWithRolesAndTenantId(
                "platform.read.alpha",
                List.of("platform:payments:read"),
                "TENANT_ALPHA");
        given().port(port)
                .header("Authorization", "Bearer " + spoofed)
                .when().get("/api/rls-lab/compare")
                .then()
                .statusCode(403)
                .body("error", equalTo("rls_forbidden"));
    }

    @Test
    void jdbcWithoutGucSeesZeroRowsAndWithGucSeesAlpha() throws Exception {
        UUID alphaTenantId;
        try (Connection owner = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = owner.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT tenant_id FROM tenants WHERE tenant_reference = 'TENANT_ALPHA'")) {
            assertThat(rs.next()).isTrue();
            alphaTenantId = rs.getObject(1, UUID.class);
        }

        try (Connection app = DriverManager.getConnection(
                postgres.getJdbcUrl(), "rls_lab_app", "rls_lab_app")) {
            app.setAutoCommit(false);
            try (Statement statement = app.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT count(*) FROM rls_lab_item")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(0);
            }

            try (Statement statement = app.createStatement()) {
                statement.execute("SELECT set_config('app.tenant_id', '" + alphaTenantId + "', true)");
                try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM rls_lab_item")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(1);
                }
            }
            app.commit();
        }
    }

    @Test
    void jdbcFakeBypassGucDoesNotLeakRows() throws Exception {
        try (Connection app = DriverManager.getConnection(
                postgres.getJdbcUrl(), "rls_lab_app", "rls_lab_app")) {
            app.setAutoCommit(false);
            try (Statement statement = app.createStatement()) {
                statement.execute("SELECT set_config('app.rls_bypass', 'on', true)");
                try (ResultSet rs = statement.executeQuery("SELECT count(*) FROM rls_lab_item")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(0);
                }
            }
            app.commit();
        }
    }

    @Test
    void jdbcOwnerSeesBothRowsAndBypassRoleSeesBothRows() throws Exception {
        try (Connection owner = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement statement = owner.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) FROM rls_lab_item")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }

        try (Connection bypass = DriverManager.getConnection(
                postgres.getJdbcUrl(), "rls_lab_bypass", "rls_lab_bypass");
             Statement statement = bypass.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) FROM rls_lab_item")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }
}
