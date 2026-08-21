package lab.paymentquality.support.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lab.paymentquality.support.SupportCasePriority;
import lab.paymentquality.support.SupportCaseStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "support_cases")
public class SupportCase {

    @Id
    @Column(name = "case_id", nullable = false, updatable = false)
    private UUID caseId;

    @Column(name = "case_reference", length = 32, nullable = false, unique = true)
    private String caseReference;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Column(name = "payment_order_id")
    private UUID paymentOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SupportCaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10, nullable = false)
    private SupportCasePriority priority;

    @Column(name = "assignee_subject", length = 255)
    private String assigneeSubject;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SupportCase() {
    }

    public static SupportCase create(
            UUID caseId,
            String caseReference,
            UUID tenantId,
            UUID merchantId,
            UUID paymentOrderId,
            String title,
            SupportCasePriority priority,
            String assigneeSubject) {
        var supportCase = new SupportCase();
        supportCase.caseId = caseId;
        supportCase.caseReference = caseReference;
        supportCase.tenantId = tenantId;
        supportCase.merchantId = merchantId;
        supportCase.paymentOrderId = paymentOrderId;
        supportCase.title = title;
        supportCase.priority = priority == null ? SupportCasePriority.NORMAL : priority;
        supportCase.assigneeSubject = assigneeSubject;
        supportCase.status = SupportCaseStatus.NEW;
        Instant now = Instant.now();
        supportCase.createdAt = now;
        supportCase.updatedAt = now;
        return supportCase;
    }

    public void moveTo(SupportCaseStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalSupportTransitionException(status, target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public void assign(String subject) {
        this.assigneeSubject = subject;
        this.updatedAt = Instant.now();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public UUID getPaymentOrderId() {
        return paymentOrderId;
    }

    public SupportCaseStatus getStatus() {
        return status;
    }

    public SupportCasePriority getPriority() {
        return priority;
    }

    public String getAssigneeSubject() {
        return assigneeSubject;
    }

    public String getTitle() {
        return title;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
