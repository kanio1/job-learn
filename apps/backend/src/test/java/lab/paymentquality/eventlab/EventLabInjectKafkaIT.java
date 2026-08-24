package lab.paymentquality.eventlab;

import io.restassured.http.ContentType;
import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import lab.paymentquality.testsupport.KafkaContainerSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import lab.paymentquality.testsupport.TestJwtSupport;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "kafka"})
@Import(TestJwtConfiguration.class)
@Testcontainers
public class EventLabInjectKafkaIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("eventlab_inject");

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

    @LocalServerPort int port;
    @Autowired JpaEventLabProcessedRepository repo;
    @Autowired JdbcTemplate jdbcTemplate;

    private String operateToken() {
        return TestJwtSupport.tokenWithRolesAndTenantId("op", List.of("platform:event-lab:read", "platform:event-lab:operate"), "PLATFORM_TENANT");
    }
    private String readOnlyToken() {
        return TestJwtSupport.tokenWithRolesAndTenantId("reader", List.of("platform:event-lab:read"), "PLATFORM_TENANT");
    }

    private EventLabProcessed seedProcessedRow(String targetId) {
        EventLabProcessed row = EventLabProcessed.of("eventlab-inspector", UUID.randomUUID(), "PAYMENT_AUTHORIZED",
                "PAYMENT_ORDER", targetId, "PLATFORM_TENANT", "PROCESSED", "lab.auditable-actions.v1", 0, 0L, targetId);
        return repo.saveAndFlush(row);
    }

    @Test
    void injectDuplicate_publishesRealPipelineAndKeepsOneRow() throws Exception {
        EventLabProcessed row = seedProcessedRow("dup-" + UUID.randomUUID());
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(operateToken()))
                .contentType(ContentType.JSON).body(Map.of("eventId", row.getEventId().toString()))
                .when().post("/api/event-lab/inject/duplicate")
                .then().statusCode(201);

        // the duplicate goes through the real topic; listener keeps a single row
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            long rows = repo.findByConsumerGroupAndEventId("eventlab-inspector", row.getEventId()).stream().count();
            assertThat(rows).isEqualTo(1L);
        });
    }

    @Test
    void injectPoison_producesDeadOnContractDlt_withinBudget() throws Exception {
        EventLabProcessed row = seedProcessedRow("poison-" + UUID.randomUUID());
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(operateToken()))
                .contentType(ContentType.JSON).body(Map.of("eventId", row.getEventId().toString()))
                .when().post("/api/event-lab/inject/poison")
                .then().statusCode(201);

        // Real pipeline: poison marker -> retry -> DLT -> @DltHandler writes DEAD row with the contract DLT topic
        Awaitility.await().atMost(Duration.ofSeconds(8)).untilAsserted(() -> {
            var opt = repo.findByConsumerGroupAndEventId("eventlab-inspector", row.getEventId());
            assertThat(opt).isPresent();
            assertThat(opt.get().getStatus()).isEqualTo("DEAD");
        });
        EventLabProcessed dead = repo.findByConsumerGroupAndEventId("eventlab-inspector", row.getEventId()).orElseThrow();
        assertThat(dead.getTopic()).isEqualTo("lab.event-lab.dlq.v1");

        // Physical consume from the contract DLT proves the record landed there with the poison eventId.
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(dltConsumerProps())) {
            consumer.subscribe(List.of("lab.event-lab.dlq.v1"));
            final boolean[] found = {false};
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(400));
                for (ConsumerRecord<String, byte[]> r : records) {
                    String payload = r.value() == null ? "" : new String(r.value(), java.nio.charset.StandardCharsets.UTF_8);
                    if (payload.contains(row.getEventId().toString())) {
                        found[0] = true;
                        break;
                    }
                }
                return found[0];
            });
            assertThat(found[0]).isTrue();
        }

        // business invariants: no new payment order, no audit row
        assertThat(jdbcTemplate.queryForObject("select count(*) from payment_orders", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_event", Integer.class)).isZero();
    }

    @Test
    void injectReadOnly_gets403() {
        // read-only caller must be refused
        EventLabProcessed row = seedProcessedRow("ro-" + UUID.randomUUID());
        given().port(port).header(HttpHeaders.AUTHORIZATION, bearer(readOnlyToken()))
                .contentType(ContentType.JSON).body(Map.of("eventId", row.getEventId().toString()))
                .when().post("/api/event-lab/inject/duplicate")
                .then().statusCode(403);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
    private static Properties dltConsumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ra-inject-dlt-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }
}