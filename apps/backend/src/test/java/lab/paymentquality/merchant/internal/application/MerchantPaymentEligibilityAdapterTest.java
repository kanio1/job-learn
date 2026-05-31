package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.MerchantPaymentEligibility;
import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantPaymentEligibilityAdapterTest {

    @Mock
    private JpaMerchantRepository repository;

    @InjectMocks
    private MerchantPaymentEligibilityAdapter adapter;

    @Test
    void activeMerchantReturnsEligibleWithActiveTrue() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = Merchant.create(merchantId, "MERCH-001", "Test Merchant");
        merchant.activate();
        when(repository.findById(merchantId)).thenReturn(Optional.of(merchant));

        Optional<MerchantPaymentEligibility> result = adapter.findEligibility(merchantId);

        assertThat(result).isPresent();
        assertThat(result.get().merchantId()).isEqualTo(merchantId);
        assertThat(result.get().normalizedReference()).isEqualTo("MERCH-001");
        assertThat(result.get().active()).isTrue();
    }

    @Test
    void draftMerchantReturnsEligibleWithActiveFalse() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = Merchant.create(merchantId, "MERCH-002", "Draft Merchant");
        when(repository.findById(merchantId)).thenReturn(Optional.of(merchant));

        Optional<MerchantPaymentEligibility> result = adapter.findEligibility(merchantId);

        assertThat(result).isPresent();
        assertThat(result.get().active()).isFalse();
    }

    @Test
    void suspendedMerchantReturnsEligibleWithActiveFalse() {
        UUID merchantId = UUID.randomUUID();
        Merchant merchant = Merchant.create(merchantId, "MERCH-003", "Suspended Merchant");
        merchant.activate();
        merchant.suspend();
        when(repository.findById(merchantId)).thenReturn(Optional.of(merchant));

        Optional<MerchantPaymentEligibility> result = adapter.findEligibility(merchantId);

        assertThat(result).isPresent();
        assertThat(result.get().active()).isFalse();
    }

    @Test
    void unknownMerchantReturnsEmpty() {
        UUID merchantId = UUID.randomUUID();
        when(repository.findById(merchantId)).thenReturn(Optional.empty());

        Optional<MerchantPaymentEligibility> result = adapter.findEligibility(merchantId);

        assertThat(result).isEmpty();
    }
}
