package lab.paymentquality.payment;

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
class PaymentOrderListAmountIndexesIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("payment_list_amount_indexes_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayV24CreatesPaymentOrderListAmountIndexes() {
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE script = 'V24__payment_order_list_amount_index.sql'
                """, Integer.class)).isEqualTo(1);

        assertThat(paymentOrderIndexNames()).contains(
                "idx_payment_orders_merchant_amount",
                "idx_payment_orders_merchant_status_created");
    }

    private List<String> paymentOrderIndexNames() {
        return jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE schemaname = 'public' AND tablename = 'payment_orders'
                """, String.class);
    }
}
