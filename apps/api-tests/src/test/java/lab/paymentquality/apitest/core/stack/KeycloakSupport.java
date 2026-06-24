package lab.paymentquality.apitest.core.stack;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Wraps a Testcontainers {@link GenericContainer} for Keycloak 26 in {@code start-dev} mode.
 *
 * <p>The container joins the shared TC network with alias {@code keycloak} so other containers
 * (e.g., the backend) can reach it internally. The realm is imported from the classpath on
 * container start via {@code --import-realm}.
 *
 * <p><strong>Issuer URL strategy</strong> (see Phase 6C doc):
 * <ul>
 *   <li>{@link #issuerUri()} — host-mapped URL. Used as the backend's
 *       {@code SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI} so the {@code iss} claim
 *       in tokens minted by the test JVM matches what the backend validates.</li>
 *   <li>{@link #internalJwksUri()} — internal network URL. Used as the backend's
 *       {@code SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI} so the backend container
 *       can reach Keycloak for JWKS verification without leaving the TC network.</li>
 *   <li>{@link #tokenEndpoint()} — host-mapped URL for
 *       {@link lab.paymentquality.apitest.core.auth.KeycloakTokenFactory} running in the test JVM.</li>
 * </ul>
 *
 * <p>SDET learning: the split avoids the Docker-host localhost resolution problem — containers
 * cannot reach {@code localhost:<host-mapped-port>} from within the network, but can always
 * reach peer containers via their network alias.
 */
public final class KeycloakSupport {

    private static final String IMAGE = "quay.io/keycloak/keycloak:26.6.1";
    private static final int KC_PORT = 8080;
    private static final String REALM = "payment-quality";
    private static final String NETWORK_ALIAS = "keycloak";
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(120);

    private final GenericContainer<?> container;

    @SuppressWarnings("resource")
    public KeycloakSupport(Network network) {
        this.container = new GenericContainer<>(DockerImageName.parse(IMAGE))
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .withExposedPorts(KC_PORT)
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                .withClasspathResourceMapping(
                        "keycloak/payment-quality-realm.json",
                        "/opt/keycloak/data/import/payment-quality-realm.json",
                        BindMode.READ_ONLY)
                .withCommand("start-dev", "--import-realm")
                .withLogConsumer(frame -> System.err.print("[KEYCLOAK] " + frame.getUtf8String()))
                .waitingFor(
                        Wait.forHttp("/realms/" + REALM + "/.well-known/openid-configuration")
                                .forStatusCode(200)
                                .withStartupTimeout(STARTUP_TIMEOUT));
    }

    public void start() {
        try {
            container.start();
        } catch (Exception e) {
            System.err.println("=== KEYCLOAK CONTAINER FAILED TO START ===");
            System.err.println(container.getLogs());
            throw e;
        }
    }

    public void stop() {
        container.stop();
    }

    /**
     * Token endpoint for the test JVM (host-mapped port).
     * Used by {@link lab.paymentquality.apitest.core.auth.KeycloakTokenFactory}.
     */
    public String tokenEndpoint() {
        return hostMappedBase() + "/realms/" + REALM + "/protocol/openid-connect/token";
    }

    /**
     * Issuer URI based on the host-mapped port.
     * Matches the {@code iss} claim embedded by Keycloak in tokens minted via the host-mapped URL.
     * Pass to backend as {@code SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI}.
     */
    public String issuerUri() {
        return hostMappedBase() + "/realms/" + REALM;
    }

    /**
     * JWK Set URI using the container-internal network alias.
     * The backend container can reach this without going through the host.
     * Pass to backend as {@code SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI}.
     */
    public String internalJwksUri() {
        return "http://" + NETWORK_ALIAS + ":" + KC_PORT
                + "/realms/" + REALM + "/protocol/openid-connect/certs";
    }

    private String hostMappedBase() {
        return "http://" + container.getHost() + ":" + container.getMappedPort(KC_PORT);
    }
}
