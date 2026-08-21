package lab.paymentquality.ops.internal.web;

import lab.paymentquality.ops.OpsFeedFrame;

import java.time.Instant;
import java.util.UUID;

public record InjectFeedResponse(
        UUID eventId,
        Instant occurredAt,
        UUID merchantId,
        UUID paymentOrderId,
        String type,
        String label,
        boolean malformed
) {

    public static InjectFeedResponse fromFrame(OpsFeedFrame frame) {
        return new InjectFeedResponse(
                frame.eventId(),
                frame.occurredAt(),
                frame.merchantId(),
                frame.paymentOrderId(),
                frame.type(),
                frame.label(),
                false);
    }

    public static InjectFeedResponse ignoredRaw() {
        return new InjectFeedResponse(null, null, null, null, null, null, true);
    }
}
