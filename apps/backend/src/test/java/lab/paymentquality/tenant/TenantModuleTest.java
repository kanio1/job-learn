package lab.paymentquality.tenant;

import jakarta.persistence.EntityManager;
import lab.paymentquality.PaymentQualityApplication;
import lab.paymentquality.tenant.internal.domain.Tenant;
import lab.paymentquality.tenant.internal.infrastructure.JpaTenantRepository;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
class TenantModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = startPostgres("tenant_module_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    TenantResolver tenantResolver;

    @Autowired
    JpaTenantRepository tenantRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    void tenantModuleBootsWithResolverRepositoryAndEntityMapping() {
        assertThat(tenantResolver).isNotNull();
        assertThat(applicationContext.getBean("tenantResolverService")).isSameAs(tenantResolver);
        assertThat(tenantRepository.findByTenantReference("TENANT_ALPHA")).isPresent();
        assertThat(entityManager.getMetamodel().entity(Tenant.class).getName()).isEqualTo("Tenant");
    }

    @Test
    void applicationModuleArchitectureStillVerifies() {
        ApplicationModules.of(PaymentQualityApplication.class).verify();
    }
}
