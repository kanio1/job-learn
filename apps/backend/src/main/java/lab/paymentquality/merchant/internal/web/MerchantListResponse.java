package lab.paymentquality.merchant.internal.web;

import java.util.List;

public record MerchantListResponse(
        List<MerchantResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public MerchantListResponse {
        content = List.copyOf(content != null ? content : List.of());
    }

    public static MerchantListResponse empty(int page, int size) {
        return new MerchantListResponse(List.of(), page, size, 0, 0);
    }
}
