package lab.paymentquality.checkoutlab.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCheckoutSessionRequest(
        @NotBlank @Size(max = 120) @JsonProperty("extOrderId") String extOrderId,
        @NotNull @Min(1) @Max(100_000_000) @JsonProperty("amountMinor") Long amountMinor,
        @NotBlank @JsonProperty("currency") String currency,
        @NotBlank @JsonProperty("continueUrl") String continueUrl,
        @NotBlank @JsonProperty("notifyUrl") String notifyUrl,
        @NotNull @Min(1) @JsonProperty("validitySeconds") Long validitySeconds,
        @JsonProperty("language") String language
) {
}
