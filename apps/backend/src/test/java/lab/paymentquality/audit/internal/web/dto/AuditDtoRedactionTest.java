package lab.paymentquality.audit.internal.web.dto;

import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AuditDtoRedactionTest {

    private static final String[] ALLOWED_FIELDS = {
            "id", "occurredAt", "actorDisplay", "action", "targetType",
            "targetId", "tenantId", "correlationId", "outcome"
    };

    @Test
    void summaryAndDetailExposeExactlyTheSafeFieldSet() {
        assertThat(componentNames(AuditEventSummary.class)).containsExactly(ALLOWED_FIELDS);
        assertThat(componentNames(AuditEventDetail.class)).containsExactly(ALLOWED_FIELDS);
    }

    @Test
    void mappingUsesDisplayIdentityAndSafeEventMetadata() {
        AuditEvent event = AuditEvent.fromEvent(new AuditableActionOccurred(
                Instant.parse("2026-06-19T09:30:00Z"),
                "internal-actor-id",
                "Visible Operator",
                "USER_UPDATED",
                "USER",
                "user-9",
                "TENANT_ALPHA",
                "correlation-9",
                Outcome.SUCCESS));

        AuditEventSummary summary = AuditEventSummary.from(event);
        AuditEventDetail detail = AuditEventDetail.from(event);

        assertThat(summary.actorDisplay()).isEqualTo("Visible Operator");
        assertThat(detail.actorDisplay()).isEqualTo("Visible Operator");
        assertThat(summary.targetId()).isEqualTo("user-9");
        assertThat(detail.tenantId()).isEqualTo("TENANT_ALPHA");
    }

    private static String[] componentNames(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
