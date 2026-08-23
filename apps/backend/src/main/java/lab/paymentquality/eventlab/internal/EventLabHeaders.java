package lab.paymentquality.eventlab.internal;

import lab.paymentquality.shared.events.AuditableActionOccurred;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * v1 header builder for lab.auditable-actions.v1. Header values are String/byte[] as required by Kafka.
 */
public final class EventLabHeaders {

    private EventLabHeaders() {}

    public static Map<String, Object> from(AuditableActionOccurred event) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("eventId", event.eventId().toString());
        headers.put("action", event.action());
        headers.put("targetType", event.targetType());
        headers.put("tenantRef", event.tenantRef());
        headers.put("correlationId", event.correlationId());
        headers.put("occurredAt", event.occurredAt().toString());
        headers.put("schemaVersion", "v1");
        return Map.copyOf(headers);
    }

    /**
     * Helper for tests: convert header map to Kafka header bytes style check.
     */
    public static Map<String, byte[]> asBytes(Map<String, Object> headers) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        headers.forEach((k, v) -> out.put(k, String.valueOf(v).getBytes(StandardCharsets.UTF_8)));
        return out;
    }
}
