package lab.paymentquality.audit.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_subject", length = 160, nullable = false, updatable = false)
    private String actorSubject;

    @Column(name = "actor_display", length = 160, nullable = false, updatable = false)
    private String actorDisplay;

    @Column(name = "action", length = 80, nullable = false, updatable = false)
    private String action;

    @Column(name = "target_type", length = 80, nullable = false, updatable = false)
    private String targetType;

    @Column(name = "target_id", length = 160, nullable = false, updatable = false)
    private String targetId;

    @Column(name = "tenant_id", length = 64, nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "correlation_id", length = 128, updatable = false)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 20, nullable = false, updatable = false)
    private Outcome outcome;

    protected AuditEvent() {
    }

    public static AuditEvent fromEvent(AuditableActionOccurred event) {
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.id = UUID.randomUUID();
        auditEvent.occurredAt = event.occurredAt();
        auditEvent.actorSubject = event.actorSubject();
        auditEvent.actorDisplay = event.actorDisplay();
        auditEvent.action = event.action();
        auditEvent.targetType = event.targetType();
        auditEvent.targetId = event.targetId();
        auditEvent.tenantId = event.tenantRef();
        auditEvent.correlationId = event.correlationId();
        auditEvent.outcome = event.outcome();
        return auditEvent;
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getActorSubject() {
        return actorSubject;
    }

    public String getActorDisplay() {
        return actorDisplay;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Outcome getOutcome() {
        return outcome;
    }
}
