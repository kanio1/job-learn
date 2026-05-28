package lab.paymentquality.payment.internal.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePaymentOrderRequest(
        @NotNull(message = "amountMinor is required")
        @Min(value = 1, message = "amountMinor must be at least 1")
        @Max(value = 100000000, message = "amountMinor must be at most 100000000")
        Long amountMinor,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be exactly 3 characters")
        String currency,

        @NotBlank(message = "clientOrderReference is required")
        @Size(max = 120, message = "clientOrderReference must not exceed 120 characters")
        String clientOrderReference
) {
}
