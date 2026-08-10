package lab.paymentquality.checkoutlab.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record CheckoutSessionResponse(
        @JsonProperty("sessionId") UUID sessionId,
        @JsonProperty("extOrderId") String extOrderId,
        @JsonProperty("status") CheckoutSessionStatus status,
        @JsonProperty("amountMinor") long amountMinor,
        @JsonProperty("currency") String currency,
        @JsonProperty("validityUntil") Instant validityUntil,
        @JsonProperty("continueUrl") String continueUrl,
        @JsonProperty("notifyUrl") String notifyUrl,
        @JsonProperty("redirectUri") String redirectUri,
        @JsonProperty("correlationId") String correlationId
) {
}
