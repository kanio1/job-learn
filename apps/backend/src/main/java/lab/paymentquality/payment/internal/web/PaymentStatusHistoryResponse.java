package lab.paymentquality.payment.internal.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentStatusHistoryResponse(List<StatusHistoryEntry> content) {

    public record StatusHistoryEntry(
            UUID statusHistoryId,
            UUID paymentOrderId,
            String fromStatus,
            String toStatus,
            String action,
            String actorSubject,
            String idempotencyKeyHash,
            String correlationId,
            Instant createdAt
    ) {
    }
}
