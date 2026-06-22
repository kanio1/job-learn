package lab.paymentquality.testing;

import lab.paymentquality.PaymentQualityApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

import static org.assertj.core.api.Assertions.assertThat;

class TestingModuleTest {

    @Test
    void applicationModuleArchitectureVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }

    @Test
    void testingModuleExistsInApplicationModuleGraph() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);
        assertThat(modules.getModuleByName("testing")).isPresent();
    }

    @Test
    void testingModuleHasNoExplicitPublicNamedInterfaces() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);
        ApplicationModule testingModule = modules.getModuleByName("testing").orElseThrow();
        assertThat(testingModule.getNamedInterfaces().hasExplicitInterfaces()).isFalse();
    }

    @Test
    void noDomainModuleDependsOnTesting() {
        ApplicationModules modules = ApplicationModules.of(PaymentQualityApplication.class);

        modules.stream()
                .filter(m -> !m.getName().equals("testing"))
                .forEach(m -> assertThat(m.getDependencies(modules).containsModuleNamed("testing"))
                        .as("Module '%s' must not depend on testing", m.getName())
                        .isFalse());
    }
}
