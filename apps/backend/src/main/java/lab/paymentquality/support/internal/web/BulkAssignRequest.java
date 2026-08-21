package lab.paymentquality.support.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BulkAssignRequest(
        List<UUID> caseIds,
        @NotBlank @Size(max = 255) String assigneeSubject
) {
}
