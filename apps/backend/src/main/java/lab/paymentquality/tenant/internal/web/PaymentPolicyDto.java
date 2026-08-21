package lab.paymentquality.tenant.internal.web;

import lab.paymentquality.tenant.internal.domain.PaymentPolicy;
import lab.paymentquality.tenant.internal.domain.RefundPolicy;

public record PaymentPolicyDto(
        Boolean autoCapture,
        Integer maxAutoCaptureMinor,
        Integer riskThreshold,
        String refundPolicy
) {

    public static PaymentPolicyDto from(PaymentPolicy policy) {
        PaymentPolicy value = policy != null ? policy : PaymentPolicy.defaults();
        RefundPolicy refund = value.refundPolicy() != null ? value.refundPolicy() : RefundPolicy.MANUAL;
        return new PaymentPolicyDto(
                value.autoCapture(),
                value.maxAutoCaptureMinor(),
                value.riskThreshold(),
                refund.name());
    }

    public PaymentPolicy toDomain() {
        return PaymentPolicy.of(autoCapture, maxAutoCaptureMinor, riskThreshold, refundPolicy);
    }
}
