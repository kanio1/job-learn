package lab.paymentquality.mirrorlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mrl_refund_approvals")
public class MirrorLabRefundApproval {

    @Id
    @Column(name = "approval_id")
    private UUID approvalId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "maker_subject", nullable = false, length = 128)
    private String makerSubject;

    @Column(name = "checker_subject", length = 128)
    private String checkerSubject;

    @Column(name = "step_up_until")
    private Instant stepUpUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MirrorLabRefundApproval() {
    }

    public static MirrorLabRefundApproval pending(
            UUID merchantId, long amountMinor, String maker, Instant now, Instant stepUpUntil) {
        MirrorLabRefundApproval row = new MirrorLabRefundApproval();
        row.approvalId = UUID.randomUUID();
        row.merchantId = merchantId;
        row.amountMinor = amountMinor;
        row.status = "PENDING_APPROVAL";
        row.makerSubject = maker;
        row.stepUpUntil = stepUpUntil;
        row.createdAt = now;
        row.updatedAt = now;
        return row;
    }

    public void approve(String checker, Instant now) {
        this.status = "APPROVED";
        this.checkerSubject = checker;
        this.updatedAt = now;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public String getStatus() {
        return status;
    }

    public String getMakerSubject() {
        return makerSubject;
    }

    public Instant getStepUpUntil() {
        return stepUpUntil;
    }
}
