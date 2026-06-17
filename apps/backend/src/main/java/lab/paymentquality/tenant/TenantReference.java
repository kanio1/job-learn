package lab.paymentquality.tenant;

/**
 * Immutable natural-key wrapper for the tenant_reference string carried by the JWT tenant_id claim.
 */
public record TenantReference(String value) {
    public TenantReference {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantReference must not be blank");
        }
    }

    public static TenantReference of(String value) {
        return new TenantReference(value.strip());
    }
}
