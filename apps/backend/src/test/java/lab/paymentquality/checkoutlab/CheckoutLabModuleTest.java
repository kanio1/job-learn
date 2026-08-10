package lab.paymentquality.checkoutlab;

import lab.paymentquality.PaymentQualityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutLabModuleTest {

    @Test
    void applicationModuleArchitectureVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }

    @Test
    void checkoutLabModuleExistsInApplicationModuleGraph() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);
        assertThat(modules.getModuleByName("checkoutlab")).isPresent();
    }

    @Test
    void checkoutLabModuleHasNoExplicitPublicNamedInterfaces() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);
        ApplicationModule checkoutLabModule = modules.getModuleByName("checkoutlab").orElseThrow();
        assertThat(checkoutLabModule.getNamedInterfaces().hasExplicitInterfaces()).isFalse();
    }

    @Test
    void noDomainModuleDependsOnCheckoutLab() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);

        modules.stream()
                .filter(module -> !module.getName().equals("checkoutlab"))
                .forEach(module -> assertThat(module.getDependencies(modules).containsModuleNamed("checkoutlab"))
                        .as("Module '%s' must not depend on checkoutlab", module.getName())
                        .isFalse());
    }
}
