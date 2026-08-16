package lab.paymentquality.payment.internal.domain;

import java.util.UUID;

public class PaymentRefundApprovalNotFoundException extends RuntimeException {

    public PaymentRefundApprovalNotFoundException(UUID approvalId) {
        super("Refund approval not found: " + approvalId);
    }
}
