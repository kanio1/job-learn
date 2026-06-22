package lab.paymentquality.payment.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_record_id", nullable = false, updatable = false)
    private UUID idempotencyRecordId;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "idempotency_key_hash", length = 64, nullable = false, updatable = false)
    private String idempotencyKeyHash;

    @Column(name = "request_fingerprint_hash", length = 64, nullable = false)
    private String requestFingerprintHash;

    @Column(name = "payment_order_id")
    private UUID paymentOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "action", length = 20)
    private String action;

    protected IdempotencyRecord() {
    }

    public static IdempotencyRecord reserve(UUID idempotencyRecordId, UUID merchantId,
                                              String idempotencyKeyHash, String requestFingerprintHash) {
        return reserve(idempotencyRecordId, merchantId, idempotencyKeyHash, requestFingerprintHash, "CREATE", null);
    }

    public static IdempotencyRecord reserve(UUID idempotencyRecordId, UUID merchantId,
                                             String idempotencyKeyHash, String requestFingerprintHash,
                                             String action, UUID paymentOrderId) {
        var record = new IdempotencyRecord();
        record.idempotencyRecordId = idempotencyRecordId;
        record.merchantId = merchantId;
        record.idempotencyKeyHash = idempotencyKeyHash;
        record.requestFingerprintHash = requestFingerprintHash;
        record.action = action;
        record.paymentOrderId = paymentOrderId;
        record.createdAt = Instant.now();
        return record;
    }

    public void complete(UUID paymentOrderId) {
        this.paymentOrderId = paymentOrderId;
        this.completedAt = Instant.now();
    }

    public void setAction(String action) {
        this.action = action;
    }

    public UUID getIdempotencyRecordId() {
        return idempotencyRecordId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getIdempotencyKeyHash() {
        return idempotencyKeyHash;
    }

    public String getRequestFingerprintHash() {
        return requestFingerprintHash;
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getAction() {
        return action;
    }
}
