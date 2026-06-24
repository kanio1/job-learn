package lab.paymentquality.apitest.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mints Keycloak access tokens via ROPC (Resource Owner Password Credentials Grant).
 *
 * <p>Uses the {@code payment-quality-dashboard} client (public, ROPC-enabled) against the
 * {@code payment-quality} realm. One instance is shared per live test run via {@link Identities#install}.
 *
 * <p>Tokens are cached by username and reused until within 30 seconds of expiry.
 *
 * <p>This class is <strong>not</strong> usable in offline unit tests — call
 * {@link TokenFactory#placeholder()} when no live Keycloak is available.
 *
 * <p>SDET learning notes:
 * <ul>
 *   <li>ROPC requires a public client with {@code directAccessGrantsEnabled: true} in Keycloak.</li>
 *   <li>Tokens minted via the host-mapped URL have {@code iss = http://localhost:<port>/realms/...}.
 *       The backend must validate against the same issuer URL — see {@link KeycloakSupport#issuerUri()}.</li>
 *   <li>Strategy pattern: {@code forUser()} captures credentials in a lambda and returns a
 *       {@link TokenFactory}. The {@link Identity} stores the factory; the test calls
 *       {@link Identity#token()} without knowing whether it's hitting Keycloak or returning a placeholder.</li>
 * </ul>
 */
public final class KeycloakTokenFactory {

    private static final String CLIENT_ID = "payment-quality-dashboard";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final String tokenEndpoint;
    private final HttpClient httpClient;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    public KeycloakTokenFactory(String tokenEndpoint) {
        this.tokenEndpoint = Objects.requireNonNull(tokenEndpoint, "tokenEndpoint");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Returns a {@link TokenFactory} that mints (or returns a cached) access token for the given
     * Keycloak user on each call to {@link TokenFactory#tokenFor}.
     */
    public TokenFactory forUser(String username, String password) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        return identity -> getOrMint(username, password);
    }

    private synchronized String getOrMint(String username, String password) {
        CachedToken cached = cache.get(username);
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        CachedToken fresh = mintToken(username, password);
        cache.put(username, fresh);
        return fresh.token();
    }

    private CachedToken mintToken(String username, String password) {
        String body = "grant_type=password"
                + "&client_id=" + encode(CLIENT_ID)
                + "&username=" + encode(username)
                + "&password=" + encode(password);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Keycloak ROPC failed for user '" + username + "': HTTP " + response.statusCode()
                                + " — " + response.body());
            }
            return parseTokenResponse(response.body());
        } catch (IOException e) {
            throw new IllegalStateException("Token endpoint unreachable: " + tokenEndpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Token minting interrupted for user '" + username + "'", e);
        }
    }

    private static CachedToken parseTokenResponse(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String token = node.get("access_token").asText();
            long expiresIn = node.path("expires_in").asLong(300);
            return new CachedToken(token, Instant.now().plusSeconds(expiresIn));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Keycloak token response", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Cached token with its expiry time. Package-private for unit testing of expiry logic.
     *
     * <p>30-second safety margin prevents using a token that is about to expire mid-request.
     */
    static record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt.minusSeconds(30));
        }
    }
}
