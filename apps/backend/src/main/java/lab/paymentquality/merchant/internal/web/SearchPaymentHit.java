package lab.paymentquality.merchant.internal.web;

import java.util.UUID;

public record SearchPaymentHit(UUID paymentOrderId, UUID merchantId, String clientOrderReference) {
}
