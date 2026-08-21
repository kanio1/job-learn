package lab.paymentquality.merchant;

import java.util.UUID;

public record PaymentOrderSearchHit(UUID paymentOrderId, UUID merchantId, String clientOrderReference) {
}
