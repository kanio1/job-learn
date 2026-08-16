package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refund_approvals")
public class PaymentRefundApproval {

    public enum Status {
        PENDING,
        APPROVED
    }

    @Id
    @Column(name = "approval_id", nullable = false, updatable = false)
    private UUID approvalId;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "payment_order_id", nullable = false, updatable = false)
    private UUID paymentOrderId;

    @Column(name = "amount_minor")
    private Long amountMinor;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "maker_subject", nullable = false, updatable = false)
    private String makerSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected PaymentRefundApproval() {
    }

    public static PaymentRefundApproval pending(UUID merchantId, UUID paymentOrderId, Long amountMinor,
                                                String reason, String makerSubject) {
        var approval = new PaymentRefundApproval();
        approval.approvalId = UUID.randomUUID();
        approval.merchantId = merchantId;
        approval.paymentOrderId = paymentOrderId;
        approval.amountMinor = amountMinor;
        approval.reason = reason;
        approval.makerSubject = makerSubject;
        approval.status = Status.PENDING;
        approval.createdAt = Instant.now();
        return approval;
    }

    public void markApproved() {
        this.status = Status.APPROVED;
        this.approvedAt = Instant.now();
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public Long getAmountMinor() {
        return amountMinor;
    }

    public String getReason() {
        return reason;
    }

    public String getMakerSubject() {
        return makerSubject;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }
}
