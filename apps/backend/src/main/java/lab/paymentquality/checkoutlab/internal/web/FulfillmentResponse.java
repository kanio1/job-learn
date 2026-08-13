package lab.paymentquality.checkoutlab.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillment;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutFulfillmentStatus;

import java.time.Instant;
import java.util.UUID;

public record FulfillmentResponse(
        @JsonProperty("fulfillmentId") UUID fulfillmentId,
        @JsonProperty("sessionId") UUID sessionId,
        @JsonProperty("status") CheckoutFulfillmentStatus status,
        @JsonProperty("sourceEventId") String sourceEventId,
        @JsonProperty("confirmedAt") Instant confirmedAt
) {
    static FulfillmentResponse from(CheckoutFulfillment fulfillment) {
        return new FulfillmentResponse(
                fulfillment.getFulfillmentId(),
                fulfillment.getSessionId(),
                fulfillment.getStatus(),
                fulfillment.getSourceEventId(),
                fulfillment.getConfirmedAt());
    }
}
