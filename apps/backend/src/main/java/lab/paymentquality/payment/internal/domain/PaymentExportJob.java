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
@Table(name = "payment_export_jobs")
public class PaymentExportJob {

    public enum Status {
        PENDING,
        READY,
        FAILED
    }

    @Id
    @Column(name = "job_id", nullable = false, updatable = false)
    private UUID jobId;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private Status status;

    @Column(name = "csv_content")
    private String csvContent;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected PaymentExportJob() {
    }

    public static PaymentExportJob pending(UUID merchantId, String createdBy, String idempotencyKey) {
        var job = new PaymentExportJob();
        job.jobId = UUID.randomUUID();
        job.merchantId = merchantId;
        job.status = Status.PENDING;
        job.createdBy = createdBy;
        job.idempotencyKey = (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey;
        job.createdAt = Instant.now();
        return job;
    }

    public void markReady(String csv) {
        this.status = Status.READY;
        this.csvContent = csv;
        this.completedAt = Instant.now();
    }

    public void markFailed(String message) {
        this.status = Status.FAILED;
        this.errorMessage = message == null ? "export failed" : message.substring(0, Math.min(500, message.length()));
        this.completedAt = Instant.now();
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public Status getStatus() {
        return status;
    }

    public String getCsvContent() {
        return csvContent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
