package lab.paymentquality.audit.internal.web.dto;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.shared.events.Outcome;

import java.time.Instant;
import java.util.UUID;

public record AuditEventSummary(
        UUID id,
        Instant occurredAt,
        String actorDisplay,
        String action,
        String targetType,
        String targetId,
        String tenantId,
        String correlationId,
        Outcome outcome
) {
    public static AuditEventSummary from(AuditEvent event) {
        return new AuditEventSummary(
                event.getId(),
                event.getOccurredAt(),
                event.getActorDisplay(),
                event.getAction(),
                event.getTargetType(),
                event.getTargetId(),
                event.getTenantId(),
                event.getCorrelationId(),
                event.getOutcome());
    }
}
