package lab.paymentquality.payment.internal.web;

// Feature 011 helper exception: maps a missing conditional update header to HTTP 428 instead of generic validation.
public class PaymentPreconditionRequiredException extends RuntimeException {

    public PaymentPreconditionRequiredException(String message) {
        super(message);
    }
}
