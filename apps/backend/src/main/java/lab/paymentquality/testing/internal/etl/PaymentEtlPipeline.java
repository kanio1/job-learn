package lab.paymentquality.testing.internal.etl;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Component
@Profile("!prod")
@ConditionalOnProperty(name = "app.testing.enabled", havingValue = "true")
public class PaymentEtlPipeline {

    public static final String PIPELINE_NAME = "payment-etl";

    private static final String INSERT_RUNNING = """
            INSERT INTO learning_etl.batch_run (
                batch_id, pipeline_name, load_type, status, started_at,
                watermark_from, watermark_to
            ) VALUES (?, ?, ?, 'RUNNING', ?, ?, ?)
            """;

    private static final String EXTRACT_BASE = """
            INSERT INTO learning_staging.payment (
                batch_id, source_payment_order_id, source_merchant_id, source_tenant_id,
                source_tenant_reference, client_order_reference, amount_minor, currency, status,
                created_at, updated_at, authorized_at, captured_at, cancelled_at, refunded_at,
                captured_amount_minor, refunded_amount_minor, extracted_at, source_updated_at
            )
            SELECT
                ?, po.payment_order_id, po.merchant_id, m.tenant_id, t.tenant_reference,
                po.client_order_reference, po.amount_minor, po.currency, po.status,
                po.created_at, po.updated_at, po.authorized_at, po.captured_at,
                po.cancelled_at, po.refunded_at,
                po.captured_amount_minor, po.refunded_amount_minor, ?, po.updated_at
            FROM payment_orders po
            JOIN merchants m ON m.merchant_id = po.merchant_id
            JOIN tenants t ON t.tenant_id = m.tenant_id
            """;

    private static final String EXTRACT_INCREMENTAL_PREDICATE =
            " WHERE po.updated_at > ? AND po.updated_at <= ?";

    private static final String LOAD_UPSERT = """
            INSERT INTO learning_dwh.fact_payment (
                payment_order_id, merchant_id, tenant_id, tenant_reference, client_order_reference,
                amount_minor, currency, source_status, captured_amount_minor, refunded_amount_minor,
                amount_major, is_captured, is_refunded, is_cancelled, is_terminal,
                lifecycle_step_count, capture_duration_seconds, source_updated_at, loaded_at, batch_id
            )
            SELECT
                s.source_payment_order_id,
                s.source_merchant_id,
                s.source_tenant_id,
                s.source_tenant_reference,
                s.client_order_reference,
                s.amount_minor,
                s.currency,
                s.status,
                s.captured_amount_minor,
                s.refunded_amount_minor,
                ROUND((s.amount_minor::numeric / 100), 2),
                s.captured_amount_minor IS NOT NULL,
                s.refunded_amount_minor IS NOT NULL,
                s.status = 'CANCELLED',
                s.status IN ('REFUNDED', 'CANCELLED', 'EXPIRED'),
                (SELECT COUNT(*) FROM payment_order_status_history h
                 WHERE h.payment_order_id = s.source_payment_order_id),
                CASE
                    WHEN s.captured_at IS NOT NULL AND s.authorized_at IS NOT NULL
                    THEN EXTRACT(EPOCH FROM (s.captured_at - s.authorized_at))::BIGINT
                    ELSE NULL
                END,
                s.source_updated_at,
                ?,
                s.batch_id
            FROM learning_staging.payment s
            WHERE s.batch_id = ?
            ON CONFLICT (payment_order_id) DO UPDATE SET
                merchant_id = EXCLUDED.merchant_id,
                tenant_id = EXCLUDED.tenant_id,
                tenant_reference = EXCLUDED.tenant_reference,
                client_order_reference = EXCLUDED.client_order_reference,
                amount_minor = EXCLUDED.amount_minor,
                currency = EXCLUDED.currency,
                source_status = EXCLUDED.source_status,
                captured_amount_minor = EXCLUDED.captured_amount_minor,
                refunded_amount_minor = EXCLUDED.refunded_amount_minor,
                amount_major = EXCLUDED.amount_major,
                is_captured = EXCLUDED.is_captured,
                is_refunded = EXCLUDED.is_refunded,
                is_cancelled = EXCLUDED.is_cancelled,
                is_terminal = EXCLUDED.is_terminal,
                lifecycle_step_count = EXCLUDED.lifecycle_step_count,
                capture_duration_seconds = EXCLUDED.capture_duration_seconds,
                source_updated_at = EXCLUDED.source_updated_at,
                loaded_at = EXCLUDED.loaded_at,
                batch_id = EXCLUDED.batch_id
            """;

    private final JdbcTemplate jdbc;
    private final PaymentEtlClock clock;
    private final PaymentEtlFault fault;

