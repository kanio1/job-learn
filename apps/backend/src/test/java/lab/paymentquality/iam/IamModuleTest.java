package lab.paymentquality.iam;

import lab.paymentquality.PaymentQualityApplication;
import lab.paymentquality.iam.internal.application.UserManagementService;
import lab.paymentquality.iam.internal.infrastructure.KeycloakAdminClient;
import lab.paymentquality.iam.internal.infrastructure.KeycloakAdminTokenProvider;
import lab.paymentquality.iam.internal.web.UserManagementController;
import lab.paymentquality.iam.internal.web.UserManagementExceptionHandler;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
import static org.assertj.core.api.Assertions.assertThatNoException;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ActiveProfiles("test")
@Testcontainers
class IamModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("iam_module_test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    UserManagementService userManagementService;

    @Autowired
    KeycloakAdminClient keycloakAdminClient;

    @Autowired
    KeycloakAdminTokenProvider keycloakAdminTokenProvider;

    @Autowired
    UserManagementController userManagementController;

    @Autowired
    UserManagementExceptionHandler exceptionHandler;

    @Test
    void iamModuleBootsWithCoreBeans() {
        assertThat(userManagementService).isNotNull();
        assertThat(keycloakAdminClient).isNotNull();
        assertThat(keycloakAdminTokenProvider).isNotNull();
        assertThat(userManagementController).isNotNull();
        assertThat(exceptionHandler).isNotNull();
    }

    @Test
    void applicationModuleArchitectureStillVerifies() {
        assertThatNoException().isThrownBy(
                () -> ApplicationModules.of(PaymentQualityApplication.class).verify());
    }

    @Test
    void iamModuleDeclaresNoJpaEntity() {
        Path iamRoot = Path.of("src/main/java/lab/paymentquality/iam");
        assertThat(Files.isDirectory(iamRoot)).isTrue();

        try (Stream<Path> files = Files.walk(iamRoot)) {
            assertThat(files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .map(this::readContent)
                    .noneMatch(content -> content.contains("@Entity")))
                    .as("iam module must not declare any JPA @Entity")
                    .isTrue();
        } catch (Exception e) {
            throw new AssertionError("Failed to scan iam module for JPA entities", e);
        }
    }

    @Test
    void iamModuleContributesNoFlywayMigration() {
        Path migrationRoot = Path.of("src/main/resources/db/migration");
        if (Files.exists(migrationRoot)) {
            try (Stream<Path> files = Files.walk(migrationRoot)) {
                assertThat(files
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String path = p.toString().replace('\\', '/');
                            return path.contains("/iam/");
                        })
                        .toList())
                        .as("iam module must not contribute any Flyway migration")
                        .isEmpty();
            } catch (Exception e) {
                throw new AssertionError("Failed to scan for iam Flyway migrations", e);
            }
        }
    }

    private String readContent(Path file) {
        try {
            return Files.readString(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
