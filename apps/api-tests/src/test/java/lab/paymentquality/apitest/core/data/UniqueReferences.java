package lab.paymentquality.apitest.core.data;

import java.util.UUID;

/**
 * Generates unique, readable reference strings for test entities.
 *
 * <p>Provides collision-resistant, prefix-based identifiers for merchants, payment orders,
 * and other domain entities created during tests. Prefixes make failure output scannable:
 * {@code "expected merchant alpha-m-a3f7c2d1 but found..."} is faster to diagnose than a bare UUID.
 *
 * <p>SDET learning: deterministic-enough (via UUID suffix) but readable test data reduces
 * mean-time-to-debug on test failures. Prefix conventions also help when tailing logs.
 */
public final class UniqueReferences {

    private UniqueReferences() {}

    /**
     * Generates a merchant reference string.
     * Example: {@code merchantRef()} → {@code merch-a1b2c3d4}
     */
    public static String merchantRef() {
        return "merch-" + shortSuffix();
    }

    /**
     * Generates a merchant reference string with a scenario label.
     * Example: {@code merchantRef("alpha")} → {@code merch-alpha-a1b2c3d4}
     */
    public static String merchantRef(String label) {
        return "merch-" + sanitize(label) + "-" + shortSuffix();
    }

    /**
     * Generates a payment order client reference.
     * Example: {@code paymentRef("checkout")} → {@code pay-checkout-a1b2c3d4}
     */
    public static String paymentRef(String label) {
        return "pay-" + sanitize(label) + "-" + shortSuffix();
    }

    /** Generates a generic unique reference with a custom prefix. */
    public static String of(String prefix) {
        return sanitize(prefix) + "-" + shortSuffix();
    }

    /** Generates a full UUID — for cases requiring UUID format (e.g. tenant/merchant ID seeds). */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    private static String shortSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "ref";
        return raw.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-{2,}", "-");
    }
}
