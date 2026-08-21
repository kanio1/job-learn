package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.*;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.merchant.internal.infrastructure.MerchantSpecification;
import lab.paymentquality.merchant.internal.web.DuplicateMerchantReferenceException;
import lab.paymentquality.merchant.internal.web.MerchantListRequest;
import lab.paymentquality.merchant.internal.web.MerchantListResponse;
import lab.paymentquality.merchant.internal.web.MerchantMapper;
import lab.paymentquality.merchant.internal.web.MerchantResponse;
import lab.paymentquality.shared.events.AuditableActionEventFactory;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);
    private static final int LIST_LIMIT = 50;
    private static final TenantReference LEGACY_DEFAULT_TENANT_REFERENCE =
            TenantReference.of("PLACEHOLDER_TENANT_ID");

    private final JpaMerchantRepository repository;
    private final TenantResolver tenantResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public MerchantService(
            JpaMerchantRepository repository,
            TenantResolver tenantResolver,
            ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.tenantResolver = tenantResolver;
        this.eventPublisher = eventPublisher;
    }

    public MerchantService(JpaMerchantRepository repository, TenantResolver tenantResolver) {
        this(repository, tenantResolver, event -> { });
    }

    public Merchant create(String ref, String displayName) {
        UUID defaultTenantId = tenantResolver.resolveTenantId(LEGACY_DEFAULT_TENANT_REFERENCE);
        TenantContext legacyContext = new TenantContext(
                defaultTenantId,
                LEGACY_DEFAULT_TENANT_REFERENCE,
                false);
        return create(ref, displayName, legacyContext, null);
    }

    public Merchant create(String ref, String displayName, TenantContext tenantContext, String requestedTenantRef) {
        MerchantReference normalizedRef = MerchantReference.from(ref);
        DisplayName validatedName = DisplayName.from(displayName);
        String normalized = normalizedRef.normalized();
        UUID assignedTenantId = resolveAssignedTenantId(tenantContext, requestedTenantRef);

        if (repository.findByNormalizedReference(normalized).isPresent()) {
            log.warn("merchant.create.failed.duplicate normalizedReference={} correlationId={}",
                    normalized, MDC.get("correlationId"));
            throw new DuplicateMerchantReferenceException(normalized);
        }

        UUID id = UUID.randomUUID();
        Merchant merchant;
        try {
            merchant = Merchant.create(id, normalized, validatedName.value(), assignedTenantId);
            repository.saveAndFlush(merchant);
        } catch (DataIntegrityViolationException e) {
            log.warn("merchant.create.failed.duplicate normalizedReference={} correlationId={}",
                    normalized, MDC.get("correlationId"));
            throw new DuplicateMerchantReferenceException(normalized);
        }

        log.info("merchant.create.succeeded merchantId={} normalizedReference={} status={} correlationId={}",
                id, normalized, merchant.getStatus(), MDC.get("correlationId"));
        String auditTenantReference = tenantContext.isTenantScoped()
                ? tenantContext.tenantReference().value()
                : requestedTenantRef;
        publishSuccess("MERCHANT_CREATED", id, auditTenantReference);
        return merchant;
    }

    private UUID resolveAssignedTenantId(TenantContext tenantContext, String requestedTenantRef) {
        Objects.requireNonNull(tenantContext, "tenantContext must not be null");

        if (tenantContext.isTenantScoped()) {
            return tenantContext.tenantId();
        }

        if (requestedTenantRef == null || requestedTenantRef.isBlank()) {
            throw new MissingTenantReferenceException();
        }

        TenantReference tenantReference = TenantReference.of(requestedTenantRef);
        try {
            return tenantResolver.resolveTenantId(tenantReference);
        } catch (TenantResolutionException e) {
            throw new UnresolvableTenantReferenceException(tenantReference.value());
        }
    }

    @Transactional(readOnly = true)
    public MerchantResponse findById(UUID id) {
        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponse findById(UUID id, TenantContext tenantContext) {
        Objects.requireNonNull(tenantContext, "tenantContext must not be null");

        Merchant merchant = tenantContext.isTenantScoped()
                ? repository.findByMerchantIdAndTenantId(id, tenantContext.tenantId())
                        .orElseThrow(() -> new MerchantNotFoundException(id.toString()))
                : repository.findById(id)
                        .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        return MerchantMapper.toResponse(merchant);
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> listFirstPage() {
        PageRequest page = PageRequest.of(0, LIST_LIMIT);
        return repository.findAllByOrderByCreatedAtDescMerchantIdAsc(page).stream()
                .map(MerchantMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> listFirstPage(TenantContext tenantContext, UUID filterTenantId) {
        return list(tenantContext, MerchantListRequest.defaults(), filterTenantId).content();
    }

    @Transactional(readOnly = true)
    public MerchantListResponse list(
            TenantContext tenantContext,
            MerchantListRequest request,
            UUID filterTenantId) {
        Objects.requireNonNull(tenantContext, "tenantContext must not be null");
        Objects.requireNonNull(request, "request must not be null");

        int page = request.effectivePage();
        int size = request.effectiveSize();
        Sort sort = parseSort(request.effectiveSort());

        Specification<Merchant> spec = (root, query, cb) -> cb.conjunction();
        if (tenantContext.isTenantScoped()) {
            spec = spec.and(MerchantSpecification.hasTenantId(tenantContext.tenantId()));
        } else if (filterTenantId != null) {
            spec = spec.and(MerchantSpecification.hasTenantId(filterTenantId));
        }
        spec = addIfNotNull(spec, MerchantSpecification.hasStatus(request.status()));
        spec = addIfNotNull(spec, MerchantSpecification.riskFlagged(request.riskFlagged()));
        spec = addIfNotNull(spec, MerchantSpecification.matchesQuery(request.q()));

        Page<Merchant> result = repository.findAll(spec, PageRequest.of(page, size, sort));
        return MerchantMapper.toListResponse(result);
    }

    @Transactional(readOnly = true)
    public List<MerchantResponse> listByTenantId(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return repository.findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(tenantId, Pageable.unpaged())
                .stream()
                .map(MerchantMapper::toResponse)
                .toList();
    }

    private static Sort parseSort(String sortParam) {
        String[] sortParts = sortParam.split(",");
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, sortParts[0]).and(Sort.by(Sort.Direction.ASC, "merchantId"));
    }

    private static Specification<Merchant> addIfNotNull(
            Specification<Merchant> base,
            Specification<Merchant> additional) {
        return additional != null ? base.and(additional) : base;
    }

    public MerchantResponse activate(UUID id) {
        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        MerchantStatus statusBefore = merchant.getStatus();
        merchant.activate();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.activate.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        publishStatusChange("MERCHANT_ACTIVATED", id, null, statusBefore, merchant.getStatus());
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse activate(UUID id, TenantContext tenantContext) {
        return activate(id, tenantContext, null);
    }

    public MerchantResponse activate(UUID id, TenantContext tenantContext, Long expectedVersion) {
        Merchant merchant = findMerchantEnforcingTenantBoundary(id, tenantContext);
        requireCurrentVersion(merchant, expectedVersion);
        MerchantStatus statusBefore = merchant.getStatus();
        merchant.activate();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.activate.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        publishStatusChange("MERCHANT_ACTIVATED", id, tenantContext.tenantReference().value(),
                statusBefore, merchant.getStatus());
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse suspend(UUID id) {
        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        MerchantStatus statusBefore = merchant.getStatus();
        merchant.suspend();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.suspend.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        publishStatusChange("MERCHANT_SUSPENDED", id, null, statusBefore, merchant.getStatus());
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse suspend(UUID id, TenantContext tenantContext) {
        return suspend(id, tenantContext, null);
    }

    public MerchantResponse suspend(UUID id, TenantContext tenantContext, Long expectedVersion) {
        Merchant merchant = findMerchantEnforcingTenantBoundary(id, tenantContext);
        requireCurrentVersion(merchant, expectedVersion);
        MerchantStatus statusBefore = merchant.getStatus();
        merchant.suspend();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.suspend.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        publishStatusChange("MERCHANT_SUSPENDED", id, tenantContext.tenantReference().value(),
                statusBefore, merchant.getStatus());
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse updateRiskFlag(UUID id, boolean riskFlagged) {
        return updateRiskFlag(id, riskFlagged, null);
    }

    public MerchantResponse updateRiskFlag(UUID id, boolean riskFlagged, Long expectedVersion) {
        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        requireCurrentVersion(merchant, expectedVersion);
        merchant.updateRiskFlag(riskFlagged);
        repository.saveAndFlush(merchant);
        String action = riskFlagged ? "MERCHANT_RISK_FLAGGED" : "MERCHANT_RISK_FLAG_CLEARED";
        log.info("merchant.risk.flag.updated merchantId={} riskFlagged={} correlationId={}",
                id, riskFlagged, MDC.get("correlationId"));
        publishSuccess(action, id, null);
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse rename(UUID id, String displayName, TenantContext tenantContext, Long expectedVersion) {
        Merchant merchant = findMerchantEnforcingTenantBoundary(id, tenantContext);
        requireCurrentVersion(merchant, expectedVersion);
        DisplayName validated = DisplayName.from(displayName);
        merchant.rename(validated.value());
        repository.saveAndFlush(merchant);
        log.info("merchant.display-name.updated merchantId={} correlationId={}", id, MDC.get("correlationId"));
        publishSuccess("MERCHANT_RENAMED", id, tenantContext.tenantReference().value());
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse patch(
            UUID id,
            lab.paymentquality.merchant.internal.web.UpdateMerchantRequest request,
            TenantContext tenantContext,
            Long expectedVersion) {
        Merchant merchant = findMerchantEnforcingTenantBoundary(id, tenantContext);
        requireCurrentVersion(merchant, expectedVersion);
        if (request.displayNameSpecified()) {
            DisplayName validated = DisplayName.from(request.displayName());
            merchant.rename(validated.value());
        }
        if (request.contactPhoneSpecified()) {
            requireContactBound("contactPhone", request.contactPhone(), 32);
        }
        if (request.contactAddressSpecified()) {
            requireContactBound("contactAddress", request.contactAddress(), 200);
        }
        merchant.updateContact(
                request.contactPhone(), request.contactPhoneSpecified(),
                request.contactAddress(), request.contactAddressSpecified());
        repository.saveAndFlush(merchant);
        log.info("merchant.contact.updated merchantId={} correlationId={}", id, MDC.get("correlationId"));
        publishSuccess("MERCHANT_UPDATED", id, tenantContext.tenantReference().value());
        return MerchantMapper.toResponse(merchant);
    }

    private static void requireContactBound(String field, String value, int maxLength) {
        if (value == null) {
            return;
        }
        if (value.length() > maxLength) {
            throw new InvalidMerchantContactException(field, field + " must be at most " + maxLength + " characters");
        }
    }

    private static void requireCurrentVersion(Merchant merchant, Long expectedVersion) {
        if (expectedVersion == null) {
            return;
        }
        Long current = merchant.getVersion();
        if (current == null || current != expectedVersion) {
            throw new MerchantVersionMismatchException();
        }
    }

    private void publishSuccess(String action, UUID merchantId, String tenantReference) {
        eventPublisher.publishEvent(AuditableActionEventFactory.success(
                action,
                "MERCHANT",
                merchantId.toString(),
                tenantReference));
    }

    /** Status-transition variant — feeds the audit diff drawer (F-D7). */
    private void publishStatusChange(
            String action,
            UUID merchantId,
            String tenantReference,
            MerchantStatus statusBefore,
            MerchantStatus statusAfter) {
        eventPublisher.publishEvent(AuditableActionEventFactory.success(
                action,
                "MERCHANT",
                merchantId.toString(),
                tenantReference,
                null,
                null,
                Map.of("status", statusBefore.name()),
                Map.of("status", statusAfter.name())));
    }

    private Merchant findMerchantEnforcingTenantBoundary(UUID id, TenantContext tenantContext) {
        Objects.requireNonNull(tenantContext, "tenantContext must not be null");

        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        if (tenantContext.isTenantScoped() && !merchant.getTenantId().equals(tenantContext.tenantId())) {
            throw new TenantBoundaryViolationException();
        }
        return merchant;
    }
}
