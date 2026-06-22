package lab.paymentquality.tenant;

import java.util.UUID;

/**
 * Resolved per-request tenant context derived from the JWT tenant_id claim.
 */
public record TenantContext(
        UUID tenantId,
        TenantReference tenantReference,
        boolean isPlatformScoped
) {
    public boolean isTenantScoped() {
        return !isPlatformScoped;
    }
}
