package lab.paymentquality.mirrorlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mrl_disputes")
public class MirrorLabDispute {

    @Id
    @Column(name = "dispute_id")
    private UUID disputeId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MirrorLabDispute() {
    }

    public static MirrorLabDispute open(UUID merchantId, Instant now) {
        MirrorLabDispute dispute = new MirrorLabDispute();
        dispute.disputeId = UUID.randomUUID();
        dispute.merchantId = merchantId;
        dispute.status = "OPEN";
        dispute.createdAt = now;
        dispute.updatedAt = now;
        return dispute;
    }

    public void close(Instant now) {
        this.status = "CLOSED";
        this.updatedAt = now;
    }

    public UUID getDisputeId() {
        return disputeId;
    }

    public String getStatus() {
        return status;
    }
}
