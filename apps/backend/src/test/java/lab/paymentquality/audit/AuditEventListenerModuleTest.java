package lab.paymentquality.audit;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.audit.internal.infrastructure.JpaAuditEventRepository;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class AuditEventListenerModuleTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("audit_listener_module_test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JpaAuditEventRepository repository;

    @Test
    void persistsOneMerchantEventWithPreservedFields(Scenario scenario) {
        assertSinglePersistedEvent(scenario, event(
                "MERCHANT_SUSPENDED", "MERCHANT", "merchant-41", "TENANT_ALPHA", "correlation-merchant"));
    }

    @Test
    void persistsOnePaymentEventWithPreservedFields(Scenario scenario) {
        assertSinglePersistedEvent(scenario, event(
                "PAYMENT_CAPTURED", "PAYMENT_ORDER", "payment-42", "TENANT_ALPHA", "correlation-payment"));
    }

    @Test
    void persistsOneUserEventWithPreservedFields(Scenario scenario) {
        assertSinglePersistedEvent(scenario, event(
                "USER_ROLES_ASSIGNED", "USER", "user-43", "TENANT_BETA", "correlation-user"));
    }

    private void assertSinglePersistedEvent(Scenario scenario, AuditableActionOccurred source) {
        long before = repository.count();

        scenario.publish(source)
                .andWaitAtMost(Duration.ofSeconds(5))
                .andWaitForStateChange(repository::count, count -> count == before + 1)
                .andVerify(count -> assertThat(count).isEqualTo(before + 1));

        assertThat(repository.findAll())
                .filteredOn(event -> source.correlationId().equals(event.getCorrelationId()))
                .singleElement()
                .satisfies(event -> assertPreserved(event, source));
    }

    private static void assertPreserved(AuditEvent actual, AuditableActionOccurred source) {
        assertThat(actual.getOccurredAt()).isEqualTo(source.occurredAt());
        assertThat(actual.getActorDisplay()).isEqualTo(source.actorDisplay());
        assertThat(actual.getAction()).isEqualTo(source.action());
        assertThat(actual.getTargetType()).isEqualTo(source.targetType());
        assertThat(actual.getTargetId()).isEqualTo(source.targetId());
        assertThat(actual.getTenantId()).isEqualTo(source.tenantRef());
        assertThat(actual.getCorrelationId()).isEqualTo(source.correlationId());
        assertThat(actual.getOutcome()).isEqualTo(source.outcome());
    }

    private static AuditableActionOccurred event(
            String action,
            String targetType,
            String targetId,
            String tenantReference,
            String correlationId) {
        return new AuditableActionOccurred(
                Instant.parse("2026-06-19T11:00:00Z"),
                "module-actor",
                "Module Operator",
                action,
                targetType,
                targetId,
                tenantReference,
                correlationId,
                Outcome.SUCCESS);
    }
}
