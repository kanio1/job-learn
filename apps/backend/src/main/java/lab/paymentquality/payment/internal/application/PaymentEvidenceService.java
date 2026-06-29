package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentEvidenceValidationException;
import lab.paymentquality.payment.internal.domain.PaymentOrderEvidence;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderEvidenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PaymentEvidenceService {

    public static final long MAX_EVIDENCE_SIZE_BYTES = 2L * 1024L * 1024L;
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "text/csv"
    );

    private final JpaPaymentOrderEvidenceRepository evidenceRepository;
    private final PaymentOrderService paymentOrderService;

    public PaymentEvidenceService(JpaPaymentOrderEvidenceRepository evidenceRepository,
                                  PaymentOrderService paymentOrderService) {
        this.evidenceRepository = evidenceRepository;
        this.paymentOrderService = paymentOrderService;
    }

    public PaymentOrderEvidence uploadForOrder(UUID merchantId, UUID paymentOrderId, MultipartFile file) {
        paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        String filename = validateFilename(file);
        String contentType = validateContentType(file);
        long sizeBytes = validateSize(file);

        PaymentOrderEvidence evidence = PaymentOrderEvidence.create(
                UUID.randomUUID(), paymentOrderId, filename, contentType, sizeBytes, Instant.now());
        return evidenceRepository.saveAndFlush(evidence);
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderEvidence> listForOrder(UUID merchantId, UUID paymentOrderId) {
        paymentOrderService.findForPlatform(merchantId, paymentOrderId);
        return evidenceRepository.findByPaymentOrderIdOrderByUploadedAtDescEvidenceIdAsc(paymentOrderId);
    }

    private String validateFilename(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PaymentEvidenceValidationException(HttpStatus.BAD_REQUEST, "empty_evidence_file",
                    "Evidence file must not be empty");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            throw new PaymentEvidenceValidationException(HttpStatus.BAD_REQUEST, "missing_evidence_filename",
                    "Evidence filename is required");
        }
        String trimmed = original.trim();
        if (trimmed.length() > 255) {
            throw new PaymentEvidenceValidationException(HttpStatus.BAD_REQUEST, "invalid_evidence_filename",
                    "Evidence filename must not exceed 255 characters");
        }
        if (trimmed.contains("../") || trimmed.contains("..\\")
                || trimmed.contains("/") || trimmed.contains("\\")
                || trimmed.contains(":")
                || trimmed.startsWith(".") || trimmed.startsWith("~")) {
            throw new PaymentEvidenceValidationException(HttpStatus.BAD_REQUEST, "invalid_evidence_filename",
                    "Evidence filename must not contain path segments");
        }
        return trimmed;
    }

    private String validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new PaymentEvidenceValidationException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "unsupported_evidence_content_type", "Evidence content type is required");
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new PaymentEvidenceValidationException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "unsupported_evidence_content_type", "Evidence content type is not supported");
        }
        return normalized;
    }

    private long validateSize(MultipartFile file) {
        long size = file.getSize();
        if (size <= 0) {
            throw new PaymentEvidenceValidationException(HttpStatus.BAD_REQUEST, "empty_evidence_file",
                    "Evidence file must not be empty");
        }
        if (size > MAX_EVIDENCE_SIZE_BYTES) {
            throw new PaymentEvidenceValidationException(HttpStatus.PAYLOAD_TOO_LARGE, "evidence_file_too_large",
                    "Evidence file must not exceed 2 MB");
        }
        return size;
    }
}
