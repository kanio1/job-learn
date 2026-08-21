package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_refund_challenges")
public class PaymentRefundChallenge {

    @Id
    @Column(name = "challenge_id", nullable = false, updatable = false)
    private UUID challengeId;

    @Column(name = "approval_id", nullable = false, updatable = false)
    private UUID approvalId;

    @Column(name = "payment_order_id", nullable = false, updatable = false)
    private UUID paymentOrderId;

    @Column(name = "pin_hash", nullable = false, length = 128)
    private String pinHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentRefundChallenge() {
    }

    public static PaymentRefundChallenge open(
            UUID approvalId,
            UUID paymentOrderId,
            String pinHash,
            Instant expiresAt,
            Instant createdAt) {
        var challenge = new PaymentRefundChallenge();
        challenge.challengeId = UUID.randomUUID();
        challenge.approvalId = approvalId;
        challenge.paymentOrderId = paymentOrderId;
        challenge.pinHash = pinHash;
        challenge.expiresAt = expiresAt;
        challenge.attemptCount = 0;
        challenge.createdAt = createdAt;
        return challenge;
    }

    public void markVerified(Instant at) {
        this.verifiedAt = at;
    }

    public void registerFailure(int maxAttempts, Instant lockedUntil, Instant now) {
        this.attemptCount = this.attemptCount + 1;
        if (this.attemptCount >= maxAttempts) {
            this.lockedUntil = lockedUntil;
        }
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isLocked(Instant now) {
        if (attemptCount >= 5) {
            return true;
        }
        return lockedUntil != null && !now.isAfter(lockedUntil);
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public UUID getApprovalId() {
        return approvalId;
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public String getPinHash() {
        return pinHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
