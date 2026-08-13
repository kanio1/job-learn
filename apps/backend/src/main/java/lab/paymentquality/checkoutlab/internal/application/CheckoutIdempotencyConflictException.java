package lab.paymentquality.checkoutlab.internal.application;

public class CheckoutIdempotencyConflictException extends RuntimeException {

    public CheckoutIdempotencyConflictException() {
        super("Idempotency-Key was reused with a different request fingerprint");
    }
}
