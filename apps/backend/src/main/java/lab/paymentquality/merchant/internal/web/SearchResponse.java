package lab.paymentquality.merchant.internal.web;

import java.util.List;

public record SearchResponse(List<SearchMerchantHit> merchants, List<SearchPaymentHit> payments) {
}
