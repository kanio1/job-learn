package lab.paymentquality.merchant.internal.web;

import java.util.UUID;

public record SearchMerchantHit(UUID merchantId, String merchantReference, String displayName) {
}
