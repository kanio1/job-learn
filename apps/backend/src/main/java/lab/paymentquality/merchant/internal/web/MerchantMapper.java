package lab.paymentquality.merchant.internal.web;

import lab.paymentquality.merchant.internal.domain.Merchant;
import org.springframework.data.domain.Page;

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
                merchant.getUpdatedAt(),
                merchant.isRiskFlagged(),
                merchant.getVersion(),
                merchant.getContactPhone(),
                merchant.getContactAddress());
    }

    public static MerchantListResponse toListResponse(Page<Merchant> page) {
        List<MerchantResponse> content = page.getContent().stream()
                .map(MerchantMapper::toResponse)
                .toList();
        return new MerchantListResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
