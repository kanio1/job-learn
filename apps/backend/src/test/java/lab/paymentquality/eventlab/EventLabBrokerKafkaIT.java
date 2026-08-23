package lab.paymentquality.eventlab;

import lab.paymentquality.testsupport.KafkaContainerSupport;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RA-KAFKA-001 (+): broker roundtrip | RA-KAFKA-001N (-): empty consume does not hang
 * RA-KAFKA-002: idempotent create | RA-KAFKA-003: 3 partitions RF1 | RA-KAFKA-003N: no auto-create
 */
class EventLabBrokerKafkaIT {

    @Test
    void raKafka001_produceConsumeRoundtrip() {
        String topic = "scratch-" + UUID.randomUUID();
        String value = "hello-" + UUID.randomUUID();
        createTopic(topic, 1, (short) 1);

        Properties prodProps = producerProps();
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(prodProps)) {
            producer.send(new ProducerRecord<>(topic, "k1", value)).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        Properties consProps = consumerProps("ra-001-" + UUID.randomUUID());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consProps)) {
            consumer.subscribe(List.of(topic));
            ConsumerRecord<String, String> rec = Awaitility.await()
                    .atMost(Duration.ofSeconds(10))
                    .until(() -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                        if (records.isEmpty()) return null;
                        return records.iterator().next();
                    }, r -> r != null);
            assertThat(rec.value()).isEqualTo(value);
        }
    }

    @Test
    void raKafka001N_emptyConsumeDoesNotHang() {
        String topic = "scratch-empty-" + UUID.randomUUID();
        createTopic(topic, 1, (short) 1);
        Properties consProps = consumerProps("ra-001n-" + UUID.randomUUID());
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consProps)) {
            consumer.subscribe(List.of(topic));
            // Awaitility with short timeout should fail fast when no records
            assertThatThrownBy(() -> Awaitility.await()
                    .atMost(Duration.ofSeconds(2))
                    .until(() -> {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(300));
                        if (records.isEmpty()) return null;
                        return records.iterator().next();
                    }, r -> r != null))
                    .isInstanceOf(org.awaitility.core.ConditionTimeoutException.class);
        }
    }

    @Test
    void raKafka002_idempotentCreateLabTopic() throws Exception {
        String topic = "lab.auditable-actions.v1";
        Properties adminProps = adminProps();
        try (AdminClient admin = AdminClient.create(adminProps)) {
            // create idempotently twice
            try {
                admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1)
                        .configs(Map.of(TopicConfig.RETENTION_MS_CONFIG, "604800000")))).all().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // may already exist from dev-stack
            }
            try {
                admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // second create should be topic exists
            }
            var topics = admin.listTopics().names().get(5, TimeUnit.SECONDS);
            long count = topics.stream().filter(t -> t.equals(topic)).count();
            assertThat(count).isEqualTo(1);
        }
    }

    @Test
    void raKafka003_labTopicHas3PartitionsRf1() throws Exception {
        String topic = "lab.auditable-actions.v1";
        // ensure exists
        try (AdminClient admin = AdminClient.create(adminProps())) {
            try {
                admin.createTopics(List.of(new NewTopic(topic, 3, (short) 1))).all().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            var desc = admin.describeTopics(List.of(topic)).topicNameValues().get(topic).get(5, TimeUnit.SECONDS);
            assertThat(desc.partitions()).hasSize(3);
            assertThat(desc.partitions().get(0).replicas()).hasSize(1);
        }
    }

    @Test
    void raKafka003N_noAutoCreateForRandomTopic() throws Exception {
        String randomTopic = "no-auto-" + UUID.randomUUID();
        Properties adminProps = adminProps();
        try (AdminClient admin = AdminClient.create(adminProps)) {
            var names = admin.listTopics().names().get(5, TimeUnit.SECONDS);
            assertThat(names).doesNotContain(randomTopic);
            // Producer-side guard: with allow.auto.create.topics=false the client must not auto-create
            Properties prodProps = producerProps();
            prodProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "2000");
            prodProps.put("allow.auto.create.topics", "false");
            try (KafkaProducer<String, String> producer = new KafkaProducer<>(prodProps)) {
                var future = producer.send(new ProducerRecord<>(randomTopic, "k", "v"));
                try {
                    future.get(3, TimeUnit.SECONDS);
                    // If producer succeeded, topic would have been created via broker default; treat as failure path
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    boolean isExpected = cause instanceof UnknownTopicOrPartitionException
                            || (cause != null && cause.getMessage() != null && cause.getMessage().contains("UNKNOWN_TOPIC_OR_PARTITION"))
                            || ex.getMessage().contains("UNKNOWN_TOPIC_OR_PARTITION")
                            || cause instanceof org.apache.kafka.common.errors.TimeoutException;
                    assertThat(isExpected).as("produce to unknown topic with allow.auto.create.topics=false should fail").isTrue();
                }
            }
            // After producer closed, random topic must not remain listed (client did not auto-create)
            var namesAfter = admin.listTopics().names().get(5, TimeUnit.SECONDS);
            assertThat(namesAfter).as("random topic must not be auto-created by producer").doesNotContain(randomTopic);
            // Additionally verify broker reports unknown for describe
            assertThatThrownBy(() -> admin.describeTopics(List.of(randomTopic)).topicNameValues().get(randomTopic).get(5, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(UnknownTopicOrPartitionException.class);
        }
    }

    private static void createTopic(String topic, int partitions, short rf) {
        try (AdminClient admin = AdminClient.create(adminProps())) {
            try {
                admin.createTopics(List.of(new NewTopic(topic, partitions, rf))).all().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
                try {
                    return admin.listTopics().names().get(3, TimeUnit.SECONDS).contains(topic);
                } catch (Exception e) {
                    return false;
                }
            });
        }
    }

    private static Properties adminProps() {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        return p;
    }

    private static Properties producerProps() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        return p;
    }

    private static Properties consumerProps(String groupId) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return p;
    }
}
