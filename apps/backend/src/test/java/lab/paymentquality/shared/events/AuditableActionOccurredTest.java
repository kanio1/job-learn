package lab.paymentquality.shared.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuditableActionOccurredTest {

    @Test
    void storesExplicitSafeMetadata() {
        Instant occurredAt = Instant.parse("2026-06-18T20:00:00Z");

        var event = new AuditableActionOccurred(
                occurredAt,
                "subject-123",
                "Platform Admin",
                "MERCHANT_CREATED",
                "MERCHANT",
                "merchant-123",
                "TENANT_ALPHA",
                "correlation-123",
                Outcome.SUCCESS);

        assertThat(event.occurredAt()).isEqualTo(occurredAt);
        assertThat(event.actorSubject()).isEqualTo("subject-123");
        assertThat(event.actorDisplay()).isEqualTo("Platform Admin");
        assertThat(event.action()).isEqualTo("MERCHANT_CREATED");
        assertThat(event.targetType()).isEqualTo("MERCHANT");
        assertThat(event.targetId()).isEqualTo("merchant-123");
        assertThat(event.tenantRef()).isEqualTo("TENANT_ALPHA");
        assertThat(event.correlationId()).isEqualTo("correlation-123");
        assertThat(event.outcome()).isEqualTo(Outcome.SUCCESS);
    }

    @Test
    void outcomeContainsExactlyTheSupportedValues() {
        assertThat(Outcome.values())
                .containsExactly(Outcome.SUCCESS, Outcome.DENIED, Outcome.FAILED);
    }

    @Test
    void exposesNoSensitiveOrRawDataComponents() {
        Set<String> forbiddenNameFragments = Set.of(
                "token",
                "secret",
                "password",
                "authorization",
                "credential",
                "pan",
                "cvv",
                "payload",
                "body");

        assertThat(Arrays.stream(AuditableActionOccurred.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase(Locale.ROOT)))
                .noneMatch(name -> forbiddenNameFragments.stream().anyMatch(name::contains));
    }
}
