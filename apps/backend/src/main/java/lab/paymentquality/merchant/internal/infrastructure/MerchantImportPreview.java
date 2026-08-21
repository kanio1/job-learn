package lab.paymentquality.merchant.internal.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merchant_import_previews")
public class MerchantImportPreview {

    @Id
    @Column(name = "preview_id", nullable = false, updatable = false)
    private UUID previewId;

    @Column(name = "checksum", length = 64, nullable = false, updatable = false)
    private String checksum;

    @Column(name = "created_by", length = 160, nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "platform_scoped", nullable = false, updatable = false)
    private boolean platformScoped;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private List<MerchantImportAcceptedRow> payload;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "committed_at")
    private Instant committedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MerchantImportPreview() {
    }

    public static MerchantImportPreview create(
            UUID previewId,
            String checksum,
            String createdBy,
            UUID tenantId,
            boolean platformScoped,
            List<MerchantImportAcceptedRow> payload,
            Instant expiresAt,
            Instant createdAt) {
        var preview = new MerchantImportPreview();
        preview.previewId = previewId;
        preview.checksum = checksum;
        preview.createdBy = createdBy;
        preview.tenantId = tenantId;
        preview.platformScoped = platformScoped;
        preview.payload = List.copyOf(payload);
        preview.expiresAt = expiresAt;
        preview.createdAt = createdAt;
        return preview;
    }

    public void markCommitted(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public UUID getPreviewId() {
        return previewId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public boolean isPlatformScoped() {
        return platformScoped;
    }

    public List<MerchantImportAcceptedRow> getPayload() {
        return payload;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }
}
