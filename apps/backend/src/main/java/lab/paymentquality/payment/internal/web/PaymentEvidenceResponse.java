package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentEvidenceCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentEvidenceResponse(
        UUID evidenceId,
        UUID paymentOrderId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt,
        PaymentEvidenceCategory category,
        boolean hasContent
) {

    public record ListResponse(List<PaymentEvidenceResponse> content) {
    }
}
