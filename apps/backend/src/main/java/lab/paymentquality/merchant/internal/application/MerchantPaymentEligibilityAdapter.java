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

    private MerchantPaymentEligibility toEligibility(Merchant merchant) {
        return new MerchantPaymentEligibility(
                merchant.getMerchantId(),
                merchant.getNormalizedReference(),
                merchant.getStatus() == MerchantStatus.ACTIVE
        );
    }
}
