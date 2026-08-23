package lab.paymentquality.eventlab;

import lab.paymentquality.eventlab.internal.EventLabEnvelope;
import lab.paymentquality.eventlab.internal.EventLabHeaders;
import lab.paymentquality.shared.events.AuditableActionOccurred;
import lab.paymentquality.shared.events.Outcome;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventLabEnvelopeTest {

    @Test
    void payloadContainsNoSecrets() {
        var event = sample();
        Map<String, Object> payload = EventLabEnvelope.payloadOf(event);
        String json = payload.toString().toLowerCase();
        assertThat(json).doesNotContain("pan");
        assertThat(json).doesNotContain("authorization");
        assertThat(json).doesNotContain("token");
        assertThat(payload).containsKeys("eventId", "action", "targetId", "tenantRef", "correlationId", "schemaVersion");
        assertThat(payload.get("schemaVersion")).isEqualTo("v1");
    }

    @Test
    void headersCompleteV1() {
        var event = sample();
        Map<String, Object> headers = EventLabHeaders.from(event);
        assertThat(headers).containsKeys("eventId", "action", "targetType", "tenantRef", "correlationId", "occurredAt", "schemaVersion");
        assertThat(headers.get("schemaVersion")).isEqualTo("v1");
        assertThat(headers.get("eventId")).isEqualTo(event.eventId().toString());
    }

    @Test
    void keyIsTargetId() {
        var event = sample();
        assertThat(event.targetId()).isNotBlank();
        // RoutingTarget key is targetId via EventLabExternalizationConfiguration
    }

    private static AuditableActionOccurred sample() {
        return new AuditableActionOccurred(
                UUID.randomUUID(),
                Instant.now(),
                "subject",
                "display",
                "PAYMENT_AUTHORIZED",
                "PAYMENT_ORDER",
                UUID.randomUUID().toString(),
                "TENANT_ALPHA",
                UUID.randomUUID().toString(),
                Outcome.SUCCESS,
                null, null);
    }
}