    public PaymentEtlPipeline(JdbcTemplate jdbc, PaymentEtlClock clock, PaymentEtlFault fault) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.fault = fault;
    }

    public EtlBatchResult runFull() {
        return run("FULL", null, clock.instant());
    }

    public EtlBatchResult runIncremental() {
        Instant from = lastSucceededWatermarkTo()
                .orElseThrow(IncrementalEtlNotReadyException::new);
        return run("INCREMENTAL", from, clock.instant());
    }

    public EtlBatchResult rebuild() {
        jdbc.update("DELETE FROM learning_dwh.fact_payment");
        jdbc.update("DELETE FROM learning_staging.payment");
        jdbc.update("DELETE FROM learning_etl.batch_run WHERE pipeline_name = ?", PIPELINE_NAME);
        return runFull();
    }

    private EtlBatchResult run(
            String loadType,
            Instant watermarkFrom,
            Instant watermarkTo) {
        UUID batchId = UUID.randomUUID();
        Instant startedAt = clock.instant();
        insertRunning(batchId, loadType, startedAt, watermarkFrom, watermarkTo);
        int sourceRows = 0;
        int stagedRows = 0;
        try {
            sourceRows = countSource(loadType, watermarkFrom, watermarkTo);
            stagedRows = extract(batchId, startedAt, loadType, watermarkFrom, watermarkTo);
            if (fault.consumeAfterStaging()) {
                Instant finishedAt = clock.instant();
                markFinished(batchId, "FAILED", finishedAt, sourceRows, stagedRows, 0, 0,
                        "Forced failure after staging");
                throw new EtlForcedFailureException();
            }
            int loadedRows = load(batchId, startedAt);
            int rejectedRows = 0;
            Instant finishedAt = clock.instant();
            markFinished(batchId, "SUCCEEDED", finishedAt, sourceRows, stagedRows, loadedRows, rejectedRows, null);
            return new EtlBatchResult(
                    batchId, PIPELINE_NAME, loadType, "SUCCEEDED",
                    startedAt, finishedAt, watermarkFrom, watermarkTo,
                    sourceRows, stagedRows, loadedRows, rejectedRows, null);
        } catch (EtlForcedFailureException e) {
            throw e;
        } catch (RuntimeException e) {
            Instant finishedAt = clock.instant();
            markFinished(batchId, "FAILED", finishedAt, sourceRows, stagedRows, 0, 0, e.getMessage());
            throw e;
        }
    }

    private void insertRunning(
            UUID batchId,
            String loadType,
            Instant startedAt,
            Instant watermarkFrom,
            Instant watermarkTo) {
        jdbc.update(INSERT_RUNNING,
                batchId,
                PIPELINE_NAME,
                loadType,
                ts(startedAt),
                ts(watermarkFrom),
                ts(watermarkTo));
    }

    private int extract(
            UUID batchId,
            Instant extractedAt,
            String loadType,
            Instant watermarkFrom,
            Instant watermarkTo) {
        if ("INCREMENTAL".equals(loadType)) {
            return jdbc.update(
                    EXTRACT_BASE + EXTRACT_INCREMENTAL_PREDICATE,
                    batchId, ts(extractedAt), ts(watermarkFrom), ts(watermarkTo));
        }
        return jdbc.update(EXTRACT_BASE, batchId, ts(extractedAt));
    }

    private int load(UUID batchId, Instant loadedAt) {
        return jdbc.update(LOAD_UPSERT, ts(loadedAt), batchId);
    }

    private int countSource(String loadType, Instant watermarkFrom, Instant watermarkTo) {
        if ("INCREMENTAL".equals(loadType)) {
            Integer count = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM payment_orders po
                    JOIN merchants m ON m.merchant_id = po.merchant_id
                    JOIN tenants t ON t.tenant_id = m.tenant_id
                    WHERE po.updated_at > ? AND po.updated_at <= ?
                    """, Integer.class, ts(watermarkFrom), ts(watermarkTo));
            return count == null ? 0 : count;
        }
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM payment_orders po
                JOIN merchants m ON m.merchant_id = po.merchant_id
                JOIN tenants t ON t.tenant_id = m.tenant_id
                """, Integer.class);
        return count == null ? 0 : count;
    }

    private void markFinished(
            UUID batchId,
            String status,
            Instant finishedAt,
            int sourceRows,
            int stagedRows,
            int loadedRows,
            int rejectedRows,
            String errorMessage) {
        jdbc.update("""
                UPDATE learning_etl.batch_run
                SET status = ?, finished_at = ?, source_rows = ?, staged_rows = ?,
                    loaded_rows = ?, rejected_rows = ?, error_message = ?
                WHERE batch_id = ?
                """,
                status, ts(finishedAt), sourceRows, stagedRows, loadedRows, rejectedRows,
                errorMessage, batchId);
    }

    java.util.Optional<Instant> lastSucceededWatermarkTo() {
        List<Instant> rows = jdbc.query("""
                SELECT watermark_to
                FROM learning_etl.batch_run
                WHERE pipeline_name = ? AND status = 'SUCCEEDED'
                ORDER BY finished_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("watermark_to", OffsetDateTime.class).toInstant(),
                PIPELINE_NAME);
        return rows.stream().findFirst();
    }

    private static OffsetDateTime ts(Instant instant) {
        if (instant == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
