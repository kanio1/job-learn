package lab.paymentquality.merchant.internal.domain;

public class TenantBoundaryViolationException extends RuntimeException {

    public TenantBoundaryViolationException() {
        super("Tenant boundary violation");
    }
}
