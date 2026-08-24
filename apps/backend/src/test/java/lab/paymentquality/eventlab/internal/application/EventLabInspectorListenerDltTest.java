package lab.paymentquality.eventlab.internal.application;

import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-9: the DLT handler must not silently swallow failures. When the repository
 * write fails inside {@code dlt}, the exception must propagate to the Kafka
 * container so the record is retried/observed — no false success.
 */
class EventLabInspectorListenerDltTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static ConsumerRecord<String, byte[]> poisonRecord(UUID eventId, String key) {
        String payload = """
                {"eventId":"%s","action":"PAYMENT_AUTHORIZED","targetType":"PAYMENT_ORDER","targetId":"%s","tenantRef":"TENANT_ALPHA","poison":true}
                """.formatted(eventId, key);
        ConsumerRecord<String, byte[]> record = new ConsumerRecord<>(
                "lab.event-lab.dlq.v1", 0, 7L, key, payload.getBytes(StandardCharsets.UTF_8));
        record.headers().add("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8));
        return record;
    }

    @Test
    void dltPropagatesLookupFailure_noFalseSuccess() {
        JpaEventLabProcessedRepository repo = mock(JpaEventLabProcessedRepository.class);
        when(repo.findByConsumerGroupAndEventId(any(), any())).thenThrow(new IllegalStateException("db down"));
        EventLabInspectorListener listener = new EventLabInspectorListener(repo, objectMapper);

        UUID eventId = UUID.randomUUID();
        ConsumerRecord<String, byte[]> rec = poisonRecord(eventId, "poison-key-1");

        // The handler must not swallow the exception.
        assertThatThrownBy(() -> listener.dlt(rec, "lab.event-lab.dlq.v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db down");
    }

    @Test
    void dltPropagatesSaveFailureWhenUpsertingExistingRow() {
        JpaEventLabProcessedRepository repo = mock(JpaEventLabProcessedRepository.class);
        EventLabProcessed existing = EventLabProcessed.of(
                "eventlab-inspector", UUID.randomUUID(), "PAYMENT_AUTHORIZED", "PAYMENT_ORDER",
                "k", "TENANT_ALPHA", "PROCESSED", "lab.auditable-actions.v1", 0, 1L, "k");
        when(repo.findByConsumerGroupAndEventId(any(), any())).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenThrow(new IllegalStateException("flush failed"));
        EventLabInspectorListener listener = new EventLabInspectorListener(repo, objectMapper);

        UUID eventId = UUID.randomUUID();
        ConsumerRecord<String, byte[]> rec = poisonRecord(eventId, "poison-key-2");

        assertThatThrownBy(() -> listener.dlt(rec, "lab.event-lab.dlq.v1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("flush failed");
    }
}