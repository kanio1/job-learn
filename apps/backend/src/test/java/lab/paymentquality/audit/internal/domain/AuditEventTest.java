package lab.paymentquality.audit.internal.domain;

import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventTest {

    @Test
    void fromEventCopiesOnlyExplicitContractFieldsAndAssignsId() {
        Instant occurredAt = Instant.parse("2026-06-19T09:00:00Z");
        AuditableActionOccurred source = new AuditableActionOccurred(
                occurredAt,
                "actor-17",
                "Audit Operator",
                "PAYMENT_CAPTURED",
                "PAYMENT_ORDER",
                "payment-17",
                "TENANT_ALPHA",
                "correlation-17",
                Outcome.SUCCESS);

        AuditEvent event = AuditEvent.fromEvent(source);

        assertThat(event.getId()).isNotNull();
        assertThat(event.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(event.getActorSubject()).isEqualTo("actor-17");
        assertThat(event.getActorDisplay()).isEqualTo("Audit Operator");
        assertThat(event.getAction()).isEqualTo("PAYMENT_CAPTURED");
        assertThat(event.getTargetType()).isEqualTo("PAYMENT_ORDER");
        assertThat(event.getTargetId()).isEqualTo("payment-17");
        assertThat(event.getTenantId()).isEqualTo("TENANT_ALPHA");
        assertThat(event.getCorrelationId()).isEqualTo("correlation-17");
        assertThat(event.getOutcome()).isEqualTo(Outcome.SUCCESS);
        assertThat(event.getBeforeState()).isNull();
        assertThat(event.getAfterState()).isNull();
    }

    @Test
    void fromEventCopiesBeforeAndAfterStateWhenPresent() {
        AuditableActionOccurred source = new AuditableActionOccurred(
                Instant.parse("2026-06-19T09:00:00Z"),
                "actor-18",
                "Audit Operator",
                "MERCHANT_ACTIVATED",
                "MERCHANT",
                "merchant-18",
                "TENANT_ALPHA",
                "correlation-18",
                Outcome.SUCCESS,
                Map.of("status", "PENDING"),
                Map.of("status", "ACTIVE"));

        AuditEvent event = AuditEvent.fromEvent(source);

        assertThat(event.getBeforeState()).containsExactly(Map.entry("status", "PENDING"));
        assertThat(event.getAfterState()).containsExactly(Map.entry("status", "ACTIVE"));
    }
}
