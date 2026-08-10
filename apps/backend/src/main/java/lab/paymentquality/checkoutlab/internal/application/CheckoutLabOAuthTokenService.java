package lab.paymentquality.checkoutlab.internal.application;

import lab.paymentquality.checkoutlab.internal.config.CheckoutLabProperties;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabOAuthTokenResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.checkout-lab.enabled", havingValue = "true")
public class CheckoutLabOAuthTokenService {

    private static final String CLIENT_CREDENTIALS = "client_credentials";

    private final CheckoutLabProperties properties;
    private final CheckoutLabAccessTokenService accessTokenService;

    CheckoutLabOAuthTokenService(
            CheckoutLabProperties properties,
            CheckoutLabAccessTokenService accessTokenService) {
        this.properties = properties;
        this.accessTokenService = accessTokenService;
    }

    public Optional<CheckoutLabOAuthTokenResponse> authenticateForToken(
            String contentType,
            String grantType,
            String clientId,
            String clientSecret) {
        if (!acceptsContentType(contentType)
                || !isAuthorizedClientCredentialsRequest(grantType, clientId, clientSecret)) {
            return Optional.empty();
        }
        return Optional.of(issueToken());
    }

    boolean acceptsContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        return contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }

    boolean isAuthorizedClientCredentialsRequest(String grantType, String clientId, String clientSecret) {
        if (!CLIENT_CREDENTIALS.equals(grantType)) {
            return false;
        }
        if (!constantTimeEquals(clientId, properties.oauthClientId())) {
            return false;
        }
        return constantTimeEquals(clientSecret, properties.oauthClientSecret());
    }

    CheckoutLabOAuthTokenResponse issueToken() {
        return new CheckoutLabOAuthTokenResponse(
                accessTokenService.issueAccessToken(),
                "Bearer",
                accessTokenService.expiresInSeconds());
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
