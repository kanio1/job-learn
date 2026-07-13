package lab.paymentquality.merchant;

import java.util.Optional;
import java.util.UUID;

public interface MerchantPaymentEligibilityService {

    Optional<MerchantPaymentEligibility> findEligibility(UUID merchantId);

    Optional<MerchantPaymentEligibility> findEligibilityByReference(String merchantReference);
}
