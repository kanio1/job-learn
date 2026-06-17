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

    protected Tenant() {}

    public UUID getTenantId() { return tenantId; }
    public String getTenantReference() { return tenantReference; }
    public String getName() { return name; }
    public TenantStatus getStatus() { return status; }
    public TenantType getTenantType() { return tenantType; }
    public Instant getCreatedAt() { return createdAt; }
}
