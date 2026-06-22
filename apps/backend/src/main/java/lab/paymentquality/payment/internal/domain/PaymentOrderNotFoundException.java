package lab.paymentquality.payment.internal.domain;

import java.util.UUID;

public class PaymentOrderNotFoundException extends RuntimeException {

    public PaymentOrderNotFoundException(UUID paymentOrderId) {
        super("Payment order not found: " + paymentOrderId);
    }
}
