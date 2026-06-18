package lab.paymentquality.iam.internal.domain.exception;

public class MissingTenantReferenceException extends RuntimeException {

    public MissingTenantReferenceException() {
        super("Tenant reference is required");
    }
}
