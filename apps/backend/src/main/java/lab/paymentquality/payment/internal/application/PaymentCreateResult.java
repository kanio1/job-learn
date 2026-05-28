package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentOrder;

public record PaymentCreateResult(PaymentOrder paymentOrder, boolean created) {

    public static PaymentCreateResult created(PaymentOrder order) {
        return new PaymentCreateResult(order, true);
    }

    public static PaymentCreateResult replayed(PaymentOrder order) {
        return new PaymentCreateResult(order, false);
    }
}
