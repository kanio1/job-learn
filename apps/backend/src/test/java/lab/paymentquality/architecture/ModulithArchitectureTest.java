package lab.paymentquality.architecture;

import lab.paymentquality.PaymentQualityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTest {

    @Test
    void verifiesApplicationModuleBoundaries() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }
}
