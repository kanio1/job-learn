package lab.paymentquality.tenant.internal.application;

import lab.paymentquality.tenant.TenantSeed;
import lab.paymentquality.tenant.TenantSeedCapability;
import lab.paymentquality.tenant.internal.domain.Tenant;
import lab.paymentquality.tenant.internal.domain.TenantStatus;
import lab.paymentquality.tenant.internal.domain.TenantType;
import lab.paymentquality.tenant.internal.infrastructure.JpaTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
class TenantSeedService implements TenantSeedCapability {

    private static final Instant SEED_CREATED_AT = Instant.parse("2026-01-15T08:00:00Z");

    private final JpaTenantRepository repository;

    TenantSeedService(JpaTenantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void seed(List<TenantSeed> tenants) {
        for (TenantSeed seed : tenants) {
            var tenantType = TenantType.valueOf(seed.tenantType());
            var status = TenantStatus.valueOf(seed.status());
            var tenant = repository.findById(seed.tenantId())
                    .map(existing -> {
                        existing.applySeed(seed.tenantReference(), seed.name(), tenantType, status);
                        return existing;
                    })
                    .orElseGet(() -> Tenant.seeded(
                            seed.tenantId(), seed.tenantReference(), seed.name(),
                            tenantType, status, SEED_CREATED_AT));
            repository.save(tenant);
        }
    }

    @Override
    @Transactional
    public void clear() {
        repository.deleteAllInBatch();
    }
}
