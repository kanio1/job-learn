package lab.paymentquality.testsupport;

import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton Kafka container for Failsafe *KafkaIT.
 * Image must match infra/compose/compose.kafka.yml: apache/kafka:4.0.0
 * Uses KRaft combined mode provided by the container image.
 */
public final class KafkaContainerSupport {

    private static final DockerImageName IMAGE = DockerImageName.parse("apache/kafka:4.0.0");
    private static final KafkaContainer CONTAINER = new KafkaContainer(IMAGE)
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

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
}
