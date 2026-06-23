package lab.paymentquality.apitest.core.stack;

import org.testcontainers.containers.Network;

/**
 * Singleton coordinator for the black-box test stack.
 *
 * <p>Mode resolution at startup:
 * <ul>
 *   <li>If {@code API_BASE_URI} env var is set → {@link StackMode#EXTERNAL}: no containers
 *       are started; the specified URI is used directly. Suitable for local dev when the full
 *       compose stack is already running.</li>
 *   <li>Otherwise → {@link StackMode#TESTCONTAINERS}: starts Postgres 18 and the backend
 *       OCI image as TC containers. Requires {@code BACKEND_IMAGE} env var.</li>
 * </ul>
 *
 * <p>Obtain the singleton with {@link #get()}. {@link #stop()} releases containers and the TC
 * network; {@link lab.paymentquality.apitest.support.ApiStackExtension} calls it automatically
 * via JUnit's store {@code CloseableResource}.
 *
 * <p>SDET learning: a dual-mode design lets CI run self-contained ({@code TESTCONTAINERS})
 * while local developers skip the image build ({@code EXTERNAL}) when the stack is already up.
 */
public final class ApiStack {

    private static final String ENV_BACKEND_IMAGE = "BACKEND_IMAGE";
    private static final String ENV_BASE_URI = "API_BASE_URI";

    private static volatile ApiStack instance;

    private final StackMode mode;
    private final String baseUri;

    // Non-null only in TESTCONTAINERS mode
    private Network network;
    private PostgresSupport postgres;
    private BackendSupport backend;

    private ApiStack() {
        String externalUri = System.getenv(ENV_BASE_URI);
        if (externalUri != null && !externalUri.isBlank()) {
            this.mode = StackMode.EXTERNAL;
            this.baseUri = externalUri.strip();
        } else {
            this.mode = StackMode.TESTCONTAINERS;
            this.baseUri = startContainers();
        }
    }

    private String startContainers() {
        String image = System.getenv(ENV_BACKEND_IMAGE);
        if (image == null || image.isBlank()) {
            throw new IllegalStateException(
                    "BACKEND_IMAGE env var required for TESTCONTAINERS mode.\n" +
                    "Build the backend OCI image first:\n" +
                    "  cd apps/backend && ./mvnw spring-boot:build-image -DskipTests \\\n" +
                    "    -Dspring-boot.build-image.imageName=payment-quality/backend:local\n" +
                    "Then: export BACKEND_IMAGE=payment-quality/backend:local\n" +
                    "Or set API_BASE_URI to use EXTERNAL mode against a running stack.");
        }
        network = Network.newNetwork();
        postgres = new PostgresSupport(network);
        postgres.start();
        backend = new BackendSupport(image, postgres, network);
        backend.start();
        return backend.baseUri();
    }

    /** Returns the singleton stack, initializing it on first call. Thread-safe. */
    public static ApiStack get() {
        if (instance == null) {
            synchronized (ApiStack.class) {
                if (instance == null) {
                    instance = new ApiStack();
                }
            }
        }
        return instance;
    }

    /**
     * Stops all containers and releases the TC network.
     * No-op in {@link StackMode#EXTERNAL} mode.
     * Called automatically by {@link lab.paymentquality.apitest.support.ApiStackExtension}.
     */
    public void stop() {
        if (backend != null) {
            backend.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
        if (network != null) {
            network.close();
        }
    }

    /** Base URI for REST Assured specs (either TC-mapped or externally configured). */
    public String baseUri() {
        return baseUri;
    }

    public StackMode stackMode() {
        return mode;
    }
}
