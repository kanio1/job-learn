package lab.paymentquality.merchant.internal.domain;

public class InvalidTransitionException extends RuntimeException {

    private final MerchantStatus from;
    private final MerchantStatus to;

    public InvalidTransitionException(MerchantStatus from, MerchantStatus to) {
        super("Cannot transition merchant from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public MerchantStatus getFrom() {
        return from;
    }

    public MerchantStatus getTo() {
        return to;
    }
}
