package lab.paymentquality.payment.internal.domain;

public class InvalidPaymentAmountException extends RuntimeException {

    public InvalidPaymentAmountException(long minorUnits) {
        super("Amount minor units must be between 1 and 100000000, got: " + minorUnits);
    }
}
