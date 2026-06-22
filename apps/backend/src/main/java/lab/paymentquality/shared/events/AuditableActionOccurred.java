package lab.paymentquality.shared.events;

import java.time.Instant;

public record AuditableActionOccurred(
        Instant occurredAt,
        String actorSubject,
        String actorDisplay,
        String action,
        String targetType,
        String targetId,
        String tenantRef,
        String correlationId,
        Outcome outcome
) {
}
