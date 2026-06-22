package lab.paymentquality.audit;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.audit.internal.infrastructure.JpaAuditEventRepository;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AuditEventPersistenceTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("audit_event_persistence_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JpaAuditEventRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void migrationAndJpaMappingPersistOnlyExplicitAuditFields() {
        Instant occurredAt = Instant.parse("2026-06-19T08:15:30Z");
        AuditableActionOccurred source = new AuditableActionOccurred(
                occurredAt,
                "subject-42",
                "Platform Operator",
                "MERCHANT_SUSPENDED",
                "MERCHANT",
                "merchant-42",
                "TENANT_ALPHA",
                "correlation-42",
                Outcome.SUCCESS);

        AuditEvent saved = repository.saveAndFlush(AuditEvent.fromEvent(source));

        assertThat(repository.findById(saved.getId())).get()
                .satisfies(found -> {
                    assertThat(found.getOccurredAt()).isEqualTo(occurredAt);
                    assertThat(found.getActorSubject()).isEqualTo("subject-42");
                    assertThat(found.getActorDisplay()).isEqualTo("Platform Operator");
                    assertThat(found.getAction()).isEqualTo("MERCHANT_SUSPENDED");
                    assertThat(found.getTargetType()).isEqualTo("MERCHANT");
                    assertThat(found.getTargetId()).isEqualTo("merchant-42");
                    assertThat(found.getTenantId()).isEqualTo("TENANT_ALPHA");
                    assertThat(found.getCorrelationId()).isEqualTo("correlation-42");
                    assertThat(found.getOutcome()).isEqualTo(Outcome.SUCCESS);
                });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'audit_event'",
                Integer.class)).isEqualTo(1);
        assertThat(Arrays.stream(AuditEvent.class.getDeclaredFields()).map(Field::getName))
                .containsExactlyInAnyOrder(
                        "id", "occurredAt", "actorSubject", "actorDisplay", "action",
                        "targetType", "targetId", "tenantId", "correlationId", "outcome");
    }
}
