package lab.paymentquality.merchant.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisplayNameTest {

    @Test
    void validMinLength() {
        var dn = DisplayName.from("AB");
        assertThat(dn.value()).isEqualTo("AB");
    }

    @Test
    void validMaxLength() {
        String input = "A".repeat(120);
        var dn = DisplayName.from(input);
        assertThat(dn.value()).isEqualTo(input);
    }

    @Test
    void trimsWhitespace() {
        var dn = DisplayName.from("  Hello World  ");
        assertThat(dn.value()).isEqualTo("Hello World");
    }

    @Test
    void whitespaceOnlyRejected() {
        assertThatThrownBy(() -> DisplayName.from("     "))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> DisplayName.from(null))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> DisplayName.from("  "))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void oneCharAfterTrimRejected() {
        assertThatThrownBy(() -> DisplayName.from(" A "))
                .isInstanceOf(InvalidDisplayNameException.class);
    }

    @Test
    void tooLongRejected() {
        assertThatThrownBy(() -> DisplayName.from("A".repeat(121)))
                .isInstanceOf(InvalidDisplayNameException.class);
    }
}
