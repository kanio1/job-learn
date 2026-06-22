package lab.paymentquality.payment.internal.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyKeyTest {

    @Test
    void validKey() {
        IdempotencyKey key = IdempotencyKey.of("idem-001");
        assertThat(key.rawKey()).isEqualTo("idem-001");
        assertThat(key.keyHash()).hasSize(64);
    }

    @Test
    void hashIsDeterministic() {
        String hash1 = IdempotencyKey.of("idem-001").keyHash();
        String hash2 = IdempotencyKey.of("idem-001").keyHash();
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void differentKeysProduceDifferentHashes() {
        String hash1 = IdempotencyKey.of("idem-001").keyHash();
        String hash2 = IdempotencyKey.of("idem-002").keyHash();
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void blankRejected() {
        assertThatThrownBy(() -> IdempotencyKey.of("   "))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }

    @Test
    void nullRejected() {
        assertThatThrownBy(() -> IdempotencyKey.of(null))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }

    @Test
    void maxLengthAccepted() {
        String maxKey = "A".repeat(128);
        assertThat(IdempotencyKey.of(maxKey).rawKey()).isEqualTo(maxKey);
    }

    @Test
    void aboveMaxLengthRejected() {
        assertThatThrownBy(() -> IdempotencyKey.of("A".repeat(129)))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }

    @Test
    void controlCharacterRejected() {
        String keyWithControl = "key" + (char) 1;
        assertThatThrownBy(() -> IdempotencyKey.of(keyWithControl))
                .isInstanceOf(InvalidIdempotencyKeyException.class);
    }
}
