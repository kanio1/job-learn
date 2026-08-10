package lab.paymentquality.checkoutlab.internal.domain;

import java.util.UUID;

public class CheckoutSessionNotFoundException extends RuntimeException {

    public CheckoutSessionNotFoundException(UUID sessionId) {
        super("Checkout session not found: " + sessionId);
    }
}
