package lab.paymentquality.checkoutlab.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;

import java.util.UUID;

public record CreateCheckoutSessionResponse(
        @JsonProperty("sessionId") UUID sessionId,
        @JsonProperty("redirectUri") String redirectUri,
        @JsonProperty("status") CheckoutSessionStatus status
) {
}
