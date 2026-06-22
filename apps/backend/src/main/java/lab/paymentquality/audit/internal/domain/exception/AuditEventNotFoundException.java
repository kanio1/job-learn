package lab.paymentquality.audit.internal.domain.exception;

public final class AuditEventNotFoundException extends RuntimeException {

    public AuditEventNotFoundException() {
        super("Audit event not found");
    }
}
