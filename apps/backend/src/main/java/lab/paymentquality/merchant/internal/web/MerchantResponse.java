package lab.paymentquality.merchant.internal.web;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponse(
        UUID merchantId,
        String merchantReference,
        String displayName,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
