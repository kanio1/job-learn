package lab.paymentquality.tenant.internal.application;

import lab.paymentquality.tenant.TenantDirectory;
import lab.paymentquality.tenant.TenantSummary;
import lab.paymentquality.tenant.internal.domain.Tenant;
import lab.paymentquality.tenant.internal.infrastructure.JpaTenantRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class TenantDirectoryService implements TenantDirectory {

    private final JpaTenantRepository repository;

    TenantDirectoryService(JpaTenantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantSummary> listAll() {
        return repository.findAll(Sort.by("name")).stream()
                .map(TenantDirectoryService::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantSummary> findById(UUID tenantId) {
        return repository.findById(tenantId).map(TenantDirectoryService::toSummary);
    }

    private static TenantSummary toSummary(Tenant tenant) {
        return new TenantSummary(tenant.getTenantId(), tenant.getTenantReference(), tenant.getName());
    }
}
