package lab.paymentquality.tenant.internal.application;

import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.internal.domain.Tenant;
import lab.paymentquality.tenant.internal.domain.TenantStatus;
import lab.paymentquality.tenant.internal.domain.TenantType;
import lab.paymentquality.tenant.internal.infrastructure.JpaTenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantResolverServiceTest {

    @Mock
    JpaTenantRepository repository;

    @Test
    void activePlatformTenantClaimResolvesPlatformScopedContext() {
        UUID tenantId = UUID.randomUUID();
        when(repository.findByTenantReference("PLATFORM_TENANT"))
                .thenReturn(Optional.of(tenant(tenantId, "PLATFORM_TENANT", TenantStatus.ACTIVE, TenantType.PLATFORM)));
        var service = new TenantResolverService(repository);

        var context = service.resolve(jwtWithTenantClaim("PLATFORM_TENANT"));

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.tenantReference().value()).isEqualTo("PLATFORM_TENANT");
        assertThat(context.isPlatformScoped()).isTrue();
        assertThat(context.isTenantScoped()).isFalse();
    }

    @Test
    void activeStandardTenantClaimResolvesTenantScopedContext() {
        UUID tenantId = UUID.randomUUID();
        when(repository.findByTenantReference("TENANT_ALPHA"))
                .thenReturn(Optional.of(tenant(tenantId, "TENANT_ALPHA", TenantStatus.ACTIVE, TenantType.STANDARD)));
        var service = new TenantResolverService(repository);

        var context = service.resolve(jwtWithTenantClaim("TENANT_ALPHA"));

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.tenantReference().value()).isEqualTo("TENANT_ALPHA");
        assertThat(context.isPlatformScoped()).isFalse();
        assertThat(context.isTenantScoped()).isTrue();
    }

    @Test
    void suspendedStandardTenantClaimIsRejected() {
        when(repository.findByTenantReference("TENANT_ALPHA"))
                .thenReturn(Optional.of(tenant(UUID.randomUUID(), "TENANT_ALPHA", TenantStatus.SUSPENDED, TenantType.STANDARD)));
        var service = new TenantResolverService(repository);

        assertThatThrownBy(() -> service.resolve(jwtWithTenantClaim("TENANT_ALPHA")))
                .isInstanceOf(TenantResolutionException.class);
    }

    @Test
    void suspendedPlatformTenantClaimStillResolves() {
        UUID tenantId = UUID.randomUUID();
        when(repository.findByTenantReference("PLATFORM_TENANT"))
                .thenReturn(Optional.of(tenant(tenantId, "PLATFORM_TENANT", TenantStatus.SUSPENDED, TenantType.PLATFORM)));
        var service = new TenantResolverService(repository);

        var context = service.resolve(jwtWithTenantClaim("PLATFORM_TENANT"));

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.isPlatformScoped()).isTrue();
    }

    @Test
    void absentTenantClaimIsRejected() {
        var service = new TenantResolverService(repository);

        assertThatThrownBy(() -> service.resolve(jwtWithoutTenantClaim()))
                .isInstanceOf(TenantResolutionException.class);
    }

    @Test
    void blankTenantClaimIsRejected() {
        var service = new TenantResolverService(repository);

        assertThatThrownBy(() -> service.resolve(jwtWithTenantClaim("   ")))
                .isInstanceOf(TenantResolutionException.class);
    }

    @Test
    void unknownTenantClaimIsRejected() {
        when(repository.findByTenantReference("UNKNOWN_TENANT")).thenReturn(Optional.empty());
        var service = new TenantResolverService(repository);

        assertThatThrownBy(() -> service.resolve(jwtWithTenantClaim("UNKNOWN_TENANT")))
                .isInstanceOf(TenantResolutionException.class);
    }

    private static Jwt jwtWithTenantClaim(String tenantReference) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("tenant_id", tenantReference)
                .build();
    }

    private static Jwt jwtWithoutTenantClaim() {
        return Jwt.withTokenValue("token")
                .headers(headers -> headers.putAll(Map.of("alg", "none")))
                .claim("sub", "tenantless")
                .build();
    }

    private static Tenant tenant(UUID tenantId, String tenantReference, TenantStatus status, TenantType type) {
        Tenant tenant = BeanUtils.instantiateClass(Tenant.class);
        ReflectionTestUtils.setField(tenant, "tenantId", tenantId);
        ReflectionTestUtils.setField(tenant, "tenantReference", tenantReference);
        ReflectionTestUtils.setField(tenant, "name", tenantReference);
        ReflectionTestUtils.setField(tenant, "status", status);
        ReflectionTestUtils.setField(tenant, "tenantType", type);
        ReflectionTestUtils.setField(tenant, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        return tenant;
    }
}
