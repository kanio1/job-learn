package lab.paymentquality.audit.internal.infrastructure;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JpaAuditEventRepositoryTest extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("audit_repository_test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JpaAuditEventRepository repository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void platformQuerySeesAllRowsOrderedNewestFirstAndPaginates() {
        save("2026-06-01T10:00:00Z", "actor-a", "Operator A", "MERCHANT_CREATED", "MERCHANT", "m-1", "TENANT_ALPHA");
        save("2026-06-03T10:00:00Z", "actor-b", "Operator B", "USER_UPDATED", "USER", "u-1", "TENANT_BETA");
        save("2026-06-02T10:00:00Z", "actor-c", "Operator C", "PAYMENT_CAPTURED", "PAYMENT_ORDER", "p-1", "TENANT_ALPHA");

        var page = repository.findAll(
                (root, query, cb) -> cb.conjunction(),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "occurredAt")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(AuditEvent::getTargetId)
                .containsExactly("u-1", "p-1");
    }

    @Test
    void tenantAndBusinessFiltersCombineWithInclusiveDateRange() {
        save("2026-06-01T00:00:00Z", "actor-a", "Operator A", "MERCHANT_CREATED", "MERCHANT", "m-1", "TENANT_ALPHA");
        save("2026-06-01T23:59:59Z", "actor-a", "Operator A", "MERCHANT_CREATED", "MERCHANT", "m-2", "TENANT_ALPHA");
        save("2026-06-01T12:00:00Z", "actor-a", "Operator A", "MERCHANT_CREATED", "MERCHANT", "m-3", "TENANT_BETA");
        save("2026-06-02T00:00:00Z", "actor-a", "Operator A", "MERCHANT_CREATED", "MERCHANT", "m-4", "TENANT_ALPHA");

        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant toExclusive = Instant.parse("2026-06-02T00:00:00Z");
        Specification<AuditEvent> specification = (root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), "TENANT_ALPHA"),
                cb.or(
                        cb.equal(root.get("actorSubject"), "actor-a"),
                        cb.equal(root.get("actorDisplay"), "actor-a")),
                cb.equal(root.get("action"), "MERCHANT_CREATED"),
                cb.equal(root.get("targetType"), "MERCHANT"),
                cb.greaterThanOrEqualTo(root.get("occurredAt"), from),
                cb.lessThan(root.get("occurredAt"), toExclusive));

        var result = repository.findAll(specification, Sort.by(Sort.Direction.DESC, "occurredAt"));

        assertThat(result).extracting(AuditEvent::getTargetId).containsExactly("m-2", "m-1");
        assertThat(result).allMatch(event -> event.getTenantId().equals("TENANT_ALPHA"));
    }

    @Test
    void databaseRejectsOutcomeOutsideTheExplicitContract() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO audit_event (
                    id, occurred_at, actor_subject, actor_display, action,
                    target_type, target_id, tenant_id, correlation_id, outcome
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), Timestamp.from(Instant.parse("2026-06-01T10:00:00Z")),
                "actor-db", "Database Operator", "MERCHANT_CREATED",
                "MERCHANT", "m-db", "TENANT_ALPHA", "correlation-db", "UNKNOWN"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private AuditEvent save(
            String occurredAt,
            String actorSubject,
            String actorDisplay,
            String action,
            String targetType,
            String targetId,
            String tenantReference) {
        return repository.saveAndFlush(AuditEvent.fromEvent(new AuditableActionOccurred(
                Instant.parse(occurredAt), actorSubject, actorDisplay, action,
                targetType, targetId, tenantReference, "correlation-" + targetId, Outcome.SUCCESS)));
    }
}
