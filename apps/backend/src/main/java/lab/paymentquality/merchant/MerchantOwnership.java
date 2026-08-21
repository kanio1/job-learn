package lab.paymentquality.merchant;

import java.util.UUID;

public record MerchantOwnership(UUID merchantId, UUID tenantId) {
}
