package lab.paymentquality.checkoutlab.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.checkout-lab")
public record CheckoutLabProperties(
        boolean enabled,
        String oauthClientId,
        String oauthClientSecret,
        String hmacSecret,
        long signatureToleranceSeconds,
        String hostedCheckoutBaseUrl
) {

    public CheckoutLabProperties {
        oauthClientId = oauthClientId == null || oauthClientId.isBlank()
                ? "checkout-lab-merchant"
                : oauthClientId;
        oauthClientSecret = oauthClientSecret == null || oauthClientSecret.isBlank()
                ? "change-me"
                : oauthClientSecret;
        hmacSecret = hmacSecret == null || hmacSecret.isBlank()
                ? "change-me-too"
                : hmacSecret;
        if (signatureToleranceSeconds <= 0) {
            signatureToleranceSeconds = 300;
        }
        hostedCheckoutBaseUrl = hostedCheckoutBaseUrl == null || hostedCheckoutBaseUrl.isBlank()
                ? "http://localhost:3000"
                : hostedCheckoutBaseUrl.replaceAll("/$", "");
    }
}
