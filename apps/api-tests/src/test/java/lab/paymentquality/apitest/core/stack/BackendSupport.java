package lab.paymentquality.apitest.core.stack;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Wraps a TC {@link GenericContainer} for the backend Spring Boot OCI image.
 *
 * <p>The backend joins the shared TC network so it can reach postgres via the
 * {@code postgres-db} network alias. The 8080 port is mapped to a random host port;
 * {@link #baseUri()} returns the host-accessible URL for REST Assured.
 *
 * <p><strong>Keycloak wiring — Phase 6B-lite note:</strong> placeholder JWT issuer/JWKS URLs
 * are passed so the backend starts without a live Keycloak. Spring Boot uses {@code jwk-set-uri}
 * for the JWK decoder when explicitly set, and does NOT eagerly fetch it at startup — validation
 * only happens on authenticated requests. Since 6B-lite only exercises public endpoints,
 * the placeholder URLs are never actually fetched. Replace with real Keycloak URLs in Phase 6B-full.
 */
public final class BackendSupport {

    private static final int BACKEND_PORT = 8080;
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(120);

    private static final String PLACEHOLDER_KEYCLOAK_BASE =
            "http://keycloak-placeholder.invalid/realms/payment-quality";

    private final GenericContainer<?> container;

    public BackendSupport(String image, PostgresSupport postgres, Network network) {
        this.container = new GenericContainer<>(DockerImageName.parse(image))
                .withNetwork(network)
                .withExposedPorts(BACKEND_PORT)
                .withEnv("DB_URL", postgres.internalJdbcUrl())
                .withEnv("DB_USER", postgres.username())
                .withEnv("DB_PASSWORD", postgres.password())
                .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI",
                        PLACEHOLDER_KEYCLOAK_BASE)
                .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI",
                        PLACEHOLDER_KEYCLOAK_BASE + "/protocol/openid-connect/certs")
                .withEnv("APP_TESTING_ENABLED", "true")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withLogConsumer(frame -> System.err.print("[BACKEND] " + frame.getUtf8String()))
                .waitingFor(
                        Wait.forHttp("/api/status")
                                .forStatusCode(200)
                                .withStartupTimeout(STARTUP_TIMEOUT));
    }

    public void start() {
        try {
            container.start();
        } catch (Exception e) {
            System.err.println("=== BACKEND CONTAINER FAILED TO START ===");
            System.err.println(container.getLogs());
            throw e;
        }
    }

    public void stop() {
        container.stop();
    }

    /** Host-accessible base URI for REST Assured (mapped random port). */
    public String baseUri() {
        return "http://" + container.getHost() + ":" + container.getMappedPort(BACKEND_PORT);
    }
}
