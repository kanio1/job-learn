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

import java.util.LinkedHashMap;
import java.util.Map;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.createMerchantBody;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorIfMatch;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.operatorRequest;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static lab.paymentquality.testsupport.MerchantApiTestSupport.uniqueMerchantReference;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MerchantIfMatchRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_if_match_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-M360-050 GET detail has ETag v{n}")
    void getDetailHasVersionEtag() {
        String id = createMerchant(uniqueMerchantReference("RA050"), "RA-M360-050")
                .then().statusCode(201).extract().path("merchantId");

        operatorRequest(port)
                .when().get("/api/merchants/{id}", id)
                .then()
                .statusCode(200)
                .header("ETag", startsWith("\"v"));
    }

    @Test
    @DisplayName("RA-M360-051 activate without If-Match is 428")
    void activateWithoutIfMatchIs428() {
        String id = createMerchant(uniqueMerchantReference("RA051"), "RA-M360-051")
                .then().statusCode(201).extract().path("merchantId");

        operatorRequest(port)
                .when().post("/api/merchants/{id}/activate", id)
                .then()
                .statusCode(428)
                .contentType("application/problem+json")
                .body("error", equalTo("precondition_required"));
    }

    @Test
    @DisplayName("RA-M360-052b malformed If-Match is 400 not 428")
    void malformedIfMatchIs400() {
        String id = createMerchant(uniqueMerchantReference("RA052B"), "RA-M360-052b")
                .then().statusCode(201).extract().path("merchantId");

        operatorIfMatch(port, "not-an-etag")
                .when().post("/api/merchants/{id}/activate", id)
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("error", equalTo("malformed_if_match"));
    }

    @Test
    @DisplayName("RA-M360-052 stale If-Match v99 is 412")
    void staleIfMatchIs412() {
        String id = createMerchant(uniqueMerchantReference("RA052"), "RA-M360-052")
                .then().statusCode(201).extract().path("merchantId");

        operatorIfMatch(port, "\"v99\"")
                .when().post("/api/merchants/{id}/activate", id)
                .then()
                .statusCode(412)
                .contentType("application/problem+json")
                .body("error", equalTo("merchant_version_mismatch"));
    }

    @Test
    @DisplayName("RA-M360-053 happy If-Match activate is 200 and ETag increments")
    void happyIfMatchActivateIncrementsEtag() {
        Response created = createMerchant(uniqueMerchantReference("RA053"), "RA-M360-053");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        String nextEtag = operatorIfMatch(port, etag)
                .when().post("/api/merchants/{id}/activate", id)
                .then()
                .statusCode(200)
                .header("ETag", notNullValue())
                .extract().header("ETag");

        org.assertj.core.api.Assertions.assertThat(nextEtag).isNotEqualTo(etag);
    }

    @Test
    @DisplayName("RA-M360-054 risk-flag requires If-Match")
    void riskFlagWithoutIfMatchIs428() {
        Response created = createMerchant(uniqueMerchantReference("RA054"), "RA-M360-054");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body("{\"riskFlagged\":true}")
                .when().patch("/api/merchants/{id}/risk-flag", id)
                .then()
                .statusCode(428)
                .body("error", equalTo("precondition_required"));

        requestWithToken(port, TestJwtSupport.platformAdminToken())
                .header("If-Match", etag)
                .contentType(ContentType.JSON)
                .body("{\"riskFlagged\":true}")
                .when().patch("/api/merchants/{id}/risk-flag", id)
                .then()
                .statusCode(200)
                .header("ETag", not(equalTo(etag)))
                .body("riskFlagged", equalTo(true));
    }

    @Test
    @DisplayName("RA-M360-055 tenant isolation still 403 without leaking If-Match")
    void tenantIsolationUnchanged() {
        Map<String, Object> body = new LinkedHashMap<>(createMerchantBody(
                uniqueMerchantReference("RA055"), "RA-M360-055"));
        body.put("tenantReference", "PLACEHOLDER_TENANT_ID");
        String id = requestWithToken(port, TestJwtSupport.platformAdminToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/merchants")
                .then()
                .statusCode(201)
                .extract()
                .path("merchantId");

        requestWithToken(port, TestJwtSupport.tenantAdminToken())
                .header("If-Match", "\"v0\"")
                .when().post("/api/merchants/{id}/activate", id)
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("error", equalTo("forbidden"))
                .body("error", not(equalTo("merchant_version_mismatch")));
    }

    @Test
    @DisplayName("RA-M360-052c concurrent same If-Match maps lost race to 412")
    void concurrentSameIfMatchIs412() throws Exception {
        Response created = createMerchant(uniqueMerchantReference("RA052C"), "RA-M360-052c");
        created.then().statusCode(201);
        String id = created.path("merchantId");
        String etag = created.getHeader("ETag");

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> activate = () -> operatorIfMatch(port, etag)
                    .when().post("/api/merchants/{id}/activate", id)
                    .then().extract().statusCode();
            var first = pool.submit(activate);
            var second = pool.submit(activate);
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(first.get(), second.get()))
                    .contains(200, 412);
        } finally {
            pool.shutdownNow();
        }
    }

    private Response createMerchant(String reference, String displayName) {
        return operatorRequest(port)
                .contentType(ContentType.JSON)
                .body(createMerchantBody(reference, displayName))
                .when().post("/api/merchants");
    }
}
