package lab.paymentquality.support.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSupportCaseRequest(
        @NotNull UUID merchantId,
        UUID paymentOrderId,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 10) String priority,
        @Size(max = 32) String caseReference,
        @Size(max = 255) String assigneeSubject
) {
}
