package lab.paymentquality.ops;

import lab.paymentquality.PaymentQualityApplication;
import lab.paymentquality.ops.internal.application.OpsFeedService;
import lab.paymentquality.ops.internal.application.OpsNotificationService;
import lab.paymentquality.ops.internal.web.OpsFeedController;
import lab.paymentquality.ops.internal.web.OpsNotificationController;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class OpsModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = startPostgres("ops_module_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    OpsFeedPublisher opsFeedPublisher;

    @Autowired
    OpsFeedService opsFeedService;

    @Autowired
    OpsFeedController controller;

    @Autowired
    OpsNotificationService notificationService;

    @Autowired
    OpsNotificationController notificationController;

    @Test
    void opsModuleBootsWithPublicPublisher() {
        assertThat(opsFeedPublisher).isNotNull();
        assertThat(opsFeedService).isNotNull();
        assertThat(controller).isNotNull();
        assertThat(notificationService).isNotNull();
        assertThat(notificationController).isNotNull();
    }

    @Test
    @DisplayName("Modulith verify + OpsModuleTest")
    void applicationModuleArchitectureStillVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }

    @Test
    void opsModuleDoesNotImportPaymentInternal() throws Exception {
        Path root = Path.of("src/main/java/lab/paymentquality/ops");
        StringBuilder source = new StringBuilder();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList()) {
                source.append(Files.readString(file));
            }
        }
        assertThat(source).doesNotContain("lab.paymentquality.payment.internal");
    }
}
