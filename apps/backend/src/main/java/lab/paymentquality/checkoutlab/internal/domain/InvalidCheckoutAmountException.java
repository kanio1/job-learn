package lab.paymentquality.checkoutlab.internal.domain;

public class InvalidCheckoutAmountException extends RuntimeException {

    public InvalidCheckoutAmountException(long amountMinor) {
        super("amountMinor must be between 1 and 100000000, got: " + amountMinor);
    }
}
