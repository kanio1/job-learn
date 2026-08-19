package lab.paymentquality.payment;

import lab.paymentquality.PaymentQualityApplication;
import lab.paymentquality.merchant.MerchantPaymentEligibilityService;
import lab.paymentquality.payment.internal.application.PaymentOrderService;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderRepository;
import lab.paymentquality.payment.internal.web.PaymentOrderController;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.STANDALONE)
@ActiveProfiles("test")
@Testcontainers
class PaymentModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = startPostgres("payment_module_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @MockitoBean
    MerchantPaymentEligibilityService merchantPaymentEligibilityService;

    @Autowired
    PaymentOrderService paymentOrderService;

    @Autowired
    JpaPaymentOrderRepository paymentOrderRepository;

    @Autowired
    PaymentOrderController paymentOrderController;

    @Test
    void paymentModuleBootsWithCoreBeans() {
        assertThat(paymentOrderService).isNotNull();
        assertThat(paymentOrderRepository).isNotNull();
        assertThat(paymentOrderController).isNotNull();
    }

    @Test
    void applicationModuleArchitectureStillVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }
}
