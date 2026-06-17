package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.*;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.merchant.internal.web.DuplicateMerchantReferenceException;
import lab.paymentquality.merchant.internal.web.MerchantMapper;
import lab.paymentquality.merchant.internal.web.MerchantResponse;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public MerchantService(JpaMerchantRepository repository, TenantResolver tenantResolver) {
        this.repository = repository;
        this.tenantResolver = tenantResolver;
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
        Objects.requireNonNull(tenantContext, "tenantContext must not be null");

        PageRequest page = PageRequest.of(0, LIST_LIMIT);
        if (tenantContext.isTenantScoped()) {
            return repository.findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(tenantContext.tenantId(), page)
                    .stream()
                    .map(MerchantMapper::toResponse)
                    .toList();
        }

        if (filterTenantId != null) {
            return repository.findAllByTenantIdOrderByCreatedAtDescMerchantIdAsc(filterTenantId, page)
                    .stream()
                    .map(MerchantMapper::toResponse)
                    .toList();
        }

        return repository.findAllByOrderByCreatedAtDescMerchantIdAsc(page).stream()
                .map(MerchantMapper::toResponse)
                .toList();
    }

    public MerchantResponse activate(UUID id) {
        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        merchant.activate();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.activate.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse activate(UUID id, TenantContext tenantContext) {
        Merchant merchant = findMerchantEnforcingTenantBoundary(id, tenantContext);
        merchant.activate();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.activate.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse suspend(UUID id) {
        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
        merchant.suspend();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.suspend.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        return MerchantMapper.toResponse(merchant);
    }

    public MerchantResponse suspend(UUID id, TenantContext tenantContext) {
        Merchant merchant = findMerchantEnforcingTenantBoundary(id, tenantContext);
        merchant.suspend();
        repository.saveAndFlush(merchant);
        log.info("merchant.status.suspend.succeeded merchantId={} status={} correlationId={}",
                id, merchant.getStatus(), MDC.get("correlationId"));
        return MerchantMapper.toResponse(merchant);
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
