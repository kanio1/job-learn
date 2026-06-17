package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.domain.MerchantNotFoundException;
import lab.paymentquality.merchant.internal.domain.MissingTenantReferenceException;
import lab.paymentquality.merchant.internal.domain.TenantBoundaryViolationException;
import lab.paymentquality.merchant.internal.domain.UnresolvableTenantReferenceException;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantServiceTenantTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_BETA_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final TenantContext TENANT_ALPHA_CONTEXT =
            new TenantContext(TENANT_ALPHA_ID, TenantReference.of("TENANT_ALPHA"), false);
    private static final TenantContext PLATFORM_CONTEXT =
            new TenantContext(UUID.fromString("20000000-0000-0000-0000-000000000001"),
                    TenantReference.of("PLATFORM_TENANT"), true);

    @Mock
    JpaMerchantRepository repository;

    @Mock
    TenantResolver tenantResolver;

    @Test
    void tenantScopedCreateIgnoresBodyTenantReferenceAndAssignsCallerTenant() {
        when(repository.findByNormalizedReference("MERCH-TENANT")).thenReturn(Optional.empty());
        var service = new MerchantService(repository, tenantResolver);

        service.create("MERCH-TENANT", "Tenant Merchant", TENANT_ALPHA_CONTEXT, "TENANT_BETA");

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ALPHA_ID);
        verify(tenantResolver, never()).resolveTenantId(any());
    }

    @Test
    void platformScopedCreateAssignsResolvedTenantReference() {
        when(repository.findByNormalizedReference("MERCH-PLATFORM")).thenReturn(Optional.empty());
        when(tenantResolver.resolveTenantId(TenantReference.of("TENANT_ALPHA"))).thenReturn(TENANT_ALPHA_ID);
        var service = new MerchantService(repository, tenantResolver);

        service.create("MERCH-PLATFORM", "Platform Merchant", PLATFORM_CONTEXT, "TENANT_ALPHA");

        ArgumentCaptor<Merchant> captor = ArgumentCaptor.forClass(Merchant.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ALPHA_ID);
    }

    @Test
    void platformScopedCreateWithNullTenantReferenceThrowsMissingTenantReference() {
        var service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.create("MERCH-MISSING", "Missing Tenant", PLATFORM_CONTEXT, null))
                .isInstanceOf(MissingTenantReferenceException.class);
    }

    @Test
    void platformScopedCreateWithBlankTenantReferenceThrowsMissingTenantReference() {
        var service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.create("MERCH-BLANK", "Blank Tenant", PLATFORM_CONTEXT, "   "))
                .isInstanceOf(MissingTenantReferenceException.class);
    }

    @Test
    void platformScopedCreateWithUnknownTenantReferenceThrowsUnresolvableTenantReference() {
        when(tenantResolver.resolveTenantId(TenantReference.of("UNKNOWN_TENANT")))
                .thenThrow(new TenantResolutionException("Tenant reference could not be resolved"));
        var service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.create("MERCH-UNKNOWN", "Unknown Tenant", PLATFORM_CONTEXT, "UNKNOWN_TENANT"))
                .isInstanceOf(UnresolvableTenantReferenceException.class);
    }

    @Test
    void tenantScopedFindByIdReturnsOwnTenantMerchant() {
        UUID merchantId = UUID.randomUUID();
        when(repository.findByMerchantIdAndTenantId(merchantId, TENANT_ALPHA_ID))
                .thenReturn(Optional.of(Merchant.create(merchantId, "MERCH-OWN", "Own Merchant", TENANT_ALPHA_ID)));
        var service = new MerchantService(repository, tenantResolver);

        var response = service.findById(merchantId, TENANT_ALPHA_CONTEXT);

        assertThat(response.merchantId()).isEqualTo(merchantId);
        assertThat(response.merchantReference()).isEqualTo("MERCH-OWN");
    }

    @Test
    void tenantScopedFindByIdMasksOtherTenantMerchantAsNotFound() {
        UUID merchantId = UUID.randomUUID();
        when(repository.findByMerchantIdAndTenantId(merchantId, TENANT_ALPHA_ID)).thenReturn(Optional.empty());
        var service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.findById(merchantId, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void tenantScopedWriteSameTenantProceeds() {
        UUID merchantId = UUID.randomUUID();
        when(repository.findById(merchantId))
                .thenReturn(Optional.of(Merchant.create(merchantId, "MERCH-SAME", "Same Tenant", TENANT_ALPHA_ID)));
        var service = new MerchantService(repository, tenantResolver);

        var response = service.activate(merchantId, TENANT_ALPHA_CONTEXT);

        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(repository).saveAndFlush(any(Merchant.class));
    }

    @Test
    void tenantScopedWriteOtherTenantThrowsTenantBoundaryViolation() {
        UUID merchantId = UUID.randomUUID();
        when(repository.findById(merchantId))
                .thenReturn(Optional.of(Merchant.create(merchantId, "MERCH-OTHER", "Other Tenant", TENANT_BETA_ID)));
        var service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.activate(merchantId, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(TenantBoundaryViolationException.class);
    }
}
