package lab.paymentquality.testing.internal.seed;

import jakarta.persistence.EntityManager;
import lab.paymentquality.merchant.MerchantSeedCapability;
import lab.paymentquality.payment.PaymentSeedCapability;
import lab.paymentquality.tenant.TenantSeedCapability;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class DataLearningDataset {

    private static final int BATCH_SIZE = 500;
    private static final String ACTOR = "data-learning-seed";

    private final TenantSeedCapability tenants;
    private final MerchantSeedCapability merchants;
    private final PaymentSeedCapability payments;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public DataLearningDataset(TenantSeedCapability tenants,
                               MerchantSeedCapability merchants,
                               PaymentSeedCapability payments,
                               JdbcTemplate jdbc,
                               EntityManager entityManager) {
        this.tenants = tenants;
        this.merchants = merchants;
        this.payments = payments;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    @Transactional
    public DataLearningTruth seed(DataLearningProfile profile) {
        if (profile != DataLearningProfile.SMALL) {
            throw new IllegalArgumentException("Unsupported learning profile: " + profile);
        }
        clear();
        tenants.seed(DataLearningFixtures.tenants());
        merchants.seed(DataLearningFixtures.merchants());
        entityManager.flush();
        insertPaymentsAndHistory();
        insertCheckout();
        insertAuditAndPublications();
        seedRlsLabItems();
        return readTruth();
    }

    private void clear() {
        payments.clear();
        merchants.clear();
        jdbc.update("DELETE FROM rls_lab_item_unprotected");
        jdbc.update("DELETE FROM rls_lab_item");
        tenants.clear();
        SatelliteTableWipes.clearCheckoutAuditAndPublications(jdbc);
        entityManager.flush();
        entityManager.clear();
    }

    private void insertPaymentsAndHistory() {
        var paymentSql = """
                INSERT INTO payment_orders (
                    payment_order_id, merchant_id, client_order_reference,
                    amount_minor, currency, status, version,
                    created_at, updated_at,
                    authorized_at, expires_at, captured_at, cancelled_at, refunded_at,
                    captured_amount_minor, refunded_amount_minor,
                    cancellation_reason, refund_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        var historySql = """
                INSERT INTO payment_order_status_history (
                    status_history_id, payment_order_id, from_status, to_status,
                    actor_subject, correlation_id, created_at, action,
                    idempotency_key_hash, reason, amount_minor, psp_reference
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        var paymentBatch = new ArrayList<PaymentRow>(BATCH_SIZE);
        var historyBatch = new ArrayList<PaymentLearningGenerator.HistoryStep>(BATCH_SIZE);
        for (int i = 0; i < PaymentLearningGenerator.PAYMENT_COUNT; i++) {
            String status = PaymentLearningGenerator.statusFor(i);
            Instant createdAt = PaymentLearningGenerator.createdAtFor(i);
            long amountMinor = PaymentLearningGenerator.amountMinorFor(i);
            var history = PaymentLearningGenerator.historyFor(i, status, createdAt, amountMinor);
            paymentBatch.add(new PaymentRow(
                    PaymentLearningGenerator.paymentIdFor(i),
                    PaymentLearningGenerator.merchantIdFor(i),
                    PaymentLearningGenerator.referenceFor(i),
                    amountMinor,
                    PaymentLearningGenerator.currencyFor(i),
                    status,
                    PaymentLearningGenerator.versionFor(status),
                    createdAt,
                    PaymentLearningGenerator.updatedAtFor(history),
                    PaymentLearningGenerator.authorizedAtFor(status, createdAt),
                    PaymentLearningGenerator.expiresAtFor(status, createdAt),
                    PaymentLearningGenerator.capturedAtFor(status, createdAt),
                    PaymentLearningGenerator.cancelledAtFor(status, createdAt),
                    PaymentLearningGenerator.refundedAtFor(status, createdAt),
                    PaymentLearningGenerator.capturedAmountFor(status, amountMinor),
                    PaymentLearningGenerator.refundedAmountFor(status, amountMinor),
                    PaymentLearningGenerator.cancellationReasonFor(status),
                    PaymentLearningGenerator.refundReasonFor(status)
            ));
            historyBatch.addAll(history);
            if (paymentBatch.size() == BATCH_SIZE) {
                flushPaymentBatch(paymentSql, paymentBatch);
                flushHistoryBatch(historySql, historyBatch);
            }
        }
        flushPaymentBatch(paymentSql, paymentBatch);
        flushHistoryBatch(historySql, historyBatch);
    }

    private void insertCheckout() {
        var sessionSql = """
                INSERT INTO checkout_session (
                    session_id, ext_order_id, amount_minor, currency, status,
                    continue_url, notify_url, redirect_uri, validity_until,
                    idempotency_key_hash, correlation_id, created_at, updated_at, version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        var eventSql = """
                INSERT INTO checkout_event (
                    id, event_id, session_id, event_type, payload, signature_header,
                    received_at, process_status, attempts, last_error, ack_status
                ) VALUES (?, ?, ?, ?, CAST(? AS JSONB), ?, ?, ?, ?, ?, ?)
                """;
        var fulfillmentSql = """
                INSERT INTO checkout_fulfillment (
                    fulfillment_id, session_id, status, source_event_id,
                    confirmed_at, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        var anomalySql = """
                INSERT INTO checkout_anomaly (
                    anomaly_id, session_id, kind, detail, detected_at
                ) VALUES (?, ?, ?, ?, ?)
                """;

        var sessions = new ArrayList<CheckoutLearningGenerator.SessionRow>(BATCH_SIZE);
        var events = new ArrayList<CheckoutLearningGenerator.EventRow>(BATCH_SIZE);
        var fulfillments = new ArrayList<CheckoutLearningGenerator.FulfillmentRow>(BATCH_SIZE);
        var anomalies = new ArrayList<CheckoutLearningGenerator.AnomalyRow>(BATCH_SIZE);
        for (int i = 0; i < CheckoutLearningGenerator.SESSION_COUNT; i++) {
            sessions.add(CheckoutLearningGenerator.sessionFor(i));
            events.addAll(CheckoutLearningGenerator.eventsFor(i));
            var fulfillment = CheckoutLearningGenerator.fulfillmentFor(i);
            if (fulfillment != null) {
                fulfillments.add(fulfillment);
            }
            var anomaly = CheckoutLearningGenerator.anomalyFor(i);
            if (anomaly != null) {
                anomalies.add(anomaly);
            }
            if (sessions.size() == BATCH_SIZE) {
                flushCheckoutSessions(sessionSql, sessions);
                flushCheckoutEvents(eventSql, events);
                flushCheckoutFulfillments(fulfillmentSql, fulfillments);
                flushCheckoutAnomalies(anomalySql, anomalies);
            }
        }
        flushCheckoutSessions(sessionSql, sessions);
        flushCheckoutEvents(eventSql, events);
        flushCheckoutFulfillments(fulfillmentSql, fulfillments);
        flushCheckoutAnomalies(anomalySql, anomalies);
    }

    private void flushCheckoutSessions(String sql, List<CheckoutLearningGenerator.SessionRow> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            ps.setObject(1, row.sessionId());
            ps.setString(2, row.extOrderId());
            ps.setLong(3, row.amountMinor());
            ps.setString(4, row.currency());
            ps.setString(5, row.status());
            ps.setString(6, row.continueUrl());
            ps.setString(7, row.notifyUrl());
            ps.setString(8, row.redirectUri());
            setTimestamp(ps, 9, row.validityUntil());
            ps.setString(10, row.idempotencyKeyHash());
            ps.setString(11, row.correlationId());
            setTimestamp(ps, 12, row.createdAt());
            setTimestamp(ps, 13, row.updatedAt());
            ps.setLong(14, row.version());
        });
        batch.clear();
    }

    private void flushCheckoutEvents(String sql, List<CheckoutLearningGenerator.EventRow> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            ps.setObject(1, row.id());
            ps.setString(2, row.eventId());
            ps.setObject(3, row.sessionId());
            ps.setString(4, row.eventType());
            ps.setString(5, row.payload());
            ps.setString(6, null);
            setTimestamp(ps, 7, row.receivedAt());
            ps.setString(8, row.processStatus());
            ps.setInt(9, row.attempts());
            ps.setString(10, row.lastError());
            setNullableInt(ps, 11, row.ackStatus());
        });
        batch.clear();
    }

    private void flushCheckoutFulfillments(String sql, List<CheckoutLearningGenerator.FulfillmentRow> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            ps.setObject(1, row.fulfillmentId());
            ps.setObject(2, row.sessionId());
            ps.setString(3, row.status());
            ps.setString(4, row.sourceEventId());
            setTimestamp(ps, 5, row.confirmedAt());
            setTimestamp(ps, 6, row.createdAt());
            setTimestamp(ps, 7, row.updatedAt());
        });
        batch.clear();
    }

    private void flushCheckoutAnomalies(String sql, List<CheckoutLearningGenerator.AnomalyRow> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            ps.setObject(1, row.anomalyId());
            ps.setObject(2, row.sessionId());
            ps.setString(3, row.kind());
            ps.setString(4, row.detail());
            setTimestamp(ps, 5, row.detectedAt());
        });
        batch.clear();
    }

    private void flushPaymentBatch(String sql, List<PaymentRow> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> bindPayment(ps, row));
        batch.clear();
    }

    private void flushHistoryBatch(String sql, List<PaymentLearningGenerator.HistoryStep> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, step) -> bindHistory(ps, step));
        batch.clear();
    }

    private static void bindPayment(PreparedStatement ps, PaymentRow row) throws SQLException {
        ps.setObject(1, row.paymentOrderId());
        ps.setObject(2, row.merchantId());
        ps.setString(3, row.reference());
        ps.setLong(4, row.amountMinor());
        ps.setString(5, row.currency());
        ps.setString(6, row.status());
        ps.setLong(7, row.version());
        setTimestamp(ps, 8, row.createdAt());
        setTimestamp(ps, 9, row.updatedAt());
        setTimestamp(ps, 10, row.authorizedAt());
        setTimestamp(ps, 11, row.expiresAt());
        setTimestamp(ps, 12, row.capturedAt());
        setTimestamp(ps, 13, row.cancelledAt());
        setTimestamp(ps, 14, row.refundedAt());
        setNullableLong(ps, 15, row.capturedAmountMinor());
        setNullableLong(ps, 16, row.refundedAmountMinor());
        ps.setString(17, row.cancellationReason());
        ps.setString(18, row.refundReason());
    }

    private static void bindHistory(PreparedStatement ps, PaymentLearningGenerator.HistoryStep step)
            throws SQLException {
        ps.setObject(1, step.id());
        ps.setObject(2, step.paymentId());
        ps.setString(3, step.fromStatus());
        ps.setString(4, step.toStatus());
        ps.setString(5, ACTOR);
        ps.setString(6, ACTOR);
        setTimestamp(ps, 7, step.at());
        ps.setString(8, step.action());
        ps.setString(9, step.idempotencyKeyHash());
        ps.setString(10, step.reason());
        setNullableLong(ps, 11, step.amountMinor());
        ps.setString(12, null);
    }

    private void insertAuditAndPublications() {
        var auditSql = """
                INSERT INTO audit_event (
                    id, occurred_at, actor_subject, actor_display, action,
                    target_type, target_id, tenant_id, correlation_id, outcome,
                    before_state, after_state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), CAST(? AS JSONB))
                """;
        var publicationSql = """
                INSERT INTO event_publication (
                    id, publication_date, listener_id, serialized_event, event_type,
                    completion_date, last_resubmission_date, completion_attempts, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        var audits = new ArrayList<AuditLearningGenerator.AuditRow>(BATCH_SIZE);
        var publications = new ArrayList<PublicationLearningGenerator.PublicationRow>(BATCH_SIZE);
        for (int i = 0; i < AuditLearningGenerator.AUDIT_COUNT; i++) {
            audits.add(AuditLearningGenerator.rowFor(i));
            publications.add(PublicationLearningGenerator.rowFor(i));
            if (audits.size() == BATCH_SIZE) {
                flushAudits(auditSql, audits);
                flushPublications(publicationSql, publications);
            }
        }
        flushAudits(auditSql, audits);
        flushPublications(publicationSql, publications);
    }

    private void flushAudits(String sql, List<AuditLearningGenerator.AuditRow> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            ps.setObject(1, row.id());
            setTimestamp(ps, 2, row.occurredAt());
            ps.setString(3, row.actorSubject());
            ps.setString(4, row.actorDisplay());
            ps.setString(5, row.action());
            ps.setString(6, row.targetType());
            ps.setString(7, row.targetId());
            ps.setString(8, row.tenantId());
            ps.setString(9, row.correlationId());
            ps.setString(10, row.outcome());
            ps.setString(11, row.beforeState());
            ps.setString(12, row.afterState());
        });
        batch.clear();
    }

    private void flushPublications(String sql, List<PublicationLearningGenerator.PublicationRow> batch) {
        if (batch.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(sql, batch, batch.size(), (ps, row) -> {
            ps.setObject(1, row.id());
            setTimestamp(ps, 2, row.publicationDate());
            ps.setString(3, row.listenerId());
            ps.setString(4, row.serializedEvent());
            ps.setString(5, row.eventType());
            setTimestamp(ps, 6, row.completionDate());
            setTimestamp(ps, 7, row.lastResubmissionDate());
            ps.setInt(8, row.completionAttempts());
            ps.setString(9, row.status());
        });
        batch.clear();
    }

    private void seedRlsLabItems() {
        jdbc.update("""
                INSERT INTO rls_lab_item (item_id, tenant_id, label, amount_minor)
                SELECT '00000000-0000-0000-0000-0000000000a1', tenant_id, 'Alpha secret', 100
                FROM tenants WHERE tenant_reference = 'TENANT_ALPHA'
                """);
        jdbc.update("""
                INSERT INTO rls_lab_item (item_id, tenant_id, label, amount_minor)
                SELECT '00000000-0000-0000-0000-0000000000a2', tenant_id, 'Other tenant secret', 200
                FROM tenants WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID'
                """);
        jdbc.update("""
                INSERT INTO rls_lab_item_unprotected (item_id, tenant_id, label, amount_minor)
                SELECT item_id, tenant_id, label, amount_minor FROM rls_lab_item
                """);
    }

    private DataLearningTruth readTruth() {
        return new DataLearningTruth(
                count(Relation.TENANTS),
                count(Relation.MERCHANTS),
                count(Relation.PAYMENT_ORDERS),
                count(Relation.PAYMENT_ORDER_STATUS_HISTORY),
                countWhere("CAPTURED"),
                countWhere("REFUNDED"),
                countWhere("CANCELLED"),
                countWhere("AUTHORIZED"),
                countWhere("EXPIRED"),
                countWhere("CREATED"),
                jdbc.queryForObject("""
                        SELECT COUNT(*) FROM payment_orders po
                        JOIN merchants m ON m.merchant_id = po.merchant_id
                        JOIN tenants t ON t.tenant_id = m.tenant_id
                        WHERE t.tenant_reference = 'TENANT_ALPHA'
                        """, Integer.class),
                count(Relation.CHECKOUT_SESSION),
                count(Relation.CHECKOUT_EVENT),
                count(Relation.CHECKOUT_FULFILLMENT),
                count(Relation.CHECKOUT_ANOMALY),
                count(Relation.AUDIT_EVENT),
                count(Relation.EVENT_PUBLICATION),
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL",
                        Integer.class)
        );
    }

    private int count(Relation relation) {
        return jdbc.queryForObject(relation.countSql, Integer.class);
    }

    private int countWhere(String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders WHERE status = ?",
                Integer.class, status);
    }

    private static void setTimestamp(PreparedStatement ps, int index, Instant instant) throws SQLException {
        if (instant == null) {
            ps.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
            return;
        }
        ps.setObject(index, OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    private static void setNullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
            return;
        }
        ps.setLong(index, value);
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
            return;
        }
        ps.setInt(index, value);
    }

    private enum Relation {
        TENANTS("SELECT COUNT(*) FROM tenants"),
        MERCHANTS("SELECT COUNT(*) FROM merchants"),
        PAYMENT_ORDERS("SELECT COUNT(*) FROM payment_orders"),
        PAYMENT_ORDER_STATUS_HISTORY("SELECT COUNT(*) FROM payment_order_status_history"),
        CHECKOUT_SESSION("SELECT COUNT(*) FROM checkout_session"),
        CHECKOUT_EVENT("SELECT COUNT(*) FROM checkout_event"),
        CHECKOUT_FULFILLMENT("SELECT COUNT(*) FROM checkout_fulfillment"),
        CHECKOUT_ANOMALY("SELECT COUNT(*) FROM checkout_anomaly"),
        AUDIT_EVENT("SELECT COUNT(*) FROM audit_event"),
        EVENT_PUBLICATION("SELECT COUNT(*) FROM event_publication");

        private final String countSql;

        Relation(String countSql) {
            this.countSql = countSql;
        }
    }

    private record PaymentRow(
            UUID paymentOrderId,
            UUID merchantId,
            String reference,
            long amountMinor,
            String currency,
            String status,
            long version,
            Instant createdAt,
            Instant updatedAt,
            Instant authorizedAt,
            Instant expiresAt,
            Instant capturedAt,
            Instant cancelledAt,
            Instant refundedAt,
            Long capturedAmountMinor,
            Long refundedAmountMinor,
            String cancellationReason,
            String refundReason
    ) {
    }
}
