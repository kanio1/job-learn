package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.*;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.merchant.internal.web.DuplicateMerchantReferenceException;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class MerchantServiceTest {

    @Mock
    private JpaMerchantRepository repository;

    @Mock
    private TenantResolver tenantResolver;

    @InjectMocks
    private MerchantService service;

    private static final UUID PLACEHOLDER_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID TENANT_ALPHA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID TENANT_BETA_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID PLATFORM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
    private static final TenantContext TENANT_ALPHA_CONTEXT =
            new TenantContext(TENANT_ALPHA_ID, TenantReference.of("TENANT_ALPHA"), false);
    private static final TenantContext PLATFORM_CONTEXT =
            new TenantContext(PLATFORM_TENANT_ID, TenantReference.of("PLATFORM_TENANT"), true);

    private void stubDefaultTenant() {
        when(tenantResolver.resolveTenantId(TenantReference.of("PLACEHOLDER_TENANT_ID")))
                .thenReturn(PLACEHOLDER_TENANT_ID);
    }

    @Test
    void createValidMerchantReturnsDraft() {
        stubDefaultTenant();
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        var merchant = service.create("MERCH-001", "Test Merchant");

        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.DRAFT);
        assertThat(merchant.getNormalizedReference()).isEqualTo("MERCH-001");
        assertThat(merchant.getDisplayName()).isEqualTo("Test Merchant");
        assertThat(merchant.getTenantId()).isEqualTo(PLACEHOLDER_TENANT_ID);
        verify(repository).saveAndFlush(any(Merchant.class));
    }

    @Test
    void createRejectsPostTrimShortDisplayName() {
        assertThatThrownBy(() -> service.create("MERCH-001", " A "))
                .isInstanceOf(InvalidDisplayNameException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void createDuplicateThrowsException() {
        var existing = Merchant.create(UUID.randomUUID(), "MERCH-001", "Existing");
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("MERCH-001", "Test"))
                .isInstanceOf(DuplicateMerchantReferenceException.class);
    }

    @Test
    void createDataIntegrityViolationTranslated() {
        stubDefaultTenant();
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Merchant.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> service.create("MERCH-001", "Test"))
                .isInstanceOf(DuplicateMerchantReferenceException.class);
    }

    @Test
    void tenantScopedCreateAssignsPrincipalTenantAndIgnoresRequestTenantReference() {
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        var merchant = service.create("MERCH-001", "Test Merchant", TENANT_ALPHA_CONTEXT, "PLATFORM_TENANT");

        assertThat(merchant.getTenantId()).isEqualTo(TENANT_ALPHA_ID);
        verifyNoInteractions(tenantResolver);
    }

    @Test
    void platformScopedCreateRequiresTenantReference() {
        assertThatThrownBy(() -> service.create("MERCH-001", "Test Merchant", PLATFORM_CONTEXT, " "))
                .isInstanceOf(MissingTenantReferenceException.class);
        verifyNoInteractions(repository, tenantResolver);
    }

    @Test
    void platformScopedCreateAssignsResolvedTenant() {
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.empty());
        when(tenantResolver.resolveTenantId(TenantReference.of("TENANT_ALPHA"))).thenReturn(TENANT_ALPHA_ID);
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        var merchant = service.create("MERCH-001", "Test Merchant", PLATFORM_CONTEXT, "TENANT_ALPHA");

        assertThat(merchant.getTenantId()).isEqualTo(TENANT_ALPHA_ID);
    }

    @Test
    void platformScopedCreateRejectsUnknownTenantReference() {
        when(tenantResolver.resolveTenantId(TenantReference.of("UNKNOWN_TENANT")))
                .thenThrow(new TenantResolutionException("not found"));

        assertThatThrownBy(() -> service.create("MERCH-001", "Test Merchant", PLATFORM_CONTEXT, "UNKNOWN_TENANT"))
                .isInstanceOf(UnresolvableTenantReferenceException.class);
    }

    @Test
    void findByIdReturnsMerchant() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test");
        when(repository.findById(id)).thenReturn(Optional.of(merchant));

        var response = service.findById(id);

        assertThat(response.merchantId()).isEqualTo(id);
        assertThat(response.status()).isEqualTo("DRAFT");
    }

    @Test
    void findByIdThrowsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void tenantScopedFindByIdUsesTenantFilteredLookup() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test", TENANT_ALPHA_ID);
        when(repository.findByMerchantIdAndTenantId(id, TENANT_ALPHA_ID)).thenReturn(Optional.of(merchant));

        var response = service.findById(id, TENANT_ALPHA_CONTEXT);

        assertThat(response.merchantId()).isEqualTo(id);
        verify(repository, never()).findById(id);
    }

    @Test
    void tenantScopedFindByIdMasksForeignMerchantAsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findByMerchantIdAndTenantId(id, TENANT_ALPHA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void listFirstPageFiltersForTenantScopedContext() {
        var merchant = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test", TENANT_ALPHA_ID);
        when(repository.findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(eq(TENANT_ALPHA_ID), any()))
                .thenReturn(List.of(merchant));

        var response = service.listFirstPage(TENANT_ALPHA_CONTEXT, TENANT_BETA_ID);

        assertThat(response).extracting("merchantId").containsExactly(merchant.getMerchantId());
        verify(repository, never()).findAllByOrderByCreatedAtDescMerchantIdAsc(any());
    }

    @Test
    void listFirstPageSupportsPlatformTenantFilter() {
        var merchant = Merchant.create(UUID.randomUUID(), "MERCH-001", "Test", TENANT_BETA_ID);
        when(repository.findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(eq(TENANT_BETA_ID), any()))
                .thenReturn(List.of(merchant));

        var response = service.listFirstPage(PLATFORM_CONTEXT, TENANT_BETA_ID);

        assertThat(response).extracting("merchantId").containsExactly(merchant.getMerchantId());
    }

    @Test
    void activateDraftMerchant() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test");
        when(repository.findById(id)).thenReturn(Optional.of(merchant));
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.activate(id);

        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void activateActiveMerchantThrows() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test");
        merchant.activate();
        when(repository.findById(id)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service.activate(id))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void suspendActiveMerchant() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test");
        merchant.activate();
        when(repository.findById(id)).thenReturn(Optional.of(merchant));
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.suspend(id);

        assertThat(response.status()).isEqualTo("SUSPENDED");
    }

    @Test
    void suspendDraftMerchantThrows() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test");
        when(repository.findById(id)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service.suspend(id))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void tenantScopedActivateRejectsForeignMerchant() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test", TENANT_BETA_ID);
        when(repository.findById(id)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> service.activate(id, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(TenantBoundaryViolationException.class);
    }

    @Test
    void tenantScopedSuspendAllowsOwnTenantMerchant() {
        var id = UUID.randomUUID();
        var merchant = Merchant.create(id, "MERCH-001", "Test", TENANT_ALPHA_ID);
        merchant.activate();
        when(repository.findById(id)).thenReturn(Optional.of(merchant));
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.suspend(id, TENANT_ALPHA_CONTEXT);

        assertThat(response.status()).isEqualTo("SUSPENDED");
    }

    @Test
    void representativeLogsContainSafeContextAndNoSecrets(CapturedOutput output) {
        stubDefaultTenant();
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create("MERCH-001", "Test Merchant");

        assertThat(output).contains("merchant.create.succeeded")
                .contains("MERCH-001")
                .doesNotContain("Authorization")
                .doesNotContain("access_token")
                .doesNotContain("refresh_token")
                .doesNotContain("password");
    }
}
