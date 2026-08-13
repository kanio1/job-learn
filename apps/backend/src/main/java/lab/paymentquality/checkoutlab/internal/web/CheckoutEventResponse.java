package lab.paymentquality.checkoutlab.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import lab.paymentquality.checkoutlab.internal.application.CheckoutLabDeliveryLog;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutEvent;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutEventProcessStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CheckoutEventResponse(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("sessionId") UUID sessionId,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("signatureHeader") String signatureHeader,
        @JsonProperty("processStatus") CheckoutEventProcessStatus processStatus,
        @JsonProperty("attempts") int attempts,
        @JsonProperty("ackStatus") Integer ackStatus,
        @JsonProperty("payload") Map<String, Object> payload,
        @JsonProperty("receivedAt") Instant receivedAt,
        @JsonProperty("lastError") String lastError
) {
    static CheckoutEventResponse from(CheckoutEvent event) {
        return new CheckoutEventResponse(
                event.getEventId(),
                event.getSessionId(),
                event.getEventType(),
                event.getSignatureHeader(),
                event.getProcessStatus(),
                event.getAttempts(),
                event.getAckStatus(),
                event.getPayload(),
                event.getReceivedAt(),
                event.getLastError());
    }

    public record DeliveryResponse(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("attempt") int attempt,
            @JsonProperty("responseStatus") int responseStatus,
            @JsonProperty("at") Instant at
    ) {
        static DeliveryResponse from(CheckoutLabDeliveryLog.DeliveryAttempt attempt) {
            return new DeliveryResponse(
                    attempt.eventId(),
                    attempt.attempt(),
                    attempt.responseStatus(),
                    attempt.at());
        }
    }
}
