package lab.paymentquality.tenant.internal.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentPolicy(
        boolean autoCapture,
        int maxAutoCaptureMinor,
        int riskThreshold,
        RefundPolicy refundPolicy
) {

    public static PaymentPolicy defaults() {
        return new PaymentPolicy(false, 0, 50, RefundPolicy.MANUAL);
    }

    public static PaymentPolicy of(
            Boolean autoCapture,
            Integer maxAutoCaptureMinor,
            Integer riskThreshold,
            String refundPolicyRaw) {
        if (autoCapture == null) {
            throw new InvalidPaymentPolicyException("autoCapture is required");
        }
        if (riskThreshold == null) {
            throw new InvalidPaymentPolicyException("riskThreshold is required");
        }
        if (riskThreshold < 0 || riskThreshold > 100) {
            throw new InvalidPaymentPolicyException("riskThreshold must be between 0 and 100 inclusive");
        }
        RefundPolicy refundPolicy = parseRefundPolicy(refundPolicyRaw);
        int max;
        if (autoCapture) {
            if (maxAutoCaptureMinor == null) {
                throw new InvalidPaymentPolicyException(
                        "maxAutoCaptureMinor is required when autoCapture is true");
            }
            if (maxAutoCaptureMinor < 1) {
                throw new InvalidPaymentPolicyException(
                        "maxAutoCaptureMinor must be at least 1 when autoCapture is true");
            }
            max = maxAutoCaptureMinor;
        } else {
            max = 0;
        }
        return new PaymentPolicy(autoCapture, max, riskThreshold, refundPolicy);
    }

    private static RefundPolicy parseRefundPolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidPaymentPolicyException("refundPolicy is required");
        }
        try {
            return RefundPolicy.valueOf(raw.strip());
        } catch (IllegalArgumentException ex) {
            throw new InvalidPaymentPolicyException("refundPolicy must be MANUAL or AUTOMATIC");
        }
    }
}
