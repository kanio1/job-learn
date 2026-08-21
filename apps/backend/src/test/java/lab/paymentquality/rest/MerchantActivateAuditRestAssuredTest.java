package lab.paymentquality.rest;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
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

import java.util.List;
import java.util.Map;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorIfMatch;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantActivateAuditRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_activate_audit_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-M360-081 audit list after activate includes MERCHANT_ACTIVATED")
    void auditListAfterActivateIncludesMerchantActivated() throws InterruptedException {
        Response created = operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(uniqueMerchantReference("RA081"), "RA-M360-081"))
                .when().post("/api/merchants");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        operatorIfMatch(port, etag)
                .when().post("/api/merchants/{id}/activate", id)
                .then()
                .statusCode(200);

        List<Map<String, Object>> matching = List.of();
        for (int attempt = 0; attempt < 40; attempt++) {
            List<Map<String, Object>> content = requestWithToken(port, TestJwtSupport.supportAgentToken())
                    .queryParam("action", "MERCHANT_ACTIVATED")
                    .queryParam("target_type", "MERCHANT")
                    .queryParam("size", 100)
                    .when()
                    .get("/api/audit")
                    .then()
                    .statusCode(200)
                    .extract()
                    .jsonPath()
                    .getList("content");
            matching = content.stream()
                    .filter(row -> id.equals(String.valueOf(row.get("targetId"))))
                    .toList();
            if (!matching.isEmpty()) {
                break;
            }
            Thread.sleep(100);
        }

        assertThat(matching)
                .as("async audit listener should persist MERCHANT_ACTIVATED for %s", id)
                .isNotEmpty();
        assertThat(matching.getFirst().get("action")).isEqualTo("MERCHANT_ACTIVATED");
        assertThat(matching.getFirst().get("targetType")).isEqualTo("MERCHANT");
    }
}
