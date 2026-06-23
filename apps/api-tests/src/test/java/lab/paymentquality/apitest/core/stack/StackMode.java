package lab.paymentquality.apitest.core.stack;

/**
 * Determines how the test suite connects to the backend under test.
 *
 * <p>Resolution logic (implemented in {@code ApiStack} — Phase 6B):
 * <ul>
 *   <li>If {@code API_BASE_URI} environment variable is set → {@link #EXTERNAL}.</li>
 *   <li>Otherwise → {@link #TESTCONTAINERS}.</li>
 * </ul>
 *
 * <p>SDET learning: a dual-mode design lets CI always use {@link #TESTCONTAINERS} (self-contained)
 * while local developers can set {@code API_BASE_URI} to avoid rebuilding the backend image
 * on every run when the compose stack is already up.
 *
 * <p>Phase 6A: enum committed as documentation anchor. {@code ApiStack} that uses it is Phase 6B.
 */
public enum StackMode {

    /**
     * Postgres, Keycloak, and backend are started as Testcontainers containers.
     * Requires {@code BACKEND_IMAGE} env variable to point to a pre-built backend OCI image.
     * Safe for CI — no external dependencies.
     */
    TESTCONTAINERS,

    /**
     * Connects to an already-running backend at {@code API_BASE_URI}.
     * No containers are started. Fast for local development when the stack is running.
     * Not suitable for CI unless the stack is managed externally.
     */
    EXTERNAL
}
