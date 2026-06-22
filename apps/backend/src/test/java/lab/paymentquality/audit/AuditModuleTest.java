package lab.paymentquality.audit;

import lab.paymentquality.PaymentQualityApplication;
import lab.paymentquality.audit.internal.application.AuditEventService;
import lab.paymentquality.audit.internal.infrastructure.JpaAuditEventRepository;
import lab.paymentquality.audit.internal.web.AuditController;
import lab.paymentquality.testsupport.PostgresContainerSupport;
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
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class AuditModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("audit_module_test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    AuditEventService service;

    @Autowired
    JpaAuditEventRepository repository;

    @Autowired
    AuditController controller;

    @Test
    void auditModuleBootsWithInternalBeans() {
        assertThat(service).isNotNull();
        assertThat(repository).isNotNull();
        assertThat(controller).isNotNull();
    }

    @Test
    void applicationModuleArchitectureStillVerifies() {
        assertThatNoException().isThrownBy(
                () -> ApplicationModules.of(PaymentQualityApplication.class).verify());
    }

    @Test
    void auditModuleExposesNoJavaTypeFromItsRootPackage() throws Exception {
        Path root = Path.of("src/main/java/lab/paymentquality/audit");
        try (Stream<Path> files = Files.list(root)) {
            assertThat(files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".java")))
                    .containsExactly("package-info.java");
        }
    }

    @Test
    void emittingModulesUseSharedEventsAndNeverAuditInternals() throws Exception {
        List<Path> roots = List.of(
                Path.of("src/main/java/lab/paymentquality/merchant"),
                Path.of("src/main/java/lab/paymentquality/payment"),
                Path.of("src/main/java/lab/paymentquality/iam"));

        String sources = readJavaSources(roots);

        assertThat(sources).contains("lab.paymentquality.shared.events");
        assertThat(sources).doesNotContain("lab.paymentquality.audit.internal");
    }

    private static String readJavaSources(List<Path> roots) throws Exception {
        StringBuilder source = new StringBuilder();
        for (Path root : roots) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .toList()) {
                    source.append(Files.readString(file));
                }
            }
        }
        return source.toString();
    }
}
