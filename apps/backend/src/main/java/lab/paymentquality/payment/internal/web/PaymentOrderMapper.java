package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentOrderStatusHistory;

import java.util.List;

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
                order.getCapturedAmountMinor(),
                order.getRefundedAmountMinor(),
                order.getAuthorizedAt(),
                order.getExpiresAt(),
                order.getCapturedAt(),
                order.getCancelledAt(),
                order.getRefundedAt(),
                order.getCancellationReason(),
                order.getRefundReason(),
                order.getMetadata(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static PaymentLifecycleResponse toLifecycleResponse(PaymentOrder order) {
        return new PaymentLifecycleResponse(
                order.getPaymentOrderId(),
                order.getMerchantId(),
                order.getClientOrderReference(),
                order.getStatus().name(),
                order.getAmountMinor(),
                order.getCurrency(),
                order.getCapturedAmountMinor(),
                order.getRefundedAmountMinor(),
                order.getAuthorizedAt(),
                order.getExpiresAt(),
                order.getCapturedAt(),
                order.getCancelledAt(),
                order.getRefundedAt(),
                order.getCancellationReason(),
                order.getRefundReason(),
                order.getMetadata(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static PaymentStatusHistoryResponse toHistoryResponse(List<PaymentOrderStatusHistory> entries) {
        List<PaymentStatusHistoryResponse.StatusHistoryEntry> content = entries.stream()
                .map(e -> new PaymentStatusHistoryResponse.StatusHistoryEntry(
                        e.getStatusHistoryId(),
                        e.getPaymentOrderId(),
                        e.getFromStatus(),
                        e.getToStatus(),
                        e.getAction() != null ? e.getAction().name() : null,
                        e.getActorSubject(),
                        e.getCorrelationId(),
                        e.getCreatedAt()
                ))
                .toList();
        return new PaymentStatusHistoryResponse(content);
    }
}
