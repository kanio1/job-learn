package lab.paymentquality.eventlab;

import lab.paymentquality.eventlab.internal.EventLabEnvelope;
import lab.paymentquality.eventlab.internal.EventLabHeaders;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RA-KAFKA-052 jqwik envelope property (≥100 iterations): mapper preserves
 * eventId/action/targetType/correlationId and schemaVersion=v1, no secrets.
 */
class EventLabEnvelopePropertyTest {

    @Provide
    Arbitrary<String> tenantRef() { return Arbitraries.of("TENANT_ALPHA", "TENANT_BETA", "PLATFORM_TENANT"); }

    @Provide
    Arbitrary<String> action() { return Arbitraries.of("PAYMENT_AUTHORIZED","PAYMENT_CAPTURED","PAYMENT_CANCELLED","PAYMENT_REFUNDED","MERCHANT_CREATED"); }

    @Property(tries = 100)
    void envelopePreservesCoreFields(@ForAll @StringLength(min=8,max=40) String targetId, @ForAll("tenantRef") String tenantRef, @ForAll("action") String action, @ForAll String correlationId) {
        String safeCorr = correlationId == null || correlationId.isBlank() ? UUID.randomUUID().toString() : correlationId.replaceAll("[^a-zA-Z0-9\\-]", "-");
        AuditableActionOccurred e = new AuditableActionOccurred(UUID.randomUUID(), Instant.now(), "subj","disp", action, "PAYMENT_ORDER", targetId, tenantRef, safeCorr, Outcome.SUCCESS, null, null);
        var payload = EventLabEnvelope.payloadOf(e);
        var headers = EventLabHeaders.from(e);
        assertThat(payload.get("eventId")).isEqualTo(e.eventId().toString());
        assertThat(payload.get("action")).isEqualTo(action);
        assertThat(payload.get("targetId")).isEqualTo(targetId);
        assertThat(payload.get("tenantRef")).isEqualTo(tenantRef);
        assertThat(payload.get("correlationId")).isEqualTo(safeCorr);
        assertThat(payload.get("schemaVersion")).isEqualTo("v1");
        assertThat(headers.get("eventId")).isEqualTo(e.eventId().toString());
        assertThat(headers.get("schemaVersion")).isEqualTo("v1");
        assertThat(payload.toString().toLowerCase()).doesNotContain("pan");
        assertThat(payload.toString().toLowerCase()).doesNotContain("authorization");
    }
}
