package lab.paymentquality.merchant;

import java.util.List;
import java.util.UUID;

/**
 * Payment-owned search used by merchant command-palette search.
 * Implemented in the payment module so merchant never reads {@code payment_orders}.
 */
public interface PaymentOrderSearch {

    List<PaymentOrderSearchHit> searchByClientOrderReference(
            String query,
            UUID tenantId,
            UUID merchantId,
            int limit);
}
