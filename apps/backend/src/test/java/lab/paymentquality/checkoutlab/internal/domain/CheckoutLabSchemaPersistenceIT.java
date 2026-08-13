package lab.paymentquality.checkoutlab.internal.domain;

import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutEventRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutFulfillmentRepository;
import lab.paymentquality.checkoutlab.internal.infrastructure.JpaCheckoutSessionRepository;
import lab.paymentquality.testsupport.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class CheckoutLabSchemaPersistenceIT extends PostgresContainerSupport {

    @Container
    static PostgreSQLContainer postgres = newPostgresContainer("checkout_lab_schema_persistence_it");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, postgres);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    JpaCheckoutSessionRepository sessionRepository;

    @Autowired
    JpaCheckoutEventRepository eventRepository;

    @Autowired
    JpaCheckoutFulfillmentRepository fulfillmentRepository;

    @Test
    void flywayCreatesCheckoutLabTables() {
        assertThat(tableExists("checkout_session")).isTrue();
        assertThat(tableExists("checkout_event")).isTrue();
        assertThat(tableExists("checkout_fulfillment")).isTrue();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history
                WHERE script = 'V12__create_checkout_lab.sql'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void jpaEntitiesRoundTripAgainstCheckoutLabSchema() {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        UUID sessionId = UUID.randomUUID();
        UUID fulfillmentId = UUID.randomUUID();
        UUID eventRowId = UUID.randomUUID();

        CheckoutSession session = new CheckoutSession();
        session.assignForPersistence(
                sessionId,
                "BOOK-123",
                1999L,
                "PLN",
                CheckoutSessionStatus.CREATED,
                "http://localhost:3000/checkout-lab/return",
                "http://localhost:8080/api/checkout-lab/notify",
                "http://localhost:3000/psp/checkout/" + sessionId,
                now.plusSeconds(900),
                "corr-smoke-01",
                now,
                now);
        sessionRepository.saveAndFlush(session);

        CheckoutFulfillment fulfillment = new CheckoutFulfillment();
        fulfillment.assignForPersistence(
                fulfillmentId,
                sessionId,
                CheckoutFulfillmentStatus.AWAITING_PAYMENT,
                now,
                now);
        fulfillmentRepository.saveAndFlush(fulfillment);

        CheckoutEvent event = new CheckoutEvent();
        event.assignForPersistence(
                eventRowId,
                "evt-smoke-01",
                sessionId,
                "payment.completed",
                Map.of("sessionId", sessionId.toString(), "status", "COMPLETED"),
                "t=0,v1=test",
                now,
                CheckoutEventProcessStatus.RECEIVED,
                202);
        eventRepository.saveAndFlush(event);

        assertThat(sessionRepository.findById(sessionId)).get()
                .satisfies(found -> {
                    assertThat(found.getExtOrderId()).isEqualTo("BOOK-123");
                    assertThat(found.getStatus()).isEqualTo(CheckoutSessionStatus.CREATED);
                });
        assertThat(fulfillmentRepository.findById(fulfillmentId)).get()
                .extracting(CheckoutFulfillment::getStatus)
                .isEqualTo(CheckoutFulfillmentStatus.AWAITING_PAYMENT);
        assertThat(eventRepository.findById(eventRowId)).get()
                .satisfies(found -> {
                    assertThat(found.getEventId()).isEqualTo("evt-smoke-01");
                    assertThat(found.getPayload()).containsEntry("status", "COMPLETED");
                });

        assertThat(jdbcTemplate.queryForObject(
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_name = 'checkout_event' AND column_name = 'payload'",
                String.class)).isEqualTo("jsonb");
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count == 1;
    }
}
