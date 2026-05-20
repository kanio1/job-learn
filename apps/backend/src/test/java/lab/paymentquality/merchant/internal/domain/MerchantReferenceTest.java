package lab.paymentquality.merchant.internal.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantReferenceTest {

    @ParameterizedTest
    @ValueSource(strings = {"MERCH-001", "A01", "ABC", "A1B2C"})
    void validReferences(String raw) {
        var ref = MerchantReference.from(raw);
        assertThat(ref.normalized()).isEqualTo(raw.trim().toUpperCase());
    }

    @Test
    void lowercaseInputNormalized() {
        var ref = MerchantReference.from("merch-001");
        assertThat(ref.normalized()).isEqualTo("MERCH-001");
    }

    @Test
    void digitsOnlyAcceptable() {
        var ref = MerchantReference.from("123");
        assertThat(ref.normalized()).isEqualTo("123");
    }

    @Test
    void maxLength64Valid() {
        String input = "A" + "B".repeat(62) + "C";
        assertThat(input.length()).isEqualTo(64);
        var ref = MerchantReference.from(input);
        assertThat(ref.normalized()).isEqualTo(input.toUpperCase());
    }

    @Test
    void length63Valid() {
        String input = "A" + "B".repeat(61) + "C";
        assertThat(input.length()).isEqualTo(63);
        assertThat(MerchantReference.from(input).normalized()).isEqualTo(input);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    void nullBlankWhitespaceRejected(String raw) {
        assertThatThrownBy(() -> MerchantReference.from(raw))
                .isInstanceOf(InvalidMerchantReferenceException.class);
    }

    @Test
    void tooShortRejected() {
        assertThatThrownBy(() -> MerchantReference.from("A1"))
                .isInstanceOf(InvalidMerchantReferenceException.class);
    }

    @Test
    void tooLongRejected() {
        String input = "X" + "Y".repeat(64);
        assertThat(input.length()).isGreaterThan(64);
        assertThatThrownBy(() -> MerchantReference.from(input))
                .isInstanceOf(InvalidMerchantReferenceException.class);
    }

    @Test
    void leadingHyphenRejected() {
        assertThatThrownBy(() -> MerchantReference.from("-ABC"))
                .isInstanceOf(InvalidMerchantReferenceException.class);
    }

    @Test
    void trailingHyphenRejected() {
        assertThatThrownBy(() -> MerchantReference.from("ABC-"))
                .isInstanceOf(InvalidMerchantReferenceException.class);
    }

    @Test
    void underscoreRejected() {
        assertThatThrownBy(() -> MerchantReference.from("ABC_DEF"))
                .isInstanceOf(InvalidMerchantReferenceException.class);
    }
}
