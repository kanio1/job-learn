package lab.paymentquality.tenant.internal.domain;

public class InvalidPaymentPolicyException extends RuntimeException {

    public InvalidPaymentPolicyException(String message) {
        super(message);
    }
}
