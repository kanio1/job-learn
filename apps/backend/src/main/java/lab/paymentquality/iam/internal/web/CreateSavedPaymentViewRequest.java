package lab.paymentquality.iam.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record CreateSavedPaymentViewRequest(
        @NotBlank @Size(max = 80) String name,
        Map<String, Object> filters,
        List<String> columns,
        Boolean isDefault) {
}
