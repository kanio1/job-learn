package lab.paymentquality.iam.internal.web.dto;

import java.util.List;

public record UserSummary(
        String id,
        String username,
        String email,
        boolean enabled,
        String tenantId,
        String merchantId,
        List<String> roles) {

    public UserSummary {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
