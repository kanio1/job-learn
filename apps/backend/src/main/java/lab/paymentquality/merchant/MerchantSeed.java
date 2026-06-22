package lab.paymentquality.merchant;

import java.util.UUID;

public record MerchantSeed(
        UUID merchantId,
        String merchantReference,
        String displayName,
        String status,
        UUID tenantId
) {
}
