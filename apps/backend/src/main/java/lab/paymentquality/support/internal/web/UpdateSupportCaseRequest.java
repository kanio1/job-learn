package lab.paymentquality.support.internal.web;

import jakarta.validation.constraints.Size;

public record UpdateSupportCaseRequest(
        @Size(max = 20) String status,
        @Size(max = 255) String assigneeSubject
) {
}
