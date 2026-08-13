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
    private static final String TRUSTED_MERCHANT = "trusted_merchant";

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
            String clientSecret,
            String email,
            String extCustomerId) {
        if (!acceptsContentType(contentType)
                || !isAuthorizedRequest(grantType, clientId, clientSecret, email, extCustomerId)) {
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

    boolean isAuthorizedRequest(
            String grantType,
            String clientId,
            String clientSecret,
            String email,
            String extCustomerId) {
        if (!constantTimeEquals(clientId, properties.oauthClientId())
                || !constantTimeEquals(clientSecret, properties.oauthClientSecret())) {
            return false;
        }
        if (CLIENT_CREDENTIALS.equals(grantType)) {
            return true;
        }
        if (TRUSTED_MERCHANT.equals(grantType)) {
            return email != null && !email.isBlank() && extCustomerId != null && !extCustomerId.isBlank();
        }
        return false;
    }

    boolean isAuthorizedClientCredentialsRequest(String grantType, String clientId, String clientSecret) {
        return isAuthorizedRequest(grantType, clientId, clientSecret, "n/a", "n/a")
                && CLIENT_CREDENTIALS.equals(grantType);
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
