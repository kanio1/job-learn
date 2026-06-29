package lab.paymentquality.tenant.internal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "tenant_reference", length = 64, nullable = false, unique = true)
    private String tenantReference;

    @Column(name = "name", length = 120, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TenantStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", length = 20, nullable = false)
    private TenantType tenantType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Column(name = "timezone", length = 64, nullable = false)
    private String timezone = "UTC";

    @Column(name = "webhook_base_url", length = 500)
    private String webhookBaseUrl;

    @Column(name = "settings_version", nullable = false)
    private long settingsVersion = 0;

    protected Tenant() {}

    public static Tenant seeded(UUID tenantId, String tenantReference, String name,
                                TenantType tenantType, TenantStatus status, Instant createdAt) {
        var tenant = new Tenant();
        tenant.tenantId = tenantId;
        tenant.tenantReference = tenantReference;
        tenant.name = name;
        tenant.tenantType = tenantType;
        tenant.status = status;
        tenant.createdAt = createdAt;
        return tenant;
    }

    public void applySeed(String tenantReference, String name, TenantType tenantType, TenantStatus status) {
        this.tenantReference = tenantReference;
        this.name = name;
        this.tenantType = tenantType;
        this.status = status;
    }

    public void updateSettings(String contactEmail, String timezone, String webhookBaseUrl) {
        this.contactEmail = contactEmail;
        this.timezone = timezone != null ? timezone : "UTC";
        this.webhookBaseUrl = webhookBaseUrl;
        this.settingsVersion++;
    }

    public UUID getTenantId() { return tenantId; }
    public String getTenantReference() { return tenantReference; }
    public String getName() { return name; }
    public TenantStatus getStatus() { return status; }
    public TenantType getTenantType() { return tenantType; }
    public Instant getCreatedAt() { return createdAt; }
    public String getContactEmail() { return contactEmail; }
    public String getTimezone() { return timezone; }
    public String getWebhookBaseUrl() { return webhookBaseUrl; }
    public long getSettingsVersion() { return settingsVersion; }
}
