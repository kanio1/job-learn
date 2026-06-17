package lab.paymentquality.merchant.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "normalized_reference", length = 64, nullable = false, unique = true)
    private String normalizedReference;

    @Column(name = "display_name", length = 120, nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MerchantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    protected Merchant() {
    }

    public static Merchant create(UUID merchantId, String normalizedReference, String displayName, UUID tenantId) {
        var m = new Merchant();
        m.merchantId = merchantId;
        m.normalizedReference = normalizedReference;
        m.displayName = displayName;
        m.tenantId = tenantId;
        m.status = MerchantStatus.DRAFT;
        m.createdAt = Instant.now();
        m.updatedAt = m.createdAt;
        return m;
    }

    /**
     * Backward-compatible overload; tenantId will be set properly in Wave 2 MerchantService update.
     */
    public static Merchant create(UUID merchantId, String normalizedReference, String displayName) {
        return create(merchantId, normalizedReference, displayName, null);
    }

    public void activate() {
        if (!status.canTransitionTo(MerchantStatus.ACTIVE)) {
            throw new InvalidTransitionException(status, MerchantStatus.ACTIVE);
        }
        this.status = MerchantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        if (!status.canTransitionTo(MerchantStatus.SUSPENDED)) {
            throw new InvalidTransitionException(status, MerchantStatus.SUSPENDED);
        }
        this.status = MerchantStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public String getNormalizedReference() {
        return normalizedReference;
    }

    public String getDisplayName() {
        return displayName;
    }

    public MerchantStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
