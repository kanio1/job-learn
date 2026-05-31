package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import org.springframework.data.domain.Page;

import java.util.List;

public final class PaymentOrderListMapper {

    private PaymentOrderListMapper() {
    }

    public static PaymentOrderListResponse toListResponse(Page<PaymentOrder> page) {
        List<PaymentOrderResponse> content = page.getContent().stream()
                .map(PaymentOrderMapper::toResponse)
                .toList();
        return new PaymentOrderListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
