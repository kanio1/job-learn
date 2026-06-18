package lab.paymentquality.iam.internal.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Server-side configuration for the dedicated Keycloak administration client.
 * Values are supplied through external configuration using the
 * {@code payment-quality.keycloak.admin} prefix.
 */
@Component
@ConfigurationProperties(prefix = "payment-quality.keycloak.admin")
public class KeycloakAdminProperties {

    private String baseUrl;
    private String realm;
    private String clientId;
    private String clientSecret;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
