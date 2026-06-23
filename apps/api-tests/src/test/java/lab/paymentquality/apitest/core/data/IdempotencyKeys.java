package lab.paymentquality.apitest.core.data;

import java.util.UUID;

/**
 * Generates readable idempotency keys for payment API tests.
 *
 * <p>Format: {@code idem-<scenario>-<uuid-short>}
 * Example: {@code idem-authorize-a1b2c3d4e5f6}
 *
 * <p>Rules:
 * <ul>
 *   <li>Each call produces a unique key — safe for first-create semantics.</li>
 *   <li>To test idempotency replay, generate once and reuse the same key for both calls.</li>
 *   <li>Keys are safe for HTTP headers: lowercase alphanumeric and hyphens only.</li>
 *   <li>Keys are NOT secrets — do not use them for security purposes.</li>
 * </ul>
 *
 * <p>SDET learning: idempotency keys enable safe retry. The payment API contract is:
 * same key + same body → 200 with identical response; same key + different body → 409 conflict.
 * Testing both cases requires generating one key and reusing it twice.
 */
public final class IdempotencyKeys {

    private IdempotencyKeys() {}

    /**
     * Generates an idempotency key for the given scenario/action name.
     * Example: {@code generate("authorize")} → {@code idem-authorize-a1b2c3d4e5f6}
     */
    public static String generate(String scenario) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return "idem-" + sanitize(scenario) + "-" + suffix;
    }

    /** Generates an idempotency key without a semantic label. */
    public static String generate() {
        return generate("op");
    }

    private static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "op";
        return raw.toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-{2,}", "-");
    }
}
