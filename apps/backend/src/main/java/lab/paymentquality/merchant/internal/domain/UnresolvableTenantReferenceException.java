package lab.paymentquality.merchant.internal.domain;

public class UnresolvableTenantReferenceException extends RuntimeException {

    private final String tenantReference;

    public UnresolvableTenantReferenceException(String tenantReference) {
        super("Tenant reference could not be resolved");
        this.tenantReference = tenantReference;
    }

    public String getTenantReference() {
        return tenantReference;
    }
}
