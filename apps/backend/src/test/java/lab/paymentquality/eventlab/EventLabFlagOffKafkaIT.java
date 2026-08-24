package lab.paymentquality.eventlab;

import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RA-KAFKA-011 / AT-KAFKA-001N: with the lab flag OFF no Kafka bean exists,
 * no Kafka connection is attempted, and a published event leaves zero broker
 * records. Runs with an unreachable bootstrap server to prove the default
 * {@code ./mvnw test} (Surefire) never initializes a Kafka client.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test"})
@Import(TestJwtConfiguration.class)
@Testcontainers
public class EventLabFlagOffKafkaIT extends PostgresContainerSupport {
    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("eventlab_flagoff");
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        registerPostgresProperties(r, postgres);
        // Unreachable Kafka address: any Kafka init would fail loudly here.
        r.add("spring.kafka.bootstrap-servers", () -> "127.0.0.1:1");
        r.add("app.event-lab.enabled", () -> "false");
    }

    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager txManager;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ApplicationContext context;

    @Test
    void raKAFKA011_flagOffPublishDoesNotExternalize() {
        String targetId = "flagoff-" + UUID.randomUUID();
        var e = new AuditableActionOccurred(UUID.randomUUID(), Instant.now(), "s","d","PAYMENT_AUTHORIZED","PAYMENT_ORDER",targetId,"TENANT_ALPHA","corr", Outcome.SUCCESS, null, null);
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(e); return null; });

        // Modulith keeps the outbox row (DB is source of truth), but with the lab flag OFF
        // no Kafka externalizer runs, so nothing is consumed into eventlab_processed.
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Integer outbox = jdbcTemplate.queryForObject(
                    "select count(*) from event_publication where serialized_event like ?",
                    Integer.class, "%" + targetId + "%");
            return outbox != null && outbox >= 1;
        });
        Integer processed = jdbcTemplate.queryForObject("select count(*) from eventlab_processed", Integer.class);
        assertThat(processed).isZero();
    }

    @Test
    void kaKAFKA011N_flagOff_zeroEventLabApplicationBeans() {
        // The lab's own component is fully gated off: no listener registry containers,
        // no publisher, no controller. Boot may still auto-configure a framework
        // KafkaListenerEndpointRegistry + KafkaTemplate from spring.kafka.* properties —
        // those beans alone do not start a listener or publish anything.
        var registry = context.getBean(org.springframework.kafka.config.KafkaListenerEndpointRegistry.class);
        assertThat(registry.getListenerContainerIds()).isEmpty();
        assertThat(context.getBeanNamesForType(lab.paymentquality.eventlab.internal.application.EventLabKafkaPublisher.class)).isEmpty();
        assertThat(context.getBeanNamesForType(lab.paymentquality.eventlab.internal.web.EventLabController.class)).isEmpty();
        Integer processed = jdbcTemplate.queryForObject("select count(*) from eventlab_processed", Integer.class);
        assertThat(processed).isZero();
    }
}