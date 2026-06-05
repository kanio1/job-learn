package lab.paymentquality.payment.internal.web;

import java.time.Instant;
import java.util.UUID;

public record PaymentLifecycleResponse(
        UUID paymentOrderId,
        UUID merchantId,
        String status,
        long amountMinor,
        String currency,
        Long capturedAmountMinor,
        Long refundedAmountMinor,
        Instant authorizedAt,
        Instant expiresAt,
        Instant capturedAt,
        Instant cancelledAt,
        Instant refundedAt,
        String cancellationReason,
        String refundReason
) {
}
