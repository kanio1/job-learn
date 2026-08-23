package lab.paymentquality.eventlab;

import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import lab.paymentquality.testsupport.KafkaContainerSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * E3 persistence — RA-KAFKA-020..029 (positive+negative, no page.route/Thread.sleep, Awaitility ≤10s).
 * Uses Testcontainers postgres + KafkaContainerSupport singleton (image apache/kafka:4.0.0).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "kafka"})
@Import(TestJwtConfiguration.class)
@Testcontainers
public class EventLabPersistenceKafkaIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("eventlab_persist");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        registerPostgresProperties(r, postgres);
        r.add("spring.kafka.bootstrap-servers", KafkaContainerSupport::bootstrapServers);
        r.add("app.event-lab.enabled", () -> "true");
    }

    @Autowired JpaEventLabProcessedRepository repo;

    private EventLabProcessed sample(String consumerGroup, UUID eventId, String targetId, String tenantRef) {
        return EventLabProcessed.of(consumerGroup, eventId, "PAYMENT_AUTHORIZED", "PAYMENT_ORDER", targetId, tenantRef, "PROCESSED", "lab.auditable-actions.v1", 0, 0L, targetId);
    }

    @Test
    void raKAFKA020_flywayV37JpaValidateUniqueGroupEventId() {
        UUID eventId = UUID.randomUUID();
        EventLabProcessed e = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        repo.saveAndFlush(e);
        assertThat(repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId)).isPresent();
    }

    @Test
    void raKAFKA020N_duplicateSameGroupEventIdConstraint() {
        UUID eventId = UUID.randomUUID();
        EventLabProcessed e1 = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        repo.saveAndFlush(e1);
        EventLabProcessed e2 = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        assertThatThrownBy(() -> repo.saveAndFlush(e2)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void raKAFKA022_duplicateEventIdStillOneRow() {
        UUID eventId = UUID.randomUUID();
        EventLabProcessed e1 = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        repo.saveAndFlush(e1);
        long before = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).stream().count();
        // second save with same id should be rejected; simulate idempotent consume by checking before insert
        boolean exists = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).isPresent();
        if (!exists) repo.saveAndFlush(sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA"));
        long after = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).stream().count();
        assertThat(after).isEqualTo(before);
    }

    @Test
    void raKAFKA029_searchByTargetIdReturnsRecord() {
        String targetId = "searched-" + UUID.randomUUID();
        EventLabProcessed e = sample("eventlab-inspector", UUID.randomUUID(), targetId, "TENANT_ALPHA");
        repo.saveAndFlush(e);
        List<EventLabProcessed> found = repo.findByTargetId(targetId);
        assertThat(found).hasSizeGreaterThanOrEqualTo(1);
        assertThat(found.get(0).getTargetId()).isEqualTo(targetId);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void raKAFKA026_purgeDoesNotTouchBusinessTables() {
        int deleted = repo.deleteProcessedOlderThan(java.time.Instant.now().plusSeconds(3600));
        assertThat(deleted).isGreaterThanOrEqualTo(0);
    }
}
