package lab.paymentquality.payment.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientOrderReferenceTest {

    @Test
    void validReference() {
        assertThat(ClientOrderReference.of("PAY-001").value()).isEqualTo("PAY-001");
    }

    @Test
    void trimmedReference() {
        assertThat(ClientOrderReference.of("  PAY-001  ").value()).isEqualTo("PAY-001");
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> ClientOrderReference.of("   "))
                .isInstanceOf(InvalidClientOrderReferenceException.class);
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> ClientOrderReference.of(null))
                .isInstanceOf(InvalidClientOrderReferenceException.class);
    }

    @Test
    void maxLengthAccepted() {
        String maxRef = "A".repeat(120);
        assertThat(ClientOrderReference.of(maxRef).value()).isEqualTo(maxRef);
    }

    @Test
    void aboveMaxLengthRejected() {
        assertThatThrownBy(() -> ClientOrderReference.of("A".repeat(121)))
                .isInstanceOf(InvalidClientOrderReferenceException.class);
    }
}
