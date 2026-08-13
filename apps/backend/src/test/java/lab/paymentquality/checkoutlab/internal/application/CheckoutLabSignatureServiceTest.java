package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.config.CheckoutLabProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CheckoutLabSignatureServiceTest {

    @Test
    void acceptsMatchingHmacAndRejectsTamperedBody() {
        CheckoutLabClock clock = new CheckoutLabClock();
        clock.setFixed(Instant.parse("2026-01-01T00:00:00Z"));
        CheckoutLabProperties properties = new CheckoutLabProperties(
                true, "id", "secret", "hmac-secret", 300, "http://localhost:3000");
        CheckoutLabSignatureService service = new CheckoutLabSignatureService(properties, clock);
        byte[] body = "{\"id\":\"evt_1\"}".getBytes(StandardCharsets.UTF_8);
        String header = service.sign(clock.instant().getEpochSecond(), body);

        assertDoesNotThrow(() -> service.verify(header, body));
        assertThatThrownBy(() -> service.verify(header, "{\"id\":\"evt_2\"}".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidCheckoutSignatureException.class);
    }

    @Test
    void simulateTokenMatchesSessionAndRejectsTamperedToken() {
        CheckoutLabClock clock = new CheckoutLabClock();
        clock.setFixed(Instant.parse("2026-01-01T00:00:00Z"));
        CheckoutLabProperties properties = new CheckoutLabProperties(
                true, "id", "secret", "hmac-secret", 300, "http://localhost:3000");
        CheckoutLabSignatureService service = new CheckoutLabSignatureService(properties, clock);
        UUID sessionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant validityUntil = Instant.parse("2026-01-01T00:10:00Z");

        CheckoutLabSimulateToken issued = service.issueSimulateToken(sessionId, validityUntil);
        assertDoesNotThrow(() -> service.verifySimulateToken(sessionId, issued.token(), validityUntil));
        assertThatThrownBy(() -> service.verifySimulateToken(sessionId, "00".repeat(32), validityUntil))
                .isInstanceOf(InvalidCheckoutSimulateTokenException.class);
    }

    @Test
    void simulateTokenRejectsExpiredSession() {
        CheckoutLabClock clock = new CheckoutLabClock();
        clock.setFixed(Instant.parse("2026-01-01T00:10:00Z"));
        CheckoutLabProperties properties = new CheckoutLabProperties(
                true, "id", "secret", "hmac-secret", 300, "http://localhost:3000");
        CheckoutLabSignatureService service = new CheckoutLabSignatureService(properties, clock);
        UUID sessionId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant validityUntil = Instant.parse("2026-01-01T00:05:00Z");

        assertThatThrownBy(() -> service.issueSimulateToken(sessionId, validityUntil))
                .isInstanceOf(InvalidCheckoutSimulateTokenException.class);
    }

    @Test
    void missingSimulateTokenHeaderIsRejected() {
        CheckoutLabClock clock = new CheckoutLabClock();
        CheckoutLabProperties properties = new CheckoutLabProperties(
                true, "id", "secret", "hmac-secret", 300, "http://localhost:3000");
        CheckoutLabSignatureService service = new CheckoutLabSignatureService(properties, clock);

        assertThatThrownBy(() -> service.verifySimulateToken(UUID.randomUUID(), null, null))
                .isInstanceOf(MissingCheckoutSimulateTokenException.class);
    }
}
