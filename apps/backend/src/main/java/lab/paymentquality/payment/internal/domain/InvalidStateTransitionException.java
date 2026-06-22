package lab.paymentquality.payment.internal.domain;

public class InvalidStateTransitionException extends RuntimeException {

    private final PaymentStatus fromStatus;
    private final PaymentStatus toStatus;

    public InvalidStateTransitionException(PaymentStatus fromStatus, PaymentStatus toStatus) {
        super("Cannot transition from " + fromStatus + " to " + toStatus);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

    public PaymentStatus getFromStatus() {
        return fromStatus;
    }

    public PaymentStatus getToStatus() {
        return toStatus;
    }
}
