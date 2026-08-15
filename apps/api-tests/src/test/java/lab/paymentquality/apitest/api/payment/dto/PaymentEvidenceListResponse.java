package lab.paymentquality.apitest.api.payment.dto;

import java.util.List;

/**
 * Envelope for {@code GET .../payment-orders/{id}/evidence}.
 *
 * <p>Mirrors {@code PaymentEvidenceResponse.ListResponse} ({@code { "content": [...] }}).
 */
public record PaymentEvidenceListResponse(List<PaymentEvidenceResponse> content) {
}
