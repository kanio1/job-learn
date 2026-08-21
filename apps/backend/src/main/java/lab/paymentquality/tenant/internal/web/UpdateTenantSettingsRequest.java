package lab.paymentquality.tenant.internal.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// F-C4: PATCH /api/tenants/current/settings request body.
public record UpdateTenantSettingsRequest(

        @Email(message = "contactEmail must be a valid email address")
        @Size(max = 320, message = "contactEmail must not exceed 320 characters")
        String contactEmail,

        @NotBlank(message = "timezone must not be blank")
        @Size(max = 64, message = "timezone must not exceed 64 characters")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9/_+-]{0,63}$",
                 message = "timezone must be a valid IANA timezone identifier")
        String timezone,

        @Size(max = 500, message = "webhookBaseUrl must not exceed 500 characters")
        @Pattern(regexp = "^(https://.*)?$",
                 message = "webhookBaseUrl must start with https:// if provided")
        String webhookBaseUrl,

        @Valid
        PaymentPolicyDto paymentPolicy
) {}
