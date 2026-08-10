package lab.paymentquality.checkoutlab.internal.application;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckoutLabOAuthTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn
) {
}
