package lab.paymentquality.tenant.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentPolicyTest {

    @Test
    void autoCaptureOffIgnoresMaxAndStoresZero() {
        PaymentPolicy policy = PaymentPolicy.of(false, 999, 0, "MANUAL");
        assertThat(policy.autoCapture()).isFalse();
        assertThat(policy.maxAutoCaptureMinor()).isZero();
        assertThat(policy.riskThreshold()).isZero();
        assertThat(policy.refundPolicy()).isEqualTo(RefundPolicy.MANUAL);
    }

    @Test
    void autoCaptureOnRequiresMaxAtLeastOne() {
        assertThatThrownBy(() -> PaymentPolicy.of(true, null, 50, "MANUAL"))
                .isInstanceOf(InvalidPaymentPolicyException.class)
                .hasMessageContaining("maxAutoCaptureMinor");
        assertThatThrownBy(() -> PaymentPolicy.of(true, 0, 50, "MANUAL"))
                .isInstanceOf(InvalidPaymentPolicyException.class)
                .hasMessageContaining("maxAutoCaptureMinor");
        PaymentPolicy ok = PaymentPolicy.of(true, 1, 100, "AUTOMATIC");
        assertThat(ok.maxAutoCaptureMinor()).isEqualTo(1);
        assertThat(ok.riskThreshold()).isEqualTo(100);
        assertThat(ok.refundPolicy()).isEqualTo(RefundPolicy.AUTOMATIC);
    }

    @Test
    void riskThresholdMustBeInclusiveZeroToOneHundred() {
        assertThatThrownBy(() -> PaymentPolicy.of(false, 0, -1, "MANUAL"))
                .isInstanceOf(InvalidPaymentPolicyException.class);
        assertThatThrownBy(() -> PaymentPolicy.of(false, 0, 101, "MANUAL"))
                .isInstanceOf(InvalidPaymentPolicyException.class);
        assertThat(PaymentPolicy.of(false, 0, 0, "MANUAL").riskThreshold()).isZero();
        assertThat(PaymentPolicy.of(false, 0, 100, "MANUAL").riskThreshold()).isEqualTo(100);
    }
}
