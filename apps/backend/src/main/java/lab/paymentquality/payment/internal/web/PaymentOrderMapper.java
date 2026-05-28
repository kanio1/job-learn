package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentOrder;

public final class PaymentOrderMapper {

    private PaymentOrderMapper() {
    }

    public static PaymentOrderResponse toResponse(PaymentOrder order) {
        return new PaymentOrderResponse(
                order.getPaymentOrderId(),
                order.getMerchantId(),
                order.getClientOrderReference(),
                order.getAmountMinor(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
