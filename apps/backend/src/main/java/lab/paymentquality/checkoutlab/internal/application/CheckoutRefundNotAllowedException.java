package lab.paymentquality.checkoutlab.internal.application;

import java.util.UUID;

public class CheckoutRefundNotAllowedException extends RuntimeException {

    public CheckoutRefundNotAllowedException(UUID sessionId) {
        super("Refund is allowed only for COMPLETED checkout sessions: " + sessionId);
    }
}
