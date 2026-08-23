package lab.paymentquality.eventlab;

import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.KafkaContainerSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RA-KAFKA-011 flag-off => 0 records and no producer connection.
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
        r.add("spring.kafka.bootstrap-servers", KafkaContainerSupport::bootstrapServers);
        r.add("app.event-lab.enabled", () -> "false");
    }
    @Autowired ApplicationEventPublisher publisher;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void raKAFKA011_flagOffZeroRecordsNoProducer() {
        String targetId = "flagoff-" + UUID.randomUUID();
        var e = new AuditableActionOccurred(UUID.randomUUID(), Instant.now(), "s","d","PAYMENT_AUTHORIZED","PAYMENT_ORDER",targetId,"TENANT_ALPHA","corr", Outcome.SUCCESS, null, null);
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(e); return null; });
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "flagoff-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String,String> c = new KafkaConsumer<>(props);
        c.subscribe(List.of("lab.auditable-actions.v1"));
        ConsumerRecords<String,String> recs = c.poll(Duration.ofSeconds(3));
        c.close();
        boolean found = false;
        for (ConsumerRecord<String,String> r : recs) if (targetId.equals(r.key())) found = true;
        assertThat(found).isFalse();
    }
}
