package lab.paymentquality.testsupport;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

public class KeycloakContainerSupport {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.6.1";
    private static final int KEYCLOAK_PORT = 8080;
    private static final String REALM_FILE = "/opt/keycloak/data/import/payment-quality-realm.json";

    private final GenericContainer<?> container;

    public KeycloakContainerSupport() {
        this.container = new GenericContainer<>(DockerImageName.parse(KEYCLOAK_IMAGE))
                .withCommand("start-dev", "--import-realm")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../../infra/keycloak/realms/payment-quality-realm.json"),
                        REALM_FILE
                )
                .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
                .withEnv("KC_HTTP_PORT", String.valueOf(KEYCLOAK_PORT))
                .withEnv("PAYMENT_QUALITY_KEYCLOAK_ADMIN_CLIENT_SECRET", "test-admin-secret")
                .withExposedPorts(KEYCLOAK_PORT)
                .waitingFor(Wait.forHttp("/realms/payment-quality/.well-known/openid-configuration")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    public String getBaseUrl() {
        return String.format("http://%s:%d", container.getHost(), container.getMappedPort(KEYCLOAK_PORT));
    }

    public String getIssuerUri() {
        return getBaseUrl() + "/realms/payment-quality";
    }

    public String getJwksUri() {
        return getIssuerUri() + "/protocol/openid-connect/certs";
    }

    public String getAdminApiBaseUrl() {
        return getBaseUrl() + "/admin/realms/payment-quality";
    }

    public String getTokenEndpoint() {
        return getIssuerUri() + "/protocol/openid-connect/token";
    }
}
