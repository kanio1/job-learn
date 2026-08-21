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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ActiveProfiles("test")
@Testcontainers
class IamModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = startPostgres("iam_module_test");

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
    void iamModuleOwnsUserSavedViewEntity() {
        Path entity = Path.of("src/main/java/lab/paymentquality/iam/internal/domain/UserSavedView.java");
        assertThat(entity).exists();
        assertThat(readContent(entity)).contains("@Entity").contains("user_saved_views");
    }

    @Test
    void iamModuleContributesV35SavedViewsMigration() {
        Path migration = Path.of("src/main/resources/db/migration/iam/V35__create_user_saved_views.sql");
        assertThat(migration).exists();
        assertThat(readContent(migration)).contains("user_saved_views").contains("owner_subject");
    }

    private String readContent(Path file) {
        try {
            return Files.readString(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
