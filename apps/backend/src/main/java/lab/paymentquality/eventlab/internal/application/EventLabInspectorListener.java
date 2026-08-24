package lab.paymentquality.eventlab.internal.application;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lab.paymentquality.eventlab.internal.EventLabTopics;
import lab.paymentquality.eventlab.internal.domain.EventLabProcessed;
import lab.paymentquality.eventlab.internal.infrastructure.JpaEventLabProcessedRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
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
    private static final String GROUP = EventLabTopics.INSPECTOR_GROUP;
    private static final String TOPIC = EventLabTopics.AUDITABLE_ACTIONS;

    private final JpaEventLabProcessedRepository repository;
    private final ObjectMapper objectMapper;

    public EventLabInspectorListener(JpaEventLabProcessedRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @RetryableTopic(
            attempts = "3",
            backOff = @BackOff(delay = 500, multiplier = 1.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "true",
            include = GamblingException.class,
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlt",
            numPartitions = "3",
            replicationFactor = "1"
    )
    @KafkaListener(
            topics = TOPIC,
            groupId = GROUP,
            properties = {
                    "auto.offset.reset=earliest",
                    "key.deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                    "value.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer"
            }
    )
    public void onMessage(ConsumerRecord<String, byte[]> record,
                          @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic,
                          @Header(value = KafkaHeaders.OFFSET, required = false) Long offset,
                          @Header(value = KafkaHeaders.RECEIVED_PARTITION, required = false) Integer partition) {
        String payload = record.value() == null ? null : new String(record.value(), StandardCharsets.UTF_8);
        try {
            Map<String, Object> map = payload == null || payload.isBlank() ? Map.of() : objectMapper.readValue(payload, new TypeReference<>() {});
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
            String recordKey = record.key();
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
                    recordKey);
            repository.save(entity);
        } catch (GamblingException ge) {
            throw ge;
        } catch (DataIntegrityViolationException dup) {
            log.debug("duplicate eventId ignored via unique constraint");
        } catch (Exception e) {
            log.warn("failed to process record", e);
            throw new GamblingException(e.getMessage(), e);
        }
    }

    @DltHandler
    @Transactional
    public void dlt(ConsumerRecord<String, byte[]> record,
                    @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String dltTopic) {
        String payload = record.value() == null ? null : new String(record.value(), StandardCharsets.UTF_8);
        Map<String, Object> map = payload == null || payload.isBlank() ? Map.of() : objectMapper.readValue(payload, new TypeReference<>() {});
        String eventIdStr = headerOrField(record, "eventId", map);
        UUID eventId = eventIdStr != null ? UUID.fromString(eventIdStr) : UUID.randomUUID();
        String action = headerOrField(record, "action", map);
        String targetType = headerOrField(record, "targetType", map);
        String targetId = headerOrField(record, "targetId", map);
        String tenantRef = headerOrField(record, "tenantRef", map);
        String rk = record.key();
        EventLabProcessed dead = EventLabProcessed.of(
                GROUP, eventId, action != null ? action : "UNKNOWN", targetType != null ? targetType : "UNKNOWN",
                targetId != null ? targetId : "UNKNOWN", tenantRef != null ? tenantRef : "PLATFORM_TENANT",
                "DEAD", dltTopic != null ? dltTopic : EventLabTopics.DLT,
                record.partition() >= 0 ? record.partition() : 0,
                record.offset(), rk);
        dead.setLastError("poison");
        repository.findByConsumerGroupAndEventId(GROUP, eventId)
                .ifPresentOrElse(existing -> {
                    // Same event may already be PROCESSED from a prior successful consume;
                    // poison must flip the observable row to DEAD (idempotent upsert) and
                    // point it at the contract DLT record.
                    existing.setStatus("DEAD");
                    existing.setLastError("poison");
                    existing.setTopic(dltTopic != null ? dltTopic : EventLabTopics.DLT);
                    existing.setRecordOffset(record.offset());
                    existing.setPartitionNo(record.partition() >= 0 ? record.partition() : 0);
                    existing.setRecordKey(rk);
                    repository.save(existing);
                }, () -> repository.save(dead));
    }

    private static String headerString(ConsumerRecord<String, byte[]> record, String key) {
        var h = record.headers().lastHeader(key);
        if (h == null) return null;
        return new String(h.value(), StandardCharsets.UTF_8);
    }

    private static String headerOrField(ConsumerRecord<String, byte[]> record, String key, Map<String, Object> map) {
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
