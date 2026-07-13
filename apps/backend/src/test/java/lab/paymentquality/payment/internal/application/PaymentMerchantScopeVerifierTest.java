package lab.paymentquality.payment.internal.application;

import lab.paymentquality.merchant.MerchantPaymentEligibility;
import lab.paymentquality.merchant.MerchantPaymentEligibilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentMerchantScopeVerifierTest {

    @Mock
    private MerchantPaymentEligibilityService merchantPaymentEligibilityService;

    @Test
    void matchingUuidClaimRemainsSupportedWithoutMerchantLookup() {
        UUID merchantId = UUID.randomUUID();
        PaymentMerchantScopeVerifier verifier = new PaymentMerchantScopeVerifier(merchantPaymentEligibilityService);

        assertThat(verifier.matches(merchantId, merchantId.toString())).isTrue();
        verify(merchantPaymentEligibilityService, never()).findEligibilityByReference(merchantId.toString());
    }

    @Test
    void matchingNaturalReferenceResolvesToPathMerchant() {
        UUID merchantId = UUID.randomUUID();
        when(merchantPaymentEligibilityService.findEligibilityByReference("MERCHANT_ALPHA_001"))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(
                        merchantId, "MERCHANT_ALPHA_001", true)));
        PaymentMerchantScopeVerifier verifier = new PaymentMerchantScopeVerifier(merchantPaymentEligibilityService);

        assertThat(verifier.matches(merchantId, "MERCHANT_ALPHA_001")).isTrue();
    }

    @Test
    void foreignNaturalReferenceDoesNotMatchPathMerchant() {
        UUID pathMerchantId = UUID.randomUUID();
        UUID foreignMerchantId = UUID.randomUUID();
        when(merchantPaymentEligibilityService.findEligibilityByReference("MERCHANT_BETA_001"))
                .thenReturn(Optional.of(new MerchantPaymentEligibility(
                        foreignMerchantId, "MERCHANT_BETA_001", true)));
        PaymentMerchantScopeVerifier verifier = new PaymentMerchantScopeVerifier(merchantPaymentEligibilityService);

        assertThat(verifier.matches(pathMerchantId, "MERCHANT_BETA_001")).isFalse();
    }

    @Test
    void missingOrUnknownClaimDoesNotMatch() {
        UUID merchantId = UUID.randomUUID();
        when(merchantPaymentEligibilityService.findEligibilityByReference("UNKNOWN-MERCHANT"))
                .thenReturn(Optional.empty());
        PaymentMerchantScopeVerifier verifier = new PaymentMerchantScopeVerifier(merchantPaymentEligibilityService);

        assertThat(verifier.matches(merchantId, null)).isFalse();
        assertThat(verifier.matches(merchantId, " ")).isFalse();
        assertThat(verifier.matches(merchantId, "UNKNOWN-MERCHANT")).isFalse();
    }
}
