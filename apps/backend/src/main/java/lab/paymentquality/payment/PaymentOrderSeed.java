package lab.paymentquality.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentOrderSeed(
        UUID paymentOrderId,
        UUID merchantId,
        String clientOrderReference,
        long amountMinor,
        String currency,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant authorizedAt,
        Instant expiresAt,
        Instant capturedAt,
        Instant cancelledAt,
        Instant refundedAt,
        Long capturedAmountMinor,
        Long refundedAmountMinor,
        String cancellationReason,
        String refundReason
) {
}
