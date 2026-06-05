package lab.paymentquality.payment.internal.domain;

public class InvalidCaptureAmountException extends RuntimeException {

    private final long requestedAmount;
    private final long authorizedAmount;

    public InvalidCaptureAmountException(long requestedAmount, long authorizedAmount) {
        super("Capture amount " + requestedAmount + " exceeds authorized amount " + authorizedAmount);
        this.requestedAmount = requestedAmount;
        this.authorizedAmount = authorizedAmount;
    }

    public long getRequestedAmount() {
        return requestedAmount;
    }

    public long getAuthorizedAmount() {
        return authorizedAmount;
    }
}
