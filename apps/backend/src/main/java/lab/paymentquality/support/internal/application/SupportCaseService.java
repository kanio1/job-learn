package lab.paymentquality.support.internal.application;

import lab.paymentquality.merchant.MerchantOwnership;
import lab.paymentquality.merchant.MerchantOwnershipLocator;
import lab.paymentquality.shared.events.AuditableActionEventFactory;
import lab.paymentquality.support.SupportCasePriority;
import lab.paymentquality.support.SupportCaseSeed;
import lab.paymentquality.support.SupportCaseSeedCapability;
import lab.paymentquality.support.SupportCaseStatus;
import lab.paymentquality.support.internal.domain.SupportCase;
import lab.paymentquality.support.internal.domain.SupportCaseAlreadyResolvedException;
import lab.paymentquality.support.internal.domain.SupportCaseNotFoundException;
import lab.paymentquality.support.internal.domain.SupportCaseVersionMismatchException;
import lab.paymentquality.support.internal.domain.SupportMerchantNotFoundException;
import lab.paymentquality.support.internal.infrastructure.JpaSupportCaseRepository;
import lab.paymentquality.support.internal.web.BulkAssignFailure;
import lab.paymentquality.support.internal.web.BulkAssignResponse;
import lab.paymentquality.support.internal.web.CreateSupportCaseRequest;
import lab.paymentquality.support.internal.web.DuplicateSupportCaseReferenceException;
import lab.paymentquality.support.internal.web.InvalidSupportCaseRequestException;
import lab.paymentquality.support.internal.web.SupportCaseMapper;
import lab.paymentquality.support.internal.web.SupportCaseResponse;
import lab.paymentquality.support.internal.web.UpdateSupportCaseRequest;
import lab.paymentquality.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class SupportCaseService implements SupportCaseSeedCapability {

    private static final Logger log = LoggerFactory.getLogger(SupportCaseService.class);

    private final JpaSupportCaseRepository repository;
    private final MerchantOwnershipLocator merchantOwnershipLocator;
    private final ApplicationEventPublisher eventPublisher;
    private final SupportCaseAssignmentWriter assignmentWriter;

    public SupportCaseService(
            JpaSupportCaseRepository repository,
            MerchantOwnershipLocator merchantOwnershipLocator,
            ApplicationEventPublisher eventPublisher,
            SupportCaseAssignmentWriter assignmentWriter) {
        this.repository = repository;
        this.merchantOwnershipLocator = merchantOwnershipLocator;
        this.eventPublisher = eventPublisher;
        this.assignmentWriter = assignmentWriter;
    }

    public SupportCaseResponse create(CreateSupportCaseRequest request, TenantContext tenantContext) {
        MerchantOwnership merchant = merchantOwnershipLocator.find(request.merchantId())
                .orElseThrow(() -> new SupportMerchantNotFoundException(String.valueOf(request.merchantId())));
        if (tenantContext.isTenantScoped() && !merchant.tenantId().equals(tenantContext.tenantId())) {
            throw new SupportCaseNotFoundException(String.valueOf(request.merchantId()));
        }

        String reference = resolveReference(request.caseReference());
        if (repository.findByCaseReference(reference).isPresent()) {
            throw new DuplicateSupportCaseReferenceException(reference);
        }

        SupportCasePriority priority = parsePriority(request.priority());
        SupportCase supportCase = SupportCase.create(
                UUID.randomUUID(),
                reference,
                merchant.tenantId(),
                merchant.merchantId(),
                request.paymentOrderId(),
                request.title().strip(),
                priority,
                blankToNull(request.assigneeSubject()));
        try {
            repository.saveAndFlush(supportCase);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateSupportCaseReferenceException(reference);
        }

        publish("SUPPORT_CASE_CREATED", supportCase);
        log.info("support.case.create.succeeded caseId={} reference={} correlationId={}",
                supportCase.getCaseId(), supportCase.getCaseReference(), MDC.get("correlationId"));
        return SupportCaseMapper.toResponse(supportCase);
    }

    @Transactional(readOnly = true)
    public SupportCaseResponse getById(UUID caseId, TenantContext tenantContext) {
        return SupportCaseMapper.toResponse(requireVisible(caseId, tenantContext));
    }

    @Transactional(readOnly = true)
    public List<SupportCase> list(TenantContext tenantContext, String status, String assignee) {
        SupportCaseStatus parsedStatus = status == null || status.isBlank() ? null : parseStatus(status);
        String assigneeSubject = blankToNull(assignee);
        if (tenantContext.isTenantScoped()) {
            UUID tenantId = tenantContext.tenantId();
            if (parsedStatus != null) {
                return repository.findByTenantIdAndStatusOrderByUpdatedAtDescCaseIdAsc(tenantId, parsedStatus);
            }
            if (assigneeSubject != null) {
                return repository.findByTenantIdAndAssigneeSubjectOrderByUpdatedAtDescCaseIdAsc(
                        tenantId, assigneeSubject);
            }
            return repository.findByTenantIdOrderByUpdatedAtDescCaseIdAsc(tenantId);
        }
        if (parsedStatus != null) {
            return repository.findByStatusOrderByUpdatedAtDescCaseIdAsc(parsedStatus);
        }
        if (assigneeSubject != null) {
            return repository.findByAssigneeSubjectOrderByUpdatedAtDescCaseIdAsc(assigneeSubject);
        }
        return repository.findAllByOrderByUpdatedAtDescCaseIdAsc();
    }

    public SupportCaseResponse update(
            UUID caseId,
            UpdateSupportCaseRequest request,
            TenantContext tenantContext,
            long expectedVersion) {
        SupportCase supportCase = requireVisible(caseId, tenantContext);
        if (!Objects.equals(supportCase.getVersion(), expectedVersion)) {
            throw new SupportCaseVersionMismatchException();
        }
        boolean changed = false;
        if (request.status() != null && !request.status().isBlank()) {
            supportCase.moveTo(parseStatus(request.status()));
            changed = true;
            publish("SUPPORT_CASE_MOVED", supportCase);
        }
        if (request.assigneeSubject() != null) {
            supportCase.assign(blankToNull(request.assigneeSubject()));
            changed = true;
            publish("SUPPORT_CASE_ASSIGNED", supportCase);
        }
        if (!changed) {
            throw new InvalidSupportCaseRequestException("status or assigneeSubject is required");
        }
        try {
            repository.saveAndFlush(supportCase);
        } catch (OptimisticLockingFailureException e) {
            throw new SupportCaseVersionMismatchException();
        }
        return SupportCaseMapper.toResponse(supportCase);
    }

    @Override
    public UUID seedCase(SupportCaseSeed seed) {
        SupportCase supportCase = SupportCase.create(
                UUID.randomUUID(),
                generateReference(),
                seed.tenantId(),
                seed.merchantId(),
                seed.paymentOrderId(),
                seed.title(),
                seed.priority() == null ? SupportCasePriority.NORMAL : seed.priority(),
                seed.assigneeSubject());
        repository.saveAndFlush(supportCase);
        return supportCase.getCaseId();
    }

    public BulkAssignResponse bulkAssign(List<UUID> caseIds, String assigneeSubject, TenantContext tenantContext) {
        if (caseIds == null || caseIds.isEmpty()) {
            throw new InvalidSupportCaseRequestException("caseIds must not be empty");
        }
        if (caseIds.size() > 100) {
            throw new InvalidSupportCaseRequestException("caseIds must be at most 100");
        }
        String assignee = assigneeSubject == null ? "" : assigneeSubject.strip();
        if (assignee.isEmpty()) {
            throw new InvalidSupportCaseRequestException("assigneeSubject is required");
        }
        int succeeded = 0;
        List<BulkAssignFailure> failed = new ArrayList<>();
        for (UUID caseId : caseIds) {
            try {
                assignmentWriter.assign(caseId, assignee, tenantContext);
                succeeded++;
            } catch (SupportCaseAlreadyResolvedException e) {
                failed.add(new BulkAssignFailure(e.getCaseId(), e.getCaseReference(), "already_resolved"));
            } catch (SupportCaseNotFoundException e) {
                failed.add(new BulkAssignFailure(caseId, null, "not_found"));
            } catch (OptimisticLockingFailureException e) {
                String reference = repository.findById(caseId)
                        .map(SupportCase::getCaseReference)
                        .orElse(null);
                failed.add(new BulkAssignFailure(caseId, reference, "precondition_failed"));
            }
        }
        return new BulkAssignResponse(succeeded, failed);
    }

    SupportCase requireVisible(UUID caseId, TenantContext tenantContext) {
        if (tenantContext.isTenantScoped()) {
            return repository.findByCaseIdAndTenantId(caseId, tenantContext.tenantId())
                    .orElseThrow(() -> new SupportCaseNotFoundException(caseId.toString()));
        }
        return repository.findById(caseId)
                .orElseThrow(() -> new SupportCaseNotFoundException(caseId.toString()));
    }

    JpaSupportCaseRepository repository() {
        return repository;
    }

    private String resolveReference(String requested) {
        if (requested == null || requested.isBlank()) {
            return generateReference();
        }
        String stripped = requested.strip();
        if (stripped.length() > 32) {
            throw new InvalidSupportCaseRequestException("caseReference must be at most 32 characters");
        }
        return stripped;
    }

    private static String generateReference() {
        return "INC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private static SupportCasePriority parsePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return SupportCasePriority.NORMAL;
        }
        try {
            return SupportCasePriority.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidSupportCaseRequestException("priority must be LOW, NORMAL, or HIGH");
        }
    }

    private static SupportCaseStatus parseStatus(String raw) {
        try {
            return SupportCaseStatus.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidSupportCaseRequestException("status must be NEW, IN_PROGRESS, WAITING, or RESOLVED");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private void publish(String action, SupportCase supportCase) {
        eventPublisher.publishEvent(AuditableActionEventFactory.success(
                action,
                "SUPPORT_CASE",
                supportCase.getCaseId().toString(),
                supportCase.getTenantId().toString()));
    }
}
