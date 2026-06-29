package lab.paymentquality.payment.internal.web;

import java.time.Instant;
import java.util.UUID;

public record PaymentOrderNoteDto(
        UUID id,
        String body,
        String authorDisplay,
        Instant createdAt) {
}
