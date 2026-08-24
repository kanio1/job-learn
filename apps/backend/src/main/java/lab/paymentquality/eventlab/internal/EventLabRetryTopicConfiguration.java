package lab.paymentquality.eventlab.internal;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Lab topics. Auto-create is OFF, so KafkaAdmin must create the three
 * contract topics (main, retry, DLT) explicitly. The inject publisher and the
 * Modulith externalizer share Boot's auto-configured KafkaTemplate (value
 * serializer set to ByteArraySerializer in the kafka profile overlay).
 */
@Configuration
@Profile("kafka")
@ConditionalOnProperty(name = "app.event-lab.enabled", havingValue = "true")
class EventLabRetryTopicConfiguration {

    @Bean
    KafkaAdmin eventLabKafkaAdmin(@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        KafkaAdmin admin = new KafkaAdmin(configs);
        admin.setFatalIfBrokerNotAvailable(false);
        return admin;
    }

    @Bean
    KafkaAdmin.NewTopics eventLabTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(EventLabTopics.AUDITABLE_ACTIONS).partitions(3).replicas(1).build(),
                TopicBuilder.name(EventLabTopics.RETRY).partitions(3).replicas(1).build(),
                TopicBuilder.name(EventLabTopics.DLT).partitions(3).replicas(1).build()
        );
    }

}
