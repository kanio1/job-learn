package lab.paymentquality.payment.internal.web;

import java.time.Instant;
import java.util.UUID;

public record PaymentOrderResponse(
        UUID paymentOrderId,
        UUID merchantId,
        String clientOrderReference,
        long amountMinor,
        String currency,
        String status,
        Long capturedAmountMinor,
        Long refundedAmountMinor,
        Instant authorizedAt,
        Instant expiresAt,
        Instant capturedAt,
        Instant cancelledAt,
        Instant refundedAt,
        String cancellationReason,
        String refundReason,
        String metadata,
        Instant createdAt,
        Instant updatedAt
) {
}
