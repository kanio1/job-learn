package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.*;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.merchant.internal.web.DuplicateMerchantReferenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

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
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MerchantService service;

    private static final UUID PLACEHOLDER_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private void stubPlaceholderTenant() {
        when(jdbcTemplate.queryForObject(anyString(), eq(UUID.class)))
                .thenReturn(PLACEHOLDER_TENANT_ID);
    }

    @Test
    void createValidMerchantReturnsDraft() {
        stubPlaceholderTenant();
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Merchant.class))).thenAnswer(inv -> inv.getArgument(0));

        var merchant = service.create("MERCH-001", "Test Merchant");

        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.DRAFT);
        assertThat(merchant.getNormalizedReference()).isEqualTo("MERCH-001");
        assertThat(merchant.getDisplayName()).isEqualTo("Test Merchant");
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
        stubPlaceholderTenant();
        when(repository.findByNormalizedReference("MERCH-001")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(Merchant.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> service.create("MERCH-001", "Test"))
                .isInstanceOf(DuplicateMerchantReferenceException.class);
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
    void representativeLogsContainSafeContextAndNoSecrets(CapturedOutput output) {
        stubPlaceholderTenant();
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
