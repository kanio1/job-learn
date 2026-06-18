package lab.paymentquality.iam.internal.domain.exception;

public class TenantBoundaryViolationException extends RuntimeException {

    public TenantBoundaryViolationException() {
        super("Access denied");
    }
}
