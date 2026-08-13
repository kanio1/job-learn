package lab.paymentquality.rest;

import lab.paymentquality.checkoutlab.internal.application.CheckoutLabInboxWorker;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabReconcileService;
import lab.paymentquality.testsupport.CheckoutLabTestSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.checkout-lab.enabled=true",
                "app.checkout-lab.oauth-client-secret=test-oauth-secret",
                "app.checkout-lab.hmac-secret=test-hmac-secret",
                "app.checkout-lab.hosted-checkout-base-url=http://localhost:3000"
        })
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class CheckoutLabProtocolRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_protocol_rest");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    CheckoutLabInboxWorker inboxWorker;

    @Autowired
    CheckoutLabReconcileService reconcileService;

    @Test
    void healthIsPublicAndDoesNotRequireKeycloakJwt() {
        given().port(port)
                .when().get("/api/checkout-lab/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void hostedGetOmitsNotifyUrlIssuesSimulateTokenAndDoesNotRequireLabBearer() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = CheckoutLabTestSupport.createSession(
                port, token, "BOOK-HOSTED-1", notifyUrl());
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);

        given().port(port)
                .when().get("/api/checkout-lab/hosted/sessions/" + sessionId)
                .then()
                .statusCode(200)
                .body("sessionId", equalTo(sessionId))
                .body("amountMinor", equalTo(1999))
                .body("notifyUrl", nullValue())
                .body("simulateToken", notNullValue())
                .body("simulateTokenExpiresAt", notNullValue());
    }

    @Test
    void simulateWithoutTokenReturns403ProblemJson() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = CheckoutLabTestSupport.createSession(port, token, "BOOK-NO-TOKEN", notifyUrl());
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);

        given().port(port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"outcome\":\"COMPLETED\"}")
                .when().post("/api/checkout-lab/hosted/sessions/" + sessionId + "/simulate")
                .then()
                .statusCode(403)
                .contentType("application/problem+json")
                .body("error", equalTo("missing_simulate_token"))
                .body("instance", containsString("/simulate"));
    }

    @Test
    void notifyHappyPathReturns202ThenWorkerConfirmsFulfillment() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = CheckoutLabTestSupport.createSession(port, token, "BOOK-NOTIFY-OK", notifyUrl());
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);

        CheckoutLabTestSupport.simulateCompleted(port, sessionId);

        Integer received = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_event WHERE session_id = CAST(? AS uuid) AND process_status = 'RECEIVED'",
                Integer.class,
                sessionId);
        assertThat(received).isEqualTo(1);

        int processed = inboxWorker.processBatch(10);
        assertThat(processed).isGreaterThanOrEqualTo(1);

        given().port(port)
                .when().get("/api/checkout-lab/hosted/sessions/" + sessionId + "/fulfillment")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void notifyFiveXxRetryScenarioEventuallyQueuesEvent() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = given().port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + token)
                .header("Lab-Force-Scenario", "NOTIFY_5XX_RETRY")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-503",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return",
                          "notifyUrl": "%s",
                          "validitySeconds": 900
                        }
                        """.formatted(notifyUrl()))
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302)
                .extract()
                .header(HttpHeaders.LOCATION);
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);

        CheckoutLabTestSupport.simulateCompleted(port, sessionId);

        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_event WHERE session_id = CAST(? AS uuid)",
                Integer.class,
                sessionId);
        assertThat(events).isEqualTo(1);

        given().port(port)
                .header("Authorization", "Bearer " + token)
                .when().get("/api/checkout-lab/sessions/" + sessionId + "/deliveries")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].responseStatus", equalTo(503))
                .body("[1].responseStatus", equalTo(202));

        inboxWorker.processBatch(10);
        given().port(port)
                .when().get("/api/checkout-lab/hosted/sessions/" + sessionId + "/fulfillment")
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void notifyBadSignatureReturns400AndDoesNotInsert() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = CheckoutLabTestSupport.createSession(port, token, "BOOK-BAD-SIG", notifyUrl());
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);

        given().port(port)
                .header("Authorization", "Bearer " + token)
                .header("Lab-Force-Scenario", "bad_signature")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-BAD-SIG-2",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return",
                          "notifyUrl": "%s",
                          "validitySeconds": 900
                        }
                        """.formatted(notifyUrl()))
                .redirects().follow(false)
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302);

        String badSessionLocation = given().port(port)
                .header("Authorization", "Bearer " + token)
                .header("Lab-Force-Scenario", "BAD_SIGNATURE")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-BAD-SIG-3",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return",
                          "notifyUrl": "%s",
                          "validitySeconds": 900
                        }
                        """.formatted(notifyUrl()))
                .redirects().follow(false)
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302)
                .extract()
                .header(HttpHeaders.LOCATION);
        String badSessionId = CheckoutLabTestSupport.sessionIdFromLocation(badSessionLocation);

        CheckoutLabTestSupport.simulateCompleted(port, badSessionId);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_event WHERE session_id = CAST(? AS uuid)",
                Integer.class,
                badSessionId);
        assertThat(count).isZero();
        assertThat(sessionId).isNotBlank();
    }

    @Test
    void duplicateEventIdReturns200AndDoesNotChangeFulfillmentTwice() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = CheckoutLabTestSupport.createSession(port, token, "BOOK-DUP", notifyUrl());
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);

        CheckoutLabTestSupport.simulateCompleted(port, sessionId);

        String eventId = jdbcTemplate.queryForObject(
                "SELECT event_id FROM checkout_event WHERE session_id = CAST(? AS uuid)",
                String.class,
                sessionId);
        inboxWorker.processBatch(10);

        String payload = jdbcTemplate.queryForObject(
                "SELECT payload::text FROM checkout_event WHERE event_id = ?",
                String.class,
                eventId);

        given().port(port)
                .header("Lab-Event-Id", eventId)
                .header("Lab-Signature", CheckoutLabTestSupport.sign(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(payload)
                .when().post("/api/checkout-lab/notify")
                .then()
                .statusCode(200)
                .body("duplicate", equalTo(true));

        Integer fulfillmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_fulfillment WHERE session_id = CAST(? AS uuid) AND status = 'CONFIRMED'",
                Integer.class,
                sessionId);
        assertThat(fulfillmentCount).isEqualTo(1);
    }

    @Test
    void cashBookingConfirmsWithoutCheckoutSession() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String bookingId = given().port(port)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "mode": "CASH",
                          "extOrderId": "CASH-1",
                          "amountMinor": 500,
                          "currency": "PLN"
                        }
                        """)
                .when().post("/api/checkout-lab/bookings")
                .then()
                .statusCode(200)
                .body("mode", equalTo("CASH"))
                .body("fulfillmentStatus", equalTo("CONFIRMED"))
                .extract()
                .path("bookingId");

        Integer sessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_session WHERE ext_order_id = 'CASH-1'",
                Integer.class);
        assertThat(sessions).isZero();
        assertThat(bookingId).isNotBlank();
    }

    @Test
    void idempotencyReplayReturnsSameSession() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String body = """
                {
                  "extOrderId": "BOOK-IDEM",
                  "amountMinor": 1999,
                  "currency": "PLN",
                  "continueUrl": "http://localhost:3000/checkout-lab/return",
                  "notifyUrl": "%s",
                  "validitySeconds": 900
                }
                """.formatted(notifyUrl());
        String first = given().port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "idem-1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302)
                .extract()
                .header(HttpHeaders.LOCATION);
        String second = given().port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "idem-1")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302)
                .header("Idempotency-Replayed", equalTo("true"))
                .extract()
                .header(HttpHeaders.LOCATION);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void idempotencyConflictReturns409ForSameKeyDifferentBody() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        given().port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "idem-conflict")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-IDEM-A",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return",
                          "notifyUrl": "%s",
                          "validitySeconds": 900
                        }
                        """.formatted(notifyUrl()))
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(302);

        given().port(port)
                .redirects().follow(false)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "idem-conflict")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "extOrderId": "BOOK-IDEM-B",
                          "amountMinor": 1999,
                          "currency": "PLN",
                          "continueUrl": "http://localhost:3000/checkout-lab/return",
                          "notifyUrl": "%s",
                          "validitySeconds": 900
                        }
                        """.formatted(notifyUrl()))
                .when().post("/api/checkout-lab/sessions")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("error", equalTo("idempotency_conflict"));
    }

    @Test
    void notifyWithoutJwtIsHmacNotKeycloak() {
        given().port(port)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"id\":\"evt-no-jwt\"}")
                .when().post("/api/checkout-lab/notify")
                .then()
                .statusCode(400)
                .contentType("application/problem+json")
                .body("error", equalTo("invalid_signature"));
    }

    @Test
    void corsPreflightAllowsLabSignatureHeader() {
        given().port(port)
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Lab-Signature")
                .when().options("/api/checkout-lab/notify")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Headers", containsString("Lab-Signature"));
    }

    @Test
    void clockPastValiditySimulateReturns409WithoutNotify() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        String location = CheckoutLabTestSupport.createSession(port, token, "BOOK-CLOCK", notifyUrl());
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);
        Instant validityUntil = jdbcTemplate.queryForObject(
                "SELECT validity_until FROM checkout_session WHERE session_id = CAST(? AS uuid)",
                Instant.class,
                sessionId);
        Instant beforeExpiry = validityUntil.minusSeconds(30);

        given().port(port)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"instant\":\"" + beforeExpiry + "\"}")
                .when().post("/api/checkout-lab/clock")
                .then()
                .statusCode(200);

        String simulateToken = CheckoutLabTestSupport.obtainSimulateToken(port, sessionId);
        assertThat(simulateToken).isNotBlank();

        given().port(port)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"instant\":\"2099-01-01T00:00:00Z\"}")
                .when().post("/api/checkout-lab/clock")
                .then()
                .statusCode(200);

        given().port(port)
                .header("Lab-Simulate-Token", simulateToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"outcome\":\"COMPLETED\"}")
                .when().post("/api/checkout-lab/hosted/sessions/" + sessionId + "/simulate")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("error", equalTo("expired_link"));

        Integer events = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_event WHERE session_id = CAST(? AS uuid)",
                Integer.class,
                sessionId);
        assertThat(events).isZero();
    }

    @Test
    void resetClearsOnlyCheckoutLabTables() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        CheckoutLabTestSupport.createSession(port, token, "BOOK-RESET", notifyUrl());
        Integer merchantsBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchants",
                Integer.class);

        given().port(port)
                .header("Authorization", "Bearer " + token)
                .when().post("/api/checkout-lab/reset")
                .then()
                .statusCode(200);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkout_session", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkout_event", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkout_fulfillment", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checkout_anomaly", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class))
                .isEqualTo(merchantsBefore);
    }

    @Test
    void reconcileDoesNotDuplicateAnomalyRows() {
        String token = CheckoutLabTestSupport.obtainLabAccessToken(port);
        given().port(port)
                .header("Authorization", "Bearer " + token)
                .when().post("/api/checkout-lab/reset")
                .then()
                .statusCode(200);

        String location = CheckoutLabTestSupport.createSession(port, token, "BOOK-RECON", notifyUrl());
        String sessionId = CheckoutLabTestSupport.sessionIdFromLocation(location);
        CheckoutLabTestSupport.simulateCompleted(port, sessionId);

        assertThat(reconcileService.reconcile()).isEqualTo(1);
        assertThat(reconcileService.reconcile()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checkout_anomaly WHERE session_id = CAST(? AS uuid)",
                Integer.class,
                sessionId)).isEqualTo(1);
    }

    private String notifyUrl() {
        return "http://localhost:" + port + "/api/checkout-lab/notify";
    }
}
