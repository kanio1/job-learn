package lab.paymentquality.tenant;

/**
 * Thrown when the tenant_id JWT claim cannot be resolved to a valid, active Tenant.
 * Maps to 403 Forbidden. Message must not include tenant_reference of other tenants.
 */
public class TenantResolutionException extends RuntimeException {
    public TenantResolutionException(String message) {
        super(message);
    }
}
