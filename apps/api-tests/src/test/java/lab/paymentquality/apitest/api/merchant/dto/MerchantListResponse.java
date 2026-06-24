package lab.paymentquality.apitest.api.merchant.dto;

import java.util.List;

/**
 * Test-side representation of {@code GET /api/merchants} response body.
 *
 * <p>Mirrors the backend's {@code MerchantListResponse} envelope:
 * {@code { "merchants": [ {...}, {...} ] }}.
 *
 * <p>Deserialized via {@code TypeRef<MerchantListResponse>} in {@link lab.paymentquality.apitest.api.merchant.MerchantsApi}
 * to preserve generic type information at runtime.
 */
public record MerchantListResponse(List<MerchantResponse> merchants) {
}
