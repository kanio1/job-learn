package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.MerchantPaymentEligibility;
import lab.paymentquality.merchant.MerchantPaymentEligibilityService;
import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.domain.MerchantStatus;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class MerchantPaymentEligibilityAdapter implements MerchantPaymentEligibilityService {

    private final JpaMerchantRepository repository;

    public MerchantPaymentEligibilityAdapter(JpaMerchantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MerchantPaymentEligibility> findEligibility(UUID merchantId) {
        return repository.findById(merchantId)
                .map(this::toEligibility);
    }

    @Override
    public Optional<MerchantPaymentEligibility> findEligibilityByReference(String merchantReference) {
        if (merchantReference == null || merchantReference.isBlank()) {
            return Optional.empty();
        }

        // This is an identity-claim lookup, not create-merchant input validation.
        // The established Keycloak/seed contract uses MERCHANT_ALPHA_001, while
        // newly submitted merchant references intentionally use a stricter public
        // format. Resolve the opaque claim against the persisted canonical value.
        String canonicalReference = merchantReference.trim().toUpperCase(Locale.ROOT);
        return repository.findByNormalizedReference(canonicalReference)
                .map(this::toEligibility);
    }

    private MerchantPaymentEligibility toEligibility(Merchant merchant) {
        return new MerchantPaymentEligibility(
                merchant.getMerchantId(),
                merchant.getNormalizedReference(),
                merchant.getStatus() == MerchantStatus.ACTIVE
        );
    }
}
