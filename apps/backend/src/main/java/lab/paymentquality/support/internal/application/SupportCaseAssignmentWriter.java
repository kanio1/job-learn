package lab.paymentquality.support.internal.application;

import lab.paymentquality.shared.events.AuditableActionEventFactory;
import lab.paymentquality.support.SupportCaseStatus;
import lab.paymentquality.support.internal.domain.SupportCase;
import lab.paymentquality.support.internal.domain.SupportCaseAlreadyResolvedException;
import lab.paymentquality.support.internal.domain.SupportCaseNotFoundException;
import lab.paymentquality.support.internal.infrastructure.JpaSupportCaseRepository;
import lab.paymentquality.tenant.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class SupportCaseAssignmentWriter {

    private final JpaSupportCaseRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public SupportCaseAssignmentWriter(
            JpaSupportCaseRepository repository,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assign(UUID caseId, String assigneeSubject, TenantContext tenantContext) {
        SupportCase supportCase = repository.lockById(caseId)
                .orElseThrow(() -> new SupportCaseNotFoundException(caseId.toString()));
        if (tenantContext.isTenantScoped() && !supportCase.getTenantId().equals(tenantContext.tenantId())) {
            throw new SupportCaseNotFoundException(caseId.toString());
        }
        if (supportCase.getStatus() == SupportCaseStatus.RESOLVED) {
            throw new SupportCaseAlreadyResolvedException(supportCase.getCaseId(), supportCase.getCaseReference());
        }
        if (Objects.equals(supportCase.getAssigneeSubject(), assigneeSubject)) {
            return;
        }
        try {
            supportCase.assign(assigneeSubject);
            repository.saveAndFlush(supportCase);
        } catch (OptimisticLockingFailureException e) {
            throw e;
        }
        eventPublisher.publishEvent(AuditableActionEventFactory.success(
                "SUPPORT_CASE_ASSIGNED",
                "SUPPORT_CASE",
                supportCase.getCaseId().toString(),
                supportCase.getTenantId().toString(),
                null,
                null,
                null,
                java.util.Map.of(
                        "merchantId", supportCase.getMerchantId().toString(),
                        "assigneeSubject", assigneeSubject,
                        "caseReference", supportCase.getCaseReference())));
    }
}
