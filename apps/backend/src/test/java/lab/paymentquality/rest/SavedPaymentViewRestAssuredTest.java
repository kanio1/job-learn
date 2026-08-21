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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class SavedPaymentViewRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("saved_payment_view_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RA-OPS-140 POST view 201; GET list 1")
    void postCreatesViewAndListReturnsOne() {
        String subject = "view.owner." + UUID.randomUUID();
        String token = paymentReaderToken(subject);
        String name = "Large EUR captured " + UUID.randomUUID();
        String viewId = requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .body(viewBody(name, filters(Map.of(
                        "status", "CAPTURED",
                        "currency", "EUR",
                        "minAmount", 10000,
                        "sort", "createdAt,desc"))))
                .when()
                .post("/api/users/me/payment-views")
                .then()
                .statusCode(201)
                .header("Location", startsWith("/api/users/me/payment-views/"))
                .body("id", notNullValue())
                .body("name", equalTo(name))
                .body("resource", equalTo("PAYMENT_ORDERS"))
                .body("filters.status", equalTo("CAPTURED"))
                .body("filters.currency", equalTo("EUR"))
                .body("filters.minAmount", equalTo(10000))
                .body("filters.sort", equalTo("createdAt,desc"))
                .extract()
                .path("id");

        requestWithToken(port, token)
                .when()
                .get("/api/users/me/payment-views")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].id", equalTo(viewId))
                .body("content[0].name", equalTo(name));

        String owner = jdbcTemplate.queryForObject(
                "select owner_subject from user_saved_views where view_id = ?",
                String.class,
                UUID.fromString(viewId));
        assertThat(owner).isEqualTo(subject);

        Boolean hasPage = jdbcTemplate.queryForObject(
                """
                select (filters->>'page') is not null or (filters->>'size') is not null
                from user_saved_views
                where view_id = ?
                """,
                Boolean.class,
                UUID.fromString(viewId));
        assertThat(hasPage).isFalse();
    }

    @Test
    @DisplayName("RA-OPS-141 second subject GET empty")
    void secondSubjectDoesNotSeeOtherOwnersViews() {
        String name = "Owner only " + UUID.randomUUID();
        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(viewBody(name, filters(Map.of("status", "CAPTURED"))))
                .when()
                .post("/api/users/me/payment-views")
                .then()
                .statusCode(201);

        requestWithToken(port, TestJwtSupport.opsFeedManagerToken("00000000-0000-0000-0000-0000000000b1"))
                .when()
                .get("/api/users/me/payment-views")
                .then()
                .statusCode(200)
                .body("content", hasSize(0));
    }

    @Test
    @DisplayName("RA-OPS-142 second default flips first is_default false")
    void secondDefaultFlipsFirst() {
        String token = paymentReaderToken("view.default." + UUID.randomUUID());
        String firstName = "First default " + UUID.randomUUID();
        String secondName = "Second default " + UUID.randomUUID();
        String firstId = requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .body(viewBody(firstName, filters(Map.of("status", "CAPTURED")), true))
                .when()
                .post("/api/users/me/payment-views")
                .then()
                .statusCode(201)
                .body("isDefault", equalTo(true))
                .extract()
                .path("id");

        String secondId = requestWithToken(port, token)
                .contentType(ContentType.JSON)
                .body(viewBody(secondName, filters(Map.of("currency", "EUR"))))
                .when()
                .post("/api/users/me/payment-views")
                .then()
                .statusCode(201)
                .extract()
                .path("id");

        requestWithToken(port, token)
                .when()
                .post("/api/users/me/payment-views/{id}/default", secondId)
                .then()
                .statusCode(200)
                .body("id", equalTo(secondId))
                .body("isDefault", equalTo(true));

        requestWithToken(port, token)
                .when()
                .get("/api/users/me/payment-views")
                .then()
                .statusCode(200)
                .body("content.find { it.id == '" + firstId + "' }.isDefault", equalTo(false))
                .body("content.find { it.id == '" + secondId + "' }.isDefault", equalTo(true));

        Boolean firstDefault = jdbcTemplate.queryForObject(
                "select is_default from user_saved_views where view_id = ?",
                Boolean.class,
                UUID.fromString(firstId));
        Boolean secondDefault = jdbcTemplate.queryForObject(
                "select is_default from user_saved_views where view_id = ?",
                Boolean.class,
                UUID.fromString(secondId));
        assertThat(firstDefault).isFalse();
        assertThat(secondDefault).isTrue();
    }

    @Test
    @DisplayName("RA-OPS-143 unknown filter key 400")
    void unknownFilterKeyIs400() {
        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(viewBody("Bad filter " + UUID.randomUUID(), filters(Map.of(
                        "status", "CAPTURED",
                        "amountMin", 10000))))
                .when()
                .post("/api/users/me/payment-views")
                .then()
                .statusCode(400);
    }

    private static Map<String, Object> viewBody(String name, Map<String, Object> filters) {
        return viewBody(name, filters, false);
    }

    private static Map<String, Object> viewBody(String name, Map<String, Object> filters, boolean isDefault) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("filters", filters);
        body.put("columns", java.util.List.of("clientOrderReference", "amountMinor", "status", "createdAt"));
        body.put("isDefault", isDefault);
        return body;
    }

    private static Map<String, Object> filters(Map<String, Object> values) {
        return new LinkedHashMap<>(values);
    }

    private static String paymentReaderToken(String subject) {
        return TestJwtSupport.tokenWithRolesAndTenantId(
                subject,
                java.util.List.of("platform:payments:read"),
                "PLATFORM_TENANT");
    }
}
