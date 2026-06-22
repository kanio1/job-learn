package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.MerchantSeed;
import lab.paymentquality.merchant.MerchantSeedCapability;
import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.domain.MerchantStatus;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
class MerchantSeedService implements MerchantSeedCapability {

    private static final Instant SEED_TIMESTAMP = Instant.parse("2026-01-15T09:00:00Z");

    private final JpaMerchantRepository repository;

    MerchantSeedService(JpaMerchantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void seed(List<MerchantSeed> merchants) {
        for (MerchantSeed seed : merchants) {
            var status = MerchantStatus.valueOf(seed.status());
            var merchant = repository.findById(seed.merchantId())
                    .map(existing -> {
                        existing.applySeed(seed.merchantReference(), seed.displayName(), seed.tenantId(),
                                status, SEED_TIMESTAMP);
                        return existing;
                    })
                    .orElseGet(() -> Merchant.seeded(
                            seed.merchantId(), seed.merchantReference(), seed.displayName(), seed.tenantId(),
                            status, SEED_TIMESTAMP));
            repository.save(merchant);
        }
    }

    @Override
    @Transactional
    public void clear() {
        repository.deleteAllInBatch();
    }
}
