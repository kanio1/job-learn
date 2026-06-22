package lab.paymentquality.payment.internal.domain;

// Feature 011 domain exception: signals that the caller's If-Match version no longer matches the current order version.
public class PaymentOrderVersionMismatchException extends RuntimeException {

    public PaymentOrderVersionMismatchException() {
        super("Payment order was modified after the client loaded it.");
    }
}
