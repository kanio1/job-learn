package lab.paymentquality.apitest.core.http;

/**
 * Backend base URI configuration.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Environment variable {@code API_BASE_URI}</li>
 *   <li>System property {@code api.base.uri}</li>
 *   <li>Default: {@code http://localhost:8080}</li>
 * </ol>
 *
 * <p>{@link #fromStack()} is reserved for Phase 6 when the Testcontainers stack is wired.
 * The stack will call {@link #of(String)} with the mapped port from {@code ApiStack}.
 */
public final class ApiConfig {

    private static final String ENV_VAR = "API_BASE_URI";
    private static final String SYS_PROP = "api.base.uri";
    private static final String DEFAULT_URI = "http://localhost:8080";

    private final String baseUri;

    private ApiConfig(String baseUri) {
        this.baseUri = baseUri;
    }

    /** Creates a config with the given base URI. */
    public static ApiConfig of(String baseUri) {
        if (baseUri == null || baseUri.isBlank()) {
            throw new IllegalArgumentException("baseUri must not be blank");
        }
        return new ApiConfig(baseUri.stripTrailing().replaceAll("/+$", ""));
    }

    /**
     * Resolves base URI from environment/system property, falling back to localhost.
     * Safe to call in offline unit tests — no network needed.
     */
    public static ApiConfig fromEnvironment() {
        String env = System.getenv(ENV_VAR);
        if (env != null && !env.isBlank()) {
            return of(env);
        }
        String prop = System.getProperty(SYS_PROP);
        if (prop != null && !prop.isBlank()) {
            return of(prop);
        }
        return of(DEFAULT_URI);
    }

    /**
     * Phase 6: returns the base URI provided by the Testcontainers stack.
     * Replace this with {@code ApiStack.get().baseUri()} when the stack is wired.
     *
     * @throws UnsupportedOperationException until Phase 6 is implemented
     */
    public static ApiConfig fromStack() {
        throw new UnsupportedOperationException(
                "ApiConfig.fromStack() requires the Testcontainers stack (Phase 6). " +
                "Use ApiConfig.fromEnvironment() for offline tests, or wire ApiStack in Phase 6.");
    }

    public String baseUri() {
        return baseUri;
    }

    @Override
    public String toString() {
        return "ApiConfig{baseUri='" + baseUri + "'}";
    }
}
