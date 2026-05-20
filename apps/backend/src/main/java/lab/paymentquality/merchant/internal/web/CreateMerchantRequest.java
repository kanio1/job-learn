package lab.paymentquality.merchant.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMerchantRequest(
        @NotBlank @Size(max = 64) String merchantReference,
        @NotBlank @Size(min = 2, max = 120) String displayName) {
}
