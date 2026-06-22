package lab.paymentquality.iam.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RoleAssignmentRequest(
        @NotNull List<@NotBlank String> assign,
        @NotNull List<@NotBlank String> remove) {

    public RoleAssignmentRequest {
        assign = assign == null ? null : List.copyOf(assign);
        remove = remove == null ? null : List.copyOf(remove);
    }
}
