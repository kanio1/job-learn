package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.domain.Merchant;

import java.util.List;

public final class MerchantMapper {

    private MerchantMapper() {
    }

    public static MerchantResponse toResponse(Merchant merchant) {
        return new MerchantResponse(
                merchant.getMerchantId(),
                merchant.getNormalizedReference(),
                merchant.getDisplayName(),
                merchant.getStatus().name(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt());
    }

    public static MerchantListResponse toListResponse(List<Merchant> merchants) {
        List<MerchantResponse> list = merchants.stream()
                .map(MerchantMapper::toResponse)
                .toList();
        return new MerchantListResponse(list);
    }
}
