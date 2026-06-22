package lab.paymentquality.payment.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrencyCodeTest {

    @Test
    void plnIsValid() {
        assertThat(CurrencyCode.of("PLN").code()).isEqualTo("PLN");
    }

    @Test
    void eurIsValid() {
        assertThat(CurrencyCode.of("EUR").code()).isEqualTo("EUR");
    }

    @Test
    void usdIsValid() {
        assertThat(CurrencyCode.of("USD").code()).isEqualTo("USD");
    }

    @Test
    void unsupportedUppercaseRejected() {
        assertThatThrownBy(() -> CurrencyCode.of("GBP"))
                .isInstanceOf(InvalidCurrencyCodeException.class);
    }

    @Test
    void lowercaseRejected() {
        assertThatThrownBy(() -> CurrencyCode.of("pln"))
                .isInstanceOf(InvalidCurrencyCodeException.class);
    }

    @Test
    void malformedRejected() {
        assertThatThrownBy(() -> CurrencyCode.of("PL"))
                .isInstanceOf(InvalidCurrencyCodeException.class);
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> CurrencyCode.of(null))
                .isInstanceOf(InvalidCurrencyCodeException.class);
    }
}
