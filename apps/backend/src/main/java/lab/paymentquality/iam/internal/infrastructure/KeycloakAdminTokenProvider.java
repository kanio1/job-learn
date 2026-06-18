package lab.paymentquality.iam.internal.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import lab.paymentquality.iam.internal.domain.exception.KeycloakAdminUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class KeycloakAdminTokenProvider {

    private static final Duration EXPIRY_SKEW = Duration.ofSeconds(30);

    private final KeycloakAdminProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    private volatile CachedToken cachedToken;

    @Autowired
    public KeycloakAdminTokenProvider(KeycloakAdminProperties properties) {
        this(properties, RestClient.create(), Clock.systemUTC());
    }

    KeycloakAdminTokenProvider(
            KeycloakAdminProperties properties,
            RestClient restClient,
            Clock clock) {
        this.properties = properties;
        this.restClient = restClient;
        this.clock = clock;
    }

    String getAdminToken() {
        CachedToken current = cachedToken;
        if (isUsable(current)) {
            return current.value();
        }

        synchronized (this) {
            current = cachedToken;
            if (isUsable(current)) {
                return current.value();
            }

            cachedToken = requestToken();
            return cachedToken.value();
        }
    }

    synchronized void invalidate(String rejectedToken) {
        CachedToken current = cachedToken;
        if (current != null && current.value().equals(rejectedToken)) {
            cachedToken = null;
        }
    }

    String refreshAfterUnauthorized(String rejectedToken) {
        invalidate(rejectedToken);
        return getAdminToken();
    }

    private boolean isUsable(CachedToken token) {
        return token != null && clock.instant().isBefore(token.expiresAt());
    }

    private CachedToken requestToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        try {
            TokenResponse response = restClient.post()
                    .uri(properties.getBaseUrl() + "/realms/{realm}/protocol/openid-connect/token",
                            properties.getRealm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null
                    || response.getAccessToken() == null
                    || response.getAccessToken().isBlank()
                    || response.getExpiresIn() <= 0) {
                throw new KeycloakAdminUnavailableException();
            }

            Instant expiresAt = clock.instant()
                    .plusSeconds(response.getExpiresIn())
                    .minus(EXPIRY_SKEW);
            return new CachedToken(response.getAccessToken(), expiresAt);
        } catch (KeycloakAdminUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new KeycloakAdminUnavailableException(exception);
        }
    }

    private record CachedToken(String value, Instant expiresAt) {
    }

    private static final class TokenResponse {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private long expiresIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }
}
