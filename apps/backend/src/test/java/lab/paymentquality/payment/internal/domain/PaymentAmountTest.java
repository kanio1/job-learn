package lab.paymentquality.payment.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentAmountTest {

    @Test
    void validMinimumAmount() {
        assertThat(PaymentAmount.of(1).minorUnits()).isEqualTo(1);
    }

    @Test
    void validMaximumAmount() {
        assertThat(PaymentAmount.of(100_000_000).minorUnits()).isEqualTo(100_000_000);
    }

    @Test
    void zeroAmountRejected() {
        assertThatThrownBy(() -> PaymentAmount.of(0))
                .isInstanceOf(InvalidPaymentAmountException.class);
    }

    @Test
    void negativeAmountRejected() {
        assertThatThrownBy(() -> PaymentAmount.of(-1))
                .isInstanceOf(InvalidPaymentAmountException.class);
    }

    @Test
    void aboveMaximumRejected() {
        assertThatThrownBy(() -> PaymentAmount.of(100_000_001))
                .isInstanceOf(InvalidPaymentAmountException.class);
    }
}
