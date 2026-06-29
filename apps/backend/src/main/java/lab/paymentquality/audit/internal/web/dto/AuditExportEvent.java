package lab.paymentquality.audit.internal.web.dto;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.shared.events.Outcome;

import java.time.Instant;
import java.util.UUID;

public record AuditExportEvent(
        UUID eventId,
        Instant occurredAt,
        String actorDisplay,
        String action,
        String targetType,
        String targetId,
        String correlationId,
        Outcome outcome
) {
    public static AuditExportEvent from(AuditEvent event) {
        return new AuditExportEvent(
                event.getId(),
                event.getOccurredAt(),
                event.getActorDisplay(),
                event.getAction(),
                event.getTargetType(),
                event.getTargetId(),
                event.getCorrelationId(),
                event.getOutcome());
    }
}
