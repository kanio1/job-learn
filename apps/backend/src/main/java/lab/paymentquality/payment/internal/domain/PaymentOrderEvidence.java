package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_order_evidence")
public class PaymentOrderEvidence {

    @Id
    @Column(name = "evidence_id", nullable = false, updatable = false)
    private UUID evidenceId;

    @Column(name = "payment_order_id", nullable = false, updatable = false)
    private UUID paymentOrderId;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", length = 200, nullable = false)
    private String storageKey;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected PaymentOrderEvidence() {
    }

    public static PaymentOrderEvidence create(UUID evidenceId, UUID paymentOrderId, String originalFilename,
                                              String contentType, long sizeBytes, Instant uploadedAt) {
        var evidence = new PaymentOrderEvidence();
        evidence.evidenceId = evidenceId;
        evidence.paymentOrderId = paymentOrderId;
        evidence.originalFilename = originalFilename;
        evidence.contentType = contentType;
        evidence.sizeBytes = sizeBytes;
        evidence.uploadedAt = uploadedAt;
        evidence.storageKey = "payment-order-evidence/" + paymentOrderId + "/" + evidenceId;
        return evidence;
    }

    public UUID getEvidenceId() {
        return evidenceId;
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
