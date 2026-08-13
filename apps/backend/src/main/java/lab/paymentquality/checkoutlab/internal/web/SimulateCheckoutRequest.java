package lab.paymentquality.checkoutlab.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lab.paymentquality.checkoutlab.internal.domain.CheckoutSessionStatus;

public record SimulateCheckoutRequest(
        @NotBlank @JsonProperty("outcome") String outcome
) {
    CheckoutSessionStatus toStatus() {
        return switch (outcome.toUpperCase()) {
            case "COMPLETED", "APPROVED" -> CheckoutSessionStatus.COMPLETED;
            case "CANCELED", "CANCELLED", "DECLINED" -> CheckoutSessionStatus.CANCELED;
            case "PENDING" -> CheckoutSessionStatus.PENDING;
            default -> throw new IllegalArgumentException("Unknown simulate outcome: " + outcome);
        };
    }
}
