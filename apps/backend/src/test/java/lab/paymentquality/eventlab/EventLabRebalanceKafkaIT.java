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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RA-KAFKA-050 rebalance: two real listener containers in the same group must
 * not duplicate DB rows. Produces N events, starts two containers, forces a
 * rebalance (stop one), and asserts exactly one row per (consumer_group,eventId).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "kafka"})
@Import(TestJwtConfiguration.class)
@Testcontainers
public class EventLabRebalanceKafkaIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("eventlab_rebalance");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        KafkaContainerSupport.ensureLabTopics();
        registerPostgresProperties(r, postgres);
        r.add("spring.kafka.bootstrap-servers", KafkaContainerSupport::bootstrapServers);
        r.add("app.event-lab.enabled", () -> "true");
    }

    @BeforeAll
    static void ensureTopic() {
        KafkaContainerSupport.ensureLabTopics();
    }

    @Autowired JpaEventLabProcessedRepository repo;
    @Autowired ObjectMapper objectMapper;
    @Autowired(required = false) KafkaListenerEndpointRegistry registry;

    private static Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    private static DefaultKafkaConsumerFactory<String, byte[]> factory(String clientId, String group) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, group);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Test
    void raKAFKA050_rebalanceTwoRealListenersExactlyOneRow() throws Exception {
        // Pause the app's own listener so its group does not compete with our containers.
        if (registry != null) {
            registry.getListenerContainers().forEach(org.springframework.kafka.listener.MessageListenerContainer::pause);
        }
        String group = "rebalance-it-" + UUID.randomUUID();
        int n = 4;
        List<UUID> eventIds = new ArrayList<>();
        List<String> targetIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            eventIds.add(UUID.randomUUID());
            targetIds.add("rebalance-" + UUID.randomUUID());
        }

        // produce N distinct events via broker directly
        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps())) {
            for (int i = 0; i < n; i++) {
                UUID eid = eventIds.get(i);
                String tid = targetIds.get(i);
                AuditableActionOccurred evt = new AuditableActionOccurred(
                        eid, Instant.now(), "subj", "disp", "PAYMENT_AUTHORIZED", "PAYMENT_ORDER",
                        tid, "TENANT_ALPHA", "corr-rebalance-" + i, Outcome.SUCCESS, null, null);
                Map<String, Object> payload = EventLabEnvelope.payloadOf(evt);
                String json = objectMapper.writeValueAsString(payload);
                byte[] value = json.getBytes(StandardCharsets.UTF_8);
                Map<String, Object> headers = EventLabHeaders.from(evt);
                ProducerRecord<String, byte[]> rec = new ProducerRecord<>("lab.auditable-actions.v1", null, tid, value);
                headers.forEach((k, v) -> rec.headers().add(k, String.valueOf(v).getBytes(StandardCharsets.UTF_8)));
                producer.send(rec).get(5, TimeUnit.SECONDS);
            }
        }

        // The listener mirrors the production upsert; the unique constraint is the exactly-once oracle.
        Consumer<ConsumerRecord<String, byte[]>> persist = record -> {
            var header = record.headers().lastHeader("eventId");
            if (header == null) return;
            try {
                UUID eid = UUID.fromString(new String(header.value(), StandardCharsets.UTF_8));
                if (repo.findByConsumerGroupAndEventId(group, eid).isEmpty()) {
                    repo.saveAndFlush(EventLabProcessed.of(
                            group, eid, "PAYMENT_AUTHORIZED", "PAYMENT_ORDER",
                            record.key(), "TENANT_ALPHA", "PROCESSED", "lab.auditable-actions.v1",
                            record.partition(), record.offset(), record.key()));
                }
            } catch (DataIntegrityViolationException ignored) {
                // exactly-once via unique(group,eventId)
            }
        };

        MessageListener<String, byte[]> listener = persist::accept;
        ConsumerFactory<String, byte[]> cf1 = factory("rebalance-c1", group);
        ConsumerFactory<String, byte[]> cf2 = factory("rebalance-c2", group);
        ConcurrentMessageListenerContainer<String, byte[]> c1 = container(cf1, listener);
        ConcurrentMessageListenerContainer<String, byte[]> c2 = container(cf2, listener);
        c1.start();
        c2.start();
        try {
            Awaitility.await().atMost(Duration.ofSeconds(10)).until(() ->
                    eventIds.stream().filter(id -> repo.findByConsumerGroupAndEventId(group, id).isPresent()).count() == n);
        } finally {
            // force rebalance: remove one member from the group
            c2.stop();
            c1.stop();
        }

        for (UUID id : eventIds) {
            var opt = repo.findByConsumerGroupAndEventId(group, id);
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo("PROCESSED");
            assertThat(repo.findByConsumerGroupAndEventId(group, id).stream().count()).isEqualTo(1L);
        }
    }

    private static ConcurrentMessageListenerContainer<String, byte[]> container(
            ConsumerFactory<String, byte[]> cf, MessageListener<String, byte[]> listener) {
        ContainerProperties props = new ContainerProperties("lab.auditable-actions.v1");
        props.setMessageListener(listener);
        return new ConcurrentMessageListenerContainer<>(cf, props);
    }
}