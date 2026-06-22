package lab.paymentquality.iam.internal.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String temporaryPassword,
        String tenantId,
        String merchantId,
        @NotNull @Size(min = 1) List<@NotBlank String> roles) {

    public CreateUserRequest {
        roles = roles == null ? null : List.copyOf(roles);
    }
}
