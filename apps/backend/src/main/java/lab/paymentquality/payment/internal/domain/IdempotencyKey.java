package lab.paymentquality.payment.internal.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record IdempotencyKey(String rawKey, String keyHash) {

    private static final int MAX_LENGTH = 128;

    public static IdempotencyKey of(String rawKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key must not be blank");
        }
        String trimmed = rawKey.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new InvalidIdempotencyKeyException(
                    "Idempotency-Key must not exceed " + MAX_LENGTH + " characters");
        }
        for (int i = 0; i < rawKey.length(); i++) {
            char c = rawKey.charAt(i);
            if (c < 0x20 || c > 0x7E) {
                throw new InvalidIdempotencyKeyException(
                        "Idempotency-Key must contain only printable ASCII characters");
            }
        }
        return new IdempotencyKey(trimmed, sha256Hex(trimmed));
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
