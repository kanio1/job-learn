package lab.paymentquality.merchant;

import java.util.UUID;

public record MerchantPaymentEligibility(
        UUID merchantId,
        String normalizedReference,
        boolean active
) {
}
