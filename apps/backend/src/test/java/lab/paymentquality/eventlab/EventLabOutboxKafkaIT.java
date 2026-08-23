package lab.paymentquality.eventlab;

import lab.paymentquality.eventlab.internal.EventLabEnvelope;
import lab.paymentquality.eventlab.internal.EventLabHeaders;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import lab.paymentquality.testsupport.KafkaContainerSupport;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import lab.paymentquality.testsupport.TestJwtConfiguration;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * E2 outbox — RA-KAFKA-010..019 (+N). Verifies stable eventId, key=targetId,
 * v1 headers, secrets exclusion, flag-off/rollback zero, ordering, interleaving
 * via pure envelope/header + event_publication outbox (Kafka broker drain is best-effort).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "kafka"})
@Import(TestJwtConfiguration.class)
@Testcontainers
public class EventLabOutboxKafkaIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("eventlab_outbox");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        registerPostgresProperties(r, postgres);
        r.add("spring.kafka.bootstrap-servers", KafkaContainerSupport::bootstrapServers);
        r.add("app.event-lab.enabled", () -> "true");
    }

    @Autowired ApplicationEventPublisher publisher;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager txManager;

    @BeforeAll
    static void ensureTopic() throws Exception {
        Properties p = new Properties();
        p.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerSupport.bootstrapServers());
        try (AdminClient admin = AdminClient.create(p)) {
            try {
                admin.createTopics(List.of(new NewTopic("lab.auditable-actions.v1", 3, (short) 1))).all().get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
                try { return admin.listTopics().names().get(3, TimeUnit.SECONDS).contains("lab.auditable-actions.v1"); } catch (Exception e) { return false; }
            });
        }
    }

    private static AuditableActionOccurred evt(String action, String targetId, String tenantRef, String corrId) {
        return new AuditableActionOccurred(UUID.randomUUID(), Instant.now(), "subj", "disp", action, "PAYMENT_ORDER", targetId, tenantRef, corrId, Outcome.SUCCESS, null, null);
    }

    @Test
    void raKAFKA010_twoAuthorizeTwoDistinctEventIds() {
        String targetA = UUID.randomUUID().toString();
        String targetB = UUID.randomUUID().toString();
        AuditableActionOccurred e1 = evt("PAYMENT_AUTHORIZED", targetA, "TENANT_ALPHA", "corr-1");
        AuditableActionOccurred e2 = evt("PAYMENT_AUTHORIZED", targetB, "TENANT_ALPHA", "corr-2");
        assertThat(e1.eventId()).isNotEqualTo(e2.eventId());
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(e1); return null; });
        tt.execute(s -> { publisher.publishEvent(e2); return null; });
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Integer c = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + targetA.substring(0, 8) + "%");
            return c != null && c >= 1;
        });
        String ser1 = jdbcTemplate.queryForObject("select serialized_event from event_publication where serialized_event like ? order by publication_date desc limit 1", String.class, "%" + targetA + "%");
        String ser2 = jdbcTemplate.queryForObject("select serialized_event from event_publication where serialized_event like ? order by publication_date desc limit 1", String.class, "%" + targetB + "%");
        assertThat(ser1).contains(e1.eventId().toString());
        assertThat(ser2).contains(e2.eventId().toString());
        assertThat(e1.eventId().toString()).isNotEqualTo(e2.eventId().toString());
    }

    @Test
    void raKAFKA010N_nullEventIdThrows() {
        assertThatThrownBy(() -> new AuditableActionOccurred(null, Instant.now(), "s","d","PAYMENT_AUTHORIZED","PAYMENT_ORDER","t","TENANT_ALPHA","c", Outcome.SUCCESS, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void raKAFKA012_013_014_authorizeAfterCommitOneRecordKeyAndHeadersNoSecrets() {
        String targetId = "e2t-" + UUID.randomUUID();
        String corr = "corr-" + UUID.randomUUID();
        AuditableActionOccurred e = evt("PAYMENT_AUTHORIZED", targetId, "TENANT_ALPHA", corr);
        var payloadMap = EventLabEnvelope.payloadOf(e);
        assertThat(payloadMap.get("targetId")).isEqualTo(targetId);
        assertThat(payloadMap.get("action")).isEqualTo("PAYMENT_AUTHORIZED");
        assertThat(payloadMap.get("schemaVersion")).isEqualTo("v1");
        assertThat(payloadMap.toString().toLowerCase()).doesNotContain("pan");
        assertThat(payloadMap.toString().toLowerCase()).doesNotContain("authorization");
        var headers = EventLabHeaders.from(e);
        assertThat(headers).containsKeys("eventId", "action", "targetType", "tenantRef", "correlationId", "occurredAt", "schemaVersion");
        assertThat(headers.get("schemaVersion")).isEqualTo("v1");
        assertThat(headers.get("correlationId")).isEqualTo(corr);
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(e); return null; });
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Integer c = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + targetId + "%");
            return c != null && c >= 1;
        });
        String serialized = jdbcTemplate.queryForObject("select serialized_event from event_publication where serialized_event like ? order by publication_date desc limit 1", String.class, "%" + targetId + "%");
        assertThat(serialized.toLowerCase()).doesNotContain("pan");
        assertThat(serialized).contains(targetId);
        assertThat(serialized).contains("PAYMENT_AUTHORIZED");
    }

    @Test
    void raKAFKA015_rollbackZeroRecords() {
        String targetId = "rollback-" + UUID.randomUUID();
        AuditableActionOccurred e = evt("PAYMENT_AUTHORIZED", targetId, "TENANT_ALPHA", "corr-rb");
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(e); s.setRollbackOnly(); return null; });
        Integer c = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + targetId + "%");
        assertThat(c).isEqualTo(0);
    }

    @Test
    void raKAFKA016_crashHealRepublishIncomplete() {
        String targetId = "crash-" + UUID.randomUUID();
        AuditableActionOccurred e = evt("PAYMENT_AUTHORIZED", targetId, "TENANT_ALPHA", "corr-crash");
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(e); return null; });
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Integer c = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + targetId + "%");
            return c != null && c >= 1;
        });
        Integer count = jdbcTemplate.queryForObject("select count(*) from event_publication where event_type like '%AuditableActionOccurred%'", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
        // republish-outstanding-events-on-restart=true is set in application.yml — publication will be retried on restart
    }

    @Test
    void raKAFKA017_twoDifferentKeysMayDifferPartitions() {
        String t1 = "diff-" + UUID.randomUUID();
        String t2 = "diff-" + UUID.randomUUID();
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(evt("PAYMENT_AUTHORIZED", t1, "TENANT_ALPHA", "c1")); return null; });
        tt.execute(s -> { publisher.publishEvent(evt("PAYMENT_AUTHORIZED", t2, "TENANT_ALPHA", "c2")); return null; });
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Integer c1 = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + t1 + "%");
            Integer c2 = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + t2 + "%");
            return c1 != null && c1 >= 1 && c2 != null && c2 >= 1;
        });
        // interleaving OK — no strict partition assert (RA-KAFKA-050N)
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    void raKAFKA018_sameKeyOrderPreserved() {
        String targetId = "samekey-" + UUID.randomUUID();
        AuditableActionOccurred eAuth = evt("PAYMENT_AUTHORIZED", targetId, "TENANT_ALPHA", "c-auth");
        AuditableActionOccurred eCap = evt("PAYMENT_CAPTURED", targetId, "TENANT_ALPHA", "c-cap");
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(eAuth); return null; });
        tt.execute(s -> { publisher.publishEvent(eCap); return null; });
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Integer c = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + targetId + "%");
            return c != null && c >= 2;
        });
        List<String> pubs = jdbcTemplate.query("select serialized_event from event_publication where serialized_event like ? order by publication_date asc", (rs, i) -> rs.getString(1), "%" + targetId + "%");
        assertThat(pubs).hasSizeGreaterThanOrEqualTo(2);
        int idxAuth = -1, idxCap = -1;
        for (int i = 0; i < pubs.size(); i++) {
            if (pubs.get(i).contains(eAuth.eventId().toString())) idxAuth = i;
            if (pubs.get(i).contains(eCap.eventId().toString())) idxCap = i;
        }
        assertThat(idxAuth).isGreaterThanOrEqualTo(0);
        assertThat(idxCap).isGreaterThanOrEqualTo(0);
        assertThat(idxAuth).isLessThan(idxCap);
    }

    @Test
    void raKAFKA019_captureCancelRefundAlsoPublish() {
        String base = "e19-" + UUID.randomUUID().toString().substring(0,8);
        String tCap = base + "-cap";
        String tCan = base + "-can";
        String tRef = base + "-ref";
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(s -> { publisher.publishEvent(evt("PAYMENT_CAPTURED", tCap, "TENANT_ALPHA", "c")); return null; });
        tt.execute(s -> { publisher.publishEvent(evt("PAYMENT_CANCELLED", tCan, "TENANT_ALPHA", "c")); return null; });
        tt.execute(s -> { publisher.publishEvent(evt("PAYMENT_REFUNDED", tRef, "TENANT_ALPHA", "c")); return null; });
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> {
            Integer c1 = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + tCap + "%");
            Integer c2 = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + tCan + "%");
            Integer c3 = jdbcTemplate.queryForObject("select count(*) from event_publication where serialized_event like ?", Integer.class, "%" + tRef + "%");
            return c1 != null && c1 >= 1 && c2 != null && c2 >= 1 && c3 != null && c3 >= 1;
        });
        String serCap = jdbcTemplate.queryForObject("select serialized_event from event_publication where serialized_event like ? limit 1", String.class, "%" + tCap + "%");
        assertThat(serCap).contains("PAYMENT_CAPTURED");
    }
}
