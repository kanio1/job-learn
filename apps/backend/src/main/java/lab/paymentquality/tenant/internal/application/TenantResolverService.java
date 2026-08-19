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

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Service
class TenantResolverService implements TenantResolver {

    private final JpaTenantRepository repository;

    TenantResolverService(JpaTenantRepository repository) {
        this.repository = repository;
    }

    private static final String PLATFORM_TENANT_REFERENCE = "PLATFORM_TENANT";
    private static final String PLATFORM_ADMIN_ROLE = "PLATFORM_ADMIN";

    @Override
    @Transactional(readOnly = true)
    public TenantContext resolve(Jwt jwt) {
        if (hasPlatformAdminRole(jwt)) {
            Tenant platform = repository.findByTenantReference(PLATFORM_TENANT_REFERENCE)
                    .orElseThrow(() ->
                            new TenantResolutionException("Tenant claim could not be resolved"));
            return new TenantContext(
                    platform.getTenantId(),
                    TenantReference.of(platform.getTenantReference()),
                    true
            );
        }

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

    private static boolean hasPlatformAdminRole(Jwt jwt) {
        Object realmAccessClaim = jwt.getClaims().get("realm_access");
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return false;
        }
        Object rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return false;
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(PLATFORM_ADMIN_ROLE::equals);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveTenantId(TenantReference tenantReference) {
        return repository.findByTenantReference(tenantReference.value())
                .map(Tenant::getTenantId)
                .orElseThrow(() ->
                        new TenantResolutionException("Tenant reference could not be resolved"));
    }
}
