package lab.paymentquality.merchant;

import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class MerchantListQueryIndexesIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_list_query_indexes_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayV23CreatesMerchantListQueryIndexes() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE script = 'V23__merchant_list_query_indexes.sql'
                """, Integer.class)).isEqualTo(1);

        assertThat(merchantIndexNames()).contains(
                "idx_merchants_tenant_status_updated",
                "idx_merchants_tenant_updated",
                "idx_merchants_normalized_reference_lower",
                "idx_merchants_display_name_lower",
                "idx_merchants_status",
                "idx_merchants_created_at",
                "idx_merchants_tenant_id");
    }

    private List<String> merchantIndexNames() {
        return jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE schemaname = 'public' AND tablename = 'merchants'
                """, String.class);
    }
}
