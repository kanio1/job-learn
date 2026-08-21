package lab.paymentquality.rest;

import lab.paymentquality.testsupport.MerchantApiTestSupport;
import lab.paymentquality.testsupport.PaymentApiTestSupport;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class RefundChallengeRestAssuredTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("refund_challenge_rest_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("RA-OPS-170 amount ≤ 100000: create challenge 400 pin_not_required")
    void lowValueChallengeIs400() {
        CapturedOrder order = captured(100_000);
        pendingApproval(order, 100_000);
        MerchantApiTestSupport.requestWithToken(port, order.checkerToken())
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges",
                        order.merchantId(), order.paymentOrderId())
                .then()
                .statusCode(400)
                .body("error", equalTo("pin_not_required"));
    }

    @Test
    @DisplayName("RA-OPS-171 amount 100001: 201 challenge; hash ≠ pin")
    void highValueChallengeReturnsPinInTest() {
        CapturedOrder order = captured(100_001);
        pendingApproval(order, 100_001);
        var response = MerchantApiTestSupport.requestWithToken(port, order.checkerToken())
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges",
                        order.merchantId(), order.paymentOrderId())
                .then()
                .statusCode(201)
                .body("challengeId", notNullValue())
                .body("pin", notNullValue())
                .extract();
        String pin = response.path("pin");
        String challengeId = response.path("challengeId");
        String hash = jdbcTemplate.queryForObject(
                "select pin_hash from payment_refund_challenges where challenge_id = ?::uuid",
                String.class,
                challengeId);
        assertThat(hash).isNotEqualTo(pin);
        assertThat(pin).hasSize(6);
    }

    @Test
    @DisplayName("RA-OPS-172 verify correct → 200; approval still PENDING")
    void verifyLeavesApprovalPending() {
        HighValueChallenge challenge = highValueChallenge();
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .contentType("application/json")
                .body(Map.of("pin", challenge.pin()))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                .then()
                .statusCode(200)
                .body("verifiedAt", notNullValue());
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .when()
                .get("/api/merchants/{mid}/payment-orders/{pid}/refund-approvals",
                        challenge.order().merchantId(), challenge.order().paymentOrderId())
                .then()
                .statusCode(200)
                .body("content[0].status", equalTo("PENDING"));
    }

    @Test
    @DisplayName("RA-OPS-173 wrong pin ×4 still 400; 5th 429")
    void fifthInvalidPinIs429() {
        HighValueChallenge challenge = highValueChallenge();
        for (int i = 0; i < 4; i++) {
            MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                    .contentType("application/json")
                    .body(Map.of("pin", "000000"))
                    .when()
                    .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                            challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                    .then()
                    .statusCode(400)
                    .body("error", equalTo("invalid_pin"));
        }
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .contentType("application/json")
                .body(Map.of("pin", "000000"))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                .then()
                .statusCode(429)
                .body("error", equalTo("rate_limited"))
                .body("lockedUntil", notNullValue());
    }

    @Test
    @DisplayName("RA-OPS-174 429 body error=rate_limited; locked_until")
    void rateLimitedIncludesLockedUntil() {
        HighValueChallenge challenge = highValueChallenge();
        for (int i = 0; i < 5; i++) {
            MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                    .contentType("application/json")
                    .body(Map.of("pin", "111111"))
                    .when()
                    .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                            challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId());
        }
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .contentType("application/json")
                .body(Map.of("pin", "111111"))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                .then()
                .statusCode(429)
                .body("error", equalTo("rate_limited"))
                .body("lockedUntil", notNullValue());
    }

    @Test
    @DisplayName("RA-OPS-175 verify after TTL 400 expired")
    void expiredChallengeIs400() {
        HighValueChallenge challenge = highValueChallenge();
        jdbcTemplate.update(
                "update payment_refund_challenges set expires_at = now() - interval '1 second' where challenge_id = ?::uuid",
                challenge.challengeId());
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .contentType("application/json")
                .body(Map.of("pin", challenge.pin()))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                .then()
                .statusCode(400)
                .body("error", equalTo("expired"));
    }

    @Test
    @DisplayName("RA-OPS-176 reuse verified 409")
    void reuseVerifiedIs409() {
        HighValueChallenge challenge = highValueChallenge();
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .contentType("application/json")
                .body(Map.of("pin", challenge.pin()))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                .then()
                .statusCode(200);
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .contentType("application/json")
                .body(Map.of("pin", challenge.pin()))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                .then()
                .statusCode(409)
                .body("error", equalTo("already_verified"));
    }

    @Test
    @DisplayName("RA-OPS-177 maker verify 409/403")
    void makerVerifyIs409() {
        HighValueChallenge challenge = highValueChallenge();
        MerchantApiTestSupport.requestWithToken(port, challenge.order().makerToken())
                .contentType("application/json")
                .body(Map.of("pin", challenge.pin()))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges/{cid}/verify",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.challengeId())
                .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("RA-OPS-178 approve high-value without verify → 400 step_up_required")
    void approveWithoutVerifyIs400() {
        HighValueChallenge challenge = highValueChallenge();
        MerchantApiTestSupport.requestWithToken(port, challenge.order().checkerToken())
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("nopin"))
                .header("If-Match", challenge.order().etag())
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-approvals/{aid}/approve",
                        challenge.order().merchantId(), challenge.order().paymentOrderId(), challenge.approvalId())
                .then()
                .statusCode(400)
                .body("error", equalTo("step_up_required"));
    }

    @Test
    @DisplayName("RA-OPS-179 readonly 403")
    void readonlyCreateChallengeIs403() {
        CapturedOrder order = captured(100_001);
        pendingApproval(order, 100_001);
        String readonly = TestJwtSupport.merchantPaymentReaderToken(order.merchantId());
        MerchantApiTestSupport.requestWithToken(port, readonly)
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges",
                        order.merchantId(), order.paymentOrderId())
                .then()
                .statusCode(403);
    }

    private HighValueChallenge highValueChallenge() {
        CapturedOrder order = captured(100_001);
        String approvalId = pendingApproval(order, 100_001);
        var response = MerchantApiTestSupport.requestWithToken(port, order.checkerToken())
                .contentType("application/json")
                .body(Map.of())
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-challenges",
                        order.merchantId(), order.paymentOrderId())
                .then()
                .statusCode(201)
                .extract();
        return new HighValueChallenge(order, approvalId, response.path("challengeId"), response.path("pin"));
    }

    private String pendingApproval(CapturedOrder order, long amountMinor) {
        return MerchantApiTestSupport.requestWithToken(port, order.makerToken())
                .contentType("application/json")
                .body(Map.of("amountMinor", amountMinor, "reason", "high-value refund"))
                .when()
                .post("/api/merchants/{mid}/payment-orders/{pid}/refund-approvals",
                        order.merchantId(), order.paymentOrderId())
                .then()
                .statusCode(201)
                .extract()
                .path("approvalId");
    }

    private CapturedOrder captured(long amountMinor) {
        String merchantId = PaymentApiTestSupport.createActiveMerchant(
                port, MerchantApiTestSupport.operatorRequest(port));
        String creator = TestJwtSupport.merchantPaymentCreatorToken(merchantId);
        String paymentOrderId = MerchantApiTestSupport.requestWithToken(port, creator)
                .contentType("application/json")
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("pin-create"))
                .body(PaymentApiTestSupport.createPaymentOrderBody(
                        amountMinor, "PLN", PaymentApiTestSupport.uniquePaymentReference("PIN")))
                .when()
                .post("/api/merchants/{merchantId}/payment-orders", merchantId)
                .then()
                .statusCode(201)
                .extract()
                .path("paymentOrderId");
        String reader = TestJwtSupport.merchantPaymentReaderToken(merchantId);
        String maker = TestJwtSupport.merchantPaymentLifecycleToken(merchantId);
        String etag = MerchantApiTestSupport.requestWithToken(port, reader)
                .when()
                .get("/api/merchants/{id}/payment-orders/{oid}", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
        String afterAuth = MerchantApiTestSupport.requestWithToken(port, maker)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("pin-auth"))
                .header("If-Match", etag)
                .contentType("application/json")
                .body("{}")
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/authorize", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
        String afterCapture = MerchantApiTestSupport.requestWithToken(port, maker)
                .header("Idempotency-Key", PaymentApiTestSupport.uniqueIdempotencyKey("pin-cap"))
                .header("If-Match", afterAuth)
                .contentType("application/json")
                .body(Map.of("amountMinor", amountMinor))
                .when()
                .post("/api/merchants/{id}/payment-orders/{oid}/capture", merchantId, paymentOrderId)
                .then()
                .statusCode(200)
                .extract()
                .header("ETag");
        return new CapturedOrder(
                merchantId,
                paymentOrderId,
                afterCapture,
                maker,
                TestJwtSupport.platformAdminToken());
    }

    private record CapturedOrder(
            String merchantId,
            String paymentOrderId,
            String etag,
            String makerToken,
            String checkerToken
    ) {
    }

    private record HighValueChallenge(
            CapturedOrder order,
            String approvalId,
            String challengeId,
            String pin
    ) {
    }
}
