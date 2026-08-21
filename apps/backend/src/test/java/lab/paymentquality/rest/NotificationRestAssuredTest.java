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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static lab.paymentquality.testsupport.MerchantApiTestSupport.requestWithToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class NotificationRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("ops_notification_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RA-OPS-190 inbox is actionable only (FAILED in, CAPTURE out)")
    void inboxAudienceIsActionableOnly() {
        UUID captureEvent = UUID.randomUUID();
        UUID failedEvent = UUID.randomUUID();

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(injectBody(captureEvent, "PAYMENT_CAPTURED", "PO-190  CAPTURED"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201);

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .body("content.eventId", not(hasItem(captureEvent.toString())))
                .body("content.eventType", not(hasItem("PAYMENT_CAPTURED")));

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(injectBody(failedEvent, "PAYMENT_FAILED", "PO-190  FAILED"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201);

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .body("content.eventId", hasItem(failedEvent.toString()))
                .body("content.eventType", hasItem("PAYMENT_FAILED"));
    }

    @Test
    @DisplayName("RA-OPS-191 mark read persists read_at")
    void markReadPersistsReadAt() {
        UUID eventId = UUID.randomUUID();
        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(injectBody(eventId, "REFUND_APPROVAL_NEEDED", "PO-191  refund-approval-needed"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201);

        String notificationId = requestWithToken(port, TestJwtSupport.opsInjectToken())
                .when()
                .get("/api/notifications?unreadOnly=true")
                .then()
                .statusCode(200)
                .body("content.eventId", hasItem(eventId.toString()))
                .extract()
                .path("content.find { it.eventId == '" + eventId + "' }.notificationId");

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .when()
                .post("/api/notifications/{id}/read", notificationId)
                .then()
                .statusCode(200)
                .body("readAt", notNullValue());

        Instant readAt = jdbcTemplate.queryForObject(
                "select read_at from ops_notifications where notification_id = ?",
                Instant.class,
                UUID.fromString(notificationId));
        assertThat(readAt).isNotNull();
    }

    @Test
    @DisplayName("RA-OPS-192 unique (recipient_subject, event_id) second insert 0 rows")
    void duplicateEventIdDoesNotInsertSecondNotification() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> body = injectBody(eventId, "SUPPORT_CASE_ASSIGNED", "INC-192  assigned");

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201);

        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201);

        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from ops_notifications where event_id = ? and recipient_subject = ?",
                Integer.class,
                eventId,
                "platform.admin");
        assertThat(rows).isEqualTo(1);
    }

    @Test
    @DisplayName("RA-OPS-193 BOLA GET foreign notification 404")
    void foreignNotificationGetIs404() {
        UUID eventId = UUID.randomUUID();
        requestWithToken(port, TestJwtSupport.opsInjectToken())
                .contentType(ContentType.JSON)
                .body(injectBody(eventId, "PAYMENT_FAILED", "PO-193  FAILED"))
                .when()
                .post("/api/ops/feed/inject")
                .then()
                .statusCode(201);

        String notificationId = requestWithToken(port, TestJwtSupport.opsInjectToken())
                .when()
                .get("/api/notifications")
                .then()
                .statusCode(200)
                .extract()
                .path("content.find { it.eventId == '" + eventId + "' }.notificationId");

        requestWithToken(port, TestJwtSupport.opsFeedReadonlyToken())
                .when()
                .get("/api/notifications/{id}", notificationId)
                .then()
                .statusCode(404)
                .body("error", equalTo("not_found"));
    }

    private static Map<String, Object> injectBody(UUID eventId, String type, String label) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventId", eventId.toString());
        body.put("occurredAt", Instant.parse("2026-08-20T10:42:03Z").toString());
        body.put("merchantId", UUID.randomUUID().toString());
        body.put("paymentOrderId", UUID.randomUUID().toString());
        body.put("type", type);
        body.put("label", label);
        return body;
    }
}
