package lab.paymentquality.payment.internal.domain;

public class InvalidRefundAmountException extends RuntimeException {

    private final long requestedAmount;
    private final long capturedAmount;

    public InvalidRefundAmountException(long requestedAmount, long capturedAmount) {
        super("Refund amount " + requestedAmount + " exceeds captured amount " + capturedAmount);
        this.requestedAmount = requestedAmount;
        this.capturedAmount = capturedAmount;
    }

    public long getRequestedAmount() {
        return requestedAmount;
    }

    public long getCapturedAmount() {
        return capturedAmount;
    }
}
