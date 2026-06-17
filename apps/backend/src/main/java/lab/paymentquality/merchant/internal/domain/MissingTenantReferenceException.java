package lab.paymentquality.merchant.internal.domain;

public class MissingTenantReferenceException extends RuntimeException {

    public MissingTenantReferenceException() {
        super("Tenant reference is required");
    }
}
