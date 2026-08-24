package lab.paymentquality.testsupport;

import lab.paymentquality.eventlab.internal.EventLabTopics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Singleton Kafka container for Failsafe *KafkaIT.
 * Image must match infra/compose/compose.kafka.yml: apache/kafka:4.0.0
 * Uses KRaft combined mode provided by the container image.
 */
public final class KafkaContainerSupport {

    private static final DockerImageName IMAGE = DockerImageName.parse("apache/kafka:4.0.0");
    private static final KafkaContainer CONTAINER = new KafkaContainer(IMAGE)
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false")
            .withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

    static {
        CONTAINER.start();
    }

    private KafkaContainerSupport() {
    }

    public static KafkaContainer container() {
        return CONTAINER;
    }

    public static String bootstrapServers() {
        return CONTAINER.getBootstrapServers();
    }

    /** Creates wave-1 lab topics (main, retry, contract DLT). Auto-create is OFF. */
    public static void ensureLabTopics() {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers());
        try (AdminClient admin = AdminClient.create(p)) {
            List<NewTopic> topics = List.of(
                    new NewTopic(EventLabTopics.AUDITABLE_ACTIONS, 3, (short) 1),
                    new NewTopic(EventLabTopics.RETRY, 3, (short) 1),
                    new NewTopic(EventLabTopics.DLT, 3, (short) 1)
            );
            for (NewTopic topic : topics) {
                try {
                    admin.createTopics(List.of(topic)).all().get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String msg = cause.getClass().getName() + ": " + cause.getMessage();
                    if (!msg.contains("TopicExistsException")) {
                        System.err.println("ensureLabTopics failed for " + topic.name() + " — " + msg);
                    }
                }
            }
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
                try {
                    Set<String> names = admin.listTopics().names().get(3, TimeUnit.SECONDS);
                    return names.contains(EventLabTopics.AUDITABLE_ACTIONS)
                            && names.contains(EventLabTopics.RETRY)
                            && names.contains(EventLabTopics.DLT);
                } catch (Exception e) {
                    return false;
                }
            });
        }
    }
}
