package lab.paymentquality.mirrorlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mrl_dispute_evidence")
public class MirrorLabDisputeEvidence {

    @Id
    @Column(name = "evidence_id")
    private UUID evidenceId;

    @Column(name = "dispute_id", nullable = false)
    private UUID disputeId;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected MirrorLabDisputeEvidence() {
    }

    public static MirrorLabDisputeEvidence create(
            UUID disputeId, String filename, String contentType, long sizeBytes, Instant now) {
        MirrorLabDisputeEvidence evidence = new MirrorLabDisputeEvidence();
        evidence.evidenceId = UUID.randomUUID();
        evidence.disputeId = disputeId;
        evidence.filename = filename;
        evidence.contentType = contentType;
        evidence.sizeBytes = sizeBytes;
        evidence.uploadedAt = now;
        return evidence;
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }
}
