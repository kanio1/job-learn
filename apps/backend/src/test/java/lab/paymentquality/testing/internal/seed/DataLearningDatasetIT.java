package lab.paymentquality.testing.internal.seed;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class DataLearningDatasetIT extends PostgresContainerSupport {

    private static final Instant RANGE_START = DataLearningFixtures.RANGE_START;
    private static final Instant RANGE_END = DataLearningFixtures.RANGE_END;
    private static final UUID MERCHANT_ALPHA_001 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("data_learning_dataset_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    DataLearningDataset dataset;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void smallSeedMatchesPaymentTruthAndLegalHistories() {
        DataLearningTruth truth = dataset.seed(DataLearningProfile.SMALL);

        assertThat(truth.tenants()).isEqualTo(5);
        assertThat(truth.merchants()).isEqualTo(20);
        assertThat(truth.payments()).isEqualTo(10_000);
        assertThat(truth.paymentHistoryRows()).isEqualTo(28_000);
        assertThat(truth.capturedPayments()).isEqualTo(6_000);
        assertThat(truth.refundedPayments()).isEqualTo(1_200);
        assertThat(truth.cancelledPayments()).isEqualTo(800);
        assertThat(truth.authorizedPayments()).isEqualTo(800);
        assertThat(truth.expiredPayments()).isEqualTo(400);
        assertThat(truth.createdPayments()).isEqualTo(800);
        assertThat(truth.tenantAlphaPayments()).isEqualTo(5_500);

        assertThat(count("SELECT COUNT(*) FROM tenants")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM merchants")).isEqualTo(20);
        assertThat(count("SELECT COUNT(*) FROM payment_orders")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM payment_order_status_history")).isEqualTo(28_000);
        assertThat(truth.checkoutSessions()).isEqualTo(2_000);
        assertThat(truth.checkoutEvents()).isEqualTo(5_000);
        assertThat(truth.checkoutFulfillments()).isEqualTo(1_950);
        assertThat(truth.checkoutAnomalies()).isEqualTo(50);
        assertThat(truth.auditEvents()).isEqualTo(10_000);
        assertThat(truth.publicationEvents()).isEqualTo(10_000);
        assertThat(truth.incompletePublications()).isEqualTo(100);
        assertThat(truth.totalAmountMinor()).isEqualTo(59_995_000L);
        assertThat(truth.paymentsPln()).isEqualTo(3_334);
        assertThat(truth.paymentsEur()).isEqualTo(3_333);
        assertThat(truth.paymentsUsd()).isEqualTo(3_333);
        assertThat(truth.totalCapturedAmountMinor()).isPositive();
        assertThat(truth.totalRefundedAmountMinor()).isPositive();

        assertThat(count("SELECT COUNT(*) FROM payment_refund_approvals")).isEqualTo(0);
        assertThat(count("SELECT COUNT(*) FROM rls_lab_item")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM checkout_session")).isEqualTo(2_000);
        assertThat(count("SELECT COUNT(*) FROM checkout_event")).isEqualTo(5_000);
        assertThat(count("SELECT COUNT(*) FROM checkout_fulfillment")).isEqualTo(1_950);
        assertThat(count("SELECT COUNT(*) FROM checkout_anomaly")).isEqualTo(50);
        assertThat(count("SELECT COUNT(*) FROM audit_event")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM event_publication")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL")).isEqualTo(100);
        assertThat(count("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NOT NULL AND completion_attempts = 1")).isEqualTo(9_000);
        assertThat(count("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NOT NULL AND completion_attempts = 2")).isEqualTo(700);
        assertThat(count("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NOT NULL AND completion_attempts >= 3")).isEqualTo(200);

        assertThat(count("SELECT COUNT(*) FROM audit_event WHERE tenant_id = 'TENANT_ALPHA'")).isEqualTo(5_500);
        assertThat(count("SELECT COUNT(*) FROM audit_event WHERE tenant_id = 'PLATFORM_TENANT'")).isEqualTo(2_000);
        assertThat(count("SELECT COUNT(*) FROM audit_event WHERE tenant_id = 'LEARN_TENANT_C'")).isEqualTo(1_500);
        assertThat(count("SELECT COUNT(*) FROM audit_event WHERE tenant_id = 'LEARN_TENANT_D'")).isEqualTo(800);
        assertThat(count("SELECT COUNT(*) FROM audit_event WHERE tenant_id = 'PLACEHOLDER_TENANT_ID'")).isEqualTo(200);
        assertThat(count("""
                SELECT COUNT(*) FROM payment_orders po
                JOIN merchants m ON m.merchant_id = po.merchant_id
                JOIN tenants t ON t.tenant_id = m.tenant_id
                WHERE t.tenant_reference = 'PLATFORM_TENANT'
                """)).isEqualTo(2_000);
        assertThat(count("""
                SELECT COUNT(*) FROM payment_orders po
                JOIN merchants m ON m.merchant_id = po.merchant_id
                JOIN tenants t ON t.tenant_id = m.tenant_id
                WHERE t.tenant_reference = 'LEARN_TENANT_C'
                """)).isEqualTo(1_500);
        assertThat(count("""
                SELECT COUNT(*) FROM payment_orders po
                JOIN merchants m ON m.merchant_id = po.merchant_id
                JOIN tenants t ON t.tenant_id = m.tenant_id
                WHERE t.tenant_reference = 'LEARN_TENANT_D'
                """)).isEqualTo(800);
        assertThat(count("""
                SELECT COUNT(*) FROM payment_orders po
                JOIN merchants m ON m.merchant_id = po.merchant_id
                JOIN tenants t ON t.tenant_id = m.tenant_id
                WHERE t.tenant_reference = 'PLACEHOLDER_TENANT_ID'
                """)).isEqualTo(200);
        assertThat(jdbc.queryForList("SELECT DISTINCT action FROM audit_event", String.class))
                .containsExactlyInAnyOrder(
                        "MERCHANT_CREATED",
                        "MERCHANT_ACTIVATED",
                        "MERCHANT_SUSPENDED",
                        "MERCHANT_RISK_FLAGGED",
                        "PAYMENT_CREATED",
                        "PAYMENT_CAPTURED",
                        "PAYMENT_REFUNDED");
        assertThat(jdbc.queryForList("SELECT DISTINCT outcome FROM audit_event", String.class))
                .containsExactlyInAnyOrder("SUCCESS", "DENIED", "FAILED");
        assertThat(count("SELECT COUNT(*) FROM audit_event WHERE before_state IS NOT NULL AND after_state IS NOT NULL"))
                .isGreaterThan(0);

        assertThat(count("SELECT COUNT(*) FROM checkout_session s LEFT JOIN checkout_fulfillment f ON f.session_id = s.session_id WHERE f.session_id IS NULL"))
                .isEqualTo(50);
        assertThat(count("SELECT COUNT(*) FROM checkout_anomaly a WHERE NOT EXISTS (SELECT 1 FROM checkout_fulfillment f WHERE f.session_id = a.session_id)"))
                .isEqualTo(50);

        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM checkout_session", String.class))
                .contains("COMPLETED", "CANCELED", "EXPIRED", "REFUNDED", "CREATED", "PENDING")
                .allMatch(status -> List.of("CREATED", "PENDING", "COMPLETED", "CANCELED", "EXPIRED", "REFUNDED").contains(status));
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM checkout_fulfillment", String.class))
                .contains("CONFIRMED", "CANCELLED", "EXPIRED", "AWAITING_PAYMENT")
                .allMatch(status -> List.of("AWAITING_PAYMENT", "CONFIRMED", "CANCELLED", "EXPIRED").contains(status));

        assertThat(count("""
                SELECT COUNT(*) FROM checkout_session s
                JOIN checkout_fulfillment f ON f.session_id = s.session_id
                WHERE s.status = 'COMPLETED' AND f.status = 'CONFIRMED'
                """)).isGreaterThan(0);
        assertThat(count("""
                SELECT COUNT(*) FROM checkout_session s
                JOIN checkout_fulfillment f ON f.session_id = s.session_id
                WHERE s.status = 'CANCELED' AND f.status = 'CANCELLED'
                """)).isGreaterThan(0);
        assertThat(count("""
                SELECT COUNT(*) FROM checkout_session s
                JOIN checkout_fulfillment f ON f.session_id = s.session_id
                WHERE s.status = 'EXPIRED' AND f.status = 'EXPIRED'
                """)).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM checkout_session WHERE status = 'REFUNDED'")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM checkout_event WHERE attempts > 1")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM checkout_event WHERE ack_status = 503")).isGreaterThan(0);
        assertThat(count("SELECT COUNT(*) FROM checkout_event WHERE process_status = 'DUPLICATE'")).isGreaterThan(0);
        assertThat(count("""
                SELECT COUNT(*) FROM checkout_session s
                JOIN checkout_fulfillment f ON f.session_id = s.session_id
                WHERE s.status = 'COMPLETED' AND f.status = 'AWAITING_PAYMENT'
                """)).isGreaterThan(0);

        Integer alpha001 = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders WHERE merchant_id = ?",
                Integer.class, MERCHANT_ALPHA_001);
        assertThat(alpha001).isEqualTo(5_500);

        assertThat(jdbc.queryForList("SELECT DISTINCT currency FROM payment_orders", String.class))
                .containsExactlyInAnyOrder("PLN", "EUR", "USD");

        Instant minCreated = jdbc.queryForObject("SELECT MIN(created_at) FROM payment_orders", Instant.class);
        Instant maxCreated = jdbc.queryForObject("SELECT MAX(created_at) FROM payment_orders", Instant.class);
        assertThat(minCreated).isAfterOrEqualTo(RANGE_START);
        assertThat(maxCreated).isBeforeOrEqualTo(RANGE_END);

        Integer illegal = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT to_status,
                           LAG(to_status) OVER (
                               PARTITION BY payment_order_id
                               ORDER BY created_at ASC, status_history_id ASC
                           ) AS prev
                    FROM payment_order_status_history
                ) chained
                WHERE NOT (
                    (prev IS NULL AND to_status = 'CREATED')
                    OR (prev = 'CREATED' AND to_status IN ('AUTHORIZED', 'CANCELLED'))
                    OR (prev = 'AUTHORIZED' AND to_status IN ('CAPTURED', 'EXPIRED'))
                    OR (prev = 'CAPTURED' AND to_status = 'REFUNDED')
                )
                """, Integer.class);
        assertThat(illegal).isZero();
    }

    @Test
    void twoConsecutiveSmallSeedsAreIdentical() {
        dataset.seed(DataLearningProfile.SMALL);
        UUID firstZero = paymentIdAtIndex(0);
        UUID firstLast = paymentIdAtIndex(9_999);
        int firstZeroHistory = historyCount(firstZero);
        int firstLastHistory = historyCount(firstLast);
        List<UUID> firstTen = firstTenPaymentIds();
        UUID firstSessionZero = sessionIdAtIndex(0);
        UUID firstSessionLast = sessionIdAtIndex(1_999);
        UUID firstAuditZero = auditIdAtIndex(0);
        UUID firstAuditLast = auditIdAtIndex(9_999);
        UUID firstPublicationZero = publicationIdAtIndex(0);
        UUID firstPublicationLast = publicationIdAtIndex(9_999);

        dataset.seed(DataLearningProfile.SMALL);
        assertThat(paymentIdAtIndex(0)).isEqualTo(firstZero);
        assertThat(paymentIdAtIndex(9_999)).isEqualTo(firstLast);
        assertThat(historyCount(firstZero)).isEqualTo(firstZeroHistory);
        assertThat(historyCount(firstLast)).isEqualTo(firstLastHistory);
        assertThat(firstTenPaymentIds()).isEqualTo(firstTen);
        assertThat(sessionIdAtIndex(0)).isEqualTo(firstSessionZero);
        assertThat(sessionIdAtIndex(1_999)).isEqualTo(firstSessionLast);
        assertThat(auditIdAtIndex(0)).isEqualTo(firstAuditZero);
        assertThat(auditIdAtIndex(9_999)).isEqualTo(firstAuditLast);
        assertThat(publicationIdAtIndex(0)).isEqualTo(firstPublicationZero);
        assertThat(publicationIdAtIndex(9_999)).isEqualTo(firstPublicationLast);
        assertThat(count("SELECT COUNT(*) FROM payment_orders")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM payment_order_status_history")).isEqualTo(28_000);
        assertThat(count("SELECT COUNT(*) FROM checkout_session")).isEqualTo(2_000);
        assertThat(count("SELECT COUNT(*) FROM checkout_event")).isEqualTo(5_000);
        assertThat(count("SELECT COUNT(*) FROM checkout_fulfillment")).isEqualTo(1_950);
        assertThat(count("SELECT COUNT(*) FROM checkout_anomaly")).isEqualTo(50);
        assertThat(count("SELECT COUNT(*) FROM audit_event")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM event_publication")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL")).isEqualTo(100);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    private UUID auditIdAtIndex(int index) {
        return jdbc.queryForObject(
                "SELECT id FROM audit_event WHERE correlation_id = ?",
                UUID.class, "LEARN-AUD-" + String.format("%06d", index));
    }

    private UUID publicationIdAtIndex(int index) {
        return jdbc.queryForObject(
                "SELECT id FROM event_publication WHERE serialized_event = ?",
                UUID.class, "{\"index\":" + index + ",\"source\":\"data-learning-seed\"}");
    }

    private UUID sessionIdAtIndex(int index) {
        return jdbc.queryForObject(
                "SELECT session_id FROM checkout_session WHERE ext_order_id = ?",
                UUID.class, "LEARN-CHK-" + String.format("%06d", index));
    }

    private UUID paymentIdAtIndex(int index) {
        return jdbc.queryForObject(
                "SELECT payment_order_id FROM payment_orders WHERE client_order_reference = ?",
                UUID.class, "LEARN-PAY-" + String.format("%06d", index));
    }

    private int historyCount(UUID paymentOrderId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_order_status_history WHERE payment_order_id = ?",
                Integer.class, paymentOrderId);
    }

    private List<UUID> firstTenPaymentIds() {
        return jdbc.queryForList(
                "SELECT payment_order_id FROM payment_orders WHERE client_order_reference LIKE 'LEARN-PAY-00000%' ORDER BY client_order_reference",
                UUID.class);
    }
}
