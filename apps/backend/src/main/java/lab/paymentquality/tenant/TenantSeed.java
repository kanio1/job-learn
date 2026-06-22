package lab.paymentquality.tenant;

import java.util.UUID;

public record TenantSeed(
        UUID tenantId,
        String tenantReference,
        String name,
        String tenantType,
        String status
) {
}
