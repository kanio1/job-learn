package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.MerchantOwnership;
import lab.paymentquality.merchant.MerchantOwnershipLocator;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MerchantOwnershipLocatorAdapter implements MerchantOwnershipLocator {

    private final JpaMerchantRepository repository;

    public MerchantOwnershipLocatorAdapter(JpaMerchantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MerchantOwnership> find(UUID merchantId) {
        return repository.findById(merchantId)
                .map(merchant -> new MerchantOwnership(merchant.getMerchantId(), merchant.getTenantId()));
    }
}
