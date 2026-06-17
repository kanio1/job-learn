package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.*;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.merchant.internal.web.DuplicateMerchantReferenceException;
import lab.paymentquality.merchant.internal.web.MerchantResponse;
import lab.paymentquality.merchant.internal.web.MerchantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);
    private static final int LIST_LIMIT = 50;

    private final JpaMerchantRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public MerchantService(JpaMerchantRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Merchant create(String ref, String displayName) {
        MerchantReference normalizedRef = MerchantReference.from(ref);
        DisplayName validatedName = DisplayName.from(displayName);
        String normalized = normalizedRef.normalized();

        if (repository.findByNormalizedReference(normalized).isPresent()) {
            log.warn("merchant.create.failed.duplicate normalizedReference={} correlationId={}",
                    normalized, MDC.get("correlationId"));
            throw new DuplicateMerchantReferenceException(normalized);
        }

        // Wave 1 bridge: look up placeholder tenant until Wave 2 provides TenantContext.
        // Wave 2 will replace this with the real tenant resolved from the JWT claim.
        UUID placeholderTenantId = jdbcTemplate.queryForObject(
                "SELECT tenant_id FROM tenants WHERE tenant_reference = 'PLACEHOLDER_TENANT_ID'",
                UUID.class);

        UUID id = UUID.randomUUID();
        Merchant merchant;
        try {
            merchant = Merchant.create(id, normalized, validatedName.value(), placeholderTenantId);
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

    @Transactional(readOnly = true)
    public MerchantResponse findById(UUID id) {
        Merchant merchant = repository.findById(id)
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

    public MerchantResponse activate(UUID id) {
        Merchant merchant = repository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException(id.toString()));
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
}
