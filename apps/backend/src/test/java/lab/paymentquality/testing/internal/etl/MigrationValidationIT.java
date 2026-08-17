package lab.paymentquality.testing.internal.etl;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfiguration.class)
@Testcontainers
class MigrationValidationIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("migration_validation_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void merchantTenantIdMigrationInvariantsHold() {
        int merchants = jdbc.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class);
        int nullTenant = jdbc.queryForObject(
                "SELECT COUNT(*) FROM merchants WHERE tenant_id IS NULL", Integer.class);
        int orphans = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM merchants m
                LEFT JOIN tenants t ON t.tenant_id = m.tenant_id
                WHERE t.tenant_id IS NULL
                """, Integer.class);
        int fk = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = 'public'
                  AND table_name = 'merchants'
                  AND constraint_name = 'fk_merchants_tenant_id'
                  AND constraint_type = 'FOREIGN KEY'
                """, Integer.class);
        int index = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'idx_merchants_tenant_id'
                """, Integer.class);

        assertThat(nullTenant).isZero();
        assertThat(orphans).isZero();
        assertThat(fk).isEqualTo(1);
        assertThat(index).isEqualTo(1);
        assertThat(merchants).isGreaterThanOrEqualTo(0);
    }

    @Test
    void checkoutAnomalyUniqueIndexHoldsAndDedupExerciseKeepsOneSurvivor() {
        int liveDuplicates = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT session_id, kind
                    FROM checkout_anomaly
                    WHERE session_id IS NOT NULL
                    GROUP BY session_id, kind
                    HAVING COUNT(*) > 1
                ) d
                """, Integer.class);
        int uniqueIndex = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'uk_checkout_anomaly_session_kind'
                """, Integer.class);
        assertThat(liveDuplicates).isZero();
        assertThat(uniqueIndex).isEqualTo(1);

        jdbc.execute("""
                CREATE TEMP TABLE checkout_anomaly_lab (
                    anomaly_id UUID PRIMARY KEY,
                    session_id UUID,
                    kind VARCHAR(64) NOT NULL
                )
                """);
        jdbc.update("""
                INSERT INTO checkout_anomaly_lab VALUES
                    ('00000000-0000-0000-0000-00000000aa01', '00000000-0000-0000-0000-00000000bb01', 'MISSING_FULFILLMENT'),
                    ('00000000-0000-0000-0000-00000000aa02', '00000000-0000-0000-0000-00000000bb01', 'MISSING_FULFILLMENT'),
                    ('00000000-0000-0000-0000-00000000aa03', '00000000-0000-0000-0000-00000000bb02', 'WRONG_AMOUNT')
                """);
        int duplicatesBefore = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT session_id, kind
                    FROM checkout_anomaly_lab
                    WHERE session_id IS NOT NULL
                    GROUP BY session_id, kind
                    HAVING COUNT(*) > 1
                ) d
                """, Integer.class);
        assertThat(duplicatesBefore).isEqualTo(1);

        jdbc.update("""
                DELETE FROM checkout_anomaly_lab a
                USING checkout_anomaly_lab b
                WHERE a.anomaly_id > b.anomaly_id
                  AND a.session_id IS NOT NULL
                  AND a.session_id = b.session_id
                  AND a.kind = b.kind
                """);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM checkout_anomaly_lab", Integer.class))
                .isEqualTo(2);
        int duplicatesAfter = jdbc.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT session_id, kind
                    FROM checkout_anomaly_lab
                    WHERE session_id IS NOT NULL
                    GROUP BY session_id, kind
                    HAVING COUNT(*) > 1
                ) d
                """, Integer.class);
        assertThat(duplicatesAfter).isZero();
    }

    @Test
    void rlsMigrationRuntimeContractIsTenantGucAndBypassRole() {
        Boolean forced = jdbc.queryForObject("""
                SELECT c.relforcerowsecurity
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = 'public' AND c.relname = 'rls_lab_item'
                """, Boolean.class);
        assertThat(forced).isTrue();

        String usingExpr = jdbc.queryForObject("""
                SELECT pg_get_expr(polqual, polrelid)
                FROM pg_policy
                WHERE polrelid = 'public.rls_lab_item'::regclass
                """, String.class);
        assertThat(usingExpr).contains("app.tenant_id");
        assertThat(usingExpr).doesNotContain("app.rls_bypass");

        List<Map<String, Object>> roles = jdbc.queryForList("""
                SELECT rolname, rolbypassrls
                FROM pg_roles
                WHERE rolname IN ('rls_lab_app', 'rls_lab_bypass')
                ORDER BY rolname
                """);
        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).get("rolname")).isEqualTo("rls_lab_app");
        assertThat((Boolean) roles.get(0).get("rolbypassrls")).isFalse();
        assertThat(roles.get(1).get("rolname")).isEqualTo("rls_lab_bypass");
        assertThat((Boolean) roles.get(1).get("rolbypassrls")).isTrue();
    }
}
