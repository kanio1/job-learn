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

    private static final String[] ALLOWED_SUMMARY_FIELDS = {
            "id", "occurredAt", "actorDisplay", "action", "targetType",
            "targetId", "tenantId", "correlationId", "outcome"
    };

    // Detail additionally exposes beforeState/afterState for the audit diff
    // drawer (F-D7) — intentionally NOT on the list-view Summary, which stays
    // list-weight only.
    private static final String[] ALLOWED_DETAIL_FIELDS = {
            "id", "occurredAt", "actorDisplay", "action", "targetType",
            "targetId", "tenantId", "correlationId", "outcome", "beforeState", "afterState"
    };

    private static final String[] ALLOWED_EXPORT_FIELDS = {
            "eventId", "occurredAt", "actorDisplay", "action", "targetType",
            "targetId", "correlationId", "outcome"
    };

    @Test
    void summaryExposesExactlyTheSafeFieldSet() {
        assertThat(componentNames(AuditEventSummary.class)).containsExactly(ALLOWED_SUMMARY_FIELDS);
    }

    @Test
    void detailExposesExactlyTheSafeFieldSetPlusDiffState() {
        assertThat(componentNames(AuditEventDetail.class)).containsExactly(ALLOWED_DETAIL_FIELDS);
    }

    @Test
    void exportEventExposesOnlyComplianceSafeFieldSet() {
        assertThat(componentNames(AuditExportEvent.class)).containsExactly(ALLOWED_EXPORT_FIELDS);
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
        AuditExportEvent exportEvent = AuditExportEvent.from(event);

        assertThat(summary.actorDisplay()).isEqualTo("Visible Operator");
        assertThat(detail.actorDisplay()).isEqualTo("Visible Operator");
        assertThat(exportEvent.actorDisplay()).isEqualTo("Visible Operator");
        assertThat(exportEvent.eventId()).isEqualTo(event.getId());
        assertThat(summary.targetId()).isEqualTo("user-9");
        assertThat(detail.tenantId()).isEqualTo("TENANT_ALPHA");
    }

    private static String[] componentNames(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
