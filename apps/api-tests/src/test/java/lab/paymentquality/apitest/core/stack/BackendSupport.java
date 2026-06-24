package lab.paymentquality.apitest.core.stack;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Wraps a TC {@link GenericContainer} for the backend Spring Boot OCI image.
 *
 * <p>The backend joins the shared TC network so it can reach both {@code postgres-db} and
 * {@code keycloak} via their network aliases. The 8080 port is mapped to a random host port;
 * {@link #baseUri()} returns the host-accessible URL for REST Assured.
 *
 * <p><strong>Keycloak URL strategy:</strong> ISSUER_URI uses the host-mapped Keycloak URL (matches
 * the {@code iss} claim in tokens minted from the test JVM). JWK_SET_URI uses the internal network
 * alias (the backend container must reach Keycloak's JWKS endpoint to verify token signatures, and
 * cannot use {@code localhost} to do so from within the TC network).
 *
 * <p>See {@link KeycloakSupport} for issuer/JWKS URL details and the rationale for the split.
 */
public final class BackendSupport {

    private static final int BACKEND_PORT = 8080;
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(120);

    private final GenericContainer<?> container;

    @SuppressWarnings("resource")
    public BackendSupport(String image, PostgresSupport postgres, KeycloakSupport keycloak, Network network) {
        this.container = new GenericContainer<>(DockerImageName.parse(image))
                .withNetwork(network)
                .withExposedPorts(BACKEND_PORT)
                .withEnv("DB_URL", postgres.internalJdbcUrl())
                .withEnv("DB_USER", postgres.username())
                .withEnv("DB_PASSWORD", postgres.password())
                .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI", keycloak.issuerUri())
                .withEnv("SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI", keycloak.internalJwksUri())
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
