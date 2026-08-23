package lab.paymentquality.eventlab.internal;

import lab.paymentquality.shared.events.AuditableActionOccurred;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure mapper from AuditableActionOccurred to externalized payload.
 * Contains only safe fields; never includes PAN/secrets.
 */
public final class EventLabEnvelope {

    private EventLabEnvelope() {}

    public static Map<String, Object> payloadOf(AuditableActionOccurred event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", event.eventId().toString());
        payload.put("occurredAt", event.occurredAt().toString());
        payload.put("actorSubject", event.actorSubject());
        payload.put("actorDisplay", event.actorDisplay());
        payload.put("action", event.action());
        payload.put("targetType", event.targetType());
        payload.put("targetId", event.targetId());
        payload.put("tenantRef", event.tenantRef());
        payload.put("correlationId", event.correlationId());
        payload.put("outcome", event.outcome().name());
        payload.put("schemaVersion", "v1");
        if (event.beforeState() != null) payload.put("beforeState", event.beforeState());
        if (event.afterState() != null) payload.put("afterState", event.afterState());
        return Map.copyOf(payload);
    }
}
