package lab.paymentquality.support;

import lab.paymentquality.PaymentQualityApplication;
import lab.paymentquality.merchant.PaymentOrderSearch;
import lab.paymentquality.support.internal.application.SupportCaseService;
import lab.paymentquality.support.internal.infrastructure.JpaSupportCaseRepository;
import lab.paymentquality.support.internal.web.SupportCaseController;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ActiveProfiles("test")
@Testcontainers
class SupportModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = startPostgres("support_module_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @MockitoBean
    PaymentOrderSearch paymentOrderSearch;

    @Autowired
    SupportCaseService supportCaseService;

    @Autowired
    JpaSupportCaseRepository repository;

    @Autowired
    SupportCaseController controller;

    @Test
    void supportModuleBootsWithCoreBeans() {
        assertThat(supportCaseService).isNotNull();
        assertThat(repository).isNotNull();
        assertThat(controller).isNotNull();
    }

    @Test
    @DisplayName("RA-OPS-122 Modulith verify + SupportModuleTest")
    void applicationModuleArchitectureStillVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }
}
