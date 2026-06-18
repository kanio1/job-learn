package lab.paymentquality.iam.internal.web.dto;

import java.util.List;

public record UserDetail(
        String id,
        String username,
        String email,
        boolean enabled,
        String tenantId,
        String merchantId,
        List<String> roles) {

    public UserDetail {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
