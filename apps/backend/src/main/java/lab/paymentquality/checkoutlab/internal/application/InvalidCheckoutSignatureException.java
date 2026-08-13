package lab.paymentquality.checkoutlab.internal.application;

public class InvalidCheckoutSignatureException extends RuntimeException {

    public InvalidCheckoutSignatureException(String message) {
        super(message);
    }
}
