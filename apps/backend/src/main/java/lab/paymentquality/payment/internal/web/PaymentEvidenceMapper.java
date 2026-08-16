package lab.paymentquality.payment.internal.web;

import lab.paymentquality.payment.internal.domain.PaymentOrderEvidence;

public final class PaymentEvidenceMapper {

    private PaymentEvidenceMapper() {
    }

    public static PaymentEvidenceResponse toResponse(PaymentOrderEvidence evidence) {
        return new PaymentEvidenceResponse(
                evidence.getEvidenceId(),
                evidence.getPaymentOrderId(),
                evidence.getOriginalFilename(),
                evidence.getContentType(),
                evidence.getSizeBytes(),
                evidence.getUploadedAt(),
                evidence.getCategory(),
                evidence.getContentBytes() != null && evidence.getContentBytes().length > 0
        );
    }
}
