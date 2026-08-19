package lab.paymentquality.merchant;

import lab.paymentquality.PaymentQualityApplication;
import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.merchant.internal.web.MerchantController;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

// Merchant depends on the tenant module public API for Wave 2 tenant-aware service behavior.
@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ActiveProfiles("test")
@Testcontainers
class MerchantModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = startPostgres("merchant_module_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    MerchantService merchantService;

    @Autowired
    JpaMerchantRepository repository;

    @Autowired
    MerchantController controller;

    @Test
    void merchantModuleBootsWithCoreBeans() {
        assertThat(merchantService).isNotNull();
        assertThat(repository).isNotNull();
        assertThat(controller).isNotNull();
    }

    @Test
    void applicationModuleArchitectureStillVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }
}
