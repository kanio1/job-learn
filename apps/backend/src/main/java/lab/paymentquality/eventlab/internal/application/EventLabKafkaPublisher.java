package lab.paymentquality.eventlab.internal.application;

import lab.paymentquality.eventlab.internal.EventLabEnvelope;
import lab.paymentquality.eventlab.internal.EventLabHeaders;
import lab.paymentquality.eventlab.internal.EventLabTopics;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes inject records through the real lab topic so the production
 * listener, retry budget and DLT are exercised — never bypasses the broker.
 * A poison inject adds a {@code poison=true} header + payload marker that
 * {@link EventLabInspectorListener} treats as a GamblingException, routing the
 * record through @RetryableTopic to the contract DLT.
 */
@Service
@Profile("kafka")
@ConditionalOnProperty(name = "app.event-lab.enabled", havingValue = "true")
public class EventLabKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventLabKafkaPublisher.class);
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(5);

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventLabKafkaPublisher(KafkaTemplate<Object, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /** Re-publishes an existing envelope (same eventId, same key) — used by duplicate injection. */
    public void publishDuplicate(UUID eventId, String targetId, String tenantRef, String correlationId, String action, String targetType) {
        publish(eventId, targetId, tenantRef, correlationId, action, targetType, false);
    }

    /** Publishes a poisoned envelope: the listener's {@code poison=true} path sends it to retry then DLT. */
    public void publishPoison(UUID eventId, String targetId, String tenantRef, String correlationId, String action, String targetType) {
        publish(eventId, targetId, tenantRef, correlationId, action, targetType, true);
    }

    private void publish(UUID eventId, String targetId, String tenantRef, String correlationId,
                         String action, String targetType, boolean poison) {
        AuditableActionOccurred event = new AuditableActionOccurred(
                eventId, Instant.now(), "injector", "Event Lab operator",
                action, targetType, targetId, tenantRef, correlationId, Outcome.SUCCESS, null, null);
        Map<String, Object> payload = new java.util.LinkedHashMap<>(EventLabEnvelope.payloadOf(event));
        if (poison) {
            payload.put("poison", Boolean.TRUE);
        }
        byte[] value;
        try {
            value = objectMapper.writeValueAsBytes(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize inject envelope", e);
        }
        ProducerRecord<Object, Object> record = new ProducerRecord<>(EventLabTopics.AUDITABLE_ACTIONS, targetId, value);
        EventLabHeaders.from(event).forEach((k, v) ->
                record.headers().add(k, String.valueOf(v).getBytes(StandardCharsets.UTF_8)));
        if (poison) {
            record.headers().add("poison", Boolean.TRUE.toString().getBytes(StandardCharsets.UTF_8));
        }
        try {
            kafkaTemplate.send(record).get(SEND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            log.info("eventlab inject published eventId={} poison={} topic={}", eventId, poison, EventLabTopics.AUDITABLE_ACTIONS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing inject", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to publish inject to Kafka", e);
        }
    }
}