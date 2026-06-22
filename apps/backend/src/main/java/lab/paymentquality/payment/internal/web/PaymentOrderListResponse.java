package lab.paymentquality.payment.internal.web;

import java.util.List;

public record PaymentOrderListResponse(
        List<PaymentOrderResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public PaymentOrderListResponse {
        content = List.copyOf(content != null ? content : List.of());
    }
}
