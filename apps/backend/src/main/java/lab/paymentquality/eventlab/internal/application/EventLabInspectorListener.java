package lab.paymentquality.eventlab.internal.application;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("kafka")
@ConditionalOnProperty(name = "app.event-lab.enabled", havingValue = "true")
public class EventLabInspectorListener {

    private static final Logger log = LoggerFactory.getLogger(EventLabInspectorListener.class);
    private static final String GROUP = "eventlab-inspector";
    private static final String TOPIC = "lab.auditable-actions.v1";
    private static final String DLT = "lab.event-lab.dlq.v1";

    private final JpaEventLabProcessedRepository repository;
    private final ObjectMapper objectMapper;

    public EventLabInspectorListener(JpaEventLabProcessedRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP,
            properties = {"auto.offset.reset=earliest"}
    )
    @Transactional
    public void onMessage(ConsumerRecord<String, String> record,
                          @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                          @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
                          @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition) {
        String payload = record.value();
        try {
            Map<String, Object> map = objectMapper.readValue(payload, new TypeReference<>() {});
            String eventIdStr = headerOrField(record, "eventId", map);
            String action = headerOrField(record, "action", map);
            String targetType = headerOrField(record, "targetType", map);
            String targetId = headerOrField(record, "targetId", map);
            String tenantRef = headerOrField(record, "tenantRef", map);
            String schemaVersion = headerOrField(record, "schemaVersion", map);
            if (eventIdStr == null || eventIdStr.isBlank()) {
                throw new GamblingException("missing eventId");
            }
            UUID eventId = UUID.fromString(eventIdStr);
            // Poison marker for tests: if payload contains "poison":true or header poison=true
            Object poison = map.get("poison");
            if ("true".equalsIgnoreCase(String.valueOf(poison)) || "true".equalsIgnoreCase(headerString(record, "poison"))) {
                throw new GamblingException("poison pill");
            }
            if (repository.findByConsumerGroupAndEventId(GROUP, eventId).isPresent()) {
                log.debug("duplicate eventId {} ignored", eventId);
                return;
            }
            EventLabProcessed entity = EventLabProcessed.of(
                    GROUP, eventId,
                    action != null ? action : "UNKNOWN",
                    targetType != null ? targetType : "UNKNOWN",
                    targetId != null ? targetId : "UNKNOWN",
                    tenantRef != null ? tenantRef : "PLATFORM_TENANT",
                    "PROCESSED",
                    topic != null ? topic : TOPIC,
                    partition != null ? partition : 0,
                    offset != null ? offset : 0L,
                    record.key());
            repository.save(entity);
        } catch (GamblingException ge) {
            throw ge;
        } catch (Exception e) {
            log.warn("failed to process record", e);
            throw new GamblingException(e.getMessage(), e);
        }
    }

    @org.springframework.kafka.annotation.DltHandler
    public void dlt(ConsumerRecord<String, String> record) {
        try {
            Map<String, Object> map = objectMapper.readValue(record.value(), new TypeReference<>() {});
            String eventIdStr = headerOrField(record, "eventId", map);
            UUID eventId = eventIdStr != null ? UUID.fromString(eventIdStr) : UUID.randomUUID();
            String action = headerOrField(record, "action", map);
            String targetType = headerOrField(record, "targetType", map);
            String targetId = headerOrField(record, "targetId", map);
            String tenantRef = headerOrField(record, "tenantRef", map);
            EventLabProcessed dead = EventLabProcessed.of(
                    GROUP, eventId, action != null ? action : "UNKNOWN", targetType != null ? targetType : "UNKNOWN",
                    targetId != null ? targetId : "UNKNOWN", tenantRef != null ? tenantRef : "PLATFORM_TENANT",
                    "DEAD", "lab.event-lab.dlq.v1", 0, 0L, record.key());
            dead.setLastError("poison");
            // idempotent save
            if (repository.findByConsumerGroupAndEventId(GROUP, eventId).isEmpty()) {
                repository.save(dead);
            }
        } catch (Exception ex) {
            log.error("dlt handler failed", ex);
        }
    }

    private static String headerString(ConsumerRecord<String, String> record, String key) {
        var h = record.headers().lastHeader(key);
        if (h == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }

    private static String headerOrField(ConsumerRecord<String, String> record, String key, Map<String, Object> map) {
        String hv = headerString(record, key);
        if (hv != null) return hv;
        Object fv = map.get(key);
        return fv != null ? String.valueOf(fv) : null;
    }

    static class GamblingException extends RuntimeException {
        GamblingException(String msg) { super(msg); }
        GamblingException(String msg, Throwable cause) { super(msg, cause); }
    }
}
