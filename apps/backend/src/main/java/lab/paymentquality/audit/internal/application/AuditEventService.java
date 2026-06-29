package lab.paymentquality.audit.internal.application;

import jakarta.persistence.criteria.Predicate;
import lab.paymentquality.audit.internal.domain.AuditEvent;
import lab.paymentquality.audit.internal.domain.exception.AuditEventNotFoundException;
import lab.paymentquality.audit.internal.infrastructure.JpaAuditEventRepository;
import lab.paymentquality.audit.internal.web.dto.AuditEventDetail;
import lab.paymentquality.audit.internal.web.dto.AuditExportEvent;
import lab.paymentquality.audit.internal.web.dto.AuditExportResponse;
import lab.paymentquality.audit.internal.web.dto.AuditEventSummary;
import lab.paymentquality.audit.internal.web.dto.AuditListResponse;
import lab.paymentquality.audit.internal.web.dto.AuditQuery;
import lab.paymentquality.tenant.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuditEventService {

    private final JpaAuditEventRepository repository;

    public AuditEventService(JpaAuditEventRepository repository) {
        this.repository = repository;
    }

    public AuditListResponse list(AuditQuery query, TenantContext tenantContext) {
        Page<AuditEvent> result = findPage(query, tenantContext);
        List<AuditEventSummary> content = result.getContent().stream()
                .map(AuditEventSummary::from)
                .toList();
        return new AuditListResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    public AuditExportResponse export(AuditQuery query, TenantContext tenantContext) {
        Page<AuditEvent> result = findPage(query, tenantContext);
        List<AuditExportEvent> content = result.getContent().stream()
                .map(AuditExportEvent::from)
                .toList();
        return new AuditExportResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private Page<AuditEvent> findPage(AuditQuery query, TenantContext tenantContext) {
        Sort sort = Sort.by(Sort.Direction.DESC, "occurredAt");
        PageRequest pageRequest = PageRequest.of(query.page(), query.size(), sort);
        return repository.findAll(specification(query, tenantContext), pageRequest);
    }

    public AuditEventDetail get(String id, TenantContext tenantContext) {
        AuditEvent event = repository.findById(parseId(id))
                .orElseThrow(AuditEventNotFoundException::new);
        if (tenantContext.isTenantScoped()
                && !event.getTenantId().equals(tenantContext.tenantReference().value())) {
            throw new AuditEventNotFoundException();
        }
        return AuditEventDetail.from(event);
    }

    private Specification<AuditEvent> specification(AuditQuery query, TenantContext tenantContext) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (tenantContext.isTenantScoped()) {
                predicates.add(criteriaBuilder.equal(
                        root.get("tenantId"), tenantContext.tenantReference().value()));
            }
            if (query.actor() != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(root.get("actorSubject"), query.actor()),
                        criteriaBuilder.equal(root.get("actorDisplay"), query.actor())));
            }
            if (query.action() != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), query.action()));
            }
            if (query.targetType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetType"), query.targetType()));
            }
            if (query.from() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("occurredAt"), query.from().atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
            if (query.to() != null) {
                predicates.add(criteriaBuilder.lessThan(
                        root.get("occurredAt"), query.to().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            throw new AuditEventNotFoundException();
        }
    }
}
