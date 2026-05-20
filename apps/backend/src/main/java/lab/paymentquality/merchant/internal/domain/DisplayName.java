package lab.paymentquality.merchant.internal.domain;

/**
 * Value object for a trimmed, length-validated display name (2-120 chars).
 */
public record DisplayName(String value) {

    public static DisplayName from(String raw) {
        if (raw == null) {
            throw new InvalidDisplayNameException(null);
        }
        String trimmed = raw.trim();
        if (trimmed.length() < 2 || trimmed.length() > 120) {
            throw new InvalidDisplayNameException(trimmed);
        }
        return new DisplayName(trimmed);
    }
}
