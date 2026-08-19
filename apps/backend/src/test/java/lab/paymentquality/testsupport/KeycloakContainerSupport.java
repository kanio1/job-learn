package lab.paymentquality.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class KeycloakContainerSupport {

    private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:26.6.1";
    private static final int KEYCLOAK_PORT = 8080;
    private static final String REALM_FILE = "/opt/keycloak/data/import/payment-quality-realm.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                .withEnv("PAYMENT_QUALITY_KEYCLOAK_ADMIN_CLIENT_SECRET", "dev-admin-client-secret")
                .withExposedPorts(KEYCLOAK_PORT)
                .waitingFor(Wait.forHttp("/realms/payment-quality/.well-known/openid-configuration")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public void start() {
        container.start();
        configureUserProfile();
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

    private void configureUserProfile() {
        try {
            String adminToken = obtainBootstrapAdminToken();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest getRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getAdminApiBaseUrl() + "/users/profile"))
                    .header("Authorization", "Bearer " + adminToken)
                    .GET()
                    .build();

            HttpResponse<String> getResponse = client.send(getRequest, HttpResponse.BodyHandlers.ofString());
            requireSuccessful(getResponse, "read Keycloak user profile");
            String updatedProfile = addCustomAttributes(getResponse.body());

            HttpRequest putRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getAdminApiBaseUrl() + "/users/profile"))
                    .header("Authorization", "Bearer " + adminToken)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(updatedProfile))
                    .build();

            HttpResponse<String> putResponse = client.send(putRequest, HttpResponse.BodyHandlers.ofString());
            requireSuccessful(putResponse, "configure Keycloak user profile");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure Keycloak user profile", e);
        }
    }

    private String addCustomAttributes(String existingProfile) throws IOException {
        ObjectNode profile = (ObjectNode) OBJECT_MAPPER.readTree(existingProfile);
        ArrayNode attributes = profile.withArray("attributes");
        addCustomAttributeIfMissing(attributes, "tenant_id", "Tenant ID");
        addCustomAttributeIfMissing(attributes, "merchant_id", "Merchant ID");
        return OBJECT_MAPPER.writeValueAsString(profile);
    }

    private void addCustomAttributeIfMissing(ArrayNode attributes, String name, String displayName) {
        for (JsonNode attribute : attributes) {
            if (name.equals(attribute.path("name").asText())) {
                return;
            }
        }

        ObjectNode attribute = attributes.addObject();
        attribute.put("name", name);
        attribute.put("displayName", displayName);
        attribute.putObject("validations");
        attribute.putObject("annotations");
        ObjectNode permissions = attribute.putObject("permissions");
        permissions.putArray("view").add("admin");
        permissions.putArray("edit").add("admin");
    }

    private String obtainBootstrapAdminToken() throws IOException, InterruptedException {
        String form = "grant_type=password&client_id=admin-cli&username=admin&password=admin";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getTokenEndpoint().replace("/realms/payment-quality/", "/realms/master/")))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        requireSuccessful(response, "obtain bootstrap admin token");
        String token = OBJECT_MAPPER.readTree(response.body()).path("access_token").asText();
        if (token.isBlank()) {
            throw new IOException("Bootstrap admin token response did not contain an access token");
        }
        return token;
    }

    private static void requireSuccessful(HttpResponse<String> response, String operation) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to " + operation + ": HTTP " + response.statusCode());
        }
    }
}
