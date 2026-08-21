package lab.paymentquality.ops;

import java.time.Instant;
import java.util.UUID;

public record OpsFeedFrame(
        UUID eventId,
        Instant occurredAt,
        UUID merchantId,
        UUID paymentOrderId,
        String type,
        String label
) {
}
