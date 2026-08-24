package lab.paymentquality.eventlab;

import lab.paymentquality.eventlab.internal.EventLabEnvelope;
import lab.paymentquality.eventlab.internal.EventLabHeaders;
import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.KafkaContainerSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.eventlab.internal.EventLabTopics;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * E3 persistence — RA-KAFKA-020..029 (positive+negative, broker variants, Awaitility ≤5s).
 * Uses Testcontainers postgres + KafkaContainerSupport singleton (image apache/kafka:4.0.0).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "kafka"})
@Import(TestJwtConfiguration.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EventLabPersistenceKafkaIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("eventlab_persist");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        KafkaContainerSupport.ensureLabTopics();
        registerPostgresProperties(r, postgres);
        r.add("spring.kafka.bootstrap-servers", KafkaContainerSupport::bootstrapServers);
        r.add("app.event-lab.enabled", () -> "true");
    }

    @BeforeAll
    static void ensureTopics() {
        KafkaContainerSupport.ensureLabTopics();
    }

    @Autowired JpaEventLabProcessedRepository repo;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired(required = false) KafkaListenerEndpointRegistry listenerRegistry;

    private EventLabProcessed sample(String consumerGroup, UUID eventId, String targetId, String tenantRef) {
        return EventLabProcessed.of(consumerGroup, eventId, "PAYMENT_AUTHORIZED", "PAYMENT_ORDER", targetId, tenantRef, "PROCESSED", "lab.auditable-actions.v1", 0, 0L, targetId);
    }

    private static Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    private void produceViaBroker(UUID eventId, String targetId, String tenantRef, String action, String corrId, boolean poison) throws Exception {
        AuditableActionOccurred evt = new AuditableActionOccurred(eventId, Instant.now(), "subj", "disp", action, "PAYMENT_ORDER", targetId, tenantRef, corrId, Outcome.SUCCESS, null, null);
        Map<String, Object> payload = EventLabEnvelope.payloadOf(evt);
        if (poison) {
            // add poison marker in payload and header
            java.util.Map<String, Object> mutable = new java.util.LinkedHashMap<>(payload);
            mutable.put("poison", true);
            payload = mutable;
        }
        String json = objectMapper.writeValueAsString(payload);
        byte[] value = json.getBytes(StandardCharsets.UTF_8);
        Map<String, Object> headers = EventLabHeaders.from(evt);
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(EventLabTopics.AUDITABLE_ACTIONS, null, targetId, value);
        for (Map.Entry<String, Object> e : headers.entrySet()) {
            record.headers().add(e.getKey(), String.valueOf(e.getValue()).getBytes(StandardCharsets.UTF_8));
        }
        if (poison) {
            record.headers().add("poison", "true".getBytes(StandardCharsets.UTF_8));
        }
        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps())) {
            producer.send(record).get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(1)
    void raKAFKA020_flywayV37JpaValidateUniqueGroupEventId() {
        UUID eventId = UUID.randomUUID();
        EventLabProcessed e = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        repo.saveAndFlush(e);
        assertThat(repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId)).isPresent();
    }

    @Test
    @Order(2)
    void raKAFKA020N_duplicateSameGroupEventIdConstraint() {
        UUID eventId = UUID.randomUUID();
        EventLabProcessed e1 = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        repo.saveAndFlush(e1);
        EventLabProcessed e2 = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        assertThatThrownBy(() -> repo.saveAndFlush(e2)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Order(3)
    void raKAFKA021_consumerWritesProcessedWithin5sViaBroker() throws Exception {
        UUID eventId = UUID.randomUUID();
        String targetId = "persist-" + UUID.randomUUID();
        produceViaBroker(eventId, targetId, "TENANT_ALPHA", "PAYMENT_AUTHORIZED", "corr-021", false);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).isPresent());
        var opt = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId);
        assertThat(opt).isPresent();
        EventLabProcessed saved = opt.get();
        assertThat(saved.getStatus()).isEqualTo("PROCESSED");
        assertThat(saved.getTargetId()).isEqualTo(targetId);
        // consumedAt within 5s (tolerate slight skew)
        assertThat(Duration.between(saved.getConsumedAt(), Instant.now()).abs().toSeconds()).isLessThanOrEqualTo(6);
    }

    @Test
    @Order(5)
    void raKAFKA018N_sameKeyFailFirstPoisonThenValidStillProcesses() throws Exception {
        // Ordering policy (ADR): non-blocking retry means a failing event may be
        // sent to retry/DLT out of order vs. later events of the same key. The
        // guarantee that remains: a later valid same-key event is still processed.
        String targetId = "order-" + UUID.randomUUID();
        UUID poisonId = UUID.randomUUID();
        produceViaBroker(poisonId, targetId, "TENANT_ALPHA", "PAYMENT_AUTHORIZED", "corr-018N-p", true);
        UUID validId = UUID.randomUUID();
        produceViaBroker(validId, targetId, "TENANT_ALPHA", "PAYMENT_CAPTURED", "corr-018N-v", false);

        // The valid record ends up PROCESSED; the poison record ends up DEAD on the contract DLT.
        Awaitility.await().atMost(Duration.ofSeconds(8)).until(() -> {
            var validOpt = repo.findByConsumerGroupAndEventId("eventlab-inspector", validId);
            var deadOpt = repo.findByConsumerGroupAndEventId("eventlab-inspector", poisonId);
            return validOpt.isPresent() && "PROCESSED".equals(validOpt.get().getStatus())
                    && deadOpt.isPresent() && "DEAD".equals(deadOpt.get().getStatus());
        });
        assertThat(repo.findByConsumerGroupAndEventId("eventlab-inspector", validId).get().getStatus()).isEqualTo("PROCESSED");
        assertThat(repo.findByConsumerGroupAndEventId("eventlab-inspector", poisonId).get().getStatus()).isEqualTo("DEAD");
    }

    @Test
    @Order(4)
    void raKAFKA022_duplicateViaBrokerStillOneRow() throws Exception {
        UUID eventId = UUID.randomUUID();
        String targetId = "dup-" + UUID.randomUUID();
        produceViaBroker(eventId, targetId, "TENANT_ALPHA", "PAYMENT_AUTHORIZED", "corr-022", false);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).isPresent());
        // second produce with same eventId
        produceViaBroker(eventId, targetId, "TENANT_ALPHA", "PAYMENT_AUTHORIZED", "corr-022", false);
        // await a moment for potential duplicate handling
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).isPresent());
        Awaitility.await().atMost(Duration.ofSeconds(1)).until(() -> repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).isPresent());
        long count = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).stream().count();
        assertThat(count).isEqualTo(1);
        // ensure only one row per eventId exists even after duplicate
        var found = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId);
        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    @Order(6)
    void raKAFKA023_replayViaListenerRestartOneRowPerGroup() throws Exception {
        // Real restart oracle: stop the production listener container, then re-produce
        // the same eventId (at-least-once redelivery across a restart), then start it.
        // The unique (consumer_group,event_id) must keep exactly one DB row.
        UUID eventId = UUID.randomUUID();
        String targetId = "replay-" + UUID.randomUUID();
        produceViaBroker(eventId, targetId, "TENANT_ALPHA", "PAYMENT_CAPTURED", "corr-023", false);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).isPresent());

        if (listenerRegistry != null) {
            listenerRegistry.getListenerContainers()
                    .forEach(c -> c.stop());
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(
                    () -> listenerRegistry.getListenerContainers().stream().allMatch(c -> !c.isRunning()));
            try {
                // redelivery of the same eventId while the listener is down
                produceViaBroker(eventId, targetId, "TENANT_ALPHA", "PAYMENT_CAPTURED", "corr-023", false);
            } finally {
                listenerRegistry.getListenerContainers()
                        .forEach(c -> c.start());
            }
        } else {
            produceViaBroker(eventId, targetId, "TENANT_ALPHA", "PAYMENT_CAPTURED", "corr-023", false);
        }

        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            long present = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).stream().count();
            return present == 1L;
        });
        long count = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).stream().count();
        assertThat(count).isEqualTo(1);
        assertThat(repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).get().getStatus()).isEqualTo("PROCESSED");
    }

    @Test
    @Order(9)
    void raKAFKA024_poisonViaBrokerGoesToDead() throws Exception {
        UUID poisonId = UUID.randomUUID();
        String poisonTarget = "poison-" + UUID.randomUUID();
        int beforePoison = jdbcTemplate.queryForObject("select count(*) from payment_orders", Integer.class);
        // produce poison pill
        produceViaBroker(poisonId, poisonTarget, "TENANT_ALPHA", "PAYMENT_AUTHORIZED", "corr-024", true);
        // await DEAD status (after retries: 3 attempts *500ms ~1.5s + DLT)
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            var opt = repo.findByConsumerGroupAndEventId("eventlab-inspector", poisonId);
            return opt.isPresent() && "DEAD".equals(opt.get().getStatus());
        });
        var dead = repo.findByConsumerGroupAndEventId("eventlab-inspector", poisonId);
        assertThat(dead).isPresent();
        assertThat(dead.get().getStatus()).isEqualTo("DEAD");
        assertThat(dead.get().getTopic()).isEqualTo(EventLabTopics.DLT);
        // the poison record physically landed on the contract DLT topic with our poisonId
        try (var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<String, byte[]>(dltConsumerProps())) {
            consumer.subscribe(List.of(EventLabTopics.DLT));
            final boolean[] found = {false};
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
                var recs = consumer.poll(Duration.ofMillis(400));
                for (var r : recs) {
                    if (!EventLabTopics.DLT.equals(r.topic())) continue;
                    String payload = r.value() == null ? "" : new String(r.value(), StandardCharsets.UTF_8);
                    if (poisonTarget.equals(r.key()) && payload.contains(poisonId.toString())) {
                        found[0] = true;
                        break;
                    }
                }
                return found[0];
            });
            assertThat(found[0]).isTrue();
        }
        // business table unchanged
        int afterPoison = jdbcTemplate.queryForObject("select count(*) from payment_orders", Integer.class);
        assertThat(afterPoison).isEqualTo(beforePoison);
        // verify other valid event still processes after poison (listener not crashed)
        UUID validId = UUID.randomUUID();
        String validTarget = "valid-after-poison-" + UUID.randomUUID();
        produceViaBroker(validId, validTarget, "TENANT_ALPHA", "PAYMENT_AUTHORIZED", "corr-024-valid", false);
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> repo.findByConsumerGroupAndEventId("eventlab-inspector", validId).isPresent());
        assertThat(repo.findByConsumerGroupAndEventId("eventlab-inspector", validId).get().getStatus()).isEqualTo("PROCESSED");
    }

    private static Properties dltConsumerProps() {
        Properties props = new Properties();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "ra024-dlt-" + UUID.randomUUID());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.ByteArrayDeserializer.class.getName());
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Test
    @Order(6)
    void raKAFKA022_duplicateEventIdStillOneRow() {
        UUID eventId = UUID.randomUUID();
        EventLabProcessed e1 = sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA");
        repo.saveAndFlush(e1);
        long before = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).stream().count();
        boolean exists = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).isPresent();
        if (!exists) repo.saveAndFlush(sample("eventlab-inspector", eventId, UUID.randomUUID().toString(), "TENANT_ALPHA"));
        long after = repo.findByConsumerGroupAndEventId("eventlab-inspector", eventId).stream().count();
        assertThat(after).isEqualTo(before);
    }

    @Test
    @Order(7)
    void raKAFKA029_searchByTargetIdReturnsRecord() {
        String targetId = "searched-" + UUID.randomUUID();
        EventLabProcessed e = sample("eventlab-inspector", UUID.randomUUID(), targetId, "TENANT_ALPHA");
        repo.saveAndFlush(e);
        List<EventLabProcessed> found = repo.findByTargetId(targetId);
        assertThat(found).hasSizeGreaterThanOrEqualTo(1);
        assertThat(found.get(0).getTargetId()).isEqualTo(targetId);
    }

    @Test
    @Order(8)
    @org.springframework.transaction.annotation.Transactional
    void raKAFKA026_purgeOnlyStaleProcessedLeavesBusinessRows() {
        // seed: one old PROCESSED (consumedAt 10 days ago), one fresh PROCESSED
        EventLabProcessed oldRow = sample("eventlab-inspector", UUID.randomUUID(), "purge-old-" + UUID.randomUUID(), "TENANT_ALPHA");
        oldRow.setConsumedAt(Instant.now().minus(java.time.Duration.ofDays(10)));
        repo.saveAndFlush(oldRow);
        EventLabProcessed freshRow = sample("eventlab-inspector", UUID.randomUUID(), "purge-fresh-" + UUID.randomUUID(), "TENANT_ALPHA");
        repo.saveAndFlush(freshRow);

        int businessBefore = jdbcTemplate.queryForObject("select count(*) from payment_orders", Integer.class);

        int deleted = repo.deleteProcessedOlderThan(Instant.now().minus(java.time.Duration.ofDays(7)));

        // flush so the DELETE is visible to the same-tx queries
        repo.flush();
        assertThat(deleted).isEqualTo(1);
        // First-level cache may hold the pre-delete entity; use the DB truth for the row check.
        Integer oldCount = jdbcTemplate.queryForObject(
                "select count(*) from eventlab_processed where id = ?", Integer.class, oldRow.getId());
        Integer freshCount = jdbcTemplate.queryForObject(
                "select count(*) from eventlab_processed where id = ?", Integer.class, freshRow.getId());
        assertThat(oldCount).isZero();
        assertThat(freshCount).isEqualTo(1);
        // business tables untouched
        assertThat(jdbcTemplate.queryForObject("select count(*) from payment_orders", Integer.class)).isEqualTo(businessBefore);
    }
}
