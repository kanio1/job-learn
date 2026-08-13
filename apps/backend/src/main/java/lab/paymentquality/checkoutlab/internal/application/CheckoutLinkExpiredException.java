package lab.paymentquality.checkoutlab.internal.application;

import java.util.UUID;

public class CheckoutLinkExpiredException extends RuntimeException {

    public CheckoutLinkExpiredException(UUID sessionId) {
        super("Checkout session " + sessionId + " payment link has expired");
    }
}
