package lab.paymentquality.ops.internal.web;

import java.time.Instant;
import java.util.UUID;

public record InjectFeedRequest(
        UUID eventId,
        Instant occurredAt,
        UUID merchantId,
        UUID paymentOrderId,
        String type,
        String label,
        String raw
) {
}
