package lab.paymentquality.merchant.internal.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object representing a normalized merchant reference.
 * Trims input, uppercases, validates against regex {@code ^[A-Z0-9][A-Z0-9-]{1,62}[A-Z0-9]$}
 * (3-64 chars, no leading/trailing hyphens).
 */
public record MerchantReference(String normalized) {

    private static final Pattern PATTERN =
            Pattern.compile("^[A-Z0-9][A-Z0-9-]{1,62}[A-Z0-9]$");

    public static MerchantReference from(String raw) {
        if (raw == null) {
            throw new InvalidMerchantReferenceException(null);
        }
        String trimmed = raw.trim().toUpperCase(Locale.ROOT);
        if (!PATTERN.matcher(trimmed).matches()) {
            throw new InvalidMerchantReferenceException(trimmed);
        }
        return new MerchantReference(trimmed);
    }
}
