package lab.paymentquality.rlslab;

import lab.paymentquality.PaymentQualityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class RlsLabModuleTest {

    @Test
    void applicationModuleArchitectureVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }

    @Test
    void rlsLabModuleExistsInApplicationModuleGraph() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);
        assertThat(modules.getModuleByName("rlslab")).isPresent();
    }

    @Test
    void rlsLabModuleHasNoExplicitPublicNamedInterfaces() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);
        ApplicationModule rlsLabModule = modules.getModuleByName("rlslab").orElseThrow();
        assertThat(rlsLabModule.getNamedInterfaces().hasExplicitInterfaces()).isFalse();
    }

    @Test
    void noDomainModuleDependsOnRlsLab() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);

        modules.stream()
                .filter(module -> !module.getName().equals("rlslab"))
                .forEach(module -> assertThat(module.getDependencies(modules).containsModuleNamed("rlslab"))
                        .as("Module '%s' must not depend on rlslab", module.getName())
                        .isFalse());
    }
}
