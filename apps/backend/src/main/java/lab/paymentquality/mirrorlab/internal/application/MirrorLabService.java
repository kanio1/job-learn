package lab.paymentquality.mirrorlab.internal.application;

import lab.paymentquality.mirrorlab.internal.config.MirrorLabProperties;
import lab.paymentquality.mirrorlab.internal.domain.MirrorLabConsent;
import lab.paymentquality.mirrorlab.internal.domain.MirrorLabDispute;
import lab.paymentquality.mirrorlab.internal.domain.MirrorLabDisputeEvidence;
import lab.paymentquality.mirrorlab.internal.domain.MirrorLabRefundApproval;
import lab.paymentquality.mirrorlab.internal.infrastructure.JpaMirrorLabConsentRepository;
import lab.paymentquality.mirrorlab.internal.infrastructure.JpaMirrorLabDisputeEvidenceRepository;
import lab.paymentquality.mirrorlab.internal.infrastructure.JpaMirrorLabDisputeRepository;
import lab.paymentquality.mirrorlab.internal.infrastructure.JpaMirrorLabRefundApprovalRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("!prod")
@ConditionalOnProperty(name = "app.mirror-lab.enabled", havingValue = "true")
public class MirrorLabService {

    public static final long MAX_EVIDENCE_SIZE_BYTES = 2L * 1024L * 1024L;
    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/png", "image/jpeg", "text/plain", "text/csv");

    private static final int TPP_LIMIT_PER_MINUTE = 30;

    private final ConcurrentHashMap<String, long[]> tppWindows = new ConcurrentHashMap<>();
    private final MirrorLabProperties properties;
    private final JpaMirrorLabDisputeRepository disputeRepository;
    private final JpaMirrorLabDisputeEvidenceRepository evidenceRepository;
    private final JpaMirrorLabRefundApprovalRepository approvalRepository;
    private final JpaMirrorLabConsentRepository consentRepository;

    public MirrorLabService(
            MirrorLabProperties properties,
            JpaMirrorLabDisputeRepository disputeRepository,
            JpaMirrorLabDisputeEvidenceRepository evidenceRepository,
            JpaMirrorLabRefundApprovalRepository approvalRepository,
            JpaMirrorLabConsentRepository consentRepository) {
        this.properties = properties;
        this.disputeRepository = disputeRepository;
        this.evidenceRepository = evidenceRepository;
        this.approvalRepository = approvalRepository;
        this.consentRepository = consentRepository;
    }

    public Map<String, Object> highValueRefund(long amountMinor, UUID merchantId, String stepUpHeader) {
        if (amountMinor >= properties.stepUpThresholdMinor()
                && !"confirmed".equalsIgnoreCase(stepUpHeader)) {
            throw new MirrorLabProblemException(
                    HttpStatus.FORBIDDEN,
                    "step_up_required",
                    "Amount " + amountMinor + " for merchant " + merchantId + " requires step-up");
        }
        return Map.of(
                "status", "accepted",
                "amountMinor", amountMinor,
                "merchantId", merchantId.toString());
    }

    public void assertTppRateLimit(String clientKey) {
        String key = clientKey == null || clientKey.isBlank() ? "anon" : clientKey;
        long now = System.currentTimeMillis();
        long[] slot = tppWindows.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing[0] > 60_000L) {
                return new long[] { now, 1L };
            }
            return new long[] { existing[0], existing[1] + 1L };
        });
        if (slot[1] > TPP_LIMIT_PER_MINUTE) {
            throw new MirrorLabProblemException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "rate_limit_exceeded",
                    "TPP lab token probing is rate limited");
        }
    }

    public byte[] statement(String format) {
        if ("pdf".equalsIgnoreCase(format)) {
            return "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        }
        return "date,amount\n2026-01-01,10.00\n".getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public MirrorLabDispute openDispute(UUID merchantId) {
        return disputeRepository.save(MirrorLabDispute.open(merchantId, Instant.now()));
    }

    @Transactional
    public MirrorLabDispute uploadEvidence(UUID disputeId, MultipartFile file) {
        MirrorLabDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new MirrorLabProblemException(HttpStatus.NOT_FOUND, "not_found", "Dispute not found"));
        if (file == null || file.isEmpty()) {
            throw new MirrorLabProblemException(HttpStatus.BAD_REQUEST, "empty_evidence_file", "Evidence file must not be empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new MirrorLabProblemException(HttpStatus.BAD_REQUEST, "invalid_evidence_filename", "Evidence filename must not contain path segments");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new MirrorLabProblemException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "unsupported_evidence_content_type",
                    "Evidence content type is not supported");
        }
        if (file.getSize() > MAX_EVIDENCE_SIZE_BYTES) {
            throw new MirrorLabProblemException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "evidence_file_too_large",
                    "Evidence file must not exceed 2 MB");
        }
        evidenceRepository.save(MirrorLabDisputeEvidence.create(
                disputeId, filename.trim(), contentType, file.getSize(), Instant.now()));
        dispute.close(Instant.now());
        return disputeRepository.save(dispute);
    }

    @Transactional
    public MirrorLabRefundApproval createApproval(UUID merchantId, long amountMinor, String maker) {
        Instant now = Instant.now();
        Instant stepUpUntil = amountMinor >= properties.stepUpThresholdMinor()
                ? now.plusSeconds(300)
                : null;
        return approvalRepository.save(MirrorLabRefundApproval.pending(merchantId, amountMinor, maker, now, stepUpUntil));
    }

    @Transactional
    public MirrorLabRefundApproval approve(UUID approvalId, String checker) {
        MirrorLabRefundApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new MirrorLabProblemException(HttpStatus.NOT_FOUND, "not_found", "Approval not found"));
        if (!"PENDING_APPROVAL".equals(approval.getStatus())) {
            throw new MirrorLabProblemException(HttpStatus.CONFLICT, "approval_not_pending", "Approval is not pending");
        }
        if (checker.equals(approval.getMakerSubject())) {
            throw new MirrorLabProblemException(HttpStatus.FORBIDDEN, "self_approve_forbidden", "Maker cannot approve");
        }
        approval.approve(checker, Instant.now());
        return approvalRepository.save(approval);
    }

    @Transactional
    public MirrorLabConsent grant(String owner) {
        String token = UUID.randomUUID().toString().replace("-", "");
        return consentRepository.save(MirrorLabConsent.grant(owner, token, Instant.now()));
    }

    @Transactional
    public void revoke(UUID consentId, String actor) {
        MirrorLabConsent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new MirrorLabProblemException(HttpStatus.NOT_FOUND, "not_found", "Consent not found"));
        if (!actor.equals(consent.getOwnerSubject())) {
            throw new MirrorLabProblemException(HttpStatus.FORBIDDEN, "consent_owner_mismatch", "Only the grant owner may revoke");
        }
        consent.revoke(Instant.now());
        consentRepository.save(consent);
    }

    @Transactional(readOnly = true)
    public void requireActiveConsent(String token) {
        MirrorLabConsent consent = consentRepository.findByAccessToken(token)
                .orElseThrow(() -> new MirrorLabProblemException(HttpStatus.FORBIDDEN, "consent_denied", "Unknown consent token"));
        if (!"GRANTED".equals(consent.getStatus())) {
            throw new MirrorLabProblemException(HttpStatus.FORBIDDEN, "consent_denied", "Consent revoked");
        }
    }
}
