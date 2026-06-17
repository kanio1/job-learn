package lab.paymentquality.tenant.internal.application;

import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import lab.paymentquality.tenant.internal.domain.Tenant;
import lab.paymentquality.tenant.internal.domain.TenantStatus;
import lab.paymentquality.tenant.internal.domain.TenantType;
import lab.paymentquality.tenant.internal.infrastructure.JpaTenantRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TenantResolverService implements TenantResolver {

    private final JpaTenantRepository repository;

    TenantResolverService(JpaTenantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantContext resolve(Jwt jwt) {
        String claim = jwt.getClaimAsString("tenant_id");
        if (claim == null || claim.isBlank()) {
            throw new TenantResolutionException("JWT does not carry a tenant_id claim");
        }

        Tenant tenant = repository.findByTenantReference(claim.strip())
                .orElseThrow(() ->
                    new TenantResolutionException("Tenant claim could not be resolved"));

        boolean isPlatform = tenant.getTenantType() == TenantType.PLATFORM;

        if (!isPlatform && tenant.getStatus() == TenantStatus.SUSPENDED) {
            throw new TenantResolutionException("Tenant access is not available");
        }

        return new TenantContext(
                tenant.getTenantId(),
                TenantReference.of(tenant.getTenantReference()),
                isPlatform
        );
    }
}
