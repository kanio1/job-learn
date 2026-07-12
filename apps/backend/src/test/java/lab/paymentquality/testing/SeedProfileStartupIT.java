package lab.paymentquality.testing;

import lab.paymentquality.testing.internal.seed.DeterministicDataset;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the seed profile activates SeedRunner on startup, which populates
 * the DB with the deterministic dataset before any test method runs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "seed"})
@Import(TestJwtConfiguration.class)
@Testcontainers
class SeedProfileStartupIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("seed_startup_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    DeterministicDataset dataset;

    @Test
    void seedRunnerPopulatedTenantsOnStartup() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void seedRunnerPopulatedMerchantsOnStartup() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @Test
    void seedRunnerPopulated104PaymentOrdersOnStartup() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class);
        assertThat(count).isEqualTo(104);
    }

    @Test
    void seedRunnerPopulated104StatusHistoryRowsOnStartup() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM payment_order_status_history", Integer.class);
        assertThat(count).isEqualTo(104);
    }

    @Test
    void deterministicTenantUuidsArePresent() {
        assertTenantExists("00000000-0000-0000-0000-0000000000a1");
        assertTenantExists("00000000-0000-0000-0000-0000000000a2");
        assertTenantExists("00000000-0000-0000-0000-0000000000a3");
    }

    @Test
    void deterministicMerchantUuidsArePresent() {
        assertMerchantExists("00000000-0000-0000-0000-0000000000b1");
        assertMerchantExists("00000000-0000-0000-0000-0000000000b2");
        assertMerchantExists("00000000-0000-0000-0000-0000000000b3");
        assertMerchantExists("33333333-3333-3333-3333-333333333333");
    }

    @Test
    void merchantAlpha001HasExactly101PaymentOrders() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders WHERE merchant_id = ?",
                Integer.class,
                UUID.fromString("00000000-0000-0000-0000-0000000000b1"));
        assertThat(count).isEqualTo(101);
    }

    @Test
    void reSeedingProducesSameState() {
        dataset.seed();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tenants", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM merchants", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_orders", Integer.class)).isEqualTo(104);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_order_status_history", Integer.class)).isEqualTo(104);
    }

    private void assertTenantExists(String uuid) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenants WHERE tenant_id = ?",
                Integer.class, UUID.fromString(uuid));
        assertThat(count).as("Tenant %s must exist", uuid).isEqualTo(1);
    }

    private void assertMerchantExists(String uuid) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM merchants WHERE merchant_id = ?",
                Integer.class, UUID.fromString(uuid));
        assertThat(count).as("Merchant %s must exist", uuid).isEqualTo(1);
    }
}
