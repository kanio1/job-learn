package lab.paymentquality.merchant;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MerchantPersistenceIT extends PostgresContainerSupport {

    private static UUID durableMerchantId;
    private static String durableReference;

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("merchant_persistence_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    MerchantService merchantService;

    @Autowired
    JpaMerchantRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    void flywayMigrationCreatedMerchantsTableAndSchemaHistory() {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'merchants'
                """, Integer.class);
        Integer historyCount = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
                """, Integer.class);

        assertThat(tableCount).isEqualTo(1);
        assertThat(historyCount).isEqualTo(1);
    }

    @Test
    @Order(2)
    void createRetrieveActivateSuspendAndListAgainstPostgresql18() {
        var merchant = merchantService.create("MERCH-IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), "Integration Merchant");

        var active = merchantService.activate(merchant.getMerchantId());
        var suspended = merchantService.suspend(merchant.getMerchantId());

        assertThat(active.status()).isEqualTo("ACTIVE");
        assertThat(suspended.status()).isEqualTo("SUSPENDED");
        assertThat(repository.findById(merchant.getMerchantId())).isPresent();
        assertThat(merchantService.listFirstPage()).extracting("merchantId").contains(merchant.getMerchantId());
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void createActiveMerchantBeforeContextRestart() {
        durableReference = "MERCH-DURABLE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var merchant = merchantService.create(durableReference, "Durable Merchant");
        var active = merchantService.activate(merchant.getMerchantId());
        durableMerchantId = active.merchantId();

        assertThat(active.status()).isEqualTo("ACTIVE");
    }

    @Test
    @Order(4)
    void activeMerchantPersistsAfterContextRestart() {
        assertThat(durableMerchantId).isNotNull();

        var merchant = merchantService.findById(durableMerchantId);

        assertThat(merchant.status()).isEqualTo("ACTIVE");
        assertThat(merchant.merchantReference()).isEqualTo(durableReference);
        assertThat(merchant.displayName()).isEqualTo("Durable Merchant");
    }
}
