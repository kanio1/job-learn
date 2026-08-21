package lab.paymentquality.merchant.internal.application;

import lab.paymentquality.merchant.internal.domain.DisplayName;
import lab.paymentquality.merchant.internal.domain.InvalidDisplayNameException;
import lab.paymentquality.merchant.internal.domain.InvalidMerchantReferenceException;
import lab.paymentquality.merchant.internal.domain.MerchantImportAlreadyCommittedException;
import lab.paymentquality.merchant.internal.domain.MerchantImportMalformedException;
import lab.paymentquality.merchant.internal.domain.MerchantImportPreviewNotFoundException;
import lab.paymentquality.merchant.internal.domain.MerchantReference;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantImportPreviewRepository;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.merchant.internal.infrastructure.MerchantImportAcceptedRow;
import lab.paymentquality.merchant.internal.infrastructure.MerchantImportPreview;
import lab.paymentquality.merchant.internal.web.MerchantImportCommitResponse;
import lab.paymentquality.merchant.internal.web.MerchantImportPreviewResponse;
import lab.paymentquality.merchant.internal.web.MerchantImportRowResult;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MerchantImportService {

    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private static final String VALID = "VALID";
    private static final String WARNING = "WARNING";
    private static final String REJECTED = "REJECTED";

    private final JpaMerchantRepository merchantRepository;
    private final JpaMerchantImportPreviewRepository previewRepository;
    private final MerchantService merchantService;
    private final TenantResolver tenantResolver;

    public MerchantImportService(
            JpaMerchantRepository merchantRepository,
            JpaMerchantImportPreviewRepository previewRepository,
            MerchantService merchantService,
            TenantResolver tenantResolver) {
        this.merchantRepository = merchantRepository;
        this.previewRepository = previewRepository;
        this.merchantService = merchantService;
        this.tenantResolver = tenantResolver;
    }

    @Transactional
    public MerchantImportPreviewResponse preview(MultipartFile file, TenantContext tenantContext, String createdBy) {
        byte[] bytes = readBytes(file);
        List<List<String>> table = MerchantImportCsv.parse(bytes);
        if (table.isEmpty()) {
            throw new MerchantImportMalformedException("CSV header is missing");
        }
        List<String> header = table.getFirst().stream().map(value -> value.trim()).toList();
        boolean platform = tenantContext.isPlatformScoped();
        List<String> expected = platform
                ? List.of("merchantReference", "displayName", "tenantReference")
                : List.of("merchantReference", "displayName");
        if (!header.equals(expected)) {
            throw new MerchantImportMalformedException(
                    "CSV header must be " + String.join(",", expected));
        }
        if (table.size() == 1) {
            throw new MerchantImportMalformedException("CSV has no data rows");
        }

        List<MerchantImportRowResult> rows = new ArrayList<>();
        List<MerchantImportAcceptedRow> accepted = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int validCount = 0;
        int warningCount = 0;
        int rejectedCount = 0;

        for (int i = 1; i < table.size(); i++) {
            int line = i + 1;
            List<String> cells = table.get(i);
            while (cells.size() < expected.size()) {
                cells = new ArrayList<>(cells);
                cells.add("");
            }
            String rawReference = cells.get(0);
            String rawName = cells.get(1);
            String rawTenant = platform ? cells.get(2) : tenantContext.tenantReference().value();
            boolean trimmedName = rawName != null && !rawName.equals(rawName.trim());
            MerchantImportRowResult classified = classifyRow(
                    line, rawReference, rawName, rawTenant, tenantContext, seen);
            rows.add(classified);
            if (REJECTED.equals(classified.status())) {
                rejectedCount++;
                continue;
            }
            seen.add(MerchantReference.from(rawReference).normalized());
            accepted.add(new MerchantImportAcceptedRow(
                    classified.merchantReference(),
                    classified.displayName(),
                    classified.tenantReference()));
            if (WARNING.equals(classified.status()) || trimmedName) {
                warningCount++;
            } else {
                validCount++;
            }
        }

        Instant now = Instant.now();
        UUID previewId = UUID.randomUUID();
        previewRepository.saveAndFlush(MerchantImportPreview.create(
                previewId,
                checksum(bytes),
                createdBy == null || createdBy.isBlank() ? "unknown" : createdBy,
                tenantContext.tenantId(),
                platform,
                accepted,
                now.plus(PREVIEW_TTL),
                now));
        return new MerchantImportPreviewResponse(previewId, validCount, warningCount, rejectedCount, rows);
    }

    @Transactional
    public MerchantImportCommitResponse commit(UUID previewId, TenantContext tenantContext) {
        MerchantImportPreview preview = previewRepository.findById(previewId)
                .orElseThrow(MerchantImportPreviewNotFoundException::new);
        if (!preview.getTenantId().equals(tenantContext.tenantId())
                || preview.isPlatformScoped() != tenantContext.isPlatformScoped()) {
            throw new MerchantImportPreviewNotFoundException();
        }
        if (preview.getCommittedAt() != null) {
            throw new MerchantImportAlreadyCommittedException();
        }
        if (preview.getExpiresAt().isBefore(Instant.now())) {
            throw new MerchantImportPreviewNotFoundException();
        }
        for (MerchantImportAcceptedRow row : preview.getPayload()) {
            merchantService.create(
                    row.merchantReference(),
                    row.displayName(),
                    tenantContext,
                    tenantContext.isPlatformScoped() ? row.tenantReference() : null);
        }
        preview.markCommitted(Instant.now());
        previewRepository.saveAndFlush(preview);
        return new MerchantImportCommitResponse(preview.getPayload().size());
    }

    private MerchantImportRowResult classifyRow(
            int line,
            String rawReference,
            String rawName,
            String rawTenant,
            TenantContext tenantContext,
            Set<String> seen) {
        String status = VALID;
        String reason = null;
        String reference;
        String displayName;
        String tenantReference = rawTenant == null ? "" : rawTenant.trim();
        try {
            reference = MerchantReference.from(rawReference).normalized();
        } catch (InvalidMerchantReferenceException ex) {
            return new MerchantImportRowResult(line, REJECTED, rawReference, rawName, tenantReference,
                    "invalid_merchant_reference");
        }
        try {
            displayName = DisplayName.from(rawName).value();
        } catch (InvalidDisplayNameException ex) {
            return new MerchantImportRowResult(line, REJECTED, reference, rawName, tenantReference,
                    "invalid_display_name");
        }
        if (seen.contains(reference)) {
            return new MerchantImportRowResult(line, REJECTED, reference, displayName, tenantReference,
                    "duplicate_in_file");
        }
        if (merchantRepository.findByNormalizedReference(reference).isPresent()) {
            return new MerchantImportRowResult(line, REJECTED, reference, displayName, tenantReference,
                    "duplicate_merchant_reference");
        }
        if (tenantContext.isPlatformScoped()) {
            if (tenantReference.isBlank()) {
                return new MerchantImportRowResult(line, REJECTED, reference, displayName, tenantReference,
                        "missing_tenant_reference");
            }
            try {
                tenantResolver.resolveTenantId(TenantReference.of(tenantReference));
            } catch (TenantResolutionException | IllegalArgumentException ex) {
                return new MerchantImportRowResult(line, REJECTED, reference, displayName, tenantReference,
                        "unknown_tenant_reference");
            }
        } else {
            tenantReference = tenantContext.tenantReference().value();
        }
        if (rawName != null && !rawName.equals(rawName.trim())) {
            status = WARNING;
            reason = "display_name_trimmed";
        }
        return new MerchantImportRowResult(line, status, reference, displayName, tenantReference, reason);
    }

    private static byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MerchantImportMalformedException("CSV file is empty");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new MerchantImportMalformedException("CSV file could not be read");
        }
    }

    private static String checksum(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required", ex);
        }
    }
}
