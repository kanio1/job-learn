package lab.paymentquality.apitest.core.data;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing an HTTP ETag value from the payment API.
 *
 * <p>The payment API uses <strong>strong ETags</strong> in {@code "vN"} format:
 * {@code "v1"}, {@code "v2"}, etc. (quoted, always incremented on state change).
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link #raw()} — the full quoted value for {@code If-Match} header</li>
 *   <li>{@link #version()} — the integer version number extracted from the tag</li>
 *   <li>{@link #isQuoted()} — validates the {@code "vN"} format</li>
 * </ul>
 *
 * <p>SDET learning: ETag values must be sent in the {@code If-Match} header exactly as
 * received (quoted). Stripping quotes causes 400 or 412 depending on backend validation.
 * This value object makes it impossible to accidentally use an unquoted ETag.
 */
public final class ETag {

    private static final Pattern QUOTED_PATTERN = Pattern.compile("^\"v\\d+\"$");

    private final String raw;

    private ETag(String raw) {
        this.raw = Objects.requireNonNull(raw, "ETag value must not be null");
    }

    /** Creates an ETag from the raw value received in an HTTP response header. */
    public static ETag of(String rawValue) {
        Objects.requireNonNull(rawValue, "ETag rawValue must not be null");
        if (rawValue.isBlank()) {
            throw new IllegalArgumentException("ETag rawValue must not be blank");
        }
        return new ETag(rawValue.trim());
    }

    /** The raw ETag value as received — use this in {@code If-Match} headers. */
    public String raw() {
        return raw;
    }

    /**
     * Extracts the integer version number from a {@code "vN"} ETag.
     *
     * @throws IllegalStateException if the ETag is not in {@code "vN"} format
     */
    public int version() {
        if (!isQuoted()) {
            throw new IllegalStateException(
                    "Cannot extract version from ETag that is not in \"vN\" format: " + raw);
        }
        // raw is "vN" — strip quotes and leading 'v'
        String inner = raw.substring(1, raw.length() - 1); // removes surrounding quotes
        return Integer.parseInt(inner.substring(1));        // removes leading 'v'
    }

    /**
     * Returns {@code true} if this ETag is in the expected {@code "vN"} format.
     * The payment API always returns ETags in this format for payment order resources.
     */
    public boolean isQuoted() {
        return QUOTED_PATTERN.matcher(raw).matches();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ETag other)) return false;
        return raw.equals(other.raw);
    }

    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    @Override
    public String toString() {
        return "ETag{" + raw + "}";
    }
}
