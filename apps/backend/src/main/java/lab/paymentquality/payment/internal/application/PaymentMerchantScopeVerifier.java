package lab.paymentquality.payment.internal.application;

import lab.paymentquality.merchant.MerchantPaymentEligibilityService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentMerchantScopeVerifier {

    private final MerchantPaymentEligibilityService merchantPaymentEligibilityService;

    public PaymentMerchantScopeVerifier(MerchantPaymentEligibilityService merchantPaymentEligibilityService) {
        this.merchantPaymentEligibilityService = merchantPaymentEligibilityService;
    }

    public boolean matches(UUID merchantId, String merchantClaim) {
        if (merchantClaim == null || merchantClaim.isBlank()) {
            return false;
        }

        try {
            if (merchantId.equals(UUID.fromString(merchantClaim))) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            // Keycloak uses the merchant natural reference; UUID claims remain supported for existing clients.
        }

        return merchantPaymentEligibilityService.findEligibilityByReference(merchantClaim)
                .map(eligibility -> merchantId.equals(eligibility.merchantId()))
                .orElse(false);
    }
}
