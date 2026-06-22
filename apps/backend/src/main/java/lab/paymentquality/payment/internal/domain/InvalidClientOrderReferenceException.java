package lab.paymentquality.payment.internal.domain;

public class InvalidClientOrderReferenceException extends RuntimeException {

    public InvalidClientOrderReferenceException(String message) {
        super(message);
    }
}
