package lab.paymentquality.shared.events;

import java.time.Instant;
import java.util.Map;

/**
 * beforeState/afterState are optional field-level snapshots for the audit
 * diff drawer (F-D7) — null when the action has no meaningful before/after
 * state (e.g. a read, or a creation with no prior state).
 */
public record AuditableActionOccurred(
        Instant occurredAt,
        String actorSubject,
        String actorDisplay,
        String action,
        String targetType,
        String targetId,
        String tenantRef,
        String correlationId,
        Outcome outcome,
        Map<String, Object> beforeState,
        Map<String, Object> afterState
) {
    public AuditableActionOccurred(
            Instant occurredAt,
            String actorSubject,
            String actorDisplay,
            String action,
            String targetType,
            String targetId,
            String tenantRef,
            String correlationId,
            Outcome outcome) {
        this(occurredAt, actorSubject, actorDisplay, action, targetType, targetId, tenantRef, correlationId,
                outcome, null, null);
    }
}
