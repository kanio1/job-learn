package lab.paymentquality.audit.internal.web.dto;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.shared.events.Outcome;

import java.time.Instant;
import java.util.UUID;

public record AuditEventDetail(
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
    public static AuditEventDetail from(AuditEvent event) {
        return new AuditEventDetail(
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
