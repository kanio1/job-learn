package lab.paymentquality.testing.internal.etl;

import io.restassured.RestAssured;
import lab.paymentquality.testing.internal.seed.DataLearningDataset;
import lab.paymentquality.testing.internal.seed.DataLearningProfile;
import lab.paymentquality.testing.internal.seed.DataLearningTruth;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.BeforeEach;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"app.testing.enabled=true"})
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class PaymentEtlIT extends PostgresContainerSupport {

    private static final Instant T0 = Instant.parse("2026-08-17T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-08-17T13:00:00Z");

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_etl_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @LocalServerPort
    int port;

    @Autowired
    DataLearningDataset dataset;

    @Autowired
    PaymentEtlPipeline pipeline;

    @Autowired
    LearningPaymentTouch touch;

    @Autowired
    PaymentEtlClock etlClock;

    @Autowired
    PaymentEtlFault etlFault;

    @Autowired
    JdbcTemplate jdbc;

    private static final String[] MUTATION_REFS = {
            "LEARN-PAY-000003",
            "LEARN-PAY-000004",
            "LEARN-PAY-000005",
            "LEARN-PAY-000010",
            "LEARN-PAY-000011"
    };

    private static DataLearningTruth truth;

    @BeforeEach
    void seedOnceThenWipeWarehouse() {
        if (truth == null) {
            truth = dataset.seed(DataLearningProfile.SMALL);
        }
        restoreMutationRefs();
        jdbc.update("DELETE FROM learning_dwh.fact_payment");
        jdbc.update("DELETE FROM learning_staging.payment");
        jdbc.update("DELETE FROM learning_etl.batch_run");
        etlFault.disarm();
        etlClock.setFixed(T0);
    }

    @Test
    void fullLoadCreatesExactlyTenThousandFacts() {
        EtlBatchResult result = pipeline.runFull();

        assertThat(result.loadType()).isEqualTo("FULL");
        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.sourceRows()).isEqualTo(10_000);
        assertThat(result.stagedRows()).isEqualTo(10_000);
        assertThat(result.loadedRows()).isEqualTo(10_000);
        assertThat(result.rejectedRows()).isZero();
        assertThat(result.watermarkFrom()).isNull();
        assertThat(result.watermarkTo()).isEqualTo(T0);
        assertThat(count("SELECT COUNT(*) FROM learning_staging.payment WHERE batch_id = ?", result.batchId()))
                .isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
        assertThat(jdbc.queryForObject("""
                SELECT status FROM learning_etl.batch_run WHERE batch_id = ?
                """, String.class, result.batchId())).isEqualTo("SUCCEEDED");
        assertThat(jdbc.queryForObject("""
                SELECT load_type FROM learning_etl.batch_run WHERE batch_id = ?
                """, String.class, result.batchId())).isEqualTo("FULL");
    }

    @Test
    void sourceAndTargetTotalsReconcile() {
        pipeline.runFull();

        Map<String, Object> counts = jdbc.queryForMap(sql("01-count-recon.sql"));
        assertThat(((Number) counts.get("source_count")).intValue()).isEqualTo(10_000);
        assertThat(((Number) counts.get("target_count")).intValue()).isEqualTo(10_000);

        assertThat(jdbc.queryForList(sql("02-aggregate-recon.sql"))).isEmpty();

        Map<String, Integer> classes = recordClasses();
        assertThat(classes.getOrDefault("MATCH", 0)).isEqualTo(10_000);
        assertThat(classes.getOrDefault("MISSING_SOURCE", 0)).isZero();
        assertThat(classes.getOrDefault("MISSING_TARGET", 0)).isZero();
        assertThat(classes.getOrDefault("VALUE_MISMATCH", 0)).isZero();
        assertThat(jdbc.queryForList(sql("03b-duplicate-recon.sql"))).isEmpty();

        long sourceAmount = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_minor), 0)::bigint FROM payment_orders", Long.class);
        long sourceCaptured = jdbc.queryForObject(
                "SELECT COALESCE(SUM(captured_amount_minor), 0)::bigint FROM payment_orders", Long.class);
        long sourceRefunded = jdbc.queryForObject(
                "SELECT COALESCE(SUM(refunded_amount_minor), 0)::bigint FROM payment_orders", Long.class);
        long factAmount = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_minor), 0)::bigint FROM learning_dwh.fact_payment", Long.class);
        long factCaptured = jdbc.queryForObject(
                "SELECT COALESCE(SUM(captured_amount_minor), 0)::bigint FROM learning_dwh.fact_payment", Long.class);
        long factRefunded = jdbc.queryForObject(
                "SELECT COALESCE(SUM(refunded_amount_minor), 0)::bigint FROM learning_dwh.fact_payment", Long.class);
        assertThat(factAmount).isEqualTo(sourceAmount);
        assertThat(factCaptured).isEqualTo(sourceCaptured);
        assertThat(factRefunded).isEqualTo(sourceRefunded);
        assertThat(factAmount).isEqualTo(59_995_000L);
        assertThat(factAmount).isEqualTo(truth.totalAmountMinor());
        assertThat(factCaptured).isEqualTo(truth.totalCapturedAmountMinor());
        assertThat(factRefunded).isEqualTo(truth.totalRefundedAmountMinor());
    }

    @Test
    void fullLoadRerunIsIdempotent() {
        pipeline.runFull();
        long amountBefore = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_minor), 0)::bigint FROM learning_dwh.fact_payment", Long.class);

        EtlBatchResult second = pipeline.runFull();

        assertThat(second.stagedRows()).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(DISTINCT payment_order_id) FROM learning_dwh.fact_payment"))
                .isEqualTo(10_000);
        assertThat(count("SELECT COUNT(*) FROM learning_staging.payment")).isEqualTo(20_000);
        long amountAfter = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount_minor), 0)::bigint FROM learning_dwh.fact_payment", Long.class);
        assertThat(amountAfter).isEqualTo(amountBefore);
        assertThat(jdbc.queryForList(sql("03b-duplicate-recon.sql"))).isEmpty();
    }

    @Test
    void incrementalRunProcessesOnlyChangedRecords() {
        pipeline.runFull();
        long original0003 = amountOf("LEARN-PAY-000003");

        touch.touch(T1, "LEARN-PAY-000003", "LEARN-PAY-000004", "LEARN-PAY-000005");
        etlClock.setFixed(T1);
        EtlBatchResult incremental = pipeline.runIncremental();

        assertThat(incremental.loadType()).isEqualTo("INCREMENTAL");
        assertThat(incremental.status()).isEqualTo("SUCCEEDED");
        assertThat(incremental.watermarkFrom()).isEqualTo(T0);
        assertThat(incremental.watermarkTo()).isEqualTo(T1);
        assertThat(incremental.sourceRows()).isEqualTo(3);
        assertThat(incremental.stagedRows()).isEqualTo(3);
        assertThat(incremental.loadedRows()).isEqualTo(3);
        assertThat(incremental.rejectedRows()).isZero();
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
        assertThat(amountOf("LEARN-PAY-000003")).isEqualTo(original0003 + 1);
        assertThat(factAmount("LEARN-PAY-000003")).isEqualTo(original0003 + 1);
        assertThat(factAmount("LEARN-PAY-000004")).isEqualTo(amountOf("LEARN-PAY-000004"));
        assertThat(factAmount("LEARN-PAY-000005")).isEqualTo(amountOf("LEARN-PAY-000005"));
    }

    @Test
    void watermarkBoundaryUsesGreaterThanFromAndLessOrEqualTo() {
        pipeline.runFull();
        long original10 = amountOf("LEARN-PAY-000010");
        long original11 = amountOf("LEARN-PAY-000011");
        UUID fullBatch = jdbc.queryForObject(
                "SELECT batch_id FROM learning_dwh.fact_payment WHERE client_order_reference = 'LEARN-PAY-000010'",
                UUID.class);

        touch.touch(T0, "LEARN-PAY-000010");
        touch.touch(T1, "LEARN-PAY-000011");

        etlClock.setFixed(T1);
        EtlBatchResult incremental = pipeline.runIncremental();

        assertThat(incremental.stagedRows()).isEqualTo(1);
        assertThat(incremental.loadedRows()).isEqualTo(1);
        assertThat(factAmount("LEARN-PAY-000010")).isEqualTo(original10);
        assertThat(factAmount("LEARN-PAY-000011")).isEqualTo(original11 + 1);
        UUID after10 = jdbc.queryForObject(
                "SELECT batch_id FROM learning_dwh.fact_payment WHERE client_order_reference = 'LEARN-PAY-000010'",
                UUID.class);
        UUID after11 = jdbc.queryForObject(
                "SELECT batch_id FROM learning_dwh.fact_payment WHERE client_order_reference = 'LEARN-PAY-000011'",
                UUID.class);
        assertThat(after10).isEqualTo(fullBatch);
        assertThat(after11).isEqualTo(incremental.batchId());
    }

    @Test
    void transformationsMatchRepresentativeStatuses() {
        pipeline.runFull();

        assertFact("LEARN-PAY-000000", "CAPTURED", "10.00", true, false, false, false, 3, 180L);
        assertFact("LEARN-PAY-000060", "REFUNDED", "10.60", true, true, false, true, 4, 180L);
        assertFact("LEARN-PAY-000072", "CANCELLED", "10.72", false, false, true, true, 2, null);
        assertFact("LEARN-PAY-000092", "CREATED", "10.92", false, false, false, false, 1, null);
    }

    @Test
    void recordLevelReconciliationDetectsControlledTargetDefects() {
        pipeline.runFull();
        executeStatements(sql("04-inject-target-defects.sql"));

        Map<String, Integer> classes = recordClasses();
        assertThat(classes.get("MISSING_TARGET")).isEqualTo(1);
        assertThat(classes.get("VALUE_MISMATCH")).isEqualTo(2);
        assertThat(classes.get("MATCH")).isEqualTo(9_997);

        EtlBatchResult rebuilt = pipeline.rebuild();
        assertThat(rebuilt.status()).isEqualTo("SUCCEEDED");
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
        Map<String, Integer> after = recordClasses();
        assertThat(after.getOrDefault("MATCH", 0)).isEqualTo(10_000);
        assertThat(after.getOrDefault("MISSING_TARGET", 0)).isZero();
        assertThat(after.getOrDefault("VALUE_MISMATCH", 0)).isZero();
    }

    @Test
    void failedRunDoesNotDuplicateFactsAndRerunSucceeds() {
        etlFault.armAfterStaging();
        assertThatThrownBy(() -> pipeline.runFull())
                .isInstanceOf(EtlForcedFailureException.class);

        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isZero();
        assertThat(count("SELECT COUNT(*) FROM learning_staging.payment")).isEqualTo(10_000);
        Map<String, Object> failed = jdbc.queryForMap("""
                SELECT status, source_rows, staged_rows, loaded_rows, rejected_rows
                FROM learning_etl.batch_run
                ORDER BY started_at DESC
                LIMIT 1
                """);
        assertThat(failed.get("status")).isEqualTo("FAILED");
        assertThat(((Number) failed.get("source_rows")).intValue()).isEqualTo(10_000);
        assertThat(((Number) failed.get("staged_rows")).intValue()).isEqualTo(10_000);
        assertThat(((Number) failed.get("loaded_rows")).intValue()).isZero();
        assertThat(((Number) failed.get("rejected_rows")).intValue()).isZero();

        EtlBatchResult retry = pipeline.runFull();
        assertThat(retry.status()).isEqualTo("SUCCEEDED");
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
        assertThat(count("SELECT COUNT(DISTINCT payment_order_id) FROM learning_dwh.fact_payment"))
                .isEqualTo(10_000);
    }

    @Test
    void failedRunDoesNotAdvanceWatermarkForNextIncremental() {
        pipeline.runFull();
        etlClock.setFixed(T1);
        etlFault.armAfterStaging();
        assertThatThrownBy(() -> pipeline.runFull())
                .isInstanceOf(EtlForcedFailureException.class);

        assertThat(pipeline.lastSucceededWatermarkTo()).contains(T0);

        touch.touch(T1);
        etlClock.setFixed(T1);
        EtlBatchResult incremental = pipeline.runIncremental();

        assertThat(incremental.watermarkFrom()).isEqualTo(T0);
        assertThat(incremental.watermarkTo()).isEqualTo(T1);
        assertThat(incremental.stagedRows()).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
    }

    @Test
    void incrementalWithoutPriorSuccessFailsClearly() {
        assertThatThrownBy(() -> pipeline.runIncremental())
                .isInstanceOf(IncrementalEtlNotReadyException.class);
        assertThat(count("SELECT COUNT(*) FROM learning_etl.batch_run")).isZero();
    }

    @Test
    void postFullLoadReturns200AfterLearningSeed() {
        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/full")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("operation", equalTo("etl-payments-full"))
                .body("status", equalTo("completed"))
                .body("batch.loadType", equalTo("FULL"))
                .body("batch.status", equalTo("SUCCEEDED"))
                .body("batch.sourceRows", equalTo(10_000))
                .body("batch.stagedRows", equalTo(10_000))
                .body("batch.loadedRows", equalTo(10_000))
                .body("batch.rejectedRows", equalTo(0));
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
    }

    @Test
    void postIncrementalWithoutPriorSuccessReturns409() {
        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/incremental")
                .then()
                .statusCode(409)
                .contentType("application/problem+json")
                .body("status", equalTo(409))
                .body("title", equalTo("Conflict"))
                .body("error", equalTo("conflict"))
                .body("code", equalTo("CONFLICT"))
                .body("detail", equalTo("Incremental payment ETL requires a prior SUCCEEDED run"))
                .header("X-Correlation-ID", notNullValue());
    }

    @Test
    void postIncrementalAfterFullLoadReturns200ForTouchedRows() {
        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/full")
                .then()
                .statusCode(200)
                .body("batch.status", equalTo("SUCCEEDED"));

        touch.touch(T1);
        etlClock.setFixed(T1);

        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/incremental")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("operation", equalTo("etl-payments-incremental"))
                .body("status", equalTo("completed"))
                .body("batch.loadType", equalTo("INCREMENTAL"))
                .body("batch.status", equalTo("SUCCEEDED"))
                .body("batch.sourceRows", equalTo(3))
                .body("batch.stagedRows", equalTo(3))
                .body("batch.loadedRows", equalTo(3));
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
    }

    @Test
    void postRebuildReturns200AndRestoresTenThousandFacts() {
        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/full")
                .then()
                .statusCode(200);
        executeStatements(sql("04-inject-target-defects.sql"));

        RestAssured.given().port(port)
                .when().post("/api/test/etl/payments/rebuild")
                .then()
                .statusCode(200)
                .header("X-Correlation-ID", notNullValue())
                .body("operation", equalTo("etl-payments-rebuild"))
                .body("status", equalTo("completed"))
                .body("batch.loadType", equalTo("FULL"))
                .body("batch.status", equalTo("SUCCEEDED"))
                .body("batch.loadedRows", equalTo(10_000));
        assertThat(count("SELECT COUNT(*) FROM learning_dwh.fact_payment")).isEqualTo(10_000);
        assertThat(recordClasses().getOrDefault("MATCH", 0)).isEqualTo(10_000);
    }

    private void assertFact(
            String reference,
            String status,
            String amountMajor,
            boolean captured,
            boolean refunded,
            boolean cancelled,
            boolean terminal,
            int steps,
            Long captureSeconds) {
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT source_status, amount_minor, amount_major, is_captured, is_refunded,
                       is_cancelled, is_terminal, lifecycle_step_count, capture_duration_seconds
                FROM learning_dwh.fact_payment
                WHERE client_order_reference = ?
                """, reference);
        assertThat(row.get("source_status")).isEqualTo(status);
        assertThat((Boolean) row.get("is_captured")).isEqualTo(captured);
        assertThat((Boolean) row.get("is_refunded")).isEqualTo(refunded);
        assertThat((Boolean) row.get("is_cancelled")).isEqualTo(cancelled);
        assertThat((Boolean) row.get("is_terminal")).isEqualTo(terminal);
        assertThat(((Number) row.get("lifecycle_step_count")).intValue()).isEqualTo(steps);
        BigDecimal major = (BigDecimal) row.get("amount_major");
        assertThat(major).isEqualByComparingTo(amountMajor);
        if (captureSeconds == null) {
            assertThat(row.get("capture_duration_seconds")).isNull();
        } else {
            assertThat(((Number) row.get("capture_duration_seconds")).longValue()).isEqualTo(captureSeconds);
        }
    }

    private Map<String, Integer> recordClasses() {
        return jdbc.query(sql("03-record-recon.sql"), rs -> {
            Map<String, Integer> map = new java.util.HashMap<>();
            while (rs.next()) {
                map.put(rs.getString("result_class"), rs.getInt("row_count"));
            }
            return map;
        });
    }

    private int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private long amountOf(String reference) {
        return jdbc.queryForObject(
                "SELECT amount_minor FROM payment_orders WHERE client_order_reference = ?",
                Long.class, reference);
    }

    private long factAmount(String reference) {
        return jdbc.queryForObject(
                "SELECT amount_minor FROM learning_dwh.fact_payment WHERE client_order_reference = ?",
                Long.class, reference);
    }

    private static String sql(String name) {
        Path[] candidates = {
                Path.of("../../docs/data-learning/etl-migration/sql", name),
                Path.of("docs/data-learning/etl-migration/sql", name),
                Path.of("../docs/data-learning/etl-migration/sql", name)
        };
        for (Path candidate : candidates) {
            Path resolved = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(resolved)) {
                try {
                    return Files.readString(resolved, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        throw new IllegalStateException("Missing learning ETL SQL: " + name);
    }

    private void restoreMutationRefs() {
        for (String reference : MUTATION_REFS) {
            int index = Integer.parseInt(reference.substring("LEARN-PAY-".length()));
            long amount = 1_000L + (index % 50_000);
            jdbc.update("""
                    UPDATE payment_orders po
                    SET amount_minor = ?,
                        updated_at = (
                            SELECT MAX(h.created_at)
                            FROM payment_order_status_history h
                            WHERE h.payment_order_id = po.payment_order_id
                        )
                    WHERE po.client_order_reference = ?
                    """, amount, reference);
        }
    }

    private void executeStatements(String script) {
        for (String statement : script.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                jdbc.execute(trimmed);
            }
        }
    }
}
