package lab.paymentquality.rest;

import io.restassured.http.ContentType;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class OpsFeedInjectRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("ops_feed_inject_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Test
    @DisplayName("RA-OPS-125 inject 201; readonly inject 403")
    void injectIs201ForAdminAnd403ForReadonly() {
        UUID eventId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();
        UUID paymentOrderId = UUID.randomUUID();

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(injectBody(eventId, merchantId, paymentOrderId, "PAYMENT_CAPTURED", "PO-OPS-125  CAPTURED"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201)
                .body("eventId", equalTo(eventId.toString()))
                .body("type", equalTo("PAYMENT_CAPTURED"))
                .body("label", equalTo("PO-OPS-125  CAPTURED"))
                .body("merchantId", equalTo(merchantId.toString()))
                .body("paymentOrderId", equalTo(paymentOrderId.toString()))
                .body("occurredAt", notNullValue());

        requestWithToken(port, TestJwtSupport.opsFeedReadonlyToken())
                .contentType(ContentType.JSON)
                .body(injectBody(UUID.randomUUID(), merchantId, paymentOrderId, "PAYMENT_CAPTURED", "PO-OPS-125-RO  CAPTURED"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("RA-OPS-126 inject malformed accepted 201")
    void injectMalformedRawIs201() {
        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(Map.of("raw", "{not-json"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201)
                .body("malformed", equalTo(true));
    }

    @Test
    @DisplayName("RA-OPS-127 manager inject 403")
    void merchantManagerInjectIs403() {
        String manager = TestJwtSupport.opsFeedManagerToken(UUID.randomUUID().toString());

        requestWithToken(port, manager)
                .contentType(ContentType.JSON)
                .body(injectBody(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "PAYMENT_CAPTURED",
                        "PO-OPS-127  CAPTURED"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(403);
    }

    private static Map<String, Object> injectBody(
            UUID eventId,
            UUID merchantId,
            UUID paymentOrderId,
            String type,
            String label) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId.toString());
        body.put("occurredAt", Instant.parse("2026-08-20T10:42:03Z").toString());
        body.put("merchantId", merchantId.toString());
        body.put("paymentOrderId", paymentOrderId.toString());
        body.put("type", type);
        body.put("label", label);
        return body;
    }
}
