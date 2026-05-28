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
        Instant createdAt,
        Instant updatedAt
) {
}
