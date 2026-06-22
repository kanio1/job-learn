package lab.paymentquality.iam.internal.domain;

import java.util.List;

public record ManagedUser(
        String id,
        String username,
        String email,
        boolean enabled,
        String tenantId,
        String merchantId,
        List<String> roles) {

    public ManagedUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
