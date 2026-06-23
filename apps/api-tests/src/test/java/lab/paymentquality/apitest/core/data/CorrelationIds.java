package lab.paymentquality.apitest.core.data;

import java.util.UUID;

/**
 * Generates readable correlation IDs for test requests.
 *
 * <p>Format: {@code test-<prefix>-<8-char-suffix>}
 * Example: {@code test-merchant-a3f7c2d1}
 *
 * <p>IDs use only lowercase alphanumeric and hyphen characters — safe for HTTP headers,
 * MDC log keys, and JSON fields.
 *
 * <p>SDET learning: correlation IDs are the primary observability link between a test assertion
 * and backend logs. Readable prefixes make log searches fast during debugging.
 */
public final class CorrelationIds {

    private CorrelationIds() {}

    /**
     * Generates a correlation ID with a descriptive prefix.
     * Example: {@code generate("create-merchant")} → {@code test-create-merchant-a3f7c2d1}
     */
    public static String generate(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "test-" + sanitize(prefix) + "-" + suffix;
    }

    /** Generates a generic correlation ID without a semantic prefix. */
    public static String generate() {
        return generate("req");
    }

    /** Validates that a string looks like a correlation ID (non-blank). */
    public static boolean isValid(String value) {
        return value != null && !value.isBlank();
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "auto";
        return raw.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-{2,}", "-");
    }
}
