package lab.paymentquality.payment.internal.web;

// Feature 011 helper exception: separates malformed If-Match syntax from stale-version precondition failures.
public class MalformedPaymentEtagException extends RuntimeException {

    public MalformedPaymentEtagException(String message) {
        super(message);
    }
}
